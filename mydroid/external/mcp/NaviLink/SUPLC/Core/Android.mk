LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
suplc_c_src_files := \
		src/codec/AltitudeInfo.c \
		src/codec/CdmaCellInformation.c  \
		src/codec/CellInfo.c  \
		src/codec/CellMeasuredResults.c  \
		src/codec/CellMeasuredResultsList.c  \
		src/codec/CellParametersID.c  \
		src/codec/CPICH-Ec-N0.c  \
		src/codec/CPICH-RSCP.c  \
		src/codec/DGANSS-Sig-Id-Req.c  \
		src/codec/EncodingType.c  \
		src/codec/ExtendedEphCheck.c  \
		src/codec/ExtendedEphemeris.c  \
		src/codec/FormatIndicator.c  \
		src/codec/FQDN.c  \
		src/codec/FrequencyInfo.c  \
		src/codec/FrequencyInfoFDD.c  \
		src/codec/FrequencyInfoTDD.c  \
		src/codec/GanssDataBits.c  \
		src/codec/GanssExtendedEphCheck.c  \
		src/codec/GANSSextEphTime.c  \
		src/codec/GanssNavigationModelData.c  \
		src/codec/GanssReqGenericData.c  \
		src/codec/GanssRequestedCommonAssistanceDataList.c  \
		src/codec/GanssRequestedGenericAssistanceDataList.c  \
		src/codec/GANSSSignals.c  \
		src/codec/GPSTime.c  \
		src/codec/GsmCellInformation.c  \
		src/codec/Horandveruncert.c  \
		src/codec/Horandvervel.c  \
		src/codec/Horvel.c  \
		src/codec/Horveluncert.c  \
		src/codec/IPAddress.c  \
		src/codec/KeyIdentity2.c  \
		src/codec/KeyIdentity3.c  \
		src/codec/KeyIdentity4.c  \
		src/codec/KeyIdentity.c  \
		src/codec/LocationId.c  \
		src/codec/MAC.c  \
		src/codec/MeasuredResults.c  \
		src/codec/MeasuredResultsList.c  \
		src/codec/NavigationModel.c  \
		src/codec/NMR.c  \
		src/codec/NMRelement.c  \
		src/codec/Notification.c  \
		src/codec/NotificationType.c  \
		src/codec/Pathloss.c  \
		src/codec/Position.c  \
		src/codec/PositionEstimate.c  \
		src/codec/PosMethod.c  \
		src/codec/PosPayLoad.c  \
		src/codec/PosProtocol.c  \
		src/codec/PosTechnology.c  \
		src/codec/PrefMethod.c  \
		src/codec/PrimaryCCPCH-RSCP.c  \
		src/codec/PrimaryCPICH-Info.c  \
		src/codec/QoP.c  \
		src/codec/ReqDataBitAssistanceList.c  \
		src/codec/RequestedAssistData.c  \
		src/codec/SatelliteInfo.c  \
		src/codec/SatelliteInfoElement.c  \
		src/codec/SatellitesListRelatedData.c  \
		src/codec/SatellitesListRelatedDataList.c  \
		src/codec/SessionID.c  \
		src/codec/SETAuthKey.c  \
		src/codec/SETCapabilities.c  \
		src/codec/SETId.c  \
		src/codec/SETNonce.c  \
		src/codec/SetSessionID.c  \
		src/codec/SLPAddress.c  \
		src/codec/SLPMode.c  \
		src/codec/SlpSessionID.c  \
		src/codec/SPCAuthKey.c  \
		src/codec/Status.c  \
		src/codec/StatusCode.c  \
		src/codec/SUPLAUTHREQ.c  \
		src/codec/SUPLAUTHRESP.c  \
		src/codec/SUPLEND.c  \
		src/codec/SUPLINIT.c  \
		src/codec/SUPLPOS.c  \
		src/codec/SUPLPOSINIT.c  \
		src/codec/SUPLRESPONSE.c  \
		src/codec/SUPLSTART.c  \
		src/codec/TGSN.c  \
		src/codec/TimeslotISCP.c  \
		src/codec/TimeslotISCP-List.c  \
		src/codec/UARFCN.c  \
		src/codec/UlpMessage.c  \
		src/codec/ULP-PDU.c  \
		src/codec/UTRA-CarrierRSSI.c  \
		src/codec/Velocity.c  \
		src/codec/Ver2-RequestedAssistData-extension.c  \
		src/codec/Ver.c  \
		src/codec/Version.c  \
		src/codec/WcdmaCellInformation.c \
		src/per_codec/ANY.c \
		src/per_codec/asn_codecs_prim.c \
		src/per_codec/asn_SEQUENCE_OF.c \
		src/per_codec/asn_SET_OF.c \
		src/per_codec/BIT_STRING.c \
		src/per_codec/BMPString.c \
		src/per_codec/BOOLEAN.c \
		src/per_codec/constraints.c \
		src/per_codec/constr_CHOICE.c \
		src/per_codec/constr_SEQUENCE.c \
		src/per_codec/constr_SEQUENCE_OF.c \
		src/per_codec/constr_SET.c \
		src/per_codec/constr_SET_OF.c \
		src/per_codec/constr_TYPE.c \
		src/per_codec/ENUMERATED.c \
		src/per_codec/GeneralizedTime.c \
		src/per_codec/GeneralString.c \
		src/per_codec/GraphicString.c \
		src/per_codec/IA5String.c \
		src/per_codec/INTEGER64.c \
		src/per_codec/INTEGER.c \
		src/per_codec/ISO646String.c \
		src/per_codec/NativeEnumerated.c \
		src/per_codec/NativeInteger64.c \
		src/per_codec/NativeInteger.c \
		src/per_codec/NativeReal.c \
		src/per_codec/NULL.c \
		src/per_codec/NumericString.c \
		src/per_codec/ObjectDescriptor.c \
		src/per_codec/OBJECT_IDENTIFIER.c \
		src/per_codec/OCTET_STRING.c \
		src/per_codec/per_decoder.c \
		src/per_codec/per_encoder.c \
		src/per_codec/per_ext_sup.c \
		src/per_codec/per_support.c \
		src/per_codec/PrintableString.c \
		src/per_codec/REAL.c \
		src/per_codec/RELATIVE-OID.c \
		src/per_codec/T61String.c \
		src/per_codec/TeletexString.c \
		src/per_codec/UINTEGER.c \
		src/per_codec/UniversalString.c \
		src/per_codec/UnsignedInteger.c \
		src/per_codec/UTCTime.c \
		src/per_codec/UTF8String.c \
		src/per_codec/VideotexString.c \
		src/per_codec/VisibleString.c 
	

