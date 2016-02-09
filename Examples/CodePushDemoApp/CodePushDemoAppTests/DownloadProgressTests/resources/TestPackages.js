import { Platform } from "react-native";

const packages = [
  {
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
  {
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
  {
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
];

packages.forEach((aPackage) => {
  if (Platform.OS === "android") {
    aPackage.downloadUrl = "http://10.0.3.2:8081/CodePushDemoAppTests/DownloadProgressTests/resources/" + aPackage.downloadUrl;
  } else if (Platform.OS === "ios") {
    aPackage.downloadUrl = "http://localhost:8081/CodePushDemoAppTests/DownloadProgressTests/resources/" + aPackage.downloadUrl;
  }
});

module.exports = packages;
