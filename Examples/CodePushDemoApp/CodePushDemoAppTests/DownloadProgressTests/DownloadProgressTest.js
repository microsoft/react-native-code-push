"use strict";

var RCTTestModule = require("NativeModules").TestModule;
var React = require("react-native");
var CodePushSdk = require("react-native-code-push");
var NativeBridge = require("react-native").NativeModules.CodePush;
var { NativeAppEventEmitter } = require("react-native");

var {
  Text,
  View,
} = React;

var DownloadProgressTest = React.createClass({
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
      this.runTest();
    }
  },
  
  checkReceivedAndExpectedBytesEqual() {
    if (this.state.progress.receivedBytes !== this.state.progress.totalBytes) {
      throw new Error("Bytes do not tally: Received bytes=" + this.state.progress.receivedBytes + " Total bytes=" + this.state.progress.totalBytes);
    }
  },
  
  runTest() {
    var downloadProgressSubscription = NativeAppEventEmitter.addListener(
      "CodePushDownloadProgress",
      (progress) => {
        this.setState({
          progress:progress,
          done: false,
        });
      }
    );
    
    var updates = require("./TestPackages");
    NativeBridge.downloadUpdate(updates.smallPackage)
      .then((smallPackage) => {
        if (smallPackage) {
          this.checkReceivedAndExpectedBytesEqual();
          return NativeBridge.downloadUpdate(updates.mediumPackage);
        } else {
          throw new Error("Small package download failed.");
        }
      })
      .then((mediumPackage) => {
        if (mediumPackage) {
          this.checkReceivedAndExpectedBytesEqual();
          return NativeBridge.downloadUpdate(updates.largePackage);
        } else {
          throw new Error("Medium package download failed.");
        }
      })
      .done((largePackage) => {
        if (largePackage) {
          this.checkReceivedAndExpectedBytesEqual();
          this.setState({done: true}, RCTTestModule.markTestCompleted);
        } else {
          throw new Error("Large package download failed.");
        }
      });
  },

  render() {
    var progressView;
    if (this.state.progress) {
      progressView = (
        <Text>{this.state.progress.receivedBytes} of {this.state.progress.totalBytes} bytes received</Text>
      );
    } 
    
    return (
      <View style={{backgroundColor: "white", padding: 40}}>
        <Text>
          {this.constructor.displayName + ": "}
          {this.state.done ? "Done" : "Testing..."}
        </Text>
        {progressView}
      </View>
    );
  }
});

DownloadProgressTest.displayName = "DownloadProgressTest";

module.exports = DownloadProgressTest;