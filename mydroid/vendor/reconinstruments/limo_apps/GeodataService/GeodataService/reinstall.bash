adb -d uninstall com.reconinstruments.geodataservice
adb -d install -r bin/GeodataService-debug.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.geodataservice/com.reconinstruments.geodataservice.GeodataServiceActivity"

