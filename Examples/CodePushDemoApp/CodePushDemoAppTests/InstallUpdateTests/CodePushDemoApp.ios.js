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

var RCTTestModule = require('NativeModules').TestModule;
var NativeCodePush = require('react-native').NativeModules.CodePush;

var CodePushDemoApp = React.createClass({
  componentDidMount: function() {
    NativeCodePush.setUsingTestFolder(true);
    NativeCodePush.getCurrentPackage().then(
      (savedPackage) => {
        if (savedPackage) {
          var testPackage = require("./TestPackage");
          for (var key in testPackage) {
            if (savedPackage[key] !== testPackage[key]) {
              throw new Error("The local package is still different from the updated package after installation");
            }
          }
        } else {
          throw new Error("The updated package was not saved");
        }
      }, 
      (err) => {
        throw new Error("The updated package was not saved");
      }
    );
  },
  render: function() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          If you see this, you have successfully installed an update!
        </Text>
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
  }
});

AppRegistry.registerComponent('CodePushDemoApp', () => CodePushDemoApp);
