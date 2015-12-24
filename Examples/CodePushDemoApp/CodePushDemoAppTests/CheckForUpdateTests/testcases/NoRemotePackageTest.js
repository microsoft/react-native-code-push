"use strict";

import React from "react-native";
import CodePush from "react-native-code-push";
const NativeCodePush = React.NativeModules.CodePush;
import createTestCaseComponent from "../../utils/createTestCaseComponent";
const PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
import assert from "assert";
import createMockAcquisitionSdk from "../../utils/mockAcquisitionSdk";

const serverPackage = null;
const localPackage =  {};

let NoRemotePackageTest = createTestCaseComponent(
  "NoRemotePackageTest",
  "should not return an update when the server has none",
  () => {
    let mockAcquisitionSdk = createMockAcquisitionSdk(serverPackage, localPackage);       
    let mockConfiguration = { appVersion : "1.5.0" };
    CodePush.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeCodePush);
    CodePush.getCurrentPackage = async () => {
      return localPackage;
    };
  },
  async () => {
    let update = await CodePush.checkForUpdate();
    assert(!update, "checkForUpdate should not return an update if there is none on the server");
  }
);

export default NoRemotePackageTest;