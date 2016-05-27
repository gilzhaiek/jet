LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under,.)
LOCAL_REQUIRED_MODULES := \
    com.reconinstruments.mfi \
    com.stonestreetone.bluetopiapm

LOCAL_JAVA_LIBRARIES := \
    com.reconinstruments.mfi \
    com.stonestreetone.bluetopiapm \
    core \
    framework

LOCAL_PACKAGE_NAME := MFiService
LOCAL_SDK_VERSION := current
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_CERTIFICATE := platform
include $(BUILD_PACKAGE)


#    com.reconinstruments.mfi \
