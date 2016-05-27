bash make_app.bash $1
adb -d uninstall com.reconinstruments.osmMapImages
adb -d install -r bin/osmMapImages-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.osmimages/com.reconinstruments.osmimages.OsmActivity"
