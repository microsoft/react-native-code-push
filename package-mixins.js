var extend = require("extend");

module.exports = (NativeCodePush) => {
  var remote = {
    download: function download() {
      // Use the downloaded package info. Native code will save the package info
      // so that the client knows what the current package version is.
      return NativeCodePush.downloadUpdate(this)
        .then((downloadedPackage) => {
          return extend({}, downloadedPackage, local);
        });
    },
    abortDownload: function abortDownload() {
      return NativeCodePush.abortDownload(this);
    }
  };

  var local = {
    apply: function apply(rollbackTimeout = 0) {
      return NativeCodePush.applyUpdate(this, rollbackTimeout);
    }
  };

  return {
    remote: remote,
    local: local
  };
};
