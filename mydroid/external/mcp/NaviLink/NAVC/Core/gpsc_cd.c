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
#include "gpsc_types.h"
#include "gpsc_data.h"
#include "gpsc_consts.h"
#include "gpsc_cd.h"
#include "gpsc_msg.h"
#include "mcpf_services.h"
#include <utils/Log.h>


#define WGS84_GRAVITIONAL_CONSTANT	   (double)(3.986005e14)   // units m^3/s^2
#define WGS84_EARTH_ROTATION_RATE	   (double)7.2921151467e-5 //rad/sec
#define SEMICIRCLES_TO_RAD					   (double)3.1415926535898
#define RAD_TO_SEMICIRCLES					   (double)1.0/SEMICIRCLES_TO_RAD;
#define PI 3.1415926535897932384626433


/************************************************************************/
/*					Internal Functions Definition                       */
/************************************************************************/

void gpsc_cd_DecodeEphUnits(pe_RawEphemeris *p_Input, cd_Ephemeris *p_New);
void gpsc_cd_ComputeGPSSatellitePos(pe_RawEphemeris *RawEphemeris, McpU32 GpsTime, gpsc_SV_xyz_type *svs);
void gpsc_cd_GetSatEleAzi(McpDBL *Userpos, gpsc_SV_xyz_type *Svpos, McpDBL *Ele, McpDBL *Azi);
void gpsc_cd_ConvertLLA2XYZ(gpsc_inject_pos_est_type *Lla, McpDBL *Xyz);
void gpsc_cd_ConvertXYZ2LLA(McpDBL *Userpos, McpDBL *Lla);



/************************************************************************/
/*							APIs                                        */
/************************************************************************/

/** 
* \fn     gpsc_update_sv_visibility
* \brief  Calculation of SV visibility 
* 
* This function is used to calculate visibility of SVs that Ephemeris are available for.
* 
* \note
* \param	*p_zGPSCControl    		- GPSC control structure
* \return 	Return TRUE if SVs are avialable, FALSE otherwise.
* \sa     	gpsc_update_sv_visibility
*/ 


