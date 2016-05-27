name=JetBLEANTSwitcherTester
foldername=JetBLEANTSwitcherTester
platform=$1
if [ "$platform" = "" ]; then
platform="jet"
fi
if [ "$platform" = "limo" ]; then
android update project -p . -n $name -t android-17
else
android update project -p . -n $name -t android-17
fi

ant clean
ant release
cp bin/$name-*unsi*.apk ~/recon/limo_apps/signing/$platform/
cd ~/recon/limo_apps/signing/$platform/
java -jar signapk.jar platform.x509.pem platform.pk8 $name-*unsi*.apk $name-release.apk
mv $name-release.apk $OLDPWD/bin/
rm $name-*unsi*.apk
cd $OLDPWD

adb install -r bin/JetBLEANTSwitcherTester-release.apk

adb shell am start -n com.reconinstruments.jetbleantswitchertester/.MainActivity