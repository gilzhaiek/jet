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

/** \file   navc_rrlp.c
 *  \brief  RRLP (Radio Resource LCS Protocol) module implementation
 *
 * This file contains functions to decode RRLP messages received from the SMLC
 * (Serving Mobile Location Centre) and to encode RRLP messages tranmitted to SMLC.
 *
 *
 *  \see    navc_rrlp.h
 */

#include "mcpf_defs.h"
#include "mcpf_mem.h"
#include "mcpf_msg.h"
#include "mcpf_report.h"
#include "mcpf_services.h"
#include <math.h>
#include "navc_defs.h"
#include "navc_api.h"
#include "navc_internalEvtHandler.h"
#include "navc_cmdHandler.h"
#include "navc_rrlp.h"
#include "navl_api.h"
#include "mcp_hal_os.h"
#include <utils/Log.h>

#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"



/************************************************************************
 * Defines
 ************************************************************************/

/* Optional Parameters Preamble for Assistance Data Sequence */
#define C_ASSIS_OPT_PREAM_EXTCONT   0x01 /* Bit 1 (right most bit of 6):
                                            extension container 			*/
#define C_ASSIS_OPT_PREAM_MORE_DATA 0x02 /* Bit 2: more AssisDataToBeSent 	*/
#define C_ASSIS_OPT_PREAM_GPSASSIS  0x04 /* Bit 3: GPS Asisstance Data 		*/
#define C_ASSIS_OPT_PREAM_SYSINFO   0x08 /* Bit 4: systemInfoAssistData 	*/
#define C_ASSIS_OPT_PREAM_MSR_ASSIS 0x10 /* Bit 5: msrAssisData 			*/
#define C_ASSIS_OPT_PREAM_REF_ASSIS 0x20 /* Bit 6: referenceAssistData 		*/

/* Optional Parameters Preamble for GPS-Assistance Data Sequence */
#define C_GPSASSIS_OPT_PREAM_RTI    0x001  /* realTimeInetrity 	*/
#define C_GPSASSIS_OPT_PREAM_ACQ    0x002  /* acquisAssis 		*/
#define C_GPSASSIS_OPT_PREAM_ALM    0x004  /* almanac 			*/
#define C_GPSASSIS_OPT_PREAM_UTC    0x008  /* utcModel 			*/
#define C_GPSASSIS_OPT_PREAM_IONO   0x010  /* ionosphericModel 	*/
#define C_GPSASSIS_OPT_PREAM_NAVM   0x020  /* navigationModel 	*/
#define C_GPSASSIS_OPT_PREAM_DGPS   0x040  /* dgpsCorrections 	*/
#define C_GPSASSIS_OPT_PREAM_REFL   0x080  /* refLocation 		*/
#define C_GPSASSIS_OPT_PREAM_REFT   0x100  /* referenceTime 	*/



/* RRLP measurement position response options */
#define C_POSRESP_OPT_PREAM_MULTIPLE_SETS     	0x40
#define C_POSRESP_OPT_PREAM_REFERENCE_IDENTITY  0x20
#define C_POSRESP_OPT_PREAM_OTD_MEASUREINFO     0x10
#define C_POSRESP_OPT_PREAM_LOC     			0x08   /* location info   */
#define C_POSRESP_OPT_PREAM_MEAS    			0x04   /* gps-Measurement */
#define C_POSRESP_OPT_PREAM_LOCERR  			0x02   /* Location Error  */
#define C_POSRESP_OPT_EXT_MOBILE_ID 			0x01   /* Mobile ID Private Extension */

//RRLP-REL5 Start
#define C_POSRESP_OPT_EXT_REL98					0x02
#define C_POSRESP_OPT_EXT_REL5					0x01
//RRLP-REL5 End


/* uAvailabilityFlags bitmask values are equal to GPSC_REQ_xxx bits,
 * extension is defined bellow
 */
#define C_ANGLE_AVAIL        0x0400
#define C_REF_GSMTIME_AVAIL  0x0800 		/* optional for ref time */
#define C_REF_TOW_AVAIL      0x1000 		/* optional for ref time */

#define RRLP_DEBUG
/* uRrlpChoiceTag (RRLP PDU Choice Tags) */
enum
{
  C_RRLP_CHOICE_MSRPOSREQ,
  C_RRLP_CHOICE_MSRPOSRESP,
  C_RRLP_CHOICE_ASSISTDATA,
  C_RRLP_CHOICE_ASSISTACK,
  C_RRLP_CHOICE_PROTCOL_ERR
};

/* Optional Parameter Preamble for MsrPositionRequest Sequence */
#define C_MSRREQ_REF_ASSIS		0x10
#define C_MSRREQ_MSR_ASSIS		0x08
#define C_MSRREQ_SYSINFO_ASSIS  0x04
#define C_MSRREQ_GPS_ASSIS      0x02
#define C_MSRREQ_EXT_CONT       0x01

/* RRLP ProtocolError IDs */
enum
{
  C_RRLP_PROTERR_UNDEFINED,
  C_RRLP_PROTERR_MISSING_COMPONENT,
  C_RRLP_PROTERR_INCORRECT_DATA,
  C_RRLP_PROTERR_MISSING_IE_OR_COMPONENT_ELEMENT,
  C_RRLP_PROTERR_MESSAGE_TOO_SHORT,
  C_RRLP_PROTERR_UNKNOWN_REFERENCE_NUMBER
};

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

/* Measurment request.position instruction.position method */
#define NAVC_RRLP_POSITION_METHOD_EOTD		0
#define NAVC_RRLP_POSITION_METHOD_GPS		1
#define NAVC_RRLP_POSITION_METHOD_GPS_EOTD	2

/* Assistance data request */
#define GPS_ASSIST_DATA_REQ_MAX_SIZE	50
#define GPS_ASSIST_DATA_REQ_IEI			0x4B

/* RRLP Position Method type */
enum
{
	ENAVC_RRLP_POS_METHOD_TYPE_MS_ASSISTED,
	ENAVC_RRLP_POS_METHOD_TYPE_MS_BASED,
	ENAVC_RRLP_POS_METHOD_TYPE_MS_BASED_PREF,
	ENAVC_RRLP_POS_METHOD_TYPE_MS_ASSISTED_PREF

} ENAVC_RRLP_POS_METHOD_TYPE;


/************************************************************************
 * Types
 ************************************************************************/

/* Types for RRLP Response Message */

/* RRLP sequence of Gps-msrElement */
typedef struct
{
  McpU8  u_SvId_R6;         /* 0..63, R6 	*/
  McpU8  u_Cno_R6;          /* 0..63, R6 	*/
  McpU16 w_Doppler_R16;     /* -32768..32768, R16, LSB = 0.2 Hz */
  McpU16 w_WholeChips_R10;  /* 0..1022, R10 */
  McpU16 w_FracChips_R11;   /* 0..1024, R11 */
  McpU8  u_MpathIndic_R2;   /* 0..3, R2 	*/
  McpU8  u_PseuRangeRMS_R6; /* 0..63, R6 	*/

  U32 q_Msec;            /* whole SvTime msec, not part of the PDU, for debug only 	*/
  DBL d_AdjSubMsec;      /* total submsec adjusted for bias, for debug only 		*/

}GpsMsrElement;

/* RRLP sequence of gps-msrList */
typedef struct
{
  McpU8        u_Length_R4; /* 4..12, R4 */
  GpsMsrElement  z_GpsMsrElement[N_LCHAN];

}GpsMsrList;

/* RRLP Gps-MsrSetElement Sequence */
typedef struct
{
  McpU8        	u_OptPream_R1; 		/* 0..1, R1 	    */
  McpU16        w_RefFrame_R16; 	/* 0..65535, R16    */
  McpU32        q_GpsTow_R24;  		/* 0..14399999, R24 */
  GpsMsrList    z_GpsMrsList;

}GpsMsrSetElement;

/* RRLP Gps-MeasurementInfo Sequence */
typedef struct
{
  McpU8          	u_Length_R2;   /* 1..3, R2 */
  GpsMsrSetElement  z_GpsMsrSetElement;

}GpsMsrInfo;

typedef struct
{
  McpU8   uAssistDataLen;  							 /*  0..40 bytes */
  McpU8   uAssistData[GPS_ASSIST_DATA_REQ_MAX_SIZE]; /* assist. data complient to 3GPP TS 49.031 standard */

}GpsAssistData;

typedef struct
{
  McpU8  		u_OptPreambleAdd;
  McpU8  		u_ExtContAdd;
  GpsAssistData z_GpsAssistData;

}AdditionAssistData;

typedef struct
{
  McpU8  u_ExtInd_R1;
  McpU8  u_OptParam_R1;
  McpU8  u_ExtIndLocReason_R1;
  McpU8  u_LocationReason_R4;

  /* may include the optional AdditionalAssistaceData later */
  AdditionAssistData   z_AdditionAssistData;

}LocErr;

#ifdef MOBILE_ID_EXTENSION_CONTAINER

/* Object ID for the Mobild ID Extension
 *
 * This definition is arbitrary.
 * It will change when formally registered ID is used.
 */

#define OBJECTID_MOBILE_ID_BYTE_0 0x2b /* 40 * 1 + 3 */
#define OBJECTID_MOBILE_ID_BYTE_1 0x00 /* 0 */
#define OBJECTID_MOBILE_ID_BYTE_2 0x01 /* 1 */

/* MOBILE_ID_EXTENSION_CONTAINER */
typedef struct {
  McpU8  u_ParameterBitmap;
  McpU8  u_ObjectIDLength;
  McpU8  u_ObjecttID[3];
  McpU8  u_LengthDeterminant;
  McpU8  u_ExtensionMarker;
  McpU8  u_TestExtensionRevision;
  McpU8  u_TransposedImsi[8];
  McpU8  u_PaddingBits;

} TestExtension;

typedef struct {
  McpU8  u_Valid;
  McpU8  u_ExtensionIndicator;
  McpU8  u_ParameterBitmap;
  McpU8  u_ExtensionCount;
  TestExtension z_Test;

} PrivExt;
#endif

//RRLP-REL5 Start

typedef struct
{
  McpU8		u_smlccode_R6;          /* 0 -63 to indicate SMLC ID*/
  McpU32	u_transactionID_R18;     /*  0 ... 262143 to indicate transaction ID */

}ExtendedRef;


typedef struct
{
  McpU8    u_Rel5MsrRspExt1_R7;
  McpU8    u_OptParam_R2;
  McpU8    u_Rel5MsrRspExt2_R8;
  McpU8    u_Rel5_Marker_R1;
  McpU8    u_OptParam_R3;
  ExtendedRef   z_ExtRef;

}MsrPosRsp_Rel5Ext;

//RRLP-REL5 End


typedef struct
{
  McpU8 u_ShapeCode; 	/* use 0x90 */
  McpU8 u_Lat0;
  McpU8 u_Lat1;
  McpU8 u_Lat2;
  McpU8 u_Long0;
  McpU8 u_Long1;
  McpU8 u_Long2;
  McpU8 u_Alt0;
  McpU8 u_Alt1;
  McpU8 u_UncMajor; 	/* unc semi-major */
  McpU8 u_UncMinor; 	/* unc semi-minor */
  McpU8 u_OrientMajor; 	/* orientation of major axis */
  McpU8 u_UncAlt;   	/* unc. altitude */
  McpU8 u_Confidence;
}EllipAltUncEllip;

typedef struct
{
  McpU8 u_ShapeCode; 	/* use 0x30 */
  McpU8 u_Lat0;
  McpU8 u_Lat1;
  McpU8 u_Lat2;
  McpU8 u_Long0;
  McpU8 u_Long1;
  McpU8 u_Long2;
  McpU8 u_UncMajor; 	/* unc semi-major */
  McpU8 u_UncMinor; 	/* unc semi-minor */
  McpU8 u_OrientMajor; 	/* orientation of major axis */
  McpU8 u_Confidence;
}EllipUncEllip;

typedef union
{
  /* ellipsoid point w/ altitude and uncertainty ellipsoid */
  EllipAltUncEllip z_EllipAltUncEllip;

  /* more to be added later */
   /* ellipsoid point w/  uncertainty ellipse */
  EllipUncEllip    z_EllipUncEllip;

}PosEstimate;

typedef struct
{
  McpU8      	u_OptParam_R1; /* 1: opt. parameter gpsTow present;
                                0: not present */
  McpU8      	u_RefFrame_R16;
  McpU32     	q_GpsTow_R24;
  McpU8      	u_FixType_R1;
  PosEstimate 	z_PosEstimate;
}LocInfo;


/* RRLP MsrPositionRepsonse Sequence, with only GpsMsrInfo */
typedef struct
{
  McpU8      	u_ExtIndPream_R1; 	/* ext. indicator preamble     */
  McpU8      	u_OptPream_R7;    	/* optional parameter preamble */
  LocInfo     	z_LocInfo;
  GpsMsrInfo  	z_GpsMsrInfo;
  LocErr      	z_LocErr;
#ifdef MOBILE_ID_EXTENSION_CONTAINER
  PrivExt    	z_MobileIdExt;
#endif
  MsrPosRsp_Rel5Ext  z_MsrPosRsp_Rel5Ext;


}MsrPosResp;


 /* Position Instruct Sequence */
typedef struct
{
  McpU8 u_OptPream_R1; 			/* if 1: environment character is present */

  McpU8 u_ChoiceTag_R2; 		/* 0: msAssisted; 1: msBased; 2: MsBasedPref; 3: msAsssitedPref; */

  McpU8 u_EstAccOptPream_R1; 	/* if Choice = 0, this is present, which decides if Accuracy is present; for
								 * all other values of Choice, Accuracy always is present
								 */
  McpU8 u_Accuracy_R7; 			/* integer from 1 to 127 			*/
  McpU8 u_PosMethod_R2; 		/* 0: EOTD; 1: GPS; 2: Gps or EOTD 	*/
  McpU8 u_MeasRespTime_R3;
  McpU8 u_UseMultiSets_R1; 		/* 0: multi sets; 1: one set */
  McpU8 u_EnvChar_R2; 			/* 0: badArea; 1: notBadArea; 2: mixedArea */

}PosInstruct;

/* Measurement Position Request: currently only supports one optional parameter: GpsAssistance */
typedef struct
{
  McpU8      	u_ExtIndPream_R1; 	/* ext. indicator preamble 		*/
  McpU8      	u_OptPream_R5; 		/* optional parameter preamble 	*/
  PosInstruct  	z_PosInstruct; 		/* Position Instruction 		*/

  /* to save time and memory, there is no need to store
     incoming GpsAssistance sequences, they will be decoded
     on the spot and processed accordingly, therefore this
     structure does not include these sequences */

}MsrPosReq;

typedef union
{
  MsrPosReq  z_MsrPosReq;
  MsrPosResp z_MsrPosResp;

}CompSeqOfChoice;

/* Component of the RRLP PDU sequence */
typedef struct
{
  McpU8          	u_ExtIndPream_R1;
  McpU8          	u_ChoiceTag_R3;
  CompSeqOfChoice   z_SeqOfChoice;

}RrlpComp;

/* Protocol Data Unit (PDU) */
typedef struct
{
  McpU8     u_RefNum_R3;
  RrlpComp  z_RrlpComp;

}RrlpPdu;


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

} TRrlp_acqElement;  	/* raw RRLP acquisition assistance's 2's complement */

typedef struct
{
  McpU8  uAzimuth;    		/* 5 bit data. Resolution = 11.25 degrees.  */
  McpU8  uElevation;  		/* 3 bit data, Resolution = 11.25 degrees. 0xFF to indicate invalid */

} TRrlp_additionalAngle;  	/* raw RRLP acquisition assistance's data */


typedef struct
{
  McpU32           		uGpsTow;  				/* GPS TimeOfWeek: in msec */
  McpU32		   		uAcqAsstListedSVs; 		/* SV List (bitmap) for Visible Satellites */
  McpU8            		uNumSV;   				/* Number of SVs in the RRLP message */
  TRrlp_acqElement      tAcquisElement[RRLP_MAX_SVS_ACQ_ASSIST];
  TRrlp_additionalAngle tAddAngle[RRLP_MAX_SVS_ACQ_ASSIST];

} TRrlp_acquisitionAssist;


typedef struct
{
	McpBool						bValidGsmTime;
	McpU16						bcchCarrier;
	McpU8						bsic;
	McpU32						frameNumber;
	McpU8						timeSlot;
	McpU8						bitNumber;

} TRrlp_gsmTimeAssist;

typedef struct
{
	handle_t hMcpf;
	handle_t hCmdPool;
	handle_t 	hEvtPool;

	EmcpTaskId  eSrcTaskId;
	McpU8		uSrcQId;

	McpU16 	uAvailabilityFlags;
	McpU16 	uWishlist;

	/* current frame params */
	McpU8 	uRefNum;
	McpU8	uRrlpExtInd;
	McpU8 	uRrlpChoiceTag;
	McpBool	bMoreData;			/* more assistance data is expected */
	McpU32	uSystemTimeMs;		/* system time (ms) of recieved RRLP message */

	/* Rrlp Protocol Error */
	McpU8  	uProtocolError;
	McpU8  	uValidExtInd;
	McpU8	uExtProtoclError;

	/* msrPos-Req 			*/
	/*    positionInstrcut  */
	McpU8 	uMethodType;
	McpU16 	uAccuracy;
    /*	  end-of-positionInstrcut */

	McpU8 	uPosMethod; 			/* eotd (0), gps (1), gpsOrEOTD (2) 		  */
	McpU8 	uMeasureResponseTime; 	/* 2^n */
	McpU8 	uUseMultipleSets; 		/* multiple sets are allowed (0), oneSet (1)  */
	McpU8 	uEnvironmentCharacter; 	/* badArea (0), notBadArea (1), mixedArea (2) */
	McpS32 	lLongtitue;
	McpS32 	lLatitude;
	McpS16 	xHeight;
	McpU32 	qUncertainty;
	McpU32 	uSvNumInResp;

	/* ini configuration */
	McpBool bPosOffsetDefined;
	McpBool bPosUncertaintyDefined;
	float	fltPosOffset;
	float	fltPosUncertainty;
	McpU32  uSessionId;

	/* Time estimation */
	TNAVCD_GpsTime 	tGpsTime;
	TRrlp_acquisitionAssist tAcquisitionAssist;

	/* Real time integrity, bit map of bad satellites */
	/* bit 0: not used, bit 1: SVId 1 not healthy, bit N: SVId N not healthy */
	McpU32	uRealTimeIntegrity[2];

	//RRLP-REL5 Start
	McpU8	uRel5ExtInd;       /* Added for RRLP REL5 EXT.*/
	McpU8	smlccode;          /* 0 -63 to indicate SMLC ID*/
	McpU32	transactionID;     /*  0 ... 262143 to indicate transaction ID */
	//RRLP-REL5 END

	TRrlp_gsmTimeAssist tGsmTimeAssist;

} TRrlp;

/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

static void methodType_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void gpsTime_decode (TRrlp 	*pRrlp,
                            McpU8	* MsgBuf,
                            McpU32 	*pOffset,
							T_GPSC_assistance_inject *pInjectAssist);

