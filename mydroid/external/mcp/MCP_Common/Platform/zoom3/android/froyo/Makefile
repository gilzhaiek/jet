# environment settings for make usage

SHELL=/bin/bash

#SNAPSHOT_PATH ?= /data/wlan_wcs_android/x0144805/IdoR_MCP_2.x_1_2
SNAPSHOT_WILINK_PATH = $(SNAPSHOT_PATH)/WiLink
NTHREADS ?= 8

# version tags

OMAPMANIFEST_TAG := RLS25.INC3.3rc0
UBOOT_TAG_NAME	:=	L25.INC3.3
UBOOT_TAG_HASH	:=	fbe4cef852de5a39412234b4acd47a830d0282a2
XLOADER_TAG_NAME := L25.INC3.3
XLOADER_TAG_HASH := eb8b047c29a2027fbdd841cd1289fa27ddb22d72

# compilation flags

MYDROID ?= mydroid
ARCH = arm
MYDROID_CROSS_COMPILE = $(PWD)/$(MYDROID)/prebuilt/linux-x86/toolchain/arm-eabi-4.4.0/bin/arm-eabi-
#MYDROID_CROSS_COMPILE ?= /work/arm-2009q1/bin/arm-none-linux-gnueabi-
#MYDROID_CROSS_COMPILE ?= /apps/android/arm-2008q3/bin/arm-none-linux-gnueabi-

UBOOT_CROSS_COMPILE ?= $(MYDROID_CROSS_COMPILE)
XLOADER_CROSS_COMPILE ?= $(MYDROID_CROSS_COMPILE)
KERNEL_CROSS_COMPILE ?= $(MYDROID_CROSS_COMPILE)
MCP2X_CROSS_COMPILE ?= /apps/android/arm-2008q3/bin/arm-none-linux-gnueabi-
#/apps/android/arm-2008q3/bin/arm-none-linux-gnueabi-
#/work/arm-2009q1/bin/arm-none-linux-gnueabi-
#$(MYDROID_CROSS_COMPILE)

# utilities

PATCH=patch -N
REM_PATCH=$(PATCH) -R
COPY=/bin/cp
DEL=/bin/rm
LINK=/bin/ln
SYMLYNK=/bin/ln -s
CHMOD=/bin/chmod
MKDIR=/bin/mkdir
RMDIR=/bin/rmdir
ECHO=/bin/echo

# functions

print = @$(ECHO) -e "\033[7m$1\033[0m"

umount-dir =	$(Q)if [ ! "`/bin/mount | grep $(1)`" = "" ]; then 	\
					$(ECHO) -e "Un-Mounting target folder";			\
					sudo /bin/umount $(1); 							\
				fi;

# paths

ROOTFS_PATH=$(MYDROID)/out/target/product/zoom2/root

OUTPUT_PATH=$(PWD)/output
BOOT_PATH=$(OUTPUT_PATH)/boot
MYFS_PATH=$(OUTPUT_PATH)/myfs
MCP2X_PATH=$(OUTPUT_PATH)/mcp2.x

# make rules

use-snapshot-mount := MOUNT
.PHONY: help all install clean distclean delete check-patches

help:
	@$(ECHO)
	@$(ECHO) '	Texas Instruments WLAN Group Make Utility							'
	@$(ECHO) '	-----------------------------------------							'
	@$(ECHO) '	Normal Usage:														'
	@$(ECHO) '	help 		- show this message										'
	@$(ECHO) '	all 		- build entire project (u-boot, x-loader,				'
	@$(ECHO) '			  kernel, file system, MCP2.x driver)					'
	@$(ECHO) '	install		- same as "all", plus copying the project output to		'
	@$(ECHO) '			  ./output folder										'
	@$(ECHO) '	clean		- clean entire project									'
	@$(ECHO) '	distclean	- distclean entire project								'
	@$(ECHO) '	delete		- delete entire project (use only when fatal...)		'
	@$(ECHO) '																		'
	@$(ECHO) '																		'
	@$(ECHO) '	Extended Usage:														'
	@$(ECHO) '	u-boot-make	- build only u-boot project								'
	@$(ECHO) '	x-loader-make	- build only x-loader project						'
	@$(ECHO) '	kernel-make	- build only kernel project								'
	@$(ECHO) '	$(MYDROID)-make	- build only android file system project			'
	@$(ECHO) '																		'
	@$(ECHO) '	Default execution (when running just make) is all					'
	@$(ECHO) '																		'
	@$(ECHO) '																		'
	@$(ECHO) '	MAKE Arguments:														'
	@$(ECHO) '	SNAPSHOT_PATH	- path for the CC snapshot root						'
	@$(ECHO) '			  (where the "/Wilink" folder exist)						'
	@$(ECHO) '	NTHREADS	- number of processorsto make with						'
	@$(ECHO) '																		'

bringup:	$(MYDROID) 											\
			install-toolchain-u-boot-$(UBOOT_CROSS_COMPILE) 	\
			install-toolchain-x-loader-$(XLOADER_CROSS_COMPILE) \
			install-toolchan-kernel-$(KERNEL_CROSS_COMPILE)		\
			install-toolchan-mcp2x-$(MCP2X_CROSS_COMPILE)		\
			u-boot		\
			x-loader	\
			kernel		\
			mcp2x
	@$(ECHO) -e "BRINGUP DONE" > bringup
	$(call print, "BRINGUP DONE" )

