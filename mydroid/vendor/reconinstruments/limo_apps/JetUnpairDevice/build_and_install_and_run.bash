bash make_app.bash $1
adb -d uninstall com.reconinstruments.jetunpairdevice
adb -d install -r bin/JetUnpairDevice-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.jetunpairdevice/com.reconinstruments.jetunpairdevice.PairedDevicesActivity"
