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
  description: "Angry flappy birds",
  downloadUrl: "http://www.windowsazure.com/blobs/awperoiuqpweru",
  isAvailable: true,
  isMandatory: false,
  packageHash: "hash240",
  packageSize: 1024,
  updateAppVersion: false
};

var localPackage = serverPackage;

var SamePackageTest = createTestCaseComponent(
  "SamePackageTest",
  "should not return an update when the server's version is the same as the local version",
  () => {
    var mockAcquisitionSdk = createMockAcquisitionSdk(serverPackage, localPackage);       
    var mockConfiguration = { appVersion : "1.5.0" };
    CodePush.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeCodePush);
    CodePush.getCurrentPackage = function () {
      return Promise.resolve(localPackage);
    }
    return Promise.resolve();
  },
  () => {
    return CodePush.checkForUpdate()
      .then((update) => {
        if (update) {
          throw new Error("checkForUpdate should not return a package when local package is identical");
        }
      });
  }
);

module.exports = SamePackageTest;