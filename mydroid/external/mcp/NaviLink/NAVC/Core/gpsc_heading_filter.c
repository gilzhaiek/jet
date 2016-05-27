#include <math.h>
#include <stdio.h>
#include "gpsc_hdgfilter.h"
#include "gpsc_msg.h"
#include <utils/Log.h>
/*
 ******************************************************
 * gpsc_HdgFiltInit()
 * Description - Initialize the heading filter global
 * state variables
 * Input - none
 * Output - none
 *******************************************************
 */
/*HdgFiltStateVar z_HdgFiltStateVar = {

	.f_StateNoise   = 50.0,
	.f_StateNew     = 0,
	.f_StatePrev    = 0,
	.u_FilterStart  = 1,
	.q_FCount       = 1

};
*/
HdgFiltStateVar z_HdgFiltStateVar= {50.0,0,0,1,1};

void gpsc_HdgFiltInit(void)
{
	STATUSMSG("gpsc_HdgFiltInit: Entering");	


	z_HdgFiltStateVar.f_StateNoise  = 50.0;
	z_HdgFiltStateVar.f_StateNew    = 0;
	z_HdgFiltStateVar.f_StatePrev   = 0;
	z_HdgFiltStateVar.u_FilterStart = 1;

	STATUSMSG("gpsc_HdgFiltInit: z_HdgFiltStateVar.f_StateNoise   [%f]\n",z_HdgFiltStateVar.f_StateNoise);
	STATUSMSG("gpsc_HdgFiltInit: z_HdgFiltStateVar.f_StateNew     [%f]\n",z_HdgFiltStateVar.f_StateNew);
	STATUSMSG("gpsc_HdgFiltInit: z_HdgFiltStateVar.f_StatePrev    [%f]\n",z_HdgFiltStateVar.f_StatePrev);
	STATUSMSG("gpsc_HdgFiltInit: z_HdgFiltStateVar.u_FilterStart  [%d]\n",z_HdgFiltStateVar.u_FilterStart);
	STATUSMSG("gpsc_HdgFiltInit: Exiting");	
}

/*
 *********************************************************
 * gpsc_HdgFilter
 * - implements the Kalamn filter for heading filter
 *
 * Input - pointers to current velocity and new velocity
 * velocity is in m/s and heading in deg
 *
 * output - None
 *********************************************************
 */

