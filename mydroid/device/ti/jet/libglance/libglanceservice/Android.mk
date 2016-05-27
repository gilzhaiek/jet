LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= GlanceService.cpp

LOCAL_SHARED_LIBRARIES:= \
	libutils \
	libbinder \
	libglanceclient \
	libhardware

LOCAL_C_INCLUDES := \
	$(TOP)/device/ti/jet/libglance/include \
	$(TOP)/device/ti/jet/libglance/hal

LOCAL_MODULE:= libglanceservice

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
