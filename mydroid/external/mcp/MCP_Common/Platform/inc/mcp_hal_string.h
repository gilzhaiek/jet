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
*   FILE NAME:      mcp_hal_string.h
*
*   BRIEF:          This file defines String-related HAL utilities.
*
*   DESCRIPTION:    General
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/

#ifndef __MCP_HAL_STRING_H
#define __MCP_HAL_STRING_H


/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/

#include "mcp_hal_types.h"
#include "mcp_hal_config.h"
#include "mcp_hal_defs.h"

/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_StrCmp()
 *
 * Brief: 
 *	    Compares two strings for equality.
 * 
 * Description: 
 *	    Compares two strings for equality.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *     Str1 [in] - String to compare.
 *
 *     Str2 [in] - String to compare.
 *
 * Returns:
 *     Zero - If strings match.
 *     Non-Zero - If strings do not match.
 */
McpU8 MCP_HAL_STRING_StrCmp(const char *Str1, const char *Str2);


/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_StriCmp()
 *
 * Brief: 
 *     Compares two strings for equality regardless of case for the ASCII
 *     characters (value 0x1-0x7F) in the string.
 *
 * Description: 
 *     Compares two strings for equality regardless of case for the ASCII
 *     characters (value 0x1-0x7F) in the string.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		Str1 [in] - String to compare.
 *
 *		Str2 [in]- String to compare.
 *
 * Returns:
 *     Zero - If strings match.
 *     Non-Zero - If strings do not match.
 */
McpU8 MCP_HAL_STRING_StriCmp(const char *Str1, const char *Str2);


/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_StrLen()
 *
 * Brief: 
 *	    Calculate the length (number of bytes) in the 0-terminated string.
 *
 * Description: 
 *	    Calculate the length (number of bytes) in the 0-terminated string.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		Str [in]- String to count length 
 *
 * Returns:
 *     Returns length of string.(number of bytes)
 */
McpU16 MCP_HAL_STRING_StrLen(const char *Str);


/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_StrCpy()
 *
 * Brief: 
 *	    Copy a string (same as ANSI C strcpy)
 *
 * Description: 
 *	    Copy a string (same as ANSI C strcpy)
 *  
 * 	    The OS_StrCpy function copies StrSource, including the terminating null character, 
 *	    to the location specified by StrDest. No overflow checking is performed when strings 
 *	    are copied or appended. 
 *
 *	    The behavior of OS_StrCpy is undefined if the source and destination strings overlap 
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		StrDest [out]- Destination string.
 *
 *		StrSource [in]- Source string
 *
 * Returns:
 *      Returns StrDest. No return value is reserved to indicate an error.
 */
char* MCP_HAL_STRING_StrCpy(char* StrDest, const char *StrSource);


/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_StrnCpy()
 *
 * Brief: 
 *	    Copy characters of one string to another (same as ANSI C strncpy)
 *
 * Description: 
 *	    Copy characters of one string to another (same as ANSI C strncpy)
 *
 * 		The OS_StrnCpy function copies the initial Count characters of StrSource to StrDest and 
 *		returns StrDest. If Count is less than or equal to the length of StrSource, a null character 
 *		is not appended automatically to the copied string. If Count is greater than the length of 
 *		StrSource, the destination string is padded with null characters up to length Count. 
 *
 *		The behavior of OS_StrnCpy is undefined if the source and destination strings overlap.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		StrDest [out] - Destination string.
 *
 *		StrSource [in] - Source string
 *
 *		Count - Number of bytes to be copied
 *
 * Returns:
 *     Returns strDest. No return value is reserved to indicate an error.
 */
char* MCP_HAL_STRING_StrnCpy(char* StrDest, const char *StrSource, McpU32 Count);


/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_StrCat()
 *
 * Brief: 
 *		Append a string (same as ANSI C strcat)
 *
 * Description: 
 *		Append a string (same as ANSI C strcat)
 *
 * 		The OS_StrrChr function finds the last occurrence of c (converted to char) in string. 
 *		The search includes the terminating null character.
 *		The OS_StrCat function appends strSource to strDest and terminates the resulting string 
 *		with a null character. The initial character of strSource overwrites the terminating null 
 *		character of strDest. No overflow checking is performed when strings are copied or 
 *		appended. The behavior of OS_StrCat is undefined if the source and destination strings 
 *		overlap
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		strDest [in] - Null-terminated destination string.
 *
 *		strSource [in] - Null-terminated source string
 *
 * Returns:
 *		Returns the destination string (strDest). No return value is reserved to indicate an error.
 */
