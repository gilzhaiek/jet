# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# common settings for all ASR builds, exports some variables for sub-makes
#include $(ASR_MAKE_DIR)/Makefile.defs

LOCAL_SRC_FILES := \
  src/android_supl_HelperService_JNI.cpp \


LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/inc \

LOCAL_C_INCLUDES += \
  $(JNI_H_INCLUDE) \
  external/mcp/NaviLink/SUPLC/Core/include/ti_client_wrapper \
  external/mcp/MCP_Common/global_inc \
  external/mcp/MCP_Common/inc \
  external/mcp/MCP_Common/Platform/inc \
  external/mcp/MCP_Common/Platform/os/LINUX \
  external/mcp/MCP_Common/Platform/os/LINUX/common/inc \
  external/mcp/MCP_Common/Platform/os/LINUX/android_zoom3/inc \
  external/mcp/MCP_Common/tran \
  external/mcp/NaviLink/NAVC \
  external/mcp/NaviLink/NAVC/Core \
  external/mcp/NaviLink/NAVC/Lib \
  external/mcp/NaviLink/inc \
  external/mcp/NaviLink/modem \
  external/mcp/NaviLink/SUPLC/Core/include/ti_supl \
  external/mcp/NaviLink/SUPLC/Core/include/ti_client_wrapper \
  external/mcp/NaviLink/SUPLC/Core/include/ \
  external/mcp/NaviLink/platforms/modem/ \
  bionic/libc/include \
  bionic/libc/private

LOCAL_PRELINK_MODULE:=false 

#  include/javavm  \

#LOCAL_CFLAGS += \
#	/hardware/ti/omap3/gps/NaviLink/SUPLC/Core/include/ti_client_wrapper \
#	/hardware/ti/omap3/gps/MCP_Common/global_inc \

LOCAL_SHARED_LIBRARIES := \
  libutils \
  libhardware_legacy \
  libcutils \
  libandroid_runtime \
  libandroid_servers \


#LOCAL_STATIC_LIBRARIES := \
#  libzipfile \
#  libunz \

#LOCAL_WHOLE_STATIC_LIBRARIES := \
#  libESR_Shared \
#  libESR_Portable \
#  libSR_AcousticModels \
#  libSR_AcousticState \
#  libSR_Core \
#  libSR_EventLog \
#  libSR_Grammar \
#  libSR_G2P \
#  libSR_Nametag \
#  libSR_Recognizer \
#  libSR_Semproc \
#  libSR_Session \
#  libSR_Vocabulary \


LOCAL_LDLIBS += -lpthread \
		-lm \
 		-lc \
		-lstdc++ \
 		-lutils \
		-lcutils \
		-ldl \
		-lz \
		-llog \

LOCAL_MODULE := libsuplhelperservicejni
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_TAGS += debug

include $(BUILD_SHARED_LIBRARY)

