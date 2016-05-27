android update project -p .
ant release
java -jar signapk.jar platform.x509.pem platform.pk8 ./bin/*-release-unsigned.apk ./bin/GpsAssist-release.apk

adb install -r bin/GpsAssist-release.apk