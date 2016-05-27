/******************************************************************************\
##                                                                            *
## Unpublished Proprietary and Confidential Information of Texas Instruments  *
## Israel Ltd. Do Not Disclose.                                               *
## Copyright 2008 Texas Instruments Israel Ltd.                               *
## All rights reserved. All unpublished rights reserved.                      *
##                                                                            *
## No part of this work may be used or reproduced in any form or by any       *
## means, or stored in a database or retrieval system, without prior written  *
## permission of Texas Instruments Israel Ltd. or its parent company Texas    *
## Instruments Incorporated.                                                  *
## Use of this work is subject to a license from Texas Instruments Israel     *
## Ltd. or its parent company Texas Instruments Incorporated.                 *
##                                                                            *
## This work contains Texas Instruments Israel Ltd. confidential and          *
## proprietary information which is protected by copyright, trade secret,     *
## trademark and other intellectual property rights.                          *
##                                                                            *
## The United States, Israel  and other countries maintain controls on the    *
## export and/or import of cryptographic items and technology. Unless prior   *
## authorization is obtained from the U.S. Department of Commerce and the     *
## Israeli Government, you shall not export, reexport, or release, directly   *
## or indirectly, any technology, software, or software source code received  *
## from Texas Instruments Incorporated (TI) or Texas Instruments Israel,      *
## or export, directly or indirectly, any direct product of such technology,  *
## software, or software source code to any destination or country to which   *
## the export, reexport or release of the technology, software, software      *
## source code, or direct product is prohibited by the EAR. The subject items *
## are classified as encryption items under Part 740.17 of the Commerce       *
## Control List (CCL). The assurances provided for herein are furnished in  *
## compliance with the specific encryption controls set forth in Part 740.17  *
## of the EAR -Encryption Commodities and Software (ENC).                     *
##                                                                            *
## NOTE: THE TRANSFER OF THE TECHNICAL INFORMATION IS BEING MADE UNDER AN     *
## EXPORT LICENSE ISSUED BY THE ISRAELI GOVERNMENT AND THE APPLICABLE EXPORT  *
## LICENSE DOES NOT ALLOW THE TECHNICAL INFORMATION TO BE USED FOR THE        *
## MODIFICATION OF THE BT ENCRYPTION OR THE DEVELOPMENT OF ANY NEW ENCRYPTION.*
## UNDER THE ISRAELI GOVERNMENT'S EXPORT LICENSE, THE INFORMATION CAN BE USED *
## FOR THE INTERNAL DESIGN AND MANUFACTURE OF TI PRODUCTS THAT WILL CONTAIN   *
## THE BT IC.                                                                 *
##                                                                            *
\******************************************************************************/

#ifndef __NAVILINK_CUSTOM_H
#define __NAVILINK_CUSTOM_H

/******* MCPF type  definitions --- start**************** */
/* -------------------------------------------------------------
*					Platform-Depndent Part							
*																
* 		SET THE VALUES OF THE FOLLOWING PRE-PROCESSOR 			
*		DEFINITIONS 	TO THE VALUES THAT APPLY TO THE 				
*		TARGET PLATFORM											
*																
*/

/* Size of type (char) in the target platform, in bytes	*/
#define MCP_CHAR_SIZE		(1)

/* Size of type (short) in the target platform, in bytes */
#define MCP_SHORT_SIZE 	    (2)

/* Size of type (long) in the target platform, in bytes	*/
#define MCP_LONG_SIZE 		(4)

/* Size of type (int) in the target platform, in bytes	*/
#define MCP_INT_SIZE 		(4)

/* -------------------------------------------------------------
*					8 Bits Types
*/
#if MCP_CHAR_SIZE == 1

typedef unsigned char McpU8;
typedef signed char McpS8;

#elif MCP_SHORT_SIZE == 1

typedef unsigned short McpU8;
typedef short McpS8;

#elif MCP_INT_SIZE == 1

typedef unsigned int McpU8;
typedef int McpS8;

#else

#error Unable to define 8-bits basic types!

#endif

/* -------------------------------------------------------------
*					16 Bits Types
*/
#if MCP_SHORT_SIZE == 2

typedef unsigned short McpU16;
typedef short McpS16;

#elif MCP_INT_SIZE == 2

typedef unsigned int McpU16;
typedef int McpS16;

#else

#error Unable to define 16-bits basic types!

#endif

/* -------------------------------------------------------------
*					32 Bits Types
*/
#if MCP_INT_SIZE == 4

typedef unsigned int McpU32;
typedef int McpS32;

#elif MCP_LONG_SIZE == 4

typedef unsigned long McpU32;
typedef long McpS32;

#else

#error Unable to define 32-bits basic types!

#endif

/* -------------------------------------------------------------
*			Native Float and Double Types (# of bits irrelevant)
*/

typedef float McpFLT;
typedef double McpDBL;

/* -------------------------------------------------------------
*			Native Integer Types (# of bits irrelevant)
*/
typedef int McpInt;
typedef unsigned int McpUint;


/* -------------------------------------------------------------
*					UTF8,16 types
*/
typedef McpU8 McpUtf8;
typedef McpU16 McpUtf16;

/* --------------------------------------------------------------
*					Boolean Definitions							 
*/
typedef McpInt McpBool;

#define MCP_TRUE  (1 == 1)
#define MCP_FALSE (0==1) 

/* --------------------------------------------------------------
*					Null Definition							 
*/
#ifndef NULL
#define NULL    0
#endif

/* -------------------------------------------------------------
*					LIMITS						
*/

#define	MCP_U8_MAX			((McpU8)0xFF)
#define	MCP_U16_MAX			((McpU16)0xFFFF)
#define	MCP_U32_MAX			((McpU32)0xFFFFFFFF)

#if MCP_INT_SIZE == 4

#define MCP_UINT_MAX			(MCP_U32_MAX)

#elif MCP_INT_SIZE == 2

#define MCP_UINT_MAX			(MCP_U16_MAX)

#endif

#define GPSC_MAX_COMPATIBLE_VERSIONS (0x4)

/* Types Definition */
typedef void * handle_t; /* abstraction for module/component context */

/******************MCPF Type definitions END *******************/

/*****************HOST SOCKET MESSAGING FORMATS START****************/

/************************************************************************/
/*						Definitions                                     */
/************************************************************************/
#define   HOSTSOCKET_MESSAGE_HEADER_SIZE	8
#define	HOSTSOCKET_MESSAGE_SYNC_START	0xF3F2F1F0


/************************************************************************/
/*						Enums & Structures                              */

/************************************************************************/
typedef enum
{
    HOSTSOCK_WAIT_HEADER,
    HOSTSOCK_WAIT_PAYLOAD
} hostSocket_clinetState_e;

typedef struct
{
    McpU32 syncStart;
    McpU8 msgClass;
    McpU8 opCode;
    McpU16 payloadLen;
} hostSocket_header_t;


/*****************HOST SOCKET MESSAGING FORMATS END****************/




/******************NAVC DEFINITIONS Start**********************************/

/************************************************************************
* Defines
************************************************************************/

#define NAVC_EVT_HEADER_SIZE	(sizeof(ENAVC_cmdOpcode) + sizeof(EMcpfRes))
#define GPSC_MAX_STRING                (0x1e0)    
#define GPSC_PROT_MAX_SV_MEASUREMENT   (0xc)      
#define GPSC_RAW_MAX_SV_REPORT         (0x11)           
#define N_SV_15							15
#define MAX_VERTICES					10 //ashish

/* Assistance availibility flags */
#define C_ALM_AVAIL				0x0001
#define C_UTC_AVAIL				0x0002
#define C_IONO_AVAIL			0x0004
#define C_EPH_AVAIL				0x0008
#define C_DGPS_AVAIL			0x0010
#define C_REFLOC_AVAIL			0x0020
#define C_REF_GPSTIME_AVAIL		0x0040 
#define C_ACQ_AVAIL				0x0080
#define C_RTI_AVAIL				0x0100
#define C_ANGLE_AVAIL			0x0400
#define C_REF_GSMTIME_AVAIL		0x0800
#define C_REF_TOW_AVAIL			0x1000 
#define C_TCXO_AVAIL			0x2000 
#define C_DL_RX_BUF_LENGTH		2048

/***********************************************************/
/* NAVC_CMD_REG_FOR_ASSISTANCE command parameters                           */
/***********************************************************/
#define NAVC_REG_EPH                0x0001              /* Register for Ephimeris */
#define NAVC_REG_LOC                0x0002              /* Register for Position */ /*(NOT SUPPORTED)*/ 
#define NAVC_REG_GPSTIME            0x0004              /* Register for GPS TIME */
#define NAVC_REG_ALM                0x0008              /* Register for Almanac */ /*(NOT SUPPORTED)*/
#define NAVC_REG_UTC                0x0010              /* Register for UTC */ /*(NOT SUPPORTED)*/
#define NAVC_REG_IONO               0x0020              /* Register for IONO */ /*(NOT SUPPORTED)*/

typedef struct
{
    McpU16            AssistBitmap;                     /* bit map indicating the registration for a particular assistance */
}TNAVC_Reg_For_Assist;

typedef enum
{
    RES_COMPLETE, /* synchronous operation (completed successfully) */
    RES_OK = RES_COMPLETE,
    RES_PENDING, /* a-synchronous operation (completion event will follow) */
    RES_ERROR,
    RES_MEM_ERROR, /* failure in memory allocation */
    RES_FILE_ERROR, /* failure in file access */
    RES_STATE_ERROR, /* invalid state */
    RES_UNKNOWN_OPCODE /* unknown opcode */
} EMcpfRes;


/***********************************************************/
/* NAVC_CMD_SET_LOG_MODE command parameters             */
/***********************************************************/

/*
* Individual control of Logger, FromSensor and ToSensor logs.
*
*/
typedef struct
{
    McpU8 navc_log_mode; /*ENAVC_LogMode_Control for navc internal logger data*/
    McpU8 tosensor_mode; /*ENAVC_LogMode_Control for ToSensor data */
    McpU8 fromsensor_mode; /*ENAVC_LogMode_Control for FromSensor data */
} TNAVC_LogMode;

/***********************************************************/
/* NAVC_CMD_SET_WIFI_POSITION command parameters             */

/***********************************************************/

typedef struct
{
    McpU8 valid;
    McpDBL wifi_latitude;
    McpDBL wifi_longitude;
    McpDBL wifi_altitude;
    McpDBL wifi_EastUnc; /*In metres*/
    McpDBL wifi_NorthUnc; /*In metres*/
    McpDBL wifi_EastVel; /* In m/s */
    McpDBL wifi_NorthVel; /* In m/s */
    McpDBL wifi_EastVelUnc; /* In m/s */
    McpDBL wifi_NorthVelUnc; /* In m/s */
    McpDBL wifi_unc; /*2D Unc*/
    McpU8 num_of_ap_used; /* number of APs used for WPC positioning */
    McpU32 time_of_wifiposition; /* time tag when the fix obtained */
    McpU8 FixAvailable; /* indicate 0:no fix  1: Fix Availabel*/
    McpU8 FixPropagated; /* Indicate 0: Actual fix  1: Propagated */
    McpU8 FixFromKalmanFilter; /* Indicate 0: Centroid Fix 1: KalmanFilter*/
}
TNAVC_wifiPosition;

typedef struct velocity
{
    McpU8	velocity_flag;
    McpU16	verdirect;
    McpU16	bearing;
    McpU16	horspeed;
    McpU16	verspeed;
    McpU16	horuncertspeed;
    McpU16	veruncertspeed;
}my_Velocity;

typedef struct position
{
    McpU8		UTCTimeStampNumByte;
    McpU8		UTCTimeStamp[20];
    McpU8		latitude_sign;
    McpU8		pos_opt_bitmap;
    McpU8		altitudeDirection;
    McpU8		altUncertainty;
    McpU8		uncertaintySemiMajor;
    McpU8		uncertaintySemiMinor;
    McpU8		orientationMajorAxis;
    McpU8		confidence;
    McpU32		latitude;
    McpU32		longtitude;
    McpU32		altitude;
    my_Velocity  velocity;
}my_Position;

typedef struct posIndData
{
    my_Position position;
    McpU16	  app_id;
}TNAVC_posIndData;

/*  NAVC command opcodes definition */
typedef enum
{
    NAVC_CMD_START 				  	= 0x01,
    NAVC_CMD_STOP,
    NAVC_CMD_REQUEST_FIX		  	= 0x10,
    NAVC_CMD_STOP_FIX,
    NAVC_CMD_INJECT_ASSISTANCE	  	= 0x20,//32
    NAVC_CMD_COMPLETE_ASSISTANCE,
    NAVC_CMD_DELETE_ASISTANCE,
    NAVC_CMD_GET_ASSIST_EPH,
    NAVC_CMD_GET_ASSIST_IONO,
    NAVC_CMD_GET_ASSIST_ALMANAC,
    NAVC_CMD_GET_ASSIST_TIME,
    NAVC_CMD_GET_ASSIST_POSITION,
    NAVC_CMD_GET_SV_STATUS,
    NAVC_CMD_INJECT_CONFIGURATION 	= 0x30,//48
    NAVC_CMD_SET_HOST_WAKEUP_PARAMS,
    NAVC_CMD_SET_APM_PARAMS,
    NAVC_CMD_SET_SBAS_PARAMS,
    NAVC_CMD_SET_MOTION_MASK,
    NAVC_CMD_ENABLE_KALMAN_FILTER,
    NAVC_CMD_READ_CONFIGURATION,
    NAVC_CMD_SET_REFCLK_PARAMETER,
    NAVC_CMD_SET_TOW,
    NAVC_CMD_PLT,
    NAVC_CMD_GET_VERSION,
    NAVC_CMD_INJECT_CALIB_CTRL,
    NAVC_CMD_GET_MOTION_MASK, //ashish
    //NAVC_CMD_SET_GEOFENCE,//60
    NAVC_CMD_SAGPS_PROVIDER_REGISTER,
    NAVC_CMD_SUPL_PROVIDER_REGISTER,
    NAVC_CMD_GPS_SLEEP,
    NAVC_CMD_GPS_WAKEUP,
    NAVC_CMD_TO_SHUTDOWN,
    NAVC_CMD_STOP_FOREVER,
    NAVC_CMD_GET_ASSIST_UTC,
    NAVC_CMD_GET_ASSIST_ACQ,
    NAVC_CMD_GET_ASSIST_RTI,
    NAVC_CMD_GET_ASSIST_TTL,//70
    NAVC_CMD_GET_ASSIST_DGPS,
    NAVC_CMD_GET_ASSIST_TCXO,
    NAVC_CMD_GET_WISHLIST,
    NAVC_CMD_GET_ASSISTANCE_NEEDED,
    NAVC_CMD_INJ_TCXO,
    NAVC_CMD_DELETE_GENERAL_ASISTANCE,
    NAVC_CMD_DELETE_EPH_ASISTANCE,
    NAVC_CMD_DELETE_ALM_ASISTANCE,
    NAVC_CMD_START_CNF,
    NAVC_CMD_REGISTER_ASSIST_SRC,//80
    NAVC_CMD_GET_ASSIST_PRIORITY,
    NAVC_CMD_SET_ASSIST_PRIORITY,
    NAVC_CMD_ENTER_FWD_MODE,
    NAVC_CMD_EXIT_FWD_MODE,
    NAVC_CMD_FWD_TX_DATA,
    NAVC_CMD_SW_RESET,
    NAVC_CMD_SAVE_ASSISTANCE,
    NAVC_CMD_PLT_NON_CW,
    NAVC_CMD_SET_LOG_MODE,
    NAVC_CMD_GET_AGC, //90
    NAVC_SUPLC_CMD_START,
    NAVC_SUPLC_CMD_STOP,
    NAVC_CMD_BLF_STATUS,
    NAVC_CMD_BLF_BUFFER_DUMP,
    NAVC_CMD_SET_WIFI_POSITION,
    NAVC_CMD_POS_IND,
    NAVC_CMD_CHECK_INIT_STATE,
    NAVC_CMD_INAV_CTRL, //Inertial Navigation Control
    NAVC_CMD_SIMULATE_DR,
    NAVC_CMD_SUPLC_SESSION_RESULT, //100
    NAVC_CMD_BLF_CONFIG,
    NAVC_CMD_QOP_TIMEOUT,
    NAVC_CMD_WPL_REGISTER,
    NAVC_CMD_REG_FOR_ASSISTANCE
} ENAVC_cmdOpcode;

