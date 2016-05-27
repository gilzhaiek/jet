MY_PATH := $(LOCAL_PATH)/../jet

DEVICE_PACKAGE_OVERLAYS := device/ti/jet/overlay

# Internal copy (not to mydroid/out)
$(shell cp -f device/ti/jet/power_profile_$(JET_PRODUCT).xml device/ti/jet/overlay/frameworks/base/core/res/res/xml/power_profile.xml)

PRODUCT_COPY_FILES := \
    device/ti/jet/init_$(JET_PRODUCT).omap4jetboard.rc:root/init.omap4jetboard.rc \
    device/ti/jet/ueventd.omap4jetboard.rc:root/ueventd.omap4jetboard.rc \
    device/ti/jet/twl6030_pwrbutton.kl:system/usr/keylayout/twl6030_pwrbutton.kl \
    device/ti/common-open/audio/audio_policy.conf:system/etc/audio_policy.conf \
    device/ti/jet/media_profiles.xml:system/etc/media_profiles.xml \
    device/ti/jet/media_codecs.xml:system/etc/media_codecs.xml

# to mount the external storage (sdcard)
# Recon Specific and Factory Specific
PRODUCT_COPY_FILES += \
    device/ti/jet/vold.fstab:system/etc/vold.fstab \
    device/ti/jet/limo_key.sh:system/bin/limo_key.sh \
    device/ti/jet/limo_key_win.sh:system/bin/limo_key_win.sh \
    device/ti/jet/recon_boot.sh:system/vendor/bin/recon_boot.sh

# These are the hardware-specific features
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.sensor.compass.xml:system/etc/permissions/android.hardware.sensor.compass.xml \
    frameworks/native/data/etc/android.software.sip.voip.xml:system/etc/permissions/android.software.sip.voip.xml \
    device/ti/jet/android.hardware.location.gps.xml:system/etc/permissions/android.hardware.location.gps.xml \
    frameworks/native/data/etc/android.hardware.usb.host.xml:system/etc/permissions/android.hardware.usb.host.xml \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml \
    frameworks/native/data/etc/android.hardware.sensor.barometer.xml:system/etc/permissions/android.hardware.sensor.barometer.xml \
    frameworks/native/data/etc/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
    frameworks/native/data/etc/android.hardware.wifi.direct.xml:system/etc/permissions/android.hardware.wifi.direct.xml \
    device/ti/jet/android.hardware.bluetooth.xml:system/etc/permissions/android.hardware.bluetooth.xml \
    frameworks/native/data/etc/android.hardware.bluetooth_le.xml:system/etc/permissions/android.hardware.bluetooth_le.xml \
    frameworks/native/data/etc/android.hardware.sensor.accelerometer.xml:system/etc/permissions/android.hardware.sensor.accelerometer.xml \
    frameworks/native/data/etc/android.hardware.sensor.light.xml:system/etc/permissions/android.hardware.sensor.light.xml \
    device/ti/jet/jet_core_hardware.xml:system/etc/permissions/jet_core_hardware.xml \
    device/ti/jet/init_$(JET_PRODUCT).omap4jetboard.usb.rc:root/init.omap4jetboard.usb.rc \
    device/ti/jet/libsensors/sensors_$(JET_PRODUCT).conf:system/lib/hw/sensors.conf \
    device/ti/jet/etc/GPSCConfigFile_$(JET_PRODUCT).cfg:system/etc/gps/config/GPSCConfigFile.cfg \
    device/ti/jet/etc/pathconfigfile_$(JET_PRODUCT).txt:system/etc/gps/config/pathconfigfile.txt \
    device/ti/jet/etc/WW15MGH.DAC:data/gps/WW15MGH.DAC

# Extra Hardware stuff for Sun
ifeq ($(JET_PRODUCT), sun)
PRODUCT_COPY_FILES += \
    device/ti/jet/jet-keys.kl:system/usr/keylayout/jet-keys.kl \
    frameworks/native/data/etc/android.hardware.sensor.proximity.xml:system/etc/permissions/android.hardware.sensor.proximity.xml \
    frameworks/native/data/etc/android.hardware.camera.xml:system/etc/permissions/android.hardware.camera.xml \
    device/ti/jet/sun_core_hardware.xml:system/etc/permissions/sun_core_hardware.xml \
    device/ti/jet/etc/apds9900_qa:system/bin/apds9900_qa
endif

PRODUCT_PACKAGES := \
    com.reconinstruments.os \
    com.reconinstruments.os.xml

PRODUCT_PROPERTY_OVERRIDES := \
    hwui.render_dirty_regions=false

PRODUCT_DEFAULT_PROPERTY_OVERRIDES += \
    persist.sys.usb.config=mtp

PRODUCT_PROPERTY_OVERRIDES += \
    ro.opengles.version=131072

PRODUCT_PROPERTY_OVERRIDES += \
    ro.sf.lcd_density=160

PRODUCT_TAGS += dalvik.gc.type-precise

# Generated kcm keymaps
PRODUCT_PACKAGES += \
    jet_hdcp_keys

# SS1 Bluetopia PM
include external/ss1/products/ss1_base.mk
PRODUCT_PACKAGES += audio.a2dp.jet
PRODUCT_PACKAGES +=     \
    LinuxDEVM       \
    LinuxGATM_CLT   \
    LinuxAUDM       \
    LinuxDEVM       \
    LinuxHFRM_HF    \
    LinuxMAPM_MCE   \
    LinuxMAPM_MSE   \
    LinuxPANM       \
    LinuxPBAM       \
    LinuxSCOM       \
    LinuxSPPM       \
    LinuxSPPM_MFi   \
    SS1Tool


# ANT plugin modules
ifeq ($(JET_PRODUCT), sun)
PRODUCT_PACKAGES += \
        StonestreetOneAntService \
        ANTRadioService \
        AntPlusPluginsService \
        com.dsi.ant.antradio_library
PRODUCT_COPY_FILES += \
    device/ti/jet/framework/JET_ANT/Android_antradio-library/com.dsi.ant.antradio_library.xml:system/etc/permissions/com.dsi.ant.antradio_library.xml 
endif

$(call inherit-product, frameworks/native/build/tablet-dalvik-heap.mk)

