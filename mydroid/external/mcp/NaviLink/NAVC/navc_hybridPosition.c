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
 * FileName			:	navc_hybridPosition.c
 *
 * Description     	:
 * Implementation of wifi position blender and sending the blended out put the requestor and Sensor
 *
 *
 * Author         	: Raghavendra MR
 *
 *
 ******************************************************************************
 */


#include "gpsc_data.h"
#include "mcpf_services.h"
#include "mcpf_time.h"
#include "mcpf_report.h"
#include "mcpf_msg.h"
#include "mcp_hal_fs.h"
#include "mcp_hal_string.h"
#include "navc_defs.h"
#include "navc_api.h"
#include "gpsc_msg.h"
#include "navc_hybridPosition.h"
#include "gpsc_app_api.h"
#include "math.h"
#include "gpsc_mgp_tx.h"


U32 q_wifiindexcount;


/************************************************************************
 * Defines
 ************************************************************************/
	/* WiFi */
#define GOOD_WIFI_FIX_THLD  70
#define WIFI_POS_BUF_LEN  5
#define MAX_SPEED 20 /* meters per sec */
#define MAX_DIFF_GPS_WIFI_FIXES 200
#define GPSC_GPS_POS_VALIDITY_THRESHOLD 1000
#define GPSC_WIFI_POS_VALIDITY_THRESHOLD 2000
#define MIN_WIFI_UNC 200

#define KF_FIX 0x1000 //In GPS position extended(?) report, KF fix is indicated by 13 bit from right

#define TRACK1_BLENDER  0
#define TRACK2_BLENDER  1
#define GPS_UNC_THRESHOLD 10  /*in metres (If GPS uncertainity less than threshold blended output is GPS itself)*/
//#define DIST_THRESHOLD 300 /*If GPS solution is from WLS mode*/
#define DIST_THRESHOLD 250 /*If GPS solution is from KF mode*/


#define pi 3.14159265358979
#define C_PI	3.1415926535898
/* The ICD-GPS-200 document provides these earth constants to convert from
 * the ECEF coordiante system into the WGS-84 lat, long, alt system.
*/

#define WGS84_A		6378137.000		/* Semi-major axis (m). (to equator)*/
#define WGS84_B		6356752.314		/* Semi-minor axis (m). (to poles)  */


/* Earth's obliquity and flatness terms. */

#define WGS84_F		(WGS84_A - WGS84_B) / WGS84_A
#define WGS84_1MF	(1.0 - WGS84_F)
#define WGS84_R2	((WGS84_B * WGS84_B) / (WGS84_A * WGS84_A))
#define WGS84_E2	(1.0 - (WGS84_B * WGS84_B) / (WGS84_A * WGS84_A))


/************************************************************************
 * Global
 ************************************************************************/

/************************************************************************
 * Internal functions prototypes
 ************************************************************************/
 U8 gpsc_wifi_pos_selector
(
	DBL d_gps_lla[],
	DBL d_wifi_lla[],
	U8 u_gps_pos_valid,
	U8 u_wifi_pos_valid,
	DBL gps_2DUnc,
	DBL wifi_Unc
);

U8 gpsc_wifi_pos_blender
(
	DBL d_gps_lla[],
	DBL d_wifi_lla[],
	U8 u_gps_pos_valid,
	U8 u_wifi_pos_valid,
	DBL gps_2DUnc,
	DBL wifi_Unc,
	DBL d_out_lla[],
	DBL *d_out_blen_unc
);

/******************************************************************************
 * Function description:
 *  Converts WGS84 Lat, Lon & Alt to ECEF.
 *
 * Parameters:
 *	d_lla		- Latitude (rad), longitude (rad) and altitude (m).
 *	d_ecef		- XYZ position (m) in the ECEF coordinate system.
 *
 * Return value:
 *	none
 ******************************************************************************
*/

void cd_Lla2Ecef_ind(
	DBL d_lla[],
	DBL d_ecef[]
);


/******************************************************************************
 * Function description:
 *  cd_Ecef2Lla() converts an ECEF position into a WGS-84 Lat, Lon & Alt.
 *
 * Parameters:
 *	d_Ecef		- XYZ position (m) in the ECEF coordinate system.
 *	d_Lla		- Latitude (rad), longitude (rad) and altitude (m).
 *
 * Return value:	None
 *
 ******************************************************************************
*/

void cd_Ecef2Lla_ind(
	DBL d_Ecef[],
	DBL	d_Lla[]
);


void log_wifi_selector_data
(
	gpsc_ctrl_type* p_zGPSCControl,
	McpDBL *d_gps_lla,
	McpDBL d_gps_Unc_east,
	McpDBL d_gps_Unc_north,
	McpDBL d_gps_Unc_vertical,
	McpDBL d_gps_2DUnc,
	gpsc_db_wifi_pos_type* p_z_WifiPos,
	McpDBL *d_out_lla,
	McpDBL *d_out_unc,
	McpU8 Sel_WiFi,
	McpU8 u_gps_pos_valid,
	McpU8 u_wifi_pos_valid,
	McpU32 q_Current_FCount,
	McpU32 q_GPS_FCount
 );



void log_gps_wifi_blender_data(gpsc_ctrl_type* p_zGPSCControl,U8 u_gps_pos_valid,U8 u_wifi_pos_valid,gpsc_report_pos_type  *p_zReportPos,
 gpsc_db_wifi_pos_type *p_z_WifiPos,gpsc_db_blended_pos_type *p_z_BlendedPos);


/************************************************************************
 *
 *   Module functions implementation
 *
 ************************************************************************/
