#!/bin/bash

# Copyright (c) 2015-present, Microsoft Inc.
# All rights reserved.
#
# This source code is licensed under the BSD-style license found in the
# LICENSE file in the root directory of this source tree. An additional grant
# of patent rights can be found in the PATENTS file in the same directory.

# The goal of this script is to automate the testing of react-native, react-native-code-push and 
# their internal compatibility. Run **./run.sh** to create an app and test it. 

# You can configure below variables in the bash. 

# 1. `code_push_version`
# 2. `react_native_code_push_version`
# 3. `react_native_version`
# 4. `mobile_env`

# If you use other libs or packages, and fail to run this test, welcome to modify this script 
# so that we can reproduce any issues that you have.


echo 'Automate synchronization testing between React Native and React Native Code Push';
echo

rm -rf testapp_rn
 
# Please make sure you installed react native in your mac machine

echo '************************ Configuration ***********************************';

####################  Code Push Config  #########################W#########################

code_push_cli_version=`code-push --version`;
echo 'Code Push CLI version: ' + ${code_push_cli_version};

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

echo '********************* Running IOS ***************************************';

react-native run-ios

echo '********************* Running Android ***************************************';

read -p "you need to open the android simulator manually before continue running this script. Enter [Enter] to continue" 

react-native run-android

# cd ..
# rm -rf testapp
# exit;