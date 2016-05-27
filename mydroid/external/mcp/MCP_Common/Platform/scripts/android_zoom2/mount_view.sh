#!/bin/sh

if [ -z "$ANDROID_HOME" ]
then
	echo "ANDROID_HOME is not set. Aborting."
	exit 1
fi

ANDROID_BT_PATH="$ANDROID_HOME/mydroid/external/btips"
if [ -z $MCP_SNAPVIEW_ROOT ]
then
	echo "Views folder undefined, defaulting to /data/btips_wcs_android/views"	
	MCP_SNAPVIEW_ROOT="/data/btips_wcs_android/views"
fi
VIEW=`pwd | cut -f5 -d'/'`


check_and_mount()
{
	SOURCE=$1
	SOURCE_DIR=`dirname $1`
	SOURCE_FNAME=`basename $1`
	DEST="$ANDROID_HOME/mydroid/$1"
	SOURCE="$MCP_VIEW_PATH/EBTIPS/adaptors/Android/$PLATFORM_NAME/$1"
	DEST_DIR="$ANDROID_HOME/mydroid/$SOURCE_DIR"

	#echo checking if "$DEST" exists...
	if [ -L "$DEST" ]
	then
		#echo unlinking "$DEST" 
		unlink "$DEST"
	elif [ -e "$DEST" ]
	then
		#echo "$DEST" is a real file, deleting...
		#mv "$DEST" "${DEST}.previous"
		rm "$DEST"
	fi
	echo "$SOURCE_FNAME --> $DEST_DIR" 
	ln -s -t "$DEST_DIR" "$SOURCE"
	
}

# check if $1/$3 exits and is a link. removes if necessary. Than mount $2/$3 within $1
check_and_mount_params()
{
	DESTINATION_DIR=$1
	SOURCE_DIR=$2
	FILE_OR_FOLDER_TO_LINK=$3

    backup_destination $1 $3
    
	echo trying to link $3 in $2 into $1
	if [ -L "$DESTINATION_DIR/$FILE_OR_FOLDER_TO_LINK" ]
	then
		echo "$DESTINATION_DIR/$FILE_OR_FOLDER_TO_LINK" s already a link, unlinking...
		unlink "$DESTINATION_DIR/$FILE_OR_FOLDER_TO_LINK"
	elif [ -e "$DESTINATION_DIR/$FILE_OR_FOLDER_TO_LINK" ]
	then
		echo "$DESTINATION_DIR/$FILE_OR_FOLDER_TO_LINK" is a real file, deleting...
		rm -rf "$DESTINATION_DIR/$FILE_OR_FOLDER_TO_LINK"
	fi
	echo linking "$SOURCE_DIR/$FILE_OR_FOLDER_TO_LINK" to "$DESTINATION_DIR/$FILE_OR_FOLDER_TO_LINK"
	ln -s -t "$DESTINATION_DIR" "$SOURCE_DIR/$FILE_OR_FOLDER_TO_LINK"
}

if [ -z $MCP_VIEW_PATH ]
then
MCP_VIEW_PATH="$MCP_SNAPVIEW_ROOT/$VIEW"
fi

# first link the view to external/mcp
if [ -L "$ANDROID_HOME/mydroid/external/mcp/B_TIPS" ]
then
	unlink "$ANDROID_HOME/mydroid/external/mcp/B_TIPS"
fi

# delete the folder if it exists
if [ -e "$ANDROID_HOME/mydroid/external/mcp/B_TIPS" ]
then
	rm -rf "$ANDROID_HOME/mydroid/external/mcp/B_TIPS"
fi

if [ -L "$ANDROID_HOME/mydroid/external/mcp/EBTIPS" ]
then
        unlink "$ANDROID_HOME/mydroid/external/mcp/EBTIPS"
fi

# delete the folder if it exists
if [ -e "$ANDROID_HOME/mydroid/external/mcp/EBTIPS" ]
then
	rm -rf "$ANDROID_HOME/mydroid/external/mcp/EBTIPS"
fi


