bash make_app.bash $1
adb -d uninstall com.reconinstruments.jetconnectdevice
adb -d install -r bin/JetConnectDevice-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.jetconnectdevice/com.reconinstruments.jetconnectdevice.ConnectSmartphoneActivity"
