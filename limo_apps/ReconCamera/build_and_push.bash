bash make_app.bash
adb remount
adb push bin/ReconCamera-release.apk /system/app/ReconCamera.apk
adb shell am start -n com.reconinstruments.camera/.app.CameraActivity