if [ -L "$ANDROID_HOME/mydroid/external/mcp/HSW_FMStack" ]
then
        unlink "$ANDROID_HOME/mydroid/external/mcp/HSW_FMStack"
fi

# delete the folder if it exists
if [ -e "$ANDROID_HOME/mydroid/external/mcp/HSW_FMStack" ]
then
	rm -rf "$ANDROID_HOME/mydroid/external/mcp/HSW_FMStack"
fi

if [ -L "$ANDROID_HOME/mydroid/external/mcp/MCP_Common" ]
then
        unlink "$ANDROID_HOME/mydroid/external/mcp/MCP_Common"
fi

# delete the folder if it exists
if [ -e "$ANDROID_HOME/mydroid/external/mcp/MCP_Common" ]
then
	rm -rf "$ANDROID_HOME/mydroid/external/mcp/MCP_Common"
fi

if [ -L "$ANDROID_HOME/mydroid/external/mcp/NaviLink" ]
then
        unlink "$ANDROID_HOME/mydroid/external/mcp/NaviLink"
fi

# delete the folder if it exists
if [ -e "$ANDROID_HOME/mydroid/external/mcp/NaviLink" ]
then
	rm -rf "$ANDROID_HOME/mydroid/external/mcp/NaviLink"
fi

# copy Android.mk to view and UCM components folders
cp "$MCP_VIEW_PATH/MCP_Common/Platform/scripts/Android_root.mk" "$MCP_VIEW_PATH/Android.mk"
cp "$MCP_VIEW_PATH/EBTIPS/apps/Android.mk" "$MCP_VIEW_PATH/EBTIPS"
cp "$MCP_VIEW_PATH/EBTIPS/apps/Android.mk" "$MCP_VIEW_PATH/B_TIPS"

PWD=`pwd`
cd "$ANDROID_HOME/mydroid/external"
mkdir "mcp"
cd "mcp"


ln -s -T "$MCP_VIEW_PATH/B_TIPS" B_TIPS 
ln -s -T "$MCP_VIEW_PATH/EBTIPS" EBTIPS 
ln -s -T "$MCP_VIEW_PATH/HSW_FMStack" HSW_FMStack 
ln -s -T "$MCP_VIEW_PATH/MCP_Common" MCP_Common 
ln -s -T "$MCP_VIEW_PATH/NaviLink" NaviLink 
ln -s -f "$MCP_VIEW_PATH/Android.mk" "Android.mk"
cd "$PWD" 

