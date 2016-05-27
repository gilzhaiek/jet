name=RootMODLive
foldername=Dev_and_test_apps/RootMODLive
android update project -p . -n $name -t android-10
ant clean
ant release
cp bin/$name-*unsi*.apk ../../signing/limo
cd ../../signing/limo
java -jar signapk.jar platform.x509.pem platform.pk8 $name-*unsigned*.apk $name-release.apk
mv $name-release.apk ../../$foldername/bin/
rm $name*-unsigned*.apk
cd ../../$foldername
