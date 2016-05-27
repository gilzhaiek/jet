#
# Copyright (C) 2011 Texas Instruments Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

ifeq ($(TARGET_PREBUILT_KERNEL),)
LOCAL_KERNEL := device/ti/jet/boot/zImage
else
LOCAL_KERNEL := $(TARGET_PREBUILT_KERNEL)
endif

#to flow down to ti-wpan-products.mk
#BLUETI_ENHANCEMENT := false

PRODUCT_COPY_FILES := \
    $(LOCAL_KERNEL):kernel \
    device/ti/jet/boot/fastboot.sh:fastboot.sh \
    device/ti/jet/boot/fastboot:fastboot \
    $(LOCAL_KERNEL):boot/zImage \
    device/ti/jet/boot/MLO_es2.2_emu:boot/MLO_es2.2_emu \
    device/ti/jet/boot/MLO_es2.2_gp:boot/MLO_es2.2_gp \
    device/ti/jet/boot/u-boot.bin:boot/u-boot.bin
    
PRODUCT_COPY_FILES +=device/ti/jet/etc/ducati-m3_B3.bin:system/vendor/firmware/ducati-m3.bin

# This is the first one, so make sure that we don't add PRODUCT_PACKAGES before it because the ":=" will reset the packages
# Recon Instruments Server / Recon OS Services
# The Manager (com.reconinstruments.os) is located in the common.mk because it is provided as an SDK addon
PRODUCT_PACKAGES := \
    HUDServer \
    HUDBluetooth \
    com.reconinstruments.lib \
    com.reconinstruments.lib.xml \
    libreconinstruments_jni \
    com.reconinstruments.os.analyticsservice \
    com.reconinstruments.os.analyticsagent.xml \
    net.lingala.zip4j.xml \
    AnalyticsService \
    com.reconinstruments.mfi \
    com.reconinstruments.mfi.xml \
    MFiService \
    com.reconinstruments.os.hudremote \
    com.reconinstruments.os.hudremote.xml

# Tools: TODO: Remove some of these before production
PRODUCT_PACKAGES += \
    i2cdump \
    i2cset \
    i2cget \
    BatteryLog

PRODUCT_PACKAGES += \
    ti_omap4_ducati_bins \
    libOMX_Core \
    libOMX.TI.DUCATI1.VIDEO.DECODER

# Tiler
PRODUCT_PACKAGES += \
    libtimemmgr

#Lib Skia test
PRODUCT_PACKAGES += \
    SkLibTiJpeg_Test

# Sun Specific
ifeq ($(JET_PRODUCT), sun)
PRODUCT_PACKAGES += \
    Camera \
    CameraOMAP4 \
    camera_test

# Amplifier for NXP Tfa9xxx
PRODUCT_PACKAGES += \
    libamplifier \
    amplifier \
    NXP-config

PRODUCT_COPY_FILES += \
    device/ti/jet/etc/climax:system/xbin/climax

endif

PRODUCT_PACKAGES += \
    boardidentity \
    libboardidentity \
    libboard_idJNI

# TODO - move this to lean base
PRODUCT_PACKAGES += \
    librs_jni \
    charger \
    charger_res_images \
    com.android.future.usb.accessory \
    com.reconinstruments.reconsensor \
    libreconsensor \
    libglanceservice \
    libglanceclient \
    glanceserver \

# RX Networks AGPS
PRODUCT_PACKAGES += \
    RXN_IntApp

PRODUCT_COPY_FILES += \
    external/rx_networks/license.key:system/etc/gps/RXN/license.key \
    external/rx_networks/security.key:system/etc/gps/RXN/security.key \
    external/rx_networks/msl/MSLConfig.txt:system/etc/gps/RXN/MSLConfig.txt
    

# Filesystem management tools
PRODUCT_PACKAGES += \
    make_ext4fs

# WI-Fi
PRODUCT_PACKAGES += \
    dhcpcd.conf \
    hostapd.conf \
    wifical.sh \
    TQS_D_1.7.ini \
    TQS_D_1.7_127x.ini \
    crda \
    regulatory.bin \
    calibrator

# Add modem scripts
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/modem-detect.sh:system/vendor/bin/modem-detect.sh
# Audio HAL module
PRODUCT_PACKAGES += audio.primary.omap4
PRODUCT_PACKAGES += audio.hdmi.omap4

# tinyalsa utils
PRODUCT_PACKAGES += \
    tinymix \
    tinyplay \
    tinycap

# BlueZ a2dp Audio HAL module
PRODUCT_PACKAGES += audio.a2dp.default

# Audioout libs
PRODUCT_PACKAGES += libaudioutils

# Lights
PRODUCT_PACKAGES += \
        lights.jet

# Sensors
PRODUCT_PACKAGES += \
        sensors.jet

# Glance
PRODUCT_PACKAGES += \
		glance.jet

# BlueZ test tools & Shared Transport user space mgr
PRODUCT_PACKAGES += \
    hciconfig \
    hcitool

# SMC components for secure services like crypto, secure storage
PRODUCT_PACKAGES += \
        smc_pa.ift \
        smc_normal_world_android_cfg.ini \
        libsmapi.so \
        libtf_crypto_sst.so \
        libtfsw_jce_provider.so \
        tfsw_jce_provider.jar \
        tfctrl

# Enable AAC 5.1 decode (decoder)
PRODUCT_PROPERTY_OVERRIDES += \
    media.aac_51_output_enabled=true

# for bugmailer
PRODUCT_PACKAGES += send_bug
PRODUCT_COPY_FILES += \
    system/extras/bugmailer/bugmailer.sh:system/bin/bugmailer.sh \
    system/extras/bugmailer/send_bug:system/bin/send_bug

# Boot animation
ifeq ($(JET_PRODUCT), sun)
PRODUCT_COPY_FILES += \
    device/ti/jet/bootanimation_jet.zip:system/media/bootanimation.zip
else
PRODUCT_COPY_FILES += \
    device/ti/jet/bootanimation_snow.zip:system/media/bootanimation.zip
endif

$(call inherit-product, hardware/ti/omap4xxx/omap4.mk)
$(call inherit-product-if-exists, hardware/ti/wpan/ti-wpan-products.mk)
$(call inherit-product-if-exists, device/ti/proprietary-open/omap4/ti-omap4-vendor.mk)
$(call inherit-product-if-exists, device/ti/proprietary-open/wl12xx/wlan/wl12xx-wlan-fw-products.mk)
$(call inherit-product-if-exists, device/ti/proprietary-open/omap4/ducati-full_jet.mk)
$(call inherit-product-if-exists, device/ti/proprietary-open/omap4/dsp_fw.mk)
