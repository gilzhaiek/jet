bash make_app.bash
adb -d uninstall com.reconinstruments.dashnotificatoin
adb -d install -r bin/DashNotification-release.apk
