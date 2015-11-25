'use strict';

var React = require('react-native');
var CodePushSdk = require('react-native-code-push');
var NativeCodePush = require("react-native").NativeModules.CodePush;
var RCTTestModule = require('NativeModules').TestModule || {};

var {
  AppRegistry,
  Text,
  View,
} = React;

var DownloadAndInstallUpdateTest = React.createClass({
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
    NativeCodePush.setUsingTestFolder(true);
    CodePushSdk.setUpTestDependencies(null, mockConfiguration, NativeCodePush);
  },
  
  runTest() {
    var update = require("./TestPackage");
    NativeCodePush.downloadUpdate(update).done((downloadedPackage) => {
      NativeCodePush.installUpdate(downloadedPackage, /*rollbackTimeout*/ 1000, CodePushSdk.InstallMode.IMMEDIATE)
        .then(() => {
          CodePushSdk.getCurrentPackage().then((localPackage) => {
            if (localPackage.packageHash == update.packageHash) {
              this.setState({done: true}, RCTTestModule.markTestCompleted);
            } else {
              throw new Error("Update was not installed");
            }
          });
        });
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

DownloadAndInstallUpdateTest.displayName = 'DownloadAndInstallUpdateTest';
AppRegistry.registerComponent('CodePushDemoApp', () => DownloadAndInstallUpdateTest);

module.exports = DownloadAndInstallUpdateTest;