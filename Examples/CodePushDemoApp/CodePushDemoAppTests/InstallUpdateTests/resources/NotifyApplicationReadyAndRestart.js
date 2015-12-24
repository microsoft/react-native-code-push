"use strict";

import React from "react-native";
import { Platform, AppRegistry, Text, View } from "react-native";
import CodePush from "react-native-code-push";

const NativeCodePush = React.NativeModules.CodePush;
const RCTTestModule = React.NativeModules.TestModule;

let NotifyApplicationReadyTest = React.createClass({
  getInitialState() {
    return {};
  },
  async componentDidMount() {
    await CodePush.notifyApplicationReady();
    if (Platform.OS === "android") {
      await NativeCodePush.downloadAndReplaceCurrentBundle("http://10.0.3.2:8081/CodePushDemoAppTests/InstallUpdateTests/resources/PassNotifyApplicationReadyTest.includeRequire.runModule.bundle?platform=android&dev=true");
    } else if (Platform.OS === "ios") {
      await NativeCodePush.downloadAndReplaceCurrentBundle("http://localhost:8081/CodePushDemoAppTests/InstallUpdateTests/resources/PassNotifyApplicationReadyTest.includeRequire.runModule.bundle?platform=ios&dev=true");
    }
    
    CodePush.restartApp();
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

AppRegistry.registerComponent("NotifyApplicationReadyTest", () => NotifyApplicationReadyTest);