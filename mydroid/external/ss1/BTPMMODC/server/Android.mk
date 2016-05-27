LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

# Include all source files in the current directory
LOCAL_SRC_FILES += $(subst $(LOCAL_PATH),,$(wildcard $(LOCAL_PATH)/*.c))

LOCAL_C_INCLUDES +=  $(JNI_H_INCLUDE)              \
                     frameworks/base/include/utils \
                     $(BTPM_SERVER_INCLUDES)

LOCAL_CFLAGS += -fvisibility=hidden $(SS1_COMMON_CFLAGS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libBTPMMODC_SRV

include $(BUILD_STATIC_LIBRARY)

