package com.reconinstruments.mapsdk.mapview.renderinglayers;

import android.graphics.RectF;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
  
public class World2DrawingTransformer {
	private final static String TAG = "World2DrawingTransformer";
	
	public static final float	DISTANCE_PER_PIXEL = 3.0f;//0.8;
	public static final double MERIDONIAL_CIRCUMFERENCE = 40007860.0; // m  - taken from wikipedia/Earth
	public static final double EQUITORIAL_CIRCUMFERENCE = 40075017.0; // m  - taken from wikipedia/Earth

	protected double	mOriginX = -1.0;
	protected double	mOriginY = -1.0;
	
	public World2DrawingTransformer() {
	}
	
	public void SetDrawingOrigin(double lng, double lat) {			// defines (0, 0) point in drawing coordinates - will requires regeneration of any drawing objects in cache !!
		mOriginX = lng;
		mOriginY = lat;
	}
	
	public boolean isDrawingOriginSet() {
		return (mOriginX != -1.0 && mOriginY != -1.0);
	}

	public boolean isDrawingOriginSetCloseTo(double latitude, double longitude) {		
		float distInM = DistanceBetweenGPSPoints((float) longitude,(float) mOriginX,(float) latitude,(float) mOriginY);
		return (distInM < 100000);
	}

	public RectF TransformGPSRectXYToRectF(RectXY gpsRect) {
		RectF drawingRect = new RectF();
		drawingRect.left =   (float) TransformLongitude(gpsRect.left,  gpsRect.top);
		drawingRect.right =  (float) TransformLongitude(gpsRect.right, gpsRect.top);
		drawingRect.top =    (float) TransformLatitude(gpsRect.left, gpsRect.top);
		drawingRect.bottom = (float) TransformLatitude(gpsRect.left, gpsRect.bottom);
		
		return drawingRect;
	}

	public PointXY TransformGPSPointToDrawingPoint(PointXY gpsPoint) {
		PointXY drawingPoint = new PointXY(0.f,0.f);
		drawingPoint.x = (float) TransformLongitude(gpsPoint.x, gpsPoint.y);
		drawingPoint.y = (float) TransformLatitude(gpsPoint.x, gpsPoint.y);
		return drawingPoint;
	}
	
	public float TransformLatitude(float longitude, float latitude) {
		return (float)(-((double)latitude-mOriginY)/360.0 * MERIDONIAL_CIRCUMFERENCE / DISTANCE_PER_PIXEL) ;  // == pixel coordinate  (-ve because in Android drawing/graphics Y is opposite real-world (GPS) Y direction);
	}

	public float TransformLongitude(float longitude, float latitude) { 
		return (float)((((double)longitude-mOriginX)/360.0) * EQUITORIAL_CIRCUMFERENCE*Math.cos(Math.toRadians((double)latitude)) / DISTANCE_PER_PIXEL); // == pixel coordinate
	}

	public static float DistanceBetweenGPSPoints(float longitude1, float latitude1, float longitude2, float latitude2) {
		double horizDist = (double)((((double)longitude1-longitude2)/360.0) * EQUITORIAL_CIRCUMFERENCE*Math.cos(Math.toRadians((double)latitude1))); 
		double vertDist  = (double)(-((double)latitude1-latitude2)/360.0 * MERIDONIAL_CIRCUMFERENCE) ;  // == pixel coordinate  (-ve because in Android drawing/graphics Y is opposite real-world (GPS) Y direction);
		float  distInM   = (float) Math.sqrt(horizDist * horizDist + vertDist * vertDist);
		
		return distInM;
		
	}
}
