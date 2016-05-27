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

#include <windows.h>
#include <commctrl.h>
#include <stdio.h>
#include "resource.h"
#ifdef _FM_APP
#include "fmc_config.h"
#endif
#include "mcp_ui.h"
#include "bmtrace.h"


#ifndef UNUSED_PARAMETER
#define UNUSED_PARAMETER(_PARM) 	((_PARM) = (_PARM))
#endif


#ifdef _BT_APP
HWND *Btips_App( HINSTANCE Inst, HINSTANCE PrevInst, LPTSTR CmdLine, int CmdShow );
#endif // _BT_APP

#ifdef _FM_APP
extern HWND *FM_RX_APP_Init( HINSTANCE Inst, HINSTANCE PrevInst, LPTSTR CmdLine, int CmdShow );
extern HWND *FM_TX_APP_Init( HINSTANCE Inst, HINSTANCE PrevInst, LPTSTR CmdLine, int CmdShow );
extern HWND FM_RX_Deinit(void);
extern HWND FM_TX_Deinit(void);
#endif // _FM_APP

// The apps
#ifdef _NAVC_APP
extern HWND *NAVC_APP_Init( HINSTANCE Inst, HINSTANCE PrevInst, LPTSTR CmdLine, int CmdShow );
#endif

#ifdef _DEBUG
extern HWND *Report_APP_Init( HINSTANCE Inst, HINSTANCE PrevInst, LPTSTR CmdLine, int CmdShow, handle_t hMcpf );
#endif

extern HWND *THROUGHPUT_APP_Init( HINSTANCE Inst, HINSTANCE PrevInst, LPTSTR CmdLine, int CmdShow );
extern void MDGA_Init(void);
extern void MDGA_Deinit(void);
extern HWND Splash_Create(HINSTANCE Inst, HWND appWnd, int res_id, int CmdShow);


/* Internal Functions */
static BOOL CALLBACK AppWndProc(HWND, UINT, WPARAM, LPARAM);
static void InitSecurityCheckBoxes(HWND hWnd);


/* GUI Application internal variables, etc */
static HINSTANCE        hInst = 0;
static HWND             bluemgrWnd = 0;
HWND	McpAppWnd = 0; 
handle_t g_hMcpf;


/*---------------------------------------------------------------------------
 *            APP_Init()
 *---------------------------------------------------------------------------
 *
 * Synopsis:  Main entry point for our application
 *
 * Return:    
 *
 */
HWND *MCP_App( HINSTANCE Inst, HINSTANCE PrevInst, LPTSTR CmdLine, int CmdShow, handle_t hMcpf )
{	
    
    MCP_UNUSED_PARAMETER(CmdLine);
    MCP_UNUSED_PARAMETER(PrevInst);
    
    hInst = Inst;
#ifdef _NAVC_APP
	ui_init(hMcpf);
#endif
	g_hMcpf = hMcpf;

    InitCommonControls();

    /* Create Application GUI */
    McpAppWnd = CreateDialog(hInst, MAKEINTRESOURCE(IDD_MCPAPP), NULL, AppWndProc);
    if (McpAppWnd == NULL) 
	{
      return 0;
    }

    ShowWindow( McpAppWnd, CmdShow ) ;
    

/*BT   Disable button */
#ifndef _BT_APP
       EnableWindow(GetDlgItem(McpAppWnd, ID_BT_BUTTON), FALSE);
#endif

       /*FM   Disable button */
#ifndef _FM_APP
#if (FMC_CONFIG_FM_STACK == FMC_CONFIG_DISABLED)
       EnableWindow(GetDlgItem(McpAppWnd, ID_FMRX_BUTTON), FALSE);
       EnableWindow(GetDlgItem(McpAppWnd, ID_FMTX_BUTTON), FALSE);
#endif
#endif

#ifndef _NAVC_APP
	    EnableWindow(GetDlgItem(McpAppWnd, ID_NAVC_BUTTON), FALSE) ;
#endif

#ifndef _DEBUG
	    EnableWindow(GetDlgItem(McpAppWnd, ID_REPORT_BUTTON), FALSE) ;
#endif

    return &McpAppWnd;
}

