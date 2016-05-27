/*
 * Copyright (C) 2015 Recon Instruments
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reconinstruments.hudserver;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.IWindowManager;
import com.reconinstruments.os.hardware.motion.IActivityMotionDetectionListener;
import com.reconinstruments.os.hardware.motion.IHUDActivityMotionService;
import com.reconinstruments.os.hardware.motion.HUDActivityMotionManager;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

//For Accelerometer access
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

// For GPS access
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;


class IHUDActivityMotionServiceImpl extends IHUDActivityMotionService.Stub {
    private static final String TAG = "IHUDActivityMotionServiceImpl";
    private static final boolean DEBUG = false;

    private static final int SUCCESS = 1;
    private static final int FAILURE = 0;

    static private final int START_GPS_LISTENER = 0;
    static private final int STOP_GPS_LISTENER = 1;
    static private final int START_ACCEL_LISTENER = 2;
    static private final int STOP_ACCEL_LISTENER = 3;

    private final Context mContext;
    private int mType = HUDActivityMotionManager.MOTION_DETECT_NOT_SUPPORTED;
    private int mEvent = HUDActivityMotionManager.EVENT_INVALID;
    private BaseAlgo mMotionAlgo;

    private Object mStartStopSync = new Object();

    private final Map<IBinder, DetectionListenerTracker> mDetectionListeners =
        new HashMap<IBinder, DetectionListenerTracker>();

    // Handle messages from the motion algorithms
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            LocationManager locationManager;
            SensorManager sensorManager;
            
            if (DEBUG) Log.d(TAG, "handleMessage: " + message.what);

            switch (message.what) {
                case START_GPS_LISTENER:
                    int gps_rate = message.arg1;
                    if (DEBUG) Log.d(TAG, "Starting GPS listener at rate: " + gps_rate);
                    locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gps_rate, 0, (LocationListener) mMotionAlgo); 
                    break;
                case STOP_GPS_LISTENER:
                    locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                    locationManager.removeUpdates((LocationListener) mMotionAlgo);
                    break;
                case START_ACCEL_LISTENER:
                    int accel_rate = message.arg1;
                    if (DEBUG) Log.d(TAG, "Starting Accel listener at rate: " + accel_rate);
                    sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
                    Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    sensorManager.registerListener((SensorEventListener) mMotionAlgo, accelerometer, accel_rate * 1000); //rate must be in microseconds
                    break;
                case STOP_ACCEL_LISTENER:
                    sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
                    sensorManager.unregisterListener((SensorEventListener) mMotionAlgo);
                    break;
                default:
                    Log.e(TAG, "Invalid Message");
                    break;
            }
            synchronized (mStartStopSync) {
                // Indicate that a start/stop is completed within the handler.
                mStartStopSync.notifyAll();
            }
            if (DEBUG) Log.d(TAG, "handleMessage X");
        }
    };

    IHUDActivityMotionServiceImpl(Context context) {
        this.mContext = context;
    }

    public int registerActivityMotionDetection(IActivityMotionDetectionListener listener, int type) throws RemoteException {
        int rc = FAILURE;
        if (listener != null) {
            // Only allow one type of activity to be running at a time.
            if (mType == HUDActivityMotionManager.MOTION_DETECT_NOT_SUPPORTED || mType == type) {
                IBinder binder = listener.asBinder();
                synchronized(this.mDetectionListeners) {
                    if (this.mDetectionListeners.containsKey(binder)) {
                        Log.w(TAG, "Ignoring duplicate detection listener: " + listener);
                    } else {
                        // Create the detection listener to report events back to clients
                        DetectionListenerTracker listenerTracker = new DetectionListenerTracker(listener);
                        binder.linkToDeath(listenerTracker, 0);
                        this.mDetectionListeners.put(binder, listenerTracker);

                        if (DEBUG) Log.d(TAG, "Registered detection listener: " + listener);
                        if (this.mDetectionListeners.size() == 1) {
                            if (DEBUG) Log.d(TAG, "Start motion detection");
                            mType = type;

                            // First listener, so start motion detection
                            synchronized (mStartStopSync) {
                                switch(mType) {
                                    case HUDActivityMotionManager.MOTION_DETECT_RUNNING:
                                        mMotionAlgo = new RunningAlgo(this, mHandler);
                                        break;
                                    case HUDActivityMotionManager.MOTION_DETECT_CYCLING:
                                        mMotionAlgo = new CyclingAlgo(this, mHandler);
                                        break;
                                    case HUDActivityMotionManager.MOTION_DETECT_NOT_SUPPORTED:
                                    //fallthrough
                                    default:
                                        Log.e(TAG, "Error activity type not supported");
                                        return rc;
                                }

                                mMotionAlgo.start();
                                // Wait until the handler has finished with the start before we continue on
                                try {
                                    mStartStopSync.wait();
                                } catch (InterruptedException e) {
                                }
                                if (DEBUG) Log.d(TAG, "Algorithm type " + mType + " has started");
                            }
                        }
                        rc = SUCCESS;
                    }
                }
            }
        }
        return rc;
    }

    public void unregisterActivityMotionDetection(IActivityMotionDetectionListener listener) {
        if (listener != null) {
            IBinder binder = listener.asBinder();
            synchronized(this.mDetectionListeners) {
                DetectionListenerTracker listenerTracker = this.mDetectionListeners.remove(binder);
                if (listenerTracker == null) {
                    Log.w(TAG, "Ignoring unregistered listener: " + binder);
                } else {
                    if (DEBUG) Log.d(TAG, "Unregistered listener: " + binder);
                    binder.unlinkToDeath(listenerTracker, 0);
                    if (this.mDetectionListeners.isEmpty()) {
                        if (DEBUG) Log.d(TAG, "Stop motion detection");

                        synchronized (mStartStopSync) {
                            mMotionAlgo.stop();
                            // Wait until the handler has finished with the stop before we continue on
                            try {
                                mStartStopSync.wait();
                            } catch (InterruptedException e) {
                            }
                            if (DEBUG) Log.d(TAG, "Algorithm type " + mType + " has stopped");
                        }
                        // Reset activity type
                        mType = HUDActivityMotionManager.MOTION_DETECT_NOT_SUPPORTED;
                    }
                }
            }
        }
    }

    public int getActivityMotionDetectedEvent() {
        return mEvent;
    }

    private final class DetectionListenerTracker implements IBinder.DeathRecipient {
        private final IActivityMotionDetectionListener listener;

        public DetectionListenerTracker(IActivityMotionDetectionListener listener) {
            this.listener = listener;
        }

        public IActivityMotionDetectionListener getListener() {
            return this.listener;
        }

        public void binderDied() {
            IHUDActivityMotionServiceImpl.this.unregisterActivityMotionDetection(this.listener);
        }
    }


    
    public void onEvent(int event, int type) {
        if (DEBUG) Log.d(TAG, "Received event from motion algo " + event + " type: " + type);
        if (mType != type) {
            Log.e(TAG, "Invalid type returned! " + mType + " != " + type);
        } else {
            boolean inMotion = true;
            switch (event) {
                case HUDActivityMotionManager.EVENT_STATIONARY:
                    inMotion = false;
                    // fall thru
                case HUDActivityMotionManager.EVENT_IN_MOTION:
                    synchronized(IHUDActivityMotionServiceImpl.this.mDetectionListeners) {
                        // Go through all registered listeners and report the event.
                        for (Map.Entry<IBinder, DetectionListenerTracker> entry : IHUDActivityMotionServiceImpl.this.mDetectionListeners.entrySet()) {
                            IActivityMotionDetectionListener listener = entry.getValue().getListener();
                            try {
                                if (DEBUG) Log.d(TAG, "Notifying detection listener: " + entry.getKey());
                                listener.onDetectEvent(inMotion, type);
                            } catch (RemoteException e) {
                                Log.e(TAG, "Failed to update detection listener: " + entry.getKey(), e);
                                IHUDActivityMotionServiceImpl.this.unregisterActivityMotionDetection(listener);
                            }
                        }
                    }
                    // Record the most recent event.
                    mEvent = event;
                    break;
                default:
                    // Event not reported.
                    break;
            }
        }
    };

    private abstract class BaseAlgo {

        protected static final boolean DEBUG = false;

        private static final double SENSOR_RATE_SLACK = 0.95; // We allow a 5% slack in the sensor data rate period.
                                                              // i.e. a data rate period of 95% of the desired rate is still acceptable 

        protected int mType;
        protected boolean mStarted;
        protected int mEvent;
        protected IHUDActivityMotionServiceImpl mActivityMotionServiceImpl;
        protected Handler mHandler;

        public BaseAlgo(IHUDActivityMotionServiceImpl activityMotionServiceImpl, Handler handler) {
            mActivityMotionServiceImpl = activityMotionServiceImpl;
            mHandler = handler;
            mStarted = false;
            mEvent = HUDActivityMotionManager.EVENT_INVALID;
        }

        public abstract void start();
        public abstract void stop();

        protected boolean checkTimestamp(long lastTimestampMs, long timestampMs, long rateMs) {
            // Check that the data rate period is no shorter than 95% of the desired rate period
            return ((timestampMs - lastTimestampMs) >= (rateMs * SENSOR_RATE_SLACK));
        }
    };

    /*
     * CyclingAlgo provides motion detection algorithm within the context of a cycling activity. It
     * utilizes gps speed data within its algorithm. The algorithm it uses to determine if the
     * HUD is moving or stationary is the following:
     *
     * - Every GP_SENSOR_RATE ms, the GPS speed from the GPS is received. The data from
     *   is stored into an array (mGPSSpeeds).
     *
     *   If the last MOVING_SPEEDS GPS speeds are higher than MOVE_THRESH_KPM km/h then we
     *   consider the user to moving.
     *   Else if the last HELD_SPEEDS GPS speeds are lower than STOP_THRESH_KPH km/h then we
     *   consider the user to be stopped.
     *   If non of the above we report the previous motion state whatever it is.
     */   
    private class CyclingAlgo extends BaseAlgo implements LocationListener {

      
        private static final int HELD_SPEEDS = 10;  // Number for GPS speeds to save
        private static final int MOVING_SPEEDS = 2; // Number of consecutive GPS speeds to determine motion

        private static final float STOP_THRESH_KPH = (float) 7.5;   // Speed threshold underwhich we are stationary
        private static final float MOVE_THRESH_KPH = (float) 10;    // Speed threshold abovewhich we are moving

        private static final float MPS_TO_KPH = (float) 3.6;    // Conversion from m/s to k/h

        private static final int GPS_SENSOR_RATE = 1000;    // in ms so 1HZ

        private long mLastGPSSpeedTimestampMs = -1; // Timestamp of last recorded GPS speed

        private ArrayList<Float> mGPSSpeeds;    // ArrayList for our recorded GPS speeds

        public CyclingAlgo(IHUDActivityMotionServiceImpl activityMotionServiceImpl, Handler handler) {
            super(activityMotionServiceImpl, handler);
            mType = HUDActivityMotionManager.MOTION_DETECT_CYCLING;
            mGPSSpeeds = new ArrayList<Float>();
            mLastGPSSpeedTimestampMs = -1;
        };

        @Override
        public void start() {

            // Make sure we haven't started the algo yet
            if (mStarted == false) {
                
                // Tell the main thread to start listening for GPS at GPS_SENSOR_RATE
                Message message = mHandler.obtainMessage(mActivityMotionServiceImpl.START_GPS_LISTENER);
                message.arg1 = GPS_SENSOR_RATE;
                mHandler.sendMessage(message);

                // Mark that the algorithm has started
                mStarted = true;
            }
        }

        @Override
        public void stop() {

            // Only do something if the algo is currently running
            if (mStarted == true) {
                // Tell the main thread to stop listening for GPS
                Message message = mHandler.obtainMessage(mActivityMotionServiceImpl.STOP_GPS_LISTENER);
                mHandler.sendMessage(message);

                // Mark the algorithm as stopped
                mStarted = false;
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            int tempEvent;

            long timestampMs = location.getTime(); // location time is in milliseconds

            // Check that the sensor data rate respects the desired algoritm rate
            if (!checkTimestamp(mLastGPSSpeedTimestampMs, timestampMs, (long) GPS_SENSOR_RATE)) {
                // Data is coming in too fast, skip this value
                return; 
            }

            // Update the last GPS speed timestamp
            mLastGPSSpeedTimestampMs = timestampMs;

            // add the latest GPS speed to our arrayList of GPS speeds
            // Index 0 is most recent
            mGPSSpeeds.add(0, location.getSpeed() * MPS_TO_KPH);

            // Remove the last member if the arrayList is full 
            if (mGPSSpeeds.size() > HELD_SPEEDS) {
                mGPSSpeeds.remove(HELD_SPEEDS);
            }
        
            // Determine if our motion state has changed
            tempEvent = calcMotion();

            if (DEBUG) Log.d(TAG, "speed: " + location.getSpeed() * MPS_TO_KPH + " Accuracy: " + location.getAccuracy() + " event: " + tempEvent);

            // Only report a motion event if the state has changed
            if (mEvent != tempEvent)
            {
                // Motion state has changed
                mEvent = tempEvent;
                // Report the new event
                mActivityMotionServiceImpl.onEvent(mEvent, mType);
            }
        }

        private int calcMotion() {

            // Make sure our ArrayList is full before starting
            // to calculate motion
            if (mGPSSpeeds.size() == HELD_SPEEDS) {
                if (checkMoving()) {
                    return HUDActivityMotionManager.EVENT_IN_MOTION;
                } else if (checkStopped()) {
                    return HUDActivityMotionManager.EVENT_STATIONARY;
                }
            }
            return mEvent;
        }

        private boolean checkMoving() {
            for (int i = 0; i < MOVING_SPEEDS; i++) {
                if (mGPSSpeeds.get(i) < MOVE_THRESH_KPH) {
                   return false;
                }
            }
            return true;
        }

        private boolean checkStopped() {
            for (int i = 0; i < HELD_SPEEDS; i++) {
                if (mGPSSpeeds.get(i) > STOP_THRESH_KPH) {
                    return false;
                }
            }
            return true;
        }

        // LocationListener abstract functions
        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    /*
     * RunningAlgo provides motion detection algorithm within the context of a Running activity. It
     * utilizes accelerometer data within its algorithm. The algorithm it uses to determine if the
     * HUD is moving or stationary is the following:
     *
     * - Every SENSOR_RATE ms, the sensor data from the accelerometer is received. The data from
     *   the Z-axis gets stored into an array (mRawAccValues).
     * - When the mRawAccValues array has NUM_ACCEL_VALUES elements, the variance of the data in the
     *   array is computed.
     * - The computed variance is stored into another array (mAccVariances). If it has HELD_VARIANCES
     *   number of members, the algorithm determines whether the HUD is moving based on the values
     *   in mAccVariances:
     *      - If all values are less than STOP_VAR_THRESHOLD, then HUD is definitely stationary
     *      - If the NUM_MOVE_VARS most recent values are greater than MOVE_VAR_THRESHOLD, then HUD
     *        is definitely moving
     *      - If the NUM_PROB_MOVE_VARS most recent values are between STOP_VAR_THRESHOLD and
     *        MOVE_VAR_THRESHOLD, then HUD is probably moving
     * - The oldest member within mAccVariances is removed.
     * - All the values of mRawAccValues are erased and will be filled up again every ACCEL_SENSOR_RATE.
     * - The event is reported to upper layer.
     */   
    private class RunningAlgo extends BaseAlgo implements SensorEventListener {

        private static final float  STOP_VAR_THRESHOLD = 0.85f; // Stopped variance threshold
        private static final float  MOVE_VAR_THRESHOLD = 30.5f; // Moving variance threshold
        private static final int    HELD_VARIANCES = 6;         // Number of most recent variances to keep track of for determining if the HUD is moving/stationary

        private static final int    NUM_ACCEL_VALUES = 25;      // Number of raw accelerometer values before calculating variance
        private static final int    NUM_MOVE_VARS = 2;          // Number of most recent values to check for definitely moving
        private static final int    NUM_PROB_MOVE_VARS = 3;     // Number of most recent values to check for probably moving

        private static final int    ACCEL_SENSOR_RATE = 40;     // in ms, so 25 Hz
        private static final int    Z_AXIS_VALUE = 2;           // Index of Z-axis accelerometer values in SensorEvent

        private ArrayList<Float> mAccVariances;    //ArrayList to keep track of calculated accelerometer variances in the Z-axis
        private ArrayList<Float> mRawAccValues;    //ArrayList to keep track of raw accelerometer data in the Z-axis

        private long mLastAccelTimestampMs = -1;

        public RunningAlgo(IHUDActivityMotionServiceImpl activityMotionServiceImpl, Handler handler) {
            super(activityMotionServiceImpl, handler);
            mType = HUDActivityMotionManager.MOTION_DETECT_RUNNING;

            mLastAccelTimestampMs = -1;

            mAccVariances = new ArrayList<Float>();
            mRawAccValues = new ArrayList<Float>();
        };

        @Override
        public void start() {

            // Make sure we haven't started the algo yet
            if (mStarted == false) {

                // Tell the main thread to start listening to accelerometer updates at ACCEL_SENSOR_RATE
                Message message = mHandler.obtainMessage(mActivityMotionServiceImpl.START_ACCEL_LISTENER);
                message.arg1 = ACCEL_SENSOR_RATE;
                mHandler.sendMessage(message);

                mStarted = true;
            }
        }

        @Override
        public void stop() {

            // Make sure algo is already stopped
            if (mStarted == true) {
                // Tell the main thread to stop listening to accelerometer updates
                Message message = mHandler.obtainMessage(mActivityMotionServiceImpl.STOP_ACCEL_LISTENER);
                mHandler.sendMessage(message);

                mStarted = false;
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                long timestampMs = event.timestamp/1000000; //event.timestamp is in nanoseconds

                // Check that the data rate respects the desired algoritm rate
                if (!checkTimestamp(mLastAccelTimestampMs, timestampMs, (long) ACCEL_SENSOR_RATE)) {
                    // data is coming in too fast, skip this value
                    return; 
                }
                

                // Update the last accel reading timestamp
                mLastAccelTimestampMs = timestampMs;

                // Add the new Z-axis accelerometer value to our ArrayList
                mRawAccValues.add(event.values[Z_AXIS_VALUE]);

                // Check when the arrayList is full
                if (mRawAccValues.size() == NUM_ACCEL_VALUES) {
                    // Caculate and add variance
                    addAccVariance();

                    // Clear all elements in the arrayList of accel sensor readings
                    mRawAccValues.clear();

                    // Calculate the new cycling motion state
                    int tempEvent = calcMotion();            

                    if (DEBUG) Log.d(TAG, "Event: " + tempEvent);

                    // Only report motion if  event has changed
                    if (mEvent != tempEvent)
                    {
                        // Motion event has changed
                        mEvent = tempEvent;
                        // Report the new event
                        mActivityMotionServiceImpl.onEvent(mEvent, mType);
                    }
                }
            }
        }


        private void addAccVariance() {

            int size = mRawAccValues.size();
            if (size > 0) {
                float valSum = 0;
                float squareSum = 0;
                float calcVar = 0;
                float rawVal = 0;

                // Calculate the variance of all collected accel in the accel arrayList
                // variance = (SUM(x(i) - mean(x))^2)/(N-1)
                // This can simplify to (SUM(x(i)^2) - SUM(x(i))/N) * SUM(x(i)))/(N-1)
                for (int i = 0; i < size; i++) {
                    rawVal = mRawAccValues.get(i);
                    valSum += rawVal;
                    squareSum += rawVal * rawVal;
                }

                calcVar = (squareSum - ((valSum / size) * valSum)) / (size - 1);

                if (DEBUG) Log.d(TAG, "addAccVariance - calcVar: " + calcVar);

                // Add the variance to our variance ArrayList
                // Index 0 is the most recent
                mAccVariances.add(0, calcVar);

                // Remove the last member if the arrayList is full
                if (mAccVariances.size() > HELD_VARIANCES) {
                    mAccVariances.remove(HELD_VARIANCES);
                }
            }
        }

        private int calcMotion() {
            if (mAccVariances.size() == HELD_VARIANCES) {
                if (checkDefinitelyStopped()) {
                    // definitely stopped
                    return HUDActivityMotionManager.EVENT_STATIONARY;
                } else if (checkDefinitelyMoving()) {
                    // definitely moving
                    return HUDActivityMotionManager.EVENT_IN_MOTION;
                } else if (checkProbablyMoving()) {
                    // probably moving
                    return HUDActivityMotionManager.EVENT_IN_MOTION;
                }
            }
            // return previously detected event
            return mEvent;
        }

        private boolean checkDefinitelyStopped() {
            for (int i = 0; i < HELD_VARIANCES; i++) {
                if (mAccVariances.get(i) >= STOP_VAR_THRESHOLD) {
                    return false;
                }   
            }
            return true;
        }

        private boolean checkDefinitelyMoving() {
            for (int i = 0; i < NUM_MOVE_VARS; i++) {
                if (mAccVariances.get(i) <= MOVE_VAR_THRESHOLD) {
                    return false;
                }
            }
            return true;
        }

        private boolean checkProbablyMoving() {
            for (int i = 0; i < NUM_PROB_MOVE_VARS; i++) {
                if (mAccVariances.get(i) < STOP_VAR_THRESHOLD || mAccVariances.get(i) > MOVE_VAR_THRESHOLD) {
                    return false;
                }
            }
            return true;
        }

        // SensorEventListener abstract functions:
        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {}
    };
}
