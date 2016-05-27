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


/** \file   navc_init.h
 *  \brief  NAVC stack initialization API file
 *
 *  NAVC initialization module provides:
 * - NAVC initialization/de-initialization
 * - NAVC command: stak start/stop
 *
 *  \see    lm_sys_main.c
 */

#ifndef __NAVC_INIT_API_H__
#define __NAVC_INIT_API_H__

#include "mcpf_defs.h"

/************************************************************************
 * Defines
 ************************************************************************/


/************************************************************************
 * Macros
 ************************************************************************/


/************************************************************************
 * Types
 ************************************************************************/



/************************************************************************
 * Functions
 ************************************************************************/

/**
 * \fn     NAVC_Create
 * \brief  Initializes NAVC stack
 *
 * Allocate, clear NAVC object, initialize NAVC stack, creates threads and
 * registrate NAVC stack to CCM (Con Chip Manager)
 *
 * \note
 * \param   hMcpf - MCPF handle
 * \param   *CmdLine - Command Line string
 * \return  status of operation: OK or Error
 * \sa      NAVC_Destroy
 */
 #ifdef __cplusplus 
extern "C" {
EMcpfRes NAVC_Create(handle_t hMcpf, char *CmdLine);
}
#else
EMcpfRes NAVC_Create(handle_t hMcpf, char *CmdLine);
#endif

/**
 * \fn     NavcActionHandler
 * \brief  Perform NAVC command
 *
 * Start NAVC command execution
 *
 * \note
 * \param   hNavc  - pointer to NAVC control object
 * \param   pMsg   - pointer to MCPF message containing NAVC command
 * \return  status of operation: OK or Error
 * \sa      NavcActionHandler
 */
void NavcActionHandler (handle_t hNavc, TmcpfMsg * pMsg);


/**
 * \fn     NAVC_Destroy
 * \brief  De-initialize NAVC stack
 *
 * De-registers NAVC stack from CCM and delete NAVC object
 *
 * \note
 * \param   void
 * \return  status of operation: OK or Error
 * \sa      NAVC_Create
 */
EMcpfRes NAVC_Destroy (void);


#endif /*__NAVC_INIT_API_H__*/