/*  NAVC event opcodes definition */
typedef enum
{
    NAVC_EVT_CMD_COMPLETE = 0x01,
    NAVC_EVT_POSITION_FIX_REPORT,
    NAVC_EVT_ASSISTANCE_REQUEST,
    NAVC_EVT_REQUEST_PULSE,
    NAVC_EVT_RRLP_ACK,
    NAVC_EVT_RRC_ACK,
    NAVC_EVT_RRLP_RESP,
    NAVC_EVT_RRC_RESP,
    NAVC_EVT_VERSION_RESPONSE,
    NAVC_EVT_PLT_RESPONSE,
    NAVC_EVT_BLF_REP_STATUS,
    NAVC_EVT_ASSIST_REP_EPH,
    NAVC_EVT_ASSIST_REP_IONO,
    NAVC_EVT_ASSIST_REP_ALMANAC,
    NAVC_EVT_ASSIST_REP_TIME,
    NAVC_EVT_ASSIST_REP_POSITION,
    NAVC_EVT_SV_STATUS_REPORT,
    NAVC_EVT_ASSIST_REP_UTC,
    NAVC_EVT_ASSIST_REP_RTI,
    NAVC_EVT_ASSIST_REP_TTL,
    NAVC_EVT_ASSIST_REP_SLEEP,
    NAVC_EVT_ASSIST_REP_WAKEUP,
    NAVC_EVT_ASSIST_DEL_RES,
    NAVC_EVT_ASSIST_REP_ACQ,
    NAVC_EVT_INJECT_FREQ_EST_RES,
    NAVC_EVT_ASSIST_SRC_REP_PRIORITY,
    NAVC_EVT_REFCLK_REQ,
    NAVC_EVT_FWD_RX_DATA,
    NAVC_EVT_ASSIST_REP_TOW,
    NAVC_EVT_FATAL_ERR,//90
    NAVC_EVT_AGC_RESP,
    NAVC_EVT_MOTION_MASK_STATUS, //ashish
    NAVC_EVT_MOTION_MASK_SETTINGS, //ashish
    NAVC_EVT_GPSC_IN_ACTIVE_STATE,
    NAVC_EVT_GPS_WIFI_BLEND_REPORT,   //dec 34 and  in hex 0x22
    NAVC_EVT_STOP_REQUEST
} ENAVC_evtOpcode;

/* NAVC parameter types depending on command/event opcode */

/***********************************************************/
/* NAVC_CMD_START command parameters                       */
/***********************************************************/
/* Action Q command - no parameters */

/***********************************************************/
/* NAVC_CMD_STOP command parameters                        */
/***********************************************************/
/* Action Q command - no parameters */

/***********************************************************/
/* NAVC_CMD_REQUEST_FIX command parameters                 */

/***********************************************************/
typedef enum
{
    GPSC_FIXMODE_AUTONOMOUS = 0x0, /* Perform Autonomous position fix */
    GPSC_FIXMODE_MSBASED = 0x1, /* Perform MS Based position fix  */
    GPSC_FIXMODE_MSASSISTED = 0x2 /* Perform MS Assisted position fix */
} T_GPSC_loc_fix_mode;

typedef McpU32 EGPSC_LocFixMode;

typedef struct
{
  McpU16 blf_fix_count;
  McpU32 uSessionId;
  McpU16 loc_fix_result_type_bitmap;          /*  Location Fix Result Type Bitmap */
  EGPSC_LocFixMode loc_fix_mode;              /*  Location Fix Mode (T_GPSC_loc_fix_mode) */
  McpU16 loc_fix_max_ttff;          	      /*  Location fix max TTFF, seconds, 0xFFFF = no max TTFF, 0x0000 = unknown */
  McpU16 loc_fix_num_reports;       	      /*  Number of reports in loc fix request */
  McpU16 loc_fix_period;            	      /*  Location fix period */
}TNAVC_BLF_Session_Config;

/*
* Location fix Quality of Position. A set of attributes associated with a request for the geographic position of SET. The attributes include the required horizontal accuracy, vertical accuracy, max location age, and response time of the SET position.
* CCDGEN:WriteStruct_Count==30
*/
typedef struct
{
    McpU8 qop_flag; /* Presently not being used ,This flag indicates if a set of attributes associated with a request for the geographic position of SET is made. */
    McpU8 delay_n; /* Presently not being used ,Delay N. 2^N, N from (0..7), unit is seconds       */
    McpU16 horizontal_accuracy; /* Horizontal Accuracy. This describes the uncertainty for latitude and longitude, set OxFF if not in use*/
    McpU16 vertical_accuracy; /* Presently not being used Vertical Accuracy.This describes the uncertainity in the altitude. */
    McpU16 max_response_time; /* Maximum response time set by the network. Units in seconds from 0 to 65535. set OXFFFF if not being used */
} T_GPSC_loc_fix_qop;

typedef struct
{
    McpU8   horr_acc;
    McpU8   qop_optional_bitmap;
    McpU8   ver_acc;
    McpU16  max_loc_age;
    McpU8   delay;
    McpU8   max_response_time;
}T_GPSC_supl_qop;

/*
* Location fix Velocity. Velocity may be associated with a universal Geographical Area Description when both are applied to a common entity at a common time.
* CCDGEN:WriteStruct_Count==31
*/
typedef struct
{
    McpU8 velocity_flag; /* This flag indicates the velocity with a universal Geographical Area Description when both are applied to a common entity at a common time. */
    McpU8 zzz_align0; /* alignment */
    McpU16 vertical_direction; /* Vertical speed direction is encoded using 1 bit: a bit value of 0 indicates upward speed; a bit value of 1 indicates downward speed. */
    McpU16 Bearing; /* Bearing is encoded in increments of 1 degree measured clockwise from North using a 9 bit binary coded number N. */
    McpU16 horizontal_speed; /* Horizontal speed is encoded in increments of 1 kilometre per hour using a 16 bit binary coded number N. */
    McpU16 vertical_speed; /* Vertical speed is encoded in increments of 1 kilometre per hour using 8 bits giving a number N between 0 and 28-1. */
    McpU16 horizontal_speed_uncertainity; /* Uncertainty speed is encoded in increments of 1 kilometre per hour using an 8 bit binary coded number N. The value of N gives the uncertainty speed except for N=255 which indicates that the uncertainty is not specified. */
    McpU16 vertical_speed_uncertainity; /* Uncertainty speed is encoded in increments of 1 kilometre per hour using an 8 bit binary coded number N. The value of N gives the uncertainty speed except for N=255 which indicates that the uncertainty is not specified. */
    McpU8 zzz_align1[2]; /* alignment */
} T_GPSC_loc_fix_velocity;

typedef enum
{
    GPSC_RESULT_NMEA_GGA = 0x1, /* NMEA GGA output selected. Ignored when mode is MS-Assisted. */
    GPSC_RESULT_NMEA_GLL = 0x2, /* NMEA GLL output selected. Ignored when mode is MS-Assisted. */
    GPSC_RESULT_NMEA_GSA = 0x4, /* NMEA GSA output selected. Ignored when mode is MS-Assisted. */
    GPSC_RESULT_NMEA_GSV = 0x8, /* NMEA GSV output selected. Ignored when mode is MS-Assisted. */
    GPSC_RESULT_NMEA_RMC = 0x10, /* NMEA RMC output selected. Ignored when mode is MS-Assisted. */
    GPSC_RESULT_NMEA_VTG = 0x20, /* NMEA VTG output selected. Ignored when mode is MS-Assisted. */
    GPSC_RESULT_RAW = 0x1000, /* Position (MS-Based or Autonomous) or measurement data (MS-Assisted) output in native GPS baseband format. */
    GPSC_RESULT_PROT = 0x2000 /* Position (MS-Based or Autonomous) or measurement data (MS-Assisted) output in protocol format. */
} T_GPSC_loc_fix_result_type_bitmap;

typedef struct
{
    EGPSC_LocFixMode loc_fix_mode; /*  Location Fix Mode (T_GPSC_loc_fix_mode) */
    McpU16 loc_fix_result_type_bitmap; /*  Location Fix Result Type Bitmap */
    McpU16 loc_fix_num_reports; /*  Number of reports in loc fix request */
    McpU16 loc_fix_period; /*  Location fix period */
    McpU16 loc_fix_max_ttff; /*  Location fix max TTFF, seconds, 0xFFFF = no max TTFF, 0x0000 = unknown */
    McpU32 loc_fix_session_id; /*  The session tag ID */
    T_GPSC_loc_fix_qop loc_fix_qop; /*  Location fix Quality of Position. A set of attributes associated with a request for the geographic position of SET. The attributes include the required horizontal accuracy, vertical accuracy, max location age, and response time of the SET position. */
    T_GPSC_loc_fix_velocity loc_fix_velocity; /*  Velocity may be associated with a universal Geographical Area Description when both are applied to a common entity at a common time. */
} TNAVC_reqLocFix; /* Replaces  T_GPSC_loc_fix_req_params */

/***********************************************************/
/* NAVC_CMD_STOP_FIX command parameters                       */

/***********************************************************/
typedef struct
{
    McpU32 uSessionId;

} TNAVC_stopLocFix;

/***********************************************************/
/* NAVC_CMD_INJECT_ASSISTANCE command parameters           */
/***********************************************************/

/*
* enum to UnionController assistance_inject_union
*/
typedef enum
{
    GPSC_ASSIST_ACQ = 0x0,
    GPSC_ASSIST_EPH = 0x1,
    GPSC_ASSIST_IONO = 0x2,
    GPSC_ASSIST_UTC = 0x3,
    GPSC_ASSIST_DGPS = 0x4,
    GPSC_ASSIST_ALMANAC = 0x5,
    GPSC_ASSIST_TOW = 0x6,
    GPSC_ASSIST_RTI = 0x7,
    GPSC_ASSIST_TIME = 0x8,
    GPSC_ASSIST_POSITION = 0x9,
    GPSC_ASSIST_COMPL_SET = 0xa
} T_GPSC_ctrl_assistance_inject_union;
typedef McpU32 EGPSC_AssistanceType;
;

/*
* Acquisition assistance data
*/
typedef struct
{
    McpU8 svid; /* Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64)     */
    McpU8 zzz_align0[3]; /* alignment                                               */
    McpU32 gps_tow; /* GPS TOW                                                 */
    McpS16 aa_doppler0; /* -5,120 - 5,120.5 Hz. Resolution 2.5Hz                   */
    McpU8 v_aa_doppler1; /* valid-flag                                              */
    McpU8 aa_doppler1; /* -1 - 0.5                                                */
    McpU8 v_aa_doppler_uncertainty; /* valid-flag                                   */
    McpU8 aa_doppler_uncertainty; /* T_GPSC_aa_doppler_uncertainty,  Range: 12.5Hz - 200Hz. Formula: 200 * 2-n Hz, n = 0..4 */
    McpU16 aa_code_phase; /* T_GPSC_aa_code_phase,  0 - 1022 chips; resolution 1 chip */
    McpU8 aa_int_code_phase; /* T_GPSC_aa_int_code_phase,  0 - 19            */
    McpU8 aa_gps_bit_number; /* 0 - 3                                        */
    McpU8 aa_code_phase_search_window; /* T_GPSC_aa_code_phase_search_window,  1 - 192 chips */
    McpU8 v_aa_azimuth; /* valid-flag                                   */
    McpU8 aa_azimuth; /* T_GPSC_aa_azimuth,  0 - 348.75 deg. Resolution 11.25 deg */
    McpU8 v_aa_elevation; /* valid-flag                                   */
    McpU8 aa_elevation; /* T_GPSC_aa_elevation,  0 - 78.75 deg. Resolution 11.25 deg */
    McpU8 zzz_align1[1]; /* alignment                                    */
} T_GPSC_acquisition_assist;

