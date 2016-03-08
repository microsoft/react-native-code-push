import { AcquisitionManager as Sdk } from "code-push/script/acquisition-sdk";
import { Alert } from "./AlertAdapter";
import requestFetchAdapter from "./request-fetch-adapter";
import { Platform } from "react-native";

let NativeCodePush = require("react-native").NativeModules.CodePush;
const PackageMixins = require("./package-mixins")(NativeCodePush);

async function checkForUpdate(deploymentKey = null) {
  /*
   * Before we ask the server if an update exists, we
   * need to retrieve three pieces of information from the 
   * native side: deployment key, app version (e.g. 1.0.1)
   * and the hash of the currently running update (if there is one).
   * This allows the client to only receive updates which are targetted
   * for their specific deployment and version and which are actually
   * different from the CodePush update they have already installed.
   */
  const nativeConfig = await getConfiguration();
  
  /*
   * If a deployment key was explicitly provided,
   * then let's override the one we retrieved
   * from the native-side of the app. This allows
   * dynamically "redirecting" end-users at different
   * deployments (e.g. an early access deployment for insiders).
   */
  const config = deploymentKey ? { ...nativeConfig, ...{ deploymentKey } } : nativeConfig;
  const sdk = getPromisifiedSdk(requestFetchAdapter, config);

  // Use dynamically overridden getCurrentPackage() during tests.
  const localPackage = await module.exports.getCurrentPackage();
  
  /*
   * If the app has a previously installed update, and that update
   * was targetted at the same app version that is currently running,
   * then we want to use its package hash to determine whether a new
   * release has been made on the server. Otherwise, we only need
   * to send the app version to the server, since we are interested
   * in any updates for current app store version, regardless of hash.
   */
  let queryPackage;
  if (localPackage) {
    queryPackage = localPackage;
  } else {
    queryPackage = { appVersion: config.appVersion };
    if (Platform.OS === "ios" && config.packageHash) {
      queryPackage.packageHash = config.packageHash;
    }
  }
  
  const update = await sdk.queryUpdateWithCurrentPackage(queryPackage);
  
  /*
   * There are four cases where checkForUpdate will resolve to null:
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
   * 4) The server said there is an update, but the update's hash is the same as that
   *    of the binary's currently running version. This should only happen in Android -
   *    unlike iOS, we don't attach the binary's hash to the updateCheck request
   *    because we want to avoid having to install diff updates against the binary's
   *    version, which we can't do yet on Android.
   */
  if (!update || update.updateAppVersion || 
      localPackage && (update.packageHash === localPackage.packageHash) || 
      (!localPackage || localPackage._isDebugOnly) && config.packageHash === update.packageHash) {
    if (update && update.updateAppVersion) {
      log("An update is available but it is targeting a newer binary version than you are currently running.");
    }
    
    return null;
  } else {
    const remotePackage = { ...update, ...PackageMixins.remote(sdk.reportStatusDownload) };
    remotePackage.failedInstall = await NativeCodePush.isFailedUpdate(remotePackage.packageHash);
    remotePackage.deploymentKey = deploymentKey || nativeConfig.deploymentKey;
    return remotePackage;
  }
}

const getConfiguration = (() => {
  let config;
  return async function getConfiguration() {
    if (config) {
      return config;
    } else if (testConfig) {
      return testConfig;
    } else {
      config = await NativeCodePush.getConfiguration(); 
      return config;
    }
  }
})();

async function getCurrentPackage() {
  const localPackage = await NativeCodePush.getCurrentPackage();
  if (localPackage) {
      localPackage.failedInstall = await NativeCodePush.isFailedUpdate(localPackage.packageHash);
      localPackage.isFirstRun = await NativeCodePush.isFirstRun(localPackage.packageHash);
  }
  return localPackage;
}

