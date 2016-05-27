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
 * FileName			:	gpsc_drv_api.c
 *
 * Description     	:
 * This file is provides an api for the Driver portion of the GPSC
 * interface.
 *
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_data.h"
#include "gpsc_drv_api.h"
#include "gpsc_msg.h"
#include "gpsc_comm.h"
#include "gpsc_state.h"
#include "gpsc_app_api.h"
#include "gpsc_timer_api.h"
#include "mcpf_services.h"
#include "mcpf_report.h"
#include "mcpf_services.h"
#include "gpsc_mgp_tx.h"
#include "mcpf_mem.h"
#include "nt_adapt.h"
#include "navc_defs.h"
#include "mcp_hal_fs.h"

#define C_TIME_DISCRETE_TIMEOUT 2000

/*** THIS IS STILL IN CONTEXT OF THREAD THAT IS BLOCKING ON /dev/tigps
 
Only purpose of this routine seems to be to dump To/From sensor files, then route rest
to gpsc_comm_rx_bytes. However, there is a bug in log files init sequence - so if you enable
Android logging but disable everything else, this will now start barfing errors. Since it
is useless anyways, it is commented out

 ****/
extern T_GPSC_result gpsc_drv_receive_data_req (U8* p_uDataBuffer, U16 num_bytes)
{
   gpsc_ctrl_type* p_zGPSCControl = gp_zGPSCControl;
//   TNAVCD_Ctrl*    pNavcControl   = (TNAVCD_Ctrl*) p_zGPSCControl->p_zSysHandlers->hNavcCtrl;

/* Temporarily enable fromSensor Logs for Alpha build. Make sure we remove for production */
#ifdef ALL_AI2_COMM_IN_FILE
   if (p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd)
   {
	   gpsc_all_ai2_hdr curr_ai2_hdr = {0}; 

	   curr_ai2_hdr.magic_word = ALL_AI2_MAGIC_WORD;
	   gettimeofday(&curr_ai2_hdr.currtime, NULL);
	   curr_ai2_hdr.length = num_bytes;
	   curr_ai2_hdr.to_or_from = AI2_FROM_SENSOR;

	   if ( mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, 
		   p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd, 
		   (void *)&curr_ai2_hdr, sizeof(curr_ai2_hdr)) != sizeof(curr_ai2_hdr))
	   {
		   MCPF_REPORT_ERROR(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG, 
			   ("gpsc_drv_receive_data_req: Failed to write to AllSensorAI2 File")); 
	   }

	   if ( mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, 
		   p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd, 
		   (void *)p_uDataBuffer, num_bytes) != num_bytes )
	   {
		   MCPF_REPORT_ERROR(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG, 
			   ("gpsc_drv_receive_data_req: Failed to write to AllSensorAI2 File")); 
	   }
   }
#else
   if (p_zGPSCControl->p_zSysHandlers->uFromSensorFd)
   {
	   if ( mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, 
		   p_zGPSCControl->p_zSysHandlers->uFromSensorFd, 
		   (void*)p_uDataBuffer, num_bytes) != num_bytes )
	   {
		   MCPF_REPORT_ERROR(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG, 
			   ("gpsc_drv_receive_data_req: Failed to write to FromSensor File")); 
	   }
   }
#endif   

   return gpsc_comm_rx_bytes(p_zGPSCControl, p_uDataBuffer, num_bytes);
}

extern T_GPSC_result gpsc_drv_transmit_data_res (handle_t hCbHandle, void *pBuf)
{
	gpsc_ctrl_type *p_zGPSCControl;
	gpsc_comm_tx_buff_type * p_Ai2TxBuf;
	T_GPSC_result x_Result = GPSC_SUCCESS;

	SAPENTER(gpsc_drv_transmit_data_res);

	p_zGPSCControl = gp_zGPSCControl;
	p_Ai2TxBuf  = p_zGPSCControl->p_zGPSCCommTxBuff;

	MCPF_UNUSED_PARAMETER(hCbHandle);
	MCPF_UNUSED_PARAMETER(pBuf);

	switch(x_Result)
	{
		case GPSC_SUCCESS	:
				STATUSMSG("Status: Tx Success Received");
				gpsc_comm_tx_success(p_zGPSCControl);
			break;

		case GPSC_FAIL	:	ERRORMSG("Error: GPSM failed transmit request");
							gpsc_os_fatal_error_ind(GPSC_TX_FAIL); 
							GpscSm(p_zGPSCControl->p_zGpscSm, E_GPSC_EVENT_SHUTDOWN);
							break;
		default			 :	ERRORMSG("Error: Invalid result from Driver");
								break;
	}
	SAPLEAVE(gpsc_drv_transmit_data_res, GPSC_SUCCESS)
}

