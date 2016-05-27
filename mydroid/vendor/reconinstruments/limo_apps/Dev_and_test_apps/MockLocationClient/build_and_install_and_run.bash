bash make_app.bash $1
adb -d uninstall com.reconinstruments.mocklocationclient
adb -d install -r bin/MockLocationClient-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.mocklocationclient/com.reconinstruments.mocklocationclient.MockLocationClientActivity"