function getPromisifiedSdk(requestFetchAdapter, config) {
  // Use dynamically overridden AcquisitionSdk during tests.
  const sdk = new module.exports.AcquisitionSdk(requestFetchAdapter, config);
  sdk.queryUpdateWithCurrentPackage = (queryPackage) => {
    return new Promise((resolve, reject) => {
      module.exports.AcquisitionSdk.prototype.queryUpdateWithCurrentPackage.call(sdk, queryPackage, (err, update) => {
        if (err) {
          reject(err);
        } else {
          resolve(update);
        }
      }); 
    });
  };

  sdk.reportStatusDeploy = (deployedPackage, status, previousLabelOrAppVersion, previousDeploymentKey) => {
    return new Promise((resolve, reject) => {
      module.exports.AcquisitionSdk.prototype.reportStatusDeploy.call(sdk, deployedPackage, status, previousLabelOrAppVersion, previousDeploymentKey, (err) => {
        if (err) {
          reject(err);
        } else {
          resolve();
        }
      }); 
    });
  };

  sdk.reportStatusDownload = (downloadedPackage) => {
    return new Promise((resolve, reject) => {
      module.exports.AcquisitionSdk.prototype.reportStatusDownload.call(sdk, downloadedPackage, (err) => {
        if (err) {
          reject(err);
        } else {
          resolve();
        }
      }); 
    });
  };

  return sdk;
}

/* Logs messages to console with the [CodePush] prefix */
function log(message) {
  console.log(`[CodePush] ${message}`)
}

// This ensures that notifyApplicationReadyInternal is only called once 
// in the lifetime of this module instance.
const notifyApplicationReady = (() => {
  let notifyApplicationReadyPromise;
  return () => {
    if (!notifyApplicationReadyPromise) {
      notifyApplicationReadyPromise = notifyApplicationReadyInternal();
    }
    
    return notifyApplicationReadyPromise;
  };
})();

async function notifyApplicationReadyInternal() {
  await NativeCodePush.notifyApplicationReady();  
  const statusReport = await NativeCodePush.getNewStatusReport();
  if (statusReport) {
    const config = await getConfiguration();
    const previousLabelOrAppVersion = statusReport.previousLabelOrAppVersion;
    const previousDeploymentKey = statusReport.previousDeploymentKey || config.deploymentKey;
    if (statusReport.appVersion) {
      const sdk = getPromisifiedSdk(requestFetchAdapter, config);
      sdk.reportStatusDeploy(/* deployedPackage */ null, /* status */ null, previousLabelOrAppVersion, previousDeploymentKey);
    } else {
      config.deploymentKey = statusReport.package.deploymentKey;
      const sdk = getPromisifiedSdk(requestFetchAdapter, config);
      sdk.reportStatusDeploy(statusReport.package, statusReport.status, previousLabelOrAppVersion, previousDeploymentKey);
    }
  }
}

function restartApp(onlyIfUpdateIsPending = false) {
  NativeCodePush.restartApp(onlyIfUpdateIsPending);
}

var testConfig;

// This function is only used for tests. Replaces the default SDK, configuration and native bridge
function setUpTestDependencies(testSdk, providedTestConfig, testNativeBridge) {
  if (testSdk) module.exports.AcquisitionSdk = testSdk;
  if (providedTestConfig) testConfig = providedTestConfig;
  if (testNativeBridge) NativeCodePush = testNativeBridge;
}

// This function allows only one syncInternal operation to proceed at any given time.
// Parallel calls to sync() while one is ongoing yields CodePush.SyncStatus.SYNC_IN_PROGRESS.
const sync = (() => {
  let syncInProgress = false;
  const setSyncCompleted = () => { syncInProgress = false; };
  
  return (options = {}, syncStatusChangeCallback, downloadProgressCallback) => {
    if (syncInProgress) {
      typeof syncStatusChangeCallback === "function"
        ? syncStatusChangeCallback(CodePush.SyncStatus.SYNC_IN_PROGRESS)
        : log("Sync already in progress.");
      return Promise.resolve(CodePush.SyncStatus.SYNC_IN_PROGRESS);
    } 
    
    syncInProgress = true;
    const syncPromise = syncInternal(options, syncStatusChangeCallback, downloadProgressCallback);
    syncPromise
      .then(setSyncCompleted)
      .catch(setSyncCompleted);

    return syncPromise;
  };
})();

/*
 * The syncInternal method provides a simple, one-line experience for
 * incorporating the check, download and installation of an update.
 * 
 * It simply composes the existing API methods together and adds additional
 * support for respecting mandatory updates, ignoring previously failed
 * releases, and displaying a standard confirmation UI to the end-user
 * when an update is available.
 */
