
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw

LOCAL_SHARED_LIBRARIES := liblog libcutils libdl libutils 

LOCAL_SRC_FILES := risensors.cpp risensors_conf.cpp rieventreader.cpp

LOCAL_MODULE := sensors.jet

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SHARED_LIBRARIES := liblog libcutils

LOCAL_MODULE    := senstest
LOCAL_SRC_FILES := senstest.c

LOCAL_MODULE_TAGS := tests

include $(BUILD_EXECUTABLE)

