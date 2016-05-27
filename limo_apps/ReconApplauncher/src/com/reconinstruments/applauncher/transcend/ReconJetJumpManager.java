package com.reconinstruments.applauncher.transcend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.SystemClock;
import android.util.Log;


import com.reconinstruments.applauncher.R;
import com.reconinstruments.notification.ReconNotification;
import com.reconinstruments.reconsensor.ReconSensor;

public class ReconJetJumpManager extends ReconStatsManager implements JumpEndEvent {
    private final static String TAG = ReconJetJumpManager.class.getSimpleName();
    public final static boolean DEBUG = false;

    ///////////////////////////////////////////////////////
    // Constants
    ///////////////////////////////////////////////////////

    public final static String BROADCAST_ACTION_STRING  = "RECON_MOD_BROADCAST_JUMP";
    public final static String JUMPINFO_FILE                            = "jumpinfo";

    private final static String PREF_FILE                                       = "JumpManagerPers";
    private final static int JUMP_FAILSAFE_TIME                         = 10000;

    ///////////////////////////////////////////////////////
    // Variables
    ///////////////////////////////////////////////////////
        
    /**
     * Handler for checking if jump detection has failed and need to run Fixing Task
     */
    private Handler mJumpFailSafeHandler;
        
    private Runnable mJumpFailSafeRunnable = new Runnable() {
                
            @Override
            public void run() {
                Log.e(TAG, "ERROR IN JUMP DETECTION : FAILSAFE ACTIVATED");
                mJumpAnalyzer.onFailSafeActivated();
            }
        };

    /**
     * Handler to talk to the TrescendService for writing jump
     */
    private ReconEventHandler  mReconEventHandler;

    /**
     * SensorManager to retrieve the sensors
     */
    private SensorManager mSensorManager;

    /**
     * FreeFall detection sensor
     */
    private Sensor mFFSensor;

    /**
     * Pressure Sensor
     */
    private Sensor mPressureSensor;

    /**
     * Jump Analyzer Instance That fires landed() callback whenever a jump is detected 
     */
    public JetJumpAnalyzer mJumpAnalyzer;


    /*-------------------------------------------
     * Variables used for jump detection
     *-------------------------------------------*/
    private static final int NUM_STORE_HORZ_SPEEDS = 5;
    private CircularFloatArray mHorzSpeeds;
    private CircularFloatArray mSpeeds;

    /**
     * We keep the counter independent of the size of jump incase we remove
     * elements from jump. So you can think of mJumpCounter as the
     * maximum number field of the jump 
     */
    private int mJumpCounter=0;
    private ArrayList<ReconJump> mJumps;
    private int mBestJumpIdx = -1;
    private ReconJump mAllTimeBestJump = new ReconJump();
    private float mLastPressure = 0f;
    private long mLastUpdateTime        = 0;

    /**
     * lag between FreeFall interrupt detection and propagation through Android Software Stack [ms]
     */
    public static final int FreeFallLAG = 100; 


    @Override
    public String getBasePersistanceName() {
        return TAG;
    }

    /*---------------------------------------------
     * Listeners
     *---------------------------------------------*/

    /**
     * FreeFall Event Listener
     */
    private final SensorEventListener mFFListener = new SensorEventListener() 
        {

            public void onAccuracyChanged(Sensor sensor, int accuracy)
            {}

            public void onSensorChanged(SensorEvent event) 
            {
                // unregister FreeFall and Pressure Listener first
                mSensorManager.unregisterListener(this);

                Date jstart = new Date ( event.timestamp / 1000000);
                String strLog = String.format("FreeFall Event Received!  Kernel Date: %s. Starting JumpAnalyzer... ",
                                              jstart.toString() );
                if (DEBUG) Log.i(TAG, strLog);


                        
                mLastPressure = mRTS.getReconAltManager().getPressure();
                mLastUpdateTime = mRTS.mTimeMan.mLastUpdate;

                        
                mJumpFailSafeHandler.postDelayed(mJumpFailSafeRunnable, JUMP_FAILSAFE_TIME);
                        
                // now start JumpAnalyzer which will call us back once Landing is detected
                // with, or without valid Jump Object
                mJumpAnalyzer.Begin(
                                    ReconJetJumpManager.this,     // callback to invoke when Landing is detected, with or without Jump object
                                    mLastPressure,         // last pressure measurement we did
                                    mLastUpdateTime,       // timestamp of last pressure measurement
                                    SystemClock.elapsedRealtime() );       // freefall timestamp
            }            
        };
        
