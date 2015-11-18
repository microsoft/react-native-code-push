/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 */
'use strict';

var React = require('react-native');
var {
  AppRegistry,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} = React;

var Button = require("react-native-button");

var CodePush = require('react-native-code-push');

var CodePushDemoApp = React.createClass({

  componentDidMount: function() {
  },
  sync: function() {
    var self = this;
    CodePush.sync(
      { 
        updateNotification: true,
        installMode: CodePush.InstallMode.ON_NEXT_RESUME
      }, 
      function(syncStatus) {
        switch(syncStatus) {
          case CodePush.SyncStatus.CHECKING_FOR_UPDATE: 
            self.setState({
              syncMessage: "Checking for update."
            });
            break;
          case CodePush.SyncStatus.DOWNLOADING_PACKAGE:
            self.setState({
              syncMessage: "Downloading package."
            });
            break;
          case CodePush.SyncStatus.AWAITING_USER_ACTION:
            self.setState({
              syncMessage: "Awaiting user action."
            });
            break;
          case CodePush.SyncStatus.INSTALLING_UPDATE:
            self.setState({
              syncMessage: "Installing update."
            });
            break;
          case CodePush.SyncStatus.IDLE:
            self.setState({
              syncMessage: "Update installed and will be run when the app next resumes.",
              progress: false
            });
            break;
        }
      },
      function(progress) {
        self.setState({
          progress: progress
        });
      }
    ).then(function(syncResult) {
      switch(syncResult) {
        case CodePush.SyncResult.UP_TO_DATE: 
          self.setState({
            syncMessage: "App up to date."
          });
          break;
        case CodePush.SyncResult.UPDATE_IGNORED: 
          self.setState({
            syncMessage: "Update cancelled by user."
          });
          break;
      }
    });
  },
  getInitialState: function() {
    return { };
  },
  render: function() {
    var syncView;
    var syncButton;
    var progressView;
    
    if (this.state.syncMessage) {
      syncView = (
        <Text style={styles.messages}>{this.state.syncMessage}</Text>
      );
    } else {
      syncButton = ( 
        <Button style={{color: 'green'}} onPress={this.sync}>
          Start Sync!
        </Button>
      );
    }
    
    if (this.state.progress) {
      progressView = (
        <Text style={styles.messages}>{this.state.progress.receivedBytes} of {this.state.progress.totalBytes} bytes received</Text>
      );
    }
    
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to CodePush!
        </Text>
        {syncButton}
        {syncView}
        {progressView}
      </View>
    );
  }
});

var styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
    marginTop: 50
  },
  messages: {
    textAlign: 'center',
  },
});

AppRegistry.registerComponent('CodePushDemoApp', () => CodePushDemoApp);
