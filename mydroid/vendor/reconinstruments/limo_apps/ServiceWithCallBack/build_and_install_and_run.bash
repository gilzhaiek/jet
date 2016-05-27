bash make_app.bash $1
adb -d uninstall com.reconinstruments.heading
adb -d install -r bin/HeadingService-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.heading/com.reconinstruments.heading.HeadingActivity"