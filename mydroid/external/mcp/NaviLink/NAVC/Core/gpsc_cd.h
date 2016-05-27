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
 * FileName			:	gpsc_cd.h
 *
 * Description     	:
 * This header file contains definitions and APIs to calculate constellation data.
 *
 *
 * Author         	: 	Sharath Srinivsan - sharath@ti.com
 *
 *
 ******************************************************************************
 */
#ifndef GPSC_CD_H
#define GPSC_CD_H
/** Defines **/
#define WEEK			604800
#define HALFWEEK		302400
#define OMEGA_DOT_E		7.2921151467E-5
#define MU				3.986005E14

#define WGS84_A		6378137.000		/* Semi-major axis (m). (to equator)*/
#define WGS84_B		6356752.314		/* Semi-minor axis (m). (to poles)  */


/* Earth's obliquity and flatness terms. */
#define WGS84_F		(WGS84_A - WGS84_B) / WGS84_A
#define WGS84_1MF	(1.0 - WGS84_F)
#define WGS84_R2	((WGS84_B * WGS84_B) / (WGS84_A * WGS84_A))
#define WGS84_E2	(1.0 - (WGS84_B * WGS84_B) / (WGS84_A * WGS84_A))

#define		TWO_TO_4		16.0
#define		TWO_TO_5		32.0
#define		TWO_TO_11		2048.0
#define		TWO_TO_12		4096.0
#define		TWO_TO_14		16384.0
#define		TWO_TO_16		65536.0
#define		TWO_TO_19		524288.0
#define		TWO_TO_20		1048576.0
#define		TWO_TO_21		2097152.0
#define		TWO_TO_23		8388608.0
#define		TWO_TO_24		16777216.0
#define		TWO_TO_27		134217728.0
#define		TWO_TO_29		536870912.0
#define		TWO_TO_30		1073741824.0
#define		TWO_TO_31		2147483648.0
#define		TWO_TO_33		8589934592.0
#define		TWO_TO_38		274877906944.0
#define		TWO_TO_41		2199023255552.0
#define		TWO_TO_43		8796093022208.0
#define		TWO_TO_50		1.12589990684E+15
#define		TWO_TO_55		3.602879701896E+16

/*structurs*/
typedef struct 
{
	McpU8			u_FlagSF1;	
	McpU8			u_Prn;		
	McpU8			u_Valid;	
	McpU8			u_Health;	
	McpU8			u_CodeL2;	
	McpU8			u_L2Pdata;	
	McpU8			u_Ura;		
	McpU8			u_Iode;		
	McpU8			u_Fti;		
	McpU16			w_Iodc;		
	McpDBL			d_M0;		
	McpDBL			d_Omega0;	
	McpDBL			d_I0;		
	McpDBL			d_Omega;	
	McpDBL			d_OmegaDot;	
	McpDBL			d_SqrtA;	
	McpDBL			d_Crs;		
	McpDBL			d_DeltaN;	
	McpDBL			d_Cuc;		
	McpDBL			d_E;		
	McpDBL			d_Cus;		
	McpDBL			d_Cic;		
	McpDBL			d_Cis;		
	McpDBL			d_Crc;		
	McpDBL			d_Idot;		
	McpDBL			d_Toe;		
} cd_Ephemeris;

typedef struct
{
  U8  d_Prn;          /* 6 bits  */
  U8  d_Health;        /* 8 bits  */
  U8  d_Toa;          /* 8 bits  */
  U16  d_E;          /* 16 bits */
  U16  d_DeltaI;        /* 16 bits */
  U16  d_OmegaDot;        /* 16 bits */
  U32  d_SqrtA;        /* 24 bits */
  U32  d_OmegaZero;      /* 24 bits */
  U32  d_Omega;        /* 24 bits */
  U32 d_MZero;        /* 24 bits */
  U16  d_Af0;          /* 11 bits */
  U16  d_Af1;          /* 11 bits */

  U16  d_Week;          /* 10 bits */
  DBL d_FullToa;        /* Cumulative GPS time of the TOA. */
  DBL d_UpdateTime;      /* Cumulative GPS time of the update. */
} cd_Almanac;
/** API Functions **/


/******************************************************************************
 * Function description:
 *  gpsc_update_sv_visibility() calculates the direction for all 
 *  available satellite Ephemeris.
 *
 *
 * Parameters: 	p_zGPSCControl		- Gpsc control structure.
 *
 * Return value: 	Return TRUE if SVs are avialable, FALSE otherwise.
 *
 ******************************************************************************
*/
McpBool gpsc_update_sv_visibility( gpsc_ctrl_type*     p_zGPSCControl);
McpBool gpsc_update_sv_visibility_Almanac( gpsc_ctrl_type*     p_zGPSCControl);
#endif
