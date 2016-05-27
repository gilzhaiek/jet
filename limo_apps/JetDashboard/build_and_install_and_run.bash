bash make_app.bash $1
adb -d uninstall com.reconinstruments.dashboard
adb -d install -r bin/JetDashboard-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.dashboard/com.reconinstruments.dashboard.DashboardActivity"
