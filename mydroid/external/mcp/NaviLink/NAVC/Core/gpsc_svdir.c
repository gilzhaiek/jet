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
 * FileName		: gpsc_cd.c
 *
 * Description     	: This file contains functions to calculate constellation data.
 *
 * Author         	: Sharath Srinivasan - sharath@ti.com
 *
 *
 ******************************************************************************
 */
#include "gpsc_svdir.h"

/************************************************************************/
/*							APIs                                        */
/************************************************************************/


/************************************************************************/
/*					Internal Functions Implementation                   */
/************************************************************************/

/** 
 * \fn     gpsc_svdir_DecodeEphUnits
 * \brief  decodes ephemeris to ICD equivalent units
 * 
 * This function is used to decode the ephemeris to ICS equivalent units.
 * 
 * \note
 * \param	p_In     		- Raw Ephemeris
 * \param	p_Out     		- decoded Ephemeris
 * \return 	void
 * \sa     	gpsc_svdir_DecodeEphUnits
 */ 
void gpsc_svdir_DecodeEphUnits(pe_RawEphemeris	*p_In, cd_Ephemeris	*p_Out)
{
	p_Out->u_Prn = p_In->u_Prn;

	/*--------------------- 
	 * Decoding subframe 2.
	 *---------------------
	*/

	/* IODE, 8 bits: 61 - 68 */

	p_Out->u_Iode = p_In->u_Iode;

	/* Crs, 16 bits: 69 - 84
	 * Two's complement, meters.
	*/

	p_Out->d_Crs = (DBL) (S16) p_In->w_Crs * (1.0 / TWO_TO_5);

	/* Delta n, 16 bits: 91 - 106.
	 * Two's complement, semi-circles/sec.  Converting to radians/sec.
	*/

	p_Out->d_DeltaN = (DBL) (S16) p_In->w_DeltaN * (C_PI / TWO_TO_43);

	/* M0, 32 bits, split between 107 - 114 and 121 - 144.
	 * Two's complement, semi-circles.  Converting to radians.
	*/

	p_Out->d_M0 = (DBL) (S32) p_In->q_MZero * (C_PI / TWO_TO_31);

	/* Cuc, 16 bits: 151 - 166.
	 * Two's complement, radians.
	*/

	p_Out->d_Cuc = (DBL) (S16) p_In->w_Cuc * (1.0 / TWO_TO_29);

	/* Eccentricity - e, 32 bits: split between 167 - 174 and 181 - 204.
	 * Dimensionless.
	*/

	p_Out->d_E = (DBL) p_In->q_E * (1.0 / TWO_TO_33);

	/* Cus, 16 bits, 211 - 226.
	 * Two's complement, radians.
	*/

	p_Out->d_Cus = (DBL) (S16) p_In->w_Cus * (1.0 / TWO_TO_29);

	/* Square root of A, 32 bits: split between 227 - 234 and 241 - 264.
	 * Units of meters^(1/2).
	*/

	p_Out->d_SqrtA = (DBL) p_In->q_SqrtA * (1.0 / TWO_TO_19);

	/* Toe, 16 bits: 271 - 286.  seconds.
	*/

	p_Out->d_Toe = (DBL) p_In->w_Toe * TWO_TO_4;

	/*--------------------- 
	 * Decoding subframe 3.
	 *---------------------
	*/

	/* Cic, 16 bits: 61 - 76,
	 * Two's complement, radians.
	*/

	p_Out->d_Cic = (DBL) (S16) p_In->w_Cic * (1.0 / TWO_TO_29);

	/* Omega0, 32 bits: split between 77 - 84 and 91 - 114.
	 * Two's complement, semi-circles.  Converting to radians.
	*/

	p_Out->d_Omega0 = (DBL) (S32) p_In->q_OmegaZero * (C_PI / TWO_TO_31);

	/* Cis, 16 bits: 121 - 136.
	 * Two's complement, radians. 
	*/

	p_Out->d_Cis = (DBL) (S16) p_In->w_Cis * (1.0 / TWO_TO_29);

	/* I0, 32 bits: split between 137 - 144 and 151 - 174.
	 * Two's complement, semi-circles.	Converting to radians.
	 */

	p_Out->d_I0 = (DBL) (S32) p_In->q_IZero * (C_PI / TWO_TO_31);

	/* Crc, 16 bits: 181 - 196.
	 * Two's complement, meters.
	*/
	
	p_Out->d_Crc = (DBL) (S16) p_In->w_Crc * (1.0 / TWO_TO_5);

	/* d_Omega, 32 bits: split between 197 - 204 and 211 - 234.
	 * Two's complement, semi-circles.  Converting to radians.
	*/

	p_Out->d_Omega = (DBL) (S32) p_In->q_Omega * (C_PI / TWO_TO_31);

	/* d_Omega dot, 24 bits: 241 - 264.
	 * Two's complement, semi-circles/sec.	Converting to radians/sec.
	*/
	
	p_Out->d_OmegaDot = (DBL) ((S32) (p_In->q_OmegaDot << 8) >> 8) 
		* (C_PI / TWO_TO_43);

	/* I dot, 14 bits: 279 - 292. 
	 * Two's complement, semi-circles/sec.  Converting to radians/sec.
	*/

	p_Out->d_Idot = (DBL) ((S16) (p_In->w_IDot << 2) >> 2) 
		* (C_PI / TWO_TO_43);
}

