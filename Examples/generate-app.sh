#!/bin/bash

# Copyright (c) 2015-present, Microsoft Inc.
# All rights reserved.
#
# This source code is licensed under the BSD-style license found in the
# LICENSE file in the root directory of this source tree. An additional grant
# of patent rights can be found in the PATENTS file in the same directory.

echo 'CodePush + RN sample app generation script';
echo

rm -rf testapp_rn
 
echo '************************ Configuration ***********************************';

####################  Configure versions  #################################################

read -p "Enter React Native version (default: latest):" react_native_version
read -p "Enter CodePush version (default: latest): " react_native_code_push_version

echo

if [ ! $react_native_version]; then
	react_native_version=`npm view react-native version`
fi
echo 'React Native version: ' + $react_native_version

if [ ! $react_native_code_push_version ]; then
	react_native_code_push_version=`npm view react-native-code-push version`
fi
echo 'React Native Code Push version: ' + $react_native_code_push_version
echo

####################  Create app  #########################################################

echo '********************* Creating app ***************************************';

current_dir=`pwd`;
echo 'Current directory: ' + $current_dir;

echo 'Create testapp_rn app';
rninit init testapp_rn --source react-native@$react_native_version

cd testapp_rn

echo 'Install React Native Code Push Version $react_native_code_push_version' 
npm install --save react-native-code-push@$react_native_code_push_version

echo 'react native link to react native code push'
react-native link react-native-code-push

rm index.android.js
rm index.ios.js
cp ../CodePushDemoApp/*js .
mkdir images
cp ../CodePushDemoApp/images/* images

# Make changes required to test CodePush in debug mode (see OneNote)
sed -ie '162s/AppRegistry.registerComponent("CodePushDemoApp", () => CodePushDemoApp);/AppRegistry.registerComponent("testapp_rn", () => CodePushDemoApp);/' demo.js
perl -i -p0e 's/#ifdef DEBUG.*?#endif/jsCodeLocation = [CodePush bundleURL];/s' ios/testapp_rn/AppDelegate.m
sed -ie '17,20d' node_modules/react-native/packager/react-native-xcode.sh
sed -ie '90s/targetName.toLowerCase().contains("release")/true/' node_modules/react-native/react.gradle
