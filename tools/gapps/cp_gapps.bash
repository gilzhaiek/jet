adb remount
adb shell rm /system/vendor/bin/pvrsrvctl_SGX544_112
adb shell rm /system/vendor/lib/libglslcompiler_SGX544_112.so
adb shell rm /system/vendor/lib/libPVRScopeServices_SGX544_112.so
adb shell rm /system/vendor/lib/libsrv_um_SGX544_112.so
adb shell rm /system/vendor/lib/egl/libGLESv1_CM_POWERVR_SGX544_112.so
adb shell rm /system/vendor/lib/egl/libGLESv2_POWERVR_SGX544_112.so
adb shell rm /system/vendor/lib/egl/libEGL_POWERVR_SGX544_112.so
adb shell rm /system/vendor/lib/libpvr2d_SGX544_112.so
adb shell rm /system/vendor/lib/libpvrANDROID_WSEGL_SGX544_112.so
adb shell rm /system/vendor/lib/libsrv_init_SGX544_112.so
adb shell rm /system/vendor/lib/libIMGegl_SGX544_112.so
adb shell rm /system/vendor/lib/libusc_SGX544_112.so
adb shell rm /system/lib/modules/pvrsrvkm_sgx544_112.ko

adb push system/addon.d /system/
adb push system/usr/srec /system/usr/
adb push system/app/ChromeBookmarksSyncAdapter.apk /system/app/.
adb push system/app/GenieWidget.apk /system/app/.
adb push system/app/GoogleBackupTransport.apk /system/app/.
adb push system/app/GoogleCalendarSyncAdapter.apk /system/app/.
adb push system/app/GoogleContactsSyncAdapter.apk /system/app/.
adb push system/app/GoogleEars.apk /system/app/.
adb push system/app/GoogleFeedback.apk /system/app/.
adb push system/app/GoogleLoginService.apk /system/app/.
adb push system/app/GooglePartnerSetup.apk /system/app/.
adb push system/app/GoogleServicesFramework.apk /system/app/.
adb push system/app/GoogleTTS.apk /system/app/.
adb shell chmod 644 /system/app/ChromeBookmarksSyncAdapter.apk
adb shell chmod 644 /system/app/GenieWidget.apk
adb shell chmod 644 /system/app/GoogleBackupTransport.apk
adb shell chmod 644 /system/app/GoogleCalendarSyncAdapter.apk
adb shell chmod 644 /system/app/GoogleContactsSyncAdapter.apk
adb shell chmod 644 /system/app/GoogleEars.apk
adb shell chmod 644 /system/app/GoogleFeedback.apk
adb shell chmod 644 /system/app/GoogleLoginService.apk
adb shell chmod 644 /system/app/GooglePartnerSetup.apk
adb shell chmod 644 /system/app/GoogleServicesFramework.apk
adb shell chmod 644 /system/app/GoogleTTS.apk

adb push system/app/MediaUploader.apk /system/app/
adb push system/app/Microbes.apk /system/app/
adb push system/app/NetworkLocation.apk /system/app/
adb push system/app/OneTimeInitializer.apk /system/app/
adb push system/app/Phonesky.apk /system/app/
adb push system/app/SetupWizard.apk /system/app/
adb push system/app/Thinkfree.apk /system/app/
adb push system/framework/com.google.android.maps.jar /system/framework/
adb push system/framework/com.google.android.media.effects.jar /system/framework/
adb push system/framework/com.google.widevine.software.drm.jar /system/framework/
adb push system/vendor/lib/libPVRScopeServices.so /system/vendor/lib/
adb push system/app/Talk.apk /system/app/
adb push system/app/Talkback.apk   /system/app/
adb push system/app/VoiceSearchStub.apk /system/app/
adb shell chmod 644 /system/app/MediaUploader.apk
adb shell chmod 644 /system/app/Microbes.apk
adb shell chmod 644 /system/app/NetworkLocation.apk
adb shell chmod 644 /system/app/OneTimeInitializer.apk
adb shell chmod 644 /system/app/Phonesky.apk
adb shell chmod 644 /system/app/SetupWizard.apk
adb shell chmod 644 /system/app/Thinkfree.apk
adb shell chmod 644 /system/framework/com.google.android.maps.jar
adb shell chmod 644 /system/framework/com.google.android.media.effects.jar
adb shell chmod 644 /system/framework/com.google.widevine.software.drm.jar
adb shell chmod 644 /system/vendor/lib/libPVRScopeServices.so
adb shell chmod 644 /system/app/Talk.apk
adb shell chmod 644 /system/app/Talkback.apk
adb shell chmod 644 /system/app/VoiceSearchStub.apk
