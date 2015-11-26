var { Platform } = require("react-native");

var testPackage = {
  description: "Angry flappy birds",
  appVersion: "1.5.0",
  label: "2.4.0",
  isMandatory: false,
  isAvailable: true,
  updateAppVersion: false,
  packageHash: "hash240",
  packageSize: 1024
};

if (Platform.OS === "android") {
  // Genymotion forwards 10.0.3.2 to host machine's localhost
  testPackage.downloadUrl = "http://10.0.3.2:8081/CodePushDemoAppTests/InstallUpdateTests/CodePushDemoApp.includeRequire.runModule.bundle?platform=android&dev=true"
} else if (Platform.OS === "ios") {
  testPackage.downloadUrl = "http://localhost:8081/CodePushDemoAppTests/InstallUpdateTests/CodePushDemoApp.includeRequire.runModule.bundle?platform=ios&dev=true"
}

module.exports = testPackage;