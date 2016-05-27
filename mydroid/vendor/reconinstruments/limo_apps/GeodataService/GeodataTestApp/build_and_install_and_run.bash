bash make_app.bash $1
adb -d uninstall com.reconinstruments.GeodataTestApp
adb -d install -r bin/GeodataTestApp-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.geodatatestapp/com.reconinstruments.geodatatestapp.GeodataTestAppActivity"
