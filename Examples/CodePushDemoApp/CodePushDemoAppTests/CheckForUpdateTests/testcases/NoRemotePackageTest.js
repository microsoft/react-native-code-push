"use strict";

var React = require("react-native");
var CodePush = require("react-native-code-push");
var NativeCodePush = React.NativeModules.CodePush;
var createTestCaseComponent = require("../../utils/createTestCaseComponent");
var PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
var assert = require("assert");
var createMockAcquisitionSdk = require("../../utils/mockAcquisitionSdk");

var serverPackage = null;
var localPackage =  {};

var NoRemotePackageTest = createTestCaseComponent(
  "NoRemotePackageTest",
  "should not return an update when the server has none",
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
          throw new Error("checkForUpdate should not return an update if there is none on the server");
        }
      });
  }
);

module.exports = NoRemotePackageTest;