static void gsmTime_decode (TRrlp 	*pRrlp,
							McpU8	*MsgBuf,
							McpU32 	*pOffset,
							TRrlp_gsmTimeAssist *pGsmTimeAssist);

static void gpsTowAssist_decodeAndInject (TRrlp  *pRrlp,
										  McpU8  *MsgBuf,
										  McpU32 *pOffset);

static void referenceTime_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void refLocation_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void dgpsCorrections_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void navigationModel_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void ionosphericModel_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void utcModel_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void almanac_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void acquisAssist_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void seqOfBadSatelliteSet_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void controlHeader_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void gpsAssistData_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void posInstruct_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void protocolError_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void assistData_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static void msrPosition_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU32 *pOffset);

static McpU16 rrlp_decode (TRrlp *pRrlp, McpU8* MsgBuf, McpU16 MsgLength);

static EMcpfRes processRrlpMsg( TRrlp *pRrlp, McpU8* pMsgBuf, McpU16 uMsgLength);

static EMcpfRes checkAssistDataForLocFix (TRrlp *pRrlp);

static void sendLocFixReq(TRrlp *pRrlp);

static void sendAssistanceToNavc (TRrlp *pRrlp, TNAVC_cmdParams *pNavcCmd);

static void sendLocFixStop (TRrlp *pRrlp);

static void injectGpsTimeToNavc (TRrlp *pRrlp, TNAVCD_GpsTime *pGpsTime);

static void rrlpDataReset (TRrlp *pRrlp);

static void rrlp_loc_info_encode (TRrlp 		*pRrlp,
								  LocInfo       *p_zLocInfo,
								  McpU8         *pMsgBuf,
								  McpU32   	    *pBitOffset);

static void gps_msr_info_construct (TRrlp 	   	*pRrlp,
									GpsMsrInfo 	*p_zGpsMsrInfo,
								    T_GPSC_loc_fix_response *loc_fix_response);

static void rrlp_gps_msr_encode (TRrlp 			*pRrlp,
							     GpsMsrInfo    	*p_zGpsMsrInfo,
								 McpU32         q_GoodSvs,
								 McpU8         	*pMsgBuf,
								 McpU32   	    *pBitOffset);

static EMcpfRes ext_geo_loc_construct (LocInfo					*p_zLocInfo,
									  T_GPSC_loc_fix_response	*loc_fix_response,
									  McpU8             		u_PosShape);

static void locationErrorEncode (TRrlp			*pRrlp,
								 LocErr			*pLocErr,
								 McpU8 			*pMsgBuf,
								 McpU32 		*pBitOffset);

static void sendRrlpResponse (TRrlp						*pRrlp,
							  T_GPSC_loc_fix_response	*pLocFixResponse,
							  TNAVCD_LocError 			*pLocError,
							  ERrlp_MeasrType			eMeasType,
							  ERrlp_LocReason			eLocReason);

static void sendRrlpMsg (TRrlp	*pRrlp, McpU8 *pBuf, McpU32	uBufLen);

static void loc_error_construct (TNAVCD_LocError 	*pLocError,
								 ERrlp_LocReason 	eCause,
								 LocErr 			*pRrlpLocErr);

#ifdef MOBILE_ID_EXTENSION_CONTAINER
static void extensionContainerEncode (TRrlp		*pRrlp,
									  PrivExt	*p_zMobileIdExt,
									  McpU8 	*pMsgBuf,
									  McpU32 	*pBitOffset);

static McpU8 *RrlpImsiPacker (McpU8 *p_Imsi, McpU8 *p_B);
#endif

#ifdef RRLP_DEBUG
static void dumpMsg (TRrlp *pRrlp, McpU8 *pMsgBuf, McpU16 uMsgLength);
#endif

//RRLP-REL5 Start
static void rrlp_rel5_ext_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset);
//RRLP-REL5 End


/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/

/**
 * \fn     RRLP_Init
 * \brief  Initialize NAVC RRLP module object
 *
 */
handle_t RRLP_Init (handle_t 	hMcpf,
					handle_t 	hCmdPool,
					handle_t 	hEvtPool,
					EmcpTaskId  eSrcTaskId,
					McpU8		uSrcQId)
{
	TRrlp *pRrlp = (TRrlp *)mcpf_mem_alloc(hMcpf, sizeof(TRrlp));

	if (!pRrlp)
	{
		return NULL;
	}

	mcpf_mem_zero (hMcpf, pRrlp, sizeof(TRrlp));

	pRrlp->hMcpf 		= hMcpf;
	pRrlp->hCmdPool 	= hCmdPool;
	pRrlp->hEvtPool 	= hEvtPool;
	pRrlp->eSrcTaskId 	= eSrcTaskId;
	pRrlp->uSrcQId    	= uSrcQId;

	/* Set mandatory bits for both modes: MS-Based and MS-Assisted */
	pRrlp->uWishlist = GPSC_REQ_AA | GPSC_REQ_LOC | GPSC_REQ_TIME | GPSC_REQ_NAV;

	return  pRrlp;
}

/**
 * \fn     RRLP_Destroy
 * \brief  Destroy NAVC RRLP module object
 *
 */
void RRLP_Destroy(handle_t hRrlp)
{
	TRrlp *pRrlp = (TRrlp *) hRrlp;

	mcpf_mem_free (pRrlp->hMcpf, hRrlp);
}

/**
 * \fn     RRLP_StopLocationFixRequest
 * \brief  Stop the Ongoing Location Request
 *
 */
void RRLP_StopLocationFixRequest(handle_t hRrlp)
{
	TRrlp *pRrlp = (TRrlp *) hRrlp;
	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
								("RRC_StopLocationFixRequest: Stop Session Id=%d\n", pRrlp->uSessionId));

	sendLocFixStop (pRrlp);
}


/**
 * \fn     RRLP_processRxMsg
 * \brief  Process incoming RRLP message
 *
 */
EMcpfRes RRLP_processRxMsg (handle_t 	hRrlp,
							McpU8 		*pMsg,
							McpU16		uLen,
							McpU32 		uSystemTimeInMsec)
{
	TRrlp 			*pRrlp = hRrlp;
	EMcpfRes 		eRes;

	/* Sent Ack Response for the RRLP Data */



	/* Copy system time from received RRLP message stamped by ADS (or other used
	 * interface) on receive. The system time is used for reference time and
	 * acquisition assistance.
	 */

	pRrlp->uSystemTimeMs = uSystemTimeInMsec;

	/* This function decodes the incoming RRLP data and its components
	 * and stores in the local and global data structure.
	 */

	eRes = processRrlpMsg (pRrlp, pMsg, uLen);

	return eRes;
}


/**
 * \fn     RRLP_encodeResponse
 * \brief  Encode RRLP response message
 *
 */
McpU16 RRLP_encodeResponse (handle_t				hRrlp,
						  T_GPSC_loc_fix_response	*p_zLocFixResponse,
						  TNAVCD_LocError 			*pLocError,
						  ERrlp_MeasrType			eMeasType,
						  ERrlp_LocReason			eLocReason,
						  McpU8 					*pMsgBuf)
{
	TRrlp				*pRrlp = (TRrlp *) hRrlp;
	McpU32		 		uMsgBitOffset = 0;
	McpU16 				uMsgLen;
	RrlpPdu 			tRrlpPdu; 		/* structure to hold the entire message for RRLP encoding */
	RrlpPdu    			*p_zRrlpPdu    = &tRrlpPdu;
	RrlpComp   			*p_zRrlpComp   = &p_zRrlpPdu->z_RrlpComp;
	MsrPosResp 			*p_zMsrPosResp = &p_zRrlpComp->z_SeqOfChoice.z_MsrPosResp;
	GpsMsrInfo 			*p_zGpsMsrInfo = &p_zMsrPosResp->z_GpsMsrInfo;
	LocInfo    			*p_zLocInfo    = &p_zMsrPosResp->z_LocInfo;
	LocErr     			*p_zLocErr     = &p_zMsrPosResp->z_LocErr;
#ifdef MOBILE_ID_EXTENSION_CONTAINER
	PrivExt    			*p_zMobileIdExt= &p_zMsrPosResp->z_MobileIdExt;
#endif

/* RRLP-REL5 Start */
	MsrPosRsp_Rel5Ext        *p_zMsrPosRsp_Rel5Ext = &p_zMsrPosResp->z_MsrPosRsp_Rel5Ext;
/* RRLP-REL5 End */



	p_zRrlpPdu->u_RefNum_R3 = pRrlp->uRefNum;
	p_zRrlpComp->u_ExtIndPream_R1 = 0; /* no extension */

	p_zRrlpComp->u_ChoiceTag_R3 = (McpU8)C_RRLP_CHOICE_MSRPOSRESP;

	LOGD("\n REL5 - Inside RRLP_encodeResponse ");

	/* If decoder supports RRLP REL5 ext support this bit should be one */
	/* currenly no support for extension need to debug this issue */
	p_zMsrPosResp->u_ExtIndPream_R1 = 0; /*pRrlp->uRel5ExtInd *//* no extension*/
    LOGD(" REL5-8 - from RRLP_encode  p_zMsrPosResp->u_ExtIndPream_R1 is %x ",  p_zMsrPosResp->u_ExtIndPream_R1);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("RRLP_encodeResponse: start measType=%u reason=%u\n",
							 eMeasType, eLocReason));

	/*
	 * Though RRLP Measurement Position Response specification standard does
	 * not preclude the inclusion of more than one optional parameters, our
	 * implementation will include only one in an RRLP message
	 * (either location, Gps measurement, or LocError code)
	 */
	switch (eMeasType)
	{
	case C_RRLP_REP_POS:
	case C_RRLP_REP_MEAS:

		/* Analyze response result */
		switch(p_zLocFixResponse->ctrl_loc_fix_response_union)
		{
		case GPSC_RESULT_PROT_POSITION:

			switch (p_zLocFixResponse->loc_fix_response_union.prot_position.prot_fix_result)
			{
			case GPSC_PROT_FIXRESULT_2D:
			{
				p_zMsrPosResp->u_OptPream_R7 = C_POSRESP_OPT_PREAM_LOC;
				ext_geo_loc_construct (p_zLocInfo,
									   p_zLocFixResponse,
									   C_POS_SHAPE_ELLIP_UNC_ELLIP);
			    MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									   ("RRLP_encodeResponse: constructing 2D with shape C_POS_SHAPE_ELLIP_UNC_ELLIP\n"));

			}
			   break;
			case GPSC_PROT_FIXRESULT_3D:

				p_zMsrPosResp->u_OptPream_R7 = C_POSRESP_OPT_PREAM_LOC;
				ext_geo_loc_construct (p_zLocInfo,
									   p_zLocFixResponse,
									   C_POS_SHAPE_ELLIP_ALT_UNC_ELLIP);
				MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									   ("RRLP_encodeResponse: constructing 3D with shape C_POS_SHAPE_ELLIP_ALT_UNC_ELLIP\n"));

				break;

			case GPSC_PROT_FIXRESULT_NOFIX:

				/* Prepare RRLP location error response */
				p_zMsrPosResp->u_OptPream_R7 = C_POSRESP_OPT_PREAM_LOCERR;
				p_zLocErr->u_ExtInd_R1 			= 0;
				p_zLocErr->u_OptParam_R1 		= 1; 	/* currently no support for additional AssistanceData */
				p_zLocErr->u_ExtIndLocReason_R1 = 0; 	/* LocationReason no extension */
				p_zLocErr->u_LocationReason_R4  = (McpU8) C_RRLP_LOC_REASON_LOC_REQ_US;
				p_zLocErr->u_OptParam_R1 		= 0;

				break;

			default:
				MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG,
								  ("RRLP_encodeResponse unexpected prot result=%u!\n",
								   p_zLocFixResponse->loc_fix_response_union.prot_position.prot_fix_result));
			break;
			}
			break;

		case GPSC_RESULT_PROT_MEASUREMENT:

			if(p_zLocFixResponse->loc_fix_response_union.prot_measurement.c_sv_measurement != 0)
			{
				p_zMsrPosResp->u_OptPream_R7 = C_POSRESP_OPT_PREAM_MEAS;
				gps_msr_info_construct ( pRrlp, p_zGpsMsrInfo, p_zLocFixResponse );
			}
			else
			{
				/* Prepare RRLP location error response */
				p_zMsrPosResp->u_OptPream_R7 = C_POSRESP_OPT_PREAM_LOCERR;
				p_zLocErr->u_ExtInd_R1 			= 0;
				p_zLocErr->u_OptParam_R1 		= 1; 	/* currently no support for additional AssistanceData */
				p_zLocErr->u_ExtIndLocReason_R1 = 0; 	/* LocationReason no extension */
				p_zLocErr->u_LocationReason_R4  = (McpU8) C_RRLP_LOC_REASON_LOC_REQ_US;
				p_zLocErr->u_OptParam_R1 		= 0;
			}
			break;

		default:
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG,
							  ("RRLP_encodeResponse unexpected prot response=%u!\n",
							   p_zLocFixResponse->ctrl_loc_fix_response_union));
			break;
		}
		break;

	case C_RRLP_REP_LOCERR:

		p_zMsrPosResp->u_OptPream_R7 = C_POSRESP_OPT_PREAM_LOCERR;
		loc_error_construct (pLocError, eLocReason, p_zLocErr);
		break;

	default:
		MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("RRLP_encodeResponse uknown measType=%d!\n", eMeasType));

		return 0;	/* return zero length in the case of error */
		break;

	}

#ifdef MOBILE_ID_EXTENSION_CONTAINER
	/* Set Option BitFlag for Mobile Id Extension Container */
	p_zMsrPosResp->u_OptPream_R7 |= C_POSRESP_OPT_EXT_MOBILE_ID;
#endif


//RRLP-REL5 Start
/* currenly no support for extension need to debug this issue */
#if 0
	if( pRrlp->uRel5ExtInd )
	{
		p_zMsrPosRsp_Rel5Ext->u_Rel5MsrRspExt1_R7 = 1;
		p_zMsrPosRsp_Rel5Ext->u_OptParam_R2 = C_POSRESP_OPT_EXT_REL5;
		p_zMsrPosRsp_Rel5Ext->u_Rel5MsrRspExt2_R8 = 4;
		p_zMsrPosRsp_Rel5Ext->u_Rel5_Marker_R1 = 0;
		p_zMsrPosRsp_Rel5Ext->u_OptParam_R3 = 4;
		p_zMsrPosRsp_Rel5Ext->z_ExtRef.u_smlccode_R6 = pRrlp->smlccode;
		p_zMsrPosRsp_Rel5Ext->z_ExtRef.u_transactionID_R18 = pRrlp->transactionID;

	}

//RRLP-REL5 End
#endif
	/* constructing RRLP message */
	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zRrlpPdu->u_RefNum_R3, 3);

	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zRrlpComp->u_ExtIndPream_R1, 1);    /* no ext. */

	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zRrlpComp->u_ChoiceTag_R3, 3);      /* choice msrPosResp  */

	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zMsrPosResp->u_ExtIndPream_R1, 1);  /* msrPosResp no ext. */

	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zMsrPosResp->u_OptPream_R7, 7);

	/* go throught the 7 bits of MsrPositionResponse Sequence Optional */

    if (p_zMsrPosResp->u_OptPream_R7 & C_POSRESP_OPT_PREAM_MULTIPLE_SETS)
	{
		/* multiple sets: unsupported */
	}

	if (p_zMsrPosResp->u_OptPream_R7 & C_POSRESP_OPT_PREAM_REFERENCE_IDENTITY)
	{
		/* reference identity: unsupported */
	}

	if (p_zMsrPosResp->u_OptPream_R7 & C_POSRESP_OPT_PREAM_OTD_MEASUREINFO)
	{
		/* otd-MeasureInfo: unsupported */
	}

	if (p_zMsrPosResp->u_OptPream_R7 & C_POSRESP_OPT_PREAM_LOC)
	{
		rrlp_loc_info_encode (pRrlp, p_zLocInfo, pMsgBuf, &uMsgBitOffset);
	}

	if (p_zMsrPosResp->u_OptPream_R7 & C_POSRESP_OPT_PREAM_MEAS)
	{
		rrlp_gps_msr_encode(pRrlp,
							p_zGpsMsrInfo,
							(McpU32)p_zLocFixResponse->loc_fix_response_union.prot_measurement.c_sv_measurement,
							pMsgBuf,
							&uMsgBitOffset);
	}

	if (p_zMsrPosResp->u_OptPream_R7 & C_POSRESP_OPT_PREAM_LOCERR)
	{
		locationErrorEncode (pRrlp, p_zLocErr, pMsgBuf, &uMsgBitOffset);
	}

#ifdef MOBILE_ID_EXTENSION_CONTAINER
	if (p_zMsrPosResp->u_OptPream_R7 & C_POSRESP_OPT_EXT_MOBILE_ID)
	{
		extensionContainerEncode (pRrlp, p_zMobileIdExt, pMsgBuf, &uMsgBitOffset);
	}
#endif

//RRLP-REL5 Start
    /* RRLP REL5 Ext*/
    if(pRrlp->uRel5ExtInd)
    	{
           	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zMsrPosRsp_Rel5Ext->u_Rel5MsrRspExt1_R7 , 7);
		   	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zMsrPosRsp_Rel5Ext->u_OptParam_R2, 2);
		   	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zMsrPosRsp_Rel5Ext->u_Rel5MsrRspExt2_R8, 8);
		   	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zMsrPosRsp_Rel5Ext->u_Rel5_Marker_R1, 1);
		   	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zMsrPosRsp_Rel5Ext->u_OptParam_R3, 3);
		   	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zMsrPosRsp_Rel5Ext->z_ExtRef.u_smlccode_R6, 6);
		   	mcpf_putBits (pMsgBuf, &uMsgBitOffset, p_zMsrPosRsp_Rel5Ext->z_ExtRef.u_transactionID_R18, 18);


    	}

  //RRLP-REL5 End


	/* add padding zero bits */


	if (uMsgBitOffset % 8)
	{
		mcpf_putBits (pMsgBuf, &uMsgBitOffset, 0, (8 - (uMsgBitOffset % 8)));
	}

	uMsgLen = (McpU16) (uMsgBitOffset / 8);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("RRLP_encodeResponse: end msgLen=%u\n", uMsgLen));

	return uMsgLen;
}


/************************************************************************
 *
 *   Private functions implementation
 *
 ************************************************************************/

/**
 * \fn     sendRrlpResponse
 * \brief  Send RRLP encoded response
 *
 * Allocate buffer for outgoing event message, encode RRLP response message and send it
 * to destination task (requested service)
 *
 * \note
 * \param	pRrlp - pointer to RRLP object
 * \param  	pLocFixResponse - pointer to GPSC locaton response structure or NULL if not provided
 * \param  	pLocError  - measurement type
 * \param  	eLocReason - location error reason
 * \return  void
 * \sa
 */
