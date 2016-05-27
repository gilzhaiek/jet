#!/bin/bash

# $1 - the string to be printed
function print_highlight {
echo -e "\033[7m$1\033[0m"
}

# param 1 = source view path
# param 2 = destination path

MY_TAR_NAME="MCP_GPS_BT_FM_SOURCES_FROYO.tar"

if [ "$1" = "" ]; then
print_highlight "Error!, Please enter your snapshot view full path!"
exit
fi

SOURCE_PATH=$1
DEST_FOLDER=$PWD

print_highlight "Gathering BTIPS,FM & GPS source files for ANDROID ZOOM3 - 2.2.1 (Froyo)"

# copy all required files from source drive to destination folder
cd $SOURCE_PATH

################################
#         $SOURCE_PATH         #
################################

tar -cf $MY_TAR_NAME Android.mk
echo "SOURCE_PATH Done."

################################
#           B_TIPS             #
################################

find B_TIPS/ -name '*.c' | tar -f $MY_TAR_NAME --append --files-from=-
find B_TIPS/ -name '*.h' | tar -f $MY_TAR_NAME --append --files-from=-
find B_TIPS/ -name '*.java' | tar -f $MY_TAR_NAME --append --files-from=-
find B_TIPS/ -name 'Android.mk*' | tar -f $MY_TAR_NAME  --append --files-from=-
find B_TIPS/ -name 'Makefile*' | tar -f $MY_TAR_NAME --append --files-from=-
echo "B_TIPS Done."

################################
#            EBTIPS            #
################################

find EBTIPS/ -name '*.c' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/ -name '*.h' | tar -f $MY_TAR_NAME  --append --files-from=-
find EBTIPS/ -name 'Android.mk*' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/ -name '*.patch' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/ -name '*.manual_patch' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/ -name '*.conf' | tar -f $MY_TAR_NAME --append --files-from=-
tar -f $MY_TAR_NAME --append EBTIPS/adaptors/Android/2.2/
tar -f $MY_TAR_NAME --append EBTIPS/apps/LINUX/btips_debug_app/     
tar -f $MY_TAR_NAME --append EBTIPS/apps/LINUX/btips_cli/
find EBTIPS/ -name 'Makefile*' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/apps/LINUX/ -name '*.h' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/apps/LINUX/ -name '*.py' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/apps/LINUX/ -name '*.sh' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/apps/LINUX/ -name '*.pyc' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/apps/LINUX/ -name '*.xml' | tar -f $MY_TAR_NAME --append --files-from=-
find EBTIPS/apps/LINUX/ -name '*.tmpl' | tar -f $MY_TAR_NAME --append --files-from=-
tar -f $MY_TAR_NAME --append EBTIPS/apps/LINUX/btips_log_tracer/Makefile
tar -f $MY_TAR_NAME --append EBTIPS/apps/LINUX/btips_log_tracer/btips_log_tracer.c
echo "EBTIPS Done."

#####################################
#            HSW_FMStack            #
#####################################

find HSW_FMStack/ -name '*.c' | tar -f $MY_TAR_NAME --append --files-from=-
find HSW_FMStack/ -name '*.h' | tar -f $MY_TAR_NAME --append --files-from=-
find HSW_FMStack/ -name '*.java' | tar -f $MY_TAR_NAME --append --files-from=-
find HSW_FMStack/build/ -name '*.dsp' | tar -f $MY_TAR_NAME --append --files-from=-
find HSW_FMStack/ -name 'Android.mk*' | tar -f $MY_TAR_NAME  --append --files-from=-
find HSW_FMStack/ -name 'Makefile*' | tar -f $MY_TAR_NAME --append --files-from=-
echo "HSW_FMStack Done."

#####################################
#            MCP_Common             #
#####################################

find MCP_Common/ -name '*.c*' | tar -f $MY_TAR_NAME --append --files-from=-     # *.c & *.cpp
find MCP_Common/ -name '*.h' | tar -f $MY_TAR_NAME --append --files-from=-
find MCP_Common/ -name '*.java' | tar -f $MY_TAR_NAME --append --files-from=-
find MCP_Common/ -name '*.mk*' | tar -f $MY_TAR_NAME --append --files-from=-    #all Android.mk files
find MCP_Common/ -name '*.sh' | tar -f $MY_TAR_NAME --append --files-from=-
find MCP_Common/Platform/init_script/android_zoom/ -name '*.bts' | tar -f $MY_TAR_NAME --append --files-from=-
find MCP_Common/Platform/init_script/android_zoom/ -name '*.ini' | tar -f $MY_TAR_NAME --append --files-from=-
find MCP_Common/Platform/scripts/android_zoom3/ -name '*.txt' | tar -f $MY_TAR_NAME --append --files-from=-
find MCP_Common/Platform/scripts/android_zoom3/ -name '*.rc' | tar -f $MY_TAR_NAME --append --files-from=-
find MCP_Common/Platform -name '*.conf' | tar -f $MY_TAR_NAME --append --files-from=-
tar -f $MY_TAR_NAME --append MCP_Common/Platform/scripts/android_zoom3/pan/dnsmasq
tar -f $MY_TAR_NAME --append MCP_Common/Platform/scripts/android_zoom3/pan/tcpdump

