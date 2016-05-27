#!/bin/sh

if [ -z "$ANDROID_HOME" ]
then
	echo "ANDROID_HOME is not set. Aborting."
        exit 1
fi

#VIEW_SYSTEM_PATH=`pwd`
cd $ANDROID_HOME/mydroid
MYDROID=`pwd`
YOUR_PATH=$ANDROID_HOME

BTIPS_SCRIPTS_PATH=$MYDROID/external/mcp/MCP_Common/Platform/scripts/android_blaze/
#ADDITIONAL_BIN_PATH=$YOUR_PATH/additional_bin

#check the version
cd $MYDROID
PLATFORM_VERSION=`grep "PLATFORM_VERSION := " build/core/version_defaults.mk | awk 'BEGIN{FS=" "}{print $3}'`
TARGET_PRODUCT=`grep "TARGET_PRODUCT:=" buildspec.mk | awk 'BEGIN{FS="="}{print $2}'`
PLATFORM_NAME=""
if [ "$PLATFORM_VERSION" = "1.6" ]; then
	ANDROID_VERSION="L25.14"
elif [ "$PLATFORM_VERSION" = "Eclair" ]; then
	ANDROID_VERSION="L25E.16"
elif [ "$PLATFORM_VERSION" = "2.1" ]; then
	ANDROID_VERSION="L25E.19"
elif [ "$PLATFORM_VERSION" = "2.2" ]; then
	ANDROID_VERSION="L25.Inc3.3"
elif [ "$PLATFORM_VERSION" = "2.2.1" ]; then
	if [ "$TARGET_PRODUCT" = "zoom2" ]; then
		ANDROID_VERSION="L25.Inc3.4"
	elif [ "$TARGET_PRODUCT" = "blaze" ]; then
		ANDROID_VERSION="L27.Inc10.1"
		BSP_DIR_NAME=L27.Inc1.10.1
	else 
		echo "UNKNOWN PLATFORM_VERSION-$PLATFORM_VERSION - Aborting "
	fi
elif [ "$PLATFORM_VERSION" = "2.3.3" ]; then
	ANDROID_VERSION="L27.INC1.12.1"
	BSP_DIR_NAME=L27.INC1.12.1
else
    echo "UNKNOWN $PLATFORM_VERSION - Aborting (currently supporting Donut / Eclair(2.0 & 2.1) / Froyo (2.2) / Gingerbread (2.3.3) only)"
#	exit
fi

BSP_PATH=/data/btips_wcs_android/admin/$BSP_DIR_NAME
MYBOOT_PATH=$BSP_PATH/myboot

echo "Creating Android File System for $ANDROID_VERSION based code"
echo "Boot path: $MYBOOT_PATH"

#if [ -z $ADDITIONAL_BIN_PATH ]
#then
#	echo "Binary path not defined, defaulting to /data/btips_wcs_android/admin/$ANDROID_VERSION"
#	ADDITIONAL_BIN_PATH=/data/btips_wcs_android/admin/$ANDROID_VERSION
#fi

#if [ -e $ANDROID_HOME/myfs ]
#then
#	rm -rf $ANDROID_HOME/myfs
#fi

#echo "DSP Socket Nodes"
#cp -Rfp $YOUR_PATH/dsp/system/lib/* $MYDROID/out/target/product/blaze/system/lib
#cp -Rfp $YOUR_PATH/dsp/system/bin/* $MYDROID/out/target/product/blaze/system/bin

OUTPUT_PATH=$YOUR_PATH/output

cd $YOUR_PATH
mkdir -p output
cd output
mkdir -p myfs
cd myfs

