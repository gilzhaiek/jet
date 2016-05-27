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
## Control List (�CCL�). The assurances provided for herein are furnished in  *
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


/** \file   msp_transDefs.h 
 *  \brief  HCI (Host Controller Interface protocol) common definitions
 *
 *  \see    msp_uartBusDrv.c
 */


#ifndef _MCP_TRANS_DEFS_API_H_
#define _MCP_TRANS_DEFS_API_H_

/************************************************************************
 * Defines
 ************************************************************************/

/* HCI packet type - the first byte of HCI frame over UART */
#define HCI_PKT_TYPE_COMMAND		0x01
#define HCI_PKT_TYPE_ACL			0x02
#define HCI_PKT_TYPE_SCO			0x03
#define HCI_PKT_TYPE_EVENT			0x04 
#define HCI_PKT_TYPE_NAVC			0x09 

/* Interface Sleep Management packet types */
#define HCI_PKT_TYPE_SLEEP_IND		0x30
#define HCI_PKT_TYPE_SLEEP_ACK		0x31
#define HCI_PKT_TYPE_WAKEUP_IND		0x32
#define HCI_PKT_TYPE_WAKEUP_ACK		0x33 


#define HCI_PKT_TYPE_LEN			1

#define HCI_HEADER_SIZE_COMMAND     3
#define HCI_HEADER_SIZE_ACL         4
#define HCI_HEADER_SIZE_SCO         3
#define HCI_HEADER_SIZE_EVENT       2
#define HCI_HEADER_SIZE_NAVC        3
#define HCI_HEADER_SIZE_IFSLPMNG    0

#define HCI_PREAMBLE_SIZE(type)		(HCI_HEADER_SIZE_##type + HCI_PKT_TYPE_LEN)

#endif  /* _MCP_TRANS_DEFS_API_H_ */
