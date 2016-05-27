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

#include <stdio.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <poll.h>
#include <semaphore.h>
#include <sys/ioctl.h>
#include <termios.h>
#include <fcntl.h>
#include <pthread.h>
#include "mcp_hal_hci.h"
#include "mcpf_report.h"
#include "mcpf_mem.h"


/* Events opcode */
#define HAL_HCI_EVT_COMPLETED   	0x0e
#define HAL_HCI_EVT_STATUS			0x0f

#define HAL_HCI_COMMAND_PKT         	0x01
#define HAL_HCI_EVENT_PKT           	0x04

#define HAL_HCI_MAX_FRAME_SIZE      1028

#define HAL_HCI_COMMAND_HDR_SIZE 	3

#define HCIDEVUP						1

#define HAL_HCI_HOST_NUM_COMPLETED_PACKETS   0x0C35

typedef sem_t*          	SEM_HANDLE;

typedef struct  {
	McpU32	chan_mask;
	McpU8	evt_type[4]; /* up to 4 events can be registered per client (0 will mark EOF)
								   evtType[0] == 0xff makes a wildcard */  	
} hci_filter_t;

typedef struct  {

	handle_t 			hMcpf;

	int				fd;
	hci_filter_t		flt;
	fHciCallback		RxIndCallabck;
	fHciCallback		CmdCompleteCallback;
	handle_t			hRxInd;
	handle_t			hCmdComplete;
   
	McpU16		opcodeSent; 

	/* internal parameters */
	pthread_mutex_t		opcodeLock;
	pthread_t				hReceiverThread;
	sem_t               		tSemBufAvail;
	sem_t		   		tSemRxThreadClose;
	
 } tHalHci;

static handle_t hal_hci_ReceiverThread(handle_t hHalHci);
static void signalSem  (handle_t hMcpf, SEM_HANDLE hSem);
static void waitForSem (handle_t hMcpf, SEM_HANDLE hSem);


#define MCP_HAL_CONFIG_HCI_DEV_NAME      "/dev/tihci"

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
handle_t hal_hci_OpenSocket(handle_t hMcpf, int hci_dev, McpU32 pktTypeMask, McpU8 evtType[], 
						 fHciCallback RxIndCallabck, handle_t hRxInd )
{
	tHalHci *pHalHci = (tHalHci*)malloc(sizeof(tHalHci));
	int rc;

	MCPF_UNUSED_PARAMETER(hci_dev);
	
	MCPF_REPORT_INFORMATION(hMcpf, HAL_HCI_MODULE_LOG, ("hal_hci_OpenSocket Enter"));
	
	if(!pHalHci)
		return NULL;

	mcpf_mem_set(hMcpf ,pHalHci, 0, sizeof(tHalHci));

	pHalHci->hMcpf = hMcpf;
	
	/* Create Buf Avialable semaphore */
    if (sem_init(&pHalHci->tSemBufAvail,
                    0,                  /* Not shared between processes */
                    0) < 0 )            /* Not available */
   	{
        	MCPF_REPORT_ERROR (pHalHci->hMcpf, HAL_HCI_MODULE_LOG, ("tSemBufAvail sem_init failed!\n"));
        	free(pHalHci);
        	return NULL;
    }

	/* Create Rx thread Close semaphore */
    if (sem_init(&pHalHci->tSemRxThreadClose,
                    0,                  /* Not shared between processes */
                    0) < 0 )            /* Not available */
   	{
        	MCPF_REPORT_ERROR (pHalHci->hMcpf, HAL_HCI_MODULE_LOG, ("tSemRxThreadClose sem_init failed!\n"));
		sem_destroy(&pHalHci->tSemBufAvail);			
        	free(pHalHci);
        	return NULL;
    }
	MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, ("hal_hci_OpenSocket: before open"));
	
	/*
	 * we do not close this socket; it will stay open as long as FM stack
	 * lives, and will be used to send commands
	 */
	pHalHci->fd = open(MCP_HAL_CONFIG_HCI_DEV_NAME, O_RDWR);
	if (pHalHci->fd < 0) {
		MCPF_REPORT_ERROR(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
								("hal_hci_OpenSocket: Can't create raw socket. errno = %s", strerror(errno)));

		sem_destroy(&pHalHci->tSemBufAvail);
		sem_destroy(&pHalHci->tSemRxThreadClose);
		free(pHalHci);
		return NULL;
	}

	MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, ("hal_hci_OpenSocket: opened successfuly"));

	/* prepare RX filter */
	pHalHci->flt.chan_mask = pktTypeMask;
	if(evtType != NULL)
	{
		int i = 0;
		while(i<4 && evtType[i] != 0)
		{
			pHalHci->flt.evt_type[i] = evtType[i];
			i++;
		}
	}
	else
	{
		pHalHci->flt.evt_type[0] = 0;
	}
	
	if (ioctl(pHalHci->fd, HCIDEVUP, &pHalHci->flt) < 0)
	{
		MCPF_REPORT_ERROR(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
								("hal_hci_OpenSocket: Can't create raw socket. errno = %s", strerror(errno)));
		close(pHalHci->fd);
		sem_destroy(&pHalHci->tSemBufAvail);
		sem_destroy(&pHalHci->tSemRxThreadClose);
		free(pHalHci);
		return NULL;

	}

	MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, ("hal_hci_OpenSocket: device is up"));
	pHalHci->RxIndCallabck = RxIndCallabck;
	pHalHci->hRxInd = hRxInd;

	pthread_mutex_init(&pHalHci->opcodeLock, NULL);
	rc = pthread_create(&pHalHci->hReceiverThread, // thread structure
			NULL, // no attributes for now
			hal_hci_ReceiverThread,
			(void *)pHalHci);
	if(0 != rc)
	{
		MCPF_REPORT_ERROR(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
								("hal_hci_OpenSocket: pthread_create() failed to create receiver thread: %s", strerror(errno)));

		pthread_mutex_destroy(&pHalHci->opcodeLock);
		close(pHalHci->fd);
		sem_destroy(&pHalHci->tSemBufAvail);
		sem_destroy(&pHalHci->tSemRxThreadClose);
		free(pHalHci);
		return NULL;
	}

	MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, ("hal_hci_OpenSocket Exit"));

	return pHalHci;
}

