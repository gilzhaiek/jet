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
/*******************************************************************************\
*
*   FILE NAME:      mcp_hal_string.c
*
*   DESCRIPTION:    This file implements the MCP HAL string utilities for Windows.
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <mbstring.h>

#include "mcp_hal_string.h"

/****************************************************************************
 *
 * Local Prototypes
 *
 ***************************************************************************/

/****************************************************************************
 *
 * Local Functions Definitions
 *
 ***************************************************************************/
static void MCP_HAL_STRING_itoa(int n, unsigned char s[]);
static void MCP_HAL_STRING_reverse(char s[]);
 
/****************************************************************************
 *
 * Public Functions
 *
 ***************************************************************************/

McpU8 MCP_HAL_STRING_StrCmp(const char *Str1, const char *Str2)
{
	while (*Str1 == *Str2) {
        if (*Str1 == 0 || *Str2 == 0) {
            break;
        }
        Str1++;
        Str2++;
    }

    /* Return zero on success, just like the ANSI strcmp() */
    if (*Str1 == *Str2)
        return 0;

    return 1;
}

McpU8 MCP_HAL_STRING_StriCmp(const char *Str1, const char *Str2)
{	
	const McpU32 lowerToUpperDiff = 'a' - 'A';
	
	while (*Str1 != 0 || *Str2 != 0)
	{
		if ( 	(*Str1==*Str2) || 
			((*Str1 >= 'A') && (*Str1 <= 'Z') && ((char)(*Str1 + lowerToUpperDiff) == *Str2)) ||
			((*Str2 >= 'A') && (*Str2 <= 'Z') && ((char)(*Str2 + lowerToUpperDiff) == *Str1)))
		{
			Str1++;
			Str2++;
		}
		else
			return 1;
	}
	if (*Str1 == *Str2) /* both pointers reached NULL */
	        return 0;
	return 1;
}

McpU16 MCP_HAL_STRING_StrLen(const char *Str)
{
	const char  *cp = Str;

	while (*cp != 0) cp++;

	return (McpU16)(cp - Str);
}

char* MCP_HAL_STRING_StrCpy(char* StrDest, const char *StrSource)
{
	return strcpy(StrDest, StrSource);
}

char* MCP_HAL_STRING_StrnCpy(char* StrDest, const char *StrSource, McpU32 Count)
{
	return strncpy(StrDest, StrSource, Count);
}

char *MCP_HAL_STRING_StrCat(char *strDest, const char *strSource)
{
	return strcat(strDest, strSource);
}

char *MCP_HAL_STRING_StrrChr(const char *Str, McpS32 c)
{
	return strrchr(Str, c);
}

char *MCP_HAL_STRING_Strstr(const char *Str1, const char *Str2)
{	
    return strstr(Str1,Str2);
}

McpS32 MCP_HAL_STRING_Sprintf(char* StrDest,const char* format,...)
{
	va_list ap;
	McpS32 len = 0;
	if (format != NULL){
		va_start(ap, format);
		len = vsprintf(StrDest, format, ap);
		va_end(ap);
	}
	return len;
}

