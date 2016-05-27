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
*   FILE NAME:      ccm_vaci_debug.c
*
*   BRIEF:          This file include debug code for the Connectivity Chip
*                   Manager (CCM) Voice and Audio Control (VAC) component.
*                  
*
*   DESCRIPTION:    This file includes mainly enum-string conversion functions
*
*   AUTHOR:         Ronen Kalish
*
\*******************************************************************************/
#include "ccm_vaci_debug.h"

const char *_CCM_VAC_DebugResourceStr(ECAL_Resource eResource)
{
    const char *recourceName;
    
    switch(eResource)
    {
        case CAL_RESOURCE_I2SH:
            recourceName = "I2S";
            break;
        
        case CAL_RESOURCE_PCMH:
            recourceName = "PCM";
            break;
        
        case CAL_RESOURCE_PCMT_1:
            recourceName = "PCM_FRAME_1";
            break;
        
        case CAL_RESOURCE_PCMT_2:
            recourceName = "PCM_FRAME_2";
            break;
        
        case CAL_RESOURCE_PCMT_3:
            recourceName = "PCM_FRAME_3";
            break;
        
        case CAL_RESOURCE_PCMT_4:
            recourceName = "PCM_FRAME_4";
            break;
        
        case CAL_RESOURCE_PCMT_5:
            recourceName = "PCM_FRAME_5";
            break;
        
        case CAL_RESOURCE_PCMT_6:
            recourceName = "PCM_FRAME_6";
            break;
        
        case CAL_RESOURCE_FM_ANALOG:
            recourceName = "FM_ANALOG";
            break;
        
        case CAL_RESOURCE_PCMIF:
            recourceName = "PCM_IF";
            break;
        
        case CAL_RESOURCE_FMIF:
            recourceName = "FM_IF";
            break;
        
        case CAL_RESOURCE_CORTEX:
            recourceName = "CORTEX";
            break;
        
        case CAL_RESOURCE_FM_CORE:
            recourceName = "FM_CORE";
            break;
        
        default:
            recourceName = "UNKNOWN";
            break;
    }
    
    return (recourceName);
}

const char *_CCM_VAC_DebugOperationStr(ECAL_Operation eOperation)
{
    const char *operationName;
    
    switch(eOperation)
    {
        case CAL_OPERATION_FM_TX:
            operationName = "FM_TX";
            break;
        
        case CAL_OPERATION_FM_RX:
            operationName = "FM_RX";
            break;
        
        case CAL_OPERATION_A3DP:
            operationName = "A3DP";
            break;
        
        case CAL_OPERATION_BT_VOICE:
            operationName = "BT_VOICE";
            break;
        
        case CAL_OPERATION_WBS:
            operationName = "WBS";
            break;
        
        case CAL_OPERATION_AWBS:
            operationName = "AWBS";
            break;
        
        case CAL_OPERATION_FM_RX_OVER_SCO:
            operationName = "FM over SCO";
            break;
        
        case CAL_OPERATION_FM_RX_OVER_A3DP:
            operationName = "FM over A3DP";
            break;
        
        default:
            operationName = "UNKNOWN";
            break;
    }
    
    return (operationName);
}

