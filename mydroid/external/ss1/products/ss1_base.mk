SS1_LOCAL_DIR := $(strip $(eval md_file_ := $$(lastword $$(MAKEFILE_LIST))) $(patsubst %/,%,$(dir $(md_file_))))

include ndk/build/gmsl/gmsl

# Build the SS1 BluetopiaPM server daemon only for Android 4.1 and older
ifeq ($(call lt,$(PLATFORM_SDK_VERSION),17),$(true))
    PRODUCT_PACKAGES += SS1BTPM
endif

# Include the SS1 Java API
PRODUCT_PACKAGES += \
    com.stonestreetone.bluetopiapm \
    libbtpmj

PRODUCT_COPY_FILES += \
    $(SS1_LOCAL_DIR)/../API/com.stonestreetone.bluetopiapm.xml:/system/etc/permissions/com.stonestreetone.bluetopiapm.xml

SS1_LOCAL_DIR :=

