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
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Button;
import android.widget.TextView;

public class MotionDetectorProfile extends Activity implements SensorEventListener, LocationListener {

    public static final String TAG = "MotionDetectorProfile";

    //Pulls settings from the rest of the application
    private SharedPreferences mPreferences;

    /*
     * Constant values for SensorEvent axes
     */

    private static final int X_AXIS_VALUE = 0;
    private static final int Y_AXIS_VALUE = 1;
    private static final int Z_AXIS_VALUE = 2;

    /*
     * Constant flags for turning off/on logging + polling aspects (Logging, Gyroscope, GPS)
     */

    //USE LOGGING PARAMATER WILL COMPLETELY DISABLE LOGGING IF DISABLED
    private static final boolean USE_LOG = true;

    //USE GYRO PARAMATER WILL COMPLETELY DISABLE GYRO IF DISABLED
    private static final boolean USE_GYRO = true;

    //USE GPS PARAMETER WILL COMPLETELY DISABLE GPS IF DISABLED
    private static final boolean USE_GPS = true;

    /*
     * Constant values related to poll frequency
     */

    //Accelerometer reading frequency
    private static final int ACCEL_VALUES = 25; //was 25

    //Sampling period for accelerometer; defined as 1,000,000 microseconds / ACCEL_VALUES
    private static final int SAMPLING_RATE = 1000*1000/ACCEL_VALUES;

    //Highest index of an array defined by ACCEL_VALUES
    private static final int HIGHEST_ARR_VAL = ACCEL_VALUES - 1;

    /*
     * Simple values used for calculation and state determination
     */

    //A class for managing movement algorithms through variance of raw data
    private VarianceManager mVarianceManager;

    //A simple ticker to determine when an accelerometer variance cycle is done
    private int mAccelTick;

    //A simple ticker to determine when a gyro cycle is done
    //NOTE: Is this needed?
    //private int mGyroTick;

    //an array to store raw data values from the accelerometer
    private float[] mRawAccelValues;

    //an array to store raw data values from the gyroscope
    //first dimension reserved for mGyroTick (Currently unused but could be used to simulate FIFO dump) Necessary?
    private float[][] mRawGyroValues;

    //Application start time used for zeroing start
    private long mStartTime;

    //Status for both types of profile
    private enum MovementState { 
        STOP(0), WALK(1), COAST(2), DONE(3), JOG(4), PEDAL(5);

        private final int state;
        MovementState(int state) { this.state = state; }
        public int getState() { return isMoving() + isFast(); }
        public int isMoving() { return state > 0 ? 1 : 0; }
        public int isFast() { return state > 3 ? 1 : 0; }
    };

    //The profiles to be initialized
    private MovementState mMovementProfile[];

    private int mProfileTimes[];

    private int mCurrentState;

    //Counts up to ~6750 for 270 seconds worth of 25Hz data logging
    private int mProfileTicker;

    /*
     * Frameworks for collecting data from sensors through Android
     */

    //The SensorManager
    private SensorManager mSensorManager;

    //The Accelerometer sensor
    private Sensor mAccelerometer;

    //The Gyroscope sensor
    private Sensor mGyroscope;

    /*
     * Frameworks for collecting data from GPS through Android
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
    private static RelativeLayout sMainLayout;

    //The textbox which counts down to the next shift
    private static TextView sCountdownText;

    //The textbox which states whether the user should be moving
    private static TextView sMovingText;

    //The textbox which states the next user movement state
    private static TextView sNextText;

    //The textbox which states the current variance
    private static TextView sVarianceText;

    /*
     * This is called upon creation of the Application
     *   All initialization of values is to be done here
     */

