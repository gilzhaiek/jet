/*
 * $Header: /FoxProjects/FoxSource/win32/LocationManager 1/10/04 7:53p Lleirer $
 ******************************************************************************
 *  Copyright (C) 1999 SnapTrack, Inc.

 *

 *                  SnapTrack, Inc.

 *                  4040 Moorpark Ave, Suite 250

 *                  San Jose, CA  95117

 *

 *     This program is confidential and a trade secret of SnapTrack, Inc. The

 * receipt or possession of this program does not convey any rights to

 * reproduce or disclose its contents or to manufacture, use or sell anything

 * that this program describes in whole or in part, without the express written

 * consent of SnapTrack, Inc.  The recipient and/or possessor of this program

 * shall not reproduce or adapt or disclose or use this program except as

 * expressly allowed by a written authorization from SnapTrack, Inc.

 *

 *

 ******************************************************************************/


 /*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*


   L O C A T I O N   S E R V I C E S   M A N A G E R   M O D U L E


  Copyright (c) 2002 by QUALCOMM INCORPORATED. All Rights Reserved.

 

 Export of this technology or software is regulated by the U.S. Government.

 Diversion contrary to U.S. law prohibited.

 *====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*/


/*********************************************************************************

                                   TI GPS Confidential

*********************************************************************************/
/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName			:	gpsc_utils.h
 *
 * Description     	:
 * This file contains utility functions used by the GPSC
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#ifndef GPSC_UTILS_H
#define GPSC_UTILS_H


#define C_2_POWER_32    (DBL)4294967296
#define C_POSITIVE_LIMIT_S32  (DBL)2147483647.0
#define C_NEGATIVE_LIMIT_S32    (DBL)( 0- (DBL)2147483648.0)

/*===========================================================================

FUNCTION
  NBytesPut

DESCRIPTION
  NBytesPut adds up to 4 bytes of data to a U8 array in an endian
  independent manner. The bytes are added in LS byte to MS byte order.
  The address of the next free byte in the array is returned.

PARAMETERS
  q_Word - Up to 4 bytes of source data
  p_B    - Points to append point of the U8 array
  q_N    - Number of bytes to take from q_Word

RETURN VALUES
  p_NextAppend - Value of the append point upon function completion

===========================================================================*/
U8 *NBytesPut( U32 q_Word, U8 *p_B, U32 q_N );


/*===========================================================================

FUNCTION
  NBytesPut_BE

DESCRIPTION
  NBytesPut adds up to 4 bytes of data to a U8 array. The bytes are added in
  MS byte to LS byte order.	The address of the next free byte in the array is
  returned.

PARAMETERS
  q_Word - Up to 4 bytes of source data
  p_B    - Points to append point of the U8 array
  q_N    - Number of bytes to take from q_Word

RETURN VALUES
  p_NextAppend - Value of the append point upon function completion

===========================================================================*/
U8* NBytesPut_BE (U32  q_Word, U8*  p_B, U32  q_N);



/*===========================================================================

FUNCTION
  NBytesGet

DESCRIPTION
  NBytesGet will construct a U32 value from up to 4 bytes from an input
  U8 array.  The bytes are read from LS byte to MS byte order.

PARAMETERS
  p_B - Pointer to a data array pointer. This allows the input pointer
        to be updated.
  q_N - Number of bytes to read in from U8 array.

RETURN VALUES
  q_Word - U32 constructed from input array; data array pointer is
           updated to point to first byte of next field.

===========================================================================*/

U32 NBytesGet (U8 **p_B,U8 u_NumBytes);


/*===========================================================================

FUNCTION
  NBytesGet_BE

DESCRIPTION
  NBytesGet will construct a U32 value from up to 4 bytes from an input
  U8 array.  The bytes are read from MSB byte to LSB byte order: big endian

PARAMETERS
  p_B - Pointer to a data array pointer. This allows the input pointer
        to be updated.
  q_N - Number of bytes to read in from U8 array.

RETURN VALUES
  q_Word - U32 constructed from input array; data array pointer is
           updated to point to first byte of next field.

===========================================================================*/

U32 NBytesGet_BE (U8**  p_B, U8 u_NumBytes );


/******************************************************************************
 * FullGpsMs()
 *
 * Function description:
 *
 *  Simple helper function to convert weeks/msecs into the number
 *  of msecs since GPS clock was started. This is used to store the
 *  RTC structure.
 *
 * Parameters:
 *
 *  w_WeekNum - Number of weeks since GPS clock started
 *
 *  q_Ms - Number of msecs into the current week
 *
 * Return value:
 *
 *  Time is msecs since the GPS clock was started
 *
 ******************************************************************************
*/

DBL FullGpsMs( U16 w_WeekNum, U32 q_Ms );

