"use strict";

var React = require("react-native");
var CodePush = require("react-native-code-push");
var NativeCodePush = React.NativeModules.CodePush;
var createTestCaseComponent = require("../../utils/createTestCaseComponent");
var PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
var assert = require("assert");
var createMockAcquisitionSdk = require("../../utils/mockAcquisitionSdk");

var serverPackage = {
  appVersion: "1.5.0",
  description: "",
  downloadUrl: "",
  isAvailable: false,
  isMandatory: false,
  packageHash: "",
  updateAppVersion: true
};

var localPackage = {};

var RemotePackageAppVersionNewerTest = createTestCaseComponent(
  "RemotePackageAppVersionNewerTest",
  "should drop the update when the server reports one with a newer binary version",
  () => {
    return new Promise((resolve, reject) => { 
      var mockAcquisitionSdk = createMockAcquisitionSdk(serverPackage, localPackage);       
      var mockConfiguration = { appVersion : "1.0.0" };
      CodePush.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeCodePush);
      CodePush.getCurrentPackage = function () {
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