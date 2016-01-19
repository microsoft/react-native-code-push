"use strict";

import React, {
  AppRegistry,
  Platform,
  Text,
  View,
} from "react-native";
import CodePush from "react-native-code-push";

const RCTTestModule = React.NativeModules.TestModule;
const NativeCodePush = React.NativeModules.CodePush;
const PackageMixins = require("react-native-code-push/package-mixins.js")(NativeCodePush);

let RollbackTest = React.createClass({
  async componentDidMount() {
    let remotePackage = require("./remotePackage");
    remotePackage.packageHash = "hash241";

    if (Platform.OS === "android") {
      remotePackage.downloadUrl = "http://10.0.3.2:8081/CodePushDemoAppTests/InstallUpdateTests/resources/RollbackTestBundleV2.includeRequire.runModule.bundle?platform=android&dev=true"
      await CodePush.notifyApplicationReady();
      await NativeCodePush.downloadAndReplaceCurrentBundle("http://10.0.3.2:8081/CodePushDemoAppTests/InstallUpdateTests/resources/RollbackTestBundleV1Pass.includeRequire.runModule.bundle?platform=android&dev=true");
    } else if (Platform.OS === "ios") {
      remotePackage.downloadUrl = "http://localhost:8081/CodePushDemoAppTests/InstallUpdateTests/resources/RollbackTestBundleV2.includeRequire.runModule.bundle?platform=ios&dev=true"
      await CodePush.notifyApplicationReady()
      await NativeCodePush.downloadAndReplaceCurrentBundle("http://localhost:8081/CodePushDemoAppTests/InstallUpdateTests/resources/RollbackTestBundleV1Pass.includeRequire.runModule.bundle?platform=ios&dev=true");
    }
    
    remotePackage = Object.assign(remotePackage, PackageMixins.remote());

    let localPackage = await remotePackage.download();
    return await localPackage.install(NativeCodePush.codePushInstallModeImmediate);
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