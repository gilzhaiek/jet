#ifeq ($(BOARD_ANT_WIRELESS_DEVICE), true)
LOCAL_PATH:= $(call my-dir)

### Wilink ANT firmware: 
#include $(CLEAR_VARS)
#LOCAL_MODULE := BT2.12_ANT_1.17.bts
#LOCAL_MODULE_TAGS := optional
#LOCAL_MODULE_TAGS += debug
#LOCAL_MODULE_CLASS := ETC
#LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/firmware
#LOCAL_SRC_FILES := prebuilts/$(LOCAL_MODULE)
#include $(BUILD_PREBUILT)

### BUILD_PACKAGE: StonestreetOneAntService.apk
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under,src)
LOCAL_JAVA_LIBRARIES := com.stonestreetone.bluetopiapm
LOCAL_PACKAGE_NAME := StonestreetOneAntService
LOCAL_STATIC_JAVA_LIBRARIES := pre_antchiplib
LOCAL_CERTIFICATE := platform
include $(BUILD_PACKAGE)

### BUILD_PREBUILT: ANTRadioService_4.7.0.apk
include $(CLEAR_VARS)
LOCAL_MODULE := ANTRadioService
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := prebuilts/$(LOCAL_MODULE).apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := platform
include $(BUILD_PREBUILT)

### BUILD_PREBUILT: AntPlusPluginsService_3.1.0.apk
include $(CLEAR_VARS)
LOCAL_MODULE := AntPlusPluginsService
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := prebuilts/$(LOCAL_MODULE).apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := platform
include $(BUILD_PREBUILT)

### BUILD_MULTI_PREBUILT
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := pre_antchiplib:libs/antchiplib.jar
include $(BUILD_MULTI_PREBUILT)

#endif
