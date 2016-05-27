/*
* Copyright (c) 2007-2011 Rx Networks, Inc. All rights reserved.
*
* Property of Rx Networks
* Proprietary and Confidential
* Do NOT distribute
* 
* Any use, distribution, or copying of this document requires a 
* license agreement with Rx Networks. 
* Any product development based on the contents of this document 
* requires a license agreement with Rx Networks. 
* If you have received this document in error, please notify the 
* sender immediately by telephone and email, delete the original 
* document from your electronic files, and destroy any printed 
* versions.
*
* This file contains sample code only and the code carries no 
* warranty whatsoever.
* Sample code is used at your own risk and may not be functional. 
* It is not intended for commercial use.   
*
* Example code to illustrate how to integrate Rx Networks PGPS 
* System into a client application.
*
* The emphasis is in trying to explain what goes on in the software,
* and how to the various API functions relate to each other, 
* rather than providing a fully optimized implementation.
*
*************************************************************************
* $LastChangedDate: 2009-03-02 15:53:19 -0800 (Mon, 02 Mar 2009) $
* $Revision: 9396 $
*************************************************************************
*
* This file contains implementation of APIs exposed within the RXN_CL_Data.h file.
* 
*/


#include "RXN_CL_Common.h"  /* Includes declarations for fcns within and common declarations. */

//////////////////////////////////////////////////////////////////////////////
// Add Methods ///////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

U08 CL_BFAdd_S08(CL_BITFIELD_t* pBField, S08 Value, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper start bit and number of bits is provided.
	if(uValStartBit + uValNumBits > 8)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Add the value as a U64.
	return CL_BFAddValue(pBField, (U64) Value, 8, uValStartBit, uValNumBits);
}

U08 CL_BFAdd_U08(CL_BITFIELD_t* pBField, U08 Value, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper start bit and number of bits is provided.
	if(uValStartBit + uValNumBits > 8)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Add the value as a U64.
	return CL_BFAddValue(pBField, (U64) Value, 8, uValStartBit, uValNumBits);
}

U08 CL_BFAdd_S16(CL_BITFIELD_t* pBField, S16 Value, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper start bit and number of bits is provided.
	if(uValStartBit + uValNumBits > 16)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Add the value as a U64.
	return CL_BFAddValue(pBField, (U64) Value, 16, uValStartBit, uValNumBits);
}

U08 CL_BFAdd_U16(CL_BITFIELD_t* pBField, U16 Value, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper start bit and number of bits is provided.
	if(uValStartBit + uValNumBits > 16)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Add the value as a U64.
	return CL_BFAddValue(pBField, (U64) Value, 16, uValStartBit, uValNumBits);
}

U08 CL_BFAdd_S32(CL_BITFIELD_t* pBField, S32 Value, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper start bit and number of bits is provided.
	if(uValStartBit + uValNumBits > 32)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Add the value as a U64.
	return CL_BFAddValue(pBField, (U64) Value, 32, uValStartBit, uValNumBits);
}

U08 CL_BFAdd_U32(CL_BITFIELD_t* pBField, U32 Value, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper start bit and number of bits is provided.
	if(uValStartBit + uValNumBits > 32)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Add the value as a U64.
	return CL_BFAddValue(pBField, (U64) Value, 32, uValStartBit, uValNumBits);
}

U08 CL_BFAdd_S64(CL_BITFIELD_t* pBField, S64 Value, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper start bit and number of bits is provided.
	if(uValStartBit + uValNumBits > 64)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Add the value as a U64.
	return CL_BFAddValue(pBField, (U64) Value, 64, uValStartBit, uValNumBits);
}

U08 CL_BFAdd_U64(CL_BITFIELD_t* pBField, U64 Value, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper start bit and number of bits is provided.
	if(uValStartBit + uValNumBits > 64)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Add the value as a U64.
	return CL_BFAddValue(pBField, (U64) Value, 64, uValStartBit, uValNumBits);
}

