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
 * FileName			:	gpsc_utils.c
 *
 * Description     	:
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */

#include "gpsc_types.h"
#include "gpsc_sap.h"
#include "gpsc_consts.h"
#include "gpsc_me_api.h"
#include "gpsc_utils.h"
#include "gpsc_msg.h"
#include "gpsc_ext_api.h"


/*===========================================================================

                DEFINITIONS AND DECLARATIONS FOR MODULE

This section contains definitions for constants, macros, types, variables
and other items needed by this module.

===========================================================================*/




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

U8* NBytesPut (U32  q_Word, U8*  p_B, U32  q_N)
{
  for( ;q_N; q_N-- )
  {
    *p_B++ = (U8) q_Word;
    q_Word >>= 8;
  }

  return( p_B );
}


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

U8* NBytesPut_BE (U32  q_Word, U8*  p_B, U32  q_N)
{
  U8 u_byte;
  for( ;q_N; q_N-- )
  {
    u_byte = (U8) (q_Word >> ( (q_N - 1 ) * 8 ) );
    *p_B++ = u_byte;
  }

  return( p_B );
}



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

U32 NBytesGet (U8**  p_B, U8 u_NumBytes )
{
  U32  q_Word = 0;
  U8  u_ByteCount;

  if (u_NumBytes > 4)
    u_NumBytes = 4;

  /* Pre increment pointer to start off with MSB instead of LSB */
  /* This saves a multiply operation when constructing the U32 */
  *p_B += (u_NumBytes - 1);

  /* Build U32 */
  for (u_ByteCount=u_NumBytes; u_ByteCount; u_ByteCount--)
  {
    q_Word <<= 8;
    q_Word |= (U32) *((*p_B)--);
  }

  /* Set Pointer to next field */
  *p_B += u_NumBytes + 1;

  return (q_Word);
}


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

U32 NBytesGet_BE (U8**  p_B, U8 u_NumBytes )
{
  U32  q_Word = 0;
  U8  u_ByteCount;

  if (u_NumBytes > 4)
    u_NumBytes = 4;

  /* Build U32 */
  for (u_ByteCount=u_NumBytes; u_ByteCount; u_ByteCount--)
  {
    q_Word <<= 8;
    q_Word |= (U32) *((*p_B)++);
  }

  return (q_Word);
}



/******************************************************************************
 * Function description:
 *  MemDiff() determines if two data sets are equal, or not.
 *
 * Parameters:     p_1       - Pointer to first data set.
 *                  p_2      - Pointer to second data set.
 *                  w_Length - Length of data to test.
 *
 * Return value:   TRUE if different, else FALSE
 *
 ******************************************************************************
*/

U32  MemDiff( U8 *p_1, U8 *p_2 , U16 w_Length )
{
  while (w_Length--)
  {
    if (*p_1++ != *p_2++) return (TRUE);
  }

  return (FALSE);
}

/******************************************************************************
 * Function description:
 *  GpscMemCpy() - copy the same structure data.
 *
 * Parameters:     pDest       - Pointer to destination data set.
 *                  pSrc     - Pointer to source data set.
 *                  w_Length - Length of data to copy.
 *
 * Return value:   TRUE if different, else FALSE
 *
 ******************************************************************************
*/

U32  GpscMemCpy( U8 *pDest, U8 *pSrc, U16 w_Length )
{
  while (w_Length--)
  {
    *pDest++ = *pSrc++;
  }

  return (TRUE);
}


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
 * Note:
 *
 *  As MSB of w_WeekNum sometimes is used as a flag for the fine/coarse nature
 *  of time uncertainty, make sure the flag is removed here.
 *
 ******************************************************************************
*/


DBL FullGpsMs( U16 w_WeekNum, U32 q_Ms )
{
  return( (DBL) (w_WeekNum) * (DBL) WEEK_MSECS + (DBL) q_Ms );

}



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

