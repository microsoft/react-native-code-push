# React Native plugin for CodePush

This plugin provides client-side integration for the [CodePush service](https://microsoft.github.io/code-push), allowing you to easily add a dynamic code update experience to your React Native apps.

The CodePush React Native API provides two primary mechanisms for discovering updates and dynamically applying them within your apps:

1. [**Sync mode**](#codepushsync), which allows you to call a single method--presumably as part of mounting your app's root component or in response to a button click--that will automatically check for an update, download and apply it, while respecting the policies and metadata associated with each release (e.g. if the release is mandatory then it doesn't give the end-user the option to ignore it)
2. [**Advanced mode**](#codepushcheckforupdate), which provides a handful of "low-level" methods which give you complete control over the update experience, at the cost of added complexity.

When getting started using CodePush, we would recommended using the sync mode until you discover that it doesn't suit your needs. That said, if you have a user scenario
that isn't currently covered well by sync, please [let us know](mailto:codepushfeed@microsoft.com) since that would be valuable feedback.

*NOTE: We don't currently have support for an "automatic mode" which provides a "code-free" experience to adding in dynamic update discovery and acquisition. If that would be valuable to you, please [let us know](mailto:codepushfeed@microsoft.com).*

## Supported React Native platforms

- iOS
- Coming soon: Android (Try it out on [this branch](https://github.com/Microsoft/react-native-code-push/tree/first-check-in-for-android))

## How does it work?

<img src="https://cloud.githubusercontent.com/assets/116461/10835297/20b7cdf0-7e5e-11e5-8e44-ea6144839e5f.png" align="right" />

A React Native application's assets (JavaScript code and other resources) are traditionally bundled up as a ```.jsbundle``` file which is loaded from the application installation location on the target device during runtime. After you submit an update to the store, the user downloads the update, and those assets will be replaced with the new assets.

CodePush is here to simplify this process by allowing you to instantly update your application's assets without having to submit a new update to the store. We do this by allowing you to upload and manage your React Native app bundles on our CodePush server. In the application, we check for the presence of updated bundles on the server. If they are available, we will install and persist them to the internal storage of the device. If a new bundle is installed, the application will reload from the updated package location.

## Plugin Acquisition

Acquire the React Native CodePush plugin by running the following command within your app's root directory:

```
npm install --save react-native-code-push
```

## Plugin Installation 

Once you've acquired the CodePush plugin, you need to integrate it into the Xcode project of your React Native app. To do this, take the following steps:

1. Open your app's Xcode project
2. Find the `CodePush.xcodeproj` file witin the `node_modules/react-native-code-push` directory, and drag it into the `Libraries` node in Xcode

    ![Add CodePush to project](https://cloud.githubusercontent.com/assets/516559/10322414/7688748e-6c32-11e5-83c1-00d3e6758df4.png)

3. Select the project node in Xcode and select the "Build Phases" tab of your project configuration.
4. Drag `libCodePush.a` from `Libraries/CodePush.xcodeproj/Products` into the "Link Binary With Libraries" secton of your project's "Build Phases" configuration.

    ![Link CodePush during build](https://cloud.githubusercontent.com/assets/516559/10322221/a75ea066-6c31-11e5-9d88-ff6f6a4d6968.png)

5. Under the "Build Settings" tab of your project configuration, find the "Header Search Paths" section and edit the value.
Add a new value, `$(SRCROOT)/../node_modules/react-native-code-push` and select "recursive" in the dropdown.

    ![Add CodePush library reference](https://cloud.githubusercontent.com/assets/516559/10322038/b8157962-6c30-11e5-9264-494d65fd2626.png)

## Plugin Configuration

Once your Xcode project has been setup to build/link the CodePush plugin, you need to configure your app to consult CodePush for the location of your JS bundle, since it will "take control" of managing the current and all future versions. To do this, perform the following steps:

1. Open up the `AppDelegate.m` file, and add an import statement for the CodePush headers:

    ```
    #import "CodePush.h"
    ```

2. Find the following line of code, which loads your JS Bundle from the packager's dev server:

    ```
    jsCodeLocation = [NSURL URLWithString:@"http://localhost:8081/index.ios.bundle?platform=ios&dev=true"];
    ```
3. Replace it with this line:

    ```
    jsCodeLocation = [CodePush getBundleUrl];
    ```

This change configures your app to always load the most recent version of your app's JS bundle. On the initial launch, this will correspond to the file that was compiled with the app. However, after an update has been pushed via CodePush, this will return the location of the most recently applied update.

To let the CodePush runtime know which deployment it should query for updates against, perform the following steps:

1. Open your app's `Info.plist` and add a new `CodePushDeploymentKey` entry, whose value is the key of the deployment you want to configure this app against (e.g. the Staging deployment for FooBar app)
2. In your app's `Info.plist` make sure your `CFBundleShortVersionString` value is a valid [semver](http://semver.org/) version (e.g. 1.0.0 not 1.0)

## Plugin consumption

With the CodePush plugin downloaded and linked, and your app asking CodePush where to get the right JS bundle from, the only thing left is to add the neccessary code to your app to control the following:

1. When (and how often) to check for an update? (e.g. app start, in response to clicking a button in a settings page, periodically at some fixed interval)
2. When an update is available, how to present it to the end-user?

The simplest way to do this is to perform the following in your app's root component:

1. Import the JavaScript module for CodePush:

    ```
    var CodePush = require("react-native-code-push")
    ```

2. Call the `sync` method from within the `componentDidMount` lifecycle event, to initiate a background update on each app start:

    ```
    CodePush.sync();
    ```

If an update is available, a dialog will be displayed to the user asking them if they would like to install it. If the update was marked as mandatory, then the dialog will
omit the option to decline installation. The `sync` method takes a handful of options to customize this experience, so refer to its [API reference](#codepushsync) if you'd like to tweak its default behavior.

## Releasing code updates

Once your app has been configured and distributed to your users, and you've made some JS changes, it's time to release it to them instantly! To do this, run the following steps:

1. Execute `react-native bundle` in order to generate the JS bundle for your app.
2. Execute `code-push release <appName> <deploymentName> ./ios/main.jsbundle <appVersion>` in order to publish the generated JS bundle to the server (assuming your CWD is the root directory of your React Native app).

And that's it! For more information regarding the CodePush API, including the various options you can pass to the `sync` method, refer to the reference section below.

---

## API Reference

### Top-level module methods

When you require the `react-native-code-push` module, that object provides the following methods directly on it:

* [checkForUpdate](#codepushcheckforupdate): Queries the CodePush service for an update against the configured deployment. This method returns a promise which resolves to a `RemotePackage` that can be subsequently downloaded.
* [getCurrentPackage](#codepushgetcurrentpackage): Gets information about the currently applied package (e.g. description, installation time)
* [notifyApplicationReady](#codepushnotifyapplicationready): Notifies the CodePush runtime that an applied update is considered successful. This is an optional API, but is useful when you want to expicitly enable "rollback protection" in the event that an exception occurs in any code that you've deployed to production
* [sync](#codepushsync): Allows checking for an update, downloading it and applying it, all with a single call. Unless you need custom UI and/or behavior, we recommend most developers to use this method when integrating CodePush into their apps

#### codePush.checkForUpdate

```javascript
codePush.checkForUpdate(): Promise<RemotePackage>;
```

Queries the CodePush service for an update against the configured deployment. This method returns a promise which resolves to a `RemotePackage` that can be subsequently downloaded.

`checkForUpdate` returns a Promise that resolves to one of two values:

* `null` if there is no update available
* A `RemotePackage` instance that represents an available update that can be downloaded

Example Usage: 

```javascript
codePush.checkForUpdate().then((update) => {
    if (!update) {
        console.log("The app is up to date!"); 
    } else {
        console.log("An update is available! Should we download it?");
    }
});

```

#### codePush.getCurrentPackage

```javascript
codePush.getCurrentPackage(): Promise<LocalPackage>;
```

Gets information about the currently applied package (e.g. description, installation time).

This method returns a Promise that resolves with the `LocalPackage` instance that represents the running update. This API is only useful for advanced scenarios, and so many devs won't need to concern themselves with it.

#### codePush.notifyApplicationReady

```javascript
codePush.notifyApplicationReady(): Promise<void>;
```

Notifies the CodePush runtime that an update is considered successful, and therefore, a rollback isn't neccessary. Calling this function is required whenever the `rollbackTimeout` parameter is specified when calling either ```LocalPackage.apply``` or `sync`. If you specify a `rollbackTimeout`, and don't call `notifyApplicationReady`, the CodePush runtime will assume that the applied update has failed and roll back to the previous version.

If the `rollbackTimeout` parameter was not specified, the CodePush runtime will not enforce any automatic rollback behavior, and therefore, calling this function is not required and will result in a no-op.

#### codePush.sync

```javascript
codePush.sync(options: Object): Promise<Number>;
```

Provides a simple option for checking for an update, displaying a notification to the user, downloading it and then applying it, all while also respecting the policy that your release was published with. This method effectively composes together the "advanced mode" APIs for you, so that you don't need to handle any of the following scenarios yourself:

1. Checking for an update and displaying a standard confirmation dialog asking if they would like to install it
2. Automatically ignoring updates which have previously failed to apply (due to automatic rollback), and therefore, likely don't make sense trying to apply again (let's blacklist them!)
3. Looking to see whether an available update is mandatory, and if so, don't give the end-user the choice to ignore it
4. Displaying the description of an update to the end-user as part of the install confirmation experience

If you want to pivot whether you check and/or download an available update based on the end-user's device battery level, network conditions, etc. then simply wrap the call to `sync` in a condition that ensures you only call it when desired.

The method accepts an options object that allows you to customize numerous aspects of the default behavior, all of which provide sensible values by default:

* __appendReleaseDescription__ (Boolean) - Indicates whether you would like to append the description of an available release to the notification message which is displayed to the end-user. Defaults to `false`.
* __descriptionPrefix__ (String) - Indicates the string you would like to prefix the release description with, if any, when displaying the update notification to the end-user. Defaults to `" Description: "`
* __ignoreFailedUpdates__ (Boolean) - Indicates whether you would like to automatically ignored updates which are available, but have been previously attemped to install, but failed. Defaults to `true`.
* __mandatoryContinueButtonLabel__ (String) - The text to use for the button the end-user must press in order to install a mandatory update. Defaults to `"Continue"`.
* __mandatoryUpdateMessage__ (String) - The text used as the body of an update notification, when the update is specified as mandatory. Defaults to `"An update is available that must be installed."`.
* __optionalIgnoreButtonLabel__ (String) - The text to use for the button the end-user can press in order to ignore an optional update that is available. Defaults to `"Ignore"`.
* __optionalInstallButtonLabel__ (String) - The text to use for the button the end-user can press in order to install an optional update. Defaults to `"Install"`.
* __optionalUpdateMessage__ (String) - The text used as the body of an update notification, when the update is optional. Defaults to `"An update is available. Would you like to install it?"`.
* __rollbackTimeout__ (String) - The number of seconds that you want the runtime to wait after an update has been applied before considering it failed and rolling it back. Defaults to `0`, which disabled rollback protection.
* __updateTitle__ (String) - The text used as the header of an update notification that is displayed to the end-user. Defaults to `"Update available"`.

The method returns a `Promise` that is resolved to a `SyncStatus` integer code, which indicates why the `sync` call succeeded. This code can be one of the following values:

* __CodePush.SyncStatus.UP_TO_DATE__ *(0)* - The app doesn't have an available update.
* __CodePush.SyncStatus.UPDATE_IGNORED__ *(1)* - The app has an optional update, that the user chose to ignore.
* __CodePush.SyncStatus.UPDATE_APPLIED__ *(2)* - The app had an optional or mandatory update that was successfully downloaded and is about to be applied. If your app needs to do any data persistence/migration before restarting, this is the time to do it.

If the update check and/or the subseqeuent download fails for any reason, the `Promise` object returned by `sync` will be rejected with the reason.

Example Usage: 

```javascript
codePush.sync()
    .then((status) => {
        if (status == codePush.SyncStatus.UPDATE_APPLIED) {
            // Do any neccessary work here before the app
            // is restarted in order to apply the update
        }
    })
    .catch((reason) => {
        // Do something with the failure  
    });
```

The `sync` method can be called anywhere you'd like to check for an update. That could be in the `componentWillMount` lifecycle event of your root component, the onPress handler of a `<TouchableHighlight>` component, in the callback of a periodic timer, or whatever else makes sense for your needs. Just like the `checkForUpdate` method, it will perform the network request to check for an update in the background, so it won't impact your UI thread and/or JavaScript thread's responsiveness.

### Package objects

The `checkForUpdate` and `getCurrentPackage` methods return promises, that when resolved, provide acces to "package" objects. The package represents your code update as well as any extra metadata (e.g. description, mandatory). The CodePush API has the distinction between the following types of packages:

* [LocalPackage](#localpackage): Represents a locally available package that either representing the currently running code or an update that hasn't been applied yet
* [RemotePackage](#remotepackage): Represents a remotely available package that provides an update to the app, and can be downloaded

#### LocalPackage

Contains details about an update package that has been downloaded locally or already applied (currently installed package). You can get a reference to an instance of this object either by calling the module-level `getCurrentPackage` method, or as the value of the promise returned by the `download` method of a RemotePackage.

##### Properties
- __appVersion__: The native version of the application this package update is intended for. (String)
- __deploymentKey__: Deployment key of the package. (String) This is the same value that you added to your `Info.plst` file.
- __description__: Package description. (String) This is the same value that you specified in the CLI when you released the update
- __failedApply__: Indicates whether this package instance had been previously applied but was rolled back. (Boolean) The `sync` method will automatically ignore updates which have previously failed, so you only need to worry about this property if using `checkForUpdate`.
- __label__: Package label. (String)
- __isMandatory__: Flag indicating if the update is mandatory. (Boolean) This is the value that you specified in the CLI when you released the update
- __packageHash__: The hash value of the package. (String)
- __packageSize__: The size of the package, in bytes. (Number)
- __isFirstRun__: Flag indicating whether this is the first time the package has been run after being applied. (Boolean) This is useful for determining whether you would like to show a "What's New?" UI to the end-user after applying an update.

##### Methods
- __apply(rollbackTimeout): Promise__: Applies this package to the application. The application will be reloaded with this package and on every application launch this package will be loaded.
If the rollbackTimeout parameter is provided, the application will wait for a `notifyApplicationReady` for the given number of milliseconds.
If `notifyApplicationReady` is called before the time period specified by rollbackTimeout, the apply operation is considered a success.
Otherwise, the apply operation will be marked as failed, and the application is reverted to its previous version.

#### RemotePackage

Contains details about an update package that is available for download. You get a reference to an instance this object by calling the `checkForUpdate` method when an update is available. If you are using the `sync` API, you don't need to worry about the `RemotePackage`, since it will handle the download and application process automatically for you.

##### Properties

The `RemotePackage` inherits all of the same properties as the `LocalPackage`, but includes one additional one:

- __downloadUrl__: The URL at which the package is available for download. (String). This property is only needed for advanced usage, since the `download` method will automatically handle the acquisition of updates for you.

##### Methods
- __download(): Promise<LocalPackage>__: Downloads the package update from the CodePush service. Returns a Promise that resolves with the `LocalPackage`.

---

## Running the Example

* Clone this repository
* From the root of this project, run `npm install`
* `cd` into `Examples/CodePushDemoApp`
* From this demo app folder, run `npm install`
* Open `Info.plist` and fill in the value for CodePushDeploymentKey
* Run `npm start` to launch the packager
* Open `CodePushDemoApp.xcodeproj` in Xcode
* Launch the project

## Running Tests

* Open `CodePushDemoApp.xcodeproj` in Xcode
* Navigate to the test explorer (small grey diamond near top left)
* Click on the 'play' button next to CodePushDemoAppTests
* After the tests are completed, green ticks should appear next to the test cases to indicate success
