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
## Control List (“CCL”). The assurances provided for herein are furnished in  *
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

/** \file   navc_rrc.c
 *  \brief  RRC (Radio Resource Control Protocol) module implementation
 *
 * This file contains functions to decode RRC messages received from the SMLC
 * (Serving Mobile Location Centre) and to encode RRC messages tranmitted to SMLC.
 *
 *
 *  \see    navc_rrc.h
 */

#include "mcpf_defs.h"
#include "mcpf_mem.h"
#include "mcpf_msg.h"
#include "mcpf_report.h"
#include "mcpf_services.h"
#include "mcp_hal_memory.h"
#include <math.h>
#include "navc_defs.h"
#include "navc_api.h"
#include "navc_internalEvtHandler.h"
#include "navc_cmdHandler.h"
#include "navc_rrc.h"
#include "navl_api.h"
#include "mcp_hal_os.h"

#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"
#include <utils/Log.h>


/************************************************************************
 * Defines
 ************************************************************************/

/* uAvailabilityFlags bitmask values are equal to GPSC_REQ_xxx bits,
 * extension is defined bellow
 */
#define C_ANGLE_AVAIL        0x0400
#define C_REF_GSMTIME_AVAIL  0x0800 		/* optional for ref time */
#define C_REF_TOW_AVAIL      0x1000 		/* optional for ref time */

/* RRC message first choice tag */
enum
{
	RRC_CHOICE_ASSIST_DATA_DELIVERY 	= 1,
	RRC_CHOICE_MSR_CTRL 				= 8,

} ERRC_ChoiceTag;

enum
{
  RRC_CHOICE_ASSIST_DATA_DELIVERY_R3,
  RRC_CHOICE_ASSIST_DATA_DELIVERY_LATER_R3

} ERRC_ChoiceTagAssitDelivery;

#define C_ASSIS_OPT_PREAM_V3A0NONCRTCLEXT 	0x01 /* Bit 0: v3a0NonCriticalExtensions */

#define C_ASSIS_OPT_PREAM_OTDOAASSIST 		0x01 /* Bit 0: OTDOA Assistance */
#define C_ASSIS_OPT_PREAM_GPSASSIST 		0x02 /* Bit 1: GPS   Assistance	*/

/* GPS Assistance Data Options bits */
#define C_GPS_ASSIST_REFERENCE_TIME 		0x0200
#define C_GPS_ASSIST_REFERENCE_LOCATION		0x0100
#define C_GPS_ASSIST_DGPS_CORRECTIONS		0x0080
#define C_GPS_ASSIST_NAVIGATION_MODEL		0x0040
#define C_GPS_ASSIST_IONO_MODEL				0x0020
#define C_GPS_ASSIST_UTC_MODEL				0x0010
#define C_GPS_ASSIST_ALMANAC				0x0008
#define C_GPS_ASSIST_ACQUISITION_ASSIST		0x0004
#define C_GPS_ASSIST_RTI					0x0002
#define C_GPS_ASSIST_DUMMY					0x0001


/* GPS Assistance Data Options - Reference Time option bits */
#define C_REFERENCE_TIME_UTRAN_GPS_REFERENCE_TIME 		0x08
#define C_REFERENCE_TIME_SFN_TOW_UNCERTAINTY 			0x04
#define C_REFERENCE_TIME_UTRAN_GPS_DRIFTRATE 			0x02
#define C_REFERENCE_TIME_GPS_TOW_ASSISTLIST 			0x01


enum
{
  RRC_CHOICE_MSRCTRL_R3,
  RRC_CHOICE_MSRCTRL_LATER_R3

} ERRC_ChoiceTagMsrControl;


enum
{
  RRC_CHOICE_MSRCTRL_R3_COMMAND_SETUP,
  RRC_CHOICE_MSRCTRL_R3_COMMAND_MODIFY,
  RRC_CHOICE_MSRCTRL_R3_COMMAND_RELEASE

} ERRC_ChoiceTagMsrControlR3;

#define C_MSRCONTROL_OPT_MSR_REPORT_MODE 				0x04
#define C_MSRCONTROL_OPT_ADDITIONAL_MSR_LIST			0x02
#define C_MSRCONTROL_OPT_DPCH_COMPRES_MODE_STATUS_INFO 	0x01

typedef struct
{
  McpU8		uTransferMode;
  McpU8		uPeriodicalOrEventTrigg;

} TReportMode;


enum
{
  RRC_CHOICE_MSR_TYPE_INTRAFREQMSR,
  RRC_CHOICE_MSR_TYPE_INTERFREQMSR,
  RRC_CHOICE_MSR_TYPE_INTERRATMSR,
  RRC_CHOICE_MSR_TYPE_POSMSR,
  RRC_CHOICE_MSR_TYPE_TRAFVOLMSR,
  RRC_CHOICE_MSR_TYPE_QUALMSR,
  RRC_CHOICE_MSR_TYPE_INTMSR

} ERRC_ChoiceTagMsrCmd_MsrType;

#define C_ASSIS_OPT_POS_OTDO_AASSIST  	0x02
#define C_ASSIS_OPT_POS_GPS_ASSIST  	0x01


/* RRC/RRLP Position Method type */
enum
{
	POS_METHOD_TYPE_MS_ASSISTED,
	POS_METHOD_TYPE_MS_BASED,
	POS_METHOD_TYPE_MS_BASED_PREF,
	POS_METHOD_TYPE_MS_ASSISTED_PREF

} ERRC_PosMethodType;



/* Positioning Reporting Quantity Measurment - Positioning Method */
enum {
	RRC_POSITION_METHOD_OTDOA,
	RRC_POSITION_METHOD_GPS,
	RRC_POSITION_METHOD_OTDOA_GPS,
	RRC_POSITION_METHOD_CELLID

} ERRC_PositioningMethod;


#define C_POSITION_REPORT_OPT_PREAM_ENV_CHARAC   	0x01
#define C_POSITION_REPORT_OPT_PREAM_HORIZ_ACCURACY 	0x02

enum
{
  RRC_CHOICE_POS_REPORT_EVNTLST,
  RRC_CHOICE_POS_REPORT_PERIODIC,
  RRC_CHOICE_POS_REPORT_NO

} ERRC_ChoiceTagPositioningReportCriteria;


enum
{
  C_RRC_CHOICE_POS_THRESH_POSCNG,
  C_RRC_CHOICE_POS_THRESH_SFNCHG,
  C_RRC_CHOICE_POS_THRESH_GPSCHG

} ERRC_ChoiceTagPositioningEventSpecificInfo;

enum
{
	C_RRC_CHOICE_MODE_SPECIFIC_INFO_FDD,
	C_RRC_CHOICE_MODE_SPECIFIC_INFO_TDD

} ERRC_ChoiceModeSpecificInfo;

/* RRC measurement response types */
#define RRC_MSRRESP_OPT_PREAM_NON_EXT     		0x01
#define RRC_MSRRESP_OPT_PREAM_EVENT_RESULT    	0x02
#define RRC_MSRRESP_OPT_PREAM_MSR_RESULT_LIST 	0x04
#define RRC_MSRRESP_OPT_PREAM_MSR_RSLT_RACH 	0x08
#define RRC_MSRRESP_OPT_PREAM_MSR_RSLT 			0x10

/* RRC UE-Positioning-Measurement-v390ext options */
#define RRC_POS_MSR_V390EXT_REP_QUANTITY   			0x04
#define RRC_POS_MSR_V390EXT_VALIDITY    			0x02
#define RRC_POS_MSR_V390EXT_OTDOA_ASSISTDATA_UEB 	0x01



#define C_LSB_LAT_REP			(65536.0 * 65536.0 / C_PI)
#define C_LSB_LON_REP			(65536.0 * 32768.0 / C_PI)
#define C_LSB_HT_REP			2

#define C_RECIP_LSB_POS_SIGMA 	10.0

#define C_EARTH_RADIUS			6371.04
#define C_PI					3.1415926535898
#define C_RAD_TO_DEG           (180.0 / C_PI )   /* factor converting from
                                                    radius to degree */
#define C_DEG_TO_RAD           (C_PI / 180.0 )   /* factor converting from
                                                    degree to radius */

/* DGPS Assistance Constants */
#define C_PRC_SCALEFACTOR		0.32
#define C_RRC_SCALEFACTOR		0.032
#define C_PRC_OFFSET			655.04	     /* 2^11-1*0.32 */
#define C_RRC_OFFSET			4.064	     /* 2^7-1*0.032 */

#define C_PRC_AI2_SCALEFACTOR  	0.02
#define C_RRC_AI2_SCALEFACTOR  	0.002

#define RRC_MAX_MEAS_EVENT  8
#define RRC_MAX_SAT			16


/************************************************************************
 * Types
 ************************************************************************/

typedef union
{
  McpU8		uThresholdPosChg;
  McpU8		uThresholdSfnChg;
  McpU8		uThresholdSfnGpsTow;

} TEventSpecificInfoChoice;


typedef struct
{
  McpU8		uReportingAmount;
  McpU8		uReportFirstFix;
  McpU8		uPosMsrInt;
  TEventSpecificInfoChoice	tEventSpecificInfoChoice;

} TEventParamLst;

typedef struct
{
  McpU8  uSvId;          /* 1-64; 0 used to indicate invalid */
  McpS16 sDoppler0;      /* 12 bit data, sign extended. 0'th order doppler:
                          -2048 to 2047, LSB for 2.5Hz */
  McpU8  uDoppler1;      /* 6 bit data. 1'st order doppler: 0-63, representing
                          (-1 to 0.5 Hz/sec ). Optional (0xFF indicates not
                          present)  */
  McpU8  uDopplerUnc;    /* 3 bit data. representing doppler unc of
                          [ 2**(-u_DopplerUnc) * 200 ].   Optional (0xFF
                          indicates not present)  */

  McpU16 uCodePhase;     /* 10 bit data */
  McpU8  uIntCodePhase;  /* 5 bit data */
  McpU8  uGpsBitNum;     /* 2 bit data */
  McpU8  uSrchWin;       /* 4 bit data, Code phase search window */

} TAcqElement;  		/* raw acquisition assistance's 2's complement */

typedef struct
{
  McpU8  uAzimuth;    		/* 5 bit data. Resolution = 11.25 degrees.  */
  McpU8  uElevation;  		/* 3 bit data, Resolution = 11.25 degrees. 0xFF to indicate invalid */

} TAdditionalAngle;  	/* raw acquisition assistance's data */


typedef struct
{
  McpU32           uGpsTow;  				/* GPS TimeOfWeek: in msec */
  McpU32		   uAcqAsstListedSVs; 		/* SV List (bitmap) for Visible Satellites */
  McpU8            uNumSV;   				/* Number of SVs in the message */
  TAcqElement      tAcquisElement[RRC_MAX_SVS_ACQ_ASSIST];
  TAdditionalAngle tAddAngle[RRC_MAX_SVS_ACQ_ASSIST];

} TAcquisitionAssist;


/********************    RRC Measurement Response   *******************/


/*-------------------------MeasuredResults-------------------------------*/

/*---------------------------UE-Positioning-Error--------------------------*/

typedef struct
{
	McpU8	satID; 	/* 0..63 	*/
	McpU8	iode;	/* 0..255 	*/

} TSatData;

typedef struct
{
  McpU16		uGpsWeek;
  McpU8			uGpsToe;
  McpU8			uToeLimit;
  McpU8			uSize;
  TSatData		tSatDataList[ RRC_MAX_SAT ];

} TNavModel;

typedef struct
{
	McpU8  		uOptPreambNav;

	McpU8		bAlmanacRequest;
	McpU8		bUtcModelRequest;
	McpU8		bIonosphericModelRequest;
	McpU8		bNavigationModelRequest;
	McpU8 	    bDgpsCorrectionsRequest;
	McpU8 	    bReferenceLocationRequest;
	McpU8 	    bReferenceTimeRequest;
	McpU8 	    bAcquisitionAssistanceRequest;
	McpU8		bRealTimeIntegrityRequest;

	TNavModel	tNavModel;

} TAdditionalAssistDataReq;

typedef struct
{
  McpU8  					uOptParam_R1;
  McpU8						uErrorCause;
  TAdditionalAssistDataReq	tAddAsistDataReq;

} TPosError;


/*---------------------------UE-Positioning-Error--------------------------*/

/*----------------------------UE-Positioning-GPS-MeasurementResults---------*/

typedef struct
{
   McpU8 	uSatelliteID;
   McpU8 	uCN0;
   McpU32 	uDoppler;
   McpU16 	uWholeGPSChips;
   McpU16 	uFractionalGPSChips;
   McpU32	uMultipathIndicator;
   McpU8 	uPseudorangeRMSError;

} TMsrParamLst;


typedef struct
{
  U8		z_CellParamsId;

}MsrResultTddChoice;


typedef struct
{
   U16		z_MsrResultScramblingCode;
}MsrResultFddChoice;


typedef union
{
  MsrResultFddChoice		z_MsrResultFddChoice;
  MsrResultTddChoice		z_MsrResultTddChoice;

} TMsrResultModeSpecificInfoChoice;


typedef struct
{
   McpU16 	uMsPart;
   McpU32 	uLsPart;

} TMsrResultGpsTimingCell;


typedef struct
{
  TMsrResultGpsTimingCell			tMsrResultGpsTimingCell;
  TMsrResultModeSpecificInfoChoice	tMsrResultModeSpecificInfoChoice;
  McpU16							uMsrResultSfn;

} TMsrResultGpsReferencetimeResult;


typedef struct
{
  U32								uMsrResultGpsReferencetimeOnly;
  TMsrResultGpsReferencetimeResult	tMsrResultGpsReferencetimeResult;

} TMsrResultReferencetimeChoice;

#define RRC_POS_MSR_RES_CHOISE_GPS_REF_TIME_ONLY	1

typedef struct
{
  McpU8								uReferencetimeChoice;
  TMsrResultReferencetimeChoice		tMsrResultReferencetimeChoice;

} TMsrResultReferencetime;


typedef struct
{
   McpU8					uLength_R2;
   TMsrResultReferencetime	tMsrResultReferencetime;
   TMsrParamLst				tMsrParamLst[RRC_MAX_SAT];

} TPosGpsMsrResult;


/*----------------------------UE-Positioning-GPS-MeasurementResults---------*/

/*-----------------------------UE-Positioning-PositionEstimateInfo-----------*/

typedef struct
{
  McpU8		uCellChannelId;

} TCellTimingTddChoice;


typedef struct
{
   McpU16		uCellTimingScramblingCode;

} TCellTimingFddChoice;


typedef union
{
  TCellTimingFddChoice		tCellTimingFddChoice;
  TCellTimingTddChoice		tCellTimingTddChoice;

} TCellTimingModespecificInfoChoice;


typedef struct
{
  U16	uSfn;
  TCellTimingModespecificInfoChoice	tCellTimingModespecificInfoChoice;

} TCellTiming;


typedef struct
{
  McpU8		uCellParamsId;

} TTddChoice;


typedef struct
{
   McpU16		uScramblingCode;

} TFddChoice;


typedef union
{
  TFddChoice		tFddChoice;
  TTddChoice		tTddChoice;

} TModeSpecificInfoChoice;


typedef struct
{
   McpU16 	uMsPart;
   McpU32 	uLsPart;

} TGpsTimingCell;


typedef struct
{
  TGpsTimingCell			tGpsTimingCell;
  TModeSpecificInfoChoice	tModeSpecificInfoChoice;
  McpU16					uSfn;

} TGpsReferencetimeResult;


typedef union
{
  McpU32					uGpsReferencetimeOnly;
  TGpsReferencetimeResult	tGpsReferencetimeResult;
  TCellTiming				tCellTiming;

} TReferencetimeChoice;


#define RRC_POS_EST_INFO_CHOISE_GPS_REF_TIME_ONLY	1

typedef struct
{
  McpU8					uRefTimechoice;
  TReferencetimeChoice	tReferenceTimeChoice;

} TReferenceTime;


typedef struct
{
  McpU8		uLatitudeSign;
  McpU32	uLatitude;
  McpU32	uLongitude;
  McpU8		uAltitudeSign;
  McpU16	uAltitude;
  McpU8		uUncMajor;
  McpU8		uUncMinor;
  McpU8		uOrientMajor;
  McpU8		uUncAlt;
  McpU8		uConfidence;

} TEllipPointAltEllip;



typedef union
{
  TEllipPointAltEllip		tEllipPointAltEllip;

} TPosEstimateChoice;

#define RRC_POS_EST_CHOISE_ELLIP_POINT_ALTIT_ELLIP	4

/* UE-Positioning-PositionEstimateInfo */
typedef struct
{
  McpU8					uPosestChoice;
  TPosEstimateChoice	tPosEstimateChoice;

} TPosEstimate;


typedef struct
{
   TReferenceTime		tReferenceTime;
   TPosEstimate			tPosEstimate;

} TPosEstimateInfo;


/*-----------------------------UE-Positioning-PositionEstimateInfo-----------*/

#define C_MSRRPT_OPT_PREAM_OTDOA    0x08
#define C_MSRRPT_OPT_PREAM_LOC      0x04
#define C_MSRRPT_OPT_PREAM_MEAS     0x02
#define C_MSRRPT_OPT_PREAM_POSERR   0x01

