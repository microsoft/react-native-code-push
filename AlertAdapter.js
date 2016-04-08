import React, { Platform } from "react-native";
let { Alert } = React;

if (Platform.OS === "android") {
  const { NativeModules: { CodePushDialog } } = React;
    
  Alert = {
    alert(title, message, buttons) {
      if (buttons.length > 2) {
        throw "Can only show 2 buttons for Android dialog.";
      }
      
      const button1Text = buttons[0] ? buttons[0].text : null,
            button2Text = buttons[1] ? buttons[1].text : null;
      
      CodePushDialog.showDialog(
        title, message, button1Text, button2Text,
        (buttonId) => { buttons[buttonId].onPress && buttons[buttonId].onPress(); }, 
        (error) => { throw error; });
    }
  };
}

module.exports = { Alert };