"use strict";

import React from "react-native";
import CodePush from "react-native-code-push";
import createTestCaseComponent from "../../utils/createTestCaseComponent";
import assert from "assert";
import createMockAcquisitionSdk from "../../utils/mockAcquisitionSdk";
import { serverPackage } from "../resources/testPackages";

const NativeCodePush = React.NativeModules.CodePush;
const PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
const localPackage = {};
const deploymentKey = "myKey123";

let SwitchDeploymentKeyTest = createTestCaseComponent(
  "SwitchDeploymentKeyTest",
  "should check for an update under the specified deployment key",
  () => {
    let mockAcquisitionSdk = createMockAcquisitionSdk(serverPackage, localPackage, deploymentKey);       
    let mockConfiguration = { appVersion : "1.5.0" };
    CodePush.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeCodePush);
    CodePush.getCurrentPackage = async () => {
      return localPackage;
    };
  },
  async () => {
    let update = await CodePush.checkForUpdate(deploymentKey);
    assert.equal(JSON.stringify(update), JSON.stringify({ ...serverPackage, ...PackageMixins.remote(), failedInstall: false, deploymentKey }), "checkForUpdate did not return the update from the server");
  }
);

module.exports = SwitchDeploymentKeyTest;