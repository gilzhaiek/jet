bash make_app.bash $1
adb -d uninstall com.reconinstruments.dashwarning
adb -d install -r bin/DashWarning-release.apk
adb -d shell "am start -n com.reconinstruments.dashwarning/.WarningActivity"
