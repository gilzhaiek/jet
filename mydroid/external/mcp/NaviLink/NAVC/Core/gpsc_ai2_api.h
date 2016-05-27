/*
 * $Header: /FoxProjects/FoxSource/win32/LocationManager 1/10/04 7:53p Lleirer $
 ******************************************************************************
 *  Copyright (C) 1999 SnapTrack, Inc.

 *

 *                  SnapTrack, Inc.

 *                  4040 Moorpark Ave, Suite 250

 *                  San Jose, CA  95117

 *

 *     This program is confidential and a trade secret of SnapTrack, Inc. The

 * receipt or possession of this program does not convey any rights to

 * reproduce or disclose its contents or to manufacture, use or sell anything

 * that this program describes in whole or in part, without the express written

 * consent of SnapTrack, Inc.  The recipient and/or possessor of this program

 * shall not reproduce or adapt or disclose or use this program except as

 * expressly allowed by a written authorization from SnapTrack, Inc.

 *

 *

 ******************************************************************************/


 /*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*


   L O C A T I O N   S E R V I C E S   M A N A G E R   M O D U L E


  Copyright (c) 2002 by QUALCOMM INCORPORATED. All Rights Reserved.

 

 Export of this technology or software is regulated by the U.S. Government.

 Diversion contrary to U.S. law prohibited.

 *====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*/


/*********************************************************************************

                                   TI GPS Confidential

*********************************************************************************/
/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName			:	gpsc_ai2_api.h
 *
 * Description     	:
 * This file contains the api to the ai2 library routines. The routines
 * are used to construct ai2 messages for transmission and to decode
 * message from received serial bytes.
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_AI2_API_H
#define _GPSC_AI2_API_H


/*
 * Include files
*/

#include "gpsc_types.h"
#include "gpsc_consts.h"

/*
 * Constant definitions
*/
enum
{
  AI_REQ_PING,
  AI_REC_ACTION=2,
  AI_REQ_SER_CFG,
  AI_INJ_SER_CFG,
  AI_REQ_EVENT_CFG,
  AI_INJ_EVENT_CFG,
  AI_REQ_REC_CFG,
  AI_INJ_REC_CFG,
  AI_REQ_CLOCK_REP,
  AI_INJ_TIME_EST,
  AI_REQ_POS,
  AI_INJ_POS_EST,
  AI_REQ_POS_STAT,
  AI_REQ_MEAS,
  AI_REQ_MEAS_STAT,
  AI_REQ_EPHEM,
  AI_INJ_EPHEM,
  AI_REQ_ALM,
  AI_INJ_ALM,
  AI_REQ_IONO,
  AI_INJ_IONO,
  AI_REQ_UTC,
  AI_INJ_UTC,
  AI_REQ_SV_HEALTH,
  AI_INJ_SV_HEALTH,
  AI_REQ_EPHEM_STAT,
  AI_REQ_ALM_STAT,
  AI_INJ_PRED_SV_DATA,
  AI_INJ_ALTITUDE,
  AI_DEL_SV_INFO,
  AI_INJ_SV_STEERING,
  AI_INJ_FREQ_EST,
  AI_INJ_DGPS_CORR,
  AI_REQ_SV_DIR,
  AI_INJ_SV_DIR,
  AI_INJ_SV_TIME_DIFF,
  AI_INJ_CE_OSC_PARAMS
};

/*TI custom requests*/


enum
{
  AI_REQ_BLF_CFG = 55,
  AI_REQ_BLF_STS,
  AI_REQ_BLF_DUMP = 58
};




