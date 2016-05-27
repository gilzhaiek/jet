bash make_app.bash
adb -d uninstall com.reconinstruments.motiondetectiontest
adb -d install -r bin/MotionDetectionTest-release.apk
