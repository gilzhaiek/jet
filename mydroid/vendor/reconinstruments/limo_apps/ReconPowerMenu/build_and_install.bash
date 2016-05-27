bash make_app.bash $1
adb -d uninstall com.reconinstruments.power
adb -d install -r bin/ReconPowerMenu-release.apk
