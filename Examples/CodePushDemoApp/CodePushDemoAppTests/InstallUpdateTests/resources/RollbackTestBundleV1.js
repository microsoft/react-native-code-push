"use strict";

var React = require("react-native");
var CodePush = require("react-native-code-push");
var RCTTestModule = React.NativeModules.TestModule;
var NativeCodePush = React.NativeModules.CodePush;
var PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);

var {
  AppRegistry,
  Platform,
  Text,
  View,
} = React;

var RollbackTest = React.createClass({
  componentDidMount() {
    var remotePackage = {
      description: "Angry flappy birds",
      appVersion: "1.5.0",
      label: "2.4.0",
      isMandatory: false,
      isAvailable: true,
      updateAppVersion: false,
      packageHash: "hash241",
      packageSize: 1024
    };

    if (Platform.OS === "android") {
      remotePackage.downloadUrl = "http://10.0.3.2:8081/CodePushDemoAppTests/InstallUpdateTests/resources/RollbackTestBundleV2.includeRequire.runModule.bundle?platform=android&dev=true"
      CodePush.notifyApplicationReady()
        .then(() => {
          NativeCodePush.downloadAndReplaceCurrentBundle("http://10.0.3.2:8081/CodePushDemoAppTests/InstallUpdateTests/resources/RollbackTestBundleV1Pass.includeRequire.runModule.bundle?platform=android&dev=true");
        });
    } else if (Platform.OS === "ios") {
      remotePackage.downloadUrl = "http://localhost:8081/CodePushDemoAppTests/InstallUpdateTests/resources/RollbackTestBundleV2.includeRequire.runModule.bundle?platform=ios&dev=true"
      CodePush.notifyApplicationReady()
        .then(() => {
          NativeCodePush.downloadAndReplaceCurrentBundle("http://localhost:8081/CodePushDemoAppTests/InstallUpdateTests/resources/RollbackTestBundleV1Pass.includeRequire.runModule.bundle?platform=ios&dev=true");
        });
    }
    
    remotePackage = Object.assign(remotePackage, PackageMixins.remote);

    remotePackage.download()
      .then((localPackage) => {
        return localPackage.install(NativeCodePush.codePushInstallModeImmediate);
      });
  },
  render() {
    return (
      <View style={{backgroundColor: "white", padding: 40}}>
        <Text>
          Testing...
        </Text>
      </View>
    );
  }
});

AppRegistry.registerComponent("RollbackTest", () => RollbackTest);