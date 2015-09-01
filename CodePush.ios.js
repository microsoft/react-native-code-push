/**
 * @providesModule CodePush
 * @flow
 */

'use strict';

var NativeCodePush = require('react-native').NativeModules.CodePush;
var requestFetchAdapter = require("./request-fetch-adapter.js");
var semver = require('semver');
var Sdk = require("code-push/script/acquisition-sdk").AcquisitionManager;
var sdk;
var config;

// This function is only used for tests. Replaces the default SDK, configuration and native bridge
function setUpTestDependencies(testSdk, testConfiguration, testNativeBridge){
  if (testSdk) sdk = testSdk;
  if (testConfiguration) config = testConfiguration;
  if (testNativeBridge) NativeCodePush = testNativeBridge;
}

function getConfiguration() {
  return Promise
    .resolve(config || NativeCodePush.getConfiguration())
    .then((configuration) => {
      if (!config) config = configuration;
      return config;
    });
}

function getSdk(callback) {
  if (sdk) {
    setImmediate(function() {
      callback(/*error=*/ null, sdk);
    });
  } else {
    getConfiguration().then(function(configuration) {
      sdk = new Sdk(requestFetchAdapter, configuration);
      callback(/*error=*/ null, sdk);
    });
  }
}

function checkForUpdate(callback) {
  getConfiguration().then(function(configuration) {
    getSdk(function(err, sdk) {
      if (err) callback(err);
      NativeCodePush.getLocalPackage(function(err, localPackage) {
        var queryPackage = {appVersion: configuration.appVersion};
        if (!err && localPackage && localPackage.appVersion === configuration.appVersion) {
          queryPackage = localPackage;
        } else if (err) {
          console.log(err);
        }

        sdk.queryUpdateWithCurrentPackage(queryPackage, callback);
      });
    });
  });
}

function download(updatePackage) {
  // Use the downloaded package info. Native code will save the package info
  // so that the client knows what the current package version is.
  return NativeCodePush.downloadUpdate(updatePackage);
}

function apply(updatePackage) {
  return NativeCodePush.applyUpdate(updatePackage);
}

function getCurrentPackage() {
  return NativeCodePush.getCurrentPackage();
}

var CodePush = {
  getConfiguration: getConfiguration,
  checkForUpdate: checkForUpdate,
  download: download,
  apply: apply,
  getCurrentPackage: getCurrentPackage,
  setUpTestDependencies: setUpTestDependencies
};

module.exports = CodePush;
