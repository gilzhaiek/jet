
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

*********************************************************************************//*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName			:	gpsc_types.h
 *
 * Description     	: 	
 * This file contains the type definitions for the data types used within
 * the Sensor software. This file may require modification as part of a host
 * plaform / compiler change to support variable length change
 *
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */
#ifndef _GPSC_TYPES_H
#define _GPSC_TYPES_H

/*
 * Constant definitions
*/
typedef unsigned char   U8;
typedef unsigned short  U16;
typedef unsigned long   U32;
typedef signed	char     S8;
typedef signed short    S16;
typedef signed long     S32;
typedef double          DBL;
typedef float           FLT;

#define MAX_U8          255
#define MIN_U8          0
#define MAX_U16         65535
#define MIN_U16         0
#define MAX_U32         4294967295
#define MIN_U32         0
#define MAX_S16         32767
#define MIN_S16         -32767
#define MAX_S32         2147483647L
#define MIN_S32         -2147483647L

/*
 * Structure definitions
*/

/* Structure used to process 32 bit complex quantities */
typedef struct
{
  S32  l_I;
  S32  l_Q;
} CplxS32;


/* COUNTERDIFF returns the difference between two Unsigned quantities.
 * In the case that the subtrahend is greater than the minuend,
 * the macro assumes that the minuend has "rolled over", and adds
 * the maximum value for the minuend type to its present value
 * before committing the difference operation.
 *
 * This is intended to help in cases where the minuend and subtrahend
 * are values for the same incrementing counter.
 */

#define MAX_U(x) ((1<<(8*sizeof(x)))-1)
#define COUNTERDIFF(x,y) ((x>y)?x-y:MAX_U(x)+x-y)

#define COUNTERDIFF32(x,y) ((x>y)?x-y:MAX_U32+x-y)
#endif /* _GPSC_TYPES_H */
