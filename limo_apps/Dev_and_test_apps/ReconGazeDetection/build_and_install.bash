bash make_app.bash
adb -d uninstall com.reconinstruments.gazedetectionapp
adb -d install -r bin/ReconGazeDetection-release.apk
