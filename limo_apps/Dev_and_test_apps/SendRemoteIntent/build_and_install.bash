bash make_app.bash
adb -d uninstall com.reconinstruments.interdevice
adb -d install -r bin/SendRemoteIntent-release.apk
