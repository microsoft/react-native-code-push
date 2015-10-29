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

/**
 * The sync method provides a simple, one-line experience for
 * incorporating the check, download and application of an update.
 * 
 * It simply composes the existing API methods together and adds additional
 * support for respecting mandatory updates, ignoring previously failed
 * releases, and displaying a standard confirmation UI to the end-user
 * when an update is available.
 */
function sync(options = {}) {  
  var syncOptions = {
    ignoreFailedUpdates: true,
    
    mandatoryContinueButtonLabel: "Continue",
    mandatoryUpdateMessage: "An update is available that must be installed.",
    
    optionalIgnoreButtonLabel: "Ignore",
    optionalInstallButtonLabel: "Install",
    optionalUpdateMessage: "An update is available. Would you like to install it?",
    
    rollbackTimeout: 0,
    
    updateTitle: "Update available",
    useReleaseDescription: false,
    
    ...options 
  };

  return new Promise((resolve, reject) => {
    checkForUpdate()
      .then((remotePackage) => {
        if (!remotePackage || (remotePackage.failedAppy && syncOptions.ignoreFailedUpdates)) {
          resolve(CodePush.SyncStatus.NO_UPDATE_AVAILABLE);
        }
        else {
          var message = null;
          var dialogButtons = [
            {
              text: null,
              onPress: () => { 
                remotePackage.download()
                  .then((localPackage) => {
                    resolve(CodePush.SyncStatus.UPDATE_DOWNLOADED);
                    localPackage.apply(syncOptions.rollbackTimeout);
                  }, reject);
              }
            }
          ];
          
          if (remotePackage.isMandatory) {
            message = syncOptions.mandatoryUpdateMessage;
            dialogButtons[0].text = syncOptions.mandatoryContinueButtonLabel;
          } else {
            message = syncOptions.optionalUpdateMessage;
            dialogButtons[0].text = syncOptions.optionalInstallButtonLabel;     
            
            // Since this is an optional update, add another button
            // to allow the end-user to ignore it       
            dialogButtons.push({
              text: syncOptions.optionalIgnoreButtonLabel,
              onPress: () => resolve(CodePush.SyncStatus.UPDATE_IGNORED)
            });
          }
          
          // If the update has a description, and the developer
          // explicitly chose to display it, then set that as the message
          if (syncOptions.useReleaseDescription && remotePackage.description) {
            message = remotePackage.description;  
          }
          
          AlertIOS.alert(syncOptions.updateTitle, message, dialogButtons);
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
    NO_UPDATE_AVAILABLE: 0, // The running app is up-to-date
    UPDATE_IGNORED: 1, // The app had an optional update and the end-user chose to ignore it
    UPDATE_DOWNLOADED: 2 // The app had an optional/mandatory update that was successfully downloaded and is about to be applied
  }
};

module.exports = CodePush;