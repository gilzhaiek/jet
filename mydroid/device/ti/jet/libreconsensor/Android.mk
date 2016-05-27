LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE:= com.reconinstruments.reconsensor

include $(BUILD_JAVA_LIBRARY)

# put the classes.jar, with full class files instead of classes.dex inside, into the dist directory
$(call dist-for-goals, droidcore, $(full_classes_jar):com.reconinstruments.reconsensor.jar)

# ============================================================
include $(CLEAR_VARS)

include $(wildcard $(LOCAL_PATH)/*/Android.mk)
