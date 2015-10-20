/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 */
'use strict';

var React = require('react-native');
var CodePush = require("react-native-code-push");
var Button = require("react-native-button");

var {
  AppRegistry,
  StyleSheet,
  Text,
  View,
} = React;

var CodePushDemoApp = React.createClass({
  componentDidMount: function() {
    this.checkUpdate();
  },
  checkUpdate: function() {
    CodePush.checkForUpdate().done((update) => {
      this.setState({ update: update });
    });
  },
  getInitialState: function() {
    return { update: false };
  },
  handlePress: function() {
    this.state.update.download().done((localPackage) => {
      localPackage.apply();
    });
  },
  render: function() {
    var updateView;
    if (this.state.update) {
      updateView = (
        <View>
          <Text>Update Available: {'\n'} {this.state.update.scriptVersion} - {this.state.update.description}</Text>
          <Button style={{color: 'green'}} onPress={this.handlePress}>
            Update
          </Button>
        </View>
      );
    };
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to React Native!
        </Text>
        <Text style={styles.instructions}>
          To get started, edit index.ios.js
        </Text>
        <Text style={styles.instructions}>
          Press Cmd+R to reload,{'\n'}
          Cmd+D or shake for dev menu
        </Text>
        {updateView}
      </View>
    );
  }
});
var styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});

AppRegistry.registerComponent('CodePushDemoApp', () => CodePushDemoApp);
