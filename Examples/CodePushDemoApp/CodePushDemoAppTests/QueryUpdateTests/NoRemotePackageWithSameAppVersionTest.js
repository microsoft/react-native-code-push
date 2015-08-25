'use strict';

var React = require('react-native');
var RCTTestModule = React.NativeModules.TestModule;
var CodePushSdk = require('react-native-code-push');
var NativeBridge = require('react-native').NativeModules.CodePush;

var {
  Text,
  View,
} = React;

var NoRemotePackageWithSameAppVersionTest = React.createClass({
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
      this.setUp(this.runTest);
    }
  },
  
  setUp(callWhenDone) {
    var mockAcquisitionSdk = {
      latestPackage: {
        downloadUrl: "http://www.windowsazure.com/blobs/awperoiuqpweru",
        description: "Angry flappy birds",
        appVersion: "1.5.0",
        label: "2.4.0",
        isMandatory: false,
        isAvailable: true,
        updateAppVersion: false,
        packageHash: "hash240",
        packageSize: 1024
      },
      queryUpdateWithCurrentPackage: function(queryPackage, callback){
        if (!this.latestPackage || queryPackage.appVersion !== this.latestPackage.appVersion ||
          queryPackage.packageHash == this.latestPackage.packageHash) {
          callback(/*err:*/ null, false);
        } else {
          callback(/*err:*/ null, latestPackage);
        } 
      }
    };
    
    NativeBridge.setUsingTestFolder(true);
    var localPackage = JSON.stringify({
      downloadURL: "http://www.windowsazure.com/blobs/awperoiuqpweru",
      description: "Angry flappy birds",
      appVersion: "1.0.0",
      label: "2.4.0",
      isMandatory: false,
      isAvailable: true,
      updateAppVersion: false,
      packageHash: "hash123",
      packageSize: 1024
    });
    
    var mockConfiguration = { appVersion : "1.0.0" };
    CodePushSdk.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeBridge);
    NativeBridge.writeToLocalPackage(localPackage, function(err){
      if (err) {
        throw new Error('Setup: Error removing local package');
      } else {
        callWhenDone();
      }
    });
  },
  
  runTest() {
    CodePushSdk.queryUpdate((err, update) => {
      if (update) {
        throw new Error('SDK should not return a package if remote package is of a different version');
      } else if (err) {
        throw new Error(err.message);
      } else {
        this.setState({done: true}, RCTTestModule.markTestCompleted);
      }
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

NoRemotePackageWithSameAppVersionTest.displayName = 'NoRemotePackageWithSameAppVersionTest';

module.exports = NoRemotePackageWithSameAppVersionTest;