    @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.motion_profile);

        //Initialize settings
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Initialize profile

        if (mPreferences.getString("Profile","").equals("Walk1")) {
            mMovementProfile = new MovementState[]{ MovementState.STOP,
                                                    MovementState.WALK,
                                                    MovementState.STOP,
                                                    MovementState.WALK,
                                                    MovementState.JOG,
                                                    MovementState.STOP,
                                                    MovementState.JOG,
                                                    MovementState.WALK,
                                                    MovementState.STOP,
                                                    MovementState.DONE};

            mProfileTimes = new int[]{30, 30, 30, 30, 30, 30, 30, 30, 30, 0};
        }
        else {
            mMovementProfile = new MovementState[]{ MovementState.STOP,
                                                    MovementState.PEDAL,
                                                    MovementState.STOP,
                                                    MovementState.PEDAL,
                                                    MovementState.COAST,
                                                    MovementState.STOP,
                                                    MovementState.PEDAL,
                                                    MovementState.COAST,
                                                    MovementState.PEDAL,
                                                    MovementState.STOP,
                                                    MovementState.DONE};

            mProfileTimes = new int[]{30, 30, 30, 30, 15, 30, 30, 15, 30, 30, 0};
        }

        //Initialize Timers
        mStartTime = getTime();

        //Initialize data logging if enabled
        if (USE_LOG) {
            mFileLog = new FileLogger();
            if (!mFileLog.Activate("Motion-Detector-Profile.csv"))
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

        //Initialize Layout

        sMainLayout = (RelativeLayout) findViewById(R.id.motion_profile);

        sCountdownText = (TextView) findViewById(R.id.countdowntext);
        sMovingText = (TextView) findViewById(R.id.movingtext);
        sNextText = (TextView) findViewById(R.id.nexttext);
        sVarianceText = (TextView) findViewById(R.id.variance);

        //Initialize sensors if enabled
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SAMPLING_RATE);
        if (USE_GYRO) {
            mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorManager.registerListener(this, mGyroscope, SAMPLING_RATE);
        }

        //Initialize GPS if enabled
        if (USE_GPS) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    //In theory this is the only exit function that should be called
    @Override
    public void onBackPressed() {
        if (USE_LOG)
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
        if (USE_LOG)
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
        if (USE_LOG)
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
            mRawGyroValues[0][X_AXIS_VALUE] = event.values[X_AXIS_VALUE];
            mRawGyroValues[0][Y_AXIS_VALUE] = event.values[Y_AXIS_VALUE];
            mRawGyroValues[0][Z_AXIS_VALUE] = event.values[Z_AXIS_VALUE];
        }

        //get Accel values + Calculate movement every second
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mRawAccelValues[mAccelTick] = event.values[Z_AXIS_VALUE];

            mProfileTicker++;
            mAccelTick++;

            if (mAccelTick == ACCEL_VALUES) {
                mVarianceManager.addAccVariance(mRawAccelValues);
                mAccelTick = 0;

                mProfileTimes[mCurrentState]--;
                if (mProfileTimes[mCurrentState] == 0 && mMovementProfile[mCurrentState + 1] != MovementState.DONE)
                    mCurrentState++;

                //Update Current task
                switch (mMovementProfile[mCurrentState]) {
                case STOP:
                    sMovingText.setText("PLEASE STOP");
                    //Set background light red
                    sMainLayout.setBackgroundColor(Color.rgb(255,127,127));
                    break;
                case WALK:
                    sMovingText.setText("PLEASE WALK");
                    //Set background light green
                    sMainLayout.setBackgroundColor(Color.rgb(127,255,127));
                    break;
                case COAST:
                    sMovingText.setText("PLEASE COAST");
                    //Set background light green
                    sMainLayout.setBackgroundColor(Color.rgb(127,255,127));
                    break;
                case JOG:
                    sMovingText.setText("PLEASE JOG");
                    //Set background light blue
                    sMainLayout.setBackgroundColor(Color.rgb(127,127,255));
                    break;
                case PEDAL:
                    sMovingText.setText("PLEASE PEDAL");
                    //Set background light blue
                    sMainLayout.setBackgroundColor(Color.rgb(127,127,255));
                    break;
                default:
                    sMovingText.setText("ERR");
                    sMainLayout.setBackgroundColor(Color.rgb(0,0,0));
                    break;
                }
                sMainLayout.invalidate();

                //Update Next task
                switch (mMovementProfile[mCurrentState + 1]) {
                case STOP:
                    sNextText.setText("Next status: STOP");
                    break;
                case WALK:
                    sNextText.setText("Next status: WALK");
                    break;
                case COAST:
                    sNextText.setText("Next status: COAST");
                    break;
                case JOG:
                    sNextText.setText("Next status: JOG");
                    break;
                case PEDAL:
                    sNextText.setText("Next status: PEDAL");
                    break;
                default:
                    sNextText.setText("Next status: DONE");
                    break;
                }

                sCountdownText.setText("" + mProfileTimes[mCurrentState]);
                sVarianceText.setText("Variance: " + mVarianceManager.getVariance());
            }

            //Continue to log data as long as we are within the logging profile

            if ((USE_LOG) && (mProfileTicker < 270*ACCEL_VALUES)) {

                //Pull all raw data to /storage/sdcard/Motion-detector-profile.csv accounting for sensor flags
                mFileLog.WriteToFile(event.values[X_AXIS_VALUE] + "," + event.values[Y_AXIS_VALUE] + "," + 
                                     event.values[Z_AXIS_VALUE] + "," + mVarianceManager.getVariance() + "," + 
                                     (getTime() - mStartTime) + "," + booleanToInt(calcMovement()) + "," +
                                     mMovementProfile[mCurrentState].isMoving() + "," +
                                     mMovementProfile[mCurrentState].isFast() + "," +
                                     mMovementProfile[mCurrentState].getState());
                if (USE_GYRO)
                    mFileLog.WriteToFile("," + mRawGyroValues[0][X_AXIS_VALUE] + 
                                         "," + mRawGyroValues[0][Y_AXIS_VALUE] + 
                                         "," + mRawGyroValues[0][Z_AXIS_VALUE]);
                if (USE_GPS)
                    mFileLog.WriteToFile("," + mSpeed + "," + mLatitude + "," + mLongitude + "," + mAccuracy);
                mFileLog.WriteToFile("\n");
            }

            //Once we finish logging profile, enter end state
            //  uninitialize sensors and deactivate logging+GPS

            else {
                sMovingText.setText("TEST DONE");
                sNextText.setText("Please exit this test");
                sMainLayout.setBackgroundColor(Color.rgb(200,200,200));
                sMainLayout.invalidate();

                if (USE_LOG)
                    mFileLog.DeActivate();
                if (mSensorManager != null)
                    mSensorManager.unregisterListener(this);
                if (mLocationManager != null)
                    mLocationManager.removeUpdates(this);
            }
        }
    }

    //Calculate movement based on variance in accordance of the R&D algorithm
    private boolean calcMovement() {
        if (mPreferences.getString("Algorithm", "").equals("Etienne1"))
            return mVarianceManager.etienneAlgorithm();
        else
            return mVarianceManager.ahmedAccAlgorithm();
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