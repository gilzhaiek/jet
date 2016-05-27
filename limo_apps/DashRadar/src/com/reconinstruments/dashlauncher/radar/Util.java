package com.reconinstruments.dashlauncher.radar;

import java.util.ArrayList;

import javax.vecmath.Tuple2d;
import javax.vecmath.Vector2d;

import com.reconinstruments.dashlauncher.radar.prim.Vector3;

import android.graphics.PointF;
import android.util.Log;

public class Util {
	private final static String TAG = "Util";
	
	// ----------------------- //
	// -- Render operations -- //
	// ----------------------- //
	public static final float PITCH_MAX			= -20;
	public static final float PITCH_MIN			= -55;
	public static final float PITCH_OFFSET		= -25;
	public static float mZoomMin				= 350;
	public static float mZoomMax				= 1250;
	public static float mZoomDlt				= mZoomMax - mZoomMin;
	protected static final float YAW_FILTER 	= 0.80f;
	protected static final float PITCH_FILTER 	= 0.80f;
	
	protected static Vector3 mHelperVectorA = new Vector3();
	protected static Vector3 mHelperVectorB = new Vector3();
		
	public static void SetZoomLevels(float zoomMin, float zoomMax) {
		mZoomMin  = zoomMin;
		mZoomMax  = zoomMax;
		mZoomDlt  = mZoomMax - mZoomMin;		
	}
	
	public static float GetLocalDistanceBetweenPositions(PointF p1, PointF p2)
	{
		return mHelperVectorA.set(p1.x, p1.y, 0.0f).sub(mHelperVectorB.set(p2.x, p2.y, 0.0f)).magnitude();
	}
	
	public static float GetLocalDistanceBetweenPositions(Vector3 p1, Vector3 p2)
	{
		return mHelperVectorA.set(p1).sub(p2).magnitude();
	}
	
	// Casts a pitch value from one range to another
	// Example: For a initial range of 0 -> 60, a pitch of 30 for a cast range of -30 -> 0 will return -15
	public static float getCastPitch(float pitch, float initMax, float initMin, float max, float min)
	{
		
		if(max < -90.0f) max = -90.0f;
		if(min < -90.0f) min = -90.0f;
		
		//Log.v(TAG,"pitch="+pitch);
		
		if(pitch >= initMax){ return max; }
		if(pitch <= initMin){ return min; }
		
		float ratio = (pitch - initMin)/(initMax - initMin);
		
		
		return min + ratio*(max - min);
	}
	
	// Returns a *smart* pitch value
	public static float getSmartPitch(float pitch)
	{
		return Math.min(PITCH_MAX, Math.max(PITCH_MIN, (pitch + PITCH_OFFSET)));		
	}
	
	public static float getPitchZOffset(float pitch)
	{
		float newZOffset = mZoomMin; // Default zoom		
		newZOffset	    += mZoomDlt*((PITCH_MAX + pitch)/PITCH_MAX);		
		return newZOffset;
	}
	
	public static float getZoomYOffset(float zoom)
	{
		return 120*(zoom/mZoomMin);
	}
	
	public static float getPitchYOffset(float pitch)
	{
		return 120*(pitch/PITCH_MIN);		
	}
	
	public static float getPanRatio(float zoom)
	{
		return zoom/(mZoomMax*2);
	}
	
	public static float getYaw(Vector3 v1, Vector3 v2)
	{
		//return (Util.radiansToDegrees((float)-Math.atan2(v1.y - v2.y, v1.x - v2.x)) + 90)%360;
		return (radiansToDegrees((float)-Math.atan2(v1.y - v2.y, v1.x - v2.x)) + 90)%360;
	}
	
	public static float getYaw(Vector3 v)
	{
		//return (Util.radiansToDegrees((float)-Math.atan2(v1.y - v2.y, v1.x - v2.x)) + 90)%360;
		return (radiansToDegrees((float)-Math.atan2(v.y, v.x)) + 90)%360;
	}

	public static float radiansToDegrees(float rads)
	{
		float PI2 = 2 * 3.14159f;
		
		// Correct for when signs are reversed.
		if(rads < 0){ rads += PI2; }

		// Check for wrap due to addition of declination.
		if(rads > PI2){ rads -= PI2; }

		// Convert radians to degrees for readability.
		return rads * (180 / 3.14159f);
	}
	
	public static float degreesToRadians(float degs)
	{
		float PI2 = 2 * 3.14159f;

		// Convert radians to degrees for readability.
		return degs / (180 / 3.14159f);
	}
	
	public static float lowPassPitchFilter(float pitch, float oldPitch){
		return oldPitch*PITCH_FILTER + pitch*(1-PITCH_FILTER);
	}

	public static float lowPassYawFilter(float yaw, float oldYaw){		
		float returnYaw;
		if (oldYaw-yaw>180){
			//Log.v(TAG,"oldYaw("+oldYaw+")-yaw("+yaw+")>180");
			returnYaw= oldYaw*YAW_FILTER+(yaw+360)*(1-YAW_FILTER);
			while(returnYaw > 360){ returnYaw -= 360; }  
			while(returnYaw <   0){ returnYaw += 360; }
		}
		else if(oldYaw-yaw<-180){
			//Log.v(TAG,"oldYaw("+oldYaw+")-yaw("+yaw+")<-180");
			returnYaw= oldYaw*YAW_FILTER+(yaw-360)*(1-YAW_FILTER);
			while(returnYaw > 360){ returnYaw -= 360; } 
			while(returnYaw <   0){ returnYaw += 360; }
		}
		else{
			//Log.v(TAG,"oldYaw("+oldYaw+")-yaw("+yaw+") == 180");
			returnYaw = oldYaw*YAW_FILTER+yaw*(1-YAW_FILTER);
		}
		
		//Log.v(TAG,"returnYaw="+returnYaw);
		return returnYaw;
	}	
	
	public static String GetDistanceString(float meters, int units)
	{
		if(units == ReconSettingsUtil.RECON_UINTS_METRIC) {
			if(meters < 1000)
			{
				return (int)meters + "m";
			}
			else if(meters < 100000) //99.9km
			{
				return GetRoundedFloat(meters/1000, 1) + "km";
			}
			return (int)Math.round(meters/1000) + "km";
		} else {
			float feet = meters*3.28084f;
			if(feet < 5280.0f)
			{
				return (int)feet + "ft";
			}
			else if(feet < 528000.f) // 99.9mi
			{
				return GetRoundedFloat(feet/5280.0f, 1) + "mi";
			}
			return (int)Math.round(feet/5280.0f) + "mi";			
		}
	}
	
	public static float GetRoundedFloat(float n, int dec)
	{
		float factor = (float)Math.pow(10, dec);
		return Math.round(n * factor)/factor;
	}
}
