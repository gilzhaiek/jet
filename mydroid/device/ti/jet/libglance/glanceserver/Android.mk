LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	main_glanceserver.cpp

LOCAL_SHARED_LIBRARIES := \
	libglanceservice \
	libutils \
	libbinder

LOCAL_C_INCLUDES := \
	$(TOP)/device/ti/jet/libglance/libglanceservice \
	$(TOP)/device/ti/jet/libglance/include \
	$(TOP)/device/ti/jet/libglance/hal

LOCAL_MODULE := glanceserver

LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)
