"use strict";

import React from "react-native";
import CodePush from "react-native-code-push";

let {
  AppRegistry,
  Text,
  View,
} = React;

let IsFirstRunTest = React.createClass({
  getInitialState() {
    return {};
  },
  componentDidMount() {
    CodePush.getCurrentPackage()
      .then((localPackage) => {
        if (localPackage.isFirstRun) {
          this.setState({ passed: true });
        } else {
          this.setState({ passed: false });
        }
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

AppRegistry.registerComponent("IsFirstRunTest", () => IsFirstRunTest);