/******************************************************************************
 * Function description:
 *  MemDiff() determines if two data sets are equal, or not.
 *
 * Parameters:      p_1       - Pointer to first data set.
 *                  p_2      - Pointer to second data set.
 *                  w_Length - Length of data to test.
 *
 * Return value:   TRUE if different, else FALSE
 *
 ******************************************************************************
*/
U32  MemDiff( U8 *p_1, U8 *p_2 , U16 w_Length );


U32  GpscMemCpy( U8* pDest, U8* pSrc, U16 w_Length );
/*
 ******************************************************************************
 * S32Convert
 *
 * Function description:
 *
 *  This function takes an unsigned 32-bit value and converts it to a 32-bit
 *  sign extended value.
 *
 * Parameters:
 *
 *  q_Word - U32 to be converted
 *  u_MsbBitPos - Position of Most Significant Bit of input
 *
 * Return value:
 *
 *  l_Word - Signed 32-bit value
 *
 ******************************************************************************
*/

S32 S32Convert (U32 q_Word, U8 u_MsbBitPos);


/*
 ******************************************************************************
 * GpsMsecWeekLimit
 *
 * Function description:
 *
 *   me_GpsMsecWeekLimit is a helper function used to perform the
 *  fairly common check to see if a msec is indeed with the allowable
 *  range of 0 thru WEEK_MSECS-1. Only values within a single week
 *  are entertained. (ie- Multiple week adjustments are not performed
 *
 *  Assigning a NULL pointer to p_GpsWeek will disable the week
 *  adjustment logic ... ie - The p_GpsMsec will only be affected.
 *
 * Parameters:
 *
 *  p_GpsMsecs - Pointer to the msec value under test
 *  p_GpsWeek - Pointer to week number which may be adjusted
 *
 * Return value:
 *
 *  void
 *
 ******************************************************************************
*/

void GpsMsecWeekLimit( S32 *p_GpsMsecs, U16 *p_GpsWeek );

/*
 ******************************************************************************
 * gpsc_util_time_offset
 *
 * Function description: adds an arbitary amount of offset to the GPS time
 * expression, adjust for possible week roll over.
 *
 * Parameters:
 *
 *  p_zMeTime: pointer to a structure of me_Time holding the original GPS
 *             time, and is the destination for the resulting adjusted GPS
 *             time.
 *
 *  d_OffsetTimeMsec: amount of msec. to be added from the original time.
 *
 * Return value:
 *
 *  TRUE: adjustment done.
 *  FALSE: adjustment not done.
 *
 ******************************************************************************
*/

U8 gpsc_util_time_offset (me_Time *p_zMeTime, DBL d_OffsetTimeMsec);


/*
 ******************************************************************************
 * FCountToGpsTime
 *
 * Function description:
 *
 *   With a given Sensor FCOUNT, get the GPS time based on the time in the
 *  basebase, which is referenced to an FCOUNT.
 *
 * Parameters:
 *
 *  q_FCount - Sensor FCOUNT at which GPS time is sought
 *  q_FCountRef - Sensor FCOUNT at which GPS time is known and given in p_meTime
 *  p_meTime - pointer to the structure holding GPS time at q_FCountClockDB,
 *             also, the result GPS time at q_FCount will be written here.
 *
 * Return value:
 *
 *  None
 *
 ******************************************************************************
*/

void FCountToGpsTime( U32 q_FCount, U32 q_FCountRef, me_Time *p_meTime);

/*
 *****************************************************************************
 * EncodeM11E5U16
 *
 * Function description:
 *
 *   EncodeM11E5U16 converts a FLT to a U16 floating point representation
 *  that consists of a 11 bit unsigned mantissa (Bits 15 .. 5 of output)
 *  and a 5 bit exponent (Bits 4 .. 0 of output).
 *
 *  The max / min ranges are checked to see if the number is representable.
 *  Appropriate clipping is used.
 *
 *  If the number is representable it is split into mantissa and exponent
 *  portions and adjusted to suit the output range.
 *
 *  To reconstitute use * ldexp( (DBL) w_Mantissa, u_Exponent );
 *
 * Parameters:
 *
 *    f_Flt - Number to be converted
 *
 * Return value: none
 *
 *    w_Result -  Bits (15 .. 5)  : Unsigned mantissa
 *          Bits (4 .. 0)  : Unsigned exponent
 *
 ******************************************************************************
*/

U16 EncodeM11E5U16( FLT f_Flt );
FLT DecodeM11E5U16( U16 u_M11E5U16 );


/*
 *****************************************************************************
 * EncodeM11E5S16
 *
 * Function description:
 *
 *   EncodeM11E5S16 converts a FLT to a S16 floating point representation
 *  that consists of a 11 bit signed mantissa (Bits 15 .. 5 of output)
 *  and a 5 bit exponent (Bits 4 .. 0 of output).
 *
 *  The max / min ranges are checked to see if the number is representable.
 *  Appropriate clipping is used.
 *
 *  If the number is representable it is split into mantissa and exponent
 *  portions and adjusted to suit the output range.
 *
 *  To reconstitute use * ldexp( (DBL) x_Mantissa, u_Exponent );
 *
 * Parameters:
 *
 *    f_Flt - Number to be converted
 *
 * Return value: none
 *
 *    w_Result -  Bits (15 .. 5)  : Signed mantissa
 *          Bits (4 .. 0)  : Unsigned exponent
 *
 ******************************************************************************
*/