/*
* Ephemeris assistance data
*/
typedef struct
{
    McpU8    svid;                     /* Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64) */
    McpU8    iode;                     /* ICD-200                                            */
    McpS16   ephem_crs;                /* ICD-200                                            */
    McpS16   ephem_delta_n;            /* ICD-200                                            */
    McpS16   ephem_cuc;                /* ICD-200                                            */
    McpS32   ephem_m0;                 /* ICD-200                                            */
    McpU32   ephem_e;                  /* ICD-200                                            */
    McpU32   ephem_a_power_half;       /* ICD-200                                            */
    McpS32   ephem_omega_a0;           /* ICD-200                                            */
    McpS16   ephem_cus;                /* ICD-200                                            */
    McpU16   ephem_toe;                /* ICD-200                                            */
    McpS16   ephem_cic;                /* ICD-200                                            */
    McpS16   ephem_cis;                /* ICD-200                                            */
    McpS32   ephem_i0;                 /* ICD-200                                            */
    McpS32   ephem_w;                  /* ICD-200                                            */
    McpS32   ephem_omega_adot;         /* ICD-200                                            */
    McpS16   ephem_crc;                /* ICD-200                                            */
    McpS16   ephem_idot;               /* ICD-200                                            */
    McpU8    ephem_code_on_l2;         /* ICD-200                                            */
    McpU8    ephem_ura;                /* ICD-200                                            */
    McpU8    ephem_svhealth;           /* ICD-200                                            */
    McpS8    ephem_tgd;                /* ICD-200                                            */
    McpU16   ephem_iodc;               /* ICD-200                                            */
    McpU16   ephem_toc;                /* ICD-200                                            */
    McpS8    ephem_af2;                /* ICD-200                                            */
    McpU8    zzz_align0;               /* alignment                                          */
    McpS16   ephem_af1;                /* ICD-200                                            */
    McpS32   ephem_af0;                /* ICD-200                                            */
    McpU8	 ephem_predicted;		   /* Set to TRUE if the ephemeris is predicted, FALSE if it is decoded from sky */
    McpU8    ephem_predSeedAge;	       /* Used for storing the Ephemeris seed age incase of Prediceted Aphemeris*/
    McpU16   ephem_week;		       /* ICD-200, 65535 if unknown */
} T_GPSC_ephemeris_assist;

typedef T_GPSC_ephemeris_assist TNAVC_assistEphemeris;

/*
* Ionospheric assistance data
*/
typedef struct
{
    McpS8 iono_alfa0; /* ICD-200                                            */
    McpS8 iono_alfa1; /* ICD-200                                            */
    McpS8 iono_alfa2; /* ICD-200                                            */
    McpS8 iono_alfa3; /* ICD-200                                            */
    McpS8 iono_beta0; /* ICD-200                                            */
    McpS8 iono_beta1; /* ICD-200                                            */
    McpS8 iono_beta2; /* ICD-200                                            */
    McpS8 iono_beta3; /* ICD-200                                            */
    McpU8 v_iono_assist; /* 0: invalid, 1: valid iono data */
} T_GPSC_iono_assist;

/*
* UTC assistance data
*/
typedef struct
{
    McpS32 utc_a1; /* ICD-200                                            */
    McpS32 utc_a0; /* ICD-200                                            */
    McpU8 utc_tot; /* ICD-200                                            */
    McpU8 utc_wnt; /* ICD-200                                            */
    McpS8 utc_delta_tls; /* ICD-200                                            */
    McpU8 utc_wnlsf; /* ICD-200                                            */
    McpS8 utc_dn; /* ICD-200                                            */
    McpS8 utc_delta_tlsf; /* ICD-200                                            */
    McpU8 utc_valid; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
} T_GPSC_utc_assist;

/*
* DGPS assistance data
*/
typedef struct
{
    McpU8 svid; /*Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64) */
    McpU8 iode; /*ICD-200                                            */
    McpU8 dgps_udre; /*Range: 0 to 3                                      */
    McpU8 zzz_align0; /*alignment                                          */
    McpS32 dgps_pseudo_range_cor; /*Range is -655.04  to +655.04 meters                */
    McpS16 dgps_range_rate_cor; /*Range is -4.064 to ???4.064 meters/sec             */
    McpS8 dgps_deltapseudo_range_cor2; /*Presently  this is not being used                  */
    McpS8 dgps_deltarange_rate_cor2; /*Presently  this is not being used                  */
    McpS8 dgps_deltapseudo_range_cor3; /*Presently  this is not being used                  */
    McpS8 dgps_deltarange_rate_cor3; /*Presently  this is not being used                  */
    McpU8 zzz_align1[2]; /*alignment                                          */
} T_GPSC_dgps;

/*
* DGPS assistance data
*/
typedef struct
{
    McpU32 gps_tow; /* GPS TOW                                            */
    McpU8 dgps_status; /* Range: 0 to 7                                      */
    McpU8 dgps_Nsat; /* Range: 0 to 16                                     */
    McpU8 zzz_align0[2]; /* alignment                                          */
    T_GPSC_dgps dgps; /* DGPS assistance data                               */
} T_GPSC_dgps_assist;

/*
* Almanac assistance data
*/
typedef struct
{
    McpU8 svid; /* Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64) */
    McpU8 almanac_svhealth; /* ICD-200                                            */
    McpU16 almanac_week; /* ICD-200                                            */
    McpU8 almanac_toa; /* ICD-200                                            */
    McpU8 zzz_align0; /* alignment                                          */
    McpU16 almanac_e; /* ICD-200                                            */
    McpS16 almanac_ksii; /* ICD-200                                            */
    McpS16 almanac_omega_dot; /* ICD-200                                            */
    McpU32 almanac_a_power_half; /* ICD-200                                            */
    McpS32 almanac_omega0; /* ICD-200                                            */
    McpS32 almanac_w; /* ICD-200                                            */
    McpS32 almanac_m0; /* ICD-200                                            */
    McpS16 almanac_af0; /* ICD-200                                            */
    McpS16 almanac_af1; /* ICD-200                                            */
} T_GPSC_almanac_assist;

/*
* TOW assistance data
*/
typedef struct
{
    McpU8 svid; /* Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64) */
    McpU8 zzz_align0; /* alignment                                          */
    McpU16 towa_tlm_word; /* 14-bit value representing the Telemetry Message (TLM) begin broadcast by the GPS satellite */
    McpU8 towa_anti_spoof_flag; /* The Anti-Spoof flag                                */
    McpU8 towa_alert_flag; /* The Alert flag                                     */
    McpU8 towa_tlm_reserved_bits; /* The two reserved bits in the TLM word. The MSB occurs first in the satellite transmission. */
    McpU8 zzz_align1; /* alignment                                          */
} T_GPSC_tow_assist;

/*
* RTI assistance data
*/
typedef struct
{
    McpU32 rti_bitmask[2]; /* Satellite integrity bitmap (set bit means omm.el)  */
} T_GPSC_rti_assist;

typedef enum
{
    GPSC_TIME_ACCURACY_FINE_ARBITRARY_REFFERENCE = 0x0, /* Fine/Pulse mode with arbitary reference scheme. */
    GPSC_TIME_ACCURACY_COARSE = 0x1, /* Coarse mode for time           */
    GPSC_TIME_ACCURACY_FINE_LAST_PULSE = 0x2 /* Fine/Pulse mode for time       */
} T_GPSC_time_accuracy;
typedef McpU32 EGPSC_TimeAccuracy;

/*
* Time of the week data
*/
typedef struct
{
  McpU16               gps_week;                 /* GPS week number                                    */
  McpU8                zzz_align[2];             /* alignment                                          */
  McpU32               gps_msec;                 /* GPS milliseconds                                   */
  McpU32               sub_ms;                   /* GPS sub-milliseconds - 1 bit = 1 microsecond       */
  McpU32               time_unc;                 /* Uncertainty in nanoseconds                         */
  EGPSC_TimeAccuracy   time_accuracy;            /* T_GPSC_time_accuracy Mode of time injection/Report */
} T_GPSC_time_assist;

/*
* Position assistance data
*/
typedef struct
{
    McpU32 latitude; /*<  0:  4> 0..2^23, LSB = (2^23)/90 degrees                   */
    McpS32 longitude; /*<  4:  4> 0..2^24, LSB = (2^24)/360 degrees                  */
    McpU16 altitude; /*<  8:  2> 0..2^15, LSB = 1 meter                             */
    McpU8 latitude_sign; /*< 10:  1> 0..1, Sign of latitude; 0=North, 1=South           */
    McpU8 altitude_sign; /*< 11:  1> 0..1, Sign of altitude; 0=Height, 1=Depth          */
    McpU32 position_uncertainty; /*< 12:  4> 0..429496729.5 m LSB = .1 m                        */
    McpDBL d_assist_ToaSec; /* RTC Timestamp, in seconds*/
} T_GPSC_position_assist;

typedef union
{
    T_GPSC_acquisition_assist acquisition_assist[N_SV_15]; /* Acquisition assistance data                        */
    T_GPSC_ephemeris_assist ephemeris_assist[N_SV_15]; /* Ephemeris assistance data                          */
    T_GPSC_iono_assist iono_assist; /* Ionospheric assistance data                        */
    T_GPSC_utc_assist utc_assist; /* UTC assistance data                                */
    T_GPSC_dgps_assist dgps_assist; /* DGPS assistance data                               */
    T_GPSC_almanac_assist almanac_assist[N_SV_15]; /* Almanac assistance data                            */
    T_GPSC_tow_assist tow_assist[N_SV_15]; /* TOW assistance data                                */
    T_GPSC_rti_assist rti_assist; /* RTI assistance data                                */
    T_GPSC_time_assist time_assist; /* Time of the week data                              */
    T_GPSC_position_assist position_assist; /* Position assistance data                           */
    McpU8 assist_comp; /* Assistance Completion Indication                   */
    McpU8 zzz_align0[3]; /* alignment */
} T_GPSC_assistance_inject_union;

/*
* Assistance injection data
* CCDGEN:WriteStruct_Count==37
*/
typedef struct
{
    EGPSC_AssistanceType ctrl_assistance_inject_union; /* Type of Assistance (T_GPSC_ctrl_assistance_inject_union) */
    T_GPSC_assistance_inject_union assistance_inject_union; /* Assistance injection data union */
} T_GPSC_assistance_inject;

typedef struct
{
    T_GPSC_assistance_inject tAssistance;
    McpU32 uSessionId;
    McpU32 uSystemTimeInMsec;
    McpU8 bValidGsmTime;     
    // following are GSM time params (for used by NAVC)
    McpU8 bsic;
    McpU16 bcchCarrier;
    McpU32 frameNumber;
    McpU8 timeSlot;
    McpU8 bitNumber;
} TNAVC_injectAssist;

/***********************************************************/
/* NAVC_CMD_DELETE_ASISTANCE command parameters            */

/***********************************************************/
typedef enum
{
    GPSC_DEL_AIDING_TIME = 0x1, /* Delete time                    */
    GPSC_DEL_AIDING_POSITION = 0x2, /* Delete position                */
    GPSC_DEL_AIDING_EPHEMERIS = 0x4, /* Delete ephemeris               */
    GPSC_DEL_AIDING_ALMANAC = 0x8, /* Delete almanac                 */
    GPSC_DEL_AIDING_IONO_UTC = 0x10, /* Delete Iono/UTC                */
    GPSC_DEL_AIDING_SVHEALTH = 0x20, /* Delete SV health               */
    GPSC_DEL_AIDING_SVDIR = 0x40, /* Delete SV direction	*/
    GPSC_DEL_AIDING_ACQ = 0x80 /* Delete ACQ		*/
} T_GPSC_del_assist_bitmap;

typedef struct
{
    McpU8 uDelAssistBitmap;
    McpU8 zzz_align0[3]; /* alignment */
    McpU32 uSvBitmap;

} TNAVC_delAssist;

/***********************************************************/
/* NAVC_CMD_SET_TOW command parameters                     */
/***********************************************************/

/* Time mode: time now and pulse (fine time injection) */

typedef enum
{
    NAVCD_TIME_NOW = GPSC_TIME_ACCURACY_COARSE,
    NAVCD_TIME_PULSE = GPSC_TIME_ACCURACY_FINE_LAST_PULSE,
    NAVCD_TIME_MODE_MAX_NUM

} ENAVCD_TimeMode;

typedef struct
{
    McpU32 eTimeMode; /* ENAVCD_TimeMode */
    McpU32 uSystemTimeInMsec;
    T_GPSC_time_assist tTimeAssist;
} TNAVC_setTow;

/***********************************************************/
/* NAVC_CMD_GET_ASSIST_EPH command parameters              */
/***********************************************************/
/* no parameters */

/***********************************************************/
/* NAVC_CMD_GET_ASSIST_IONO command parameters            */
/***********************************************************/
/* no parameters */

/***********************************************************/
/* NAVC_CMD_GET_ASSIST_ALMANAC command parameters          */
/***********************************************************/
/* no parameters */

/***********************************************************/
/* NAVC_CMD_GET_ASSIST_TIME command parameters             */
/***********************************************************/
/* no parameters */

/***********************************************************/
/* NAVC_CMD_GET_ASSIST_POSITION command parameters         */
/***********************************************************/

/* no parameters */

typedef struct
{
    McpU32 i2c_data_rate; /*<  0:  4> (enum=32bit)<->T_GPSC_i2c_data_rate I2C transmission rate */
    McpU8 i2c_logicalid; /*<  4:  1> I2C logical device ID                              */
    McpU8 zzz_align0; /*<  5:  1> alignment                                          */
    McpU8 zzz_align1; /*<  6:  1> alignment                                          */
    McpU8 zzz_align2; /*<  7:  1> alignment                                          */
    McpU32 i2c_ce_address_mode; /*<  8:  4> (enum=32bit)<->T_GPSC_i2c_ce_address_mode I2C CE addressing mode */
    McpU16 i2c_ce_address; /*< 12:  2> I2C CE address                                     */
    McpU8 zzz_align3; /*< 14:  1> alignment                                          */
    McpU8 zzz_align4; /*< 15:  1> alignment                                          */
    McpU32 i2c_gps_address_mode; /*< 16:  4> (enum=32bit)<->T_GPSC_i2c_gps_address_mode I2C GPS addressing mode */
    McpU16 i2c_gps_address; /*< 20:  2> I2C CE address                                     */
    McpU8 zzz_align5; /*< 22:  1> alignment                                          */
    McpU8 zzz_align6; /*< 23:  1> alignment                                          */
    McpU8 i2c_mode_gps_i2c_addr;
    McpU8 i2c_gps_opr_mode;
    McpU8 i2c_slave_transfer;
    McpU8 i2c_max_msg_len;
} T_GPSC_comm_config_i2c;

typedef struct
{
    McpU32 uart_baud_rate; /* T_GPSC_uart_baud_rate UART baud rate */
} T_GPSC_comm_config_uart;

/*
* Comm config_union
*/
typedef union
{
    T_GPSC_comm_config_uart comm_config_uart; /*<  0:  4> Comm config UART                                   */
    T_GPSC_comm_config_i2c comm_config_i2c; /*<  0: 24> Comm config I2C                                    */
} T_GPSC_comm_config_union;

typedef struct
{
    McpU32 ctrl_comm_config_union; /* controller for union  */
    T_GPSC_comm_config_union comm_config_union; /* Comm config_union     */
} T_GPSC_comm_config;

