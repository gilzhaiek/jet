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


/** \file   navc_sm.c
 *  \brief  NAVC main state machine implementation
 *
  */


/************************************************************************
 * Includes
 ************************************************************************/

#include "pla_defs.h"
#include "gpsc_data.h"
#include "gpsc_state.h"
#include "navc_init.h"
#include "navc_api.h"
#include "navc_defs.h"
#include "navc_cmdHandler.h"
#include "mcpf_main.h"
#include "mcpf_report.h"
#include "mcpf_mem.h"
#include "mcpf_msg.h"
#include "mcpf_services.h"
#include "bmtrace.h"
#include "navc_sm.h"
#include "nt_adapt.h"
#include "mcp_hal_os.h"
#include "mcp_hal_fs.h"

extern void MCP_HAL_LOG_EnableUdpLogging(const char* ip, unsigned long port);

static void Destroy(TNavcSm *pNavcSm);
static void Stop(TNavcSm *pNavcSm);
static void NavcSaPowerCtrl(TNavcSm *pNavcSm, McpBool bPowerOn);
static ENavcSMState StateNull(TNavcSm *pNavcSm, ENavcSMEvent event);
static ENavcSMState StateIdle(TNavcSm *pNavcSm, ENavcSMEvent event);
static ENavcSMState StateHwInit(TNavcSm *pNavcSm, ENavcSMEvent event);
static ENavcSMState StateSwInit(TNavcSm *pNavcSm, ENavcSMEvent event);
static ENavcSMState StateReady(TNavcSm *pNavcSm, ENavcSMEvent event);
static ENavcSMState StateRunning(TNavcSm *pNavcSm, ENavcSMEvent event);
static ENavcSMState StateHwDeInit(TNavcSm *pNavcSm, ENavcSMEvent event);
static ENavcSMState StateError(TNavcSm *pNavcSm, ENavcSMEvent event);
static char *SmState(ENavcSMState state);
static char *SmEvent(ENavcSMEvent event);
static U8 CalcCheckSum(U8 *p_uBuffer, U32 q_length);
static EMcpfRes ReadConfigFile(TNavcSm *pNavcSm, McpS8* filename, T_GPSC_config_file *pCfgFile);


handle_t NavcSmCreate(handle_t hMcpf, handle_t hNavc)
{
	TNavcSm *pNavcSm = mcpf_mem_alloc(hMcpf, sizeof(TNavcSm));
	if(pNavcSm)
	{
		/* Prepare SM database */
		pNavcSm->stateHnd[E_NAVC_ST_NULL] = StateNull;
		pNavcSm->stateHnd[E_NAVC_ST_IDLE] = StateIdle;
		pNavcSm->stateHnd[E_NAVC_ST_HW_INIT] = StateHwInit;
		pNavcSm->stateHnd[E_NAVC_ST_SW_INIT] = StateSwInit;
		pNavcSm->stateHnd[E_NAVC_ST_READY] = StateReady;
		pNavcSm->stateHnd[E_NAVC_ST_RUNNING] = StateRunning;
		pNavcSm->stateHnd[E_NAVC_ST_HW_DEINIT] = StateHwDeInit;
		pNavcSm->stateHnd[E_NAVC_ST_ERROR] = StateError;

		pNavcSm->currState = E_NAVC_ST_NULL;

		pNavcSm->hMcpf = hMcpf;
		pNavcSm->hNavc = hNavc;

		pNavcSm->bDestroy = FALSE;
	}
	return pNavcSm;
}

void NavcSmDestroy(handle_t hNavcSm)
{
	TNavcSm *pNavcSm = (TNavcSm *)hNavcSm;

	mcpf_mem_free(pNavcSm->hMcpf, hNavcSm);
}

void NavcSm(handle_t hNavcSm, ENavcSMEvent event)
{
	TNavcSm *pNavcSm = (TNavcSm *)hNavcSm;
	handle_t hMcpf = pNavcSm->hMcpf;
	MCPF_REPORT_INFORMATION(hMcpf, NAVC_MODULE_LOG, ("NavcSm: State=%s, Event=%s", SmState(pNavcSm->currState), SmEvent(event)));
	pNavcSm->currState = pNavcSm->stateHnd[pNavcSm->currState](pNavcSm, event);
	MCPF_REPORT_INFORMATION(hMcpf, NAVC_MODULE_LOG, ("-->: State=%s", SmState(pNavcSm->currState)));
}
/************************************************************************
 *
 *   Private functions implementation
 *
 ************************************************************************/
