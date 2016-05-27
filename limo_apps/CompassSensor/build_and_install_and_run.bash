bash make_app.bash $1
adb -d uninstall com.reconinstruments.compass
adb -d install bin/CompassCalibration-release.apk

