#!/bin/bash
# 
# This script is for testing the gps and camera. These features are tested together because
# of a bug in the GPS where high system temperature can trigger TXCO fluctuations and produce
# inaccuracies in the gps location. This can manifest as speed spikes of over 100km/h even
# when the unit is sitting on the desk. The camera is used because it can cause the
# board to heat up enough to cause these issues.
#
# This 

set -e
CALIBRATE_ACCEL=0
# https://gist.github.com/davejamesmiller/1965569
function ask {
    while true; do
 
        if [ "${2:-}" = "Y" ]; then
            prompt="Y/n"
            default=Y
        elif [ "${2:-}" = "N" ]; then
            prompt="y/N"
            default=N
        else
            prompt="y/n"
            default=
        fi
 
        # Ask the question
        read -p "$1 [$prompt] " REPLY
 
        # Default?
        if [ -z "$REPLY" ]; then
            REPLY=$default
        fi
 
        # Check if the reply is valid
        case "$REPLY" in
            Y*|y*) return 0 ;;
            N*|n*) return 1 ;;
        esac
 
    done
}
function delete_aiding()
{
    wait_adb
        echo "Deleting aiding files"
}
function reboot_wait()
{
    adb reboot
    wait_adb
    sleep 15
}
function start_cam()
{
    echo "========================================================="
    adb shell am start -n com.reconinstruments.camera/.app.CameraActivity
    sleep 5
    echo "Get ready to take photo of color board then press enter"
    read -p
    echo "Taking photo 1 in 5 seconds"
    for x in {1..5}
    do
        printf .
        sleep 1
    done
    adb shell input keyevent 66 # take photo
    printf "\n"
    echo "Taking photo 2 in 5 seconds"
    for x in {1..5}
    do
        printf .
        sleep 1
    done
    adb shell input keyevent 66 # take photo
    printf "\n"
    echo "Done... returning to previous app"
    sleep 3 
    adb shell input keyevent 4 # back button
    echo "========================================================="
}
function run_gps()
{
    echo "========================================================="
    echo "Running gps"
    adb shell am start -n com.reconinstruments.alphatester/.GPS
    echo "Wait for lock, then wait for timer to expire (take some photos in the mean time), then copy files back"
    echo "========================================================="
}
function copy_data()
{
    echo "========================================================="
    echo "Copying data"
    mkdir -p gps
    cd gps
    adb pull $(adb shell ls /data/gps/logs/FromSensor\* | sort -r | head -n1 | dos2unix)
    adb pull $(adb shell ls /data/data/com.reconinstruments.alphatester/files/gps_speed.\* | sort -r | head -n1 | dos2unix)
    adb pull /data/data/com.reconinstruments.alphatester/files/gps_speed_spike_detected.$(ls ./gps* | head -n1 | awk -F '.' '{print $2}')
    cd ..
    mkdir -p camera
    cd camera
    for x in $(adb shell ls /sdcard/DCIM/Camera/IMG\* | sort -r | head -n2 | dos2unix | xargs)
    do 
        adb pull $x
    done
    cd ..
    mkdir -p compass
    cd compass
    adb pull $(adb shell ls /data/data/com.reconinstruments.alphatester/files/linearity\* | sort -r | head -n1 | dos2unix)
    cd ..
    echo "========================================================="
}
function wait_adb()
{
    echo "Waiting for adb"
    while [ $(adb devices | wc -l) -le 2 ]; do printf .; sleep 5; done
    printf "\n"
}
function run_compass()
{
    echo "========================================================="
    wait_adb
    echo "Calibrating compass, press enter to launch compass test"
    adb shell am start -a com.reconinstruments.compass.CALIBRATE
    read -p
    echo "running compass test"
    adb shell am start -n com.reconinstruments.alphatester/.Compass
    echo "Compass linearity testing must be done with USB disconnected. Align the unit facing north and then (using a paired remote) press select to record the angle at north and begin the test. The LED on the unit will flash each time an angle is recorded. It will flash 3 times when the last angle has been captured. Then reconnect the usb"
    read -p "Press enter when compass testing is complete"
    wait_adb
    echo "========================================================="
}
function run_all()
{
echo "Deleting GPS aiding files. Device will take some time to reacquire lock next time"
delete_aiding
echo "Done"
echo "Rebooting device"
reboot_wait  
echo "TEST 1: GPS"
echo " - This script will attempt to start the GPS test app. The app will search for GPS lock and then hold it for 5 minutes. After this the test will be complete and files can be copied at any point. During the GPS test, the camera test will be run. The camera app will be started and 2 photos will be taken in an attempt to cause the GPS clock to drift by heating up the PCB"
run_gps      
read -p "Press enter once lock is achieved so camera test can proceed"
start_cam
read -p "Press enter when GPS test is complete, and begin compass test"
run_compass
# TODO
copy_data
}
function test_func()
{
CALIBRATE_ACCEL=$(ask "Do you want to attempt to install accelerometer calibration data, reboot, and re-test compass?")
if [ CALIBRATE_ACCEL == '1' ]
 then
echo "test"
else
echo "nope"
fi

}
prompt="Pick an option:"
options=("Run All" "Delete aiding files" "Reboot device" "Start GPS" "Take photos" "Compass Linearity" "Copy results" "Test func")

PS3="$prompt "
select opt in "${options[@]}" "Quit"; do 

case "$REPLY" in

1 ) run_all;;
2 ) delete_aiding;;
3 ) reboot_wait  ;;
4 ) run_gps      ;;
5 ) start_cam    ;;
6 ) run_compass    ;;
7 ) copy_data    ;;
8 ) test_func    ;;

$(( ${#options[@]}+1 )) ) echo "Goodbye!"; break;;
*) echo "Invalid option. Try another one.";continue;;

esac

done




