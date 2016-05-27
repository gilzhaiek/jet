//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.Log;
import java.io.File;

import com.reconinstruments.notification.ReconNotification;
import com.reconinstruments.os.hardware.power.HUDPowerManager;

public class ReconTimeManager extends ReconStatsManager{
    private static final String TAG = "ReconTimeManager";
    public long mLastUpdate; //Time in miliseconds
    private long mTheUpdateBefore; //Time in milisecods
    private long mUTCTimems;
    public static final String BROADCAST_ACTION_STRING = "RECON_MOD_BROADCAST_TIME";    
        
    private int mDayNo; //The day number
    private long mStartOfDayTime;
    private long mLastShutDownTime;
    private boolean mInNewDay=false;
        
    public static final long OFF_TIME_FOR_NEW_DAY = 6*3600*1000;// 6 hours in ms
    //public static final long OFF_TIME_FOR_NEW_DAY = 1*30*1000;// 30 secs in ms

    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    private boolean isNewDay(long currenttime){
        //Log.v(TAG,"current time is" + currenttime);
        //Log.v(TAG,"mLastShutDownTime is " + mLastShutDownTime);
        return (currenttime > mLastShutDownTime + OFF_TIME_FOR_NEW_DAY);

    }

    public boolean inNewDay(){
        return mInNewDay;
    }
    public void writeShutDownTime(){
        Log.d(TAG,"Saving Shutdown time info info"+System.currentTimeMillis());
        SharedPreferences.Editor editor = mPersistantStats.edit();
        editor.putLong("LastShutDownTime", System.currentTimeMillis());
        editor.commit();
    }
        
    public ReconTimeManager(ReconTranscendService rts){
        // Don't run super on this
        Log.d(TAG,"ReconTimeManger Constructor");
        mRTS = rts;
        fixPersistanceSetup();
        mDayNo = mPersistantStats.getInt("DayNo", 0);
        long currentTime = System.currentTimeMillis();
        mLastShutDownTime = mPersistantStats.getLong("LastShutDownTime", 0);
        //Note for above: This causes a new day in case stuff has been messed 

        mStartOfDayTime = mPersistantStats.getLong("StartOfDayTime", currentTime);//So in case 

        updateMembers(null);
        updateMembers(null);
    } 
        

    public static final long TIME_LOSS_SAFETY_BUFFER = 10l * 24l * 3600l * 1000l; // 10 days millisecond
    public static final long SOME_TIME_IN_THE_PAST = 1426270673l*1000l; // in milli seconds
    public boolean isTimeStillLost() {
        return (System.currentTimeMillis() < SOME_TIME_IN_THE_PAST);
    }
    public void handlePossibilityOfTimeLoss() {
        Log.v(TAG,"last shutdown time"+mLastShutDownTime);
        Log.v(TAG,"current time"+System.currentTimeMillis());
        if (isTimeStillLost()) {
            Log.v(TAG,"Time is messed up");
            // SET Current time
            Log.v(TAG,"Do nothing");
            //SystemClock.setCurrentTimeMillis(mLastShutDownTime);          
        }
    }
    public void handlePossibilityOfNewDay() {
        long currentTime = System.currentTimeMillis();
        if (isNewDay(currentTime)){
            Log.v(TAG,"New day");
            mInNewDay = true;
            // TODO: sendBroadcast for new day:
            broadcastBundle(BROADCAST_ACTION_STRING);
            
        } else {
            Log.v(TAG,"not a new day");
        }

    }
    //Deprecietated Never use until it gets removed
    public ReconTimeManager(ReconTranscendService rts, int DayNo){
        mRTS = rts;
        mDayNo = DayNo;
        mPersistantStats = mRTS.getSharedPreferences("TimeManagerPers", Context.MODE_WORLD_WRITEABLE);

        //Run update members twice
        updateMembers(null);
        updateMembers(null);
    }
        
    public long getUTCTimems(){
        return mUTCTimems;
    }
        
    public Time getUTCTime(){
        Time t = new Time();
        t.switchTimezone(Time.TIMEZONE_UTC);
        t.set(mUTCTimems);
        return t;
    }
        
