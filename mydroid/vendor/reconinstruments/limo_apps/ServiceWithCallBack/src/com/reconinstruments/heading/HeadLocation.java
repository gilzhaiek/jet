package com.reconinstruments.heading;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;

public class HeadLocation implements LocationListener {
	private static final String TAG = "HeadLocation";
	static protected final int PREVIOUS_OFFSET_QUEUE_DEPTH = 10; // MUST BE EVEN
	static protected final float MAX_AUTO_ROTATION = 15.f;
	static protected final float MIN_GPS_SPEED_HEADING = 8.0f; 
	static protected final int HEADING_STATE_THRESHOLD = 5;

	static protected boolean mIsPaused = true;

	static protected long mLastGPSChange = 0; 

	protected static Location mLocation	= null;
	protected static float mYaw			= 0.0f;
	protected static float mPitch		= 0.0f;
	protected static float mRoll        = 0.0f;

	protected static float mDeclination = 999.0f;

	protected static GeomagneticField mGeomagneticField = null;

	static protected int mLastSpeedPointer = 0;
	static protected float[] mLastSpeeds = new float[HEADING_STATE_THRESHOLD];

	static protected int mGoingUpCounter = 0;
	static protected int mGoingDownCounter = 0;
	static protected float mOldAlt = 0.0f;

	protected static boolean mSensorOn = false;

	protected static SensorManager mSensorManager = null;

	private LocationManager mLocationManager	= null;
	protected float mLocationLongitude, mLocationLatitude;

	private static PointF mAvgDirection = null;

	private static int mPreviouseOffsetLoc = -1;
	private static PointF[] mPreviousOffsets = new PointF[PREVIOUS_OFFSET_QUEUE_DEPTH];

	protected static final ReentrantLock mLock = new ReentrantLock();

	private HeadingService mTheService = null;

