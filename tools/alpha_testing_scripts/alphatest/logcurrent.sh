#!/system/bin/sh
# this script runs on the target device and logs the current from the PMIC driver
# to a file once every ten seconds. these numbers are meaningless with USB connected
# as they will show positive numbers (indicating current flowing into the device)
# to run with usb disconnected, start it over adb shell using the nohup program
while true
    do
    echo $(date +%s),$(cat /sys/class/power_supply/twl6030_battery/current_now) >> /data/current.log
    sleep 10
done
