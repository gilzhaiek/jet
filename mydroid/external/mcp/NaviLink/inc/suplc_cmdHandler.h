/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_cmdHandler.h
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */


#ifndef __SUPLC_CMD_HANDLER_H__
#define __SUPLC_CMD_HANDLER_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus*/

#include "mcpf_defs.h"

#include "suplc_defs.h"
#include "suplc_api.h"


/**
 * Function:        SUPLC_commandHandler
 * Brief:           Handles various commands sent to SUPLC
 * Description:     This functions is called whenever a message intended for SUPLC is
                    put on its command queue.
 * Note:
 * Params:          hSuplc - handle to SUPLC's control structure.
                    pMsg - Contains all details about the message.
 * Return:          Success: RES_SUCCESS.
                    Failure: RES_ERROR.
 */
EMcpfRes SUPLC_commandHandler(handle_t hSuplc, TmcpfMsg *p_msg);



/**
 * Function:        setLocationFixMode
 * Brief:           This function sets the location fix mode.
 * Description:
 * Note:
 * Params:          p_cmdParams - Command Params.
                    p_suplCntl - SUPLC's Control Structure.
                    pMsg - Contains all details about the message.
 * Return:          Success: SUPLC_SUCCESS.
                    Failure: SUPLC_ERROR_XXX.
 */
//static eSUPLC_Result setLocationFixMode(const TSUPLC_CmdParams *const p_cmdParams,
//                                       TSUPL_Ctrl *const p_suplCntl);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __SUPLC_CMD_HANDLER_H__ */
