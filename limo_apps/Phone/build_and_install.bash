bash make_app.bash $1
adb -d install -r bin/ReconPhone-release.apk
adb shell am startservice -a "RECON_PHONE_RELAY_SERVICE"
