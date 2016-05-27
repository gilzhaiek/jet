
LOCAL_PATH := $(call my-dir)
AGPS_SOURCE := ../reconagps
include $(CLEAR_VARS)

BOARD_VENDOR_TI_GPS_HARDWARE := omap4 
BOARD_GPS_LIBRARIES:= libgps

LOCAL_MODULE := gps.omap4
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_TAGS += debug

ifneq ($(BOARD_GPS_LIBRARIES),)
  LOCAL_CFLAGS           += -DHAVE_GPS_HARDWARE
  #LOCAL_CFLAGS           += -DENABLE_GEOFENCE
  #LOCAL_CFLAGS          += -DGPS_DEBUG
  LOCAL_SHARED_LIBRARIES += $(BOARD_GPS_LIBRARIES)
  LOCAL_SHARED_LIBRARIES += libcutils
endif

LOCAL_SHARED_LIBRARIES += libmcphalgps libutils
#LOCAL_C_INCLUDES := $(GPS_VENDOR_INCLUDES)
LOCAL_C_INCLUDES += \
  $(GPS_VENDOR_INCLUDES) \
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
  hardware/ti/reconagps \
  bionic/libc/include \
  bionic/libc/private

LOCAL_SRC_FILES += \
   gpshal.cpp GpsInterfaceImpl.cpp GPSResponseHandler.cpp \
   HALGPSResponseHandler.cpp GPSDriver.cpp TIGPSDriver.cpp GPSUtilities.cpp GpsWatchdog.cpp \
   $(AGPS_SOURCE)/agpsalmanac.cpp $(AGPS_SOURCE)/yumalmanac.cpp $(AGPS_SOURCE)/agpsephemeris.cpp

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw

include $(BUILD_SHARED_LIBRARY)

