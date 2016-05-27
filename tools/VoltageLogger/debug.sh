#!/bin/sh
android update project -p . -n VoltageLogger -t android-16
ant clean
ant debug
adb install -r bin/VoltageLogger-debug.apk 
