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

/*****************************************************************************
 * File:        report_app.c
 *
 * Description: The file contains the gui code for REPORT APP.
 * 				
 *
 * Created:     
 *
 * Author: 	
 *
 ****************************************************************************/

#include "windows.h"
#include "windowsx.h"
#include <stdio.h>

#ifdef _DEBUG
#include "commctrl.h"
#include "resource.h"


#include "mcpf_defs.h"
#include "mcp_defs.h"
#include "mcpf_report.h"
#include "mcpf_main.h"	

#define UNUSED_PARAMETER(_PARM) 	((_PARM) = (_PARM))

/********************************************************************************
 *
 * Globals
 *
 *******************************************************************************/

static HWND        _REPROT_AppWnd;
static HINSTANCE   hInst;
handle_t g_hMcpf;

/****************************************************************************
 *
 * Internal Function prototypes
 *
 ***************************************************************************/
static BOOL CALLBACK  _REPORT__REPORT_AppWndProc(HWND, UINT, WPARAM, LPARAM);
static void checkOpenReports();

/*
 * Main Entry point for our application
 */
HWND *Report_APP_Init( HINSTANCE Inst, HINSTANCE PrevInst, LPTSTR CmdLine, int CmdShow, handle_t hMcpf )
{	
	UNUSED_PARAMETER(CmdLine);
	UNUSED_PARAMETER(PrevInst);

	hInst = Inst;
	g_hMcpf = hMcpf;

	InitCommonControls();

	if (_REPROT_AppWnd == NULL)
	{
		/* Create Application GUI */
		_REPROT_AppWnd = CreateDialog(Inst, MAKEINTRESOURCE(REPORT_APP), NULL, _REPORT__REPORT_AppWndProc);

		if (_REPROT_AppWnd == NULL)
		{
			return 0;
		}

		checkOpenReports();

		ShowWindow( _REPROT_AppWnd, CmdShow ) ;
		
	}
	else
	{
		/* If the App was already initialized, just set the focus to its windows to allow its usage */
		HWND prevFocusWnd = SetFocus(_REPROT_AppWnd);
		MCPF_Assert(prevFocusWnd != NULL);
	}

	return &_REPROT_AppWnd;
}

BOOL CALLBACK _REPORT__REPORT_AppWndProc( HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam )
{	
	UNUSED_PARAMETER(lParam);

	switch (uMsg)
	{
		case WM_SHOWWINDOW:
			break;
		
		case WM_COMMAND:
			switch (LOWORD(wParam)) 
			{	
			/* Severity */
			case IDC_SEV_INIT:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_INIT)) == BST_CHECKED)
					report_SetReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_INIT);	
				else
					report_ClearReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_INIT);	
				break;

			case IDC_SEV_INFORMATION:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_INFORMATION)) == BST_CHECKED)
					report_SetReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_INFORMATION);
				else
					report_ClearReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_INFORMATION);	
				break;

			case IDC_SEV_WARNING:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_WARNING)) == BST_CHECKED)
					report_SetReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_WARNING);	
				else
					report_ClearReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_WARNING);
				break;

			case IDC_SEV_ERROR:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_ERROR)) == BST_CHECKED)
					report_SetReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_ERROR);	
				else
					report_ClearReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_ERROR);	
				break;

			case IDC_SEV_FATALERROR:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_FATALERROR)) == BST_CHECKED)
					report_SetReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_FATAL_ERROR);
				else
					report_ClearReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_FATAL_ERROR);	
				break;

			case IDC_SEV_SM:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_SM)) == BST_CHECKED)
					report_SetReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_SM);	
				else
					report_ClearReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_SM);
				break;

			case IDC_SEV_CONSOLE:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_CONSOLE)) == BST_CHECKED)
					report_SetReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_CONSOLE);	
				else
					report_ClearReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_CONSOLE);	
				break;

			case IDC_SEV_DEBUGRX:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_DEBUGRX)) == BST_CHECKED)
					report_SetReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_DEBUG_RX);
				else
					report_ClearReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_DEBUG_RX);	
				break;

			case IDC_SEV_DEBUGTX:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_DEBUGTX)) == BST_CHECKED)
					report_SetReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_DEBUG_TX);	
				else
					report_ClearReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_DEBUG_TX);
				break;

			case IDC_SEV_DEBUGCONTROL:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_DEBUGCONTROL)) == BST_CHECKED)
					report_SetReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_DEBUG_CONTROL);	
				else
					report_ClearReportSeverity(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_SEVERITY_DEBUG_CONTROL);	
				break;

			/* Modules */
			case IDC_MOD_NAVC:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_NAVC)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), NAVC_MODULE_LOG);
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), NAVC_MODULE_LOG);	
				break;

			case IDC_MOD_BT:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_BT)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), BT_MODULE_LOG);	
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), BT_MODULE_LOG);	
				break;

			case IDC_MOD_FM:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_FM)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), FM_MODULE_LOG);	
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), FM_MODULE_LOG);		
				break;

			case IDC_MOD_MCPF:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_MCPF)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), MCPF_MODULE_LOG);
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), MCPF_MODULE_LOG);		
				break;

			case IDC_MOD_TRANS:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_TRANS)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), TRANS_MODULE_LOG);	
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), TRANS_MODULE_LOG);	
				break;

			case IDC_MOD_QUEUE:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_QUEUE)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), QUEUE_MODULE_LOG);	
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), QUEUE_MODULE_LOG);		
				break;

			case IDC_MOD_REPORT:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_REPORT)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_MODULE_LOG);
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), REPORT_MODULE_LOG);		
				break;

			case IDC_MOD_IFSLPMNG:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_IFSLPMNG)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), IFSLPMNG_MODULE_LOG);	
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), IFSLPMNG_MODULE_LOG);	
				break;

			case IDC_MOD_BUSDRV:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_BUSDRV)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), BUS_DRV_MODULE_LOG);	
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), BUS_DRV_MODULE_LOG);		
				break;

			case IDC_MOD_HALUART:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_HALUART)) == BST_CHECKED)
				{
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), HAL_UART_MODULE_LOG);
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), HAL_ST_MODULE_LOG);
				}
				else
				{
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), HAL_UART_MODULE_LOG);
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), HAL_ST_MODULE_LOG);		
				}
				break;

			case IDC_MOD_UI:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_UI)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), UI_MODULE_LOG);	
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), UI_MODULE_LOG);	
				break;

			case IDC_MOD_ADS:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_ADS)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), ADS_MODULE_LOG);	
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), ADS_MODULE_LOG);		
				break;

			case IDC_MOD_RR:
				if(Button_GetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_RR)) == BST_CHECKED)
					report_SetReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), RR_MODULE_LOG);
				else
					report_ClearReportModule(MCPF_GET_REPORT_HANDLE(g_hMcpf), RR_MODULE_LOG);		
				break;
			}
			break;

		case WM_CLOSE:
				DestroyWindow(_REPROT_AppWnd);
				_REPROT_AppWnd = NULL;
			break;
	}

	return 0;
}


