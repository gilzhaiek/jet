name=ReconCameraTest
android update test-project -p . -m ../
ant clean

ant release
cp bin/tests-release-unsigned.apk ~/recon/limo_apps/signing/jet/
cd ~/recon/limo_apps/signing/jet/
java -jar signapk.jar platform.x509.pem platform.pk8 tests-release-unsigned.apk ReconCameraTest-release.apk
mv ReconCameraTest-release.apk $OLDPWD/
rm tests-release-unsigned.apk
cd -
