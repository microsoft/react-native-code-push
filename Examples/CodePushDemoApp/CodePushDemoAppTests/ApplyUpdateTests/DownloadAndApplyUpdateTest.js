'use strict';

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
    NativeBridge.downloadUpdate(update).done((downloadedPackage) => {
      NativeBridge.applyUpdate(downloadedPackage, 1000);
    });
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