/**
 * \fn     hybrid_gps_wifi_pos_blender
 * \brief  this methos imlement the blending of gps and wifi position
 *
 * \note    Presently it just support simple selection between wifi and gps and
 *             later blender part needs to be added here
 * \param
 * \return 	void
 * \sa
 */
  void hybrid_gps_wifi_pos_blender (gpsc_ctrl_type* p_zGPSCControl,gpsc_sess_specific_cfg_type* p_zSessSpecificCfg )
{

	gpsc_db_pos_type *p_z_DBPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBPos;
	gpsc_report_pos_type  *p_zReportPos  = &p_z_DBPos->z_ReportPos;
	gpsc_db_gps_meas_type*       p_zDBGpsMeas  = &p_zGPSCControl->p_zGPSCDatabase->z_DBGpsMeas;
	gpsc_meas_report_type *p_zMeasReport = &p_zDBGpsMeas->z_MeasReport;
	gpsc_db_wifi_pos_type *p_z_WifiPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBWifiPos;
	gpsc_state_type*           p_zGPSCState      = p_zGPSCControl->p_zGPSCState;

	gpsc_db_blended_pos_type *p_z_BlendedPos = &p_zGPSCControl->p_zGPSCDatabase->z_DBBlendedPos;
	gpsc_db_sensor_calib_report_type *p_zSensorReport = &p_zGPSCControl->p_zGPSCDatabase->z_DBSensorCalibReport;

	DBL d_gps_lla[3]={0,0,0};
	DBL d_wifi_lla[3]={0,0,0};
	DBL d_wifi_ecef[3] = {0,0,0};

	DBL gps_2DUnc = 0;
	U8 u_gps_pos_valid = 0;
	U8 u_wifi_pos_valid = 0;
	U8 Sel_WiFi = 0;

	DBL d_out_lla[3]={0,0,0};
	DBL d_out_unc[3]={0,0,0};
	DBL d_out_blen_unc = 0;
    U8 sel_out = 0;

	//Initialize the 2 msb bit to differentiate wifi and gps fix
	p_zReportPos->w_PositionFlags = p_zReportPos->w_PositionFlags & 0x3FFF; //Bit 15 is GPS and Bit 14 for Wifi

	/* If Internally computed GPS Fix available */
	u_gps_pos_valid = (U8)((p_zReportPos->w_PositionFlags & 0x02 || p_zReportPos->w_PositionFlags & 0x01) && !(p_zReportPos->w_PositionFlags & 0x40));

	/* If available fix is not stale */
	if(u_gps_pos_valid)
	{
		if((p_zMeasReport->q_FCount - p_zReportPos->q_RefFCount) > GPSC_GPS_POS_VALIDITY_THRESHOLD)
		{
            STATUSMSG("hybrid_gps_wifi_pos_blender-GPS Fix Is Stale");
			u_gps_pos_valid = 0;
		}

	}
	if (u_gps_pos_valid) /* If GPS Position available and valid*/
	{
		/* Fill GPS Data in Radians */
		d_gps_lla[0] = (DBL)(p_zReportPos->l_Lat) * (DBL)C_LSB_LAT;
		d_gps_lla[1] = (DBL)(p_zReportPos->l_Long) * (DBL)C_LSB_LON;
		d_gps_lla[2] = (DBL) p_zReportPos->x_Height  / 2;

		/* Calculate GPS 2D uncertainity */
		gps_2DUnc = sqrt( (DBL)p_zReportPos->w_EastUnc * (DBL)p_zReportPos->w_EastUnc +  (DBL)p_zReportPos->w_NorthUnc * (DBL)p_zReportPos->w_NorthUnc);
		gps_2DUnc = gps_2DUnc / 10;
	}

	u_wifi_pos_valid = p_z_WifiPos->u_Valid;

	if(u_wifi_pos_valid)
	{
		if((p_zMeasReport->q_FCount - p_z_WifiPos->q_RefFCount) > GPSC_WIFI_POS_VALIDITY_THRESHOLD)
		{
			u_wifi_pos_valid = 0;
			STATUSMSG("hybrid_gps_wifi_pos_blender-WIFI Fix Is Stale");
		}
	}
	if(u_wifi_pos_valid)
	{

		d_wifi_lla[0] = p_z_WifiPos->d_WifiLatitude ; // may not be needed as this is already in radians* (DBL)C_DEG_TO_RAD;
		d_wifi_lla[1] = p_z_WifiPos->d_WifiLongitude; // * (DBL)C_DEG_TO_RAD;
		d_wifi_lla[2] = p_z_WifiPos->d_WifiAltitude;

		if(u_gps_pos_valid)
		{
			/* wifi Alitude is same as GPS */
			d_wifi_lla[2] =  d_gps_lla[2];
			p_z_WifiPos->d_WifiAltitude = d_gps_lla[2];
			p_z_WifiPos->d_VerticalUnc = p_zReportPos->w_VerticalUnc;
		}

	} /* if(u_wifi_pos_valid) */
	else
	{
		d_wifi_lla[0] = 0;
		d_wifi_lla[1] = 0;
		d_wifi_lla[2] = 0;
	}

    STATUSMSG("hybrid_gps_wifi_pos_blender: Before call Blender :u_wifi_pos_valid= %d, u_gps_pos_valid = %d",
		       u_wifi_pos_valid,u_gps_pos_valid);

if(TRACK1_BLENDER)
  {
		Sel_WiFi = gpsc_wifi_pos_selector(d_gps_lla,d_wifi_lla,u_gps_pos_valid,u_wifi_pos_valid,gps_2DUnc,p_z_WifiPos->d_WifiUnc);

		/* Convert d_gps_lla to Degrees */
		d_gps_lla[0] = (d_gps_lla[0]  * (DBL)C_RAD_TO_DEG);
		   d_gps_lla[1] = (d_gps_lla[1]  * (DBL)C_RAD_TO_DEG);

		if(Sel_WiFi)
		{
			d_out_lla[0] = p_z_WifiPos->d_WifiLatitude * (DBL)C_RAD_TO_DEG;
			d_out_lla[1] = p_z_WifiPos->d_WifiLongitude * (DBL)C_RAD_TO_DEG;
			d_out_lla[2] = p_z_WifiPos->d_WifiAltitude;

			d_out_unc[0] = p_z_WifiPos->d_WifiUnc/sqrt(2);
			d_out_unc[1] = p_z_WifiPos->d_WifiUnc/sqrt(2);
			d_out_unc[2] = p_z_WifiPos->d_VerticalUnc;

			/* indicate the selected position is from wifi*/
			p_zReportPos->w_PositionFlags = p_zReportPos->w_PositionFlags | 0x4000;
			gpsc_app_send_wifi_position_result(p_zGPSCControl,p_zSessSpecificCfg,u_gps_pos_valid);
		}
		else if (u_gps_pos_valid)
		{
			d_out_lla[0] = d_gps_lla[0];
			d_out_lla[1] = d_gps_lla[1];
			d_out_lla[2] = d_gps_lla[2];

			d_out_unc[0] = p_zReportPos->w_EastUnc;
			d_out_unc[1] = p_zReportPos->w_NorthUnc ;
			d_out_unc[2] = p_zReportPos->w_VerticalUnc;

			/* indicate the selected position is from GPS*/
			p_zReportPos->w_PositionFlags = p_zReportPos->w_PositionFlags | 0x8000;
			gpsc_app_send_gps_position_result(p_zGPSCControl,p_zSessSpecificCfg);
   		}

		/* Log Data to File and Logger*/
		log_wifi_selector_data(
								p_zGPSCControl,
								d_gps_lla,
								(McpDBL)p_zReportPos->w_EastUnc / 10,
								(McpDBL)p_zReportPos->w_NorthUnc / 10,
								(McpDBL)p_zReportPos->w_VerticalUnc / 10,
								gps_2DUnc,
								p_z_WifiPos,
								d_out_lla,
								d_out_unc,
								Sel_WiFi,
								u_gps_pos_valid,
								u_wifi_pos_valid,
								p_zMeasReport->q_FCount,
								p_zReportPos->q_RefFCount
								);

   }
 else
   {
   STATUSMSG("hybrid_gps_wifi_pos_blender: TRACK1 Disabled ");

   }



if(TRACK2_BLENDER)
  {

    STATUSMSG("hybrid_gps_wifi_pos_blender: Uses TRACK2 BLENDER ");

	if( ((p_zSessSpecificCfg->u_LocFixPosBitmap & GPS_WIFI_BLEND)== GPS_WIFI_BLEND) && (u_gps_pos_valid ||u_wifi_pos_valid) )
	{
       STATUSMSG("BLENDED POSITION REPORTED");
	   gps_wifi_pos_blender(u_gps_pos_valid,u_wifi_pos_valid,p_zSensorReport,p_zReportPos, p_z_WifiPos,p_z_BlendedPos);

	   /* We need to send the feed back to GPS from HERE if no seesion running only for GPS
	          Also inject the feedback only once per report and not for all the session*/
       if((!check_any_gps_session(p_zGPSCControl))&& (!p_zGPSCState->u_blenderfeedback))
       	{
       	  p_zGPSCState->u_blenderfeedback =TRUE;
	      gpsc_mgp_tx_inject_wlan_assist_info(p_zGPSCControl,p_z_BlendedPos,p_zReportPos);
       	}
	   else
	   	{
	   	  STATUSMSG("No Feedback to GPS");
	   	}


	   /* indicate the selected position is from BLended*/
	   p_zReportPos->w_PositionFlags = p_zReportPos->w_PositionFlags | 0x8000;

	   /* Send the bleneded position out here */
	   gpsc_app_send_blended_position_result( p_zGPSCControl, p_zSessSpecificCfg,u_gps_pos_valid );

	   #ifdef WIFI_DEMO
	   STATUSMSG("gpsc_app_send_gps_wifi_blend_report()...");
	   gpsc_app_send_gps_wifi_blend_report(p_zGPSCControl,p_zSessSpecificCfg,u_gps_pos_valid, u_wifi_pos_valid);
	   #endif


    }
	else if( ((p_zSessSpecificCfg->u_LocFixPosBitmap & WIFI_POS) == WIFI_POS)&& (u_wifi_pos_valid) )
	{

      STATUSMSG("WIFI POITION ALONE REPORTED");
	  p_zReportPos->w_PositionFlags = p_zReportPos->w_PositionFlags | 0x4000;
	  gpsc_app_send_wifi_position_result(p_zGPSCControl,p_zSessSpecificCfg,u_gps_pos_valid);
	}
	else
     STATUSMSG("hybrid_gps_wifi_pos_blender: No Wifi or Blend position Reported");


	log_gps_wifi_blender_data(p_zGPSCControl,u_gps_pos_valid,u_wifi_pos_valid,p_zReportPos, p_z_WifiPos,p_z_BlendedPos);

   }
  else
   {

   STATUSMSG("hybrid_gps_wifi_pos_blender: TRACK2 Disabled  ");

   }

}


