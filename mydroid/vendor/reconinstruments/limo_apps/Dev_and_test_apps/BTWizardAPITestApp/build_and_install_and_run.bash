bash make_app.bash
adb -d uninstall com.reconinstruments.btwizardapitest
adb -d install -r bin/BTWizardAPITestApp-debug.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.btwizardapitest/com.reconinstruments.btwizardapitest.BTWizardAPITestActivity2"

