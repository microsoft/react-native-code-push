'use strict';

var React = require('react-native');
var CodePushSdk = require('react-native-code-push');
var NativeCodePush = require("react-native").NativeModules.CodePush;
var RCTTestModule = require('NativeModules').TestModule || {};

var {
  Text,
  View,
} = React;

var NoRemotePackageTest = React.createClass({
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
  
  setUp() {
    var mockAcquisitionSdk = {
      latestPackage: null,
      queryUpdateWithCurrentPackage: function(queryPackage, callback){
        if (!this.latestPackage || queryPackage.appVersion !== this.latestPackage.appVersion ||
          queryPackage.packageHash == this.latestPackage.packageHash) {
          callback(/*err:*/ null, false);
        } else {
          callback(/*err:*/ null, latestPackage);
        } 
      }
    };
    
    var mockConfiguration = { appVersion : "1.5.0" };
    NativeCodePush.setUsingTestFolder(true);
    CodePushSdk.setUpTestDependencies(mockAcquisitionSdk, mockConfiguration, NativeCodePush);
  },
  
  runTest() {
    CodePushSdk.checkForUpdate().then(
      (update) => {
        if (update) {
          throw new Error('SDK should not return a package if remote does not contain a package');
        } else {
          this.setState({done: true}, RCTTestModule.markTestCompleted);
        }
      },
      (err) => {
        throw new Error(err.message);
      },
    );
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

NoRemotePackageTest.displayName = 'NoRemotePackageTest';

module.exports = NoRemotePackageTest;