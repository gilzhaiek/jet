bash make_app.bash $1
adb -d uninstall com.reconinstruments.jetsensorconnect
adb -d install -r bin/JetSensorConnect-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.jetsensorconnect/com.reconinstruments.jetsensorconnect.SensorConnectActivity"