U08 CL_BFAddOnes(CL_BITFIELD_t* pBField, U16 uNumBits)
{
  U16 uNumQuadWords = 0;
  U08 uPartialBits = 0;
  U08 uResult;
  int x =0;

	// Calc the number of full U64's that will be added.
	uNumQuadWords = uNumBits / 64;

	// Calc the number of partial bits that will be added.
	uPartialBits = uNumBits % 64;

	// Add full quad words.
	
	for(x =0; x < uNumQuadWords; x++)
	{
		uResult = CL_BFAddValue(pBField, (U64) 0xFFFFFFFF, 64, 0, 64);
		if(uResult != RXN_SUCCESS)
		{
			return uResult;
		}
	}

	// Add the partial bits.
	return CL_BFAddValue(pBField, (U64) 0xFFFFFFFF, 64, 0, uPartialBits);
}

U08 CL_BFAddZeros(CL_BITFIELD_t* pBField, U16 uNumBits)
{
  U16 uNumQuadWords = 0;
  U08 uPartialBits = 0;
  U08 uResult;
  int x =0;

	// Calc the number of full U64's that will be added.
	uNumQuadWords = uNumBits / 64;

	// Calc the number of partial bits that will be added.
	uPartialBits = uNumBits % 64;

	// Add full quad words.
	uResult;
	for(x =0; x < uNumQuadWords; x++)
	{
		uResult = CL_BFAddValue(pBField, (U64) 0x00000000, 64, 0, 64);
		if(uResult != RXN_SUCCESS)
		{
			return uResult;
		}
	}

	// Add the partial bits.
	return CL_BFAddValue(pBField, (U64) 0x00000000, 64, 0, uPartialBits);
}

//////////////////////////////////////////////////////////////////////////////
// SetByByte Methods /////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

U08 CL_BFSetByByte_S08(CL_BITFIELD_t* pBField, S08 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 7)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFSetByBit_S08(pBField, Value, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits, uValStartBit);
}

U08 CL_BFSetByByte_U08(CL_BITFIELD_t* pBField, U08 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 7)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFSetByBit_U08(pBField, Value, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits, uValStartBit);
}

U08 CL_BFSetByByte_S16(CL_BITFIELD_t* pBField, S16 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 15)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFSetByBit_S16(pBField, Value, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits, uValStartBit);
}

U08 CL_BFSetByByte_U16(CL_BITFIELD_t* pBField, U16 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 15)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFSetByBit_U16(pBField, Value, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits, uValStartBit);
}

U08 CL_BFSetByByte_S32(CL_BITFIELD_t* pBField, S32 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 31)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFSetByBit_S32(pBField, Value, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits, uValStartBit);
}

U08 CL_BFSetByByte_U32(CL_BITFIELD_t* pBField, U32 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 31)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFSetByBit_U32(pBField, Value, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits, uValStartBit);
}

U08 CL_BFSetByByte_S64(CL_BITFIELD_t* pBField, S64 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 63)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFSetByBit_S64(pBField, Value, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits, uValStartBit);
}

U08 CL_BFSetByByte_U64(CL_BITFIELD_t* pBField, U64 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 63)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFSetByBit_U64(pBField, Value, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits, uValStartBit);
}

//////////////////////////////////////////////////////////////////////////////
// SetByBit Methods //////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

U08 CL_BFSetByBit_S08(CL_BITFIELD_t* pBField, S08 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper total bit offset, start bit and number of bits is provided.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValStartBit + uValNumBits > 8))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value as a U64.
	CL_BFSetValue(pBField, (U64) Value, 8, uTotBitOffset, uValStartBit, uValNumBits);

	return RXN_SUCCESS;
}

U08 CL_BFSetByBit_U08(CL_BITFIELD_t* pBField, U08 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper total bit offset, start bit and number of bits is provided.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValStartBit + uValNumBits > 8))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value as a U64.
	CL_BFSetValue(pBField, (U64) Value, 8, uTotBitOffset, uValStartBit, uValNumBits);

	return RXN_SUCCESS;
}

U08 CL_BFSetByBit_S16(CL_BITFIELD_t* pBField, S16 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper total bit offset, start bit and number of bits is provided.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValStartBit + uValNumBits > 16))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value as a U64.
	CL_BFSetValue(pBField, (U64) Value, 16, uTotBitOffset, uValStartBit, uValNumBits);

	return RXN_SUCCESS;
}

U08 CL_BFSetByBit_U16(CL_BITFIELD_t* pBField, U16 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper total bit offset, start bit and number of bits is provided.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValStartBit + uValNumBits > 16))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value as a U64.
	CL_BFSetValue(pBField, (U64) Value, 16, uTotBitOffset, uValStartBit, uValNumBits);

	return RXN_SUCCESS;
}