typedef struct
{
    McpU32 driver_tx_response_required; /* T_GPSC_patch_available Driver transmission response required Boolean values */
    McpU32 ai2_ack_required; /* T_GPSC_patch_available AI2 acknowledge required Boolean values */
    McpU32 auto_power_save_enable; /* T_GPSC_patch_available Automatic GPS powersave request Boolean values */
    McpU32 auto_power_ready_enable; /* T_GPSC_patch_available Automatic GPS ready request Boolean values */
    McpU32 power_mgmt_enable; /* T_GPSC_patch_available Enable power management */
    McpU32 driver_tx_timeout; /* Driver transmission response required, msec        */
    McpU32 ai2_comm_timeout; /* AI2 acknowledge required, msec                     */
    McpU32 auto_power_save_timeout; /* Automatic GPS powersave request, msec              */
    McpU32 internal_1; /* Internal Variable, should be set as TBD            */
    McpU32 internal_2; /* Internal Variable, should be set as TBD            */
    McpU32 internal_3; /* Internal Variable, should be set as TBD            */
    McpU32 internal_4; /* Internal Variable, should be set as TBD            */
    McpU32 smlc_comm_timeout; /* Automatic GPS ready request, msec                  */
    McpU32 sleep_entry_delay_timeout; /* Sleep entry delay, msec                            */
    McpU32 default_max_ttff; /* GSPC default internal max TTFF, msec               */
    McpU32 patch_available; /* T_GPSC_patch_available Patch available Boolean values */
    T_GPSC_comm_config comm_config; /* Communication configuration                        */
    McpU16 assist_bitmap_msbased_mandatory_mask; /* GPSC configuration for mandatory assistance data for msbased mode */
    McpU16 assist_bitmap_msassisted_mandatory_mask; /* GPSC configuration for mandatory assistance data for msassisted mode */
    McpU32 compatible_gps_versions[GPSC_MAX_COMPATIBLE_VERSIONS]; /* GPSC/GPS compatibility                             */
    McpU32 ref_clock_frequency; /* Reference clock frequency0 to 4294967295 Hz, resolution 1 Hz per bit. */
    McpU16 ref_clock_quality; /* Reference clock quality, 0.00 to 655.35 ppm, resolution 0.01 ppm per bit. */
    McpU16 max_clock_acceleration; /* Maximum reference clock acceleration, unsigned, 1 mm/sec^2 per bit, 0 to 65535 mm/sec^2 */
    McpU16 max_user_acceleration; /* Unsigned, 1 mm/sec^2 per bit, 0 to 65535 mm/sec^2  */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
    McpU32 altitude_hold_mode; /* T_GPSC_altitude_hold_mode Altitude hold mode */
    McpS8 elevation_mask; /* Elevation mask, signed, 90/2^7 degrees per bit, range -90 to 90 - 90/2^7 degrees */
    McpU8 pdop_mask; /* PDOP mask                                          */
    McpU8 zzz_align2; /* alignment                                          */
    McpU8 zzz_align3; /* alignment                                          */
    McpU32 timestamp_edge; /* T_GPSC_timestamp_edge Timestamp signal registration edge */
    McpU32 pa_blank_enable; /* T_GPSC_patch_available PA blanking enable Boolean values */
    McpU32 pa_blank_polarity; /* T_GPSC_pa_blank_polarity PA blanking signal active polarity */
    McpU16 gps_minimum_week; /* GPS minimum week                                   */
    McpU8 utc_leap_seconds; /* UTC leap seconds                                   */
    McpU8 zzz_align4; /* alignment                                          */
    McpU16 diag_report_control; /* Diagnostic Report Control                          */
    McpU8 front_end_loss; /* RF Front End Loss .1dB per bit                     */
    McpU8 kalman_feat_control; /* Kalman feature control enable or disable bit.0 : Enable Kalman filter feature, 1: Disable Kalman filter feature. */
    McpU8 checksum; /* Config file checksum                               */
    McpU8 zzz_align5; /* alignment                                          */
    McpU8 zzz_align6; /* alignment                                          */
    McpU8 zzz_align7; /* alignment                                          */
    McpU32 apm_control; /* T_GPSC_apm_control Advance Power Management Feature Activation */
    McpU32 search_mode; /* T_GPSC_search_mode Advance Power Management search mode Activation */
    McpU32 saving_options; /* T_GPSC_saving_options Advance Power Management saving option. Power save state to enter while no active acquisition is ongoing. */
    McpU8 power_save_qc; /* Unsigned: 10 msec per bit, Range: 100ms default to 900ms maximum */
    McpU8 zzz_align8; /* alignment                                          */
    McpU8 zzz_align9; /* alignment                                          */
    McpU8 zzz_align10; /* alignment                                          */
    McpU32 sbas_control; /* T_GPSC_sbas_control SBAS Activation. */
    McpU32 sbas_prn; /* SBAS prn mask.Bit field 0 - 18 corresponds to SBAS SVs 120 to 138. Bit field 19-31 is reserved and set to 0. By default all bits are set to 0 which corresponds to searching all PRN. */
    McpU8 sbas_mode; /* SBAS mode. Set to 0.                               */
    McpU8 sbas_flags; /* SBAS flags. Set to 0.                              */
    McpU8 block_almanac; /* Block the injection of Almanac to the sensor while doing 3GPP test. Set as  0 to No Block (Default),1 to Block. */
    McpU8 rx_opmode; /* Select the receiver operation  mode as accuracy  - 0,combo - 1,speed  - 2 */
    McpU8 lna_crystal; /* LNA GPS Crystal control.This is used to configure presence of external LNA and crystal. On power-up GPS engine comes up with EXTN_LDO and crystal core turned ON. Depending on the system configuration (TCXO v/s Crystal, Internal LNA v/s External LNA), CE could use this message to optimize power. Only on receiving this message packet, GPS engine will turn-off unwanted blocks. Bit 0 : Crystal, TCXO control . 1 : TCXO connected. 0 : Crystal connected. Bit 1: LNA configuration. 0 : Internal LNA (Default). 1 : External LNA.Bit 2-7: Reserve */
    McpU8 enable_timeout; /* To enable or disable Timeout and CEP info;Bit1: Enable, Bit0 : Disable */
    McpU16 timeout1; /* Unsigned, Scale: 0.5s per bit;In the range of 0 to 65534s. The special value of 65535 shall be used when GPSC does not know the timeout information. Default value is 20. */
    McpU16 timeout2; /* Unsigned, Scale: 0.5s per bit , In the range of 0 to 65534s. The special value of 65535 shall be used when GPSC does not know the timeout information. Default value is 12. */
    McpU16 accuracy1; /* Unsigned, Scale: 1m per bit , 2D position accuracy */
    McpU16 accuracy2; /* Unsigned, Scale: 1m per bit , 2D position accuracy */
    McpU8 autonomous_test_flag; /* Defines the autonomous test flag.Value0:Cold Start,Value1:Hot Start,Value2:Warm Start. */
    McpU8 rti_enable; /* Enable and Disable RTI feature                     */
    McpU8 data_wipe; /* Data wipe feature enable or disable bit 0 ; Data wipe off feature disabled (Default), 1: Data wipe off feature enabled */
    McpU8 dll; /* 0: Disable DLL based tracking (Default in NL5500 PG-1.0)1: Enable DLL based tracking */
    McpU8 carrier_phase; /* 0: Disable carrier phase measurement (Default in NL5500 PG-1.0)1: Enable carrier phase measurement */
    McpU8 hw_req_opt; /* Specifies host request signaling options for communication.Bit0: 0: Host request feature disabled 1: Host request feature enabled (Default);Bit 7:1: Reserved */
    McpU16 hw_assert_delay; /* Minimum delay between host request signal (GPS_IRQ) assertion to commencement of transmission from GE. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec(Default : 5 ms) */
    McpU16 hw_reassert_delay; /* Minimum delay between host request (GPS_IRQ) signal de-assertion to re-assertion. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec(Default : 5 ms) */
    McpU8 hw_ref_clk_req_opt; /* Specifies host reference clock request signaling options for calibration purposes. 0: Disable,1: GPS HW signal controlled calibration,2: GPS SW message controlled calibration */
    McpU8 hw_ref_clk_req_sig_sel; /* Selects signal to be used for REF_CLK request. Applicable only for GPS HW signal controlled calibration option.0: Use GPS_IRQ pin for making reference clock requests,1: Use REF_CLK_REQ pin for making reference clock request */
    McpU16 hw_ref_clk_assert_dly; /* Minimum delay between host reference clock request signal assertion to availability of clock. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 ssec(Default : 5 ms) */
    McpU16 hw_ref_clk_reassert_dly; /* Minimum delay between host reference clock request signal de-assertion to re-assertion for next clock request. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec (Default : 91.5 usec) */
    McpU8 hw_sigout_type_ctrl; /* Bit 1: Output type control for Host REF_CLK Request0: Open source signal (Default)1: Push/Pull signal.Bit2: Output type control for TCXO_CLK_REQ signal0: Open source signal (Default)1: Push/Pull signal */
    McpU8 zzz_align11; /* alignment                                          */
    McpU16 tcxo_startup_time; /* Unsigned, Scale: 1/32768S (=30.517uS) per bit Range - 0 to 65535 Interpreted Value - 30.517uS to (2S-30.517uS)Values outside the range are considered as invalid. Default value is 5ms. */
    McpU8 gps_sleep_ctrl; /* Bit 0: Auto Sleep Control Enable Disable,Bit1: Wakeup Source Control */
    McpU8 geofence_enable; /* Enable and Disable Geofence feature                */
    McpU16 recv_min_rep_period; /* Minimum Report Period to enable and disable  fast TTFF Default: 1000 ms Disable fast TTFF and 500 ms - Enable Fast TTFF */
    McpU8 count_invalid_fix; /* Invalid fix count for re-request assistance in case of timeouts */
    McpU8 low_power_state; /* low power state of GPSC. ,Bit 0:Sleep state (Default), Bit 1:Idle state */
    McpU16 calib_period; /* Calibration Period: Unsigned, 1ms per bit,Range: 0 to 4000 ms */
    McpU16 period_uncertainity; /* Period Uncertainty: Unsigned, 1ns per bit,Range: 0 to 65.535 us */
    McpU16 max_gps_clk_unc; /* To provide GPS maximum clock uncertainty           */
    McpU8 zzz_align12; /* alignment                                          */
    McpU8 zzz_align13; /* alignment                                          */
    McpFLT shrt_term_gps_clk_unc; /* To provide GPS short term clock uncertainty        */
    //McpU32					shrt_term_gps_clk_unc;    /* To provide GPS short term clock uncertainty        */
    McpU32 system_time_unc; /* To provide the System time uncertainty             */
    McpU8 enable_manualrefclk; /* To switch between Manual Reference clock and pre-configured Reference clock */
    McpU8 enable_finetime; /* To enable fine time injection                      */
    McpS16 altitude_estimate; /* Altitude Estimate Scale: 0.5s per bit , range 16384 to 16383.5 meters */
    McpU16 altitude_unc; /* Altitude Uncertainty Scale: 0.5s per bit , range 0 to 32767.5 meters */
    McpU8 ref_clock_calib_type; /* Reference Clock Calibration Type: 0-xx as defined in T_GPSC_calib_type  */
    McpU8 zzz_align15; /* alignment                                          */
    McpU8 priority_sagps; /* priority for sagps */
    McpU8 priority_pgps; /* priority for pgps */
    McpU8 priority_supl; /* priority for supl */
    McpU8 priority_cplane; /* priority for cplane */
    McpU8 priority_custom_agps_provider1; /* priority for custom provider1 */
    McpU8 priority_custom_agps_provider2; /* priority for custom provider2 */
    McpU8 priority_custom_agps_provider3; /* priority for custom provider3 */
    McpU16 pgps_sagps_timeout1; /* Unsigned, Scale: 0.5s per bit;In the range of 0 to 65534s. The special value of 65535 shall be used when GPSC does not know the timeout information. Default value is 20. */
    McpU16 pgps_sagps_timeout2; /* Unsigned, Scale: 0.5s per bit , In the range of 0 to 65534s. The special value of 65535 shall be used when GPSC does not know the timeout information. Default value is 12. */
    McpU16 pgps_sagps_accuracy1; /* Unsigned, Scale: 1m per bit , 2D position accuracy */
    McpU16 pgps_sagps_accuracy2; /* Unsigned, Scale: 1m per bit , 2D position accuracy */
    McpU8 alt_2d_pdop; /* To provide ALT 2D PDOP        */
    McpFLT tcxo_unc_longterm; /* To provide TCXO Long term clock uncertainty        */
    McpFLT tcxo_aging; /* To provide TCXO Aging        */
    McpU32 sys_time_sync;
    McpU32 unc_threshold;
    McpU16 pos_velocty;
    McpU16 pos_unc_threshold;
    McpU16 hours_thrshold;
    McpU16 week_threshold;
    McpU16 rtc_quality;
    McpU8 rtc_calibration;
    McpU32 prm_adjustment;
    McpU8 pdop_mask_time_out; /* PDOP mask Time Out */
    McpU32 pps_output;
    McpU32 pps_polarity;
    McpU32 time_injection;
    McpU8 enable_rtc_time_injection;
    McpU8 time_inj_check;
    McpU8 time_validate_check;
    McpU8 pos_inject_check;
    McpU8 pos_validate_check;
    McpU8 eph_inject_check;
    McpU8 eph_validate_check;
    McpU8 alm_inject_check;
    McpU8 alm_validate_check;
    McpU8 tcxo_inject_check;
    McpU8 tcxo_validate_check;
    McpU8 utc_inject_check;
    McpU8 utc_validate_check;
    McpU8 ion_inject_check;
    McpU8 ion_validate_check;
    McpU8 health_inject_check;
    McpU8 health_validate_check;
    McpU8 systemtime_or_injectedtime;
    McpS16 tone_power;
    McpS8 nf_correction_factor;
    McpU16 fft_average_number;
} T_GPSC_config_file;

/***********************************************************/
/* NAVC_CMD_INJECT_CONFIGURATION command parameters        */

/***********************************************************/
typedef struct
{
    T_GPSC_config_file tConfigFile;
    McpU32 tSaveToFile; /* TBD */

} TNAVC_injectConfig;

/***********************************************************/
/* NAVC_CMD_SET_HOST_WAKEUP_PARAMS command parameters      */
/***********************************************************/