static void sendRrlpResponse (TRrlp						*pRrlp,
							  T_GPSC_loc_fix_response	*pLocFixResponse,
							  TNAVCD_LocError 			*pLocError,
							  ERrlp_MeasrType			eMeasType,
							  ERrlp_LocReason			eLocReason)
{
	NavlMsg_t	*pMsg;
    TNAVC_evt   *pEvt = NULL;
	McpU32		uOutLen;
	EMcpfRes	eRes;

    pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);
    MCPF_REPORT_ERROR(pRrlp->hMcpf, NAVC_MODULE_LOG, ("Inside sendRrlpResponse\n"));

    if (pEvt)
	{
		/* Encode and send RRLP Response message */
		uOutLen = RRLP_encodeResponse (pRrlp,
									   pLocFixResponse,
									   pLocError,
									   eMeasType,
									   eLocReason,
                                       pEvt->tParams.tRrFrame.uRRpayload);
        pEvt->tParams.tRrFrame.uRrFrameLength = uOutLen;
        pMsg = (NavlMsg_t *) pEvt;

		//mcpf_mem_copy (pRrlp->hMcpf, pMsg->Payload, pEvt->tParams.tRrFrame.uRRpayload, uOutLen);

		eRes = mcpf_SendMsg (pRrlp->hMcpf,
							 pRrlp->eSrcTaskId,
							 pRrlp->uSrcQId,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 NAVC_EVT_RRLP_RESP,
							 uOutLen,
							 0, 				/* user defined */
							 pMsg);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pRrlp->hMcpf, pMsg);
            MCPF_REPORT_ERROR(pRrlp->hMcpf, NAVC_MODULE_LOG, ("sendRrlpResponse: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pRrlp->hMcpf, NAVC_MODULE_LOG, ("sendRrlpResponse: mcpf_mem_alloc_from_pool failed\n"));
	}
}


/**
 * \fn     sendRrlpMsg
 * \brief  Send RRLP raw message
 *
 * Allocate buffer for outgoing event message, copy input buffer
 * pointed by pBuf as RRLP data and send message to destination task
 * (requested service)
 *
 * \note
 * \param	pRrlp - pointer to RRLP object
 * \param  	pBuf  - pointer to source buffer to send as RRLP data
 * \param  	uBufLen - buffer size
 * \return  void
 * \sa
 */
static void sendRrlpMsg (TRrlp	*pRrlp, McpU8 *pBuf, McpU32	uBufLen)
{
	NavlMsg_t	*pMsg;
    TNAVC_evt   *pEvt = NULL;
	EMcpfRes	eRes;

    pEvt = (TNAVC_evt *) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);

	MCPF_REPORT_ERROR(pRrlp->hMcpf, NAVC_MODULE_LOG, ("Inside sendRrlpMsg\n"));
    if (pEvt)
	{
		/* Encode and send RRLP Response message */

        mcpf_mem_copy (pRrlp->hMcpf, pEvt->tParams.tRrFrame.uRRpayload, pBuf, uBufLen);
        pEvt->tParams.tRrFrame.uRrFrameLength = uBufLen;
        pMsg = (NavlMsg_t *) pEvt;
		//mcpf_mem_copy (pRrlp->hMcpf, pMsg->Payload, pBuf, uBufLen);

		eRes = mcpf_SendMsg (pRrlp->hMcpf,
							 pRrlp->eSrcTaskId,
							 pRrlp->uSrcQId,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID,
							 NAVC_EVT_RRLP_ACK,
							 uBufLen,
							 0, 				/* user defined */
							 pMsg);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pRrlp->hMcpf, pMsg);
            MCPF_REPORT_ERROR(pRrlp->hMcpf, NAVC_MODULE_LOG, ("sendRrlpMsg: mcpf_SendMsg failed\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pRrlp->hMcpf, NAVC_MODULE_LOG, ("sendRrlpMsg: mcpf_mem_alloc_from_pool failed\n"));
	}
}


/**
 * \fn     methodType_decode
 * \brief  Decode RRLP message type
 *
 * Decode RRLP message type from received RRLP message pointed by pMsgBuf and starting from bit offset
 * pOffset
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void methodType_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8	uChoiceTag, uOptPream=0;

	uChoiceTag = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);

	pRrlp->uMethodType = uChoiceTag;

	if ( uChoiceTag == 0 ) /* msAssisted */
	{
		uOptPream = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 1);
	}
	if(uChoiceTag || uOptPream)
	{
		pRrlp->uAccuracy = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 7);
		pRrlp->uAccuracy = (McpU8)(10*(pow(1.1,pRrlp->uAccuracy)-1));  /* r= 10*(pow(1.1,k)-1)*/
	}
	else
		pRrlp->uAccuracy = NAVCD_ACCURACY_UNKNOWN ;

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("methodType_decode: uMethType=%u opts=%x acuracy=%u ofs=%u\n",
							 pRrlp->uMethodType, uOptPream, pRrlp->uAccuracy, *pOffset));

	/* Set which list bit flags for required assistance data */
	switch (pRrlp->uMethodType)
	{
	case ENAVC_RRLP_POS_METHOD_TYPE_MS_ASSISTED:
		pRrlp->uWishlist = GPSC_REQ_AA | GPSC_REQ_TIME;
		break;

	case ENAVC_RRLP_POS_METHOD_TYPE_MS_BASED:
		pRrlp->uWishlist = GPSC_REQ_LOC| GPSC_REQ_TIME| GPSC_REQ_NAV;
		break;

	case ENAVC_RRLP_POS_METHOD_TYPE_MS_BASED_PREF:
		pRrlp->uWishlist = GPSC_REQ_LOC| GPSC_REQ_TIME| GPSC_REQ_NAV;
		break;

	case ENAVC_RRLP_POS_METHOD_TYPE_MS_ASSISTED_PREF:
		pRrlp->uWishlist = GPSC_REQ_AA | GPSC_REQ_TIME;
		break;

	default:
		MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("methodType_decode unknown methodType=%u !\n", pRrlp->uMethodType));
		break;
	}

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("methodType_decode: end WishList=%x ofs=%u\n", pRrlp->uWishlist, *pOffset));

}


/**
 * \fn     gpsTime_decode
 * \brief  Decode RRLP GPS time field
 *
 * Decode GPS time field from received RRLP message pointed by pMsgBuf and starting from bit offset
 * pOffset
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \param  	pInjectAssist - pointer to assistance structure to store decoded values
 * \return  void
 * \sa
 */
static void gpsTime_decode (TRrlp *pRrlp,
                            McpU8* pMsgBuf,
                            McpU32 *pOffset,
							T_GPSC_assistance_inject *pInjectAssist)
{
	McpU32 q_GpsTow23b;
	McpU16 w_GpsWeek; /* note RRLP gives only the 10-bit version */

	/* 23 bits Gps Time of Week */
	q_GpsTow23b = (McpU32)mcpf_getBits(pMsgBuf, pOffset, 23);

	q_GpsTow23b *= 80;
	pInjectAssist->assistance_inject_union.time_assist.gps_msec = q_GpsTow23b ;// + q_TAssisUncOneSided;

	/* 10-bit Gps Week */
	w_GpsWeek = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 10);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("gpsTime_decode: GpsTow23b*80=%u GpsWeek10b=%u ofs=%u\n",
							 q_GpsTow23b, w_GpsWeek, *pOffset));

	/* Offset the time by the one-sided latency value so that the search
	window is properly centered. */
	pInjectAssist->assistance_inject_union.time_assist.gps_week = (McpU16)(w_GpsWeek+1024);

	/* Set the one-sided time uncertainty. */
	/* Get the two-sided time uncertainty from the latency value in the
	config and make it one-sided for the sensor. */
	pInjectAssist->assistance_inject_union.time_assist.time_unc = 2000000;	/* usec */
	pInjectAssist->assistance_inject_union.time_assist.sub_ms   = 0;
	pInjectAssist->assistance_inject_union.time_assist.time_accuracy = GPSC_TIME_ACCURACY_COARSE;

	/* signalling the receival of RefTime */
	pRrlp->uAvailabilityFlags |= GPSC_REQ_TIME;

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("gpsTime_decode: end ofs=%u\n", *pOffset));
}

/**
 * \fn     gsmTime_decode
 * \brief  Decode RRLP GSM time field
 *
 * Decode GSM time field from received RRLP message pointed by pMsgBuf and starting from bit offset
 * pOffset
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \param  	pGsmTimeAssist - pointer to assistance structure to store decoded values
 * \return  void
 * \sa
 */
static void gsmTime_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset,
							TRrlp_gsmTimeAssist *pGsmTimeAssist)
{

	pGsmTimeAssist->bcchCarrier = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 10);
	pGsmTimeAssist->bsic 		= (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);
	pGsmTimeAssist->frameNumber = mcpf_getBits(pMsgBuf, pOffset, 21);
	pGsmTimeAssist->timeSlot 	= (McpU8)mcpf_getBits(pMsgBuf, pOffset, 3);
	pGsmTimeAssist->bitNumber 	= (McpU8)mcpf_getBits(pMsgBuf, pOffset, 8);
	pGsmTimeAssist->bValidGsmTime = MCP_TRUE;
	pRrlp->uAvailabilityFlags 	|= C_REF_GSMTIME_AVAIL;

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("gsmTime_decode: bcCr=%u bsic=%u fN=%u ts=%u bitN=%u ofs=%u\n",
							 pGsmTimeAssist->bcchCarrier,
							 pGsmTimeAssist->bsic,
							 pGsmTimeAssist->frameNumber,
							 pGsmTimeAssist->timeSlot,
							 pGsmTimeAssist->bitNumber,
							 *pOffset));
}

/**
 * \fn     gpsTowAssist_decodeAndInject
 * \brief  Decode RRLP time of week and jnect the data to GPS receiver
 *
 * Decode RRLP Time Of Week (TOW) field and inject TOW to GPS chip
 * pOffset
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void gpsTowAssist_decodeAndInject (TRrlp  *pRrlp,
										  McpU8  *pMsgBuf,
										  McpU32 *pOffset)
{
	McpU8 uIdx, uGpsTowAssisLength;

#if 0
	/* length: num of SVs */
	uGpsTowAssisLength = (McpU8)(mcpf_getBits (pMsgBuf, pOffset, 4) + 1);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("gpsTowAssist: length=%u ofs=%u\n", uGpsTowAssisLength, *pOffset));

	for (uIdx=0; uIdx < uGpsTowAssisLength; uIdx++)
	{
		TNAVC_cmdParams   *pNavcCmd;
		T_GPSC_tow_assist *pGpscTowAssist;

		pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);

		if (pNavcCmd)
		{
			pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_TOW;
			pGpscTowAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.tow_assist;

			pGpscTowAssist->svid 				   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 6);
			pGpscTowAssist->towa_tlm_word 		   = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 14);
			pGpscTowAssist->towa_anti_spoof_flag   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);
			pGpscTowAssist->towa_alert_flag 	   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);
			pGpscTowAssist->towa_tlm_reserved_bits = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("gpsTowAssist: i=%u svid=%u tlm=%u spoof=%u alrt=%u rsrv=%u ofs=%u\n", uIdx,
									 pGpscTowAssist->svid,
									 pGpscTowAssist->towa_tlm_word,
									 pGpscTowAssist->towa_anti_spoof_flag,
									 pGpscTowAssist->towa_alert_flag,
									 pGpscTowAssist->towa_tlm_reserved_bits,
									 *pOffset));

			sendAssistanceToNavc (pRrlp, pNavcCmd);

		}
		else
		{
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("gpsTowAssist_decodeAndInject: allocation failed!\n"));
		}
	}
	pRrlp->uAvailabilityFlags |= C_REF_TOW_AVAIL;
#endif
		McpU8 nSV; /* num. of navModelElement */
		McpU8 i, j=0;
		TNAVC_cmdParams   *pNavcCmd = NULL;
		T_GPSC_tow_assist *pGpscTowAssist;
		nSV = (McpU8)(1+mcpf_getBits(pMsgBuf, pOffset, 4));
		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
								("gpsTowAssist_decodeAndInject: nSV=%u ofs=%u", nSV, *pOffset));
		if (nSV != 0)
		{
			pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);
			if(NULL == pNavcCmd)
			{
				MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("gpsTowAssist: MEM allocation failed!\n"));
				MCPF_Assert(1);
				return;
			}
			mcpf_mem_set(pRrlp->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
			j = 0;
			for (i=0; i<nSV; i++,j++) /* list length = num of SVs */
			{
				McpU32 u32;
				McpU8  uSvId = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);
				if (pNavcCmd)
				{
					pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_TOW;
					pGpscTowAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.tow_assist[j];
					pGpscTowAssist->svid				   = uSvId;
					pGpscTowAssist->towa_tlm_word		   = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 14);
					pGpscTowAssist->towa_anti_spoof_flag   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);
					pGpscTowAssist->towa_alert_flag 	   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);
					pGpscTowAssist->towa_tlm_reserved_bits = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);
				}
				else
				{
					MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("gpsTowAssist_decodeAndInject: allocation failed!"));
				}
				if((i+1) % N_SV_15 == 0)
				{
					if(pNavcCmd)
					{

						/* Inject ephemeris assistance data into GPSC */
						sendAssistanceToNavc (pRrlp, pNavcCmd);
					}
					pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);
					if(NULL == pNavcCmd)
					{
						MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("gpsTowAssist: MEM allocation failed!\n"));
						MCPF_Assert(1);
						return;
					}
					mcpf_mem_set(pRrlp->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
					j = 0;
				}


			} /* end bracket of Sv list for-loop */


			if(i % N_SV_15 != 0) /* There are still some more parsed packets yet to be sent to NAVC. Send them now */
			{
				if(pNavcCmd)
				{
					/* Inject ephemeris assistance data into GPSC */
					sendAssistanceToNavc (pRrlp, pNavcCmd);
				}
			}

		}



		pRrlp->uAvailabilityFlags |= C_REF_TOW_AVAIL;		  /* signalling the receival of TOW Assist */

}

/**
 * \fn     referenceTime_decode
 * \brief  Decode RRLP reference time field and jnect the data to GPS receiver
 *
 * Decode RRLP reference time field including GPS time and optional GSM and Time Of Week fields
 * and inject data to GPS chip
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void referenceTime_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU32	        uOptPream;
	TNAVC_cmdParams *pNavcCmd;
    T_GPSC_assistance_inject *pAssist;


	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);

	if (pNavcCmd)
	{
		pAssist = &pNavcCmd->tInjectAssist.tAssistance;

		pAssist->ctrl_assistance_inject_union = GPSC_ASSIST_TIME;

		/* Opt. Param. Preamble: xy - x GSM Time Seq; y - GpsTowAssis Seq */
		uOptPream = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
								("referenceTime_decode: opts=%x ofs=%u\n", uOptPream, *pOffset));

		/*************** retrieve the mandatory GPS time ******************/
		gpsTime_decode(pRrlp, pMsgBuf, pOffset, pAssist);

		/* Store time refernce (TOW, ms and system msec) in NAVC object
		 * to retrieve on GPS TOW request
		 */

		pRrlp->tGpsTime.tTime = pAssist->assistance_inject_union.time_assist;
		pRrlp->tGpsTime.uSystemTimeMs = pRrlp->uSystemTimeMs;

		/* Synchronize GPS time in NAVC object */
		injectGpsTimeToNavc (pRrlp, &pRrlp->tGpsTime);

		/*************** retrieve the optional GSM time ******************/
		if (uOptPream & 0x2)
		{
			pRrlp->tGsmTimeAssist.bValidGsmTime = MCP_TRUE;
			gsmTime_decode(pRrlp, pMsgBuf, pOffset, &pRrlp->tGsmTimeAssist);
		}
		else
		{
			pRrlp->tGsmTimeAssist.bValidGsmTime = MCP_FALSE;
		}

		sendAssistanceToNavc (pRrlp, pNavcCmd);

		/* retrieve the optional GpsTowAssistance */
		if (uOptPream & 0x1)
		{
			gpsTowAssist_decodeAndInject(pRrlp, pMsgBuf, pOffset);
		}

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
								("referenceTime_decode: end ofs=%u\n", *pOffset));
	}
	else
	{
		MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("referenceTime_decode: allocation failed!\n"));
	}
}

/**
 * \fn     referenceTime_decode
 * \brief  Decode RRLP reference time field and jnect the data to GPS receiver
 *
 * Decode RRLP reference time field including GPS time and optional GSM and Time Of Week fields
 * and inject data to GPS chip
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void refLocation_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpDBL      f_Uncertain;
	McpU8       u_RefLocLength; /* length of the reference location sequence */
	McpU8       u_RefLocShape;  /* shape of the reference location */
	McpU8		uUncertainSemiMajor, uUncertainSemiMinor;
	McpU8		uConfidence, uUncAlt;
	McpU16		uOrientationMajorAxis;
	double      d_Lat, d_Long;
	TNAVC_cmdParams  *pNavcCmd;
	T_GPSC_position_assist *pAssist;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_POSITION;
		pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.position_assist;

		u_RefLocLength = (McpU8)(1+mcpf_getBits(pMsgBuf, pOffset, 5)); 	/* Length of ext-geograph. octet string  */
		u_RefLocShape = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 8); 		/* shape  */

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
								("refLocation_decode: RefLocLength=%u RefLocShape=%u ofs=%u\n",
								 u_RefLocLength, u_RefLocShape, *pOffset));

		/* Shape: ellipsoid point with altitude: See GSM03.32
		Other shapes not supported currently, but eventually at least we
		need to skip the bits gracefully for compatibility purposes */

		if ( ( u_RefLocShape >> 4  ) == 0x08 || ( u_RefLocShape >> 4  ) == 0x09 )
		{
			McpS16 s16;
			McpS32 s32;

			s32 = mcpf_getSignedBits(pMsgBuf, pOffset, 24);

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("refLocation_decode: raw latitude=%d ofs=%u\n", s32, *pOffset));

			/* convert latitude to degrees */
			if ( s32 < 0 )
			{
				d_Lat = (McpS32)( 0 - (McpS32)(s32 & 0x007FFFFF) )
					* C_90_OVER_2_23;
				pAssist->latitude_sign = 1;
				pAssist->latitude = (McpU32)(s32 & 0x007FFFFF);
			}
			else
			{
				d_Lat = s32 * C_90_OVER_2_23;
				pAssist->latitude_sign = 0;
				pAssist->latitude = (McpU32)s32;
			}

				/* do we need to move this to gpsc_app_api.c/gpsc_populate_pos_assist() */
				/* If defined, perturb the latitude by the user defined offset (in Km). */
			if (pRrlp->bPosOffsetDefined)
			{
				MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG, ("*** Lat before bias: %f\n", d_Lat));
				d_Lat += (C_RAD_TO_DEG * (pRrlp->fltPosOffset / C_EARTH_RADIUS));
				MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG, ("*** Lat  after bias: %f\n", d_Lat));
			}

			/* convert latitude from degree to radius and apply AI2 spec. */
			pRrlp->lLatitude = (McpS32) (d_Lat * C_DEG_TO_RAD * C_LSB_LAT_REP);


			/* retrieve longitude */
			s32 = mcpf_getSignedBits(pMsgBuf, pOffset, 24);

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("refLocation_decode: raw longitude=%d ofs=%u\n", s32, *pOffset));

			/* convert longitude to degrees */
			d_Long = s32 * C_360_OVER_2_24;
			pAssist->longitude = s32;

				/* do we need to move this to gpsc_app_api.c/gpsc_populate_pos_assist() */
				/* If defined, perturb the longitude by the user defined offset (in Km) */
			if (pRrlp->bPosOffsetDefined)
			{
				MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG, ("*** Long before bias: %f\n", d_Long));
				d_Long += (C_RAD_TO_DEG * (pRrlp->fltPosOffset / (C_EARTH_RADIUS * cos(d_Lat * C_DEG_TO_RAD))));
				MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG, ("*** Long  after bias: %f\n", d_Long));
			}
			/* convert longitude from degrees to radius and appply AI2 spec. */
			pRrlp->lLongtitue = (McpS32)(d_Long * C_DEG_TO_RAD * C_LSB_LON_REP);

			/* retrieve altitude */
			s16 = (McpS16)mcpf_getSignedBits(pMsgBuf, pOffset, 16);

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("refLocation_decode: raw altitude=%d ofs=%u\n", s16, *pOffset));

			/* apply AI2 spec. */
			/* if the depth bit is set in the altitude bits */
			if ( s16 < 0 )
			{
				McpS16 x_Ht;

				/* convert depth into a negative number expressed by x_Ht */
				x_Ht = (McpS16)(0 - ((McpU16)s16 & 0x7FFF));
				pRrlp->xHeight = (McpS16)(x_Ht * C_LSB_HT_REP);
				pAssist->altitude_sign = 1; /* Depth */
				pAssist->altitude = (McpU16)(-1 * x_Ht);
			}
			else
			{
				pRrlp->xHeight = (McpS16)((McpU16)s16 * C_LSB_HT_REP);
				pAssist->altitude_sign = 0; /* Height */
				pAssist->altitude = (McpU16)s16;
			}

             /* if the shape is ellipsoid point with altitude and uncertainty ellipsoid (0x09)
			    then extract the below fields and may use fyll in the future-Raghavendra*/
              if( (u_RefLocShape >> 4  ) == 0x09)
			  {
#if 0 //For Debug of SUPL RRLP Packet Decode
                uUncertainSemiMajor   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);
		        uUncertainSemiMinor   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);
		        uOrientationMajorAxis = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);
		        uOrientationMajorAxis *= 2;

		        uUncAlt		= (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);
		        uConfidence = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 7);
