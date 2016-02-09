"use strict";

import React from "react-native";
import CodePush from "react-native-code-push";
import createTestCaseComponent from "../../utils/createTestCaseComponent";
import assert from "assert";
import testPackages from "../resources/TestPackages";

const NativeCodePush = React.NativeModules.CodePush;
const PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
const localPackage = {};

let saveProgress;

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

let DownloadProgressTest = createTestCaseComponent(
  "DownloadProgressTest",
  "should successfully download all the bytes contained in the test packages",
  () => {
    testPackages.forEach((aPackage, index) => {
      testPackages[index] = Object.assign(aPackage, PackageMixins.remote());
    });
    return Promise.resolve();
  },
  () => {
    let downloadProgressCallback = (downloadProgress) => {
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