static void Destroy(TNavcSm *pNavcSm)
{
	MCPF_UNUSED_PARAMETER(pNavcSm);
	
	/* De-initialize GPSC */
	gpsc_app_deinit_req();
}
static void Stop(TNavcSm *pNavcSm)
{
	
    gpsc_ctrl_type	*p_zGPSCControl =(gpsc_ctrl_type *) pNavcSm->hGpsc;

	mcpf_MsgqDisable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);
	mcpf_MsgqDisable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
	if(gpsc_app_shutdown_req() != GPSC_SUCCESS)
		MCP_ASSERT(0);
	NTA_Close();
#ifdef MCP_STK_ENABLE
	mcpf_SendMsg (pNavcSm->hMcpf, TASK_NAV_ID,  NAVC_QUE_INTERNAL_EVT_ID,
                         TASK_NAV_ID, NAVC_QUE_CMD_ID, NAVCD_EVT_CCM_COMPL_IND, 0, 0, NULL);
#else
	NavcSaPowerCtrl(pNavcSm, FALSE);
#endif
}

static void NavcSaPowerCtrl(TNavcSm *pNavcSm, McpBool bPowerOn)
{
	McpU32      uOutByte;
	
	MCPF_REPORT_INFORMATION(pNavcSm->hMcpf, NAVC_MODULE_LOG, 
							("IO Control: Power=%u\n", (U8)bPowerOn));

    	uOutByte = (bPowerOn)?1:0;
	mcpf_hwGpioSet (pNavcSm->hMcpf, BIT_GPS_EN_RESET, (McpU8) (uOutByte));
    	MCP_HAL_OS_Sleep(1000);	/* pause to stabilize the output signal */

    	mcpf_SendMsg (pNavcSm->hMcpf, 
                         TASK_NAV_ID,  
                         NAVC_QUE_INTERNAL_EVT_ID,
                         TASK_NAV_ID,
                         NAVC_QUE_CMD_ID,
                         NAVCD_EVT_CCM_COMPL_IND,
                         0,                   /* data size    */
                         uOutByte, 			  /* user defined */
                         NULL);
}

extern TNAVCD_Ctrl *pNavcCtrl;

static ENavcSMState StateNull(TNavcSm *pNavcSm, ENavcSMEvent event)
{
	ENavcSMState nextState;

	TNAVCD_Ctrl	*pNavc = (TNAVCD_Ctrl *)pNavcSm->hNavc;
	switch(event)
	{
		case E_NAVC_EV_CREATE:
		{
			gpsc_sys_handlers sysHandlers;

			gpsc_init_syshandlers(&sysHandlers);
			gpsc_parse_control_file(pNavcSm, &sysHandlers, pNavc->uPathCtrlFile);

			if(ReadConfigFile(pNavcSm, sysHandlers.uGpscConfigFile, &sysHandlers.GpscConfigFile) != RES_OK)
			{
				MCPF_REPORT_INFORMATION(pNavc->hMcpf, NAVC_MODULE_LOG, ("ERROR While Reading %s", sysHandlers.uGpscConfigFile));	
				nextState = E_NAVC_ST_ERROR;
			}
			else
			{
				/* Init GPSC */
				sysHandlers.hMcpf = pNavcSm->hMcpf;
				sysHandlers.hNavcCtrl = pNavcSm->hNavc;

				gpsc_app_init_req((void *)&sysHandlers, &pNavcSm->hGpsc);

			    /* initialize priority db from the config file */

				pNavc->tAssistSrcDb[PGPS_PROVIDER].uAssistSrcPriority = sysHandlers.GpscConfigFile.priority_pgps;
				pNavc->tAssistSrcDb[SAGPS_PROVIDER].uAssistSrcPriority = sysHandlers.GpscConfigFile.priority_sagps;
				pNavc->tAssistSrcDb[SUPL_PROVIDER].uAssistSrcPriority = sysHandlers.GpscConfigFile.priority_supl;
				pNavc->tAssistSrcDb[CPLANE_PROVIDER].uAssistSrcPriority = sysHandlers.GpscConfigFile.priority_cplane;
				pNavc->tAssistSrcDb[CUSTOM_AGPS_PROVIDER1].uAssistSrcPriority = sysHandlers.GpscConfigFile.priority_custom_agps_provider1;
				pNavc->tAssistSrcDb[CUSTOM_AGPS_PROVIDER2].uAssistSrcPriority = sysHandlers.GpscConfigFile.priority_custom_agps_provider2;
				pNavc->tAssistSrcDb[CUSTOM_AGPS_PROVIDER3].uAssistSrcPriority = sysHandlers.GpscConfigFile.priority_custom_agps_provider3;


				/* Enable ActionQ */
				mcpf_MsgqEnable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
				nextState = E_NAVC_ST_IDLE;
			}
		}
		break;
		case E_NAVC_EV_DESTROY:
		{
			nextState = E_NAVC_ST_NULL;
		}
		break;
		default:
			MCP_ASSERT(0);
			nextState = E_NAVC_ST_ERROR;
	}
	return nextState;
}


