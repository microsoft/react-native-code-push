"use strict";

import React from "react-native";
import { DeviceEventEmitter, Platform, AppRegistry } from "react-native";
import CodePush from "react-native-code-push";
import createTestCaseComponent from "../../utils/createTestCaseComponent";
import assert from "assert";

const NativeCodePush = React.NativeModules.CodePush;
const PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);

let remotePackage = require("../resources/remotePackage");

let InstallModeOnNextRestartTest = createTestCaseComponent(
  "InstallModeOnNextRestartTest",
  "App should boot up the new version after it is installed and restarted",
  () => {
    if (Platform.OS === "android") {
      // Genymotion forwards 10.0.3.2 to host machine's localhost
      remotePackage.downloadUrl = "http://10.0.3.2:8081/CodePushDemoAppTests/InstallUpdateTests/resources/PassInstallModeOnNextRestartTest.includeRequire.runModule.bundle?platform=android&dev=true"
    } else if (Platform.OS === "ios") {
      remotePackage.downloadUrl = "http://localhost:8081/CodePushDemoAppTests/InstallUpdateTests/resources/PassInstallModeOnNextRestartTest.includeRequire.runModule.bundle?platform=ios&dev=true"
    }
    
    remotePackage = Object.assign(remotePackage, PackageMixins.remote());
  },
  async () => {
    let localPackage = await remotePackage.download();
    await localPackage.install(NativeCodePush.codePushInstallModeOnNextRestart);
    CodePush.restartApp();
  },
  /*passAfterRun*/ false
);

AppRegistry.registerComponent("InstallModeOnNextRestartTest", () => InstallModeOnNextRestartTest);