react-native-code-push
===

React Native module for deploying script updates using the Code Push service.

Installation
---

```
npm install --save react-native-code-push
```

After installing the React Native Code Push plugin, open your project in Xcode. Open the `react-native-code-push` in Finder, and drag the `CodePush.xcodeproj` into the Libraries folder of Xcode.

In Xcode, click on your project, and select the "Build Phases" tab of your project configuration. Drag libCodePush.a from `Libraries/CodePush.xcodeproj/Products` into the "Link Binary With Libraries" secton of your project's "Build Phases" configuration.

Finally, edit your project's `AppDelegate.m`. Find the following code:

```
jsCodeLocation = [NSURL URLWithString:@"http://localhost:8081/index.ios.bundle?platform=ios&dev=true"];
```

Replace it with the following:

```
jsCodeLocation = [CodePush getBundleUrl];
```

This change allows Code Push to load the updated app location after an update has been applied.

Methods
---

* checkForUpdate: Checks the service for updates
* notifyApplicationReady: Notifies the plugin that the update operation succeeded.
* getCurrentPackage: Gets information about the currently applied package.

Objects
---

* LocalPackage: Contains information about a locally installed package.
* RemotePackage: Contains information about an updated package available for download.

Getting Started:
---

* Add the plugin to your app
* Open your app's `Info.plist` and add a "CodePushDeploymentKey" entry with your app's deployment key
* To publish an update for your app, run `react-native bundle`, and then publish `iOS/main.jsbundle` using the Code Push CLI.

Running the Example
---

* Clone this repository
* From the root of this project, run `npm install`
* `cd` into `Examples/CodePushDemoApp`
* From this demo app folder, run `npm install`
* Open `Info.plist` and fill in the value for CodePushDeploymentKey
* Run `npm start` to launch the packager
* Open `CodePushDemoApp.xcodeproj` in Xcode
* Launch the project

Running Tests
---

* Open `CodePushDemoApp.xcodeproj` in Xcode
* Navigate to the test explorer (small grey diamond near top left)
* Click on the 'play' button next to CodePushDemoAppTests
* After the tests are completed, green ticks should appear next to the test cases to indicate success
