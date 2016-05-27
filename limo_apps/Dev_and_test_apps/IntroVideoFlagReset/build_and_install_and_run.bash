bash make_app.bash
adb -d install -r bin/IntroVideoFlagReset-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.introvideoflagreset/.MainActivity"
adb uninstall com.reconinstruments.introvideoflagreset
