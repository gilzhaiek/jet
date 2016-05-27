#!/bin/sh
echo copying senstest and i2c tools
adb push apps/senstest /sbin/
adb push apps/i2cget /sbin
adb push apps/i2cset /sbin
adb push apps/i2cdump /sbin

adb -d shell chmod 766 /sbin/senstest
adb -d shell chmod 766 /sbin/i2cget
adb -d shell chmod 766 /sbin/i2cset
adb -d shell chmod 766 /sbin/i2cdump
