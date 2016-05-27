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
#ifndef _GPSC_MGP_ASSIST_H
#define _GPSC_MGP_ASSIST_H

enum {
  E_GPSC_ASSIST_INJECT_NO_ASSISTANCE,
  E_GPSC_ASSIST_INJECT_COMPLETE,
  E_GPSC_ASSIST_INJECT_WAIT_LOC_REQ
};
U8 gpsc_mgp_assist_apply (gpsc_ctrl_type *p_zGPSCControl);

T_GPSC_result gpsc_inject_nvs_data(gpsc_ctrl_type* p_zGPSCControl);
U8 gpsc_mgp_assist_pmatch_apply (gpsc_ctrl_type *p_zGPSCControl);
U8 gpsc_mgp_assist_svdir_save (gpsc_ctrl_type *p_zGPSCControl);
U8 gpsc_mgp_assist_svdir_apply (gpsc_ctrl_type *p_zGPSCControl);
U8 gpsc_mgp_assist_svdiff_apply (gpsc_ctrl_type *p_zGPSCControl);
U8 gpsc_mgp_assist_steering_apply (gpsc_ctrl_type *p_zGPSCControl);
U8 gpsc_mgp_assist_steering_sort(gpsc_ctrl_type *p_zGPSCControl);
U8 gpsc_mgp_assist_rti_apply (gpsc_ctrl_type *p_zGPSCControl);
U8 gpsc_mgp_assist_eph_apply (gpsc_ctrl_type *p_zGPSCControl);
void alm_week_reconcile(pe_RawAlmanac *p_zSmlcRawAlm, U16 w_CurrentWeek);
#endif /* _GPSC_MGP_ASSIST_H */
