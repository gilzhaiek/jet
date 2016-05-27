bash make_app.bash $1;
adb -d install -r bin/ReconStats-release.apk
adb shell "am start -a RECON_STATS";