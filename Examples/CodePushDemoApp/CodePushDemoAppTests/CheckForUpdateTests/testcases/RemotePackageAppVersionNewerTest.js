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
  description: "",
  downloadUrl: "",
  isAvailable: false,
  isMandatory: false,
  packageHash: "",
  updateAppVersion: true
};

let localPackage = {};

let RemotePackageAppVersionNewerTest = createTestCaseComponent(
  "RemotePackageAppVersionNewerTest",
  "should drop the update when the server reports one with a newer binary version",
  () => {
    return new Promise((resolve, reject) => { 
      let mockAcquisitionSdk = createMockAcquisitionSdk(serverPackage, localPackage);       
      let mockConfiguration = { appVersion : "1.0.0" };
      CodePush.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeCodePush);
      CodePush.getCurrentPackage = () => {
        return Promise.resolve(localPackage);
      }
      resolve();
    });
  },
  () => {
    return CodePush.checkForUpdate()
      .then((update) => {
        if (update) {
          throw new Error("checkForUpdate should not return an update if remote package is of a different binary version");
        }
      });
  }
);

module.exports = RemotePackageAppVersionNewerTest;