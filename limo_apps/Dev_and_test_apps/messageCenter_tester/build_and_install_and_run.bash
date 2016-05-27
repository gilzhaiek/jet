make_app.bash
adb -d install -r bin/MessageCenterTester-debug.apk
adb -d shell am start -a "message_center_tester"
