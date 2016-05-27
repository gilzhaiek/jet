LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := AndroidMAPM_MCE

LOCAL_MODULE_TAGS := optional tests

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := com.stonestreetone.bluetopiapm

#Add all util lib source files
LOCAL_SRC_FILES += $(patsubst %.java,../%.java,$(UTIL_JAVA_SRC))

#Include all util lib resources
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res $(UTIL_RES_DIR)

#Since we are including resouces for two packages (This app and the util lib),
#we need to tell aapt to generate an R.java for the util package as well

AAPT_PACKAGE := $(LOCAL_JAVA_LIBRARIES).sample.util

ifeq ($(PKG_FLAG_AVAILABLE), true)

#If the sdk version is >= 14 then the extra packages flag is part of the app packaging tool.
#This flag directs AAPT to add the resources of the extra package to the build.
LOCAL_AAPT_FLAGS   := --extra-packages $(AAPT_PACKAGE) --auto-add-overlay

else

#if the sdk version is < 14 then AAPT needs to be launched here to add the resource file to the build.
LOCAL_AAPT_FLAGS   := --auto-add-overlay

#Get the appropriate intermediate directory and then the full path to the resource file.
AAPT_R_DIR  := $(call intermediates-dir-for, APPS, $(LOCAL_PACKAGE_NAME),, COMMON)/src
R_JAVA_UTIL := $(AAPT_R_DIR)/$(subst .,/,$(AAPT_PACKAGE))/R.java

#Make the sample package build dependent on the extra resource file.
$(LOCAL_PACKAGE_NAME): $(R_JAVA_UTIL)

#Make the out directory and call aapt package to create the resource file.
$(R_JAVA_UTIL): PRIVATE_AAPT_R_DIR := $(AAPT_R_DIR)

$(R_JAVA_UTIL): $(UTIL_RESOURCES) $(AAPT_MANIFEST) $(AAPT_INCLUDE)
	@mkdir -p $(@D)
	$(AAPT)                                $(AAPT_FLAGS)      \
		$(addprefix -S ,               $(AAPT_RESOURCES)) \
		$(addprefix -M ,               $(AAPT_MANIFEST))  \
		$(addprefix -I ,               $(AAPT_INCLUDE))   \
		$(addprefix --custom-package , $(AAPT_PACKAGE))   \
		$(addprefix -J ,               $(PRIVATE_AAPT_R_DIR))

endif

include $(BUILD_PACKAGE)