enum
{
    AI_CLAPP_INJ_WLAN_ASSIST_INFO = 210,
	AI_CLAPP_INJ_CUSTOMER_CONFIG_EXTENDED = 221,
	AI_CLAPP_REQ_USERDEF_CW_TEST = 198,
    AI_CLAPP_REQ_SYNCTEST =199,
	AI_CLAPP_REQ_VERSION = 240,
	AI_CLAPP_PATCH_DWLD_CONTROL,
	AI_CLAPP_SET_DOWNLOAD_RECORD,
	AI_CLAPP_INJ_GPS_OPR_MODE,
	AI_CLAPP_REQ_POS_EXT,
	AI_CLAPP_REQ_ACQ_TEST=202,
	AI_CLAPP_REQ_SELF_TEST,
	AI_CLAPP_REQ_GPS_OSC_TEST,
	AI_CLAPP_REQ_RTC_OSC_TEST,
	AI_CLAPP_REQ_RAM_CHKSUM_TEST,
	AI_CLAPP_REQ_PREDEF_CW_TEST,
	AI_CLAPP_INJ_CLK_SLICER_CNTRL,
	AI_CLAPP_INJ_TIME_INJECT_TEST_CNTRL,
	AI_CLAPP_REQ_RF_REG_CONTENT,
	AI_CLAPP_INJ_RF_ADC_DRV_LVL,
	AI_CLAPP_INJ_RF_AGC_LVL,
	AI_CLAPP_INJ_RF_BASEBAND_AMP_GAIN,
	AI_CLAPP_REQ_RF_FILTER_RETUNE,
	AI_CLAPP_INJ_RF_TESTPOINT_OUTPUT_CNTRL,
	AI_CLAPP_INJ_RF_AGC_INT_TIME,
	AI_CLAPP_INJ_RF_IF_FILTER_CNTR_FREQ,
	AI_CLAPP_INJ_RF_INJ_RF_EXT_AGC,
	AI_CLAPP_INJ_RF_FILTER_RETUNE_INTVL,
	AI_CLAPP_REQ_CUSTOMER_CONFIG,
	AI_CLAPP_INJ_CUSTOMER_CONFIG,
	AI_CLAPP_INJ_TIME_STAMP_POL,
	AI_CLAPP_INJ_WATCHDOG_CNTRL,
	AI_CLAPP_INJ_AUTO_SLEEP_CNTRL,
	AI_CLAPP_INJ_PA_BLANK_CNTRL,
	AI_CLAPP_INJ_CNO_RF_FRONTEND_LOSS,
	AI_CLAPP_SET_MAX_CLK_ACC,
	AI_CLAPP_INJ_UART2_BAUD_RATE,
	AI_CLAPP_NMEA_OUTPUT_CONTROL,
	AI_CLAPP_SET_I2C_CONFIG =230,
	AI_CLAPP_INJECT_PRM_ADJUST,
	AI_CLAPP_LEAP_SEC_CONTROL=236,
	AI_CLAPP_INJ_CALIB_CONTROL,
	AI_CLAPP_INJ_PPS_CONTROL,
	AI_CLAPP_SET_RF_REG_CONTENT = 239,
  AI_CLAPP_INJ_COMM_PROT = 245,
	AI_CLAPP_INJ_BAUD_RATE = 250,
	AI_CLAPP_INJ_CUSTOM_PACKET = 251
};