#endif
                        uUncertainSemiMajor   = ((McpU8) mcpf_getBits(pMsgBuf, pOffset, 8)) & 0x7f; //Top bit is a spare as per 3gpp 23.032 spec -section 7.3.6
		        uUncertainSemiMinor   = ((McpU8) mcpf_getBits(pMsgBuf, pOffset, 8)) & 0x7f; //Top bit is a spare as per 3gpp 23.032 spec -section 7.3.6;
		        uOrientationMajorAxis = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 8);
		        uOrientationMajorAxis *= 2;
		        uUncAlt		= ((McpU8) mcpf_getBits(pMsgBuf, pOffset, 8)) & 0x7f; //Top bit is a spare as per 3gpp 23.032 spec -section 7.3.6;
		        uConfidence = ((McpU8) mcpf_getBits(pMsgBuf, pOffset, 8)) & 0x7f; //Top bit is a spare as per 3gpp 23.032 spec -section 7.3.6;

				f_Uncertain = 10 * (pow(1.1,uUncertainSemiMajor)-1);

		       /* Covert this to integer and pass it */
		        pRrlp->qUncertainty = (McpU32) f_Uncertain;

		        pRrlp->qUncertainty = (McpU32) (pRrlp->qUncertainty * C_RECIP_LSB_POS_SIGMA)/3;
			  }
			  else
			  {



			/* Use User-Defined position uncertainty,
			* or else assume position uncertainty is mainly caused
			* by the radius of the cell sector, and convert it to
			* apply the AI2 spec.
			*/
			pRrlp->qUncertainty = (McpU32)
				(pRrlp->bPosUncertaintyDefined ?
				pRrlp->fltPosUncertainty * 1000 * C_RECIP_LSB_POS_SIGMA :
				15 /*KM*/ * 1000 * C_RECIP_LSB_POS_SIGMA);
				/* deleted p_LmControl->p_LmConfig->u_SmlcLAssisUnc */
			/* OLD HARD CODED UNCERTAINTY VALUE
			(McpU32) (C_CELL_RADIUS * C_RECIP_LSB_POS_SIGMA);
			*/
			  }
			pAssist->position_uncertainty = pRrlp->qUncertainty;

			/* Inject AA data into GPSC */
			sendAssistanceToNavc (pRrlp, pNavcCmd);

			/* signalling the receival of RefLoc */
			pRrlp->uAvailabilityFlags |= GPSC_REQ_LOC;
		}

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
								("refLocation_decode: end ofs=%u\n", *pOffset));
	}
	else
	{
		MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("refLocation_decode: allocation failed!\n"));
	}
}

/**
 * \fn     dgpsCorrections_decode
 * \brief  Decode RRLP DGPS correction field and jnect the data to GPS receiver
 *
 * Decode RRLP DGPS correction field and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void dgpsCorrections_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8 	uIdx;
	McpU8 	u_DgpsLength;
	McpU32	uTow;
	McpU8   uDgpsStatus;
	TNAVC_cmdParams    *pNavcCmd;
	T_GPSC_dgps_assist *pAssist;


	uTow 	 	 = (McpU32)mcpf_getBits(pMsgBuf, pOffset, 20);
	uDgpsStatus  = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);
	u_DgpsLength = (McpU8) (mcpf_getBits(pMsgBuf, pOffset, 4) + 1);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("dgpsCorrections: tow=%u status=%u Nsat=%u ofs=%u\n",
							 uTow, uDgpsStatus, u_DgpsLength, *pOffset));

	for (uIdx=0; uIdx < u_DgpsLength; uIdx++)
	{
		pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);

		if (pNavcCmd)
		{
			pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.dgps_assist;
			pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_DGPS;

			pAssist->gps_tow 	 = uTow;
			pAssist->dgps_status = uDgpsStatus;
			pAssist->dgps_Nsat 	 = u_DgpsLength;

			pAssist->dgps.svid = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);
			pAssist->dgps.iode = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 8);
			pAssist->dgps.dgps_udre= (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);
			pAssist->dgps.dgps_pseudo_range_cor = (McpS32)(((mcpf_getBits(pMsgBuf, pOffset, 12) * C_PRC_SCALEFACTOR) - C_PRC_OFFSET)/ C_PRC_AI2_SCALEFACTOR);
			pAssist->dgps.dgps_range_rate_cor 	= (McpS16)(((mcpf_getBits(pMsgBuf, pOffset, 8) * C_RRC_SCALEFACTOR) - C_RRC_OFFSET) / C_RRC_AI2_SCALEFACTOR);
			pAssist->dgps.dgps_deltapseudo_range_cor2 	= (McpS8)mcpf_getBits(pMsgBuf, pOffset, 8);
			pAssist->dgps.dgps_deltarange_rate_cor2		= (McpS8)mcpf_getBits(pMsgBuf, pOffset, 4);
			pAssist->dgps.dgps_deltapseudo_range_cor3	= (McpS8)mcpf_getBits(pMsgBuf, pOffset, 8);
			pAssist->dgps.dgps_deltarange_rate_cor3		= (McpS8)mcpf_getBits(pMsgBuf, pOffset, 4);

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("dgpsCorrections: i=%u svid=%u iode=%u udre=%u rngCor=%u rateCor=%u\n",
									 uIdx, pAssist->dgps.svid, pAssist->dgps.iode, pAssist->dgps.dgps_udre,
									 pAssist->dgps.dgps_range_rate_cor, pAssist->dgps.dgps_range_rate_cor));

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("dgpsCorrections: drngCor2=%d drateCor2=%d drngCor3=%d drateCor3=%d ofs=%u\n",
									 (McpS32) pAssist->dgps.dgps_deltapseudo_range_cor2,
									 (McpS32) pAssist->dgps.dgps_deltarange_rate_cor2,
									  (McpS32) pAssist->dgps.dgps_deltapseudo_range_cor3,
									  (McpS32) pAssist->dgps.dgps_deltarange_rate_cor3,
									 *pOffset ));

			sendAssistanceToNavc (pRrlp, pNavcCmd);

		}
		else
		{
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("dgpsCorrections_decode: allocation failed!\n"));
		}
	}

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("dgpsCorrections: end ofs=%u\n", *pOffset));

	pRrlp->uAvailabilityFlags |= GPSC_REQ_DGPS;          /* signalling the receival of DGPS */
}


/**
 * \fn     navigationModel_decode
 * \brief  Decode RRLP navigation model field and jnect the data to GPS receiver
 *
 * Decode RRLP navigation model (array of ephemeris per specified satellites) field and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void navigationModel_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU8 nSV; /* num. of navModelElement */
	McpU8 i, j=0,k=0,sentSV=0;
	TNAVC_cmdParams   *pNavcCmd = NULL;
	T_GPSC_ephemeris_assist  *pAssist;

	/* Length of ext-geograph. octet string  */
	nSV = (McpU8)(1+mcpf_getBits(pMsgBuf, pOffset, 4));

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("navigationModel: nSV=%u ofs=%u\n", nSV, *pOffset));

#if 0
	for (i=0; i<nSV; i++,j++) /* list length = num of SVs */
	{
		McpU32 u32;
		McpU8  uSvId = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);

		if(i % N_SV_15 == 0)
		{
			if(pNavcCmd)
			{
				/* Inject ephemeris assistance data into GPSC */
				sendAssistanceToNavc (pRrlp, pNavcCmd);
			}
			pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);
			mcpf_mem_set(pRrlp->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
			j = 0;
		}

		if (pNavcCmd && (j < N_SV_15))
		{
			pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_EPH;
			pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.ephemeris_assist[j];

			/* SvId */
			pAssist->svid = uSvId;

			/* Skip extension indicator preamble */
			mcpf_getBits(pMsgBuf, pOffset, 1);

			/* SatStatus ChoiceTag - currently SMLC only supports newNavModelUC (2) */
			u32 = mcpf_getBits(pMsgBuf, pOffset, 2);

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("navigationModel: svid=%u tag=%u ofs=%u\n", uSvId, u32, *pOffset));

			if ( u32 == 1 ) /* oldStatelliteAndModel: no encoded eph. values for this SV will follow */
			{
				mcpf_mem_free_from_pool (pRrlp->hMcpf, pNavcCmd);
				continue;
			}

			pAssist->ephem_code_on_l2 = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);    		/* CodeOnL2 */
			pAssist->ephem_ura 		  = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 4);      	/* URA */
			pAssist->ephem_svhealth   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 6);      	/* SvHealth */
			pAssist->ephem_iodc 	  = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 10);     	/* IODE */
			pAssist->iode 			  = (McpU8)pAssist->ephem_iodc;
			mcpf_getBits(pMsgBuf, pOffset, 1);                   /* L2Pflag    */
			mcpf_getBits(pMsgBuf, pOffset, 23);                  /* Reserved 1 */
			mcpf_getBits(pMsgBuf, pOffset, 24);                  /* Reserved 2 */
			mcpf_getBits(pMsgBuf, pOffset, 24);                  /* Reserved 3 */
			mcpf_getBits(pMsgBuf, pOffset, 16);                  /* Reserved 4 */
			pAssist->ephem_tgd = (McpS8)(mcpf_getBits(pMsgBuf, pOffset, 8) - 128);        	/* Tgd */
			pAssist->ephem_toc = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 16);             	/* Toc */
			pAssist->ephem_af2 = (McpS8)(mcpf_getBits(pMsgBuf, pOffset, 8) - 128);       	/* AF2 */
			pAssist->ephem_af1 = (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768); 		/* AF1 */
			pAssist->ephem_af0 = (McpS32)(mcpf_getBits(pMsgBuf, pOffset, 22) - 2097152);	/* AF0 */
			pAssist->ephem_crs = (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768);		/* Crs */
			pAssist->ephem_delta_n = (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768); 	/* DeltaN */
			pAssist->ephem_m0  = (McpS32)(mcpf_getBits(pMsgBuf, pOffset, 32) - 2147483648L);/* M0 */
			pAssist->ephem_cuc = (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768);   	/* Cuc */
			pAssist->ephem_e   = (McpU32) mcpf_getBits(pMsgBuf, pOffset, 32);               	/* E */
			pAssist->ephem_cus = (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768);   	/* Cus */
			pAssist->ephem_a_power_half = (McpU32)mcpf_getBits(pMsgBuf, pOffset, 32);    	/* APowerHalf */
			pAssist->ephem_toe = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 16);             	/* Toe */


			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("navigationModel: CodeOnL2=%u URA=%u SvHealth=%u iodc=%u\n",
									 pAssist->ephem_code_on_l2, pAssist->ephem_ura,
									 pAssist->ephem_svhealth, pAssist->ephem_iodc));

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("navigationModel: tgd=%d toc=%u af2=%d af1=%d af0=%d crs=%d\n",
									 pAssist->ephem_tgd, (McpU32) pAssist->ephem_toc,
									 (McpS32)pAssist->ephem_af2, (McpS32)pAssist->ephem_af1,
                                     (McpS32)pAssist->ephem_af0, (McpS32)pAssist->ephem_crs));

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("navigationModel: delN=%d eM0=%d cuc=%d eE=%u eCus=%d pwH=%u eToe=%u ofs=%u\n",
									 (McpS32) pAssist->ephem_delta_n, (McpS32) pAssist->ephem_m0,
									 (McpS32) pAssist->ephem_cuc,  (McpU32) pAssist->ephem_e,  (McpS32) pAssist->ephem_cus,
									  (McpU32) pAssist->ephem_a_power_half,  (McpU32) pAssist->ephem_toe, *pOffset));


			/* The Fit Interval flag is parsed. This determines if the ephemeris data is valid for 4
			 * hours (value 0) or longer, 6 hours (value 1). This field is passed to the sensor in
			 * the MSB of the u_Accuracy field.
			 */
#endif
	if(nSV != 0)
	{
        for (i=0, j=0; i<nSV; i++,j++) /* list length = num of SVs */
        {
            McpU32 u32;
            McpU8  uSvId = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);

            if (i % N_SV_15 == 0)
            {
                /* Allocate memory once for a set of 15 satellites. */
                pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);
                if(NULL == pNavcCmd)
                {
                    MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("navigationModel: MEM allocation failed!\n"));
                    MCPF_Assert(1);
                    return;
                }

                mcpf_mem_set(pRrlp->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
                j = 0;
            }

			if (pNavcCmd && (j < N_SV_15))
			{
				pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_EPH;
				pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.ephemeris_assist[j];
				pAssist->svid = uSvId;
				mcpf_getBits(pMsgBuf, pOffset, 1);
				u32 = mcpf_getBits(pMsgBuf, pOffset, 2);
				if ( u32 == 1 ) /* oldStatelliteAndModel: no encoded eph. values for this SV will follow */
				{
					mcpf_mem_free_from_pool (pRrlp->hMcpf, pNavcCmd);
					continue;
				}
				pAssist->ephem_code_on_l2 = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 2);    		/* CodeOnL2 */
				pAssist->ephem_ura 		  = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 4);      	/* URA */
				pAssist->ephem_svhealth   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 6);      	/* SvHealth */
				pAssist->ephem_iodc 	  = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 10);     	/* IODE */
				pAssist->iode 			  = (McpU8)pAssist->ephem_iodc;
				mcpf_getBits(pMsgBuf, pOffset, 1);                   /* L2Pflag    */
				mcpf_getBits(pMsgBuf, pOffset, 23);                  /* Reserved 1 */
				mcpf_getBits(pMsgBuf, pOffset, 24);                  /* Reserved 2 */
				mcpf_getBits(pMsgBuf, pOffset, 24);                  /* Reserved 3 */
				mcpf_getBits(pMsgBuf, pOffset, 16);                  /* Reserved 4 */
				pAssist->ephem_tgd = (McpS8)(mcpf_getBits(pMsgBuf, pOffset, 8) - 128);        	/* Tgd */
				pAssist->ephem_toc = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 16);             	/* Toc */
				pAssist->ephem_af2 = (McpS8)(mcpf_getBits(pMsgBuf, pOffset, 8) - 128);       	/* AF2 */
				pAssist->ephem_af1 = (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768); 		/* AF1 */
				pAssist->ephem_af0 = (McpS32)(mcpf_getBits(pMsgBuf, pOffset, 22) - 2097152);	/* AF0 */
				pAssist->ephem_crs = (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768);		/* Crs */
				pAssist->ephem_delta_n = (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768); 	/* DeltaN */
				pAssist->ephem_m0  = (McpS32)(mcpf_getBits(pMsgBuf, pOffset, 32) - 2147483648L);/* M0 */
				pAssist->ephem_cuc = (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768);   	/* Cuc */
				pAssist->ephem_e   = (McpU32) mcpf_getBits(pMsgBuf, pOffset, 32);               	/* E */
				pAssist->ephem_cus = (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768);   	/* Cus */
				pAssist->ephem_a_power_half = (McpU32)mcpf_getBits(pMsgBuf, pOffset, 32);    	/* APowerHalf */
				pAssist->ephem_toe = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 16);             	/* Toe */
				if (mcpf_getBits(pMsgBuf, pOffset, 1)) /* FitFlag */
					pAssist->ephem_ura |= 0x80;
				mcpf_getBits(pMsgBuf, pOffset, 5); /* skip AODA */
				pAssist->ephem_cic 		= (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768);        	/* Cic */
				pAssist->ephem_omega_a0 = (McpS32)(mcpf_getBits(pMsgBuf, pOffset, 32) - 2147483648L);  	/* OmegaA0 */
				pAssist->ephem_cis 		= (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768);        	/* Cis */
				pAssist->ephem_i0 		= (McpS32)(mcpf_getBits(pMsgBuf, pOffset, 32) - 2147483648L);  	/* I0 */
				pAssist->ephem_crc 		= (McpS16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768);        	/* Crc */
				pAssist->ephem_w 		= (McpS32)(mcpf_getBits(pMsgBuf, pOffset, 32) - 2147483648L);  	/* W */
				pAssist->ephem_omega_adot = (McpS32)(mcpf_getBits(pMsgBuf, pOffset, 24) - 8388608);  	/* OmegaADot */
				pAssist->ephem_idot 	= (McpU16)(mcpf_getBits(pMsgBuf, pOffset, 14) - 8192);          /* IDot */
				/* set the below two fieds as 0 as these info is needed only for predicted eph*/
				pAssist->ephem_predicted =0;
				pAssist->ephem_predSeedAge=0;

				MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
										("navigationModel: cic=%d omA0=%d cis=%d eI0=%d\n",
										  (McpS32) pAssist->ephem_cic,  (McpS32) pAssist->ephem_omega_a0,
										  (McpS32) pAssist->ephem_cis,  (McpS32) pAssist->ephem_i0 ));

				MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
										("navigationModel: eCrc=%d eW=%d adot=%d idot=%d ofs=%u\n",
										  (McpS32) pAssist->ephem_crc,  (McpS32) pAssist->ephem_w,  (McpS32)
										  (McpS32) pAssist->ephem_omega_adot,  (McpS32) pAssist->ephem_idot, *pOffset));
			}
			else
			{
				MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("navigationModel: allocation failed!\n"));
			}

			if(((i+1) % N_SV_15 == 0) && (pNavcCmd))
			{
				/* Inject ephemeris assistance data into GPSC */
				sendAssistanceToNavc (pRrlp, pNavcCmd);
			}

		} /* end bracket of Sv list for-loop */
	}
	if((i % N_SV_15 != 0) && (pNavcCmd))
	{
		/* Inject ephemeris assistance data into GPSC */
		sendAssistanceToNavc (pRrlp, pNavcCmd);
	}
	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("navigationModel: end ofs=%u\n", *pOffset));

	pRrlp->uAvailabilityFlags |= GPSC_REQ_NAV;        /* signalling the receival of NavModel */
}