/*
 * hal_hci_CloseSocket - close raw HCI socket and free all aloocated resources (memory, thread)
 *
 * Parameters:
 *  hHalHci - handle of HalHci
 *
 * Return Code: HAL_HCI_STATUS_SUCCESS
 */
eHalHciStatus hal_hci_CloseSocket(handle_t hHalHci)
{
	tHalHci *pHalHci = (tHalHci*)hHalHci;

	MCPF_Assert(pHalHci); 

	/* Release all the thread waiting for this semaphore before deleting it. */
   	signalSem (pHalHci->hMcpf, &pHalHci->tSemBufAvail);

	close(pHalHci->fd);

	waitForSem(pHalHci->hMcpf, &pHalHci->tSemRxThreadClose);
	
	pthread_mutex_destroy(&pHalHci->opcodeLock);
	sem_destroy(&pHalHci->tSemBufAvail);
	sem_destroy(&pHalHci->tSemRxThreadClose);
	free(pHalHci);

	return HAL_HCI_STATUS_SUCCESS;
}



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
                    McpU32   hciFragLen[])
{
	eHalHciStatus	status = HAL_HCI_STATUS_PENDING; //all callers expect PENDING as success indication
	static McpU8 hciBuffer[HAL_HCI_MAX_FRAME_SIZE];
	McpU32 hciLen, i;
	tHalHci *pHalHci = (tHalHci*)hHalHci;

#ifdef TRAN_DBG
       MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
								("Enter hal_hci_SendPacket"));
#endif

	MCPF_Assert(pHalHci); 

	/* build the HCI command */
	hciBuffer[0] = pktType;
	hciLen = 1;

	for(i=0; i<nHciFrags; i++)
	{
		MCPF_Assert(hciLen + hciFragLen[i] <= HAL_HCI_MAX_FRAME_SIZE);
		memcpy(&hciBuffer[hciLen], hciFrag[i], hciFragLen[i]);
		hciLen += hciFragLen[i];
	}
	
	/* write the RAW HCI command */
	if (write(pHalHci->fd, hciBuffer, hciLen) < 0)
	{
		MCPF_REPORT_ERROR(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
								("hal_hci_SendPacket: failed to send command"));
		status = HAL_HCI_STATUS_FAILED;
	}

    return status;
}

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
                    void *hHalHci,
                    McpU16  hciOpcode, 
                    McpU8   *hciCmdParms,
                    McpU32   hciCmdLen,
					fHciCallback	CmdCompleteCallback, 
					void			*hCmdComplete)
{
	McpU8 hciHeader[HAL_HCI_COMMAND_HDR_SIZE], *hciFrag[2];
	McpU32 HciFragLen[2];
	McpU8 hciCmdLenU8 = (McpU8)hciCmdLen;
	tHalHci *pHalHci = (tHalHci *)hHalHci;

#ifdef TRAN_DBG
       MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
                               ("Enter hal_hci_SendCommand"));
#endif
	
	MCPF_Assert(pHalHci); 

	/* Only one command at a time is allowed && 
	    HOST_NUM_COMPLETED_PACKETS command doesn't have a CMD COMPLETE */
	MCPF_Assert((pHalHci->opcodeSent == 0) || 
		(pHalHci->opcodeSent == HAL_HCI_HOST_NUM_COMPLETED_PACKETS)); 

	pHalHci->CmdCompleteCallback = CmdCompleteCallback;
	pHalHci->hCmdComplete = hCmdComplete;


	pthread_mutex_lock(&pHalHci->opcodeLock);
	pHalHci->opcodeSent = __cpu_to_le16(hciOpcode);
	pthread_mutex_unlock(&pHalHci->opcodeLock);
	