all: 		$(SNAPSHOT_WILINK_PATH)								\
			$(MYDROID) 											\
			install-toolchain-u-boot-$(UBOOT_CROSS_COMPILE) 	\
			install-toolchain-x-loader-$(XLOADER_CROSS_COMPILE) \
			install-toolchan-kernel-$(KERNEL_CROSS_COMPILE)		\
			install-toolchan-mcp2x-$(MCP2X_CROSS_COMPILE)		\
			u-boot												\
			x-loader											\
			kernel												\
			mcp2x
	$(MAKE) u-boot-make
	$(MAKE) x-loader-make
	$(MAKE) kernel-make
	$(MAKE) kernel-make-modules
	$(MAKE) $(MYDROID)-make
	$(MAKE) mcp2x-make
	$(call print, "MAKE ALL FINISHED" )

pack-fs:	all
	$(call print, "PACKING File System" )
	$(call print, "PACKING File System DONE" )	

output:
	@$(MKDIR) -p $(OUTPUT_PATH)
	@$(MKDIR) -p $(BOOT_PATH)
	@$(MKDIR) -p $(MYFS_PATH)
	@$(MKDIR) -p $(MCP2X_PATH)
#	@$(MKDIR) -p $(MCP2X_PATH)/trio $(MCP2X_PATH)/quatrro
#	@$(MKDIR) -p $(MCP2X_PATH)/trio/NoCCX $(MCP2X_PATH)/trio/CCX $(MCP2X_PATH)/trio/GEM
#	@$(MKDIR) -p $(MCP2X_PATH)/quattro/NoCCX $(MCP2X_PATH)/quattro/CCX $(MCP2X_PATH)/quattro/GEM

output-clean:
	@/bin/rm -rf $(OUTPUT_PATH)