S32 S32Convert (U32 q_Word, U8 u_MsbBitPos)
{
  S32 l_Word;

  l_Word = (S32) q_Word;
  l_Word <<= (32 - u_MsbBitPos);
  l_Word >>= (32 - u_MsbBitPos);

  return (l_Word);
}


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

void GpsMsecWeekLimit( S32 *p_GpsMsecs, U16 *p_GpsWeek )
{
  if( *p_GpsMsecs < 0 )
  {
    (*p_GpsMsecs) += WEEK_MSECS;

    if( (p_GpsWeek != NULL) && (*p_GpsWeek != C_GPS_WEEK_UNKNOWN) )
      (*p_GpsWeek) --;
  }
  else if( *p_GpsMsecs >= WEEK_MSECS )
  {
    (*p_GpsMsecs) -= WEEK_MSECS;

    if( (p_GpsWeek != NULL) && (*p_GpsWeek != C_GPS_WEEK_UNKNOWN) )
      (*p_GpsWeek) ++;
  }
}


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

void FCountToGpsTime( U32 q_FCount, U32 q_FCountRef, me_Time *p_meTime)
{

  S32 l_Msec;
  U16 w_Week;
  S32 l_DiffMsec;

  w_Week = p_meTime->w_GpsWeek;
  l_Msec = (S32)p_meTime->q_GpsMsec;

  l_DiffMsec = q_FCount - q_FCountRef;

    l_Msec = p_meTime->q_GpsMsec + l_DiffMsec;

  GpsMsecWeekLimit( &l_Msec, &w_Week );

  p_meTime->w_GpsWeek = w_Week;
  p_meTime->q_GpsMsec = (U32)l_Msec;

}



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
 *  To reconstitute use * ext_ldexp( (DBL) w_Mantissa, u_Exponent );
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

U16 EncodeM11E5U16( FLT f_Flt )
{
  U16 w_Result;

  /* The output encoding is
     (11 bit unsigned exponent) * 2 ** (5 bit mantissa) */


  /* Min check - Min number is 0 */
  if( f_Flt <= (FLT) 0.0 )
    w_Result = 0;

  /* Max check - Max number is 2047 * 2**31 */
  else if( f_Flt >= (FLT) (2047.0 * 32768.0 * 65536.0) )
    w_Result = 0xffff;

  else
  {
  U8  u_Exponent;
  U32 q_Mantissa;

    /* Number is representable in 11:5 format. Convert to a
       representable U32. Need to avoid overflow condition
       so split the number range into 2 by an appropriate prescale
       which is reflected in the initial mantissa value.

       The total range of numbers is representable in 42 bit (11+31)
       so it makes sense to split at 2**21. (This is somewhat arbitary */

    if( f_Flt < (FLT) (1 << 21) )
    {
      q_Mantissa = (U32) f_Flt;
      u_Exponent = 0;
    }
    else
    {
      q_Mantissa = (U32) (f_Flt * (FLT) (1.0/(FLT)(1L << 10)));
      u_Exponent = 10;
    }

    /* Now keep downshifting the mantissa until it falls into the
       correct output range. */

    while( q_Mantissa > 2047 )
    {
      u_Exponent++;
      q_Mantissa >>= 1;
    }

    /* By this point q_Mantissa is in the range 0 .. 2047, exponent is
       0 .. 31 */

    w_Result = (U16) ( (q_Mantissa << 5) | (u_Exponent) );
  }

  return( w_Result );
}
/*
 *****************************************************************************
 * DecodeM11E5U16
 *
 * Function description:
 *
 *   Convert M11E5U16 into a FLT.
 ******************************************************************************
*/

FLT DecodeM11E5U16(U16 w_M11E5U16)
{
  U16 w_Mantissa = (U16)(w_M11E5U16 >> 5);
  U16 w_Exponent = (U16)(w_M11E5U16 & 0x1F);
  return (FLT) ext_ldexp((DBL) w_Mantissa, w_Exponent);
}

