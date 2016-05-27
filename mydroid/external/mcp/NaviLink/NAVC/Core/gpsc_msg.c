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
 * FileName			:	gpsc_msg.c
 *
 * Description     	:
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_msg.h"
#include "mcpf_report.h"
#include "gpsc_data.h"


/*****************************************************************************
 * FUNCTION:  gpsc_diagnostic(2,
 *
 * PARAMETERS:  S8 * ps_DiagMsg      pointer to asciz message string
 *        list of arguments (...)
 *
 * DESCRIPTION:
 *    Formats a string according to ps_DiagMsg from supplied arguments
 *      Writes resulting string to Named Pipe Handle p_hPipe.
 *
 * USER:
 *    Any Thread in the system and the system manager
 *
 * DEPENDENCIES:
 *    p_hPipe must be valid
 *      stderr must be valid
 *
 ****************************************************************************/

extern void gpsc_diagnostic(T_GPSC_diag_level DiagLevel ,char *p_sDiagMsg, ...)
{
  char local_buffer[MAX_DIAGSTRING_LENGTH+32];
  gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;

  va_list marker;
  va_start(marker, p_sDiagMsg);

   vsprintf(local_buffer, p_sDiagMsg, marker);

   if (strlen(local_buffer) > MAX_DIAGSTRING_LENGTH)
   {
     fprintf(stderr,"Error: DiagString too long. %s\n", local_buffer);
   }

	switch(DiagLevel)
	{
		case GPSC_DIAG_ERROR_WARNING :
			MCPF_REPORT_ERROR(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG,
								("%s: %s", "ERROR", local_buffer));
			break;
		case GPSC_DIAG_GPSC_INTERNAL_STATUS :
			MCPF_REPORT_INFORMATION(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG,
										("%s: %s", "GIS", local_buffer));
			break;
		case GPSC_DIAG_GPSM_INTERACTION :
			MCPF_REPORT_INFORMATION(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG,
										("%s: %s", "GMI", local_buffer));
			break;
		case GPSC_DIAG_AI2_INTERACTION :
			MCPF_REPORT_INFORMATION(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG,
										("%s: %s", "AI2", local_buffer));
			break;
		default:
			break;
	}

}

extern void gpsc_status_msg(char *p_sDiagMsg, ...)
{
  char local_buffer[MAX_DIAGSTRING_LENGTH+32];
  gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;

  va_list marker;
  va_start(marker, p_sDiagMsg);

   vsprintf(local_buffer, p_sDiagMsg, marker);

   if (strlen(local_buffer) > MAX_DIAGSTRING_LENGTH)
   {
     fprintf(stderr,"Error: DiagString too long. %s\n", local_buffer);
   }

   MCPF_REPORT_INFORMATION(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG,
							("%s: %s", "GIS", local_buffer));
}
