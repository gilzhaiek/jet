bash make_app.bash $1
adb -d uninstall com.reconinstruments.myactivities
adb -d install -r bin/ReconMyActivities-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.myactivities/com.reconinstruments.myactivities.MyActiviesActivity"
