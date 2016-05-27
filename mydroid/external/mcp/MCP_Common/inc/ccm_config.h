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
#ifndef _CCM_CONFIG_H
#define _CCM_CONFIG_H

#include "mcp_hal_types.h"

/*-------------------------------------------------------------------------------
 * IM (Init-Manager) Module
 *
 *     Represents configuration parameters for CCM-IM module.
 */

/*
*   TBD
*/
#define CCM_IM_CONFIG_MUTEX_NAME                ("CCM_Mutex")

/*
*   TBD
*/
#define CCM_CONFIG_IM_MAX_NUM_OF_REGISTERED_CLIENTS     ((McpUint)10)

/*-------------------------------------------------------------------------------
 * VAC (Voice and Audio Control) Module
 *
 *     Represents configuration parameters for CCM-VAC module.
 */

#define CCM_VAC_MAX_NUMBER_OF_CLIENTS_PER_OPERATION     2
#define CCM_CAL_MAX_NUMBER_OF_PROPERTIES_PER_RESOURCE   1

/*
 * ini file definitions
 */
#define CCM_VAC_CONFIG_FILE_NAME						("vac_config.ini")
#define CCM_VAC_CONFIG_PATH_NAME						(MCP_HAL_CONFIG_FS_SCRIPT_FOLDER)
#define CCM_VAC_MEM_CONFIG                              ("[operations]\n" \
                                                         "number_of_operations = 6\n" \
                                                         "operation = CAL_OPERATION_BT_VOICE\n" \
                                                         "operation = CAL_OPERATION_A3DP\n" \
                                                         "operation = CAL_OPERATION_FM_RX\n" \
                                                         "operation = CAL_OPERATION_FM_TX\n" \
                                                         "operation = CAL_OPERATION_FM_RX_OVER_SCO\n" \
                                                         "operation = CAL_OPERATION_FM_RX_OVER_A3DP\n" \
                                                         "[resources]\n" \
                                                         "number_of_resources = 3\n" \
                                                         "resource = CAL_RESOURCE_PCMH\n" \
                                                         "resource = CAL_RESOURCE_I2SH\n" \
                                                         "resource = CAL_RESOURCE_FM_ANALOG\n" \
                                                         "[mapping]\n" \
                                                         "operation = CAL_OPERATION_BT_VOICE\n" \
                                                         "number_of_possible_resources = 1\n" \
                                                         "resource = CAL_RESOURCE_PCMH\n" \
                                                         "default_resource = CAL_RESOURCE_PCMH\n" \
                                                         "operation = CAL_OPERATION_A3DP\n" \
                                                         "number_of_possible_resources = 2\n" \
                                                         "resource = CAL_RESOURCE_PCMH\n" \
                                                         "resource = CAL_RESOURCE_I2SH\n" \
                                                         "default_resource = CAL_RESOURCE_PCMH\n" \
                                                         "operation = CAL_OPERATION_FM_RX\n" \
                                                         "number_of_possible_resources = 3\n" \
                                                         "resource = CAL_RESOURCE_PCMH\n" \
                                                         "resource = CAL_RESOURCE_I2SH\n" \
                                                         "resource = CAL_RESOURCE_FM_ANALOG\n" \
                                                         "default_resource = CAL_RESOURCE_FM_ANALOG\n" \
                                                         "operation = CAL_OPERATION_FM_TX\n" \
                                                         "number_of_possible_resources = 3\n" \
                                                         "resource = CAL_RESOURCE_PCMH\n" \
                                                         "resource = CAL_RESOURCE_I2SH\n" \
                                                         "resource = CAL_RESOURCE_FM_ANALOG\n" \
                                                         "default_resource = CAL_RESOURCE_FM_ANALOG\n" \
                                                         "operation = CAL_OPERATION_FM_RX_OVER_SCO\n" \
                                                         "number_of_possible_resources = 0\n" \
                                                         "operation = CAL_OPERATION_FM_RX_OVER_A3DP\n" \
                                                         "number_of_possible_resources = 0\n"\
								"[codec_config]\n" \
								"NUMBER_OF_PCM_CLK_CH1 = 1\n" \
								"NUMBER_OF_PCM_CLK_CH2 = 17\n" \
								"PCM_CLOCK_RATE	= 3072\n" \
								"PCM_DIRECTION_ROLE = 0\n" \
								"FRAME_SYNC_DUTY_CYCLE = 1\n" \
								"FRAME_SYNC_EDGE = 1\n" \
								"FRAME_SYNC_POLARITY = 0\n" \
								"CH1_DATA_OUT_SIZE = 16\n" \
								"CH1_DATA_OUT_OFFSET = 1\n" \
								"CH1_OUT_EDGE = 1\n" \
								"CH1_DATA_IN_SIZE = 16\n" \
								"CH1_DATA_IN_OFFSET = 1\n" \
								"CH1_IN_EDGE = 0\n" \
								"CH2_DATA_OUT_SIZE = 16\n" \
								"CH2_DATA_OUT_OFFSET = 17\n" \
								"CH2_OUT_EDGE = 1\n" \
								"CH2_DATA_IN_SIZE = 16\n" \
								"CH2_DATA_IN_OFFSET = 17\n" \
								"CH2_IN_EDGE = 0\n" \
								"[fm_pcm_conf]\n" \
								"FM_PCMI_RIGHT_LEFT_SWAP = 0\n" \
								"FM_PCMI_BIT_OFFSET_VECTOR = 0\n" \
								"FM_PCMI_SLOT_OFSET_VECTOR = 0\n" \
								"FM_PCMI_PCM_INTERFACE_CHANNEL_DATA_SIZE = 0\n" \
								"[fm_i2s_conf]\n" \
								"FM_I2S_DATA_WIDTH = 10\n" \
								"FM_I2S_DATA_FORMAT = 0\n" \
								"FM_I2S_MASTER_SLAVE = 0\n" \
								"FM_I2S_SDO_TRI_STATE_MODE = 0\n" \
								"FM_I2S_SDO_PHASE_WS_PHASE_SELECT = 3\n" \
								"FM_I2S_SDO_3ST_ALWZ = 0\n")




