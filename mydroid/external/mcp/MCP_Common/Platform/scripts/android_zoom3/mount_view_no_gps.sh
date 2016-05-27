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

if [ -z $SNAPSHOT_PATH ]
then
   echo "CRITICAL ERROR - SNAPSHOT_PATH NOT DEFINED !!! Aborting..."
   exit 1
fi



check_and_mount()
{
	SOURCE_FNAME=`basename $1`
	DEST="$ANDROID_HOME/$1"
	SOURCE="$MCP_VIEW_PATH/EBTIPS/adaptors/Android/$PLATFORM_NAME/$1"
	DEST_TRUNC=${DEST%.rename_me}

	echo "$SOURCE_FNAME --> $DEST_TRUNC"

	ln -s -f "$SOURCE" "$DEST_TRUNC"
}

create_mount_folders()
{
	cd $1
	for PATHNAME in $(find . -type d -name "*" ! -name "_*" -print | sed 's/^\.\///')
	do
		echo Creating $ANDROID_HOME/$PATHNAME
		mkdir -p $ANDROID_HOME/$PATHNAME
	done
	cd -
}

link_mount_files()
{
	cd $1
	for FILENAME in $(find . -type f -name "*" ! -name "_*.*" ! -name "*.contrib*" ! -name "*.keep*" -print | grep -v "/_" | sed 's/^\.\///')
	do
		check_and_mount $FILENAME
	done
	cd -
}

# check if $1/$3 exits and is a link. removes if necessary. Than mount $2/$3 within $1
check_and_mount_params()
{
	DESTINATION_DIR=$1
	SOURCE_DIR=$2
	FILE_OR_FOLDER_TO_LINK=$3

#  backup_destination $1 $3
    
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
MCP_VIEW_PATH="$SNAPSHOT_PATH"
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

# first link the view to external/mcp
if [ -L "$ANDROID_HOME/mydroid/external/mcp/MCP_Common" ]
then
        unlink "$ANDROID_HOME/mydroid/external/mcp/MCP_Common"
fi


# copy Android.mk to view and UCM components folders
cp "$MCP_VIEW_PATH/MCP_Common/Platform/scripts/Android_root.mk" "$MCP_VIEW_PATH/Android.mk"
cp "$MCP_VIEW_PATH/EBTIPS/apps/Android.mk" "$MCP_VIEW_PATH/EBTIPS"
cp "$MCP_VIEW_PATH/EBTIPS/apps/Android.mk" "$MCP_VIEW_PATH/B_TIPS"
PWD=`pwd`
cd "$ANDROID_HOME/mydroid/external"
mkdir -p "mcp"

cd "mcp"

# Create link to MCP_Common if it not exists.
if [ -e "$ANDROID_HOME/mydroid/external/mcp/MCP_Common" ]
then
	echo "MCP_Common exists, due to GPS part1.sh execution."
else
	echo "Linking MCP_Common..."
	ln -s -T "$MCP_VIEW_PATH/MCP_Common" MCP_Common 
fi


ln -s -T "$MCP_VIEW_PATH/B_TIPS" B_TIPS 
ln -s -T "$MCP_VIEW_PATH/EBTIPS" EBTIPS 
ln -s -T "$MCP_VIEW_PATH/HSW_FMStack" HSW_FMStack 


ln -s -f "$MCP_VIEW_PATH/Android.mk" "Android.mk"
cd "$PWD" 

cd "$ANDROID_HOME/mydroid"

mkdir -p out/target/product/zoom2/obj/lib
mkdir -p out/target/product/zoom2/system/lib


#*/
cd "$PWD" 

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
elif [ "$PLATFORM_VERSION" = "2.2" ]; then
		PLATFORM_NAME="2.2"
elif [ "$PLATFORM_VERSION" = "2.2.1" ]; then
		PLATFORM_NAME="2.2"
else
        echo "UNKNOWN - Aborting (currently supporting Donut or Eclair(2.0 & 2.1) or Froyo (2.2 & 2.2.1) only)"
		exit
fi

cd - >/dev/null

# Create the new paths to hold the files
create_mount_folders "$MCP_VIEW_PATH/EBTIPS/adaptors/Android/$PLATFORM_NAME"

#link the files (ignore files starting with _ (underscore) and files in folder starting with _ (underscore).
link_mount_files "$MCP_VIEW_PATH/EBTIPS/adaptors/Android/$PLATFORM_NAME"

# link uim files
cd "$MCP_VIEW_PATH/MCP_Common/STK/STK_zoom3/uim"
for UIM_FILENAME in $(find . -type f -name "*" -print | sed 's/^\.\///')
do
	check_and_mount_params $ANDROID_HOME/mydroid/hardware/ti/omap3/ti_st/uim $MCP_VIEW_PATH/MCP_Common/STK/STK_zoom3/uim $UIM_FILENAME
done

# link stk files
cd "$MCP_VIEW_PATH/MCP_Common/STK/STK_zoom3/ti-st"
for STK_FILENAME in $(find . -type f -name "*" -print | sed 's/^\.\///')
do
	check_and_mount_params $ANDROID_HOME/kernel/android-2.6.32/drivers/misc/ti-st $MCP_VIEW_PATH/MCP_Common/STK/STK_zoom3/ti-st $STK_FILENAME
done

cd - >/dev/null

chmod a+wr $ANDROID_HOME/mydroid/frameworks/base/api/current.xml
echo "Done !!!"
echo "Done" >> $ANDROID_HOME/../mount_view_no_gps.done
