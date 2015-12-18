"use strict";

var React = require("react-native");

var {
  AppRegistry,
  Text,
  View,
} = React;

var RollbackTest = React.createClass({
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