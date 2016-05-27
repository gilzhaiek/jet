bash make_app.bash
adb -d uninstall com.reconinstruments.test.music
adb -d install -r bin/BLEMusicControlTest-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.test.music/com.reconinstruments.test.music.MusicControlTest"