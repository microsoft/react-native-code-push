"use strict";

import React from "react-native";
import { DeviceEventEmitter, Text, View } from "react-native";
let NativeCodePush = React.NativeModules.CodePush;

// RCTTestModule is not implemented yet for RN Android.
let RCTTestModule = React.NativeModules.TestModule || {};

function createTestCaseComponent(displayName, description, setUp, runTest, passAfterRun = true) {     
  let TestCaseComponent = React.createClass({
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
            {this.state.done ? "Test Passed!" : "Testing..."}
          </Text>
        </View>
      );
    }
  });
  
  TestCaseComponent.displayName = displayName;
  TestCaseComponent.description = description;
  
  return TestCaseComponent;
}

export default createTestCaseComponent;