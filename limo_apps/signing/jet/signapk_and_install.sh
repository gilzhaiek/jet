java -jar signapk.jar platform.x509.pem platform.pk8 ${1} signed_${1}
adb install -r signed_${1}


