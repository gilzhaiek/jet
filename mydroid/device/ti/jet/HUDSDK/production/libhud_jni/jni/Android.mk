LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := \
	com_reconinstruments_lib_hardware_HUDPower.cpp \
	com_reconinstruments_lib_hardware_HUDGlance.cpp \
	com_reconinstruments_lib_hardware_HUDScreen.cpp \
	com_reconinstruments_lib_hardware_HUDAshmemNative.cpp \
	onload.cpp

LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE) \
	$(TOP)/device/ti/jet/libglance/include \

LOCAL_CFLAGS += -g -O0
LOCAL_SHARED_LIBRARIES := \
	libhardware \
	libnativehelper \
	libutils \
	libandroid_runtime \
	libglanceclient \
	libcutils

LOCAL_MODULE := libreconinstruments_jni
include $(BUILD_SHARED_LIBRARY)