	public HeadLocation(HeadingService headingService) {
		Log.d(TAG,"Constructor");
		mAvgDirection = new PointF(0, 0);

		for (int i = 0; i < mPreviousOffsets.length; i++)
		{
			mPreviousOffsets[i] = null;
		}

		for(int i = 0; i < HEADING_STATE_THRESHOLD; i++) {
			mLastSpeeds[i] = 0.0f;
		}

		ClearValues();

		if(headingService != null) {
			mTheService = headingService;
			mSensorManager = (SensorManager)headingService.getSystemService(Context.SENSOR_SERVICE);
			mLocationManager = (LocationManager)mTheService.getSystemService(Context.LOCATION_SERVICE);
			mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, this);
		}
	}

	protected void ClearValues() {
		mGoingUpCounter = 0;
		mGoingDownCounter = 0;
		mOldAlt = 0.0f;
	}

	protected void UnregisterSensorListener(){
		Log.v(TAG, "UnregisterSensorListener");

		mSensorOn = false;

		if(mTheService == null) return;

		mSensorManager.unregisterListener(mListener);
	}

	protected void RegisterSensorListener(){
		Log.v(TAG, "RegisterSensorListener");

		if(mTheService == null) return;
		mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), (int)20000); //50Hz = 20ms
		mSensorOn = true;
	}

	protected void onResume(){
		mIsPaused = false;
		ClearValues();
		RegisterSensorListener();
	}

	protected void onPause() {
		mIsPaused = true;
		UnregisterSensorListener();
	}

	public final SensorEventListener mListener = new SensorEventListener()
	{
		public float radiansToDegrees(float rads)
		{
			float PI2 = 2 * 3.14159f;

			// Correct for when signs are reversed.
			if(rads < 0){ rads += PI2; }

			// Check for wrap due to addition of declination.
			if(rads > PI2){ rads -= PI2; }

			// Convert radians to degrees for readability.
			return rads * (180 / 3.14159f);
		}

		public void onSensorChanged(SensorEvent event)
		{
			switch (event.sensor.getType())
			{
            case Sensor.TYPE_ROTATION_VECTOR:
                float[] rotationVector = event.values;
                float[] rotationMatrix = new float[9];
                float[] orientation = new float[3];

                SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
                SensorManager.getOrientation(rotationMatrix, orientation);
                orientationRadToDeg(orientation);
                mYaw = orientation[0] + (mDeclination != 999 ? mDeclination : 0);
                mPitch = orientation[1];
                mRoll = orientation[2];

				MessageRegisterdClients();
                break;
			}
		}

        float[] orientationRadToDeg(float[] orientation) {
            for (int i = 0; i < orientation.length; i++) {
                orientation[i] = (float) Math.toDegrees(orientation[i]);
            }
            return orientation;
        }
        
        float[] orientationAdjustPositive(float[] orientation) {
            if (orientation[0] < 0)
                orientation[0] += 360;

            if (orientation[1] < 0)
                orientation[1] += 360;

            if (orientation[2] < 0)
                orientation[2] += 360;
        
            return orientation;
        }
		
		public void onAccuracyChanged(Sensor sensor, int accuracy){	}
	};	


	protected void FilterOffsetSpikes()
	{
		int offsetLoc = mPreviouseOffsetLoc;
		PointF offset1, offset2, offset3;

		offset1 = mPreviousOffsets[offsetLoc];
		if (offset1 == null)
		{
			return;
		}

		if (--offsetLoc < 0)
		{
			offsetLoc = (PREVIOUS_OFFSET_QUEUE_DEPTH - 1);
		}
		int offsetLoc2 = offsetLoc;
		offset2 = mPreviousOffsets[offsetLoc];
		if (offset2 == null)
		{
			return;
		}

		if (--offsetLoc < 0)
		{
			offsetLoc = (PREVIOUS_OFFSET_QUEUE_DEPTH - 1);
		}
		offset3 = mPreviousOffsets[offsetLoc];
		if (offset3 == null)
		{
			return;
		}

		if (Math.abs(offset1.x - offset3.x) < Math.abs(offset2.x - offset3.x))
		{
			mPreviousOffsets[offsetLoc2].x = (offset1.x + offset3.x) / 2.0f;
		}
		if (Math.abs(offset1.y - offset3.y) < Math.abs(offset2.y - offset3.y))
		{
			mPreviousOffsets[offsetLoc2].y = (offset1.y + offset3.y) / 2.0f;
		}
	}

	protected void AddOffset(float x, float y)
	{
		if (++mPreviouseOffsetLoc >= mPreviousOffsets.length)
		{
			mPreviouseOffsetLoc = 0;
		}

		if (mPreviousOffsets[mPreviouseOffsetLoc] == null)
		{
			mPreviousOffsets[mPreviouseOffsetLoc] = new PointF(x, y);
		}
		else
		{
			mPreviousOffsets[mPreviouseOffsetLoc].x = x;
			mPreviousOffsets[mPreviouseOffsetLoc].y = y;
		}

		FilterOffsetSpikes();
	}

	public void UpdateLocation (Location location, double lng, double lat) {
		mLocationLongitude = (float)lng;
		mLocationLatitude = (float)lat;
		mLocation = location;

		AddOffset(mLocationLongitude, mLocationLatitude);
	}

	public void updateSpeed(float speed) {
		mLock.lock();
		try{
			mLastSpeeds[mLastSpeedPointer] = speed;
			mLastSpeedPointer++;
			mLastSpeedPointer %= mLastSpeeds.length;
		}
		catch (Exception e) {}
		finally {mLock.unlock();}
	}

	public void updateAltitude(float alt){
		mLock.lock();
		try{
			if(alt >= mOldAlt){ // Uphill or flat
				mGoingUpCounter++;
				mGoingDownCounter = 0;
			} else { // Down Hill
				mGoingUpCounter = 0;
				mGoingDownCounter ++;
			}
			mOldAlt = alt;
		}
		catch (Exception e) {}
		finally {mLock.unlock();}
	}

	protected static boolean IsGoingUp(){
		return (mGoingUpCounter >= HEADING_STATE_THRESHOLD);
	}
	protected static boolean IsGoingDown(){
		return (mGoingDownCounter >= HEADING_STATE_THRESHOLD);
	}

	protected static PointF GetAvgDirection()
	{
		int offsetLoc = mPreviouseOffsetLoc;

		PointF firstAvg = new PointF(0, 0);
		PointF lastAvg = new PointF(0, 0);

		for (int i = 0; i < PREVIOUS_OFFSET_QUEUE_DEPTH; i++)
		{
			if (mPreviousOffsets[offsetLoc] != null)
			{
				if (i < (PREVIOUS_OFFSET_QUEUE_DEPTH / 2))
				{
					lastAvg.x += mPreviousOffsets[offsetLoc].x;
					lastAvg.y += mPreviousOffsets[offsetLoc].y;
				}
				if (i >= (PREVIOUS_OFFSET_QUEUE_DEPTH / 2))
				{
					firstAvg.x += mPreviousOffsets[offsetLoc].x;
					firstAvg.y += mPreviousOffsets[offsetLoc].y;
				}
			}

			if (--offsetLoc < 0)
			{
				offsetLoc = (PREVIOUS_OFFSET_QUEUE_DEPTH - 1);
			}
		}

		mAvgDirection.x = lastAvg.x - firstAvg.x;
		mAvgDirection.y = lastAvg.y - firstAvg.y;

		return mAvgDirection;
	}

	protected void MessageRegisterdClients(){
		while(mYaw > 360.0f) {mYaw -= 360.0f;}
		while(mYaw < 0.0f) {mYaw += 360.0f;}

		Bundle b = generateHeadingBundle();
		mTheService.callOnLocationHeadingChanged(b);
	}

	@Override
	public void onLocationChanged(Location location) {

		double longitude = location.getLongitude();
		double latitude = location.getLatitude();
		mLastGPSChange = System.currentTimeMillis();
		mGeomagneticField = new GeomagneticField((float) latitude, (float) longitude, (float) location.getAltitude(), mLastGPSChange);
		float decl = mGeomagneticField.getDeclination();
		if (decl != 999.0f)	mDeclination = decl;
		UpdateLocation(location, longitude, latitude);

		updateSpeed(location.getSpeed());

		if(!mIsPaused) {
			MessageRegisterdClients();
		}
	}

	private Bundle generateHeadingBundle() {
		Bundle b = new Bundle();

		b.putParcelable("Location", mLocation);
		b.putFloat("Yaw", mYaw);
		b.putFloat("Pitch", mPitch);
		b.putFloat("Roll", mRoll);

        // GPSHeading is removed. Kept for legacy support
		b.putBoolean("IsGPSHeading", false);

		//Log.v(TAG,"generateHeadingBundle, mYaw="+mYaw+" IsGpsYaw="+mUserGPSForYaw);

		return b;
	}

	public boolean IsUsingGPS(){
        // GPSHeading is removed. Kept for legacy support
		return false;
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}
