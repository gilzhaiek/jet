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
*   FILE NAME:      mcp_hal_utils.c
*
*   DESCRIPTION:    This file implements the MCP HAL Utils for Windows.
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/
#include "mcp_endian.h"
#include "mcp_hal_log.h"

/****************************************************************************
 *
 * Local Prototypes
 *
 ***************************************************************************/

/****************************************************************************
 *
 * Public Functions
 *
 ***************************************************************************/
McpU16 MCP_ENDIAN_BEtoHost16(const McpU8 *beBuff16)
{
    return (McpU16)( ((McpU16) *beBuff16 << 8) | ((McpU16) *(beBuff16+1)) ); 
}

McpU32 MCP_ENDIAN_BEtoHost32(const McpU8 *beBuff32)
{
    return (McpU32)( ((McpU32) *beBuff32 << 24)   | \
                  ((McpU32) *(beBuff32+1) << 16) | \
                  ((McpU32) *(beBuff32+2) << 8)  | \
                  ((McpU32) *(beBuff32+3)) );
}

McpU16 MCP_ENDIAN_LEtoHost16(const McpU8 *leBuff16)
{
    return (McpU16)( ((McpU16) *(leBuff16+1) << 8) | ((McpU16) *leBuff16) ); 
}

McpU32 MCP_ENDIAN_LEtoHost32(const McpU8 *leBuff32)
{
    return (McpU32)( ((McpU32) *(leBuff32+3) << 24)   | \
                  ((McpU32) *(leBuff32+2) << 16) | \
                  ((McpU32) *(leBuff32+1) << 8)  | \
                  ((McpU32) *(leBuff32)) );
}

void MCP_ENDIAN_HostToLE16(McpU16 hostValue16, McpU8 *leBuff16)
{
   leBuff16[1] = (McpU8)(hostValue16>>8);
   leBuff16[0] = (McpU8)hostValue16;
}

void MCP_ENDIAN_HostToLE32(McpU32 hostValue32, McpU8 *leBuff32)
{
   leBuff32[3] = (McpU8) (hostValue32>>24);
   leBuff32[2] = (McpU8) (hostValue32>>16);
   leBuff32[1] = (McpU8) (hostValue32>>8);
   leBuff32[0] = (McpU8) hostValue32;
}

void MCP_ENDIAN_HostToBE16(McpU16 hostValue16, McpU8 *beBuff16)
{
   beBuff16[0] = (McpU8)(hostValue16>>8);
   beBuff16[1] = (McpU8)hostValue16;
}

void MCP_ENDIAN_HostToBE32(McpU32 hostValue32, McpU8 *beBuff32)
{
   beBuff32[0] = (McpU8) (hostValue32>>24);
   beBuff32[1] = (McpU8) (hostValue32>>16);
   beBuff32[2] = (McpU8) (hostValue32>>8);
   beBuff32[3] = (McpU8) hostValue32;
}

McpU32 mcpf_getBits(McpU8* pBuf, McpU32 *pBitOffset, McpU32 uBitLength)
{
    McpU32  uOfs        = *pBitOffset;
    McpU32  uFirstByte  = uOfs / 8;
    McpU32  uLastByte   = (uOfs + uBitLength) / 8;
    McpU32  uFirstBits  = uOfs % 8;
    McpU32  uLsbBits    = (uOfs + uBitLength) % 8;
    McpU32  uVal		= 0;
	McpU32	uIndx		= uFirstByte;
	McpU32	uMask;

    while (uIndx < uLastByte) 
    {
        uVal <<= 8;
        uVal |= pBuf[uIndx++];
    }

    if (uLsbBits) 
    {
		McpU8  uLsbVal = pBuf[uIndx];  /* copy the lsb byte */

        if ((uFirstBits + uBitLength) <= 8 ) 
        {
            /* the required bit field resides in one byte */

            uVal = uLsbVal >> (8 - uFirstBits - uBitLength);
        }
        else 
        {
            /* LSB bits are to be copied from the the next byte */

            uLsbVal >>= (8 - uLsbBits);
            uVal <<= uLsbBits;
            uVal |= uLsbVal;
        }
    }

	if (uBitLength < 32)
	{
		uMask = (1 << uBitLength) - 1;
	}
	else
	{
		uMask = 0xFFFFFFFF;
	}
    uVal &= uMask;  /* clear not existing most significant bits */ 

    *pBitOffset = uOfs + uBitLength;

#ifdef RRLP_DEBUG
	MCP_HAL_LOG_DEBUG( "mcpf", 0, "mcpf_getBits", ("first=%u last=%u ofs0=%u ofs1=%u len=%u val=%u\n",
													uFirstByte, uLastByte, uOfs, *pBitOffset-1, uBitLength, uVal)); 
#endif
    return uVal;
}

