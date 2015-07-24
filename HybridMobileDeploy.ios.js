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

function getConfiguration(cb) {
  if (config) {
    setTimeout(function() {
      cb(null, config);
    });
  } else {
    NativeHybridMobileDeploy.getConfiguration(function(err, configuration) {
      if (err) cb(err);
      config = configuration;
      cb(null, config);
    });
  }
}

function getSdk(cb) {
  if (sdk) {
    setTimeout(function() {
      cb(null, sdk);
    });
  } else {
    getConfiguration(function(err, configuration) {
      sdk = new Sdk(requestFetchAdapter, configuration);
      cb(null, sdk);
    });
  }
}

function queryUpdate(cb) {
  getSdk(function(err, sdk) {
    var pkg = {appVersion: "1.2.3"};
    sdk.queryUpdateWithCurrentPackage(pkg, cb);
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
