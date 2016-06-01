# Contributing

## Using the plugin

### Environment setup

`node.js` and `npm` are needed for using this project. `npm` comes bundled with the `node.js` installer. You can download the `node.js` installer here: https://nodejs.org/download/.

Once you have installed `node.js` and `npm`, install the dev dependencies for the project.

```
npm install
```

### Using the plugin manually

Follow these steps to test your modifications to the plugin manually:
- clone this repository
- install the dependencies

	Navigate to the root folder from your command line console and run:
	```
	npm install
	```
- install the plugin in a React-Native project

	Navigate to the root folder of your React-Native project from your command line console and run:
	```
	npm install local_path_to_your_clone_of_this_repo
	```
- configure the plugin using the steps in the README.md
- build and run your app on an emulator or device

## Test

### Environment setup

First, make sure you have installed the dependencies for the plugin by following the steps above.

Then, make sure you have installed `gulp`.

```
npm install -g gulp
```

To run Android tests, make sure you have `sdk\tools` and  `sdk\platform-tools` in your PATH.

To run iOS tests, make sure you've installed CocoaPods and have `.gem/bin` in your PATH.

### Supported platforms

The plugin has end to end tests for Android and iOS. Depending on your development machine OS, you can run some or all the tests.

OS            | Supported tests
------------- | -------------
OS X          | Android, iOS
Windows       | Android

### Test descriptions

The tests first build the app.

They then check if the required emulators are currently running.

If an Android emulator is not running, it attempts to boot an Android emulator named `emulator`. You can specify an emulator by adding `--androidemu yourEmulatorNameHere` as a command line option to the gulp task.

If an iOS simulator is not running, it attempts to boot the latest iOS iPhone simulator. You can specify a simulator by adding `--iosemu yourSimulatorNameHere` as a command line option to the gulp task.

If all the required emulators are not running and the tests fail to boot them, the tests will fail.

If you would like the tests to always restart the necessary emulators (killing them if they are currently running), add a `--clean` flag to the command.

The desired unit tests are then run.

If you would like to skip building, add a `-fast` to the end of the command you'd like to run. For example, `gulp test-ios` becomes `gulp test-ios-fast`.

There is a both a full unit test suite and a "core" set of unit tests that you may run. If you would like to run only the core tests, add a `--core` flag to the command.

If you would like to pull the plugin from NPM rather than running the tests on the local version, add a `--npm` flag to the command.

If you add a `--report` flag to the command, the mocha reporter outputs individual results files for each platform. These are `./test_android.xml`, `./test-ios-ui.xml`, and `./test-ios-wk.xml`.

#### Default

To run all of the unit tests on Android and iOS:
```
gulp test
```

#### iOS

To run all of the unit tests on iOS:
```
gulp test-ios
```

#### Android

To run all of the unit tests on Android:
```
gulp test-android
```

#### More examples

All possible testing configurations have tasks!

The platforms are ordered as follows, and ran in that order:
android, ios

To run the core unit tests on Android:
```
gulp test-android --core
```

To run all of the unit tests on iOS and pull the plugin from NPM:
```
gulp test-ios --npm
```

To run all of the unit tests on Android and iOS without building first:
```
gulp test-fast
```

To run all of the unit tests on iOS and restart the emulators:
```
gulp test-ios --clean
```

To run the core unit tests on Android and pull the plugin from NPM:
```
gulp test-android --core --npm
```

...and so on!