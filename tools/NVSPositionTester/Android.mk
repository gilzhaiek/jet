LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	NVSPositionTester.c

LOCAL_SHARED_LIBRARIES := \

LOCAL_C_INCLUDES := \

LOCAL_MODULE := NVSPositionTester

LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)
