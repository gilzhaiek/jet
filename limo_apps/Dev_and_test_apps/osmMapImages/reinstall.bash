adb -d uninstall com.reconinstruments.osmMapImages
adb -d uninstall com.reconinstruments.maps
adb -d install -r bin/osmMapImages-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.osmimages/com.reconinstruments.osmimages.OsmActivity"
