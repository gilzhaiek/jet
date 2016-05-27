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
 */

#ifndef RXN_CL_DATA_H
#define RXN_CL_DATA_H

#ifdef __cplusplus
extern "C" {
#endif

/* Define Error Codes */
#define CL_BITFIELD_NO_SPACE			    10
#define CL_BITFIELD_BAD_BIT_PARAMS		11
#define CL_BITFIELD_BAD_BYTE_PARAMS	  12

/* Struct defining bitfield data. */
typedef struct CL_BITFIELD
{
	U08* buf;	      /*!< Buf pointer. */
	U16 byteSize;	  /*!< Buf capacity in bytes. */
	U16 bitCnt;	    /*!< Number of bits in buf. */
  U16 byteCnt;    /*!< Number of bytes in buf. */
} CL_BITFIELD_t;

U08 CL_BFAdd_S08(CL_BITFIELD_t* pBField, S08 Value, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFAdd_U08(CL_BITFIELD_t* pBField, U08 uValue, U08 uValNumBits, U08 uStartBit);

U08 CL_BFAdd_S16(CL_BITFIELD_t* pBField, S16 Value, U08 uValNumBits, U08 uStartBit);

U08 CL_BFAdd_U16(CL_BITFIELD_t* pBField, U16 uValue, U08 uValNumBits, U08 uStartBit);

U08 CL_BFAdd_S32(CL_BITFIELD_t* pBField, S32 Value, U08 uValNumBits, U08 uStartBit);

U08 CL_BFAdd_U32(CL_BITFIELD_t* pBField, U32 uValue, U08 uValNumBits, U08 uStartBit);

U08 CL_BFAdd_S64(CL_BITFIELD_t* pBField, S64 Value, U08 uValNumBits, U08 uStartBit);

U08 CL_BFAdd(CL_BITFIELD_t* pBField, U64 uValue, U08 uValNumBits, U08 uStartBit);

U08 CL_BFAddOnes(CL_BITFIELD_t* pBField, U16 uNumBits);

U08 CL_BFAddZeros(CL_BITFIELD_t* pBField, U16 uNumBits);

U08 CL_BFSetByByte_S08(CL_BITFIELD_t* pBField, S08 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByBit_S08(CL_BITFIELD_t* pBField, S08 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByByte_U08(CL_BITFIELD_t* pBField, U08 uValue, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByBit_U08(CL_BITFIELD_t* pBField, U08 uValue, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByByte_S16(CL_BITFIELD_t* pBField, S16 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByBit_S16(CL_BITFIELD_t* pBField, S16 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByByte_U16(CL_BITFIELD_t* pBField, U16 uValue, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByBit_U16(CL_BITFIELD_t* pBField, U16 uValue, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByByte_S32(CL_BITFIELD_t* pBField, S32 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByBit_S32(CL_BITFIELD_t* pBField, S32 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByByte_U32(CL_BITFIELD_t* pBField, U32 uValue, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByBit_U32(CL_BITFIELD_t* pBField, U32 uValue, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByByte_S64(CL_BITFIELD_t* pBField, S64 Value, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByBit_S64(CL_BITFIELD_t* pBField, S64 Value, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByByte_U64(CL_BITFIELD_t* pBField, U64 uValue, U16 uByteOffset, U08 uByteBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFSetByBit_U64(CL_BITFIELD_t* pBField, U64 uValue, U32 uTotBitOffset, U08 uValNumBits, U08 uValStartBit);

U08 CL_BFGetByByte_S08(CL_BITFIELD_t* pBField, S08* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits);

U08 CL_BFGetByBit_S08(CL_BITFIELD_t* pBField, S08* pValue, U16 uTotBitOffset, U08 uValNumBits);

U08 CL_BFGetByByte_U08(CL_BITFIELD_t* pBField, U08* puValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits);

U08 CL_BFGetByBit_U08(CL_BITFIELD_t* pBField, U08* puValue, U16 uTotBitOffset, U08 uValNumBits);

U08 CL_BFGetByByte_S16(CL_BITFIELD_t* pBField, S16* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits);

U08 CL_BFGetByBit_S16(CL_BITFIELD_t* pBField, S16* pValue, U16 uTotBitOffset, U08 uValNumBits);

U08 CL_BFGetByByte_U16(CL_BITFIELD_t* pBField, U16* puValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits);

U08 CL_BFGetByBit_U16(CL_BITFIELD_t* pBField, U16* puValue, U16 uTotBitOffset, U08 uValNumBits);

U08 CL_BFGetByByte_S32(CL_BITFIELD_t* pBField, S32* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits);

U08 CL_BFGetByBit_S32(CL_BITFIELD_t* pBField, S32* pValue, U16 uTotBitOffset, U08 uValNumBits);

U08 CL_BFGetByByte_U32(CL_BITFIELD_t* pBField, U32* puValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits);

U08 CL_BFGetByBit_U32(CL_BITFIELD_t* pBField, U32* puValue, U16 uTotBitOffset, U08 uValNumBits);

U08 CL_BFGetByByte_S64(CL_BITFIELD_t* pBField, S64* pValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits);

U08 CL_BFGetByBit_S64(CL_BITFIELD_t* pBField, S64* pValue, U16 uTotBitOffset, U08 uValNumBits);

U08 CL_BFGetByByte_U64(CL_BITFIELD_t* pBField, U64* puValue, U16 uByteOffset, U16 uByteBitOffset, U08 uValNumBits);

U08 CL_BFGetByBit_U64(CL_BITFIELD_t* pBField, U64* puValue, U16 uTotBitOffset, U08 uValNumBits);

///////////////////////
// Utility Functions //
///////////////////////

U08 CL_BFAddValue(CL_BITFIELD_t* pBField, U64 uValue, U08 uValueSize, U08 uStartBit, U08 uValNumBits);

void CL_BFAddBit(CL_BITFIELD_t* pBField, U08 uBitVal);

void CL_BFSetValue(CL_BITFIELD_t* pBField, U64 uValue, U08 uValueSize, U32 uTotBitOffset, U08 uStartBit, U08 uValNumBits);

void CL_BFSetBit(CL_BITFIELD_t* pBField, U08 uBitVal, U32 uTotIncBitOffset);

U64 CL_BFGetValue(CL_BITFIELD_t* pBField, U32 uTotBitOffset, U08 uValNumBits);

U08 CL_BFGetBit(CL_BITFIELD_t* pBField, U32 uTotIncBitOffset);

U16 CL_SwapU16(U16 uiOriginal);

S16 CL_SwapS16(S16 iOriginal);

U32 CL_SwapU32(U32 uiOriginal);

S32 CL_SwapS32(S32 iOriginal);

U64 CL_SwapU64(U64 uiOriginal);

S64 CL_SwapS64(S64 iOriginal);

void CL_ExtendSign_S08(S08* pValue, U08 uSignBitPos);

void CL_ExtendSign_S16(S16* pValue, U08 uSignBitPos);

void CL_ExtendSign_S32(S32* pValue, U08 uSignBitPos);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* RXN_CL_DATA_H */
