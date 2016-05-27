package com.reconinstruments.os.hardware.sensors;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.util.Log;

/** {@hide} */
public class HeadLocation implements SensorEventListener, LocationListener{
    private final String TAG = this.getClass().getSimpleName();

    private static final boolean DEBUG = false;
    private static final int SAMPLING_RATE = 20000; // unit of micro second, 50Hz = 20ms

    private float mYaw            = 0.0f;
    private float mPitch          = 0.0f;
    private float mRoll           = 0.0f;
    private float mDeclination    = 999.0f;

    private boolean mIsRunning = false;
    private SensorManager mSensorManager = null;
    private LocationManager mLocationManager = null;

    private final HeadLocationListener mHeadLocationListener;

    enum HeadCommand {
        START,
        STOP
    }

    public HeadLocation(Context context, HeadLocationListener headLocationListener) {
        if(DEBUG) Log.d(TAG,"Creating HeadLocation");

        mHeadLocationListener = headLocationListener;
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public void stop(){
        if(DEBUG) Log.d(TAG,"stop");

        mSensorManager.unregisterListener(this);
        sendCommand(HeadCommand.STOP);

        mIsRunning = false;
    }

    public void start(){
        if(DEBUG) Log.d(TAG,"start");

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SAMPLING_RATE);
        sendCommand(HeadCommand.START);

        mIsRunning = true;
    }

    private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch(HeadCommand.values()[message.arg1]) {
                case START:
                    mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, HeadLocation.this);
                    break;
                case STOP:
                    mLocationManager.removeUpdates(HeadLocation.this);
                    break;
                }
            }
        };

    private void sendCommand(HeadCommand headCommand) {
        Message message = mHandler.obtainMessage();
        message.arg1 = headCommand.ordinal();
        mHandler.sendMessage(message);
    }

    @Override
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

                //users expecting yaw value within 0 - 360
                while(mYaw > 360.0f) {mYaw -= 360.0f;}
                while(mYaw < 0.0f) {mYaw += 360.0f;}

                mHeadLocationListener.onHeadLocation(mYaw, mPitch, mRoll);
                break;
            }
    }

    private float[] orientationRadToDeg(float[] orientation) {
        for (int i = 0; i < orientation.length; i++) {
            orientation[i] = (float)Math.toDegrees(orientation[i]);
        }
        return orientation;
    }

    @Override
    public void onLocationChanged(Location location) {
        GeomagneticField geomagneticField = new GeomagneticField((float)location.getLatitude(), (float)location.getLongitude(), (float)location.getAltitude(), System.currentTimeMillis());
        float decl = geomagneticField.getDeclination();
        if (decl != 999.0f) {
            mDeclination = decl;
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy){}
    @Override public void onProviderDisabled(String provider) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
}
