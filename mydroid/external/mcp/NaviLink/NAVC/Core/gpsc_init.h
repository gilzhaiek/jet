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
 * FileName			:	gpsc_api.h
 *
 * Description     	:
 * This file contains initialisation functions for the GPSC.
 *
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef _GPSC_INIT_H
#define _GPSC_INIT_H

#include "gpsc_sap.h"
#include "gpsc_data.h"

T_GPSC_result gpsc_load_from_nvs(gpsc_ctrl_type* p_zGPSCControl, gpsc_cfg_type* pGPSCConfig);

void gpsc_init_database(gpsc_db_type *p_zGPSCDatabase, U8 u_Clean);
void gpsc_init_sess_config(gpsc_sess_cfg_type*	p_zSessConfig);

void gpsc_init_state(gpsc_ctrl_type *p_zGPSCControl);

void gpsc_init_smlc(gpsc_ctrl_type *p_zGPSCControl);

void gpsc_init_config(gpsc_cfg_type *p_zGPSCConfig, U8 u_UpdateNvs);

void gpsc_init_steering(gpsc_db_type *p_zGPSCDatabase);

void gpsc_mmgps_init(gpsc_ctrl_type *p_zGPSCControl);

U8 gpsc_cfg_sanity(gpsc_cfg_type* p_zGPSCConfig);
U8 gpsc_cfg_checksum(gpsc_cfg_type* p_zGPSCConfig);

void gpsc_process_cell_in_service_status(gpsc_ctrl_type* p_zGPSCControl, S16 s_CellInServiceStatus );

U8 gpsc_memory_init(handle_t hMcpf);

void gpsc_init_result_buffer(gpsc_ctrl_type*    p_zGPSCControl);

extern void LmConfigEditor();

void gpsc_init_flags(gpsc_ctrl_type*    p_zGPSCControl);

void gpsc_init_syshandlers(gpsc_sys_handlers*    p_GPSCSysHandlers);


#endif /* _GPSC_INIT_H */