/*TI custom packet - sub packet*/
enum
{
CW_RES_TYPE_1MHZ_IQ_SAMPLES=0,
CW_RES_TYPE_2KHZ_IQ_SAMPLES,
CW_RES_TYPE_WIDEBAND_FFT,
CW_RES_TYPE_NARROWBAND_FFT,
CW_RES_TYPE_STAT_REPORT,
CW_RES_TYPE_2MHZ_IQ_SAMPLES,
CW_RES_TYPE_8MHZ_IQ_SAMPLES,
CW_RES_TYPE_32MHZ_IQ_SAMPLES,
CW_RES_TYPE_32MHZ_IQ_SAMPLES_AFTER_NOTCH
};
/*TI custom packet - sub packet*/
enum
{
	AI_CLAPP_CUSTOM_SUB_PKT_INJ_RTC_TIME_INJECT_CTRL = 0,
	AI_CLAPP_CUSTOM_SUB_PKT_REQ_RTC_TIME_INJECT_CTRL,
	AI_CLAPP_CUSTOM_SUB_PKT_AGPS_REF_CLK_REQ_CTRL,
	AI_CLAPP_CUSTOM_SUB_PKT_REQ_GPIO_STATUS,
	AI_CLAPP_CUSTOM_SUB_PKT_INJ_GPIO_CTRL,
	AI_CLAPP_CUSTOM_SUB_PKT_RESET_GPIO_CTRL,
	AI_CLAPP_CUSTOM_SUB_PKT_CMD_ADC_CONN_TEST, /* not supported in GPS53xx*/
	AI_CLAPP_CUSTOM_SUB_PKT_INJ_CALIB_TIME_STAMP_PERIOD,
	AI_CLAPP_CUSTOM_SUB_PKT_REQ_MEAS_EXTD_REP,
	AI_CLAPP_CUSTOM_SUB_PKT_INJ_EXTN_LNA_GPS_CRYSTAL_CTRL,
	AI_CLAPP_CUSTOM_SUB_PKT_INJ_MAX_USER_VELOCITY,
	AI_CLAPP_CUSTOM_SUB_PKT_REQ_FAST_CALIB_TEST_MODE,
	AI_CLAPP_CUSTOM_SUB_PKT_INJ_ADV_POWER_MGMT_CONF, /* GPS5350 Only */
	AI_CLAPP_CUSTOM_SUB_PKT_INJ_HOST_WAKEUP_CONF,	/* GPS5350 Only) */
	AI_CLAPP_CUSTOM_SUB_PKT_INJ_SBAS_CTRL,	/* GPS5350 Only) */
	AI_CLAPP_CUSTOM_SUB_PKT_INJ_MOTION_MASK_SETTING = 15, /* GPS5350 Only),*/
	AI_CLAPP_CUSTOM_SUB_PKT_REQ_SINGLE_SHOT_HW_EVENT,
	AI_CLAPP_CUSTOM_SUB_PKT_REQ_SV_MEAS_DEL,
	AI_CLAPP_CUSTOM_SUB_PKT_INJ_TIMEOUT_CEP_INFO = 21,
	AI_CLAPP_CUSTOM_SUB_PKT_INJ_GPS_TCXO_LDO_VOLT_CTRL,
  AI_CLAPP_CUSTOM_SUB_PKT_GPS_SHUTDOWN_CTRL = 23,
  AI_CLAPP_CUSTOM_SUB_PKT_REF_CLK_REQ_RESP,
  AI_CLAPP_CUSTOM_SUB_PKT_INJ_WAKEUP_SEQ,
  AI_CLAPP_CUSTOM_SUB_PKT_INJ_SENSOR_ASSIST=0x20,
 AI_CLAPP_CUSTOM_SUB_PKT_MEM_RW = 0xD0,
  AI_CLAPP_CUSTOM_SUB_PKT_REQ_MOTION_MASK_SETTING =31
};

/* Enumerate Response Types */
enum
{
  AI_REP_PING,
  AI_REP_SER_CFG=2,
  AI_REP_EVENT_CFG,
  AI_REP_REC_CFG,
  AI_REP_CLOCK,
  AI_REP_POS,
  AI_REP_POS_STAT,
  AI_REP_MEAS,
  AI_REP_MEAS_STAT,
  AI_REP_EPHEM,
  AI_REP_ALM,
  AI_REP_IONO,
  AI_REP_UTC,
  AI_REP_SV_HEALTH,
  AI_REP_EPHEM_STAT,
  AI_REP_ALM_STAT,
  AI_REP_SV_DIR,
  AI_REQ_ASSIST_DATA,
  AI_REP_DIAG_STR,
  AI_REP_SYNCTEST_EVENT,
  AI_CW_TEST_RESULTS,
  AI_REP_MGP6200_TEST,        /* =22 */

  AI_REP_BLF_STATUS = 57,
  AI_REP_BLF_DUMP = 59,
  AI_REP_ASYNC_REC_EVENT=128,
  AI_REP_POS_EXT = 213,   /*added for Extended Position report */
  AI_REP_ERROR_STATUS = 246,
  AI_REP_CUSTOM_PACKET = 247,  /* Custom packet for config 4 callibration status and 
  								   Extended Measurement report */
  AI_REP_BAUD_RATE_CHANGE = 254 /* to Report Baud Rate Change, issue 152 */
};

/* Enumerate Custom packet sub packet types */
enum
{
  AI_REP_CALIB_CONTROL = 2,
  AI_REP_MEAS_EXT = 1,
  AI_REP_GPIO_STATUS = 4,
  AI_REP_MOTION_MASK_SETTINGS = 11,
  AI_REP_MOTION_MASK_STATUS = 12
};



