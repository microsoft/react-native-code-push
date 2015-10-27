/**
 * @providesModule CodePush
 * @flow
 */

'use strict';

var extend = require("extend");
var NativeCodePush = require('react-native').NativeModules.CodePush;
var requestFetchAdapter = require("./request-fetch-adapter.js");
var Sdk = require("code-push/script/acquisition-sdk").AcquisitionManager;
var packageMixins = require("./package-mixins")(NativeCodePush);
var { AlertIOS } = require("react-native");

// This function is only used for tests. Replaces the default SDK, configuration and native bridge
function setUpTestDependencies(providedTestSdk, providedTestConfig, testNativeBridge){
  if (providedTestSdk) testSdk = providedTestSdk;
  if (providedTestConfig) testConfig = providedTestConfig;
  if (testNativeBridge) NativeCodePush = testNativeBridge;
}
var testConfig;
var testSdk;

var getConfiguration = (() => {
  var config;
  return function getConfiguration() {
    if (config) {
      return Promise.resolve(config);
    } else if (testConfig) {
      return Promise.resolve(testConfig);
    } else {
      return NativeCodePush.getConfiguration()
        .then((configuration) => {
          if (!config) config = configuration;
          return config;
        });
    }
  }
})();

var getSdk = (() => {
  var sdk;
  return function getSdk() {
    if (sdk) {
      return Promise.resolve(sdk);
    } else if (testSdk) {
      return Promise.resolve(testSdk);
    } else {
      return getConfiguration()
        .then((configuration) => {
          sdk = new Sdk(requestFetchAdapter, configuration);
          return sdk;
        });
    }
  }
})();

function checkForUpdate() {
  var config;
  var sdk;
  return getConfiguration()
    .then((configResult) => {
      config = configResult;
      return getSdk();
    })
    .then((sdkResult) => {
      sdk = sdkResult;
      return getCurrentPackage();
    })
    .then((localPackage) => {
      var queryPackage = {appVersion: config.appVersion};
      if (localPackage && localPackage.appVersion === config.appVersion) {
        queryPackage = localPackage;
      }

      return new Promise((resolve, reject) => {
        sdk.queryUpdateWithCurrentPackage(queryPackage, (err, update) => {
          if (err) return reject(err);
          if (update) {
            // There is an update available for a different native app version. In the current version of this plugin, we treat that as no update.
            if (update.updateAppVersion) resolve(false);
            else resolve(extend({}, update, packageMixins.remote));
          } else {
            resolve(update);
          }
        });
      });
    });
}

function getCurrentPackage() {
  return NativeCodePush.getCurrentPackage();
}

function notifyApplicationReady() {
  return NativeCodePush.notifyApplicationReady();
}

function sync(options = {}) {  
  return new Promise((resolve, reject) => {
    checkForUpdate()
    .then((remotePackage) => {
      if (!remotePackage) {
        resolve(CodePush.SyncStatus.NO_UPDATE_AVAILABLE);
      }
      else {
        var dialogButtons = [
          {
            text: options.updateButtonText || "Update",
            onPress: () => { 
              remotePackage.download()
              .then((localPackage) => {
                resolve(CodePush.SyncStatus.APPLY_SUCCESS);
                localPackage.apply(options.rollbackTImeout);
              }, reject);
            }
          }
        ];
        
        if (!remotePackage.isMandatory) {
          dialogButtons.push({
            text: options.cancelButtonText || "Ignore",
            onPress: () => resolve(CodePush.SyncStatus.USER_CANCELLED)
          });
        }
        
        AlertIOS.alert(options.title || "Update available", remotePackage.description, dialogButtons);
      }
    }, reject);
  });     
};

var CodePush = {
  getConfiguration: getConfiguration,
  checkForUpdate: checkForUpdate,
  getCurrentPackage: getCurrentPackage,
  notifyApplicationReady: notifyApplicationReady,
  setUpTestDependencies: setUpTestDependencies,
  sync: sync,
  SyncStatus: {
    NO_UPDATE_AVAILABLE: 0,
    USER_CANCELLED: 1,
    APPLY_SUCCESS: 2    
  }
};

module.exports = CodePush;