LOCAL_PATH := $(call my-dir)

# Build the library
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.reconinstruments.os.analyticsagent
LOCAL_SRC_FILES := $(call all-java-files-under,.)
LOCAL_SRC_FILES += com/reconinstruments/os/analyticsagent/IAnalyticsServiceListener.aidl
LOCAL_SRC_FILES += com/reconinstruments/os/analyticsagent/IAnalyticsService.aidl
LOCAL_AIDL_INCLUDES := $(call all-java-files-under,.)
include $(BUILD_JAVA_LIBRARY)

# Build the documentation
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-subdir-java-files) $(call all-subdir-html-files)
LOCAL_MODULE:= com.reconinstruments.os.analyticsagent_doc
LOCAL_DROIDDOC_OPTIONS := com.reconinstruments.os.analyticsagent
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_DROIDDOC_USE_STANDARD_DOCLET := true
include $(BUILD_DROIDDOC)

# Copy com.reconinstruments.os.analytics.xml to /system/etc/permissions/
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.reconinstruments.os.analyticsagent.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)
