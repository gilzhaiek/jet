name=ReconAppLauncher
foldername=ReconApplauncher
platform=$1
if [ "$platform" = "" ]; then
platform="jet"
fi
android update project -p . -n $name -t android-16
ant clean
# write version info into the file
cat ../svn_info | awk '/Revision/ {print "package com.reconinstruments.applauncher.transcend;\nclass VersionInfo {\n static final public int SVN_VERSION_NUMBER = "$2";\n}"}' > src/com/reconinstruments/applauncher/transcend/VersionInfo.java;
ant release
cp bin/$name-*unsi*.apk ../signing/$platform/
cd ../signing/$platform/
java -jar signapk.jar platform.x509.pem platform.pk8 $name-*unsi*.apk $name-release.apk
mv $name-release.apk ../../$foldername/bin/
rm $name-*unsi*.apk
cd ../../$foldername