/* UE-Positioning-MeasuredResults */
typedef struct
{
  McpU8					uOptPream_R3;
  TPosEstimateInfo		tPosEstimateInfo;
  TPosGpsMsrResult		tPosGpsMsrResult;
  TPosError				tPosError;

} TPositionMeasuredResult;

#define RRC_MSR_RESULT_CHOISE_POSITIONING  	0x06

typedef union
{
  TPositionMeasuredResult	tPosMeasuredResult;

} TPositionMeasuredResultChoice;


typedef struct
{
  McpU8								uMsrRsltChoice;
  TPositionMeasuredResultChoice   	tPosMsrResultChoice;

} TMeasuredResults;

/*----------------------------------MeasuredResults-----------------------------*/

/*--------------------------EventResults----------------------------------*/
typedef union
{
  TPosEstimateInfo		tPosEstimateInfo;
  TPosGpsMsrResult	    tPosGpsMsrResult;

} TMeasuredEventresultsChoice;


typedef union
{
  TMeasuredEventresultsChoice	tMeasuredEventresultsChoice;

} TEventresultsChoice;

/*--------------------------EventResults----------------------------------*/

/*-------------------------MeasuredResultsList----------------------------*/

#define RRC_MAX_ADD_MEAS	4

typedef struct
{
  McpU8				uLength_R6;
  TMeasuredResults	tMeasuredResultsList[RRC_MAX_ADD_MEAS];

} TMeasuredResultsList;

/*-------------------------MeasuredResultsList----------------------------*/

typedef struct
{
   McpU8					uOptPream_R2;
   McpU8					uMeasurementIdentity;	/* 1..16 */
   TMeasuredResults			tMeasuredResults;
   TMeasuredResultsList		tMeasuredResultsList;
   TEventresultsChoice		tEventresultsChoice;

} TMeasurementRpt;

/*-----------------------MeasurementReport----------------------------------------*/

/* -----------------------Measurement Control Fail---------------------------- */

typedef union
{
  McpU8     uSpare;
  McpU8		uProtocolErrorCause;

} TDiagTypeChoice;


typedef struct
{
  TDiagTypeChoice 	tDiagTypeChoice ;

} TProtocolError;


typedef struct
{
  McpU8			uTgpsi;

} TCompRunError;


typedef union
{
   McpU8			tCompRunError;      /* maxTGPS = 6 */
   TProtocolError	tProtocolError;

} TFailureCauseProtChoice;


typedef struct
{
   McpU8					uOptPream_R2;
   McpU8   					uTransIdentifier;
   TFailureCauseProtChoice 	tFailureCauseProtChoice;

} TMeasurementCtrlFail;

/* ---------------------Measurement Control Fail---------------------------- */

typedef union
{
  TMeasurementCtrlFail      tMeasurementCtrlFail;
  TMeasurementRpt   		tMeasurementRpt;

} TUplinkResponseChoice;


typedef struct
{
  McpU8					uOptPreamle_R1;
  McpU8					uChoiceTag_R1;
  TUplinkResponseChoice tUplinkResponseChoice;

} TRrcPdu;

enum
{
  RRC_MSG_TYPE_CHOICE_MSR_CTRLFAIL 	= 7,
  RRC_MSG_TYPE_CHOICE_MSR_REPORT	= 8

} ERrcUL_MessageType;


/********************    RRC Measurement Response End  *******************/



typedef struct
{
	handle_t hMcpf;
	handle_t hCmdPool;
	handle_t hEvtPool;

	EmcpTaskId  eSrcTaskId;
	McpU8		uSrcQId;

	McpU16 	uAvailabilityFlags;
	McpU16 	uWishlist;

	/* current frame params */
	McpU8 	uRrcChoiceTag;
    McpU8 	uTransactionId;
    McpU8 	uMsrId;
	McpU8	uMsrCtrlCmd;
	McpU8	uMsrType;
	McpU8 	uMsgSeqNum;
	McpBool bIntegrityCheck;

	McpU32	uSystemTimeMs;		/* system time (ms) of recieved RRC message */

	/* msrPos-Req 			*/
	/*    positionInstrcut  */
	McpU8 	uMethodType;
	McpU16 	uAccuracy;
    /*	  end-of-positionInstrcut */

	McpU8 	uPosMethod; 			/* eotd (0), gps (1), gpsOrEOTD (2) 		  */
	McpS32 	lLongtitue;
	McpS32 	lLatitude;
	McpS16 	xHeight;
	McpU32 	qUncertainty;

	/* ini configuration */
	McpBool bPosOffsetDefined;
	float	fltPosOffset;
	float	fltPosUncertainty;
	McpU32  uSessionId;

	/* Time estimation */
	TNAVCD_GpsTime  		tGpsTime;
	TAcquisitionAssist 		tAcquisitionAssist;

	/* Real time integrity, bit map of bad satellites */
	/* bit 0: not used, bit 1: SVId 1 not healthy, bit N: SVId N not healthy */
	McpU32	uRealTimeIntegrity[2];

	McpU8			uPosReportCriteriaChoiceTag;
	TEventParamLst	tEvtParamList[RRC_MAX_MEAS_EVENT];
	McpU16			uPeriodReportAmount;
	McpU16   		uPeriodReportingIntLong;
	TReportMode		tReportMode;

} TRrc;

static McpU16 uReportingIntLong_g[16] =
{ 0,250,500,1000,2000,3000,4000,6000,8000,12000,16000,20000,24000,28000,32000,64000 };

/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

static void methodType_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static void gpsTowAssist_decodeAndInject (TRrc   *pRrc,
										  McpU8  *MsgBuf,
										  McpU32 *pOffset);

static void referenceTime_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static void refLocation_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static void dgpsCorrections_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static void navigationModel_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static void ionosphericModel_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static void utcModel_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static void almanac_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static void acquisAssist_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static void seqOfBadSatelliteSet_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static void gpsAssistData_decode (TRrc *pRrc, McpU8* MsgBuf, McpU32 *pOffset);

static void assistDataDelivery_decode (TRrc *pRrc, McpU8* MsgBuf, McpU32 *pOffset);

static void utranGPSReferenceTime_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static McpU16 rrc_decode (TRrc *pRrc, McpU8* MsgBuf, McpU16 MsgLength);

static EMcpfRes processRrcMsg( TRrc *pRrc, McpU8* pMsgBuf, McpU16 uMsgLength);

static EMcpfRes checkAssistDataForLocFix (TRrc *pRrc);

static void sendLocFixReq(TRrc *pRrc);

static void sendAssistanceToNavc (TRrc *pRrc, TNAVC_cmdParams *pNavcCmd);

static void sendLocFixStop (TRrc *pRrc);

static void injectGpsTimeToNavc (TRrc *pRrc, TNAVCD_GpsTime *pGpsTime);

static void rrcDataReset (TRrc *pRrc);

static void gps_msr_info_construct (TRrc 					*pRrc,
									TPosGpsMsrResult 		*pRrcMsr,
									T_GPSC_loc_fix_response *pLoc_fix_response);

static void rrc_gps_msr_encode (TRrc 					*pRrc,
							    TPositionMeasuredResult *pMsrRslt,
								McpU8         			*pMsgBuf,
								McpU32   	    		*pBitOffset);

static void ext_geo_loc_construct (TPosEstimateInfo	 		*pRrcPos,
                                   T_GPSC_loc_fix_response	*pLoc_fix_response);

static void sendRrcResponse (TRrc						*pRrc,
							 T_GPSC_loc_fix_response	*pLocFixResponse,
							 TNAVCD_LocError 			*pLocError,
							 ERrc_MeasrType				eMeasType,
							 ERrc_PosErrorCause			eLocReason);

static void pos_error_construct (TNAVCD_LocError 	*pLocError,
								 ERrc_PosErrorCause eCause,
								 TPosError 			*pRrcPosErr);

static EMcpfRes integrityCheckInfo_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

static void v390nonCriticalExtensions_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset);

#ifdef RRC_DEBUG
static void dumpMsg (TRrc *pRrc, McpU8 *pMsgBuf, McpU16 uMsgLength);
#endif


/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/

/**
 * \fn     RRC_Init
 * \brief  Initialize NAVC RRC module object
 *
 */
handle_t RRC_Init (handle_t 	hMcpf,
				   handle_t 	hCmdPool,
				   handle_t 	hEvtPool,
				   EmcpTaskId  	eSrcTaskId,
				   McpU8		uSrcQId)
{
	TRrc *pRrc = (TRrc *)mcpf_mem_alloc(hMcpf, sizeof(TRrc));

	if (!pRrc)
	{
		return NULL;
	}

	mcpf_mem_zero (hMcpf, pRrc, sizeof(TRrc));

	pRrc->hMcpf 	 = hMcpf;
	pRrc->hCmdPool 	 = hCmdPool;
	pRrc->hEvtPool 	 = hEvtPool;
	pRrc->eSrcTaskId = eSrcTaskId;
	pRrc->uSrcQId    = uSrcQId;

	/* Set mandatory bits for both modes: MS-Based and MS-Assisted */
	pRrc->uWishlist = GPSC_REQ_AA | GPSC_REQ_LOC | GPSC_REQ_TIME | GPSC_REQ_NAV;

	return  pRrc;
}



/**
 * \fn     RRC_StopLocationFixRequest
 * \brief  Stop the Ongoing Location Request
 *
 */
void RRC_StopLocationFixRequest(handle_t hRrc)
{
	TRrc *pRrc = (TRrc *) hRrc;
	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("RRC_StopLocationFixRequest: Stop Session Id=%d\n", pRrc->uSessionId));
	sendLocFixStop (pRrc);
}



/**
 * \fn     RRC_Destroy
 * \brief  Destroy NAVC RRC module object
 *
 */
void RRC_Destroy (handle_t hRrc)
{
	TRrc *pRrc = (TRrc *) hRrc;

	mcpf_mem_free (pRrc->hMcpf, hRrc);
}

/**
 * \fn     RRC_processRxMsg
 * \brief  Process incoming RRC message
 *
 */
EMcpfRes RRC_processRxMsg (handle_t 	hRrc,
						   McpU8 		*pMsg,
						   McpU16		uLen,
						   McpU32 		uSystemTimeInMsec)

{
	TRrc 		*pRrc = hRrc;
	EMcpfRes 	eRes;

	/* Copy system time from received message stamped by ADS (or other used
	 * interface) on receive. The system time is used for reference time and
	 * acquisition assistance.
	 */

	pRrc->uSystemTimeMs = uSystemTimeInMsec;

	/* This function decodes the incoming RRC data and its components
	 * and stores in the local and global data structure.
	 */

	eRes = processRrcMsg(pRrc, pMsg, uLen);

	return eRes;
}


/**
 * \fn     RRC_encodeResponse
 * \brief  Encode RRC response message
 *
 */
McpU16 RRC_encodeResponse (handle_t					hRrc,
						   T_GPSC_loc_fix_response	*p_zLocFixResponse,
						   TNAVCD_LocError 			*pLocError,
						   ERrc_MeasrType			eMeasType,
						   ERrc_PosErrorCause		eLocReason,
						   McpU8 					*pMsgBuf)
{
	TRrc		*pRrc = (TRrc *) hRrc;
	McpU32		uMsgBitOffset = 0;
	McpU16 		uMsgLen;

	TRrcPdu						tRrcPdu;
	TRrcPdu						*pRrcPdu = &tRrcPdu;
	TMeasurementRpt				*pMeasurementRpt = &pRrcPdu->tUplinkResponseChoice.tMeasurementRpt;
	TPositionMeasuredResult		*pPosMsrRslt =
								&pMeasurementRpt->tMeasuredResults.tPosMsrResultChoice.tPosMeasuredResult;
	TPosError  					*pPosError 	= &pPosMsrRslt->tPosError;

    mcpf_mem_zero (pRrc->hMcpf, pRrcPdu, sizeof (tRrcPdu));

    pRrcPdu->uChoiceTag_R1  = RRC_MSG_TYPE_CHOICE_MSR_REPORT;
	pRrcPdu->uOptPreamle_R1 = 0;  /* IntegrityCheckInfo is not supported */

	pMeasurementRpt->uOptPream_R2 = RRC_MSRRESP_OPT_PREAM_MSR_RSLT;
	pMeasurementRpt->tMeasuredResults.uMsrRsltChoice = RRC_MSR_RESULT_CHOISE_POSITIONING;

	switch (eMeasType)
	{
	case RRC_REP_POS:
	case RRC_REP_MEAS:

		/* Analyze response result */
		switch(p_zLocFixResponse->ctrl_loc_fix_response_union)
		{
		case GPSC_RESULT_PROT_POSITION:

			switch (p_zLocFixResponse->loc_fix_response_union.prot_position.prot_fix_result)
			{
			case GPSC_PROT_FIXRESULT_2D:
			case GPSC_PROT_FIXRESULT_3D:

				pPosMsrRslt->uOptPream_R3 = C_MSRRPT_OPT_PREAM_LOC;
				ext_geo_loc_construct (&pPosMsrRslt->tPosEstimateInfo, p_zLocFixResponse);
				break;

			case GPSC_PROT_FIXRESULT_NOFIX:

				/* Prepare RRC location error response */
				pPosMsrRslt->uOptPream_R3 = C_MSRRPT_OPT_PREAM_POSERR;
				pos_error_construct (NULL, C_RRC_LOC_REASON_NOT_PROCESSED, pPosError);
				break;

			default:
				MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
								  ("RRC_encodeResponse unexpected prot result=%u!\n",
								   p_zLocFixResponse->loc_fix_response_union.prot_position.prot_fix_result));
			break;
			}
			break;

		case GPSC_RESULT_PROT_MEASUREMENT:

			if(p_zLocFixResponse->loc_fix_response_union.prot_measurement.c_sv_measurement != 0)
			{
				pPosMsrRslt->uOptPream_R3 = C_MSRRPT_OPT_PREAM_MEAS;
				gps_msr_info_construct ( pRrc, &pPosMsrRslt->tPosGpsMsrResult, p_zLocFixResponse);
			}
			else
			{
				/* Prepare RRC location error response */
				pPosMsrRslt->uOptPream_R3 = C_MSRRPT_OPT_PREAM_POSERR;
				pos_error_construct (NULL, C_RRC_LOC_REASON_NOT_PROCESSED, pPosError);
			}
			break;

		default:
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
							  ("RRC_encodeResponse unexpected prot response=%u!\n",
							   p_zLocFixResponse->ctrl_loc_fix_response_union));
			break;
		}
		break;

	case RRC_REP_LOCERR:

        /* Prepare RRC location error response */
		pPosMsrRslt->uOptPream_R3 = C_MSRRPT_OPT_PREAM_POSERR;
        pos_error_construct (pLocError, eLocReason, pPosError );
		break;

	default:
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("RRC_encodeResponse uknown measType=%d!\n", eMeasType));

		return 0;	/* return zero length in the case of error */
		break;

	}

  /* constructing RRC message */

  mcpf_putBits (pMsgBuf, &uMsgBitOffset, pRrcPdu->uOptPreamle_R1, 1);

  mcpf_putBits (pMsgBuf, &uMsgBitOffset, pRrcPdu->uChoiceTag_R1, 5);

  mcpf_putBits (pMsgBuf, &uMsgBitOffset, pMeasurementRpt->uOptPream_R2, 5);

  mcpf_putBits (pMsgBuf, &uMsgBitOffset, pRrc->uMsrId, 4);

  mcpf_putBits (pMsgBuf, &uMsgBitOffset,
				pMeasurementRpt->tMeasuredResults.uMsrRsltChoice, 3);

  if (pMeasurementRpt->uOptPream_R2 & RRC_MSRRESP_OPT_PREAM_MSR_RSLT)
  {
	  rrc_gps_msr_encode(pRrc, pPosMsrRslt, pMsgBuf, &uMsgBitOffset);
  }

  /* add padding zero bits */
  if (uMsgBitOffset % 8)
  {
	  mcpf_putBits (pMsgBuf, &uMsgBitOffset, 0, (8 - (uMsgBitOffset % 8)));
  }

  uMsgLen = (McpU16) (uMsgBitOffset / 8);

  MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
						  ("RRC_encodeResponse: end msgLen=%u\n", uMsgLen));

  return uMsgLen;
}


/************************************************************************
 *
 *   Private functions implementation
 *
 ************************************************************************/

/**
 * \fn     sendRrcResponse
 * \brief  Send RRC encoded response
 *
 * Allocate buffer for outgoing event message, encode RRC response message and send it
 * to destination task (requested service)
 *
 * \note
 * \param	pRrc - pointer to RRC object
 * \param  	pLocFixResponse - pointer to GPSC locaton response structure or NULL if not provided
 * \param  	pLocError  - measurement type
 * \param  	eLocReason - location error reason
 * \return  void
 * \sa
 */
