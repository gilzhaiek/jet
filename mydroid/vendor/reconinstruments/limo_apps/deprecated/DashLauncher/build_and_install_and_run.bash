bash make_app.bash
adb -d uninstall com.reconinstruments.dashlauncher
adb -d install -r bin/DashLauncher-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.dashlauncher/com.reconinstruments.dashlauncher.WarningActivity"
