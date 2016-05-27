name=mapApp
foldername=mapApp
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
ant debug
bin/$name-*unsi*.apk ../signing/$platform/
cd ../signing/$platform/
java -jar signapk.jar platform.x509.pem platform.pk8 $name-*unsi*.apk $name-debug.apk
mv $name-debug.apk ../../$foldername/bin/
rm $name-*unsi*.apk
cd ../../$foldername
