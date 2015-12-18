import { DeviceEventEmitter } from "react-native";

// This function is used to augment remote and local
// package objects with additional functionality/properties
// beyond what is included in the metadata sent by the server.
module.exports = (NativeCodePush) => {
  const remote = {
    abortDownload() {
      return NativeCodePush.abortDownload(this);
    },
    
    download(downloadProgressCallback) {
      if (!this.downloadUrl) {
        return Promise.reject(new Error("Cannot download an update without a download url"));
      }

      var downloadProgressSubscription;
      if (downloadProgressCallback) {
        // Use event subscription to obtain download progress.   
        downloadProgressSubscription = DeviceEventEmitter.addListener(
          "CodePushDownloadProgress",
          downloadProgressCallback
        );
      }
      
      // Use the downloaded package info. Native code will save the package info
      // so that the client knows what the current package version is.
      return NativeCodePush.downloadUpdate(this)
        .then((downloadedPackage) => {
          downloadProgressSubscription && downloadProgressSubscription.remove();
          return Object.assign({}, downloadedPackage, local);
        })
        .catch((error) => {
          downloadProgressSubscription && downloadProgressSubscription.remove();
          // Rethrow the error for subsequent handlers down the promise chain.
          throw error;
        });
    },
    
    isPending: false // A remote package could never be in a pending state
  };

  const local = {
    install(installMode = NativeCodePush.codePushInstallModeOnNextRestart, updateInstalledCallback) {
      let localPackage = this;
      return NativeCodePush.installUpdate(this, installMode)
        .then(() => {
          updateInstalledCallback && updateInstalledCallback();
          if (installMode == NativeCodePush.codePushInstallModeImmediate) {
            NativeCodePush.restartApp();
          } else {
            localPackage.isPending = true; // Mark the package as pending since it hasn't been applied yet
          }
        });
    },
    
    isPending: false // A local package wouldn't be pending until it was installed
  };

  return { local, remote };
};