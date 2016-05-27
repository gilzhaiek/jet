SS1_LOCAL_DIR := $(strip $(eval md_file_ := $$(lastword $$(MAKEFILE_LIST))) $(patsubst %/,%,$(dir $(md_file_))))

# Name of the add-on. Only set this if it wasn't already set.
ifeq ($(strip $(patsubst @inherit:%,,$(PRODUCT_SDK_ADDON_NAME))),)
    PRODUCT_SDK_ADDON_NAME := StonestreetOne_BluetopiaPM
endif

# Copy the manifest and hardware files for the SDK add-on.
# The content of those files is manually created for now.
# Only do this if these were not already set. Normally, these
# are defined by the product defintion, not by individual SDK
# components.
ifeq ($(strip $(patsubst @inherit:%,,$(PRODUCT_SDK_ADDON_COPY_FILES))),)
PRODUCT_SDK_ADDON_COPY_FILES += \
    $(SS1_LOCAL_DIR)/../sdk_addon/manifest.ini:manifest.ini \
    $(SS1_LOCAL_DIR)/../sdk_addon/hardware.ini:hardware.ini
endif

# Add this to PRODUCT_SDK_ADDON_COPY_FILES to copy the files for an
# emulator skin (or for samples)
#$(call find-copy-subdir-files,*,device/sample/skins/WVGAMedDpi,skins/WVGAMedDpi)

# Copy the jar files for the optional libraries that are exposed as APIs.
PRODUCT_SDK_ADDON_COPY_MODULES += \
    com.stonestreetone.bluetopiapm:libs/com.stonestreetone.bluetopiapm.jar

ifeq ($(strip $(PRODUCT_SDK_ADDON_STUB_DEFS)),)
    PRODUCT_SDK_ADDON_STUB_DEFS += $(SS1_LOCAL_DIR)/../sdk_addon/stub.def
else
    # The Android build system doesn't account for multiple stub definitions -
    # mkstubs.jar expects each definition file to be prefixed with '@' on the
    # command line, but Android only prefixes a single '@' to the entire
    # variable contents. As such, when PRODUCT_SDK_ADDON_STUB_DEFS already
    # contains some stub definitions, we need to include the '@' manually.
    PRODUCT_SDK_ADDON_STUB_DEFS += @$(SS1_LOCAL_DIR)/../sdk_addon/stub.def
endif

# Name of the doc to generate and put in the add-on. This must match the name defined
# in the optional library with the tag
#    LOCAL_MODULE:= platform_library
# in the documentation section.
PRODUCT_SDK_ADDON_DOC_MODULE += BluetopiaPM

#ifeq ($(PRODUCT_SDK_ADDON_NAME),StonestreetOne_BluetopiaPM)
# This add-on extends the default sdk product.
#$(call inherit-product, $(SRC_TARGET_DIR)/product/sdk.mk)
#endif

## Real name of the add-on. This is the name used to build the add-on.
## Use 'make PRODUCT-<PRODUCT_NAME>-sdk_addon' to build the add-on.
#ifeq ($(strip $(PRODUCT_NAME)),)
#    PRODUCT_NAME:=BluetopiaPM_Addon
#endif
#
#ifeq ($(strip $(PRODUCT_BRAND)),)
#    PRODUCT_BRAND := Stonestreet One, LLC
#endif

SS1_LOCAL_PATH :=