/*---------------------------------------------------------------------------
 *            APP_Deinit()
 *---------------------------------------------------------------------------
 *
 * Synopsis:  Deinitializes the Headset Audio Gateway
 *
 * Return:    
 *
 */
void  APP_Deinit(void)
{
#ifdef _NAVC_APP
	ui_destroy();
#endif
}

/*---------------------------------------------------------------------------
 *            APP_Thread()
 *---------------------------------------------------------------------------
 *
 * Synopsis:  Unused
 *
 * Return:    
 *
 */
void APP_Thread(void)
{
    /* Unused */
}

// Following Show..... functions are used to start showing the individual dialogs for the
// mini applications. Please insert code to start showing dialogs.



#ifdef _FM_APP
void ShowFMRx()
{
#if (FMC_CONFIG_FM_STACK == FMC_CONFIG_ENABLED)

	FM_RX_APP_Init(hInst, 0, 0, SW_SHOW);

#endif /* FMC_CONFIG_FM_STACK == FMC_CONFIG_ENABLED */
}

void ShowFMTx()
{
#if (FMC_CONFIG_FM_STACK == FMC_CONFIG_ENABLED)

	FM_TX_APP_Init(hInst, 0, 0, SW_SHOW);
#endif /* FMC_CONFIG_FM_STACK == FMC_CONFIG_ENABLED */
}
#endif // _FM_APP

#ifdef _BT_APP
void ShowBT()
{
	
	Btips_App(hInst, 0, 0, SW_SHOW);
}
#endif // _BT_APP


#ifdef _NAVC_APP
void ShowNAVCApp()
{
	NAVC_APP_Init(hInst, 0, 0, SW_SHOW);
}
#endif // _NAVC_APP

#ifdef _DEBUG
void ShowReportApp()
{
	Report_APP_Init(hInst, 0, 0, SW_SHOW, g_hMcpf);
}
#endif // _DEBUG

/*---------------------------------------------------------------------------
 *            AppWndProc()
 *---------------------------------------------------------------------------
 *
 * Synopsis:  Handler for Window messages generated by GUI
 *
 * Return:    
 *
 */
 BOOL CALLBACK AppWndProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
static BOOL guilessAppsInitialized = FALSE;
     
     switch (uMsg)
	{
		case WM_SETFOCUS:
			// There is no EXIT button in MCP main dialog
			// EnableWindow(GetDlgItem(McpAppWnd, BTUG_EXIT), TRUE);

            if (FALSE == guilessAppsInitialized)
            {
                /* Meanwhile, unless GUI is developed for MDG APP, initialize
                 * this application here */
                /* Since there is no sample up, we don't initialize it */
                /* MDGA_Init(); */
                guilessAppsInitialized = TRUE;
            }
			return TRUE;
        break;

		case WM_COMMAND:
			{

			switch (LOWORD(wParam)) 
			{
				case WM_DESTROY:
					
		                    /* Meanwhile, unless GUI is developed for MDG APP,
		                     * deinitialize this application here */
		                    /*MDGA_Deinit();*/
							
					PostQuitMessage(0);
					
				return TRUE;


#ifdef _FM_APP

				case ID_FMRX_BUTTON:
					ShowFMRx();
				break;
				
				case ID_FMTX_BUTTON:
					ShowFMTx();
				break;
#endif // _FM_APP
				
#ifdef _BT_APP
				case ID_BT_BUTTON:
					ShowBT();
				break;
#endif // _BT_APP

#ifdef _NAVC_APP
				case ID_NAVC_BUTTON:
					ShowNAVCApp();
				break;
#endif // _NAVC_APP

				case IDC_TRACE_BUTTON:
					CL_TRACE_DISABLE();
					CL_TRACE_SAVE();
					CL_TRACE_ENABLE();
				break;

#ifdef _DEBUG
				case ID_REPORT_BUTTON:
					ShowReportApp();
				break;
#endif //_DEBUG

				default:
				break;
			}
			}

        break;

		default:
		break;
    }
    return FALSE;
}




