# List of modules to include in the the add-on system image
#PRODUCT_PACKAGES +=

include $(LOCAL_PATH)/../jet/common.mk

# The name of this add-on (for the SDK)
PRODUCT_SDK_ADDON_NAME := jet_sdk_addon

# Copy the following files for this add-on's SDK
PRODUCT_SDK_ADDON_COPY_FILES := \
        $(LOCAL_PATH)/manifest.ini:manifest.ini \
        $(LOCAL_PATH)/hardware.ini:hardware.ini

# Copy the jar files for the libraries (APIs) exposed in this add-on's SDK
PRODUCT_SDK_ADDON_COPY_MODULES := \
        com.reconinstruments.os:libs/com.reconinstruments.os.jar

PRODUCT_SDK_ADDON_STUB_DEFS := $(LOCAL_PATH)/jet_sdk_addon_stub_defs.txt

# Define the name of the documentation to generate for this add-on's SDK
PRODUCT_SDK_ADDON_DOC_MODULES := \
        com.reconinstruments.os_doc

# Define the host tools and libs that are parts of the SDK.
-include sdk/build/product_sdk.mk
-include development/build/product_sdk.mk

# The name of this add-on (for the build system)
# Use 'make PRODUCT-<PRODUCT_NAME>-sdk_addon' to build the an add-on,
# so in this case, we would run 'make PRODUCT-jet_sdk_addon-sdk_addon'
PRODUCT_NAME := jet_sdk_addon
PRODUCT_DEVICE := jet
PRODUCT_BRAND := Android
PRODUCT_MANUFACTURER := Recon Instruments
ifeq ($(JET_PRODUCT), sun)
        PRODUCT_MODEL := JET
else
        PRODUCT_MODEL := Snow2
endif

