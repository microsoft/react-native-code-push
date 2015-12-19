"use strict";

import React from "react-native";
import CodePush from "react-native-code-push";
let NativeCodePush = React.NativeModules.CodePush;
import createTestCaseComponent from "../../utils/createTestCaseComponent";
let PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
import assert from "assert";
import createMockAcquisitionSdk from "../../utils/mockAcquisitionSdk";

import { serverPackage } from "../resources/testPackages";
const localPackage = {};

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