find MCP_Common/ -name '*.ds*' | tar -f $MY_TAR_NAME --append --files-from=-            # *.dsp & *.dsw
find MCP_Common/ -name 'Makefile*' | tar -f $MY_TAR_NAME --append --files-from=-
echo "MCP_Common Done."

#####################################
#               STK                 #
#####################################

tar -f $MY_TAR_NAME --append MCP_Common/STK/ti-st/Kconfig
tar -f $MY_TAR_NAME --append MCP_Common/STK/ti-st/Makefile
echo "STK Done."

#####################################
#             NaviLink              #
#####################################

find NaviLink/platforms/os/LINUX/Android/ -name '*' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/platforms/os/LINUX/Android/services/ -name '*' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name '*.c*' | tar -f $MY_TAR_NAME --append --files-from=-               # *.c & *.cpp & *.ce & *.cfg
find NaviLink/ -name '*.h' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name '*.java' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/SUPLC/ -name '*.*' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name 'Android.mk*' | tar -f $MY_TAR_NAME  --append --files-from=-
find NaviLink/ -name 'Makefile*' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name '*.apk' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/platforms/ -name '*.opt' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/platforms/ -name '*.ds*' | tar -f $MY_TAR_NAME --append --files-from=-    # *.dsp & *.dsw
find NaviLink/platforms/ -name '*.ncb' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/platforms/ -name '*.plg' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name '*.txt' | tar -f $MY_TAR_NAME --append --files-from=-
find NaviLink/ -name 'libsupl*.so' | tar -f $MY_TAR_NAME --append --files-from=-
echo "NaviLink Done."

#####################################
#             GPS_HOSTSW            #
#####################################

find GPS_HOSTSW/ -name '*.c*' | tar -f $MY_TAR_NAME --append --files-from=-     # *.c & *.cpp & *.ce & *.cfg
find GPS_HOSTSW/ -name '*.h' | tar -f $MY_TAR_NAME --append --files-from=-
find GPS_HOSTSW/ -name '*.java' | tar -f $MY_TAR_NAME --append --files-from=-
find GPS_HOSTSW/ -name 'Android.mk' | tar -f $MY_TAR_NAME  --append --files-from=-
find GPS_HOSTSW/ -name 'Makefile*' | tar -f $MY_TAR_NAME --append --files-from=-
find GPS_HOSTSW/ -name '*.apk' | tar -f $MY_TAR_NAME --append --files-from=-
find GPS_HOSTSW/ -name '*.opt' | tar -f $MY_TAR_NAME --append --files-from=-
find GPS_HOSTSW/ -name '*.ds*' | tar -f $MY_TAR_NAME --append --files-from=-    # *.dsp & *.dsw
find GPS_HOSTSW/ -name '*.so' | tar -f $MY_TAR_NAME --append --files-from=-
find GPS_HOSTSW/ -name '*.vsd' | tar -f $MY_TAR_NAME --append --files-from=-
echo "GPS_HOSTSW Done."

##################################################
#             Del unnecessary files              #
##################################################
echo "Deleting unnecessary files..."
tar -f $MY_TAR_NAME --delete EBTIPS/adaptors/Android/1.6/
tar -f $MY_TAR_NAME --delete EBTIPS/adaptors/Android/2.1/
tar -f $MY_TAR_NAME --delete MCP_Common/Platform/scripts/android_zoom2/
tar -f $MY_TAR_NAME --delete MCP_Common/Platform/os/WinXP/
tar -f $MY_TAR_NAME --delete MCP_Common/Platform/inc/WinXP/
tar -f $MY_TAR_NAME --delete MCP_Common/Platform/inc/ubuntu_x86/
tar -f $MY_TAR_NAME --delete MCP_Common/Platform/inc/2.0_zoom2/
tar -f $MY_TAR_NAME --delete MCP_Common/Platform/inc/1.6_zoom2/
tar -f $MY_TAR_NAME --delete MCP_Common/Platform/hw/WinXP
tar -f $MY_TAR_NAME --delete MCP_Common/Platform/fmhal/WinXP/
tar -f $MY_TAR_NAME --delete MCP_Common/Platform/bthal/WinXP
tar -f $MY_TAR_NAME --delete MCP_Common/Platform/audio_config/
tar -f $MY_TAR_NAME --delete MCP_Common/Platform/audio_codecs/WinXP/
tar -f $MY_TAR_NAME --delete EBTIPS/apps/WinXP-btips/
tar -f $MY_TAR_NAME --delete HSW_FMStack/apps/WinXP

echo "Compressing..."
gzip $MY_TAR_NAME
/bin/mv -f $PWD/$MY_TAR_NAME.gz $DEST_FOLDER/

print_highlight "Done."
print_highlight ""
print_highlight "The $MY_TAR_NAME.gz package is in:"
print_highlight "<< $DEST_FOLDER >>"
exit
