bash make_app.bash $1
adb -d uninstall com.reconinstruments.dashmusic
adb -d install -r bin/DashMusic-release.apk
