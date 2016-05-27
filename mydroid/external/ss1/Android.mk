LOCAL_PATH := $(call my-dir)

include ndk/build/gmsl/gmsl

BTPM_DIST_REL_PATH  := Dist
BTPM_DIST_FULL_PATH := $(LOCAL_PATH)/$(BTPM_DIST_REL_PATH)
SS1_VENDOR_PATH     := $(LOCAL_PATH)

SS1_SUPPORTED_MODULES :=

BTPM_COMMON_INCLUDES := $(JNI_H_INCLUDE)                                                     \
                        $(BTPM_DIST_FULL_PATH)/BluetopiaPM/include                           \
                        $(BTPM_DIST_FULL_PATH)/Bluetopia/include                             \
                        $(BTPM_DIST_FULL_PATH)/Bluetopia/debug/include                       \
                        $(BTPM_DIST_FULL_PATH)/Bluetopia/SBC/include                         \
                        $(wildcard $(BTPM_DIST_FULL_PATH)/Bluetopia/profiles/*/include)      \
                        $(wildcard $(BTPM_DIST_FULL_PATH)/Bluetopia/profiles_gatt/*/include)

BTPM_CLIENT_INCLUDES := $(BTPM_DIST_FULL_PATH)/BluetopiaPM/include/client \
                        $(BTPM_COMMON_INCLUDES)                           \
                        $(BTPM_DIST_FULL_PATH)/Bluetopia/SBC/include

BTPM_SERVER_INCLUDES := $(BTPM_DIST_FULL_PATH)/BluetopiaPM/include/server \
                        $(BTPM_COMMON_INCLUDES)

# These library lists will be populated dynamically as libraries are defined.
BTPM_CLIENT_LIBS :=
BTPM_SERVER_LIBS :=

SS1_COMMON_INCLUDES := $(SS1_VENDOR_PATH)/SS1UTIL

SS1_COMMON_LIBS := libSS1UTIL

SS1_COMMON_CFLAGS := -DSS1_PLATFORM_SDK_VERSION=$(PLATFORM_SDK_VERSION) \
                     -DSS1_PLATFORM_VERSION_MAJOR=$(or $(word 1,$(subst ., ,$(PLATFORM_VERSION))),0) \
                     -DSS1_PLATFORM_VERSION_MINOR=$(or $(word 2,$(subst ., ,$(PLATFORM_VERSION))),0) \
                     -DSS1_PLATFORM_VERSION_REV=$(or $(word 3,$(subst ., ,$(PLATFORM_VERSION))),0)

ifeq ($(BOARD_HAVE_BLUETOOTH),true)
  SS1_COMMON_CFLAGS += -DHAVE_BLUETOOTH
endif



############################
## Prebuilt libraries

# Define a prebuilt library for use by PM Client runtimes.
# Usage: $(call ss1-add-prebuilt-client-lib,
#               <Module Name>,
#               <List of Source Files>)
define ss1-add-prebuilt-client-lib
$(if $(filter $(1),$(ALL_MODULES)), \
, \
   $(eval $(ss1-prebuilt-lib)) \
) \
$(eval BTPM_CLIENT_LIBS += $(strip $(1)))
endef

# Define a prebuilt library for use by the PM Server runtime.
# Usage: $(call ss1-add-prebuilt-server-lib,
#               <Module Name>,
#               <List of Source Files>)
define ss1-add-prebuilt-server-lib
$(if $(filter $(1),$(ALL_MODULES)), \
, \
   $(eval $(ss1-prebuilt-lib)) \
   $(eval $(call ss1-record-module,$(1),$(true))) \
) \
$(eval BTPM_SERVER_LIBS += $(strip $(1)))
endef

# Define a prebuilt library for use by the PM Server runtime, on condition that
# the library file exists.
# Usage: $(call ss1-add-optional-prebuilt-server-lib,
#               <Module Name>,
#               <List of Source Files>)
define ss1-add-optional-prebuilt-server-lib
$(if $(call seq,$(wildcard $(addprefix $(LOCAL_PATH)/,$(2))),$(addprefix $(LOCAL_PATH)/,$(2))), \
   $(if $(filter $(1),$(ALL_MODULES)), \
   , \
      $(eval $(ss1-prebuilt-lib)) \
      $(eval $(call ss1-record-module,$(1),$(true))) \
   ) \
   $(eval BTPM_SERVER_LIBS += $(strip $(1))) \
, \
   $(eval $(call ss1-record-module,$(1),$(false))) \
)
endef

