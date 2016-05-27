LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := i2cdump

LOCAL_C_INCLUDES := \
	i2cbusses.h, util.h, version.h, i2c-dev.h \
	$(LOCAL_PATH)/include

#LOCAL_CFLAGS := -fno-strict-aliasing -O0

LOCAL_SRC_FILES := i2cdump.c i2cbusses.c util.c

include $(BUILD_EXECUTABLE)


