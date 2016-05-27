bash make_app.bash
adb -d uninstall com.reconinstruments.dashradar
adb -d install -r bin/DashRadar-release.apk