static void checkOpenReports()
{
	McpU8			aSeverityTable[REPORT_SEVERITY_MAX];				
    McpU8			aModuleTable[MCP_MAX_LOG_MODULES];

	report_GetReportModuleTable(MCPF_GET_REPORT_HANDLE(g_hMcpf), aModuleTable);
	report_GetReportSeverityTable(MCPF_GET_REPORT_HANDLE(g_hMcpf), aSeverityTable);
	
	/* Severity */
	if(aSeverityTable[REPORT_SEVERITY_INIT] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_INIT), BST_CHECKED);

	if(aSeverityTable[REPORT_SEVERITY_INFORMATION] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_INFORMATION), BST_CHECKED);

	if(aSeverityTable[REPORT_SEVERITY_WARNING] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_WARNING), BST_CHECKED);

	if(aSeverityTable[REPORT_SEVERITY_ERROR] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_ERROR), BST_CHECKED);

	if(aSeverityTable[REPORT_SEVERITY_FATAL_ERROR] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_FATALERROR), BST_CHECKED);

	if(aSeverityTable[REPORT_SEVERITY_SM] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_SM), BST_CHECKED);

	if(aSeverityTable[REPORT_SEVERITY_CONSOLE] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_CONSOLE), BST_CHECKED);

	if(aSeverityTable[REPORT_SEVERITY_DEBUG_RX] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_DEBUGRX), BST_CHECKED);

	if(aSeverityTable[REPORT_SEVERITY_DEBUG_TX] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_DEBUGTX), BST_CHECKED);

	if(aSeverityTable[REPORT_SEVERITY_DEBUG_CONTROL] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_SEV_DEBUGCONTROL), BST_CHECKED);

	/* Modules */
	if(aModuleTable[NAVC_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_NAVC), BST_CHECKED);

	if(aModuleTable[BT_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_BT), BST_CHECKED);

	if(aModuleTable[FM_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_FM), BST_CHECKED);

	if(aModuleTable[MCPF_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_MCPF), BST_CHECKED);

	if(aModuleTable[TRANS_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_TRANS), BST_CHECKED);

	if(aModuleTable[QUEUE_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_QUEUE), BST_CHECKED);

	if(aModuleTable[REPORT_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_REPORT), BST_CHECKED);

	if(aModuleTable[IFSLPMNG_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_IFSLPMNG), BST_CHECKED);

	if(aModuleTable[BUS_DRV_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_BUSDRV), BST_CHECKED);

	if(aModuleTable[HAL_ST_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_HALUART), BST_CHECKED);

	if(aModuleTable[UI_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_UI), BST_CHECKED);

	if(aModuleTable[ADS_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_ADS), BST_CHECKED);

	if(aModuleTable[RR_MODULE_LOG] == '1')
			Button_SetCheck(GetDlgItem(_REPROT_AppWnd, IDC_MOD_RR), BST_CHECKED);
}
#endif
