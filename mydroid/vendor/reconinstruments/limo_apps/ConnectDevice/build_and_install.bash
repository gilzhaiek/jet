bash make_app.bash
adb -d uninstall com.reconinstruments.connectdevice
adb -d install -r bin/ConnectDevice-release.apk