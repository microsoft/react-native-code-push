code-push-react-native
===

React Native module for deploying script updates

Running the Example
---

* Make sure you have https://github.com/Microsoft/hybrid-mobile-deploy cloned beside the react-native project in a folder called `website`. This is hacky, and will be cleaned up as soon as React Native's packager supports symlinks.
* Start the CodePush server with `gulp serve`, after installing the prerequisites described in the [project readme](https://github.com/Microsoft/hybrid-mobile-deploy/blob/master/README.md)
* From the root of this project, run `npm install`
* `cd` into `Examples/CodePushDemoApp`
* From this demo app folder, run `npm install`
* Open `Info.plist` and fill in the values for CodePushDeploymentKey and CodePushServerUrl
* Run `npm start` to launch the packager
* Open `CodePushDemoApp.xcodeproj` in Xcode
* Launch the project

Running Tests
---

* Open `CodePushDemoApp.xcodeproj` in Xcode
* Navigate to the test explorer (small grey diamond near top left)
* Click on the 'play' button next to CodePushDemoAppTests
* After the tests are completed, green ticks should appear next to the test cases to indicate success