'use strict';

var { Platform } = require("react-native");
var Alert;

if (Platform.OS === "android") {
  var CodePushDialog = require("react-native").NativeModules.CodePushDialog;
  Alert = {
    alert: function(title, message, buttons) {
      if (buttons.length > 2) {
        throw "Can only show 2 buttons for Android dialog.";
      }
      
      var button1Text = buttons[0] ? buttons[0].text : null;
      var button2Text = buttons[1] ? buttons[1].text : null;
      
      CodePushDialog.showDialog(
        title, message, button1Text, button2Text,
        (buttonPressedId) => {
          buttons[buttonPressedId].onPress && buttons[buttonPressedId].onPress();
        }, 
        (error) => {
          throw error;
        });
    }
  };
} else if (Platform.OS === "ios") {   
  var { AlertIOS } = require("react-native");
  Alert = AlertIOS;
}

module.exports = {
  Alert: Alert
}