/**
 * \fn     gpsc_wifi_pos_selector
 * \brief  this methos imlement the selection of gps and wifi position
 *
 * \note    Presently it just support simple selection between wifi and gps and
 *             later blender part needs to be added here
 *            Input LLA for GPS and WiFi are assumed to be in radians
 * \param
 * \return 	Gps or wifi select indication
 * \sa
 */
U8 gpsc_wifi_pos_selector
(
	DBL d_gps_lla[],
	DBL d_wifi_lla[],
	U8 u_gps_pos_valid,
	U8 u_wifi_pos_valid,
	DBL gps_2DUnc,
	DBL wifi_Unc
)
{
	U8 Sel_WiFi = 0;
	DBL d_Dist_Wifi_GPS_fixes = 0;

    DBL d_gps_ecef[3]= {0,0,0};
    DBL d_wifi_ecef[3] = {0,0,0};

	U8 index = 0;

    if (u_gps_pos_valid && u_wifi_pos_valid) /* If Both GPS and Wifi Position available */
    {

        /* Get GPS position in ECEF Format */
        cd_Lla2Ecef_ind(d_gps_lla,d_gps_ecef);

        /* Get Wifi position in ECEF Format */
        cd_Lla2Ecef_ind(d_wifi_lla,d_wifi_ecef);

/*
	STATUSMSG("gpsc_prepare_wifi_pos_selector: GPS ECEF: X = %lf, Y = %lf, Z = %lf",d_gps_ecef[0],d_gps_ecef[1],d_gps_ecef[2]);
	STATUSMSG("gpsc_prepare_wifi_pos_selector: WiFi ECEF: X = %lf, Y = %lf, Z = %lf",d_wifi_ecef[0],d_wifi_ecef[1],d_wifi_ecef[2]);
*/
        for(index=0;index<3;index++)
            d_Dist_Wifi_GPS_fixes += (d_gps_ecef[index] - d_wifi_ecef[index]) * (d_gps_ecef[index] - d_wifi_ecef[index]);

	STATUSMSG("gpsc_wifi_pos_selector: Distance Between GPS and WiFi Fixes = %lf",sqrt(d_Dist_Wifi_GPS_fixes));

        if((d_Dist_Wifi_GPS_fixes < MAX_DIFF_GPS_WIFI_FIXES * MAX_DIFF_GPS_WIFI_FIXES) && (wifi_Unc < gps_2DUnc))
        {
            Sel_WiFi = 1;
        }
    }
    else if (u_wifi_pos_valid)   /* If Only Wifi Position available */
    {
        Sel_WiFi = 1;
    }

    return Sel_WiFi;
}



/**
 * \fn     cd_Lla2Ecef_ind
 * \brief  This function converts LLA from Radians to ECEF Format
 *
 * \note
 * \param
 * \return 	void
 * \sa
 */

void cd_Lla2Ecef_ind( DBL d_lla[] , DBL d_ecef[] )
{
	DBL	d_n, d_cosLat, d_sinLat, d_cosLon, d_sinLon, d_temp;

	d_cosLat = cos( d_lla[0] );
	d_sinLat = sin( d_lla[0] );
	d_cosLon = cos( d_lla[1] );
	d_sinLon = sin( d_lla[1] );

	/* Compute the radius of curvature along the prime vertical. */

	d_temp = (WGS84_B / WGS84_A) * d_sinLat;

	d_n = WGS84_A / sqrt( d_cosLat * d_cosLat +  d_temp * d_temp );

	d_temp = (d_n + d_lla[2]) * d_cosLat;

	d_ecef[0] = d_temp * d_cosLon;							/* x-axis */
	d_ecef[1] = d_temp * d_sinLon;							/* y-axis */
	d_ecef[2] = (WGS84_R2 * d_n + d_lla[2]) * d_sinLat;		/* z-axis */
}
/* End of cd_Lla2Ecef(). */

