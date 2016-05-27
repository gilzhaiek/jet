
#!/bin/bash

# $1 - the string to be printed
function print_highlight {
echo -e "\033[7m$1\033[0m"
}

# param 1 = source view path
# param 2 = destination path
# param 3 = android version 
# $1 - the string to be printed

MY_TAR_NAME="MCP_GPS_BT_FM_SOURCES.tar"

if [ "$1" = "" ]; then
print_highlight "Error!, Please enter your snapshot view full path!"
exit
fi

SOURCE_PATH=$1
TMP_FOLDER=__tmp

print_highlight "I will now build the BTIPS,FM & GPS sources package for ANDROID ZOOM 2 2.1 (Eclair)"

# copy all required files from source drive to destination folder
cd $SOURCE_PATH

find B_TIPS/ -name '*.c' | tar -f $MY_TAR_NAME --append --files-from=-
find B_TIPS/ -name '*.h' | tar -f $MY_TAR_NAME --append --files-from=-
find B_TIPS/ -name 'Android.mk*' | tar -f $MY_TAR_NAME -X MCP_Common/Platform/scripts/android_zoom2/Exclude_For_Android_Zoom2_No_Audio --append --files-from=-

# Packaging EBTIPS VOB sources 
find EBTIPS/ -name '*.c' | tar -f $MY_TAR_NAME -X MCP_Common/Platform/scripts/android_zoom2/Exclude_For_Android_Zoom2_No_Audio --append --files-from=-
find EBTIPS/ -name '*.h' | tar -f $MY_TAR_NAME -X MCP_Common/Platform/scripts/android_zoom2/Exclude_For_Android_Zoom2_No_Audio --append --files-from=-
find EBTIPS/ -name 'Android.mk*' | tar -f $MY_TAR_NAME -X MCP_Common/Platform/scripts/android_zoom2/Exclude_For_Android_Zoom2_No_Audio --append --files-from=-
find EBTIPS/ -name '*.patch' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/ -name '*.manual_patch' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/ -name '*.conf' | tar -f $MY_TAR_NAME --append --files-from=-

#call copy_android_adaptor $SOURCE_PATH $DEST_PATH/NaviLink-linux-6.2 2.1
find EBTIPS/adaptors/Android/2.1 -name '*.java' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.aidl' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.html' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.mk' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*preloaded-classes' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.cpp' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.h' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.xml' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*MODULE_LICENSE_APACHE2' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*NOTICE' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.png' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.map' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.classpath' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.project' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.properties' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.css' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.jar' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.zip' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.ogg' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name 'README.TXT*' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name 'package-list*' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.patch' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name '*.c' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name 'Kconfig*' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1 -name 'Makefile*' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/adaptors/Android/2.1/external/bridge-utils/ -name '*' | tar -f $MY_TAR_NAME --append --files-from=-

#call copy_btscripts.bat $SOURCE_PATH $DEST_PATH/NaviLink-linux-6.2
find EBTIPS/apps/LINUX/btscripts/ -name '*.h' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/apps/LINUX/btscripts/ -name '*.py' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/apps/LINUX/btscripts/ -name '*.sh' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/apps/LINUX/btscripts/ -name '*.pyc' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/apps/LINUX/btscripts/ -name '*.xml' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/apps/LINUX/btscripts/ -name '*.tmpl' | tar -f $MY_TAR_NAME --append --files-from=-
tar -f $MY_TAR_NAME --append EBTIPS/apps/LINUX/btips_log_tracer/Makefile
tar -f $MY_TAR_NAME --append EBTIPS/apps/LINUX/btips_log_tracer/btips_log_tracer.c
tar -f $MY_TAR_NAME --append EBTIPS/apps/LINUX/btips_log_tracer/Makefile

find HSW_FMStack/ -name '*.c' | tar -f $MY_TAR_NAME --append --files-from=-
find HSW_FMStack/ -name '*.h' | tar -f $MY_TAR_NAME --append --files-from=-
find HSW_FMStack/ -name 'Android.mk*' | tar -f $MY_TAR_NAME -X MCP_Common/Platform/scripts/android_zoom2/Exclude_For_Android_Zoom2_No_Audio --append --files-from=-

