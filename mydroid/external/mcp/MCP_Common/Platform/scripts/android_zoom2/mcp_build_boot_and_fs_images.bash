
#!/bin/bash

# $1 - the string to be printed
function print_highlight {
echo -e "\033[7m$1\033[0m"
}

# $1 - the string to be printed
function pause {
read -p "$1"
}


if [ "$1" = "" ]; then
print_highlight "Please enter view full path. usage: $0 <view_full_path>"
print_highlight "i.e. /data/wlan_wcs_android/a0387670/LiorC_MCP_2.x_1_BuildOnly"
exit
fi

if [ "$2" = "" ]; then
print_highlight "Please enter OPBU tar file full path . usage: $0 <view_full_path> <OPBU tar file full path>"
print_highlight "i.e. /data/btips_wcs_android/admin/L25E.19/OPBU_Linux_L25E.19_BE_MM.tar.bz2"
exit
fi

if [ "$3" = "" ]; then
print_highlight "Please enter the destination folder (must be new and empty one !!!!!!!!!) to extract the tar file to"
print_highlight "i.e. /data/wlan_wcs_android/Android/Release"
exit
fi

IsFolderEmpty=`ls $3`

if [ "$IsFolderEmpty" != "" ]; then
print_highlight "Error, Destination folder is not empty as required !!!"
fi
START_TIME=$(date +%s)

# This script must be run from the scripts folder only!!!!
SCRIPTS_FOLDER=$PWD

VIEW_FULL_PATH=$1
OPBU_FULL_PATH=$2
TAR_DEST_FOLDER_PATH=$3

print_highlight "Extracting $OPBU_FULL_PATH, it may take few minutes..."
tar -C $TAR_DEST_FOLDER_PATH -xjf $OPBU_FULL_PATH


cd $TAR_DEST_FOLDER_PATH
OPBU_MAIN_DIRECTORY=`ls`

print_highlight "I'll now build the  boot and fs images!"
print_highlight "Building boot images..."

export ANDROID_HOME="$PWD/$OPBU_MAIN_DIRECTORY"

print_highlight "Changing directory to scripts folder $SCRIPTS_FOLDER"
cd $SCRIPTS_FOLDER


print_highlight "ANDROID_HOME variable was set to $ANDROID_HOME"
./mcp_build_boot_images_L25.19.bash $VIEW_FULL_PATH

#pause "Press any key to continue (After boot images)..."

export VIEW_PATH=$VIEW_FULL_PATH

print_highlight "Building File System..."
./mcp_build_fs_script.bash

#pause "Press any key to continue (After mcp_build_fs_script.bash)..."

print_highlight "Running post fs build script..."
./mcp_post_fs_script.bash

#pause "Press any key to continue (After mcp_post_fs_script.bash)..."

print_highlight "$0 Done."

END_TIME=$(date +%s)
DIFF=$(( $END_TIME - $START_TIME ))
print_highlight "FYI - The full boot and fs images build process took $DIFF seconds."

exit
