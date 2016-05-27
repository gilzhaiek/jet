bash make_app.bash $1
adb -d uninstall com.reconinstruments.quickactions
adb -d install -r bin/ReconQuickActions-release.apk
