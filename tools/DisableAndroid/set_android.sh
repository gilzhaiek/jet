#!/bin/bash

if [[ "$1" == *off* ]]
then
  echo -e "\033[1;32m------ Trying to Disable Android ------\033[m"
    adb remount;
    adb shell mv /system/bin/app_process{,_off};
    adb shell mv /system/bin/servicemanager{,_off};
    adb shell mv /system/bin/surfaceflinger{,_off};
    adb reboot
  exit 0
fi

if [[ "$1" == *on* ]]
then
  echo -e "\033[1;32m------ Trying to Enable Android ------\033[m"
    adb remount;
    adb shell mv /system/bin/app_process{_off,};
    adb shell mv /system/bin/servicemanager{_off,};
    adb shell mv /system/bin/surfaceflinger{_off,};
    adb reboot
  exit 0
fi

echo -e "\033[1;32m------ Error: need to provide on/off option ------\033[m"





