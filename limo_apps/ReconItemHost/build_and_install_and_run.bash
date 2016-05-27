bash make_app.bash $1
adb -d uninstall com.reconinstruments.itemhost
adb -d install -r bin/ReconItemHost-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.itemhost/com.reconinstruments.itemhost.ItemHostActivity"
