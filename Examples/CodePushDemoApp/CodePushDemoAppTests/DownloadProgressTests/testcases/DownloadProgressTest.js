"use strict";

var React = require("react-native");
var CodePush = require("react-native-code-push");
var NativeCodePush = React.NativeModules.CodePush;
var createTestCaseComponent = require("../../utils/createTestCaseComponent");
var PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
var assert = require("assert");

var testPackages = require("../resources/TestPackages");
var localPackage = {};
var saveProgress;

function checkReceivedAndExpectedBytesEqual() {
  assert(saveProgress, "Download progress was not reported.");
  assert.equal(
    saveProgress.receivedBytes, 
    saveProgress.totalBytes, 
    `Bytes do not tally: Received bytes=${saveProgress.receivedBytes} Total bytes=${saveProgress.totalBytes}`
  );
  console.log("Downloaded one package.");
  saveProgress = null;
}

var DownloadProgressTest = createTestCaseComponent(
  "DownloadProgressTest",
  "should successfully download all the bytes contained in the test packages",
  () => {
    testPackages.forEach((aPackage, index) => {
      testPackages[index] = Object.assign(aPackage, PackageMixins.remote);
    });
    return Promise.resolve();
  },
  () => {
    var downloadProgressCallback = (downloadProgress) => {
      console.log(`Expecting ${downloadProgress.totalBytes} bytes, received ${downloadProgress.receivedBytes} bytes.`);
      saveProgress = downloadProgress;
    };
    
    // Chains promises together.
    return testPackages.reduce((aPackageDownloaded, nextPackage, index) => {
      return aPackageDownloaded
        .then(() => {
          // Skip the first time.
          index && checkReceivedAndExpectedBytesEqual();
          return nextPackage.download(downloadProgressCallback);
        })
    }, Promise.resolve());
  }
);

module.exports = DownloadProgressTest;