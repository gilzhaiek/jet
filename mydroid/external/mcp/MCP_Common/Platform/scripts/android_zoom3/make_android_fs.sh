#!/bin/sh

if [ -z "$ANDROID_HOME" ]
then
	echo "ANDROID_HOME is not set. Aborting."
        exit 1
fi

VIEW_SYSTEM_PATH=`pwd`

#check the version
cd "$ANDROID_HOME/mydroid"
PLATFORM_VERSION=`grep "PLATFORM_VERSION := " build/core/version_defaults.mk | awk 'BEGIN{FS=" "}{print $3}'`
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
		ANDROID_VERSION="L25.Inc3.4.P1"
else
        echo "UNKNOWN - Aborting (currently supporting Donut / Eclair(2.0 & 2.1) / Froyo (2.2 & 2.2.1) only)"
		exit
fi

echo "Creating Android File System for $ANDROID_VERSION based code"

if [ -z $ADDITIONAL_BIN_PATH ]
then
	echo "Binary path not defined, defaulting to /data/btips_wcs_android/admin/$ANDROID_VERSION"
	ADDITIONAL_BIN_PATH=/data/btips_wcs_android/admin/$ANDROID_VERSION
fi

#if [ -e $ANDROID_HOME/myfs ]
#then
#	rm -rf $ANDROID_HOME/myfs
#fi

echo "DSP Socket Nodes"
cd $ANDROID_HOME/mydroid
cp -Rfp ../dsp/system/lib/* ./out/target/product/zoom2/system/lib

cd $ANDROID_HOME
mkdir -p myfs
cd myfs
echo "Copying kernel modules"
cp -Rfp $ADDITIONAL_BIN_PATH/my_kernel_modules/*.ko .

echo "Copying root"
cp -Rfp ../mydroid/out/target/product/zoom2/root/* .

echo "Copying system"
cp -Rfp ../mydroid/out/target/product/zoom2/system/ .

echo "Copying data"
cp -Rfp ../mydroid/out/target/product/zoom2/data/ .

echo "Copying Sorenson spark DSP"
cp -Rfp $ADDITIONAL_BIN_PATH/sparkdec_sn.dll64P ./system/lib/dsp

echo "Copying FM app data"
mkdir -p data/data/com.ti.fmrxapp
cp -Rfp $VIEW_SYSTEM_PATH/fm/presets.txt data/data/com.ti.fmrxapp

echo "Copying PAN scripts, configuration and binaries"
mkdir -p data/btips/TI/scripts
mkdir -p var/lib/misc
mkdir -p var/run
cp $ANDROID_HOME/mydroid/external/mcp/MCP_Common/Platform/scripts/android_zoom3/pan/*.sh data/btips/TI/scripts/
cp $ANDROID_HOME/mydroid/external/mcp/MCP_Common/Platform/scripts/android_zoom3/pan/dnsmasq system/bin/
cp $ANDROID_HOME/mydroid/external/mcp/MCP_Common/Platform/scripts/android_zoom3/pan/tcpdump system/bin/
cp $ANDROID_HOME/mydroid/external/mcp/MCP_Common/Platform/scripts/android_zoom3/pan/20-dns.conf system/etc/dhcpcd/dhcpcd-hooks/
cp $ANDROID_HOME/mydroid/external/mcp/MCP_Common/Platform/scripts/android_zoom3/pan/dhcpcd.conf system/etc/dhcpcd/
cp $ANDROID_HOME/mydroid/external/mcp/MCP_Common/Platform/scripts/android_zoom3/pan/dnsmasq_gn.conf system/etc/
cp $ANDROID_HOME/mydroid/external/mcp/MCP_Common/Platform/scripts/android_zoom3/pan/dnsmasq_nap.conf system/etc/

echo "Setting up init.rc"
mv init.rc init.rc.bak
cp -Rfp $VIEW_SYSTEM_PATH/init.rc .

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
cp $ANDROID_HOME/mydroid/external/mcp/MCP_Common/Platform/init_script/android_zoom/* data/btips/TI/BTInitScript/
cp data/btips/TI/BTInitScript/tiinit_10.4.27.bts system/etc/firmware/TIInit_10.4.27.bts
cp data/btips/TI/BTInitScript/tiinit_7.2.31.bts system/etc/firmware/TIInit_7.2.31.bts
cp data/btips/TI/BTInitScript/tiinit_7.6.15.bts system/etc/firmware/TIInit_7.6.15.bts
cp data/btips/TI/BTInitScript/tiinit_10.5.20.bts system/etc/firmware/TIInit_10.5.20.bts
cp data/btips/TI/BTInitScript/tiinit_10.6.15.bts system/etc/firmware/TIInit_10.6.15.bts

echo "Copying busybox"
cp -Rfp $ADDITIONAL_BIN_PATH/busybox .

#cp -Rfp $ADDITIONAL_BIN_PATH/sparkdec_sn.dll64P system/lib/dsp
#cp -Rfp $ANDROID_HOME/mydroid/external/mcp/NaviLink/general/GPSCConfigFile.cfg .
#cp -Rfp $ANDROID_HOME/mydroid/external/mcp/NaviLink/general/GPSCConfigFile.cfg system/bin
#cp -Rfp $ANDROID_HOME/mydroid/external/mcp/NaviLink/general/patch-X.0.ce btips/TI/BTInitScript/ 
#cp -Rfp $ANDROID_HOME/mydroid/external/mcp/NaviLink/general/patch-X.0.ce system/bin
#cp -Rfp $ANDROID_HOME/mydroid/external/mcp/NaviLink/platforms/os/LINUX/Android/general/PeriodicConfFile.cfg .
#cp -Rfp $ANDROID_HOME/mydroid/external/mcp/NaviLink/platforms/os/LINUX/Android/general/GPSFTAppAdv.apk system/app
#cp -Rfp $ANDROID_HOME/mydroid/external/mcp/NaviLink/platforms/os/LINUX/Android/general/GPSStatus2.apk system/app
#cp -Rfp $ANDROID_HOME/mydroid/external/mcp/NaviLink/platforms/os/LINUX/Android/general/GpsConfigFile.txt system/bin
#cp -Rfp $ANDROID_HOME/mydroid/external/mcp/NaviLink/platforms/os/LINUX/Android/general/SuplConfig.spl system/bin
#mkdir certificate
#cp -Rfp $ANDROID_HOME/mydroid/external/mcp/NaviLink/platforms/os/LINUX/Android/general/client_keystore.bks certificate

echo "Applying all permissons to all files"
chmod 777 * -R

#export MY_IP=`echo $SSH_CLIENT | awk 'BEGIN{FS=" "}{print $1}'`
#if [ -z "$MY_IP" ]
#then
#     MY_IP="192.168.40.1"
#fi
#echo "Setting up logger output ip to $MY_IP"
#sed -i "s/192.168.40.1/$MY_IP/g" init.rc

echo "Now your Android file system at $ANDROID_HOME/myfs folder is ready to be copied to a microSD card or a NFS folder."