/** 
* \fn     gpsc_cd_kepler
* \brief  Kepler's anomaly
* 
* This function used for calculation of Kepler's eccentric anomaly.
* 
* \note
* \param	*mk     		- mk
* \param	*q_E     		- eccentricity
* \param	*ek     		- anomaly
* \return 	void
* \sa     	gpsc_cd_kepler
*/ 
void gpsc_cd_kepler(McpDBL *mk, McpDBL *q_E, McpDBL *ek)
{
		/* iteration with wegstein's accelerator */
		McpDBL x;
		McpDBL y;
		McpDBL x1,y1,x2;
		int i;

		x=*mk;
		y=*mk-(x-*q_E*mcpf_Sin(x));
		x1=x;
		x=y;

		for(i=0;i<16;i++)
		{
			x2=x1;
			x1=x;
			y1=y;
			y=*mk-(x-*q_E*mcpf_Sin(x));
			if(mcpf_Fabs(y-y1)<1.0E-15) break;
			x=(x2*y-x*y1)/(y-y1);
		} /* for iterations */

		*ek=x;
}

/** 
* \fn     gpsc_cd_ephxyz
* \brief  Eph to xyz calculation
* 
* This function is used for calculation of XYZ position of an SV
* 
* \note
* \param	GpsTime     		- GPS seconds time in milliseconds
* \param	*RawEphemeris 	- Raw ephemeris of SV
* \param	*svs     			- structure to store XYZ position
* \return 	void
* \sa     	gpsc_cd_ephxyz  
*/ 
void gpsc_cd_ephxyz(McpU32 GpsTime, pe_RawEphemeris *RawEphemeris, gpsc_SV_xyz_type *svs)
{
		McpDBL n0; /* computed mean motion */
		McpDBL n; /* corrected mean motion */
		McpDBL tk; /* time from ephemeris reference epoch */
		McpDBL mk; /* mean anomaly */
		McpDBL ek; /* eccentric anomaly */
		McpDBL vk; /* true anomaly */
		McpDBL pk; /* argument of latitude */
		McpDBL sin2pk; /* sin 2*pk */
		McpDBL cos2pk; /* cos 2*pk */
		McpDBL uk; /* corrected agument of latitude */
		McpDBL duk; /* latitude correction */
		McpDBL drk; /* radius correction */
		McpDBL dik; /* inclination correction */
		McpDBL rk; /* corrected radius */
		McpDBL ik; /* corrected inclination */
		McpDBL xkp; /* x position in orbital plane */
		McpDBL ykp; /* y position in orbital plane */
		McpDBL ok; /* corrected longitude of ascending node */
		McpDBL sinok; /* sin of ok */
		McpDBL cosok; /* cos of ok */
		McpDBL ykpcosik; /* ykp * cos (ik) */
		McpDBL sinvk; /* sin (vk) */
		McpDBL cosvk; /* cos (vk) */
		McpDBL sinek; /* sin (ek) */
		McpDBL cosek; /* cos (ek) */
		cd_Ephemeris Decoded_ephemeris;
		cd_Ephemeris *ephemeris = &Decoded_ephemeris;
		
		gpsc_svdir_DecodeEphUnits(RawEphemeris, ephemeris);
		
		svs->u_Prn=ephemeris->u_Prn;
		n0=mcpf_Sqrt(MU/mcpf_Pow(ephemeris->d_SqrtA,6.0)); /* computed mean motion */
		tk=(GpsTime/1000)-  ephemeris->d_Toe; /* time from ephemeris epoch */ 
		if (tk>HALFWEEK) tk-=WEEK;
		if(tk<-HALFWEEK) tk+=WEEK;
		n=n0+ephemeris->d_DeltaN; /* corrected mean motion */
		mk=ephemeris->d_M0+n*tk; /* mean anomaly */
		gpsc_cd_kepler(&mk,&ephemeris->d_E,&ek); /* kepler's equation for eccentric anomaly */
		cosek=mcpf_Cos(ek);
		sinek=mcpf_Sin(ek);
		cosvk=(cosek-ephemeris->d_E);
		sinvk=mcpf_Sqrt(1.0-ephemeris->d_E*ephemeris->d_E)*sinek;
		vk=mcpf_Atan2(sinvk,cosvk); /* true anomaly */
		if (vk<0.0) vk+=2*C_PI;
		pk=vk+ephemeris->d_Omega; /* argument of latitude */
		sin2pk=mcpf_Sin(2.0*pk);
		cos2pk=mcpf_Cos(2.0*pk);
		duk=ephemeris->d_Cus*sin2pk+ephemeris->d_Cuc*cos2pk; /* argument of latitude correction */
		drk=ephemeris->d_Crc*cos2pk+ephemeris->d_Crs*sin2pk; /* radius correction */
		dik=ephemeris->d_Cic*cos2pk+ephemeris->d_Cis*sin2pk; /* correction to inclination */
		uk=pk+duk; /* latitude */
		rk=ephemeris->d_SqrtA*ephemeris->d_SqrtA*(1.0-ephemeris->d_E*cosek)+drk; /* corrcted radius */
		ik=ephemeris->d_I0+dik+ephemeris->d_Idot*tk; /* corrected inclination */
		xkp=rk*mcpf_Cos(uk); /* x in orbital plane */
		ykp=rk*mcpf_Sin(uk); /* y in orbital plane */
		ok=ephemeris->d_Omega0+(ephemeris->d_OmegaDot-OMEGA_DOT_E)*tk-OMEGA_DOT_E*ephemeris->d_Toe; /* longitude of ascending node */
		ykpcosik=ykp*mcpf_Cos(ik);
		sinok=mcpf_Sin(ok);
		cosok=mcpf_Cos(ok);

		/* sv xyz */
		svs->xyz_x=xkp*cosok-ykpcosik*sinok;
		svs->xyz_y=xkp*sinok+ykpcosik*cosok;
		svs->xyz_z=ykp*mcpf_Sin(ik);

	return;
}

