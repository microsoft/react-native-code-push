var extend = require("extend");

module.exports = (NativeCodePush) => {
  var remote = {
    abortDownload: function abortDownload() {
      return NativeCodePush.abortDownload(this);
    },
    download: function download() {
      if (!this.downloadUrl) {
        return Promise.reject(new Error("Cannot download an update without a download url"));
      }

      // Use the downloaded package info. Native code will save the package info
      // so that the client knows what the current package version is.
      return NativeCodePush.downloadUpdate(this)
        .then((downloadedPackage) => {
          return extend({}, downloadedPackage, local);
        });
    }
  };

  var local = {
    apply: function apply(rollbackTimeout = 0, restartImmediately = true) {
      return NativeCodePush.applyUpdate(this, rollbackTimeout, restartImmediately);
    }
  };

  return {
    remote: remote,
    local: local
  };
};