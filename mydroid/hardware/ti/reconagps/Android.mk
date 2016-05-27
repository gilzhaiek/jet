
LOCAL_PATH := $(call my-dir)
HAL_SOURCE := ../gps
include $(CLEAR_VARS)


LOCAL_MODULE := libreconagps
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_TAGS += debug

LOCAL_SHARED_LIBRARIES := liblog libcutils libdl libutils 

#LOCAL_SHARED_LIBRARIES += libmcphalgps   ### Put here SO compiled from C++ classes!!! ###
LOCAL_C_INCLUDES += \
  external/mcp/NaviLink/SUPLC/Core/include/ti_client_wrapper \
  external/mcp/MCP_Common/global_inc \
  external/mcp/MCP_Common/inc \
  external/mcp/MCP_Common/Platform/inc \
  external/mcp/MCP_Common/Platform/os/LINUX \
  external/mcp/MCP_Common/Platform/os/LINUX/common/inc \
  external/mcp/MCP_Common/Platform/os/LINUX/common/ \
  external/mcp/MCP_Common/Platform/os/LINUX/inc \
  external/mcp/MCP_Common/Platform/os/LINUX/android_zoom2/inc\
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
  bionic/libc/private \
  hardware/ti/gps \


LOCAL_SRC_FILES += \
    agpsnative.cpp agpsresponsehandler.cpp agpsalmanac.cpp yumalmanac.cpp agpsephemeris.cpp $(HAL_SOURCE)/GPSResponseHandler.cpp $(HAL_SOURCE)/GPSDriver.cpp $(HAL_SOURCE)/TIGPSDriver.cpp $(HAL_SOURCE)/GPSUtilities.cpp $(HAL_SOURCE)/GpsWatchdog.cpp

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)

include $(BUILD_SHARED_LIBRARY)