/** 
* \fn     gpsc_cd_Ecef2Enu
* \brief  Eph to ENU calculation
* 
* This function is used  to convert an XYZ vector in ECEF coordinates to East North Up.
* The vector will typically be the difference between the satellite's position
* and the receiver's position, or the difference between the velocities for calculation 
* of XYZ position of an SV
* 
* \note
* \param	d_Xform     		- structure containing a valid transformation matrix.
* \param	d_Ecef		 	- XYZ vector in the ECEF frame
* \param	d_Enu     			- XYZ vector in the ENU frame.
* \return 	void
* \sa     	gpsc_cd_ephxyz
*/ 
void gpsc_cd_Ecef2Enu(McpDBL d_Xform[][3], const McpDBL *d_Ecef, McpDBL *d_Enu)
{
	McpU16   w_I, w_J;

	for (w_I=0; w_I < 3; w_I++)
	{
		d_Enu[w_I] = 0.0;

		for(w_J = 0; w_J < 3; w_J++)
		{
			d_Enu[w_I] += d_Xform[w_I][w_J] * d_Ecef[w_J];
		}
	}
}

/** 
* \fn     gpsc_cd_Lla2Ecef
* \brief  LLA to ECEF calculation
* 
* This function is used  to  Convert positions from WGS-84 Lat, Lon & Alt into ECEF.
* 
* \note
* \param	d_lla     		- Latitude (rad), longitude (rad) and altitude (m).
* \param	d_ecef		- XYZ position (m) in the ECEF coordinate system.
* \return 	void
* \sa     	gpsc_cd_Lla2Ecef
*/ 
void gpsc_cd_Lla2Ecef(McpDBL d_lla[], McpDBL d_ecef[])
{
	McpDBL	d_n, d_cosLat, d_sinLat, d_cosLon, d_sinLon, d_temp;

	d_cosLat = mcpf_Cos( d_lla[0] );
	d_sinLat = mcpf_Sin( d_lla[0] );
	d_cosLon = mcpf_Cos( d_lla[1] );
	d_sinLon = mcpf_Sin( d_lla[1] );

	/* Compute the radius of curvature along the prime vertical. */

	d_temp = (WGS84_B / WGS84_A) * d_sinLat;

	d_n = WGS84_A / mcpf_Sqrt( d_cosLat * d_cosLat +  d_temp * d_temp );

	d_temp = (d_n + d_lla[2]) * d_cosLat;

	d_ecef[0] = d_temp * d_cosLon;							/* x-axis */
	d_ecef[1] = d_temp * d_sinLon;							/* y-axis */
	d_ecef[2] = (WGS84_R2 * d_n + d_lla[2]) * d_sinLat;		/* z-axis */
}