/*
* enum to Variable host_req_opt
* Host Wakeup Activation. Supported both in NL5500 and NL5350.
*/

typedef struct
{
    McpU32 host_req_opt; /* T_GPSC_host_req_opt Host Wakeup Activation. Supported both in NL5500 and NL5350. */
    McpU16 host_assert_delay; /* Minimum delay between host request signal (GPS_IRQ) assertion to commencement of transmission from GE. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec(Default : 5 ms).Supported only in NL5500. */
    McpU16 host_reassert_delay; /* Minimum delay between host request (GPS_IRQ) signal de-assertion to re-assertion. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec(Default : 5 ms). Supported only in NL5500. */
    McpU8 host_ref_clk_req_opt; /* Specifies host reference clock request signaling options for calibration purposes. 0: Disable,1: GPS HW signal controlled calibration,2: GPS SW message controlled calibration. Supported only in NL5500. */
    McpU8 host_ref_clk_req_sig_sel; /* Selects signal to be used for REF_CLK request. Applicable only for GPS HW signal controlled calibration option.0: Use GPS_IRQ pin for making reference clock requests,1: Use REF_CLK_REQ pin for making reference clock request. Supported both in NL5500 and NL5350. */
    McpU16 host_ref_clk_assert_dly; /* Minimum delay between host reference clock request signal assertion to availability of clock. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 ssec(Default : 5 ms). Supported both in NL5500 and NL5350. */
    McpU16 host_ref_clk_reassert_dly; /* Minimum delay between host reference clock request signal de-assertion to re-assertion for next clock request. Unsigned, 30.5 usec (RTC clock period) per bit Range: 0 to 1 sec (Default : 91.5 usec). Supported both in NL5500 and NL5350. */
    McpU8 host_sigout_type_ctrl; /* Bit 1: Output type control for Host REF_CLK Request0: Open source signal (Default)1: Push/Pull signal.Bit2: Output type control for TCXO_CLK_REQ signal0: Open source signal (Default)1: Push/Pull signal. Supported only in NL5500. */
} TNAVC_SetHostWakeupParams;

/********************************************************************/
/* NAVC_CMD_SET_APM_PARAMS (Advanced Power Mode) command parameters */

/********************************************************************/
typedef struct
{
    McpU32 apm_control; /* T_GPSC_apm_control Advance Power Management Feature Activation */
    McpU32 search_mode; /* T_GPSC_search_mode Advance Power Management search mode Activation */
    McpU32 saving_options; /* T_GPSC_saving_options Advance Power Management saving option. Power save state to enter while no active acquisition is ongoing. */
    McpU8 Rsvd_pwr_mgmt1; /* Advance Power Management reserved bits. 3 bytes are reserved for Advanced Power Management. */
    McpU8 power_save_qc; /* Unsigned: 10 msec per bit, Range: 100ms default to 900ms maximum */
    McpU8 Rsvd_pwr_mgmt2; /* Advance Power Management reserved bits. 3 bytes are reserved for Advanced Power Management. */
    McpU8 Rsvd_pwr_mgmt3; /* Advance Power Management reserved bits. 3 bytes are reserved for Advanced Power Management. */
} TNAVC_SetAPMParams;

/***********************************************************/
/* NAVC_CMD_SET_SBAS_PARAMS command parameters             */

/***********************************************************/
typedef struct
{
    McpU32 sbas_control; /* T_GPSC_sbas_control SBAS Activation. */
    McpU32 sbas_prn_mask; /* SBAS prn mask.Bit field 0 - 18 corresponds to SBAS SVs 120 to 138. Bit field 19-31 is reserved and set to 0. By default all bits are set to 0 which corresponds to searching all PRN. */
    McpU8 Mode; /*  SBAS mode. Set to 0.                               */
    McpU8 Flags; /*  SBAS flags. Set to 0.                              */
    McpU8 zzz_align0[2]; /* alignment */
    McpU32 rsvd_sbas1; /*  SBAS reserved bits. 8 bytes are reserved for SBAS. */
    McpU32 rsvd_sbas2; /*  SBAS reserved bits. 8 bytes are reserved for SBAS. */
} TNAVC_SetSBASParams;

/***********************************************************/
/* NAVC_CMD_SET_MOTION_MASK command parameters             */

/***********************************************************/
typedef struct
{
    McpU32 motion_mask_control; /* Check GEOFENCE flags for the options */
    McpS32 altitude_limit; /*< 80:	2> Motion Maskaltitude limit..Signed, 1 meters per bit.Range: -32768 to 32767 meters. */
    McpS32 area_altitude; /*< 68:  4> Motion Mask area origin latitude.Signed, pi/32 radians per bit.Range: -pi/2 to pi/2 - pi/232 radians.Used only if Motion Mask Control Bit 5 is '1'. */
    McpS32 latitude[MAX_VERTICES]; /* Motion Mask Latitude signed  7.31 radians ber bit.Range: -pi/2 to pi/2 - pi/232 radians.    */
    McpS32 longitude[MAX_VERTICES]; /* Motion Mask Longitude pi/231 radians be bit.Range: -pi/2 to pi/2 - pi/232 radians. */
    McpU16 speed_limit; /*< 82:  2> Motion Mask speed limit.Unsigned, 0.01 meters/sec per bit.Range: 0 to 655.35 meters/sec */
    McpU16 radius_of_circle; /*< 78:  2> Motion Mask radius of circle.Unsigned, 1 meters per bit.Range: 1 to 65535 meters. */
    McpU8 uSessionId;
    McpU8 report_configuration; /*  1: always send position reports 0: send postion reports only when specified geo fence voilation occurs*/
    McpU8 region_number; /*the index of the region   range:1 to 25 */
    McpU8 region_type; /* 1:polygon 0:circle */
    McpU8 no_of_vertices; /*3 to 10 for polygon , 1 for circle */
    McpU8 z_Alignment1;
    McpU8 z_Alignment2;
    McpU8 z_Alignment3;


} TNAVC_SetMotionMask;

typedef struct
{
    McpU32 FCount;
    McpU32 status; /* bit 0-24 correspond to  status of the region 1 to 25 bit value : 1 geo fence voilation has occured, bit value 0 : user is within geofence*/
    McpU32 geofence_session_id;

} TNAVC_MotionMask_Status;

typedef struct
{
    McpU32 motion_mask_control; /* Check GEOFENCE flags for the options */
    McpS32 altitude_limit; /*< 80:	2> Motion Maskaltitude limit..Signed, 1 meters per bit.Range: -32768 to 32767 meters. */
    McpS32 area_altitude; /*< 68:  4> Motion Mask area origin latitude.Signed, pi/32 radians per bit.Range: -pi/2 to pi/2 - pi/232 radians.Used only if Motion Mask Control Bit 5 is '1'. */
    McpS32 latitude[MAX_VERTICES]; /* Motion Mask Latitude signed  7.31 radians ber bit.Range: -pi/2 to pi/2 - pi/232 radians.    */
    McpS32 longitude[MAX_VERTICES]; /* Motion Mask Longitude pi/231 radians be bit.Range: -pi/2 to pi/2 - pi/232 radians. */
    McpU16 speed_limit; /*< 82:  2> Motion Mask speed limit.Unsigned, 0.01 meters/sec per bit.Range: 0 to 655.35 meters/sec */
    McpU16 radius_of_circle; /*< 78:  2> Motion Mask radius of circle.Unsigned, 1 meters per bit.Range: 1 to 65535 meters. */
    McpU8 report_configuration; /*  1: always send position reports 0: send postion reports only when specified geo fence voilation occurs*/
    McpU8 region_number; /*the index of the region   range:1 to 25 */
    McpU8 region_type; /* 1:polygon 0:circle */
    McpU8 no_of_vertices; /*3 to 10 for polygon , 1 for circle */


} TNAVC_GetMotionMask;

/*
* enum to Variable motion_mask_control
* Motion Mask Activation.
*/
;

/***********************************************************/
/* NAVC_CMD_ENABLE_KALMAN_FILTER command parameters           */

/***********************************************************/
typedef struct
{
    McpU8 kalman_control; /* Kalman feature control enable or disable bit.0 : Enable Kalman filter feature, 1: Disable Kalman filter feature. */
    McpU8 zzz_align0[3]; /* alignment */
} TNAVC_EnableKalmanFilter;

/***********************************************************/
/* NAVC_CMD_READ_CONFIGURATION command parameters          */

/***********************************************************/
typedef struct
{
    T_GPSC_config_file tConfigFile;

} TNAVC_readConfig;


/***********************************************************/
/* NAVC_CMD_SET_REFCLK_PARAMETER command parameters        */

/***********************************************************/
typedef struct
{
    McpU16 uRefClockQuality;
    McpU8 zzz_align0[2]; /* alignment */
    McpU32 uRefClockFrequency;

} TNAVC_refClockParams;


/***********************************************************/
/* NAVC_CMD_SET_TOW command parameters                     */
/***********************************************************/

/***********************************************************/
/* NAVC_CMD_PLT command parameters                         */
/***********************************************************/

/*
* enum to Variable req_type
* Specify the which production line test is requested
*/
typedef enum
{
    GPSC_USERDEF_CWTEST_REQ = 0xc6, /* User Defined CW Test           */
    GPSC_SIGACQ_TEST_REQ = 0xca, /* Signal Acquisition Test        */
    GPSC_GPSOSC_TEST_REQ = 0xcc, /* GPS Oscillator test            */
    GPSC_RTC_TEST_REQ = 0xcd, /* RTC Offset Test                */
    GPSC_SYNC_TEST_REQ = 0xce, /* Sync Test                      */
    GPSC_PREDEF_CWTEST_REQ = 0xcf /* Predefined CW Test             */
} T_GPSC_req_type;

/*
* gpio test request  parameters
* CCDGEN:WriteStruct_Count==47
*/
typedef struct
{
    McpU16 write_value; /* GPIO write value- bit field                        */
    McpU16 write_mask; /* GPIO write mask                                    */
    McpU16 status_mask; /* GPIO read status mask                              */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
} T_GPSC_gpio_test_params;

/*
* cw test request  parameters
*/
typedef struct
{
    McpU16   test_request;             /*                                                    */
    McpU16   start_delay;              /* Range 0 to 65535 msec                              */
    McpU8   zzz_align0;               /* alignment                                          */
    McpU8   zzz_align1;               /* alignment                                          */
    McpS32  wideband_centfreq;        /* Range -8.184 MHz to 8.184 - 16.368/224 MHz         */
    McpS32  narrowband_centfreq;      /* Range -8.184 MHz to 8.184 - 16.368/224 MHz         */
    McpU8   wideband_peaks;           /* Range is  0 to 10                                  */
    McpU8   wideband_adj_samples;     /* Range is  0 to 255                                 */
    McpU8   narrowband_peaks;         /* Range is  0 to 10                                  */
    McpU8   narrowband_adj_samples;   /* Range is  0 to 255                                 */
    McpU8   rawiqsample_rawfft_capture;
    McpU8   nf_correction_factor;
    McpU16  input_tone;
    McpU8   test_mode;
    McpU8   narrowband_decimation;
    McpU16  reserved;
} T_GPSC_cw_test_params;

/*
* enum to Variable cwresponsetype
* Specify the Response type for CW test
*/
typedef enum
{
    GPSC_IQ_SAMPLES_1MHZ = 0x0, /* CW Test IQ Samples for 1 MHz   */
    GPSC_IQ_SAMPLES_2MHZ = 0x1, /* CW Test IQ Samples for 2 MHz   */
    GPSC_WIDEBAND_FFT = 0x2, /* CW Test Wideband FFT Result    */
    GPSC_NARROWBAND_FFT = 0x3, /* CW Test Narrowband FFT Result  */
    GPSC_CWSTATUS = 0x4 /* CW Test Status                 */
} T_GPSC_cwresponsetype;

/*
* Continuous Wave Test response
*/

typedef struct
{
    McpU16 wideband_peak_index; /* Range: 0 to 4096			 */
    McpU16 wideband_peak_snr; /* Range: 0 to 65535 dB		 */
} T_GPSC_cwtest_widebandpeakinfo;

typedef struct
{
    McpU16 narrowband_peak_index; /* Range: 0 to 4096									*/
    McpU16 narrowband_peak_snr; /* Range: 0 to 65535 dB								 */
} T_GPSC_cwtest_narrowbandpeakinfo;

typedef struct
{
    McpS16 isamplepacketnumber;
    McpS16 qsamplepacketnumber;
} T_GPSC_cwtest_iqsamples_packetbody;

typedef struct
{
    T_GPSC_cwresponsetype		   cwresponsetype;           /* (enum=32bit)<->T_GPSC_cwresponsetype Specify the Response type for CW test */
    McpU16                       totalpacket;              /* Range: 0 to 32                                     */
    McpU16                       packetnumber;             /* Range: 0 to 32                                     */
    McpU16                       can1bin_peak;             /* Range: 0 to 65535 dB                               */
    McpU16                       can2to3bin_peak;          /* Range: 0 to 65535 dB                               */
    McpU16                       can4to7bin_peak;          /* Range: 0 to 65535 dB                               */
    McpU16                       can8to15bin_peak;         /* Range: 0 to 65535 dB                               */
    McpU16                       can16to31bin_peak;        /* Range: 0 to 65535 dB                               */
    McpU16                       can32to63bin_peak;        /* Range: 0 to 65535 dB                               */
    McpU16                       can64to127bin_peak;       /* Range: 0 to 65535 dB                               */
    McpU16                       can128to255bin_peak;      /* Range: 0 to 65535 dB                               */
    McpU16                       can256to511bin_peak;      /* Range: 0 to 65535 dB                               */
    McpU8                        noise_figure;
    McpS32                        tcxo_offset;
    T_GPSC_cwtest_widebandpeakinfo 		wideband_peakinfo[128];
    McpU8						 num_wideband_peak; 	   /* Range: 0 to 255									 */
    McpS32					 wideband_centfreq; 	   /* Range -8.184 MHz to 8.184 - 16.368/224 MHz		 */
    T_GPSC_cwtest_narrowbandpeakinfo 		narrowband_peakinfo[128];
    McpU8						 num_narrowband_peak;	   /* Range: 0 to 255									 */
    McpS32					 narrowband_centfreq;	   /* Range -8.184 MHz to 8.184 - 16.368/224 MHz		 */
    T_GPSC_cwtest_iqsamples_packetbody 	iqsamples_packetbody[128];
} T_GPSC_cw_test;