/*
 *****************************************************************************
 * EncodeM11E5S16
 *
 * Function description:
 *
 *  EncodeM11E5S16 converts a FLT to a S16 floating point representation
 *  that consists of a 11 bit signed mantissa (Bits 15 .. 5 of output)
 *  and a 5 bit exponent (Bits 4 .. 0 of output).
 *
 *  The max / min ranges are checked to see if the number is representable.
 *  Appropriate clipping is used.
 *
 *  If the number is representable it is split into mantissa and exponent
 *  portions and adjusted to suit the output range.
 *
 *  To reconstitute use * ext_ldexp( (DBL) x_Mantissa, u_Exponent );
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

S16 EncodeM11E5S16( FLT f_Flt )
{
U8 u_SignBit, u_Exponent;
U16 w_UnsignedResult;
S16 x_Mantissa, x_Result;

  if( f_Flt < 0.0 )
  {
    f_Flt    = -f_Flt;
    u_SignBit  = 1;
  }
  else
    u_SignBit = 0;

  /* No need to replicate the code in EncodeU16 */

  w_UnsignedResult = EncodeM11E5U16( f_Flt );

  /* Need to shift in a sign bit. This will normally consist of
     a shift and an exponent increment. There are conditions where
     this cannot occur. */

  /* Reconstitute the various components */
  x_Mantissa = (S16) (w_UnsignedResult >> 5);  /* 0 thru 2047 */
  u_Exponent = (U8)(w_UnsignedResult & 0x1f);

  /* If bit 10 is already 0 (this occurs on smaller numbers) then we
     already have room for a sign bit and no shift is necessary. Under
     these conditions simply multiply sign bit straight into the
     mantissa and do not adjust the exponent. */

  if( x_Mantissa < 1024 )
  {
    if( u_SignBit )
      x_Mantissa = (S16)-x_Mantissa;
  }
  else
  {
    /* x_Mantissa is in the range 1024 thru 2047. A sign bit is created
       by shifting x_Mantissa right, however, we need to check that this
       won't overflow the exponent. */

    if( u_Exponent == 31 )
    {
      /* No room for the sign bit. Clip the output at max pos / neg
         as appropriate. */

      if( u_SignBit )
        x_Mantissa = -1024;
      else
        x_Mantissa = 1023;
    }
    else
    {
      /* We can fit the sign bit. */
      u_Exponent++;

      x_Mantissa >>= 1;

      if( u_SignBit )
        x_Mantissa = (S16)-x_Mantissa;

    }
  }

  /* Reconstitute from new components */
  x_Result = (S16)((x_Mantissa << 5) | u_Exponent);

  return( x_Result );
}

/*
 *****************************************************************************
 * DecodeM11E5S16
 *
 * Function description:
 *
 *   Convert M11E5S16 into a FLT.
 ******************************************************************************
*/

FLT DecodeM11E5S16(S16 s_M11E5S16)
{
  S16 s_Mantissa = (S16)(s_M11E5S16 >> 5);
  U16 u_Exponent = (U16)(s_M11E5S16 & 0x1F);

  return (FLT) ext_ldexp((DBL) s_Mantissa, u_Exponent);
}


/*
 ******************************************************************************
 * gpsc_GpsWeekExtend
 *
 * Function description:
 *
 *   gpsc_GpsWeekExtend is a helper function used to locate a propsective
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

U16 gpsc_GpsWeekExtend( U16 w_GpsWeek )
{

  /* Make sure that no-one passes a bogus week by mistake */
  if( w_GpsWeek != C_GPS_WEEK_UNKNOWN )
  {
    /* Only operate on the 10 lsbs */
    w_GpsWeek &= 0x3ff;

    /* Keep adding in mod 1024 rollover events until >= C_MIN_GPS_WEEK
       requirement is met */

    while( w_GpsWeek < C_MIN_GPS_WEEK )
      w_GpsWeek += 1024;
  }

  return( w_GpsWeek );

}


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

