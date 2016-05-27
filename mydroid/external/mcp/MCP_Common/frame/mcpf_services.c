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

/** Include Files **/
#include "mcpf_services.h"
#include "mcp_hal_os.h"
#include "mcp_hal_fs.h"
#include "pla_hw.h"

#include <utils/Log.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <libgen.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>


/** APIs - Critical Section **/

/** 
 * \fn     mcpf_critSec_CreateObj
 * \brief  Create critical section object
 * 
 * This function creates a critical section object.
 * 
 * \note
 * \param	hMcpf - MCPF handler. 
 * \param	*sCritSecName - name of critical section object.
 * \param	*pCritSecHandle - Handler to the created critical section object.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_critSec_CreateObj
 */ 
EMcpfRes mcpf_critSec_CreateObj(handle_t hMcpf, const char *sCritSecName, handle_t *pCritSecHandle)
{
	MCPF_UNUSED_PARAMETER(hMcpf);
	return MCP_HAL_OS_CreateSemaphore(sCritSecName, pCritSecHandle);
}

/** 
 * \fn     mcpf_critSec_DestroyObj
 * \brief  Destroy the critical section object
 * 
 * This function destroys the critical section object.
 * 
 * \note
 * \param	hMcpf - MCPF handler. 
 * \param	*pCritSecHandle - Handler to the created critical section object.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_critSec_DestroyObj
 */ 
EMcpfRes mcpf_critSec_DestroyObj(handle_t hMcpf, handle_t *pCritSecHandle)
{
	MCPF_UNUSED_PARAMETER(hMcpf);
	return MCP_HAL_OS_DestroySemaphore(pCritSecHandle);
}

/** 
 * \fn     mcpf_critSec_Enter
 * \brief  Enter critical section
 * 
 * This function tries to enter the critical section, be locking a semaphore.
 * 
 * \note
 * \param	hMcpf - MCPF handler. 
 * \param	hCritSecHandle - Handler to the created critical section object.
 * \param	uTimeout - How long to wait for the semaphore.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_critSec_Enter
 */ 
EMcpfRes mcpf_critSec_Enter(handle_t hMcpf, handle_t hCritSecHandle, McpUint uTimeout)
{
	MCPF_UNUSED_PARAMETER(hMcpf);
	return MCP_HAL_OS_LockSemaphore(hCritSecHandle, uTimeout);
}

/** 
 * \fn     mcpf_critSec_Exit
 * \brief  Exit critical section
 * 
 * This function exits the critical section, be un-locking the semaphore.
 * 
 * \note
 * \param	hMcpf - MCPF handler. 
 * \param	hCritSecHandle - Handler to the created critical section object.
  * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_critSec_Exit
 */ 
EMcpfRes mcpf_critSec_Exit(handle_t hMcpf, handle_t hCritSecHandle)
{
	MCPF_UNUSED_PARAMETER(hMcpf);
	return MCP_HAL_OS_UnlockSemaphore(hCritSecHandle);
}



/** APIs - Files **/

EMcpfRes mcpf_file_open(handle_t hMcpf, McpU8 *pFileName, McpU32 uFlags, McpS32 *pFd)

{
	MCPF_UNUSED_PARAMETER(hMcpf);

	if (MCP_HAL_FS_Open(pFileName, uFlags, pFd) == MCP_HAL_FS_STATUS_SUCCESS)

		return RES_OK;

	else

		return RES_ERROR;

}


EMcpfRes mcpf_file_close(handle_t hMcpf, McpS32 iFd)

{

	MCPF_UNUSED_PARAMETER(hMcpf);



	return MCP_HAL_FS_Close(iFd);

	

}


McpU32 mcpf_file_read(handle_t hMcpf, McpS32 iFd, McpU8 *pMem, McpU16 uNumBytes)

{

	McpU32 uNumRead = 0;


	MCPF_UNUSED_PARAMETER(hMcpf);



	MCP_HAL_FS_Read(iFd, pMem, uNumBytes, &uNumRead);	

	

	return uNumRead;

}


McpU32 mcpf_file_write(handle_t hMcpf, McpS32 iFd, McpU8 *pMem, McpU16 uNumBytes)

