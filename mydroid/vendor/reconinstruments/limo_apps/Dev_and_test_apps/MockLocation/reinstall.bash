adb -d uninstall com.reconinstruments.mocklocation
adb -d install -r bin/MockLocationActivity-debug.apk
adb -d shell "am startservice -a RECON_MOCK_LOCATION_SERVICE"

if [ $1 != "" ]; then
adb -d uninstall com.reconinstruments.mocklocationclient
adb -d install -r ../MockLocationClient/bin/MockLocationClient-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.mocklocationclient/com.reconinstruments.mocklocationclient.MockLocationClientActivity"
fi
