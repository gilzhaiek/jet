/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   navc_inav_if.h
 *
 * Description      :   NAVC<->INAVC interface decleration header
 *
 * Author           :   A.N.Naveen
 * Date             :   16 Dec 2010
 *
 ******************************************************************************
 */

#ifndef NAVC_INAVC_IF_H
#define NAVC_INAVC_IF_H

/************************************************************************
 * Header Files
 ************************************************************************/
#include "mcpf_defs.h"

/************************************************************************
 * Defines
 ************************************************************************/

/************************************************************************
 * Types
 ************************************************************************/

/************************************************************************
 * Internal functions prototypes
 ************************************************************************/

/**
 * \fn     navc_notify_cb
 * \brief  This routine is registered with INAVC to be invoked to send
 *         msg to the Rx.
 */
McpS32 navc_notify_cb(handle_t h_reg_cb, McpU32 event, void *param);

/**
 * \fn     navc_set_inavc_hndl
 * \brief  This routine used to store the INAVC handle.
 */
 void navc_set_inavc_hndl(handle_t h_inavc);

#endif /* NAVC_INAVC_IF_H*/
