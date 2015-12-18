"use strict";

var React = require("react-native");
var CodePush = require("react-native-code-push");

var {
  AppRegistry,
  Text,
  View,
} = React;

var RollbackTest = React.createClass({
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