void gpsc_HdgFilter(VelocityVar* p_CurrentVelocityVar, VelocityVar* p_FilteredVelocityVar)
{
	FLT f_tempHdg = 0, f_DeltaHdg = 0;
	DBL f_AbsVel = 0, f_RNoise = 0, f_QNoise = 0, f_KState = 0;

	STATUSMSG("gpsc_HdgFilter: Entering\n");

	STATUSMSG("gpsc_HdgFilter: z_HdgFiltStateVar.f_StateNoise   [%f]\n",z_HdgFiltStateVar.f_StateNoise);
	STATUSMSG("gpsc_HdgFilter: z_HdgFiltStateVar.f_StateNew     [%f]\n",z_HdgFiltStateVar.f_StateNew);
	STATUSMSG("gpsc_HdgFilter: z_HdgFiltStateVar.f_StatePrev    [%f]\n",z_HdgFiltStateVar.f_StatePrev);
	STATUSMSG("gpsc_HdgFilter: z_HdgFiltStateVar.u_FilterStart  [%d]\n",z_HdgFiltStateVar.u_FilterStart);

	// find the absolute horrizontal velocity using the N and E velocity components
	f_AbsVel = sqrt(pow(p_CurrentVelocityVar->f_VelEast,2) + pow(p_CurrentVelocityVar->f_VelNorth, 2));

	// maintain a copy of the current heading
	f_tempHdg = p_CurrentVelocityVar->f_Heading;

	// Check if this is the first sample in this session
	// If yes initialize the heading filter variables
	// Also we may need to include a check based on f_count to
	// detrmine if we need to reset the filter as there is a discontinuity
	// in received fixes

	if (z_HdgFiltStateVar.u_FilterStart == 1)
	{
		STATUSMSG("gpsc_HdgFilter: u_FilterStart == 1\n");
		gpsc_HdgFiltInit();
		z_HdgFiltStateVar.u_FilterStart ++;
	}

	// if the velocity is less than 0.5m/s propagate the previous heading
	// retain the current velocity

	if (f_AbsVel < 0.5)
	{
		STATUSMSG("gpsc_HdgFilter: f_AbsVel < 0.5 [%f]\n",f_AbsVel);
		p_FilteredVelocityVar->f_Heading = (FLT)z_HdgFiltStateVar.f_StatePrev;
		p_FilteredVelocityVar->f_VelEast = p_CurrentVelocityVar->f_VelEast;
		p_FilteredVelocityVar->f_VelNorth = p_CurrentVelocityVar->f_VelNorth;
	}
	else
	{
		STATUSMSG("gpsc_HdgFilter: f_AbsVel >= 0.5 [%f]\n",f_AbsVel);
		f_DeltaHdg = f_tempHdg - (FLT)z_HdgFiltStateVar.f_StatePrev;

		if(f_DeltaHdg < -180.0)
		{
			f_DeltaHdg += 360.0;
		}
		else if (f_DeltaHdg > 180.0)
		{
			f_DeltaHdg -= 360.0;
		}

		f_RNoise = 4.0 / pow(f_AbsVel,2);
		//%%%%%%%%%% Kalman Filter Starts Here  %%%%%
		f_QNoise = (f_AbsVel < 2.0) ? QNOISE_FACT1 : QNOISE_FACT2;

		z_HdgFiltStateVar.f_StateNoise += f_QNoise;
		f_KState = z_HdgFiltStateVar.f_StateNoise /(f_RNoise + z_HdgFiltStateVar.f_StateNoise);
		z_HdgFiltStateVar.f_StateNew = z_HdgFiltStateVar.f_StatePrev + (f_KState * f_DeltaHdg);
		z_HdgFiltStateVar.f_StateNoise = (pow((1 - f_KState),2)* z_HdgFiltStateVar.f_StateNoise) + (pow(f_KState,2) * f_RNoise);

		z_HdgFiltStateVar.f_StatePrev = z_HdgFiltStateVar.f_StateNew;

		p_FilteredVelocityVar->f_Heading = (FLT)z_HdgFiltStateVar.f_StateNew;

		// compute new velocity based on filtered heading
		p_FilteredVelocityVar->f_VelEast = (FLT)((DBL)f_AbsVel * sin(p_FilteredVelocityVar->f_Heading * (PI/180.0)));
		p_FilteredVelocityVar->f_VelNorth = (FLT)((DBL)f_AbsVel * cos(p_FilteredVelocityVar->f_Heading * (PI/180.0)));
	}

	if (p_FilteredVelocityVar->f_Heading < 0)
		p_FilteredVelocityVar->f_Heading += 360;
	else if (p_FilteredVelocityVar->f_Heading >= 360.0)
		p_FilteredVelocityVar->f_Heading -= 360;

	z_HdgFiltStateVar.q_FCount = p_CurrentVelocityVar->q_FCount;

	STATUSMSG("gpsc_HdgFilter: z_HdgFiltStateVar.f_StateNoise   [%f]\n",z_HdgFiltStateVar.f_StateNoise);
	STATUSMSG("gpsc_HdgFilter: z_HdgFiltStateVar.f_StateNew     [%f]\n",z_HdgFiltStateVar.f_StateNew);
	STATUSMSG("gpsc_HdgFilter: z_HdgFiltStateVar.f_StatePrev    [%f]\n",z_HdgFiltStateVar.f_StatePrev);
	STATUSMSG("gpsc_HdgFilter: z_HdgFiltStateVar.u_FilterStart  [%d]\n",z_HdgFiltStateVar.u_FilterStart);

	STATUSMSG("gpsc_HdgFilter: Exiting\n");
}

