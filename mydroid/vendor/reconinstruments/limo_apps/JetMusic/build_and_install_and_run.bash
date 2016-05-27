bash make_app.bash $1
adb -d uninstall com.reconinstruments.jetmusic
adb -d install -r bin/JetMusic-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.jetmusic/com.reconinstruments.jetmusic.MainActivity"
