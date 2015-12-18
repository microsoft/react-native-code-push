"use strict";

var React = require("react-native");
var { DeviceEventEmitter } = require("react-native");
var NativeCodePush = React.NativeModules.CodePush;

// RCTTestModule is not implemented yet for RN Android.
var RCTTestModule = React.NativeModules.TestModule || {};

var {
  Text,
  View,
} = React;

function createTestCaseComponent(displayName, description, setUp, runTest, passAfterRun = true) {     
  var TestCaseComponent = React.createClass({
    propTypes: {
      shouldThrow: React.PropTypes.bool,
      waitOneFrame: React.PropTypes.bool,
    },
    getInitialState() {
      return {
        done: false,
      };
    },
    componentDidMount() {
      setUp()
        .then(runTest)
        .then(() => {
          if (passAfterRun) {
            this.setState({done: true}, RCTTestModule.markTestCompleted);
          }
        })
        .catch((err) => {
          console.error(err);
          throw err;
        });
    },
    render() {
      return (
        <View style={{backgroundColor: "white", padding: 40}}>
          <Text>
            {this.state.done ? "Done" : "Testing..."}
          </Text>
        </View>
      );
    }
  });
  
  TestCaseComponent.displayName = displayName;
  TestCaseComponent.description = description;
  
  return TestCaseComponent;
}

module.exports = createTestCaseComponent;