async function syncInternal(options = {}, syncStatusChangeCallback, downloadProgressCallback) {
  let resolvedInstallMode;
  const syncOptions = {
    deploymentKey: null,
    ignoreFailedUpdates: true,
    installMode: CodePush.InstallMode.ON_NEXT_RESTART,
    mandatoryInstallMode: CodePush.InstallMode.IMMEDIATE,
    updateDialog: null,
    ...options 
  };
  
  syncStatusChangeCallback = typeof syncStatusChangeCallback === "function"
    ? syncStatusChangeCallback
    : (syncStatus) => {
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
            if (resolvedInstallMode == CodePush.InstallMode.ON_NEXT_RESTART) {
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
    
  downloadProgressCallback = typeof downloadProgressCallback === "function" 
    ? downloadProgressCallback 
    : (downloadProgress) => {
        log(`Expecting ${downloadProgress.totalBytes} bytes, received ${downloadProgress.receivedBytes} bytes.`);
      };
  
  try {
    await CodePush.notifyApplicationReady();
    
    syncStatusChangeCallback(CodePush.SyncStatus.CHECKING_FOR_UPDATE);
    const remotePackage = await checkForUpdate(syncOptions.deploymentKey);
    
    const doDownloadAndInstall = async () => {
      syncStatusChangeCallback(CodePush.SyncStatus.DOWNLOADING_PACKAGE);
      const localPackage = await remotePackage.download(downloadProgressCallback);
      
      // Determine the correct install mode based on whether the update is mandatory or not.
      resolvedInstallMode = localPackage.isMandatory ? syncOptions.mandatoryInstallMode : syncOptions.installMode;
      
      syncStatusChangeCallback(CodePush.SyncStatus.INSTALLING_UPDATE);
      await localPackage.install(resolvedInstallMode, () => {
        syncStatusChangeCallback(CodePush.SyncStatus.UPDATE_INSTALLED);
      });
      
      return CodePush.SyncStatus.UPDATE_INSTALLED;
    };
    
    const updateShouldBeIgnored = remotePackage && (remotePackage.failedInstall && syncOptions.ignoreFailedUpdates);
    if (!remotePackage || updateShouldBeIgnored) {
      if (updateShouldBeIgnored) {
          log("An update is available, but it is being ignored due to having been previously rolled back.");
      }
      
      syncStatusChangeCallback(CodePush.SyncStatus.UP_TO_DATE);
      return CodePush.SyncStatus.UP_TO_DATE;
    } else if (syncOptions.updateDialog) {
      // updateDialog supports any truthy value (e.g. true, "goo", 12),
      // but we should treat a non-object value as just the default dialog
      if (typeof syncOptions.updateDialog !== "object") {
        syncOptions.updateDialog = CodePush.DEFAULT_UPDATE_DIALOG;
      } else {
        syncOptions.updateDialog = { ...CodePush.DEFAULT_UPDATE_DIALOG, ...syncOptions.updateDialog };
      }
        
      return await new Promise((resolve, reject) => {  
        let message = null;
        const dialogButtons = [{
          text: null,
          onPress: async () => { 
            resolve(await doDownloadAndInstall());
          }
        }];
        
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
      });
    } else {
      return await doDownloadAndInstall();
    }
  } catch (error) {
    syncStatusChangeCallback(CodePush.SyncStatus.UNKNOWN_ERROR);
    log(error.message); 
    throw error;
  } 
};

let CodePush;

// If the "NativeCodePush" variable isn't defined, then 
// the app didn't properly install the native module,
// and therefore, it doesn't make sense initializing 
// the JS interface when it wouldn't work anyways.
if (NativeCodePush) {
    CodePush = {
        AcquisitionSdk: Sdk,
        checkForUpdate,
        getConfiguration,
        getCurrentPackage,
        log,
        notifyApplicationReady,
        restartApp,
        setUpTestDependencies,
        sync,
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
            SYNC_IN_PROGRESS: 7, // There is an ongoing "sync" operation in progress.
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
    }
} else {
    log("The CodePush module doesn't appear to be properly installed. Please double-check that everything is setup correctly.");
}

module.exports = CodePush;
