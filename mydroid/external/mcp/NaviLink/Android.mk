#
# NaviLink Lib
#

BTIPS_DEBUG?=0

LOCAL_PATH:= $(call my-dir)
#Enable this flag to enable WIFI feature
WIFI_FEATURE = 'n'
ifeq ($(WIFI_FEATURE), 'y')
WIFI_FILES := NAVC/navc_wplServices.c  NAVC/navc_hybridPosition.c 
else
WIFI_FILES :=
endif

navilink_src_files := \
		NAVC/navc_outEvt.c \
		NAVC/navc_priority_handler.c \
		NAVC/navc_main.c \
		NAVC/navc_sm.c \
		NAVC/nt_adapt.c \
		NAVC/navc_cmdHandler.c \
		NAVC/navc_services.c \
		NAVC/navc_internalEvtHandler.c \
		NAVC/navc_time.c \
		NAVC/navc_errHandler.c \
		NAVC/navc_inavc_if.c \
		NAVC/Core/gpsc_ai2.c \
		NAVC/Core/gpsc_app_api.c \
		NAVC/Core/gpsc_comm.c \
		NAVC/Core/gpsc_database.c \
		NAVC/Core/gpsc_drv_api.c \
		NAVC/Core/gpsc_ext_api.c \
		NAVC/Core/gpsc_geo_fence.c \
		NAVC/Core/gpsc_init.c \
		NAVC/Core/gpsc_meas.c \
		NAVC/Core/gpsc_mgp.c \
		NAVC/Core/gpsc_mgp_assist.c \
		NAVC/Core/gpsc_mgp_cfg.c \
		NAVC/Core/gpsc_mgp_rx.c \
		NAVC/Core/gpsc_mgp_tx.c \
		NAVC/Core/gpsc_msg.c \
		NAVC/Core/gpsc_pm_assist.c \
		NAVC/Core/gpsc_sess.c \
		NAVC/Core/gpsc_state.c \
		NAVC/Core/gpsc_time_inject_api.c \
		NAVC/Core/gpsc_timer_api.c \
		NAVC/Core/gpsc_utils.c \
		NAVC/Core/gpsc_sequence.c \
		NAVC/Core/gpsc_cd.c \
		NAVC/Core/gpsc_svdir.c \
		NAVC/Core/gpsc_heading_filter.c \
		NAVC/Lib/navc_rrc.c \
		NAVC/Lib/navc_rrlp.c \
		navl/navl_api.c \
		platforms/modem/hostSocket.c \
		platforms/modem/pla_app_ads_protocol.c \
		platforms/modem/pla_app_ads_transport.c \
		SUPLC/suplc_cmdHandler.c \
		SUPLC/suplc_main.c \
		SUPLC/suplc_actionHandler.c \
		SUPLC/suplc_evtHandler.c \
		SUPLC/suplc_outEvt.c \
		SUPLC/Core/src/ti_supl/suplc_app_api.c \
		SUPLC/Core/src/ti_supl/suplc_protPos_api.c \
		SUPLC/Core/src/ti_supl/suplc_core_api.c \
		SUPLC/Core/src/ti_supl/suplc_state.c \
		services/logm/nav_log_msg.c \
		$(WIFI_FILES)


navilink_c_includes := \
	$(LOCAL_PATH)/../MCP_Common/Platform/os/LINUX/inc \
	$(LOCAL_PATH)/../MCP_Common/Platform/os/LINUX/common/inc \
	$(LOCAL_PATH)/../MCP_Common/Platform/os/LINUX/android_zoom3/inc \
	$(LOCAL_PATH)/../../MCP_Common/Platform/os/LINUX/android_zoom2/inc \
	$(LOCAL_PATH)/../MCP_Common/inc \
	$(LOCAL_PATH)/../MCP_Common/global_inc \
	$(LOCAL_PATH)/../MCP_Common/Platform/inc \
	$(LOCAL_PATH)/../MCP_Common/Platform/inc/int \
	$(LOCAL_PATH)/../MCP_Common/Platform/os/LINUX \
	$(LOCAL_PATH)/../MCP_Common/tran \
	$(LOCAL_PATH)/NAVC \
	$(LOCAL_PATH)/NAVC/Core \
	$(LOCAL_PATH)/NAVC/Lib \
        $(LOCAL_PATH)/WPL \
      $(LOCAL_PATH)/swlib/inc \
	$(LOCAL_PATH)/inc \
	$(LOCAL_PATH)/platforms/modem \
	$(LOCAL_PATH)/SUPLC/Core/include/ti_supl \
	$(LOCAL_PATH)/SUPLC/Core/include/ti_client_wrapper \
	$(LOCAL_PATH)/SUPLC/Core/include/ \
	$(LOCAL_PATH)/inavc \
	$(LOCAL_PATH)/swlib/inc \
	$(LOCAL_PATH)/services/logm \
	bionic/libc/include \
	bionic/libc/private 

navilink_shared_libraries := libutils libcutils libc liblog libsupllocationprovider libmcphalgps libgpsservices 
ifeq ($(WIFI_FEATURE), 'y')
navilink_shared_libraries += libwifipe
endif

navilink_static_libraries := 
		 

##
##
## Build navilink lib
##
##
include $(CLEAR_VARS)
LOCAL_MODULE := libgps
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_TAGS += debug
LOCAL_PRELINK_MODULE := false
LOCAL_SRC_FILES := $(navilink_src_files)

