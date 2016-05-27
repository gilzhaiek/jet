package com.reconinstruments.motiondetector;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MotionDetectorDisplay extends Activity implements SensorEventListener, LocationListener {

    public static final String TAG = "MotionDetectorDisplay";

    //Preferences sheet that is constant between all activities in this application
    private SharedPreferences mPreferences;

    /*
     * Constant values for SensorEvent axes
     */

    private static final int X_AXIS_VALUE = 0;
    private static final int Y_AXIS_VALUE = 1;
    private static final int Z_AXIS_VALUE = 2;

    /*
     * Constant flags for turning off/on display + polling aspects (Gyroscope, GPS)
     */

    //USE_GYRO PARAMETER WILL COMPLETELY DISABLE GYRO IF DISABLED
    private static final boolean USE_GYRO = true;

    //USE_GPS PARAMETER WILL COMPLETELY DISABLE GPS IF DISABLED
    private static final boolean USE_GPS = true;

    //USE_DISP PARAMATER WILL COMPLETELY DISABLE DISPLAY IF ENABLED
    private static final boolean USE_DISP = true;

    /*
     * Settings flags for turning off/on logging + TODO: polling aspects (Logging, Gyroscope, GPS, etc.)
     */

    //Turn off logging
    private static boolean sUseLog;

    /*
     * Constant values related to poll frequency
     */

    //Accelerometer reading frequency DEFAULT: 25
    private static final int ACCEL_VALUES = 25;

    //Sampling period for accelerometer; defined as 1,000,000 microseconds / ACCEL_VALUES
    private static final int SAMPLING_RATE = 1000*1000/ACCEL_VALUES;

    //Highest index of an array defined by ACCEL_VALUES
    private static final int HIGHEST_ARR_VAL = ACCEL_VALUES - 1;

    /*
     * Simple values used for calculation and state determination
     */

    //A class for managing movement algorithms through variance of raw data
    private VarianceManager mVarianceManager;

    //A class for managing movement algorithms through GPS data
    private GPSCycleManager mGpsCycleManager;

    //A simple ticker to determine when an accelerometer variance cycle is done
    private int mAccelTick;

    //A simple ticker to determine how long a gyro variance cycle is
    private int mGyroTick;

    //an array to store raw data values from the accelerometer (z)
    private float[] mRawAccelValues;

    //an array to store raw data values from the gyroscope (x,y,z)
    private float[][] mRawGyroValues;

    //Application start time used for zeroing start
    private long mStartTime;

    //An enum to store when the user claims to be moving for logging
    private enum MovementState {
        STOP(0), WALK(1), JOG(2);

        private final int state;
        MovementState(int state) { this.state = state; }
        public int getState() { return state; }
        public int isMoving() { return state > 0 ? 1 : 0; }
        public int isFast() { return state > 1 ? 1 : 0; }
    };

    private MovementState mMovementState;

    /*
     * Frameworks for collecting data from sensors
     */

    //The SensorManager
    private SensorManager mSensorManager;

    //The Accelerometer sensor
    private Sensor mAccelerometer;

    //The Gyroscope sensor
    private Sensor mGyroscope;

    /*
     * Frameworks for collecting data from GPS
     */

    //The LocationManager
    private LocationManager mLocationManager;

    //Some GPS logging vars
    private float mSpeed;
    private float mAccuracy;
    private float mLatitude;
    private float mLongitude;

    /*
     * Class for logging data to a csv file
     */

    //The Filelogger used to store a .csv file at /storage/sdcard0
    protected FileLogger mFileLog;

    /*
     * Frameworks for manipulating UI through Android
     */

    //The overall RelativeLayout; used to change background colour
    private static RelativeLayout mMainLayout;

    //The GUI buttons
    private Button mStopButton;
    private Button mWalkButton;
    private Button mJogButton;

    //The textbox which states whether the user is calculated to be moving or not
    private static TextView sMovementText;

    //The textbox which states the current variance
    private static TextView sVarianceText;

    /*
     * This is called upon creation of the Application
     *   All initialization of values is to be done here
     */

    @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.motion_display);

        //Load settings
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (mPreferences.getString("Logging", "").equals("True"))
            sUseLog = true;
        else
            sUseLog = false;

        //Initialize data logging if enabled
        if (sUseLog) {

            mStartTime = getTime();

            mFileLog = new FileLogger();
            if (!mFileLog.Activate("Motion-Detector-Display.csv"))
                return;

            mFileLog.WriteToFile("Raw AccX, Raw AccY, Raw AccZ, Variance, time, Calcmoving, Stop/move, Slow/fast, Movestatus");
            if (USE_GYRO)
                mFileLog.WriteToFile(", Raw GyroX, Raw GyroY, Raw GyroZ");
            if (USE_GPS)
                mFileLog.WriteToFile(", GPSspeed, GPSlat, GPSlon, GPSAccu");
            mFileLog.WriteToFile("\n");
        }

        //Initialize Variance values
        mVarianceManager = new VarianceManager();
        mRawAccelValues = new float[ACCEL_VALUES];

        if (USE_GYRO)
            mRawGyroValues = new float[ACCEL_VALUES][3];

        //Initialize layout if enabled
        if (USE_DISP || sUseLog)
            mMovementState = MovementState.STOP;

        if (USE_DISP) {
            mMainLayout = (RelativeLayout) findViewById(R.id.motion_display);

            sMovementText = (TextView) findViewById(R.id.movingtext);
            sVarianceText = (TextView) findViewById(R.id.variance);

            mStopButton = (Button) findViewById(R.id.stopbutton);
            mStopButton.setOnClickListener( new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mMovementState = MovementState.STOP;
                    }
                });

            mWalkButton = (Button) findViewById(R.id.walkbutton);
            mWalkButton.setOnClickListener( new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mMovementState = MovementState.WALK;
                    }
                });

            mJogButton = (Button) findViewById(R.id.jogbutton);
            mJogButton.setOnClickListener( new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mMovementState = MovementState.JOG;
                    }
                });
        }

        //Initialize sensors if enabled
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SAMPLING_RATE);
        if (USE_GYRO) {
            mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorManager.registerListener(this, mGyroscope, SAMPLING_RATE);
        }

        //Initialize GPS if enabled
	mGpsCycleManager = new GPSCycleManager();
        if (USE_GPS) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    //In theory this is the only exit function that should be called
    @Override
    public void onBackPressed() {
        if (sUseLog)
            mFileLog.DeActivate();
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
        if (mLocationManager != null)
            mLocationManager.removeUpdates(this);
        super.onBackPressed();
    }

    //What to do when activity is hidden
    @Override
    public void onPause() {
        if (sUseLog)
            mFileLog.DeActivate();
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
        if (mLocationManager != null)
            mLocationManager.removeUpdates(this);
        super.onPause();
    }

    //What to do when activity is stopped
    @Override
    public void onStop() {
        if (sUseLog)
            mFileLog.DeActivate();
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
        if (mLocationManager != null)
            mLocationManager.removeUpdates(this);
        super.onStop();
    }

    /*
     * Constant polling function which updates every time a new value is
     *   collected from sensors (Accelerometer, Gyroscope)
     * Should be called ACCEL_VALUES*#ofsensors times per second
     * Most calculation is done here
     */

    @Override
    public void onSensorChanged(SensorEvent event) {

        //get Gyro values if enabled
        if (USE_GYRO && (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)) {
            mRawGyroValues[mGyroTick][X_AXIS_VALUE] = event.values[X_AXIS_VALUE];
            mRawGyroValues[mGyroTick][Y_AXIS_VALUE] = event.values[Y_AXIS_VALUE];
            mRawGyroValues[mGyroTick][Z_AXIS_VALUE] = event.values[Z_AXIS_VALUE];
            if (mGyroTick < HIGHEST_ARR_VAL)
                mGyroTick++;
        }

        //get Accel values + Calculate movement every second
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mRawAccelValues[mAccelTick] = event.values[Z_AXIS_VALUE];

            mAccelTick++;

            if (mAccelTick == ACCEL_VALUES) {
                mVarianceManager.addAccVariance(mRawAccelValues);
                mAccelTick = 0;

                if (USE_GYRO) {
                    mVarianceManager.addGyroVariance(mRawGyroValues, mGyroTick);
                    mGyroTick = 0;
                }

                //Only update display if allowed
                if (USE_DISP) {

                    if (calcMovement()) {
                        sMovementText.setText("Moving status: TRUE");
                        mMainLayout.setBackgroundColor(Color.GREEN);
                        mMainLayout.invalidate();
                    }
                    else {
                        sMovementText.setText("Moving status: FALSE");
                        mMainLayout.setBackgroundColor(Color.RED);
                        mMainLayout.invalidate();
                    }
                    sVarianceText.setText("Variance: " + mVarianceManager.getVariance());
                }
            }

            //Push all raw data to /storage/sdcard/Motion-Detector-Display.csv if enabled
            if (sUseLog) {
                mFileLog.WriteToFile(event.values[X_AXIS_VALUE] + "," + event.values[Y_AXIS_VALUE] + "," + 
                                     event.values[Z_AXIS_VALUE] + "," + mVarianceManager.getVariance() + "," + 
                                     (getTime() - mStartTime) + "," + booleanToInt(calcMovement()) + "," + 
                                     mMovementState.isMoving() + "," + mMovementState.isFast() + "," +
                                     mMovementState.getState());
                if(USE_GYRO)
                    mFileLog.WriteToFile("," + mRawGyroValues[0][X_AXIS_VALUE] + 
                                         "," + mRawGyroValues[0][Y_AXIS_VALUE] + 
                                         "," + mRawGyroValues[0][Z_AXIS_VALUE]);
                if (USE_GPS)
                    mFileLog.WriteToFile("," + mSpeed + "," + mLatitude + "," + mLongitude + "," + mAccuracy);
                mFileLog.WriteToFile("\n");
            }
        }
    }

    //Calculate movement based on variance in accordance of the R&D algorithm
    private boolean calcMovement() {
        if (mPreferences.getString("Algorithm", "").equals("Etienne1"))
            return mVarianceManager.etienneAlgorithm();
	else if (mPreferences.getString("Algorithm", "").equals("EasyBike"))
	    return mGpsCycleManager.movementAlgorithm();
        else if (!USE_GYRO || mPreferences.getString("Algorithm", "").equals("Ahmed1"))
            return mVarianceManager.ahmedAccAlgorithm();
        else
            return mVarianceManager.ahmedAccGyroAlgorithm();
    }

    //Return system time
    private long getTime() { return System.currentTimeMillis(); }

    //Convert a Boolean to an integer bit
    private int booleanToInt(boolean x) { return x ? 1 : 0; }

    //Collect GPS data
    @Override
    public void onLocationChanged(Location location) {
        mSpeed = (float)location.getSpeed();
        mLatitude = (float)location.getLatitude();
        mLongitude = (float)location.getLongitude();
        mAccuracy = (float)location.getAccuracy();
	mGpsCycleManager.addNewSpeed(mSpeed);
    }

    /*
     * A bunch of overridden abstract functions
     */

    //SensorEventListener abstract functions:
    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {}

    //LocationListener abstract functions:
    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}