/* TI Custom Response Types */
enum
{
  	AI_CLAPP_REP_VERSION = 240,
  	AI_CLAPP_REP_GPS_STATUS,
  	AI_CLAPP_REP_BEGIN_DWNLD,
	AI_CLAPP_REP_DWNLD_REC_RES,
	AI_CLAPP_REP_DWNLD_COMPLETE,
  	AI_CLAPP_REP_INVALID_MSG,
  	AI_CLAPP_REP_FATAL_ERR_DETECT,
  	AI_CLAPP_REP_BAUD_RATE_CHANGES=254,
  	AI_CLAPP_REP_RCVR_SELF_TEST_RES = 201,
  	AI_CLAPP_REP_GPS_SIGNAL_ACQ_TEST_RES,
  	AI_CLAPP_REP_FULL_FIX_TEST_RES,
  	AI_CLAPP_REP_GPS_OSC_TEST_RES,
  	AI_CLAPP_REP_RTC_OSC_TEST_RES,
  	AI_CLAPP_REP_RAM_CHKSUM_RES,
  	AI_CLAPP_REP_RF_REG_CONTENT = 210,
  	AI_CLAPP_REP_NMEA_SENTENCE,
  	AI_CLAPP_REP_AUTO_SLEEP,
  	AI_CLAPP_REP_POS_EXT,
  	AI_CLAPP_REP_CUSTOM_PACKET =247,
  	AI_CLAPP_REP_CUSTOMER_CONFIG = 220

};

/* TI Custom Response Types - Sub packets */
enum
{
  	AI_CLAPP_CUSTOM_SUB_PKT_REP_RTC_TIME_INJ_CTRL = 0,
	AI_CLAPP_CUSTOM_SUB_PKT_REP_MEAS_EXTND,
	AI_CLAPP_CUSTOM_SUB_PKT_REP_CFG4_CALIB_STATUS,
	AI_CLAPP_CUSTOM_SUB_PKT_REP_WAKEUP_COMPLETE,
	AI_CLAPP_CUSTOM_SUB_PKT_REP_GPIO_STATUS,
	AI_CLAPP_CUSTOM_SUB_PKT_REP_FAST_CALIB_TEST_RESULT,
	AI_CLAPP_CUSTOM_SUB_PKT_REP_ADC_CON_TEST,
	AI_CLAPP_CUSTOM_SUB_PKT_REP_MEM_CONTENTS,
	AI_CLAPP_CUSTOM_SUB_PKT_REP_CALIB_TIME_PULSE_REF_CLK,
	AI_CLAPP_CUSTOM_SUB_PKT_REP_SENSOR_PE_DATA = 14
};

/* Enumerate Asynchronous Events */
enum
{
  AI_EVENT_ENGINE_ON,
  AI_EVENT_ENGINE_OFF,
  AI_EVENT_NEW_ALM,
  AI_EVENT_NEW_EPHEM,
  AI_EVENT_NEW_IONO_UTC,
  AI_EVENT_NEW_SV_HEALTH,
  AI_EVENT_EXT_HW,
  AI_EVENT_ENGINE_IDLE,
  AI_EVENT_NO_NEW_POS,
  AI_EVENT_NO_NEW_MEAS,
  AI_EVENT_SV_ASSIGN_MODE,
  AI_EVENT_ENTRY_MET,
  AI_EVENT_EXIT_MET,
  AI_EVENT_GPS_AWAKE
};

/* Enumerate Receiver Action Types */
enum
{
  C_RECEIVER_RESET,
  C_RECEIVER_OFF,
  C_RECEIVER_IDLE,
  C_RECEIVER_ON,
  C_DELETE_TIME,
  C_DELETE_POS,
  C_DELETE_EPHEM,
  C_DELETE_ALM,
  C_DELETE_IONO_UTC,
  C_DELETE_SV_HEALTH
};

enum
{
  C_AI2_RX_MODE_SYNC,
  C_AI2_RX_MODE_GET_DATA,
  C_AI2_RX_MODE_GET_DATA_DLE
};

enum
{
  C_AI2_NO_ACK,
  C_AI2_ACK_REQ,
  C_AI2_ACK_RESPOND
};


#define C_DLE 0x10
#define C_ETX 0x03

#define C_AI2_DWLD_SUCCESS 0x00
#define C_AI2_DWLD_FAIL 0x08

#define C_AI2_REC_RESULT_OK 0x00		//No error
#define	C_AI2_REC_RESULT_BADCHKSUM 0xFF //Bad data checksum
#define	C_AI2_REC_RESULT_BADADDR 0xEE	//Bad address specified
#define C_AI2_REC_RESULT_INAPPLICABLE 0xDD //Patch not applicable or invalid