DOCOMO_FEATURE = 'y'
DOCOMO_FEATURE_MSISDN = 'n'
DOCOMO_FEATURE_WCDMA = 'n'

ifeq ($(DOCOMO_FEATURE), 'y')
ifeq ($(DOCOMO_FEATURE_MSISDN), 'y')
LOCAL_CFLAGS+=-DDOCOMO_SUPPORT_MSISDN
endif
ifeq ($(DOCOMO_FEATURE_WCDMA), 'y')
LOCAL_CFLAGS+=-DDOCOMO_SUPPORT_WCDMA
endif
endif # //end DOCOMO_FEATURE



LOCAL_CFLAGS += -Wall -DLINUX -D_NAVC_APP -DGPSC_ASSIST -DGPSM_TOOLKIT_COMM -DGPSC_NL5500 -DFEATURE_PDAPI -DTIMER_HACK
LOCAL_CFLAGS += -DROM_5_10_3_BUILD -DGPS5300 -DGPSC_ULTS -DGPSC_ULTS_DBG  -DGPSC_AUTO
LOCAL_CFLAGS += -DEBTIPS_RELEASE -UGPSC_EXT_MEASPOS -DREPORT_LOG -DTI_DBG -D_DEBUG -fno-short-enums
#LOCAL_CFLAGS += -DTI_LOG_DISABLE
LOCAL_CFLAGS += -DANDROID -DTRAN_DBG -DENABLE_SUPLC_DEBUG -DENABLE_NAL_DEBUG -DMCP_STK_ENABLE
LOCAL_CFLAGS += -DLINUX -D_BYTE_ORDER=_LITTLE_ENDIAN
LOCAL_CFLAGS += -DNO_PWR_MGMT
ifeq ($(WIFI_FEATURE), 'y')
LOCAL_CFLAGS += -DWIFI_ENABLE -DWIFI_DEMO
endif

INAV_ASSIST_FEATURE = 'n'
ifeq ($(INAV_ASSIST_FEATURE), 'y')
se_src_files := \
		    inavc/inavc_main.c \
		    inavc/inavc_thread.c \
		    inavc/inavc_sesyncdrv.c \
		    inavc/inavc_asensor_if.c \
		    inavc/inavc_platform.c \
            	    inavc/clientsocket_comm.c 
navilink_shared_libraries += libins libandroid
LOCAL_CFLAGS += -DENABLE_INAV_ASSIST
LOCAL_CFLAGS += -DUNIFIED_TOOLKIT_COMM
LOCAL_CFLAGS += -USIMULATE_DR
LOCAL_CFLAGS += -DSELIB_VER_2_3
LOCAL_CFLAGS += -DLOG_INPUT
LOCAL_CFLAGS += -DLOG_SENSOR_DATA
LOCAL_CFLAGS += -DGET_ACCEL_BY_NAME
LOCAL_SRC_FILES += $(se_src_files)
endif

ifeq ($(BTIPS_DEBUG),1)
	LOCAL_CFLAGS+= -DMCP_LOGD -DMCP_LOGV -DMCP_LOGI
endif

LOCAL_C_INCLUDES := $(navilink_c_includes)
LOCAL_SHARED_LIBRARIES := $(navilink_shared_libraries)
LOCAL_STATIC_LIBRARIES := $(navilink_static_libraries)

LOCAL_LDLIBS += -lpthread
include $(BUILD_SHARED_LIBRARY)

##
##
## Build navd exe
##
##

include $(CLEAR_VARS)

LOCAL_CFLAGS += -O2 -DANDROID -W -Wall -D_NAVC_APP

ifeq ($(BTIPS_DEBUG),1)
	LOCAL_CFLAGS+= -DMCP_LOGD -DMCP_LOGV -DMCP_LOGI
endif

LOCAL_SRC_FILES:= \
		nav/nav_main.c \
		../MCP_Common/Platform/os/LINUX/android_zoom2/mcp_common_task.c


LOCAL_C_INCLUDES := \
		$(LOCAL_PATH)/../MCP_Common/inc \
		$(LOCAL_PATH)/../MCP_Common/Platform/os/LINUX/common/inc \
		$(LOCAL_PATH)/../MCP_Common/Platform/os/LINUX/android_zoom2/inc \
		$(LOCAL_PATH)/../MCP_Common/Platform/inc \
		$(LOCAL_PATH)/inc \
            $(LOCAL_PATH)/NAVC/Core \
            $(LOCAL_PATH)/WPC  \
        $(LOCAL_PATH)/swlib/inc \
		$(LOCAL_PATH)/NAVC \
		$(LOCAL_PATH)/inavc \
		$(LOCAL_PATH)/services/logm \
		$(LOCAL_PATH)/swlib/inc
		
ifeq ($(INAV_ASSIST_FEATURE), 'y')
LOCAL_CFLAGS += -DENABLE_INAV_ASSIST
endif		
ifeq ($(WIFI_FEATURE), 'y')
LOCAL_CFLAGS += -DWIFI_ENABLE 
endif

LOCAL_STATIC_LIBRARIES := 
		
LOCAL_SHARED_LIBRARIES := liblog libmcphalgps libgps libgpsservices 

ifeq ($(WIFI_FEATURE), 'y')
LOCAL_SHARED_LIBRARIES += libwifipe
endif

LOCAL_LDLIBS := -lpthread -lrt

LOCAL_MODULE:= navd
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_TAGS += debug

include $(BUILD_EXECUTABLE)


include $(call all-makefiles-under,$(LOCAL_PATH))

