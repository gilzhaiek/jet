#!/bin/bash
set -e
TESTLEN=10 
read -e -p "Enter the board serial number: " -i "B0000" FOLDER
echo "Waiting for adb"
while [ $(adb devices | wc -l) -le 2 ]; do printf .; sleep 1; done
mkdir -p $FOLDER
cd $FOLDER
adb shell senstest 1 1 1
adb shell senstest 1 2
adb shell senstest 1 4 1
adb shell senstest 3 1 10
adb shell senstest 3 2 10
adb shell senstest 3 4 10
read -p "Place the device in the jig (CPU/LED facing up) and press enter..."
echo "Capturing data..."
timeout $TESTLEN adb shell senstest 4 | grep 'acc\|mag\|gyr' > cpu.csv &
i=$TESTLEN;while [ $i -ge 1 ]; do printf $i" "; sleep 1; i=$((i - 1)); printf "\r"; done
echo "Done"
read -p "Place the device in the jig (memory facing up/LED facing down) and press enter..."
echo "Capturing data..."
timeout $TESTLEN adb shell senstest 4 | grep 'acc\|mag\|gyr' > mem.csv &
i=$TESTLEN;while [ $i -ge 1 ]; do printf $i" "; sleep 1; i=$((i - 1)); printf "\r"; done
echo "Done"
wc *
adb shell senstest 2 1
adb shell senstest 2 2
adb shell senstest 2 4
#adb shell mv /data/current.log /data/current.$FOLDER.log
adb shell reboot -p
generate .5 | grep conv_B > conv_B
cd ..
echo $FOLDER tested on $(date) >> log.txt 
echo "Device shutdown"