static void sendRrcResponse (TRrc						*pRrc,
							 T_GPSC_loc_fix_response	*pLocFixResponse,
							 TNAVCD_LocError 			*pLocError,
							 ERrc_MeasrType				eMeasType,
							 ERrc_PosErrorCause			eLocReason)
{
	NavlMsg_t *pMsg;
    TNAVC_evt   *pEvt = NULL;
	McpU32		uOutLen;
	EMcpfRes	eRes;

        pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);
    if (pEvt)
	{
		/* Encode and send RRC Response message */
		uOutLen = RRC_encodeResponse (pRrc,
									  pLocFixResponse,
									  pLocError,
									  eMeasType,
									  eLocReason,
                                      pEvt->tParams.tRrFrame.uRRpayload);
        pEvt->tParams.tRrFrame.uRrFrameLength = uOutLen;
        pMsg = (NavlMsg_t *)pEvt;


		eRes = mcpf_SendMsg (pRrc->hMcpf,
							 pRrc->eSrcTaskId,
							 pRrc->uSrcQId,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 NAVC_EVT_RRC_RESP,
							 uOutLen,
							 0, 				/* user defined */
							 pMsg);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pRrc->hMcpf, pMsg);
            MCPF_REPORT_ERROR(pRrc->hMcpf, NAVC_MODULE_LOG, ("sendRrcResponse: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, NAVC_MODULE_LOG, ("sendRrcResponse: mcpf_mem_alloc_from_pool failed\n"));
	}
}

/**
 * \fn     methodType_decode
 * \brief  Decode RRC message type
 *
 * Decode RRC message type from received RRC message pointed by pMsgBuf and starting from bit offset
 * pOffset
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRC field bit length on return
 * \return  void
 * \sa
 */
static void methodType_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{

	pRrc->uMethodType = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);


	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("methodType_decode: uMethType=%u ofs=%u\n", pRrc->uMethodType, *pOffset));

	/* Set which list bit flags for required assistance data */
	switch (pRrc->uMethodType)
	{
	case POS_METHOD_TYPE_MS_ASSISTED:
		pRrc->uWishlist = GPSC_REQ_AA | GPSC_REQ_TIME;
		break;

	case POS_METHOD_TYPE_MS_BASED:
		pRrc->uWishlist = GPSC_REQ_LOC| GPSC_REQ_TIME| GPSC_REQ_NAV;
		break;

	case POS_METHOD_TYPE_MS_BASED_PREF:
		pRrc->uWishlist = GPSC_REQ_LOC| GPSC_REQ_TIME| GPSC_REQ_NAV;
		break;

	case POS_METHOD_TYPE_MS_ASSISTED_PREF:
		pRrc->uWishlist = GPSC_REQ_AA | GPSC_REQ_TIME;
		break;

	default:
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
						  ("methodType_decode unknown methodType=%u !\n", pRrc->uMethodType));
		break;
	}

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("methodType_decode: end WishList=%x ofs=%u\n", pRrc->uWishlist, *pOffset));

}

/**
 * \fn     gpsTowAssist_decodeAndInject
 * \brief  Decode RRC time of week and jnect the data to GPS receiver
 *
 * Decode RRC Time Of Week (TOW) field and inject TOW to GPS chip
 * pOffset
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \param  	pInjectAssist - pointer to assistance structure to store decoded values
 * \return  void
 * \sa
 */
static void gpsTowAssist_decodeAndInject (TRrc	 *pRrc,
										  McpU8  *pMsgBuf,
										  McpU32 *pOffset)
{
	McpU8 uIdx, uGpsTowAssisLength, j=0;

  	TNAVC_cmdParams   *pNavcCmd = NULL;
	T_GPSC_tow_assist *pAssist;

	/* length: num of SVs */
	uGpsTowAssisLength = (McpU8)(mcpf_getBits (pMsgBuf, pOffset, 4) + 1);

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("gpsTowAssist_decodeAndInject: length=%u ofs=%u\n", uGpsTowAssisLength, *pOffset));

	if(uGpsTowAssisLength != 0)
	{

   		pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);
              if(NULL == pNavcCmd)
              {
                   MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("gpsTowAssist: MEM allocation failed!\n"));
                   MCPF_Assert(1);
                   return;
              }
              mcpf_mem_set(pRrc->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
              j = 0;
	for (uIdx=0; uIdx < uGpsTowAssisLength; uIdx++,j++)
	{

		if (pNavcCmd)
		{
			pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_TOW;
			pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.tow_assist[j];

			pAssist->svid 				    = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 6);
			pAssist->towa_tlm_word 		    = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 14);
			pAssist->towa_tlm_reserved_bits = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);
			pAssist->towa_alert_flag 	    = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);
			pAssist->towa_anti_spoof_flag   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("gpsTowAssist: i=%u svid=%u tlm=%u spoof=%u alrt=%u rsrv=%u ofs=%u\n", uIdx,
									 pAssist->svid,
									 pAssist->towa_tlm_word,
									 pAssist->towa_anti_spoof_flag,
									 pAssist->towa_alert_flag,
									 pAssist->towa_tlm_reserved_bits,
									 *pOffset));



		}
		else
		{
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("gpsTowAssist_decodeAndInject: allocation failed!\n"));
		}
        if((uIdx+1) % N_SV_15 == 0)
        {
           if(pNavcCmd)
           {

              sendAssistanceToNavc (pRrc, pNavcCmd);
           }
	        pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);
	        if(NULL == pNavcCmd)
	        {
	                MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("gpsTowAssist: MEM allocation failed!\n"));
	                MCPF_Assert(1);
	                return;
	        }
	        mcpf_mem_set(pRrc->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
	        j = 0;
                                }
	}
	if(uIdx % N_SV_15 != 0) /* There are still some more parsed packets yet to be sent to NAVC. Send them now */
	{
		if(pNavcCmd)
		{
			sendAssistanceToNavc (pRrc, pNavcCmd);
		}
	}
	} // if uGpsTowAssisLength != 0
	pRrc->uAvailabilityFlags |= C_REF_TOW_AVAIL;
}

/**
 * \fn     referenceTime_decode
 * \brief  Decode RRC reference time field and jnect the data to GPS receiver
 *
 * Decode RRC reference time field including GPS time and optional GSM and Time Of Week fields
 * and inject data to GPS chip
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRC field bit length on return
 * \return  void
 * \sa
 */
static void referenceTime_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	TNAVC_cmdParams *pNavcCmd;
	T_GPSC_assistance_inject *pAssist;
	McpU8	        uOptPream, uSfnTowUnc, uDriftRate;
	U16 			uGpsWeek; 				/* note RRC gives only the 10-bit version */
	U32 			uGpsTOW1msec;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_TIME;
		pAssist = &pNavcCmd->tInjectAssist.tAssistance;

		uOptPream = (McpU8)  mcpf_getBits(pMsgBuf, pOffset, 4);

		uGpsWeek  = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 10);

		uGpsTOW1msec = (McpU32) mcpf_getBits(pMsgBuf, pOffset, 30);

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("referenceTime_decode: opts=%x gpsWeek=%u msec=%u ofs=%u\n",
								 uOptPream, (McpU32) uGpsWeek, (McpU32) uGpsTOW1msec, (McpU32)*pOffset));

		pAssist->assistance_inject_union.time_assist.gps_week = (McpU16) (uGpsWeek + 1024);

		pAssist->assistance_inject_union.time_assist.gps_msec = uGpsTOW1msec;

		pAssist->assistance_inject_union.time_assist.time_unc = (McpU32) 2000000;

		pAssist->assistance_inject_union.time_assist.sub_ms   = 0;

		pAssist->assistance_inject_union.time_assist.time_accuracy = GPSC_TIME_ACCURACY_COARSE;

		/* Store time refernce (TOW, ms and system msec) in RRC object
		 * to retrieve on GPS TOW request
		 */

		pRrc->tGpsTime.tTime = pAssist->assistance_inject_union.time_assist;
		pRrc->tGpsTime.uSystemTimeMs = pRrc->uSystemTimeMs;

		/* Synchronize GPS time in NAVC object */
		injectGpsTimeToNavc (pRrc, &pRrc->tGpsTime);

		/* signalling the receival of RefTime */
		pRrc->uAvailabilityFlags |= GPSC_REQ_TIME;

		sendAssistanceToNavc (pRrc, pNavcCmd);

		if (uOptPream & C_REFERENCE_TIME_UTRAN_GPS_REFERENCE_TIME)
		{
			utranGPSReferenceTime_decode (pRrc, pMsgBuf, pOffset);
		}

		if (uOptPream & C_REFERENCE_TIME_SFN_TOW_UNCERTAINTY)
		{
			uSfnTowUnc = (McpU8)mcpf_getBits (pMsgBuf, pOffset, 1);

		}

		if (uOptPream & C_REFERENCE_TIME_UTRAN_GPS_DRIFTRATE)
		{
			uDriftRate = (McpU8)mcpf_getBits (pMsgBuf, pOffset, 4);
		}

		if (uOptPream & C_REFERENCE_TIME_GPS_TOW_ASSISTLIST)
		{
			gpsTowAssist_decodeAndInject(pRrc,pMsgBuf, pOffset);

		}

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("referenceTime_decode: end ofs=%u\n", *pOffset));
	}
	else
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("referenceTime_decode: allocation failed!\n"));
	}

}


/**
 * \fn     utranGPSReferenceTime_decode
 * \brief  Decode RRC utran-GPSReferenceTime IE
 *
 * Decode RRC Decode RRC utran-GPSReferenceTime IE
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRC field bit length on return
 * \return  void
 * \sa
 */
static void utranGPSReferenceTime_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8	uOpt, uChoiceTag;
	McpU16	uMspart;
	McpU32	uLspart;
	McpU16	uPrimaryScarmbCode, uSfn;
	McpU8	uCellId;

	uOpt = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 1);

	/* utran-GPSTimingOfCell IE */
	uMspart = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 10);
	uLspart = (McpU32) mcpf_getBits(pMsgBuf, pOffset, 32);

	if (uOpt)
	{
		uChoiceTag = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

		/* modeSpecificInfo IE */
		switch (uChoiceTag)
		{
		case C_RRC_CHOICE_MODE_SPECIFIC_INFO_FDD:
			uPrimaryScarmbCode = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 9);
			break;

		case C_RRC_CHOICE_MODE_SPECIFIC_INFO_TDD:
			uCellId = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);
			break;

		default:
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
							  ("utranGPSReferenceTime_decode invalid tag=%u!\n", uChoiceTag));
			break;
		}
	}

	uSfn = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 12);
}

/**
 * \fn     refLocation_decode
 * \brief  Decode RRC reference location fields and jnect the data to GPS receiver
 *
 * Decode RRC reference location fields and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void refLocation_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	double      d_Lat, d_Long, f_Uncertain;
	McpU8		uUncertainSemiMajor, uUncertainSemiMinor;
	McpU8		uConfidence, uUncAlt;
	McpU16		uOrientationMajorAxis;
	McpS32 		s32;
	TNAVC_cmdParams   *pNavcCmd;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);

	if (pNavcCmd)
	{
		T_GPSC_position_assist *pAssist;

		pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_POSITION;
		pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.position_assist;

		pAssist->latitude_sign = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);
		pAssist->latitude 	   = mcpf_getBits(pMsgBuf, pOffset, 23);

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("refLocation_decode: latit=%u sign=%u ofs=%u\n",
								 (McpU32)pAssist->latitude, (McpU32)pAssist->latitude_sign, (McpU32)*pOffset));

		/* Convert latitude to degrees */
		if (pAssist->latitude_sign)
		{
			d_Lat = (McpS32)(0 - (McpS32)(pAssist->latitude)) * C_90_OVER_2_23;
		}
		else
		{
			d_Lat = pAssist->latitude * C_90_OVER_2_23;
		}

			/* do we need to move this to gpsc_app_api.c/gpsc_populate_pos_assist() */
			/* If defined, perturb the latitude by the user defined offset (in Km). */
		if (pRrc->bPosOffsetDefined)
		{
			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG, ("*** Lat before bias: %f\n", d_Lat));
			d_Lat += (C_RAD_TO_DEG * (pRrc->fltPosOffset / C_EARTH_RADIUS));
			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG, ("*** Lat  after bias: %f\n", d_Lat));
		}

		/* convert latitude from degree to radius and apply AI2 spec. */
		pRrc->lLatitude = (McpS32) (d_Lat * C_DEG_TO_RAD * C_LSB_LAT_REP);

		/* retrieve longitude */
		s32 = mcpf_getBits(pMsgBuf, pOffset, 24);   /* mcpf_getSignedBits */

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("refLocation_decode: longitude=%d ofs=%u\n", s32, *pOffset));

		/* convert longitude to degrees */
		d_Long = s32 * C_360_OVER_2_24;
		pAssist->longitude = s32 - 8388608;

			/* do we need to move this to gpsc_app_api.c/gpsc_populate_pos_assist() */
			/* If defined, perturb the longitude by the user defined offset (in Km) */
		if (pRrc->bPosOffsetDefined)
		{
			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG, ("*** Long before bias: %f\n", d_Long));
			d_Long += (C_RAD_TO_DEG * (pRrc->fltPosOffset / (C_EARTH_RADIUS * cos(d_Lat * C_DEG_TO_RAD))));
			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG, ("*** Long  after bias: %f\n", d_Long));
		}
		/* convert longitude from degrees to radius and appply AI2 spec. */
		pRrc->lLongtitue = (McpS32)(d_Long * C_DEG_TO_RAD * C_LSB_LON_REP);

		/* retrieve altitude */

		pAssist->altitude_sign = (McpU8)  mcpf_getBits(pMsgBuf, pOffset, 1);
		pAssist->altitude      = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 15);

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("refLocation_decode: altitude=%u sign=%u ofs=%u\n",
								 pAssist->altitude, pAssist->altitude_sign, *pOffset));

		/* apply AI2 spec. */
		/* if the depth bit is set in the altitude bits */
		if (pAssist->altitude_sign)
		{
			McpS16 x_Ht;

			/* convert depth into a negative number expressed by x_Ht */
			x_Ht = (McpS16)(0 - (McpS16)pAssist->altitude);
			pRrc->xHeight = (McpS16)(x_Ht * C_LSB_HT_REP);
			pAssist->altitude = (McpU16)(-1 * x_Ht);
		}
		else
		{
			pRrc->xHeight = (McpS16)((McpU16)pAssist->altitude * C_LSB_HT_REP);
		}

		uUncertainSemiMajor   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);
		uUncertainSemiMinor   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);
		uOrientationMajorAxis = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);
		uOrientationMajorAxis *= 2;

		uUncAlt		= (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);
		uConfidence = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);

		/* RRC requires reference location to be interpreted per 23.032 which
		   specifies a formula for this 7 bit integer to position unc
		*/

		f_Uncertain = 10 * (pow(1.1,uUncertainSemiMajor)-1);

		/* Covert this to integer and pass it */
		pRrc->qUncertainty = (McpU32) f_Uncertain;

		pRrc->qUncertainty = (McpU32) (pRrc->qUncertainty * C_RECIP_LSB_POS_SIGMA)/3;
		pAssist->position_uncertainty = pRrc->qUncertainty;

		/* Inject position assistance data into GPSC */
		sendAssistanceToNavc (pRrc, pNavcCmd);

		/* signalling the receival of RefLoc */
		pRrc->uAvailabilityFlags |= GPSC_REQ_LOC;

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("refLocation_decode: end ofs=%u\n", *pOffset));
	}
	else
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("refLocation_decode: allocation failed!\n"));
	}
}

