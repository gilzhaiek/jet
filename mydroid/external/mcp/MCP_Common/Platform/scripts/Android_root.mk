############################################################################
# Root make file for the btips on Android platform
#
# 

LOCAL_PATH:= $(call my-dir)

# This will query the sources and find the current SDK version and the target platform.
# To override this, simply export BTIPS_TARGET_PLATFORM and PLATFORM_VERSION from your shell before 
# running the compilation.
#
BTIPS_TARGET_PLATFORM := $(shell cd $(LOCAL_PATH)/MCP_Common/Platform/scripts; ./detect_android_platform.sh; cd - >/dev/null)
PLATFORM_VERSION := $(shell cd $(LOCAL_PATH)/MCP_Common/Platform/scripts; ./detect_android_sdk.sh; cd - cd - >/dev/null)

$(info ====================================================== )
$(info  Building BTIPS for $(BTIPS_TARGET_PLATFORM) with PLATFORM VERSION $(PLATFORM_VERSION))
$(info ====================================================== )

include $(call all-subdir-makefiles)
