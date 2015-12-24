"use strict";

import React, {
  AppRegistry,
  Platform,
  Text,
  View,
} from "react-native";
import CodePush from "react-native-code-push";
import createMockAcquisitionSdk from "../../utils/mockAcquisitionSdk";

const NativeCodePush = React.NativeModules.CodePush;
const PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);

let IsFailedUpdateTest = React.createClass({
  getInitialState() {
    return {};
  },
  async componentDidMount() {
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
    
    await CodePush.notifyApplicationReady()
    let remotePackage = await CodePush.checkForUpdate();
    if (remotePackage.failedInstall) {
      this.setState({ passed: true });
    } else {
      let localPackage = await remotePackage.download();
      return localPackage && await localPackage.install(NativeCodePush.codePushInstallModeImmediate);
    }
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