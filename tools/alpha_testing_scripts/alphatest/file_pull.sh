#!/bin/bash
set -e
# https://gist.github.com/davejamesmiller/1965569
function reboot_wait()
{
    adb reboot
    wait_adb
    sleep 15
}
function copy_gps()
{
    mkdir -p gps
    cd gps
    wait_adb
    echo "Copying GPS data"
    adb pull $(adb shell ls /data/gps/logs/FromSensor\* | sort -r | head -n1 | dos2unix)
    adb pull $(adb shell ls /data/data/com.reconinstruments.alphatester/files/gps_speed.\* | sort -r | head -n1 | dos2unix)
    adb pull /data/data/com.reconinstruments.alphatester/files/gps_speed_spike_detected.$(ls gps* | head -n1 | awk -F '.' '{print $2}')
    cd ..
}
function copy_cam()
{
    mkdir -p camera
    cd camera
    wait_adb
    echo "Copying camera images"
    for x in $(adb shell ls /sdcard/DCIM/Camera/IMG\* | sort -r | head -n2 | dos2unix | xargs)
    do 
        adb pull $x
    done
    cd ..
}
function copy_compass()
{
    wait_adb
    echo "Copying compass linearity data"
    mkdir -p compass
    cd compass
    for x in $(adb shell ls /data/data/com.reconinstruments.alphatester/files/linearity\* | sort -r | head -n2 | dos2unix | xargs)
    do 
        adb pull $x
    done
    cd ..
}
function copy_battery()
{
    wait_adb
    echo "Copying battery data"
    mkdir -p battery
    cd battery
    adb pull $(adb shell ls /data/data/com.reconinstruments.voltagelogger/files/\* | sort -r | head -n1 | dos2unix)
    cd ..
}
function wait_adb()
{
    echo "Waiting for adb"
    while [ $(adb devices | wc -l) -le 2 ]; do printf .; sleep 5; done
}
function run_all()
{
copy_gps
copy_cam
copy_compass
}
prompt="Pick an option:"
options=("Copy All" "Copy GPS Data" "Copy camera images" "Copy compass data" "Copy battery data")

PS3="$prompt "
select opt in "${options[@]}" "Quit"; do 

case "$REPLY" in

1 ) run_all;;
2 ) copy_gps;;
3 ) copy_cam  ;;
4 ) copy_compass      ;;
5 ) copy_battery      ;;

$(( ${#options[@]}+1 )) ) echo "Goodbye!"; break;;
*) echo "Invalid option. Try another one.";continue;;

esac

done