U8 gpsc_util_time_offset (me_Time *p_zMeTime, DBL d_OffsetTimeMsec)
{

  U16 w_GpsWeek = p_zMeTime->w_GpsWeek;

  DBL d_GpsMsec = (DBL)p_zMeTime->q_GpsMsec -
                (DBL)p_zMeTime->f_ClkTimeBias;

  DBL d_AdjustedGpsMsec;

  S32 l_AdjustedGpsMsec; /* ext_floor of d_AdjustedGpsMsec */

  DBL d_AdjustedGpsSubMsec; /* submsec added to l_AdjustedGpsMsec, always + */


  U8 u_ret = FALSE;


  /* Adjusted for offset, could now be negative referenced
     to the same week number */
  d_AdjustedGpsMsec = d_GpsMsec + d_OffsetTimeMsec;

  /* in order to use the GpsMsecWeekLimit function, the whole
     number part of d_AdjustedGpsMsec must be within the limit
     of S32 */

  if ( (d_AdjustedGpsMsec >= C_NEGATIVE_LIMIT_S32) &&
     (d_AdjustedGpsMsec <= C_POSITIVE_LIMIT_S32)
     )
  {

    l_AdjustedGpsMsec = (S32)ext_floor(d_AdjustedGpsMsec); /** + or - */

    /* this number always positive */
    d_AdjustedGpsSubMsec = d_AdjustedGpsMsec -
                (DBL)l_AdjustedGpsMsec;


    /* adjust for week rollover, after this l_AdjustedGpsMsec always + */
    GpsMsecWeekLimit(&l_AdjustedGpsMsec, &w_GpsWeek);


    p_zMeTime->w_GpsWeek = w_GpsWeek; /* update week number */
    p_zMeTime->q_GpsMsec = (U32)l_AdjustedGpsMsec; /* update msec */

    /* convert submsec to time bias, and adjust Msec if necessary */
    if ( d_AdjustedGpsSubMsec >= 0 )
    {
      if (d_AdjustedGpsSubMsec > 0.5)
      {

        p_zMeTime->f_ClkTimeBias  = (FLT)(1.0 - d_AdjustedGpsSubMsec);
        p_zMeTime->q_GpsMsec += 1;
      }
      else
      {
        p_zMeTime->f_ClkTimeBias  = (FLT)(0.0 - d_AdjustedGpsSubMsec);
      }
    }
    else
    {
      if ( d_AdjustedGpsSubMsec >= -0.5f )
      {
        p_zMeTime->f_ClkTimeBias  = (FLT)d_AdjustedGpsSubMsec;
      }
      else
      {
        p_zMeTime->f_ClkTimeBias = (FLT)( -1.0 - d_AdjustedGpsSubMsec );
        p_zMeTime->q_GpsMsec -= 1;
      }
    }

    u_ret = TRUE;

  }

  return u_ret;

}

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

S32 minXMinusY(U32 q_X, U32 q_YModR, S32 l_R)
{
  S32 l_C;

  l_C = q_YModR - (q_X % l_R);

  if (l_C > l_R/2) l_C -= l_R;
  else if (l_C < -l_R/2) l_C += l_R;

  return l_C;
}


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

void sv_time_delta( me_SvTime *p_SvTime, DBL d_TimeDelta, U8 u_EvalMs )
{
DBL d_TimeMs, d_IntMs;

  /*
   * Add the time delta to the sub millisecond time. Using (DBL) arithmetic
   * limits the precision error from a 1 sec time delta to nanometers. (Using
   * (FLT) here would result in a 18 meter error from a 1 sec time delta.
  */

  d_TimeMs       = (DBL) p_SvTime->f_SubMs + d_TimeDelta;

  /* Extract the integer portion */
  d_IntMs       = ext_floor( d_TimeMs );

  /* And the fractional portion */
  p_SvTime->f_SubMs  = (FLT) ( d_TimeMs - d_IntMs);

  /* Evaluate the integer portion if required, else set to 0 */
  if( u_EvalMs )
  {
    volatile S32 temp; //To avoid compiler issue*/
	temp = (S32) d_IntMs;
	p_SvTime->q_Ms  += temp;
    GpsMsecWeekLimit( (S32 *) &p_SvTime->q_Ms, NULL );
  }
  else
    p_SvTime->q_Ms = 0;
}

