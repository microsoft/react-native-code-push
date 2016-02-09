"use strict";

import React from "react-native";
import CodePush from "react-native-code-push";
import createTestCaseComponent from "../../utils/createTestCaseComponent";
import assert from "assert";
import createMockAcquisitionSdk from "../../utils/mockAcquisitionSdk";
import { serverPackage } from "../resources/testPackages";

const NativeCodePush = React.NativeModules.CodePush;
const PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
const localPackage = serverPackage;

let SamePackageTest = createTestCaseComponent(
  "SamePackageTest",
  "should not return an update when the server's version is the same as the local version",
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
    assert(!update, "checkForUpdate should not return a package when local package is identical");
  }
);

module.exports = SamePackageTest;