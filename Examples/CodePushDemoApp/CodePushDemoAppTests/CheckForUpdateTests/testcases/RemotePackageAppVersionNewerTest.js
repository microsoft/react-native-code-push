"use strict";

import React from "react-native";
import CodePush from "react-native-code-push";
let NativeCodePush = React.NativeModules.CodePush;
import createTestCaseComponent from "../../utils/createTestCaseComponent";
let PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
import assert from "assert";
import createMockAcquisitionSdk from "../../utils/mockAcquisitionSdk";

import { updateAppVersionPackage as serverPackage } from "../resources/testPackages";
let localPackage = {};

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
    let update = await CodePush.checkForUpdate()
    assert(!update, "checkForUpdate should not return an update if remote package is of a different binary version");
  }
);

export default RemotePackageAppVersionNewerTest;