[![appcenterbanner](https://user-images.githubusercontent.com/31293287/32969262-3cc5d48a-cb99-11e7-91bf-fa57c67a371c.png)](http://microsoft.github.io/code-push/)

# React Native Module for CodePush

*Note: This README is only relevant to the latest version of our plugin. If you are using an older version, please switch to the relevant tag on [our GitHub repo](https://github.com/Microsoft/react-native-code-push) to view the docs for that particular version.*

![Switching tags](https://cloud.githubusercontent.com/assets/8598682/17350832/ce0dec40-58de-11e6-9c8c-906bb114c34f.png)

This plugin provides client-side integration for the [CodePush service](https://microsoft.github.io/code-push/), allowing you to easily add a dynamic update experience to your React Native app(s).

<!-- React Native Catalog -->

* [How does it work?](#how-does-it-work)
* [Supported React Native Platforms](#supported-react-native-platforms)
* [Supported Components](#supported-components)
* [Getting Started](#getting-started)
    * [iOS Setup](docs/setup-ios.md)
    * [Android Setup](docs/setup-android.md)
    * [Windows Setup](docs/setup-windows.md)
* [Plugin Usage](#plugin-usage)
    * [Store Guideline Compliance](#store-guideline-compliance)
* [Releasing Updates](#releasing-updates)
* [Multi-Deployment Testing](#multi-deployment-testing)
    * [Android](docs/multi-deployment-testing-android.md)
    * [iOS](docs/multi-deployment-testing-ios.md)
* [Dynamic Deployment Assignment](#dynamic-deployment-assignment)
* [API Reference](#api-reference)
    * [JavaScript API](docs/api-js.md)
    * [Objective-C API Reference (iOS)](docs/api-ios.md)
    * [Java API Reference (Android)](docs/api-android.md)
* [Debugging / Troubleshooting](#debugging--troubleshooting)
* [Example Apps / Starters](#example-apps--starters)
* [Continuous Integration / Delivery](#continuous-integration--delivery)
* [TypeScript Consumption](#typescript-consumption)

<!-- React Native Catalog -->

## How does it work?

A React Native app is composed of JavaScript files and any accompanying [images](https://facebook.github.io/react-native/docs/images.html#content), which are bundled together by the [packager](https://github.com/facebook/react-native/tree/master/packager) and distributed as part of a platform-specific binary (i.e. an `.ipa` or `.apk` file). Once the app is released, updating either the JavaScript code (e.g. making bug fixes, adding new features) or image assets, requires you to recompile and redistribute the entire binary, which of course, includes any review time associated with the store(s) you are publishing to.

The CodePush plugin helps get product improvements in front of your end users instantly, by keeping your JavaScript and images synchronized with updates you release to the CodePush server. This way, your app gets the benefits of an offline mobile experience, as well as the "web-like" agility of side-loading updates as soon as they are available. It's a win-win!

In order to ensure that your end users always have a functioning version of your app, the CodePush plugin maintains a copy of the previous update, so that in the event that you accidentally push an update which includes a crash, it can automatically roll back. This way, you can rest assured that your newfound release agility won't result in users becoming blocked before you have a chance to [roll back](https://docs.microsoft.com/en-us/appcenter/distribution/codepush/cli#rolling-back-updates) on the server. It's a win-win-win!

*Note: Any product changes which touch native code (e.g. modifying your `AppDelegate.m`/`MainActivity.java` file, adding a new plugin) cannot be distributed via CodePush, and therefore, must be updated via the appropriate store(s).*

## Supported React Native platforms

- iOS (7+)
- Android (4.1+)
- Windows (UWP)

We try our best to maintain backwards compatibility of our plugin with previous versions of React Native, but due to the nature of the platform, and the existence of breaking changes between releases, it is possible that you need to use a specific version of the CodePush plugin in order to support the exact version of React Native you are using. The following table outlines which CodePush plugin versions officially support the respective React Native versions:

| React Native version(s) | Supporting CodePush version(s)                        |
|-------------------------|-------------------------------------------------------|
| <0.14                   | **Unsupported**                                       |
| v0.14                   | v1.3 *(introduced Android support)*                   |
| v0.15-v0.18             | v1.4-v1.6 *(introduced iOS asset support)*            |
| v0.19-v0.28             | v1.7-v1.17 *(introduced Android asset support)*       |
| v0.29-v0.30             | v1.13-v1.17 *(RN refactored native hosting code)*     |
| v0.31-v0.33             | v1.14.6-v1.17 *(RN refactored native hosting code)*   |
| v0.34-v0.35             | v1.15-v1.17 *(RN refactored native hosting code)*     |
| v0.36-v0.39             | v1.16-v1.17 *(RN refactored resume handler)*          |
| v0.40-v0.42             | v1.17 *(RN refactored iOS header files)*              |
| v0.43-v0.44             | v2.0+ *(RN refactored uimanager dependencies)*        |
| v0.45                   | v3.0+ *(RN refactored instance manager code)*         |
| v0.46                   | v4.0+ *(RN refactored js bundle loader code)*         |
| v0.46-v0.53             | v5.1+ *(RN removed unused registration of JS modules)*|
| v0.54-v0.55             | v5.3+ *(Android Gradle Plugin 3.x integration)*       |
| v0.56-v0.58             | v5.4+ *(RN upgraded versions for Android tools)*      |
| v0.59                   | v5.6+ *(RN refactored js bundle loader code)*         |

We work hard to respond to new RN releases, but they do occasionally break us. We will update this chart with each RN release, so that users can check to see what our "official" support is.

### Supported Components

When using the React Native assets system (i.e. using the `require("./foo.png")` syntax), the following list represents the set of core components (and props) that support having their referenced images updated via CodePush:

| Component                                       | Prop(s)                                  |
|-------------------------------------------------|------------------------------------------|
| `Image`                                         | `source`   |
| `MapView.Marker` <br />*(Requires [react-native-maps](https://github.com/lelandrichardson/react-native-maps) `>=O.3.2`)* | `image`                             |
| `ProgressViewIOS`                               | `progressImage`, `trackImage`            |
| `TabBarIOS.Item`                                | `icon`, `selectedIcon`                   |
| `ToolbarAndroid` <br />*(React Native 0.21.0+)* | `actions[].icon`, `logo`, `overflowIcon` |

The following list represents the set of components (and props) that don't currently support their assets being updated via CodePush, due to their dependency on static images (i.e. using the `{ uri: "foo" }` syntax):

| Component   | Prop(s)                                                              |
|-------------|----------------------------------------------------------------------|
| `SliderIOS` | `maximumTrackImage`, `minimumTrackImage`, `thumbImage`, `trackImage` |
| `Video`     | `source`                                                             |

As new core components are released, which support referencing assets, we'll update this list to ensure users know what exactly they can expect to update using CodePush.



## Getting Started

Once you've followed the general-purpose ["getting started"](https://docs.microsoft.com/en-us/appcenter/distribution/codepush/index) instructions for setting up your CodePush account, you can start CodePush-ifying your React Native app by running the following command from within your app's root directory:

```shell
npm install --save react-native-code-push
```

As with all other React Native plugins, the integration experience is different for iOS and Android, so perform the following setup steps depending on which platform(s) you are targeting. Note, if you are targeting both platforms it is recommended to create separate CodePush applications for each platform.

If you want to see how other projects have integrated with CodePush, you can check out the excellent [example apps](#example-apps--starters) provided by the community. Additionally, if you'd like to quickly familiarize yourself with CodePush + React Native, you can check out the awesome getting started videos produced by [Bilal Budhani](https://www.youtube.com/watch?v=uN0FRWk-YW8&feature=youtu.be) and/or [Deepak Sisodiya ](https://www.youtube.com/watch?v=f6I9y7V-Ibk).

*NOTE: This guide assumes you have used the `react-native init` command to initialize your React Native project. As of March 2017, the command `create-react-native-app` can also be used to initialize a React Native project. If using this command, please run `npm run eject` in your project's home directory to get a project very similar to what `react-native init` would have created.*

Then continue with installing the native module
  * [iOS Setup](docs/setup-ios.md)
  * [Android Setup](docs/setup-android.md)
  * [Windows Setup](docs/setup-windows.md)


## Plugin Usage

With the CodePush plugin downloaded and linked, and your app asking CodePush where to get the right JS bundle from, the only thing left is to add the necessary code to your app to control the following policies:

1. When (and how often) to check for an update? (e.g. app start, in response to clicking a button in a settings page, periodically at some fixed interval)

2. When an update is available, how to present it to the end user?

The simplest way to do this is to "CodePush-ify" your app's root component. To do so, you can choose one of the following two options:

* **Option 1: Wrap your root component with the `codePush` higher-order component:**

    ```javascript
    import codePush from "react-native-code-push";

    class MyApp extends Component {
    }

    MyApp = codePush(MyApp);
    ```

* **Option 2: Use the [ES7 decorator](https://github.com/wycats/javascript-decorators) syntax:**

    *NOTE: Decorators are not yet supported in Babel 6.x pending proposal update.* You may need to enable it by installing and using [babel-preset-react-native-stage-0](https://github.com/skevy/babel-preset-react-native-stage-0#babel-preset-react-native-stage-0).

    ```javascript
    import codePush from "react-native-code-push";

    @codePush
    class MyApp extends Component {
    }
    ```

By default, CodePush will check for updates on every app start. If an update is available, it will be silently downloaded, and installed the next time the app is restarted (either explicitly by the end user or by the OS), which ensures the least invasive experience for your end users. If an available update is mandatory, then it will be installed immediately, ensuring that the end user gets it as soon as possible.

If you would like your app to discover updates more quickly, you can also choose to sync up with the CodePush server every time the app resumes from the background.

```javascript
let codePushOptions = { checkFrequency: codePush.CheckFrequency.ON_APP_RESUME };

class MyApp extends Component {
}

MyApp = codePush(codePushOptions)(MyApp);
```

Alternatively, if you want fine-grained control over when the check happens (e.g. a button press or timer interval), you can call [`CodePush.sync()`](docs/api-js.md#codepushsync) at any time with your desired `SyncOptions`, and optionally turn off CodePush's automatic checking by specifying a manual `checkFrequency`:

```javascript
let codePushOptions = { checkFrequency: codePush.CheckFrequency.MANUAL };

class MyApp extends Component {
    onButtonPress() {
        codePush.sync({
            updateDialog: true,
            installMode: codePush.InstallMode.IMMEDIATE
        });
    }

    render() {
        return (
            <View>
                <TouchableOpacity onPress={this.onButtonPress}>
                    <Text>Check for updates</Text>
                </TouchableOpacity>
            </View> 
        )
    }
}

MyApp = codePush(codePushOptions)(MyApp);
```

If you would like to display an update confirmation dialog (an "active install"), configure when an available update is installed (e.g. force an immediate restart) or customize the update experience in any other way, refer to the [`codePush()`](docs/api-js.md#codepush) API reference for information on how to tweak this default behavior.

*NOTE: If you are using [Redux](http://redux.js.org) and [Redux Saga](http://yelouafi.github.io/redux-saga/), you can alternatively use the [react-native-code-push-saga](http://github.com/lostintangent/react-native-code-push-saga) module, which allows you to customize when `sync` is called in a perhaps simpler/more idiomatic way.*

### Store Guideline Compliance

While Google Play and internally distributed apps (e.g. Enterprise, Fabric, HockeyApp) have no limitations over how to publish updates using CodePush, the iOS App Store and its corresponding guidelines have more precise rules you should be aware of before integrating the solution within your application.

Paragraph **3.3.2**, since back in 2015's [Apple Developer Program License Agreement](https://developer.apple.com/programs/ios/information/) fully allowed performing over-the-air updates of JavaScript and assets -  and in its latest version (20170605) [downloadable here](https://developer.apple.com/terms/) this ruling is even broader:

> Interpreted code may be downloaded to an Application but only so long as such code: (a) does not change the primary purpose of the Application by providing features or functionality that are inconsistent with the intended and advertised purpose of the Application as submitted to the App Store, (b) does not create a store or storefront for other code or applications, and (c) does not bypass signing, sandbox, or other security features of the OS.

CodePush allows you to follow these rules in full compliance so long as the update you push does not significantly deviate your product from its original App Store approved intent.

To further remain in compliance with Apple's guidelines we suggest that App Store-distributed apps don't enable the `updateDialog` option when calling `sync`, since in the [App Store Review Guidelines](https://developer.apple.com/app-store/review/guidelines/) it is written that:

> Apps must not force users to rate the app, review the app, download other apps, or other similar actions in order to access functionality, content, or use of the app.

This is not necessarily the case for `updateDialog`, since it won't force the user to download the new version, but at least you should be aware of that ruling if you decide to show it.

## Releasing Updates

Once your app has been configured and distributed to your users, and you've made some JS and/or asset changes, it's time to instantly release them! The simplest (and recommended) way to do this is to use the `release-react` command in the CodePush CLI, which will handle bundling your JavaScript and asset files and releasing the update to the CodePush server.

In it's most basic form, this command only requires two parameters: your app name and the platform you are bundling the update for (either `ios` or `android`).

```shell
code-push release-react <appName> <platform>

code-push release-react MyApp-iOS ios
code-push release-react MyApp-Android android
```

The `release-react` command enables such a simple workflow because it provides many sensible defaults (e.g. generating a release bundle, assuming your app's entry file on iOS is either `index.ios.js` or `index.js`). However, all of these defaults can be customized to allow incremental flexibility as necessary, which makes it a good fit for most scenarios.

```shell
# Release a mandatory update with a changelog
code-push release-react MyApp-iOS ios -m --description "Modified the header color"

# Release an update for an app that uses a non-standard entry file name, and also capture
# the sourcemap file generated by react-native bundle
code-push release-react MyApp-iOS ios --entryFile MyApp.js --sourcemapOutput ../maps/MyApp.map

# Release a dev Android build to just 1/4 of your end users
code-push release-react MyApp-Android android --rollout 25% --dev true

# Release an update that targets users running any 1.1.* binary, as opposed to
# limiting the update to exact version name in the build.gradle file
code-push release-react MyApp-Android android --targetBinaryVersion "~1.1.0"

```

The CodePush client supports differential updates, so even though you are releasing your JS bundle and assets on every update, your end users will only actually download the files they need. The service handles this automatically so that you can focus on creating awesome apps and we can worry about optimizing end user downloads.

For more details about how the `release-react` command works, as well as the various parameters it exposes, refer to the [CLI docs](https://github.com/Microsoft/code-push/tree/master/cli#releasing-updates-react-native). Additionally, if you would prefer to handle running the `react-native bundle` command yourself, and therefore, want an even more flexible solution than `release-react`, refer to the [`release` command](https://github.com/Microsoft/code-push/tree/master/cli#releasing-updates-general) for more details.

If you run into any issues, or have any questions/comments/feedback, you can ping us within the [#code-push](https://discord.gg/0ZcbPKXt5bWxFdFu) channel on Reactiflux, [e-mail us](mailto:codepushfeed@microsoft.com) and/or check out the [troubleshooting](#debugging--troubleshooting) details below.

*NOTE: CodePush updates should be tested in modes other than Debug mode. In Debug mode, React Native app always downloads JS bundle generated by packager, so JS bundle downloaded by CodePush does not apply.*

### Multi-Deployment Testing

In our [getting started](#getting-started) docs, we illustrated how to configure the CodePush plugin using a specific deployment key. However, in order to effectively test your releases, it is critical that you leverage the `Staging` and `Production` deployments that are auto-generated when you first created your CodePush app (or any custom deployments you may have created). This way, you never release an update to your end users that you haven't been able to validate yourself.

*NOTE: Our client-side rollback feature can help unblock users after installing a release that resulted in a crash, and server-side rollbacks (i.e. `code-push rollback`) allow you to prevent additional users from installing a bad release once it's been identified. However, it's obviously better if you can prevent an erroneous update from being broadly released in the first place.*

Taking advantage of the `Staging` and `Production` deployments allows you to achieve a workflow like the following (feel free to customize!):

1. Release a CodePush update to your `Staging` deployment using the `code-push release-react` command (or `code-push release` if you need more control)

2. Run your staging/beta build of your app, sync the update from the server, and verify it works as expected

3. Promote the tested release from `Staging` to `Production` using the `code-push promote` command

4. Run your production/release build of your app, sync the update from the server and verify it works as expected

*NOTE: If you want to get really fancy, you can even choose to perform a "staged rollout" as part of #3, which allows you to mitigate additional potential risk with the update (e.g. did your testing in #2 touch all possible devices/conditions?) by only making the production update available to a percentage of your users (e.g. `code-push promote <APP_NAME> Staging Production -r 20%`). Then, after waiting for a reasonable amount of time to see if any crash reports or customer feedback comes in, you can expand it to your entire audience by running `code-push patch <APP_NAME> Production -r 100%`.*

You'll notice that the above steps refer to a "staging build" and "production build" of your app. If your build process already generates distinct binaries per "environment", then you don't need to read any further, since swapping out CodePush deployment keys is just like handling environment-specific config for any other service your app uses (e.g. Facebook). However, if you're looking for examples (**including demo projects**) on how to setup your build process to accommodate this, then refer to the following sections, depending on the platform(s) your app is targeting:

  * [Android](docs/multi-deployment-testing-android.md)
  * [iOS](docs/multi-deployment-testing-ios.md)


### Dynamic Deployment Assignment

The above section illustrated how you can leverage multiple CodePush deployments in order to effectively test your updates before broadly releasing them to your end users. However, since that workflow statically embeds the deployment assignment into the actual binary, a staging or production build will only ever sync updates from that deployment. In many cases, this is sufficient, since you only want your team, customers, stakeholders, etc. to sync with your pre-production releases, and therefore, only they need a build that knows how to sync with staging. However, if you want to be able to perform A/B tests, or provide early access of your app to certain users, it can prove very useful to be able to dynamically place specific users (or audiences) into specific deployments at runtime.

In order to achieve this kind of workflow, all you need to do is specify the deployment key you want the current user to syncronize with when calling the `codePush` method. When specified, this key will override the "default" one that was provided in your app's `Info.plist` (iOS) or `MainActivity.java` (Android) files. This allows you to produce a build for staging or production, that is also capable of being dynamically "redirected" as needed.

```javascript
// Imagine that "userProfile" is a prop that this component received
// which includes the deployment key that the current user should use.
codePush.sync({ deploymentKey: userProfile.CODEPUSH_KEY });
```

With that change in place, now it's just a matter of choosing how your app determines the right deployment key for the current user. In practice, there are typically two solutions for this:

1. Expose a user-visible mechanism for changing deployments at any time. For example, your settings page could have a toggle for enabling "beta" access. This model works well if you're not concerned with the privacy of your pre-production updates, and you have power users that may want to opt-in to earlier (and potentially buggy) updates at their own will (kind of like Chrome channels). However, this solution puts the decision in the hands of your users, which doesn't help you perform A/B tests transparently.

2. Annotate the server-side profile of your users with an additional piece of metadata which indicates the deployment they should sync with. By default, your app could just use the binary-embedded key, but after a user has authenticated, your server can choose to "redirect" them to a different deployment, which allows you to incrementally place certain users or groups in different deployments as needed. You could even choose to store the server-response in local storage so that it becomes the new default. How you store the key alongside your user's profiles is entirely up to your authentication solution (e.g. Auth0, Firebase, custom DB + REST API), but is generally pretty trivial to do.

*NOTE: If needed, you could also implement a hybrid solution that allowed your end-users to toggle between different deployments, while also allowing your server to override that decision. This way, you have a hierarchy of "deployment resolution" that ensures your app has the ability to update itself out-of-the-box, your end users can feel rewarded by getting early access to bits, but you also have the ability to run A/B tests on your users as needed.*

Since we recommend using the `Staging` deployment for pre-release testing of your updates (as explained in the previous section), it doesn't neccessarily make sense to use it for performing A/B tests on your users, as opposed to allowing early-access (as explained in option #1 above). Therefore, we recommend making full use of custom app deployments, so that you can segment your users however makes sense for your needs. For example, you could create long-term or even one-off deployments, release a variant of your app to it, and then place certain users into it in order to see how they engage.

```javascript
// #1) Create your new deployment to hold releases of a specific app variant
code-push deployment add [APP_NAME] test-variant-one

// #2) Target any new releases at that custom deployment
code-push release-react [APP_NAME] ios -d test-variant-one
```

*NOTE: The total user count that is reported in your deployment's "Install Metrics" will take into account users that have "switched" from one deployment to another. For example, if your `Production` deployment currently reports having 1 total user, but you dynamically switch that user to `Staging`, then the `Production` deployment would report 0 total users, while `Staging` would report 1 (the user that just switched). This behavior allows you to accurately track your release adoption, even in the event of using a runtime-based deployment redirection solution.*

---

## API Reference

* [JavaScript API](docs/api-js.md)
* [Objective-C API Reference (iOS)](docs/api-ios.md)
* [Java API Reference (Android)](docs/api-android.md)

### Example Apps / Starters

The React Native community has graciously created some awesome open source apps that can serve as examples for developers that are getting started. The following is a list of OSS React Native apps that are also using CodePush, and can therefore be used to see how others are using the service:

* [F8 App](https://github.com/fbsamples/f8app) - The official conference app for [F8 2016](https://www.fbf8.com/).
* [Feline for Product Hunt](https://github.com/arjunkomath/Feline-for-Product-Hunt) - An Android client for Product Hunt.
* [GeoEncoding](https://github.com/LynxITDigital/GeoEncoding) - An app by [Lynx IT Digital](https://digital.lynxit.com.au) which demonstrates how to use numerous React Native components and modules.
* [Math Facts](https://github.com/Khan/math-facts) - An app by Khan Academy to help memorize math facts more easily.

Additionally, if you're looking to get started with React Native + CodePush, and are looking for an awesome starter kit, you should check out the following:

* [Native Starter Pro](http://strapmobile.com/native-starter-pro/)
* [Pepperoni](http://getpepperoni.com/)

*Note: If you've developed a React Native app using CodePush, that is also open-source, please let us know. We would love to add it to this list!*

### Debugging / Troubleshooting

The `sync` method includes a lot of diagnostic logging out-of-the-box, so if you're encountering an issue when using it, the best thing to try first is examining the output logs of your app. This will tell you whether the app is configured correctly (e.g. can the plugin find your deployment key?), if the app is able to reach the server, if an available update is being discovered, if the update is being successfully downloaded/installed, etc. We want to continue improving the logging to be as intuitive/comprehensive as possible, so please [let us know](mailto:codepushfeed@microsoft.com) if you find it to be confusing or missing anything.

The simplest way to view these logs is to run the `code-push debug` command for the specific platform you are currently working with (e.g. `code-push debug ios`). This will output a log stream that is filtered to just CodePush messages, for the specified platform. This makes it easy to identify issues, without needing to use a platform-specific tool, or wade through a potentially high volume of logs.

<img width="540" alt="screen shot 2016-06-21 at 10 15 42 am" src="https://cloud.githubusercontent.com/assets/116461/16246973/838e2e98-37bc-11e6-9649-685f39e325a0.png">

Additionally, you can also use any of the platform-specific tools to view the CodePush logs, if you are more comfortable with them. Simple start up the Chrome DevTools Console, the Xcode Console (iOS), the [OS X Console](https://en.wikipedia.org/wiki/Console_%28OS_X%29#.7E.2FLibrary.2FLogs) (iOS) and/or ADB logcat (Android), and look for messages which are prefixed with `[CodePush]`.

Note that by default, React Native logs are disabled on iOS in release builds, so if you want to view them in a release build, you need to make the following changes to your `AppDelegate.m` file:

1. Add an `#import <React/RCTLog.h>` statement. For RN < v0.40 use: `#import "RCTLog.h"`

2. Add the following statement to the top of your `application:didFinishLaunchingWithOptions` method:

    ```objective-c
    RCTSetLogThreshold(RCTLogLevelInfo);
    ```

Now you'll be able to see CodePush logs in either debug or release mode, on both iOS or Android. If examining the logs don't provide an indication of the issue, please refer to the following common issues for additional resolution ideas:

| Issue / Symptom | Possible Solution |
|-----------------|-------------------|
| Compilation Error | Double-check that your version of React Native is [compatible](#supported-react-native-platforms) with the CodePush version you are using. |
| Network timeout / hang when calling `sync` or `checkForUpdate` in the iOS Simulator | Try resetting the simulator by selecting the `Simulator -> Reset Content and Settings..` menu item, and then re-running your app. |
| Server responds with a `404` when calling `sync` or `checkForUpdate` | Double-check that the deployment key you added to your `Info.plist` (iOS), `build.gradle` (Android) or that you're passing to `sync`/`checkForUpdate`, is in fact correct. You can run `code-push deployment ls [APP_NAME] -k` to view the correct keys for your app deployments. |
| Update not being discovered | Double-check that the version of your running app (e.g. `1.0.0`) matches the version you specified when releasing the update to CodePush. Additionally, make sure that you are releasing to the same deployment that your app is configured to sync with. |
| Update not being displayed after restart | If you're not calling `sync` on app start (e.g. within `componentDidMount` of your root component), then you need to explicitly call `notifyApplicationReady` on app start, otherwise, the plugin will think your update failed and roll it back. |
| I've released an update for iOS but my Android app also shows an update and it breaks it | Be sure you have different deployment keys for each platform in order to receive updates correctly |
| I've released new update but changes are not reflected | Be sure that you are running app in modes other than Debug. In Debug mode, React Native app always downloads JS bundle generated by packager, so JS bundle downloaded by CodePush does not apply.
| No JS bundle is being found when running your app against the iOS simulator | By default, React Native doesn't generate your JS bundle when running against the simulator. Therefore, if you're using `[CodePush bundleURL]`, and targetting the iOS simulator, you may be getting a `nil` result. This issue will be fixed in RN 0.22.0, but only for release builds. You can unblock this scenario right now by making [this change](https://github.com/facebook/react-native/commit/9ae3714f4bebdd2bcab4d7fdbf23acebdc5ed2ba) locally.

### Continuous Integration / Delivery

In addition to being able to use the CodePush CLI to "manually" release updates, we believe that it's important to create a repeatable and sustainable solution for contiously delivering updates to your app. That way, it's simple enough for you and/or your team to create and maintain the rhythm of performing agile deployments. In order to assist with seting up a CodePush-based CD pipeline, refer to the following integrations with various CI servers:

* [Visual Studio Team Services](https://marketplace.visualstudio.com/items?itemName=ms-vsclient.code-push) - *NOTE: VSTS also has extensions for publishing to [HockeyApp](https://marketplace.visualstudio.com/items?itemName=ms.hockeyapp) and the [Google Play](https://github.com/Microsoft/google-play-vsts-extension) store, so it provides a pretty great mobile CD solution in general.*
* [Travis CI](https://github.com/mondora/code-push-travis-cli)

Additionally, if you'd like more details of what a complete mobile CI/CD workflow  can look like, which includes CodePush, check out this [excellent article](https://zeemee.engineering/zeemee-engineering-and-the-quest-for-the-holy-mobile-dev-grail-1310be4953d1#.zfwaxtbco) by the [ZeeMee engineering team](https://zeemee.engineering).

### TypeScript Consumption

This module ships its `*.d.ts` file as part of its NPM package, which allows you to simply `import` it, and receive intellisense in supporting editors (e.g. Visual Studio Code), as well as compile-time type checking if you're using TypeScript. For the most part, this behavior should just work out of the box, however, if you've specified `es6` as the value for either the `target` or `module` [compiler option](http://www.typescriptlang.org/docs/handbook/compiler-options.html) in your [`tsconfig.json`](http://www.typescriptlang.org/docs/handbook/tsconfig-json.html) file, then just make sure that you also set the `moduleResolution` option to `node`. This ensures that the TypeScript compiler will look within the `node_modules` for the type definitions of imported modules. Otherwise, you'll get an error like the following when trying to import the `react-native-code-push` module: `error TS2307: Cannot find module 'react-native-code-push'`.

---

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