S16 EncodeM11E5S16( FLT f_Flt );
FLT DecodeM11E5S16( S16 s_M11E5U16 );
/*
 ******************************************************************************
 * function description:
 *  minXMinusY() calculates the correction C, where X+C=Y, such that \X - Y\
 *  is minimized in the sense of being withing mod R limit, when X, (Y mod R),
 *  and R are given.
 *
 * parameters:
 *    X, (Y % R), R
 *
 * return value:
 *    C
 *
 ******************************************************************************
*/

S32 minXMinusY(U32 q_X, U32 q_YModR, S32 l_R);

/*
 ******************************************************************************
 * sv_time_delta
 *
 * Function description:
 *
 *  sv_time_delta is a helper function used to apply an arbritrary time delta
 *  to a me_SvTime structure. To accomodate large time deltas without loss of
 *  precision DBL arithmetic in used. The ability to operate only on the
 *  sub millisecond portion of the time is afforded by the u_EvalMs flag.
 *
 * Parameters:
 *
 *  p_SvTime - pointer to the me_SvTime structure that is to be adjusted
 *  d_TimeDelta - Value of the time delta [msecs]
 *  u_EvalMs - TRUE, forces the msec time delta to be evaluated
 *
 * Return value:
 *
 *  void
 *
 ******************************************************************************
*/

void sv_time_delta( me_SvTime *p_SvTime, DBL d_TimeDelta, U8 u_EvalMs );


/*
 ******************************************************************************
 * static void gpsc_StringToBcd()
 *
 * Function description:
 *
 * EncodeASCIIZ string into a BCD Packed Nibble sequence.
 * The LSByte of the ASCIIZ string is encoded into the MSNibble of the
 * BCD buffer.
 * ASCIIZ bytes of increasing significance are packed into BCD Nibbles
 * of consecutively decreasing significance.
 *
 * Parameters:
 *
 *  ps_bcd              - destination buffer for packed BCD nibbles.
 *  string              - source buffer of ASCIIZ bytes.
 *  u_MaxStringDigits   - maximum length of ASCIIZ string.
 *
 * Return value:
 *
 *  void
 *
 ******************************************************************************
*/


void gpsc_StringToBcd(S8 * ps_bcd, S8 *string, U16 u_MaxStringDigits);


/*
 ******************************************************************************
 * static void gpsc_BcdToString()
 *
 * Function description:
 *
 * Decode Packed BCD sequences into ASCIIZ Byte sequences.
 * The ASCIIZ Byte unpacked from the MSNibble of the BCD buffer
 * is decoded into the LSByte of the ASCIIZ buffer.
 * As BCD Nibbles are decoded from MSNibble toward LSNIbble,
 * ASCIIZ Bytes are unpacked into consecutively higher byte
 * positions in the ASCIIZ buffer.
 *
 * Parameters:
 *
 *  ps_string           - pointer to a byte buffer with
 *                        minimum length s_MaxBcdDigits*2+1
 *  s_MaxStringDigits   - length of buffer *ps_string
 *  ps_bcd              - pointer to buffer bearing packed bcd
 *  s_MaxBcdDigits      - length of BCD buffer. If the BCD string
 *                        is shorter then the buffer can hold, pad
 *                        the buffer with nibbles of value 0xf.
 *
 *
 * Return value:
 *
 *  void
 *
 ******************************************************************************
*/


void gpsc_BcdToString(S8 * ps_string, S16 s_MaxStringDigits, S8 * ps_bcd, S16 s_MaxBcdDigits);

/*
 ******************************************************************************
 * gpsc_GpsWeekExtend
 *
 * Function description:
 *
 *   me_GpsWeekExtend is a helper function used to locate a propsective
 *  week number (10 bits) in the valid week number range. In essence this
 *  handles all of the week number rollover effects within the receiver.
 *
 *  The return value from this function is constrained to be in the range
 *  of C_MIN_GPS_WEEK through (C_MIN_GPS_WEEK+1023)
 *
 * Parameters:
 *
 *  w_GpsWeek
 *
 * Return value:
 *
 *  Bounded w_GpsWeek
 *
 ******************************************************************************
*/

U16 gpsc_GpsWeekExtend( U16 w_GpsWeek );
U8 CalculateCheckSum(U8 *p_uBuffer, U32 q_length);

U32 GetCETimeInSecs(void);

U32 GetLmTickCount(void);

#endif /* _GPSC_UTILS_H */
