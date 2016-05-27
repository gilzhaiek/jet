LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under,src)
LOCAL_REQUIRED_MODULES := \
	com.reconinstruments.os.analyticsagent \
	com.reconinstruments.os \
	com.android.server \
	net.lingala.zip4j
LOCAL_JAVA_LIBRARIES := \
	com.reconinstruments.os.analyticsagent \
	com.reconinstruments.os \
	net.lingala.zip4j \
	core \
	framework \
	services
LOCAL_PACKAGE_NAME := AnalyticsService
LOCAL_SDK_VERSION := current
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_CERTIFICATE := platform
include $(BUILD_PACKAGE)

