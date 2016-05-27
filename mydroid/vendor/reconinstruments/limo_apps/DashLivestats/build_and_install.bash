bash make_app.bash
adb -d uninstall com.reconinstruments.dashlivestats
adb -d install -r bin/DashLivestats-release.apk
