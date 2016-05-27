1. Set OMAP_ENHANCEMENT_CPCAM := true in mydroid/device/ti/jet/BoardConfig.mk

2. Call ./build_module_sun.sh CameraOMAP to build the app

3. The TI camera app has CPCAM mode, but in order to make it work, we have to goto mydroid/hardware/ti/omap4xxx/cpcam
And push com.ti.omap.android.cpcam.xml to /etc/permissions
Push com.ti.omap.android.cpcam.jar to /system/framework/
Push libcpcam_jni.so to /system/lib/

We didn't link those changes to our OMAP_ENHANCEMENT_CPCAM change because the CPcam mode is still under investigation(it seems keep crashing in the TI camera app) and may not be very useful for our camera.
From mydroid/hardware/libhardware/include/hardware/camera.h, the CPCAM seems just add ability to modify camera buffers.
