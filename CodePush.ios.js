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

function getConfiguration(callback) {
  if (config) {
    setImmediate(function() {
      callback(/*error=*/ null, config);
    });
  } else {
    NativeCodePush.getConfiguration(function(err, configuration) {
      if (err) callback(err);
      config = configuration;
      callback(/*error=*/ null, config);
    });
  }
}

function getSdk(callback) {
  if (sdk) {
    setImmediate(function() {
      callback(/*error=*/ null, sdk);
    });
  } else {
    getConfiguration(function(err, configuration) {
      sdk = new Sdk(requestFetchAdapter, configuration);
      callback(/*error=*/ null, sdk);
    });
  }
}

function queryUpdate(callback) {
  getConfiguration(function(err, configuration) {
    if (err) callback(err);
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

function installUpdate(update) {
  // Use the downloaded package info. Native code will save the package info
  // so that the client knows what the current package version is.
  NativeCodePush.installUpdate(update, JSON.stringify(update), (err) => console.log(err));
}

var CodePush = {
  getConfiguration: getConfiguration,
  queryUpdate: queryUpdate,
  installUpdate: installUpdate,
  setUpTestDependencies: setUpTestDependencies
};

module.exports = CodePush;
