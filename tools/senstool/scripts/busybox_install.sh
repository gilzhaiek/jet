#!/bin/sh
echo copying busybox binary and install script
adb push apps/busybox-armv7l busybox
adb shell chmod 766 busybox
#adb shell mkdir .bbdir
adb shell "echo echo Creating symlinks >> bbinstall"
adb shell "echo for i in \$\(./busybox --list\) > bbinstall"
adb shell "echo do >> bbinstall"
adb shell "echo ln -s /busybox /sbin/\\\$i >> bbinstall"
adb shell "echo done >> bbinstall"
adb shell "echo echo Done >> bbinstall"
adb shell "export PATH=/.bbdir/:\$PATH"
adb shell chmod 777 bbinstall
echo inflating busybox
adb shell /bbinstall
adb shell rm /bbinstall
adb shell export PATH=.bbdir:\\\$PATH
