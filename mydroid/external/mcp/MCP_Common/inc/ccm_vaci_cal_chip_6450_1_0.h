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
*   FILE NAME:      ccm_vaci_cal_chip_6450_1_0.h
*
*   BRIEF:          This file define the audio capabilities and 
*                   configuration API for Orca 6450 PG 1.0
*                  
*
*   DESCRIPTION:    This is an internal configuration file for Orca 6450 PG 1.0
                    For the CCM_VACI_CAL moudule.
*
*   AUTHOR:         Malovany Ram
*
\*******************************************************************************/
#ifndef __CCM_VACI_CAL_CHIP_6450_1_0_H_
#define __CCM_VACI_CAL_CHIP_6450_1_0_H_

/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/

#include "ccm_audio_types.h"
#include "ccm_vaci_chip_abstraction_int.h"

/********************************************************************************
 *
 * Function declarations
 *
 *******************************************************************************/
#if 0/* todo zvi This was added to debig and should be removed*/
    void Debug_Set_FM_DigitalAudioConfiguration(
        McpU16 pcmiLeftRightSwap,
        McpU16 bitOffsetVector,
        McpU16 slotOffsetVector,
        McpU16 pcmInterfaceChannelDataSize,
        McpU16 dataWidth,
        McpU16 dataFormat,
        McpU16 masterSlave,
        McpU16 sdoTriStateMode,
        McpU16 sdoPhaseSelect_WsPhase,
        McpU16 sdo3stAlwz
    );
#endif
/*-------------------------------------------------------------------------------
 * CAL_GetAudioCapabilities_6450_1_0()
 *
 * Brief:  
 *     Gets the audio capabilities for 6450 PG 1.0
 *
 * Description:
 *     Gets the audio capabilities for 6450 PG 1.0
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      tOperationtoresource [out] - pointer to a struct that holds information
 *          regarding mapping operation to resource acorrding to eChip_Version.
 *      tOpPairConfig [out] - pointer to a struct holding allowed operation 
 *          pairs
 *      tResource [out] - pointer to a struct that holds information regarding 
 *          resource support acording to eChip_Version
 *      tOperation [out] - pointer to a struct that holds information regarding
 *          opeartion support acording to eChip_Version
     
 *
 * Returns:
 *      N/A
 */
void CAL_GetAudioCapabilities_6450_1_0 (TCAL_MappingConfiguration  *tOperationtoresource,
                                        TCAL_OpPairConfig *tOpPairConfig,
                                        TCAL_ResourceSupport *tResource,
                                        TCAL_OperationSupport *tOperation);



/*-------------------------------------------------------------------------------
 * Cal_Prep_Mux_Config_6450_1_0()
 *
 * Brief:  
 *  Prepre the MUXING command for 6450 PG 1.0     
 *
 * Description:
 *  Prepre the MUXING command for 6450 PG 1.0
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      pToken [out] - pointer to the command token
 *      pUserData [out] - pointer to user data
 *      
 *
 * Returns:
 *      N/A
 */
void Cal_Prep_Mux_Config_6450_1_0(McpHciSeqCmdToken *pToken, void *pUserData);

/*-------------------------------------------------------------------------------
 * Cal_Prep_AVPR_Enable_6450_1_0()
 *
 * Brief:  
 *  Prepare the AVPR enable command for 6450 PG 1.0
 *
 * Description:
 *  Prepare the AVPR enable command for 6450 PG 1.0
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *      pToken [out] - pointer to the command token
 *      pUserData [out] - pointer to user data
 *      
 *
 * Returns:
 *      N/A
 */
void Cal_Prep_AVPR_Enable_6450_1_0(McpHciSeqCmdToken *pToken, void *pUserData);

/*-------------------------------------------------------------------------------
 * Cal_Prep_BtOverFm_Config_6450_1_0()
 *
 * Brief:  
 *  Prepre the BT over FM command for 6450 PG 1.0     
 *
 * Description:
 *  Prepre the BT over FM command for 6450 PG 1.0 
 *
 * Type:
 *      
 *
 * Parameters:
 *      pToken [out] - pointer to the command token
 *      pUserData [out] - pointer to user data
 *
 * Returns:
 *      N/A
 */
void Cal_Prep_BtOverFm_Config_6450_1_0(McpHciSeqCmdToken *pToken, void *pUserData);

/*-------------------------------------------------------------------------------
 * Cal_Prep_FMIF_I2S_Config_6450()
 *
 * Brief:  
 *  bbb   
 *
 * Description:
 *  bbb
 *
 * Type:
 *      
 *
 * Parameters:
 *      pToken [out] - pointer to the command token
 *      pUserData [out] - pointer to user data
 *
 * Returns:
 *      N/A
 */
void Cal_Prep_FMIF_I2S_Config_6450(McpHciSeqCmdToken *pToken, void *pUserData);
/*-------------------------------------------------------------------------------
 * Cal_Prep_FMIF_PCM_Pcm_mode_Config_6450()
 *
 * Brief:  
 *  bbb  
 *
 * Description:
 *  bbb
 *
 * Type:
 *      
 *
 * Parameters:
 *      pToken [out] - pointer to the command token
 *      pUserData [out] - pointer to user data
 *
 * Returns:
 *      N/A
 */
void Cal_Prep_FMIF_PCM_Pcm_mode_Config_6450(McpHciSeqCmdToken *pToken, void *pUserData);
/*-------------------------------------------------------------------------------
 * Cal_Prep_FMIF_PCM_I2s_mode_Config_6450()
 *
 * Brief:  
 *  bbb  
 *
 * Description:
 *  bbb
 *
 * Type:
 *      
 *
 * Parameters:
 *      pToken [out] - pointer to the command token
 *      pUserData [out] - pointer to user data
 *
 * Returns:
 *      N/A
 */
void Cal_Prep_FMIF_PCM_I2s_mode_Config_6450(McpHciSeqCmdToken *pToken, void *pUserData);



#endif /* __CCM_VACI_CAL_CHIP_6450_1_0_H_*/