# Record a module in the appropriate lists.  A module is only added if its name
# matches the pattern followed by either a PM module or a Bluetopia support
# library (such as SBC, but not profiles).  The lists are kept sorted.
# Usage: $(eval $(call ss1-record-module,
#                      <Module Name>,
#                      <Is Supported>)
#        <Is Supported> - $(true) if the module is supported, $(false)
#                         otherwise.
define ss1-record-module
LIB := $$(filter-out libSS1BT%,$(1))
LIB := $$(filter-out $$(LIB),$$(LIB:libBTPM%_SRV=%) $$(LIB:libSS1%=%))
SS1_SUPPORTED_MODULES := $$(sort $$(SS1_SUPPORTED_MODULES) $$(if $(2),$$(LIB)))
endef

# Prebuilt library 
define ss1-prebuilt-lib
include $$(CLEAR_VARS)
LOCAL_MODULE := $(1)
LOCAL_SRC_FILES := $(2)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := STATIC_LIBRARIES
LOCAL_UNINSTALLABLE_MODULE := true
ifeq ($(call lt,$(PLATFORM_SDK_VERSION),14),$(true))
   include $$(BUILD_SYSTEM)/base_rules.mk
   $$(LOCAL_BUILT_MODULE) : $$(SS1_VENDOR_PATH)/$$(LOCAL_SRC_FILES) | $$(ACP)
	$$(transform-prebuilt-to-target)
   $$(LOCAL_BUILT_MODULE).a : $$(SS1_VENDOR_PATH)/$$(LOCAL_SRC_FILES) | $$(ACP)
	$$(transform-prebuilt-to-target)
else
   LOCAL_MODULE_SUFFIX := .a
   include $$(BUILD_PREBUILT)
endif
endef


# PM Client libs

$(call ss1-add-prebuilt-client-lib, libSS1BTPM_CLT, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/lib/client/libSS1BTPM.a )

BTPM_CLIENT_LIBS += libBTPMMODC_CLT

# Add main PM Client library again to satisfy circular dependencies in MODC.
$(call ss1-add-prebuilt-client-lib, libSS1BTPM_CLT, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/lib/client/libSS1BTPM.a )

$(call ss1-add-prebuilt-client-lib, libBTPSFILE, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/lib/libBTPSFILE.a)


# PM Server libs (BTPM)

$(call ss1-add-prebuilt-server-lib, libSS1BTPML_SRV, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/ss1btpm/server/libSS1BTPML.a )

$(call ss1-add-prebuilt-server-lib, libBTPMERR, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/btpmerr/libBTPMERR.a )

BTPM_SERVER_LIBS += libBTPMMODC_SRV

# ANCM does not follow the standard pattern for library naming and location.
$(call ss1-add-optional-prebuilt-server-lib, libBTPMANCM_SRV, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/modules/btpmancm/libBTPMANCM.a )

# Declare the prebuilt targets for the remaining PM modules which follow the
# standard naming convension.
$(foreach library,$(wildcard $(BTPM_DIST_FULL_PATH)/BluetopiaPM/modules/btpm*/server/libBTPM*.a), \
   $(call ss1-add-optional-prebuilt-server-lib, \
      $(patsubst %.a,%_SRV,$(notdir $(library))), \
      $(library:$(BTPM_DIST_FULL_PATH)/%=$(BTPM_DIST_REL_PATH)/%) \
   ) \
)


# PM Server libs (Bluetopia)

$(call ss1-add-prebuilt-server-lib, libSS1BTDBG, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/debug/lib/libSS1BTDBG.a )

