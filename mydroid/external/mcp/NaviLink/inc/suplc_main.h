/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_main.h
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */

#ifndef __SUPLC_MAIN_H__
#define __SUPLC_MAIN_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */


#include "mcp_hal_defs.h"                   /* For DLL_EXPORT. */

#include "suplc_defs.h"


/**
 * Function:        SUPL_Create
 * Brief:           Init the SUPL
 * Description:     This function is called from MCPF’s socket server task for creating
                    SUPL_C. It creates a SUPLC task and registers its event callbacks
                    with MCPF. It also creates SUPL_Core component.
                    (Allocates internal data and starts SUPLC main thread.)
 * Note:            External Function.
 * Params:          hMCpf - handle to MCPF
 * Return:          Success: RES_SUCCESS.
                    Failure: RES_ERROR.
 */
DLL_EXPORT EMcpfRes SUPL_Create(handle_t hMcpf);

/**
 * Function:        SUPL_Destroy
 * Brief:           DeInit the SUPL
 * Description:     This function is called from MCPF’s socket server task for destroying
                    SUPL_C.
 * Note:            External Function.
 * Params:          None
 * Return:          Success: RES_COMPLETE.
                    Failure: RES_ERROR.
 */
DLL_EXPORT EMcpfRes SUPL_Destroy();

/**
 * Function:        suplcRegisterHandlers
 * Brief:           Registers various SUPLC's queue handlers.
 * Description:     In order for SUPLC to be able to receive messages,
                    it should register its message handler callbacks with MCPF.
 * Note:            Internal Function.
 * Params:          p_inSuplCtrl - Handle to SUPLC Control Structure.
 * Return:          Success: RES_COMPLETE.
                    Failure: RES_ERROR.
 */
static EMcpfRes suplcRegisterHandlers(const TSUPL_Ctrl *const p_inSuplCtrl);


/**
 * Function:        suplcUnRegisterHandlers
 * Brief:           UnRegisters various SUPLC's queue handlers.
 * Description:     UnRegister SUPC's message handler callbacks with MCPF.
 * Note:            Internal Function.
 * Params:          p_inSuplCtrl - Handle to SUPLC Control Structure.
 * Return:          Success: RES_SUCCESS.
                    Failure: RES_ERROR.
 */
static EMcpfRes suplcUnRegisterHandlers(const TSUPL_Ctrl *const p_inSuplCtrl);


/**
 * Function:        suplcEnableMessageQueues
 * Brief:           Enables various SUPLC's queues.
 * Description:
 * Note:
 * Params:          p_inSuplCtrl - Handle to SUPLC Control Structure.
 * Return:          Success: RES_SUCCESS.
                    Failure: RES_ERROR.
 */
static EMcpfRes suplcEnableMessageQueues(const TSUPL_Ctrl *const p_inSuplCtrl);



/**
 * Function:        suplcDisableMessageQueues
 * Brief:           Disable SUPLC's queues.
 * Description:
 * Note:
 * Params:          p_inSuplCtrl - Handle to SUPLC Control Structure.
 * Return:          Success: RES_SUCCESS.
                    Failure: RES_ERROR.
 */
static EMcpfRes suplcDisableMessageQueues(const TSUPL_Ctrl *const p_inSuplCtrl);


#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __SUPLC_MAIN_H__ */
