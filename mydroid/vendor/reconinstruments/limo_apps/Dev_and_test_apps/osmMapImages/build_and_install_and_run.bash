ant release
adb -d uninstall com.reconinstruments.osmimages
adb -d install -r bin/osmMapImages-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.osmimages/com.reconinstruments.osmimages.OsmActivity"
