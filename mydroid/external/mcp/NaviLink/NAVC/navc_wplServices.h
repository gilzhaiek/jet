
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
 * FileName			:	navc_wpcServices.h
 *
 * Description     	:
 * Function and parametre definition
 *
 *
 * Author         	: 	Raghavendra MR
 *
 *
 ******************************************************************************
 */


#ifndef _NAVC_WPCSERVICES_H
#define _NAVC_WPCSERVICES_H
#include "gpsc_data.h"
#include "navc_api.h"
/*  WPC command opcodes definition */
typedef enum
{
    WPC_WPL_ERROR,                                 /* for reporting error from WPL */
    WPC_WPL_LOC_IND,                               /* for getting the AP location from WPL Module*/
    WPC_WPL_REGISTER,                              /* Register WPL client with WPE */
	WPC_WPL_READY,                                 /* To indicate WPE for ready ness of WPL module after all the initialization*/
    WPC_WPL_SERVER_BASED_LOCATION,
    WPC_POSITION_REQUEST = 0x10,                   /* for requesting the wifi position from WPE by external application*/
    WPC_STOP_LREQ,                                 /* for stop the wifi position from the external application */
    WPC_GPS_INFO_IND                               /* NAVC update the info needed for scan optimization upon receiving the each pos report*/
} EWPC_cmdOpcode;

/*  WPC queues definition */
typedef enum
{
	WPC_QUE_CMD_ID =0x1,
	WPC_QUE_MAX_ID

} EWPC_queId;

void wpl_location_request( TNAVC_reqLocFix* loc_fix_req_params);
T_GPSC_result wpl_location_stop_request(  U32 q_SessionTagId);
//void populate_wifi_fix_report(void* param,  TNAVC_wifiPosition *pwifiPosition);

#endif //_NAVC_WPCSERVICES_H

