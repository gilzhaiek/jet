bash make_app.bash $1
adb -d uninstall com.reconinstruments.camera
adb -d install -r bin/ReconCamera-release.apk