suplc_cpp_src_files := \
		src/device/LinDevice.cpp \
		src/device/Device.cpp \
		src/algorithm/SUPLResponseAlgorithm.cpp \
		src/algorithm/SUPLEndAlgorithm.cpp \
		src/algorithm/Algorithm.cpp \
		src/algorithm/SUPLPosAlgorithm.cpp \
		src/algorithm/SUPLPosInitAlgorithm.cpp \
		src/algorithm/EndAlgorithm.cpp \
		src/algorithm/SUPLInitAlgorithm.cpp \
		src/algorithm/SUPLStartAlgorithm.cpp \
		src/algorithm/StartAlgorithm.cpp \
		src/suplcontroller/supl_timer.cpp \
		src/suplcontroller/SUPLController.cpp \
		src/session/Session.cpp \
		src/ti_client_wrapper/suplc_core_wrapper.cpp \
		src/ti_client_wrapper/suplc_hal_os.cpp \
		src/network/NetListener.cpp \
		src/network/WAPListener.cpp \
		src/network/SMSListener.cpp \
		src/network/NetworkComponent.cpp \
		src/network/Network.cpp \
		src/network/TLSConnection.cpp \
		src/android/SUPLTests.cpp \
		src/android/lp_utils.cpp \
		src/android/SUPLLocationProvider.cpp \
		src/common/Address.cpp \
		src/common/SETCapabilities.cpp \
		src/common/endianess.cpp \
		src/common/MSG.cpp \
		src/common/Velocity.cpp \
		src/common/setsessionid.cpp \
		src/common/PosPayLoad.cpp \
		src/common/types.cpp \
		src/common/RequestedAssistData.cpp \
		src/common/slpsessionid.cpp \
		src/common/TList.cpp \
		src/common/LocationID.cpp \
		src/common/ExtendedEphemeris.cpp \
		src/common/SessionID.cpp \
		src/common/Position.cpp \
		src/common/QoP.cpp \
		src/messages/SUPLPos.cpp \
		src/messages/SUPLResponse.cpp \
		src/messages/SUPLPosInit.cpp \
		src/messages/SUPLEnd.cpp \
		src/messages/SUPLMessage.cpp \
		src/messages/SUPLInit.cpp \
		src/messages/SUPLStart.cpp \
		src/gps/Command.cpp \
		src/gps/DataReqCmd.cpp \
		src/gps/LinGPS.cpp \
		src/gps/StopIndCmd.cpp \
		src/gps/StopReqCmd.cpp \
		src/gps/NotifyRespCmd.cpp \
		src/gps/StartLocIndCmd.cpp \
		src/gps/StartReqCmd.cpp \
		src/gps/GPSCommand.cpp \
		src/gps/DataIndCmd.cpp \
		src/gps/NotifyIndCmd.cpp \
		src/gps/GPS.cpp \
		src/gps/PosIndCmd.cpp \
		src/ULP_Processor/IntegerDiv.cpp \
		src/ULP_Processor/ULP_Common.cpp 

				
