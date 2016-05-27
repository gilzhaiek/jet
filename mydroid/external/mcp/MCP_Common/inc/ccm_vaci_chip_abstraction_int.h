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
*   FILE NAME:      ccm_vaci_chip_abstraction_int.h
*
*   BRIEF:          This file defines the internal API of the Connectivity Chip
*                   Manager (CCM) Voice and Audio Control (VAC) mapping
*                   engine component.
*                  
*
*   DESCRIPTION:    The mapping engine is a CCM-VAC internal module storing
*                   possible and current mapping between operations and 
*                   audio resources.
*
*   AUTHOR:         Ronen Kalish
*
\*******************************************************************************/
#ifndef __CCM_VACI_CHIP_ABTRACTION_INT_H__
#define __CCM_VACI_CHIP_ABTRACTION_INT_H__

/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/
#include "ccm_vaci_chip_abstration.h"
#include "mcp_hci_sequencer.h"
#include "mcp_defs.h"

/*******************************************************************************
 *
 * Macro definitions
 *
 ******************************************************************************/
#define CAL_COMMAND_PARAMS_MAX_LEN          34

#define CAL_UNUSED_PARAMETER(_PARM)     ((_PARM) = (_PARM))

/*Operation support macro */
#define OPERATION(_s)                ((_s)->bOperationSupport)
/*Resource support macro */
#define RESOURCE(_s)             ((_s)->bResourceSupport)

/*Optional Resource List macro */
#define OPTIONALRESOURCELIST(_s, _i)     ((_s)->tOpToResMap[(_i)].tOptionalResources.tOptionalResourceLists)

#define FM_PCMI_I2S_SELECT_OFFSET				(0)
#define FM_PCMI_RIGHT_LEFT_SWAP_OFFSET				(1)
#define FM_PCMI_BIT_OFFSET_VECTOR_OFFSET			(2)
#define FM_PCMI_SLOT_OFSET_VECTOR_OFFSET			(5)
#define FM_PCMI_PCM_INTERFACE_CHANNEL_DATA_SIZE_OFFSET	(9)

#define FM_I2S_DATA_WIDTH_OFFSET			(0)
#define FM_I2S_DATA_FORMAT_OFFSET			(4)
#define FM_I2S_MASTER_SLAVE_OFFSET			(6)
#define FM_I2S_SDO_TRI_STATE_MODE_OFFSET			(7)
#define FM_I2S_SDO_PHASE_WS_PHASE_SELECT_OFFSET 		(8)
#define FM_I2S_SDO_3ST_ALWZ_OFFSET		(10)
#define FM_I2S_FRAME_SYNC_RATE_OFFSET		(12)

/******************************************************************************/

/*-------------------------------------------------------------------------------
 * Cal_Resource_Data
 *
 * holds information on a specific resource to the config that was sent in CAL_ConfigResource()
 */
typedef struct _Cal_Resource_Data
{
    TCAL_CB                 CB;
    void                    *pUserData;
    ECAL_Operation          eOperation;
    ECAL_Resource           eResource;
    TCAL_DigitalConfig      *ptConfig;
    McpU8                   hciseqCmdPrms[ CAL_COMMAND_PARAMS_MAX_LEN ];
    MCP_HciSeq_Context      hciseq;
    McpHciSeqCmd            hciseqCmd[ HCI_SEQ_MAX_CMDS_PER_SEQUENCE ];
    TCAL_ResourceProperties tProperties;
    McpBool                 bIsStart;  /* for FM RX over SCO command only - indicates if this is start or stop command */
} Cal_Resource_Data;

/*-------------------------------------------------------------------------------
 * Cal_Resource_Config
 *
 * holds information according to resource.
 */

typedef struct _Cal_Resource_Config
{
        Cal_Resource_Data   tResourceConfig[CAL_RESOURCE_MAX_NUM];

} Cal_Resource_Config;

typedef struct
{
	char	*	keyName;
	McpS32 value;
}_Cal_Codec_Config;

typedef enum
{
	FM_PCMI_RIGHT_LEFT_SWAP_LOC = 0,
	FM_PCMI_BIT_OFFSET_VECTOR_LOC,
	FM_PCMI_SLOT_OFSET_VECTOR_LOC ,
	FM_PCMI_PCM_INTERFACE_CHANNEL_DATA_SIZE_LOC,
	FM_PCM_PARAM_MAX_VALUE_LOC = FM_PCMI_PCM_INTERFACE_CHANNEL_DATA_SIZE_LOC,
}_Cal_Fm_Pcm_Param_Loc;
typedef enum
{
	FM_I2S_DATA_WIDTH_LOC = 0,
	FM_I2S_DATA_FORMAT_LOC,
	FM_I2S_MASTER_SLAVE_LOC,
	FM_I2S_SDO_TRI_STATE_MODE_LOC,
	FM_I2S_SDO_PHASE_WS_PHASE_SELECT_LOC,
	FM_I2S_SDO_3ST_ALWZ_LOC,
	FM_I2S_PARAM_MAX_VALUE_LOC = FM_I2S_SDO_3ST_ALWZ_LOC,
}_Cal_Fm_I2s_Param_Loc;

extern _Cal_Codec_Config fmI2sConfigParams[];
extern _Cal_Codec_Config fmPcmConfigParams[];

void CAL_Config_CB_Complete(CcmaClientEvent *pEvent);
void CAL_Config_Complete_Null_CB(CcmaClientEvent *pEvent);

#endif /* __CCM_VACI_CHIP_ABTRACTION_INT_H__ */

