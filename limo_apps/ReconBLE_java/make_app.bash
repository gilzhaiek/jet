android update project -p . -t android-10 -n ReconBLE
ant clean
ant debug
ndk-build
ant debug
mv bin/ReconBLE-debug.apk bin/ReconBLE-release.apk