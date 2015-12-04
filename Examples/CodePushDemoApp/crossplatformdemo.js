'use strict';

var React = require('react-native');
var {
  AppRegistry,
  Dimensions,
  Image,
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
        updateDialog: true,
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
          case CodePush.SyncStatus.UP_TO_DATE:
            self.setState({
              syncMessage: "App up to date.",
              progress: false
            });
            break;
          case CodePush.SyncStatus.UPDATE_IGNORED:
            self.setState({
              syncMessage: "Update cancelled by user.",
              progress: false
            });
            break;
          case CodePush.SyncStatus.UPDATE_INSTALLED:
            self.setState({
              syncMessage: "Update installed and will be run when the app next resumes.",
              progress: false
            });
            break;
          case CodePush.SyncStatus.UNKNOWN_ERROR:
            self.setState({
              syncMessage: "An unknown error occurred.",
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
    ).catch(function(error) {
      CodePush.log(error);
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
        <Image style={styles.image} resizeMode={Image.resizeMode.contain} source={require('./images/laptop_phone_howitworks.png')}/>
      </View>
    );
  }
});

var styles = StyleSheet.create({
  image: {
    marginTop: 50,
    width: Dimensions.get('window').width - 100,
    height: 365 * (Dimensions.get('window').width - 100) / 651,
  },
  container: {
    flex: 1,
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
    paddingTop: 50
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10
  },
  messages: {
    textAlign: 'center',
  },
});

AppRegistry.registerComponent('CodePushDemoApp', () => CodePushDemoApp);