static ENavcSMState StateIdle(TNavcSm *pNavcSm, ENavcSMEvent event)
{
	ENavcSMState nextState;
	switch(event)
	{
		case E_NAVC_EV_START:
		{
			mcpf_MsgqEnable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_INTERNAL_EVT_ID);
			if(NTA_Open() == RES_ERROR)
			{
				MCP_ASSERT(0);
				nextState = E_NAVC_ST_ERROR;
			}
			else
			{
#ifdef MCP_STK_ENABLE
				mcpf_SendMsg (pNavcSm->hMcpf, TASK_NAV_ID,  NAVC_QUE_INTERNAL_EVT_ID,
                         					TASK_NAV_ID, NAVC_QUE_CMD_ID, NAVCD_EVT_CCM_COMPL_IND, 
                         					0, 0, NULL);
#else
				NavcSaPowerCtrl(pNavcSm, TRUE);
#endif
				nextState = E_NAVC_ST_HW_INIT;
			}
		}
		break;
		case E_NAVC_EV_DESTROY:
		{
			Destroy(pNavcSm);
			nextState = E_NAVC_ST_NULL;
		}
		break;

		case E_NAVC_EV_COMPLETE:
		{
			if(pNavcSm->bDestroy)
			{
				Stop(pNavcSm);
				nextState = E_NAVC_ST_HW_DEINIT;
			}
			else
			{
			//	mcpf_MsgqEnable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);
			//	mcpf_MsgqEnable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
			//	NavcCmdCompleteHandler (pNavcSm->hNavc, RES_OK);
				nextState = E_NAVC_ST_IDLE;
			}
		}
		break;
		default:
			nextState = E_NAVC_ST_ERROR;
	}
	return nextState;
}

static ENavcSMState StateHwInit(TNavcSm *pNavcSm, ENavcSMEvent event)
{
	ENavcSMState nextState;
	UNREFERENCED_PARAMETER(pNavcSm);

	switch(event)
	{
		case E_NAVC_EV_COMPLETE:
		{
			gpsc_app_gps_ready_req();
			pNavcSm->bInitialized = TRUE;
			nextState = E_NAVC_ST_SW_INIT;

		}
		break;
		case E_NAVC_EV_DESTROY:
		{
			pNavcSm->bDestroy = TRUE;
			nextState = E_NAVC_ST_HW_INIT;
		}
		break;
		default:
			nextState = E_NAVC_ST_ERROR;
	}
	return nextState;
}

static ENavcSMState StateSwInit(TNavcSm *pNavcSm, ENavcSMEvent event)
{
	ENavcSMState nextState;

	switch(event)
	{
		case E_NAVC_EV_COMPLETE:
		{
			if(pNavcSm->bDestroy)
			{
				Stop(pNavcSm);
				nextState = E_NAVC_ST_HW_DEINIT;
			}
			else
			{
				mcpf_MsgqEnable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);
				mcpf_MsgqEnable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
				NavcCmdCompleteHandler (pNavcSm->hNavc, RES_OK);

				nextState = E_NAVC_ST_READY;
			}
		}
		break;
		case E_NAVC_EV_DESTROY:
		{
			pNavcSm->bDestroy = TRUE;
			nextState = E_NAVC_ST_SW_INIT;
		}
		break;
		case E_NAVC_EV_FAILURE:
		{
			NavcCmdCompleteHandler(pNavcSm->hNavc, RES_ERROR);
			nextState = E_NAVC_ST_SW_INIT;
		}
		break;
		default:
			nextState = E_NAVC_ST_ERROR;
	}
	return nextState;
}

static ENavcSMState StateReady(TNavcSm *pNavcSm, ENavcSMEvent event)
{
	ENavcSMState nextState;

	switch(event)
	{
		case E_NAVC_EV_START:
		{
			if(!pNavcSm->bInitialized)
			{
			mcpf_MsgqDisable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);
			mcpf_MsgqDisable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
			nextState = E_NAVC_ST_RUNNING;
			}
			else
			{
			nextState = E_NAVC_ST_READY;
			}

		}
		break;

		case E_NAVC_EV_COMPLETE:
			nextState = E_NAVC_ST_READY;
		break;

		case E_NAVC_EV_DESTROY:
			pNavcSm->bDestroy = TRUE;
		case E_NAVC_EV_STOP:
		{
			Stop(pNavcSm);
			NavcCmdCompleteHandler (pNavcSm->hNavc, RES_OK);
			pNavcSm->bInitialized = FALSE;
			MCPF_REPORT_INFORMATION(pNavcSm->hMcpf, NAVC_MODULE_LOG, ("StateReady: mcpf_MsgqEnable - NAVC_QUE_CMD_ID and ACTION_ID"));
			mcpf_MsgqEnable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);
			mcpf_MsgqEnable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
			nextState = E_NAVC_ST_IDLE;
		}
		break;
		default:
			nextState = E_NAVC_ST_ERROR;
	}
	return nextState;
}
static ENavcSMState StateRunning(TNavcSm *pNavcSm, ENavcSMEvent event)
{
	return StateSwInit(pNavcSm, event);
}