#echo "Copying kernel modules"
cp -Rfp $MYBOOT_PATH/*.ko .

echo "Copying root"
cp -Rfp $MYDROID/out/target/product/blaze/root/* .

echo "Copying system"
cp -Rfp $MYDROID/out/target/product/blaze/system/ .

echo "Copying data"
cp -Rfp $MYDROID/out/target/product/blaze/data/ .

if [ "$PLATFORM_VERSION" = "2.2.1" ]; then
    if [ "$TARGET_PRODUCT" = "blaze" ]; then
        echo "Copying GFX data"
        cp -Rfp $YOUR_PATH/GFX/* .
    fi
fi

if [ "$PLATFORM_VERSION" = "2.3.3" ]; then
    echo "Copying graphic data"
    cp -rf $MYDROID/device/ti/proprietary-open/graphics/omap4/* .
fi

#echo "Copying Sorenson spark DSP"
#cp -Rfp $ADDITIONAL_BIN_PATH/sparkdec_sn.dll64P ./system/lib/dsp

echo "Copying FM app data"
mkdir -p data/data/com.ti.fmapp
cp -Rfp $BTIPS_SCRIPTS_PATH/fm/presets.txt data/data/com.ti.fmapp

echo "Copying PAN scripts, configuration and binaries"
mkdir -p data/btips/TI/scripts
mkdir -p var/lib/misc
mkdir -p var/run
mkdir -p system/bin/
cp $MYDROID/external/mcp/MCP_Common/Platform/scripts/android_blaze/pan/*.sh data/btips/TI/scripts/
cp $MYDROID/external/mcp/MCP_Common/Platform/scripts/android_blaze/pan/dnsmasq system/bin/
cp $MYDROID/external/mcp/MCP_Common/Platform/scripts/android_blaze/pan/tcpdump system/bin/
cp $MYDROID/external/mcp/MCP_Common/Platform/scripts/android_blaze/pan/20-dns.conf system/etc/dhcpcd/dhcpcd-hooks/
cp $MYDROID/external/mcp/MCP_Common/Platform/scripts/android_blaze/pan/dhcpcd.conf system/etc/dhcpcd/
cp $MYDROID/external/mcp/MCP_Common/Platform/scripts/android_blaze/pan/dnsmasq_gn.conf system/etc/
cp $MYDROID/external/mcp/MCP_Common/Platform/scripts/android_blaze/pan/dnsmasq_nap.conf system/etc/

echo "Setting up init.rc"
mv init.rc init.rc.bak
if [ "$PLATFORM_VERSION" = "2.2.1" ]; then
    if [ "$TARGET_PRODUCT" = "blaze" ]; then
        cp -Rfp $BTIPS_SCRIPTS_PATH/init_2.2.1_blaze.rc ./init.rc
    else
        cp -Rfp $BTIPS_SCRIPTS_PATH/init.rc ./init.rc
    fi
else
    cp -Rfp $BTIPS_SCRIPTS_PATH/init.rc ./init.rc
fi

echo "Creating additional folders"
mkdir -p data/btips/TI/BTInitScript
mkdir -p data/btips/TI/opp
mkdir -p data/btips/TI/ftproot
mkdir -p data/btips/TI/ftproot_c
mkdir -p data/btips/TI/images/bip/xml
mkdir -p data/btips/TI/bip
mkdir -p data/btips/TI/bpp
mkdir -p system/etc/firmware
mkdir -p tmp
mkdir -p usr


echo "Copying BT Init scripts"
cp $MYDROID/external/mcp/MCP_Common/Platform/init_script/android_zoom/* data/btips/TI/BTInitScript/
cp data/btips/TI/BTInitScript/tiinit_10.4.27.bts system/etc/firmware/TIInit_10.4.27.bts
cp data/btips/TI/BTInitScript/tiinit_7.2.31.bts system/etc/firmware/TIInit_7.2.31.bts
cp data/btips/TI/BTInitScript/tiinit_7.6.15.bts system/etc/firmware/TIInit_7.6.15.bts
cp data/btips/TI/BTInitScript/tiinit_10.5.20.bts system/etc/firmware/TIInit_10.5.20.bts
cp data/btips/TI/BTInitScript/tiinit_10.6.15.bts system/etc/firmware/TIInit_10.6.15.bts
#echo "Copying busybox"
#cp -Rfp $ADDITIONAL_BIN_PATH/busybox .


#RAM UPDATE START
#echo "Copying WLAN components"
#cp -Rfp $YOUR_PATH/WLAN/* system/etc/wifi/

#echo "Copying GPS components"
#cp -f $MYDROID/external/mcp/NaviLink/general/GPSCConfigFile.cfg                              system/bin/
#cp -f $MYDROID/external/mcp/NaviLink/general/patch-X.0.ce                                    system/bin/
#cp -f $MYDROID/external/mcp/NaviLink/general/pathconfigfile.txt                              system/bin/
#cp -f $MYDROID/external/mcp/NaviLink/platforms/os/LINUX/Android/general/PeriodicConfFile.cfg system/bin/
#cp -f $MYDROID/external/mcp/NaviLink/platforms/os/LINUX/Android/general/GpsConfigFile.txt    system/bin/
#cp -f $MYDROID/external/mcp/NaviLink/platforms/os/LINUX/Android/general/SuplConfig.spl       system/bin/
#cp -f $MYDROID/external/mcp/NaviLink/platforms/os/LINUX/Android/general/busybox              .
#mkdir -p certificate
#cp -f $MYDROID/external/mcp/NaviLink/platforms/os/LINUX/Android/general/client_keystore.bks  certificate/
#RAM UPDATE END


echo "Applying all permissons to all files"
chmod 777 * -R

echo "Tar file system"
tar -cf ../myfs.tar * 

echo "Copying boot from $MYBOOT_PATH"
cd $OUTPUT_PATH
mkdir -p boot
cp -Rfp $MYBOOT_PATH/* boot

echo "Now your Android file system at $YOUR_PATH/output folder is ready to be copied to a microSD card or a NFS folder."

