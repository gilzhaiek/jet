LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := i2cget

LOCAL_C_INCLUDES :=        \
	i2cbusses.h, util.h, version.h, i2c-dev.h \
	$(LOCAL_PATH)/include

LOCAL_SRC_FILES := i2cget.c i2cbusses.c util.c 

include $(BUILD_EXECUTABLE)


