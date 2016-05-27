LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# According to newsgroup, this value is never cleared by include $(CLEAR_VARS)
# but that is what we need for fast execution. We'll include it in every Android.mk.
LOCAL_ARM_MODE := arm
LOCAL_CFLAGS += -DANDROID -W -Wall -Wno-unused-parameter $(ADDITIONAL_DEFINES)
ifeq ($(NDK_VERSION),)
NDK_VERSION = android-ndk-r7b
endif

LOCAL_SRC_FILES:= \
		src/IntApp.c

LOCAL_C_INCLUDES := \
		$(LOCAL_PATH)/../api/include \
		$(LOCAL_PATH)/../msl/include \
		$(LOCAL_PATH)/../cl/include \
		$(LOCAL_PATH)/../security/include

LOCAL_MODULE := RXN_IntApp

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_LIBRARIES := RXN_MSL 
# Standard SDK package release
LOCAL_LDFLAGS := \
		$(LOCAL_PATH)/../api/lib/armeabi-v7a-android-ndk-r8d/RXN_API.a \
		$(LOCAL_PATH)/../security/lib/armeabi-v7a-android-ndk-r8d/RXN_security.a 

chipset = ti_wl128x

# Chipset Libraries
ifeq ($(chipset), ti_wl128x)
	LOCAL_CFLAGS += -DUSE_CL
	LOCAL_STATIC_LIBRARIES += RXN_CL_WL128x
else
	#Compile without chipset support
endif
include $(BUILD_EXECUTABLE)
