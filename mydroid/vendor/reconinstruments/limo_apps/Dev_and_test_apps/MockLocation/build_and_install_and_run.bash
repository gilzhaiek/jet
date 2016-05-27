ant clean;
ant debug;
adb -d install -r bin/MockLocationActivity-debug.apk
adb -d shell "am startservice -a RECON_MOCK_LOCATION_SERVICE"