U08 CL_BFSetByBit_S32(CL_BITFIELD_t* pBField, S32 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper total bit offset, start bit and number of bits is provided.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValStartBit + uValNumBits > 32))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value as a U64.
	CL_BFSetValue(pBField, (U64) Value, 32, uTotBitOffset, uValStartBit, uValNumBits);

	return RXN_SUCCESS;
}

U08 CL_BFSetByBit_U32(CL_BITFIELD_t* pBField, U32 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper total bit offset, start bit and number of bits is provided.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValStartBit + uValNumBits > 32))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value as a U64.
	CL_BFSetValue(pBField, (U64) Value, 32, uTotBitOffset, uValStartBit, uValNumBits);

	return RXN_SUCCESS;
}

U08 CL_BFSetByBit_S64(CL_BITFIELD_t* pBField, S64 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper total bit offset, start bit and number of bits is provided.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValStartBit + uValNumBits > 64))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value as a U64.
	CL_BFSetValue(pBField, (U64) Value, 64, uTotBitOffset, uValStartBit, uValNumBits);

	return RXN_SUCCESS;
}

U08 CL_BFSetByBit_U64(CL_BITFIELD_t* pBField, U64 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit)
{
	// Validate that a proper total bit offset, start bit and number of bits is provided.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValStartBit + uValNumBits > 64))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value as a U64.
	CL_BFSetValue(pBField, (U64) Value, 64, uTotBitOffset, uValStartBit, uValNumBits);

	return RXN_SUCCESS;
}

//////////////////////////////////////////////////////////////////////////////
// GetByByte Methods /////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

U08 CL_BFGetByByte_S08(CL_BITFIELD_t* pBField, S08* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 7)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFGetByBit_S08(pBField, pValue, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits);
}

U08 CL_BFGetByByte_U08(CL_BITFIELD_t* pBField, U08* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 7)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFGetByBit_U08(pBField, pValue, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits);
}

U08 CL_BFGetByByte_S16(CL_BITFIELD_t* pBField, S16* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 15)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFGetByBit_S16(pBField, pValue, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits);
}

U08 CL_BFGetByByte_U16(CL_BITFIELD_t* pBField, U16* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 15)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFGetByBit_U16(pBField, pValue, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits);
}

U08 CL_BFGetByByte_S32(CL_BITFIELD_t* pBField, S32* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 31)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFGetByBit_S32(pBField, pValue, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits);
}

U08 CL_BFGetByByte_U32(CL_BITFIELD_t* pBField, U32* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 31)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFGetByBit_U32(pBField, pValue, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits);
}

U08 CL_BFGetByByte_S64(CL_BITFIELD_t* pBField, S64* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 63)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFGetByBit_S64(pBField, pValue, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits);
}

U08 CL_BFGetByByte_U64(CL_BITFIELD_t* pBField, U64* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits)
{
	// Validate that a proper byte offset is provided.
	if((uByteOffset + 1) > pBField->byteCnt)
	{
		return CL_BITFIELD_BAD_BYTE_PARAMS;
	}

	// Validate that a proper bit offset within a byte is provided.
	if(uByteBitOffset > 63)
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Set the value using the total bit offset.
	return CL_BFGetByBit_U64(pBField, pValue, (U32) ((uByteOffset * 8) + uByteBitOffset), uValNumBits);
}

//////////////////////////////////////////////////////////////////////////////
// GetByBit Methods //////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

U08 CL_BFGetByBit_S08(CL_BITFIELD_t* pBField, S08* pValue, U16 uTotBitOffset, U08 uValNumBits)
{
  U64 uValue = 0;

	// Verify that a correct number of bits was specified.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValNumBits > 8))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Get the value as a U64.
	uValue = CL_BFGetValue(pBField, uTotBitOffset, uValNumBits);

	// Set pValue.
	*pValue = (S08) uValue;

	return RXN_SUCCESS;
}

U08 CL_BFGetByBit_U08(CL_BITFIELD_t* pBField, U08* pValue, U16 uTotBitOffset, U08 uValNumBits)
{
  U64 uValue = 0;

	// Verify that a correct number of bits was specified.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValNumBits > 8))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Get the value as a U64.
	uValue = CL_BFGetValue(pBField, uTotBitOffset, uValNumBits);

	// Set pValue.
	*pValue = (U08) uValue;

	return RXN_SUCCESS;
}

