LOCAL_PATH := $(call my-dir)

# Build the library
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.reconinstruments.os.hudremote
LOCAL_SRC_FILES := $(call all-java-files-under,.)

LOCAL_AIDL_INCLUDES := $(call all-java-files-under,.)
include $(BUILD_JAVA_LIBRARY)

# Copy com.reconinstruments.os.hudremote.xml to /system/etc/permissions/
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.reconinstruments.os.hudremote.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)
