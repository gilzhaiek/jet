ant release
adb -d uninstall com.reconinstruments.maps
adb -d install -r bin/StaticMapDemo2-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.maps/com.reconinstruments.maps.StaticMapDemo2Activity"
