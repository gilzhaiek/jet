######< Android.mk >##########################################################
#      Copyright 2010 - 2014 Stonestreet One.                                #
#      All Rights Reserved.                                                  #
#                                                                            #
#  Android.mk - Build script for the Stonestreet One Bluetopia Protocol      #
#               Stack Platform Manager Java API wrapper.                     #
#                                                                            #
#  Author:  Greg Hensley                                                     #
#                                                                            #
### MODIFICATION HISTORY #####################################################
#                                                                            #
#   mm/dd/yy  F. Lastname    Description of Modification                     #
#   --------  -----------    ------------------------------------------------#
#   11/20/11  G. Hensley     Initial creation.                               #
##############################################################################

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE           := libbtpmj

LOCAL_MODULE_TAGS      := optional

LOCAL_CFLAGS           := -D__ANDROID__
LOCAL_CFLAGS           += -fvisibility=hidden
LOCAL_CFLAGS           += $(SS1_COMMON_CFLAGS)

# When building inside the Android source tree, NDK_ROOT is not defined.
ifeq ($(strip $(NDK_ROOT)),)


# Include sources for each enabled module, if the source file exists.
LOCAL_SRC_FILES        := com_stonestreetone_bluetopiapm.cpp \
                          $(SS1_SUPPORTED_MODULES:%=com_stonestreetone_bluetopiapm_%.cpp) \
                          $(if $(filter AUDM,$(SS1_SUPPORTED_MODULES)),com_stonestreetone_bluetopiapm_AVRCP.cpp)

# Remove any files from the sources list that do not exist in the filesystem.
LOCAL_SRC_FILES        := $(patsubst $(LOCAL_PATH)/%,%,$(wildcard $(addprefix $(LOCAL_PATH)/,$(LOCAL_SRC_FILES))))

# Display a warning for any sources that have been excluded due to lacking
# support in the stack.
$(foreach src,$(filter-out $(LOCAL_SRC_FILES),$(notdir $(wildcard $(LOCAL_PATH)/*.cpp))), \
   $(warning Excluded from SS1 API JNI library: $(src)))

LOCAL_C_INCLUDES       := $(BTPM_CLIENT_INCLUDES) \
                          $(SS1_VENDOR_PATH)/SS1UTIL

LOCAL_STATIC_LIBRARIES := $(BTPM_CLIENT_LIBS)

LOCAL_SHARED_LIBRARIES := liblog

LOCAL_PRELINK_MODULE   := FALSE

LOCAL_CFLAGS           += -D__ANDROID_API__=$(PLATFORM_SDK_VERSION)
LOCAL_CFLAGS           += -D__ANDROID_TREE__


else # NDK_ROOT non empty


$(info libbtpmj: Building under the Android NDK)

# When building with the Android NDK, NDK_ROOT is defined to be the
# path to the root of the NDK installation directory.

BTPM_DIST ?= $(LOCAL_PATH)/../../Dist
$(info BTPM is located in $(BTPM_DIST))

LOCAL_SRC_FILES  := $(notdir $(wildcard $(LOCAL_PATH)/*.cpp))

LOCAL_C_INCLUDES := $(BTPM_DIST)/BluetopiaPM/include                           \
                    $(BTPM_DIST)/BluetopiaPM/include/client                    \
                    $(BTPM_DIST)/Bluetopia/include                             \
                    $(wildcard $(BTPM_DIST)/Bluetopia/profiles/*/include)      \
                    $(wildcard $(BTPM_DIST)/Bluetopia/profiles_gatt/*/include) \
                    $(BTPM_DIST)/Bluetopia/SBC/include                         \
                    $(BTPM_DIST)/Bluetopia/debug/include

LOCAL_LDFLAGS := -llog                                                                     \
                 -L$(BTPM_DIST)/BluetopiaPM/lib/client                                     \
                 -L$(BTPM_DIST)/BluetopiaPM/btpmmodc/client                                \
                 $(foreach x,$(wildcard $(BTPM_DIST)/BluetopiaPM/modules/*/client),-L$(x)) \
                 -L$(BTPM_DIST)/Bluetopia/SBC/lib                                          \
                 -lSS1BTPM                                                                 \
                 -lBTPMMODC                                                                \
                 -lSS1BTPM                                                                 \
                 -lSS1SBC


endif # NDK_ROOT test


include $(BUILD_SHARED_LIBRARY)
