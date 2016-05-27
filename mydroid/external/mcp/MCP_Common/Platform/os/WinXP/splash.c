/***************************************************************************
 *
 * File:
 *     $Workfile$ for iAnywhere Blue SDK, Version 2.1.2
 *     $Revision$
 *
 * Description: Source file for sample application splash screen.
 *
 * Copyright 2002-2005 Extended Systems, Inc.
 * Portions copyright 2005-2006 iAnywhere Solutions, Inc.
 * All rights reserved. All unpublished rights reserved.
 *
 * Unpublished Confidential Information of iAnywhere Solutions, Inc.  
 * Do Not Disclose.
 *
 * No part of this work may be used or reproduced in any form or by any 
 * means, or stored in a database or retrieval system, without prior written 
 * permission of iAnywhere Solutions, Inc.
 * 
 * Use of this work is governed by a license granted by iAnywhere Solutions, 
 * Inc.  This work contains confidential and proprietary information of 
 * iAnywhere Solutions, Inc. which is protected by copyright, trade secret, 
 * trademark and other intellectual property rights.
 *
 ****************************************************************************/
 
#include "stdio.h"
#include "windows.h"
#include "resource.h"

#include "mcp_defs.h"

/* Turn this define on when the splash screen is desired in the application. */
//#define DEMO        1

#define XA_NAME             "iAnywhere Embedded SDK"
#if XA_DEBUG == XA_ENABLED
#define XA_VERSION          "Version 2.1.1 Debug Build (© 2005 iAnywhere Solutions, Inc.)"
#else
#define XA_VERSION          "Version 2.1.1 Release Build (© 2005 iAnywhere Solutions, Inc.)"
#endif

#ifdef DEMO

static BOOL CALLBACK SplashDlgProc(HWND hDlg, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
    RECT    ww_rect, b_rect, v_rect, w_rect;     /* Window, Bitmap and Version rectangles */
    DWORD   w_width, w_height; //, w_left, w_top;
//    DWORD   w_temp1, w_temp2;
    float   aspect;

    switch (uMsg) {
    case WM_INITDIALOG:
        /* Position and Resize the Splash Screen */
        GetWindowRect(hDlg, &ww_rect);
        GetClientRect(hDlg, &w_rect);
        GetClientRect(GetDlgItem(hDlg, IDB_BITMAP), &b_rect);
        GetClientRect(GetDlgItem(hDlg, IDC_VERSION), &v_rect);

//        w_temp1 = (b_rect.right - b_rect.left);
//        w_temp2 = (v_rect.right - v_rect.left);
//        w_width = max(w_temp1, w_temp2)+8+6;

        // Resize bitmap to window size
        if (v_rect.right > b_rect.right) {
            aspect = 1+ ((float)(v_rect.right - b_rect.right) / (float)b_rect.right);

            SetWindowPos(GetDlgItem(hDlg, IDB_BITMAP), 0, 4, 4, (DWORD)(b_rect.right*aspect),
                         (DWORD)(b_rect.bottom*aspect), SWP_NOZORDER);

            GetClientRect(GetDlgItem(hDlg, IDB_BITMAP), &b_rect);
        }

        w_width = max(b_rect.right, v_rect.right)+8+6;
            
        w_height = (b_rect.bottom - b_rect.top)+8;  /* Height of Bitmap w/ borders */
        w_height += (v_rect.bottom - v_rect.top)+4; /* Height of version w/ border */
        w_height += ww_rect.bottom - ww_rect.top - w_rect.bottom;

//        w_left = p_rect.left + ((p_rect.right - p_rect.left - w_width) / 2);
//        w_top = p_rect.top + ((p_rect.bottom - p_rect.top - w_height) / 2);

        SetWindowPos(hDlg, HWND_TOP, 0, 0, w_width, w_height, SWP_NOZORDER|SWP_NOMOVE);

        // Center Bitmap with native dimensions
//        SetWindowPos(GetDlgItem(hDlg, IDB_BITMAP), 0, ((w_width-b_rect.right) / 2)-4, 4,
//                     0, 0, SWP_NOZORDER|SWP_NOSIZE);

        SetWindowPos(GetDlgItem(hDlg, IDC_VERSION), 0, ((8) / 2), (b_rect.bottom-b_rect.top)+8,
                     (w_width - 8), v_rect.bottom, SWP_NOZORDER);

        SendMessage(GetDlgItem(hDlg, IDC_VERSION), WM_SETTEXT, 0, 
                    (LPARAM)XA_NAME "\n" XA_VERSION);

        if ((SetTimer(hDlg, 0, (1000*15), 0)) == 0)      /* Splash for 5 seconds */
            DestroyWindow(hDlg);
        break;

    case WM_TIMER:
        KillTimer(hDlg, 0);
    
    case WM_CLOSE:
        DestroyWindow(hDlg);
        break;
    }
    return FALSE;
}

/*  This function creates a splash dialogue when DEMO is defined. The main
    purpose of the splash dialogue is to get the ESI copyright notice in front of
    the user when SDK application are used as DEMO's.
 */
HWND Splash_Create(HINSTANCE Inst, HWND appWnd, int res_id, int CmdShow)
{
    HWND splashWnd;

    /* Demo builds enable and automatically open the splash screen */
    splashWnd = CreateDialog(Inst, MAKEINTRESOURCE(res_id), appWnd, SplashDlgProc);
    if (splashWnd)
        ShowWindow(splashWnd, CmdShow);
        
    return splashWnd;
}

#else

HWND Splash_Create(HINSTANCE Inst, HWND appWnd, int res_id, int CmdShow)
{
     MCP_UNUSED_PARAMETER(Inst);
     MCP_UNUSED_PARAMETER(appWnd);
     MCP_UNUSED_PARAMETER(res_id);
     MCP_UNUSED_PARAMETER(CmdShow);
    
    return (HWND) 0;
}

#endif                      /* DEMO */
