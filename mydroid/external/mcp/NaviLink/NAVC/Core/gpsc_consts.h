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
 * FileName			:	gpsc_consts.c
 *
 * Description     	:
 * This file contains system wide constants. It should not be used to
 * store constants that pertain only to the application.
 *
 *
 * Author         	: 	Gaurav Arora - gaurav@ti.com
 *
 *
 ******************************************************************************
 */
#ifndef _GPSC_CONSTS_H
#define _GPSC_CONSTS_H

/*
 * Constant definitions
*/
#define  N_SV           32    /* Number of available GPS satellites */

#ifdef GPS5300
#define  N_CHAN         12    /* Number of hardware channels in the receiver */
#define  N_LCHAN        17    /* Number of logical channels in the receiver */
#else
#define  N_CHAN          8    /* Number of hardware channels in the receiver */
#define  N_LCHAN        13    /* Number of logical channels in the receiver */
#endif

#ifndef NULL
#define NULL    ((void *)0)
#endif


#ifndef FALSE
#define FALSE               0
#endif

#ifndef TRUE
#define TRUE                1
#endif


#ifdef ON
#undef ON
#endif

#ifdef OFF
#undef OFF
#endif

#define OFF             FALSE
#define ON              TRUE

#define DISABLE         FALSE
#define ENABLE          TRUE

#define WEEK_HRS	(7U*24U)
#define WEEK_SECS       (WEEK_HRS*3600U)        /* # of secs in week */
#define WEEK_MSECS      (WEEK_SECS*1000U)     /* # of msecs in week */
#define WEEK_BITS       (WEEK_SECS*50U)       /* # of bits in week */
#define WEEK_ZCOUNTS    (WEEK_SECS/6U)        /* # of zcounts in week */
#define DAY_SEC			(24*3600)			  /* # seconds in a day */
#define DAY_MSECS       (DAY_SEC*1000U)      /* # of msecs in a day */

#define C_MIN_GPS_WEEK  1054                  /* Minimum GPS week number */

/* Speed of light from ICD */
#define LIGHT_SEC       (299792458.0)         /* m/s */
#define LIGHT_MSEC      (299792458.0/1000.0)  /* m/s */

#define C_PI            3.1415926535898       /* From ICD */

#define C_MAX_CLK_FREQ_UNC  (10.0e-6*LIGHT_SEC)                  /* == 10 ppm */
#define C_MAX_CLK_TIME_UNC  (100.0*365.0*24.0*60.0*60.0*1000.0)  /* 100 years */
#define C_MAX_SAT_TIME_UNC  (100.0*365.0*24.0*60.0*60.0*1000.0)  /* 100 years */

#define C_MAX_SAT_SPEED_UNC (900.0)  /* Line of sight, 1 sided m/sec */

/* GPS Signal Conversions */

#define CA_CHIPS_MSEC  1023  /* # of C/A chips per msec */
#define CA_FREQ        (1000.0 * CA_CHIPS_MSEC)
#define L1_FREQ        (1540.0 * CA_FREQ)


#define CA_WAVELENGTH  (LIGHT_SEC / CA_FREQ)

#define L1_WAVELENGTH  (LIGHT_SEC / L1_FREQ)


/* Meters/Sec to L1 Hz and vice versa */

#define C_MsToL1Hz     (1.0/L1_WAVELENGTH)
#define C_L1HzToMs     (L1_WAVELENGTH)


#endif /* _GPSC_CONSTS_H */
