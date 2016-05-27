#!/bin/sh
waitforadb(){
if [ $(adb devices | wc | awk '{print $1}') -le 2 ]
then 
printf "waiting for device"
while [ $(adb devices | wc | awk '{print $1}') -le 2 ]; do
    sleep 3
    printf "."
done
echo " and we're back" 
fi
}
zygon(){
echo enabling
waitforadb
adb remount
adb shell mv /system/xbin/charge_monitor_off /system/xbin/charge_monitor
echo rebooting
adb reboot
waitforadb
}
zygoff(){
waitforadb
adb remount
adb shell mv /system/xbin/charge_monitor /system/xbin/charge_monitor_off
echo rebooting
adb reboot
waitforadb
}
copytools(){
waitforadb
scripts/busybox_install.sh
scripts/all.sh
adb push scripts/runtest.sh /
adb shell chmod 755 /runtest.sh
}
install() {
    echo "Disable android and reboot device?? [y/n] "
    read yn 
    case $yn in
        [Yy]* ) zygoff; ;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
echo "done"
copyfiles
echo Installation complete
exit
}
restore() {
    echo  "Re-enable android and reboot device?? [y/n] " 
    read yn
    case $yn in
        [Yy]* ) zygon; ;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
}
copyresults(){
waitforadb
mkdir results
cd results
adb pull results
}
while :
do
echo "1) Install (disable zygote, reboot, then copy utils)"
echo "2) Install (copy utils only)"
echo "3) Uninstall (re-enable zygote and reboot)"
echo "4) Copy results back"
echo "q) Quit"
read choice
case "$choice" in
    1|ins) install ;;
    2|ins2) copytools ;;
    3|res) restore  ;;
    4|copy) copyresults  ;;
    5|quit) exit  ;;
    q|quit2) exit  ;;
esac
done
