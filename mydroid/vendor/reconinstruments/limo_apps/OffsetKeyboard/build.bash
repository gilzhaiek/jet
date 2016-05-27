ant clean
ant release
java -jar ../signing/signapk.jar ../signing/platform.x509.pem ../signing/platform.pk8 bin/OffsetKeyboard-release-unsigned.apk OffsetKeyboard.apk
adb install -r OffsetKeyboard.apk
#adb reboot