/**
 * \fn     dgpsCorrections_decode
 * \brief  Decode RRC DGPS correction field and jnect the data to GPS receiver
 *
 * Decode RRC DGPS correction field and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void dgpsCorrections_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8 	uIdx;
	McpU8 	uDgpsLength;
	McpU8	uOptPreamble;
	McpU32	uTow;
	McpU8   uDgpsStatus;

	uTow 		= (McpU32) mcpf_getBits(pMsgBuf, pOffset, 20);
	uDgpsStatus = (McpU8)  mcpf_getBits(pMsgBuf, pOffset, 3);
	uDgpsLength = (McpU8) (mcpf_getBits(pMsgBuf, pOffset, 4) + 1);

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("dgpsCorrections: tow=%u status=%u Nsat=%u ofs=%u\n",
							 uTow, uDgpsStatus, uDgpsLength, *pOffset));

	for (uIdx=0; uIdx < uDgpsLength; uIdx++)
	{
   		TNAVC_cmdParams    *pNavcCmd;
		T_GPSC_dgps_assist *pAssist;

   		pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);

		if (pNavcCmd)
		{
			pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_DGPS;
			pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.dgps_assist;

			uOptPreamble = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);

			pAssist->gps_tow 	 = uTow;
			pAssist->dgps_status = uDgpsStatus;
			pAssist->dgps_Nsat 	 = uDgpsLength;

			pAssist->dgps.svid = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);
			pAssist->dgps.iode = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 8);
			pAssist->dgps.dgps_udre= (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);
			pAssist->dgps.dgps_pseudo_range_cor = (McpS32)(((mcpf_getBits(pMsgBuf, pOffset, 12) * C_PRC_SCALEFACTOR) - C_PRC_OFFSET)/ C_PRC_AI2_SCALEFACTOR);
			pAssist->dgps.dgps_range_rate_cor 	= (McpS16)(((mcpf_getBits(pMsgBuf, pOffset, 8) * C_RRC_SCALEFACTOR) - C_RRC_OFFSET) / C_RRC_AI2_SCALEFACTOR);
			pAssist->dgps.dgps_deltapseudo_range_cor2 	= (McpS8)mcpf_getBits(pMsgBuf, pOffset, 8);
			pAssist->dgps.dgps_deltarange_rate_cor2		= (McpS8)mcpf_getBits(pMsgBuf, pOffset, 4);

			if (uOptPreamble & 0x2)
			{
				pAssist->dgps.dgps_deltapseudo_range_cor3	= (McpS8)mcpf_getBits(pMsgBuf, pOffset, 8);
			}

			if (uOptPreamble & 0x1)
			{
				pAssist->dgps.dgps_deltarange_rate_cor3		= (McpS8)mcpf_getBits(pMsgBuf, pOffset, 4);
			}

			/* Inject DGPS data into GPSC */
			sendAssistanceToNavc (pRrc, pNavcCmd);

			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("dgpsCorrections: i=%u svid=%u iode=%u udre=%u rngCor=%u rateCor=%u\n",
									 uIdx, pAssist->dgps.svid, pAssist->dgps.iode, pAssist->dgps.dgps_udre,
									 pAssist->dgps.dgps_range_rate_cor, pAssist->dgps.dgps_range_rate_cor));

			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("dgpsCorrections: drngCor2=%d drateCor2=%d drngCor3=%d drateCor3=%d ofs=%u\n",
									 pAssist->dgps.dgps_deltapseudo_range_cor2,
									 pAssist->dgps.dgps_deltarange_rate_cor2,
									 pAssist->dgps.dgps_deltapseudo_range_cor3,
									 pAssist->dgps.dgps_deltarange_rate_cor3,
									 (McpU32)pOffset ));
		}
		else
		{
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("dgpsCorrections: allocation failed!\n"));
		}
	}

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("dgpsCorrections: end ofs=%u\n", *pOffset));

	pRrc->uAvailabilityFlags |= GPSC_REQ_DGPS;          /* signalling the receival of DGPS */
}


/**
 * \fn     navigationModel_decode
 * \brief  Decode RRC navigation model field and jnect the data to GPS receiver
 *
 * Decode RRC navigation model (array of ephemeris per specified satellites) field and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrc   - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRC field bit length on return
 * \return  void
 * \sa
 */
static void navigationModel_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8 	nSV; 	/* num. of navModelElement */
	McpU8 	uOpt;
	McpU8 	i,j = 0;
	TNAVC_cmdParams   		 *pNavcCmd = NULL;

	/* Length of satellite list  */
	nSV = (McpU8)(mcpf_getBits(pMsgBuf, pOffset, 4) + 1);

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("navigationModel: nSV=%u ofs=%u\n", nSV, *pOffset));


	if(nSV != 0)
	{
        for (i=0, j=0; i<nSV; i++, j++) /* list length = num of SVs */
		{
		T_GPSC_ephemeris_assist  *pAssist;
		McpU8  satStatus;
		McpU8  uSvId;

            if (i % N_SV_15 == 0)
            {

                pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);
                if(NULL == pNavcCmd)
				{
                        MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("navigationModel: MEM allocation failed!\n"));
                        MCPF_Assert(1);
                        return;
				}
				mcpf_mem_set(pRrc->hMcpf, pNavcCmd, 0xFF,sizeof(TNAVC_cmdParams));
				j = 0;
			}

			if (pNavcCmd && (j < N_SV_15))
			{
				pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_EPH;
				pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.ephemeris_assist[j];


				/* Option bit */
				uOpt  = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

				uSvId = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 6);

				/* SvId */
				pAssist->svid = uSvId;

				/* Get SatStatus */
				satStatus = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);

				MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
										("navigationModel: svid=%u status=%u ofs=%u\n", uSvId, satStatus, *pOffset));

				if ( satStatus == 1 )
				{
					 /* SatStatus Enumerated IE = ES_SN existing satellite, same Navigation Model.*/
					continue;
				}

				if (uOpt)
				{
					pAssist->ephem_code_on_l2 = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);    	/* CodeOnL2   */
					pAssist->ephem_ura 		  = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 4);    	/* URA        */
					pAssist->ephem_svhealth   = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);    	/* SvHealth   */
					pAssist->ephem_iodc 	  = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 10);  	/* IODE       */
					pAssist->iode 			  = (McpU8)pAssist->ephem_iodc;
					mcpf_getBits(pMsgBuf, pOffset, 1);                   						/* L2Pflag    */
					mcpf_getBits(pMsgBuf, pOffset, 23);                  						/* Reserved 1 */
					mcpf_getBits(pMsgBuf, pOffset, 24);                  						/* Reserved 2 */
					mcpf_getBits(pMsgBuf, pOffset, 24);                  						/* Reserved 3 */
					mcpf_getBits(pMsgBuf, pOffset, 16);                  						/* Reserved 4 */
					pAssist->ephem_tgd = (McpU8)  mcpf_getBits(pMsgBuf, pOffset,  8);       	/* Tgd 		  */
					pAssist->ephem_toc = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16);       	/* Toc 		  */
					pAssist->ephem_af2 = (McpU8)  mcpf_getBits(pMsgBuf, pOffset,  8);       	/* AF2 		  */
					pAssist->ephem_af1 = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16);   		/* AF1 		  */
					pAssist->ephem_af0 = (McpU32) mcpf_getSignedBits(pMsgBuf, pOffset, 22);		/* AF0 		  */
					pAssist->ephem_crs = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16);   		/* Crs 		  */
					pAssist->ephem_delta_n = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16); 		/* DeltaN 	  */
					pAssist->ephem_m0  = (McpU32) mcpf_getBits(pMsgBuf, pOffset, 32);			/* M0 		  */
					pAssist->ephem_cuc = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16);   		/* Cuc 		  */
					pAssist->ephem_e   = (McpU32) mcpf_getBits(pMsgBuf, pOffset, 32);        	/* E 		  */
					pAssist->ephem_cus = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16);   		/* Cus 		  */
					pAssist->ephem_a_power_half = (McpU32)mcpf_getBits(pMsgBuf, pOffset, 32);   /* APowerHalf */
					pAssist->ephem_toe = (McpU16)mcpf_getBits(pMsgBuf, pOffset,  16);           /* Toe 		  */


					MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
											("navigationModel: CodeOnL2=%u URA=%u SvHealth=%u iodc=%u\n",
											 pAssist->ephem_code_on_l2, pAssist->ephem_ura,
											 pAssist->ephem_svhealth, pAssist->ephem_iodc));

					MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
											("navigationModel: tgd=%d toc=%u af2=%d af1=%d af0=%d crs=%d\n",
											 (McpS32) pAssist->ephem_tgd, (McpU32) pAssist->ephem_toc,
											 (McpS32) pAssist->ephem_af2, (McpS32) pAssist->ephem_af1, (McpS32) pAssist->ephem_af0, (McpS32) pAssist->ephem_crs));

					MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
											("navigationModel: delN=%d eM0=%d cuc=%d eE=%d eCus=%d pwH=%u eToe=%u ofs=%u\n",
											 (int)pAssist->ephem_delta_n, (int)pAssist->ephem_m0,
											 (int)pAssist->ephem_cuc, (int)pAssist->ephem_e, (int)pAssist->ephem_cus,
											 (McpU32) pAssist->ephem_a_power_half, (McpU32) pAssist->ephem_toe, *pOffset));

					/* The Fit Interval flag is parsed. This determines if the ephemeris data is valid for 4
					 * hours (value 0) or longer, 6 hours (value 1). This field is passed to the sensor in
					 * the MSB of the u_Accuracy field.
					 */
					if (mcpf_getBits(pMsgBuf, pOffset, 1)) /* FitFlag */
						pAssist->ephem_ura |= 0x80;
					mcpf_getBits(pMsgBuf, pOffset, 5); /* skip AODA */
					pAssist->ephem_cic 		= (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16);          /* Cic     */
					pAssist->ephem_omega_a0 = (McpU32) mcpf_getBits(pMsgBuf, pOffset, 32);  		/* OmegaA0 */
					pAssist->ephem_cis 		= (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16);         	/* Cis     */
					pAssist->ephem_i0 		= (McpU32) mcpf_getBits(pMsgBuf, pOffset, 32);      	/* I0 	   */
					pAssist->ephem_crc 		= (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16);         	/* Crc     */
					pAssist->ephem_w 		= (McpU32) mcpf_getBits(pMsgBuf, pOffset, 32);         	/* W       */
					pAssist->ephem_omega_adot = (McpU32) mcpf_getSignedBits(pMsgBuf, pOffset, 24);	/* OmegaADot */
					pAssist->ephem_idot 	  = (McpU16) mcpf_getSignedBits(pMsgBuf, pOffset, 14);  /* IDot    */
					/* set the below two fieds as 0 as these info is needed only for predicted eph*/
					pAssist->ephem_predicted =0;
					pAssist->ephem_predSeedAge=0;

					MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
											("navigationModel: cic=%d omA0=%d cis=%d eI0=%d\n",
											 (int)pAssist->ephem_cic, (int)pAssist->ephem_omega_a0,
											 (int)pAssist->ephem_cis, (int)pAssist->ephem_i0 ));

					MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
											("navigationModel: eCrc=%d eW=%d adot=%d idot=%d ofs=%u\n",
											 (int)pAssist->ephem_crc, (int)pAssist->ephem_w, (int)pAssist->ephem_omega_adot,
											 (int)pAssist->ephem_idot, *pOffset));

				}
			}
			else
			{
				MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("navigationModel_decode: allocation failed!\n"));
			}

			if(((i+1) % N_SV_15 == 0) && (pNavcCmd) )
			{
				sendAssistanceToNavc (pRrc, pNavcCmd);
			}

			} /* end bracket of Sv list for-loop */

		if((i % N_SV_15 != 0) && (pNavcCmd))
		{
			/* Inject ephemeris assistance data into GPSC */
			sendAssistanceToNavc (pRrc, pNavcCmd);
		}
	} //if nSV != 0
	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("navigationModel: end ofs=%u\n", *pOffset));

	pRrc->uAvailabilityFlags |= GPSC_REQ_NAV;        /* signalling the receival of NavModel */
}

/**
 * \fn     ionosphericModel_decode
 * \brief  Decode RRC Ionospherical model field and jnect the data to GPS receiver
 *
 * Decode RRC Ionospherical model field and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void ionosphericModel_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	TNAVC_cmdParams   	*pNavcCmd;
	T_GPSC_iono_assist  *pAssist;
	McpU8               *pIono;
    McpU8               i;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_IONO;
		pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.iono_assist;
		pIono = (McpU8 *)pAssist; /* Klocwork Changes */

		for ( i = 0; i < 8; i++ )
		{
			pIono[i] = (McpU8)(mcpf_getBits(pMsgBuf, pOffset, 8));

			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("ionosphericModel: iono=%u ofs=%u\n", pIono[i], *pOffset));
		}

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("ionosphericModel: end ofs=%u\n", *pOffset));

		/* Inject iono data into GPSC */
		sendAssistanceToNavc (pRrc, pNavcCmd);

		pRrc->uAvailabilityFlags |= GPSC_REQ_IONO;       /* signalling the receival of IONO */
	}
	else
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("ionosphericModel_decode: allocation failed!\n"));
	}

}

/**
 * \fn     utcModel_decode
 * \brief  Decode RRC/RRLP UTC model field and jnect the data to GPS receiver
 *
 * Decode RRC/RRLP UTC model field and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void utcModel_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	TNAVC_cmdParams   *pNavcCmd;
	T_GPSC_utc_assist *pAssist;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_UTC;
		pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.utc_assist;

		pAssist->utc_a1         = (McpU32)mcpf_getSignedBits(pMsgBuf, pOffset, 24);
		pAssist->utc_a0         = (McpU32)mcpf_getBits(pMsgBuf, pOffset, 32);
		pAssist->utc_tot        = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 8);
		pAssist->utc_wnt        = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 8);
		pAssist->utc_delta_tls  = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 8);
		pAssist->utc_wnlsf      = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 8);
		pAssist->utc_dn         = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 8);
		pAssist->utc_delta_tlsf = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 8);

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("utcModel: a1=%d a0=%d tot=%u wnt=%u delta=%d ofs=%u\n",
								 (McpS32) pAssist->utc_a1, (McpS32) pAssist->utc_a0,
								 (McpU32) pAssist->utc_tot, (McpU32) pAssist->utc_wnt,
								 (McpS32) pAssist->utc_delta_tls, *pOffset));

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("utcModel: wnlsf=%u dn=%u delta_tlsf=%u ofs=%u\n",
								  pAssist->utc_wnlsf, pAssist->utc_dn, pAssist->utc_delta_tlsf,
								 *pOffset));

		/* Inject UTC model data into GPSC */
		sendAssistanceToNavc (pRrc, pNavcCmd);

		pRrc->uAvailabilityFlags |= GPSC_REQ_UTC;    	/* signalling the receival of UTC */
	}
	else
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("utcModel_decode: allocation failed!\n"));
	}

}

/**
 * \fn     almanac_decode
 * \brief  Decode RRC Almanach field and jnect the data to GPS receiver
 *
 * Decode RRC Almanach - array of almanach records per specified satellites and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrc   - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void almanac_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	TNAVC_cmdParams   	  *pNavcCmd = NULL;
	T_GPSC_almanac_assist *pAssist;
	McpU8                 nSV, i, uOption, uDataId, j=0;
	McpU16                uAlmanacWeek;

	uOption = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 1);

	/* Almanac WNa  */
	uAlmanacWeek = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 8); /* 8 LSBs of the full 10-bit week number */

	/* start almanac list */
	nSV = (McpU8)(mcpf_getBits(pMsgBuf, pOffset, 4) + 1);

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("almanac: option=%u AlmWeek=%u nSV=%u ofs=%u\n", uOption,
							 uAlmanacWeek, nSV, *pOffset));

	if(nSV != 0)
		{
                pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);
                if(NULL == pNavcCmd)
			{
                        MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("gpsTowAssist: MEM allocation failed!\n"));
                        MCPF_Assert(1);
                        return;
			}
			mcpf_mem_set(pRrc->hMcpf, pNavcCmd, 0xFF,sizeof(TNAVC_cmdParams));
			j = 0;

	for (i=0; i<nSV; i++, j++)
	{
		if (pNavcCmd)
		{
			pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_ALMANAC;
			pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.almanac_assist[j];

			/* AlmanacSatInfo IE */
			uDataId					= (McpU8)  mcpf_getBits(pMsgBuf, pOffset, 2) ; 	 	 /* DataID unsupported in AI2 */
			pAssist->svid           = (McpU8)  mcpf_getBits(pMsgBuf, pOffset, 6);        /* SvId */
			pAssist->almanac_week   = uAlmanacWeek;
			pAssist->almanac_e      = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16);       /* E    */
			pAssist->almanac_toa    = (McpU8)  mcpf_getBits(pMsgBuf, pOffset, 8);        /* Toa  */
			pAssist->almanac_ksii   = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16);     	 /* Ksii */
			pAssist->almanac_omega_dot    = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 16); /* OmegaDot     */
			pAssist->almanac_svhealth     = (McpU8)  mcpf_getBits(pMsgBuf, pOffset, 8);  /* SV health    */
			pAssist->almanac_a_power_half = (McpU32) mcpf_getBits(pMsgBuf, pOffset, 24); /* A Power Half */
			pAssist->almanac_omega0 = mcpf_getSignedBits(pMsgBuf, pOffset, 24); 		 /* Omega0, signed     */
			pAssist->almanac_m0     = mcpf_getSignedBits(pMsgBuf, pOffset, 24);   	 	 /* M0, 24-bit signed  */
			pAssist->almanac_w      = mcpf_getSignedBits(pMsgBuf, pOffset, 24);   	 	 /* W, signed          */
			pAssist->almanac_af0    = (McpS16) mcpf_getSignedBits(pMsgBuf, pOffset, 11); /* AF0, 11-bit signed */
			pAssist->almanac_af1    = (McpS16) mcpf_getSignedBits(pMsgBuf, pOffset, 11); /* AF1, 11-bit signed */

			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("almanac: i=%u svid=%u E=%u toa=%u ksii=%d oDot=%d\n",
									 i, pAssist->svid, pAssist->almanac_e,
									 pAssist->almanac_toa, pAssist->almanac_ksii, pAssist->almanac_svhealth));

			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("almanac: pwH=%u om0=%d w=%d m0=%d af0=%d af1=%d ofs=%u\n",
									 (McpU32) pAssist->almanac_a_power_half, (McpS32) pAssist->almanac_omega0,
									(McpS32)  pAssist->almanac_w,  (McpS32) pAssist->almanac_m0,
									 (McpS32) pAssist->almanac_af0, (McpS32) pAssist->almanac_af1,
									 *pOffset));
		}
		else
		{
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("almanac_decode: allocation failed!\n"));
		}
                        if((i+1) % N_SV_15 == 0)
                        {
                                if(pNavcCmd)
                                {
                                        //MCPF_REPORT_SUPL_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
             //                                                                           ("almanac_decode: InLoop sent almanac for =%d\n", (i+1)));
                                        sendAssistanceToNavc (pRrc, pNavcCmd);
    } /* end of SV list for-loop */

                                pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);
                                if(NULL == pNavcCmd)
                                {
                                        MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("gpsTowAssist: MEM allocation failed!\n"));
                                        MCPF_Assert(1);
                                        return;
                                }
                                mcpf_mem_set(pRrc->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
                                j = 0;
                        }
	} /* end of SV list for-loop */
	if (uOption)
	{
		/* Global health field is discarded */
		(void) mcpf_getBits(pMsgBuf, pOffset, 364);
	}

	if(i % N_SV_15 != 0)
	{
		if(pNavcCmd)
		{
			/* Inject ephemeris assistance data into GPSC */
			sendAssistanceToNavc (pRrc, pNavcCmd);
		}
	}
	} //if nSV != 0
	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("almanac: end ofs=%u\n", *pOffset));

	pRrc->uAvailabilityFlags |= GPSC_REQ_ALMANAC;        /* signalling the receival of almanac */
}