char *MCP_HAL_STRING_StrCat(char *strDest, const char *strSource);

char *MCP_HAL_STRING_StrnCat(char *strDest, const char *strSource,McpU32 Count);

/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_StrrChr()
 *
 * Brief: 
 *      Scan a string for the last occurrence of a character (same as ANSI C strrchr).
 *
 * Description:
 *      Scan a string for the last occurrence of a character (same as ANSI C strrchr).
 * 		The MCP_HAL_STRING_StrrChr function finds the last occurrence of c (converted to char) in string. 
 *		The search includes the terminating null character.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		Str [in] - tNull-terminated string to search
 *
 *      c [in] - Character to be located.
 *
 * Returns:
 *	
 *		Returns a pointer to the last occurrence of c in Str, or NULL if c is not found.
 */
char *MCP_HAL_STRING_StrrChr(const char *Str, McpS32 c);

/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_Strstr()
 *
 * Brief: 
 *      Scan a string for the first occurrence of a second string (same as ANSI C strstr).
 * 
 * Description: 
 *      Scan a string for the first occurrence of a second string (same as ANSI C strstr).
 * 		The MCP_HAL_STRING_Strstr function finds the first occurrence of Str2 in Str1. 
 *		The search includes the terminating null character.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		Str1 [in] - Null-terminated string to search in.
 *
 *      Str2 [in] - Null-terminated string to find.
 *
 * Returns:
 *	
 *		Returns a pointer to the begining of Str2 in Str1, or NULL if Str2 is not found.
 */
char *MCP_HAL_STRING_Strstr(const char *Str1, const char *Str2);


/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_Sprintf()
 *
 * Brief: 
 *      Print a formated string into a given buffer.
 *
 * Description:
  *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		StrDest [Out] - Destination string to hold the formatted output
 *
 *      	format [in] - Format string.
 * 
 * 		... [in] - List of arguments to be printed
 *
 * Returns:
 *	
 *		Returns the total count of printed characters (not including trailing '\0'.
 */
McpS32 MCP_HAL_STRING_Sprintf(char* StrDest,const char* format,...);

/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_ItoA()
 *
 * Brief: 
 *      Return the given number as stirng
 *
 * Description:
  *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		Num [in] - The number to convert
 *
 *      	Buff [out] - The output stirng
 * 
 * Returns:
 *	
 *		None
 */
void MCP_HAL_STRING_ItoA(McpU16 Num,McpU8* Buff);


/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_AtoI(char * Buff)
 *
 * Brief: 
 *      Convert Num to String and store in Buff
 *
 * Description:
  *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		Buff [in] - String that needs to be converted to integer
 *
 *
 * Returns:
 *	
 *		McpU16
 */

McpS16 MCP_HAL_STRING_AtoI(McpU8* Buff);

/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_strtol(McpU8* Buff, McpU8* p, McpU8 base)
 *
 * Brief: 
 *      Convert string to long and stored in buff
 *
 * Description:
  *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		Buff [in] - String that needs to be converted to integer
 *		p[in] - 
 *		base [in] - base for conversation
 *
 *
 * Returns:
 *	
 */	
McpU32 MCP_HAL_STRING_strtol(McpU8* Buff, McpU8* p, McpU8 base);


/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_strtok()
 *
 * Brief: 
 *      Retrive tokens from Str1
 *
 * Description:
  *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		Str1 [in] - input buffer, tokens will be found from this strings
 *
 *      	Str2 [in] - delimeter list
 * 
 *
 * Returns:
 *	
 *		pointer to the next token
 */

McpU8* MCP_HAL_STRING_strtok(const char *Str1, const char *Str2);


/*-------------------------------------------------------------------------------
 * MCP_HAL_STRING_strupr()
 *
 * Brief: 
 *      converts all alphabets in the string to upper case
 *
 * Description:
  *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *		Str1 [in] - string that needs convertion
 *
 *
 * Returns:
 *	
 *		returns a pointer to the same string
 */

McpU8* MCP_HAL_STRING_strupr(const char *Str1);



#endif	/* __MCP_HAL_STRING_H */

