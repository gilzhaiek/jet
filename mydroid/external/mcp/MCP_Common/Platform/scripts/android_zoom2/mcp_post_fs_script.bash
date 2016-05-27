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

SCRIPTS_FOLDER=$PWD

FS_FOLDER_PATH=$ANDROID_HOME/myfs
cd $FS_FOLDER_PATH
print_highlight "Creating busybox folder and copy executable to system/bin..."
mkdir data/busybox
cp busybox system/bin/


print_highlight "Creating busybox symbolic links..."
cd data/busybox
source $SCRIPTS_FOLDER/mcp_create_busybox_symlink


print_highlight "Packaging myFS.tar..."
cd $FS_FOLDER_PATH
tar -cvf ../myFS.tar *

print_highlight "Done."

exit
