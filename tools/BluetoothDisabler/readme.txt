1. you can run the application to disable bluetooth or

2. you can install the application and run

$ adb shell am broadcast -a disable_bluetooth

from command line, check adb logcat for "DisableBluetoothReceiver" tag to check whether it was successfully received or not.
