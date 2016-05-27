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
#include <stdarg.h>
#include <string.h>
#include "Logger.h"
#include <tchar.h>
#include <stdio.h> 

#if defined(_DEBUG) || defined(LOG_RELEASE)

#ifdef USE_DEBUG_IDENTS
int CBFCDebugIdent::m_dIdent= -1;
#endif

void UninitializeDebug();

#define REGISTER_FUNC           ("RegisterLogSource")
#define REGISTER_NAME_FUNC      ("RegisterLogSourceName")
#define ADD_STRING_FUNC         ("AddDebugString")

typedef DWORD (*REGISTER_DBG)(HINSTANCE);
typedef DWORD (*REGISTER_NAME_DBG)(LPCTSTR);
typedef DWORD (*ADD_DBG_STRING)(DWORD, LPCTSTR, int, int, LPCTSTR);

struct CDebugModuleData
{
public:
    CDebugModuleData()
    {
        szModuleDebugName= NULL;
        dwDebugHandle= 0;
    };

    ~CDebugModuleData()
    {
        if (szModuleDebugName)
        {
            free(szModuleDebugName);
            szModuleDebugName= NULL;
        }
    };


    DWORD dwDebugHandle;
    LPTSTR szModuleDebugName;
};

HINSTANCE hDebugWinDLL= NULL;
static CDebugModuleData _dmd[MAX_MODULES];

REGISTER_DBG RegisterDebug= NULL;
REGISTER_NAME_DBG RegisterDebugName= NULL;
ADD_DBG_STRING AddDebugString= NULL;


class AutoUninitialize
{
public:
    ~AutoUninitialize() { UninitializeDebug(); };
} UninitializeClass;

void Logger_SetModuleName(LPCTSTR lpszModuleDebugName, int module)
{
    if (module<0 || module>=MAX_MODULES)
        return;

    _dmd[module].szModuleDebugName= _strdup(lpszModuleDebugName);
}

BOOL InitializeDebugWin()
{
    if (hDebugWinDLL)
        return TRUE;

    hDebugWinDLL= LoadLibrary(DEBUGWIN_DLL_NAME);

#ifdef _WIN32
    if (!hDebugWinDLL)
        return FALSE;
#else
    if (hDebugWinDLL <= HINSTANCE_ERROR)
    {
        hDebugWinDLL= NULL;
        return FALSE;
    }
#endif

    RegisterDebug= (REGISTER_DBG)GetProcAddress(hDebugWinDLL, REGISTER_FUNC);
    RegisterDebugName= (REGISTER_NAME_DBG)GetProcAddress(hDebugWinDLL, REGISTER_NAME_FUNC);
    AddDebugString= (ADD_DBG_STRING)GetProcAddress(hDebugWinDLL, ADD_STRING_FUNC);
    

    if (!RegisterDebug || !AddDebugString || !RegisterDebugName)
    {
        //ASSERT(FALSE);
        hDebugWinDLL= NULL;
        return FALSE;
    }

    return TRUE;
}

void UninitializeDebug()
{
#ifdef _WIN32
    if (hDebugWinDLL)
#else
    if (hDebugWinDLL > HINSTANCE_ERROR)
#endif
    {
        FreeLibrary(hDebugWinDLL);
        hDebugWinDLL= NULL;
    }
}

void Logger_LogString(LPCTSTR lpszFormat, ...)
{
    TCHAR szInfo[4196];

    va_list marker;
    va_start( marker, lpszFormat );     /* Initialize variable arguments. */
    vsprintf(szInfo, lpszFormat, marker);

    Logger_LogString(0, _T(""), 0, DL_INFO9, _T("%s"), szInfo);
}

