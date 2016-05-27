bash make_app.bash $1
adb -d uninstall com.reconinstruments.jetapplauncher
adb -d install -r bin/JetDashSettings-release.apk
