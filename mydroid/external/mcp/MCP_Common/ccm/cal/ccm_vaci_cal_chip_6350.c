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
*   FILE NAME:      ccm_vaci_cal_chip_6350.c
*
*   BRIEF:          This file define the audio capabilities and 
*                   configuration Implamantation Dolphin 6350 PG 2.11
*                  
*
*   DESCRIPTION:    This is an internal configuration file for Dolphin 6350 PG 2.11
                    For the CCM_VACI_CAL moudule.
*
*   AUTHOR:         Malovany Ram
*
\*******************************************************************************/



/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/
#include "ccm_vaci_cal_chip_6350.h"
#include "ccm_adapt.h"
#include "mcp_endian.h"

/********************************************************************************
 *
 * Internal Functions
 *
 *******************************************************************************/
static McpU8 Set_params_Fm_Over_Bt_6350(Cal_Resource_Data *pResourceData, McpU8 *pBuffer);

/********************************************************************************
 *
 * Function definitions
 *
 *******************************************************************************/

void CAL_GetAudioCapabilities_6350 (TCAL_MappingConfiguration *tOperationtoresource,
                                    TCAL_OpPairConfig *tOpPairConfig,
                                    TCAL_ResourceSupport *tResource,
                                    TCAL_OperationSupport *tOperation)
{
    McpU32      uIndex;

    /* Update chip supported operations struct for 6350 */
    OPERATION(tOperation)[CAL_OPERATION_FM_TX]=MCP_FALSE;
    OPERATION(tOperation)[CAL_OPERATION_FM_RX]=MCP_TRUE;
    OPERATION(tOperation)[CAL_OPERATION_A3DP]=MCP_FALSE;
    OPERATION(tOperation)[CAL_OPERATION_BT_VOICE]=MCP_TRUE;
    OPERATION(tOperation)[CAL_OPERATION_WBS]=MCP_FALSE;
    OPERATION(tOperation)[CAL_OPERATION_AWBS]=MCP_FALSE;
    OPERATION(tOperation)[CAL_OPERATION_FM_RX_OVER_SCO]=MCP_TRUE;
    OPERATION(tOperation)[CAL_OPERATION_FM_RX_OVER_A3DP]=MCP_FALSE;

    /* Update chip available resources struct */
    RESOURCE(tResource)[CAL_RESOURCE_I2SH]=MCP_TRUE;
    RESOURCE(tResource)[CAL_RESOURCE_PCMH]=MCP_TRUE;
    RESOURCE(tResource)[CAL_RESOURCE_PCMT_1]=MCP_FALSE;
    RESOURCE(tResource)[CAL_RESOURCE_PCMT_2]=MCP_FALSE;
    RESOURCE(tResource)[CAL_RESOURCE_PCMT_3]=MCP_FALSE;
    RESOURCE(tResource)[CAL_RESOURCE_PCMT_4]=MCP_FALSE;
    RESOURCE(tResource)[CAL_RESOURCE_PCMT_5]=MCP_FALSE;
    RESOURCE(tResource)[CAL_RESOURCE_PCMT_6]=MCP_FALSE;
    RESOURCE(tResource)[CAL_RESOURCE_PCMIF]=MCP_TRUE;
    RESOURCE(tResource)[CAL_RESOURCE_FMIF]=MCP_TRUE;
    RESOURCE(tResource)[CAL_RESOURCE_FM_ANALOG]=MCP_TRUE;
    RESOURCE(tResource)[CAL_RESOURCE_CORTEX]=MCP_FALSE;
    RESOURCE(tResource)[CAL_RESOURCE_FM_CORE]=MCP_TRUE;

    /* Update operations to resource mapping struct */
    /* FM RX operation to resource mapping */
    tOperationtoresource->tOpToResMap[CAL_OPERATION_FM_RX].eOperation=CAL_OPERATION_FM_RX;
    tOperationtoresource->tOpToResMap[CAL_OPERATION_FM_RX].tMandatoryResources.eResources[0]=CAL_RESOURCE_FM_CORE;
    tOperationtoresource->tOpToResMap[CAL_OPERATION_FM_RX].tMandatoryResources.uNumOfResources=1;
    OPTIONALRESOURCELIST(tOperationtoresource, CAL_OPERATION_FM_RX)[0].eOptionalResource=CAL_RESOURCE_PCMH; /* TODO ronen: is this valid? */
    OPTIONALRESOURCELIST(tOperationtoresource, CAL_OPERATION_FM_RX)[0].tDerivedResourcesList.eResources[0]=CAL_RESOURCE_FMIF;
    OPTIONALRESOURCELIST(tOperationtoresource, CAL_OPERATION_FM_RX)[0].tDerivedResourcesList.uNumOfResources=1;
    OPTIONALRESOURCELIST(tOperationtoresource, CAL_OPERATION_FM_RX)[1].eOptionalResource=CAL_RESOURCE_I2SH;
    OPTIONALRESOURCELIST(tOperationtoresource, CAL_OPERATION_FM_RX)[1].tDerivedResourcesList.eResources[0]=CAL_RESOURCE_FMIF;
    OPTIONALRESOURCELIST(tOperationtoresource, CAL_OPERATION_FM_RX)[1].tDerivedResourcesList.uNumOfResources=1;                                          
    OPTIONALRESOURCELIST(tOperationtoresource, CAL_OPERATION_FM_RX)[2].eOptionalResource=CAL_RESOURCE_FM_ANALOG;
    OPTIONALRESOURCELIST(tOperationtoresource, CAL_OPERATION_FM_RX)[2].tDerivedResourcesList.uNumOfResources=0;                                          
    tOperationtoresource->tOpToResMap[CAL_OPERATION_FM_RX].tOptionalResources.uNumOfResourceLists=3;

    /* BT_VOICE operation to resource mapping */
    tOperationtoresource->tOpToResMap[CAL_OPERATION_BT_VOICE].eOperation=CAL_OPERATION_BT_VOICE;
    tOperationtoresource->tOpToResMap[CAL_OPERATION_BT_VOICE].tMandatoryResources.eResources[0]=CAL_RESOURCE_PCMIF;
    tOperationtoresource->tOpToResMap[CAL_OPERATION_BT_VOICE].tMandatoryResources.uNumOfResources=1;
    OPTIONALRESOURCELIST(tOperationtoresource, CAL_OPERATION_BT_VOICE)[0].eOptionalResource=CAL_RESOURCE_PCMH;
    OPTIONALRESOURCELIST(tOperationtoresource, CAL_OPERATION_BT_VOICE)[0].tDerivedResourcesList.uNumOfResources=0;
    tOperationtoresource->tOpToResMap[CAL_OPERATION_BT_VOICE].tOptionalResources.uNumOfResourceLists=1;

    /* FM_RX_OVER_SCO operation to resource mapping */
    tOperationtoresource->tOpToResMap[CAL_OPERATION_FM_RX_OVER_SCO].eOperation=CAL_OPERATION_FM_RX_OVER_SCO;
    tOperationtoresource->tOpToResMap[CAL_OPERATION_FM_RX_OVER_SCO].tMandatoryResources.eResources[0]=CAL_RESOURCE_PCMIF;
    tOperationtoresource->tOpToResMap[CAL_OPERATION_FM_RX_OVER_SCO].tMandatoryResources.eResources[1]=CAL_RESOURCE_FMIF;
    tOperationtoresource->tOpToResMap[CAL_OPERATION_FM_RX_OVER_SCO].tMandatoryResources.eResources[2]=CAL_RESOURCE_FM_CORE;
    tOperationtoresource->tOpToResMap[CAL_OPERATION_FM_RX_OVER_SCO].tMandatoryResources.uNumOfResources=3;
    tOperationtoresource->tOpToResMap[CAL_OPERATION_FM_RX_OVER_SCO].tOptionalResources.uNumOfResourceLists=0;

    /* update allowed operation pairs */
    /* first nullify pairs for all resources */
    for (uIndex = 0; uIndex < CAL_RESOURCE_MAX_NUM; uIndex++)
    {
        tOpPairConfig->tAllowedPairs[ uIndex ].uNumOfAllowedPairs = 0;

    }
    /* set allowed pairs */
    tOpPairConfig->tAllowedPairs[ CAL_RESOURCE_FM_CORE ].uNumOfAllowedPairs = 1;
    tOpPairConfig->tAllowedPairs[ CAL_RESOURCE_FM_CORE ].tOpPairs[ 0 ].eOperations[ 0 ] = CAL_OPERATION_FM_RX;
    tOpPairConfig->tAllowedPairs[ CAL_RESOURCE_FM_CORE ].tOpPairs[ 0 ].eOperations[ 1 ] = CAL_OPERATION_FM_RX_OVER_SCO;
}


