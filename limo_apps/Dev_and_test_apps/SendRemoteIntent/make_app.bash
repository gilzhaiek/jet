name=SendRemoteIntent
foldername=SendRemoteIntent
android update project -p . -n $name -t android-10
ant clean
ant release
cp bin/$name-*unsi*.apk ../signing/
cd ../signing
java -jar signapk.jar platform.x509.pem platform.pk8 $name-*unsi*.apk $name-release.apk
mv $name-release.apk ../$foldername/bin/
rm $name-*unsi*.apk
cd ../$foldername