/**
 * \fn     acquisAssist_decode
 * \brief  Decode RRC Almanach field and jnect the data to GPS receiver
 *
 * Decode RRC Almanach - array of almanach records per specified satellites and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void acquisAssist_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	TNAVC_cmdParams   	 *pNavcCmd = NULL;
	T_GPSC_acquisition_assist *pAssist;
	McpU8 uNumSVs, uIdx, uOptPream, j=0;
	TAcquisitionAssist	 *pRrcAcqAssist = &pRrc->tAcquisitionAssist;
	McpU32				 uGpsTow;
#ifdef GPSC_NL5500
	TNAVCD_GpsTime 		 *pGpsTime = &pRrc->tGpsTime;
	T_GPSC_time_assist *pTime = &pRrc->tGpsTime.tTime;
#endif

	/* Opt. param. preamble */
	uOptPream = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 1);

	/* GpsTow */
	pRrcAcqAssist->uGpsTow = (McpU32)mcpf_getBits(pMsgBuf, pOffset, 30);
	uGpsTow = pRrcAcqAssist->uGpsTow / 80;

	pRrc->uAvailabilityFlags |= GPSC_REQ_TIME;  /* signalling the receival of RefTime */

#ifdef GPSC_NL5500

	pTime->gps_msec = pRrcAcqAssist->uGpsTow;
	pTime->time_unc = 2000000;
	pTime->sub_ms = 0;
	pTime->time_accuracy = GPSC_TIME_ACCURACY_COARSE;
	pGpsTime->uSystemTimeMs = pRrc->uSystemTimeMs;

	/* Synchronize GPS time in NAVC object */
	injectGpsTimeToNavc (pRrc, pGpsTime);

#endif

	/* set assistance avalability bit */
	pRrc->uAvailabilityFlags |= GPSC_REQ_TIME;

	if (uOptPream)
	{
		/* retrieve UTRAN GPS reference time IE */
		utranGPSReferenceTime_decode (pRrc, pMsgBuf, pOffset);
	}

	uNumSVs = (McpU8) (mcpf_getBits(pMsgBuf, pOffset, 4) + 1);   /* Num of SVs */
	pRrcAcqAssist->uNumSV = uNumSVs;

#ifndef UNLIMITED_EPHEMERIS_STORAGE
	pRrc->tAcquisitionAssist.uAcqAsstListedSVs = 0;
#endif

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("acquisAssist: opts=%x tow=%u numSVs=%u ofs=%u\n",
							uOptPream, uGpsTow, uNumSVs, *pOffset));


	if (uNumSVs != 0)
		{
                pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);
                if(NULL == pNavcCmd)
			{
                        MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("acquisAssist: MEM allocation failed!\n"));
                        MCPF_Assert(1);
                        return;
			}
			mcpf_mem_set(pRrc->hMcpf, pNavcCmd, 0xFF,sizeof(TNAVC_cmdParams));
			j = 0;
	for (uIdx=0; uIdx < uNumSVs; uIdx++, j++ ) /* RRC gives at most 16 SVs in acq. list */
	{
		TAcqElement 		*pAcqElement;
		TAdditionalAngle 	*pAddAngle;

		if (pNavcCmd && (uIdx < RRC_MAX_SVS_ACQ_ASSIST) && (j < N_SV_15))
		{
			pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_ACQ;
			pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.acquisition_assist[j];

			pAcqElement = &pRrcAcqAssist->tAcquisElement[uIdx];
			pAddAngle   = &pRrcAcqAssist->tAddAngle[uIdx];

			/* Optional Preamble */
			uOptPream = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);

			/* SvID */
			pAcqElement->uSvId = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);
			pAssist->svid = pAcqElement->uSvId;

			pAssist->gps_tow = uGpsTow;

	#ifndef UNLIMITED_EPHEMERIS_STORAGE
			pRrcAcqAssist->uAcqAsstListedSVs |= 1 << pAcqElement->uSvId;
	#endif
			/* 0th order Doppler  */
			pAcqElement->sDoppler0 = (McpS16)((McpU16)mcpf_getBits(pMsgBuf, pOffset, 12) - 2048);
			/* LCE */
			pAssist->aa_doppler0 = pAcqElement->sDoppler0;

			if (uOptPream & 0x2 )
			{
				/* Optional Additional Doppler */

				/* 1st order Doppler  */
				pAcqElement->uDoppler1 = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);

				/* Doppler unc.  */
				pAcqElement->uDopplerUnc = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 3);

				/* LCE */
				pAssist->v_aa_doppler1 = MCP_TRUE;
				pAssist->v_aa_doppler_uncertainty = MCP_TRUE;
			}
			else
			{
				pAcqElement->uDoppler1   = 0xFF; 	/* to indicate not provided */
				pAcqElement->uDopplerUnc = 0xFF;    /* to indicate not provided */
				/* LCE */
				pAssist->v_aa_doppler1 = MCP_FALSE;
				pAssist->v_aa_doppler_uncertainty = MCP_FALSE;
			}
			pAssist->aa_doppler1   			= pAcqElement->uDoppler1;
			pAssist->aa_doppler_uncertainty = pAcqElement->uDopplerUnc;

			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("acquisAssist: i=%u opts=%x svId=%u dop0=%d dop1=%u dopUnc=%u ofs=%u\n",
									 uIdx, uOptPream, pAcqElement->uSvId,
									 pAcqElement->sDoppler0, pAcqElement->uDoppler1, pAcqElement->uDopplerUnc,
									*pOffset));


			/* Code Phase: submsec  */
			pAcqElement->uCodePhase = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 10);
			/* LCE */
			pAssist->aa_code_phase = pAcqElement->uCodePhase;

			/* Integer Code Phase: "frame" */
			pAcqElement->uIntCodePhase = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 5);
			/* LCE */
			pAssist->aa_int_code_phase = pAcqElement->uIntCodePhase;

			/* GPS Bit Number */
			pAcqElement->uGpsBitNum = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);
			/* LCE */
			pAssist->aa_gps_bit_number = pAcqElement->uGpsBitNum;

			/* CodePhaseSearchWindow */
			pAcqElement->uSrchWin = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 4);
			/* LCE */
			pAssist->aa_code_phase_search_window = pAcqElement->uSrchWin;

			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("acquisAssist: codPh=%u icodPh=%u bitN=%u srcWin=%u ofs=%u\n",
									 pAcqElement->uCodePhase, pAcqElement->uIntCodePhase, pAcqElement->uGpsBitNum,
									 pAcqElement->uSrchWin, *pOffset));

			if (uOptPream & 0x1 )
			{
				/* Optional AdditionalAngel */

				/* Azimuth */
				pAddAngle->uAzimuth = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 5);

				/* Elevation */
				pAddAngle->uElevation = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 3);

				/* LCE */
				pAssist->v_aa_azimuth = MCP_TRUE;
				pAssist->v_aa_elevation = MCP_TRUE;

				/* Added by RFH: 3/25/2003 */
				pRrc->uAvailabilityFlags |= C_ANGLE_AVAIL;
			}
			else
			{
				pAddAngle->uAzimuth   = 0xFF;
				pAddAngle->uElevation = 0xFF;
				/* LCE */
				pAssist->v_aa_azimuth   = MCP_FALSE;
				pAssist->v_aa_elevation = MCP_FALSE;
			}
			/* LCE */
			pAssist->aa_azimuth   = pAddAngle->uAzimuth;
			pAssist->aa_elevation = pAddAngle->uElevation;

			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("acquisAssist: azim=%u elev=%u ofs=%u\n",
									pAddAngle->uAzimuth, pAddAngle->uElevation, *pOffset));
		}
		else
		{
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("acquisAssist_decode: allocation failed!\n"));
		}
	                if((uIdx+1) % N_SV_15 == 0)
                        {
                                if(pNavcCmd)
                                {
                                        /* Inject assistance data into GPSC */
                                        sendAssistanceToNavc (pRrc, pNavcCmd);
                                }

                                pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);

                                if(NULL == pNavcCmd)
                                {
                                        MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("acquisAssist: MEM allocation failed!\n"));
                                        MCPF_Assert(1);
                                        return;
                                }

                                mcpf_mem_set(pRrc->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
                                j = 0;
                        }
	}
	if(uIdx % N_SV_15 != 0)
	{
		if(pNavcCmd)
		{
			/* Inject ephemeris assistance data into GPSC */
			sendAssistanceToNavc (pRrc, pNavcCmd);
		}
	}
	} //if nSV !=0

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG, ("acquisAssist: end ofs=%u\n", *pOffset));

	pRrc->uAvailabilityFlags |= GPSC_REQ_AA;
}


/**
 * \fn     seqOfBadSatelliteSet_decode
 * \brief  Decode RRC sequence of bad satellites and jnect the data to GPS receiver
 *
 * Decode RRC sequence of bad satellites - array of records of specified length and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void seqOfBadSatelliteSet_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	TNAVC_cmdParams   *pNavcCmd;
	T_GPSC_rti_assist *pAssist;
	McpU8 	uLength, uIdx;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_RTI;
		pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.rti_assist;

		uLength = (McpU8)(mcpf_getBits(pMsgBuf, pOffset, 4) + 1);

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("seqOfBadSatelliteSet: uLength=%u ofs=%u\n", uLength, *pOffset));

		for (uIdx=0; uIdx<uLength; uIdx++)
		{
			McpU32 uSvId;

			uSvId = mcpf_getBits(pMsgBuf, pOffset, 6) + 1;

			if (uSvId <= 32)
			{
				pRrc->uRealTimeIntegrity[0] |= 1 << (uSvId-1);
				pAssist->rti_bitmask[0] = pRrc->uRealTimeIntegrity[0];
			}
			else
			{
				pRrc->uRealTimeIntegrity[1] |= 1 << (uSvId-33);
				pAssist->rti_bitmask[1] = pRrc->uRealTimeIntegrity[1];
			}

			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("seqOfBadSatelliteSet: i=%u svId=%u ofs=%u\n", uIdx, uSvId, *pOffset));

		}

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG, ("seqOfBadSatelliteSet: end ofs=%u\n", *pOffset));

		/* Inject RTI assistance data into GPSC */
		sendAssistanceToNavc (pRrc, pNavcCmd);

		/* signalling the receival of real-time integrity */
		pRrc->uAvailabilityFlags |= GPSC_REQ_RTI;
	}
	else
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("seqOfBadSatelliteSet_decode: allocation failed!\n"));
	}
}


/**
 * \fn     gpsAssistData_decode
 * \brief  Decode RRC GPS assistance message element
 *
 * Decode RRC GPS assistance data starting from control header
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRC field bit length on return
 * \return  void
 * \sa
 */
static void gpsAssistData_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU16	uOptPream;

	uOptPream = (McpU16) mcpf_getBits(pMsgBuf, pOffset, 10);

	if (uOptPream & C_GPS_ASSIST_REFERENCE_TIME)
	{
		referenceTime_decode (pRrc, pMsgBuf, pOffset);
	}

	if (uOptPream & C_GPS_ASSIST_REFERENCE_LOCATION)
	{
		refLocation_decode(pRrc, pMsgBuf, pOffset);
	}

	if (uOptPream & C_GPS_ASSIST_DGPS_CORRECTIONS)
	{
		dgpsCorrections_decode (pRrc, pMsgBuf, pOffset);
	}

	if (uOptPream & C_GPS_ASSIST_NAVIGATION_MODEL)
	{
		navigationModel_decode (pRrc, pMsgBuf, pOffset);
	}

	if (uOptPream & C_GPS_ASSIST_IONO_MODEL)
	{
		ionosphericModel_decode (pRrc, pMsgBuf, pOffset);
	}

	if (uOptPream & C_GPS_ASSIST_UTC_MODEL)
	{
		utcModel_decode (pRrc, pMsgBuf, pOffset);
	}

	if (uOptPream & C_GPS_ASSIST_ALMANAC)
	{
		almanac_decode (pRrc, pMsgBuf, pOffset);
	}

	if (uOptPream & C_GPS_ASSIST_ACQUISITION_ASSIST)
	{
		acquisAssist_decode (pRrc, pMsgBuf, pOffset);
	}

	if (uOptPream & C_GPS_ASSIST_RTI)
	{
		seqOfBadSatelliteSet_decode (pRrc, pMsgBuf, pOffset);
	}

	if (uOptPream & C_GPS_ASSIST_DUMMY)
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("gpsAssistData_decode: dummy IE is not supported\n"));
	}
}

/**
 * \fn     positionReportQuantity_decode
 * \brief  Decode RRC Positioning Reporting Quantity element
 *
 * Decode RRC GPS Positioning Reporting Quantity element of measurment type IE
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void positionReportQuantity_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8	uOption, uDummy, uTimingOfCellWanted, uDummy2, uAddAssistDataRqst;
	McpU8 	uEnvCharact=0xFF;

	uOption = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);

	methodType_decode (pRrc, pMsgBuf, pOffset);

	pRrc->uPosMethod = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);

	uDummy = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);

	if (uOption & C_POSITION_REPORT_OPT_PREAM_HORIZ_ACCURACY)
	{
		pRrc->uAccuracy = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);
		pRrc->uAccuracy = (McpU8)(10*(pow(1.1,pRrc->uAccuracy)-1));  /* r= 10*(pow(1.1,k)-1)*/
	}
	else
       pRrc->uAccuracy = NAVCD_ACCURACY_UNKNOWN;

	uTimingOfCellWanted = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

	uDummy2 = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

	uAddAssistDataRqst = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

	if (uOption & C_POSITION_REPORT_OPT_PREAM_ENV_CHARAC)
	{
		uEnvCharact = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);
	}

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("positionReportQuantity: posMeth=%u accur=%u envCh=%u ofs=%u\n",
							 pRrc->uPosMethod, pRrc->uAccuracy, uEnvCharact, *pOffset));
}


/**
 * \fn     reportCriteriaRepAmount_decode
 * \brief  Decode RRC Positioning Report Creteria report amount element
 *
 * Decode Decode RRC Positioning Report Creteria report amount element of measurment type IE
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  report amount of RRC, 0 - infinity
 * \sa
 */
static McpU8 reportCriteriaRepAmount_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8 uRepAmount;

	UNREFERENCED_PARAMETER(pRrc);

	uRepAmount = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);

	if (uRepAmount < 7)
	{
		uRepAmount = (McpU8) (1 << uRepAmount);
	}
	else
	{
		uRepAmount = 0; /* Infinity - in case of moving scenario only */
	}
	return uRepAmount;
}


/**
 * \fn     reportCriteria_decode
 * \brief  Decode RRC Positioning Report Creteria element
 *
 * Decode RRC GPS Positioning Report Creteria element of measurment type IE
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void reportCriteria_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8	uChoiceTag;

	uChoiceTag = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);
	pRrc->uPosReportCriteriaChoiceTag = uChoiceTag;

	switch (uChoiceTag)
	{
	case RRC_CHOICE_POS_REPORT_EVNTLST:
		{
			McpU8	uMaxMeasEvt, uIndx, uChoiceTag;

			uMaxMeasEvt = (McpU8) (mcpf_getBits(pMsgBuf, pOffset, 3) + 1);

			for (uIndx=0; uIndx < uMaxMeasEvt; uIndx++)
			{
				pRrc->tEvtParamList[uIndx].uReportingAmount =
					reportCriteriaRepAmount_decode (pRrc, pMsgBuf, pOffset);

                pRrc->tEvtParamList[uIndx].uReportFirstFix = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);
                pRrc->tEvtParamList[uIndx].uPosMsrInt      = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);

                /* UE-Positioning-EventSpecificInfo */
				uChoiceTag = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);

				switch (uChoiceTag)
				{
				case C_RRC_CHOICE_POS_THRESH_POSCNG:

					pRrc->tEvtParamList[uIndx].tEventSpecificInfoChoice.uThresholdPosChg =
							(McpU8) mcpf_getBits(pMsgBuf, pOffset, 4);
					break;

				case C_RRC_CHOICE_POS_THRESH_SFNCHG:
					pRrc->tEvtParamList[uIndx].tEventSpecificInfoChoice.uThresholdSfnChg =
							(McpU8) mcpf_getBits(pMsgBuf, pOffset, 4);
					break;

				case C_RRC_CHOICE_POS_THRESH_GPSCHG:
					pRrc->tEvtParamList[uIndx].tEventSpecificInfoChoice.uThresholdSfnGpsTow =
							(McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);
					break;
				}
			}
		}
		break;

	case RRC_CHOICE_POS_REPORT_PERIODIC:
		{
			McpU8 uOption, uInterv;

			uOption = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);	/* TODO: check the standard new version */

			if (uOption)
			{
				pRrc->uPeriodReportAmount = reportCriteriaRepAmount_decode (pRrc, pMsgBuf, pOffset);
			}

			uInterv = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 4);
			pRrc->uPeriodReportingIntLong = (McpU16) (uReportingIntLong_g[uInterv] / 1000);	/* convert from ms to sec */

			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("reportCriteria_decode: repAmount=%u repInterv=%u\n",
									 pRrc->uPeriodReportAmount, pRrc->uPeriodReportingIntLong));

		}
		break;

	case RRC_CHOICE_POS_REPORT_NO:
		pRrc->uPeriodReportAmount 	  = 0;
		pRrc->uPeriodReportingIntLong = 0;
		break;

	default:
		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("reportCriteria_decode: invalid choiceTag=%u\n", uChoiceTag));
		break;

	}

}