/**
 * \fn     cd_Ecef2Lla_ind
 * \brief  This function converts  ECEF Format to LLA in Radians
 *
 * \note
 * \param
 * \return 	void
 * \sa
 */

void cd_Ecef2Lla_ind( DBL d_Ecef[], DBL	d_Lla[] )
{
	DBL d_P, d_R, d_Mu, d_N, d_Smu, d_Cmu, d_Sphi;
	DBL d_X, d_Y, d_Z, d_Phi;

	d_X = d_Ecef[0];
	d_Y = d_Ecef[1];
	d_Z = d_Ecef[2];

	d_P = sqrt( d_X * d_X + d_Y * d_Y );	/* XY distance from center. */
	d_R = sqrt( d_P * d_P + d_Z * d_Z );	/* XYZ distance. */

	if (d_R < 1.0)	/* Bad input position - center of earth. */
	{
		d_Lla[0] = d_Lla[1] = d_Lla[2] = 1.0;	/* Bogus, but safe values. */

		return;
	}

	if (d_P > 1.0)
	{
		d_Mu = atan2( d_Z * (WGS84_1MF + WGS84_E2 * WGS84_A / d_R), d_P );
	}
	else	/* Too near the poles; ATAN2() might fail (library dependent.) */
	{
		if (d_Z > 0.0) d_Mu =  C_PI/2.0;
		else 		   d_Mu = -C_PI/2.0;
	}

	d_Smu = sin( d_Mu );
	d_Cmu = cos( d_Mu );

	/* Latitude computation */

	d_Phi = atan2( d_Z * WGS84_1MF + WGS84_E2 * WGS84_A * d_Smu * d_Smu * d_Smu,
                WGS84_1MF * (d_P - WGS84_E2 * WGS84_A * d_Cmu * d_Cmu * d_Cmu) );

	d_Sphi = sin( d_Phi );

	d_N = WGS84_A / sqrt( 1.0 - WGS84_E2 * d_Sphi * d_Sphi );

	/* Ellipsoidal height (HAE - height above ellipsoid). */

	d_Lla[2] = d_P * cos( d_Phi ) + d_Z * d_Sphi - WGS84_A * WGS84_A / d_N;

	/* Latitude and longitude estimates. */

	d_Lla[0] = d_Phi;

	if (d_P > 1.0)
	{
		d_Lla[1] = atan2( d_Y, d_X );
	}
	else { d_Lla[1] = 0.0; }
}
/* End of cd_Ecef2Lla_ind(). */



/**
 * \fn     check_any_gps_session
 * \brief  Check is any gps session is running
 *
 * \note
 * \param	p_zSessSpecificCfg
 * \return 	void
 * \sa
 */
U8 check_any_gps_session(gpsc_ctrl_type* p_zGPSCControl)
{
   gpsc_sess_specific_cfg_type *p_zSessSpecificCfg, *p_zPrevSessSpecificCfg;
   gpsc_sess_cfg_type*	p_zSessConfig;
   p_zSessConfig = p_zGPSCControl->p_zSessConfig;
   p_zPrevSessSpecificCfg = NULL;
   p_zSessSpecificCfg = NULL;

	/* get node */
    STATUSMSG("check_any_gps_session:Check for any GPS session");
	p_zSessSpecificCfg = p_zSessConfig->p_zSessSpecificCfg;
	/* check if session with this session Id already exists, if so return */
	while(p_zSessSpecificCfg != NULL)
	{
		if((p_zSessSpecificCfg->u_LocFixPosBitmap == GPS_POS_DEFAULT) || (p_zSessSpecificCfg->u_LocFixPosBitmap == GPS_POS))
		{
		  STATUSMSG("check_any_gps_session:There is a GPS Session");
		  return TRUE;
		}
	   p_zSessSpecificCfg = (gpsc_sess_specific_cfg_type *) p_zSessSpecificCfg->p_nextSession;
	} /* while(p_zSessSpecificCfg != NULL) */
	STATUSMSG("check_any_gps_session:NO GPS Session running");
	return FALSE;

}

/**
 * \fn     wifi_log_init
 * \brief  initialize the wifi selector/blender log file
 *
 * \note
 * \param	gpsc control type
 * \return 	void
 * \sa
 */

void wifi_log_init(gpsc_ctrl_type* p_zGPSCControl)
{


	McpU32 u_CurrentTime, u_TimeTemp;
	McpS8 TimeTemp[5];
	McpHalDateAndTime dateAndTimeStruct;
	McpU8 GPSWiFiSelectorLogFile[68]; /* Klocwork Changes */
    McpS8 tempbuff[256];
    McpU16 bufferlength;

    q_wifiindexcount=0;
	MCP_HAL_STRING_StrCpy (GPSWiFiSelectorLogFile, "/data/wifipos/logs/GPS_WiFi_Selector_Log_");
	u_CurrentTime = time(NULL);
	mcpf_ExtractDateAndTime(u_CurrentTime,&dateAndTimeStruct );

	/*year*/
	u_TimeTemp = dateAndTimeStruct.year;
	MCP_HAL_STRING_ItoA(u_TimeTemp,(McpU8 *)TimeTemp);
	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, TimeTemp);
	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, "-");

	/*month*/
	u_TimeTemp = dateAndTimeStruct.month;
	MCP_HAL_STRING_ItoA(u_TimeTemp,(McpU8 *)TimeTemp);
	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, TimeTemp);
	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, "-");

	/*day*/
	u_TimeTemp = dateAndTimeStruct.day;
	MCP_HAL_STRING_ItoA(u_TimeTemp,(McpU8 *)TimeTemp);
	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, TimeTemp);
	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, "_");

	/*hour*/
	u_TimeTemp = dateAndTimeStruct.hour;
	MCP_HAL_STRING_ItoA(u_TimeTemp,(McpU8 *)TimeTemp);
	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, TimeTemp);
	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, "-");

	/*minute*/
	u_TimeTemp = dateAndTimeStruct.minute;
	MCP_HAL_STRING_ItoA(u_TimeTemp,(McpU8 *)TimeTemp);
	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, TimeTemp);
	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, "-");


	/*second*/
	u_TimeTemp = dateAndTimeStruct.second;
	MCP_HAL_STRING_ItoA(u_TimeTemp,(McpU8 *)TimeTemp);
	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, TimeTemp);


	MCP_HAL_STRING_StrCat (GPSWiFiSelectorLogFile, ".txt");


	if (mcpf_file_open(p_zGPSCControl->p_zSysHandlers->hMcpf, (McpU8 *)GPSWiFiSelectorLogFile,
		MCP_HAL_FS_O_TEXT | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_APPEND,
		&p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd) != RES_OK)
	{
		MCPF_REPORT_INFORMATION(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG, ("gpsc_app_loc_fix_req: Failed to open '%s' File",GPSWiFiSelectorLogFile));
		p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd = 0;
	}
	else
	{
		MCPF_REPORT_INFORMATION(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG, ("Created '%s' to log GPS Wifi Selector data",GPSWiFiSelectorLogFile));

		MCP_HAL_STRING_Sprintf(tempbuff,"@log file version HybridPosition 0.5\n");
	       bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	       mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);
	}



}

