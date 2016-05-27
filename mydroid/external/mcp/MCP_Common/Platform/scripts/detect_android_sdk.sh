#!/bin/sh

if [ -z "$ANDROID_HOME" ]
then
	echo ""
	exit 1
fi
 
cd "$ANDROID_HOME/mydroid"
export PLATFORM_VERSION=`grep "PLATFORM_VERSION := " build/core/version_defaults.mk | awk 'BEGIN{FS=" "}{print $3}'`
echo $PLATFORM_VERSION

