LOCAL_PATH:= $(call my-dir)

include ndk/build/gmsl/gmsl

ifeq ($(BOARD_HAVE_BLUETOOTH),true)

include $(CLEAR_VARS)

LOCAL_SRC_FILES += SS1A2DP.c

LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)              \
                    frameworks/base/include/utils \
                    $(BTPM_CLIENT_INCLUDES)       \
                    $(SS1_COMMON_INCLUDES)

LOCAL_SHARED_LIBRARIES += libcutils

# Some variants of Android 4.0+ still use the old liba2dp interface. For these platforms,
# the HAL interface will not be built, but libpower is still needed.
ifneq ($(and $(call gte,$(PLATFORM_SDK_VERSION),14),$(wildcard system/bluetooth/legacy.mk)),)
   LOCAL_SHARED_LIBRARIES += libpower
endif

LOCAL_STATIC_LIBRARIES += $(SS1_COMMON_LIBS)  \
                          $(BTPM_CLIENT_LIBS)

LOCAL_CFLAGS += -fvisibility=hidden $(SS1_COMMON_CFLAGS)

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libSS1A2DP

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

ifeq ($(call gte,$(PLATFORM_SDK_VERSION),14),$(true))

   # Some variants of Android 4.0+ still use the old liba2dp interface. Only
   # build the new HAL interface if this not one of those variants.
   ifeq ($(wildcard system/bluetooth/legacy.mk),)

      # Building for normal Android 4.0+ using the new HAL
      LOCAL_MODULE := audio.a2dp.$(TARGET_BOOTLOADER_BOARD_NAME)
      LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw

      LOCAL_PRELINK_MODULE := false
      LOCAL_MODULE_TAGS := optional

      LOCAL_SHARED_LIBRARIES += libSS1A2DP \
                                libcutils  \
                                libpower

      ifeq ($(call gte,$(PLATFORM_SDK_VERSION),17),$(true))
         # Building for Android 4.2.x or newer
         LOCAL_SRC_FILES := android_audio_hw_wrapper_4.2.c
         LOCAL_SHARED_LIBRARIES := libdl libaudiohw41
      else ifeq ($(call gte,$(PLATFORM_SDK_VERSION),16),$(true))
         # Building for Android 4.1.x or newer
         LOCAL_SRC_FILES := android_audio_hw_4.1.c
      else
         # Building for Android 4.0.x
         LOCAL_SRC_FILES := android_audio_hw_4.0.c
      endif

      include $(BUILD_SHARED_LIBRARY)

      ifeq ($(call gte,$(PLATFORM_SDK_VERSION),16),$(true))
         # Building for Android 4.2.x or newer.

         # Android 4.2 does not have an Audio HAL implementation which wraps
         # the liba2dp API from Android 4.1 and earlier.  Instead, build the
         # HAL wrapper from Android 4.1 and use a dummy HAL library (defined
         # above) which overrides the hardware module info so Android will
         # accept it.  This works because all the new features in the Android
         # 4.2 HAL interface are optional.
         include $(CLEAR_VARS)

         LOCAL_MODULE := libaudiohw41

         LOCAL_PRELINK_MODULE := false
         LOCAL_MODULE_TAGS := optional

         LOCAL_SHARED_LIBRARIES += \
            libSS1A2DP \
            libcutils \
            libpower

         LOCAL_SRC_FILES := android_audio_hw_4.1.c

         include $(BUILD_SHARED_LIBRARY)
      endif
   endif
endif

endif # BOARD_HAVE_BLUETOOTH
