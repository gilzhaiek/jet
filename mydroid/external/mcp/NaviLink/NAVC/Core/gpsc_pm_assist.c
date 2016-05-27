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
 * FileName			:	gpsc_msg.c
 *
 * Description     	:
 *
 *  This file contains function for generating predicted bits message from the 
 *  GPS TLM/HOW words
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 ******************************************************************************/

#include "gpsc_data.h"
#include "gpsc_msg.h"

static const U8 u_ParityByte[ C_N_PARITY ]
=
{
  0x29, 0x16, 0x2a, 0x34,  /* D29*, D30*,   d1,   d2 */
  0x3b, 0x1c, 0x2f, 0x37,  /*   d3,   d4,   d5,   d6 */
  0x1a, 0x0d, 0x07, 0x23,  /*   d7,   d8,   d9,  d10 */
  0x31, 0x38, 0x3d, 0x3e,  /*  d11,  d12,  d13,  d14 */
  0x1f, 0x0e, 0x26, 0x32,  /*  d15,  d16,  d17,  d18 */
  0x19, 0x2c, 0x16, 0x0b,  /*  d19,  d20,  d21,  d22 */
  0x25, 0x13         /*  d23,  d24  */
}
;

/* This table is used to solve for d23 and d24 that will make D29 D30 zero
   (mc_pmatch.c) */
static const U8 u_Solve[4]
=
{
  0x00, 0x80, 0xC0, 0x40
}
;


/*
 * Local function prototypes
*/

static U32 GpsParitySum( U32 q_GpsWord );




/*
 * Functions.
*/

/*
 ******************************************************************************
 *  TlmHow
 *
 * Function description:
 *  gpsc_pm_assist_tlm_how generates 62 predicted GPS bits consisting of, first,
 *  two zeros that represent the terminal bits of a prior subframe, then two
 *  30-bit words that represent the TLM and HOW words for a designated subframe.
 *
 *  The predicted bits are partially based on expected values of the telemetry
 *  message, reserved bits, and flag bits.  These values are supplied,
 *  typically, by a remote server.  If expected values are not available,
 *  default values may be used, as designated in the argument structure
 *  definition.
 *
 * Parameters:
 *  p_Arg points to a structure that contains arguments.  On return, it also
 *  contains results.  The arguments consist of the expected values for
 *  telemetry, reserved, and flag fields and the designated subframe of week.
 *
 * Return value:
 *  The function has a void return, but the argument structure returns results:
 *  q_BitOfWeek designates the bit number two bits before the designated
 *  subframe begins.  In the case of subframe zero, this is a bit number in the
 *  prior week.
 *  q_PredictedBits contains the 62 predicted bits, starting with the high-order
 *  bit of the first word and continuing to bit 2 of the second word.
 *
 ******************************************************************************
*/

