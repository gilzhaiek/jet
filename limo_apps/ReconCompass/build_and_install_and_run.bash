bash make_app.bash $1
adb -d uninstall com.reconinstruments.dashcompass
adb -d install -r bin/ReconCompass-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.dashcompass/com.reconinstruments.dashcompass.CompassActivity"
