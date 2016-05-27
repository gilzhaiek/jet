bash make_app.bash
adb -d uninstall com.reconinstruments.interdevicetest
adb -d install -r bin/InterDeviceTest-debug.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.interdevicetest/com.reconinstruments.interdevicetest.InterDeviceTestActivity"