    /**
     * Jump was detected before the time limit and clears out the timer
     */
    @Override
    public void stopTimer() {
        if (DEBUG) Log.d(TAG, "Stopping JUMP FAILSAFE TIMER ....");
        mJumpFailSafeHandler.removeCallbacks(mJumpFailSafeRunnable);
    }

    /**
     * Jump End Callback Interface implementation
     */
    @Override
    public void landed(ReconJump jump)
    {
        if (DEBUG) Log.d(TAG, "landed()");
                
        // if this was true jump, passed object will not be null
        // we save it in mLastJump and call updateMembers
        if (jump != null)
            {
                mLastJump = jump;
                this.updateMembers();
            }
                
        if (DEBUG && jump!= null){
            Log.d(TAG, "Landed! - jump.mAir:" + jump.mAir + " jump.mDate: " + jump.mDate + " jump.mDrop:" + jump.mDrop + " jump.mHeight:" + jump.mHeight);
        }

        // at any rate register again freefall Listener 
        mSensorManager.registerListener(
                                        (SensorEventListener) mFFListener,      
                                        mFFSensor, 
                                        SensorManager.SENSOR_DELAY_FASTEST);
    }

    /*---------------------------
     * instance methods
     *---------------------------*/

    public ReconJetJumpManager(ReconTranscendService rts, ReconEventHandler reh)
    {
        mJumps = new ArrayList<ReconJump>();
        mHorzSpeeds = new CircularFloatArray(NUM_STORE_HORZ_SPEEDS);
        mSpeeds = new CircularFloatArray(NUM_STORE_HORZ_SPEEDS);
        mReconEventHandler = reh;
        prepare(rts);
        generateBundle();
        mJumpFailSafeHandler = new Handler();

        // intialize sensor subsystem. If it fails, there will be no jumps
        if (this.InitSensors() == false)
            Log.e(TAG, "Android Sensor Framework failed to initialize");
    }

    public ArrayList<ReconJump> getJumps() { return mJumps;}
    public ReconJump getLastJump() { return mJumps.get(mJumps.size() - 1); }

    private ReconJump mLastJump;

    /**** SF INTEGRATION ****/