#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_SECTION_NAME		("codec_config")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_NUMBER_OF_PCM_CLK_CH1 	("NUMBER_OF_PCM_CLK_CH1")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_NUMBER_OF_PCM_CLK_CH2	("NUMBER_OF_PCM_CLK_CH2 ")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_PCM_CLOCK_RATE	("PCM_CLOCK_RATE")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_PCM_DIRECTION_ROLE	("PCM_DIRECTION_ROLE")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_FRAME_SYNC_DUTY_CYCLE	("FRAME_SYNC_DUTY_CYCLE")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_FRAME_SYNC_EDGE	("FRAME_SYNC_EDGE")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_FRAME_SYNC_POLARITY	("FRAME_SYNC_POLARITY")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH1_DATA_OUT_SIZE	("CH1_DATA_OUT_SIZE")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH1_DATA_OUT_OFFSET	("CH1_DATA_OUT_OFFSET")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH1_OUT_EDGE	("CH1_OUT_EDGE")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH1_DATA_IN_SIZE	("CH1_DATA_IN_SIZE")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH1_DATA_IN_OFFSET	("CH1_DATA_IN_OFFSET")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH1_IN_EDGE	("CH1_IN_EDGE")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH2_DATA_OUT_SIZE	("CH2_DATA_OUT_SIZE")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH2_DATA_OUT_OFFSET	("CH2_DATA_OUT_OFFSET")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH2_OUT_EDGE	("CH2_OUT_EDGE")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH2_DATA_IN_SIZE	("CH2_DATA_IN_SIZE")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH2_DATA_IN_OFFSET	("CH2_DATA_IN_OFFSET")
#define CCM_VAC_CONFIG_INI_CODEC_CONFIG_PARAM_CH2_IN_EDGE	("CH2_IN_EDGE")