suplc_includes := \
		$(MYDROID_PATH)/bionic/libc/include \
		$(MYDROID_PATH)/bionic/libc/arch-arm/include \
		$(MYDROID_PATH)/bionic/libc/kernel/common \
		$(MYDROID_PATH)/bionic/libc/kernel/arch-arm \
		$(MYDROID_PATH)/bionic/libm/include \
		$(MYDROID_PATH)/base/include \
		$(MYDROID_PATH)/bionic/libstdc++/include \
		$(MYDROID_PATH)/frameworks/base/include \
		$(MYDROID_PATH)/system/core/include \
		$(MYDROID_PATH)/hardware/ti/omap3/gps/MCP_Common/Platform/os/LINUX/common/inc \
		$(MYDROID_PATH)/hardware/ti/omap3/gps/MCP_Common/inc \
		$(LOCAL_PATH)/include/ti_client_wrapper \
		$(LOCAL_PATH)/../../../MCP_Common/Platform/os/LINUX/inc \
        	$(LOCAL_PATH)/../../../MCP_Common/Platform/os/LINUX/common/inc \
        	$(LOCAL_PATH)/../../../MCP_Common/Platform/os/LINUX/android_zoom2/inc \
        	$(LOCAL_PATH)/../../../MCP_Common/inc \
        	$(LOCAL_PATH)/../../../MCP_Common/global_inc \
        	$(LOCAL_PATH)/../../../MCP_Common/Platform/inc \
        	$(LOCAL_PATH)/../../../MCP_Common/Platform/inc/int \
        	$(LOCAL_PATH)/../../../MCP_Common/Platform/os/LINUX \
        	$(LOCAL_PATH)/../../../MCP_Common/tran \
		$(LOCAL_PATH)/../../services/logm \
		$(LOCAL_PATH)/include 

		
CPPFLAGS := -Wno-multichar -W -Wall -Wno-unused -Wstrict-aliasing=2 -fno-exceptions -fpic -ffunction-sections -funwind-tables -fmessage-length=0 -finline-functions -fno-inline-functions-called-once -fgcse-after-reload -frerun-cse-after-loop -frename-registers -fvisibility-inlines-hidden -fomit-frame-pointer -fno-strict-aliasing -finline-limit=64 -fno-rtti -D_BYTE_ORDER=_LITTLE_ENDIAN -DLINUX  -D_LINUX_ -D_LINUX -D_ANDROID_IMSI_

CFLAGS := -Wno-multichar -W -Wall -Wno-unused -Wstrict-aliasing=2 -fno-exceptions -fpic -ffunction-sections -funwind-tables -fmessage-length=0 -finline-functions -fno-inline-functions-called-once -fgcse-after-reload -frerun-cse-after-loop -frename-registers -fomit-frame-pointer -fno-strict-aliasing -finline-limit=64 -D_BYTE_ORDER=_LITTLE_ENDIAN -DLINUX

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE := libsupllocationprovider
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_TAGS += debug
LOCAL_SRC_FILES := $(suplc_c_src_files)
LOCAL_SRC_FILES += $(suplc_cpp_src_files)
LOCAL_CPPFLAGS := $(CPPFLAGS)
LOCAL_CFLAGS := $(CFLAGS)
LOCAL_CPP_INCLUDES := $(suplc_includes)
LOCAL_C_INCLUDES := $(suplc_includes)
LOCAL_LDLIBS += -lpthread
LOCAL_SHARED_LIBRARIES := libm libc libstdc++ libutils libcutils libdl libz liblog libgpsservices libmcphalgps
include $(BUILD_SHARED_LIBRARY)