    // Initialize Sensor Subsystem (called from c-tor?)
    private boolean InitSensors()
    {
        // call from c-tor to initialize android sensor framework
        mSensorManager = (SensorManager)mRTS.getSystemService(mRTS.SENSOR_SERVICE);
        if (mSensorManager == null) {
            Log.e(TAG, "Android SENSOR_SERVICE failed to Initialize");
            return false;
        }

        /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
         * NOTE : Be careful and check if freefall sensor is properly configured in Kernel Configuration
         * and sensors.conf is properly formatted.
         !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
        if (mSensorManager.getSensorList(Sensor.TYPE_ALL).size() == 0){
            Log.d(TAG, "There is no availabe sensors, check if 'sensors.conf' is not well-formed / is corrupted");
            return false;
        }

        mPressureSensor = mSensorManager.getDefaultSensor(ReconSensor.EventType.RI_SENSOR_TYPE_FREEFALL);
        if (mPressureSensor == null) {
            Log.e(TAG, "Pressure Sensor not Available, Free Fall EVent will not be processed");
            return false;
        } 
                
        mFFSensor = mSensorManager.getDefaultSensor(ReconSensor.EventType.RI_SENSOR_TYPE_FREEFALL);
        if (mFFSensor == null) {
            Log.e(TAG, "RECON FreeFall Sensor not Available, possible wrong kernel configuration");
            return false;
        }
                

        String strLog = String.format("RECON FreeFall Sensor: Name [%s] Vendor [%s] Version [%d] Range [%f] Resolution [%f] Power [%f] Min Delay [%d]",
                                      mFFSensor.getName(), mFFSensor.getVendor(), mFFSensor.getVersion(), mFFSensor.getMaximumRange(), 
                                      mFFSensor.getResolution(), mFFSensor.getPower(), mFFSensor.getMinDelay() );
        Log.i(TAG, strLog);

        // get instance of Jump Analyzer
        mJumpAnalyzer = JetJumpAnalyzer.Instance(mRTS);
        if (mJumpAnalyzer == null)
            {
                Log.e(TAG, "Jump Analyzer Failed to Initialize\n");
                mFFSensor = null;

                return false;
            }

        // register FreeFall Listener. This is IRQ Sensor, so delay means nothing
        mSensorManager.registerListener(
                                        (SensorEventListener) mFFListener,      
                                        mFFSensor, 
                                        SensorManager.SENSOR_DELAY_FASTEST);

        // all ok
        Log.i(TAG, "Android Sensor Framework INIT - OK!");
        return true;
    }





    /**** SF INTEGRATION ****/  
    public void updateMembers()
    {
        /*** SF Integration. This version is now triggered on JumpEnd Event, 
             instead of timer polling from above. Good jump is saved in mLastJump ***/
        // push the speed value
        mHorzSpeeds.push(mRTS.getReconSpeedManager().getHorzSpeed());
        mSpeeds.push(mRTS.getReconSpeedManager().getSpeed());

        // increment and assign jumpcounter
        mJumpCounter++;
        mLastJump.mNumber = mJumpCounter;

        // Distance: Logic: Original approach was Based on
        // jump air calculate how long ago jump was
        // started and then from the array of stored
        // horizontal speeds retrieve the speed at that
        // time and calculate distance:

        // In the new approach We add one more twist
        // though. Usually at the time of takeoff, the
        // direction has changed so we calculate the angle
        // of take off as follows: From height we
        // calculate the vertical component of speed at
        // take off and from that we calculate the
        // horizontal component:
        // 
        // V_{vert}^2 = 2gh
        // V_{horz} = sqrt (V^2 - 2gh)
        // distance =  sqrt (V^2 - 2gh) * airTime
        int s_index = (int)(mLastJump.mAir);

        // Old approach: obsolete:
        //float distance = mHorzSpeeds.readPrevious(s_index)*(tempJump.mAir)/1000f;
        // New approach:
        float theSpeed = mSpeeds.readPrevious(s_index);//km/h
        if(theSpeed >=0)        
            {
                theSpeed=theSpeed/3.6f;  //m/s
                float v_2=theSpeed*theSpeed ;
                float par_2=2 * 9.8066f * mLastJump.mHeight;
                //float distance = (v_2<par_2) ? 0 : ((float)Math.sqrt(v_2-par_2)*mLastJump.mAir)/1000f;//m
                float distance = (v_2<par_2) ? (theSpeed*(float)mLastJump.mAir)/1000f : ((float)Math.sqrt(v_2-par_2)*(float)mLastJump.mAir)/1000f;//m
                //float distance = (theSpeed*(float)mLastJump.mAir)/1000f;

                // float distance = (theSpeed*theSpeed < (2 * 9.8f * mLastJump.mHeight))?
                // 0: ((float)Math.sqrt(theSpeed*theSpeed -
                // (2 * 9.8f * mLastJump.mHeight))*mLastJump.mAir)/1000f;
                Log.d(TAG,"distance: " + distance);
                mLastJump.mDistance = distance;                          
            }
        else                                                    
            mLastJump.mDistance =ReconJump.INVALID_DISTANCE;
        // Add it to the list
        mLastJump.updateBundle();
        mJumps.add(mLastJump);

        boolean bestToday = (mBestJumpIdx < 0 || mJumps.get(mBestJumpIdx).mAir < mLastJump.mAir);
        boolean bestAllTime = (mAllTimeBestJump == null || mAllTimeBestJump.mAir < mLastJump.mAir);

        Bundle notifExtrasBundle = new Bundle();
        notifExtrasBundle.putInt("position", 5);

        if(bestToday) {
            mBestJumpIdx = mJumps.size() - 1;
	    //R.drawable.notification_icon_alltime_air
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(1);
            df.setMinimumFractionDigits(1);
            double airTime = ((double) mJumps.get(mBestJumpIdx).mAir) / 1000;
	    // showPassiveNotification
        }
        if(bestAllTime) {
            mAllTimeBestJump = new ReconJump(mLastJump.getBundle());

            // Post all time best jump notification
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(1);
            df.setMinimumFractionDigits(1);
            double airTime = ((double) mAllTimeBestJump.mAir) / 1000;
	    // All time best air
	    //R.drawable.jump_icon,
	    //df.format(airTime)
	    // showPassiveNotification
        }
        /*** SF INTEGRATION ***/

        generateBundle();

        // Write to the event file
        mReconEventHandler.writeJumpEvent();

        // broadcast jump
        // HACK for jack. FIXME
        mBundle = mLastJump.generateBundle();
        // End of hack
        broadcastBundle(BROADCAST_ACTION_STRING);

        // Post notification
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(1);
        df.setMinimumFractionDigits(1);
        double airTime = ((double) mLastJump.mAir) / 1000;
	// showPassiveNotification
	//"Last Air", df.format(airTime) + "s"

        // undo hack for other apps FIXME
	mBundle = null;
        generateBundle();
    }

