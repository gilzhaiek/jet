LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	CorrectedGyroSensor.cpp \
    Fusion.cpp \
    GravitySensor.cpp \
    LinearAccelerationSensor.cpp \
    OrientationSensor.cpp \
    RotationVectorSensor.cpp \
    SensorDevice.cpp \
    SensorFusion.cpp \
    SensorInterface.cpp \
    SensorService.cpp \

LOCAL_CFLAGS:= -DLOG_TAG=\"SensorService\"

LOCAL_C_INCLUDES := \
    bionic \
    bionic/libstdc++/include \
    external/gtest/include \
    external/stlport/stlport \

LOCAL_SHARED_LIBRARIES += \
    libstlport \
	libcutils \
	libhardware \
	libutils \
	libbinder \
	libui \
	libgui \

LOCAL_MODULE:= libsensorservice

include $(BUILD_SHARED_LIBRARY)
