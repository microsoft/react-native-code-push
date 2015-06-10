#!/bin/bash

set -e

SCRIPTS_PATH=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

ROOT=$1
XCODEPROJ=$2
XCODESCHEME=$3

export REACT_PACKAGER_LOG="$ROOT/server.log"

cd $ROOT

function cleanup {
  EXIT_CODE=$?
  set +e

  sleep 3
  $SCRIPTS_PATH/stop-packager.sh

  if [ $EXIT_CODE -ne 0 ];
  then
    WATCHMAN_LOGS=/usr/local/Cellar/watchman/3.1/var/run/watchman/$USER.log
    #[ -f $WATCHMAN_LOGS ] && cat $WATCHMAN_LOGS

    #[ -f $REACT_PACKAGER_LOG ] && cat $REACT_PACKAGER_LOG
  fi
}
trap cleanup EXIT

#$SCRIPTS_PATH/stop-packager.sh
$SCRIPTS_PATH/start-packager.sh $ROOT

xctool \
  -project $XCODEPROJ \
  -scheme $XCODESCHEME -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 5,OS=8.3' \
  build 

xctool \
  -project $XCODEPROJ \
  -scheme $XCODESCHEME -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 5,OS=8.3' \
  test


