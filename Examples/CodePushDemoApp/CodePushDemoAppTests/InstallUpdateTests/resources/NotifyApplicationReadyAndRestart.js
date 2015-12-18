"use strict";

import React from "react-native";
let { Platform } = require("react-native");
import CodePush from "react-native-code-push";
let NativeCodePush = React.NativeModules.CodePush;
let RCTTestModule = React.NativeModules.TestModule;

let {
  AppRegistry,
  Text,
  View,
} = React;

let NotifyApplicationReadyTest = React.createClass({
  getInitialState() {
    return {};
  },
  componentDidMount() {
    CodePush.notifyApplicationReady()
      .then(() => { 
        if (Platform.OS === "android") {
          return NativeCodePush.downloadAndReplaceCurrentBundle("http://10.0.3.2:8081/CodePushDemoAppTests/InstallUpdateTests/resources/PassNotifyApplicationReadyTest.includeRequire.runModule.bundle?platform=android&dev=true");
        } else if (Platform.OS === "ios") {
          return NativeCodePush.downloadAndReplaceCurrentBundle("http://localhost:8081/CodePushDemoAppTests/InstallUpdateTests/resources/PassNotifyApplicationReadyTest.includeRequire.runModule.bundle?platform=ios&dev=true");
        }
      })
      .then(() => {
        CodePush.restartApp();
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

AppRegistry.registerComponent("NotifyApplicationReadyTest", () => NotifyApplicationReadyTest);