#!/bin/bash
set -e
function zygon ()
{ 
    adb remount;
    adb shell mv /system/bin/app_process{_off,};
    adb shell mv /system/bin/servicemanager{_off,};
    adb shell mv /system/bin/surfaceflinger{_off,};
    adb reboot;
}
function zygoff () 
{ 
    adb remount;
    adb shell mv /system/bin/app_process{,_off};
    adb shell mv /system/bin/servicemanager{,_off};
    adb shell mv /system/bin/surfaceflinger{,_off};
    adb reboot;
}
if [ "$1" == "on" ] 
then
zygon
echo "Rebooting device"
exit
fi
if [ "$1" ==  "off" ] 
then
zygoff
echo "Rebooting device"
exit
fi
printf "Usage: %s <on|off>\n" $0