McpBool gpsc_update_sv_visibility( gpsc_ctrl_type*     p_zGPSCControl)
{
	McpU8 u_Counter;
	McpDBL elev, azi;
	
	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;
	
	gpsc_db_type*			p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
    gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
	gpsc_inject_pos_est_type	p_zInjectPosEst;
	McpU32 GpsTime = p_zGPSCDatabase->z_DBGpsClock.q_GpsMs;
	McpDBL d_xyz[3];

	if((p_zSmlcAssist->w_AvailabilityFlags & C_REFLOC_AVAIL) || 
		(p_zSmlcAssist->w_InjectedFlags & C_REFLOC_AVAIL))
	{
		/* if pos has been injected, use it*/
		p_zInjectPosEst.l_Lat = p_zSmlcAssist->z_InjectPosEst.l_Lat;
		p_zInjectPosEst.l_Long = p_zSmlcAssist->z_InjectPosEst.l_Long;
		p_zInjectPosEst.x_Ht= p_zSmlcAssist->z_InjectPosEst.x_Ht;
		STATUSMSG("gpsc_update_sv_visibility: Using network pos to calculate visibility");

	}
	else if (p_zGPSCDatabase->z_DBPos.d_ToaSec > 0)
	{	/* else use DB Pos */
		p_zInjectPosEst.l_Lat = p_zGPSCDatabase->z_DBPos.z_ReportPos.l_Lat;
		p_zInjectPosEst.l_Long = p_zGPSCDatabase->z_DBPos.z_ReportPos.l_Long;
		p_zInjectPosEst.x_Ht = p_zGPSCDatabase->z_DBPos.z_ReportPos.x_Height;
		STATUSMSG("gpsc_update_sv_visibility: Using DB pos to calculate visibility");

	}
	else
	{
		STATUSMSG("gpsc_update_sv_visibility: No usable position");
		return MCP_FALSE;
	}

	if((p_zSmlcAssist->w_AvailabilityFlags & C_REF_GPSTIME_AVAIL)||
		(p_zGPSCState->u_TimeInjected == TRUE))
	{	/* if network time is available, use it */
		GpsTime = p_zSmlcAssist->z_SmlcGpsTime.z_InjectGpsTimeEst.z_meTime.q_GpsMsec;
		STATUSMSG("gpsc_update_sv_visibility: Using network time to calculate visibility");

	}
	else if (p_zGPSCDatabase->z_DBGpsClock.u_Valid)
	{	/*use, DB time */
		GpsTime = p_zGPSCDatabase->z_DBGpsClock.q_GpsMs;
		STATUSMSG("gpsc_update_sv_visibility: Using DB time to calculate visibility");

	}
	else
	{	
		STATUSMSG("gpsc_update_sv_visibility: No usable TIME");
		return MCP_FALSE;
	}
	
	STATUSMSG("sv_visibility: Lat = %d, Long = %d, Ht = %d, GpsTime = %d",p_zInjectPosEst.l_Lat,p_zInjectPosEst.l_Long,p_zInjectPosEst.x_Ht,GpsTime);
	gpsc_cd_ConvertLLA2XYZ(&p_zInjectPosEst,&d_xyz[0]);
	
	for (u_Counter = 0; u_Counter<=31; u_Counter++)
	{
		if(p_zSmlcAssist->z_SmlcRawEph[u_Counter].u_Prn > 0) //check for valid SV
		{	
	/* compute XYZ of SVs */
			gpsc_cd_ComputeGPSSatellitePos(&p_zSmlcAssist->z_SmlcRawEph[u_Counter], GpsTime, &p_zSmlcAssist->z_SVxyz[u_Counter]);
	
	/* Compute visibility with available position and time */
			gpsc_cd_GetSatEleAzi(&d_xyz[0], &p_zSmlcAssist->z_SVxyz[u_Counter], &elev, &azi);
		
		if (elev > 5.0)
		{
			p_zSmlcAssist->q_VisiableBitmap |= (U32)mcpf_Pow(2, u_Counter);
		}
	}

	}

	STATUSMSG("gpsc_update_sv_visibility: Visible SVs: 0x%X", p_zSmlcAssist->q_VisiableBitmap);
	
	return MCP_TRUE;
}
McpBool gpsc_update_sv_visibility_Almanac( gpsc_ctrl_type*     p_zGPSCControl)
{
	McpU8 u_Counter;
	McpDBL elev, azi;

	gpsc_smlc_assist_type*  p_zSmlcAssist  = p_zGPSCControl->p_zSmlcAssist;

	gpsc_db_type*			p_zGPSCDatabase = p_zGPSCControl->p_zGPSCDatabase;
	gpsc_db_gps_meas_type*       p_zDBGpsMeas  = &p_zGPSCDatabase->z_DBGpsMeas;
        gpsc_state_type*		p_zGPSCState    = p_zGPSCControl->p_zGPSCState;
	gpsc_inject_pos_est_type	p_zInjectPosEst;
	gpsc_inj_sv_dir z_InjectSvDir[C_MAX_SVS_ACQ_ASSIST];
	McpU32 GpsTime = p_zGPSCDatabase->z_DBGpsClock.q_GpsMs;
	gpsc_db_alm_type *p_DBAlmanac;
	gpsc_meas_report_type*       p_zMeasReport = &p_zDBGpsMeas->z_MeasReport;
	gpsc_meas_per_sv_type*       p_zMeasPerSv;
	pe_RawAlmanac *p_RawAlmanac;
	McpDBL d_xyz[3],Geopos[3];
	McpU32 UsedSattleties[N_LCHAN];
	McpDBL x = 0,y = 0,z = 0,x_average,y_average,z_average;
	McpU32 u_i;

	STATUSMSG("gpsc_update_sv_visibility_Almanac");
	/*use, DB time */
		GpsTime = p_zGPSCDatabase->z_DBGpsClock.q_GpsMs;
		STATUSMSG("gpsc_update_sv_visibility_Almanac %f for gpstime %d",p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc,GpsTime);
		if(!(p_zGPSCDatabase->z_DBGpsClock.f_TimeUnc < 16)) {
		STATUSMSG("gpsc_update_sv_visibility: time uncertianity not less than 16ms");
		return MCP_FALSE;
		}
	else
	{
		STATUSMSG("gpsc_update_sv_visibility_Almanac  valid time");
	}

	McpU32 i =0;
	McpU32 counter =0;
	for (u_Counter = 0; u_Counter<=31; u_Counter++)
	{
		 p_DBAlmanac = &p_zGPSCDatabase->z_DBAlmanac[u_Counter];
		p_RawAlmanac = &p_zGPSCDatabase->z_DBAlmanac[ u_Counter ].z_RawAlmanac;
		if((p_DBAlmanac->u_Valid ==  TRUE) && (p_RawAlmanac->u_Prn > 0)) //check for valid SV
		{
	/* compute XYZ of SVs */

			STATUSMSG("gpsc_update_sv_visibility_Almanac  valid sattelite mk %d and omega %d for svid %d",p_RawAlmanac->q_MZero,p_RawAlmanac->q_Omega,p_RawAlmanac->u_Prn);
			gpsc_cd_ComputeGPSSatellitePosAlmanac(p_RawAlmanac, GpsTime, &p_zSmlcAssist->z_SVxyz[u_Counter]);
			p_zSmlcAssist->z_SVxyz[u_Counter].u_Prn = p_RawAlmanac->u_Prn;
			STATUSMSG("gpsc_update_sv_visibility xyz calculation: prn = %d, x = %f, y = %f,z = %f",p_zSmlcAssist->z_SVxyz[u_Counter].u_Prn,p_zSmlcAssist->z_SVxyz[u_Counter].xyz_x,p_zSmlcAssist->z_SVxyz[u_Counter].xyz_y,p_zSmlcAssist->z_SVxyz[u_Counter].xyz_z);
			counter++;
	}

	else {
	p_zSmlcAssist->z_SVxyz[u_Counter].xyz_x = 0;
	p_zSmlcAssist->z_SVxyz[u_Counter].xyz_y = 0;
	p_zSmlcAssist->z_SVxyz[u_Counter].xyz_z = 0;
	p_zSmlcAssist->z_SVxyz[u_Counter].u_Prn= 0;
	}

	}
	STATUSMSG("gpsc_update_sv_visibility_Almanac  counter %d",counter);
	if (counter < 5) {
	return MCP_FALSE;
	}
	for (u_i=0; u_i < N_LCHAN; u_i++)
	{
			U8 u_SvId;
		p_zMeasPerSv     = &p_zMeasReport->z_MeasPerSv[u_i];

		STATUSMSG("gpsc_update_sv_visibility_Almanac  for channel");

		u_SvId = (U8)(p_zMeasPerSv->u_SvIdSvTimeFlag & 0x3F);

	if((p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_SM_VALID)
					&& (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_FROM_DIFF)
					&& (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_DONT_USE)
					&& (~p_zMeasPerSv->w_MeasStatus & MEAS_STATUS_XCORR)) {
					STATUSMSG("gpsc_update_sv_visibility_Almanac  for channel 1");
					if(p_zSmlcAssist->z_SVxyz[u_SvId - 1].u_Prn != 0 ) {
		STATUSMSG("gpsc_update_sv_visibility_Almanac i  %dfor svid %d, measstatus %d",i,u_SvId,p_zMeasPerSv->w_MeasStatus);
	/* Compute visibility with available position and time */
	STATUSMSG("gpsc_update_sv_visibility_Almanac  for channel average out");
	x += p_zSmlcAssist->z_SVxyz[u_SvId -1].xyz_x;
	y += p_zSmlcAssist->z_SVxyz[u_SvId-1].xyz_y;
	z += p_zSmlcAssist->z_SVxyz[u_SvId- 1].xyz_z;
	UsedSattleties[i] = p_zSmlcAssist->z_SVxyz[u_SvId - 1].u_Prn;
	i++;

		}
		}
	}
if (i >3) {
	x_average = x/i;
	y_average = y/i;
	z_average = z/i;
	d_xyz[0] = x_average;
	d_xyz[1] = y_average;
	d_xyz[2] = z_average;

	STATUSMSG("gpsc_update_sv_visibility: x_average %f, y_average %f, z_average %f",d_xyz[0],d_xyz[1],d_xyz[2]);

	gpsc_cd_ConvertXYZ2LLA(&d_xyz[0], &Geopos[0]);
	Geopos[2] = 0;
	gpsc_cd_ConvertLLA2XYZAlmanac(&Geopos[0],&d_xyz[0]);

	STATUSMSG("gpsc_update_sv_visibility1: x_average %f, y_average %f, z_average %f",d_xyz[0],d_xyz[1],d_xyz[2]);

	STATUSMSG("gpsc_update_sv_visibility: Geopos[0] %f, Geopos[1] %f, Geopos[2] %f, i = %d",Geopos[0],Geopos[1],Geopos[2],i);

	McpU32 u = 0;
	for (u_Counter = 0; u_Counter<=31; u_Counter++)
		{
			//
			STATUSMSG("gpsc_update_sv_visibility: prn = %d, x = %f, y = %f,z = %f",p_zSmlcAssist->z_SVxyz[u_Counter].u_Prn,p_zSmlcAssist->z_SVxyz[u_Counter].xyz_x,p_zSmlcAssist->z_SVxyz[u_Counter].xyz_y,p_zSmlcAssist->z_SVxyz[u_Counter].xyz_z);


		//	p_RawAlmanac = &p_zGPSCDatabase->z_DBAlmanac[ u_Counter ].z_RawAlmanac;
		//	if(p_RawAlmanac->u_Prn > 0) //check for valid SV
			if(p_zSmlcAssist->z_SVxyz[u_Counter].u_Prn != 0)
			{

				gpsc_cd_GetSatEleAzi(&d_xyz[0], &p_zSmlcAssist->z_SVxyz[u_Counter], &elev, &azi);

			STATUSMSG("gpsc_update_sv_visibility: svid :%d,elev :%f,azim :%f",p_zSmlcAssist->z_SVxyz[u_Counter].u_Prn,elev,azi);

			if (elev > -35)
			{
/*
				p_zSmlcAssist->q_VisiableBitmap |= (U32)mcpf_Pow(2, u_Counter);
				z_InjectSvDir[u_Counter].b_Elev = elev;
			  	z_InjectSvDir[u_Counter].u_Azim = azi;
			   	z_InjectSvDir[u_Counter].u_SvId = p_zSmlcAssist->z_SmlcRawEph[u_Counter].u_Prn;

			   	z_InjectSvDir[u_Counter].b_Elev = 0x80;
			  	z_InjectSvDir[u_Counter].u_Azim = 0;
			   	z_InjectSvDir[u_Counter].u_SvId = p_zSmlcAssist->z_SVxyz[u_Counter].u_Prn;
			   	STATUSMSG("gpsc_update_sv_visibility: z_InjectSvDir[u_Counter].u_SvId %d",z_InjectSvDir[u_Counter].u_SvId);
			*/

			//
			//
			}
			else {
				if(u < C_MAX_SVS_ACQ_ASSIST) {
				z_InjectSvDir[u].b_Elev = elev/(180.0/256.0);
			  	z_InjectSvDir[u].u_Azim = azi/(360.0/256.0);
			   	z_InjectSvDir[u].u_SvId = p_zSmlcAssist->z_SVxyz[u_Counter].u_Prn;
				u++;
			}
			}



		}

		}
		STATUSMSG("gpsc_update_sv_visibility: i = %d  u = %d",i,u);
		for (u_Counter = 0; u_Counter< i; u_Counter++) {

			for(u_i = 0; u_i < u; u_i++) {
			STATUSMSG("gpsc_update_sv_visibility: UsedSattleties[u_Counter] %d  z_InjectSvDir[].u_SvId %d",UsedSattleties[u_Counter],z_InjectSvDir[u_i].u_SvId);
				if(UsedSattleties[u_Counter] == z_InjectSvDir[u_i].u_SvId ) {

				  elev= z_InjectSvDir[u_i].b_Elev*(180.0/256.0);
				  if(elev < -30) {

				  	STATUSMSG("gpsc_update_sv_visibility: elev %f  svid %d less than -30 ",elev,z_InjectSvDir[u_i].u_SvId);
					return MCP_FALSE;
				  }
				}


			}


		}


	p_zGPSCControl->p_zGPSCState->injectSvdirectionColdstart = FALSE;
	gpsc_mgp_inject_sv_dir_almanac(p_zGPSCControl, &z_InjectSvDir[0], u);

	if(gpsc_comm_transmit(p_zGPSCControl, C_AI2_ACK_REQ)==FALSE)
		return GPSC_TX_INITIATE_FAIL;
	}

	return MCP_TRUE;
}