U08 CL_BFGetByBit_S16(CL_BITFIELD_t* pBField, S16* pValue, U16 uTotBitOffset, U08 uValNumBits)
{
  U64 uValue = 0;

	// Verify that a correct number of bits was specified.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValNumBits > 16))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Get the value as a U64.
	uValue = CL_BFGetValue(pBField, uTotBitOffset, uValNumBits);

	// Set pValue.
	*pValue = (S16) uValue;

	return RXN_SUCCESS;
}

U08 CL_BFGetByBit_U16(CL_BITFIELD_t* pBField, U16* pValue, U16 uTotBitOffset, U08 uValNumBits)
{
  U64 uValue = 0;

	// Verify that a correct number of bits was specified.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValNumBits > 16))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Get the value as a U64.
	uValue = CL_BFGetValue(pBField, uTotBitOffset, uValNumBits);

	// Set pValue.
	*pValue = (U16) uValue;

	return RXN_SUCCESS;
}

U08 CL_BFGetByBit_S32(CL_BITFIELD_t* pBField, S32* pValue, U16 uTotBitOffset, U08 uValNumBits)
{
  U64 uValue = 0;

	// Verify that a correct number of bits was specified.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValNumBits > 32))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Get the value as a U64.
	uValue = CL_BFGetValue(pBField, uTotBitOffset, uValNumBits);

	// Set pValue.
	*pValue = (S32) uValue;

	return RXN_SUCCESS;
}

U08 CL_BFGetByBit_U32(CL_BITFIELD_t* pBField, U32* pValue, U16 uTotBitOffset, U08 uValNumBits)
{
  U64 uValue = 0;

	// Verify that a correct number of bits was specified.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValNumBits > 32))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Get the value as a U64.
	uValue = CL_BFGetValue(pBField, uTotBitOffset, uValNumBits);

	// Set pValue.
	*pValue = (U32) uValue;

	return RXN_SUCCESS;
}

U08 CL_BFGetByBit_S64(CL_BITFIELD_t* pBField, S64* pValue, U16 uTotBitOffset, U08 uValNumBits)
{
  U64 uValue = 0;

	// Verify that a correct number of bits was specified.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValNumBits > 64))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Get the value as a U64.
	uValue = CL_BFGetValue(pBField, uTotBitOffset, uValNumBits);

	// Set pValue.
	*pValue = (S64) uValue;

	return RXN_SUCCESS;
}

U08 CL_BFGetByBit_U64(CL_BITFIELD_t* pBField, U64* pValue, U16 uTotBitOffset, U08 uValNumBits)
{
  U64 uValue = 0;

	// Verify that a correct number of bits was specified.
	if(((uTotBitOffset + uValNumBits) > pBField->bitCnt) || (uValNumBits > 64))
	{
		return CL_BITFIELD_BAD_BIT_PARAMS;
	}

	// Get the value as a U64.
	uValue = CL_BFGetValue(pBField, uTotBitOffset, uValNumBits);

	// Set pValue.
	*pValue = (U64) uValue;

	return RXN_SUCCESS;
}

///////////////////////
// Utility Functions //
///////////////////////

U08 CL_BFAddValue(CL_BITFIELD_t* pBField, U64 uValue, U08 uValueSize, U08 uStartBit, U08 uValNumBits)
{
  int x, y = 0;
  U16 uBitsRequired = 0;
  U16 uBytesRequired = 0;

	// Determine the total number of bits that will be required to support
	// the added value.
	uBitsRequired = pBField->bitCnt + uValNumBits;

	// Determine the total number of bytes that will be required.
	uBytesRequired = (uBitsRequired + 7) / 8;

	// Return an error if there is inadequate space.
	if(uBytesRequired > pBField->byteSize)
	{
		return CL_BITFIELD_NO_SPACE;
	}

	// Loop through bits individually adding them (MSbit first).
	for(x = (uValueSize - uStartBit - uValNumBits), y = 0; y < uValNumBits; x++, y++)
	{
		CL_BFAddBit(pBField, (U08) (uValue >> ((uValueSize - 1) - x)));
	}

	return RXN_SUCCESS;
}