    public float getTimeInterval(){
        //returns in seconds
        return ((float)(mLastUpdate - mTheUpdateBefore))/1000;
    }

    @Override
    public void updateCurrentValues() {
    }
    @Override
    public void updateComparativeValues() {
    }
    @Override
    public void updateMembers() {
        updateMembers(null);
    }

    public void updateMembers(Location location){
        //If location is null update using real time clock other wise update using the gps time provided
        mTheUpdateBefore = mLastUpdate;
        mLastUpdate = android.os.SystemClock.elapsedRealtime();
        //If there is data from GPS update based on that otherwise just update by calculation
        if (location != null)
            {
                mUTCTimems = (location.getTime());
            }
        else//no valid location adjust by hand
            {
                mUTCTimems  += getTimeInterval()*1000;
            }
        //The following log checks to see taht if indeed the time
        //intervals that we are getting are one second. 

        //Log.d(TAG,"timeInterval in seconds: "+getTimeInterval());
        generateBundle();
    }
        
    public int getDayNo(){
        return mDayNo;
    }
        
    public void setDayNo(int DayNo){
        mDayNo = DayNo;
    }
        
    @Override
    protected Bundle generateBundle(){
        super.generateBundle();
        mBundle.putLong("LastUpdate", mLastUpdate);
        mBundle.putLong("TheUpdateBefore", mTheUpdateBefore);
        mBundle.putLong("UTCTimems",mUTCTimems);
        mBundle.putLong("LastShutDownTime",mLastShutDownTime);
        mBundle.putBoolean("InNewDay",mInNewDay);
        mBundle.putBoolean("IsTimeStillLost",isTimeStillLost());
        mBundle.putBoolean("LastShutdownWasGraceful",lastShutdownWasGraceful());
        return mBundle;
    }

    @Override
    public void loadLastState() {
        long currentTime = System.currentTimeMillis();
        mDayNo = mPersistantStats.getInt("DayNo", 0);
        mLastShutDownTime = mPersistantStats.getLong("LastShutDownTime", 0);
        mStartOfDayTime = mPersistantStats.getLong("StartOfDayTime", currentTime);//So in case 
    }

    @Override
    public void saveState() {
        //      writeShutDownTime();
        // Save A number of fields
        Log.d(TAG,"Saving state");
        SharedPreferences.Editor editor = mPersistantStats.edit();
        editor.putLong("LastShutDownTime", System.currentTimeMillis());
        editor.putLong("StartOfDayTime", mStartOfDayTime);
        editor.putInt("DayNo",mDayNo);          
        editor.commit();
    }

    public void incrementDay(){
        Log.d(TAG,"incrementDay");
        mInNewDay = true;
        mDayNo++;
        long currentTime = System.currentTimeMillis();
        mStartOfDayTime = currentTime;
        Log.d(TAG,"Start of day time "+currentTime);
        
        // update the persistent state
        saveState();
        
        // Dismiss all stats notifications
        //for(int id : ALL_NOTIF_IDS) {
        //  dismissNotification(id);
        //}
    }

    public void findAndSetEmptyDayNo (){
        

        incrementDay();

        // Make sure that the current day number is good. 
        File path = Environment.getExternalStorageDirectory();
        File file = new File(path, ReconDataLogHandler.LOG_FOLDER+"/DAY"+mDayNo/10+""+mDayNo%10+".RIB");

        // We keep incrementing days until DAYXX.RIB does not exist,
        // because we don't want to override an old lingering DAY
        while(file.exists()){
            mDayNo++;
            file = new File(path, ReconDataLogHandler.LOG_FOLDER+"/DAY"+mDayNo/10+""+mDayNo%10+".RIB");
        }
        // Now save the day number in time manager for future reference
        saveState();

    }

    public boolean lastShutdownWasGraceful () {
        HUDPowerManager mHPM = HUDPowerManager.getInstance();
        if (mHPM.getLastShutdownReason() == 0) {
            //Log.v(TAG,"last shutdown was graceful");
            return true;
        } else {
            //Log.v(TAG,"last shutdown wasn't graceful");
            return false;
        }

    }
            
}
