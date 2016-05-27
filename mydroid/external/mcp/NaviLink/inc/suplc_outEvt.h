/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_outEvt.h
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */


#ifndef __SUPLC_OUT_EVT_H__
#define __SUPLC_OUT_EVT_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */


#include "mcp_defs.h"
#include "suplc_defs.h"


/**
 * Function:        SUPLC_registerAssistanceSource
 * Brief:           Sends messages to NAVC.
 * Description:
 * Note:
 * Params:          p_suplCtrl - SUPLC's control structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_registerAssistanceSource(const TSUPL_Ctrl *const p_inSuplCtrl);

/**
 * Function:        SUPLC_setAssistancePriority
 * Brief:           Sends messages to NAVC.
 * Description:
 * Note:
 * Params:          p_suplCtrl - SUPLC's control structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_setAssistancePriority(const TSUPL_Ctrl *const p_inSuplCtrl);

/**
 * Function:        SUPLC_sendLocationFix
 * Brief:           Sends location fix request to NAVC.
 * Description:
 * Note:
 * Params:          p_suplCtrl - SUPLC's control structure.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
eSUPLC_Result SUPLC_sendLocationFix(const TSUPL_Ctrl *const p_suplCtrl,T_GPSC_loc_fix_mode e_inGpsMode,T_GPSC_loc_fix_qop* p_inQop);

/**
 * Function:        SUPLC_startNavc
 * Brief:           Sends messages to NAVC.
 * Description:
 * Note:
 * Params:          p_inSuplCtrl - SUPLC's Control Structure.
                    e_inGpsMode - GPS Fix Mode.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
*/
eSUPLC_Result SUPLC_startNavc(const TSUPL_Ctrl *const p_inSuplCtrl,
                              T_GPSC_loc_fix_mode e_inGpsMode,T_GPSC_loc_fix_qop* p_inQop,int startorlocfixr);


/* Added for NI */
eSUPLC_Result SUPLC_stopNavc(const TSUPL_Ctrl *const p_inSuplCtrl,
								  void* p_inData, McpU32 length);

eSUPLC_Result SUPLC_sendSessResult(const TSUPL_Ctrl *const p_inSuplCtrl, 
											void* p_inData);

eSUPLC_Result SUPLC_sendPosInd(const TSUPL_Ctrl *const p_inSuplCtrl,
                              TNAVC_posIndData *, const McpU16 );

/**
 * Function:        SUPLC_sendMessage
 * Brief:           Sends messages to NAVC.
 * Description:
 * Note:
 * Params:          p_inSuplCtrl - SUPLC's control structure.
                    e_inOpcode - Opcode of the message.
                    e_inDestTaskId - Destination Task ID.
                    p_inData - Payload.
                    u16_inlength - Data length.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
static eSUPLC_Result suplcSendMessage(const TSUPL_Ctrl *const p_inSuplCtrl,
                                      const ENAVC_cmdOpcode e_inOpcode,
                                      const EmcpTaskId e_inDestTaskId,
                                      const void *const p_inData,
                                      const McpU16 u16_inlength);


#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __SUPLC_OUT_EVT_H__ */
