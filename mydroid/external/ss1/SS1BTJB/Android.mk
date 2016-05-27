
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE:= libSS1BTJB

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := ARBTJBHB.cpp   \
                   ARBTJBBTAG.cpp \
                   ARBTJBBTSO.cpp \
                   ARBTJBSS.cpp   \
                   ARBTJBBTS.cpp  \
                   ARBTJBBTEL.cpp \
                   ARBTJBBTAS.cpp \
                   ARBTJBCOM.cpp

LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)              \
                    frameworks/base/include/utils \
                    $(BTPM_CLIENT_INCLUDES)       \
                    $(SS1_COMMON_INCLUDES)

LOCAL_STATIC_LIBRARIES += $(SS1_COMMON_LIBS)

LOCAL_CFLAGS += -fvisibility=hidden $(SS1_COMMON_CFLAGS)

ifeq ($(BOARD_HAVE_BLUETOOTH),true)
  LOCAL_STATIC_LIBRARIES += $(BTPM_CLIENT_LIBS)
endif

include $(BUILD_STATIC_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))