/**
 * \fn     msrType_decode
 * \brief  Decode RRC GPS measurment type Rel-3 element
 *
 * Decode RRC GPS measurment type Rel-3 element
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void msrType_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8	uChoiceTag, uOption;

	uChoiceTag = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);
	pRrc->uMsrType = uChoiceTag;

	switch (uChoiceTag)
	{
	case RRC_CHOICE_MSR_TYPE_POSMSR:

        /* UE-Positioning-Measurement IE */

		uOption = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);

		positionReportQuantity_decode (pRrc, pMsgBuf, pOffset);

        reportCriteria_decode (pRrc, pMsgBuf, pOffset);

		if (uOption & C_ASSIS_OPT_POS_OTDO_AASSIST)
		{
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
							  ("msrType_decode: ASSIS_OPT_POS_OTDO_AASSIST unsupported\n"));
		}

		if (uOption & C_ASSIS_OPT_POS_GPS_ASSIST)
		{
			gpsAssistData_decode (pRrc, pMsgBuf, pOffset);

			/* added when first set of assistance received through MSR */
			if(pRrc->uAvailabilityFlags)
			{
				/* send assistance complete messaage if any assistance is available and injected to gpsc */
				TNAVC_cmdParams   *pNavcCmd;


				pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);
				if (pNavcCmd)
				{
					pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_COMPL_SET;
					sendAssistanceToNavc (pRrc, pNavcCmd);
				}
				else
				{
					MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("processRrcMsg: allocation failed!\n"));
				//	return RES_ERROR;
				}

			}
		}
		break;

	case RRC_CHOICE_MSR_TYPE_INTRAFREQMSR:
	case RRC_CHOICE_MSR_TYPE_INTERFREQMSR:
	case RRC_CHOICE_MSR_TYPE_INTERRATMSR:
	case RRC_CHOICE_MSR_TYPE_TRAFVOLMSR:
	case RRC_CHOICE_MSR_TYPE_QUALMSR:
	case RRC_CHOICE_MSR_TYPE_INTMSR:
		break;

	default:
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("msrType_decode: invalid choice tag=%u\n", uChoiceTag));
		break;
	}

}


/**
 * \fn     msrControlR3_decode
 * \brief  Decode RRC GPS measurment contorl Rel-3 message element
 *
 * Decode RRC GPS measurment contorl message Rel-3 element
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void msrControlR3_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8		uOption, uOption_1, uChoiceTag;
	TReportMode *pRepMode = &pRrc->tReportMode;

	uOption = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

	/* measurementControl-r3 IE */

	uOption_1 = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);

	pRrc->uTransactionId = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);
	pRrc->uMsrId 	     = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 4);

    /* MeasurementCommand IE */
	uChoiceTag = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);

	pRrc->uMsrCtrlCmd = uChoiceTag;

	switch (uChoiceTag)
	{
	case RRC_CHOICE_MSRCTRL_R3_COMMAND_SETUP:

		msrType_decode (pRrc, pMsgBuf, pOffset);
		break;

	case RRC_CHOICE_MSRCTRL_R3_COMMAND_MODIFY:
		{
			McpU8	uOption_2;

			uOption_2 = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

			if (uOption_2)
			{
				msrType_decode (pRrc, pMsgBuf, pOffset);
			}
		}
		break;

	case RRC_CHOICE_MSRCTRL_R3_COMMAND_RELEASE:
		/* NULL, do nothing */
		break;

	default:
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("msrControlR3_decode: invalid choice tag=%u\n", uChoiceTag));
		break;
	}

	if (uOption_1 & C_MSRCONTROL_OPT_MSR_REPORT_MODE)
	{
		/* MeasurementReportingMode IE */
		pRepMode->uTransferMode 		  = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);
		pRepMode->uPeriodicalOrEventTrigg = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);
	}

	if (uOption_1 & C_MSRCONTROL_OPT_ADDITIONAL_MSR_LIST)
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
						  ("msrControlR3_decode: additionalMeasurementList IE unsupported\n"));
	}

	if (uOption_1 & C_MSRCONTROL_OPT_DPCH_COMPRES_MODE_STATUS_INFO)
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
						  ("msrControlR3_decode: DPCH-CompressedModeStatusInfo IE unsupported\n"));
	}

	if (uOption & 0x01)
	{
		/* v390nonCriticalExtensions IE decoding */
		v390nonCriticalExtensions_decode (pRrc, pMsgBuf, pOffset);

        /* MeasurementControl-v390ext decoding */
	}
}

/**
 * \fn     v390nonCriticalExtensions_decode
 * \brief  Decode RRC v390nonCriticalExtensions information element
 *
 * Decode RRC v390nonCriticalExtensions information element
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void v390nonCriticalExtensions_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8	uOption, uOption_1;

	uOption = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

	/* MeasurementControl-v390ext decoding */

	uOption_1 = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

	if (uOption_1 & 0x01)
	{
		McpU8	uOption_2;
		McpU8	uPosAccuracy, uMsrValidity;

		uOption_2 = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);

		/* UE-Positioning-Measurement-v390ext */

		if (uOption_2 & RRC_POS_MSR_V390EXT_REP_QUANTITY)
		{
			/* UE-Positioning-ReportingQuantity-v390ext */
			/* UE-Positioning-Accuracy */
			uPosAccuracy = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);

		}

		if (uOption_2 & RRC_POS_MSR_V390EXT_VALIDITY)
		{
			/* MeasurementValidity  */
			uMsrValidity = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);

		}

		if (uOption_2 & RRC_POS_MSR_V390EXT_OTDOA_ASSISTDATA_UEB)
		{
			/* UE-Positioning-OTDOA-AssistanceData-UEB */
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
							  ("msrControl_decode: UE-Positioning-OTDOA-AssistanceData-UEB IE unsupported\n"));

		}


	}

	if (uOption & 0x01)
	{
		/* v3a0NonCriticalExtensions is not supported */
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
						  ("msrControl_decode: v3a0NonCriticalExtensions IE unsupported\n"));
	}


}

/**
 * \fn     msrControl_decode
 * \brief  Decode RRC GPS measurment contorl message element
 *
 * Decode RRC GPS measurment contorl message element
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void msrControl_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8	uChoiceTag;

	uChoiceTag = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

	switch (uChoiceTag)
	{
	case RRC_CHOICE_MSRCTRL_R3:

		msrControlR3_decode (pRrc, pMsgBuf, pOffset);

		break;

	case RRC_CHOICE_MSRCTRL_LATER_R3:
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("msrControl_decode: RRC_CHOICE_MSRCTRL_LATER_R3 choice unsupported\n"));
		break;

	default:
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("msrControl_decode: invalid choice tag=%u\n", uChoiceTag));
		break;
	}
}

/**
 * \fn     assistDataDelivery_decode
 * \brief  Decode RRC assistance data
 *
 * Decode RRC assistance data fields according to optional bits set,
 * currently only GPS Assistance is supported
 *
 * \note
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  void
 * \sa
 */
static void assistDataDelivery_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8	uChoiceTag, uOptPream, uOptPream_1;

	uChoiceTag = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

	switch (uChoiceTag)
	{
	case RRC_CHOICE_ASSIST_DATA_DELIVERY_R3:

		uOptPream = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

		uOptPream_1 = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);

		pRrc->uTransactionId = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);

		if (uOptPream_1 & C_ASSIS_OPT_PREAM_GPSASSIST)
		{
			gpsAssistData_decode (pRrc, pMsgBuf, pOffset);
		}

		if (uOptPream_1 & C_ASSIS_OPT_PREAM_OTDOAASSIST)
		{
            /* ue-positioning-OTDOA-AssistanceData-UEB IE is not supported */
			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("assistDataDelivery_decode: ue-positioning-OTDOA-AssistanceData-UEB unsupported\n"));
		}

		if (uOptPream & C_ASSIS_OPT_PREAM_V3A0NONCRTCLEXT)
		{
            /* v3a0NonCriticalExtensions is not supported */
			MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
									("assistDataDelivery_decode: v3a0NonCriticalExtensions IE unsupported\n"));

		}
		break;

	case RRC_CHOICE_ASSIST_DATA_DELIVERY_LATER_R3:
		MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
								("assistDataDelivery_decode: later-than-r3 IE unsupported\n"));
		break;

	default:
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
								("assistDataDelivery_decode: Invalid tag=%u\n", uChoiceTag));
		break;
	}

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("assistDataDelivery_decode: end ofs=%u\n", *pOffset));

} /* end of assistDataDelivery_decode */

/**
 * \fn     integrityCheckInfo_decode
 * \brief  Decode RRC integrity check info element
 *
 * Decode RRC integrity check info element
 *
 * \note    The message integrity is not checked, only IE is parsed
 * \param	pRrc    - pointer to RRC object
 * \param  	pMsgBuf - pointer to RRC input message
 * \param  	pOffset - bit ofset from message start, increased by processed field bit length on return
 * \return  result of integrity check: OK or ERROR
 * \sa
 */
static EMcpfRes integrityCheckInfo_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU32	uMsgAuthenticationCode;

	uMsgAuthenticationCode = mcpf_getBits(pMsgBuf, pOffset, 32);

	pRrc->uMsgSeqNum = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 4);
	pRrc->bIntegrityCheck = MCP_TRUE;

	/* Integrity Check info IE is not supported */
	MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("rrc_decode: Integrity Check is not supported!\n"));

	return RES_ERROR;
}

/**
 * \fn     rrc_decode
 * \brief  Decode RRC message
 *
 * Decode RRC message fields
 *
 * \note
 * \param	pRrc      - pointer to RRC object
 * \param  	pMsgBuf   - pointer to RRC input message
 * \param  	uMsgLength - input message length
 * \return  number of processed bits from input message
 * \sa
 */
static McpU16 rrc_decode (TRrc *pRrc, McpU8* pMsgBuf, McpU16 uMsgLength)
{
	McpU32 	offset = 0; 		/* bit offset */
	McpU8	uOption;
	EMcpfRes eRes;

	UNREFERENCED_PARAMETER(uMsgLength);

#ifdef RRC_DEBUG
	dumpMsg (pRrc, pMsgBuf, uMsgLength);
#endif

	uOption = (McpU8)mcpf_getBits(pMsgBuf, &offset, 1);

	if (uOption)
	{
		eRes = integrityCheckInfo_decode (pRrc, pMsgBuf, &offset);
		if (eRes != RES_OK)
		{
			/* Integrity check is failed */
			return 0;
		}
	}

	pRrc->uRrcChoiceTag = (McpU8) (McpU8)mcpf_getBits(pMsgBuf, &offset, 5);

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("rrc_decode: start tag=%u ofs=%u\n", pRrc->uRrcChoiceTag, offset));

	switch (pRrc->uRrcChoiceTag)
	{
		case RRC_CHOICE_ASSIST_DATA_DELIVERY:

			assistDataDelivery_decode (pRrc, pMsgBuf, &offset);
			break;

		case RRC_CHOICE_MSR_CTRL:

			msrControl_decode (pRrc, pMsgBuf, &offset);
			break;

		default:
			/* other choices are not supposed*/
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("rrc_decode: unsupported choisce tag=%u !\n", pRrc->uRrcChoiceTag));
		break;
  } /* end bracket for switch - uRrcChoiceTag */

  MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,("rrc_decode: end ofs=%u\n", offset));

  return (McpU16) offset;
}
static void sendRrcMsg (TRrc *pRrc, McpU8 *pBuf, McpU32	uBufLen)
{
	NavlMsg_t	*pMsg;
    TNAVC_evt   *pEvt = NULL;
	EMcpfRes	eRes;
        pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);
    if (pEvt)
	{
		/* Encode and send RRC Response message */
        mcpf_mem_copy (pRrc->hMcpf, pEvt->tParams.tRrFrame.uRRpayload, pBuf, uBufLen);
        pEvt->tParams.tRrFrame.uRrFrameLength = uBufLen;
        pMsg = (NavlMsg_t *) pEvt;
		eRes = mcpf_SendMsg (pRrc->hMcpf,
							 pRrc->eSrcTaskId,
							 pRrc->uSrcQId,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 NAVC_EVT_RRC_ACK,
							 uBufLen,
							 0, 				/* user defined */
							 pMsg);
		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pRrc->hMcpf, pMsg);
            MCPF_REPORT_ERROR(pRrc->hMcpf, NAVC_MODULE_LOG, ("sendRrlpMsg: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, NAVC_MODULE_LOG, ("sendRrlpMsg: mcpf_mem_alloc_from_pool failed\n"));
	}
}

/**
 * \fn     processRrcMsg
 * \brief  Process received RRC message
 *
 * Decode RRC message fields and performs requested actions: inject assistance data,
 * request location fix, send RRC response
 *
 * \note
 * \param	pRrc      - pointer to RRC object
 * \param  	pMsgBuf   - pointer to RRC input message
 * \param  	uMsgLength - input message length
 * \return  result of operation: OK or Error if number of processed bits is not equal to
 *          input message length
 * \sa      rrc_decode
 */
static EMcpfRes processRrcMsg (TRrc *pRrc, McpU8* pMsgBuf, McpU16 uMsgLength)
{
	McpU16	  	numOfParsedBits, numOfParsedBytes;
	McpU8  		uErrorReason;
	McpBool  	bLocReq = MCP_FALSE; 	/* turn TRUE if RRC-LocReq is received */
	EMcpfRes  	eRes 	= RES_ERROR;
	McpU8 		uAck;
	numOfParsedBits = rrc_decode (pRrc, pMsgBuf, uMsgLength);

	MCPF_REPORT_INFORMATION(pRrc->hMcpf, RR_MODULE_LOG,
							("processRrcMsg: msgLen=%u parsedBits=%u\n", uMsgLength, numOfParsedBits));

	numOfParsedBytes = (McpU16) (numOfParsedBits / 8);
	if (numOfParsedBits % 8)
	{
		/* one more byte for bits remainder */
		numOfParsedBytes++;
	}

	/* Check if the number of decoded bytes is equal to the received one */
	if (numOfParsedBytes == uMsgLength)
	{

		switch (pRrc->uRrcChoiceTag)
		{
		case RRC_CHOICE_ASSIST_DATA_DELIVERY:
			{
				TNAVC_cmdParams   *pNavcCmd;

				uAck = 0;
				/* The three bits RefNumber is shifted to MSB */
				uAck = (McpU8)(pRrc->uTransactionId << 5);

				/* The next 1 bit represents Extension container followed by 3 bits that
				 * represents the Assistance Data Acknowledgement and padded with 0
				 */
				uAck |= 0x06;

				sendRrcMsg (pRrc, &uAck, sizeof(uAck));
				pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);
				if (pNavcCmd)
				{
					pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_COMPL_SET;
					sendAssistanceToNavc (pRrc, pNavcCmd);
				}
				else
				{
					MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("processRrcMsg: allocation failed!\n"));
					return RES_ERROR;
				}
				eRes = RES_OK;
			}
			break;

		case RRC_CHOICE_MSR_CTRL:
				{
			TNAVC_cmdParams   *pNavcCmd;

			pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);
            if (pNavcCmd)
            {
                pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_COMPL_SET;
                sendAssistanceToNavc (pRrc, pNavcCmd);
            }
			switch (pRrc->uMsrCtrlCmd)
			{
			case RRC_CHOICE_MSRCTRL_R3_COMMAND_SETUP:
			case RRC_CHOICE_MSRCTRL_R3_COMMAND_MODIFY:

				switch (pRrc->uPosReportCriteriaChoiceTag)
				{
				case RRC_CHOICE_POS_REPORT_PERIODIC:

					if (pRrc->uPosMethod == RRC_POSITION_METHOD_GPS)
					{
						/* Honour the RRC Measurement Control message from SMLC */
						uErrorReason = C_RRC_LOC_REASON_GOOD;
						bLocReq 	 = MCP_TRUE;
						eRes 		 = RES_OK;
					}
					else
					{
						/* This location error exits if the Position Method type is other then GPS */
						sendRrcResponse (pRrc,
										 NULL,
										 NULL,
										 RRC_REP_LOCERR,
										 C_RRC_LOC_REASON_NOT_PROCESSED);

						rrcDataReset (pRrc);

						MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
										  ("processRrcMsg: Pos Method=%u is unsupported!\n",
										   pRrc->uPosMethod));
					}
					break;

				case RRC_CHOICE_POS_REPORT_NO:
					/* Do not send location fix request for NO REPORT */
					eRes = RES_OK;
					break;

				case RRC_CHOICE_POS_REPORT_EVNTLST:

					sendRrcResponse (pRrc,
									 NULL,
									 NULL,
									 RRC_REP_LOCERR,
									 C_RRC_LOC_REASON_NOT_PROCESSED);

					rrcDataReset (pRrc);

					MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
									  ("processRrcMsg: Report criteria event list is unsupported!\n"));
					break;

				default:
					MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
									  ("processRrcMsg: Report criteria=%u is invalid!\n",
									   pRrc->uPosReportCriteriaChoiceTag));
					break;
				}
				break;

			case RRC_CHOICE_MSRCTRL_R3_COMMAND_RELEASE:

				/* Stop the current location request */
				sendLocFixStop (pRrc);

				rrcDataReset (pRrc);
				eRes = RES_OK;
				break;

			default:
				MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
								  ("processRrcMsg: unsupported Msr Command=%u\n", pRrc->uMsrCtrlCmd));
				break;
			}
        }
 			break;

		default:
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
							  ("processRrcMsg: unsupported RRC command tag=%u\n", pRrc->uRrcChoiceTag));

			break;
	    }

		/* Check if incoming Measure Position command is honoured and assistance is available */
		if (bLocReq)
		{
			EMcpfRes eResult;

			eResult = checkAssistDataForLocFix (pRrc);

			if (eResult == RES_OK)
			{
				sendLocFixReq (pRrc);
				eRes = RES_OK;
			}
			else
			{
				/* Send a Location response with Location assistance data missing error */
				sendRrcResponse (pRrc,
								 NULL,
								 NULL,
								 RRC_REP_LOCERR,
								 C_RRC_LOC_REASON_ASSIS_MIS);

				/* Initialize the RRC object structure holding assistance data from SMLC */
				rrcDataReset (pRrc);
			}
		}
	}
	else
	{
		/* Number of parsed bits is not equal to input message length - error occurred */
		if (numOfParsedBits == 0)
		{
			/* RRC message IE is not supported, return error */
			sendRrcResponse (pRrc,
							 NULL,
							 NULL,
							 RRC_REP_LOCERR,
							 C_RRC_LOC_REASON_NOT_PROCESSED);
		}
		SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_WARN_UNKWN_GPS_MSG,nav_sess_getCount(0),"#Unknown GPS message received,  discard");
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("processRrcMsg: RRC message parse error\n"));
	}

	return eRes;
}

