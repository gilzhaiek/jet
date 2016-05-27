LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

# According to newsgroup, this value is never cleared by include $(CLEAR_VARS)
# but that is what we need for fast execution. We'll include it in every Android.mk.
LOCAL_ARM_MODE := arm

LOCAL_CFLAGS += -DANDROID -W -Wall -DLINUX $(ADDITIONAL_DEFINES) -Wno-unused-parameter

ifeq ($(rxn_build), 1)
	LOCAL_CFLAGS += -DRXN_BUILD
endif

LOCAL_C_INCLUDES := \
		$(LOCAL_PATH)/include \
		$(LOCAL_PATH)/include/chipsets/ \
		$(LOCAL_PATH)/include/platforms \
		$(LOCAL_PATH)/include/platforms/linux \
		$(LOCAL_PATH)/../api/include \
		$(LOCAL_PATH)/../msl/include

ifeq ($(chipset), marvell)
	LOCAL_C_INCLUDES += $(LOCAL_PATH)/customer/marvell/Aspen/include
	LOCAL_CFLAGS += -DMARVELL_CHIPSET -DMARVELL_REVERSE_BYTE_ORDER -DLOCAL
endif

ifeq ($(chipset),)
	chipset = rinex
endif

# Common Sources
LOCAL_SRC_FILES:= \
		src/RXN_CL_Common.c \
		src/RXN_CL_Protocol.c \
		src/RXN_CL_Data.c 

ifneq ($(chipset), rinex) 
LOCAL_SRC_FILES += src/RXN_CL.c
endif

# Platform Sources
LOCAL_SRC_FILES += src/platforms/linux/RXN_CL_Linux.c

# Chipset Sources
ifeq ($(chipset), hh2)
	LOCAL_SRC_FILES += src/chipsets/RXN_CL_HH2Daemon.c
	LOCAL_MODULE := RXN_CL_HH2Daemon
else ifeq ($(chipset), rinex)
	LOCAL_SRC_FILES += src/chipsets/RXN_CL_RINEX.c
	LOCAL_SRC_FILES += src/RXN_CL_RX.c
	LOCAL_MODULE := RXN_CL_RINEX
else ifeq ($(chipset), marvell)
	LOCAL_SRC_FILES += src/chipsets/RXN_CL_MARVELL.c
	LOCAL_MODULE := RXN_CL_MARVELL
else ifeq ($(chipset), sirf)
	LOCAL_SRC_FILES += src/chipsets/RXN_CL_SIRF.c
	LOCAL_MODULE := RXN_CL_SIRF
else ifeq ($(chipset), ublox_50)
	LOCAL_SRC_FILES += src/chipsets/RXN_CL_UBLOX_50.c
	LOCAL_MODULE := RXN_CL_UBLOX_50
else ifeq ($(chipset), ti_wl128x)
	LOCAL_CFLAGS += -DUSE_MSL
	LOCAL_C_INCLUDES += $(LOCAL_PATH)/customer/ti/wl1283_socket
	LOCAL_SRC_FILES += src/chipsets/RXN_CL_WL1283_SOCKET.c
	LOCAL_MODULE := RXN_CL_WL128x
else ifeq ($(chipset), ti_wl189x)
LOCAL_C_INCLUDES       +=               \
	$(LOCAL_PATH)/customer/ti/wl189x	\
    $(LOCAL_PATH)/customer/ti/wl189x/tilcs_8.5.0.8/dproxy/include   \
    $(LOCAL_PATH)/customer/ti/wl189x/tilcs_8.5.0.8/dproxy/server    \
    $(LOCAL_PATH)/customer/ti/wl189x/tilcs_8.5.0.8/include/common   \
    $(LOCAL_PATH)/customer/ti/wl189x/tilcs_8.5.0.8/include/connect  \
    $(LOCAL_PATH)/customer/ti/wl189x/tilcs_8.5.0.8/include/dproxy   \
    $(LOCAL_PATH)/customer/ti/wl189x/tilcs_8.5.0.8/connect/include  \
    $(LOCAL_PATH)/customer/ti/wl189x/tilcs_8.5.0.8/connect/core     \
    $(LOCAL_PATH)/customer/ti/wl189x/tilcs_8.5.0.8/connect/common   \
    $(LOCAL_PATH)/customer/ti/wl189x/tilcs_8.5.0.8/connect/config   \
    $(LOCAL_PATH)/customer/ti/wl189x/tilcs_8.5.0.8/connect/debug    \
    $(LOCAL_PATH)/customer/ti/wl189x/tilcs_8.5.0.8/infrastructure/log/include
LOCAL_CFLAGS           += -DUSE_MSL -DUSE_DUMMY
LOCAL_SHARED_LIBRARIES += libcutils liblog libclientlogger
LOCAL_SRC_FILES        += customer/ti/wl189x/gnss_utils.c customer/ti/wl189x/os_services.c src/chipsets/RXN_CL_WL189x.cpp
LOCAL_PRELINK_MODULE   := false
LOCAL_MODULE           := libRXN_CL_WL189x
else ifeq ($(chipset), u8500)
	LOCAL_CFLAGS += -UGPS_CLIENT
	LOCAL_C_INCLUDES += $(LOCAL_PATH)/customer/ST_E/u8500/include
	LOCAL_SRC_FILES += src/chipsets/RXN_CL_U8500.c
	LOCAL_MODULE := RXN_CL_U8500	
else
	echo 'Please set the chipset type. Example chipset=kl.'
endif

include $(BUILD_STATIC_LIBRARY)