/* Ref: http://www.colorado.edu/geography/gcraft/notes/gps/ephxyz.html
  * This function computes satellite XYZ positions from GPS Ephemeris
  * GPS Ephemeris is assumed in same units as mentioned in GPS ICD
  * for using Almanac instead of Ephemeris, Set the Ephemeris parameters not present in Almanac to Zero
  */

 void gpsc_cd_ComputeGPSSatellitePos(pe_RawEphemeris *RawEphemeris, McpU32 GpsTime, gpsc_SV_xyz_type *svs)
 {
       McpDBL  axis,
                        n0,
                        i0,
                        deltaN,
                        OMEGA,
                        OmegaDot,
                        iDot,
                        timediff,
                        Mk,
                        Ek,
                        Eccentricity,
                        t1,
                        t2,
                        t3,
                        t4,
                        t5,
                        Vk,
                        pk,
                        uk,
                        rk,
                        ik,
                        perigee,
                        sx,
                        sy,
                        sz;

        McpInt          ii;

		cd_Ephemeris Decoded_ephemeris;
		cd_Ephemeris *Gps_EphData = &Decoded_ephemeris;
		
		gpsc_cd_DecodeEphUnits(RawEphemeris,Gps_EphData);
		
		
        /* convert all parameters with untis of semi-circles  to radians */
        Mk = Gps_EphData->d_M0 * SEMICIRCLES_TO_RAD;
        deltaN = Gps_EphData->d_DeltaN * SEMICIRCLES_TO_RAD;
        OMEGA = Gps_EphData->d_Omega0 * SEMICIRCLES_TO_RAD;
        i0 = Gps_EphData->d_I0 * SEMICIRCLES_TO_RAD;
        perigee = Gps_EphData->d_Omega * SEMICIRCLES_TO_RAD; //to be verified
        OmegaDot = Gps_EphData->d_OmegaDot * SEMICIRCLES_TO_RAD;
        iDot = Gps_EphData->d_Idot * SEMICIRCLES_TO_RAD;

        Eccentricity = Gps_EphData->d_E;

        axis = Gps_EphData->d_SqrtA* Gps_EphData->d_SqrtA; // get semi-major axis
        n0 = mcpf_Sqrt((WGS84_GRAVITIONAL_CONSTANT / (axis * axis *axis))); // compute mean motion
       
		
        /* get time from GPS reference epoch and account for week roll over
        * Assumes that GPS week and Ephemeris week can be maximum off by 1 week
        */
        timediff = (GpsTime/1000) - Gps_EphData->d_Toe;
        if(timediff > 302400)
                timediff -= 604800;
        else
        {
                if(timediff < -302400)
                        timediff += 604800;
        }

        /*correct for mean motion  (units radians)*/
        n0 += deltaN;

        /* mean anamoly in radians*/
        Mk  +=  n0 * timediff;
		
		
        /* Determine Eccentricity Anamoly by iteration with wegstein's accelerator*/


        t1 = Mk;
        t2 = Mk - (t1 - Eccentricity * mcpf_Sin(t1));
        t3 = t1;
        t1 = t2;

        for(ii = 0; ii < 16; ii++)
        {
                t4 = t3;
                t3 = t1;
                t5 = t2;
                t2 = Mk - (t1 - Eccentricity * mcpf_Sin(t1));
                if(mcpf_Fabs(t2 - t5) < 1e-15)
                        break;
                t1 = (t4 * t2 - t1 * t5)/(t2 - t5);
        }
        Ek = t1;

        /* compute True anamoly */
        t1 = mcpf_Sqrt((1 - Eccentricity * Eccentricity)) * mcpf_Sin(Ek);
        t2 = mcpf_Cos(Ek) - Eccentricity;
        Vk = mcpf_Atan2(t1,t2);
		
		

		if (Vk < 0.0) Vk += 2*C_PI;

        /*argument of latitude*/
		pk = Vk+perigee;

		
		
        t1 = mcpf_Sin(2 * pk);
        t2 = mcpf_Cos(2 * pk);

        /*second harmonic perturbations*/
       	uk = Gps_EphData->d_Cus * t1 + Gps_EphData->d_Cuc * t2;
		rk = Gps_EphData->d_Crs * t1 + Gps_EphData->d_Crc * t2;
		ik = Gps_EphData->d_Cis * t1 + Gps_EphData->d_Cic * t2;

        /*correct argument of latitude*/
        uk += pk;

        /*corrected radius*/
        rk += axis *(1- Eccentricity * mcpf_Cos(Ek));

		
        /*corrected inclination*/
        ik += i0 + iDot * timediff;

		
		
        /*positions in orbital plane*/
        t1 = rk * mcpf_Cos(uk);
        t2 = rk * mcpf_Sin(uk);

        /*corrected longitude of ascending node*/
        t3 = OMEGA + (OmegaDot - WGS84_EARTH_ROTATION_RATE)*timediff - WGS84_EARTH_ROTATION_RATE * Gps_EphData->d_Toe;

        /*Earth fixed coordinates*/
        sx = t1 * mcpf_Cos(t3) - t2 * mcpf_Cos(ik)*mcpf_Sin(t3);
        sy = t1 * mcpf_Sin(t3) + t2 * mcpf_Cos(ik)*mcpf_Cos(t3);
        sz = t2 * mcpf_Sin(ik);

        /*return values*/
        svs->xyz_x = sx;
        svs->xyz_y = sy;
        svs->xyz_z = sz;
		
		/*if(svs != NULL)
		{
			STATUSMSG("sv_visibility: Prn = %d: svs->xyz_x = %f svs->xyz_y = %f svs->xyz_z = %f",RawEphemeris->u_Prn,svs->xyz_x,svs->xyz_y,svs->xyz_z);
		}
		*/
        return;
 }



