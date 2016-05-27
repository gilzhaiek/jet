bash make_app.bash
adb -d uninstall com.reconinstruments.interdevice
adb -d install -r bin/SendRemoteIntent-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.interdevice/com.reconinstruments.interdevice.SendRemoteIntentActivity"