/* Custom AI2, Inject Memory Read/Write, length constants */
#define REG_8BIT 		1	//8bit
#define REG_16BIT	2	//16bit
#define REG_32BIT	4	//32bit


enum
{
	C_AI2BUILDER_MSG_INCOMPLETE,
	C_AI2BUILDER_MSG_READY,
	C_AI2BUILDER_ACK_DETECTED
};

/*
 * Structure definitions
*/


/* Ai2 Message Field Structure */
typedef struct
{
  U8   u_Id;      /* Field ID */
  U16  w_Length;  /* Field Length */
  U8*  p_B;       /* Field Data Pointer */
} Ai2Field;

/* Ai2 Receive Data Structure */
typedef struct
{
  U8   u_Ai2RxMode;  /* State variable used to coordinate message building */
  U8   u_Ack;        /* Ack Flag */
  U16  w_BuffIndex;  /* Write index into u_Buff buffer */
  U16  w_Length;     /* Predicted length of this message */
  U16  w_Field;      /* Index of next field */
  U8*  p_Buff;       /* Pointer to an application supplied Ai2 Message buffer */
  U32  q_BuffLength; /* Length in bytes of application supplied buffer */
  U16  w_CheckSum;   /* Receive Checksum */
  U32  q_lostSyncBytes;
} Ai2Rx;

/* Ai2 Transmit Data Structure */
typedef struct
{
  U16  w_ByteCount;  /* Current byte count of transmit message */
  U8*  p_Buff;       /* Pointer to an application supplied message buffer */
  U32  q_BuffLength; /* Length in bytes of application supplied buffer */
  U16  w_Checksum;   /* Transmit Checksum */
} Ai2Tx;

/*
 * Function definitions
*/

/*
 ******************************************************************************
 * Ai2TxInstall
 *
 * Function description:
 *
 *   Ai2TxInstall is used to install the Ai2 transmit protocol. It is
 *   passed pointers to an application provided transmit buffer as well as
 *   the transmit buffer length
 *
 * Parameters:
 *
 *   p_Tx - Pointer to Ai2Tx structure
 *   p_TxBuff - Pointer to user defined transmit buffer
 *   q_TxLen - Length of user defined transmit buffer
 *
 * Return value:
 *   None
 *
 ******************************************************************************
*/
void Ai2TxInstall( Ai2Tx *p_Tx, U8 *p_TxBuff, U32 q_TxLen );

/*
 ******************************************************************************
 * Ai2RxInstall
 *
 * Function description:
 *
 *   Ai2RxInstall is used to install the Ai2 receive protocol. It is
 *   passed pointers to an application provided receive buffer as well as
 *   the receive buffer length
 *
 * Parameters:
 *
 *   p_Rx - Pointer to Ai2Rx structure
 *   p_RxBuff - Pointer to user defined transmit buffer
 *   q_RxLen - Length of user defined transmit buffer
 *
 * Return value:
 *   None
 *
 ******************************************************************************
*/
void Ai2RxInstall( Ai2Rx *p_Rx, U8 *p_RxBuff, U32 q_RxLen );

/*
 ******************************************************************************
 * AiRxBuild
 *
 * Function description:
 *
 *   AiRxBuild is used to construct a AiRx structure from a sequence of
 *   received serial bytes. Upon successful construction of an AI2 message
 *   a TRUE result is returned.
 *
 * Parameters:
 *
 *   p_Rx - Pointer to the AiRx structure under construction
 *   u_Byte - A received serial byte
 *
 * Return value:
 *
 *   TRUE - If a complete message is available in AiRx
 *
 ******************************************************************************
*/
U8 Ai2RxBuild( Ai2Rx *p_Rx, U8 u_Byte );

/*
 ******************************************************************************
 * Ai2RxInit
 *
 * Function description:
 *
 *   Ai2RxInit initializes the referenced Ai2Rx structure. This function
 *   must be called before any bytes are processed using Ai2RxBuild
 *
 * Parameters:
 *
 *   p_Rx - Pointer to the Ai2Rx structure to be initialized
 *
 * Return value:
 *   None
 *
 ******************************************************************************
*/
void Ai2RxInit( Ai2Rx *p_Rx );

