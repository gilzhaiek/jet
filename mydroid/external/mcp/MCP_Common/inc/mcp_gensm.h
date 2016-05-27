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
*   BRIEF:          This file defines the API of the general state machine
*
*   DESCRIPTION:    General
*             		This file defines the API of the general state machine
*
*   AUTHOR:            Malovany Ram
*
\*******************************************************************************/

#ifndef __MCP_GENSM_H__
#define __MCP_GENSM_H__

/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/

#include "mcp_hal_defs.h"


/********************************************************************************
 *
 * Types
 *
 *******************************************************************************/

/* action function type definition */
typedef void (*MCP_TGenSM_action) (void *pData);
typedef const char * (*Mcp_TEnumTString)( McpInt status);

/* State/Event cell */
typedef  struct _MCP_TGenSM_actionCell
{
    McpU32       uNextState; 	/**< next state in transition */
    MCP_TGenSM_action   fAction;    /**< action function */
} MCP_TGenSM_actionCell;

/* 
 * matrix type 
 * Although the state-machine matrix is actually a two-dimensional array, it is treated as a single 
 * dimension array, since the size of each dimeansion is only known in run-time
 */
typedef MCP_TGenSM_actionCell *TGenSM_matrix;


/********************************************************************************
 *
 * Data Structures
 *
 *******************************************************************************/
typedef struct MCP_TGenSM
{
    TGenSM_matrix   tMatrix;       	/**< next state/action matrix */
    McpU32	uStateNum;         		/**< Number of states in the matrix */
    McpU32       uEventNum;       	/**< Number of events in the matrix */
    McpU32       uCurrentState;     /**< Current state */
    McpU32       uPreviousState;     /**< previous state */
    McpU32       uEvent;            /**< Last event sent */
    void            *pData;        	/**< Last event data */
    McpBool	bEventPending;     		/**< Event pending indicator */
    McpBool         bInAction;     	/**< Evenet execution indicator */
    Mcp_TEnumTString         pEventTString;     	/**< Event to String function */
    Mcp_TEnumTString         pStateTString;     	/**< State to String function */

} MCP_TGenSM;


/********************************************************************************
 *
 * Function declarations
 *
 *******************************************************************************/


/*-------------------------------------------------------------------------------
 * MCP_GenSM_Create()
 *
 * Brief:  
 *      Create a SM machine with deafult values.
 *
 * Description:
 *      Create a SM machine with deafult values.
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      pGenSM [in] - hanlde to the generic state machine object.
 *      uStateNum [in] - number of states in the state machine.
 *      EventNum [in] - number of events in the state machine.
 *      pMatrix [in] -  pointer to the event/actions matrix.
 *      uInitialState [in] - Initial state.
 *
 * Returns:
 *		N/A
 */

void MCP_GenSM_Create (MCP_TGenSM *pGenSM , 
							McpU32 uStateNum, 
							McpU32	uEventNum, 
							MCP_TGenSM_actionCell *pMatrix,
							McpU32 uInitialState,
							Mcp_TEnumTString pEventTString,
							Mcp_TEnumTString pStateTString);

/*-------------------------------------------------------------------------------
 * MCP_GenSM_Destroy()
 *
 * Brief:  
 *		Currently nothing.
 *
 * Description:
 *		N/A
 *
 * Type:
 *		N/A
 *
 * Parameters:
 *		N/A
 *
 * Returns:
 *		N/A
 */

void MCP_GenSM_Destroy (MCP_TGenSM *pGenSM);


/*-------------------------------------------------------------------------------
 * MCP_GenSM_Event()
 *
 * Brief:  
 *      Enter an event to the state machine.
 *
 * Description:
 *      Enter an event to the state machine.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      pGenSM [in] - hanlde to the generic state machine object.
 *      uEvent [in] - event to enter the state machine.
 *      pData [in] - Client data for the current event.
 *
 * Returns:
 *		N/A
 */

void MCP_GenSM_Event (MCP_TGenSM *pGenSM, McpU32 uEvent, void *pData);

/*-------------------------------------------------------------------------------
 * MCP_GenSM_GetCurrentState()
 *
 * Brief:  
 *      Get the current state machine.
 *
 * Description:
 *      Get the current state machine.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      pGenSM [in] - hanlde to the generic state machine object.
 *
 * Returns:
 *		N/A
 */
McpU32 MCP_GenSM_GetCurrentState (MCP_TGenSM *pGenSM);

#endif /* __MCP_GENSM_H__ */

