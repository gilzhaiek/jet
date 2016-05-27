LOCAL_PATH := $(call my-dir)

############
# ReconOSCommonWidgets library
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
            $(call all-subdir-java-files)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= com.reconinstruments.commonwidgets

include $(BUILD_STATIC_JAVA_LIBRARY)