/* This function computes Elevation and Azimuth of a satellite wrt user position
 * Input satellite position in [X,Y,Z] and User position in [X,Y,Z]
 * Output Elevation and Azimuthy
 * Reference: Colorado Center for Astrodynamics Research, The University of Colorado
 */

void gpsc_cd_GetSatEleAzi(McpDBL *Userpos, gpsc_SV_xyz_type *Svpos, McpDBL *Ele, McpDBL *Azi)
 {
        McpDBL  diff[3],
                        Coord[3][3],
                        Geopos[3],
                        Elevation,
                        Azimuth;
        McpInt 			x, y;

        /* find position direction difference*/
        diff[0] = Svpos->xyz_x - Userpos[0];
        diff[1] = Svpos->xyz_y - Userpos[1];
        diff[2] = Svpos->xyz_z - Userpos[2];

        /*Get Geodetic position to obtain the Coordinate transformation matrix*/
        gpsc_cd_ConvertXYZ2LLA(Userpos, &Geopos[0]);

        /* using South East Up sequence */
        Coord[1][0] = -mcpf_Sin(Geopos[1]);
        Coord[1][1] = mcpf_Cos(Geopos[1]);
        Coord[1][2] = 0;

        Coord[0][0] = -mcpf_Sin(Geopos[0]) * mcpf_Cos(Geopos[1]);
        Coord[0][1] = -mcpf_Sin(Geopos[0]) * mcpf_Sin(Geopos[1]);
        Coord[0][2] = mcpf_Cos(Geopos[0]);

        Coord[2][0] = mcpf_Cos(Geopos[0]) * mcpf_Cos(Geopos[1]);
        Coord[2][1] = mcpf_Cos(Geopos[0]) * mcpf_Sin(Geopos[1]);
        Coord[2][2] = mcpf_Sin(Geopos[0]);

        /*Transform the satellite position to ENU domain*/
        for(x = 0; x < 3; x++)
        {
                Geopos[x] = 0.0;
                for(y = 0; y < 3; y ++)
                {
                        Geopos[x] += Coord[x][y] * diff[y];
                }
        }

        diff[0] = mcpf_Sqrt((Geopos[0] * Geopos[0] + Geopos[1] * Geopos[1] + Geopos[2] * Geopos[2]));

        if(diff[0] < 1.0)
        {
                /* poles*/
                if (diff[2] > 0.0)
                        Elevation = PI * 0.5;
                else
                        Elevation = -PI * 0.5;
                Azimuth = 0.0;
        }
        else
        {
                Elevation = mcpf_Atan2(Geopos[2], diff[0]);

                if(mcpf_Fabs(Geopos[0]) < 1.0)
                {
                        if(Geopos[1] > 0 )
                                Azimuth = 0.0;
                        else
                                Azimuth = -PI;
                }
                else
                {
                        Azimuth = mcpf_Atan2(Geopos[1], -Geopos[0]);
                }
        }

        *Ele =  Elevation * 180.0/PI;
        *Azi  =  Azimuth * 180/PI;
		
        return;
 }
 void gpsc_cd_ComputeGPSSatellitePosAlmanac(pe_RawAlmanac *RawAlmanc, McpU32 GpsTime, gpsc_SV_xyz_type *svs)

  {

		McpDBL	axis,

						 n0,

						 i0,

						 deltaN,

						 OMEGA,

						 OmegaDot,

						 iDot,

						 timediff,

						 Mk,

						 Ek,

						 Eccentricity,

						 t1,

						 t2,

						 t3,

						 t4,

						 t5,

						 Vk,

						 pk,

						 uk,

						 rk,

						 ik,

						 perigee,

						 sx,

						 sy,

						 sz;



		 McpInt 		 ii;



		 cd_Ephemeris Decoded_ephemeris;

		 cd_Ephemeris *Gps_EphData = &Decoded_ephemeris;



		 gpsc_cd_DecodeAlm2EphUnits(RawAlmanc,Gps_EphData);





		 /* convert all parameters with untis of semi-circles  to radians */

		 Mk = Gps_EphData->d_M0 * SEMICIRCLES_TO_RAD;

		 deltaN = Gps_EphData->d_DeltaN * SEMICIRCLES_TO_RAD;

		 OMEGA = Gps_EphData->d_Omega0 * SEMICIRCLES_TO_RAD;

		 i0 = Gps_EphData->d_I0 * SEMICIRCLES_TO_RAD;

		 perigee = Gps_EphData->d_Omega * SEMICIRCLES_TO_RAD; //to be verified

		 OmegaDot = Gps_EphData->d_OmegaDot * SEMICIRCLES_TO_RAD;

		 iDot = Gps_EphData->d_Idot * SEMICIRCLES_TO_RAD;


		 Eccentricity = Gps_EphData->d_E;



		 axis = Gps_EphData->d_SqrtA* Gps_EphData->d_SqrtA; // get semi-major axis

		 n0 = mcpf_Sqrt((WGS84_GRAVITIONAL_CONSTANT / (axis * axis *axis))); // compute mean motion



		 STATUSMSG("gpsc_update_sv_visibility_Almanac mk %f deltaN %f OMEGA %f i0 %f perigee %f OmegaDot %f iDot %f Eccentricity %f Gps_EphData->d_Toe %f  GpsTime %d",Mk,deltaN,OMEGA,i0,perigee,OmegaDot,iDot,Eccentricity,Gps_EphData->d_Toe,GpsTime);


		 /* get time from GPS reference epoch and account for week roll over

		 * Assumes that GPS week and Ephemeris week can be maximum off by 1 week

		 */

		 timediff = (GpsTime * 1e-3) - Gps_EphData->d_Toe;

		 if(timediff > 302400)

				 timediff -= 604800;

		 else

		 {

				 if(timediff < -302400)

						 timediff += 604800;

		 }



		 /*correct for mean motion	(units radians)*/

		 n0 += deltaN;



		 /* mean anamoly in radians*/

		 Mk  +=  n0 * timediff;





		 /* Determine Eccentricity Anamoly by iteration with wegstein's accelerator*/





		 t1 = Mk;

		 t2 = Mk - (t1 - Eccentricity * mcpf_Sin(t1));

		 t3 = t1;

		 t1 = t2;



		 for(ii = 0; ii < 16; ii++)

		 {

				 t4 = t3;

				 t3 = t1;

				 t5 = t2;

				 t2 = Mk - (t1 - Eccentricity * mcpf_Sin(t1));

				 if(mcpf_Fabs(t2 - t5) < 1e-15)

						 break;

				 t1 = (t4 * t2 - t1 * t5)/(t2 - t5);

		 }

		 Ek = t1;



		 /* compute True anamoly */

		 t1 = mcpf_Sqrt((1 - Eccentricity * Eccentricity)) * mcpf_Sin(Ek);

		 t2 = mcpf_Cos(Ek) - Eccentricity;

		 Vk = mcpf_Atan2(t1,t2);







				 if (Vk < 0.0) Vk += 2*C_PI;



		 /*argument of latitude*/

				 pk = Vk+perigee;







		 t1 = mcpf_Sin(2 * pk);

		 t2 = mcpf_Cos(2 * pk);



		 /*second harmonic perturbations*/

		 uk = Gps_EphData->d_Cus * t1 + Gps_EphData->d_Cuc * t2;

				 rk = Gps_EphData->d_Crs * t1 + Gps_EphData->d_Crc * t2;

				 ik = Gps_EphData->d_Cis * t1 + Gps_EphData->d_Cic * t2;



		 /*correct argument of latitude*/

		 uk += pk;



		 /*corrected radius*/

		 rk += axis *(1- Eccentricity * mcpf_Cos(Ek));





		 /*corrected inclination*/

		 ik += i0 + iDot * timediff;







		 /*positions in orbital plane*/

		 t1 = rk * mcpf_Cos(uk);

		 t2 = rk * mcpf_Sin(uk);



		 /*corrected longitude of ascending node*/

		 t3 = OMEGA + (OmegaDot - WGS84_EARTH_ROTATION_RATE)*timediff - WGS84_EARTH_ROTATION_RATE * Gps_EphData->d_Toe;



		 /*Earth fixed coordinates*/

		 sx = t1 * mcpf_Cos(t3) - t2 * mcpf_Cos(ik)*mcpf_Sin(t3);

		 sy = t1 * mcpf_Sin(t3) + t2 * mcpf_Cos(ik)*mcpf_Cos(t3);

		 sz = t2 * mcpf_Sin(ik);



		 /*return values*/

		 svs->xyz_x = sx;

		 svs->xyz_y = sy;

		 svs->xyz_z = sz;



				 /*if(svs != NULL)

				 {

						 STATUSMSG("sv_visibility: Prn = %d: svs->xyz_x = %f svs->xyz_y = %f svs->xyz_z = %f",RawEphemeris->u_Prn,svs->xyz_x,svs->xyz_y,svs->xyz_z);

				 }

				 */

		 return;

  }



