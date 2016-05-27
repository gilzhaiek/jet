bash make_app.bash $1
adb -d uninstall com.reconinstruments.symptomchecker
adb -d install -r bin/SymptomChecker-release.apk
adb shell am start -S com.reconinstruments.symptomchecker/com.reconinstruments.symptomchecker.MainActivity
