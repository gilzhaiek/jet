bash make_app.bash
adb -d uninstall com.reconinstruments.ashmemtest
adb -d install -r bin/AshmemTest-release.apk