void CL_BFAddBit(CL_BITFIELD_t* pBField, U08 uBitVal)
{
	// Set the bit.
	CL_BFSetBit(pBField, (uBitVal & 0x01), pBField->bitCnt);

	// Increase the bit count and reset the byte count accordingly.
	pBField->bitCnt++;
	if(pBField->bitCnt > 0)
	{
		pBField->byteCnt = (pBField->bitCnt + 7) / 8;
	}
	else
	{
		pBField->byteCnt = 0;
	}

	return;
}

void CL_BFSetValue(CL_BITFIELD_t* pBField, U64 uValue, U08 uValueSize, U32 uTotBitOffset, U08 uStartBit, U08 uValNumBits)
{
  int x, y = 0;

	// Loop through bits individually setting them (MSbit first).
	for(x = (uValueSize - uStartBit - uValNumBits), y = 0; y < uValNumBits; x++, y++)
	{
		CL_BFSetBit(pBField, (U08) (uValue >> ((uValueSize - 1) - x)), uTotBitOffset + y);
	}

	return;
}

void CL_BFSetBit(CL_BITFIELD_t* pBField, U08 uBitVal, U32 uTotIncBitOffset)
{
  U16 uCurByteOffset = 0;
  U08 uCurByteBitOffset = 0;

	// Get the current byte offset (account for added bit).
	uCurByteOffset = uTotIncBitOffset / 8;

	// Get the current offset within this byte.
	uCurByteBitOffset = uTotIncBitOffset % 8;

	// Different logic is required to set or clear.
	if((uBitVal & 0x01) > 0)
	{
		// Set the bit.
		pBField->buf[uCurByteOffset] |= (1 << (7 - uCurByteBitOffset));
	}
	else
	{
		// Clear the bit (~ operator will get the one's complement).
		pBField->buf[uCurByteOffset] &= ~(1 << (7 - uCurByteBitOffset));
	}

	return;
}

U64 CL_BFGetValue(CL_BITFIELD_t* pBField, U32 uTotBitOffset, U08 uValNumBits)
{
	// Loop through bits and build a U64.
	U64 uTotValue = 0;
	U64 uBitValue = 0;
  int x, y = 0;

	for(x = (uValNumBits - 1), y = 0; x >= 0; x--, y++)
	{
		// Get the current bit value.
		uBitValue = CL_BFGetBit(pBField, uTotBitOffset + y);

		// Add it to the total value.
		uTotValue |= (uBitValue << x);
	}
	
	return uTotValue;
}

U08 CL_BFGetBit(CL_BITFIELD_t* pBField, U32 uTotIncBitOffset)
{
  U16 uCurByteOffset = 0;
  U08 uCurByteBitOffset = 0;
  U08 uBitVal = 0;

	// Get the current byte offset (account for added bit).
	uCurByteOffset = uTotIncBitOffset / 8;

	// Get the current offset within this byte.
	uCurByteBitOffset = uTotIncBitOffset % 8;

	// Get the bit.
	uBitVal = (pBField->buf[uCurByteOffset] >> (7 - uCurByteBitOffset) & 0x01);

	return uBitVal;
}

	
U16 CL_SwapU16(U16 uiOriginal)
{
	// Setup a swapped U16 var.
	U16 uiSwapped = 0;
	uiSwapped |= (uiOriginal & 0xff00) >> 8;
	uiSwapped |= (uiOriginal & 0x00ff) << 8;

	return uiSwapped;
}

S16 CL_SwapS16(S16 iOriginal)
{
	// Setup a swapped U16 var.
	S16 iSwapped = 0;
	iSwapped |= (iOriginal & 0xff00) >> 8;
	iSwapped |= (iOriginal & 0x00ff) << 8;

	return iSwapped;
}

U32 CL_SwapU32(U32 uiOriginal)
{
	// Setup a swapped U32 var.
	U32 uiSwapped = 0;
	uiSwapped |= (uiOriginal & 0xff000000) >> 24;
	uiSwapped |= (uiOriginal & 0x00ff0000) >> 8;
	uiSwapped |= (uiOriginal & 0x0000ff00) << 8;
	uiSwapped |= (uiOriginal & 0x000000ff) << 24;

	return uiSwapped;
}

S32 CL_SwapS32(S32 iOriginal)
{
	// Setup a swapped U32 var.
	S32 iSwapped = 0;
	iSwapped |= (iOriginal & 0xff000000) >> 24;
	iSwapped |= (iOriginal & 0x00ff0000) >> 8;
	iSwapped |= (iOriginal & 0x0000ff00) << 8;
	iSwapped |= (iOriginal & 0x000000ff) << 24;

	return iSwapped;
}

