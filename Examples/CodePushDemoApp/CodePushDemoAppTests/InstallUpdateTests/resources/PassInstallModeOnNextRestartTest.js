"use strict";

var React = require("react-native");

var {
  AppRegistry,
  Text,
  View,
} = React;

var InstallModeOnNextRestartTest = React.createClass({
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

AppRegistry.registerComponent("InstallModeOnNextRestartTest", () => InstallModeOnNextRestartTest);