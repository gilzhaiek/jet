//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.content.BroadcastReceiver;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.SystemClock;
import android.util.Log;
import com.reconinstruments.applauncher.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.text.format.Time;

import com.reconinstruments.utils.stats.TranscendUtils;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.DeviceUtils;
/**
 * Class to take care of book-keeping of when sports activity has
 * started. How long it has been running for and other stuff.
 *
 */
public class SportsActivityManager extends ReconStatsManager {
    private static final String TAG = "SportsActivityManager";
    private static final int SPORTS_ACTIVITY_NOTI_ID = 31416;
//    public  static final String SUMMARY_FOLDER = "ReconApps/TripData";
    private int mType = ActivityUtil.SPORTS_TYPE_CYCLING;
    private int mStatus = ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY;
    private int mTempStatus = ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY;
    private long mLastKnownTime = 0;
    private long mStartTime;    // This time is synced with TimeManager
    private long mInternalStamp; // This time used internally to
                                 // calculate durations
    private long mDurationDelta = 0;
    private long mDuration = 0;
    private long mBestDuration = 0;
    private long mAllTimeDuration = 0;
    private int mTotalNumberOfRides = 0;

    // Getters
    public long getDuration() {return mDuration;}
    public long getDurationDelta() {return mDurationDelta;}
    public int getTotalNumberOfRides() {return mTotalNumberOfRides;}
    
    /**
     * Creates a new <code>SportsActivityManager</code> instance.
     *
     */
    public SportsActivityManager(ReconTranscendService rts) {
        if (DeviceUtils.isSnow2()) {
            setType(ActivityUtil.SPORTS_TYPE_SKI);
        }
        prepare(rts);
    }
    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    @Override
    public void updateCurrentValues() {
        if (isDuringSportsActivity()) {
            mDurationDelta = SystemClock.elapsedRealtime() - mInternalStamp;
        }  else {
            mDurationDelta = 0;
        }
        mDuration += mDurationDelta;
        stamp();
    }
    @Override
    public void updateComparativeValues() {
        if (mDuration > mBestDuration) {
            mBestDuration = mDuration;
            // FIXME: fix icon int
            notifyBestRecordIfNecessary(0,0,"Best Duration");
        }
        mAllTimeDuration += mDurationDelta;
    }
    

    @Override
    public void updateMembers() {
        super.updateMembers();
    }
    @Override 
    protected Bundle generateBundle() {
        super.generateBundle();
        mBundle.putInt("Status",mStatus);
        mBundle.putLong("StartTime",mStartTime);
        mBundle.putLong("InternalStamp",mInternalStamp);
        mBundle.putLong("Durations",mDuration);
        mBundle.putLong("BestDuration",mBestDuration);
        mBundle.putLong("AllTimeDuration",mAllTimeDuration);
        mBundle.putInt("TotalNumberOfRides",mTotalNumberOfRides);
        mBundle.putInt("Type",mType);
        return mBundle;
    }
    /**
     * Describe <code>getType</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getType() {
        return mType;
    }
    /**
     *  Return if we are during a sports activity or not
     *
     * @return a <code>boolean</code> value
     */
    public boolean isDuringSportsActivity() {
        return (mStatus == 1);
    }
    /**
     * Return that status of the currenct acitivy:
     * No activity, ongoing and pause
     *
     * @return an <code>int</code> value
     */
    public int getStatus() {
        return mStatus;
    }
    // helper function that update timestamp
    private void stamp() {
        mInternalStamp = SystemClock.elapsedRealtime();
    }
    /**
     * Sets the sports type
     *
     * @param sportsType an <code>int</code> value
     */
    public void setType(int sportsType) {
        mType = sportsType;        
    }