typedef struct
{
    McpU32 req_type; /* T_GPSC_req_type Specify the which production line test is requested */
    McpU32 timeout; /* In milliseconds                                    */
    McpU8 svid; /* Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64) */
    McpU8 cw_test_ver; /*                                                    */
    McpU8 termination_event; /*                                                    */
    McpU8 zzz_align0; /* alignment                                          */
    T_GPSC_cw_test_params cw_test_params; /* cw test request  parameters                        */
    T_GPSC_gpio_test_params gpio_test_params; /* gpio test request  parameters                      */
} TNAVC_plt;

/***********************************************************/
/* NAVC_CMD_GET_VERSION command parameters           */
/***********************************************************/
/* no parameters */

/***********************************************************/
/* NAVC_CMD_INJECT_CALIB_CTRL command parameters           */

/***********************************************************/
typedef enum
{
    GPSC_DISABLE_CALIBRATION = 0x0, /* To Disable the Calibration     */
    GPSC_ENABLE_PERIODIC_CALIB_REF_CLK = 0x1, /* To Enable Periodic Calibration with Reference Clock. */
    GPSC_DISABLE_ONESHOT_CALIB_REF_CLK = 0x2, /* To Disable One shot Calibration with Reference Clock */
    GPSC_ENABLE_ONESHOT_CALIB_REF_CLK = 0x3, /* To Enable One shot Calibration with Reference Clock. */
    GPSC_DISABLE_PERIODIC_CALIB_TIME_STAMP = 0x4, /* To Disable Periodic Calibration with Time Stamp. Only NL5500 */
    GPSC_ENABLE_PERIODIC_CALIB_TIME_STAMP = 0x5, /* To Enable Periodic Calibration with Time Stamp. Only NL5500 */
    GPSC_DISABLE_ONESHOT_CALIB_TIME_STAMP = 0x6, /* To Disable one shot Calibration with Time Stamp. Only NL5500 */
    GPSC_ENABLE_ONESHOT_CALIB_TIME_STAMP = 0x7, /* To Enable One Shot Calibration with Time Stamp. Only NL5500 */
    GPSC_UNDO_CALIB = 0x8 /* Disable any previous caliberation in the sensor */
} T_GPSC_calib_type;

typedef struct
{
    McpU8 uCalibType;
    McpU8 zzz_align0[3]; /* alignment */
} TNAVC_injectCalibCtrl;

/***********************************************************/
/* NAVC_CMD_SET_GEOFENCE command parameters                */
/***********************************************************/
/*
* Vertices of geo fence.
*/

/*
* This data structure contains geo-fence configuration parameters such as fence type, number of vertices, radius and center of circle or array of vertices for polygon.
*/



typedef struct
{
    McpS32 l_FreqBiasRaw;
    McpU32 q_FreqUncRaw;
    McpU8 u_ForceFlag;
} gpsc_inject_freq_est_type; /* AI2 Command Packet 32: InjectFreqEst */

typedef gpsc_inject_freq_est_type TNAVC_gpsc_inject_freq_est_type;


/***********************************************************/
/* NAVC_CMD_REGISTER_ASSIST_SRC command parameters                  */

/***********************************************************/

typedef enum
{
    SAGPS_PROVIDER, /* to register SAGPS*/
    PGPS_PROVIDER, /* to register PGPS */
    SUPL_PROVIDER, /* to Register SUPL */
    CPLANE_PROVIDER, /* To register control plane*/
    REFCLK_PROVIDER,  /* To register control plane*/
    CUSTOM_AGPS_PROVIDER1, /* to register any other agps assistance src*/
    CUSTOM_AGPS_PROVIDER2, /* to register any other agps assistance src*/
    CUSTOM_AGPS_PROVIDER3, /* to register any other agps assistance src*/
    MAX_ASSIST_PROVIDER /* maximum number of assistance source */
} ENAVC_assist_src_type;

/***********************************************************/
/* NAVC_CMD_REGISTER_ASSIST_SRC command parameters                  */

/***********************************************************/
typedef struct
{
    ENAVC_assist_src_type eAssistSrcType;
} TNAVC_assist_src_type; /* To register the assistance source type */

/***********************************************************/
/* NAVC_CMD_SET_ASSIST_PRIORITY command parameters                  */

/***********************************************************/
typedef struct
{
    ENAVC_assist_src_type eAssistSrcType; /*<  enum for assistance provider type (0 to MAX_ASSIST_PROVIDER)                                     */
    McpU8 assist_src_priority; /*<  priority ranges from 1 to MAX_ASSIST_PROVIDER and 0 is for disable the assistance src*/

} TNAVC_assist_src_priority_set;

typedef struct
{
    McpU16 numBytes; /*< AI2 message length in bytes */
    McpU8 ai2Msg[C_DL_RX_BUF_LENGTH]; /*< AI2 message array */
} TNAVC_AI2_RAW_MSG;

typedef enum
{
    INAV_ENABLE_SMARTAPM, /*  To enable inertial navigation : SmartAPM mode */
    INAV_ENABLE_DR, /* To enable inertial navigation : dead reckoning mode */
    INAV_DISABLE /* To disable inertial navigation support */
} ENAVC_inav_ctrl;

typedef struct
{
    ENAVC_inav_ctrl inav_ctrl; /*< Select the inertial navigation operation mode */
} TNAVC_INAVCtrl;

/***********************************************************/
/* command parameters union (to calc max command size)     */

/***********************************************************/
typedef union
{
    TNAVC_injectConfig			tInjectConfig;
    TNAVC_readConfig			tReadConfig;
    TNAVC_refClockParams		tRefClock;
    TNAVC_reqLocFix     		tReqLocFix;
    TNAVC_stopLocFix			tStopLocFix;
    TNAVC_injectAssist			tInjectAssist;
    TNAVC_delAssist				tDelAssist;
    TNAVC_setTow				tSetTow;
    TNAVC_plt					tPltParams;
    TNAVC_posIndData			tPosInd;
    TNAVC_injectCalibCtrl		tInjectCalibCtrl;
    TNAVC_EnableKalmanFilter	tEnableKalmanFilter;
    TNAVC_SetMotionMask			tSetMotionMask;
    TNAVC_SetSBASParams			tSetSBASParams;
    TNAVC_SetAPMParams			tSetAPMParams;
    TNAVC_SetHostWakeupParams	tSetHostWakeupParams;
    TNAVC_gpsc_inject_freq_est_type	tInjectFreqEst;
    TNAVC_assist_src_type       tAssistSrcType;
    TNAVC_assist_src_priority_set tAssistSrcPrioritySet[MAX_ASSIST_PROVIDER];
    TNAVC_AI2_RAW_MSG tAI2RawMsg;
    TNAVC_LogMode				tLogMode;
    TNAVC_INAVCtrl 			    tINAVCtrlSel;
    McpU8 				        GeoFence_regionNumber;
    TNAVC_wifiPosition			twifiPosition;
    TNAVC_BLF_Session_Config	tBlfConfig;
    McpU8				        tSuplSessionSuccess;
    TNAVC_Reg_For_Assist		tRegAssist;
} TNAVC_cmdParams;

typedef struct
{
    McpU16 assist_availability_flags;
    McpU32 eph_availability_flags;
    McpU32 pred_eph_availability_flags;
    McpU32 alm_availability_flags;
} T_GPSC_wishlist_params;

/***********************************************************/
/* NAVC_EVT_CMD_COMPLETE event parameters                  */
/***********************************************************/
/* no parameters */

/***********************************************************/
/* NAVC_EVT_REQUEST_PULSE event parameters                 */
/***********************************************************/
/* no parameters */

/***********************************************************/
/* NAVC_EVT_POSITION_FIX_REPORT event parameters           */
/***********************************************************/

/* added the response packet for report wifi,blend and gps fixes to toolkit */
typedef struct
{

  McpU32     session_id;
  McpS32     gps_latitude;         /* Latitude of position, signed, pi/2^32 radians per bit*/
  McpS32     gps_longitude;        /* Longitude of position, signed, pi/2^31 radians per bit */
  McpS16     gps_altitude;         /* WGS-84 altitude, signed 0.5 meters per bit         */
  McpU32     gps_timeofpos;        /*time of position in msec */
  McpU8      gps_fix_valid;        /* set to TRUE if  gps fix is valid */

  McpS32     wifi_latitude;         /* Latitude of position, signed, pi/2^32 radians per bit */
  McpS32     wifi_longitude;        /* Longitude of position, signed, pi/2^31 radians per bit */
  McpU32     wifi_timeofpos;        /*time of position in msec */
  McpU8      wifi_fix_valid;        /* set to TRUE if gps fix is valid */

  McpS32     blended_latitude;         /* Latitude of position, signed, pi/2^32 radians per bit */
  McpS32     blended_longitude;        /* Longitude of position, signed, pi/2^31 radians per bit */
  McpS16     blended_altitude;         /* WGS-84 altitude, signed 0.5 meters per bit         */
  McpU32     blended_timeofpos;        /*time of position in msec */

}TNAVC_gpsWifiBlendReport;

typedef enum
{
    GPSC_PROT_FIXRESULT_2D = 0x0, /* 2D position fix calculated     */
    GPSC_PROT_FIXRESULT_3D = 0x1, /* 3D position fix calculated     */
    GPSC_PROT_FIXRESULT_NOFIX = 0x8000 /* Position not calculated        */
} T_GPSC_prot_fix_result;

/*
* Ellipsoid point with altitude and uncertainty ellipsoid
*/
typedef struct
{
    McpU32 latitude; /*<  0:  4> 0..2^23, LSB = (2^23)/90 degrees                   */
    McpS32 longitude; /*<  4:  4> 0..2^24, LSB = (2^24)/360 degrees                  */
    McpU8 zzz_align0; /*<  8:  1> alignment                                          */
    McpU8 v_altitude; /*<  9:  1> valid-flag                                         */
    McpU16 altitude; /*< 10:  2> 0..2^15, LSB = 1 meter                             */
    McpU8 latitude_sign; /*< 12:  1> 0..1, Sign of latitude; 0=North, 1=South           */
    McpU8 v_altitude_sign; /*< 13:  1> valid-flag                                         */
    McpU8 altitude_sign; /*< 14:  1> 0..1, Sign of altitude; 0=Height, 1=Depth          */
    McpU8 unc_major; /*< 15:  1> 0..127, LSB = some obscure formula                 */
    McpU8 unc_minor; /*< 16:  1> 0..127, R7, LSB = some obscure formula             */
    McpU8 orient_major; /*< 17:  1> 0..179, R8, LSB = 2 degrees                        */
    McpU8 v_unc_alt; /*< 18:  1> valid-flag                                         */
    McpU8 unc_alt; /*< 19:  1> 0..127, R7, LSB = some obscure formula             */
    McpU8 confidence; /*< 20:  1> 0..100, R7, LSB = 1% confidence                    */
    McpU8 shape_code; /*< 21:  1> 0x09: 3D ellipsoid point, most significant nibble for shape */
    McpU8 zzz_align1; /*< 22:  1> alignment                                          */
    McpU8 zzz_align2; /*< 23:  1> alignment                                          */
} T_GPSC_ellip_alt_unc_ellip;

/*
* Protocol position response
*/
typedef struct
{
    McpU32 gps_tow; /* GPS TOW                                            */
    McpU32 prot_fix_result; /* T_GPSC_prot_fix_result Protocol position fix result */
    T_GPSC_ellip_alt_unc_ellip ellip_alt_unc_ellip; /* Ellipsoid point with altitude and uncertainty ellipsoid */
} T_GPSC_prot_position;

/*
* enum to Variable multipath_indicator
* Multipath indicator
*/
typedef enum
{
    GPSC_MULTIPATH_NOT_MEASURED = 0x0, /* Multipath not measured         */
    GPSC_MULTIPATH_LOW = 0x1, /* Multipath detection: low       */
    GPSC_MULTIPATH_MEDIUM = 0x2, /* Multipath detection: medium    */
    GPSC_MULTIPATH_HIGH = 0x3 /* Multipath detection: high      */
} T_GPSC_multipath_indicator;

/*
* SV measurement data
*/
typedef struct
{
    McpU8 svid; /*<  0:  1> Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64) */
    McpU8 cno; /*<  1:  1> 0..63                                              */
    McpS16 doppler; /*<  2:  2> -32767..32768, LSB = 0.2Hz                         */
    McpU16 whole_chips; /*<  4:  2> 0..1022                                            */
    McpU16 frac_chips; /*<  6:  2> 0..1024                                            */
    McpU32 multipath_indicator; /* (enum=32bit)<->T_GPSC_multipath_indicator Multipath indicator */
    McpU8 pseudorange_rms_error; /*< 12:  1> 0..63                                              */
    McpU8 zzz_align0; /*< 13:  1> alignment                                          */
    McpU8 zzz_align1; /*< 14:  1> alignment                                          */
    McpU8 zzz_align2; /*< 15:  1> alignment                                          */
} T_GPSC_sv_measurement;

/*
* Protocol measurement response
*/
typedef struct
{
    McpU32 gps_tow; /*<  0:  4> GPS TOW                                            */
    McpU8 zzz_align0; /*<  4:  1> alignment                                          */
    McpU8 zzz_align1; /*<  5:  1> alignment                                          */
    McpU8 zzz_align2; /*<  6:  1> alignment                                          */
    McpU8 c_sv_measurement; /*<  7:  1> counter                                            */
    T_GPSC_sv_measurement sv_measurement[GPSC_PROT_MAX_SV_MEASUREMENT]; /*<  8:192> SV measurement data                                */
} T_GPSC_prot_measurement;

/*
* Time of applicability for reported data
*/
typedef struct
{
    McpU16 gps_week; /*<  0:  2> GPS week number                                    */
    McpU8 zzz_align0; /*<  2:  1> alignment                                          */
    McpU8 zzz_align1; /*<  3:  1> alignment                                          */
    McpU32 gps_msec; /*<  4:  4> GPS milliseconds                                   */
    McpFLT sub_ms; /*<  8:  4> GPS sub-milliseconds - 1 bit = 1 microsecond       */
    McpFLT tUnc;
    McpFLT fbias;
    McpFLT fUnc;
    McpU32 fcount;
    McpU8 u_valid;

} T_GPSC_toa;

/*
* Information for SV used in position fix
*/
typedef struct
{
    McpU8 svid; /*<  0:  1> Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64) */
    McpU8 iode; /*<  1:  1> ICD-200                                            */
    McpS16 residual; /*<  2:  2> Measurement residual                               */
    McpU16 weight; /*<  4:  2> Weight used in fix                                 */
    McpU8 zzz_align0; /*<  6:  1> alignment                                          */
    McpU8 zzz_align1; /*<  7:  1> alignment                                          */
} T_GPSC_position_sv_info;

