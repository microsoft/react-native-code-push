"use strict";

import React from "react-native";
import CodePush from "react-native-code-push";
let NativeCodePush = React.NativeModules.CodePush;
import createTestCaseComponent from "../../utils/createTestCaseComponent";
let PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
import assert from "assert";
import createMockAcquisitionSdk from "../../utils/mockAcquisitionSdk";

let serverPackage = {
  appVersion: "1.5.0",
  description: "Angry flappy birds",
  downloadUrl: "http://www.windowsazure.com/blobs/awperoiuqpweru",
  isAvailable: true,
  isMandatory: false,
  packageHash: "hash240",
  packageSize: 1024,
  updateAppVersion: false
};

let localPackage = {};
let deploymentKey = "myKey123";

let SwitchDeploymentKeyTest = createTestCaseComponent(
  "SwitchDeploymentKeyTest",
  "should check for an update under the specified deployment key",
  () => {
    let mockAcquisitionSdk = createMockAcquisitionSdk(serverPackage, localPackage, deploymentKey);       
    let mockConfiguration = { appVersion : "1.5.0" };
    CodePush.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeCodePush);
    CodePush.getCurrentPackage = () => {
      return Promise.resolve(localPackage);
    }
    return Promise.resolve();
  },
  () => {
    return CodePush.checkForUpdate(deploymentKey)
      .then((update) => {
        if (update) {
          assert.deepEqual(update, Object.assign(serverPackage, PackageMixins.remote));
        } else {
          throw new Error("checkForUpdate did not return the update from the server");
        }
      });
  }
);

module.exports = SwitchDeploymentKeyTest;