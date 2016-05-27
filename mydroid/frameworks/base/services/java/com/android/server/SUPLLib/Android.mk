# Copyright 2008 The Android Open Source Project

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under) 

LOCAL_JAVA_RESOURCE_DIRS := .

LOCAL_JAVA_LIBRARIES := core framework

LOCAL_MODULE := android.supl

LOCAL_DX_FLAGS := --core-library

LOCAL_NO_EMMA_INSTRUMENT := true

include $(BUILD_JAVA_LIBRARY)
