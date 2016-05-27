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
#include <windows.h>
#include <stdarg.h>
#include <stdio.h>

#include "pla_cmdParse.h"


/************************************************************************/
/*	                         Definitions                                */
/************************************************************************/


/************************************************************************/
/*						    Global Variables                            */
/************************************************************************/


/************************************************************************/
/*					Internal Function Deceleration                      */
/************************************************************************/



/************************************************************************/
/*								APIs                                    */
/************************************************************************/
/** 
 * \fn     pla_cmdParse_GeneralCmdLine
 * \brief  Parses the initial command line
 * 
 * This function parses the initial command line, 
 * and returns the BT & NAVC strings, 
 * along with the common parameter.
 * 
 * \note
 * \param	CmdLine     - Received command Line.
 * \param	**bt_str - returned BT cmd line.
 * \param	**nav_str - returned NAVC cmd line.
 * \return 	Port Number
 * \sa     	pla_cmdParse_GeneralCmdLine
 */
McpU32 pla_cmdParse_GeneralCmdLine(LPSTR CmdLine, char **bt_str, char **nav_str)
{
	char		*temp1;
	int		bt_len, nav_len, port_len;
	char		*pPort;
	char		*pBt;
	char		*pNav;
	McpU32	uPortNum = 0;
	
	
	/* Find common parameter - port number */
	pPort = strstr(CmdLine, "-p");
	if(pPort == NULL)
		return 0;
	
	/* Find the BT command line */
	pBt = strstr(CmdLine, "-bt\"");
	
	/* Find the NAVC command line */
	pNav = strstr(CmdLine, "-nav\"");
	
	/* Add port number to the command line of both the Apps. */
	{
		temp1 = strstr(pPort, " ");
		if(!temp1)
			port_len = strlen(pPort);
		else
			port_len = temp1 - pPort;
		
		strncpy(*bt_str, pPort, port_len);
		strncpy(*nav_str, pPort, port_len);
		
		uPortNum = atoi(pPort+2);
		
		if(pBt)
			(*bt_str)[port_len] = ' ';
		else
			(*bt_str)[port_len] = '\0';
		
		if(pNav)
			(*nav_str)[port_len] = ' ';
		else
			(*nav_str)[port_len] = '\0';
		
		/* Added for space */
		port_len++;
	}
	
	/* Parse other parameters */
	{
		/* Find the BT command line  and add it to the 'bt_str' */
		if(pBt) /* If '-bt' exists */
		{
			pBt += 4;
			temp1 = strstr(pBt, "\"");
			bt_len = temp1-pBt;
			strncpy((*bt_str)+port_len, pBt, bt_len);
			(*bt_str)[port_len+bt_len] = '\0';
		}
		
		/* Find the NAVC command line  and add it to the 'nav_str' */
		if(pNav) /* If '-nav' exists */
		{
			pNav += 5;
			temp1 = strstr(pNav, "\"");
			nav_len = temp1-pNav;
			if(nav_len)
			{
				strncpy((*nav_str)+port_len, pNav, nav_len);
				(*nav_str)[port_len+nav_len] = '\0';
			}
			else
			{
				(*nav_str)[port_len-1] = '\0';
			}
		}
	}
	
	return uPortNum;
}


/** 
 * \fn     pla_cmdParse_NavcCmdLine
 * \brief  Parses the NAVC command line
 * 
 * This function parses the NAVC command line.
 * 
 * \note
 * \param	*nav_str - NAVC cmd line.
 * \param	*tCmdLineParams - Params to fill.
 * \return 	None
 * \sa     	pla_cmdParse_NavcCmdLine
 */
void pla_cmdParse_NavcCmdLine(char *nav_str, TcmdLineParams *tCmdLineParams)
{
	char	*temp;
	char	*pHyphen;
	char	*pSpace;
	char *pComma;
	char *pPort;
	McpU16	i = 0;
	
		
	/* Parse params */
	temp = nav_str;
	while(temp)
	{
		pHyphen = strstr(temp, "-");
		pSpace = strstr(temp, " ");
		if(pSpace != NULL)
		{
			temp = pSpace + 1;
		}
		else
		{
			temp = pSpace;
		}
		
		if( (strncmp( pHyphen, "-p", 2 ) == 0) &&  	(sscanf( pHyphen+2, "%d", &tCmdLineParams->x_SensorPcPort) == 1) )
		{
			continue;
		}

		if( (strncmp( pHyphen, "-l", 2 ) == 0) &&  (sscanf( pHyphen+2, "%d", &tCmdLineParams->x_NmeaPcPort) == 1) )
		{
			 continue;
		}

		if(strncmp( pHyphen, "-ads", 4 ) == 0) 
		{
			pPort = pHyphen+4;
			pComma = strstr(pPort, ",");
			while (pComma)
			{
			 	sscanf( pPort, "%d", &tCmdLineParams->x_AdsPort[i]);
				i++;
				pPort = pComma + 1;
				pComma = strstr(pPort, ",");
			}
			sscanf( pPort, "%d", &tCmdLineParams->x_AdsPort[i]);
			i++;
			tCmdLineParams->x_AdsPortsCounter = i; 
            		continue;
		}
		
		if( (strncmp( pHyphen, "-b", 2 ) == 0) )
		{
			/* To Restrict the Almanac data injection during 3GPP test*/
			sscanf(pHyphen+2, "%x", &tCmdLineParams->w_AlmanacDataBlockFlag);
			continue;
		}

	   if( (strncmp( pHyphen, "-c", 2 ) == 0) )
	        {
	            /* Path Control File */
	            sscanf(pHyphen+2, "%s", &tCmdLineParams->s_PathCtrlFile);
	            continue;
	        }

	}	
}