void Logger_LogString(int module, LPCTSTR lpszFileName, int nLine, int nLevel, LPCTSTR lpszFormat, ...)
{
    if (module<0 || module>=MAX_MODULES)
        return;

    if (!InitializeDebugWin())
        return;

    if (!_dmd[module].dwDebugHandle)
    {
        _dmd[module].dwDebugHandle= _dmd[module].szModuleDebugName ?
                        RegisterDebugName(_dmd[module].szModuleDebugName) :
                        RegisterDebug(GetModuleHandle(NULL));

        if (!_dmd[module].dwDebugHandle)
            return;
    }

    va_list marker;
    va_start( marker, lpszFormat );     /* Initialize variable arguments. */

    TCHAR szInfo[4196];

#ifdef USE_DEBUG_IDENTS
    if (CBFCDebugIdent::m_dIdent > 0)
    {
        memset(szInfo, ' ', CBFCDebugIdent::m_dIdent * 2);
        vsprintf(szInfo + CBFCDebugIdent::m_dIdent*2, lpszFormat, marker);
    }
    else
#endif
        vsprintf(szInfo, lpszFormat, marker);

    DWORD lResult;

    lResult= AddDebugString(_dmd[module].dwDebugHandle, lpszFileName, nLine, nLevel, szInfo);
    
    if (lResult== (DWORD)-1)
    {
        // Try to register again
        _dmd[module].dwDebugHandle= _dmd[module].szModuleDebugName ?
                    RegisterDebugName(_dmd[module].szModuleDebugName) :
                    RegisterDebug(GetModuleHandle(NULL));
        if (!_dmd[module].dwDebugHandle)
            return;

        // send again the message
        AddDebugString(_dmd[module].dwDebugHandle, lpszFileName, nLine, nLevel, szInfo);
    }
    else
    {
        if (lResult!=0)
        {
            // If auto registration has done, update _dmd[module].dwDebugHandle.
            _dmd[module].dwDebugHandle= lResult;
            AddDebugString(_dmd[module].dwDebugHandle, lpszFileName, nLine, nLevel, szInfo);
        }
    }
}

void C_Logger_Trace(LPCTSTR lpszFormat, ...)
{
    TCHAR szInfo[4196];

    va_list marker;
    va_start( marker, lpszFormat );     /* Initialize variable arguments. */
    vsprintf(szInfo, lpszFormat, marker);

    Logger_LogString(0, _T(""), 0, DL_INFO9, _T("%s"), szInfo);
}

void C_Logger_Debug(int module, LPCTSTR lpszFileName, int nLine, int nLevel, LPCTSTR lpszFormat, ...)
{
    if (module<0 || module>=MAX_MODULES)
        return;

    if (!InitializeDebugWin())
        return;

    if (!_dmd[module].dwDebugHandle)
    {
        _dmd[module].dwDebugHandle= _dmd[module].szModuleDebugName ?
                        RegisterDebugName(_dmd[module].szModuleDebugName) :
                        RegisterDebug(GetModuleHandle(NULL));

        if (!_dmd[module].dwDebugHandle)
            return;
    }

    va_list marker;
    va_start( marker, lpszFormat );     /* Initialize variable arguments. */

    TCHAR szInfo[4196];

#ifdef USE_DEBUG_IDENTS
    if (CBFCDebugIdent::m_dIdent > 0)
    {
        memset(szInfo, ' ', CBFCDebugIdent::m_dIdent * 2);
        vsprintf(szInfo + CBFCDebugIdent::m_dIdent*2, lpszFormat, marker);
    }
    else
#endif
        vsprintf(szInfo, lpszFormat, marker);

    DWORD lResult;

    lResult= AddDebugString(_dmd[module].dwDebugHandle, lpszFileName, nLine, nLevel, szInfo);
    
    if (lResult== (DWORD)-1)
    {
        // Try to register again
        _dmd[module].dwDebugHandle= _dmd[module].szModuleDebugName ?
                    RegisterDebugName(_dmd[module].szModuleDebugName) :
                    RegisterDebug(GetModuleHandle(NULL));
        if (!_dmd[module].dwDebugHandle)
            return;

        // send again the message
        AddDebugString(_dmd[module].dwDebugHandle, lpszFileName, nLine, nLevel, szInfo);
    }
    else
    {
        if (lResult!=0)
        {
            // If auto registration has done, update _dmd[module].dwDebugHandle.
            _dmd[module].dwDebugHandle= lResult;
            AddDebugString(_dmd[module].dwDebugHandle, lpszFileName, nLine, nLevel, szInfo);
        }
    }
}

#endif // _DEBUG || LOG_RELEASE
