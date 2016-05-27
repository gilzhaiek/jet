/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_actionHandler.c
 *
 * Description      :
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */


#include "suplc_actionHandler.h"


/**
 * Function:        SUPLC_actionHandler
 * Brief:           Handles various events sent to SUPLC
 * Description:     This functions is called whenever a message intended for SUPLC is
                    put on its action queue.
 * Note:
 * Params:          hSuplc - handle to SUPLC's control structure.
                    p_msg - Contains all details about the message.
 * Return:          Returns the status of operation: RES_SUCCESS or RES_ERROR
 */
EMcpfRes SUPLC_actionHandler(handle_t hSuplc, TmcpfMsg *p_msg)
{
    TSUPL_Ctrl *p_suplCntl = NULL;
    EMcpfRes mcpfRetVal = RES_COMPLETE;
    p_suplCntl = (TSUPL_Ctrl *) hSuplc;

    /* Just in case... */
    if ( p_suplCntl == NULL )
    {
        PRINT_TAG(" SUPLC_eventHandler: NULL Pointers \n");
        mcpfRetVal = RES_ERROR;
    }

    /* Free received msg and data buffer */
	/* Klocwork Changes */
    if (p_msg->pData && p_suplCntl != NULL) 
    {
        mcpf_mem_free_from_pool (p_suplCntl->hMcpf, p_msg->pData);
        p_msg->pData = NULL;
    }
	if(p_suplCntl != NULL) /* Klocwork Changes */
	{
		mcpf_mem_free_from_pool (p_suplCntl->hMcpf, (void *) p_msg);
	}
    
    p_msg = NULL;
    return mcpfRetVal;
}
