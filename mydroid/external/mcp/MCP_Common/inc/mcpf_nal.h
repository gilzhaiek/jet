/*
 *******************************************************************************
  *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
  *
  * Property of Texas Instruments, Unauthorized reproduction and/or distribution
  * is strictly prohibited.  This product  is  protected  under  copyright  law
  * and  trade  secret law as an  unpublished work.
  * (C) Copyright Texas Instruments.  All rights reserved.
  *
  * FileName         :   suplc_core_wrapper.cpp
  *
  * Description      :   Common interface between SUPL Core and NaviLink.
  *
  * Author           :   Praneet Kumar A
  * Date             :   15th June 2009
  *
  ******************************************************************************
*/

#ifndef __SUPLC_NAL_H__
#define __SUPLC_NAL_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */


#include "mcpf_defs.h"
#include "mcpf_nal_common.h"

EMcpfRes NAL_executeCommand(eNetCommands cmd,void *data);

EMcpfRes NAL_connectToHS();

EMcpfRes NAL_closeConnectionToHS();



#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __SUPLC_NAL_H__ */

