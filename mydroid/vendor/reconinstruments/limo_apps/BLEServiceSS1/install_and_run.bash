name=BLEServiceSS1
foldername=BLEServiceSS1
platform=jet

cp bin/$name.apk ../signing/$platform/
cd ../signing/$platform/
java -jar signapk.jar platform.x509.pem platform.pk8 $name.apk $name-release.apk
mv $name-release.apk ../../$foldername/bin/
rm $name.apk
cd ../../$foldername

adb install -r bin/$name-release.apk

adb shell am startservice -a "RECON_THE_BLE_SERVICE"
