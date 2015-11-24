'use strict';

var NativeCodePush = require("react-native").NativeModules.CodePush;

var Platform = require("Platform");
var Alert;

if (Platform.OS === "android") {
  /* 
   * Promisify native methods. Assumes that every native method takes
   * two callback functions, resolve and reject.
   */
  var methodsToPromisify = [
    "installUpdate",
    "downloadUpdate",
    "getConfiguration",
    "getCurrentPackage",
    "isFailedUpdate",
    "isFirstRun",
    "notifyApplicationReady",
    "setDeploymentKey"
  ];
  
  methodsToPromisify.forEach((methodName) => {
    var aMethod = NativeCodePush[methodName];
    NativeCodePush[methodName] = function() {
      var args = [].slice.apply(arguments);
      return new Promise((resolve, reject) => {
        args.push(resolve);
        args.push(reject);
        aMethod.apply(this, args);
      });
    }
  });
  
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

var PackageMixins = require("./package-mixins")(NativeCodePush);

module.exports = {
  NativeCodePush: NativeCodePush,
  PackageMixins: PackageMixins,
  Alert: Alert
}