/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
'use strict';

var RCTTestModule = require('NativeModules').TestModule;
var React = require('react-native');
var CodePushSdk = require('react-native-code-push');
var NativeBridge = require('react-native').NativeModules.CodePush;

var {
  Text,
  View,
} = React;

var DownloadAndApplyUpdateTest = React.createClass({
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
    if (this.props.waitOneFrame) {
      requestAnimationFrame(this.runTest);
    } else {
      this.setUp();
      this.runTest();
    }
  },
  
  setUp(callWhenDone) {
    var mockConfiguration = { appVersion : "1.5.0" };
    NativeBridge.setUsingTestFolder(true);
    CodePushSdk.setUpTestDependencies(null, mockConfiguration, NativeBridge);
  },
  
  runTest() {
    var update = require("./TestPackage");
    CodePushSdk.installUpdate(update);
  },

  render() {
    return (
      <View style={{backgroundColor: 'white', padding: 40}}>
        <Text>
          {this.constructor.displayName + ': '}
          {this.state.done ? 'Done' : 'Testing...'}
        </Text>
      </View>
    );
  }
});

DownloadAndApplyUpdateTest.displayName = 'DownloadAndApplyUpdateTest';

module.exports = DownloadAndApplyUpdateTest;