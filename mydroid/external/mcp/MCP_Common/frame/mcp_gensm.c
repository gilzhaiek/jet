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
/*******************************************************************************\
*
*   FILE NAME:      mcp_gensm.c
*
*   BRIEF:          This file defines the implementation of the general state machine
*
*   DESCRIPTION:    General
*             		This file defines the implementation of the general state machine
*
*   AUTHOR:            Malovany Ram
*
\*******************************************************************************/


/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/

#include "mcp_gensm.h"
#include "mcp_defs.h"
#include "mcp_hal_log.h"

MCP_HAL_LOG_SET_MODULE(MCP_HAL_LOG_MODULE_TYPE_FRAME);

/********************************************************************************
 *
 * Function definitions
 *
 *******************************************************************************/


void MCP_GenSM_Create (MCP_TGenSM *pGenSM , 
							McpU32 uStateNum, 
							McpU32	uEventNum, 
							MCP_TGenSM_actionCell *pMatrix,
							McpU32 uInitialState,
							Mcp_TEnumTString pEventTString,
							Mcp_TEnumTString pStateTString)
{
    /* set initial values for state machine */
    pGenSM->uStateNum       = uStateNum;
    pGenSM->uEventNum       = uEventNum;
    pGenSM->tMatrix         = pMatrix;
    pGenSM->uCurrentState   = uInitialState;
    pGenSM->bEventPending   = MCP_FALSE;
    pGenSM->bInAction       = MCP_FALSE;
    pGenSM->pEventTString = pEventTString;
    pGenSM->pStateTString= pStateTString;

}


void MCP_GenSM_Destroy (MCP_TGenSM *pGenSM)
{
		/*Currently this function is NULL for future purpose */
		MCP_UNUSED_PARAMETER(pGenSM);
}

void MCP_GenSM_Event (MCP_TGenSM *pGenSM, McpU32 uEvent, void *pData)
{

    McpU32           uCurrentState;
    MCP_TGenSM_actionCell   *pCell;

    /* mark that an event is pending */
    pGenSM->bEventPending = MCP_TRUE;

    /* save event and data */
    pGenSM->uEvent = uEvent;
    pGenSM->pData = pData;

    /* if an event is currently executing, return (new event will be handled when current event is done)*/
    if (MCP_TRUE == pGenSM->bInAction)
    {		       
				MCP_LOG_INFO(("Event is already in process ! delaying execution of event: %s \n",
								pGenSM->pEventTString(pGenSM->uEvent)));
        return;
    }

    /* execute events, until none is pending */
    while (MCP_TRUE == pGenSM->bEventPending)
    {
        /* get the cell pointer for the current state and event */
        pCell = &(pGenSM->tMatrix[ (pGenSM->uCurrentState * pGenSM->uEventNum) + pGenSM->uEvent ]);

	/* print state transition information */
	MCP_LOG_INFO(("Transition from State:%s, Event:%s -->  State : %s\n",
 					pGenSM->pStateTString(pGenSM->uCurrentState),
					pGenSM->pEventTString(pGenSM->uEvent),
					pGenSM->pStateTString(pCell->uNextState)));
        
        /* mark that event execution is in place */
        pGenSM->bInAction = MCP_TRUE;

        /* mark that pending event is being handled */
        pGenSM->bEventPending = MCP_FALSE;
        
        /* keep current state */
        uCurrentState = pGenSM->uCurrentState;
	/*Save previous State - For debug info */
	 pGenSM->uPreviousState= pGenSM->uCurrentState;
        /* update current state */
        pGenSM->uCurrentState = pCell->uNextState;

        /* run transition function */
        (*(pCell->fAction)) (pGenSM->pData);

        /* mark that event execution is complete */
        pGenSM->bInAction = MCP_FALSE;
    }
}


McpU32 MCP_GenSM_GetCurrentState (MCP_TGenSM *pGenSM)
{
     return pGenSM->uCurrentState;
}
