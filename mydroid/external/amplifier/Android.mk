LOCAL_PATH:= $(call my-dir)

################################################################################
# amplifier stand alone test
################################################################################
include $(CLEAR_VARS)

LOCAL_C_INCLUDES:= external/amplifier/Tfa98xxAPI/inc \
					external/tinyalsa/include
LOCAL_SRC_FILES:= amplifier_test.c NXP_I2C.c i2s_clock.c\
				Tfa98xxAPI/src/Tfa98xx.c \
				Tfa98xxAPI/src/initTfa9887.c
LOCAL_MODULE := amplifier_test
LOCAL_SHARED_LIBRARIES:= libcutils libutils libtinyalsa
LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false

include $(BUILD_EXECUTABLE)

################################################################################
# amplifier shared library
################################################################################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES:= external/amplifier/Tfa98xxAPI/inc \
					external/tinyalsa/include
LOCAL_SRC_FILES:= amplifier_init.c NXP_I2C.c i2s_clock.c\
				Tfa98xxAPI/src/Tfa98xx.c \
				Tfa98xxAPI/src/initTfa9887.c
LOCAL_MODULE := libamplifier
LOCAL_SHARED_LIBRARIES:= libcutils libutils libtinyalsa liblog
LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
################################################################################
# amplifier
################################################################################
include $(CLEAR_VARS)
LOCAL_C_INCLUDES := external/amplifier/
LOCAL_SRC_FILES := amplifier.c
LOCAL_MODULE := amplifier
LOCAL_SHARED_LIBRARIES := libamplifier
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)

################################################################################
# Install NXP config files
###############################################################################
AM_CONFIG :=external/amplifier/Config

ifneq ($(AM_CONFIG),)
include $(CLEAR_VARS)

CONFIG_COPY_PATH := $(PRODUCT_OUT)/system/etc/amplifier
LOCAL_MODULE := NXP-config
LOCAL_MODULE_CLASS := FAKE
LOCAL_MODULE_TAGS := optional

include $(BUILD_SYSTEM)/base_rules.mk

$(LOCAL_BUILT_MODULE) : $(AM_CONFIG)
	$(hide) echo "Copying amplifier config files .to $(CONFIG_COPY_PATH)"

$(LOCAL_INSTALLED_MODULE) : $(LOCAL_BUILT_MODULE) | $(ACP)
	@mkdir -p $(CONFIG_COPY_PATH)
	$(hide) $(ACP) -rvf $(AM_CONFIG)/Jet.* $(CONFIG_COPY_PATH)/
	$(hide) $(ACP) -rvf $(AM_CONFIG)/coldboot.patch $(CONFIG_COPY_PATH)/
	$(hide) $(ACP) -rvf $(AM_CONFIG)/TFA9887_N1D2_4_1_1.patch $(CONFIG_COPY_PATH)/

endif