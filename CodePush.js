'use strict';

var requestFetchAdapter = require("./request-fetch-adapter.js");
var Sdk = require("code-push/script/acquisition-sdk").AcquisitionManager;
var { NativeCodePush, PackageMixins, Alert } = require("./CodePushNativePlatformAdapter");

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
            // Allow dynamic overwrite of function. This is only to be used for tests.
            return module.exports.getCurrentPackage();
          })
          .then((localPackage) => {
            var queryPackage = { appVersion: config.appVersion };
            if (localPackage && localPackage.appVersion === config.appVersion) {
              queryPackage = localPackage;
            }
            return new Promise((resolve, reject) => {
              sdk.queryUpdateWithCurrentPackage(queryPackage, (err, update) => {
                if (err) {
                  return reject(err);
                }
                
                // Ignore updates that require a newer app version,
                // since the end-user couldn't reliably install it
                if (!update || update.updateAppVersion) {
                  return resolve(null);
                }

                update = Object.assign(update, PackageMixins.remote);
                
                NativeCodePush.isFailedUpdate(update.packageHash)
                  .then((isFailedHash) => {
                    update.failedInstall = isFailedHash;
                    resolve(update);
                  })
                  .catch(reject)
                  .done();
              })
            });
          });
}

var isConfigValid = true;