/* converts Geodetic Frame to Cartesian Frame */
void gpsc_cd_ConvertLLA2XYZ(gpsc_inject_pos_est_type *d_Llh, McpDBL *Xyz)
{
	   /* Reference: Bowring, 1985, The accuracy of geodetic latitude and height equations,
			   Survey Review, 28, 202-206.
	   */

	   McpDBL  t1,
			   Wgs84_major = 6378137.0,
			   Wgs84_minor = 6356752.314,
			   e;
	   McpDBL Lla[3];
	   
	   Lla[0] = (McpDBL)((d_Llh->l_Lat)/ (93206.75556*C_90_OVER_2_23 * C_LSB_LAT_REP)) ;
	   Lla[1] = (McpDBL)((d_Llh->l_Long)/ (46603.37778*C_360_OVER_2_24 * C_LSB_LON_REP));
	   Lla[2] = (McpDBL)(d_Llh->x_Ht)/2;
	   
	   e = mcpf_Sqrt(1.0 - ((Wgs84_minor*Wgs84_minor)/(Wgs84_major * Wgs84_major)));
	   t1 = mcpf_Sqrt(1-e*e*mcpf_Sin(Lla[0])*mcpf_Sin(Lla[0]));

	   Xyz[0] = (Wgs84_major/t1 + Lla[2])*mcpf_Cos(Lla[0]) * mcpf_Cos(Lla[1]);
	   Xyz[1] = (Wgs84_major/t1 + Lla[2])*mcpf_Cos(Lla[0]) * mcpf_Sin(Lla[1]);
	   Xyz[2] = (Wgs84_major*(1-e*e)/t1 + Lla[2])*mcpf_Sin(Lla[0]);

	   
	   return;
}
void gpsc_cd_ConvertLLA2XYZAlmanac(McpDBL *d_Llh, McpDBL *Xyz) {
	   /* Reference: Bowring, 1985, The accuracy of geodetic latitude and 		height equations,   Survey Review, 28, 202-206.
	   */

	   McpDBL  t1,
			   Wgs84_major = 6378137.0,
			   Wgs84_minor = 6356752.314,
			   e;
	   McpDBL Lla[3];

	   Lla[0] = (McpDBL)(d_Llh[0]) ;
	   Lla[1] = (McpDBL)(d_Llh[1]);
	   Lla[2] = (McpDBL)(d_Llh[2]);

	   e = mcpf_Sqrt(1.0 - ((Wgs84_minor*Wgs84_minor)/(Wgs84_major * Wgs84_major)));
	   t1 = mcpf_Sqrt(1-e*e*mcpf_Sin(Lla[0])*mcpf_Sin(Lla[0]));

	   Xyz[0] = (Wgs84_major/t1 + Lla[2])*mcpf_Cos(Lla[0]) * mcpf_Cos(Lla[1]);
	   Xyz[1] = (Wgs84_major/t1 + Lla[2])*mcpf_Cos(Lla[0]) * mcpf_Sin(Lla[1]);
	   Xyz[2] = (Wgs84_major*(1-e*e)/t1 + Lla[2])*mcpf_Sin(Lla[0]);



	   return;

}