void Cal_Prep_BtOverFm_Config_6350(McpHciSeqCmdToken *pToken, void *pUserData)
{
        Cal_Resource_Data  *pResourceData = (Cal_Resource_Data*)pUserData;      

        pToken->callback = CAL_Config_CB_Complete;
        pToken->eHciOpcode = CCMA_HCI_CMD_FM_OVER_BT_6350;
        pToken->uCompletionEvent = CCMA_HCI_EVENT_COMMAND_COMPLETE;
        pToken->pHciCmdParms = &(pResourceData->hciseqCmdPrms[0]);
        pToken->uhciCmdParmsLen = Set_params_Fm_Over_Bt_6350(pResourceData,&(pResourceData->hciseqCmdPrms[0]));              
        pToken->pUserData = (void*)pResourceData;
}

static McpU8 Set_params_Fm_Over_Bt_6350(Cal_Resource_Data *pResourceData, McpU8 *pBuffer)
{
    McpU16      uScoHandle = (McpU16)pResourceData->tProperties.tResourcePorpeties[ 0 ];

    MCP_ENDIAN_HostToLE16 (uScoHandle, &(pBuffer[0])); /*Set the SCO Handle */

    /* whether this is a strat or stop FM over SCO command */
    if (MCP_TRUE == pResourceData->bIsStart)
    {
        pBuffer[2] = 1;
    }
    else
    {
        pBuffer[2] = 0;
    }
    pBuffer[3]=0;
    pBuffer[4]=0;   
    return 5; /* number of bytes written */

}