cd "$ANDROID_HOME/mydroid"
mkdir out
mkdir out/target
mkdir out/target/product/
mkdir out/target/product/zoom2
mkdir out/target/product/zoom2/obj
mkdir out/target/product/zoom2/obj/lib
mkdir out/target/product/zoom2/system
mkdir out/target/product/zoom2/system/lib
mkdir frameworks/base/SUPLLib
ln -s -t frameworks/base/SUPLLib $MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/SUPLLib/*
cd "$PWD" 

# copy SUPL Libs to out folders
cp "$MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/general/libsuplhelperservicejni.so" "$ANDROID_HOME/mydroid/out/target/product/zoom2/system/lib"
cp "$MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/general/libsuplhelperservicejni.so" "$ANDROID_HOME/mydroid/out/target/product/zoom2/obj/lib"
cp "$MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/general/libsupllocationprovider.so" "$ANDROID_HOME/mydroid/out/target/product/zoom2/system/lib"
cp "$MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/general/libsupllocationprovider.so" "$ANDROID_HOME/mydroid/out/target/product/zoom2/obj/lib"
	
# add execution permission to RBTL automation scripts
chmod a+x "$MCP_VIEW_PATH/EBTIPS/apps/LINUX/btscripts/generate_client.sh"
chmod a+x "$MCP_VIEW_PATH/EBTIPS/apps/LINUX/btscripts/generate_server.sh"
chmod a+x "$MCP_VIEW_PATH/MCP_Common/Platform/scripts/detect_android_sdk.sh"
chmod a+x "$MCP_VIEW_PATH/MCP_Common/Platform/scripts/detect_android_platform.sh"

#check the version
cd "$ANDROID_HOME/mydroid"
PLATFORM_VERSION=`grep "PLATFORM_VERSION := " build/core/version_defaults.mk | awk 'BEGIN{FS=" "}{print $3}'`
PLATFORM_NAME=""
if [ "$PLATFORM_VERSION" = "1.6" ]; then
		PLATFORM_NAME="1.6"
elif [ "$PLATFORM_VERSION" = "Eclair" ]; then
      echo "Eclair 2.0 is not supported anymore (if you want to use it at own risk - change this logic...)!!!"
      exit
elif [ "$PLATFORM_VERSION" = "2.1" ]; then
		PLATFORM_NAME="2.1"
else
        echo "UNKNOWN - Aborting (currently supporting Donut or Eclair 2.1 only)"
		exit
fi

cd - >/dev/null

# Create the new paths to hold the files
cd "$MCP_VIEW_PATH/EBTIPS/adaptors/Android/$PLATFORM_NAME"
for PATHNAME in $(find . -type d -name "*" ! -name "_*" -print | sed 's/^\.\///')
do
	echo Creating $ANDROID_HOME/mydroid/$PATHNAME
	mkdir -p $ANDROID_HOME/mydroid/$PATHNAME
done

#link the files (ignore files starting with _ (underscore) and files in folder starting with _ (underscore).
for FILENAME in $(find . -type f -name "*" ! -name "_*.*" ! -name "*.contrib*" ! -name "*.keep*" -print | grep -v "/_" | sed 's/^\.\///')
do
	check_and_mount $FILENAME
done

# link uim files
cd "$MCP_VIEW_PATH/MCP_Common/STK/uim"
for UIM_FILENAME in $(find . -type f -name "*" -print | sed 's/^\.\///')
do
	check_and_mount_params $ANDROID_HOME/mydroid/hardware/ti/omap3/ti_st/uim $MCP_VIEW_PATH/MCP_Common/STK/uim $UIM_FILENAME
done

# link stk files
cd "$MCP_VIEW_PATH/MCP_Common/STK/ti-st"
for STK_FILENAME in $(find . -type f -name "*" -print | sed 's/^\.\///')
do
	check_and_mount_params $ANDROID_HOME/mydroid/kernel/android-2.6.29/drivers/misc/ti-st $MCP_VIEW_PATH/MCP_Common/STK/ti-st $STK_FILENAME
done

cd - >/dev/null

# link necessary files and folders in hardware
check_and_mount_params $ANDROID_HOME/mydroid/hardware/libhardware_legacy/gps $MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/libhardware_legacy gps.cpp
check_and_mount_params $ANDROID_HOME/mydroid/hardware/libhardware_legacy/gps $MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/libhardware_legacy Android.mk
check_and_mount_params $ANDROID_HOME/mydroid/hardware/libhardware_legacy/include/hardware_legacy $MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/libhardware_legacy gps.h

# link necessary files and folders in frameworks/base
check_and_mount_params $ANDROID_HOME/mydroid/frameworks/base/location/java/com/android/internal/location $MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/services GpsLocationProvider.java
#check_and_mount_params $ANDROID_HOME/mydroid/frameworks/base $MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android SUPLLib
#check_and_mount_params $ANDROID_HOME/mydroid/frameworks/base/services/java/com/android/server $MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/services SystemServer.java
check_and_mount_params $ANDROID_HOME/mydroid/frameworks/base/services/java/com/android/server $MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/services SUPLServer.java
check_and_mount_params $ANDROID_HOME/mydroid/frameworks/base/services/java/com/android/server $MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/services SUPLService.java
check_and_mount_params $ANDROID_HOME/mydroid/frameworks/base/services/java $MCP_VIEW_PATH/NaviLink/platforms/os/LINUX/Android/services Android.mk

# For Eclair Migration - begin
cd "$ANDROID_HOME/mydroid/external"
ln -s -T "$MCP_VIEW_PATH/Navilink/SUPLC/SUPL_HelperService_JNI" SUPL_HelperService_JNI 

# For Eclair Migration -end

echo "Done !!!"

