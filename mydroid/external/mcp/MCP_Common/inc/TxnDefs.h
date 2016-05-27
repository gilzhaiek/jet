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

/** \file   TxnDefs.h 
 *  \brief  Common TXN definitions                                  
 *
 * These defintions are used also by the SDIO/SPI adapters, so they shouldn't
 *     base on any non-standart types (e.g. use unsigned int and not McpU32)
 * 
 *  \see    
 */

#ifndef __TXN_DEFS_API_H__
#define __TXN_DEFS_API_H__


/************************************************************************
 * Defines
 ************************************************************************/
#define TXN_FUNC_ID_CTRL         0
#define TXN_FUNC_ID_BT           1
#define TXN_FUNC_ID_WLAN         2


/************************************************************************
 * Types
 ************************************************************************/
/* Transactions status (shouldn't override RES_OK and RES_ERROR values) */
typedef enum
{
    TXN_STATUS_NONE = 2,
    TXN_STATUS_OK,         
    TXN_STATUS_COMPLETE,   
    TXN_STATUS_PENDING,    
    TXN_STATUS_ERROR     

} ETxnStatus;


/*  Module definition */
typedef enum
{
    Txn_HalBusModule = 1,
	Txn_BusDrvModule,
	Txn_TxnQModule,
	Txn_TransModule,
	Txn_TransAdaptModule

} ETxnModuleId;


/*  Error identification */
typedef enum
{
    Txn_RxErr = 1,
	Txn_TxErr,
	Txn_MemErr,

} ETxnErrId;

/*  Error sevirity */
typedef enum
{
    TxnInfo = 1,
	TxnNormal,
	TxnMajor,
	TxnCritical

} ETxnErrSevirity;

/* Bus driver events type */
typedef enum
{
    Txn_InitComplInd = 1,
	Txn_DestroyComplInd,
	Txn_WriteComplInd,
	Txn_ReadReadylInd,
	Txn_ErrorInd

} ETxnEventType;

typedef struct
{
    ETxnModuleId 	eModuleId;
	ETxnErrId    	eErrId;
	ETxnErrSevirity eErrSeverity;
    char           *pErrStr;

} TTxnErrParams; 


typedef union
{
    TTxnErrParams err;       

} TTxnEventParams;


typedef struct
{
    ETxnEventType 	eEventType;
	TTxnEventParams tEventParams;

} TTxnEvent; 


typedef void (*TI_TxnEventHandlerCb)(handle_t hHandleCb, TTxnEvent * pEvent);



#endif /*__TXN_DEFS_API_H__*/