{

	McpU32 uNumWritten = 0;


	MCPF_UNUSED_PARAMETER(hMcpf);



	MCP_HAL_FS_Write(iFd, pMem, uNumBytes, &uNumWritten);

	

	return uNumWritten;

}

McpU32 mcpf_create_dir_path(const char *s, mode_t mode)
{
	char *q, *r = NULL, *path = NULL, *up = NULL;
	int rv;
	
	rv = -1;
	if (strcmp(s, ".") == 0 || strcmp(s, "/") == 0)
		return (0);

	if ((path = strdup(s)) == NULL)
	{
		return -1;
	}
	
	if ((q = strdup(s)) == NULL)
	{
		/* KW changes*/
		free(path);
		return -1;
	}
	
	if ((r = dirname(q)) == NULL)
		goto out;

	if ((up = strdup(r)) == NULL)
	{
		/* KW changes*/
		free(path);
		free(q);
		return -1;
	}

	if ((mcpf_create_dir_path(up, mode) == -1) && (errno != EEXIST))
		goto out;

	if ((mkdir(path, mode) == -1) && (errno != EEXIST))
		rv = -1;
	else
		rv = 0;
out:
	if (up != NULL)
		free(up);
	free(q);
	free(path);
	return (rv);
}

EMcpfRes mcpf_file_empty(handle_t hMcpf, McpS32 iFd)

{

	mcpf_file_write(hMcpf, iFd, NULL, 0);

	return RES_COMPLETE;

}


void mcpf_ExtractDateAndTime(McpU32 st_time, McpHalDateAndTime* dateAndTimeStruct)

{
	MCP_HAL_FS_ExtractDateAndTime(st_time, dateAndTimeStruct);
}


McpU32 mcpf_getFileSize  (handle_t hMcpf, McpS32 iFd)

{

	McpU32 iLen;



	MCPF_UNUSED_PARAMETER(hMcpf);


	MCP_HAL_FS_Seek(iFd, 0, MCP_HAL_FS_END);

	MCP_HAL_FS_Tell(iFd, &iLen);

	MCP_HAL_FS_Seek(iFd, 0, MCP_HAL_FS_START);

		

	return iLen;

}



/** 
 * \fn     mcpf_hwGpioSet
 * \brief  Set HW GPIOs
 * 
 * This function will set the HW GPIOs.
 * 
 * \note
 * \param	hMcpf - MCPF handler. 
 * \param	index - GPIO index.
 * \param	value - value to set.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_hwGpioSet
 */ 
EMcpfRes  	mcpf_hwGpioSet(handle_t hMcpf, McpU32 index, McpU8 value)

{

	Tmcpf *pMcpf = (Tmcpf *)hMcpf;

	return hw_gpio_set(pMcpf->hPla, index, value);

}

/** 
 * \fn     mcpf_hwRefClkSet
 * \brief  Enable/Disable Ref Clock
 * 
 * This function will Enable or Disable the Reference clock.
 * 
 * \note
 * \param	hMcpf - MCPF handler. 
 * \param	uIsEnable - Enable/Disable the ref clk.
 * \return 	Result of operation: OK or ERROR
 * \sa     	mcpf_hwRefClkSet
 */ 
EMcpfRes  	mcpf_hwRefClkSet(handle_t hMcpf, McpU8 uIsEnable)

{

	Tmcpf *pMcpf = (Tmcpf *)hMcpf;

	return hw_refClk_set(pMcpf->hPla, uIsEnable);

}


/************************************************************************/
/*                           APIs - Math                                */
/************************************************************************/ 
McpDBL	mcpf_Cos (McpDBL A)
{
	return MCP_HAL_OS_COS(A);
}

McpDBL	mcpf_Sin (McpDBL A)
{
	return MCP_HAL_OS_SIN(A);
}

McpDBL	mcpf_Atan2 (McpDBL A, McpDBL B)
{
	return MCP_HAL_OS_ATAN2(A, B);
}

McpDBL	mcpf_Sqrt (McpDBL A)
{
	return MCP_HAL_OS_SQRT(A);
}

McpDBL	mcpf_Fabs (McpDBL A)
{
	return MCP_HAL_OS_FABS(A);
}

McpDBL	mcpf_Pow (McpDBL A, McpDBL B)
{
	return MCP_HAL_OS_POW(A,B);
}