/**
 * \fn     ionosphericModel_decode
 * \brief  Decode RRLP Ionospherical model field and jnect the data to GPS receiver
 *
 * Decode RRLP Ionospherical model field and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void ionosphericModel_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	TNAVC_cmdParams   	*pNavcCmd;
	T_GPSC_iono_assist  *pAssist;
	McpU8               *pIono;
    McpU8               i;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_IONO;
		pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.iono_assist;
		pIono = (McpU8 *)pAssist; /* Klocwork Changes */

		for ( i = 0; i < 8; i++ )
		{
			pIono[i] = (McpU8)(mcpf_getBits(pMsgBuf, pOffset, 8) - 128);

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("ionosphericModel: iono=%u ofs=%u\n", pIono[i], *pOffset));
		}

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
								("ionosphericModel: end ofs=%u\n", *pOffset));

		/* Inject iono data into GPSC */
		sendAssistanceToNavc (pRrlp, pNavcCmd);

		pRrlp->uAvailabilityFlags |= GPSC_REQ_IONO;       /* signalling the receival of IONO */
	}
	else
	{
		MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("ionosphericModel_decode: allocation failed!\n"));
	}

}

/**
 * \fn     utcModel_decode
 * \brief  Decode RRLP UTC model field and jnect the data to GPS receiver
 *
 * Decode RRLP UTC model field and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void utcModel_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	TNAVC_cmdParams   *pNavcCmd;
	T_GPSC_utc_assist *pAssist;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_UTC;
		pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.utc_assist;

		pAssist->utc_a1         = (McpS32)(mcpf_getBits(pMsgBuf, pOffset, 24) - 8388608);
		pAssist->utc_a0         = (McpS32)(mcpf_getBits(pMsgBuf, pOffset, 32) - 2147483648);
		pAssist->utc_tot        = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 8);
		pAssist->utc_wnt        = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 8);
		pAssist->utc_delta_tls  = (McpS8)(mcpf_getBits(pMsgBuf, pOffset, 8) - 128);
		pAssist->utc_wnlsf      = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 8);
		pAssist->utc_dn         = (McpS8)(mcpf_getBits(pMsgBuf, pOffset, 8) - 128);
		pAssist->utc_delta_tlsf = (McpS8)(mcpf_getBits(pMsgBuf, pOffset, 8) - 128);

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
								("utcModel: a1=%d a0=%d tot=%u wnt=%u delta=%d ofs=%u\n",
								 (McpS32) pAssist->utc_a1, (McpS32) pAssist->utc_a0,
								 (McpU32) pAssist->utc_tot, (McpU32) pAssist->utc_wnt,
								 (McpS32) pAssist->utc_delta_tls, *pOffset));

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
								("utcModel: wnlsf=%u dn=%u delta_tlsf=%d ofs=%d\n",
								  pAssist->utc_wnlsf, pAssist->utc_dn, pAssist->utc_delta_tlsf,
								 *pOffset));

		/* Inject UTC model data into GPSC */
		sendAssistanceToNavc (pRrlp, pNavcCmd);

		pRrlp->uAvailabilityFlags |= GPSC_REQ_UTC;    	/* signalling the receival of UTC */
	}
	else
	{
		MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("utcModel_decode: allocation failed!\n"));

	}
}

/**
 * \fn     almanac_decode
 * \brief  Decode RRLP Almanach field and jnect the data to GPS receiver
 *
 * Decode RRLP Almanach - array of almanach records per specified satellites and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void almanac_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	TNAVC_cmdParams   	*pNavcCmd = NULL;
	McpU8               nSV, i, j=0;
	McpU16              uAlmanacWeek;
	T_GPSC_almanac_assist *pAssist;

	/* Almanac WNa  */
	uAlmanacWeek = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 8); /* 8 LSBs of the full 10-bit week number */

	/* start almanac list */
	nSV = (McpU8)(1+mcpf_getBits(pMsgBuf, pOffset, 6));

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("almanac: AlmWeek=%u nSV=%u ofs=%u\n", uAlmanacWeek, nSV, *pOffset));


	if (nSV != 0)
		{
		pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);
		if(NULL == pNavcCmd)
			{
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("almanac: MEM allocation failed!\n"));
			MCPF_Assert(1);
			return;
			}
			mcpf_mem_set(pRrlp->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
			j = 0;
		for (i=0; i<nSV; i++,j++)
		{
		if (pNavcCmd)
		{
			pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_ALMANAC;
			pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.almanac_assist[j];

			pAssist->svid           = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);                 /* SvId */
			pAssist->almanac_week   = uAlmanacWeek;
			pAssist->almanac_e      = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 16);               /* E    */
			pAssist->almanac_toa    = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 8);                 /* Toa  */
			pAssist->almanac_ksii   = (McpU16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768);     /* Ksii */
			pAssist->almanac_omega_dot  = (McpU16)(mcpf_getBits(pMsgBuf, pOffset, 16) - 32768); /* OmegaDot     */
			pAssist->almanac_svhealth   = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 8);             /* SV health    */
			pAssist->almanac_a_power_half = (McpU32)mcpf_getBits(pMsgBuf, pOffset, 24);         /* A Power Half */
			pAssist->almanac_omega0 = (McpU32)(mcpf_getBits(pMsgBuf, pOffset, 24) - 8388608);   /* Omega0, signed     */
			pAssist->almanac_w      = (McpU32)(mcpf_getBits(pMsgBuf, pOffset, 24) - 8388608);   /* W, signed          */
			pAssist->almanac_m0     = (McpU32)(mcpf_getBits(pMsgBuf, pOffset, 24) - 8388608);   /* M0, 24-bit signed  */
			pAssist->almanac_af0    = (McpU16)(mcpf_getBits(pMsgBuf, pOffset, 11) - 1024);      /* AF0, 11-bit signed */
			pAssist->almanac_af1    = (McpU16)(mcpf_getBits(pMsgBuf, pOffset, 11) - 1024);      /* AF1, 11-bit signed */

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("almanac: i=%u svid=%u E=%u toa=%u ksii=%d oDot=%d\n",
									 i, pAssist->svid, pAssist->almanac_e,
									 pAssist->almanac_toa, pAssist->almanac_ksii, pAssist->almanac_svhealth));

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("almanac: pwH=%u om0=%d w=%d m0=%d af0=%d af1=%d ofs=%u\n",
									 (McpU32) pAssist->almanac_a_power_half, (McpS32) pAssist->almanac_omega0,
									 (McpS32) pAssist->almanac_w, (McpS32) pAssist->almanac_m0,
									 (McpS32) pAssist->almanac_af0, (McpS32) pAssist->almanac_af1,
									 *pOffset));
		}
		else
		{
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("almanac_decode: allocation failed!\n"));
		}
			if((i+1) % N_SV_15 == 0)
			{
				if(pNavcCmd)
				{
					MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
											("almanac_decode: InLoop sent almanac for =%d\n", (i+1)));
					sendAssistanceToNavc (pRrlp, pNavcCmd);
	} /* end of SV list for-loop */
				pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);
				if(NULL == pNavcCmd)
				{
					MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("almanac: MEM allocation failed!\n"));
					MCPF_Assert(1);
					return;
				}
				mcpf_mem_set(pRrlp->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
				j = 0;
			}
		} /* end of SV list for-loop */

	if(i % N_SV_15 != 0)
	{
		if(pNavcCmd)
		{
			/* Inject ephemeris assistance data into GPSC */
			sendAssistanceToNavc (pRrlp, pNavcCmd);
			}
		}
	}

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("almanac: end ofs=%u\n", *pOffset));

	pRrlp->uAvailabilityFlags |= GPSC_REQ_ALMANAC;        /* signalling the receival of almanac */

}

static void acquisAssist_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	TNAVC_cmdParams		*pNavcCmd = NULL;
	McpU8 	uNumSVs, uIdx, uOptPream, j=0;
	McpU32	uTow;
	T_GPSC_acquisition_assist   *pAssist;
	TRrlp_acquisitionAssist	 	*pRrlpAcqAssist = &pRrlp->tAcquisitionAssist;
#ifdef GPSC_NL5500
	TNAVCD_GpsTime				*pGpsTime = &pRrlp->tGpsTime;
#endif

	/* Opt. param. preamble */
	uOptPream = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 1);

	/* GpsTow */
	uTow = (McpU32)mcpf_getBits(pMsgBuf, pOffset, 23);
	pRrlpAcqAssist->uGpsTow = uTow;

	pRrlp->uAvailabilityFlags |= GPSC_REQ_TIME;  /* signalling the receival of RefTime */

#ifdef GPSC_NL5500

	pGpsTime->tTime.gps_msec = uTow * 80;
	pGpsTime->tTime.time_unc = 2000000;				/* usec */
	pGpsTime->tTime.sub_ms   = 0;
	pGpsTime->tTime.time_accuracy = GPSC_TIME_ACCURACY_COARSE;
	pGpsTime->uSystemTimeMs = pRrlp->uSystemTimeMs;

	/* Synchronize GPS time in NAVC object */
	injectGpsTimeToNavc (pRrlp, &pRrlp->tGpsTime);
#endif

	/* set assistance avalability bit */
	pRrlp->uAvailabilityFlags |= GPSC_REQ_TIME;

	if (uOptPream)
	{
		/* retrieve GSM Time Sequence: Not currently supported */
	}

	uNumSVs = (McpU8) (mcpf_getBits(pMsgBuf, pOffset, 4) + 1);   /* Num of SVs */
	pRrlpAcqAssist->uNumSV = uNumSVs;

#ifndef UNLIMITED_EPHEMERIS_STORAGE
	pRrlp->tAcquisitionAssist.uAcqAsstListedSVs = 0;
#endif

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("acquisAssist: opts=%x tow=%u numSVs=%u ofs=%u\n",
							uOptPream, uTow, uNumSVs, *pOffset));

	if (uNumSVs !=0)
	{
		pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);
		if(NULL == pNavcCmd)
		{
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("acquisAssist: MEM allocation failed!\n"));
			MCPF_Assert(1);
			return;
		}
		mcpf_mem_set(pRrlp->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
		j = 0;
	for (uIdx=0; uIdx < uNumSVs; uIdx++,j++ ) /* RRLP gives at most 16 SVs in acq. list */
	{
		TRrlp_acqElement 		*pAcqElement;
		TRrlp_additionalAngle 	*pAddAngle;

		pAcqElement = &pRrlpAcqAssist->tAcquisElement[uIdx];
		pAddAngle   = &pRrlpAcqAssist->tAddAngle[uIdx];


			if(pNavcCmd && (uIdx < C_MAX_SVS_ACQ_ASSIST) && (j < N_SV_15))
			{
				/* Inject assistance data into GPSC */

			pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_ACQ;
			pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.acquisition_assist[j];



			/* Optional Preamble */
			uOptPream = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);

			/* SvID */
			pAcqElement->uSvId = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);
			/* LCE */
			pAssist->svid = pAcqElement->uSvId;

			pAssist->gps_tow = uTow;

#ifndef UNLIMITED_EPHEMERIS_STORAGE
			pRrlpAcqAssist->uAcqAsstListedSVs |= 1 << pAcqElement->uSvId;
#endif
			/* 0th order Doppler  */
			pAcqElement->sDoppler0 = (McpS16)((McpU16)mcpf_getBits(pMsgBuf, pOffset, 12) - 2048);

			/* LCE */
			pAssist->aa_doppler0 = pAcqElement->sDoppler0;

			if (uOptPream & 0x2 )
			{
				/* Optional Additional Doppler */

				/* 1st order Doppler  */
				pAcqElement->uDoppler1 = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 6);

				/* Doppler unc.  */
				pAcqElement->uDopplerUnc = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);

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

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
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

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
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
				pRrlp->uAvailabilityFlags |= C_ANGLE_AVAIL;
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

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("acquisAssist: azim=%u elev=%u ofs=%u\n",
									pAddAngle->uAzimuth, pAddAngle->uElevation, *pOffset));

		}
		else
		{
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("acquisAssist: allocation failed!\n"));
		}
			if((uIdx+1) % N_SV_15 == 0)
			{
				if(pNavcCmd)
				{
					/* Inject assistance data into GPSC */
					sendAssistanceToNavc (pRrlp, pNavcCmd);
	}
				pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);
				if(NULL == pNavcCmd)
				{
					MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("acquisAssist: MEM allocation failed!\n"));
					MCPF_Assert(1);
					return;
				}
				mcpf_mem_set(pRrlp->hMcpf, pNavcCmd, 0xFF, sizeof(TNAVC_cmdParams));
				j = 0;
			}
		}

	if(uIdx % N_SV_15 != 0)
	{
		if(pNavcCmd)
		{
			/* Inject ephemeris assistance data into GPSC */
			sendAssistanceToNavc (pRrlp, pNavcCmd);
			}
		}
	}
	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG, ("acquisAssist: end ofs=%u\n", *pOffset));

	pRrlp->uAvailabilityFlags |= GPSC_REQ_AA;
}


/**
 * \fn     seqOfBadSatelliteSet_decode
 * \brief  Decode RRLP sequence of bad satellites and jnect the data to GPS receiver
 *
 * Decode RRLP sequence of bad satellites - array of records of specified length and jnect the data to GPS receiver
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void seqOfBadSatelliteSet_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	TNAVC_cmdParams   *pNavcCmd;
	T_GPSC_rti_assist *pAssist;
	McpU8 			  uLength, uIdx;


	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_RTI;
		pAssist = &pNavcCmd->tInjectAssist.tAssistance.assistance_inject_union.rti_assist;

		uLength = (McpU8)(mcpf_getBits(pMsgBuf, pOffset, 4) + 1);

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
								("seqOfBadSatelliteSet: uLength=%u ofs=%u\n", uLength, *pOffset));

		for (uIdx=0; uIdx<uLength; uIdx++)
		{
			McpU32 uSvId;

			uSvId = mcpf_getBits(pMsgBuf, pOffset, 6) + 1;

			if (uSvId <= 32)
			{
				pRrlp->uRealTimeIntegrity[0] |= 1 << (uSvId-1);
				pAssist->rti_bitmask[0] = pRrlp->uRealTimeIntegrity[0];
			}
			else
			{
				pRrlp->uRealTimeIntegrity[1] |= 1 << (uSvId-33);
				pAssist->rti_bitmask[1] = pRrlp->uRealTimeIntegrity[1];
			}

			MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
									("seqOfBadSatelliteSet: i=%u svId=%u ofs=%u\n", uIdx, uSvId, *pOffset));

		}

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG, ("seqOfBadSatelliteSet: end ofs=%u\n", *pOffset));


		/* Inject RTI assistance data into GPSC */
		sendAssistanceToNavc (pRrlp, pNavcCmd);

		/* signalling the receival of real-time integrity */
		pRrlp->uAvailabilityFlags |= GPSC_REQ_RTI;
	}
	else
	{
		MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("seqOfBadSatelliteSet_decode: allocation failed!\n"));
	}

}

/**
 * \fn     controlHeader_decode
 * \brief  Decode RRLP GPS assistance message optional elements
 *
 * Decode RRLP GPS assistance message optional map and calls appropriate decoding functions if the relevant
 * bit is set in option field
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void controlHeader_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{

    /* GPS-AssistData Seq. Opt. Parameters Preamble */
    McpU32 uOptPream = (McpU16)mcpf_getBits(pMsgBuf, pOffset, 9);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("controlHeader_decode(assistance): opts=%x ofs=%u\n", uOptPream, *pOffset));

	/* 9-bit Optional Parameter Preamble : */
	if(uOptPream & 0x100) /* Reference Time */
	{
		referenceTime_decode(pRrlp, pMsgBuf, pOffset);
	}
	if(uOptPream & 0x080)
	{
		refLocation_decode(pRrlp, pMsgBuf, pOffset);
	}
	if(uOptPream & 0x040)
	{
		dgpsCorrections_decode(pRrlp, pMsgBuf, pOffset);
	}
	if(uOptPream & 0x020)
	{
		navigationModel_decode(pRrlp, pMsgBuf, pOffset);
	}
	if(uOptPream & 0x010)
	{
		ionosphericModel_decode(pRrlp, pMsgBuf, pOffset);
	}
	if(uOptPream & 0x008)
	{
		utcModel_decode(pRrlp, pMsgBuf, pOffset);
	}
	if(uOptPream & 0x004)
	{
		almanac_decode(pRrlp, pMsgBuf, pOffset);
	}
	if(uOptPream & 0x002)
	{
		acquisAssist_decode(pRrlp, pMsgBuf, pOffset);
	}
	if(uOptPream & 0x001)
	{
		seqOfBadSatelliteSet_decode(pRrlp, pMsgBuf, pOffset);
	}

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("controlHeader_decode(assistance): end ofs=%u\n", *pOffset));

}


/**
 * \fn     gpsAssistData_decode
 * \brief  Decode RRLP GPS assistance message element
 *
 * Decode RRLP GPS assistance data starting from control header
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void gpsAssistData_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	controlHeader_decode(pRrlp, pMsgBuf, pOffset);
}

/**
 * \fn     posInstruct_decode
 * \brief  Decode RRLP position instruction
 *
 * Decode RRLP GPS position instruction fields
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void posInstruct_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU32	uOptPream;
	McpU8 u_ResponseTime;

	uOptPream = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 1);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("posInstruct_decode: opts=%x ofs=%u\n", uOptPream, *pOffset));


	methodType_decode(pRrlp, pMsgBuf, pOffset);

	pRrlp->uPosMethod = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);
	u_ResponseTime	= (McpU8)mcpf_getBits(pMsgBuf, pOffset, 3);
	pRrlp->uMeasureResponseTime = (McpU8)(1<<u_ResponseTime);

	pRrlp->uUseMultipleSets = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 1);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("posInstruct_decode: posMeth=%u respTime=%u useMSets=%u ofs=%u\n",
							 pRrlp->uPosMethod, u_ResponseTime, pRrlp->uUseMultipleSets, *pOffset));

	if (uOptPream)
	{
		pRrlp->uEnvironmentCharacter  = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
								("posInstruct_decode: envChar=%u ofs=%u\n",
								 pRrlp->uEnvironmentCharacter, *pOffset));
	}

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG, ("posInstruct_decode: end ofs=%u\n", *pOffset));

}

/**
 * \fn     protocolError_decode
 * \brief  Decode RRLP protocol error
 *
 * Decode RRLP protocol error field
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void protocolError_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{

	/* 1 bit  Extension Bit */
	if (pRrlp->uRrlpExtInd)
	{
		pRrlp->uProtocolError = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);
		pRrlp->uValidExtInd   = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 1);

		if (pRrlp->uValidExtInd)
		{
			/* 3 bits Error Code Value */
			pRrlp->uExtProtoclError = (McpU8) mcpf_getBits(pMsgBuf, pOffset, 3);

			/* Consume 4 padding bits (0's) */
			mcpf_getBits(pMsgBuf, pOffset, 4);
		}
	}

}

