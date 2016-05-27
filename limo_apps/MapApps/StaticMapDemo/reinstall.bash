adb -d uninstall com.reconinstruments.maps
adb -d install -r bin/mapApp-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.maps/com.reconinstruments.maps.MapActivity"