/**
 * \fn     wifi_log_close
 * \brief  closing the wifi selector/blender log file
 *
 * \note
 * \param	gpsc control type
 * \return 	void
 * \sa
 */

void wifi_log_close(gpsc_ctrl_type* p_zGPSCControl)
{


  MCPF_REPORT_INFORMATION(p_zGPSCControl->p_zSysHandlers->hMcpf, NAVC_MODULE_LOG, ("closing log GPS Wifi Selector data"));
  if(p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd)
  {
  	mcpf_file_close(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd);
  }


}

/**
 * \fn     log_wifi_selector_data
 * \brief  Function for Logging Data to File
 *
 * \note
 * \param
 * \return 	void
 * \sa
 */


void log_wifi_selector_data
(
	gpsc_ctrl_type* p_zGPSCControl,
	McpDBL *d_gps_lla,
	McpDBL d_gps_Unc_east,
	McpDBL d_gps_Unc_north,
	McpDBL d_gps_Unc_vertical,
	McpDBL d_gps_2DUnc,
	gpsc_db_wifi_pos_type* p_z_WifiPos ,
	McpDBL *d_out_lla,
	McpDBL *d_out_unc,
	McpU8 Sel_WiFi,
	McpU8 u_gps_pos_valid,
	McpU8 u_wifi_pos_valid,
	McpU32 q_Current_FCount,
	McpU32 q_GPS_FCount
 )

{
    McpS8 tempbuff[256];
    McpU16 bufferlength;

	DBL  WifiLatitude;
	DBL	 WifiLongitude;
	//DBL	 WifiAltitude;

	WifiLatitude = p_z_WifiPos->d_WifiLatitude * (DBL)C_RAD_TO_DEG ;
	WifiLongitude = p_z_WifiPos->d_WifiLongitude * (DBL)C_RAD_TO_DEG ;

    STATUSMSG("gpsc_app_log_wifi_selector_data: GPS Data: ");
    STATUSMSG("GPS Validity = %d, Current FCount = %d, Reference Fcount = %d",u_gps_pos_valid, q_Current_FCount,q_GPS_FCount);
    STATUSMSG("GPS Location: Lat=%.8lf, Lon=%.8lf, Ht=%.2lf",d_gps_lla[0],d_gps_lla[1],d_gps_lla[2]);
    STATUSMSG("GPS Uncertainity: East=%.2lf, North=%.2lf, Vertical=%.2lf",d_gps_Unc_east,d_gps_Unc_north,d_gps_Unc_vertical);
    STATUSMSG("GPS 2D Uncertainity =%.2lf",d_gps_2DUnc);

    STATUSMSG("gpsc_app_log_wifi_selector_data: Wifi Data: ");
    STATUSMSG("WiFi Validity = %d, Current FCount = %d, Reference Fcount = %d",u_wifi_pos_valid, q_Current_FCount,p_z_WifiPos->q_RefFCount);
    STATUSMSG("WiFi Location: Lat=%.8lf, Lon=%.8lf",WifiLatitude,WifiLongitude);
    STATUSMSG("WiFi 2D Uncertainity =%.2lf",p_z_WifiPos->d_WifiUnc);

    STATUSMSG("gpsc_app_log_wifi_selector_data: Output Data: ");
    STATUSMSG("Output Location: Lat=%.8lf, Lon=%.8lf, Ht=%.8lf",d_out_lla[0],d_out_lla[1],d_out_lla[2]);
    STATUSMSG("Output Uncertainity: East=%.2lf, North=%.2lf, Vertical=%.2lf",d_out_unc[0],d_out_unc[1],d_out_unc[2]);

    if	(Sel_WiFi)
		STATUSMSG("gpsc_app_log_wifi_selector_data: Selection Result = WiFi");
    else if(u_gps_pos_valid)
		STATUSMSG("gpsc_app_log_wifi_selector_data: Selection Result = GPS");
    else
		STATUSMSG("gpsc_app_log_wifi_selector_data: Selection Result = NO VALID LOCATION INFO AVAILABLE");

     if((p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd) && (u_gps_pos_valid || u_wifi_pos_valid))
     {


	       McpU32 u_CurrentTime;
	       McpHalDateAndTime dateAndTimeStruct;

	      	u_CurrentTime = time(NULL);
	      	mcpf_ExtractDateAndTime(u_CurrentTime,&dateAndTimeStruct );

		/* Write Date */
		MCP_HAL_STRING_Sprintf(tempbuff,"%d-%d-%d,",dateAndTimeStruct.year,dateAndTimeStruct.month,dateAndTimeStruct.day);
	       bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	       mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

		/* Write UTC Time */
		MCP_HAL_STRING_Sprintf(tempbuff,"%d:%d:%d,",dateAndTimeStruct.hour,dateAndTimeStruct.minute,dateAndTimeStruct.second);
	       bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	       mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);


		if(u_gps_pos_valid)
		{
			/* Write GPS LLA */
              	MCP_HAL_STRING_Sprintf(tempbuff,"%.8lf,%.8lf,%.8lf,",d_gps_lla[0],d_gps_lla[1],d_gps_lla[2]);
            	 	bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     			mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

			/* Write GPS Uncertainity */
	              MCP_HAL_STRING_Sprintf(tempbuff,"%.8lf,%.8lf,%.8lf,",d_gps_Unc_east,d_gps_Unc_north,d_gps_Unc_vertical);
	              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
							(McpU8 *)tempbuff, bufferlength);
		}
		else
		{

			/* Write GPS LLA */
             		MCP_HAL_STRING_Sprintf(tempbuff,",,,,,,");
            	 	bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     			mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

		}
		if(u_wifi_pos_valid)
		{
			/* Write Wifi LLA */
	              MCP_HAL_STRING_Sprintf(tempbuff,"%.8lf,%.8lf,",WifiLatitude,WifiLongitude);
	              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
							(McpU8 *)tempbuff, bufferlength);

			/* Write Wifi Uncertainity */
	              MCP_HAL_STRING_Sprintf(tempbuff,"%.8lf,",p_z_WifiPos->d_WifiUnc);
	              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
							(McpU8 *)tempbuff, bufferlength);
		}
		else
		{

			/* Write Wifi LLA */
	              MCP_HAL_STRING_Sprintf(tempbuff,",,,");
	              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
							(McpU8 *)tempbuff, bufferlength);

		}
		/* Write Selected LLA */
              MCP_HAL_STRING_Sprintf(tempbuff,"%.8lf,%.8lf,%.8lf,",d_out_lla[0],d_out_lla[1],d_out_lla[2]);
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

		/* Write Selected Uncertainity */
              MCP_HAL_STRING_Sprintf(tempbuff,"%.8lf,%.8lf,%.8lf,",d_out_unc[0],d_out_unc[1],d_out_unc[2]);
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

		/* Write Selection Result */
		if(Sel_WiFi)
	              MCP_HAL_STRING_Sprintf(tempbuff,"WiFi,\n");
		else
	              MCP_HAL_STRING_Sprintf(tempbuff,"GPS ,\n");
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

     }
}