/**
 * \fn     assistData_decode
 * \brief  Decode RRLP assistance data
 *
 * Decode RRLP assistance data fields according to optional bits set,
 * currently only GPS Assistance is supported
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void assistData_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU32	uExtInt, uOptPream;
	uExtInt = mcpf_getBits(pMsgBuf, pOffset, 1);
    uOptPream = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("assistData_decode: ext=%x opts=%x ofs=%u\n", uExtInt, uOptPream, *pOffset));

	if (uOptPream & C_ASSIS_OPT_PREAM_REF_ASSIS )
	{
		// not supported - referenceAssistData
	}

	if (uOptPream & C_ASSIS_OPT_PREAM_MSR_ASSIS )
	{
		// not supported - msrAssistData
	}

	if (uOptPream & C_ASSIS_OPT_PREAM_SYSINFO )
	{
		// not supported - sysInfoAssistData
	}

	if (uOptPream & C_ASSIS_OPT_PREAM_EXTCONT )
	{
		// not supported - extensionContainer
	}

	if (uOptPream & C_ASSIS_OPT_PREAM_GPSASSIS )
	{
		gpsAssistData_decode(pRrlp, pMsgBuf, pOffset);
	}

	pRrlp->bMoreData = MCP_FALSE;

	if (uOptPream & C_ASSIS_OPT_PREAM_MORE_DATA )
	{
		McpU32 temp = mcpf_getBits(pMsgBuf, pOffset, 1);

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG, ("assistData_decode: moreData=%u\n", temp));

		if (temp)
		{
			pRrlp->bMoreData = MCP_TRUE;
		}
	}

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("assistData_decode: end ofs=%u\n", *pOffset));

} /* end of AssistanceData Choice */

/**
 * \fn     msrPosition_decode
 * \brief  Decode RRLP Measurement Position request
 *
 * Decode RRLP Measurement Position request fields according to optional bits set,
 * currently only GPS Assistance is supported
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */
static void msrPosition_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU32	uExtInt, uOptPream;

	LOGD(" \n REL5 - inside msrPosition_decode  \n" );

	uExtInt = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 1);
	pRrlp->uRel5ExtInd = uExtInt;

	uOptPream = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 5);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("msrPosition_decode extInd=%u opts=%x ofs=%u\n",
							 uExtInt, uOptPream, *pOffset));

	/* pos_instruct_type Sequence is mandatory */
	posInstruct_decode(pRrlp, pMsgBuf, pOffset);

	/* No more data expected if assistance data comes in messageposition request */
	pRrlp->bMoreData = MCP_FALSE;

	/* Go through MsrPosReq's Optional Parameter Preamble and
	 * process each optional sequence
	 */

	if ( uOptPream & C_MSRREQ_REF_ASSIS )
	{
		// not supported - referenceAssistData
	}

	if ( uOptPream & C_MSRREQ_MSR_ASSIS )
	{
		// not supported - msrAssistData
	}

	if ( uOptPream & C_MSRREQ_SYSINFO_ASSIS )
	{
		// not supported - sysInfoAssistData
	}

	if ( uOptPream & C_MSRREQ_GPS_ASSIS )
	{

		gpsAssistData_decode(pRrlp, pMsgBuf, pOffset);
	}

	if ( uOptPream & C_MSRREQ_EXT_CONT )
	{
		// not supported - extensionContainer
	}

    /*  RRLP-REL5 Start.*/
	if(uExtInt)
		rrlp_rel5_ext_decode(pRrlp, pMsgBuf, pOffset);

	//RRLP-REL5 End

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("msrPosition_decode end ofs=%u\n",*pOffset));

}

//RRLP-REL5 Start



/**
 * \fn     rrlp_rel5_ext_decode
 * \brief  Decode RRLP extension container for REL5
 *
 * Decode RRLP message type from received RRLP message pointed by pMsgBuf and starting from bit offset
 * pOffset
 *
 * \note
 * \param	pRrlp   - pointer to RRLP object
 * \param  	pMsgBuf - pointer to RRLP input message
 * \param  	pOffset - bit ofset from message start, increased by processed RRLP field bit length on return
 * \return  void
 * \sa
 */

static void rrlp_rel5_ext_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU32 *pOffset)
{
	McpU32	uExtInt, u_Rel5MsrRspExt1,u_Rel5MsrRspExt2, uOptParam;


	/*  Read the next 7 bits of REL5  ext.*/
	 u_Rel5MsrRspExt1 = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 7);

     /*  Read the next 2 bits of optional field.*/
	 uOptParam = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 2);

    /*  Read the next 8 bits of REL5  ext.*/
	 u_Rel5MsrRspExt1 = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 8);


     /* Ext bit for REL5 extension */
	 uExtInt = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 1);

	 LOGD("\n REL5-5 - uExtInt is %x" , uExtInt);


	/* Get SMLC code from MSR Pos Req */
	pRrlp->smlccode = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 6);

        LOGD("\n REL5-6 - pRrlp->smlccode  is %x ", pRrlp->smlccode );

	/* Get transaction ID from MSR Pos Req */
	pRrlp->transactionID = (McpU8)mcpf_getBits(pMsgBuf, pOffset, 18);

        LOGD ("\n  REL5-7 - pRrlp->transactionID is %x" ,  pRrlp->transactionID);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("rrlp_rel5_ext_decode end ofs=%u\n",*pOffset));

}

//RRLP-REL5 END




/**
 * \fn     rrlp_decode
 * \brief  Decode RRLP message
 *
 * Decode RRLP message fields
 *
 * \note
 * \param	pRrlp     - pointer to RRLP object
 * \param  	pMsgBuf   - pointer to RRLP input message
 * \param  	uMsgLength - input message length
 * \return  number of processed bits from input message
 * \sa
 */
static McpU16 rrlp_decode (TRrlp *pRrlp, McpU8* pMsgBuf, McpU16 uMsgLength)
{
	McpU32 	offset = 0; 		/* bit offset */
	McpU32	uChoiceTag;

	UNREFERENCED_PARAMETER(uMsgLength);

#ifdef RRLP_DEBUG
	LOGD(" \n REL5-1 - inside rrlp_decode  \n" );
	dumpMsg (pRrlp, pMsgBuf, uMsgLength);
	MCP_HAL_LOG_DumpBuf(NULL,NULL,NULL,"RRLP-SUPLC",pMsgBuf,uMsgLength);
#endif

	pRrlp->uRefNum = (McpU8)mcpf_getBits(pMsgBuf, &offset, 3);

	pRrlp->uRrlpExtInd = (McpU8)mcpf_getBits(pMsgBuf, &offset, 1);
	uChoiceTag = (McpU8)mcpf_getBits(pMsgBuf, &offset, 3);
	pRrlp->uRrlpChoiceTag = (McpU8) uChoiceTag;

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("rrlp_decode refNum=%u extInd=%u tag=%u ofs=%u\n",
							 pRrlp->uRefNum, pRrlp->uRrlpExtInd, pRrlp->uRrlpChoiceTag, offset));

	switch (uChoiceTag)
	{
		case C_RRLP_CHOICE_MSRPOSREQ:
			msrPosition_decode(pRrlp, pMsgBuf, &offset);
			break;

		case C_RRLP_CHOICE_ASSISTDATA:
			assistData_decode(pRrlp, pMsgBuf, &offset);
			break;

		case C_RRLP_CHOICE_PROTCOL_ERR:
			/* Extract the Protocol Error Type and the Reference # */
			protocolError_decode(pRrlp, pMsgBuf, &offset);
			break;

		default:
			/* not supposed to get any other choice tags return failure.*/
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("rrlp_decode unsupported choisce tag=%u !\n", uChoiceTag));
		break;
  } /* end bracket for switch - ChoiceTag */

  MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,("rrlp_decode end ofs=%u\n",offset));

  return (McpU16) offset;
}

/**
 * \fn     processRrlpMsg
 * \brief  Process received RRLP message
 *
 * Decode RRLP message fields and performs requested actions: inject assistance data,
 * request location fix, send RRLP response
 *
 * \note
 * \param	pRrlp     - pointer to RRLP object
 * \param  	pMsgBuf   - pointer to RRLP input message
 * \param  	uMsgLength - input message length
 * \return  result of operation: OK or Error if number of processed bits is not equal to
 *          input message length
 * \sa      rrlp_decode
 */
static EMcpfRes processRrlpMsg( TRrlp *pRrlp, McpU8* pMsgBuf, McpU16 uMsgLength)
{
	McpU8  		uAck;
	handle_t 	hMcpf = pRrlp->hMcpf;
	McpU16	  	numOfParsedBits, numOfParsedBytes;
	McpU8  		uErrorReason;
	McpBool  	bRrlpLocReq = MCP_FALSE; /* turn TRUE if RRLP-LocReq is received */
	EMcpfRes  	eRes = RES_ERROR;
	T_GPSC_loc_fix_response tLocFixResponse;

	numOfParsedBits = rrlp_decode (pRrlp, pMsgBuf, uMsgLength);

	MCPF_REPORT_INFORMATION(pRrlp->hMcpf, RR_MODULE_LOG,
							("processRrlpMsg: msgLen=%u parsedBits=%u\n", uMsgLength, numOfParsedBits));

	numOfParsedBytes = (McpU16) (numOfParsedBits / 8);
	if (numOfParsedBits % 8)
	{
		/* one more byte for bits remainder */
		numOfParsedBytes++;
	}
	/* Check if the number of decoded bytes is equal to the received one */
    //if (numOfParsedBytes == uMsgLength)
	{
		/* Check if the incoming RRLP data is Assistance Data Delivery.*/
		if ( pRrlp->uRrlpChoiceTag == C_RRLP_CHOICE_ASSISTDATA )
		{
			TNAVC_cmdParams   *pNavcCmd;

			uAck = 0;

			/* The three bits RefNumber is shifted to MSB */
			uAck = (McpU8)(pRrlp->uRefNum << 5);

			/* The next 1 bit represents Extension container followed by 3 bits that
			 * represents the Assistance Data Acknowledgement and padded with 0
			 */
			uAck |= 0x06;

			sendRrlpMsg (pRrlp, &uAck, sizeof(uAck));

			MCPF_REPORT_INFORMATION(hMcpf, RR_MODULE_LOG, ("Response to RRLP sent for assistance"));


			pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);
			if (pNavcCmd)
			{
				pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_COMPL_SET;
				sendAssistanceToNavc (pRrlp, pNavcCmd);
			}
			else
			{
				MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("processRrlpMsg: allocation failed!\n"));
				return RES_ERROR;
			}
			/* Initializes the RefNumber variable.*/
			pRrlp->uRefNum = 0xFF;
		}
		/* Check if the incoming RRLP data is Measure Position Request */
		else if ( pRrlp->uRrlpChoiceTag == C_RRLP_CHOICE_MSRPOSREQ)
		{
			TNAVC_cmdParams   *pNavcCmd;
			pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);
                        if (pNavcCmd)
                        {
                                pNavcCmd->tInjectAssist.tAssistance.ctrl_assistance_inject_union = GPSC_ASSIST_COMPL_SET;
                                sendAssistanceToNavc (pRrlp, pNavcCmd);
                        }
			if (pRrlp->uPosMethod == NAVC_RRLP_POSITION_METHOD_GPS)
			{
				uErrorReason = C_RRLP_LOC_REASON_GOOD;
				bRrlpLocReq = MCP_TRUE;  /* Honour the RRLP Measure Position Request from SMLC.*/
			}
			else
			{
				/* The location error occurs if the Position Method type is other then GPS */
				uErrorReason = C_RRLP_LOC_REASON_REQ_MTD_US;

				sendRrlpResponse (pRrlp,
								  &tLocFixResponse,
								  NULL,
								  C_RRLP_REP_LOCERR,
								  uErrorReason);

				rrlpDataReset (pRrlp);
			}
			if(!pNavcCmd)
                        {
                                MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("processRrlpMsg: allocation failed!\n"));
				return RES_ERROR;
			}
		}
		/* Check if the incoming RRLP data is Protocol Error message.*/
		else if (pRrlp->uRrlpChoiceTag == C_RRLP_CHOICE_PROTCOL_ERR )
		{

			/* Stop the current location request (set during previous location fix request) */
			SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_WARN_UNKWN_GPS_MSG,nav_sess_getCount(0),"#Unknown GPS message received,  discard");
			sendLocFixStop (pRrlp);
		}
		eRes = RES_OK;
	}
#if 0
	else
	{
		/* Returns false if the bits decoded is not equal to the number of bits recieved.
		 * The MS sends a Protocol Error C_RRLP_PROTERR_INCORRECT_DATA to SMLC.
		 */
		eRes = RES_ERROR;
		return eRes;
	}
#endif

	/* Check if no more assistance data is expected and
	 * incoming Measure Position Request is honoured
	 */
	if (!pRrlp->bMoreData && bRrlpLocReq)
	{
		EMcpfRes res;

		res = checkAssistDataForLocFix (pRrlp);

		if (res == RES_OK)
		{
			sendLocFixReq (pRrlp);
		}
		else
		{
			/* Send a Location response with Location assistance data missing error */

			sendRrlpResponse (pRrlp,
							  &tLocFixResponse,
							  NULL,
							  C_RRLP_REP_LOCERR,
							  C_RRLP_LOC_REASON_GPS_LCA_MIS);

			/* Initialize the structure holding assistance data from SMLC. */
			rrlpDataReset (pRrlp);
		}
	}

	return eRes;
}

/**
 * \fn     checkAssistDataForLocFix
 * \brief  Checks received assistance data
 *
 * Checks received assistance data by analyzing Wishlist bit map
 * depending on requested method type
 *
 * \note
 * \param	pRrlp     - pointer to RRLP object
 * \return  result of operation: OK - all assistance data received or
 *          Error - assistance data is missed
 * \sa      rrlp_decode
 */
static EMcpfRes checkAssistDataForLocFix(TRrlp *pRrlp)
{
	UNREFERENCED_PARAMETER(pRrlp);

	return RES_OK;		/* GPSC issues assistance request (not RRLP module) */

#if 0
	McpU16 		uFilteredWishlist;
	EMcpfRes    eRes;


	if (pRrlp->uMethodType == C_SMLC_COMPUTED)
	{
		/* Checks if the madatory assistance data for MS Assisted mode is received */
		uFilteredWishlist = (McpU16)(pRrlp->uWishlist & (GPSC_REQ_AA | GPSC_REQ_TIME));
	}
	else
	{	/* Checks if the madatory assistance data for MS Based mode is received */
		uFilteredWishlist = (McpU16)(pRrlp->uWishlist & (GPSC_REQ_LOC | GPSC_REQ_TIME | GPSC_REQ_NAV));
	}

	/* Reset all other assistance data bit mask to zero */
	uFilteredWishlist &= ~(GPSC_REQ_ALMANAC | GPSC_REQ_UTC | GPSC_REQ_IONO | GPSC_REQ_DGPS | GPSC_REQ_RTI);

	if ((pRrlp->uAvailabilityFlags & uFilteredWishlist) == uFilteredWishlist)
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
 * Construct session ID from RRLP reference number, source task & queue IDs and
 * RRLP protocol ID, send MSBASED or MSASSISTED location request according to
 * requeste method type.
 *
 * \note
 * \param	pRrlp     - pointer to RRLP object
 * \return  void
 * \sa      rrlp_decode
 */
static void sendLocFixReq(TRrlp *pRrlp)
{
	TNAVC_cmdParams   *pNavcCmd;
	McpU32			  uSesId = 0;


	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);

	if (pNavcCmd)
	{
		EMcpfRes 	eRes;
		TNAVC_reqLocFix *pLocFixReq = &pNavcCmd->tReqLocFix;

		/* set the session ID here based on the protocol */

		uSesId = NAVCD_SESSION_REF_NUM_SET(uSesId, 0); /* 0 to 7 bit set as 0*/
		uSesId = NAVCD_SESSION_PROT_ID_SET(uSesId, NAVCD_PROT_RRLP); /* set 8 to 15 bit as RRLP Protocol*/
		pRrlp->uSessionId = uSesId;
		pLocFixReq->loc_fix_session_id = uSesId;

		/* Set which list bit flags for required assistance data */
		switch (pRrlp->uMethodType)
		{
		case ENAVC_RRLP_POS_METHOD_TYPE_MS_ASSISTED:
			pLocFixReq->loc_fix_mode = GPSC_FIXMODE_MSASSISTED;
			pLocFixReq->loc_fix_result_type_bitmap = GPSC_RESULT_PROT;
			break;

		case ENAVC_RRLP_POS_METHOD_TYPE_MS_BASED:
			pLocFixReq->loc_fix_mode = GPSC_FIXMODE_MSBASED;
			pLocFixReq->loc_fix_result_type_bitmap = GPSC_RESULT_PROT;
			break;

		case ENAVC_RRLP_POS_METHOD_TYPE_MS_BASED_PREF:
			pLocFixReq->loc_fix_mode = GPSC_FIXMODE_MSBASED;
			pLocFixReq->loc_fix_result_type_bitmap = GPSC_RESULT_PROT;
			break;

		case ENAVC_RRLP_POS_METHOD_TYPE_MS_ASSISTED_PREF:
			pLocFixReq->loc_fix_mode = GPSC_FIXMODE_MSASSISTED;
			pLocFixReq->loc_fix_result_type_bitmap = GPSC_RESULT_PROT;
			break;

		default:
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("methodType_decode unknown methodType=%u !\n", pRrlp->uMethodType));
			break;
		}

		pLocFixReq->loc_fix_max_ttff = pRrlp->uMeasureResponseTime + MAX_TTFF_OFFSET;
		pLocFixReq->loc_sess_type=GPSC_SESSTYPE_LP;

		pLocFixReq->loc_fix_num_reports = 1; /* TODO: support multissesion mode if configured */
		pLocFixReq->loc_fix_period 	   = 1;

		/* for time out and accuracy QOP*/

	    pLocFixReq->loc_fix_qop.horizontal_accuracy = pRrlp->uAccuracy;
        pLocFixReq->loc_fix_qop.max_response_time = pRrlp->uMeasureResponseTime - NAVCD_NETWORK_DELAY; /*2 sec less to accomodate network delay*/


		//elements related wifi/sensor pos fix
	   pLocFixReq->loc_fix_pos_type_bitmap =0;

		MCPF_REPORT_INFORMATION(pRrlp->hMcpf, NAVC_MODULE_LOG, ("RRLP: NoOfRep: %d, MaxTTFF: %d, MaxRspTime: %d",
									pLocFixReq->loc_fix_num_reports,
									pLocFixReq->loc_fix_max_ttff,
									pLocFixReq->loc_fix_qop.max_response_time));

		/* ask for the location data */
		eRes = mcpf_SendMsg (pRrlp->hMcpf,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID ,
							 pRrlp->eSrcTaskId,
							 pRrlp->uSrcQId,
							 NAVC_CMD_REQUEST_FIX,
							 sizeof(pNavcCmd->tReqLocFix),
							 0,
							 pNavcCmd);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pRrlp->hMcpf, pNavcCmd);
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("sendLocFixReq: mcpf_SendMsg failed!\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("sendLocFixReq: allocation failed!\n"));
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
 * \param	pRrlp  			- pointer to RRLP object
 * \param	TNAVC_cmdParams - NAVC assistance command to send
 * \return  void
 * \sa
 */
static void sendAssistanceToNavc (TRrlp *pRrlp, TNAVC_cmdParams *pNavcCmd)
{
	EMcpfRes		  eRes;

	eRes = mcpf_SendMsg (pRrlp->hMcpf,
						 TASK_NAV_ID,
						 NAVC_QUE_CMD_ID ,
						 pRrlp->eSrcTaskId,
						 pRrlp->uSrcQId,
						 NAVC_CMD_INJECT_ASSISTANCE,
						 sizeof(pNavcCmd->tInjectAssist),
						 0,
						 pNavcCmd);

	MCP_HAL_OS_SleepMicroSec(1);

	if (eRes != RES_OK)
	{
		mcpf_mem_free_from_pool (pRrlp->hMcpf, pNavcCmd);
		MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("sendAssistanceToNavc: mcpf_SendMsg failed!\n"));
	}
}

