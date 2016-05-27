#!/bin/bash
set -e
SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P )
adb remount
adb push $SCRIPTPATH/senstest /system/bin/
adb -d shell chmod 766 /system/bin/senstest
