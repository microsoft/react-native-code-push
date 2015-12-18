"use strict";

var React = require("react-native");
var { DeviceEventEmitter, Platform, AppRegistry } = require("react-native");
var CodePush = require("react-native-code-push");
var NativeCodePush = React.NativeModules.CodePush;
var createTestCaseComponent = require("../../utils/createTestCaseComponent");
var PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
var assert = require("assert");

var remotePackage = {
  description: "Angry flappy birds",
  appVersion: "1.5.0",
  label: "2.4.0",
  isMandatory: false,
  isAvailable: true,
  updateAppVersion: false,
  packageHash: "hash240",
  packageSize: 1024
};

var RollbackTest = createTestCaseComponent(
  "RollbackTest",
  "should successfully rollback if \"notifyApplicationReady\" is not called in the installed package.",
  () => {
    if (Platform.OS === "android") {
      remotePackage.downloadUrl = "http://10.0.3.2:8081/CodePushDemoAppTests/InstallUpdateTests/resources/RollbackTestBundleV1.includeRequire.runModule.bundle?platform=android&dev=true"
    } else if (Platform.OS === "ios") {
      remotePackage.downloadUrl = "http://localhost:8081/CodePushDemoAppTests/InstallUpdateTests/resources/RollbackTestBundleV1.includeRequire.runModule.bundle?platform=ios&dev=true"
    }
    
    remotePackage = Object.assign(remotePackage, PackageMixins.remote);
    return Promise.resolve();
  },
  () => {
    remotePackage.download()
      .then((localPackage) => {
        return localPackage.install(NativeCodePush.codePushInstallModeImmediate);
      });
  },
  /*passAfterRun*/ false
);

AppRegistry.registerComponent("RollbackTest", () => RollbackTest);