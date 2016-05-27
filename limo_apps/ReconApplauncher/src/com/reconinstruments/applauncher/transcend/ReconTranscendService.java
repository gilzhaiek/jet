//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.stats.TranscendUtils;
import com.reconinstruments.utils.BundleUtils;

public class ReconTranscendService extends Service implements SensorEventListener {
    public static int PRESSURE_UPDATE_RATE = 1000000; // in nanoseconds
    public static int DATA_UPDATE_RATE_IN_SEC = 1;
    //////////////////////                             
    public static final  boolean USE_MOCK_DATA = false;//FIXME disbale for release
    //-----------------------------------------------------------
    // Note the order of initializing these variables is important
    // because Some aggregate others. The deceleration here is the
    // order of initialization
    ReconBLEServiceProvider mBLE;//For faster access
    ReconTimeManager mTimeMan;
    ReconLocationManager mRecLocMan;
    ReconAltitudeManager mAltMan;
    ReconSpeedManager mSpeedMan;
    ReconDistanceManager mDistMan;
    ReconGradeManager mGradMan;
    ReconRunManager mRunMan;
    ReconTemperatureManager mTempMan;
    ReconVerticalManager mVertMan;
    ReconEventHandler mEventHandler;
    ReconChronoManager mChronoMan;
    ReconJetJumpManager mJumpMan;
    ReconHRManager mHRMan;
    ReconCadenceManager mCadenceMan;
    ReconPowerManager mPowerMan;
    SportsActivityManager mSportsActivityMan;
    ReconCalorieManager mCalorieMan;
    //-------------------------------------------------------------
    ReconDataLogHandler mDataLogHandler;
    // Helper aggregates:
    AndroidLocationListener mAndroidLocListen;
    //-------------------------------------------------------------
    Bundle mFullInfo = null;
    static final boolean DEBUG_USING_TOAST = false;
    private static final String TAG = "ReconTranscend_service";
    private SensorManager mSensorManager;
    private ReconMockDataProvider mMockDataProvider;
    private shutdownReceiver mShutdownReceiver;
    boolean mIgnoreIncomingMessages = false;    // will set to true when shutting down
    private Handler mHandler; 
    private OnAlarmReceiver mOnAlarmReceiver;
    private boolean mIsStarted = false;
    //Handler for remote shit
    final Messenger mMessenger = new Messenger(new IncomingHandler(this));
    BroadcastCommandReceiver mBroadcastCommandReceiver = new BroadcastCommandReceiver(this);
    public static final String REBOOT_COMMAND =
        "com.reconinstruments.REBOOT";
    public static final String SHUTDOWN_COMMAND =
        "com.reconinstruments.SHUTDOWN";
    //----------------------------------------------
    // Used by the new API to access to transcend service API 
   private static Intent sFullInfoIntent =
        new Intent(TranscendUtils.FULL_INFO_UPDATED);
    
    
    //Interface to get all that Shit
    // Bunch of stupid getters ---------------------------------------
    public ReconTimeManager getReconTimeManager(){return mTimeMan; }
    public ReconAltitudeManager getReconAltManager(){return mAltMan;}
    public ReconSpeedManager getReconSpeedManager(){return mSpeedMan;}
    public ReconDistanceManager getReconDistanceManager(){return mDistMan;}
    public ReconGradeManager getReconGradeManager(){return mGradMan;}
    public ReconJetJumpManager getReconJumpManager(){return mJumpMan;}
    public ReconRunManager getReconRunManager(){return mRunMan;}
    public ReconTemperatureManager getReconTemperatureManager(){return mTempMan;}
    public ReconVerticalManager getReconVerticalManager(){return mVertMan;}
    public ReconLocationManager getReconLocationManager(){return mRecLocMan;}
    public ReconChronoManager getReconChronoManager() {return mChronoMan;}
    public ReconHRManager getReconHRManager() {return mHRMan;}
    public ReconCadenceManager getReconCadenceManager() {return mCadenceMan;}
    public ReconPowerManager getReconPowerManager() {return mPowerMan;}
    public SportsActivityManager getSportsActivityManager() {return mSportsActivityMan;}
    public ReconCalorieManager getReconCalorieManager() {return mCalorieMan;}
    public ReconDataLogHandler getReconDataLogHandler() {return mDataLogHandler;}
    public AndroidLocationListener getAndroidLocationListener() {
        return mAndroidLocListen;}
    public ReconEventHandler getReconEventHandler() {
        return mEventHandler;
    }
    //--------------------------------------------------------------------
    // Managers List; all managers are going to be put in the
    // following list so batch operations can be done on them.
    private ArrayList<ReconStatsManager> mStatsManagers =
        new ArrayList<ReconStatsManager>(); 
    private Bundle generateFullInfoBundle(){
        if (mFullInfo == null) mFullInfo = new Bundle();
        mFullInfo.putBundle("ALTITUDE_BUNDLE", mAltMan.getBundle());
        mFullInfo.putBundle("SPEED_BUNDLE", mSpeedMan.getBundle());
        mFullInfo.putBundle("DISTANCE_BUNDLE", mDistMan.getBundle());
        mFullInfo.putBundle("JUMP_BUNDLE", mJumpMan.getBundle());
        mFullInfo.putBundle("GRADE_BUNDLE", mGradMan.getBundle());
        mFullInfo.putBundle("RUN_BUNDLE", mRunMan.getBundle());
        mFullInfo.putBundle("TEMPERATURE_BUNDLE", mTempMan.getBundle());
        mFullInfo.putBundle("VERTICAL_BUNDLE", mVertMan.getBundle());
        mFullInfo.putBoolean("LOCATION_BUNDLE_VALID", mRecLocMan.bundleValid());
        mFullInfo.putBundle("LOCATION_BUNDLE", mRecLocMan.getBundle());
        mFullInfo.putBundle("CHRONO_BUNDLE", mChronoMan.getBundle());
        mFullInfo.putBundle("HR_BUNDLE", mHRMan.getBundle());
        mFullInfo.putBundle("POWER_BUNDLE", mPowerMan.getBundle());
        mFullInfo.putBundle("CADENCE_BUNDLE", mCadenceMan.getBundle());
        mFullInfo.putBundle("SPORTS_ACTIVITY_BUNDLE", mSportsActivityMan.getBundle());
        mFullInfo.putBundle("CALORIE_BUNDLE", mCalorieMan.getBundle());
        mFullInfo.putBundle("TIME_BUNDLE", mTimeMan.getBundle());
        return mFullInfo;
    }
    public Bundle getFullInfoBundle(){
        return mFullInfo;
    }
    private void broadcastFullInfoBundle() {
        // No need to call removeStickyBroadcast() here, as
        // sendStickyBroadcast() will overwrite the sticky intent
        // every time as long as the Intent Extras are the only
        // properties changed.  Having removeStickyBroadcast() here
        // adds a concurrency issue, in which a broadcast can be
        // removed just before a client (like TranscendUtils) can read
        // it, resulting in it returning null sometimes.
        sFullInfoIntent.putExtra("FullInfo",mFullInfo);
        sendStickyBroadcast(sFullInfoIntent);
    }

