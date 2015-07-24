/**
 * @providesModule HybridMobileDeploy
 * @flow
 */

'use strict';

var NativeHybridMobileDeploy = require('react-native').NativeModules.HybridMobileDeploy;
var requestFetchAdapter = require("./request-fetch-adapter.js");
var semver = require('semver');
var Sdk = require("hybrid-mobile-deploy-sdk/script/acquisition-sdk").AcquisitionManager;
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
  getSdk(function(err, sdk) {
    var pkg = {appVersion: "1.2.3"};
    sdk.queryUpdateWithCurrentPackage(pkg, callback);
  });
}

function installUpdate(update) {
  getConfiguration(function(err, config) {
    NativeHybridMobileDeploy.installUpdateFromUrl(config.serverUrl + "acquire/" + config.deploymentKey, (err) => console.log(err));
  });
}

var HybridMobileDeploy = {
  getConfiguration: getConfiguration,
  queryUpdate: queryUpdate,
  installUpdate: installUpdate
};

module.exports = HybridMobileDeploy;
