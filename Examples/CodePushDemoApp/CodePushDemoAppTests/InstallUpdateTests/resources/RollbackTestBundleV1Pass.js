"use strict";

import React from "react-native";

let {
  AppRegistry,
  Text,
  View,
} = React;

let RollbackTest = React.createClass({
  render() {
    return (
      <View style={{backgroundColor: "white", padding: 40}}>
        <Text>
          Test Passed!
        </Text>
      </View>
    );
  }
});

AppRegistry.registerComponent("RollbackTest", () => RollbackTest);