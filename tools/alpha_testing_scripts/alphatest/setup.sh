#!/bin/sh
# 
# This sets up files on the device which are required / useful for testing:
# 1) busybox
# 2) AlphaTester.apk
# 3) logcurrent.sh

# install current logging script and start it
SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P ) 
adb remount
adb install $SCRIPTPATH/AlphaTester.apk
adb install $SCRIPTPATH/VoltageLogger.apk
