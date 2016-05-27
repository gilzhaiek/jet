bash make_app.bash $1;
adb -d uninstall com.reconinstruments.ble_ss1
adb -d install -r bin/BLE_Connect-release.apk

adb shell am startservice -a "RECON_THE_BLE_SERVICE"
