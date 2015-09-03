module.exports = (NativeCodePush) => {
  return {
    remote: {
      download: function download() {
        // Use the downloaded package info. Native code will save the package info
        // so that the client knows what the current package version is.
        return NativeCodePush.downloadUpdate(this);
      }
    },
    local: {
    }
  };
};