#define CCM_VAC_CONFIG_INI_FM_PCMI_PARAM_SECTION_NAME	("fm_pcm_conf")
#define CCM_VAC_CONFIG_INI_FM_PCMI_RIGHT_LEFT_SWAP					 ("FM_PCMI_RIGHT_LEFT_SWAP")
#define CCM_VAC_CONFIG_INI_FM_PCMI_BIT_OFFSET_VECTOR ("FM_PCMI_BIT_OFFSET_VECTOR")
#define CCM_VAC_CONFIG_INI_FM_PCMI_SLOT_OFSET_VECTOR ("FM_PCMI_SLOT_OFSET_VECTOR")
#define CCM_VAC_CONFIG_INI_FM_PCMI_PCM_INTERFACE_CHANNEL_DATA_SIZE ("FM_PCMI_PCM_INTERFACE_CHANNEL_DATA_SIZE")

#define CCM_VAC_CONFIG_INI_FM_I2S_PARAM_SECTION_NAME 	("fm_i2s_conf")
#define CCM_VAC_CONFIG_INI_FM_I2S_DATA_WIDTH ("FM_I2S_DATA_WIDTH")
#define CCM_VAC_CONFIG_INI_FM_I2S_DATA_FORMAT ("FM_I2S_DATA_FORMAT")
#define CCM_VAC_CONFIG_INI_FM_I2S_MASTER_SLAVE ("FM_I2S_MASTER_SLAVE")
#define CCM_VAC_CONFIG_INI_FM_I2S_SDO_TRI_STATE_MODE ("FM_I2S_SDO_TRI_STATE_MODE")
#define CCM_VAC_CONFIG_INI_FM_I2S_SDO_PHASE_WS_PHASE_SELECT ("FM_I2S_SDO_PHASE_WS_PHASE_SELECT")
#define CCM_VAC_CONFIG_INI_FM_I2S_SDO_3ST_ALWZ ("FM_I2S_SDO_3ST_ALWZ")

#define CCM_VAC_CONFIG_INI_OPERATIONS_SECTION_NAME      ("operations")
#define CCM_VAC_CONFIG_INI_OPERATIONS_NUMBER_KEY_NAME   ("number_of_operations")
#define CCM_VAC_CONFIG_INI_OPERATION_NAME_KEY_NAME      ("operation")

#define CCM_VAC_CONFIG_INI_RESOURCES_SECTION_NAME       ("resources")
#define CCM_VAC_CONFIG_INI_RESOURCES_NUMBER_KEY_NAME    ("number_of_resources")
#define CCM_VAC_CONFIG_INI_RESOURCE_NAME_KEY_NAME       ("resource")

#define CCM_VAC_CONFIG_INI_MAPPING_SECTION_NAME         ("mapping")
#define CCM_VAC_CONFIG_INI_MAPPING_OP_KEY_NAME          ("operation")
#define CCM_VAC_CONFIG_INI_MAPPING_CONFIG_NUM_KEY_NAME  ("number_of_possible_resources")
#define CCM_VAC_CONFIG_INI_MAPPING_RESOURCE_KEY_NAME    ("resource")
#define CCM_VAC_CONFIG_INI_MAPPING_DEF_RESOURCE_KEY_NAME ("default_resource")

#define CCM_VAC_CONFIG_INI_OVERRIDE_SECTION_NAME        ("override")
#define CCM_VAC_CONFIG_INI_OVERRIDE_PROJECT_TYPE        ("project_type")
#define CCM_VAC_CONFIG_INI_OVERRIDE_VERSION_MAJOR       ("version_major")
#define CCM_VAC_CONFIG_INI_OVERRIDE_VERSION_MINOR       ("version_minor")

#endif

