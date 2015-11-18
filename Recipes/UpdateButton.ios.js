'use strict';

var pkg = require('./package');
var React = require('react-native');
var {
  AppRegistry,
  StyleSheet,
  Text,
  View,
} = React;
var Button = require('react-native-button');

var CodePush = require('react-native-code-push');

var UpdateButton = React.createClass({
  getInitialState: function() {
    return {};
  },
  componentDidMount: function() {
    CodePush.checkForUpdate().done((update) => {
      if (update && !update.downloadURL) {
        this.setState({
          update: update 
        });
      }
    });
  },
  update: function() {
    this.state.update.download().done((newPackage) => {
      newPackage.install();
    });
  },
  render: function() {
    var updateButton = null;
    if (this.state.update) {
      updateButton = <Button onPress={this.update}>Update</Button>;
    }

    return (
      <View style={styles.container}>
        <Text>
          Welcome to {pkg.name} {pkg.version}!
        </Text>
        {updateButton}
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
  }
});

AppRegistry.registerComponent('UpdateButton', () => UpdateButton);