var getConfiguration = (() => {
  var config;
  return function getConfiguration() {
    if (config && isConfigValid) {
      return Promise.resolve(config);
    } else if (testConfig) {
      return Promise.resolve(testConfig);
    } else {
      return NativeCodePush.getConfiguration()
        .then((configuration) => {
          if (!config) config = configuration;
          isConfigValid = true;
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

function getCurrentPackage() {
  return new Promise((resolve, reject) => {
    var localPackage;
    NativeCodePush.getCurrentPackage()
      .then((currentPackage) => {
        localPackage = currentPackage;
        return NativeCodePush.isFailedUpdate(currentPackage.packageHash);
      })
      .then((failedUpdate) => {
        localPackage.failedInstall = failedUpdate;
        return NativeCodePush.isFirstRun(localPackage.packageHash);
      })
      .then((isFirstRun) => {
        localPackage.isFirstRun = isFirstRun;
        resolve(localPackage);
      })
      .catch(reject)
      .done();
  });
}

/* Logs messages to console with the [CodePush] prefix */
function log(message) {
  console.log(`[CodePush] ${message}`)
}

function restartApp(rollbackTimeout = 0) {
  NativeCodePush.restartApp(rollbackTimeout);
}

function setDeploymentKey(deploymentKey) {
  return NativeCodePush.setDeploymentKey(deploymentKey)
    .then(() => {
        // Mark the local copy of the config data
        // as invalid since we just modified it
        // on the native end.
        isConfigValid = false;
    });  
}

var testConfig;
var testSdk;

// This function is only used for tests. Replaces the default SDK, configuration and native bridge
function setUpTestDependencies(providedTestSdk, providedTestConfig, testNativeBridge) {
  if (providedTestSdk) testSdk = providedTestSdk;
  if (providedTestConfig) testConfig = providedTestConfig;
  if (testNativeBridge) NativeCodePush = testNativeBridge;
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
function sync(options = {}, syncStatusChangeCallback, downloadProgressCallback) {  
  var syncOptions = {
    
    ignoreFailedUpdates: true,
    installMode: CodePush.InstallMode.ON_NEXT_RESTART,
    rollbackTimeout: 0,
    updateDialog: null,
    
    ...options 
  };
  
  syncStatusChangeCallback = typeof syncStatusChangeCallback == "function"
    ? syncStatusChangeCallback
    : function(syncStatus) {
        switch(syncStatus) {
          case CodePush.SyncStatus.CHECKING_FOR_UPDATE:
            log("Checking for update.");
            break;
          case CodePush.SyncStatus.AWAITING_USER_ACTION:
            log("Awaiting user action.");
            break;
          case CodePush.SyncStatus.DOWNLOADING_PACKAGE:
            log("Downloading package.");
            break;
          case CodePush.SyncStatus.INSTALLING_UPDATE:
            log("Installing update.");
            break;
          case CodePush.SyncStatus.UP_TO_DATE:
            log("App is up to date.");
            break;
          case CodePush.SyncStatus.UPDATE_IGNORED:
            log("User cancelled the update.");
            break;
          case CodePush.SyncStatus.UPDATE_INSTALLED:
            /* 
             * If the install mode is IMMEDIATE, this will not get returned as the
             * app will be restarted to a new Javascript context.
             */
            if (syncOptions.installMode == CodePush.InstallMode.ON_NEXT_RESTART) {
              log("Update is installed and will be run on the next app restart.");
            } else {
              log("Update is installed and will be run when the app next resumes.");
            }
            break;
          case CodePush.SyncStatus.UNKNOWN_ERROR:
            log("An unknown error occurred.");
            break;
        }
      };
    
  downloadProgressCallback = typeof downloadProgressCallback == "function" 
    ? downloadProgressCallback 
    : function(downloadProgress) {
        log(`Expecting ${downloadProgress.totalBytes} bytes, received ${downloadProgress.receivedBytes} bytes.`);
      };
  
  return new Promise((resolve, reject) => {
    syncStatusChangeCallback(CodePush.SyncStatus.CHECKING_FOR_UPDATE);
    checkForUpdate()
      .then((remotePackage) => {
        var doDownloadAndInstall = () => {
          syncStatusChangeCallback(CodePush.SyncStatus.DOWNLOADING_PACKAGE);
          remotePackage.download(downloadProgressCallback)
            .then((localPackage) => {
              syncStatusChangeCallback(CodePush.SyncStatus.INSTALLING_UPDATE);
              return localPackage.install(syncOptions.rollbackTimeout, syncOptions.installMode, () => {
                syncStatusChangeCallback(CodePush.SyncStatus.UPDATE_INSTALLED);
                resolve(CodePush.SyncStatus.UPDATE_INSTALLED);
              });
            })
            .catch(reject)
            .done();
        }
        
        if (!remotePackage || (remotePackage.failedInstall && syncOptions.ignoreFailedUpdates)) {
          syncStatusChangeCallback(CodePush.SyncStatus.UP_TO_DATE);
          resolve(CodePush.SyncStatus.UP_TO_DATE);
        }
        else if (syncOptions.updateDialog) {
          // updateDialog supports any truthy value (e.g. true, "goo", 12),
          // but we should treat a non-object value as just the default dialog
          if (typeof syncOptions.updateDialog !== "object") {
            syncOptions.updateDialog = CodePush.DEFAULT_UPDATE_DIALOG;
          } else {
            syncOptions.updateDialog = Object.assign({}, CodePush.DEFAULT_UPDATE_DIALOG, syncOptions.updateDialog);
          }
          
          var message = null;
          var dialogButtons = [
            {
              text: null,
              onPress: () => { 
                doDownloadAndInstall();
              }
            }
          ];
          
          if (remotePackage.isMandatory) {
            message = syncOptions.updateDialog.mandatoryUpdateMessage;
            dialogButtons[0].text = syncOptions.mandatoryContinueButtonLabel;
          } else {
            message = syncOptions.updateDialog.optionalUpdateMessage;
            dialogButtons[0].text = syncOptions.updateDialog.optionalInstallButtonLabel;     
            
            // Since this is an optional update, add another button
            // to allow the end-user to ignore it       
            dialogButtons.push({
              text: syncOptions.updateDialog.optionalIgnoreButtonLabel,
              onPress: () => {
                syncStatusChangeCallback(CodePush.SyncStatus.UPDATE_IGNORED);
                resolve(CodePush.SyncStatus.UPDATE_IGNORED);
              }
            });
          }
          
          // If the update has a description, and the developer
          // explicitly chose to display it, then set that as the message
          if (syncOptions.updateDialog.appendReleaseDescription && remotePackage.description) {
            message += `${syncOptions.updateDialog.descriptionPrefix} ${remotePackage.description}`;  
          }
          
          syncStatusChangeCallback(CodePush.SyncStatus.AWAITING_USER_ACTION);
          Alert.alert(syncOptions.updateDialog.title, message, dialogButtons);
        } else {
          doDownloadAndInstall();
        }
      })
      .catch((error) => {
        syncStatusChangeCallback(CodePush.SyncStatus.UNKNOWN_ERROR);
        reject(error);
      })
      .done();
  });     
};

var CodePush = {
  checkForUpdate: checkForUpdate,
  getConfiguration: getConfiguration,
  getCurrentPackage: getCurrentPackage,
  log: log,
  notifyApplicationReady: NativeCodePush.notifyApplicationReady,
  restartApp: restartApp,
  setDeploymentKey: setDeploymentKey,
  setUpTestDependencies: setUpTestDependencies,
  sync: sync,
  InstallMode: {
    IMMEDIATE: NativeCodePush.codePushInstallModeImmediate, // Restart the app immediately
    ON_NEXT_RESTART: NativeCodePush.codePushInstallModeOnNextRestart, // Don't artificially restart the app. Allow the update to be "picked up" on the next app restart
    ON_NEXT_RESUME: NativeCodePush.codePushInstallModeOnNextResume // Restart the app the next time it is resumed from the background
  },
  SyncStatus: {
    CHECKING_FOR_UPDATE: 0,
    AWAITING_USER_ACTION: 1,
    DOWNLOADING_PACKAGE: 2,
    INSTALLING_UPDATE: 3,
    UP_TO_DATE: 4, // The running app is up-to-date
    UPDATE_IGNORED: 5, // The app had an optional update and the end-user chose to ignore it
    UPDATE_INSTALLED: 6, // The app had an optional/mandatory update that was successfully downloaded and is about to be installed.
    UNKNOWN_ERROR: -1
  },
  DEFAULT_UPDATE_DIALOG: {
    appendReleaseDescription: false,
    descriptionPrefix: " Description: ",
    mandatoryContinueButtonLabel: "Continue",
    mandatoryUpdateMessage: "An update is available that must be installed.",
    optionalIgnoreButtonLabel: "Ignore",
    optionalInstallButtonLabel: "Install",
    optionalUpdateMessage: "An update is available. Would you like to install it?",
    title: "Update available"
  }
};

module.exports = CodePush;