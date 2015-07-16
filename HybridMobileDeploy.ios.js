/**
 * @providesModule HybridMobileDeploy
 * @flow
 */

'use strict';

var NativeHybridMobileDeploy = require('NativeModules').HybridMobileDeploy;
var requestFetchAdapter = require("./request-fetch-adapter.js");
var semver = require('semver');
var Sdk = require("hybrid-mobile-deploy-sdk/script/acquisition-sdk");
var serverUrl;
var appName;
var sdk;

var HybridMobileDeploy = {
  queryUpdate: function(cb) {
    var pkg = {nativeVersion: "1.2.3", scriptVersion: "1.2.0"};
    sdk.queryUpdateWithCurrentPackage(pkg, cb);
  },
  installUpdate: function(update) {
    NativeHybridMobileDeploy.installUpdateFromUrl(update.updateUrl, update.bundleName, (err) => console.log(err), () => console.log("success"));
  }
};

module.exports = function(serverUrl, deploymentKey, ignoreNativeVersion) {
  sdk = new Sdk(requestFetchAdapter, {
      serverUrl: serverUrl,
      deploymentKey: deploymentKey,
      ignoreNativeVersion: ignoreNativeVersion
  });
  return HybridMobileDeploy; 
};
