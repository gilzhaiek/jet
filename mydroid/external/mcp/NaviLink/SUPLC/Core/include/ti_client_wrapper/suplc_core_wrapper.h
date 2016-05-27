/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_core_wrapper.h
 *
 * Description      :   Common interface between SUPL Core and NaviLink.
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#ifndef __SUPLC_CORE_WRAPPER_H__
#define __SUPLC_CORE_WRAPPER_H__

#ifdef __cplusplus
extern "C" {

#include "android/supl.h"
#endif /* __cplusplus */

#include "mcpf_nal_common.h"
#include "codec/Frequencyinfo.h"
#include "codec/Measuredresultslist.h"
#include "codec/Wcdmacellinformation.h"

/** SUPL command definition.
  * direction GPSM->SUPL */
#define UPL_START_REQ       0x00030000
#define UPL_DATA_REQ        0x00030001
#define UPL_STOP_REQ        0x00030002
#define UPL_NOTIFY_RESP     0x00030003

/* Direction SUPL->GPSM */
#define UPL_DATA_IND        0x00038000
#define UPL_POS_IND         0x00038001
#define UPL_STOP_IND        0x00038002
#define UPL_START_LOC_IND   0x00038003
#define UPL_NOTIFY_IND      0x00038004


/* UPL_START_LOC_IND */
/* resp_opt_bitmap*/
#define START_LOC_QOP_BIT       0x01
#define LOC_FIX_MODE            0x02
#define LOC_FIX_RESULT_TYPE     0x04
#define LOC_FIX_PERIOD          0x08
#define LOC_FIX_NUM_REP         0x10

#define REQUEST_ASSISTANCE      0x01
#define POSITION                0x02
#define QOP                     0x04

/* Requests to NAL */
#define START_SMS_LISTEN    0x01
#define STOP_SMS_LISTEN     0x02


typedef unsigned char   uInt8;
typedef unsigned short  uInt16;
typedef unsigned long   uInt32;

typedef signed char     sInt8;
typedef signed short    sInt16;
typedef signed long     sInt32;

typedef uInt8           boolVal;

enum // pos_technology_bitmap bit meaning
{
    SET_ASSISTED_AGPS_OFFSET    = 0x1,
    SET_BASED_AGPS_OFFSET       = 0x2,
    AUTONOMUS_GPS_OFFSET        = 0x4,
    AFLT_OFFSET                 = 0x8,
    E_CID_OFFSET                = 0x10,
    E_OTD_OFFSET                = 0x20,
    OTDOA_OFFSET                = 0x40
};


#if defined(WINXP)
typedef struct imsi_t
{
    uInt8         imsi_buf[16];
    uInt8         imsi_size;
}Imsi_t;
#endif

/* Preffered method. */
typedef enum prefMethod
{
    AGPS_SET_ASSISTED = 0,
    AGPS_SET_BASED = 1,
    AGPS_NO_PREF = 2
}my_PrefMethod_t;

/** This structure contains parameters that define the SET capabilities
  * in terms of supported positioning technologies and protocols. */
typedef struct setCapabilities_t
{
    uInt8       pos_technology_bitmap;
    uInt8       pos_protocol_bitmap;
    my_PrefMethod_t          pref_method;
}SetCapabilities_t;

/* Network Measurement report. */
typedef struct gsmNmr
{
    uInt16  arfcn;
    uInt8   bsic;
    uInt8  rxlev;
}GsmNMR_t;

typedef struct cellInfoGSM_t
{
    uInt16      gsm_mcc;       // MCC
    uInt16      gsm_mnc;       // MNC
    uInt16      gsm_lac;       // LAC
    uInt16      gsm_ci;        // CI
    uInt8       gsm_opt_param;
    uInt8       gsm_ta;        // timing advance
    uInt8       nmr_quantity;  // count of NMR's
    GsmNMR_t    gsm_nmr[15];   // NMR's array
} my_CellInfoGSM_t;

typedef struct cellInfoWCDMA_t
{
	uInt32 wcdma_mcc;								//MCC
	uInt32 wcdma_mnc;								//MNC
	uInt32 wcdma_ci;								//CI
	uInt8  wcdma_opt_param;
	FrequencyInfo_t *frequencyInfo;				//Frequency Info
	long *primaryScramblingCode;			//Primary Scrambling Code
	MeasuredResultsList_t *measuredResultsList;	//Measured Results List
}my_CellInfoWCDMA_t;

typedef struct cellInfo
{
    enum CELL_TYPE
    {
    	UNKNOWN,
        GSM,
        WCDMA,
        CDMA
    }cell_type;

    my_CellInfoGSM_t cell_info_gsm;
	my_CellInfoWCDMA_t cell_info_wcdma;

}my_CellInfo_t;

/** Describes whether or not the cell info is Not Current last known
  * cell info, Current, the present cell info, Unknown (i.e. not known
  * whether the cell id is current or not current). */
typedef enum cellInfoStatus_t
{
    NO_CELL_INFO = 0,
    PREVIOUS_CELL_INFO = 1,
    PRESENT_CELL_INFO = 2,
    UNKNOWN_CELL_INFO = 3
}CellInfoStatus_t;


/** This structure contains parameters that define the globally unique
  * cell identification of the most  current serving cell. */
typedef struct locationID_t
{
    my_CellInfo_t          cell_info;
    CellInfoStatus_t    cell_info_status;
}LocationID_t;


typedef struct qop_t
{
    uInt8   horr_acc;
    uInt8   qop_optional_bitmap;
    uInt8   ver_acc;
    uInt16  max_loc_age;
    uInt8   delay;
	uInt8   max_response_time;
}my_QoP_t;


typedef struct velocity_t
{
    uInt8   velocity_flag;
    uInt16  verdirect;
    uInt16  bearing;
    uInt16  horspeed;
    uInt16  verspeed;
    uInt16  horuncertspeed;
    uInt16  veruncertspeed;
}my_Velocity_t;

/** This structure contains parameters defining the position of the SET.
  * The parameter also contains a timestamp and optionally the velocity. */
typedef struct position_t
{
    uInt8       UTCTimeStampNumByte;
    uInt8       UTCTimeStamp[20];
    uInt8       latitude_sign;
    uInt8       pos_opt_bitmap;
    uInt8       altitudeDirection;
    uInt8       altUncertainty;
    uInt8       uncertaintySemiMajor;
    uInt8       uncertaintySemiMinor;
    uInt8       orientationMajorAxis;
    uInt8       confidence;
    uInt32      latitude;
    sInt32      longtitude;
    uInt32      altitude;
    my_Velocity_t  velocity;
}my_Position_t;


/* Satellite info element. */
typedef struct satInfo_t
{
    uInt8   satelliteID;
    uInt8   iode;
}SatInfo_t;


typedef struct gpsTime_t //rsuvorov extended ephemeris
{
    uInt16  week;
    uInt8  HourOfWeek;
}my_GPSTime_t;


typedef struct eeData_t //rsuvorov extended ephemeris
{
    uInt16  validity;
    my_GPSTime_t StartCurExt; //SET keep extension from StartCurExt
    my_GPSTime_t EndCurExt;   //SET keep extension to EndCurExt
}EEData_t;


/** This structure contains parameters that define the requested GPS
  * assistance data. The presence of this element indicates that the SET
  * wants to obtain specific GPS assistance data from the SLP. The SET
  * might use this element in any combination of A-GPS SET assisted or
  * A-GPS SET based and  Network initiated or SET initiated positioning. */
typedef struct reqAssistedData_t
{
    uInt16      assitance_req_bitmap;
    uInt16      gpsWeak;
    uInt8       gpsToe;
    uInt8       nSAT;
    uInt8       toeLimit;
    uInt8       num_sat_info;
    SatInfo_t   sat_info_elemnet[31];
    EEData_t    ee_info; //rsuvorov extended ephemeris
}ReqAssistedData_t;

typedef struct startReqData
{
#if defined(WINXP)
    Imsi_t                  imsi;
#endif
    uInt8                   start_opt_param;

    // session supl pos init
    ReqAssistedData_t       a_data;     //Optional


    // session. uid
    uInt16                  app_id;

    // supl global
    SetCapabilities_t       set;

    // location id
    LocationID_t            lac;

    // session (supl start)
    my_QoP_t                   qop;

    // session supl pos
    my_Position_t              position;   //Optional

}StartReqData;


typedef struct controlPdu
{
    uInt16  ctrl_pdu_len;
    uInt8   ctrl_pdu[8192];
}ControlPDU;

/* Positioning Playload */
typedef struct suplPosPayload_t
{
    enum PayloadType
    {
        RRC_PAYLOAD=0,
        RRLP_PAYLOAD,
        TIA_801_PAYLOAD
    } type;

    ControlPDU  payload;
}SUPLPosPayload_t;

/* UPL_DATA_REQ */
typedef struct dataReqData
{
    SUPLPosPayload_t     pos_payload;
    uInt16               app_id;
    uInt8                last_rrlp_packet;   //used for indication of last rrlp packet in session
}DataReqData;


/* UPL_DATA_IND */
typedef struct dataIndData
{
    SUPLPosPayload_t    pos_payload;
    my_Velocity_t          vel;
    uInt16              app_id;
}DataIndData;


typedef struct startLocIndData
{
    uInt16  app_id;
    uInt8   resp_opt_bitmap;
    uInt8   loc_fix_mode;
    uInt16  loc_fix_result_type_bitmap;
    uInt16  loc_fix_period;
    uInt16  loc_fix_num_reports;
    my_QoP_t   qop;
}StartLocIndData;

/* Added for NI */
typedef enum statCode
{
	SUCCESS = 0,
	FAILURE = 1
}StatCode;

typedef struct stopIndData
{
	uInt16	app_id;
	StatCode	gps_stoped;
} StopIndData;


typedef struct stopReqData
{	
	StatCode	gps_stoped;
	uInt16	app_id;
} StopReqData;


/* Callback Function to be called by SUPL Core for sending response packets to NAVC. */

#ifndef __cplusplus
typedef boolVal (*GPSCallBack) (uInt16 appId, uInt32 Command,void *inbuf);
#else
typedef Engine::bool_t (*GPSCallBack) (Engine::uint16_t appId, Engine::uint32_t Command,void *inbuf);
#endif


/**
 * Function:        SUPLC_initClient
 * Brief:           Wrapper Function for calling SUPL Core's SUPL_Init.
 * Description:
 * Note:
 * Params:          gpsCallbackFunc - SUPLC's callback function.
 * Return:          Success: 1.
                    Failure: 0.
 */
uInt8 SUPLC_initClient(GPSCallBack gpsCallbackFunc, void *pNavPtr);

/**
 * Function:        SUPLC_deInitClient
 * Brief:           Wrapper Function for calling SUPL Core's SUPL_DeInit.
 * Description:
 * Note:
 * Params:          None.
 * Return:          Success: 1.
                    Failure: 0.
 */
uInt8 SUPLC_deInitClient();


/**
 * Function:        SUPLC_sendNetworkMessage
 * Brief:           Wrapper Function for calling SUPL Core's HandleMessage.
                    ( Its a Network Component Function. )
 * Description:
 * Note:
 * Params:          body - Network Message.
                    size - Message Size.
 * Return:          Success: 0.
                    Failure: -1.
 */
sInt32 SUPLC_sendNetworkMessage(const char *body, uInt16 size); /* KlockWork Changes */
/* Added for NI */
void SUPLC_passNotificationResult(int res, int tid);


/**
 * Function:        SUPLC_sendUplMsg
 * Brief:           Wrapper Function for calling SUPL Core's SUPL_Control.
 * Description:
 * Note:
 * Params:          u16_appId - App Id.
                    p_data - Pointer to data that has to be sent to Core.
                    u32_command - Command to be sent
 * Return:          Success: 0.
                    Failure: -1.
 */
uInt8 SUPLC_sendUplMsg(uInt16 u16_appId, uInt32 u32_command, uInt8 *p_data);

/**
  * Function:        SUPLC_registerNalHandler
  * Brief:           Wrapper Function for registering NAL Functions with SUPL Core.
  * Description:
  * Note:
  * Params:          p_nalExecuteCommand - Callback Function.
  * Return:          Success: 1.
                     Failure: 0.
*/

uInt8 SUPLC_registerNalHandler(NALCallback p_nalExecuteCommand);

/**
  *  Function:        SUPLC_getGsmCellInfo
  *  Brief:           Gets the cell information.
  *  Description:
  *  Note:
  *  Params:
  *  Return:          Success: 1.
  *                   Failure: 0.
  *
  */

uInt8 SUPLC_getGsmCellInfo(my_CellInfo_t *p_outGsmCellInfo);

#if defined(DOCOMO_SUPPORT_WCDMA)
uInt8 SUPLC_getWcdmaCellInfo(my_CellInfoWCDMA_t *p_outWcdmaCellInfo);
#endif

/**
 * Function:        SUPLC_activateCoreEeClient
 * Brief:           Wrapper Function for calling EE Client's Activate().
 * Description:
 * Note:
 * Params:
 * Return:          Success: 1.
                    Failure: 0.
 */
uInt8 SUPLC_activateCoreEeClient();

/**
 * Function:        SUPLC_eeCallback
 * Brief:           Wrapper Function for calling EE Callback.
 * Description:
 * Note:
 * Params:          u16_appId - App Id.
                    p_data - Pointer to data that has to be sent to Core.
                    u32_command - Command to be sent
 * Return:          Success: 1.
                    Failure: 0.
 */
uInt8 SUPLC_eeCallback(uInt16 u16_appId, uInt32 u32_command, uInt8 *p_data);

/**
 * Function:        SUPLC_sendConnectionResult
 * Brief:           Wrapper Function for calling Handle Server Connection.
 * Description:
 * Note:
 * Params:          conRes - Connection Result.
 * Return:          Success: 1.
                    Failure: 0.
 */

sInt32 SUPLC_sendConnectionResult(uInt8 conRes);


/**
 * Function:        SUPLC_RemoveQueuedUplMsgs
 * Brief:           Wrapper Function for calling SUPL Core's SUPL_Control.
 * Description:
 * Note:
 */

void SUPLC_RemoveQueuedUplMsgs();
#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __SUPLC_CORE_WRAPPER_H__ */