/* converts Cartesian Frame to Geodetic Frame */
 void gpsc_cd_ConvertXYZ2LLA(McpDBL *Userpos, McpDBL *Lla)
 {
        /* reference: http://www.ese.ogi.edu/~waldemer/SShmwk2.htm */
        McpDBL  t1,
                        t3,
                        t4,
                        Wgs84_major,
                        Wgs84_minor,
                        Wgs84_1,
                        Wgs84_2;

        Wgs84_major = 6378137.0;
        Wgs84_minor = 6356752.314;
        Wgs84_1 = 1.0 - (Wgs84_major - Wgs84_minor)/Wgs84_major;
        Wgs84_2 = (1.0 - (Wgs84_minor * Wgs84_minor) / (Wgs84_major * Wgs84_major));


		t1 = mcpf_Sqrt(Userpos[0] * Userpos[0] + Userpos[1] * Userpos[1]);
        t3 = mcpf_Atan2(Userpos[2] * Wgs84_major,t1*Wgs84_minor);
        t4 = Wgs84_2/(1.0 - Wgs84_2);


		Lla[1] = mcpf_Atan2(Userpos[1],Userpos[0]);
        Lla[0] = mcpf_Atan2((Userpos[2] + t4 * Wgs84_minor * mcpf_Sin(t3)*mcpf_Sin(t3)*mcpf_Sin(t3)),
                                        (t1 - Wgs84_2 * Wgs84_major * mcpf_Cos(t3)*mcpf_Cos(t3)*mcpf_Cos(t3)));
        Lla[2]= t1 /mcpf_Cos(Lla[0]) - (Wgs84_major /mcpf_Sqrt((1.0 - Wgs84_2 * mcpf_Sin(Lla[0]) * mcpf_Sin(Lla[0]))));


        return;
 }



