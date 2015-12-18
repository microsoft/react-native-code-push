"use strict";

var React = require("react-native");
var CodePush = require("react-native-code-push");

var {
  AppRegistry,
  Text,
  View,
} = React;

var IsFirstRunTest = React.createClass({
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
    var text = "Testing...";
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