find NaviLink/platforms/os/LINUX/Android/ -name '*' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/platforms/os/LINUX/Android/services/ -name '*' | tar -f $MY_TAR_NAME --append --files-from=-

find MCP_Common/ -name '*.c' | tar -f $MY_TAR_NAME -X MCP_Common/Platform/scripts/android_zoom2/Exclude_For_Android_Zoom2_No_Audio --append --files-from=-
find MCP_Common/ -name '*.h' | tar -f $MY_TAR_NAME -X MCP_Common/Platform/scripts/android_zoom2/Exclude_For_Android_Zoom2_With_Audio --append --files-from=-
find MCP_Common/ -name 'Android.mk*' | tar -f $MY_TAR_NAME -X MCP_Common/Platform/scripts/android_zoom2/Exclude_For_Android_Zoom2_No_Audio --append --files-from=-

#call copy_init_scripts.bat $SOURCE_PATH $DEST_PATH/NaviLink-linux-6.2 android_zoom2
find MCP_Common/Platform/init_script/android_zoom2/ -name '*.bts' | tar -f $MY_TAR_NAME --append --files-from=-
find MCP_Common/Platform/init_script/android_zoom2/ -name '*.ini' | tar -f $MY_TAR_NAME --append --files-from=-
find MCP_Common/Platform/init_script/android_zoom2/ -name '*.c' | tar -f $MY_TAR_NAME --append --files-from=-

find MCP_Common/Platform/scripts/android_zoom2/ -name '*.sh' | tar -f $MY_TAR_NAME --append --files-from=-
find MCP_Common/Platform/scripts/android_zoom2/ -name '*.txt' | tar -f $MY_TAR_NAME --append --files-from=-
find MCP_Common/Platform/scripts/android_zoom2/ -name '*.rc' | tar -f $MY_TAR_NAME --append --files-from=-

find MCP_Common/Platform -name '*.conf' | tar -f $MY_TAR_NAME --append --files-from=-
tar -f $MY_TAR_NAME --append MCP_Common/Platform/scripts/android_zoom2/pan/dnsmasq
tar -f $MY_TAR_NAME --append MCP_Common/Platform/scripts/android_zoom2/pan/tcpdump

tar -f $MY_TAR_NAME --append MCP_Common/STK/ti-st/Kconfig
tar -f $MY_TAR_NAME --append MCP_Common/STK/ti-st/Makefile

find NaviLink/ -name '*.c' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name '*.h' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name 'Android.mk*' | tar -f $MY_TAR_NAME -X MCP_Common/Platform/scripts/android_zoom2/Exclude_For_Android_Zoom2_No_Audio --append --files-from=-
find NaviLink/ -name '*.ce' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name '*.cfg' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name '*.apk' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name '*.exe' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name '*.cpp' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name '*.txt' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name 'libsupl*.so' | tar -f $MY_TAR_NAME --append --files-from=-

# Extracting the tar file to a temporary folder
# Handling the special copies commands

mkdir $TMP_FOLDER
cd $TMP_FOLDER 
tar -xf ../$MY_TAR_NAME
mkdir MCP_Common/Platform/audio_codecs
cp EBTIPS/Android.mk MCP_Common/Platform/audio_codecs/Android.mk

mkdir MCP_Common/Platform/audio_codecs/LINUX
cp ../NaviLink/platforms/os/build/build_packages/prebuilt_audiocodecs_Android.mk MCP_Common/Platform/audio_codecs/LINUX/Android.mk
cp ../NaviLink/platforms/os/build/build_packages/libaudiocodecs.a MCP_Common/Platform/audio_codecs/LINUX/libaudiocodecs.a
tar -cjf ../btips_fm_gps_sources.tar.bz2 *
cd ../
/bin/rm -rf $TMP_FOLDER

# create the zip file

echo "Done. The btips_fm_gps_sources.tar.bz2 package is ready and can be found in $PWD"
exit
