LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CFLAGS+= $(ADDITIONAL_DEFINES)

LOCAL_MODULE_TAGS      := optional eng debug

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_MODULE := RXNetworksServicesLib

LOCAL_PRELINK_MODULE   := false
# Build a jar file.
include $(BUILD_JAVA_LIBRARY)