/**
 * \fn     checkAssistDataForLocFix
 * \brief  Checks received assistance data
 *
 * Checks received assistance data availability
 *
 * \note
 * \param	pRrc     - pointer to RRC object
 * \return  result of operation: OK - all assistance data received or
 *          Error - assistance data is missed
 * \sa      rrc_decode
 */
static EMcpfRes checkAssistDataForLocFix(TRrc *pRrc)
{
	UNREFERENCED_PARAMETER(pRrc);

	return RES_OK;		/* GPSC issues assistance request (not RRC module) */

#if 0
	McpU16 		uFilteredWishlist;
	EMcpfRes    eRes;

	if (pRrc->uMethodType == C_SMLC_COMPUTED)
	{
		/* Checks if the madatory assistance data for MS Assisted mode is received */
		uFilteredWishlist = (McpU16)(pRrc->uWishlist & (GPSC_REQ_AA | GPSC_REQ_TIME));
	}
	else
	{	/* Checks if the madatory assistance data for MS Based mode is received */
		uFilteredWishlist = (McpU16)(pRrc->uWishlist & (GPSC_REQ_LOC | GPSC_REQ_TIME | GPSC_REQ_NAV));
	}

	/* Reset all other assistance data bit mask to zero */
	uFilteredWishlist &= ~(GPSC_REQ_ALMANAC | GPSC_REQ_UTC | GPSC_REQ_IONO | GPSC_REQ_DGPS | GPSC_REQ_RTI);

	if ((pRrc->uAvailabilityFlags & uFilteredWishlist) == uFilteredWishlist)
	{
		/* If the bits for all the required assistance data are set,
		 * then all the assistance data necessary for Measure Position Request is recieved.
		 */
		eRes = RES_OK;
	}
	else
	{
		/* If not the all bits required assistance data are set,
		 * then all the assistance data necessary for Measure Position Request
		 * is not recieved.
		 */
		eRes = RES_ERROR;
	}

	return eRes;
#endif
}

/**
 * \fn     sendLocFixReq
 * \brief  Send location fix request
 *
 * Construct session ID from RRC transaction ID number, source task & queue IDs and
 * RRC protocol ID, send MSBASED or MSASSISTED location request according to
 * requeste method type.
 *
 * \note
 * \param	pRrc     - pointer to RRC object
 * \return  void
 * \sa      rrc_decode
 */
static void sendLocFixReq(TRrc *pRrc)
{
	TNAVC_cmdParams   *pNavcCmd;
    McpU32      uSesId = 0;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);

	if (pNavcCmd)
	{
		EMcpfRes 	eRes;
		TNAVC_reqLocFix *pLocFixReq = &pNavcCmd->tReqLocFix;


		/* set the session ID here based on the protocol */

		uSesId = NAVCD_SESSION_REF_NUM_SET(uSesId, 0); /* 0 to 7 bit set as 0*/
		uSesId = NAVCD_SESSION_PROT_ID_SET(uSesId, NAVCD_PROT_RRC); /* set 8 to 15 bit as RRC Protocol*/
		pRrc->uSessionId = uSesId;
		pLocFixReq->loc_fix_session_id = uSesId;


		/* Set which list bit flags for required assistance data */
		switch (pRrc->uMethodType)
		{
		case POS_METHOD_TYPE_MS_ASSISTED:
			pLocFixReq->loc_fix_mode = GPSC_FIXMODE_MSASSISTED;
			pLocFixReq->loc_fix_result_type_bitmap = GPSC_RESULT_PROT;
			break;

		case POS_METHOD_TYPE_MS_BASED:
			pLocFixReq->loc_fix_mode = GPSC_FIXMODE_MSBASED;
			pLocFixReq->loc_fix_result_type_bitmap = GPSC_RESULT_PROT;
			break;

		case POS_METHOD_TYPE_MS_BASED_PREF:
			pLocFixReq->loc_fix_mode = GPSC_FIXMODE_MSBASED;
			pLocFixReq->loc_fix_result_type_bitmap = GPSC_RESULT_PROT;
			break;

		case POS_METHOD_TYPE_MS_ASSISTED_PREF:
			pLocFixReq->loc_fix_mode = GPSC_FIXMODE_MSASSISTED;
			pLocFixReq->loc_fix_result_type_bitmap = GPSC_RESULT_PROT;
			break;

		default:
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("methodType_decode unknown methodType=%u !\n", pRrc->uMethodType));
			break;
		}

		pLocFixReq->loc_fix_max_ttff	 = pRrc->uPeriodReportingIntLong + MAX_TTFF_OFFSET;
		pLocFixReq->loc_fix_num_reports 	= pRrc->uPeriodReportAmount;
		pLocFixReq->loc_fix_period 		= 0;
		pLocFixReq->loc_sess_type=GPSC_SESSTYPE_LP;

		/* timeout and accuracy */

		pLocFixReq->loc_fix_qop.horizontal_accuracy =  pRrc->uAccuracy;
		pLocFixReq->loc_fix_qop.max_response_time = pRrc->uPeriodReportingIntLong - NAVCD_NETWORK_DELAY; /*2 sec less to accomodate network delay*/

		if (pLocFixReq->loc_fix_num_reports > 1)
		{
			/* For multisession mode, RRC spec 25.331 specifies reponse time should be ignored
			 * and the default value is to be used
			 */

			pLocFixReq->loc_fix_max_ttff = 0;
			pLocFixReq->loc_fix_period 	= pRrc->uPeriodReportingIntLong;

		}

		/* added for moving scenario support */
		if (pLocFixReq->loc_fix_num_reports == 0)
		{
			/* For multisession mode, RRC spec 25.331 specifies reponse time should be ignored
			 * and the default value is to be used
			 */
			pLocFixReq->loc_fix_period 	= pRrc->uPeriodReportingIntLong;
		}

		MCPF_REPORT_INFORMATION(pRrc->hMcpf, NAVC_MODULE_LOG, ("RR: NoOfRep: %d, MaxTTFF: %d, MaxRspTime: %d",
									pLocFixReq->loc_fix_num_reports,
									pLocFixReq->loc_fix_max_ttff,
									pLocFixReq->loc_fix_qop.max_response_time));


		//elements related wifi/sensor pos fix
		pLocFixReq->loc_fix_pos_type_bitmap =0;

		/* Initiate location fix */
		eRes = mcpf_SendMsg (pRrc->hMcpf,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID ,
							 pRrc->eSrcTaskId,
							 pRrc->uSrcQId,
							 NAVC_CMD_REQUEST_FIX,
							 sizeof(pNavcCmd->tReqLocFix),
							 0,
							 pNavcCmd);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pRrc->hMcpf, pNavcCmd);
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("sendLocFixReq: mcpf_SendMsg failed!\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("sendLocFixReq: allocation failed!\n"));
	}
}

/**
 * \fn     sendAssistanceToNavc
 * \brief  Send NAVC assistance command
 *
 *  Sends NAVC assistance command to NAVC Task commnad queue
 * assistance data request
 *
 * \note
 * \param	pRrc  			- pointer to RRC object
 * \param	TNAVC_cmdParams - NAVC assistance command to send
 * \return  void
 * \sa
 */
static void sendAssistanceToNavc (TRrc *pRrc, TNAVC_cmdParams *pNavcCmd)
{
	EMcpfRes	eRes;

	eRes = mcpf_SendMsg (pRrc->hMcpf,
						 TASK_NAV_ID,
						 NAVC_QUE_CMD_ID ,
						 pRrc->eSrcTaskId,
						 pRrc->uSrcQId,
						 NAVC_CMD_INJECT_ASSISTANCE,
						 sizeof(pNavcCmd->tInjectAssist),
						 0,
						 pNavcCmd);

	MCP_HAL_OS_SleepMicroSec(1);

	if (eRes != RES_OK)
	{
		mcpf_mem_free_from_pool (pRrc->hMcpf, pNavcCmd);
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("sendAssistanceToNavc: mcpf_SendMsg failed!\n"));
	}
}

/**
 * \fn     sendLocFixStop
 * \brief  Send NAVC location fix stop command
 *
 *  Allocates and sends NAVC location fix stop command to NAVC Task commnad queue
 *
 * \note
 * \param	pRrc - pointer to RRC object
 * \return  void
 * \sa
 */
static void sendLocFixStop (TRrc *pRrc)
{
	TNAVC_cmdParams *pNavcCmd;
	EMcpfRes		 eRes;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tStopLocFix.uSessionId = pRrc->uSessionId;

		eRes = mcpf_SendMsg (pRrc->hMcpf,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID ,
							 pRrc->eSrcTaskId,
							 pRrc->uSrcQId,
							 NAVC_CMD_STOP_FIX,
							 sizeof(pNavcCmd->tStopLocFix),
							 0,
							 pNavcCmd);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pRrc->hMcpf, pNavcCmd);
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("sendLocFixStop: mcpf_SendMsg failed!\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("sendLocFixStop: allocation failed!\n"));
	}
}

/**
 * \fn     injectGpsTime
 * \brief  Set GPS time in NAVC
 *
 *  Allocates and sends NAVC TOW (GPS time of week) set command to NAVC
 *
 * \note
 * \param	pRrlp - pointer to RRLP object
 * \param	pGpsTime - pointer to GPS time structure
 * \return  void
 * \sa
 */
static void injectGpsTimeToNavc (TRrc *pRrc, TNAVCD_GpsTime *pGpsTime)
{
	TNAVC_cmdParams *pNavcCmd;
	EMcpfRes		 eRes;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrc->hMcpf, pRrc->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tSetTow.tTimeAssist   	= pGpsTime->tTime;
		pNavcCmd->tSetTow.eTimeMode 		= pGpsTime->tTime.time_accuracy;
		pNavcCmd->tSetTow.uSystemTimeInMsec = pGpsTime->uSystemTimeMs;

		eRes = mcpf_SendMsg (pRrc->hMcpf,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID ,
							 pRrc->eSrcTaskId,
							 pRrc->uSrcQId,
							 NAVC_CMD_SET_TOW,
							 sizeof(pNavcCmd->tSetTow),
							 0,
							 pNavcCmd);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pRrc->hMcpf, pNavcCmd);
			MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("injectGpsTimeToNavc: mcpf_SendMsg failed!\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG, ("injectGpsTimeToNavc: allocation failed!\n"));
	}
}

/**
 * \fn     rrcDataReset
 * \brief  Reset RRC object state
 *
 * Reset (zero) RRC object fields
 *
 * \note
 * \param	pRrc     - pointer to RRC object
 * \return  void
 * \sa      rrc_decode
 */
static void rrcDataReset (TRrc *pRrc)
{
	handle_t	hMcpf, hCmdPool;
	EmcpTaskId  eSrcTaskId;
	McpU8 		uIdx, uSrcQId;

	/* Store MCPF & NAVC handlers to be restored after setting memory to zero */
	hMcpf 	    = pRrc->hMcpf;
	hCmdPool 	= pRrc->hCmdPool;
	eSrcTaskId 	= pRrc->eSrcTaskId;
	uSrcQId		= pRrc->uSrcQId;

	mcpf_mem_zero (hMcpf, pRrc, sizeof(TRrc));

	/* Restore MCPF & NAVC handlers */
	pRrc->hMcpf 	 = hMcpf;
	pRrc->hCmdPool 	 = hCmdPool;
	pRrc->eSrcTaskId = eSrcTaskId;
	pRrc->uSrcQId    = uSrcQId;

	/* Set mandatory bits for both modes: MS-Based and MS-Assisted */
	pRrc->uWishlist = GPSC_REQ_AA | GPSC_REQ_LOC | GPSC_REQ_TIME | GPSC_REQ_NAV;


	/* If SMLC doesn't indicate more assistance data to come, it means
	 * no more to come and will be safe to inject whatever has been received,
	 * if any. SnapTrack SMLC will always indicate whether or not more data is
	 * coming, but this is optional by the GSM standard.
	 * For non-SnapTrack but compliant SMLCs, if the optional field is absent,
	 * it means no more to come. So we set default to being FALSE and
	 * set it to TRUE if that optional field in SMLC indicates so
     */

	pRrc->tGpsTime.tTime.gps_week = C_GPS_WEEK_UNKNOWN;

	for (uIdx=0; uIdx<C_MAX_SVS_ACQ_ASSIST; uIdx++)
	{
		pRrc->tAcquisitionAssist.tAcquisElement[uIdx].uDoppler1   = 0xFF;
		pRrc->tAcquisitionAssist.tAcquisElement[uIdx].uDopplerUnc = 0xFF;
		pRrc->tAcquisitionAssist.tAddAngle[uIdx].uAzimuth   	  = 0xFF;
		pRrc->tAcquisitionAssist.tAddAngle[uIdx].uElevation 	  = 0xFF;
	}

	pRrc->uTransactionId = 0xFF;      	   /* no RRC session             */
	pRrc->uMethodType 	 = 0xFF;    	   /* indicating no PosInstruct  */
	pRrc->uAccuracy   	 = 0xFFFF;    	   /* indicating no accuracy     */
}


/**
 * \fn     gps_msr_info_construct
 * \brief  Fill RRC GPS measurement information element
 *
 *  This function constructs GPS mesurement results in an RRC compliant format,
 *  using information of GPS information which is just updated with the new
 *  fix event that triggered the entering of this function.
 *
 * \note
 * \param	pRrc    		  - pointer to RRC object
 * \param	pRrcMsr    		  - pointer to output RRC measurement info structure
 * \param	pLoc_fix_response - pointer to input GPS location fix response structure
 * \return  void
 * \sa
 */