#ifdef TRAN_DBG
	MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
                            ("hal_hci_SendCommand: hci command opcode = %x", hciOpcode));
#endif


	/* build the HCI command */
	memcpy(hciHeader, &pHalHci->opcodeSent, sizeof(pHalHci->opcodeSent));
	memcpy(hciHeader + sizeof(pHalHci->opcodeSent), &hciCmdLenU8, sizeof(hciCmdLenU8));
	hciFrag[0] = hciHeader;
	HciFragLen[0] = sizeof(hciHeader);
	hciFrag[1] = hciCmdParms;
	HciFragLen[1] = hciCmdLen;
	
	
	/* send the HCI command */
	return hal_hci_SendPacket(hHalHci, HAL_HCI_COMMAND_PKT, 2, hciFrag, HciFragLen);
	
}

/** 
 * \fn     hal_hci_GetChipVersion
 * \brief  Get chip's version from the file which was written by the ST driver
 * 
 */ 
EMcpfRes hal_hci_GetChipVersion (handle_t hHalHci,
                                THalHci_ChipVersion *chipVersion)
{
    tHalHci *pHalHci = (tHalHci*)hHalHci;
    FILE *fVersion;
    int iVerFull, iProjType, iVerMajor, iVerMinor;
    int iParamsReadNum;
    EMcpfRes eRes = RES_OK;

    MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
								("Enter hal_hci_GetChipVersion"));

    fVersion = fopen(HAL_HCI_CHIP_VERSION_FILE_NAME_2, "r");
    
    if (NULL == fVersion)
    {
        /* Error opening the file */
        MCPF_REPORT_INFORMATION (pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
            ("Version file %s was not found \n", HAL_HCI_CHIP_VERSION_FILE_NAME_2));
        fVersion = fopen(HAL_HCI_CHIP_VERSION_FILE_NAME, "r");

        if (NULL == fVersion)
        {
            /* Error opening the file */
            MCPF_REPORT_ERROR (pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
            			("Version file %s open failed! \n", HAL_HCI_CHIP_VERSION_FILE_NAME));
            return RES_ERROR;
        }
    }
    
    /* Read the chip's version */
    iParamsReadNum = fscanf(fVersion,
                            "%x %d.%d.%d",
                            &iVerFull,
                            &iProjType,
                            &iVerMajor,
                            &iVerMinor);

    if (iParamsReadNum < 4)
    {
        /* Error reading from the version file */
        MCPF_REPORT_ERROR (pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
        				("Error reading from the version file %s, read only %d params! \n",
                            HAL_HCI_CHIP_VERSION_FILE_NAME,
                            iParamsReadNum));
        eRes = RES_ERROR;
    }

    if (0 == iVerFull)
    {
        /* Chip is not on */
        MCPF_REPORT_ERROR (pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
        						("Version full is 0 - chip is not on! \n"));
        eRes = RES_ERROR;
    }
    
    /* Print the version */
    MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
    								("Chip's version %d.%d.%d, full version 0x%04x",
                             					 iProjType, iVerMajor, iVerMinor, iVerFull));

    /* Close the version file */
    fclose(fVersion);

    /* Copy version info */
    chipVersion->full = (McpU16)iVerFull;
    chipVersion->projectType = (McpU16)iProjType;
    chipVersion->major = (McpU16)iVerMajor;
    chipVersion->minor = (McpU16)iVerMinor;

    return eRes;
}

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
 void hal_hci_restartReceiver (handle_t hHalHci)
{
	tHalHci *pHalHci = (tHalHci*)hHalHci;

	/* Signal semaphore that a buffer is avialable */
    	signalSem (pHalHci->hMcpf,  &pHalHci->tSemBufAvail);
}

/**********************************************************************
*						Internal Functions								   *
**********************************************************************/

/*
 * Socket receiver thread.
 * This thread waits for received traffic.
 * upon Invokation, it should receive the hci handle with all relevant parameters
 * Note that RX filter is already initated (from the open function)
 * RX process verify he validity of received packet before calling the relevant client callback 
 *  (command complete or rx callback)
 */
