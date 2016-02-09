"use strict";

import React from "react-native";
import CodePush from "react-native-code-push";
import createTestCaseComponent from "../../utils/createTestCaseComponent";
import assert from "assert";
import createMockAcquisitionSdk from "../../utils/mockAcquisitionSdk";
import { updateAppVersionPackage as serverPackage } from "../resources/testPackages";

const NativeCodePush = React.NativeModules.CodePush;
const PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
const localPackage = {};

let RemotePackageAppVersionNewerTest = createTestCaseComponent(
  "RemotePackageAppVersionNewerTest",
  "should drop the update when the server reports one with a newer binary version",
  () => {
    let mockAcquisitionSdk = createMockAcquisitionSdk(serverPackage, localPackage);       
    let mockConfiguration = { appVersion : "1.0.0" };
    CodePush.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeCodePush);
    CodePush.getCurrentPackage = async () => {
      return localPackage;
    };
  },
  async () => {
    let update = await CodePush.checkForUpdate();
    assert(!update, "checkForUpdate should not return an update if remote package is of a different binary version");
  }
);

module.exports = RemotePackageAppVersionNewerTest;