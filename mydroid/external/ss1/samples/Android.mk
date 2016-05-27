LOCAL_PATH := $(call my-dir)

UTIL_PATH := $(LOCAL_PATH)/SS1SampleUtils

UTIL_JAVA_SRC := $(call all-java-files-under, SS1SampleUtils/src)

UTIL_RES_DIR := $(wildcard $(UTIL_PATH)/res)

#Beginning with sdk version 14 the app packaging tool includes an extra-package flag that makes
#resources from other projects available to a package build. To include the extra resource file
#for builds before version 14 the aapt utility must be explicitly launched from the makefile.
#The sdk version is tested here. If it is < 14 then variables that will be used in the aapt call
# are set.
VERSION_WITH_PKG_FLAG := 14
TEST_SDK_VERSION      := echo $(PLATFORM_SDK_VERSION) | awk '{print ($$1 >= $(VERSION_WITH_PKG_FLAG)) ? "true" : "false" ; exit 0}'
PKG_FLAG_AVAILABLE    := $(shell $(TEST_SDK_VERSION))

ifeq ($(PKG_FLAG_AVAILABLE), false)

AAPT           := $(ANDROID_BUILD_TOP)/$(HISTORICAL_SDK_VERSIONS_ROOT)/tools/linux/aapt
AAPT_FLAGS     := package -v -f -m --auto-add-overlay
AAPT_RESOURCES := $(UTIL_RES_DIR)
AAPT_MANIFEST  := $(UTIL_PATH)/AndroidManifest.xml
AAPT_INCLUDE   := $(ANDROID_BUILD_TOP)/$(HISTORICAL_SDK_VERSIONS_ROOT)/$(PLATFORM_SDK_VERSION)/android.jar

FIND_FORMAT    := "$(ANDROID_BUILD_TOP)/%p "
UTIL_RESOURCES := $(shell find $(AAPT_RESOURCES) -type f -name \* -printf $(FIND_FORMAT))

endif

include $(call all-subdir-makefiles)
