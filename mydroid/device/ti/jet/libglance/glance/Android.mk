LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	Glance.cpp \
	IGlanceClient.cpp \
	IGlanceService.cpp

LOCAL_SHARED_LIBRARIES := \
	libbinder \
	libutils

LOCAL_C_INCLUDES := $(TOP)/device/ti/jet/libglance/include

LOCAL_MODULE := libglanceclient

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