U64 CL_SwapU64(U64 uiOriginal)
{
	// Setup a swapped U64 var.
	U64 uiSwapped = 0;
	uiSwapped |= (uiOriginal & 0xff00000000000000LL) >> 56;
	uiSwapped |= (uiOriginal & 0x00ff000000000000LL) >> 40;
	uiSwapped |= (uiOriginal & 0x0000ff0000000000LL) >> 24;
	uiSwapped |= (uiOriginal & 0x000000ff00000000LL) >> 8;
	uiSwapped |= (uiOriginal & 0x00000000ff000000LL) << 8;
	uiSwapped |= (uiOriginal & 0x0000000000ff0000LL) << 24;
	uiSwapped |= (uiOriginal & 0x000000000000ff00LL) << 40;
	uiSwapped |= (uiOriginal & 0x00000000000000ffLL) << 56;

	return uiSwapped;
}

S64 CL_SwapS64(S64 iOriginal)
{
	// Setup a swapped S64 var.
	S64 iSwapped = 0;
	iSwapped |= (iOriginal & 0xff00000000000000LL) >> 56;
	iSwapped |= (iOriginal & 0x00ff000000000000LL) >> 40;
	iSwapped |= (iOriginal & 0x0000ff0000000000LL) >> 24;
	iSwapped |= (iOriginal & 0x000000ff00000000LL) >> 8;
	iSwapped |= (iOriginal & 0x00000000ff000000LL) << 8;
	iSwapped |= (iOriginal & 0x0000000000ff0000LL) << 24;
	iSwapped |= (iOriginal & 0x000000000000ff00LL) << 40;
	iSwapped |= (iOriginal & 0x00000000000000ffLL) << 56;

	return iSwapped;
}

void CL_ExtendSign_S08(S08* pValue, U08 uSignBitPos)
{
	BOOL bSignSet = FALSE;
  int x = 0;

	// Do nothing if an invalid bit position is set.
	if(uSignBitPos > 7)
	{
		return;
	}

	// Determine if the sign bit is set.
	if((*pValue >> uSignBitPos) > 0)
	{
		bSignSet = TRUE;
	}

	// Set or clear bits at positions more sig than uSignBitPos.
	for(x = uSignBitPos + 1; x <= 7; x++)
	{
		if(bSignSet)
		{
			// Set the bit at position x.
			*pValue |= 0x1 << x;
		}
		else
		{
			// Clear the bit at position x using the ones-comp
			// of the rotation of a set bit.
			*pValue &= ~(0x1 << x);
		}
	}

	return;
}

void CL_ExtendSign_S16(S16* pValue, U08 uSignBitPos)
{
	BOOL bSignSet = FALSE;
  int x = 0;

	// Do nothing if an invalid bit position is set.
	if(uSignBitPos > 15)
	{
		return;
	}

	// Determine if the sign bit is set.
	if((*pValue >> uSignBitPos) > 0)
	{
		bSignSet = TRUE;
	}

	// Set or clear bits at positions more sig than uSignBitPos.
	for(x = uSignBitPos + 1; x <= 15; x++)
	{
		if(bSignSet)
		{
			// Set the bit at position x.
			*pValue |= 0x1 << x;
		}
		else
		{
			// Clear the bit at position x using the ones-comp
			// of the rotation of a set bit.
			*pValue &= ~(0x1 << x);
		}
	}

	return;
}

void CL_ExtendSign_S32(S32* pValue, U08 uSignBitPos)
{
	BOOL bSignSet = FALSE;
  int x = 0;

	// Do nothing if an invalid bit position is set.
	if(uSignBitPos > 31)
	{
		return;
	}

	// Determine if the sign bit is set.
	if((*pValue >> uSignBitPos) > 0)
	{
		bSignSet = TRUE;
	}

	// Set or clear bits at positions more sig than uSignBitPos.
	for(x = uSignBitPos + 1; x <= 31; x++)
	{
		if(bSignSet)
		{
			// Set the bit at position x.
			*pValue |= 0x1 << x;
		}
		else
		{
			// Clear the bit at position x using the ones-comp
			// of the rotation of a set bit.
			*pValue &= ~(0x1 << x);
		}
	}

	return;
}
