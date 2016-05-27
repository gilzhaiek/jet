
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE:= libSS1UTIL

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := SS1UTIL.c

LOCAL_C_INCLUDES := frameworks/base/include/utils \
                    $(BTPM_CLIENT_INCLUDES)

LOCAL_CFLAGS += -fvisibility=hidden $(SS1_COMMON_CFLAGS)

ifeq ($(BOARD_HAVE_BLUETOOTH),true)
  LOCAL_STATIC_LIBRARIES += $(BTPM_CLIENT_LIBS)
endif

include $(BUILD_STATIC_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))


