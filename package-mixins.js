var Platform = require("Platform");
var EventEmitter;

if (Platform.OS === "android") {
  var { DeviceEventEmitter } = require("react-native");
  EventEmitter = DeviceEventEmitter;
} else if (Platform.OS === "ios") {   
  var { NativeAppEventEmitter } = require("react-native");
  EventEmitter = NativeAppEventEmitter;
}

module.exports = (NativeCodePush) => {
  var remote = {
    abortDownload: function abortDownload() {
      return NativeCodePush.abortDownload(this);
    },
    download: function download(downloadProgressCallback) {
      if (!this.downloadUrl) {
        return Promise.reject(new Error("Cannot download an update without a download url"));
      }

      var downloadProgressSubscription;
      if (downloadProgressCallback) {
        // Use event subscription to obtain download progress.   
        downloadProgressSubscription = EventEmitter.addListener(
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
    }
  };

  var local = {
    install: function install(rollbackTimeout = 0, installMode = NativeCodePush.codePushInstallModeOnNextRestart, updateInstalledCallback) {
      return NativeCodePush.installUpdate(this, rollbackTimeout, installMode)
        .then(function() {
          updateInstalledCallback && updateInstalledCallback();
          if (installMode == NativeCodePush.codePushInstallModeImmediate) {
            NativeCodePush.restartImmedidateUpdate(rollbackTimeout);
          };
        });
    }
  };

  return {
    remote: remote,
    local: local
  };
};