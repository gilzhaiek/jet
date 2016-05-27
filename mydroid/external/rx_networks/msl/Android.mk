LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

# According to newsgroup, this value is never cleared by include $(CLEAR_VARS)
# but that is what we need for fast execution. We'll include it in every Android.mk.
LOCAL_ARM_MODE := arm

LOCAL_CFLAGS+= -DANDROID -W -Wall -Wno-unused-parameter

LOCAL_SRC_FILES:= \
		src/RXN_MSL_SM.c \
        src/RXN_MSL.c \
		src/RXN_MSL_Common.c \
		src/RXN_MSL_AbsCalls.c \
		src/RXN_MSL_Log.c \
		src/RXN_MSL_MsgThread.c \
		src/RXN_MSL_Time.c \
		src/RXN_MSL_PGPS.c \
		src/RXN_MSL_Rinex.c \
		src/RXN_MSL_Observer.c \
		src/platforms/RXN_MSL_Platform.c \
		src/platforms/linux/RXN_MSL_Linux.c \
		src/platforms/linux/RXN_MSL_Android.c

ifneq ($(XYBRID),)
LOCAL_SRC_FILES += src/RXN_MSL_Xybrid.c
LOCAL_SRC_FILES += src/RXN_MSL_XybridImpl.c
LOCAL_CFLAGS += -DXYBRID
endif
			
LOCAL_C_INCLUDES := \
		$(LOCAL_PATH)/include \
		$(LOCAL_PATH)/../api/include \
		$(LOCAL_PATH)/../security/include



LOCAL_MODULE := RXN_MSL

include $(BUILD_STATIC_LIBRARY)