    /**
     * Turns on GPS<br/> REASON : because aid files were not created
     * during shutdown process, we decided to turn off gps to let gps
     * to create aid files
     */
    private void turnOnGPS(){
        Context context = ReconTranscendService.this;

        String provider = Settings.Secure.getString(context.getContentResolver(),
                                                    Settings.Secure
                                                    .LOCATION_PROVIDERS_ALLOWED);
        if (! provider.contains("gps")) { //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings",
                              "com.android.settings.widget.SettingsAppWidgetProvider"); 
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3")); 
            context.sendBroadcast(poke);
        }
    }
    
    /**
     * Turns off gps
     */
    private void turnOffGPS() {
        Context context = ReconTranscendService.this;

        String provider = Settings.Secure.getString(context.getContentResolver(),
                                                    Settings.Secure
                                                    .LOCATION_PROVIDERS_ALLOWED);
        Log.d(TAG, "gps provider: " + provider);
        if (provider.contains("gps")) { //if gps is enabled
            Log.d(TAG, "trying to turn off gps!");
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings",
                              "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3")); 
            context.sendBroadcast(poke);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    @Override
    public void onCreate(){
        Log.d(TAG,"onCreate version 2");

        // JET-412 GPS aid files are not created, need to turn off GPS
        // before shutdown It is a temporary fix for creation of GPS
        // aid files requested by Gil added by :
        // patrick@reconinstruments.com
        turnOnGPS();

        //Initialize all members. Beware of order They depend on eachother
        mBLE = new ReconBLEServiceProvider(false,this);
        mTimeMan = new ReconTimeManager(this);
        mStatsManagers.add(mTimeMan);
        mRecLocMan = new ReconLocationManager(this);
        mStatsManagers.add(mRecLocMan);
        mAltMan = new ReconAltitudeManager(this);
        mStatsManagers.add(mAltMan);
        mSpeedMan = new ReconSpeedManager(this);
        mStatsManagers.add(mSpeedMan);
        mDistMan = new ReconDistanceManager(this);
        mStatsManagers.add(mDistMan);
        mGradMan = new ReconGradeManager(this);
        mStatsManagers.add(mGradMan);
        mRunMan = new ReconRunManager(this);
        mStatsManagers.add(mRunMan);
        mTempMan = new ReconTemperatureManager(this);
        mStatsManagers.add(mTempMan);
        mVertMan = new ReconVerticalManager(this);
        mStatsManagers.add(mVertMan);
        mEventHandler = new ReconEventHandler(this);
        mChronoMan = new ReconChronoManager(this, mEventHandler, this);
        mStatsManagers.add(mChronoMan);
        mJumpMan = new ReconJetJumpManager(this,mEventHandler);
        //      mStatsManagers.add(mJumpMan);// FIXME: fix the
        //      interface so that jump can be added too
        mHRMan = new ReconHRManager(this);
        mStatsManagers.add(mHRMan);
        mCadenceMan = new ReconCadenceManager(this);
        mStatsManagers.add(mCadenceMan);
        mPowerMan = new ReconPowerManager(this);
        mStatsManagers.add(mPowerMan);
        mSportsActivityMan = new SportsActivityManager(this);
        mStatsManagers.add(mSportsActivityMan);
        mCalorieMan = new ReconCalorieManager(this);
        mStatsManagers.add(mCalorieMan);
        
        mDataLogHandler = new ReconDataLogHandler(this);
        generateFullInfoBundle();
        mShutdownReceiver = new shutdownReceiver();
        mHandler = new Handler();
        mEventHandler.writeConfigFile();
        mEventHandler.writeIDFile();
        
        // initiate location manager stuff
        mAndroidLocListen = new AndroidLocationListener(this);
        mAndroidLocListen.init();
        // Now on to sensor stuff
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),PRESSURE_UPDATE_RATE);
        // Make sure that in sensors.conf the min delay is set to one second
        System.out.println("Applauncher");
        // initiate mock data
        if (USE_MOCK_DATA){
            File path = Environment.getExternalStorageDirectory();
            File filebase = new File(path, "/mockdata/");       
            mMockDataProvider =
                new ReconMockDataProvider(new File(filebase,"altitude.dat"),
                                          new File(filebase,"pressure.dat"),
                                          new File(filebase,"temperature.dat"),
                                          new File( filebase, "speed.dat"));
        }
        loadAllFromTempState();
        mTimeMan.handlePossibilityOfNewDay();
        Log.d(TAG,"Day number is "+mTimeMan.getDayNo());
        mTimeMan.handlePossibilityOfTimeLoss();
        loadAllTimeResetCandidateFromFile();
    } // end of on create
    private Runnable runppsUpdate = new Runnable() {
            public void run(){
                mHandler.postDelayed(runppsUpdate,1000);
                ppsUpdate();
            }};
    @Override
    public int onStartCommand(Intent intent, int flags, int startid){
        Log.d(TAG,"onStartCommand");
        if (mIsStarted) {
            Log.v(TAG,"already started");
            return START_STICKY;
        };
        mIsStarted = true;
        mIsStarted = true;
        //mHandler.postDelayed(runppsUpdate,1000);
        //Setup Alarm manager
        Context context = this.getApplicationContext();
        AlarmManager mgr=
            (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent("bAlarm");
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        mOnAlarmReceiver = new OnAlarmReceiver();
        //set owner
        mOnAlarmReceiver.mOwner = this;
        mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                         SystemClock.elapsedRealtime(),
                         DATA_UPDATE_RATE_IN_SEC*1000, pi);
        registerReceiver(mOnAlarmReceiver, new IntentFilter("bAlarm"));
        IntentFilter shutDownFilter = new IntentFilter(Intent.ACTION_SHUTDOWN);
        shutDownFilter.addAction("bShutDown");
        registerReceiver(mShutdownReceiver, shutDownFilter );
        registerReceiver(mBroadcastCommandReceiver,
                         new IntentFilter(TranscendUtils.BROADCAST_COMMAND));
            
        IntentFilter filter = new IntentFilter(SHUTDOWN_COMMAND);
        filter.addAction(REBOOT_COMMAND);
        registerReceiver(mShutdownRebootReceiver, filter);
        return START_STICKY;
    }
    public void saveAllTempStates() {
        // Only save the state when your clock is not fucked up.
        if (mTimeMan.isTimeStillLost()) {
            Log.v(TAG,"time is messed up. Don't save");
            return;
        }
        for (ReconStatsManager rsm : mStatsManagers) {
            rsm.saveToTempState();
        }
    }
    public void loadAllFromTempState() {
        mSportsActivityMan.loadFromTempState();
        for (ReconStatsManager rsm : mStatsManagers) {
            rsm.loadFromTempState();
        }
    }
    
    public void saveAllStates(){
        //Goes through all managers and makes them save the states.
        for (ReconStatsManager rsm : mStatsManagers) {
            rsm.saveState();
        }
    }
    public void resetStats(){
        // Goes through all members and resets their stats
        Log.d(TAG,"reset day stats.");
        for (ReconStatsManager rsm : mStatsManagers) {
            rsm.resetStats();
        }
    }
    public void resetAllTimeStats(){
        // Tag future sports_activities to reset alltime stats once
        // they are reinitialized  through reInitializeForNewSports
        File f;
        for (String key: sAllTimeResetCandidate.keySet()) {
            Log.v(TAG,"sAllTimeResetCandidate key "+key);
            // Tag it.
            sAllTimeResetCandidate.putBoolean(key,true);
            // Remove the bundle:
            f = TranscendUtils.bundleFileFromType(Integer.parseInt(key));
            try {
                if (f.exists()) {
                    f.delete();
                }
            } catch (Exception e) {
                Log.e(TAG,"Bundle file not exist");
            }
        }
        // Go through all managers and force them to reset their stats
        for (ReconStatsManager rsm : mStatsManagers) {
            rsm.resetAllTimeStats();
        }
        // update the ticket status for current on going activity.  So
        // that it doesn't reset itself once it starts the next time
        if (mSportsActivityMan.isDuringSportsActivity()) {
            sAllTimeResetCandidate.putBoolean(String.valueOf(mSportsActivityMan
                                                             .getType()),false);
        }
        saveAllTimeResetCandidateToFile();
        generateFullInfoBundle();
    }
    public void saveAllTimeResetCandidateToFile() {
        Log.v(TAG,"saveAllTimeResetCandidateToFile");
        BundleUtils.writeBundleToFile(sAllTimeResetCandidate, getResetCandidateFile());
    }
    public void loadAllTimeResetCandidateFromFile() {
        Log.v(TAG,"loadAllTimeResetCandidateFromFile");

        Bundle tempBundle = BundleUtils.readBundleFromFile(getResetCandidateFile());
        if (tempBundle != null) {
            sAllTimeResetCandidate = tempBundle;
            return;
        }
        for (final int i: new int[]{ActivityUtil.SPORTS_TYPE_OTHER,
                                    ActivityUtil.SPORTS_TYPE_SKI,
                                    ActivityUtil.SPORTS_TYPE_CYCLING,
                                    ActivityUtil.SPORTS_TYPE_RUNNING}) {
            sAllTimeResetCandidate.putBoolean(i+"",false);
        }
    }

    private File getResetCandidateFile() {
        return  new File(getFilesDir(), "AllTimeResetCandidateFile");
    }
    public static Bundle sAllTimeResetCandidate = new Bundle();

    public void startANewDay(boolean shouldResetStats){
        if (shouldResetStats){
            resetStats();
        }
        // This function ensures that no DAYxx.RIB file is
        // overriden
        mTimeMan.findAndSetEmptyDayNo ();
        mEventHandler.writeConfigFile();
        mEventHandler.writeIDFile();
        mEventHandler.writeEventHeaderFile();
        mDataLogHandler.writeDayHeaderFile();
    }

    public void reInitializeForNewSports(int sportsId, boolean withResetFlag) {
        Log.d(TAG,"reInitializeForNewSports");
        for (ReconStatsManager rsm : mStatsManagers) {
            rsm.reInitializeForNewSports(sportsId);
        }
        if (withResetFlag) {
            sAllTimeResetCandidate.putBoolean(String.valueOf(sportsId),false);
        }
    }
    private class shutdownReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Transcend Service Shut down");
            mIgnoreIncomingMessages = true; // Will stop responding to clients
            Log.d(TAG,"unregistering listeners");
            ReconTranscendService.this.unregisterReceiver(mOnAlarmReceiver);
            unregisterReceiver(mBroadcastCommandReceiver);
            saveAllTimeResetCandidateToFile();
            ReconTranscendService.this.mSensorManager.unregisterListener(ReconTranscendService.this);
            ReconTranscendService.this.mAndroidLocListen.cleanUp();
            Log.d(TAG,"Transcend Service save states");

            // Save state for future inference
            saveAllTempStates();                

            //ReconTranscendService.this.stopSelf();
            // JET-412 GPS aid files are not created, need to turn off GPS before shutdown
            // It is a temporary fix for creation of GPS aid files requested by Gil
            // added by : patrick@reconinstruments.com
            turnOffGPS();
        }
    }
    
    private void mockDataUpdateIfNecessary() {
        if (USE_MOCK_DATA) {
            float tmp;
            tmp = mMockDataProvider.getPressureAlt();
            mAltMan.setPressureAlt(tmp);
            tmp = mMockDataProvider.getPressure();
            mAltMan.setPressure(tmp);
            //FIXME put the damn temperature
            //mTempMan.updateMembers(mMockDataProvider.getTemperature());

            //Put Mock speed

            //If there is no location then create one
            if (mRecLocMan.getLocation() == null){
                Location location = new Location("GPS");
                mRecLocMan.setLocation(location);
            }

            mRecLocMan.setMockSpeed(mMockDataProvider.getSpeed());
        }
    }

    public void updatePostActivityValues() {
        for (ReconStatsManager rsm : mStatsManagers) {
            rsm.updatePostActivityValues();
            rsm.generateBundle();
        }
        generateFullInfoBundle();
    }


    private long counter=0;
    private long SAVE_STATE_PERIOD = 600; // 10mins in datapoints (or seconds)
    /**
     * <code>ppsUpdate</code> is called periodically and asks all the
     * managers to update their state. 
     */
    public void ppsUpdate(){
        mockDataUpdateIfNecessary();
        //for now it is set to regular alarm receiver update
        mBLE.updateBLE();
        // Update the members of all managers
        for (ReconStatsManager rsm : mStatsManagers) {
            rsm.updateMembers();
        }
        if (counter++ % SAVE_STATE_PERIOD == 2) {
            Log.v(TAG,"Periodic save");
            saveAllTempStates();
        }
        generateFullInfoBundle();
        //But Let's do it just for now:
        //broadcast Shit
        Intent myi = new Intent();
        myi.setAction("RECON_MOD_BROADCAST_LOCATION" );
        sendBroadcast(myi);
        broadcastFullInfoBundle();
        
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == (Sensor.TYPE_PRESSURE)){
            mAltMan.setTempPressure(event.values[0]);
        }
    }
    BroadcastReceiver mShutdownRebootReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (intent.getAction().equals(SHUTDOWN_COMMAND)) {
                    // TODO SHUTDOWN STUFF
                    Log.d(TAG, SHUTDOWN_COMMAND + " received.");
                    pm.reboot("BegForShutdown");
                                
                } else if (intent.getAction().equals(REBOOT_COMMAND)) {
                    // TODO REBOOT STUFF
                    Log.d(TAG, REBOOT_COMMAND + " received.");
                    pm.reboot("");
                }
            }
        };

}
