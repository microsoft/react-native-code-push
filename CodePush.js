/**
 * @providesModule CodePush
 * @flow
 */

var CodePush = function(platform){
  if (platform == 'ios') return require('./CodePush.ios.js');
  else if (platform == 'android') return require('./CodePush.android.js');
  else throw "Platform not supported: " + platform;
};

module.exports = CodePush;