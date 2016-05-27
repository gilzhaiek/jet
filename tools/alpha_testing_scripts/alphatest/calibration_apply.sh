#!/bin/bash
# 
# This script searches the current directory (and subdirectories) for a file called conv_B 
# which contains accelerometer calibration values. 
# If it finds one, it tries to download sensors.conf from a connected device, replace the stock values 
# with the one from the previously mentioned file, and re-upload the new sensors.conf with the 
# new values back to the device. The device may require a reboot for these values to take effect
#
# alex.bell@reconinstruments.com
set -e
function wait_adb()
{
 if [ $(adb devices | wc -l) -le 2 ] 
  then echo "waiting for a device"
 fi
 while [ $(adb devices | wc -l) -le 2 ]; do printf .; sleep 5; done
}

SN=$(basename "$(pwd)") # file must be run from the folder named after the board for this to work
echo "SN is $SN"
LOC=$(find ./ -name conv_B) # search current folder for conv_B file that contains conv_B vals for accelerometer
if [ -z "$LOC" ]; then
    echo "conv_B file not found"
    exit # no values were found, cannot continue
fi
VALS=$(cat $LOC)
wait_adb # wait for a device to be connected before trying to pull the file
if [ ! -d sensconf ]; then
  mkdir sensconf
fi
cd sensconf
echo grabbing stock file from device '(/data/system/sensors.conf)'
adb pull /data/system/sensors.conf
TARGET_LINE_NUMBER=$(cat -n sensors.conf | grep -A10 Accel | grep conv_B | awk '{print $1}')
echo "Targeting line" $TARGET_LINE_NUMBER
sed -n "1,$(($TARGET_LINE_NUMBER-1))p" sensors.conf > newfile
echo $VALS >> newfile
sed -n "$(($TARGET_LINE_NUMBER+1)),\$p" sensors.conf >> newfile
mv sensors.conf sensors.conf.old
mv newfile sensors.conf
wait_adb
adb remount
adb push sensors.conf /data/system/
cd ..
echo new sensors.conf uploaded sucessfully
adb shell cat /data/system/sensors.conf | less +$TARGET_LINE_NUMBER
