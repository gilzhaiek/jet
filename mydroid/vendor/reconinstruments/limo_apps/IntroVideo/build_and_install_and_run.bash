bash make_app.bash
adb -d uninstall com.reconinstruments.intro
adb -d install -r bin/IntroVideo-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.intro/com.reconinstruments.intro.startup.Screen1Activity"