    @Override
    protected Bundle generateBundle() {
        super.generateBundle();

        // put jump counter
        mBundle.putInt("JumpCounter", mJumpCounter);
        if (mBestJumpIdx >= 0 & mBestJumpIdx < mJumps.size())
            mBundle.putBundle("BestJump", mJumps.get(mBestJumpIdx).getBundle());
        if (mAllTimeBestJump != null)
            mBundle.putBundle("AllTimeBestJump", mAllTimeBestJump.getBundle());
        mBundle.putInt("BestJumpIndex", mBestJumpIdx);

        // Generate the bundle array
        ArrayList<Bundle> jumps = new ArrayList<Bundle>();
        for (int i = 0; i < mJumps.size(); i++) {
            jumps.add(mJumps.get(i).getBundle());
        }
        mBundle.putParcelableArrayList("Jumps", jumps);
        return mBundle;
    }

    public void loadStatsFromBundle(Bundle b) {
        mJumps = new ArrayList<ReconJump>();
        mBestJumpIdx = b.getInt("BestJumpIndex", -1);
        mAllTimeBestJump = new ReconJump(b.getBundle("AllTimeBestJump"));

        // Get the jump counter
        mJumpCounter = b.getInt("JumpCounter");

        // Get the list of jumps from bundle
        ArrayList<Bundle> jumps = b.getParcelableArrayList("Jumps");

        // Go through bundle (individual jump bundles) and construct jumps and
        // add to list
        for (int i = 0; i < jumps.size(); i++) {
            mJumps.add(new ReconJump(jumps.get(i)));
        }
    }


