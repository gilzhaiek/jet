bash make_app.bash $1
adb -d uninstall com.reconinstruments.dashsettings
adb -d install -r bin/DashSettings-release.apk