    public void setupRIBFiles() {
        // Create a new RIB file
        mRTS.mTimeMan.findAndSetEmptyDayNo();
        mRTS.mEventHandler.writeEventHeaderFile();
        mRTS.mDataLogHandler.writeDayHeaderFile();

    }
    /**
     *  <code>startSportsActivity</code> Starts the sports activity.
     * if already started then does nothing 
     *
     */
    public void startSportsActivity() {
        mRTS.getAndroidLocationListener().requestGps(true, TAG);

        if (mStatus != ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY) {
            return;
            // Doesn't make sense to start something that is already
            // started
        }

        setupRIBFiles();
        mRTS.reInitializeForNewSports(mType, true);
        mStartTime = mRTS.getReconTimeManager().getUTCTimems();
        mTotalNumberOfRides++;
        mStatus = ActivityUtil.SPORTS_ACTIVITY_STATUS_ONGOING;
        stamp();
        showStatusOnStatusBar();
        mRTS.getReconEventHandler()
            .writeSportsActivityEvent(ReconEventHandler
                                      .SPORTS_ACTIVITY_START_EVENT,
                                      mType);
        notifyAndSaveTempStates();
    }
    /**
     * Pauses the activity
     * If not ongoing then does nothing
     */
    public void pauseSportsActivity() {
        if (mStatus != ActivityUtil.SPORTS_ACTIVITY_STATUS_ONGOING) {
            return;
            //Doesn't make sense to pause something that is not running
        }
        mStatus = ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED;
        stamp();
        showStatusOnStatusBar();
        mRTS.getReconEventHandler()
            .writeSportsActivityEvent(ReconEventHandler
                                      .SPORTS_ACTIVITY_PAUSE_EVENT,
                                      mType);
        // Empty the buffer so that we always have all the data.
        SimpleLocationLogger sll = mRTS.getReconLocationManager()
            .getSimpleLocationLogger();
        if (sll != null) {
            sll.dumpBuffersToFile();
        }
        notifyAndSaveTempStates();
    }
    /**
     * Resumes the activiy. If not paused then does nothing
     *
     */
    public void resumeSportsActivity() {
        if (mStatus != ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED) {
            return;
            // Doesn't make sense to resume something that is not
            // paused
        }
        if (mRTS.mTimeMan.isTimeStillLost()) {
            Log.v(TAG,"won't resume. Time's is messed up");
            return;             // Wont' resume if time is messed up
        }
        mStatus = ActivityUtil.SPORTS_ACTIVITY_STATUS_ONGOING;
        stamp();
        showStatusOnStatusBar();
        mRTS.getReconEventHandler()
            .writeSportsActivityEvent(ReconEventHandler
                                      .SPORTS_ACTIVITY_RESUME_EVENT,
                                      mType);
        notifyAndSaveTempStates();
    }
    /**
     * Finish sports activity. If has not started then does nothing
     *
     */
    public void finishSportsActivity() {
        Log.v(TAG,"finishSportsActivity");
        mRTS.getAndroidLocationListener().requestGps(false, TAG);

        if (mStatus == ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY) {
            return;
            // Doesnt' make sense ot finish something that has not
            // started
        }
        mStatus = ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY;
        stamp();
        hideNotification();
        mRTS.getReconEventHandler()
            .writeSportsActivityEvent(ReconEventHandler
                                      .SPORTS_ACTIVITY_FINISH_EVENT,
                                      mType);
        mRTS.updatePostActivityValues();
        notifyAndSaveTempStates();
    }
    public void saveSportsActivity() {
        Log.v(TAG,"saveSportsActivity");
        TranscendUtils.dumpFullInfoBundleIntoExternalStorage(mRTS.mFullInfo, mType);
        mRTS.saveAllStates();
    }
    public void discardSportsActivity() {
        Log.v(TAG,"discardSportsActivity");
        if (mStatus != ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY) {
            Log.v(TAG,"Can't discard while there is activity");
            return;
            // Doesnt' make sense to discrad something that has not
            // started
        }
        mRTS.getReconEventHandler()
            .writeSportsActivityEvent(ReconEventHandler
                                      .SPORTS_ACTIVITY_DISCARD_EVENT,
                                      mType);
        mTotalNumberOfRides--;  // decrement the incremented day.
        if (mTotalNumberOfRides < 0) mTotalNumberOfRides = 0;
        sendActivityDiscarded();
        
    }
    @Override
    public void saveState() {
        //Log.v(TAG,"saveState");
        SharedPreferences.Editor editor = mPersistantStats.edit();
        editor.putLong("StartTime",mStartTime);
        editor.putLong("Duration",mDuration);
        editor.putLong("BestDuration",mBestDuration);
        editor.putLong("AllTimeDuration",mAllTimeDuration);
        editor.putInt("TotalNumberOfRides",mTotalNumberOfRides);
        mTempStatus = mStatus;
        if (mTempStatus == ActivityUtil.SPORTS_ACTIVITY_STATUS_ONGOING) {
            mTempStatus = ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED;
        }
        editor.putInt("Status",mStatus);
        editor.putInt("TempStatus",mTempStatus);
        editor.putInt("Type",mType);
        mLastKnownTime = mRTS.getReconEventHandler().getTime().toMillis(false);
        editor.putLong("LastKnownTime",mLastKnownTime);
        editor.commit();
    }
    @Override
    public void loadLastState() {
        mStartTime = mPersistantStats.getLong("StartTime",mStartTime);
        mDuration = mPersistantStats.getLong("Duration", mDuration);
        mStatus = mPersistantStats.getInt("Status", mStatus);
        mTempStatus = mPersistantStats.getInt("TempStatus", mStatus);
        mType = mPersistantStats.getInt("Type", mType);
        mLastKnownTime = mPersistantStats.getLong("LastKnownTime",mLastKnownTime);
    }
    @Override
    public void resetStats() {
        //Log.v(TAG,"resetStats");
        mStartTime = 0; 
        mInternalStamp = 0; 
        mDuration = 0;
        generateBundle();
    }
    @Override
    public void resetAllTimeStats() {
        //Log.v(TAG,"resetAllTimeStats");
        mAllTimeDuration = mDuration;
        mBestDuration = mDuration;
        mTotalNumberOfRides = 0;
        generateBundle();
    }
    @Override
    public void loadAllTimeStats() {
        mBestDuration = mPersistantStats.getLong("BestDuration",mDuration);
        mAllTimeDuration = mPersistantStats.getLong("AllTimeDuration",mDuration);
        mTotalNumberOfRides =
            mPersistantStats.getInt("TotalNumberOfRides",0);
    }
    @SuppressWarnings("deprecation")
    private void showNotification(int drawable) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) mRTS.getSystemService(ns);
        PendingIntent contentIntent = PendingIntent
            .getActivity(mRTS, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification(drawable, null, System.currentTimeMillis());
        n.setLatestEventInfo(mRTS, "", "", contentIntent);
        n.flags |= Notification.FLAG_ONGOING_EVENT;
        n.flags |= Notification.FLAG_NO_CLEAR;
        //      startForeground(BLE_NOTIFICATION_ID, n);
        nm.notify(SPORTS_ACTIVITY_NOTI_ID, n);
    }
    private void hideNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) mRTS.getSystemService(ns);
        nm.cancel(SPORTS_ACTIVITY_NOTI_ID);
    }
    // This is hacky needs a better design. Crappy API :-(
    private void sendActivityUpdated(boolean isDiscarded) {
        Intent i = new Intent("com.reconinstruments.SPORTS_ACTIVITY");
        i.putExtra("status",mStatus);
        i.putExtra("type",mType);
        i.putExtra("isDiscarded",isDiscarded);
        mRTS.sendBroadcast(i);
    }
    private void sendActivityUpdated() {
        sendActivityUpdated(false);
    }
    private void sendActivityDiscarded() {
        sendActivityUpdated(true);
    }

    private void showStatusOnStatusBar() {
        if (mStatus == ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY) {
            hideNotification();
        }
        else if (mStatus == ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED) {
            showNotification(R.drawable.activity_paused);
        }
        else if (mStatus == ActivityUtil.SPORTS_ACTIVITY_STATUS_ONGOING) {
            showNotification(R.drawable.activity_tracking);
        }
    }

    @Override
    public void loadFromTempState() {
        super.loadFromTempState();
        if ((mTempStatus == ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED) &&
            (mStatus != ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED)) {
            mStatus = mTempStatus; // pause
            Log.v(TAG,"Improper shutdown");
            ActivityUtil.setWasShutDownWhileActivityOngoing(mRTS,true);
            // Insert a pause event in the RIB file
            if (mLastKnownTime > 0) {
                Time utcTime = new Time();
                utcTime.switchTimezone(Time.TIMEZONE_UTC);
                utcTime.set(mLastKnownTime);
                mRTS.getReconEventHandler()
                    .writeSportsActivityEvent(ReconEventHandler
                                              .SPORTS_ACTIVITY_PAUSE_EVENT,
                                              mType,
                                              utcTime,
                                              false); // Dont'care about gsp
            }
        }
        else {
            Log.v(TAG,"Proper shutdown");
            ActivityUtil.setWasShutDownWhileActivityOngoing(mRTS,false);
        }
        showStatusOnStatusBar();
    }

    private void notifyAndSaveTempStates() {
        mRTS.saveAllTempStates();
        sendActivityUpdated();
    }
}
