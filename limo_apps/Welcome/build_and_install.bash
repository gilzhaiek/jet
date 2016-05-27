bash make_app.bash $1
adb -d uninstall com.reconinstruments.QuickstartGuide
adb -d install -r bin/Welcome-release.apk