/**
 * \fn     sendLocFixStop
 * \brief  Send NAVC location fix stop command
 *
 *  Allocates and sends NAVC location fix stop command to NAVC Task commnad queue
 *
 * \note
 * \param	pRrlp - pointer to RRLP object
 * \return  void
 * \sa
 */
static void sendLocFixStop (TRrlp *pRrlp)
{
	TNAVC_cmdParams *pNavcCmd;
	EMcpfRes		 eRes;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tStopLocFix.uSessionId = pRrlp->uSessionId;

		eRes = mcpf_SendMsg (pRrlp->hMcpf,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID ,
							 pRrlp->eSrcTaskId,
							 pRrlp->uSrcQId,
							 NAVC_CMD_STOP_FIX,
							 sizeof(pNavcCmd->tStopLocFix),
							 0,
							 pNavcCmd);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pRrlp->hMcpf, pNavcCmd);
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("sendLocFixStop: mcpf_SendMsg failed!\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("sendLocFixStop: allocation failed!\n"));
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
static void injectGpsTimeToNavc (TRrlp *pRrlp, TNAVCD_GpsTime *pGpsTime)
{
	TNAVC_cmdParams *pNavcCmd;
	EMcpfRes		 eRes;

	pNavcCmd = (TNAVC_cmdParams*) mcpf_mem_alloc_from_pool (pRrlp->hMcpf, pRrlp->hCmdPool);

	if (pNavcCmd)
	{
		pNavcCmd->tSetTow.tTimeAssist 		= pGpsTime->tTime;
		pNavcCmd->tSetTow.eTimeMode   		= pGpsTime->tTime.time_accuracy;
		pNavcCmd->tSetTow.uSystemTimeInMsec = pGpsTime->uSystemTimeMs;

		eRes = mcpf_SendMsg (pRrlp->hMcpf,
							 TASK_NAV_ID,
							 NAVC_QUE_CMD_ID ,
							 pRrlp->eSrcTaskId,
							 pRrlp->uSrcQId,
							 NAVC_CMD_SET_TOW,
							 sizeof(pNavcCmd->tSetTow),
							 0,
							 pNavcCmd);

		if (eRes != RES_OK)
		{
			mcpf_mem_free_from_pool (pRrlp->hMcpf, pNavcCmd);
			MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("injectGpsTimeToNavc: mcpf_SendMsg failed!\n"));
		}
	}
	else
	{
		MCPF_REPORT_ERROR(pRrlp->hMcpf, RR_MODULE_LOG, ("injectGpsTimeToNavc: allocation failed!\n"));
	}
}

/**
 * \fn     rrlpDataReset
 * \brief  Reset RRLP object state
 *
 * Reset (zero) RRLP object fields
 *
 * \note
 * \param	pRrlp     - pointer to RRLP object
 * \return  void
 * \sa      rrlp_decode
 */
static void rrlpDataReset (TRrlp *pRrlp)
{
	handle_t	hMcpf, hCmdPool;
	EmcpTaskId  eSrcTaskId;
	McpU8 		uIdx, uSrcQId;

	/* Store initialized handlers to be restored after setting memory to zero */
	hMcpf 	 	= pRrlp->hMcpf;
	hCmdPool 	= pRrlp->hCmdPool;
	eSrcTaskId 	= pRrlp->eSrcTaskId;
	uSrcQId		= pRrlp->uSrcQId;

	mcpf_mem_zero (hMcpf, pRrlp, sizeof(TRrlp));

	/* Restore initialized handlers */
	pRrlp->hMcpf 		= hMcpf;
	pRrlp->hCmdPool 	= hCmdPool;
	pRrlp->eSrcTaskId 	= eSrcTaskId;
	pRrlp->uSrcQId    	= uSrcQId;

	/* Set mandatory bits for both modes: MS-Based and MS-Assisted */
	pRrlp->uWishlist = GPSC_REQ_AA | GPSC_REQ_LOC | GPSC_REQ_TIME | GPSC_REQ_NAV;


	/* If SMLC doesn't indicate more assistance data to come, it means
	 * no more to come and will be safe to inject whatever has been received,
	 * if any. SnapTrack SMLC will always indicate whether or not more data is
	 * coming, but this is optional by the GSM standard.
	 * For non-SnapTrack but compliant SMLCs, if the optional field is absent,
	 * it means no more to come. So we set default to being FALSE and
	 * set it to TRUE if that optional field in SMLC indicates so
     */
	pRrlp->bMoreData = FALSE;

	pRrlp->tGpsTime.tTime.gps_week = C_GPS_WEEK_UNKNOWN;

	for (uIdx=0; uIdx<C_MAX_SVS_ACQ_ASSIST; uIdx++)
	{
		pRrlp->tAcquisitionAssist.tAcquisElement[uIdx].uDoppler1   = 0xFF;
		pRrlp->tAcquisitionAssist.tAcquisElement[uIdx].uDopplerUnc = 0xFF;
		pRrlp->tAcquisitionAssist.tAddAngle[uIdx].uAzimuth   	   = 0xFF;
		pRrlp->tAcquisitionAssist.tAddAngle[uIdx].uElevation 	   = 0xFF;
	}

	pRrlp->uRefNum 	   = 0xFF;      		/* no RRLP session             */
	pRrlp->uMethodType = 0xFF;    			/* indicating no PosInstruct   */
	pRrlp->uAccuracy   = 0xFFFF;    			/* indicating no accuracy      */
	pRrlp->uEnvironmentCharacter = 0xFF;    /* indicating no Env. charact. */
}

/**
 * \fn     rrlp_loc_info_encode
 * \brief  Encode location information
 *
 *  This function constructs the location information in an RRLP compliant
 *  format, using information in the position structure of RRLP object
 *  which is just updated with the new fix event that triggered the entering
 *  of this function.
 *
 * \note
 * \param	pRrlp      - pointer to RRLP object
 * \param	p_zLocInfo - pointer to location info structure compliant to RRLP standard
 * \param	pMsgBuf    - pointer to output message buffer containing encoded message
 * \param	pBitOffset - pointer to bit offset from start of message buffer to put
 *                       encoded RRLP fields, the value is incremented by written
 *                       RRLP fields bit length
 * \return  void
 * \sa
 */
static void rrlp_loc_info_encode (TRrlp 		*pRrlp,
								  LocInfo       *p_zLocInfo,
								  McpU8         *pMsgBuf,
								  McpU32   	    *pBitOffset)
{

	EllipAltUncEllip *p_zEllipAltUncEllip = &p_zLocInfo->z_PosEstimate.z_EllipAltUncEllip;
	EllipUncEllip *p_zEllipUncEllip = &p_zLocInfo->z_PosEstimate.z_EllipUncEllip;

	UNREFERENCED_PARAMETER(pRrlp);

	mcpf_putBits (pMsgBuf, pBitOffset, p_zLocInfo->u_OptParam_R1, 1);

	mcpf_putBits (pMsgBuf, pBitOffset, p_zLocInfo->u_RefFrame_R16, 16);

	if (p_zLocInfo->u_OptParam_R1)
	{
		mcpf_putBits (pMsgBuf, pBitOffset, p_zLocInfo->q_GpsTow_R24, 24);
	}

	mcpf_putBits (pMsgBuf, pBitOffset, p_zLocInfo->u_FixType_R1, 1);

	/* start PosEstimate. Currently only Ellipsoid Point with Altitude and
	 * Uncertainty Ellipsoid is supported. The following IF is thus not
	 * needed, but is here for future expansion
	 */
	if ( (p_zEllipAltUncEllip->u_ShapeCode<<4 & 0xF0) == 0x90 )
	{

		/* length of the ext-geographicalInformation octet string.
		 * Note range is (1..20), so while the length is 14, pack it with value of 13.
		 */
		mcpf_putBits (pMsgBuf, pBitOffset, 13, 5);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_ShapeCode << 4, 8);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_Lat0, 8);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_Lat1, 8);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_Lat2, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_Long0, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_Long1, 8);

        mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_Long2, 8);

        mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_Alt0, 8);

        mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_Alt1, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_UncMajor, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_UncMinor, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_OrientMajor, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_UncAlt, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipAltUncEllip->u_Confidence, 8);
	}

	if ( (p_zEllipUncEllip->u_ShapeCode<<4 & 0xF0) == 0x30 )
	{

		/* length of the ext-geographicalInformation octet string.
		 * Note range is (1..20), so while the length is 11, pack it with value of 10.
		 */
		mcpf_putBits (pMsgBuf, pBitOffset, 10, 5);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zEllipUncEllip->u_ShapeCode << 4, 8);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zEllipUncEllip->u_Lat0, 8);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zEllipUncEllip->u_Lat1, 8);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zEllipUncEllip->u_Lat2, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipUncEllip->u_Long0, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipUncEllip->u_Long1, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipUncEllip->u_Long2, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipUncEllip->u_UncMajor, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipUncEllip->u_UncMinor, 8);

		mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipUncEllip->u_OrientMajor, 8);

        mcpf_putBits ( pMsgBuf, pBitOffset, p_zEllipUncEllip->u_Confidence, 8);
    }
}

/**
 * \fn     gps_msr_info_construct
 * \brief  Fill RRLP GPS measurement information element
 *
 *  This function constructs gpsMsrInfo in an RRLP compliant format, using
 *  information in the Gps measurement of RRLP object which is just updated
 *  with the new fix event that triggered the entering of this function.
 *
 * \note
 * \param	pRrlp      - pointer to RRLP object
 * \param	p_zGpsMsrInfo    - pointer to GPS measurement info
 * \param	loc_fix_response - pointer to location fix response structure
 * \return  void
 * \sa
 */
static void gps_msr_info_construct(TRrlp 		*pRrlp,
								   GpsMsrInfo 	*p_zGpsMsrInfo,
								   T_GPSC_loc_fix_response *loc_fix_response)
{

    GpsMsrSetElement*           p_zGpsMsrSetElement =
		&p_zGpsMsrInfo->z_GpsMsrSetElement;
    GpsMsrList*                 p_zGpsMsrList =
		&p_zGpsMsrSetElement->z_GpsMrsList;
    GpsMsrElement*              p_zGpsMsrElement;

	T_GPSC_loc_fix_response_union* p_zLocFixRespUnion = &loc_fix_response->loc_fix_response_union;
	T_GPSC_prot_measurement* p_zRrlpMeasInfo = &p_zLocFixRespUnion->prot_measurement;

    McpU8 uIdx;

    p_zGpsMsrInfo->u_Length_R2 = 0; /* only support 1 set  */
    p_zGpsMsrSetElement->u_OptPream_R1 = 0; /* refFrame not supported   0 */


											/* Now, at p_zMeasReport->q_FCount,
	GpsTime = p_zMeasGpsTime->q_GpsMsec - p_zMeasGpsTime->f_ClkTimeBias **/

    /* we report TOW (msec into week) of the measurement moment */

     p_zGpsMsrSetElement->q_GpsTow_R24 = (McpU32)(p_zRrlpMeasInfo->gps_tow % C_FOUR_HRS_MSEC);


    /* fill out the measurement-dependent part of the message */
    /* find out how many SVs involved */

    p_zGpsMsrList->u_Length_R4 = p_zRrlpMeasInfo->c_sv_measurement;
    pRrlp->uSvNumInResp = p_zGpsMsrList->u_Length_R4;

    /* measurement for p_zGpsMsrList->u_Length_R4 is (1..16) */
    p_zGpsMsrList->u_Length_R4 -= 1;

    for (uIdx=0; uIdx < pRrlp->uSvNumInResp; uIdx++)
    {
		McpU8 u_SvId;
		T_GPSC_sv_measurement* p_zMeasPerSv;

		p_zMeasPerSv     = &p_zRrlpMeasInfo->sv_measurement[uIdx];

		p_zGpsMsrElement = &p_zGpsMsrList->z_GpsMsrElement[uIdx];  /* N_LCHAN */

		/* this is a good SV to report */

		/* SV id */
		u_SvId = p_zMeasPerSv->svid;
		p_zGpsMsrElement->u_SvId_R6 = u_SvId; /* don't apply (0...63) rule here
		as we use 0 for invalid */

		/* Cno. Add in noise figure and implementation loss (then round and
		switch from 0.1 to 1 dB-Hz/bit units) in order to reference it to
		the antenna input. */
		p_zGpsMsrElement->u_Cno_R6  = (U8)((p_zMeasPerSv->cno + 5) /10);

		/* doppler */
		/* f_Num = (FLT)p_zMeasPerSv->l_SvSpeed * (FLT)0.01
		* (FLT)C_MsToL1Hz * (FLT)(-1.0); */ /* Doppler in Hz */
		/* Per Richard, adding frequency bias to calibrate doppler : */
		p_zGpsMsrElement->w_Doppler_R16 = (McpU16)(p_zMeasPerSv->doppler + 32768);

		p_zGpsMsrElement->q_Msec = 0; /* not to be reported,
									  only for integration
		debug purposes */

		p_zGpsMsrElement->d_AdjSubMsec = 0;

		p_zGpsMsrElement->w_WholeChips_R10 = p_zMeasPerSv->whole_chips; /* whole chips in
		msec, U10 */

		if ( p_zGpsMsrElement->w_WholeChips_R10 > 1022 )
		{
			MCPF_REPORT_ERROR(pRrlp->hMcpf,
							  RR_MODULE_LOG,
							  ("gps_msr_info_construct invalid WholeChip=%u !\n",
							   p_zGpsMsrElement->w_WholeChips_R10));
		}

		/* fraction of a chip: multiple of 1/1024 chips */
		p_zGpsMsrElement->w_FracChips_R11 = p_zMeasPerSv->frac_chips ;

		/* multipath indicator */
		p_zGpsMsrElement->u_MpathIndic_R2 = (McpU8)p_zMeasPerSv->multipath_indicator; /* not used currently */

		/* PseudoRange RMS error: converted from SV time uncertainty */
		p_zGpsMsrElement->u_PseuRangeRMS_R6 = (U8)(p_zMeasPerSv->pseudorange_rms_error);

    }
}


/**
 * \fn     rrlp_gps_msr_encode
 * \brief  Fill RRLP GPS measurement information element
 *
 *  This function constructs gpsMsrInfo in an RRLP compliant format, using
 *  information in the Gps measurement database which is just updated
 *  with the new fix event that triggered the entering of this function.
 *
 * \note
 * \param	pRrlp      - pointer to RRLP object
 * \param	p_zGpsMsrInfo - pointer to lGPS measurement information  structure
 *                       compliant to RRLP standard
 * \param	uGoodSvs   - number of good satellites
 * \param	pMsgBuf    - pointer to output message buffer containing encoded message
 * \param	pBitOffset - pointer to bit offset from start of message buffer to put
 *                       encoded RRLP fields, the value is incremented by written
 *                       RRLP fields bit length
 * \return  void
 * \sa
 */
static void rrlp_gps_msr_encode (TRrlp 			*pRrlp,
							     GpsMsrInfo    	*p_zGpsMsrInfo,
								 McpU32         uGoodSvs,
								 McpU8         	*pMsgBuf,
								 McpU32   	    *pBitOffset)
{
	GpsMsrSetElement *p_zGpsMsrSetElement = &p_zGpsMsrInfo->z_GpsMsrSetElement;
	GpsMsrList       *p_zGpsMsrList       = &p_zGpsMsrSetElement->z_GpsMrsList;
	GpsMsrElement    *p_zGpsMsrElement;
	McpU8 			 uIdx;
	McpU8 			u_Byte;
 	McpS8 			b_i;

	/* to avoid warning, */
	UNREFERENCED_PARAMETER(pRrlp);
	UNREFERENCED_PARAMETER(uGoodSvs);

	/** gps-MrsSetElement occurances */

	mcpf_putBits (pMsgBuf, pBitOffset, p_zGpsMsrInfo->u_Length_R2, 2);

	/*** gps-Msr-SetElement Seq ***/

	/* Optional parameter preamble: for refFrame, assume absent */

	mcpf_putBits (pMsgBuf, pBitOffset, p_zGpsMsrSetElement->u_OptPream_R1, 1);

	if (p_zGpsMsrSetElement->u_OptPream_R1) /* optional RefFrame */
	{
		mcpf_putBits ( pMsgBuf, pBitOffset, p_zGpsMsrSetElement->w_RefFrame_R16, 16);
	}

	/** GpsTow */
	 for (b_i=2; b_i>=0; b_i--)
  	{
    		u_Byte = (McpU8)(p_zGpsMsrSetElement->q_GpsTow_R24 >> (8 * b_i) );
		mcpf_putBits (pMsgBuf, pBitOffset, u_Byte, 8);
  	}


	mcpf_putBits (pMsgBuf, pBitOffset, p_zGpsMsrList->u_Length_R4, 4);

	/* here starts SV specific Gps-msrElement sequence */
	for (uIdx=0; uIdx < pRrlp->uSvNumInResp; uIdx++)
	{
		p_zGpsMsrElement = &p_zGpsMsrList->z_GpsMsrElement[uIdx];

		mcpf_putBits (pMsgBuf, pBitOffset, p_zGpsMsrElement->u_SvId_R6, 6);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zGpsMsrElement->u_Cno_R6, 6);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zGpsMsrElement->w_Doppler_R16, 16);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zGpsMsrElement->w_WholeChips_R10, 10);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zGpsMsrElement->w_FracChips_R11, 11);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zGpsMsrElement->u_MpathIndic_R2, 2);

		mcpf_putBits (pMsgBuf, pBitOffset, p_zGpsMsrElement->u_PseuRangeRMS_R6, 6);
	}
}

