bash make_app.bash $1
adb -d uninstall com.reconinstruments.messagecenter
adb -d install -r bin/ReconSnowMessageCenter-release.apk
adb -d shell "am start -a android.intent.action.MAIN -n com.reconinstruments.messagecenter/com.reconinstruments.messagecenter.frontend.MessageGroupViewer"
