package com.reconinstruments.autocadence.sensors;

import java.util.ArrayList;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.reconinstruments.ReconSDK.IReconDataReceiver;
import com.reconinstruments.ReconSDK.ReconAltitude;
import com.reconinstruments.ReconSDK.ReconDataResult;
import com.reconinstruments.ReconSDK.ReconEvent;
import com.reconinstruments.ReconSDK.ReconSDKManager;
import com.reconinstruments.autocadence.containers.SensorValue;

/**
 * Performs positioning sensor fusion at the native level.
 * Takes raw sensor data from Android's SensorManager.
 * Takes altitude calculations from ReconSDK.
 *
 * @author wesleytsai
 *
 */
public class SensorFusionManager implements LocationListener, SensorEventListener, IReconDataReceiver {

    private static final String TAG = SensorFusionManager.class.getSimpleName();

    // Sensor Managers
    private static LocationManager mLocationManager;
    private static SensorManager mSensorManager;
    private static ReconSDKManager mReconManager;

    // Control Variables
    private static int mFrequency = 10000;

    private static Handler mHandler;
    private static long mTimeLastPolledRecon = 0;

    // Listener for callback when KF result values change
    private static KFListener mListener;

    // Container for raw sensor values
    private static SensorValue mRawValue;

    /*
     * Singleton Pattern
     */
    private SensorFusionManager() {}
    private static SensorFusionManager mInstance = null;

    /**
     * Attach a context that contains Android's Location and Sensor services
     * Usually, that just means the Android activity
     *
     * Does nothing except return the singleton instance if already initialised elsewhere
     *
     * @param context
     * @return The singleton instance
     */
    public static SensorFusionManager Initialize(Context context) {
        if (SensorFusionManager.mInstance == null) {
            SensorFusionManager.mInstance = new SensorFusionManager();

            mRawValue = new SensorValue();
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            mReconManager = ReconSDKManager.Initialize(context);

            HandlerThread thread = new HandlerThread("KalmanSensorThread");
            thread.start();

            mHandler = new Handler(thread.getLooper());

        }

        return mInstance;
    }

    public SensorValue getRawValues() {
        return mRawValue;
    }

    /**
     * Attaches listeners to Android sensors
     */
    private void start() {
        if (mListener != null) {

            mReconManager.receiveData(this, ReconEvent.TYPE_ALTITUDE);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), mFrequency, mHandler);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), mFrequency, mHandler);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), (int)20000, mHandler);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), mFrequency, mHandler);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), mFrequency, mHandler);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        } else {
            Log.d(TAG, "There is no listener attached to KF Manager!");
        }
    }

    /**
     * Detaches Android sensors listeners
     */
    private void stop() {
        if (mListener == null) {
            mReconManager.unregisterListener(ReconEvent.TYPE_ALTITUDE);
            mSensorManager.unregisterListener(this);
            mLocationManager.removeUpdates(this);
        } else {
            Log.d(TAG, "Detach listener to KF Manager first!");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        /** Coordinates are mapped in East North Up for the Snow 2 when held in regular viewing position **/
        switch (event.sensor.getType()) {

        case Sensor.TYPE_PRESSURE:
            mRawValue.setPressure(event.values[0]);
            break;

        case Sensor.TYPE_LINEAR_ACCELERATION:
            mRawValue.setLinearAcc( event.values[0], event.values[1], event.values[2] );
            if (mListener != null)
                mListener.onRawValuesChanged(mRawValue);

            break;

        case Sensor.TYPE_ACCELEROMETER:
            mRawValue.setAcc( event.values[0], event.values[1], event.values[2] );
            mRawValue.timestamp = ((double)event.timestamp / 1000000000.0);
            break;

        case Sensor.TYPE_GYROSCOPE:
            mRawValue.setGyr( event.values[0], event.values[1] , event.values[2]);

            break;

        case Sensor.TYPE_MAGNETIC_FIELD:

            // Snow 2's magnetometer Z axis is inverted, this re-inverts it
            mRawValue.setMag( event.values[0], event.values[1], event.values[2]);
            float mag_norm = (float)Math.sqrt(event.values[0]*event.values[0]+event.values[1]*event.values[1]+event.values[2]*event.values[2]);
            break;

        case Sensor.TYPE_ROTATION_VECTOR:
            float[] rotationVector = event.values;
            float[] rotationMatrix = new float[9];
            float[] correctedRotationMatrix = new float[9];
            float[] orientation = new float[3];

            SensorManager.getRotationMatrixFromVector(rotationMatrix,rotationVector);

            SensorManager.getOrientation(rotationMatrix, orientation);

            orientationRadToDeg(orientation);

            mRawValue.yaw = orientation[0];
            mRawValue.pitch = orientation[1];
            mRawValue.roll = orientation[2];

             if (mListener != null)
                    mListener.onRawValuesChanged(mRawValue);
            break;

        default:
            Log.d(TAG, "Leaked Sensor Data " + event.sensor.getName());
        }

        /* The only way to get Recon Data is through requests */
        pollReconSDK();
    }

    float[] orientationRadToDeg(float[] orientation) {
        for (int i = 0; i < orientation.length; i++) {
            orientation[i] = (float) Math.toDegrees(orientation[i]);
        }
        return orientation;
    }

    private void pollReconSDK() {
        if (System.currentTimeMillis() - mTimeLastPolledRecon >= 1000) {
            mTimeLastPolledRecon = System.currentTimeMillis();
            mReconManager.receiveData(this, ReconEvent.TYPE_ALTITUDE);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
                mRawValue.setGps(location.getLatitude(), location.getLongitude(), mRawValue.reconAltitude);
                mRawValue.speed = location.getSpeed();
                mRawValue.accuracy = location.getAccuracy();
    }

    @Override
    public void onReceiveCompleted(int type, ReconDataResult result) {
        if (result == null)
            return;

        for (ReconEvent event : result.arrItems) {
            switch (event.getType()) {
            case ReconEvent.TYPE_ALTITUDE:
                final ReconAltitude rAltitude = (ReconAltitude) event;

                mRawValue.reconAltitude = rAltitude.GetAltitude();
            default:
                break;
            }
        }
    }

    /* Listener registration for client */
    public void registerListener(KFListener listener) {
        SensorFusionManager.mListener = listener;
        this.start();
    }

    public void unregisterListener() {
        SensorFusionManager.mListener = null;
        this.stop();
    }

    /**
     * RawValuesChanged should fire at 100hz
     *
     */
    public interface KFListener {
        public abstract void onRawValuesChanged(SensorValue result);
    }

    /**
     * Unimplemented methods
     */
    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onFullUpdateCompleted(int arg0, ArrayList<ReconDataResult> arg1) {}

}