void gpsc_pm_assist_tlm_how( gpsc_tlm_how_type *p_Arg )
{
U32 q_Tlm, q_How, q_Parity;
U32 q_SfowInRange, q_NextSfow;
/*U32 q_TlmWord;*/

  /* Build TLM word.  Top 2 bits are final bits of prior word, zero by
     definition */
  q_Tlm = ((U32)0x8B << 22) |            /* Preamble, constant */
      ((p_Arg->w_TlmMessage & 0x3FFF) << 8) |
      ((p_Arg->u_ReservedBits & 3) << 6);
  q_Parity = GpsParitySum( q_Tlm );
  p_Arg->q_PredictedBits[0] = q_Tlm | q_Parity;
  STATUSMSG("Status: TLM Word: %lx", p_Arg->q_PredictedBits[0] );

  /* Ensure that the SFOW argument is within range.
    Prepare the HOW value that is the number of the following subframe */
  q_SfowInRange = p_Arg->q_SubFrameOfWeek;
  while( q_SfowInRange >= WEEK_ZCOUNTS )
    q_SfowInRange -= WEEK_ZCOUNTS;

  if( q_SfowInRange == (WEEK_ZCOUNTS-1) )
    q_NextSfow = 0;
  else
    q_NextSfow = q_SfowInRange + 1;

  #ifdef SMLC_OTA_BITS
  /* If SMLC version is such that it sends the AS and Alert flags from the HOW
     Word in the GPS TOW Assist message as over the air (OTA) bits (as opposed
     to decoded bits), then we first have to change them to decoded (or source) 
     bits in order to generate the parity bits. This has to be done only if the
     D30 parity bit of the TLM word is a 1. This bit currently resides in the LSB
     of q_Parity. Note that only these 2 bits in p_Arg->u_FlagBits are from the SMLC.
     The rest of the HOW word is generated locally, so they are already in source
     (decoded) format. Also note that other fields in the GPS TOW Assist message
     are parts of the TLM Word and that word does never need to be flipped because
     its preceeding D30 parity bit (the last bit of the previous subframe) is
     always 0. If/when SMLC sends decoded bits, then this step should be skipped: LM
     should be built without the SMLC_OTA_BITS preprocessor macro. */
  if (q_Parity & 1)
    p_Arg->u_FlagBits ^= 3;

  #endif

  /* Build HOW word preceeded by the low-order bits of TLM parity.  These two
    bits are pertinent for generating HOW parity.  The Sub Frame ID field
    cycles 1...5 starting with 1 in sub frame zero of the week. */
  q_How = (q_Parity << 30) |
      (q_NextSfow << 13) |
      ((p_Arg->u_FlagBits & 3) << 11) |
      (q_SfowInRange % 5 + 1) << 8;

  /* Initial version of parity bits might not be zero in D29 and D30.  Solve for
    d23 and d24 that will make D29 D30 zero. Then recompute parity */
  q_Parity = GpsParitySum( q_How );
  q_How |= u_Solve[ q_Parity & 0x3 ];
  q_Parity = GpsParitySum( q_How );

  /* Invert HOW data bits if required by last TLM parity bit. This is because the
     sensor expects over the air format, not decoded bit format! */

  if( q_How & 0x40000000 )
    q_How ^= 0x3FFFFFC0;

  /* Align HOW word to the left, that is, delete D29* and D30*. Include parity
     bits */
  p_Arg->q_PredictedBits[1] = (q_How | q_Parity) << 2;
  STATUSMSG("Status: HOW Word: %lx", p_Arg->q_PredictedBits[1] );
  /* Generate the bit of week that is two bits before the designated SFOW */
  if( q_SfowInRange == 0 )
    p_Arg->q_BitOfWeek = (WEEK_ZCOUNTS * 300 - 2);
  else
    p_Arg->q_BitOfWeek = q_SfowInRange * 300 - 2;
}


/*
 ******************************************************************************
 *  GpsParitySum
 *
 * Function description:
 *  GpsParitySum computes the 6 bits of parity associated with
 *  bits D30* thru d24.
 *
 * Parameters:
 *  q_GpsWord - This 32 bit data word is arranged in the same order as the
 *  data bits are sent from the satellite. ie :- Bits are shifted in from the
 *  left. The 6 lsbs are don't cares. The effect of D30* on d1 d2 .. d23 d24
 *  must be removed before calling this function. The D30* data flipping is
 *  performed using mc_GpsDataFlipD30X()
 *
 * Return value:
 *   q_Sum - A 32 bit word. The lower 6 bits of this word contain the parity
 *  sum. The remaining 24 bits are all 0.
 ******************************************************************************
*/

static U32 GpsParitySum( U32 q_GpsWord )
{
U32 q_Sum, q_Feed, q_I;  /* U32's for speed on the ARM */
const U8 *p_Parity;

  q_Sum = 0;
  q_Feed = q_GpsWord;
  p_Parity = &u_ParityByte[ 0 ];

  /*
  * Shift out D30* D29* .... d23 d24 and accumulate the parity effect of
  * each data bit on the resulting parity sum. The loop counts down to
  * 0 to optimize for the ARM implementation.
  */

  for( q_I = C_N_PARITY; q_I; q_I--)
  {
    if( q_Feed & 0x80000000 )
      q_Sum ^= *p_Parity;

    q_Feed <<= 1;
    p_Parity++;
  }

  return( q_Sum );
}

/* to avoid warning level 4: nonstandard extension used : translation unit is empty */
void gpsc_pm_assist(void)
{
}



