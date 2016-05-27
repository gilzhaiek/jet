#!/bin/bash

# $1 - the string to be printed
function print_highlight {
echo -e "\033[7m$1\033[0m"
}

if [ "$ANDROID_HOME" = "" ]; then
print_highlight "Error!, ANDROID_HOME environment variable is not defined!"
print_highlight "export ANDROID_HOME=<full path to LXX.YY folder>"
exit
fi

if [ "$1" = "" ]; then
print_highlight "Please enter view full path. usage: $0 <view_full_path>"
print_highlight "/data/wlan_wcs_android/a0387670/LiorC_MCP_2.x_1_BuildOnly"
exit
fi
VIEW_FULL_PATH=$1
PATCHES_FULL_PATH=$1/EBTIPS/adaptors/Android/2.1/_PATCHES

SCRIPT_FOLDER_ROOT=$PWD


print_highlight "I'll now build the boot images 4U (view path=$VIEW_FULL_PATH) ..."

export MYDROID=$ANDROID_HOME/mydroid
DestFolder=$MYDROID/_binaries
export PATH=$PATH:/data/btips_wcs_android/admin

mkdir $DestFolder

export MCP_SNAPVIEW_ROOT="$VIEW_FULL_PATH/../"
print_highlight "$MCP_SNAPVIEW_ROOT"


print_highlight "Mounting view $VIEW_FULL_PATH"
cd $VIEW_FULL_PATH/MCP_Common/Platform/scripts/android_zoom2/
chmod 777 mount_view.sh
./mount_view.sh

cd $MYDROID
print_highlight "Running patch 0007-zoom2_wilink7_adapter_dual_changes.manual_patch..."
patch -p1 <  $PATCHES_FULL_PATH/0007-zoom2_wilink7_adapter_dual_changes.manual_patch

cd $ANDROID_HOME
print_highlight "Running patch 0002-build_with_sym_links.patch..."
patch -p1 <  $PATCHES_FULL_PATH/0002-build_with_sym_links.patch

print_highlight "Preparing detination folder: $DestFolder"

mkdir $DestFolder

print_highlight "I'll now build the u-boot image..."
cd $MYDROID/bootable/bootloader/u-boot
mkdir $MYDROID/logs
make distclean
print_highlight "Set up the default configuration for the Zoom2® platform (cleaning .config file)"
make CROSS_COMPILE=arm-none-linux-gnueabi- omap3430zoom2_config
make
print_highlight "Copying u-boot.bin to destination folder: $DestFolder"
cp u-boot.bin $DestFolder
export PATH=$PATH:$MYDROID/bootable/bootloader/u-boot/tools
print_highlight "I'll now build the x-loader image (MLO)"
cd $MYDROID/bootable/bootloader/x-loader
make distclean
print_highlight "Set up the default configuration for the Zoom2® platform"
make CROSS_COMPILE=arm-none-linux-gnueabi- omap3430zoom2_config 
make ift
print_highlight "Copying MLO to destination folder: $DestFolder"
cp MLO $DestFolder

print_highlight "I'll now build the Images for you... "
cd $MYDROID/kernel/android-2.6.29

print_highlight "Cleaning up the kernel..."
make CROSS_COMPILE=arm-none-linux-gnueabi- distclean

print_highlight "Set up the default configuration for the Zoom2® platform..."
make CROSS_COMPILE=arm-none-linux-gnueabi- zoom2_defconfig

print_highlight "Configuring the kernel menu config via script.."
source $SCRIPT_FOLDER_ROOT/mcp_menu_config_update.bash

print_highlight "Building the kernel uImage..."
make ARCH=arm CROSS_COMPILE=arm-none-linux-gnueabi- uImage

print_highlight "Copying uImage to destination folder $DestFolder"
cp $MYDROID/kernel/android-2.6.29/arch/arm/boot/uImage $DestFolder

cd $MYDROID/kernel/android-2.6.29
print_highlight "Building the USB & STK kernel modules..."
make ARCH=arm CROSS_COMPILE=arm-none-linux-gnueabi- modules

print_highlight "Copying kernel modules to destination folder $DestFolder"
cp $MYDROID/kernel/android-2.6.29/drivers/misc/ti-st/*.ko $DestFolder   

print_highlight "Creating zoom2/root directory"
mkdir $MYDROID/out/target/product/zoom2/root

print_highlight "Copying Spark dll to zoom2 root folder"
cp /data/btips_wcs_android/admin/L25E.19/sparkdec_sn.dll64P $MYDROID/out/target/product/zoom2/root

print_highlight "Copying kernel modules to zoom2 root folder"
cp $MYDROID/kernel/android-2.6.29/drivers/misc/ti-st/*.ko $MYDROID/out/target/product/zoom2/root

print_highlight "The following binaraies were created and copy to $DestFolder"
ls -la "$DestFolder"

print_highlight "$0 Done."

exit
