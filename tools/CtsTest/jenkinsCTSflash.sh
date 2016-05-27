#!/bin/bash

#obtain workspace directory from argument
WORKSPACE=$1

#Number of tries allowed to enable ADB throughout the while script
TRIES=5

#JET mounting location
MNTPNT="/mnt/JET"

#JET enable adb location
JETADBLOCATION="/mnt/JET/ReconApps/LispXML/Input/adb.lxl"

#JET auto-generated file for adb
JETADBOUTLOCATION="/mnt/JET/ReconApps/LispXML/Output/adb.lxl"

#location of the adb-enabling lxl file
ADBLOCATION="$1/tools/CtsTest/adb.lxl"

#function to enable adb
enableADB () 
{
    echo
    echo "Enabling Android ADB through MTP...."

    #check if JET is available
    if [[ $(mtp-detect) == *"No raw devices found"* ]];
    then
        echo "no JET detected... FAIL"
        exit 1
    else
        echo "JET detected..."
    fi

    #check if adb is enabled already
    if [ $(adb get-state) == "device" ];
    then
        echo "ADB already enabled"
        return
    fi

    #check if mounting point is created already
    if [ ! -e "$MNTPNT" ];
    then
        echo
        echo "Mounting point for JET has not been created..."
        echo "Creating mounting point \"$MNTPNT\"..."
        sudo mkdir $MNTPNT
    fi

    #mount JET
    echo
    echo "Mounting JET to $MNTPNT...."
    sudo mtpfs -o allow_other $MNTPNT

    if [ -s "$MNTPNT" ];
    then
        echo "Mount FAILED..."
        echo "EXIT...."
        exit 1
    fi

    #enable adb with the .lxl file
    echo "Enabling ADB...."
    rm $JETADBLOCATION $JETADBOUTLOCATION
    cp $ADBLOCATION $JETADBLOCATION

    #unmount adb
    echo "Unmounting JET...."
    sudo umount -l $MNTPNT
    if [ ! -s "$MNTPNT" ];
    then
        echo
        echo "WARNING! UNMOUNT WAS UNSUCCESSFUL!!!"
        echo
    fi

    #delay for 2 sec then check if adb is enabled
    #if not, sleep for 10 sec restart the process
    #maximum of retries allowed are specified by the variable $TRIES
    sleep 2
    if [ $(adb get-state) != "device" ];
    then
        if (( TRIES > 0 ));
        then
            echo "Android ADB could not find device...."
            echo "RETRY..."
            sleep 10

            (( TRIES-- ))
            enableADB
        else
            echo "Android ADB could not be enables..."
            exit 1
        fi
    fi
}


echo "Target Workspace: $WORKSPACE"
enableADB

echo
echo "Flashing JET with new build...."

#flash JET
adb reboot bootloader
cd $WORKSPACE/omap4_emmc_files_jet/
sudo ./flash_new.sh

#wait for jet to boot completely
sleep 60
while true
do
    if [[ $(mtp-detect) == *"No raw devices found"* ]];
    then
        sleep 30
    else
        break
    fi
done

#enable ADB after flashing master build
sleep 10
enableADB
echo "SUCCESS...."