/**
 * \fn     log_gps_wifi_blender_data
 * \brief  Function for Logging blender track 2 Data to File
 *
 * \note
 * \param
 * \return 	void
 * \sa
 */


void log_gps_wifi_blender_data(gpsc_ctrl_type* p_zGPSCControl,U8 u_gps_pos_valid,U8 u_wifi_pos_valid,gpsc_report_pos_type  *p_zReportPos,
 gpsc_db_wifi_pos_type *p_z_WifiPos,gpsc_db_blended_pos_type *p_z_BlendedPos)
{
    McpS8 tempbuff[256];
    McpU16 bufferlength;

	gpsc_db_gps_meas_type*       p_zDBGpsMeas  = &p_zGPSCControl->p_zGPSCDatabase->z_DBGpsMeas;
	gpsc_meas_report_type *p_zMeasReport = &p_zDBGpsMeas->z_MeasReport;
    McpU32 temptime =0;
	DBL d_gps_lla[3]={0,0,0};
	DBL d_wifi_lla[3]={0,0,0};
	DBL d_gps_unc[3]={0,0,0};
	DBL d_blend_un[3]= {0,0,0};

	DBL d_blend_lla[3]={0,0,0};


	if (u_gps_pos_valid) /* If GPS Position available and valid*/
	{
		/* Fill GPS Data in Degrees */
		d_gps_lla[0] = (DBL)(p_zReportPos->l_Lat) * (DBL)C_LSB_LAT;
		d_gps_lla[1] = (DBL)(p_zReportPos->l_Long) * (DBL)C_LSB_LON;
		d_gps_lla[2] = (DBL) p_zReportPos->x_Height  / 2;

		/* Convert d_gps_lla to Degrees */
		d_gps_lla[0] = (d_gps_lla[0]  * (DBL)C_RAD_TO_DEG);
		d_gps_lla[1] = (d_gps_lla[1]  * (DBL)C_RAD_TO_DEG);

		d_gps_unc[0] = (DBL)p_zReportPos->w_EastUnc/10;
		d_gps_unc[1] = (DBL)p_zReportPos->w_NorthUnc/10 ;
		d_gps_unc[2] = (DBL)p_zReportPos->w_VerticalUnc/2;

		//d_gps_Ve[0] = (DBL)p_zReportPos->x_VelEast/10;
		//d_gps_Vel[1] = (DBL)p_zReportPos->w_NorthUnc/10 ;
		//d_gps_Vel[2] = (DBL)p_zReportPos->w_VerticalUnc/2;
	}

	if(u_wifi_pos_valid)
	{

	   /* Fill WIFI Data in Degrees */
	   d_wifi_lla[0] = (DBL)(p_z_WifiPos->d_WifiLatitude) * (DBL)C_RAD_TO_DEG;
	   d_wifi_lla[1] = (DBL)(p_z_WifiPos->d_WifiLongitude) * (DBL)C_RAD_TO_DEG;
       d_wifi_lla[2] = (DBL) (p_z_WifiPos->d_WifiAltitude);
	}

	d_blend_lla[0] = (DBL)(p_z_BlendedPos->d_BlendLatitude) * (DBL)C_RAD_TO_DEG;
	d_blend_lla[1] = (DBL)(p_z_BlendedPos->d_BlendLongitude) * (DBL)C_RAD_TO_DEG;
    d_blend_lla[2] = (DBL)(p_z_BlendedPos->d_BlendAltitude);

	d_blend_un[0] = (DBL)(p_z_BlendedPos->d_BlendEastUnc);
	d_blend_un[1] = (DBL)(p_z_BlendedPos->d_BlendNorthUnc);
	d_blend_un[2] = (DBL)(p_z_BlendedPos->d_VerticalUnc);


    STATUSMSG("log_gps_wifi_blender_data: Logging wifi,gps and blender data");

    if(u_gps_pos_valid || u_wifi_pos_valid)
    {

		STATUSMSG("GPS Location: Lat=%.8lf, Lon=%.8lf, Ht=%.2lf",d_gps_lla[0],d_gps_lla[1],d_gps_lla[2]);
		STATUSMSG("GPS Uncertainity: East=%.2lf, North=%.2lf, Vertical=%.2lf",d_gps_unc[0],d_gps_unc[1],d_gps_unc[0]);

		STATUSMSG("WiFi Location: Lat=%.8lf, Lon=%.8lf",d_wifi_lla[0],d_wifi_lla[1]);
		STATUSMSG("WiFi 2D Uncertainity =%.2lf",p_z_WifiPos->d_WifiUnc);

		STATUSMSG("gpsc_app_log_wifi_selector_data: Output Data: ");
		STATUSMSG("Bleneded Location: Lat=%.8lf, Lon=%.8lf, Ht=%.8lf",d_blend_lla[0],d_blend_lla[1],d_blend_lla[2]);
		STATUSMSG("Bleneded Uncertainity: East=%.2lf, North=%.2lf, Vertical=%.2lf",d_blend_un[0],d_blend_un[1],d_blend_un[2]);
    }
	else
	 STATUSMSG("log_gps_wifi_blender_data: Selection Result = NO VALID LOCATION INFO AVAILABLE");




	if((p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd) && (u_gps_pos_valid || u_wifi_pos_valid))
     {


	       McpU32 u_CurrentTime;
	       McpHalDateAndTime dateAndTimeStruct;

	      	u_CurrentTime = time(NULL);
	      	mcpf_ExtractDateAndTime(u_CurrentTime,&dateAndTimeStruct );
#if 0

		/* Write Date */
		MCP_HAL_STRING_Sprintf(tempbuff,"%d-%d-%d,",dateAndTimeStruct.year,dateAndTimeStruct.month,dateAndTimeStruct.day);
	       bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	       mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

		/* Write UTC Time */
		MCP_HAL_STRING_Sprintf(tempbuff,"%d:%d:%d,",dateAndTimeStruct.hour,dateAndTimeStruct.minute,dateAndTimeStruct.second);
	       bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	       mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);
#endif


		if(u_gps_pos_valid)
		{
			/* Write GPS LLA */
              	MCP_HAL_STRING_Sprintf(tempbuff,"%.8lf,%.8lf,%.8lf,",d_gps_lla[0],d_gps_lla[1],d_gps_lla[2]);
            	 	bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     			mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

			/* Write Special Message */
				MCP_HAL_STRING_Sprintf(tempbuff,"SpecialMessage,GPS,");
				   bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
				   mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
								(McpU8 *)tempbuff, bufferlength);

				/* Write UTC Time */
				MCP_HAL_STRING_Sprintf(tempbuff,"index=%d,TIME:%d:%d:%d,",q_wifiindexcount,dateAndTimeStruct.hour,dateAndTimeStruct.minute,dateAndTimeStruct.second);
				   bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
				   mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
								(McpU8 *)tempbuff, bufferlength);


			/* Write GPS Uncertainity */
	              MCP_HAL_STRING_Sprintf(tempbuff,"EastUnc:%.4lf,NorthUnc:%.4lf,VerticalUnc:%.4lf,",d_gps_unc[0],d_gps_unc[1],d_gps_unc[2]);
	              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
							(McpU8 *)tempbuff, bufferlength);

			/* Write GPS Velocity and Unc */
              MCP_HAL_STRING_Sprintf(tempbuff,"EastVel:%hd,NorthVel:%hd,VerticalVel:%hd,VelUnc:%d,",p_zReportPos->x_VelEast,p_zReportPos->x_VelNorth,p_zReportPos->x_VelVert,p_zReportPos->w_VelUnc);
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		  mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

			  /* Write message end */
              MCP_HAL_STRING_Sprintf(tempbuff,"end,GPSMsec=%d,GPSWeek=%d,ColorCode=1,\n",p_zMeasReport->z_MeasToa.q_GpsMsec,p_zMeasReport->z_MeasToa.w_GpsWeek);
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		  mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);
		}
		else
		{

			/* Write GPS LLA */
             		MCP_HAL_STRING_Sprintf(tempbuff,"NO GPS FIX AVAILABLE,");
            	 	bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     			mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);
				   	/* Write UTC Time */
				MCP_HAL_STRING_Sprintf(tempbuff,"index=%d,TIME:%d:%d:%d,\n",q_wifiindexcount,dateAndTimeStruct.hour,dateAndTimeStruct.minute,dateAndTimeStruct.second);
				   bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
				   mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
								(McpU8 *)tempbuff, bufferlength);

		}
		if(u_wifi_pos_valid)
		{
			/* Write Wifi LLA */
	              MCP_HAL_STRING_Sprintf(tempbuff,"%.8lf,%.8lf,0,",d_wifi_lla[0],d_wifi_lla[1]);
	              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
							(McpU8 *)tempbuff, bufferlength);

				/* Write Special Message */
				MCP_HAL_STRING_Sprintf(tempbuff,"SpecialMessage,WIFI,");
				   bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
				   mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
								(McpU8 *)tempbuff, bufferlength);

				   	/* Write UTC Time */
				MCP_HAL_STRING_Sprintf(tempbuff,"index=%d,TIME:%d:%d:%d,",q_wifiindexcount,dateAndTimeStruct.hour,dateAndTimeStruct.minute,dateAndTimeStruct.second);
				   bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
				   mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
								(McpU8 *)tempbuff, bufferlength);

			/* Write Wifi Uncertainity */
	              MCP_HAL_STRING_Sprintf(tempbuff,"Wifi2DUnc:%.4lf,EastUnc: %.4lf,NorthUnc: %.4lf,VertUnc: %.4lf,",p_z_WifiPos->d_WifiUnc,p_z_WifiPos->d_WiFiEastUnc,p_z_WifiPos->d_WiFiNorthUnc,p_z_WifiPos->d_VerticalUnc);
	              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
							(McpU8 *)tempbuff, bufferlength);

				/* Write Wifi Velocity and Unc */
	              MCP_HAL_STRING_Sprintf(tempbuff,"EastVel:%.4lf,NorthVel: %.4lf,EastVelUnc: %.4lf,NorthVelUnc: %.4lf,",p_z_WifiPos->d_WiFiEastVel,p_z_WifiPos->d_WiFiNorthVel,p_z_WifiPos->d_WiFiEastVelUnc,p_z_WifiPos->d_WiFiNorthVelUnc);
	              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
							(McpU8 *)tempbuff, bufferlength);

				/* no of aps and kalman usage */
	              MCP_HAL_STRING_Sprintf(tempbuff,"KFUsage: %d,PropagatedFix: %d,No AP used: %d time at scan:%u,",p_z_WifiPos->FixFromKalmanFilter,p_z_WifiPos->FixPropagated,p_z_WifiPos->u_NumOfApUsed,p_z_WifiPos->q_timeOfWifiPosition);
	              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
							(McpU8 *)tempbuff, bufferlength);

				/* calculate the GPS Msec corresponds to wifi position */
				/* Scan Time - Selected wifi pos time * 1000 - current GPS msec*/

				temptime = p_zMeasReport->z_MeasToa.q_GpsMsec - ((p_z_WifiPos->q_sytemtimeinmsec - p_z_WifiPos->q_timeOfWifiPosition)*1000);

				/* Write End Message */
              MCP_HAL_STRING_Sprintf(tempbuff,"end,GPSMsec=%u,GPSWeek=%d,ColorCode=2,\n",temptime,p_zMeasReport->z_MeasToa.w_GpsWeek);
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		  mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);
		}
		else
		{

			/* Write Wifi LLA */
	              MCP_HAL_STRING_Sprintf(tempbuff,"NO WIFI FIX AVAILABLE,");
	              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
	     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
							(McpU8 *)tempbuff, bufferlength);
			   	/* Write UTC Time */
				MCP_HAL_STRING_Sprintf(tempbuff,"index=%d,TIME:%d:%d:%d,\n",q_wifiindexcount,dateAndTimeStruct.hour,dateAndTimeStruct.minute,dateAndTimeStruct.second);
				   bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
				   mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
								(McpU8 *)tempbuff, bufferlength);

		}
		/* Write Selected LLA */
              MCP_HAL_STRING_Sprintf(tempbuff,"%.8lf,%.8lf,%.8lf,",d_blend_lla[0],d_blend_lla[1],d_blend_lla[2]);
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

			/* Write Special Message */
				MCP_HAL_STRING_Sprintf(tempbuff,"SpecialMessage,BLENDED,");
				   bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
				   mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
								(McpU8 *)tempbuff, bufferlength);

				   	/* Write UTC Time */
				MCP_HAL_STRING_Sprintf(tempbuff,"index=%d,TIME:%d:%d:%d,",q_wifiindexcount,dateAndTimeStruct.hour,dateAndTimeStruct.minute,dateAndTimeStruct.second);
				   bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
				   mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
								(McpU8 *)tempbuff, bufferlength);

		/* Write Selected Uncertainity */
              MCP_HAL_STRING_Sprintf(tempbuff,"Blend2Dunc:%.4lf,EastUnc:%.4lf,NorthUnc%.4lf,vertUnc%.4lf,2Dor3DFix=%d,",p_z_BlendedPos->d_BlendUnc,d_blend_un[0],d_blend_un[1],d_blend_un[2],p_z_BlendedPos->u_2Dor3Dfix);
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

		 /* time information*/
			if(u_gps_pos_valid)
			{
               MCP_HAL_STRING_Sprintf(tempbuff,"GPSMsec corresponding to GPS fix:%d,Fcount:%d,",p_zMeasReport->z_MeasToa.q_GpsMsec,p_zMeasReport->q_FCount);
               bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		   mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);
			}
			if(u_wifi_pos_valid)
		    {
		       MCP_HAL_STRING_Sprintf(tempbuff,"System Time at which wifi fix selected:%u, Fcount:%d,",p_z_WifiPos->q_sytemtimeinmsec,p_z_WifiPos->q_RefFCount);
               bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		   mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);
			}

			/* Write End Message */
              MCP_HAL_STRING_Sprintf(tempbuff,"end,GPSMsec=%u,GPSWeek=%d,ColorCode=3,\n",p_zMeasReport->z_MeasToa.q_GpsMsec,p_zMeasReport->z_MeasToa.w_GpsWeek);
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		  mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);

		#if 0
	              MCP_HAL_STRING_Sprintf(tempbuff,"GPS ,\n");
              bufferlength = MCP_HAL_STRING_StrLen(tempbuff);
     		mcpf_file_write(p_zGPSCControl->p_zSysHandlers->hMcpf, p_zGPSCControl->p_zSysHandlers->uGPSWifiSelectorFd,
						(McpU8 *)tempbuff, bufferlength);
		#endif
			q_wifiindexcount++;

		}
}