install: 	all output
	@$(MKDIR) -p $(BOOT_PATH)
	# copy u-boot image to install dir
	@/bin/cp u-boot/u-boot.bin $(BOOT_PATH)
	# copy x-loader image to install dir
	@/bin/cp x-loader/MLO $(BOOT_PATH)
	# copy omap image to install dir
	@/bin/cp kernel/arch/arm/boot/uImage $(BOOT_PATH)
	@chmod 777 -R $(BOOT_PATH)/*
	
	# make FS folder
	
	# create local folders
	@$(MKDIR) -p $(MYFS_PATH) $(MYFS_PATH)/tmp $(MYFS_PATH)/usr $(MYFS_PATH)/data/busybox $(MYFS_PATH)/var/run/wpa_supplicant $(MYFS_PATH)/system/bin
	# copy files from build	
	@/bin/cp -Rfp $(PWD)/kernel/drivers/usb/misc/*.ko $(ROOTFS_PATH)
	@/bin/cp -Rfp $(PWD)/kernel/drivers/misc/ti-st/*.ko $(ROOTFS_PATH)
	@/bin/cp -Rfp $(PWD)/$(MYDROID)/out/target/product/zoom2/root/* $(MYFS_PATH)
	@/bin/cp -Rfp $(PWD)/$(MYDROID)/out/target/product/zoom2/system $(MYFS_PATH)
	@/bin/cp -Rfp $(PWD)/$(MYDROID)/out/target/product/zoom2/data $(MYFS_PATH)
	# copy init.rc 
	@/bin/cp -f $(PWD)/bin/init.rc $(MYFS_PATH)
	# copy ps_lock script (disable PM) 
	@/bin/cp -f $(PWD)/bin/ps_lock.sh $(MYFS_PATH)
	# copy busybox util and run symlink
	@/bin/cp -f $(PWD)/bin/busybox $(MYFS_PATH)/system/bin/busybox
	@cd $(MYFS_PATH)/data/busybox ; source $(PWD)/bin/mcp_create_busybox_symlink
	# copy WLAN driver
	@$(MKDIR) -p $(MYFS_PATH)/system/etc/wifi 
	@$(MKDIR) -p $(MYFS_PATH)/tiwlan
	@$(MKDIR) -p $(MYFS_PATH)/data/misc/wifi
#	@cd $(MYFS_PATH)/tiwlan ; tar -xvf $(MCP2X_PATH)/quattro/NoCCX/zoom2Binaries.tar
	
	@/bin/cp -f $(MCP2X_PATH)/quattro/NoCCX/wlan_cu $(MYFS_PATH)/system/bin	
	@/bin/cp -f $(MCP2X_PATH)/quattro/NoCCX/firmware.bin $(MYFS_PATH)/system/etc/wifi
	@/bin/cp -f $(MCP2X_PATH)/quattro/NoCCX/tiwlan_1283.ini $(MYFS_PATH)/system/etc/wifi
	@/bin/cp -f $(MCP2X_PATH)/quattro/NoCCX/tiwlan_dual_1283.ini $(MYFS_PATH)/system/etc/wifi
	@/bin/cp -f $(MCP2X_PATH)/quattro/NoCCX/tiwlan_drv.ko $(MYFS_PATH)/system/etc/wifi
	
	@/bin/cp -f $(SNAPSHOT_WILINK_PATH)/config/wpa_supplicant.conf $(MYFS_PATH)/system/etc/wifi/wpa_supplicant.conf
	@/bin/cp -f $(SNAPSHOT_WILINK_PATH)/config/wpa_supplicant.conf  $(MYFS_PATH)/data/misc/wifi/wpa_supplicant.conf
	
	@ln -sf tiwlan_dual_1283.ini $(MYFS_PATH)/system/etc/wifi/tiwlan.ini	
	
	#@/bin/cp $(PWD)/$(MYDROID)/hardware/ti/wlan/wl1271/platform/os/linux/tiwlan_drv.ko system/etc/wifi	
	@chmod 777 -R $(MYFS_PATH)
	@cd $(MYFS_PATH) ; tar -cvf ../myfs.tar *
	# copy WLAN to install dir
	$(call print, "INSTALL DONE" )

# clean all projects

clean: 		output-clean		\
			mcp2x-clean			\
			$(MYDROID)-clean	\
			kernel-clean		\
			x-loader-clean		\
			u-boot-clean
	$(call print, "CLEAN DONE" )

distclean: 	$(MYDROID)-clean	\
			x-loader-distclean	\
			u-boot-distclean	\
			kernel-distclean	\
			mcp2x-distclean
	$(call print, "DISTCLEAN DONE" )

delete:		$(MYDROID)-delete 	\
			x-loader-delete		\
			u-boot-delete		\
			kernel-delete		\
			output-clean
	$(call print, "DELETE DONE" )

# omap distribution
#	Build the FS:
#	16. cd $MYDROID
#	17. cp -Rfp device/ti/zoom2/buildspec.mk.default buildspec.mk
#	18. make -j8
#	19. mkdir /work/build_output
#	20. cd /work/build_output
#	20. mkdir myfs && cd myfs
#	19. cp -Rfp out/target/product/zoom2/root/* . 
#	20. cp -Rfp target/product/zoom2/system/ .
#	21. cp -Rfp target/product/zoom2/data/ .
#	22. cp /work/mydroid_L25_froyo_3.3/hardware/ti/wlan/wl1271/platform/os/linux/tiwlan_drv.ko system/etc/wifi
#	23. chmod 777 * -R
#	24. tar -cvf ../myFS.tar *

$(use-snapshot-mount):

$(MYDROID)/hardware/ti/wlan/mcp_2.x:		$(MYDROID)	
	@$(ECHO) "Creating directory for MCP 2.x"
	@$(MKDIR) -p $(MYDROID)/hardware/ti/wlan/mcp_2.x	

# usage of CUDK as reference to existing snapshot of this version
$(MYDROID)/hardware/ti/wlan/mcp_2.x/CUDK:	$(SNAPSHOT_WILINK_PATH)	\
											$(MYDROID)/hardware/ti/wlan/mcp_2.x
	$(call print, "SNAPSHOT UPDATE" )	
	@$(call umount-dir, $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x)
#ifneq ($(filter $(use-snapshot-mount), $(MAKECMDGOALS)),)
#	@$(ECHO) -e "Mounting SnapShot into local folder"
#	sudo /bin/mount -B -o users $(SNAPSHOT_WILINK_PATH) $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x
#else
	@$(ECHO) -e "Copying files from SnapShot into local folder"
	@/bin/cp -Rfp $(SNAPSHOT_WILINK_PATH)/* $(MYDROID)/hardware/ti/wlan/mcp_2.x/
#endif
	@$(ECHO) "applying CUDK patch"
	$(MAKE) patch-wlan.CUDK
	$(call print, "SNAPSHOT UPDATE DONE" )

$(MYDROID)/hardware/ti/wlan/mcp_2.x-delete:	$(MYDROID)/hardware/ti/wlan/mcp_2.x
	@$(ECHO) "removing CUDK patch"
	$(MAKE) patch-wlan.CUDK-distclean
	@$(call umount-dir, $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x)
ifeq ($(filter $(use-snapshot-mount), $(MAKECMDGOALS)),)
	$(call print, "DELETE MCP 2.x LOCAL DIRECTORY" )
	@/bin/rm -rf $(MYDROID)/hardware/ti/wlan/mcp_2.x
	$(call print, "DELETE DONE" )
else
	$(call print, "DELETE IGNORED - FOLDER UNMOUNTED" )
endif

$(MYDROID)-make: 	$(MYDROID) 
	$(call print, "$(MYDROID) FS MAKE" )
	$(MAKE) -C $(MYDROID) -j$(NTHREADS) ARCH=$(ARCH) CROSS_COMPILE=$(MYDROID_CROSS_COMPILE)
	$(call print, "Extracting DSP Socket Nodes" )
	cd $(PWD)/$(MYDROID)/out/target/product/zoom2/system/lib ; tar -xvf $(PWD)/tars/dsp.tar
	$(call print, "Extracting Sorenson-Spark" )
	cd $(PWD)/$(MYDROID)/out/target/product/zoom2/system/lib/dsp ; tar -xvjf $(PWD)/tars/OPBU_Linux_25.INC3.3_DSPSN_Sorenson_Spark.tar.bz2 ; chmod 777 sparkdec_sn.dll64P
	$(call print, "$(MYDROID) FS MAKE DONE" )
	
$(MYDROID)-clean: 	$(MYDROID)
	$(call print, "$(MYDROID) FS CLEAN" )
	$(MAKE) clean -C $(MYDROID) -j$(NTHREADS) ARCH=$(ARCH) CROSS_COMPILE=$(MYDROID_CROSS_COMPILE)
	$(call print, "$(MYDROID) FS CLEAN DONE" )
	
$(MYDROID)-distclean: $(MYDROID)
	$(call print, "$(MYDROID) FS DISTCLEAN" )
	$(MAKE) distclean -C $(MYDROID) -j$(NTHREADS) ARCH=$(ARCH) CROSS_COMPILE=$(MYDROID_CROSS_COMPILE)
	$(call print, "$(MYDROID) FS DISTCLEAN DONE" )

$(MYDROID)-delete:
	$(call print, "DELETE $(MYDROID)" )
	@rm -rf $(MYDROID)
	$(call print, "DELETE $(MYDROID) DONE" )

$(MYDROID)-config:		$(MYDROID)	\
						check-patches
	$(call print, "$(MYDROID) CONFIG \& PATCHES" )
	@$(ECHO) "copying zoom2 buildspec.mk to local $(MYDROID) directory"
	@/bin/cp -Rfp $(MYDROID)/device/ti/zoom2/buildspec.mk.default $(MYDROID)/buildspec.mk	
	@$(ECHO) "applying zoom2boardconfig patch"
	$(MAKE) patch-zoom2BoardConfig
	@$(ECHO) "applying wpa_supplicant patch"
	$(MAKE) patch-wpa_supplicant
	$(call print, "$(MYDROID) CONFIG \& PATCHES DONE" )

$(MYDROID):
	$(call print, "$(MYDROID) BRING-UP" )
	@$(ECHO) "creating $(MYDROID) folder for omap-android distribution"
	@$(MKDIR) -p $(MYDROID)
	@$(ECHO) -E "getting omapmanifest into $(MYDROID) folder"
	git clone git://git.omapzoom.org/platform/omapmanifest.git $(MYDROID)/my_manifest/omapmanifest
	@$(ECHO) "getting omap-android distribution"
	@cd $(MYDROID)/my_manifest/omapmanifest ; git reset --hard $(OMAPMANIFEST_TAG) ; export MANIFEST=`pwd` ; cd ../../..
	@cd $(MYDROID) ; repo init -u $(PWD)/$(MYDROID)/my_manifest/omapmanifest ; repo sync ; cd ..	
	
	$(MAKE) $(MYDROID)-config
	$(MAKE) $(MYDROID)/hardware/ti/wlan/mcp_2.x/CUDK
	$(call print, "$(MYDROID) BRING-UP DONE" )

# patches install/remove

check-patches: 	patches	\
		patches/patch.propriety.sdio	\
		patches/patch.wlan.cudk.android \
		patches/patch.wlan.ext.gpio	\
		patches/patch.wpasuppl		\
		patches/patch.zoom2BoardConfig	\
		patches/wlan_kernel.patch
	$(call print, "PATCHES CHECK DONE" )

run-patches: 	check-patches		\
		patch-external-gpio	\
		patch-proprietry-sdio	\
		patch-wlan.kernel	\
		patch-zoom2BoardConfig	\
		patch-wlan.CUDK		\
		patch-kernel-asm	\
		patch-wpa_supplicant
	$(call print, "PATCHES INSTALLED" )

rem-patches:	check-patches				\
		patch-external-gpio-distclean		\
		patch-proprietry-sdio-distclean		\
		patch-wlan.kernel-distclean		\
		patch-zoom2BoardConfig-distclean	\
		patch-wlan.CUDK-distclean		\
		patch-kernel-asm-distclean		\
		patch-wpa_supplicant-distclean
	$(call print, "PATCHES REMOVED" )

# patches

patch-external-gpio:	check-patches	\
						kernel
	$(call print, "PATCH external-gpio" )
	cd kernel/arch/arm/mach-omap2 ; $(PATCH) < $(PWD)/patches/patch.wlan.ext.gpio
	$(call print, "PATCH external-gpio DONE" )
#	@touch patch-external-gpio

patch-proprietry-sdio: 	check-patches	\
						kernel
	$(call print, "PATCH proprietry-sdio" )
	cd kernel ; $(PATCH) < $(PWD)/patches/patch.propriety.sdio
	$(call print, "PATCH proprietry-sdio DONE" )
#	@touch patch-proprietry-sdio

patch-wlan.kernel:		check-patches	\
						kernel
	$(call print, "PATCH kernel wlan" )
	cd kernel/arch/arm/mach-omap2 ; $(PATCH) < $(PWD)/patches/wlan_kernel.patch
	$(call print, "PATCH wlan.kernel DONE" )
#	@touch patch-wlan.kernel

patch-kernel-asm: 		check-patches	\
						kernel
	$(call print, "PATCH ASM" )
#	$(if [ -f $(PWD)/kernel/include/asm-arm/bitsperlong.h ]; then \
#		rm $(PWD)/kernel/include/asm-arm/bitsperlong.h;	\
#	fi;
	ln -sf $(PWD)/kernel/include/asm-generic/bitsperlong.h $(PWD)/kernel/include/asm-arm/bitsperlong.h
	$(call print, "PATCH ASM DONE" )
#	@touch patch-kernel-asm

patch-zoom2BoardConfig: check-patches	\
						$(MYDROID)
	$(call print, "PATCH zoom2BoardConfig" )
	cd $(MYDROID)/device/ti/zoom2 ; $(PATCH) < $(PWD)/patches/patch.zoom2BoardConfig
	$(call print, "PATCH zoom2BoardConfig DONE" )
#	@touch patch-zoom2BoardConfig

patch-wpa_supplicant: 	check-patches	\
						$(MYDROID)
	$(call print, "PATCH wpa_supplicant" )
	cd $(MYDROID)/external ; $(PATCH) -p0 < $(PWD)/patches/patch.wpasuppl
	$(call print, "PATCH wpa_supplicant DONE" )
#	@touch patch-wpa_supplicant

patch-wlan.CUDK:		check-patches	\
						$(MYDROID)
	$(call print, "PATCH wlan.CUDK" )
	cd $(MYDROID)/hardware/ti/wlan/mcp_2.x/CUDK/os/linux/src ; $(PATCH) < $(PWD)/patches/patch.wlan.cudk.android
	$(call print, "PATCH wlan.CUDK DONE" )
#	@touch patch-wlan.CUDK

# clean patches

patch-external-gpio-distclean:
	#patch-external-gpio
	$(call print, "REMOVE PATCH external-gpio" )
	cd kernel/arch/arm/mach-omap2 ; $(REM_PATCH) < $(PWD)/patches/patch.wlan.ext.gpio
	#	@/bin/rm patch-external-gpio
	$(call print, "PATCH external-gpio REMOVED" )

patch-proprietry-sdio-distclean:
	#patch-proprietry-sdio
	$(call print, "REMOVE PATCH proprietry-sdio" )
	cd kernel ; $(REM_PATCH) < $(PWD)/patches/patch.propriety.sdio
#	@/bin/rm patch-proprietry-sdio
	$(call print, "PATCH proprietry-sdio REMOVED" )

patch-wlan.kernel-distclean: 
	#patch-wlan.kernel
	$(call print, "REMOVE PATCH kernel wlan" )
	cd kernel/arch/arm/mach-omap2 ; $(REM_PATCH) < $(PWD)/patches/wlan_kernel.patch
#	@/bin/rm patch-wlan.kernel
	$(call print, "PATCH wlan.kernel REMOVED" )

patch-zoom2BoardConfig-distclean:
	#patch-zoom2BoardConfig
	$(call print, "REMOVE PATCH zoom2BoardConfig" )
	cd $(MYDROID)/device/ti/zoom2 ; $(REM_PATCH) < $(PWD)/patches/patch.zoom2BoardConfig
#	@/bin/rm patch-zoom2BoardConfig
	$(call print, "PATCH zoom2BoardConfig REMOVED" )
	
patch-kernel-asm-distclean:
	#patch-kernel-asm
	$(call print, "REMOVE PATCH ASM" )
	@/bin/rm $(PWD)/kernel/include/asm-arm/bitsperlong.h
#	@/bin/rm patch-kernel-asm
	$(call print, "PATCH ASM REMOVED" )

patch-wlan.CUDK-distclean:
	#patch-wlan.CUDK
	$(call print, "REMOVE PATCH wlan.CUDK" )
	cd $(MYDROID)/hardware/ti/wlan/mcp_2.x/CUDK/os/linux/src ; $(REM_PATCH) < $(PWD)/patches/patch.wlan.cudk.android
#	@/bin/rm patch-wlan.CUDK
	$(call print, "PATCH wlan.CUDK REMOVED" )

patch-wpa_supplicant-distclean:
	#patch-wpa_supplicant
	$(call print, "REMOVE PATCH wpa_supplicant" )
	cd $(MYDROID)/external ; $(REM_PATCH) -p0 < $(PWD)/patches/patch.wpasuppl
#	@/bin/rm patch-wpa_supplicant
	$(call print, "PATCH wpa_supplicant REMOVED" )
	
# toolchains
	
install-toolchain-u-boot-$(UBOOT_CROSS_COMPILE): install-toolchain-$(MYDROID)
	@$(ECHO) "u-boot toolchain installed"

install-toolchain-x-loader-$(XLOADER_CROSS_COMPILE): install-toolchain-$(MYDROID)
	@$(ECHO) "x-loader toolchain installed"

install-toolchan-kernel-$(KERNEL_CROSS_COMPILE): install-toolchain-$(MYDROID)
	@$(ECHO) "kernel toolchain installed"

install-toolchan-mcp2x-$(MCP2X_CROSS_COMPILE): install-toolchain-$(MYDROID)
	@$(ECHO) "mcp2.x toolchain installed"
	
install-toolchain-$(MYDROID): $(MYDROID) $(MYDROID)/prebuilt/linux-x86/toolchain/arm-eabi-4.4.0/bin
	@$(ECHO) "$(MYDROID) toolchain installed"

#	git clone  git://git.omapzoom.org/repo/u-boot.git
#	cd u-boot/
#	git checkout -b L25.INC3.3 fbe4cef852de5a39412234b4acd47a830d0282a2
#	make ARCH=arm CROSS_COMPILE=arm-eabi- omap3630zoom3_config
#	make ARCH=arm CROSS_COMPILE=arm-eabi-
#	Output: u-boot.bin (/work/mydroid_L25_froyo_3.3/u-boot/u-boot.bin)

u-boot-make: 	u-boot
	$(call print, "u-boot MAKE" )
	$(MAKE) -C u-boot ARCH=$(ARCH) CROSS_COMPILE=$(UBOOT_CROSS_COMPILE)
	@/bin/cp -Rfp u-boot/tools/mkimage $(HOME)/bin
	#@source export PATH=${PATH}:$(PWD)/u-boot/tools
	$(call print, "u-boot MAKE DONE" )

u-boot-config: 	u-boot
	$(MAKE) -C u-boot ARCH=$(ARCH) CROSS_COMPILE=$(UBOOT_CROSS_COMPILE) omap3630zoom3_config
	#for zoom2 platform use: omap3430zoom2
	
u-boot-clean: 	u-boot
	$(call print, "u-boot CLEAN" )
	$(MAKE) clean -C u-boot ARCH=$(ARCH) CROSS_COMPILE=$(UBOOT_CROSS_COMPILE)
	$(call print, "u-boot CLEAN DONE" )

u-boot-distclean: u-boot
	$(call print, "u-boot DISTCLEAN" )
	$(MAKE) distclean -C u-boot ARCH=$(ARCH) CROSS_COMPILE=$(UBOOT_CROSS_COMPILE)
	$(call print, "u-boot DISTCLEAN DONE" )
	
u-boot-delete:
	$(call print, "u-boot DELETE" )
	@rm -rf ./u-boot
	$(call print, "u-boot DELETE DONE" )
	
u-boot:
	$(call print, "u-boot BRINGUP" )
	git clone  git://git.omapzoom.org/repo/u-boot.git
	@cd u-boot ; git checkout -b $(UBOOT_TAG_NAME) $(UBOOT_TAG_HASH)  ; cd ..
	#$(MAKE) -C u-boot ARCH=$(ARCH) CROSS_COMPILE=$(UBOOT_CROSS_COMPILE) omap3630zoom3_config
	$(MAKE) u-boot-config
	$(call print, "u-boot BRINGUP DONE" )

#	git clone git://git.omapzoom.org/repo/x-loader.git
#	cd x-loader/
#	git checkout -b L25.INC3.3 eb8b047c29a2027fbdd841cd1289fa27ddb22d72
#	make ARCH=arm CROSS_COMPILE=arm-eabi- omap3630zoom3_1G_config
#	make ARCH=arm CROSS_COMPILE=arm-eabi- ift
#	Output: MLO (/work/mydroid_L25_froyo_3.3/x-loader/MLO)
	
x-loader-make: 	x-loader
	$(call print, "x-loader MAKE" )
	$(MAKE) -C x-loader CROSS_COMPILE=$(XLOADER_CROSS_COMPILE) ift
	$(call print, "x-loader MAKE DONE" )

x-loader-config: x-loader
	$(MAKE) -C x-loader CROSS_COMPILE=$(XLOADER_CROSS_COMPILE) omap3630zoom3_1G_config
	#for zoom2 platform use: omap3430zoom2
	
x-loader-clean: x-loader
	$(call print, "x-loader  CLEAN" )
	$(MAKE) clean -C x-loader CROSS_COMPILE=$(XLOADER_CROSS_COMPILE)
	$(call print, "x-loader CLEAN DONE" )

x-loader-distclean: x-loader
	$(call print, "x-loader DISTCLEAN" )
	$(MAKE) distclean -C x-loader CROSS_COMPILE=$(XLOADER_CROSS_COMPILE)
	$(call print, "x-loader DISTCLEAN DONE" )
	
x-loader-delete:
	$(call print, "x-loader DELETE" )
	@rm -rf ./x-loader
	$(call print, "x-loader DELETE DONE" )
	
x-loader:
	$(call print, "x-loader BRINGUP" )
	git clone git://git.omapzoom.org/repo/x-loader.git
	@cd x-loader ; git checkout -b $(XLOADER_TAG_NAME) $(XLOADER_TAG_HASH) ; cd ..
	#$(MAKE) -C x-loader CROSS_COMPILE=$(XLOADER_CROSS_COMPILE) omap3630zoom3_1G_config
	$(MAKE) x-loader-config
	$(call print, "x-loader BRINGUP DONE" )
	
#	git clone git://git.omapzoom.org/kernel/omap.git
#	cd omap/
#	git checkout --track -b p-android-omap-2.6.32 origin/p-android-omap-2.6.32
#	git checkout -b L25.INC3.3 android-2.6.32-2.2-Inc3-3
#	make ARCH=arm CROSS_COMPILE=arm-eabi- android_zoom3_defconfig
#	make -j8 ARCH=arm CROSS_COMPILE=arm-eabi- uImage
#	Output: uImage   (/work/mydroid_L25_froyo_3.3/omap/arch/arm/boot/uImage)

kernel-make: 	kernel
	$(call print, "kernel MAKE" )
	$(MAKE) -C kernel -j$(NTHREADS) ARCH=$(ARCH) CROSS_COMPILE=$(KERNEL_CROSS_COMPILE) uImage
	@$(ECHO) "applying patch-kernel-asm patch"
	$(MAKE) patch-kernel-asm
	$(call print, "kernel MAKE DONE" )

kernel-make-modules: kernel
	$(call print, "kernel modules MAKE" )
	$(MAKE) -C kernel -j$(NTHREADS) ARCH=$(ARCH) CROSS_COMPILE=$(KERNEL_CROSS_COMPILE) modules
	$(call print, "kernel modules MAKE DONE" )

kernel-clean: 	kernel
	$(call print, "kernel CLEAN" )
	$(MAKE) clean -C kernel -j$(NTHREADS) ARCH=$(ARCH) CROSS_COMPILE=$(KERNEL_CROSS_COMPILE)
	$(call print, "kernel CLEAN DONE" )

kernel-distclean: kernel
	$(call print, "kernel DISTCLEAN" )
	@$(ECHO) "remove patch-kernel-asm patch"
	$(MAKE) patch-kernel-asm-distclean
	$(MAKE) distclean -C kernel -j$(NTHREADS) ARCH=$(ARCH) CROSS_COMPILE=$(KERNEL_CROSS_COMPILE)	
	@$(ECHO) "remove patch-external-gpio patch"
	$(MAKE) patch-external-gpio-distclean
	@$(ECHO) "remove patch-proprietry-sdio patch"
	$(MAKE) patch-proprietry-sdio-distclean
	@$(ECHO) "remove patch-wlan.kernel patch"
	$(MAKE) patch-wlan.kernel-distclean	
	$(call print, "kernel DISTCLEAN DONE" )
	
kernel-delete: 	
	$(call print, "kernel DELETE" )
	@rm -rf ./kernel
	$(call print, "kernel DELETE DONE" )
	
kernel-config:	kernel
	$(call print, "kernel CONFIG \& PATCHES" )
	$(MAKE) -C kernel ARCH=$(ARCH) CROSS_COMPILE=$(KERNEL_CROSS_COMPILE) android_zoom3_defconfig
#	for zoom2 platform use: omap_zoom2_defconfig		
	@$(ECHO) "applying patch-external-gpio patch"
	$(MAKE) patch-external-gpio
	@$(ECHO) "applying patch-proprietry-sdio patch"
	$(MAKE) patch-proprietry-sdio
	@$(ECHO) "applying patch-wlan.kernel patch"
	$(MAKE) patch-wlan.kernel
	$(call print, "kernel CONFIG \& PATCHES DONE" )
	
kernel:
	$(call print, "kernel BRINGUP" )
	git clone git://git.omapzoom.org/kernel/omap.git
	@cd omap ; git checkout --track -b p-android-omap-2.6.32 origin/p-android-omap-2.6.32 ; \
	git checkout -b L25.INC3.3 android-2.6.32-2.2-Inc3-3 ; cd ..
	@/bin/mv omap kernel
	$(MAKE) kernel-config
	$(call print, "kernel BRINGUP DONE" )

# MCP 2.x

mcp2x-make:		mcp2x
	$(call print, "MCP 2.x MAKE" )
#	$(MAKE) clean -C $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux ARCH=$(ARCH) CROSS_COMPILE=$(MCP2X_CROSS_COMPILE) HOST_PLATFORM=zoom2 KERNEL_DIR=$(PWD)/kernel TI_HOST_OS=ANDROID MYDROID=$(PWD)/$(MYDROID)
#	$(MAKE) mcp2x-make-trio-NoCCX
	$(MAKE) mcp2x-make-quattro-NoCCX
#	$(MAKE) clean -C $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux ARCH=$(ARCH) CROSS_COMPILE=$(MCP2X_CROSS_COMPILE) HOST_PLATFORM=zoom2 KERNEL_DIR=$(PWD)/kernel TI_HOST_OS=ANDROID MYDROID=$(PWD)/$(MYDROID)
#	$(MAKE) mcp2x-make-trio-CCX
#	$(MAKE) mcp2x-make-quattro-CCX
#	$(MAKE) clean -C $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux ARCH=$(ARCH) CROSS_COMPILE=$(MCP2X_CROSS_COMPILE) HOST_PLATFORM=zoom2 KERNEL_DIR=$(PWD)/kernel TI_HOST_OS=ANDROID MYDROID=$(PWD)/$(MYDROID)
#	$(MAKE) mcp2x-make-trio-GEM
#	$(MAKE) mcp2x-make-quattro-GEM
	$(call print, "MCP 2.x MAKE DONE" )

mcp2x-make-trio-NoCCX:
	$(call print, "BUILDING MCP 2.x TRIO NoCCX" ) 
	$(MAKE) -C $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux ARCH=$(ARCH) CROSS_COMPILE=$(MCP2X_CROSS_COMPILE) HOST_PLATFORM=zoom2 KERNEL_DIR=$(PWD)/kernel TI_HOST_OS=ANDROID MYDROID=$(PWD)/$(MYDROID) BMTRACE=n ETH_SUPPORT=y CCX=n SUPPL=WPA CONFIG_EAP_WSC=y BUILD_SUPPL=y
	@/bin/rm -rf $(MCP2X_PATH)/trio/NoCCX
	@$(MKDIR) -p $(MCP2X_PATH)/trio/NoCCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/zoom2Binaries.tar $(MCP2X_PATH)/trio/NoCCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/firmware.bin $(MCP2X_PATH)/trio/NoCCX
#	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_1273.ini $(MCP2X_PATH)/trio/NoCCX
#	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_dual_1273.ini $(MCP2X_PATH)/trio/NoCCX
	$(call print, "BUILDING MCP 2.x TRIO NoCCX - DONE" )
	
mcp2x-make-trio-CCX:
	$(call print, "BUILDING MCP 2.x TRIO CCX" )
	$(MAKE) -C $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux ARCH=$(ARCH) CROSS_COMPILE=$(MCP2X_CROSS_COMPILE) HOST_PLATFORM=zoom2 KERNEL_DIR=$(PWD)/kernel TI_HOST_OS=ANDROID MYDROID=$(PWD)/$(MYDROID) BMTRACE=n ETH_SUPPORT=y CCX=y SUPPL=DEVICESCAPE CONFIG_EAP_WSC=y BUILD_SUPPL=y
	@/bin/rm -rf $(MCP2X_PATH)/trio/CCX
	@$(MKDIR) -p $(MCP2X_PATH)/trio/CCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/zoom2Binaries.tar $(MCP2X_PATH)/trio/CCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/firmware.bin $(MCP2X_PATH)/trio/CCX
#	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_1273.ini $(MCP2X_PATH)/trio/CCX
#	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_dual_1273.ini $(MCP2X_PATH)/trio/CCX
	$(call print, "BUILDING MCP 2.x TRIO CCX - DONE" )
	
mcp2x-make-trio-GEM:
	$(call print, "BUILDING MCP 2.x TRIO GEM" )
	$(MAKE) -C $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux ARCH=$(ARCH) CROSS_COMPILE=$(MCP2X_CROSS_COMPILE) HOST_PLATFORM=zoom2 KERNEL_DIR=$(PWD)/kernel TI_HOST_OS=ANDROID MYDROID=$(PWD)/$(MYDROID) BMTRACE=n ETH_SUPPORT=y GEM=y CCX=n SUPPL=GEM CONFIG_WAPI=y CONFIG_EAP_WSC=y BUILD_SUPPL=y
	@/bin/rm -rf $(MCP2X_PATH)/trio/GEM
	@$(MKDIR) -p $(MCP2X_PATH)/trio/GEM
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/zoom2Binaries.tar $(MCP2X_PATH)/trio/GEM
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/firmware.bin $(MCP2X_PATH)/trio/GEM
#	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_1273.ini $(MCP2X_PATH)/trio/GEM
#	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_dual_1273.ini $(MCP2X_PATH)/trio/GEM
	$(call print, "BUILDING MCP 2.x TRIO GEM - DONE" )
	
mcp2x-make-quattro-NoCCX:
	$(call print, "BUILDING MCP 2.x QUATTRO NoCCX" )
	$(MAKE) -C $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux ARCH=$(ARCH) CROSS_COMPILE=$(MCP2X_CROSS_COMPILE) HOST_PLATFORM=zoom2 KERNEL_DIR=$(PWD)/kernel TI_HOST_OS=ANDROID MYDROID=$(PWD)/$(MYDROID) BMTRACE=n ETH_SUPPORT=y CCX=n SUPPL=WPA CONFIG_EAP_WSC=y BUILD_SUPPL=y TNETW=1283 USE_ANDROID_APPS_ONLY=n
	@mkdir -p $(MCP2X_PATH)/quattro/NoCCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/zoom2Binaries.tar $(MCP2X_PATH)/quattro/NoCCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/wlan_cu $(MCP2X_PATH)/quattro/NoCCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/firmware.bin $(MCP2X_PATH)/quattro/NoCCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_1283.ini $(MCP2X_PATH)/quattro/NoCCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_dual_1283.ini $(MCP2X_PATH)/quattro/NoCCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_drv.ko $(MCP2X_PATH)/quattro/NoCCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/wpa_supplicant.txt $(MCP2X_PATH)/quattro/NoCCX
#	@/bin/cp -rf $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/* $(MCP2X_PATH)/quattro/NoCCX
	$(call print, "BUILDING MCP 2.x QUATTRO NoCCX - DONE" )
	
mcp2x-make-quattro-CCX:
	$(call print, "BUILDING MCP 2.x QUATTRO CCX" )
	$(MAKE) -C $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux ARCH=$(ARCH) CROSS_COMPILE=$(MCP2X_CROSS_COMPILE) HOST_PLATFORM=zoom2 KERNEL_DIR=$(PWD)/kernel TI_HOST_OS=ANDROID MYDROID=$(PWD)/$(MYDROID) BMTRACE=n ETH_SUPPORT=y CCX=y SUPPL=DEVICESCAPE CONFIG_EAP_WSC=n BUILD_SUPPL=y TNETW=1283
	@/bin/rm -rf $(MCP2X_PATH)/quattro/CCX
	@$(MKDIR) -p $(MCP2X_PATH)/quattro/CCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/zoom2Binaries.tar $(MCP2X_PATH)/quattro/CCX
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/firmware.bin $(MCP2X_PATH)/quattro/CCX
#	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_1283.ini $(MCP2X_PATH)/quattro/CCX
#	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_dual_1283.ini $(MCP2X_PATH)/quattro/CCX
	$(call print, "BUILDING MCP 2.x QUATTRO CCX - DONE" )
	
mcp2x-make-quattro-GEM:
	$(call print, "BUILDING MCP 2.x QUATTRO GEM" )
	$(MAKE) -C $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux ARCH=$(ARCH) CROSS_COMPILE=$(MCP2X_CROSS_COMPILE) HOST_PLATFORM=zoom2 KERNEL_DIR=$(PWD)/kernel TI_HOST_OS=ANDROID MYDROID=$(PWD)/$(MYDROID) BMTRACE=n ETH_SUPPORT=y GEM=y CCX=n SUPPL=GEM CONFIG_WAPI=y CONFIG_EAP_WSC=n BUILD_SUPPL=y TNETW=1283
	@/bin/rm -rf $(MCP2X_PATH)/quattro/GEM
	@$(MKDIR) -p $(MCP2X_PATH)/quattro/GEM
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/zoom2Binaries.tar $(MCP2X_PATH)/quattro/GEM
	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/firmware.bin $(MCP2X_PATH)/quattro/GEM
#	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_1283.ini $(MCP2X_PATH)/quattro/GEM
#	@/bin/cp -f $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux/tiwlan_dual_1283.ini $(MCP2X_PATH)/quattro/GEM
	$(call print, "BUILDING MCP 2.x QUATTRO GEM - DONE" )
	

mcp2x-clean: 	mcp2x
	$(call print, "MCP 2.x CLEAN" )
	$(MAKE) clean -C $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux ARCH=$(ARCH) CROSS_COMPILE=$(MCP2X_CROSS_COMPILE) HOST_PLATFORM=zoom2 KERNEL_DIR=$(PWD)/kernel
	@/bin/rm -rf $(MCP2X_PATH)/*
	$(call print, "MCP 2.x CLEAN DONE" )

mcp2x-distclean: mcp2x
	$(call print, "MCP 2.x DISTCLEAN" )
	$(MAKE) distclean -C $(PWD)/$(MYDROID)/hardware/ti/wlan/mcp_2.x/platforms/os/linux ARCH=$(ARCH) CROSS_COMPILE=$(MCP2X_CROSS_COMPILE) HOST_PLATFORM=zoom2 KERNEL_DIR=$(PWD)/kernel
	@/bin/rm -rf $(MCP2X_PATH)/* 
	$(call print, "MCP 2.x DISTCLEAN DONE" )

mcp2x: 		$(MYDROID)									\
			kernel
#	$(call print, "MCP 2.x BRINGUP" )
	$(call print, "MCP 2.x BRINGUP DONE" )


