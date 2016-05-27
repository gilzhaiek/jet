ant clean
bash make_app.bash $1
adb -d uninstall com.reconinstruments.hudservice
adb -d install -r bin/HUDService-release.apk