static void gps_msr_info_construct (TRrc 					*pRrc,
									TPosGpsMsrResult 		*pRrcMsr,
									T_GPSC_loc_fix_response *pLoc_fix_response)
{
	T_GPSC_prot_measurement			 *pMsrInfo = &pLoc_fix_response->loc_fix_response_union.prot_measurement;
	TMsrResultReferencetimeChoice    *pRefTimeChoice = &pRrcMsr->tMsrResultReferencetime.tMsrResultReferencetimeChoice;
	McpU8 uIdx, uMaxSat;


	/* UE-Positioning-GPS-MeasurementResults */

	pRrcMsr->tMsrResultReferencetime.uReferencetimeChoice = RRC_POS_MSR_RES_CHOISE_GPS_REF_TIME_ONLY;
	pRefTimeChoice->uMsrResultGpsReferencetimeOnly = pMsrInfo->gps_tow;

	uMaxSat = pMsrInfo->c_sv_measurement;
	pRrcMsr->uLength_R2  = uMaxSat;
	pRrcMsr->uLength_R2 -= 1;

	for (uIdx=0; uIdx < uMaxSat; uIdx++)
	{
		T_GPSC_sv_measurement	*pMeasPerSv;
		TMsrParamLst  			*pRrcMsrElm;

		pMeasPerSv = &pMsrInfo->sv_measurement[uIdx];
		pRrcMsrElm = &pRrcMsr->tMsrParamLst[uIdx];

		pRrcMsrElm->uSatelliteID 		 = pMeasPerSv->svid;
		/* Cno. Add in noise figure and implementation loss (then round and
		   switch from 0.1 to 1 dB-Hz/bit units) in order to reference it to
		   the antenna input. */
		pRrcMsrElm->uCN0         		 = (U8)((pMeasPerSv->cno + 5) / 10);
		pRrcMsrElm->uDoppler	 		 = pMeasPerSv->doppler + 32768L;
		pRrcMsrElm->uFractionalGPSChips  = pMeasPerSv->frac_chips;
		pRrcMsrElm->uMultipathIndicator  = pMeasPerSv->multipath_indicator;
		pRrcMsrElm->uPseudorangeRMSError = pMeasPerSv->pseudorange_rms_error;
		pRrcMsrElm->uWholeGPSChips 		 = pMeasPerSv->whole_chips;

		 if ( pRrcMsrElm->uWholeGPSChips > 1022 )
		 {
			 MCPF_REPORT_ERROR(pRrc->hMcpf,
							   RR_MODULE_LOG,
							   ("gps_msr_info_construct invalid WholeChip=%u !\n",
								pRrcMsrElm->uWholeGPSChips));
		 }
	}
}


/**
 * \fn     rrc_gps_msr_encode
 * \brief  Fill RRC GPS measurement information element
 *
 *  This function encodes RRC compliant structure by measurement
 *  information
 *
 * \note
 * \param	pRrc       - pointer to RRC object
 * \param	pMsrRslt   - pointer to input GPS measurement information  structure
 * \param	pMsgBuf    - pointer to output message buffer containing encoded message
 * \param	pBitOffset - pointer to bit offset from start of message buffer to put
 *                       encoded RRC fields, the value is incremented by written
 *                       fields bit length
 * \return  void
 * \sa
 */
static void rrc_gps_msr_encode (TRrc 					*pRrc,
							    TPositionMeasuredResult *pMsrRslt,
								McpU8         			*pMsgBuf,
								McpU32   	    		*pBitOffset)
{


	/* Start encoding of UE-Positioning-MeasuredResults RRC information element */
	mcpf_putBits (pMsgBuf, pBitOffset, pMsrRslt->uOptPream_R3, 4);

	if(pMsrRslt->uOptPream_R3 & C_MSRRPT_OPT_PREAM_OTDOA)
	{
	  /*Do Nothing, is not Required UE-Positioning-OTDOA-Measurement*/
		MCPF_REPORT_ERROR(pRrc->hMcpf, RR_MODULE_LOG,
						  ("rrc_gps_msr_encode invalid uOptPream_R3=%u\n", pMsrRslt->uOptPream_R3));
	}

	if (pMsrRslt->uOptPream_R3 & C_MSRRPT_OPT_PREAM_LOC)
	{
		TReferenceTime 		*pRefTime = &pMsrRslt->tPosEstimateInfo.tReferenceTime;
		TEllipPointAltEllip *pEllip = &pMsrRslt->tPosEstimateInfo.tPosEstimate.tPosEstimateChoice.tEllipPointAltEllip;

		/* UE-Positioning-PositionEstimateInfo */


		pRefTime->uRefTimechoice = 0x01;

		mcpf_putBits (pMsgBuf, pBitOffset, pRefTime->uRefTimechoice, 2);

		mcpf_putBits (pMsgBuf, pBitOffset, pRefTime->tReferenceTimeChoice.uGpsReferencetimeOnly, 30);


		pMsrRslt->tPosEstimateInfo.tPosEstimate.uPosestChoice = 0x04;

		mcpf_putBits (pMsgBuf,
					  pBitOffset,
					  pMsrRslt->tPosEstimateInfo.tPosEstimate.uPosestChoice,
					  3);

		mcpf_putBits (pMsgBuf, pBitOffset, pEllip->uLatitudeSign, 1);

		mcpf_putBits (pMsgBuf, pBitOffset, pEllip->uLatitude, 23);

		mcpf_putBits (pMsgBuf, pBitOffset, pEllip->uLongitude, 24);

		mcpf_putBits (pMsgBuf, pBitOffset, pEllip->uAltitudeSign, 1);

		mcpf_putBits (pMsgBuf, pBitOffset, pEllip->uAltitude, 15);

		mcpf_putBits (pMsgBuf, pBitOffset, pEllip->uUncMajor, 7);

		mcpf_putBits (pMsgBuf, pBitOffset, pEllip->uUncMinor, 7);

		mcpf_putBits (pMsgBuf, pBitOffset, pEllip->uOrientMajor, 7);

		mcpf_putBits (pMsgBuf, pBitOffset, pEllip->uUncAlt, 7);

		mcpf_putBits (pMsgBuf, pBitOffset, pEllip->uConfidence, 7);

	}

	if (pMsrRslt->uOptPream_R3 & C_MSRRPT_OPT_PREAM_MEAS)
	{
	   /* UE-Positioning-GPS-MeasurementResults */

		TMsrResultReferencetime *pRefTime = &pMsrRslt->tPosGpsMsrResult.tMsrResultReferencetime;
		McpU8 uIdx, uMaxSat;

		mcpf_putBits (pMsgBuf, pBitOffset, pRefTime->uReferencetimeChoice, 1);

		mcpf_putBits (pMsgBuf, pBitOffset, pRefTime->tMsrResultReferencetimeChoice.uMsrResultGpsReferencetimeOnly, 30);

		mcpf_putBits (pMsgBuf, pBitOffset, pMsrRslt->tPosGpsMsrResult.uLength_R2, 4);

		uMaxSat = (McpU8) (pMsrRslt->tPosGpsMsrResult.uLength_R2 + 1);

		for (uIdx=0; uIdx < uMaxSat; uIdx++)
		{
			TMsrParamLst *pParamLst;

			pParamLst = &pMsrRslt->tPosGpsMsrResult.tMsrParamLst[uIdx];

			/* GPS-MeasurementParam */
			mcpf_putBits (pMsgBuf, pBitOffset, pParamLst->uSatelliteID, 6);

			mcpf_putBits (pMsgBuf, pBitOffset, pParamLst->uCN0, 6);

			mcpf_putBits (pMsgBuf, pBitOffset, pParamLst->uDoppler, 17); //corrected on ULTS test

			mcpf_putBits (pMsgBuf, pBitOffset, pParamLst->uWholeGPSChips, 10);

			mcpf_putBits (pMsgBuf, pBitOffset, pParamLst->uFractionalGPSChips, 10);

			mcpf_putBits (pMsgBuf, pBitOffset, pParamLst->uMultipathIndicator, 2);

			mcpf_putBits (pMsgBuf, pBitOffset, pParamLst->uPseudorangeRMSError, 6);
	   }
	}

	if(pMsrRslt->uOptPream_R3 & C_MSRRPT_OPT_PREAM_POSERR)
	{
        TPosError *pErr = &pMsrRslt->tPosError;

        /* UE-Positioning-Error */
        mcpf_putBits (pMsgBuf, pBitOffset, pErr->uOptParam_R1, 1);
        mcpf_putBits (pMsgBuf, pBitOffset, pErr->uErrorCause,  3);

        if (pErr->uOptParam_R1)
        {
            /* UE-Positioning-GPS-AdditionalAssistanceDataRequest */

            mcpf_putBits (pMsgBuf, pBitOffset, pErr->tAddAsistDataReq.uOptPreambNav, 1);

            mcpf_putBits (pMsgBuf, pBitOffset, pErr->tAddAsistDataReq.bAlmanacRequest, 1);
            mcpf_putBits (pMsgBuf, pBitOffset, pErr->tAddAsistDataReq.bUtcModelRequest, 1);
            mcpf_putBits (pMsgBuf, pBitOffset, pErr->tAddAsistDataReq.bIonosphericModelRequest, 1);
            mcpf_putBits (pMsgBuf, pBitOffset, pErr->tAddAsistDataReq.bNavigationModelRequest, 1);
            mcpf_putBits (pMsgBuf, pBitOffset, pErr->tAddAsistDataReq.bDgpsCorrectionsRequest, 1);
            mcpf_putBits (pMsgBuf, pBitOffset, pErr->tAddAsistDataReq.bReferenceLocationRequest, 1);
            mcpf_putBits (pMsgBuf, pBitOffset, pErr->tAddAsistDataReq.bReferenceTimeRequest, 1);
            mcpf_putBits (pMsgBuf, pBitOffset, pErr->tAddAsistDataReq.bAcquisitionAssistanceRequest, 1);
            mcpf_putBits (pMsgBuf, pBitOffset, pErr->tAddAsistDataReq.bRealTimeIntegrityRequest, 1);

            if (pErr->tAddAsistDataReq.uOptPreambNav)
            {
                TNavModel *pNavModel =  &pErr->tAddAsistDataReq.tNavModel;
                McpU8      uInd;

                /* UE-Positioning-GPS-NavModelAddDataReq */

                mcpf_putBits (pMsgBuf, pBitOffset, pNavModel->uGpsWeek, 10);
                mcpf_putBits (pMsgBuf, pBitOffset, pNavModel->uGpsToe,   8);
                mcpf_putBits (pMsgBuf, pBitOffset, pNavModel->uToeLimit, 4);
                mcpf_putBits (pMsgBuf, pBitOffset, pNavModel->uSize, 5);        /* 0..16 */

                for (uInd = 0; uInd < pNavModel->uSize; uInd++)
                {
                    /* SatDataList */
                    mcpf_putBits (pMsgBuf, pBitOffset, pNavModel->tSatDataList[uInd].satID, 6);
                    mcpf_putBits (pMsgBuf, pBitOffset, pNavModel->tSatDataList[uInd].iode,  8);
                }
            }
        }
	}
}

/**
 * \fn     ext_geo_loc_construct
 * \brief  Fill the RRC external geographical location information
 *
 *  This function constructs the external geographical location information
 *  data type as defined in RRC standard
 *
 * \note
 * \param	pRrcPos    - pointer to the output RRC structure
 * \param	T_GPSC_loc_fix_response - pointer to location fix response structure
 * \return  void
 * \sa
 */
static void ext_geo_loc_construct (TPosEstimateInfo	 		*pRrcPos,
                                   T_GPSC_loc_fix_response	*pLoc_fix_response)
{
	TEllipPointAltEllip  *pRrcEllip   = &pRrcPos->tPosEstimate.tPosEstimateChoice.tEllipPointAltEllip;
	TReferencetimeChoice *pRrcRefTime = &pRrcPos->tReferenceTime.tReferenceTimeChoice;
	T_GPSC_prot_position 		*pPosInfo    = &pLoc_fix_response->loc_fix_response_union.prot_position;
	T_GPSC_ellip_alt_unc_ellip	*pGpsPosInfo = &pPosInfo->ellip_alt_unc_ellip;


	pRrcPos->tPosEstimate.uPosestChoice = RRC_POS_EST_CHOISE_ELLIP_POINT_ALTIT_ELLIP;

	pRrcEllip->uAltitude 	 = pGpsPosInfo->altitude;
	pRrcEllip->uAltitudeSign = pGpsPosInfo->altitude_sign;
	pRrcEllip->uConfidence 	 = pGpsPosInfo->confidence;
	pRrcEllip->uLatitude 	 = pGpsPosInfo->latitude;
	pRrcEllip->uLatitudeSign = (McpU8)((pGpsPosInfo->latitude_sign != 0) ? 1 : 0);
	pRrcEllip->uLongitude 	 = pGpsPosInfo->longitude + 8388608L;
	pRrcEllip->uOrientMajor  = pGpsPosInfo->orient_major;
	pRrcEllip->uUncAlt 		 = pGpsPosInfo->unc_alt;
	pRrcEllip->uUncMajor 	 = pGpsPosInfo->unc_major;
	pRrcEllip->uUncMinor 	 = pGpsPosInfo->unc_minor;

	pRrcPos->tReferenceTime.uRefTimechoice = RRC_POS_EST_INFO_CHOISE_GPS_REF_TIME_ONLY;
	pRrcRefTime->uGpsReferencetimeOnly = pPosInfo->gps_tow;

}

/**
 * \fn     pos_error_construct
 * \brief  Construct erorr response
 *
 *  Build RRC position error response including error cause and missed
 * assistance data request
 *
 * \note
 * \param	pLocError  	- pointer to location error structure
 * \param	eCause 		- error cause/reason
 * \param	pRrcPosErr  - RRC compliant error structure to fill
 * \return  void
 * \sa
 */
static void pos_error_construct (TNAVCD_LocError 	*pLocError,
								 ERrc_PosErrorCause eCause,
								 TPosError 			*pRrcPosErr)
{
    pRrcPosErr->uErrorCause = (McpU8) eCause;

    if (eCause == C_RRC_LOC_REASON_ASSIS_MIS)
    {
        /* UE-Positioning-GPS-AdditionalAssistanceDataRequest presents */
        McpU16 uWishList = (McpU16) (pLocError->uAssistBitMapMandatory | pLocError->uAssistBitMapOptional);

        pRrcPosErr->uOptParam_R1 = 1;

        if (uWishList & GPSC_REQ_ALMANAC)
        {
            pRrcPosErr->tAddAsistDataReq.bAlmanacRequest = 1;
        }
        if (uWishList & GPSC_REQ_UTC)
        {
            pRrcPosErr->tAddAsistDataReq.bUtcModelRequest = 1;
        }
        if (uWishList & GPSC_REQ_IONO)
        {
            pRrcPosErr->tAddAsistDataReq.bIonosphericModelRequest = 1;
        }
        if (uWishList & GPSC_REQ_NAV)
        {
            pRrcPosErr->tAddAsistDataReq.bNavigationModelRequest = 1;

            if (pLocError->pNavModel)
            {
                TNavModel *pRrcNavModel = &pRrcPosErr->tAddAsistDataReq.tNavModel;
                McpU8      uInd;

                pRrcPosErr->tAddAsistDataReq.uOptPreambNav = 1;

                /* UE-Positioning-GPS-NavModelAddDataReq */
                pRrcNavModel->uGpsWeek  = (McpU16) (pLocError->pNavModel->gps_week % 1024);
                pRrcNavModel->uGpsToe   = pLocError->pNavModel->nav_toe;
                pRrcNavModel->uToeLimit = pLocError->pNavModel->nav_toe_limit;
                pRrcNavModel->uSize     = pLocError->pNavModel->nav_num_svs;

                for (uInd = 0; uInd < pLocError->pNavModel->nav_num_svs; uInd++)
                {
                    pRrcNavModel->tSatDataList[uInd].satID = pLocError->pNavModel->nav_data[uInd].svid;
                    pRrcNavModel->tSatDataList[uInd].iode  = pLocError->pNavModel->nav_data[uInd].iode;
                }
            }
        }
        if (uWishList & GPSC_REQ_DGPS)
        {
            pRrcPosErr->tAddAsistDataReq.bDgpsCorrectionsRequest = 1;
        }
        if (uWishList & GPSC_REQ_LOC)
        {
            pRrcPosErr->tAddAsistDataReq.bReferenceLocationRequest = 1;
        }
        if (uWishList & GPSC_REQ_TIME)
        {
            pRrcPosErr->tAddAsistDataReq.bReferenceTimeRequest = 1;
        }
        if (uWishList & GPSC_REQ_AA)
        {
            pRrcPosErr->tAddAsistDataReq.bAcquisitionAssistanceRequest = 1;
        }
        if (uWishList & GPSC_REQ_RTI)
        {
            pRrcPosErr->tAddAsistDataReq.bRealTimeIntegrityRequest = 1;
        }
    }
    else
    {
        pRrcPosErr->uOptParam_R1 = 0;
    }
}


#ifdef RRC_DEBUG
static void bitString (McpU8 uByte, McpU8 *pBitString)
{
	McpU8	i;
	McpU8	uMask = 0x80;

	for (i=0; i<8; i++, uMask >>= 1)
	{
		if (uByte & uMask)
		{
			pBitString[i] = '1';
		}
		else
		{
			pBitString[i] = '0';
		}
	}
	 pBitString[i] = 0;
}

static void dumpMsg (TRrc *pRrc, McpU8 * pMsgBuf, McpU16 uMsgLength)
{
	McpU32	uIndx;
	McpU8   uBitString[10];

	for (uIndx=0; uIndx < uMsgLength; uIndx++)
	{
		bitString (pMsgBuf[uIndx], uBitString);
		MCPF_REPORT_INFORMATION( pRrc->hMcpf, RR_MODULE_LOG, ("[%3u %5u-%5u] = %02x  %s\n", uIndx, uIndx*8, uIndx*8+7, pMsgBuf[uIndx], uBitString));
	}
}
#endif







