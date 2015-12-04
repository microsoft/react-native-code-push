'use strict';

var { Alert } = require("./AlertAdapter");
var NativeCodePush = require("react-native").NativeModules.CodePush;
var PackageMixins = require("./package-mixins")(NativeCodePush);
var requestFetchAdapter = require("./request-fetch-adapter.js");
var Sdk = require("code-push/script/acquisition-sdk").AcquisitionManager;
var semver = require("semver");

function checkForUpdate(deploymentKey = null) {
  var config, sdk;
  
  /*
   * Before we ask the server if an update exists, we
   * need to retrieve three pieces of information from the 
   * native side: deployment key, app version (e.g. 1.0.1)
   * and the hash of the currently running update (if there is one).
   * This allows the client to only receive updates which are targetted
   * for their specific deployment and version and which are actually
   * different from the CodePush update they have already installed.
   */
  return getConfiguration()
          .then((configResult) => {            
            /*
             * If a deployment key was explicitly provided,
             * then let's override the one we retrieved
             * from the native-side of the app. This allows
             * dynamically "redirecting" end-users at different
             * deployments (e.g. an early access deployment for insiders).
             */
            if (deploymentKey) {
              config = Object.assign({}, configResult, { deploymentKey });
            } else {
              config = configResult;
            }
            
            sdk = getSDK(config);
            
            // Allow dynamic overwrite of function. This is only to be used for tests.
            return module.exports.getCurrentPackage();
          })
          .then((localPackage) => {
            var queryPackage = { appVersion: config.appVersion };
            
            /*
             * If the app has a previously installed update, and that update
             * was targetted at the same app version that is currently running,
             * then we want to use its package hash to determine whether a new
             * release has been made on the server. Otherwise, we only need
             * to send the app version to the server, since we are interested
             * in any updates for current app store version, regardless of hash.
             */
            if (localPackage && semver.compare(localPackage.appVersion, config.appVersion) === 0) {
              queryPackage = localPackage;
            }
            
            return new Promise((resolve, reject) => {
              sdk.queryUpdateWithCurrentPackage(queryPackage, (err, update) => {
                if (err) {
                  return reject(err);
                }
                
                /*
                 * There are three cases where checkForUpdate will resolve to null:
                 * ----------------------------------------------------------------
                 * 1) The server said there isn't an update. This is the most common case.
                 * 2) The server said there is an update but it requires a newer binary version.
                 *    This would occur when end-users are running an older app store version than
                 *    is available, and CodePush is making sure they don't get an update that
                 *    potentially wouldn't be compatible with what they are running.
                 * 3) The server said there is an update, but the update's hash is the same as
                 *    the currently running update. This should _never_ happen, unless there is a
                 *    bug in the server, but we're adding this check just to double-check that the
                 *    client app is resilient to a potential issue with the update check.
                 */
                if (!update || update.updateAppVersion || (update.packageHash === localPackage.packageHash)) {
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

function getSDK(config) {
   if (testSdk) {
      return testSdk;
   } else {
      return new Sdk(requestFetchAdapter, config);
   }
}

/* Logs messages to console with the [CodePush] prefix */
function log(message) {
  console.log(`[CodePush] ${message}`)
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
    
    deploymentKey: null,
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
    checkForUpdate(syncOptions.deploymentKey)
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
            dialogButtons[0].text = syncOptions.updateDialog.mandatoryContinueButtonLabel;
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
  restartApp: NativeCodePush.restartApp,
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