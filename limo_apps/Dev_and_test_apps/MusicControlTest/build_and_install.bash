bash make_app.bash
adb -d uninstall com.reconinstruments.musiccontrol
adb -d install -r bin/MusicControlTest-release.apk