name=JetMusic
foldername=JetMusic
platform=$1
if [ "$platform" = "" ]; then
platform="jet"
fi
android update project -p . -n $name -t android-17
ant clean
ant release
cp bin/$name-*unsi*.apk ../signing/$platform/
cd ../signing/$platform/
java -jar signapk.jar platform.x509.pem platform.pk8 $name-*unsi*.apk $name-release.apk
mv $name-release.apk ../../$foldername/bin/
rm $name-*unsi*.apk
cd ../../$foldername