/** 
* \fn     gpsc_cd_Llh2EnuMatrixGet
* \brief  LLH to ENU calculation
* 
* This function is used  to copies the current estimate of the transformation
* matrix for ECEF to ENU. If the current position is significantly different
* from the last position used to compute the matrix, a new transformation
* matrix will be computed.
* 
* \note
* \param	d_Xform - he 3x3 transformation matrix.
* \param	d_Llh	- structure with LLH data
* \return 	void
* \sa     	gpsc_cd_Llh2EnuMatrixGet
*/ 
void gpsc_cd_Llh2EnuMatrixGet(McpDBL d_Xform[][3], McpDBL d_Llh[3])
{	
	/* Setup ENU transformations if receiver location has changed by some delta.  */	
	McpDBL	d_CosLat, d_SinLat, d_CosLon, d_SinLon;

	/* ECEF-to-ENU transformation matrix. */
	d_CosLat = mcpf_Cos( d_Llh[0] ),
	d_SinLat = mcpf_Sin( d_Llh[0] ),
	d_CosLon = mcpf_Cos( d_Llh[1] ),
	d_SinLon = mcpf_Sin( d_Llh[1] );
	
	d_Xform[0][0] = -d_SinLon;
	d_Xform[0][1] =  d_CosLon;
	d_Xform[0][2] =  0.0;
	
	d_Xform[1][0] = -d_SinLat * d_CosLon;
	d_Xform[1][1] = -d_SinLat * d_SinLon;
	d_Xform[1][2] =  d_CosLat;
	
	d_Xform[2][0] =  d_CosLat * d_CosLon;
	d_Xform[2][1] =  d_CosLat * d_SinLon;
	d_Xform[2][2] =  d_SinLat;
}

/** 
* \fn     gpsc_cd_CalcSvDirection
* \brief  direction calculation of a SV 
* 
* This function is used to calculate the Elevation and Azimuth of a SV.
* 
* \note
* \param	*p_zInjectPosEst     		- Injected position for which direction is to be calculated
* \param	*z_SvXyz			 	- XYZ position of SV
* \param	*elev	     			- returned Elevation of SV
* \param	*azi		     			- returned Elevation of SV
* \return 	void
* \sa     	gpsc_cd_CalcSvDirection
*/ 
void gpsc_cd_CalcSvDirection(gpsc_inject_pos_est_type *p_zInjectPosEst, gpsc_SV_xyz_type *z_SvXyz, 
							 McpDBL *elev, McpDBL *azi)
{
	McpDBL				d_Rho, d_Az, d_El;
	McpDBL				d_DxyzP[3], d_DenuP[3], d_dummy[3][3],d_Llh[3], d_ecef[3];

	d_Llh[0] = (McpDBL)((p_zInjectPosEst->l_Lat)/ (93206.75556*C_90_OVER_2_23 * C_LSB_LAT_REP)) ;
	d_Llh[1] = (McpDBL)((p_zInjectPosEst->l_Long)/ (46603.37778*C_360_OVER_2_24 * C_LSB_LON_REP));
	d_Llh[2] = (McpDBL)(p_zInjectPosEst->x_Ht)/2;

		gpsc_cd_Lla2Ecef(d_Llh, d_ecef);
	
		/* Direction vector in ECEF. */
		d_DxyzP[0] = z_SvXyz->xyz_x - d_ecef[0];
		d_DxyzP[1] = z_SvXyz->xyz_y - d_ecef[1];
		d_DxyzP[2] = z_SvXyz->xyz_z - d_ecef[2];


		gpsc_cd_Llh2EnuMatrixGet(d_dummy, d_Llh);

		/* Direction vector, d_DenuP, is in the East/North/Up system. */
		gpsc_cd_Ecef2Enu( d_dummy, d_DxyzP, d_DenuP );

		/* Compute SV elevation and azimuth. */
		d_Rho = mcpf_Sqrt( d_DenuP[0] * d_DenuP[0] +  d_DenuP[1] * d_DenuP[1] );

		if (d_Rho < 1.0)	/* Extremely unlikely case near the zenith/nadir. */
		{
			if (d_DenuP[2] > 0.0) d_El =  C_PI/2.0;		/* Straight up. */
			else                  d_El = -C_PI/2.0;		/* Straight down. */

			d_Az = 0.0;		/* Azimuth is not define at the poles. */
		}
		else	/* Our rho is large enough for safe computing. */
		{
			d_El  = mcpf_Atan2( d_DenuP[2], d_Rho );

			if (mcpf_Fabs(d_DenuP[1]) < 1.0)	 /* Extremely rare case. */
			{
				if (d_DenuP[0] > 0) d_Az = 0.0;
				else                d_Az = -C_PI;
			}
			else
			{
				d_Az  = mcpf_Atan2( d_DenuP[0], d_DenuP[1] );
			}
		}

		if (d_Az < 0.0)		/* Only non-negative azimuths for CD and ME. */
		{
			d_Az += 2.0 * C_PI;
		}

		*elev =  d_El * 180/C_PI;
		*azi  =  d_Az * 180/C_PI;
}


