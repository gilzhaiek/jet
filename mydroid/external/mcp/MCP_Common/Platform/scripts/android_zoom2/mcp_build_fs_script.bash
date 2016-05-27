#!/bin/bash

# $1 - the string to be printed
function print_highlight {
echo -e "\033[7m$1\033[0m"
}


if [ "$ANDROID_HOME" = "" ]; then
print_highlight "Error!, ANDROID_HOME environment variable is not defined!"
print_highlight "i.e. export ANDROID_HOME=<full path to LXX.YY folder>"
exit
fi

if [ "$VIEW_PATH" = "" ]; then
print_highlight "Error!, VIEW_PATH environment variable was is defined!"
print_highlight "i.e. export VIEW_PATH=/data/wlan_wcs_android/a0387670/LiorC_MCP_2.x_1_BuildOnly"
exit
fi

SCRIPTS_FOLDER=$PWD

MYDROID=$ANDROID_HOME/mydroid


print_highlight "I'll now build the File System for you!"
cd $ANDROID_HOME/mydroid

cp -Rfp vendor/ti/zoom2/buildspec.mk.default buildspec.mk
source build/envsetup.sh

print_highlight "Updating supplicant driver_ti.c file (both WiLink6.1 & WiLink7)"
cp $VIEW_PATH/WiLink/CUDK/wpa_suppl/wpa_supplicant/external_wpa_patch/driver_ti.c $MYDROID/system/wlan/ti/wilink_6_1/wpa_supplicant_lib/driver_ti.c
cp $VIEW_PATH/WiLink/CUDK/wpa_suppl/wpa_supplicant/external_wpa_patch/driver_ti.c $MYDROID/system/wlan/ti/wilink7/wpa_supplicant_lib/driver_ti.c

print_highlight "Start compiling file system...."
make -j4

print_highlight "Update APIs if needed...."
make update-api

print_highlight "Resuming the file system compilation...."
make -j4

print_highlight "File System compilation done."

print_highlight "Packaging the file system..."
cd $VIEW_PATH/MCP_Common/Platform/scripts/android_zoom2/

print_highlight "Runnnig make_android_fs.sh..."
chmod 777 make_android_fs.sh 
./make_android_fs.sh


print_highlight "Running fs post script..."
cd $SCRIPTS_FOLDER
./mcp_post_fs_script.bash

print_highlight "$0 Done."

exit