extern T_GPSC_result gpsc_drv_interrupt_received_req (void)
{
	gpsc_ctrl_type *p_zGPSCControl;
	SAPENTER(gpsc_drv_interrupt_received_req);
	p_zGPSCControl = gp_zGPSCControl;
	STATUSMSG("Status: Interrupt signal received from driver");
	SAPLEAVE(gpsc_drv_interrupt_received_req, GPSC_SUCCESS)
}

//sunil  *** WHAT a horror. But can't just remove because there is warped logic in navc_errHandler too
//           that depends on these variables. They were trying to fix Message Queue problem
//
U8 g_databuffer[2048];
U16 g_NumBytes=0;
U8 g_retransmitFlag =0;
extern U8 g_retransmitCount;

T_GPSC_result gpsc_transmit_data(U8* p_uDataBuffer, U32 w_NumBytes)
{
	 U16 i = 0;
    g_NumBytes = w_NumBytes;
	
	gpsc_ctrl_type* p_zGPSCControl = gp_zGPSCControl;
//	TNAVCD_Ctrl*    pNavcControl = (TNAVCD_Ctrl*) p_zGPSCControl->p_zSysHandlers->hNavcCtrl;

	if (!g_retransmitFlag)
    {
         for (i = 0; i < w_NumBytes; i++)
         {
            g_databuffer[i] = p_uDataBuffer[i];
         }
         g_retransmitCount = 0; // make zero for successful transmission
    }

/*** RECON is not using this nonsense and it has bugs, so just comment out
 *   Re-enable toSensor logs for Alpha build. Make sure we disable for production */
#ifdef ALL_AI2_COMM_IN_FILE
   if (p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd)
   {
	   gpsc_all_ai2_hdr curr_ai2_hdr = {0};

	   curr_ai2_hdr.magic_word = ALL_AI2_MAGIC_WORD;
	   gettimeofday(&curr_ai2_hdr.currtime, NULL);
	   curr_ai2_hdr.length = w_NumBytes;
	   curr_ai2_hdr.to_or_from = AI2_TO_SENSOR;

	   if( mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, 
		   p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd, 
		   (void *)&curr_ai2_hdr, sizeof(curr_ai2_hdr)) != sizeof(curr_ai2_hdr))
	   {
		   MCPF_REPORT_ERROR(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG, 
			   ("gpsc_transmit_data: Failed to write to AllSensorAI2 File")); 
	   }

	   if( mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, 
			p_zGPSCControl->p_zSysHandlers->uAllSensorAI2Fd, 
			(void *)p_uDataBuffer, (McpU16)w_NumBytes) != w_NumBytes )
	   {
		   MCPF_REPORT_ERROR(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG, 
			   ("gpsc_transmit_data: Failed to write to AllSensorAI2 File")); 
	   }
   }
#else
	if (p_zGPSCControl->p_zSysHandlers->uToSensorFd)
	
	{
		if( mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, 
			p_zGPSCControl->p_zSysHandlers->uToSensorFd, 
			(void *)p_uDataBuffer, (McpU16)w_NumBytes) != w_NumBytes )
		{
			MCPF_REPORT_ERROR(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG, 
				("gpsc_transmit_data: Failed to write to ToSensor File")); 
		}
	}
#endif
	// Send buffer to NAVC adapter 
	return NTA_SendData(p_uDataBuffer, (McpU16)w_NumBytes);
}