/*
 ******************************************************************************
 * Ai2RxFieldGet
 *
 * Function description:
 *
 *   Extracts the next field from the completed message. This generalizes
 *   the process of teasing apart multiple field messages. If there are no
 *   more fields to be read, then the function returns FALSE
 *
 * Parameters:
 *
 *   p_Rx - Pointer to the Ai2Rx structure to be read
 *   p_Field - Pointer to the field structure to be filled
 *
 * Return value:
 *
 *   FALSE if there are no more fields to parse
 *
 ******************************************************************************
*/
U8 Ai2RxFieldGet( Ai2Rx *p_Rx, Ai2Field *p_Field );

/*
 ******************************************************************************
 * Ai2TxInit
 *
 * Function description:
 *
 *   Ai2TxInit is used to initialise the Ai2Tx structure prior to building
 *   the remainder of the message. This function must be called before
 *   using Ai2TxFieldAdd and Ai2TxEnd
 *
 * Parameters:
 *
 *   p_Tx - Pointer to theAi2Tx structure to be initialized
 *   u_AckType - Sets message ACK field. Can take values C_AI2_NO_ACK,
 *               C_AI2_ACK_REQ, C_AI2_ACK_RESPOND
 *
 * Return value:
 *   None
 *
 ******************************************************************************
*/
void Ai2TxInit( Ai2Tx *p_Tx, U8 u_AckType);

/*
 ******************************************************************************
 * Ai2TxFieldAdd
 *
 * Function description:
 *
 *   Ai2TxFieldAdd is used to append the various fields to the complete
 *   message.  This function updates the total byte count and places the
 *   sub field id, sub field size and constituent byte into the Tx
 *   message.  A check is made to ensure that inclusion of the field does
 *   not cause the maximum transmit message size to be violated
 *
 * Parameters:
 *
 *   p_Tx - Pointer to the Ai2Tx structure to be added to
 *   p_Field - Pointer to the Ai2Field structure to add
 *
 * Return value:
 *   None
 *
 ******************************************************************************
*/
void Ai2TxFieldAdd( Ai2Tx *p_Tx, Ai2Field *p_Field );

/*
 ******************************************************************************
 * Ai2TxEnd
 *
 * Function description:
 *
 *   Ai2TxEnd performs the termination construction work on the Ai2Tx
 *   structure. The checksum is computed is used to append the various
 *   fields to the complete message.  This function updates the total byte
 *   count and places the sub field id, sub field size and constituent byte
 *   into the Tx message. A check is made to ensure that inclusion of the
 *   field does not cause the maximum transmit message size to be violated
 *
 * Parameters:
 *
 *   p_Tx - Pointer to the Ai2Tx structure to be added to.
 *
 * Return value:
 *   None
 *
 ******************************************************************************
*/

void Ai2TxEnd( Ai2Tx *p_Tx );

/*
 ******************************************************************************
 * Ai2AddByte
 *
 * Function description:
 *   Ai2AddByte adds a byte to the message buffer and adds the byte to the
 *   running checksum
 *
 * Parameters:
 *
 *   p_Tx - Pointer to AI2 Transmit structure
 *   u_Data - Data to add to message buffer
 *
 * Return value:
 *   None
 *
 ******************************************************************************
*/
void Ai2AddByte( Ai2Tx *p_Tx, U8 u_Data );

/*
 ******************************************************************************
 * Ai2TxMessage
 *
 * Function description:
 *   Ai2TxMessage is to be called when all fields have been added to the
 *   AI2 message. It will add the message header and tail, and stuff bytes
 *   as necessary.  This routine takes an application provided callback
 *   function pointer as one of its inputs.  This function will be the one
 *   that is used to transmit the bytes over the appropriate port. Stuffing
 *   bytes are added as the message is being sent therefore requiring less
 *   stack space than formatting an entire stuffed message and then sending
 *   it.  The application must provide a pointer to the port number or
 *   HANDLE to be used during byte transmission
 *
 * Parameters:
 *
 *   p_Tx - Pointer to Sensor Serial port structure
 *   p_Port - Pointer to Tx Port Number or HANDLE
 *   p_TxFunc - Pointer to application provided transmit byte routine.
 *
 * Return value:
 *   None
 *
 ******************************************************************************
*/

void Ai2TxMessage( Ai2Tx* p_Tx, void* p_Port, void (*p_TxFunc)(void*, U8) );

void Ai2SetAck
(
  Ai2Tx*  p_Tx,
  U8      u_AckType
);
#endif /* _GPSC_AI2_API_H */
