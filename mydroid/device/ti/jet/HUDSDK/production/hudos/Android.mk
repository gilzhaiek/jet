LOCAL_PATH := $(call my-dir)

# Build the library
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.reconinstruments.os
LOCAL_REQUIRED_MODULES := com.reconinstruments.lib com.reconinstruments.mfi  com.stonestreetone.bluetopiapm 
LOCAL_JAVA_LIBRARIES := com.reconinstruments.lib com.reconinstruments.mfi  com.stonestreetone.bluetopiapm

LOCAL_SRC_FILES := $(call all-java-files-under, com)
LOCAL_SRC_FILES += \
	com/reconinstruments/os/hardware/power/IHUDPowerService.aidl \
	com/reconinstruments/os/hardware/led/IHUDLedService.aidl \
	com/reconinstruments/os/hardware/glance/IHUDGlanceService.aidl \
	com/reconinstruments/os/hardware/glance/IGlanceCalibrationListener.aidl \
	com/reconinstruments/os/hardware/glance/IGlanceDetectionListener.aidl \
	com/reconinstruments/os/hardware/screen/IHUDScreenService.aidl \
	com/reconinstruments/os/hardware/sensors/IHeadingServiceCallback.aidl \
	com/reconinstruments/os/hardware/sensors/IHUDHeadingService.aidl \
	com/reconinstruments/os/hardware/motion/IHUDActivityMotionService.aidl \
	com/reconinstruments/os/hardware/motion/IActivityMotionDetectionListener.aidl \
	com/reconinstruments/os/connectivity/IHUDConnectivityListener.aidl \
	com/reconinstruments/os/connectivity/IHUDConnectivityService.aidl

include $(BUILD_JAVA_LIBRARY)

# put the classes.jar, with full class files instead of classes.dex inside, into the dist directory
$(call dist-for-goals, droidcore, $(full_classes_jar):com.reconinstruments.os.jar)

# Build the documentation
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_MODULE:= com.reconinstruments.os_doc
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_ADDITIONAL_JAVA_DIR := $(call intermediates-dir-for,JAVA_LIBRARIES,com.reconinstruments.os,,COMMON)
LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR:=build/tools/droiddoc/templates-recon-sdk
include $(BUILD_DROIDDOC)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.reconinstruments.os.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)



