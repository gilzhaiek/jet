# This file lists the firmware, software that are specific to
# WiLink connectivity chip on OMAPx platforms.

PRODUCT_PACKAGES += uim-sysfs \
    ti-wpan-fw

#copy firmware
    PRODUCT_COPY_FILES += \
       system/bluetooth/data/main.conf:system/etc/bluetooth/main.conf \
       external/bluetooth/blueti/dun/dun.conf:system/etc/bluetooth/dun.conf

