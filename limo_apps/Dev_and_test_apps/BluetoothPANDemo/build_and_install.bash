bash make_app.bash
adb -d uninstall com.reconinstruments.bluetoothpandemo
adb -d install -r bin/BluetoothPANDemo-release.apk