$(call ss1-add-prebuilt-server-lib, libSS1BTPS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/lib/libSS1BTPS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTAUD, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/Audio/lib/libSS1BTAUD.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTHIDH, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/HID_Host/lib/libSS1BTHIDH.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTHIDS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/HIDS/lib/libSS1BTHIDS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTANT, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/ANT/lib/libSS1BTANT.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTAVR, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/AVRCP/lib/libSS1BTAVR.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTAVC, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/AVCTP/lib/libSS1BTAVC.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTFTP, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/FTP/lib/libSS1BTFTP.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTGAT, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/GATT/lib/libSS1BTGAT.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTGAV, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/GAVD/lib/libSS1BTGAV.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTHDP, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/HDP/lib/libSS1BTHDP.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTHDS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/HDSET/lib/libSS1BTHDS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTHFR, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/HFRE/lib/libSS1BTHFR.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTHID, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/HID/lib/libSS1BTHID.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTISPP, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/ISPP/lib/libSS1BTISP.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTMAP, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/MAP/lib/libSS1BTMAP.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTOPP, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/OPP/lib/libSS1BTOPP.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTPAN, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/PAN/lib/libSS1BTPAN.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTPBA, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles/PBAP/lib/libSS1BTPBA.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTANS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/ANS/lib/libSS1BTANS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTBAS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/BAS/lib/libSS1BTBAS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTBLS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/BLS/lib/libSS1BTBLS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTCSCS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/CSCS/lib/libSS1BTCSC.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTCTS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/CTS/lib/libSS1BTCTS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTDIS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/DIS/lib/libSS1BTDIS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTGLS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/GLS/lib/libSS1BTGLS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTHRS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/HRS/lib/libSS1BTHRS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTHTS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/HTS/lib/libSS1BTHTS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTIAS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/IAS/lib/libSS1BTIAS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTLLS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/LLS/lib/libSS1BTLLS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTNDCS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/NDCS/lib/libSS1BTNDC.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTPAS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/PASS/lib/libSS1BTPAS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTRSCS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/RSCS/lib/libSS1BTRSC.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTRTUS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/RTUS/lib/libSS1BTRTU.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTSCPS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/SCPS/lib/libSS1BTSCP.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1BTTPS, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/profiles_gatt/TPS/lib/libSS1BTTPS.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1VNET, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/VNET/lib/libSS1VNET.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1IACP, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/iacptrans/lib/libSS1IACP.a )

$(call ss1-add-optional-prebuilt-server-lib, libSS1SBC, \
        $(BTPM_DIST_REL_PATH)/Bluetopia/SBC/lib/libSS1SBC.a )

# GATM APIs are normally embedded within the primary PM API library.  Add GATM
# to the list of supported modules if the API header is present.
SS1_SUPPORTED_MODULES += $(if $(wildcard $(BTPM_DIST_FULL_PATH)/BluetopiaPM/include/server/GATMAPI.h),GATM)

# Add APIs that are always present.  This simplifies code in other makefiles
# that iterate over the list of APIs.
SS1_SUPPORTED_MODULES += DEVM SPPM SCOM

# Detect included modules
SS1_COMMON_CFLAGS += $(SS1_SUPPORTED_MODULES:%=-DSS1_MODULE_ENABLED_%=1)

BTPM_SERVER_LIBS += libBTPSVEND



##############################
## Sample Applications

define ss1-add-optional-application
$(if $(call seq,$(wildcard $(addprefix $(LOCAL_PATH)/,$(2))),$(addprefix $(LOCAL_PATH)/,$(2))), \
   $(eval $(ss1-application)) \
)
endef

define ss1-application
include $$(CLEAR_VARS)
LOCAL_MODULE := $$(strip $(1))
LOCAL_SRC_FILES := $$(strip $(2))
LOCAL_C_INCLUDES := $$(BTPM_CLIENT_INCLUDES)
LOCAL_MODULE_TAGS := optional
LOCAL_SHARED_LIBRARIES := libcutils
LOCAL_STATIC_LIBRARIES := $$(BTPM_CLIENT_LIBS)
LOCAL_CFLAGS += $$(strip $(3))
include $(BUILD_EXECUTABLE)
endef

$(call ss1-add-optional-application, LinuxANCM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxANCM/LinuxANCM.c)

