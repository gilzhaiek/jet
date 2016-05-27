adb -d install bin/RootMODLive-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.root/com.reconinstruments.root.RootMODLiveActivity";
sleep 5
adb -d uninstall com.reconinstruments.root
adb -d reboot;
