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

#include <crtdbg.h>
#include "windows.h"
#include "stdio.h"
#include "mcp_hal_misc.h"
#include "mcp_main.h"
#include "mcpf_defs.h"


/****************************************************************************
 *
 * Globals
 *
 ****************************************************************************/

/* Handle of the Applicaiton main window. */
static HWND   ActiveWnd = 0;
static HANDLE appScheduleEvent;
static HANDLE messageThread;

/****************************************************************************
 *
 * Prototypes
 *
 ****************************************************************************/

/*---------------------------------------------------------------------------
 *
 * Function prototypes
 */
/*
 * Application functions 
 */
HWND *MCP_App( HINSTANCE Inst, HINSTANCE PrevInst, LPTSTR CmdLine, int CmdShow, handle_t hMcpf );
void  APP_Deinit(void);
void  APP_Thread(void);


/*
 * Internal functions
 */
void PumpMessages(void);


/*---------------------------------------------------------------------------
 *             main()
 *---------------------------------------------------------------------------
 *
 * Synopsis:  The main program. Initiailizes the Mini OS which in turn 
 *            initializes the stack.  It then initializes the application, 
 *            adds the application thread to the OS, and starts the Mini OS.
 *
 * Return:    Status of the operation.
 */
int PASCAL WinMain(HINSTANCE Inst, HINSTANCE PrevInst, LPTSTR CmdLine, int CmdShow)
{
    HWND   *appWnd;
	handle_t	hMcpf;
	
	// Enables tracking and reporting on shutdown.
	_CrtSetDbgFlag (
		_CRTDBG_ALLOC_MEM_DF |
		_CRTDBG_LEAK_CHECK_DF);
	_CrtSetReportMode ( _CRT_ERROR,
		_CRTDBG_MODE_DEBUG);

    printf(("Command line read %s\n", CmdLine));

    appScheduleEvent = CreateEvent(NULL, FALSE, TRUE, NULL);
    MCPF_Assert(appScheduleEvent);

    messageThread = GetCurrentThread();

    hMcpf = MCP_Init(CmdLine); /* protected with MCPF_Assertion */

	if ((appWnd = MCP_App(Inst, PrevInst, CmdLine, CmdShow, hMcpf)) != NULL) {

        ActiveWnd = *appWnd;

        while (ActiveWnd) {

            /* Wait until a message is available, then process it */
            MsgWaitForMultipleObjectsEx(0, NULL, 500,
                    QS_ALLEVENTS | QS_ALLINPUT | QS_ALLPOSTMESSAGE , 0);
            /* Processes any messages currently on the queue. */
            PumpMessages();
        }
    }

	APP_Deinit();
	MCP_Deinit(hMcpf);
	
    /* TODO: Bother to kill the app thread? or just exit? */

    return 0;
}


/*---------------------------------------------------------------------------
 *             PumpMessages()
 *---------------------------------------------------------------------------
 *
 * Synopsis:  This function is responsible for processing window messages.
 *
 */
void PumpMessages(void)
{
    MSG     w_msg;
    HWND    hWnd;

    while (PeekMessage(&w_msg, NULL, 0, 0, PM_NOREMOVE)) {

        if (GetMessage(&w_msg, NULL, 0, 0) == 0) {
            if ((hWnd = ActiveWnd) != 0) {
                printf(("Deinitializing\n"));
                ActiveWnd = 0;
                //APP_Deinit();
            }
            break;
        }

        if (w_msg.message == WM_APP+100) {
            /* This message is used to tell the message pump to change the 
             * window handle to the currently active dialog. It is necessary to
             * have ActiveWnd point to the correct dialog to insure keyboard
             * selection messages (tab key, down key, etc.) work.
             */
            if (w_msg.lParam) {
                MCPF_Assert(IsWindow((HWND)w_msg.lParam));
                ActiveWnd = (HWND)w_msg.lParam;
            }
            continue;
        }

        if (!ActiveWnd || !IsDialogMessage(ActiveWnd, &w_msg)) {
            TranslateMessage( &w_msg );
            DispatchMessage( &w_msg );
        }
    }
}

