adb -d uninstall com.reconinstruments.mapImages
adb -d install -r bin/mapImageGeneratorApp-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.mapImages/com.reconinstruments.mapImages.MapActivity"
