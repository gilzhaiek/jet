/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_protPos_api.h
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#ifndef __SUPLC_PROT_POS_API_H__
#define __SUPLC_PROT_POS_API_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include "navc_api.h"

#include "suplc_defs.h"


/**
 * Function:        SUPLC_initProtocolLib
 * Brief:           Initializes Position Protocol Library.
 * Description:
 * Note:            External Function
 * Params:          p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_initProtocolLib(TSUPL_Ctrl *const p_inSuplCntl);


/**
 * Function:        SUPLC_deInitProtocolLib
 * Brief:           DeInitializes Position Protocol Library.
 * Description:
 * Note:            External Function
 * Params:          p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_deInitProtocolLib(TSUPL_Ctrl *const p_inSuplCntl);



/**
 * Function:        SUPLC_getPositionPayload
 * Brief:           Encodes data into appropriate position protocols.
 * Description:
 * Note:
 * Params:          p_outData - RRLP/RRC data.
                    p_outDataLen - Data length.
                    p_inLocFixReport - Position Report Data.
                    p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_getPositionPayload(McpU8 *p_outData, McpU16 *p_outDataLen,
                                       const TNAVC_locFixReport *const p_inLocFixReport,
                                       TSUPL_Ctrl *const p_inSuplCntl);


/**
 * Function:        SUPLC_sendMessage
 * Brief:           Sends messages to NAVC.
 * Description:
 * Note:
 * Params:          p_suplCtrl - SUPLC's control structure.
                    p_assistanceData - RRLP/RRC data comming from NW.
                    u16_assistDataLength - Assistance Data Length.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
*/
eSUPLC_Result SUPLC_assistanceDataInd(TSUPL_Ctrl *const p_inSuplCtrl,
                                      const McpU8 *const p_assistanceData,
                                      const McpU16 u16_assistDataLength);


/**
 * Function:        SUPLC_StopLpPositionRequest
 * Brief:           Encodes data into appropriate position protocols.
 * Description:
 * Note:
 * Params:       p_inSuplCntl - Pointer to SUPLC's Control Structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_StopLpPositionRequest(TSUPL_Ctrl *const p_inSuplCntl);



#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __SUPLC_PROT_POS_API_H__ */