/************************************************************************/
/*					Internal Functions Implementation                   */
/************************************************************************/

/**
 * \fn     gpsc_cd_DecodeEphUnits
 * \brief  decodes ephemeris to ICD equivalent units
 *
 * This function is used to decode the ephemeris to ICS equivalent units.
 *
 * \note
 * \param	p_Input     		- Raw Ephemeris
 * \param	p_New    		- decoded Ephemeris
 * \return 	void
 * \sa     	gpsc_cd_DecodeEphUnits
 */
	void gpsc_cd_DecodeEphUnits(pe_RawEphemeris *p_Input, cd_Ephemeris *p_New)
	{


		p_New->u_Prn = p_Input->u_Prn;
		p_New->u_Iode = p_Input->u_Iode;
		p_New->d_Crs = (McpDBL) (S16) p_Input->w_Crs * (1.0 / TWO_TO_5);
		p_New->d_DeltaN = (McpDBL) (S16) p_Input->w_DeltaN * (1.0 / TWO_TO_43);
		p_New->d_M0 = (McpDBL) (S32) p_Input->q_MZero * (1.0/ TWO_TO_31);
		p_New->d_Cuc = (McpDBL) (S16) p_Input->w_Cuc * (1.0 / TWO_TO_29);
		p_New->d_E = (McpDBL) p_Input->q_E * (1.0 / TWO_TO_33);
		p_New->d_Cus = (McpDBL) (S16) p_Input->w_Cus * (1.0 / TWO_TO_29);
		p_New->d_SqrtA = (McpDBL) p_Input->q_SqrtA * (1.0 / TWO_TO_19);
		p_New->d_Toe = (McpDBL) p_Input->w_Toe * TWO_TO_4;
		p_New->d_Cic = (McpDBL) (S16) p_Input->w_Cic * (1.0 / TWO_TO_29);
		p_New->d_Omega0 = (McpDBL) (S32) p_Input->q_OmegaZero * (1.0 / TWO_TO_31);
		p_New->d_Cis = (McpDBL) (S16) p_Input->w_Cis * (1.0 / TWO_TO_29);
		p_New->d_I0 = (McpDBL) (S32) p_Input->q_IZero * (1.0 / TWO_TO_31);
		p_New->d_Crc = (McpDBL) (S16) p_Input->w_Crc * (1.0 / TWO_TO_5);
		p_New->d_Omega = (McpDBL) (S32) p_Input->q_Omega * (1.0 / TWO_TO_31);
		p_New->d_OmegaDot = (McpDBL) ((S32) (p_Input->q_OmegaDot << 8) >> 8) * (1.0 / TWO_TO_43);
		p_New->d_Idot = (McpDBL) ((S16) (p_Input->w_IDot << 2) >> 2) * (1.0 / TWO_TO_43);



	}

	void gpsc_cd_DecodeAlmUnits(pe_RawAlmanac *p_Input, cd_Almanac *p_New)
	{


		p_New->d_Prn = p_Input->u_Prn;
                p_New->d_DeltaI = (McpDBL) (S16) p_Input->w_DeltaI * (1.0 / TWO_TO_19);
                p_New->d_MZero = (McpDBL) ((S32) (p_Input->q_MZero <<8) >>8) * (1.0/ TWO_TO_23);
                p_New->d_E = (McpDBL) p_Input->w_E * (1.0 / TWO_TO_21);
                p_New->d_SqrtA = (McpDBL) p_Input->q_SqrtA * (1.0 / TWO_TO_11);
                p_New->d_Toa = (McpDBL) p_Input->u_Toa * TWO_TO_12;
                p_New->d_OmegaZero = (McpDBL) ((S32)( p_Input->q_OmegaZero <<8) >> 8) * (1.0 / TWO_TO_23);
                p_New->d_Omega = (McpDBL) ((S32) (p_Input->q_Omega << 8) >>8) * (1.0 / TWO_TO_23);
                p_New->d_OmegaDot = (McpDBL) ((S32)(p_Input->w_OmegaDot ) )* (1.0 / TWO_TO_38);

                STATUSMSG("gpsc_update_sv_visibility_Almanac p_Input->q_MZero %d p_New->d_MZero %d",p_Input->q_MZero,p_New->d_MZero);



/*
		p_New->d_Prn = p_Input->u_Prn;
		p_New->d_DeltaI = (McpDBL) (S16) p_Input->w_DeltaI * (1.0 / TWO_TO_19);
		p_New->d_MZero = (McpDBL) (S32) p_Input->q_MZero * (1.0/ TWO_TO_23);
		p_New->d_E = (McpDBL) p_Input->w_E * (1.0 / TWO_TO_21);
		p_New->d_SqrtA = (McpDBL) p_Input->q_SqrtA * (1.0 / TWO_TO_11);
		p_New->d_Toa = (McpDBL) p_Input->u_Toa * TWO_TO_12;
		p_New->d_OmegaZero = (McpDBL) (S32) p_Input->q_OmegaZero * (1.0 / TWO_TO_23);
		p_New->d_Omega = (McpDBL) (S32) p_Input->q_Omega * (1.0 / TWO_TO_23);
		p_New->d_OmegaDot = (McpDBL) ((S32) (p_Input->w_OmegaDot << 8) >> 8) * (1.0 / TWO_TO_);
		*/

	}


	void gpsc_cd_DecodeAlm2EphUnits(pe_RawAlmanac *p_Input, cd_Ephemeris *p_New)

	{

							p_New->u_Prn = p_Input->u_Prn;
							p_New->u_Iode = 0;
							p_New->d_Crs = 0;
							p_New->d_DeltaN = 0;
							p_New->d_Cuc = 0;
							p_New->d_Cus = 0;
							p_New->d_Cic = 0;
							p_New->d_Cis = 0;
							p_New->d_Crc = 0;
							p_New->d_M0 = (McpDBL) ((S32) (p_Input->q_MZero <<8 ) >> 8) * (1.0/ TWO_TO_23); //24 bits
							p_New->d_E = (McpDBL) p_Input->w_E * (1.0 / TWO_TO_21); //16 bits
							p_New->d_SqrtA = (McpDBL) p_Input->q_SqrtA * (1.0 / TWO_TO_11); //24 bits
							p_New->d_Toe = (McpDBL) p_Input->u_Toa * TWO_TO_12; //8 bits
							p_New->d_Omega0 = (McpDBL) ((S32)(p_Input->q_OmegaZero << 8) >> 8)* (1.0 / TWO_TO_23);


							p_New->d_I0 = 0.3;
							p_New->d_Omega = (McpDBL) ((S32) (p_Input->q_Omega << 8) >> 8) * (1.0 / TWO_TO_23);
							p_New->d_OmegaDot = (McpDBL) ((S32) (p_Input->w_OmegaDot)) * (1.0 / TWO_TO_38);
							p_New->d_Idot = (McpDBL) ((S16) (p_Input->w_DeltaI)) * (1.0 / TWO_TO_19);

				STATUSMSG("gpsc_update_sv_visibility_Almanac p_Input->q_MZero %d p_New->d_MZero %f",p_Input->q_MZero,p_New->d_M0);

	}