$(call ss1-add-optional-application, LinuxANPM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxANPM/LinuxANPM.c )

$(call ss1-add-optional-application, LinuxAUDM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxAUDM/LinuxAUDM.c \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxAUDM/AudioEncoder.c, \
        -DDISABLE_AUDIO_SINK_AUDIO_PROCESSING )

$(call ss1-add-optional-application, LinuxBASM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxBASM/LinuxBASM.c )

$(call ss1-add-optional-application, LinuxBLPM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxBLPM/LinuxBLPM.c )

$(call ss1-add-optional-application, LinuxDEVM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxDEVM/LinuxDEVM.c )

$(call ss1-add-optional-application, LinuxFMPM_TAR, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxFMPM/LinuxFMPM_TAR.c )

$(call ss1-add-optional-application, LinuxFTPM_CLT, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxFTPM/LinuxFTPM_CLT.c )

$(call ss1-add-optional-application, LinuxFTPM_SRV, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxFTPM/LinuxFTPM_SRV.c )

$(call ss1-add-optional-application, LinuxGATM_CLT, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxGATM/LinuxGATM_CLT.c )

$(call ss1-add-optional-application, LinuxGATM_SRV, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxGATM/LinuxGATM_SRV.c )

$(call ss1-add-optional-application, LinuxGLPM_COL, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxGLPM/LinuxGLPM_COL.c )

$(call ss1-add-optional-application, LinuxHDDM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxHDDM/LinuxHDDM.c )

$(call ss1-add-optional-application, LinuxHDPM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxHDPM/LinuxHDPM.c )

$(call ss1-add-optional-application, LinuxHDSM_AG, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxHDSM/LinuxHDSM_AG.c )

$(call ss1-add-optional-application, LinuxHDSM_HS, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxHDSM/LinuxHDSM_HS.c )

$(call ss1-add-optional-application, LinuxHFRM_AG, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxHFRM/LinuxHFRM_AG.c )

$(call ss1-add-optional-application, LinuxHFRM_HF, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxHFRM/LinuxHFRM_HF.c )

$(call ss1-add-optional-application, LinuxHIDM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxHIDM/LinuxHIDM.c )

$(call ss1-add-optional-application, LinuxHRPM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxHRPM/LinuxHRPM.c )

$(call ss1-add-optional-application, LinuxHTPM_COL, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxHTPM/LinuxHTPM_COL.c )

$(call ss1-add-optional-application, LinuxMAPM_MCE, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxMAPM/LinuxMAPM_MCE.c )

$(call ss1-add-optional-application, LinuxMAPM_MSE, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxMAPM/LinuxMAPM_MSE.c \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxMAPM/MsgStore.c )

$(call ss1-add-optional-application, LinuxOPPM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxOPPM/LinuxOPPM.c )

$(call ss1-add-optional-application, LinuxPANM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxPANM/LinuxPANM.c )

$(call ss1-add-optional-application, LinuxPASM_SRV, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxPASM/LinuxPASM_SRV.c )

$(call ss1-add-optional-application, LinuxPBAM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxPBAM/LinuxPBAM_PCE.c )

$(call ss1-add-optional-application, LinuxPXPM_MON, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxPXPM/LinuxPXPM_MON.c )

$(call ss1-add-optional-application, LinuxSCOM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxSCOM/LinuxSCOM.c )

$(call ss1-add-optional-application, LinuxSPPM, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxSPPM/LinuxSPPM.c )

$(call ss1-add-optional-application, LinuxSPPM_MFi, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxSPPM_MFi/LinuxSPPM_MFi.c )

$(call ss1-add-optional-application, LinuxTIPM_SRV, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxTIPM/LinuxTIPM_SRV.c )

$(call ss1-add-optional-application, LinuxTIPM_CLT, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/LinuxTIPM/LinuxTIPM_CLT.c )

$(call ss1-add-optional-application, SS1Tool, \
        $(BTPM_DIST_REL_PATH)/BluetopiaPM/sample/SS1Tool/SS1Tool.c )


include $(call all-makefiles-under,$(LOCAL_PATH))

