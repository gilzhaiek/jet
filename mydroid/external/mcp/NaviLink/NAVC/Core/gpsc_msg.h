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
 * FileName         :   gpsc_msg.h
 *
 * Description      :
 *
 * Author           :   Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef GPSC_MSG_H
#define GPSC_MSG_H

#include "gpsc_sap.h"

#define MAX_DIAGSTRING_LENGTH 200

extern void gpsc_diagnostic(T_GPSC_diag_level DiagLevel ,char *p_sDiagMsg, ...);
extern void gpsc_status_msg(char *ps_DiagMsg, ...);
#ifdef _DEBUG
/* GPSC_ASSERT Reports Fault Location, and Spins Infinitely */
#define GPSC_ASSERT(bool_condition) {                                \
if (!bool_condition)                                            \
  {                                                             \
    gpsc_diagnostic(GPSC_DIAG_ERROR_WARNING, "GPSC_ASSERT FAULT in %s.%d", __FILE__, __LINE__); \
    while(1);                                                   \
  }                                                             \
}
/* FATAL Reports the cause of a fatal error, and Spins Infinitely */
#else
/* In Production, GPSC_ASSERT() is nonexistent, as is FATAL() */
#define GPSC_ASSERT(bool_condition)
#endif /* _DEBUG */

#define STATEMACHINE(OldState, NewState) {\
   gpsc_diagnostic(GPSC_DIAG_GPSC_INTERNAL_STATUS, "State: %s -> %s",\
   s_StateNames[OldState],s_StateNames[NewState]); \
}

#define ERRORMSG(ErrorMsg) {\
    gpsc_diagnostic(GPSC_DIAG_ERROR_WARNING,ErrorMsg);\
}

#define ERRORMSG2(ErrorMsg,arg1) {\
    gpsc_diagnostic(GPSC_DIAG_ERROR_WARNING,ErrorMsg,arg1);\
}

#define ERRORMSG3(ErrorMsg,arg1,arg2) {\
    gpsc_diagnostic(GPSC_DIAG_ERROR_WARNING,ErrorMsg,arg1,arg2);\
}
#define STATUSMSG gpsc_status_msg


#define SAPMSG(SAPMsg) {\
    gpsc_diagnostic(GPSC_DIAG_GPSM_INTERACTION,SAPMsg);\
}


#define AI2RX(AI2Msg, Ai2Id) {\
    gpsc_diagnostic(GPSC_DIAG_AI2_INTERACTION, "AI2: Rx %s, MsgId : %d", AI2Msg, Ai2Id);\
}

#define AI2TX(AI2Msg, Ai2Id) {\
    gpsc_diagnostic(GPSC_DIAG_AI2_INTERACTION, "AI2: Tx %s, MsgId : %d", AI2Msg, Ai2Id);\
}

#define SAPENTER(Function) {\
    gpsc_diagnostic(GPSC_DIAG_GPSM_INTERACTION, "SAP: Enter %s",#Function);\
    if(gp_zGPSCControl ==NULL)\
    {return GPSC_UNITIALIZED_FAIL;}\
}

#define SAPLEAVE(Function,ReturnValue) {\
    T_GPSC_result Result;\
    Result = ReturnValue;\
    gpsc_diagnostic(GPSC_DIAG_GPSM_INTERACTION, "SAP: Return from %s with value %d", #Function,Result);\
    return Result;\
}

#define SAPCALL(Function) {\
    gpsc_diagnostic(GPSC_DIAG_GPSM_INTERACTION,"SAP: Call %s", #Function);\
}


#endif   /* GPSC_MSG_H */
