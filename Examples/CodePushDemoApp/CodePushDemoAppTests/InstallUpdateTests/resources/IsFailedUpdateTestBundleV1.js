"use strict";

var React = require("react-native");
var CodePush = require("react-native-code-push");
var NativeCodePush = React.NativeModules.CodePush;
var PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
var createMockAcquisitionSdk = require("../../utils/mockAcquisitionSdk");

var {
  AppRegistry,
  Platform,
  Text,
  View,
} = React;

var IsFailedUpdateTest = React.createClass({
  getInitialState() {
    return {};
  },
  componentDidMount() {
    var serverPackage = {
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
      serverPackage.downloadUrl = "http://10.0.3.2:8081/CodePushDemoAppTests/InstallUpdateTests/resources/IsFailedUpdateTestBundleV2.includeRequire.runModule.bundle?platform=android&dev=true"
    } else if (Platform.OS === "ios") {
      serverPackage.downloadUrl = "http://localhost:8081/CodePushDemoAppTests/InstallUpdateTests/resources/IsFailedUpdateTestBundleV2.includeRequire.runModule.bundle?platform=ios&dev=true"
    }
    
    var mockAcquisitionSdk = createMockAcquisitionSdk(serverPackage);       
    var mockConfiguration = { appVersion : "1.5.0" };
    CodePush.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeCodePush);
    
    CodePush.notifyApplicationReady()
      .then(() => {
        return CodePush.checkForUpdate();
      })
      .then((remotePackage) => {
        if (remotePackage.failedInstall) {
          this.setState({ passed: true });
        } else {
          return remotePackage.download();
        }
      })
      .then((localPackage) => {
        return localPackage && localPackage.install(NativeCodePush.codePushInstallModeImmediate);
      });
  },
  render() {
    var text = "Testing...";
    if (this.state.passed !== undefined) {
      text = this.state.passed ? "Test Passed!" : "Test Failed!";
    }
    
    return (
      <View style={{backgroundColor: "white", padding: 40}}>
        <Text>
          {text}
        </Text>
      </View>
    );
  }
});

AppRegistry.registerComponent("IsFailedUpdateTest", () => IsFailedUpdateTest);