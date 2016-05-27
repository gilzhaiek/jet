bash make_app.bash $1
adb -d uninstall com.reconinstruments.snowmap
adb -d install -r bin/SnowMapApp-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.snowmap/com.reconinstruments.snowmap.SnowMapActivity"
