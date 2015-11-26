var { Platform } = require("react-native");

var packages = {
  smallPackage: {
    downloadUrl: "smallFile",
    description: "Angry flappy birds",
    appVersion: "1.5.0",
    label: "2.4.0",
    isMandatory: false,
    isAvailable: true,
    updateAppVersion: false,
    packageHash: "hash240",
    packageSize: 1024
  },
  mediumPackage: {
    downloadUrl: "mediumFile",
    description: "Angry flappy birds",
    appVersion: "1.5.0",
    label: "2.4.0",
    isMandatory: false,
    isAvailable: true,
    updateAppVersion: false,
    packageHash: "hash240",
    packageSize: 1024
  },
  largePackage: {
    downloadUrl: "largeFile",
    description: "Angry flappy birds",
    appVersion: "1.5.0",
    label: "2.4.0",
    isMandatory: false,
    isAvailable: true,
    updateAppVersion: false,
    packageHash: "hash240",
    packageSize: 1024
  }
};

for (var aPackage in packages) {
  if (Platform.OS === "android") {
    // Genymotion forwards 10.0.3.2 to host machine's localhost
    packages[aPackage].downloadUrl = "http://10.0.3.2:8081/CodePushDemoAppTests/DownloadProgressTests/" + packages[aPackage].downloadUrl;
  } else if (Platform.OS === "ios") {
    packages[aPackage].downloadUrl = "http://localhost:8081/CodePushDemoAppTests/DownloadProgressTests/" + packages[aPackage].downloadUrl;
  }
}

module.exports = packages;
