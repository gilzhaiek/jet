ant clean
bash make_app.bash $1
adb -d uninstall com.reconinstruments.applauncher
adb -d install -r bin/ReconAppLauncher-release.apk
adb -d shell am startservice -a "RECON_MOD_SERVICE"
