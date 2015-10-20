/**
 * @providesModule CodePush
 * @flow
 */

var Platform = require("Platform");

if (Platform.OS === "android") {
    module.exports = require("./CodePush.android.js"); 
} else if (Platform.OS === "ios") {   
    module.exports = require("./CodePush.ios.js");
}