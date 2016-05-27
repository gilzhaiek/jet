/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_actionHandler.h
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#ifndef __SUPLC_ACTION_HANDLE_H__
#define __SUPLC_ACTION_HANDLE_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */


#include "mcpf_defs.h"
#include "suplc_defs.h"

/**
 * Function:        SUPLC_actionHandler
 * Brief:           Handles various events sent to SUPLC.
 * Description:     This functions is called whenever a message intended for SUPLC is
                    put on its action queue.
 * Note:            External Function.
 * Params:          hSuplc - handle to SUPLC's control structure.
                    p_msg - Contains all details about the message.
 * Return:          Returns the status of operation: RES_SUCCESS or RES_ERROR
 */
EMcpfRes SUPLC_actionHandler(handle_t hSuplc, TmcpfMsg *p_msg);


#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __SUPLC_ACTION_HANDLE_H__ */