/*
* Raw position
*/
typedef struct
{
    McpU16 raw_fix_result_bitmap; /* T_GPSC_raw_fix_result_bitmap,  Position fix result bitmap */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
    McpS32 latitude_radians; /* Latitude of position, signed, pi/2^32 radians per bit */
    McpS32 longitude_radians; /* Longitude of position, signed, pi/2^31 radians per bit */
    McpS16 altitude_wgs84; /* WGS-84 altitude, signed 0.5 meters per bit         */
    McpS16 altitude_msl; /* MSL altitude, signed 0.5 meters per bit            */
    McpU16 uncertainty_east; /* East position uncertainty, un signed, 0.1 meters per bit */
    McpU16 uncertainty_north; /* North position uncertainty, un signed, 0.1 meters per bit */
    McpU16 uncertainty_vertical; /* Vertical position uncertainty, un signed, 0.1 meters per bit */
    McpS16 velocity_east; /* East velocity, signed, 1000/65535 meters/sec per bit */
    McpS16 velocity_north; /* North velocity, signed, 1000/65535 meters/sec per bit */
    McpS16 velocity_vertical; /* Vertical velocity, signed, 1000/65535 meters/sec per bit */
    McpU16 velocity_uncertainty; /* Velocity uncertainty, unsigned, 1000/65535 meters/sec per bit */
    McpU8 PDOP; /* Position Dilution of Precision (DOP), unsigned, 0.1 per bit, no units */
    McpU8 HDOP; /* Horizontal Dilution of Precision (DOP), unsigned, 0.1 per bit, no units */
    McpU8 VDOP; /* Vertical Dilution of Precision (DOP), unsigned, 0.1 per bit, no units */
    McpU8 zzz_align2; /* alignment                                          */
    McpU16 heading_true; /* True heading, unsigned, pi/2^15 radians per bit    */
    McpU16 heading_magnetic; /* Magnetic heading, unsigned, pi/2^15 radians per bit */
    McpU16 loc_angle_unc; /* Location angle uncertainty (axis), unsigned, pi/2^15 radians per bit */
    McpU16 loc_angle_unc_on_axis; /* Location uncertainty on axis, unsigned, 0.1 meters per bit */
    McpU16 loc_angle_unc_perp_axis; /* Location uncertainty perpendicular to axis, unsigned, 0.1 meters per bit */
    McpU8 utc_hours; /* UTC hours, unsigned, 1 hour per bit                */
    McpU8 utc_minutes; /* UTC minutes, unsigned, 1 minute per bit            */
    McpU8 utc_seconds; /* UTC seconds, unsigned, 1 second per bit            */
    McpU8 utc_tenths; /* UTC tenths of seconds, unsigned, 1/10 second per bit */
    McpU8 zzz_align3; /* alignment                                          */
    McpU8 zzz_align4; /* alignment                                          */
    McpU8 zzz_align5; /* alignment                                          */
    McpU8 c_position_sv_info; /* counter                                            */
    T_GPSC_position_sv_info position_sv_info[GPSC_RAW_MAX_SV_REPORT]; /*< 52:136> Information for SV used in position fix            */
} T_GPSC_position;

/*
* Raw (native GPS baseband format) position response
*/
typedef struct
{
    McpU16 report_num; /*<  0:  2> Report number                                      */
    McpU16 num_requested_reports; /*<  2:  2> Number of requested reports                        */
    T_GPSC_toa toa; /*<  4: 12> Time of applicability for reported data            */
    T_GPSC_position position; /*< 16:188> Raw position                                       */
} T_GPSC_raw_position;

/*
* enum to Variable time_tag_info
* Time tag information
*/
typedef enum
{
    GPSC_TIME_TAG_NONE = 0x0, /* No time known                  */
    GPSC_TIME_TAG_SUBMEC = 0x1, /* Sub-msec known                 */
    GPSC_TIME_TAG_SUBBIT_SUBMEC = 0x2, /* Sub-bit and sub-msec known     */
    GPSC_TIME_TAG_MSEC_SUBBIT_SUBMEC = 0x3 /* Msec, sub-bit and sub-msec known */
} T_GPSC_time_tag_info;

/*
* Raw measurement
*/
typedef struct
{
    McpU8 svid; /* Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64) */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
    McpU8 zzz_align2; /* alignment                                          */
    McpU32 time_tag_info; /* (enum=32bit)<->T_GPSC_time_tag_info Time tag information */
    McpU16 snr; /* SV signal to noise ratio, unsigned, 0.1 dB per bit */
    McpU16 cno_tenths; /* SV C/No measurement, unsigned, 0.1 dB per bit      */
    McpS16 latency_ms; /* SV latency, signed, 1 msec per bit                 */
    McpU8 pre_int; /* Pre int, unsigned, 1 msec per bit                  */
    McpU8 zzz_align3; /* alignment                                          */
    McpU16 post_int; /* Post int, unsigned, 1 msec per bit                 */
    McpU8 zzz_align4; /* alignment                                          */
    McpU8 zzz_align5; /* alignment                                          */
    McpU32 msec; /* SV time millisecond, unsigned, 1 msec per bit      */
    McpU32 sub_msec; /* SV time sub-millisecond, unsigned, 1/2^24 msec per bit */
    McpU16 sv_time_uncertainty; /* SV time uncertainty, unsigned, 1 nsec per bit, mantissa in bits 15:5 and exponent in bit 4:0 */
    McpU8 zzz_align6; /* alignment                                          */
    McpU8 zzz_align7; /* alignment                                          */
    McpS32 sv_speed; /* SV speed, signed, 0.01 meters/sec per bit          */
    McpU32 sv_speed_uncertainty; /* SV speed uncertainty, unsigned, 0.1 meters/sec per bit */
    McpU16 meas_status_bitmap; /* T_GPSC_meas_status_bitmap,  Measurement status bitmap */
    McpU8 channel_meas_state; /* channel measurement state. 1 byte                  */
    McpU8 zzz_align8; /* alignment                                          */
    McpS32 accum_carrier_phase; /* accumulated carrier phase. signed, -262144 m to 262144 m */
    McpS32 carrier_vel; /* carrier velocity. signed, -131072m/sec to 131072m/sec */
    McpS16 carrier_acc; /* carrier acceleration. signed, -128m/s^2,+128m/s^2  */
    McpU8 loss_lock_ind; /* loss of lock indication. 0-255                     */
    McpU8 good_obv_cnt; /* good observation. unsigned, 0 to 255               */
    McpU8 total_obv_cnt; /* totalobservation. unsigned, 0 to 255               */
    McpS8 elevation; /*< 57:  1> alignment                                          */
    McpU8 azimuth; /*< 58:  1> alignment                                          */
    McpU8 zzz_align11; /* alignment                                          */
} T_GPSC_measurement;

/*
* Raw (native GPS baseband format) measurement response
*/
typedef struct
{
    McpU16 report_num; /*<  0:  2> Report number                                      */
    McpU16 num_requested_reports; /*<  2:  2> Number of requested reports                        */
    T_GPSC_toa toa; /*<  4: 12> Time of applicability for reported data            */
    McpU8 zzz_align0; /*< 16:  1> alignment                                          */
    McpU8 zzz_align1; /*< 17:  1> alignment                                          */
    McpU8 zzz_align2; /*< 18:  1> alignment                                          */
    McpU8 c_measurement; /*< 19:  1> counter                                            */
    T_GPSC_measurement measurement[GPSC_RAW_MAX_SV_REPORT]; /*< 20:1020> Raw measurement                                    */
    McpU16 assist_availability_flags;
    McpU32 eph_availability_flags;
    McpU32 pred_eph_availability_flags;
    McpU32 alm_availability_flags;
    McpU8 u_IsValidMeas;
    McpU32 q_GoodStatSvIdBitMap;
    McpDBL d_PosUnc;
} T_GPSC_raw_measurement;

/*
* NMEA response
*/
typedef struct
{
    McpS8 nmea_string[800]; /*<  0:800> NMEA sentences string                              */
} T_GPSC_nmea_response;

/*
* Location fix response union
*/
typedef union
{
    T_GPSC_prot_position prot_position; /*<  0: 32> Protocol position response                         */
    T_GPSC_prot_measurement prot_measurement; /*<  0:200> Protocol measurement response                      */
    T_GPSC_raw_position raw_position; /*<  0:204> Raw (native GPS baseband format) position response */
    T_GPSC_raw_measurement raw_measurement; /*<  0:1040> Raw (native GPS baseband format) measurement response */
    T_GPSC_nmea_response nmea_response; /*<  0:800> NMEA response                                      */
} T_GPSC_loc_fix_response_union;

typedef enum
{
    GPSC_RESULT_PROT_POSITION = 0x0,
    GPSC_RESULT_PROT_MEASUREMENT = 0x1,
    GPSC_RESULT_RAW_POSITION = 0x2,
    GPSC_RESULT_RAW_MEASUREMENT = 0x3,
    GPSC_RESULT_NMEA = 0x4
} T_GPSC_ctrl_loc_fix_response_union;

/*
* Location fix response
*/
typedef struct
{
    McpU32                         ctrl_loc_fix_response_union; /* T_GPSC_ctrl_loc_fix_response_union enum       */
    T_GPSC_loc_fix_response_union  loc_fix_response_union;      /* Location fix response union                   */
    McpU32                         loc_fix_session_id;          /* Session Tag ID. The session tag ID is a 2 byte value. The first byte contains the transport medium (RRC/RRLP) or Application Identity. The second byte contains the idenitity (Measurement Identity/Reference Number) for the session. */
    int 								MeasorFix;
    McpU8                          loc_fix_pos_type_bitmap;   /* Location fix position type ; last 3 bit indicate the type of position is requested, any combination can be used
                                                              if bleneded position is needed
                                                              000 GPS Only (Default)
                                                              001 GPS Only (Default)
                                                              010 Wifi Only
                                                              100 Sensor Only
                                                              011 Blended */
    McpU8                          loc_fix_req_mode;
    McpU8                          zzz_align1[2];            /* alignment */
    McpU32						 live_debug_flag;		   /* The bits in the field is used to convey some live debug information.
                                                           Mainly used for Sensor and WiFi live debug */
} T_GPSC_loc_fix_response;

typedef T_GPSC_loc_fix_response TNAVC_locFixReport;

typedef struct
{
    McpU16 blf_fix_count;
    McpU16 blf_fix_count_threshold;
    McpU16 blf_fix_count_rx;
    McpU8 blf_sts;
    McpU8 blf_position_sts;
    McpU8 blf_velocity_sts;
    McpU8 blf_dop_sts;
    McpU8 blf_sv_sts;
} TNAVC_BlfStatusReport;

/***********************************************************/
/* NAVC_EVT_ASSISTANCE_REQUEST event parameters           */
/***********************************************************/

/*
* Navigation data
*/
typedef struct
{
    McpU8 svid; /* Space Vehicle ID, 0 - 63 (represents SV PRN 1 - 64) */
    McpU8 iode; /* ICD-200                                            */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
} T_GPSC_nav_data;

/*
* Navigation model parameters
*/
typedef struct
{
    McpU16 gps_week; /* GPS week number                                    */
    McpU8 nav_toe; /* GPS Time of ephemeris in hours                     */
    McpU8 nav_num_svs; /* Number of satellites for which data is provided    */
    McpU8 nav_toe_limit; /* Ephemeris age tolerance                            */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
    McpU8 zzz_align2; /* alignment                                          */
    T_GPSC_nav_data nav_data[32]; /* Navigation data                                    */
} T_GPSC_nav_model_req_params;

typedef enum
{
    GPSC_REQ_ALMANAC = 0x1, /* Almanac requested              */
    GPSC_REQ_UTC = 0x2, /* UTC model requested            */
    GPSC_REQ_IONO = 0x4, /* Ionospheric model requested    */
    GPSC_REQ_NAV = 0x8, /* Navigation model requested     */
    GPSC_REQ_DGPS = 0x10, /* DGPS corrections requested     */
    GPSC_REQ_LOC = 0x20, /* Reference location requested   */
    GPSC_REQ_TIME = 0x40, /* Reference time requested       */
    GPSC_REQ_AA = 0x80, /* Acquisition assistance requested */
    GPSC_REQ_RTI = 0x100 /* Real-time integrity requested  */
} T_GPSC_assist_bitmap_mandatory;

/*
* Current Gps Time parameters
*/
typedef struct
{
    McpU16   gps_week;          /* GPS week number, if '0' or maxed out, time in unknown  */
    McpU32   Msec;				/* GPS msec, if maxed out, time is unknown			    */
    McpFLT   Uncertainty;		/* time uncertainty in milisec							*/
}T_GPSC_current_gps_time;

typedef struct
{
    McpU16 						uAssistBitmapMandatory;
    McpU16 						uAssistBitmapOptional;
    T_GPSC_nav_model_req_params tNavModelReqParams;
    my_Position position;
    T_GPSC_supl_qop qop;
    T_GPSC_current_gps_time		tGpsTime;
} TNAVC_assistReq;

typedef struct
{
	McpU32 			SessID;
	McpBool 		stopAll;
} TNAVC_stopReq;


/***********************************************************/
/* NAVC_EVT_PLT_RESPONSE event parameters                  */
/***********************************************************/

/*
* enum to UnionController prodtest_response_union 
*/
typedef enum
{
    GPSC_RESULT_RTC_OSCTEST = 0x0,
    GPSC_RESULT_GPS_OSCTEST = 0x1,
    GPSC_RESULT_SIGACQ_TEST = 0x2,
    GPSC_RESULT_CW_TEST = 0x3,
    GPSC_RESULT_GPIO_TEST = 0x4,
    GPSC_RESULT_SYNC_TEST = 0x5,
    GPSC_RESULT_SELF_TEST = 0x6,
    GPSC_RESULT_RAM_CHECKSUM_TEST = 0x7
} T_GPSC_ctrl_prodtest_response_union;

/*
* RTC oscillator offset response
*/
typedef struct
{
    McpS16 rtcoscoffset; /* Range: -32.768 Hz to 32.767 Hz                     */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
} T_GPSC_rtc_osctest;

/*
* GPS oscillator offset response
*/
typedef struct
{
    McpS16 gpsoscoffset; /* Range: -32.768 Hz to 32.767 Hz                     */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
} T_GPSC_gps_osctest;

