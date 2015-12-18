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

let FirstUpdateTest = createTestCaseComponent(
  "FirstUpdateTest",
  "should return an update when called from freshly installed binary if the server has one",
  () => {
    let mockAcquisitionSdk = createMockAcquisitionSdk(serverPackage, localPackage);
    let mockConfiguration = { appVersion : "1.5.0" };
    CodePush.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeCodePush);
    CodePush.getCurrentPackage = () => {
      return Promise.resolve(localPackage);
    }
    return Promise.resolve();
  },
  () => {
    return CodePush.checkForUpdate()
      .then((update) => {
        if (update) {
          assert.deepEqual(update, Object.assign(serverPackage, PackageMixins.remote));
        } else {
          throw new Error("checkForUpdate did not return the update from the server");
        }
      });
  }
);

module.exports = FirstUpdateTest;