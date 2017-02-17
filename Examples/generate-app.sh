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

####################  React Native Config  #################################################

echo "list all react native versions: npm show react-native versions --json" 
read -p "Enter react native version, [default] is the latest version:" react_native_version

if [ ! $react_native_version]; then
	react_native_version=`npm view react-native version`
fi
echo 'React Native version: ' + $react_native_version

npm list -g rninit

####################  React Native Code Push Config  ######################################

echo "list all react native versions: npm show react-native-code-push versions --json"
read -p "Enter react native code push version, [default] is the latest version: " react_native_code_push_version

if [ ! $react_native_code_push_version ]; then
	react_native_code_push_version=`npm view react-native-code-push version`
fi
echo 'React Native Code Push version: ' + $react_native_code_push_version
echo

####################  Start Testing  #########################################################

echo '********************* Yo, start to test ***************************************';

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

sed -i.bak '162s/AppRegistry.registerComponent("CodePushDemoApp", () => CodePushDemoApp);/AppRegistry.registerComponent("testapp_rn", () => CodePushDemoApp);/' demo.js
rm -f demo.js.bak

perl -i -p0e 's/#ifdef DEBUG.*?#endif/jsCodeLocation = [CodePush bundleURL];/s' ios/testapp_rn/AppDelegate.m

sed -i.bak '17,20d' node_modules/react-native/packager/react-native-xcode.sh
rm -f node_modules/react-native/packager/react-native-xcode.sh.bak

sed -i.bak '90s/targetName.toLowerCase().contains("release")/true/' node_modules/react-native/react.gradle
rm -f node_modules/react-native/react.gradle.bak
