#!/bin/sh

if [ -z "$ANDROID_HOME" ]
then
	echo "Please set ANDROID_HOME "
	exit 1
fi
 
cd "$ANDROID_HOME/mydroid"
export PLATFORM_VERSION=`grep "PLATFORM_VERSION := " build/core/version_defaults.mk | awk 'BEGIN{FS=" "}{print $3}'`
export TARGET_PRODUCT=`grep "TARGET_PRODUCT:=" buildspec.mk | awk 'BEGIN{FS="="}{print $2}'`
if [ -z "$TARGET_PRODUCT" ]
then
        echo "Cannot find TARGET_PRODUCT,Please copy the correct buildspec.mk from device/ti/* "
	exit 1
fi

if [ "$PLATFORM_VERSION" = "1.6" ]; then
		echo "zoom2"
elif [ "$PLATFORM_VERSION" = "Eclair" ]; then
		echo "zoom2"
elif [ "$PLATFORM_VERSION" = "2.1" ]; then
		echo "zoom2"
elif [ "$PLATFORM_VERSION" = "2.2" ]; then
		echo "zoom3"
elif [ "$PLATFORM_VERSION" = "2.2.1" ]; then

		if [ "$TARGET_PRODUCT" = "zoom2" ]; then
			echo "zoom3"
		elif [ "$TARGET_PRODUCT" = "blaze" ]; then
			echo "blaze"
		fi
elif [ "$PLATFORM_VERSION" = "2.3.3" ]; then
		echo "blaze"
else
		echo "ubuntu_x86"
fi

