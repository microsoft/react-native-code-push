/**
 * @providesModule HybridMobileDeploy
 * @flow
 */

'use strict';

var NativeHybridMobileDeploy = require('react-native').NativeModules.HybridMobileDeploy;
var requestFetchAdapter = require("./request-fetch-adapter.js");
var semver = require('semver');
var Sdk = require("code-push/script/acquisition-sdk").AcquisitionManager;
var sdk;
var config;

function getConfiguration(callback) {
  if (config) {
    setImmediate(function() {
      callback(/*error=*/ null, config);
    });
  } else {
    NativeHybridMobileDeploy.getConfiguration(function(err, configuration) {
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
      NativeHybridMobileDeploy.getLocalPackage(function(err, localPackage) {
        var defaultPackage = {appVersion: configuration.appVersion};
        if (err) {   
          console.log(err);
          sdk.queryUpdateWithCurrentPackage(defaultPackage, callback);
        } else if (localPackage == null) {
          sdk.queryUpdateWithCurrentPackage(defaultPackage, callback);
        } else {
          if (localPackage.appVersion !== configuration.appVersion) {
            sdk.queryUpdateWithCurrentPackage(defaultPackage, callback)
          } else {
            sdk.queryUpdateWithCurrentPackage(localPackage, callback);
          }
        }
      });
    });
  });
}

function installUpdate(update) {
  // Use the downloaded package info. Native code will save the package info
  // so that the client knows what the current package version is.
  NativeHybridMobileDeploy.installUpdate(update, JSON.stringify(update), (err) => console.log(err));
}

var HybridMobileDeploy = {
  getConfiguration: getConfiguration,
  queryUpdate: queryUpdate,
  installUpdate: installUpdate
};

module.exports = HybridMobileDeploy;