static ENavcSMState StateHwDeInit(TNavcSm *pNavcSm, ENavcSMEvent event)
{
	ENavcSMState nextState;

	switch(event)
	{
		case E_NAVC_EV_COMPLETE:
		{


			if(pNavcSm->bDestroy)
			{
				Destroy(pNavcSm);
				nextState = E_NAVC_ST_NULL;
			}
			else
			{
				NavcCmdCompleteHandler (pNavcSm->hNavc, RES_OK);
				mcpf_MsgqEnable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_CMD_ID);
				mcpf_MsgqEnable(pNavcSm->hMcpf, TASK_NAV_ID, NAVC_QUE_ACTION_ID);
				nextState = E_NAVC_ST_IDLE;
			}
		}
		break;
		default:
			nextState = E_NAVC_ST_ERROR;
	}
	return nextState;
}
static ENavcSMState StateError(TNavcSm *pNavcSm, ENavcSMEvent event)
{
	ENavcSMState nextState;

	switch(event)
	{
		case E_NAVC_EV_DESTROY:
		{
			Destroy(pNavcSm);
			nextState = E_NAVC_ST_NULL;
		}
		break;
		default:
			nextState = E_NAVC_ST_ERROR;
	}
	return nextState;
}

static char *SmState(ENavcSMState state)
{
	char *rc;
	switch(state) {
	case E_NAVC_ST_NULL:
		rc =  "NULL   ";
		break;
	case E_NAVC_ST_IDLE:
		rc =  "IDLE   ";
		break;
	case E_NAVC_ST_HW_INIT:
		rc =  "HW_INIT";
		break;
	case E_NAVC_ST_SW_INIT:
		rc =  "SW_INIT";
		break;
	case E_NAVC_ST_RUNNING:
		rc =  "RUNNING";
		break;
	case E_NAVC_ST_READY:
		rc =  "READY  ";
		break;
	case E_NAVC_ST_HW_DEINIT:
		rc =  "DEINIT ";
		break;
	default:
		rc =  "ERROR  ";
		break;
	}
	return rc;
}
static char *SmEvent(ENavcSMEvent event)
{
	char *rc;
	switch(event) {
	case E_NAVC_EV_CREATE:
		rc =  "CREATE  ";
		break;
	case E_NAVC_EV_START:
		rc =  "START   ";
		break;
	case E_NAVC_EV_STOP:
		rc =  "STOP    ";
		break;
	case E_NAVC_EV_COMPLETE:
		rc =  "COMPLETE";
		break;
	case E_NAVC_EV_DESTROY:
		rc =  "DESTROY ";
		break;
	case E_NAVC_EV_FAILURE:
		rc =  "FAILURE ";
		break;
	default:
		rc =  "UNKNOWN ";
		break;
	}
	return rc;
}




static U8 CalcCheckSum(U8 *p_uBuffer, U32 q_length)
{
	U32 q_i;
	U8 u_checksum=0;

	for(q_i= 0;q_i<q_length;q_i++)
	{
		u_checksum = (U8)(u_checksum ^ p_uBuffer[q_i]);
	}
	return u_checksum;
}

static EMcpfRes ReadConfigFile(TNavcSm* pNavcSm, McpS8* filename, T_GPSC_config_file* pCfgFile)
{
	McpS32	fd = 0;
	McpU32	uBytesRead = 0;
	EMcpfRes	eRetVal = RES_FILE_ERROR;

	/******Read in configuration file from NVS;                         */
	/*     if not existing, create a default one for current use,       */
	/*     and save the default to NVS                             ******/
	if (pCfgFile)
	{
		eRetVal = mcpf_file_open(pNavcSm->hMcpf, (McpU8*)filename, 
					MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_RDONLY, &fd);

		if (pCfgFile && eRetVal == RES_OK)
		{
			uBytesRead = mcpf_file_read(pNavcSm->hMcpf, fd, (void*)pCfgFile, sizeof(gpsc_cfg_type));
			mcpf_file_close(pNavcSm->hMcpf, fd);
			if ( uBytesRead == sizeof( gpsc_cfg_type ) )
			{
				if(CalcCheckSum((U8 *)pCfgFile, sizeof(T_GPSC_config_file))!=TRUE)
				{
					eRetVal = RES_OK;
				}
			}
		}
	}
	return eRetVal;
}
























