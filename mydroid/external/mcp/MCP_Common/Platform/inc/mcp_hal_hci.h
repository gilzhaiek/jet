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

#ifndef MPC_HAL_HCI_H
#define MPC_HAL_HCI_H

#include "mcp_hal_types.h"
#include "mcpf_defs.h"

#define HAL_HCI_CHIP_VERSION_FILE_NAME           "/sys/uim/version"
#define HAL_HCI_CHIP_VERSION_FILE_NAME_2         "/sys/kernel/debug/ti-st/version"
 
typedef enum {
	HAL_HCI_STATUS_SUCCESS,
	HAL_HCI_STATUS_FAILED,
	HAL_HCI_STATUS_PENDING
} eHalHciStatus;

typedef EMcpfRes (*fHciCallback)(handle_t hCallback, McpU8 *pkt, McpU32 len, eHalHciStatus status); 

typedef struct
{
    McpU16    full;         /* Full 16-bits version */
    McpU16    projectType;  /* Project type (7 for 1271, 10 for 1283) */
    McpU16    major;        /* Version major */
    McpU16    minor;        /* Version minor */
    
} THalHci_ChipVersion; 

/*
 * hal_hci_OpenSocket - Open raw HCI socket
 *
 * Parameters:
 *  hMcpf - MCPF Handler
 *  hci_dev - dev id (always 0)
 *  pktTypeMask - bit mask for supported type:
 *					0x00000004 - ACL (CH.2)
 *					0x00000008 - SCO (CH.3)
 *					0x00000010 - EVT (CH.4)
 *					0x00000100 - FM  (CH.8)
 *  evtType[] - array of (up to 4) supported events, relevant only when EVT pktType is enabled
 *                 i.e.: (PktTypeMask & 0x00000010).
 *              each entry may include 1 event opcode.
 *				opcode = 0 will mark the end of input (can be avoided if 4 entries are available)
 *				if (evtType[0]==0xff) all events will be received (wild-card)
 *  RxIndCallback - callback function for recevied fata and events (other than command complete pakcet)
 *  hRxInd - client handle that will be used as first parameter of the callbacks
 *
 * Return Code: NULL upon failure or pointer to the allocated halHci handle.  
 */
void *hal_hci_OpenSocket(handle_t hMcpf, int hci_dev, McpU32 pktTypeMask, McpU8 evtType[], 
						 fHciCallback RxIndCallabck, handle_t hRxInd );

/*
 * hal_hci_CloseSocket - close raw HCI socket and free all aloocated resources (memory, thread)
 *
 * Parameters:
 *  hHalHci - handle of HalHci
 *
 * Return Code: HAL_HCI_STATUS_SUCCESS
 */
eHalHciStatus hal_hci_CloseSocket(handle_t hHalHci);

/*
 * hal_hci_SendPacket - Send Raw HCI data (for HCI commands, please refer to hal_hci_SendCommand) 
 *
 * Parameters:
 *  hHalHci - handle of HalHci
 *  pktType - HCI channel to be used
 *  nHciFrag - number of data fragments (include HCI header + payload, but not pktType)
 *  hciFrag[] - array of pointers to the data fragments 
 *  hciFragLen[] - array of data fragment legths (corresponds to the hciFrag array)
 *
 * Return Code: HAL_HCI_STATUS_SUCCESS upon succesfull transfer or HAL_HCI_STATUS_FAILED
 */
eHalHciStatus hal_hci_SendPacket(
                    handle_t           hHalHci,
		      McpU8	pktType,
                    McpU8   nHciFrags, 
                    McpU8   *hciFrag[], 
                    McpU32   hciFragLen[]);

/*
 * hal_hci_SendCommand - Send HCI command (prepare hci command header and store command opcode to
 *                         be comared against following command complete events)
 *  Note that command complete will always be received asynchronously (i.e. in callback), while
 *  this send will return PENDING status upon successful operation.
 *
 * Parameters:
 *  hHalHci - handle of HalHci
 *  hciOpcode - HCI command opcode
 *  hciCmdParms - pointer to hci command payload
 *  hciCmdLen - length of command payload buffer
 *  CmdCompleteCallback - callback function for command complete events
 *               (same function can be registered for the 2 callbacks if needed)
 *  hCmdComplete - client handle (user data) that will be used as first parameter of the callbacks 
 *
 * Return Code: HAL_HCI_STATUS_PENDING upon succesfull transfer or HAL_HCI_STATUS_FAILED otherwise
 */
eHalHciStatus  hal_hci_SendCommand(
                    handle_t           hHalHci,
                    McpU16  hciOpcode, 
                    McpU8   *hciCmdParms,
                    McpU32   hciCmdLen,
		      fHciCallback	CmdCompleteCallabck, 
		      handle_t hCmdComplete);

 /*
 * hal_hci_GetChipVersion - Get chip's version from the file which was written by the ST driver in the
 *                         			kernel: project type, version major, and version minor.
 *
 * Parameters:
 *  hHalHci - handle of HalHci
 *  fileName - filename to read version info from
 *  chipVersion - pointer to struct for returning chip version
 *
 * Return Code: RES_OK upon succesfull read or RES_ERROR otherwise
 */
EMcpfRes hal_hci_GetChipVersion (handle_t hHalHci,
                                					THalHci_ChipVersion *chipVersion);

/*
 * hal_hci_restartReceiver -  Signal the Receiver thread to restart the receive process.
 *					       This function should be called only after one of the 
 *						Receive callbacks returned PENDING (usually when buffer memory is exhausted).
 *						Restart should be triggered when buffer becomes available.
 *
 * Parameters:
 *  hHalHci - handle of HalHci
 *
 * Return Code: None
 */
 void hal_hci_restartReceiver (handle_t hHalHci);

#endif /* MPC_HAL_HCI_H */	


