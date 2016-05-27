/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_api.h
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#ifndef __SUPLC_API_H
#define __SUPLC_API_H

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/* Location Fix Mode */
typedef enum{
  SUPLC_FIXMODE_AUTONOMOUS,           /* Perform Autonomous position fix */
  SUPLC_FIXMODE_MSBASED,              /* Perform MS Based position fix  */
  SUPLC_FIXMODE_MSASSISTED            /* Perform MS Assisted position fix */

}ESUPLC_LocFixMode;

/*  SUPLC command opcode definition */
typedef enum{
    SUPLC_CMD_FROM_NETWORK = 200,
    SUPLC_CMD_SET_MODE,
    SUPLC_CMD_INIT_CORE,
    SUPLC_CMD_DEINIT_CORE,
    SUPLC_CMD_MSGBOX_RESP, /* Added for NI */
    SUPLC_CMD_EE_ACTIVATE,
    SUPLC_CMD_SET_SLP,
    SUPLC_RSP_NAVC_STATE,
    SUPLC_CMD_SERVER_CONNECT
}ESUPLC_CmdOpcode;

/* Contains network's messages destined to SUPLC */
typedef struct suplcNetMsg{
    char a_message[8192]; /* KlockWork Changes */
    McpU16 u16_msgSize;

}TSUPLC_NetworkMsg;

/* Contains location fix mode message destined to SUPLC */
typedef struct suplcFixMode{
    ESUPLC_LocFixMode e_locFixMode;
    McpU16 u16_fixFrequency;

}TSUPLC_FixMode;

/* Contains all the message structure that come on SUPLC's command queue*/
typedef union suplcCmdParams{
    TSUPLC_NetworkMsg s_networkMsg;
    TSUPLC_FixMode s_fixMode;

}TSUPLC_CmdParams;

//Sanjay Added

typedef struct suplcSetServer{
    const char *ServerAddress ;
    McpU16 PortNo;

}TSUPLC_SetServer;


// Added for NI
void passNotificationResult(int res,int tid);
int  SetNavcInit(void *, int);
#ifdef __cplusplus
}
#endif /* __cplusplus*/

#endif /*__SUPLC_API_H */
