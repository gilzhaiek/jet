LOCAL_PATH := $(call my-dir)

SS1_JAVA_API_SRCS := java/com/stonestreetone/bluetopiapm/BluetoothAddress.java \
                     java/com/stonestreetone/bluetopiapm/BluetopiaPMException.java \
                     java/com/stonestreetone/bluetopiapm/ServerNotReachableException.java \
                     $(SS1_SUPPORTED_MODULES:%=java/com/stonestreetone/bluetopiapm/%.java) \
		     $(if $(filter AUDM,$(SS1_SUPPORTED_MODULES)),java/com/stonestreetone/bluetopiapm/AVRCP.java)

# Remove any files from the sources list that do not exist in the filesystem.
SS1_JAVA_API_SRCS := $(patsubst $(LOCAL_PATH)/%,%,$(wildcard $(addprefix $(LOCAL_PATH)/,$(SS1_JAVA_API_SRCS))))

# Display a warning for any sources that have been excluded due to lacking
# support in the stack.
$(foreach src,$(filter-out $(SS1_JAVA_API_SRCS),$(call all-subdir-java-files)), \
   $(warning Excluded from SS1 API: $(src)))

############
# BluetopiaPM-Java library
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(SS1_JAVA_API_SRCS)

LOCAL_MODULE_TAGS := optional

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_MODULE := com.stonestreetone.bluetopiapm

include $(BUILD_JAVA_LIBRARY)

#############
# Documentation
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(SS1_JAVA_API_SRCS) $(call all-subdir-html-files)

LOCAL_MODULE := BluetopiaPM
LOCAL_DROIDDOC_OPTIONS := com.stonestreetone.bluetopiapm
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_DROIDDOC_USE_STANDARD_DOCLET := true

include $(BUILD_DROIDDOC)


include $(CLEAR_VARS)

include $(call all-makefiles-under,$(LOCAL_PATH))
