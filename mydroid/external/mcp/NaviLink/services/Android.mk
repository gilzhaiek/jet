
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

gpsdrv_log_src:= logm/nav_log_msg.c


gpsdrv_log_includes := \
        $(LOCAL_PATH)/../../MCP_Common/Platform/os/LINUX/inc \
        $(LOCAL_PATH)/../../MCP_Common/Platform/os/LINUX/common/inc \
        $(LOCAL_PATH)/../../MCP_Common/Platform/os/LINUX/android_zoom2/inc \
        $(LOCAL_PATH)/../../MCP_Common/inc \
        $(LOCAL_PATH)/../../MCP_Common/global_inc \
        $(LOCAL_PATH)/../../MCP_Common/Platform/inc \
        $(LOCAL_PATH)/../../MCP_Common/Platform/inc/int \
        $(LOCAL_PATH)/../../MCP_Common/Platform/os/LINUX \
        $(LOCAL_PATH)/../../MCP_Common/tran \
        $(LOCAL_PATH)/../inc \
        $(LOCAL_PATH)/../NAVC \
        $(LOCAL_PATH)/../NAVC/inc \
        $(LOCAL_PATH)/../NAVC/Core \
        $(LOCAL_PATH)/../NAVC/Lib \
        $(LOCAL_PATH)/../services/logm \
	bionic/libc/include \
        bionic/libc/private


include $(CLEAR_VARS)		
LOCAL_MODULE:=libgpsservices
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_TAGS += debug
LOCAL_PRELINK_MODULE :=false 
LOCAL_SRC_FILES := $(gpsdrv_log_src)

LOCAL_C_INCLUDES := $(gpsdrv_log_includes)

LOCAL_SHARED_LIBRARIES := libmcphalgps liblog 
LOCAL_STATIC_LIBRARIES :=
LOCAL_LDLIBS := -lpthread -lrt

include $(BUILD_SHARED_LIBRARY)