/**
 * \fn     ext_geo_loc_construct
 * \brief  Fill the RRLP external geographical location information
 *
 *  This function constructs the external geographical location information
 *  data type as defined in GSM 09.02.
 *
 * \note
 * \param	p_zLocInfo    - pointer to the structure that stores bytes that are RRLP ready,
 *                          filled by the function
 * \param	loc_fix_response - pointer to location fix response structure
 *                       compliant to RRLP standard
 * \param	u_PosShape - position shape
 * \param	pMsgBuf    - pointer to output message buffer containing encoded message
 * \return  operation result: OK or Error
 * \sa
 */
static EMcpfRes ext_geo_loc_construct (LocInfo					*p_zLocInfo,
                                       T_GPSC_loc_fix_response	*loc_fix_response,
                                       McpU8             		u_PosShape)
{
	McpU16 	w_Word;
	McpU32 	q_LongWord;
	McpS32 	l_LongWord;
	EMcpfRes eRes = RES_ERROR;
	T_GPSC_loc_fix_response_union  	*p_zLocFixRespUnion = &loc_fix_response->loc_fix_response_union;
	T_GPSC_prot_position 			*p_zPosInfo = &p_zLocFixRespUnion->prot_position;
	T_GPSC_ellip_alt_unc_ellip		*p_zRrlpPosInfo = &p_zPosInfo->ellip_alt_unc_ellip;


	/* this paragraph is relevant to RRLP only */
	p_zLocInfo->u_OptParam_R1 = 1; /* GpsTow present */
	p_zLocInfo->u_RefFrame_R16 = 0; /* not used */
	p_zLocInfo->q_GpsTow_R24 = p_zPosInfo->gps_tow % C_FOUR_HRS_MSEC;

	p_zLocInfo->u_FixType_R1 = (McpU8)p_zPosInfo->prot_fix_result;

	if ( u_PosShape == C_POS_SHAPE_ELLIP_ALT_UNC_ELLIP )
	{
		EllipAltUncEllip *p_zEllipAltUncEllip =
			&p_zLocInfo->z_PosEstimate.z_EllipAltUncEllip;


		p_zEllipAltUncEllip->u_ShapeCode = p_zRrlpPosInfo->shape_code; /* Octet1: shape */


		/******** latitude ****************/

		p_zEllipAltUncEllip->u_Lat0 = p_zRrlpPosInfo->latitude_sign; /* sign bit high for southern
		hemesphere */

		q_LongWord = p_zRrlpPosInfo->latitude;
		p_zEllipAltUncEllip->u_Lat2 = (McpU8)q_LongWord;
		p_zEllipAltUncEllip->u_Lat1 = (McpU8)(q_LongWord >> 8);
		p_zEllipAltUncEllip->u_Lat0 |= (McpU8)(q_LongWord >> 16);


		/********** longitude ************/

		l_LongWord = p_zRrlpPosInfo->longitude;
		p_zEllipAltUncEllip->u_Long2 = (McpU8)l_LongWord;
		p_zEllipAltUncEllip->u_Long1 = (McpU8)(l_LongWord >> 8);
		p_zEllipAltUncEllip->u_Long0 = (McpU8)(l_LongWord >> 16);


		/********* altitude ***************/

		p_zEllipAltUncEllip->u_Alt0 = 0;
		w_Word = p_zRrlpPosInfo->altitude;

		if ( p_zRrlpPosInfo->v_altitude_sign && (p_zRrlpPosInfo->altitude_sign == 1) )
		{
			/*** depth into earth condition */
			p_zEllipAltUncEllip->u_Alt0 = 0x80;
		}

		p_zEllipAltUncEllip->u_Alt1 = (McpU8)w_Word;
		p_zEllipAltUncEllip->u_Alt0 |= (McpU8)(w_Word >> 8);


		/********* uncertainty  *************/

		/*** AI2 limits the representation of horizontal uncs to 6553.5m
		and RRLP's limit is 1800 km, so no need to cap uncs here */

		/* RRLP uses K to discribe altitude unc:
		Altitude (meters) = C * ( (1+x) ** K - 1 ),
		where C = 10, x = 0.1. */

		/* East-West being semi-major */
		p_zEllipAltUncEllip->u_OrientMajor = p_zRrlpPosInfo->orient_major; /* from North, clockwise */
		p_zEllipAltUncEllip->u_UncMajor = p_zRrlpPosInfo->unc_major;
		p_zEllipAltUncEllip->u_UncMinor = p_zRrlpPosInfo->unc_minor;

		/*** AI2 limits the representation of vertical unc to 32767.5m
		and RRLP's limit is 990.5m, so we need to cap it here */

		/* RRLP uses K to discribe altitude unc:
		Altitude (meters) = C * ( (1+x) ** K - 1 ),
		where C = 45, x = 0.025. Note 1/45 = 0.022222222 */
		p_zEllipAltUncEllip->u_UncAlt = p_zRrlpPosInfo->unc_alt;

		p_zEllipAltUncEllip->u_Confidence = p_zRrlpPosInfo->confidence; /* we don't use it */

		eRes = RES_OK;
	}
	else if ( u_PosShape == C_POS_SHAPE_ELLIP_UNC_ELLIP)
	{
		EllipUncEllip *p_zEllipUncEllip =
			&p_zLocInfo->z_PosEstimate.z_EllipUncEllip;


		p_zEllipUncEllip->u_ShapeCode = p_zRrlpPosInfo->shape_code; /* Octet1: shape */


		/******** latitude ****************/

		p_zEllipUncEllip->u_Lat0 = p_zRrlpPosInfo->latitude_sign; /* sign bit high for southern
		hemesphere */

		q_LongWord = p_zRrlpPosInfo->latitude;
		p_zEllipUncEllip->u_Lat2 = (McpU8)q_LongWord;
		p_zEllipUncEllip->u_Lat1 = (McpU8)(q_LongWord >> 8);
		p_zEllipUncEllip->u_Lat0 |= (McpU8)(q_LongWord >> 16);


		/********** longitude ************/

		l_LongWord = p_zRrlpPosInfo->longitude;
		p_zEllipUncEllip->u_Long2 = (McpU8)l_LongWord;
		p_zEllipUncEllip->u_Long1 = (McpU8)(l_LongWord >> 8);
		p_zEllipUncEllip->u_Long0 = (McpU8)(l_LongWord >> 16);



		/********* uncertainty	*************/

		/*** AI2 limits the representation of horizontal uncs to 6553.5m
		and RRLP's limit is 1800 km, so no need to cap uncs here */

		/* RRLP uses K to discribe altitude unc:
		Altitude (meters) = C * ( (1+x) ** K - 1 ),
		where C = 10, x = 0.1. */

		/* East-West being semi-major */
		p_zEllipUncEllip->u_OrientMajor = p_zRrlpPosInfo->orient_major; /* from North, clockwise */
		p_zEllipUncEllip->u_UncMajor = p_zRrlpPosInfo->unc_major;
		p_zEllipUncEllip->u_UncMinor = p_zRrlpPosInfo->unc_minor;

		p_zEllipUncEllip->u_Confidence = p_zRrlpPosInfo->confidence; /* we don't use it */

		eRes = RES_OK;
	}
	else {

		eRes = RES_ERROR;
    }
	return eRes;
}


/**
 * \fn     locationErrorEncode
 * \brief  Fill the RRLP external geographical location information
 *
 *  This function constructs the external geographical location information
 *  data type as defined in GSM 09.02.
 *
 * \note
 * \param	pRrlp      - pointer to RRLP object
 * \param	pLocErr    - pointer to input structure with location error information
 * \param	loc_fix_response - pointer to location fix response structure
 *                       compliant to RRLP standard
 * \param	u_PosShape - position shape
 * \param	pMsgBuf    - pointer to output message buffer containing encoded message
 * \param  	pOffset    - bit ofset from message start where to append encoded RRLP fields,
 *                      increased by bit length of newly appended RRLP fields
 * \return  void
 * \sa
 */
static void locationErrorEncode (TRrlp			*pRrlp,
								 LocErr			*pLocErr,
								 McpU8 			*pMsgBuf,
								 McpU32 		*pBitOffset)
{
	McpU8	uIndx;

	UNREFERENCED_PARAMETER(pRrlp);

	mcpf_putBits (pMsgBuf, pBitOffset, pLocErr->u_ExtInd_R1, 1);

	mcpf_putBits (pMsgBuf, pBitOffset, pLocErr->u_OptParam_R1, 1);

	mcpf_putBits (pMsgBuf, pBitOffset, pLocErr->u_ExtIndLocReason_R1, 1);

	mcpf_putBits (pMsgBuf, pBitOffset, pLocErr->u_LocationReason_R4, 4);

	if (pLocErr->u_OptParam_R1)
	{
		McpU8 uData;

		mcpf_putBits (pMsgBuf, pBitOffset, pLocErr->z_AdditionAssistData.u_ExtContAdd, 1);

		mcpf_putBits (pMsgBuf, pBitOffset, pLocErr->z_AdditionAssistData.u_OptPreambleAdd, 2);

		mcpf_putBits (pMsgBuf, pBitOffset, pLocErr->z_AdditionAssistData.z_GpsAssistData.uAssistDataLen, 6);

		for( uIndx = 0; uIndx < pLocErr->z_AdditionAssistData.z_GpsAssistData.uAssistDataLen; uIndx++)
		{
			uData = pLocErr->z_AdditionAssistData.z_GpsAssistData.uAssistData[uIndx];
			mcpf_putBits (pMsgBuf, pBitOffset, uData, 8);
		}

	}

}


/**
 * \fn     loc_error_construct
 * \brief  Construct erorr response
 *
 *  Build RRLP position error response including error cause and missed
 * assistance data request
 *
 * \note
 * \param	pLocError  	- pointer to location error structure
 * \param	eCause 		- error cause/reason
 * \param	pRrrpLocErr - RRLP compliant error structure to fill
 * \return  void
 * \sa
 */
static void loc_error_construct (TNAVCD_LocError 	*pLocError,
								 ERrlp_LocReason 	eCause,
								 LocErr 			*pRrlpLocErr)
{
	AdditionAssistData  *pAdditionAssistData = &pRrlpLocErr->z_AdditionAssistData;
	GpsAssistData  		*pGpsAssistData 	 = &pAdditionAssistData->z_GpsAssistData;

	pRrlpLocErr->u_ExtInd_R1 = 0;
	pRrlpLocErr->u_ExtIndLocReason_R1 = 0;   /* LocationReason no extension */
	pRrlpLocErr->u_LocationReason_R4 = (McpU8) eCause;

	if(pLocError == NULL)
		return;
	if ((eCause == C_RRLP_LOC_REASON_GPS_ASS_MIS) && pLocError->pNavModel)
	{
		pRrlpLocErr->u_OptParam_R1 = 1;
		pAdditionAssistData->u_ExtContAdd = 0;
		pAdditionAssistData->u_OptPreambleAdd = 2;

		if (pLocError)
		{
			McpU16 	uWishList;
			McpU8	*pData;
			McpU8	uInd=0;
			McpU16  uGpsWeek;

			pData = pGpsAssistData->uAssistData;

			uWishList = (McpU16) (pLocError->uAssistBitMapMandatory | pLocError->uAssistBitMapOptional);

			/* Build Assitance Data complient to 3GPP TS 49.031 */

		//	*pData++ = GPS_ASSIST_DATA_REQ_IEI;	/* IEI */
		//	pData++;							/* skip length indicator to fill later */
			*pData++ = (McpU8) uWishList;		/* LS byte of bit map, GPSC bit map is 3GPP TS 49.031 complient */
			*pData++ = (McpU8)(uWishList >> 8); /* MS byte of bit map, GPSC bit map is 3GPP TS 49.031 complient */

			if ((uWishList & GPSC_REQ_NAV) && pLocError->pNavModel)
			{
				uGpsWeek = (McpU16) (pLocError->pNavModel->gps_week % 1024); /* 10 bits of GPS week */
				*pData++ = (McpU8) ((uGpsWeek >> 2) & 0xC0);    	/* GPS Week (bits 7-8 octet 5 and octet 6) */
				*pData++ = (McpU8) uGpsWeek;
				*pData++ = (McpU8) pLocError->pNavModel->nav_toe;	/* GPS_Toe (octet 7) */


				*pData  = (McpU8) (pLocError->pNavModel->nav_num_svs << 4);      /* NSAT (octet 8, bits 5-8)  */
				*pData |= (McpU8) (pLocError->pNavModel->nav_toe_limit & 0x0F);  /* T-Toe limit (octet 8, bits 1-4) */
				pData++;

				for (uInd=0; uInd < pLocError->pNavModel->nav_num_svs; uInd++)
				{
				   *pData++ = (McpU8) (pLocError->pNavModel->nav_data[uInd].svid & 0x3F);  /* SatID 6 bits */
				   *pData++ = (McpU8) (pLocError->pNavModel->nav_data[uInd].iode);    	   /* IODE  8 bits */
				}
			}
			pGpsAssistData->uAssistDataLen = (McpU8) (pData - pGpsAssistData->uAssistData);
		//	pGpsAssistData->uAssistData[1] = (McpU8) (pGpsAssistData->uAssistDataLen - 2); /* not including IEI and Len indicator octests */
		}
		else
		{
			pGpsAssistData->uAssistDataLen = 0;
		}
	}
	else
	{
		pRrlpLocErr->u_OptParam_R1 = 0;
	}
}


#ifdef MOBILE_ID_EXTENSION_CONTAINER

/**
 * \fn     RrlpImsiPacker
 * \brief  Packs IMSI information
 *
 *  Pack the IMSI formation for sending to SIA in a
 *  format that is consistent with the format specified by GSM specs.
 *
 * \note
 * \param	p_Imsi - pointer to the highest element of the data array holding IMSI,
 *                   which holds the left-most element of IMSI
 * \param	p_B    - pointer to the data array in the structure in which the command
 *                   is being built
 * \return  the updated value of the pointer that is in the parameter list
 * \sa
 */
static McpU8 *RrlpImsiPacker(McpU8 *p_Imsi, McpU8 *p_B)
{
  McpU8 u_1, u_Dig1, u_Dig2;
  McpU8 u_i;

  for (u_i=0; u_i<8; u_i++)
  {
    u_1 = *p_Imsi; //transmit Digit1 and Digit2 of IMSI
    u_Dig2 = (McpU8)(u_1 & 0x0F);
    u_Dig1 = (McpU8)(u_1 >> 4);
    u_1 = (McpU8)(u_Dig1 | (u_Dig2 << 4));
    *p_B++ = u_1;
    p_Imsi--;
  }

  return p_B;
}


/**
 * \fn     extensionContainerEncode
 * \brief  Packs IMSI information
 *
 *  Pack the IMSI formation for sending to SIA in a
 *  format that is consistent with the format specified by GSM specs.
 *
 * \note
 * \param	pRrlp      - pointer to RRLP object
 * \param	p_zMobileIdExt - pointer to input structure with mobile extension information
 * \param	pMsgBuf    - pointer to output message buffer containing encoded message
 * \param  	pOffset    - bit ofset from message start where to append encoded RRLP fields,
 *                      increased by bit length of newly appended RRLP fields
 * \return  void
 * \sa
 */

static McpU8 uDummyIMSI[8];	/* TODO: use from configuration */

static void extensionContainerEncode (TRrlp		*pRrlp,
									  PrivExt	*p_zMobileIdExt,
									  McpU8 	*pMsgBuf,
									  McpU32 	*pBitOffset)
{
	McpU8 i;

	UNREFERENCED_PARAMETER(pRrlp);

	p_zMobileIdExt->u_ExtensionCount = 1;
	p_zMobileIdExt->u_ExtensionIndicator = 0;
	p_zMobileIdExt->u_ParameterBitmap = 2;

	p_zMobileIdExt->z_Test.u_ExtensionMarker = 0;
	p_zMobileIdExt->z_Test.u_LengthDeterminant = 9;
	p_zMobileIdExt->z_Test.u_ObjectIDLength = 3;
	p_zMobileIdExt->z_Test.u_ObjecttID[0] = OBJECTID_MOBILE_ID_BYTE_0;
	p_zMobileIdExt->z_Test.u_ObjecttID[1] = OBJECTID_MOBILE_ID_BYTE_1;
	p_zMobileIdExt->z_Test.u_ObjecttID[2] = OBJECTID_MOBILE_ID_BYTE_2;
	p_zMobileIdExt->z_Test.u_ParameterBitmap = 1;   					/* Private Extension Only */
	p_zMobileIdExt->z_Test.u_TestExtensionRevision = 0;

	RrlpImsiPacker (&uDummyIMSI[7], &p_zMobileIdExt->z_Test.u_TransposedImsi[0]);

	p_zMobileIdExt->z_Test.u_PaddingBits = 1;

   /*  Emit Start of Extension Container
	*  Extension Indicator Preamble
	*  Optional Parameter Bitmap
	*  Number of Private Extensions
	*/

	mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->u_ExtensionIndicator, 1);

	mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->u_ParameterBitmap, 2);

	mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->u_ExtensionCount-1, 4);

   /*  Emit Private Extension : Test Extension
	*  Optional Parameter Bitmap
	*  Object Identifier Length
	*  Object Identifier
	*/

	mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->z_Test.u_ParameterBitmap, 1);

	mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->z_Test.u_ObjectIDLength, 8);

	mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->z_Test.u_ObjecttID[0], 8);

	mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->z_Test.u_ObjecttID[1], 8);

	mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->z_Test.u_ObjecttID[2], 8);

   /*  Emit Extension Type
	*  Length Determinant
	*  Extension Marker
	*  Test Extension Revisions
	*  IMSI Value
	*  Padding
	*/

	mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->z_Test.u_LengthDeterminant, 8);

	mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->z_Test.u_ExtensionMarker, 1);

	mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->z_Test.u_TestExtensionRevision, 6);

	for (i=0; i<8; i++)
	{
		mcpf_putBits (pMsgBuf, pBitOffset, p_zMobileIdExt->z_Test.u_TransposedImsi[i] , 8);
	}

	mcpf_putBits (pMsgBuf, pBitOffset, 0, p_zMobileIdExt->z_Test.u_PaddingBits);
}
#endif /* MOBILE_ID_EXTENSION_CONTAINER */

#ifdef RRLP_DEBUG
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

static void dumpMsg (TRrlp *pRrlp, McpU8 * pMsgBuf, McpU16 uMsgLength)
{
	McpU32	uIndx;
	McpU8   uBitString[10];
        LOGD(" INSIDE DUMPMSG \n");
	if((uMsgLength & 0x8000)!=0x8000)
        {
		LOGD (" INSIDE IF OF dumpmsg \n");
		return;
        }
	else
		{
		uMsgLength &= 0x7fff;

	for (uIndx=0; uIndx < uMsgLength; uIndx++)
	  {
               LOGD ("\n insde for loop of else \n");
		bitString (pMsgBuf[uIndx], uBitString);
		MCPF_REPORT_INFORMATION( pRrlp->hMcpf, RR_MODULE_LOG, ("[%3u %5u-%5u] = %02x  %s\n", uIndx, uIndx*8, uIndx*8+7, pMsgBuf[uIndx], uBitString));
	  }
		}
}
#endif
