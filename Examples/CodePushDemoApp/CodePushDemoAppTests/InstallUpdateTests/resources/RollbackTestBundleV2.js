"use strict";

import React, {
  AppRegistry,
  Text,
  View,
} from "react-native";
import CodePush from "react-native-code-push";

let RollbackTest = React.createClass({
  componentDidMount() {
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

AppRegistry.registerComponent("RollbackTest", () => RollbackTest);