/***** Constants *************************/
#define C_SEC_JAN1_1970_TO_JAN6_1980   315964800

/******************************************************************************/

#define C_MAX_TIME_UNC_MS 60*1000 /* in microseconds*/
U32 GetCETimeInSecs(void)
{
  T_GPSC_time_assist z_CurrentTime;
  T_GPSC_result result;
  U32 q_CETimeInSecs;

  z_CurrentTime.gps_week = C_GPS_WEEK_UNKNOWN;
  z_CurrentTime.time_accuracy = GPSC_TIME_ACCURACY_COARSE;
  result = gpsc_time_request_tow_ind(&z_CurrentTime);
  
  if(result != GPSC_SUCCESS || z_CurrentTime.gps_week == C_GPS_WEEK_UNKNOWN ||
	  z_CurrentTime.time_unc > C_MAX_TIME_UNC_MS * 1000)
  {
	STATUSMSG("Status: No CE Time");
	return 0;
  }
  
  q_CETimeInSecs = z_CurrentTime.gps_week * WEEK_SECS + (U32)(z_CurrentTime.gps_msec/1000);
  return q_CETimeInSecs;
}



U8 CalculateCheckSum(U8 *p_uBuffer, U32 q_length)
{
	U32 q_i;
	U8 u_checksum=0;

	for(q_i= 0;q_i<q_length;q_i++)
	{
		u_checksum = (U8)(u_checksum ^ p_uBuffer[q_i]);
	}
	return u_checksum;
}

#ifdef TI_TEST
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


void gpsc_BcdToString(S8 * ps_string, S16 s_MaxStringDigits, S8 * ps_bcd, S16 s_MaxBcdDigits)
{
          S16   s_BcdIndex, s_StringIndex;

          /* Assure that twice as many string bytes, as bcd bytes are available */
          GPSC_ASSERT ((s_MaxBcdDigits <= (s_MaxStringDigits+1)/2));

          for ( s_BcdIndex=/*MAX_IMSI_DIGITS*/s_MaxStringDigits/2-1, s_StringIndex=0;
                s_BcdIndex >= 0;
                s_BcdIndex--, s_StringIndex+=2)
          {
            U8 bcd,hi,lo;

            bcd = (U8) ps_bcd[s_BcdIndex];
            hi = bcd>>4;
            lo = bcd&0xF;
            if (hi==0xf)
            {
              ps_string[s_StringIndex] = '\0';
              break;
            }
            else ps_string[s_StringIndex] = (S8) (hi + (U8)'0');

            if (lo==0xf)
            {
              ps_string[s_StringIndex+1] = '\0';
              break;
            }
            else ps_string[s_StringIndex+1] = (S8) (lo + '0');

            ps_string[s_StringIndex+2] = '\0';
          } /* End Build IMSI String for..loop */

}


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


void gpsc_StringToBcd(S8 * ps_bcd, S8 *string, U16 u_MaxStringDigits)
{
          S8  s_PaddedString[80];
          S16 s_StringIndex;
          S16 s_BcdIndex;
          U16 u_StringLength = strlen(string);
          S16 s_StringPad = u_MaxStringDigits - u_StringLength;

          GPSC_ASSERT((u_MaxStringDigits<80));
          GPSC_ASSERT(((u_MaxStringDigits & 1) == 0));

          for (s_StringIndex = 0; s_StringIndex < u_MaxStringDigits; s_StringIndex++)
          {
            s_PaddedString[s_StringIndex] = (s_StringIndex < u_StringLength) ?
              string[s_StringIndex] - '0' : 0xf;
          }

          for ( s_BcdIndex = u_MaxStringDigits/2-1, s_StringIndex=0;
                s_BcdIndex >= 0;
                s_BcdIndex--, s_StringIndex+=2)
          {
            ps_bcd[s_BcdIndex] =
              s_PaddedString[s_StringIndex]<<4 | (s_PaddedString[s_StringIndex+1]);
          }
}
#endif
