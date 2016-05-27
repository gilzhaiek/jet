LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    sensornative.c

LOCAL_MODULE:= libreconsensor

LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libandroid_runtime \
    libnativehelper \
    libbinder \
    libdl

# No static libraries.
LOCAL_STATIC_LIBRARIES :=

LOCAL_C_INCLUDES := $(TOP)/device/ti/jet/libreconsensor/include
LOCAL_C_INCLUDES += $(TOP)/device/ti/jet/libsensors

LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE)

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

