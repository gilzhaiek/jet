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

# Include Modem vendor specific BoardConfig file
-include device/modem-vendor/BoardConfig.mk

# These two variables are set first, so they can be overridden
# by BoardConfigVendor.mk
BOARD_USES_GENERIC_AUDIO := false

# Sun Specific
ifeq ($(JET_PRODUCT), sun)
USE_CAMERA_STUB := true
else
USE_CAMERA_STUB := false
TI_OMAP4_CAMERAHAL_VARIANT := false
endif


OMAP_ENHANCEMENT := true
OMAP_ENHANCEMENT_MULTIGPU := true
# TI's Bluetooth stack based on BlueZ
#BLUETI_ENHANCEMENT := false

ifdef OMAP_ENHANCEMENT
OMAP_ENHANCEMENT_S3D :=
OMAP_ENHANCEMENT_CPCAM :=
OMAP_ENHANCEMENT_VTC := true
endif

ifdef BLUETI_ENHANCEMENT
COMMON_GLOBAL_CFLAGS += -DBLUETI_ENHANCEMENT
endif

ENHANCED_DOMX := true
BLTSVILLE_ENHANCEMENT :=true
USE_ITTIAM_AAC := true
# Use the non-open-source parts, if they're present
#-include vendor/ti/jet/BoardConfigVendor.mk

TARGET_CPU_ABI := armeabi-v7a
TARGET_CPU_ABI2 := armeabi
TARGET_CPU_SMP := true
TARGET_ARCH_VARIANT := armv7-a-neon
ARCH_ARM_HAVE_TLS_REGISTER := true

BOARD_HAVE_BLUETOOTH := true
TARGET_NO_BOOTLOADER := true

# Recovery
#TARGET_RECOVERY_PIXEL_FORMAT := "BGRA_8888"
#TARGET_RECOVERY_UI_LIB := librecovery_ui_jet
# device-specific extensions to the updater binary
TARGET_RELEASETOOLS_EXTENSIONS := device/ti/jet

BOARD_KERNEL_BASE := 0x80000000

TARGET_NO_RADIOIMAGE := true
TARGET_BOARD_PLATFORM := omap4
TARGET_BOOTLOADER_BOARD_NAME := jet 

BOARD_EGL_CFG := device/ti/jet/egl.cfg

#BOARD_USES_HGL := true
#BOARD_USES_OVERLAY := true
USE_OPENGL_RENDERER := true
BOARD_USES_SECURE_SERVICES := true

TARGET_USERIMAGES_USE_EXT4 := true
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 536870912
#BOARD_SYSTEMIMAGE_PARTITION_SIZE := 16777216
BOARD_USERDATAIMAGE_PARTITION_SIZE := 536870912
BOARD_FLASH_BLOCK_SIZE := 4096

#TARGET_PROVIDES_INIT_RC := true
#TARGET_USERIMAGES_SPARSE_EXT_DISABLED := true

# Connectivity - Wi-Fi
USES_TI_MAC80211 := true
ifdef USES_TI_MAC80211
BOARD_WPA_SUPPLICANT_DRIVER      := NL80211
WPA_SUPPLICANT_VERSION           := VER_0_8_X_TI
BOARD_HOSTAPD_DRIVER             := NL80211
BOARD_WLAN_DEVICE                := wl12xx_mac80211
BOARD_SOFTAP_DEVICE              := wl12xx_mac80211
WIFI_DRIVER_MODULE_PATH          := "/system/lib/modules/wl12xx_sdio.ko"
WIFI_DRIVER_MODULE_NAME          := "wl12xx_sdio"
WIFI_FIRMWARE_LOADER             := ""
COMMON_GLOBAL_CFLAGS += -DUSES_TI_MAC80211
endif

ifdef OMAP_ENHANCEMENT
COMMON_GLOBAL_CFLAGS += -DOMAP_ENHANCEMENT -DTARGET_OMAP4
ifdef USE_ITTIAM_AAC
COMMON_GLOBAL_CFLAGS += -DUSE_ITTIAM_AAC
endif
ifdef OMAP_ENHANCEMENT_S3D
COMMON_GLOBAL_CFLAGS += -DOMAP_ENHANCEMENT_S3D
endif
ifdef OMAP_ENHANCEMENT_CPCAM
COMMON_GLOBAL_CFLAGS += -DOMAP_ENHANCEMENT_CPCAM
endif
ifdef OMAP_ENHANCEMENT_VTC
COMMON_GLOBAL_CFLAGS += -DOMAP_ENHANCEMENT_VTC
endif
endif

#Set 32 byte cache line to true
ARCH_ARM_HAVE_32_BYTE_CACHE_LINES := true

BOARD_LIB_DUMPSTATE := libdumpstate.jet

BOARD_VENDOR_TI_GPS_HARDWARE := omap4
BOARD_GPS_LIBRARIES := libgps

# SS1 Bluetopia PM
SS1_BTPSVEND_VARIANT := wl1281_ST

# Common device independent definitions
include device/ti/common-open/BoardConfig.mk