/**
 * \fn     gpsc_mgp_rx_sensor_calib_data_for_blend
 * \brief  Function for providing sensor PE data. Blender uses some parameter out of this
 *
 * \note
 * \param
 * \return 	void
 * \sa
 */

void gpsc_mgp_rx_sensor_calib_data_for_blend(U8 *p_B)
{


    //gp_zSensorReport = &z_SensorReport;
    gpsc_ctrl_type*	p_zGPSCControl = gp_zGPSCControl;
	gpsc_db_sensor_calib_report_type *p_zSensorReport = &p_zGPSCControl->p_zGPSCDatabase->z_DBSensorCalibReport;
	STATUSMSG("gpsc_mgp_rx_sensor_calib_data_for_blend: Received Sensor PE Data: ");
	/* Fcount */
    p_zSensorReport->q_RefFCount = (U32)NBytesGet(&p_B, sizeof(U32));

	p_zSensorReport->w_StatusFlags = (U16)NBytesGet(&p_B, sizeof(U16));


	/* Latitude */
	p_zSensorReport->f_Latitude = (FLT)NBytesGet(&p_B, sizeof(S32));
	//p_zSensorReport.f_Latitude = (gp_zSensorReport.f_Latitude * 180)/pow(2,32);

	/* Longitude */
	p_zSensorReport->f_Longitude = (FLT)NBytesGet(&p_B, sizeof(S32));
	//p_zSensorReport.f_Longitude = (gp_zSensorReport.f_Longitude * 180)/pow(2,31);

	/* Altitude */
	p_zSensorReport->f_Altitude = (FLT)NBytesGet(&p_B, sizeof(S16));
	//p_zSensorReport->f_Altitude *= 0.5;

	/* East Unc */
	p_zSensorReport->f_EastUncertainty = (FLT)NBytesGet(&p_B, sizeof(U16));
	//p_zSensorReport->f_EastUncertainty *= 0.1;

	/* North Unc */
	p_zSensorReport->f_NorthUncertainty= (FLT)NBytesGet(&p_B, sizeof(U16));
	//p_zSensorReport->f_NorthUncertainty *= 0.1;

	/* Vertical Postition Unc */
	p_zSensorReport->f_VerticalUncertainty = (FLT)NBytesGet(&p_B, sizeof(U16));
	//p_zSensorReport->f_VerticalUncertainty *= 0.5;

	/* East Velocity */
	p_zSensorReport->f_EastVelocity = (FLT)NBytesGet(&p_B, sizeof(S16));
	//p_zSensorReport->f_EastVelocity *= (1000.0/65536.0);

	/* North Velocity */
	p_zSensorReport->f_NorthVelocity = (FLT)NBytesGet(&p_B, sizeof(S16));
	//p_zSensorReport->f_NorthVelocity *= (1000.0/65536.0);

	/* Vertical Velocity */
	p_zSensorReport->f_VerticalVelocity = (FLT)NBytesGet(&p_B, sizeof(S16));
	//p_zSensorReport->f_VerticalVelocity *= (1000.0/65536.0);

	/* East Velocity Unc */
	p_zSensorReport->f_VelEastUnc = (FLT)NBytesGet(&p_B, sizeof(U16));
	//p_zSensorReport->f_VelEastUnc *= (1000.0/65536.0);

	/* North Velocity Unc */
	p_zSensorReport->f_VelNorthUnc = (FLT)NBytesGet(&p_B, sizeof(U16));
	//p_zSensorReport->f_VelNorthUnc *= (1000.0/65536.0);

	/* Vertical Velocity Unc */
	p_zSensorReport->f_VelVerticalUnc = (FLT)NBytesGet(&p_B, sizeof(U16));
	//p_zSensorReport->f_VelVerticalUnc *= (1000.0/65536.0);

	/* Number of SVs used in Position Fix */
	p_zSensorReport->u_NumSvPosFix = (U8)NBytesGet(&p_B, sizeof(U8));

	/* Number of SVs used in Velocity Fix */
	p_zSensorReport->u_NumSvVelFix = (U8)NBytesGet(&p_B, sizeof(U8));


	/* NumSvPosFixPreRaim */
	p_zSensorReport->u_NumSvPosFixPreRaim = (U8)NBytesGet(&p_B, sizeof(U8));

	/* NumSvVelFixPreRaim */
	p_zSensorReport->u_NumSvVelFixPreRaim = (U8)NBytesGet(&p_B, sizeof(U8));

	/* MP Level Indicator */
	p_zSensorReport->u_MpLevelIndicator = (U8)NBytesGet(&p_B, sizeof(U8));
	//gp_zSensorReport->u_MpLevelIndicator *= (10.0/254.0);

	/* Max Pos Error */
	p_zSensorReport->u_MaxPosError = (U8)NBytesGet(&p_B, sizeof(U8));
	//gp_zSensorReport->u_MaxPosError *= (50.0/254.0);

	/* Max Vel Error */
	p_zSensorReport->u_MaxVelError = (U8)NBytesGet(&p_B, sizeof(U8));
	//gp_zSensorReport->u_MaxVelError *= (50.0/254.0);
    STATUSMSG("gpsc_mgp_rx_sensor_calib_data_for_blend: MP Level IND:%d ",p_zSensorReport->u_MaxVelError);
}

