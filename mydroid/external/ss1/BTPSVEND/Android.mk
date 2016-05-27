LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(SS1_BTPSVEND_VARIANT),)
  BTPSVEND_SRC := BTPSVEND.c
else
  BTPSVEND_SRC := BTPSVEND_$(SS1_BTPSVEND_VARIANT).c
endif
ifeq ($(wildcard $(LOCAL_PATH)/$(BTPSVEND_SRC)),)
  $(error The selected libBTPSVEND variant "$(SS1_BTPSVEND_VARIANT)" does not exist @ $(LOCAL_PATH).)
endif

ifneq ($(wildcard $(LOCAL_PATH)/power_$(TARGET_BOOTLOADER_BOARD_NAME).c),)
  POWER_SRC := power_$(TARGET_BOOTLOADER_BOARD_NAME).c
  $(info Using platform-specific power management routines ($(POWER_SRC)).)
else
  POWER_SRC := power_default.c
  $(info Using generic power management routines ($(POWER_SRC)).)
endif

LOCAL_SRC_FILES += $(BTPSVEND_SRC) \
                   $(POWER_SRC)

LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)              \
                    frameworks/base/include/utils \
                    $(BTPM_SERVER_INCLUDES)       \
                    $(SS1_COMMON_INCLUDES)

LOCAL_CFLAGS += -fvisibility=hidden $(SS1_COMMON_CFLAGS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_LIBRARIES := libSS1UTIL

LOCAL_MODULE := libBTPSVEND

include $(BUILD_STATIC_LIBRARY)