/*
* Signal Acquisition Test response
*/
typedef struct
{
    McpU8 numofsv; /* Range: 0 to 12                                     */
    McpU8 svprn[12]; /* Range: 1 to 32                                     */
    McpU8 zzz_align0; /* alignment                                          */
    McpU16 svcno[12]; /* Range: 0 to 65535 dB                               */
    McpU8 zzz_align1; /* alignment                                          */
    McpU8 zzz_align2; /* alignment                                          */
} T_GPSC_sigacq_test;

/*
* gpio test request  parameters
*/
typedef struct
{
    McpU16 status_value; /* GPIO status value                                  */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
} T_GPSC_gpio_test;

/*
* Sync Test response
*/
typedef struct
{
    McpU8 state; /* state value                                        */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
    McpU8 zzz_align2; /* alignment                                          */
    McpU32 on_time; /* State On Time value                                */
    McpU32 start_time; /* State Start Time value                             */
    McpU32 code_sync_time; /* Code Sync Time value                               */
    McpU32 bit_sync_time; /* Bit Sync Time value                                */
    McpU32 frame_sync_time; /* Frame Sync Time value                              */
} T_GPSC_sync_test;

/*
* Self Test response
*/
typedef struct
{
    McpU8 self_test_result; /* Self test result value                             */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
    McpU8 zzz_align2; /* alignment                                          */
} T_GPSC_self_test;

/*
* Ram checksum test response
*/
typedef struct
{
    McpU8 checksumresult; /* Checksum Result                                    */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
    McpU8 zzz_align2; /* alignment                                          */
} T_GPSC_ram_checksum_test;

/*
* Production line test response union
*/
typedef union
{
    T_GPSC_rtc_osctest rtc_osctest; /* RTC oscillator offset response                     */
    T_GPSC_gps_osctest gps_osctest; /* GPS oscillator offset response                     */
    T_GPSC_sigacq_test sigacq_test; /* Signal Acquisition Test response                   */
    T_GPSC_cw_test cw_test; /* Continuous Wave Test response                      */
    T_GPSC_gpio_test gpio_test; /* gpio test request  parameters                      */
    T_GPSC_sync_test sync_test; /* Sync Test response                                 */
    T_GPSC_self_test self_test; /* Self Test response                                 */
    T_GPSC_ram_checksum_test ram_checksum_test; /* Ram checksum test response                         */
} T_GPSC_prodtest_response_union;

/*
* Production line test  response
*/
typedef struct
{
    McpU32 ctrl_prodtest_response_union; /* T_GPSC_ctrl_prodtest_response_union */
    T_GPSC_prodtest_response_union prodtest_response_union; /*<  4:2084> Production line test response union                */
} T_GPSC_prodtest_response;
typedef T_GPSC_prodtest_response TNAVC_pltResponse;



/***********************************************************/
/* NAVC_EVT_VERSION_RESPONSE event parameters                  */
/***********************************************************/

/*
* Response for HW and SW version request
* CCDGEN:WriteStruct_Count==49
*/
typedef struct
{
    McpU8 hw_major; /* Bits 7 - 4: major version of byte value 1          */
    McpU8 hw_minor; /* Bits 3 - 0: minor version of byte value 1          */
    McpU8 sw_major; /* Bits 31- 24: major version of byte value 4         */
    McpU8 sw_minor; /* Bits 23 - 16: minor version of byte value 4        */
    McpU8 sw_subminor1; /* Bits 15 - 8: sub-minor 1 of byte value 4           */
    McpU8 sw_subminor2; /* Bits 7 - 0: sub-minor 2 of byte value 4            */
    McpU8 sw_day; /* Unsigned. Range 1 to 31.                           */
    McpU8 sw_month; /* Unsigned. Range 1 to 12.                           */
    McpU16 sw_year; /* Unsigned. 2003 to 2999.                            */
    McpU8 zzz_align0; /* alignment                                          */
    McpU8 zzz_align1; /* alignment                                          */
} TNAVC_verResponse;

/***********************************************************/
/* NAVC_EVT_RRLP_RESP/NAVC_EVT_RRC_RESP event parameters                  */
/***********************************************************/

/* NAVC_EVT_RRLP/RRC_RESP event parameters, used by RRLP and RRC libraries */
typedef struct
{
    McpU32 uSystemTimeInMsec;
    McpU16 uRrFrameLength;
    McpU8 uRRpayload[1024];

} TNAVC_rrFrame;

/***********************************************************/
/* NAVC_EVT_ASSIST_REP_EPH event parameters                  */

/***********************************************************/
typedef struct
{
    T_GPSC_ephemeris_assist ephemeris_assist[32]; /*<  0:2048> Ephemeris assistance data                          */
} T_GPSC_ephemeris_assist_rep;

/***********************************************************/
/* NAVC_EVT_ASSIST_REP_IONO event parameters                  */
/***********************************************************/
/* see T_GPSC_iono_assist */

/***********************************************************/
/* NAVC_EVT_ASSIST_REP_ALMANAC event parameters                  */

/***********************************************************/
typedef struct
{
    T_GPSC_almanac_assist almanac_assist[32]; /*<  0:1024> Almanac assistance data                            */
} T_GPSC_almanac_assist_rep;

/***********************************************************/
/* NAVC_EVT_ASSIST_REP_TIME event parameters                  */
/***********************************************************/
/* see T_GPSC_time_assist */

/***********************************************************/
/* NAVC_EVT_ASSIST_REP_POSITION event parameters                  */
/***********************************************************/
/* see T_GPSC_position_assist */

/***********************************************************/
/* NAVC_EVT_ASSIST_REP_TTL event parameters                  */

/***********************************************************/
typedef struct
{
    McpU32 iono_ttl;
    McpU32 refTime_ttl;
    McpU32 refPosition_ttl;
    McpU32 utc_ttl;
    McpU32 eph_ttl[32];
    McpU32 alm_ttl[32];
    McpU32 acq_ttl[32];
    McpU32 rti_ttl[32];

} T_GPSC_ttl_assist_rep;

/***********************************************************/
/* NAVC_EVT_ASSIST_REP_XXX event parameters                  */

/***********************************************************/
typedef enum
{
    GPSC_ASSIST_EPH_REP = 0x0,
    GPSC_ASSIST_IONO_REP = 0x1,
    GPSC_ASSIST_ALMANAC_REP = 0x2,
    GPSC_ASSIST_TIME_REP = 0x3,
    GPSC_ASSIST_POSITION_REP = 0x4,
    GPSC_ASSIST_UTC_REP = 0x5,
    GPSC_ASSIST_TTL_REP = 0x6
} T_GPSC_ctrl_assistance_inject_union_report;

typedef union
{
    T_GPSC_ephemeris_assist_rep ephemeris_assist_rep; /*<  0:2048> Ephemeris assist report                            */
    T_GPSC_iono_assist iono_assist; /*<  0:  8> Ionospheric assistance data                        */
    T_GPSC_almanac_assist_rep almanac_assist_rep; /*<  0:1024> Ephemeris assist report                            */
    T_GPSC_time_assist time_assist; /*<  0: 20> Time of the week data                              */
    T_GPSC_position_assist position_assist; /*<  0: 16> Position assistance data                           */
    T_GPSC_utc_assist utc_assist; /*<  0: 16> UTC assistance data                                */
    T_GPSC_ttl_assist_rep ttl_assist; /* TTL values for each of the assistance data available */
} T_GPSC_assistance_inject_union_report;

typedef struct
{
    T_GPSC_ctrl_assistance_inject_union_report ctrl_assistance_inject_union_report; /*<  0:  4> (enum=32bit) controller for union                  */
    T_GPSC_assistance_inject_union_report assistance_inject_union_report; /*<  4:2048> Assistance injection data union report             */
} T_GPSC_assistance_database_report;

/***********************************************************/
/* T_GPSC_agc_params event parameters                  */

/***********************************************************/
typedef struct
{
    McpS32 vgaInput;
    McpS32 lnaInput;
} T_GPSC_agc_params;

/***********************************************************/
/* event parameters union (to calc max event size)     */
/***********************************************************/
//typedef T_GPSC_ephemeris_assist TNAVC_assistEphemeris;

/***********************************************************/
/* NAVC_EVT_SV_STATUS_REPORT event parameters                  */

/***********************************************************/
typedef struct
{
    McpU8 svprn; /*<  0:  1> Range: 1 to 32                                     */
    McpU8 zzz_align0; /*<  1:  1> alignment                                          */
    McpU16 snr; /*<  2:  2> SV signal to noise ratio, unsigned, 0.1 dB per bit */
    McpS8 elevation; /*<  4:  1> Signed, 180/256 degree per bit,Range: -90 to 90 - 180/256 degrees */
    McpU8 azimuth; /*<  5:  1> Unsigned, 360/256 degrees per bit,Range: 0 to 360 - 360/256 degrees */
    McpU8 iode; /*<  6:  1> iode                                          */
    McpU8 zzz_align2; /*<  7:  1> alignment                                          */

} T_GPSC_sv_status_param;

typedef struct
{
    McpU8 num_svs; /*<  0:  1> Number of visible SVs range from 1 to 32           */
    McpU8 zzz_align0; /*<  1:  1> alignment                                          */
    McpU8 zzz_align1; /*<  2:  1> alignment                                          */
    McpU8 zzz_align2; /*<  3:  1> alignment                                          */
    McpU32 ephemeris_bitmask; /*<  4:  4> Bit map indicates the SVs which has the ephemeris data, 0 indicates no data, 1 indicates the valid data in each bit 0-32 */
    McpU32 almanac_bitmask; /*<  8:  4> Bit map indicates the SVs which has the almanac data, 0 indicates no data, 1 indicates the valid data in each bit 0-32 */
    McpU32 sv_used_in_fix_bitmask; /*< 12:  4> Bit mask indicating which SVs were used for computing the most recent position fix */
    T_GPSC_sv_status_param sv_status_param[32]; /*< 16:256> SV status parameter                                */
} T_GPSC_sv_status;
typedef T_GPSC_sv_status TNAVC_svStatus;

/*
* Response for HW and SW version request
* CCDGEN:WriteStruct_Count==46
*/
typedef struct
{
    McpU8 ChipIDMajorVerNum;
    McpU8 ChipIDMinorVerNum;
    McpU8 ROMIDMajorVerNum;
    McpU8 ROMIDMinorVerNum;
    McpU8 ROMIDSubMinor1VerNum;
    McpU8 ROMIDSubMinor2VerNum;
    McpU8 ROMDay;
    McpU8 ROMMonth;
    McpU16 ROMYear;
    McpU8 PATCHMajor;
    McpU8 PATCHMinor;
    McpU8 PATCHDay;
    McpU8 PATCHMonth;
    McpU16 PATCHYear;
    McpU8 gpsc_major; /*<  2:  1> Bits 31- 24: major version of byte value 4         */
    McpU8 gpsc_minor; /*<  3:  1> Bits 23 - 16: minor version of byte value 4        */
    McpU8 gpsc_subminor1; /*<  4:  1> Bits 15 - 8: sub-minor 1 of byte value 4           */
    McpU8 gpsc_subminor2; /*<  5:  1> Bits 7 - 0: sub-minor 2 of byte value 4            */
    McpU8 gpsc_day; /*<  6:  1> Unsigned. Range 1 to 31.                           */
    McpU8 gpsc_month; /*<  7:  1> Unsigned. Range 1 to 12.                           */
    McpU16 gpsc_year; /*<  8:  2> Unsigned. 2003 to 2999.                            */
    McpU8 chipID_str[20];
    McpU8 navilink_ver[64]; /*Sharath: create #def for length */
    McpU8 navc_ver[64]; /*Sharath: create #def for length */
} T_GPSC_gps_ver_resp_params;


/***********************************************************/
/* NAVC_EVT_ASSIST_SRC_REP_PRIORITY event parameters                  */

/***********************************************************/
typedef struct
{
    ENAVC_assist_src_type eAssistSrcType; /*<  enum for assistance provider type (0 to MAX_ASSIST_PROVIDER)                                     */
    McpU8 assist_src_priority; /*<  priority ranges from 1 to MAX_ASSIST_PROVIDER and 0 is for disable the assistance src*/

} TNAVC_assist_src_priority_rep;

/***********************************************************/
/* event parameters union (to calc max event size)     */
/***********************************************************/
typedef union
{
    TNAVC_locFixReport 	tLocFixReport;
    TNAVC_BlfStatusReport   tBlfStatusReport ;  /* Parameters for BLF */
    TNAVC_assistReq	   	tAssistReq;
    TNAVC_stopReq	   	tStopReq;
    TNAVC_rrFrame	   	tRrFrame;
    TNAVC_pltResponse  	tPltResponse;
    TNAVC_verResponse  	tVerResponse;
    TNAVC_svStatus		tSvStatus;

    /* TBC - need to understand the need of this element as
    we have one more with one more with different struct */
    TNAVC_assistEphemeris tAssistSingleEphemeris;

    T_GPSC_ephemeris_assist_rep 	tAssistEphemeris;
    T_GPSC_almanac_assist_rep 	tAssistAlmanac;
    T_GPSC_iono_assist 			tAssistIono;
    T_GPSC_time_assist 			tAssistTime;
    T_GPSC_position_assist 		tAssistPosition;          /*<  0: 16> Position assistance data                           */
    T_GPSC_utc_assist         tAssistUTC;
    T_GPSC_ttl_assist_rep	  tAssistTTL;
    T_GPSC_wishlist_params		tWishlist;
    T_GPSC_agc_params		tAgc;
    TNAVC_MotionMask_Status tMotionMaskStatus; //ashish
    TNAVC_GetMotionMask     tMotionMaskSettings;
    TNAVC_assist_src_priority_rep  tAssistSrcPriorityRep[MAX_ASSIST_PROVIDER];
    TNAVC_AI2_RAW_MSG tAI2RawMsg;
    McpU32 tErrCode;
    TNAVC_gpsWifiBlendReport tGpsWifiBlendReport;
} TNAVC_evtParams;


/***********************************************************/
/* NAVC event structure                                    */

/***********************************************************/
typedef struct
{
    ENAVC_cmdOpcode eComplCmd;
    EMcpfRes eResult;
    TNAVC_evtParams tParams;
} TNAVC_evt;


/******************NAVC DEFINTIONS END**********************************/

/* MsgClass */
enum
{
    NAVL_CLASS_INTERNAL,
    NAVL_CLASS_GPS,
    NAVL_CLASS_RR,
    NAVL_CLASS_SUPL,
    NAVL_CLASS_SUPL_ADS,
    NAVL_CLASS_WPL,
};

#endif /* __NAVILINK_CUSTOM_H */
