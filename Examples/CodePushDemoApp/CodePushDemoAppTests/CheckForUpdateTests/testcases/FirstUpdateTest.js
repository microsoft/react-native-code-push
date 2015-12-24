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

let FirstUpdateTest = createTestCaseComponent(
  "FirstUpdateTest",
  "should return an update when called from freshly installed binary if the server has one",
  () => {
    let mockAcquisitionSdk = createMockAcquisitionSdk(serverPackage, localPackage);
    let mockConfiguration = { appVersion : "1.5.0" };
    CodePush.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeCodePush);
    CodePush.getCurrentPackage = async () => {
      return localPackage;
    };
  },
  async () => {
    let update = await CodePush.checkForUpdate()
    assert.deepEqual(update, Object.assign(serverPackage, PackageMixins.remote), "checkForUpdate did not return the update from the server");
  }
);

module.exports = FirstUpdateTest;