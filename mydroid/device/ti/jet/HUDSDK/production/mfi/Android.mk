LOCAL_PATH := $(call my-dir)

# Build the library
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.reconinstruments.mfi
LOCAL_REQUIRED_MODULES := com.stonestreetone.bluetopiapm 
LOCAL_JAVA_LIBRARIES := com.stonestreetone.bluetopiapm

LOCAL_SRC_FILES := $(call all-java-files-under, com)
LOCAL_SRC_FILES += \
    com/reconinstruments/mfi/IMFiServiceListener.aidl \
    com/reconinstruments/mfi/IMFiService.aidl

include $(BUILD_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.reconinstruments.mfi.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)



