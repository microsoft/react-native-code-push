"use strict";

import React from "react-native";
import CodePush from "react-native-code-push";
let NativeCodePush = React.NativeModules.CodePush;
let PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);
import createMockAcquisitionSdk from "../../utils/mockAcquisitionSdk";

let {
  AppRegistry,
  Platform,
  Text,
  View,
} = React;

let IsFailedUpdateTest = React.createClass({
  getInitialState() {
    return {};
  },
  componentDidMount() {
    let serverPackage = {
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
    
    let mockAcquisitionSdk = createMockAcquisitionSdk(serverPackage);       
    let mockConfiguration = { appVersion : "1.5.0" };
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
    let text = "Testing...";
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