McpS32 mcpf_getSignedBits(McpU8* pBuf, McpU32 *pBitOffset, McpU32 uBitLength)
{
	McpU32 u32 = mcpf_getBits(pBuf, pBitOffset, uBitLength);
	if(u32 & (1<<(uBitLength-1)))
		u32 |= (0xffffffff<<uBitLength);
	return (McpS32)u32;
}			


/** 
 * \fn     mcpf_putBits
 * \brief  Put bits field
 * 
 * Put bits field into output buffer
 * 
 */ 

#define GETBITS_U32( uInVal, uStartBitOfs, uBitLength ) \
			((McpU8)(((uInVal) >> (uStartBitOfs)) & ((1 << (uBitLength)) - 1)))

void mcpf_putBits(McpU8* pBuf, McpU32 *pBitOffset, McpU32 uInputVal, McpU32 uInputBitLength)
{
	McpU32  uBitOfs 	= *pBitOffset;
	McpU32 	uIndx		= uBitOfs / 8;                      /* starting byte of buffer to add bits      */
	McpU32 	uMsbBits 	= 8 - uBitOfs % 8; 					/* most significant bits to add 			*/
	McpU32	uLastByte   = (uBitOfs + uInputBitLength) / 8;  /* the last byte of buffer to add bit field */
	McpU32 	uLsbBits 	= (uBitOfs + uInputBitLength) % 8; 	/* least significant bits 					*/
	McpU32	uInputFromBitPos = uInputBitLength;

	/* Are MSB bits to be added to the start byte of buffer? */
	if (uMsbBits < 8)
	{
		/* Clear bits in byte to be filled */
		pBuf[uIndx] &= (McpU8) (0xFF << uMsbBits);

		if (uInputBitLength <= uMsbBits)
		{
			/* Bit field length (to add) suites to complete area size or less */
			pBuf[uIndx++] |= (uInputVal << (uMsbBits - uInputBitLength));

			*pBitOffset += uInputBitLength;
			return; /* Done */
		}
		else
		{
			/* Bit field length (to add) is greater than remained area size, 
			 * take required bit field from input value to complete the first byte
			 */
			McpU8   bitsVal;

			uInputFromBitPos -= uMsbBits;
			bitsVal = GETBITS_U32 (uInputVal, uInputFromBitPos, uMsbBits);
			pBuf[uIndx++] |= bitsVal;
		}
	}

	/* Copy to buffer in byte units */
	while (uIndx < uLastByte)
	{
		uInputFromBitPos -= 8;
		pBuf[uIndx++] = GETBITS_U32 (uInputVal, uInputFromBitPos, 8);
	}

	/* Add LSB bits to the last byte */
	if (uLsbBits)
	{
		McpU8   bitsVal;

		bitsVal = GETBITS_U32 (uInputVal, 0, uLsbBits);
		pBuf[uIndx++] = (McpU8) (bitsVal << (8 - uLsbBits));
	}

	*pBitOffset += uInputBitLength;
}
