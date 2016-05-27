bash make_app.bash
adb -d uninstall com.reconinstruments.nativetest
adb -d install -r bin/ReconBLE-release.apk