    private Bundle readStatsFromFileIntoBundle() {
        // This is a helper function

        // Here is the logic: Read the file into bytearray. Generate
        // parcel from bytearray (unmarshal). Generate bundle from
        // parcel. Load stats from bundle

        // Make sure that there is a good file
        File fi = new File(mRTS.getFilesDir(), JUMPINFO_FILE);
        int fileSize = (int) fi.length();
        Log.d(TAG, "File size is " + fi.length());
        if (fileSize > 0) {
            try {
                // read file
                FileInputStream f = mRTS.openFileInput(JUMPINFO_FILE);
                byte[] byteArray = new byte[fileSize];
                f.read(byteArray);

                // generate parcel
                final Parcel p = Parcel.obtain();
                p.unmarshall(byteArray, 0, fileSize);

                // Generate bundle from parcel
                p.setDataPosition(0);
                Bundle b = p.readBundle();
                return mBundle;

            } catch (IOException e) {
                Log.e(TAG, "Couldn't read jump info");
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void loadLastState() {
        Bundle b = readStatsFromFileIntoBundle();
        // load stats from bundle
        loadStatsFromBundle(b);
        // Apply the hack for all time best jump
        loadAllTimeStats();
    }

    @Override
    public void saveState() {
        // All you need to do is save the bundle

        // Here is the logic: Generate parcel from Bundle. Generate
        // file from parcel (marshal). Save file) So serialize and
        // attack
        Log.d(TAG, "saving jump info");
        Parcel p = Parcel.obtain();
        mBundle.writeToParcel(p, 0);
        FileOutputStream fos = null;

        try {
            fos = mRTS.openFileOutput(JUMPINFO_FILE, Context.MODE_PRIVATE);
            Log.d(TAG, "Attempt to marshal and write");
            fos.write(p.marshall());
            Log.d(TAG, "Attempt to flush");
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {

            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Now doing the alltime stats
        saveAllTimeBestJump();

    }

    @Override
    public void resetAllTimeStats() {
        if(mBestJumpIdx > -1 && mBestJumpIdx < mJumps.size())
            mAllTimeBestJump = new ReconJump((Bundle) mJumps.get(mBestJumpIdx).getBundle().clone());
        else
            mAllTimeBestJump = new ReconJump();
        generateBundle();
    }

    @Override
    public void resetStats()
    {
        mJumps = new ArrayList<ReconJump>();
        mJumpCounter = 0;       // We keep the counter independent of
        mBestJumpIdx = -1;
        generateBundle();
    }

    @Override
    public void loadAllTimeStats() {
        loadAllTimeBestJump();
        
        // FIXME
        // Bundle b = readStatsFromFileIntoBundle();
        // if (b != null) {
        // mAllTimeBestJump = new ReconJump(b.getBundle("AllTimeBestJump"));
        // }

    }

    // This is quick and dirty. Need to redo later
    private void saveAllTimeBestJump() {
        SharedPreferences.Editor editor = mPersistantStats.edit();              
        editor.putInt("AllTimeBestJumpAir",mAllTimeBestJump.mAir);
        editor.putFloat("AllTimeBestJumpDistance",mAllTimeBestJump.mDistance);
        editor.putFloat("AllTimeBestJumpDrop",mAllTimeBestJump.mDrop);
        editor.putFloat("AllTimeBestJumpHeight",mAllTimeBestJump.mHeight);
        editor.commit();

    }

    // This is quick and dirty need to redo later
    private void loadAllTimeBestJump() {
        mAllTimeBestJump = new ReconJump();
        mAllTimeBestJump.mAir =
            mPersistantStats.getInt("AllTimeBestJumpAir",0);
        mAllTimeBestJump.mDistance =
            mPersistantStats.getFloat("AllTimeBestJumpDistance",0);
        mAllTimeBestJump.mHeight =
            mPersistantStats.getFloat("AllTimeBestJumpHeight",0);
        mAllTimeBestJump.mDrop =
            mPersistantStats.getFloat("AllTimeBestJumpDrop",0);

    }

    static private byte[] readTape(int numBytes, InputStream inTape) throws FileNotFoundException, IOException 
    {
        byte[] buf = new byte[numBytes];
        try
            {
                int size;
                size = inTape.read(buf);
                if (size == numBytes)
                    {
                        return buf;
                    }
            }

        finally
            {
            }

        return null;
    }
}