static handle_t hal_hci_ReceiverThread(handle_t hHalHci)
{
	tHalHci *pHalHci = (tHalHci*)hHalHci;
	int ret = HAL_HCI_STATUS_SUCCESS, len;
	McpU8 buf[HAL_HCI_MAX_FRAME_SIZE];
 	McpU8 pktType, evtType;

	/* Identify ! */
	MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
									(" Enter hal_hci_ReceiverThread"));
	
	/* continue running as long as not terminated */
	while (1) {
		struct pollfd p;
		int n;

		p.fd = pHalHci->fd; 
		p.events = POLLIN;

		/* poll device, wait for received data */
		while ((n = poll(&p, 1, 500)) == -1) {
			if (errno == EAGAIN || errno == EINTR)
				continue;
			MCPF_REPORT_ERROR(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
									("hal_hci_ReceiverThread: failed to poll device"));
			ret = HAL_HCI_STATUS_FAILED;
			goto close;
		}

		/* we timeout once in a while - 
			TODO: check if we can elimiate this as we kill the thread on close */
		if (0 == n)
			continue;

		/* read newly arrived data */
		/* TBD: rethink assumption about single arrival */
		while ((len = read(pHalHci->fd, buf, sizeof(buf))) < 0) {
			if (errno == EAGAIN || errno == EINTR)
				continue;
			MCPF_REPORT_ERROR(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
									("hal_hci_ReceiverThread: failed to read socket"));

			ret = HAL_HCI_STATUS_FAILED;
			goto close;
		}
#ifdef TRAN_DBG
        for (i = 0; i < len; i++)
        {
            MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
				("buf[%d] = %x", i, buf[i]));
        }
#endif	

		pktType = *buf;
		evtType = *(buf + 1);
		
#ifdef TRAN_DBG
		MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
									("pktType = %x\n", pktType));
#endif	

		/* C	ommand Complete Event */
		if( (pktType == HAL_HCI_EVENT_PKT) && 
			((evtType == HAL_HCI_EVT_COMPLETED || evtType == HAL_HCI_EVT_STATUS)) )
		{
			MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
										("evtType = %x", evtType));

			MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
					("ReceiverThread call the CMD Complete callback" )); 

			pHalHci->opcodeSent = 0;
			/* If Cmd Cmplt CB returns PENDING --> Rx Congestion */
			/* Wait for indication from stack that buffer is avialable */
			while ( RES_PENDING == pHalHci->CmdCompleteCallback(pHalHci->hCmdComplete, buf, len, 
														HAL_HCI_STATUS_SUCCESS) )
			{
				waitForSem (pHalHci->hMcpf, &pHalHci->tSemBufAvail);
			}
		}
		else /* Data or A-sync event */
		{
			/* notify stack that RX packet has arrived */
#ifdef TRAN_DBG
			MCPF_REPORT_INFORMATION(pHalHci->hMcpf, HAL_HCI_MODULE_LOG, 
				("ReceiverThread call the RX callback" )); 
#endif	

			/* If Rx CB returns PENDING --> Rx Congestion */
			/* Wait for indication from stack that buffer is avialable */
			while ( RES_PENDING == pHalHci->RxIndCallabck(pHalHci->hRxInd, buf, len, 
														HAL_HCI_STATUS_SUCCESS) )
			{
				waitForSem (pHalHci->hMcpf, &pHalHci->tSemBufAvail);
			}
		}
	}

close:
	signalSem(pHalHci->hMcpf, &pHalHci->tSemRxThreadClose);
	pthread_exit((void *)ret);

	return NULL;
}

/**
 * \fn     signalSem
 * \brief  Signal semaphore
 *
 * Release counting semaphore and increments count by one unit
 *
 * \note
 * \param   hMcpf - OS framework handle
 * \param   hSem  - OS semphore handle
 * \return  void
 * \sa
 */
static void signalSem (handle_t hMcpf, SEM_HANDLE hSem)
{
    int result;

    /* Returns 0 on success */
    result = sem_post(hSem);

    if (result)
    {
        MCPF_REPORT_ERROR (hMcpf, HAL_HCI_MODULE_LOG, 
			("%s: sem_post failed, handle=%p!\n", __FUNCTION__, hSem));
    }
}


/**
 * \fn     waitForSem
 * \brief  Waits for semaphore
 *
 * Wait for counting semaphore forever
 *
 * \note
 * \param   hMcpf - OS framework handle
 * \param   hSem  - OS semphore handle
 * \return  void
 * \sa
 */
static void waitForSem (handle_t hMcpf, SEM_HANDLE hSem)
{
    int result;

    /* wait infinitely for the semaphore.
     * sem_wait() is used to acquire the semaphore access.
     * This function suspends the calling thread until the semaphore has a non-zero count.
     * It then atomically decreases the semaphore count */
    result = sem_wait (hSem);

    /* If the call was unsuccessful, the state of the semaphore is unchanged,
     * and the function returns a value of -1 and sets errno to indicate the error. */
    if (result == -1)
    {
        MCPF_REPORT_ERROR (hMcpf, HAL_HCI_MODULE_LOG,
            ("%s: sem_wait failed, result = %d \n", __FUNCTION__, result));
    }
}

