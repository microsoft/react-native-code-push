#!/bin/bash

# Copyright (c) 2015-present, Microsoft Inc.
# All rights reserved.
#
# This source code is licensed under the BSD-style license found in the
# LICENSE file in the root directory of this source tree. An additional grant
# of patent rights can be found in the PATENTS file in the same directory.

echo 'Automate synchronization testing between React Native and React Native Code Push';
echo
 
# Please make sure you installed react native in your mac machine

echo '************************ Configuration ***********************************';

####################  Code Push Config  #########################W#########################

code_push_version=`code-push --version`;
echo 'Code Push version: ' + ${code_push_version};

####################  React Native Code Push Config  ######################################

react_native_code_push_version='';  # manual label yo, yo, yo.
# command to see all react native versions: npm show react-native-code-push versions --json
if [ ! $react_native_code_push_version ]; then
	react_native_code_push_version=`npm view react-native-code-push version`
fi
echo 'React Native Code Push version: ' + $react_native_code_push_version

####################  React Native Config  #################################################

react_native_version='0.40.0'; # manual label, such as 0.39.0. yo, yo, yo.
# command to see all react native versions: npm show react-native versions --json
if [ ! $react_native_version]; then
	react_native_version=`npm view react-native version`
fi
echo 'React Native version: ' + $react_native_version

npm list -g rninit

####################  Testing Environment Config  ###########################################

mobile_env='ios'; # or android
echo 'Testing app environment' + $mobile_env;

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
cp ../skeleton/*js .
mkdir images
cp ../skeleton/images/* images

if [ $mobile_env == 'ios' ]; then
	react-native run-ios
else
	react-native run-android
fi
# cd ..
# rm -rf testapp
# exit;