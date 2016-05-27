package com.reconinstruments.mapImages.helpers;

import android.graphics.RectF;
import android.util.Log;
  
public class LocationTransformer {
	private final static String TAG = "LocationTransformer";
	
	public static final double	EARTH_RADIUS = 6371;
	public static final float	DISTANCE_PER_PIXEL = 3.0f;//0.8;
	public static final float	METERS_TO_DEGREES = 0.00001f*0.9009009009009009009009009009009f;
	private static final float	GPS_BOUNDING_BOX_SIDE_METERS 	= 4000.0f;
	
	// 1050 Homer St
	private static final double		MY_DEBUG_LOCATION_LAT	= 49.27673f;
	private static final double		MY_DEBUG_LOCATION_LONG	= -123.120732f;
	
	private static final double		CYPRESS_LAT				= 49.395984f;
	private static final double		CYPRESS_LONG			= -123.204696f;
	
	private static final double		GROUSE_LAT				= 49.381674f;
	private static final double		GROUSE_LONG				= -123.078439f;
	
	private static final boolean 	USE_DEBUG_LOCATION	= false;
	public static final double		DEBUG_OFFSET_LAT	= USE_DEBUG_LOCATION ? GROUSE_LAT-MY_DEBUG_LOCATION_LAT : 0.0f;
	public static final double		DEBUG_OFFSET_LONG	= USE_DEBUG_LOCATION ? GROUSE_LONG-MY_DEBUG_LOCATION_LONG : 0.0f;
	
	protected double	mLeft = 0;
	protected double	mTop = 0;
	protected float		mShapeScaleFactor = 1.0f;		
	protected float		mDrawOffsetX = 0;
	
	public boolean 		mBoundingBoxSet = false;
	
	public LocationTransformer() {
	}
	
	public void SetGPSPosition(double lng, double lat) {
		float width = METERS_TO_DEGREES*GPS_BOUNDING_BOX_SIDE_METERS;
		mLeft = lng-width/2.0f;
		mTop = lat-width/2.0f;
		double distanceInKm = DistanceKmFromLngLats( mLeft, mTop, mLeft+width, mTop+width );
		mShapeScaleFactor  = (float)distanceInKm*1000.0f/(width*DISTANCE_PER_PIXEL);
	}
	
	public void SetBoundingBox(RectF resortBoundingBox) {			// init object by setting coordinate system transform params - GPS -> pixels
		//float width = resortBoundingBox.right - resortBoundingBox.left;
		mLeft = resortBoundingBox.left;
		mTop = resortBoundingBox.top;
		
		double distanceInKm = DistanceKmFromLngLats( resortBoundingBox.left, resortBoundingBox.top, resortBoundingBox.right, resortBoundingBox.bottom );
		double distanceDegrees = Math.toDegrees(distanceInKm/EARTH_RADIUS);
		mShapeScaleFactor  = (float)distanceInKm*1000.0f/((float)distanceDegrees*DISTANCE_PER_PIXEL); // units are pixels/degree
		// TODO: ZZZ remove
//		Log.v(TAG,"mShapeScaleFactor="+mShapeScaleFactor+" width="+distanceDegrees+" distanceInKm="+distanceInKm+" "+resortBoundingBox.left+","+resortBoundingBox.top+","+resortBoundingBox.right+","+resortBoundingBox.bottom);
		mBoundingBoxSet = true;
	}
	
	public double DistanceKmFromLngLats( double lng1, double lat1, double lng2, double lat2 )
	{
		double dLat = Math.toRadians(lat1 - lat2);
		double dLon = Math.toRadians(lng1- lng2);
		double a = Math.sin(dLat/2)*Math.sin(dLat/2)+
        		   Math.cos( Math.toRadians(lat1) )* Math.cos(Math.toRadians(lat2)) * 
        		   Math.sin(dLon/2) * Math.sin(dLon/2); 
		double distance = EARTH_RADIUS * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)));
		
		return distance;
	}	
	
	public double TransformLatitude(double latitude, double longitude) {
//	public double TransformLatitude(double latitude) {
		//Log.v(TAG,"TransformLatitude="+((latitude-mTop)*mShapeScaleFactor));
//		return ((latitude-mTop)*mShapeScaleFactor);
		double meridionalCircumference = 40007860.0; // m  - taken from wikipedia/Earth

		return (latitude-mTop)/360.0 * meridionalCircumference / DISTANCE_PER_PIXEL ;  // == pixel coordinate
	}

	public double TransformLongitude(double latitude, double longitude) { 
//	public double TransformLongitude(double longitude) { 
//		return ((longitude-mLeft)*mShapeScaleFactor);	
		double equitorialCircumference = 40075017.0; // m  - taken from wikipedia/Earth

		return ((longitude-mLeft)/360.0) * equitorialCircumference*Math.cos(Math.toRadians(latitude)) / DISTANCE_PER_PIXEL; // == pixel coordinate
	}
	
	public float TransformMetersValue(float meters) {
		//Log.v(TAG,"Meters="+meters+" mShapeScaleFactor="+mShapeScaleFactor+" R="+METERS_TO_DEGREES*meters*mShapeScaleFactor);
		return METERS_TO_DEGREES*meters*mShapeScaleFactor;
	}
	
	public float TransformLocalValue(float local) {
		return local/(METERS_TO_DEGREES*mShapeScaleFactor);
	}
	
	public float MetersToDegree(double meters) {
		return METERS_TO_DEGREES*(float)meters;
	}
}
