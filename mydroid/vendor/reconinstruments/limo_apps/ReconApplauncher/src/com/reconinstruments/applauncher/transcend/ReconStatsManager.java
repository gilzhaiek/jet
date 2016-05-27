//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.messagecenter.ReconMessageAPI;
import java.util.ArrayList;
public abstract class ReconStatsManager {
    protected Bundle mBundle = null;
    protected ReconTranscendService mRTS;
    protected SharedPreferences mPersistantStats;
    public static String BROADCAST_ACTION_STRING;
    // ^We override this in each inherited class
    protected ArrayList<Stat> mToBeBundled = new ArrayList<Stat>();
    protected ArrayList<Stat> mToBeSaved = new ArrayList<Stat>();
    protected ArrayList<Stat> mToBeAllTimeSaved= new ArrayList<Stat>();
    protected ArrayList<Stat> mCurrentValues= new ArrayList<Stat>();
    protected ArrayList<Stat> mCumulativeValues= new ArrayList<Stat>();
    protected ArrayList<Stat> mComparativeValues= new ArrayList<Stat>();
    protected ArrayList<Stat> mPostActivityValues= new ArrayList<Stat>();
    protected void populateStatLists(){};
    protected void addToLists(Stat[] ss,ArrayList[] items) {
        for (Stat s:ss) {
            for (ArrayList item:items) {
                item.add(s);
            }
        }
    }
    protected Bundle generateBundle() {
        if (mBundle == null) {
            mBundle = new Bundle();
        }
        for (Stat s:mToBeBundled) {
            mBundle = s.putToBundle(mBundle);
        }
        return mBundle;
    }
    public Bundle getBundle(){
        return mBundle;
    }
    protected void updateStatList(ArrayList<Stat> stats) {
        for (Stat s:stats) {
            s.update();
        }
    }
    public void updateCurrentValues() {
        updateStatList(mCurrentValues);
    }
    public void updateCumulativeValues() {
        updateStatList(mCumulativeValues);
    }
    public void updateComparativeValues() {
        updateStatList(mComparativeValues);
    }

    /**
     * updates the records for post activity values such as best
     * averages.  Best averages, only make sense when the activity is
     * finished. This function should be called when the activity is
     * going to end.
     *
     */
    public void updatePostActivityValues() {
        updateStatList(mPostActivityValues);
    }
    
    /**
     * <code>saveState</code> Saves the state of the Stat Manager.
     *
     */
    public void saveState() {
        Log.d(getBasePersistanceName(),"Saving state");
        SharedPreferences.Editor editor = mPersistantStats.edit();
        for (Stat st:mToBeSaved) {
            st.saveState(editor);
        }
        for (Stat st:mToBeAllTimeSaved) {
            st.saveState(editor);
        }
        editor.commit();
    }
    
    /**
     * <code>loadLastState</code> Loads the last saved state
     *
     */
    public void loadLastState() {
        for (Stat st:mToBeSaved) {
            st.loadState(mPersistantStats);
        }
        for (Stat st:mToBeAllTimeSaved) {
            st.loadState(mPersistantStats);
        }
    }
    
    /**
     * <code>resetStats</code> reset stats but not all time stats.
     *
     */
    public void resetStats() {
        for (Stat st:mToBeSaved) {
            st.reset();
        }
    }
    /**
     *  <code>resetAllTimeStats</code> reset all time stats
     *
     */
    public void resetAllTimeStats() {
        for (Stat st:mToBeAllTimeSaved) {
            st.reset();
        }
    }
    /**
     * <code>loadAllTimeStats</code> load all time stats
     *
     */
    public void loadAllTimeStats() {
        for (Stat st:mToBeAllTimeSaved) {
            st.loadState(mPersistantStats);
        }
    }
    abstract public String getBasePersistanceName(); // forces the class to have a name

    /**
     *  <code>saveToTempState</code> Save the state in temporary
     *  location
     *
     */
    public void saveToTempState() {
        switchPersistant("tmp");
        saveState();
        switchPersistant(mRTS.mSportsActivityMan.getType()+"");
    }
    /**
     * <code>loadFromTempState</code> Load the state form the temporary
     * location
     *
     */
    public void loadFromTempState() {
        switchPersistant("tmp");
        loadLastState();
        loadAllTimeStats();
        switchPersistant(mRTS.mSportsActivityMan.getType()+"");
    }
    public ReconTranscendService getRTS() {return mRTS;} // simple getter
    private void switchPersistant(String affix) {
        mPersistantStats = mRTS.getSharedPreferences(getBasePersistanceName()+affix,
                                                     Context.MODE_WORLD_WRITEABLE);
    }
    public void reInitializeForNewSports(int sportsId) {
        Log.v(getBasePersistanceName(),"reInitializeForNewSports");
        switchPersistant(""+sportsId);
        resetStats();
        if (mRTS.sAllTimeResetCandidate
            .getBoolean(String.valueOf(sportsId))) { // need to all time reset
            resetAllTimeStats();
        } else {
            loadAllTimeStats();
        }
        resetNotificationFlags();
    }
    protected void fixPersistanceSetup() {
        mPersistantStats = mRTS
            .getSharedPreferences(getBasePersistanceName() +
                    ActivityUtil.SPORTS_TYPE_SKI,
                                  Context.MODE_WORLD_WRITEABLE);
    }
    /**
     * Perform the common grunt work. Call in constructors
     *
     * @param rts a <code>ReconTranscendService</code> value
     */
    protected void prepare(ReconTranscendService rts) {
        mRTS = rts;
        populateStatLists();
        fixPersistanceSetup();
        commonInitActions(getBasePersistanceName());
    }
    protected boolean shouldUpdateComparativeValues() {
        return (mRTS.getSportsActivityManager() != null &&
                mRTS.getSportsActivityManager().isDuringSportsActivity());
    }
    protected boolean shouldUpdateCumulativeValues() {
        return shouldUpdateComparativeValues();
    }
    public void updateMembers() {
        updateCurrentValues();
        // Only update the comparitive values when in an activity:
        if (shouldUpdateComparativeValues()) {
            updateCumulativeValues();
            updateComparativeValues();
        }
        generateBundle();
    }
    /**
     * Describe <code>broadcastBundle</code> method here.
     *
     * @param action a <code>String</code> value. It is usually
     * BROADCAST_ACTION_STRING
     */
    protected void broadcastBundle(String action) {
        Intent myi = new Intent();
        myi.setAction(action);
        myi.putExtra("Bundle",mBundle);
        Context c = mRTS.getApplicationContext();
        c.sendBroadcast(myi);
        Log.d("StatsManager", "BroadcastingBundle:" + action);
    }
    protected void broadcastBundle(String action, String whichOne) {
        Intent myi = new Intent();
        myi.setAction(action);
        myi.putExtra("Bundle",mBundle);
        myi.putExtra("WhichOne",whichOne);
        Context c = mRTS.getApplicationContext();
        c.sendBroadcast(myi);
    }
    public void commonInitActions(String tag){
        loadAllTimeStats();
        if (!mRTS.mTimeMan.inNewDay()) {
            Log.i(tag,"Not a new day, load Old stats");
            try {
                loadLastState();
            } catch (Exception e) {
                Log.w(tag,"Loading old stats failed!");
            }
        }
    }
    protected void notifyStatRecord(int icon, String text) {
        ReconMessageAPI.showPassiveNotification(mRTS,text,0);
    }

    protected boolean mHasNotifiedBestRecord[] = new boolean[1];
    protected void notifyBestRecordIfNecessary(int item, int icon, String text) {
        // Don't notify if it is the first time ever that the user is
        // doing this particular activity
        if (mRTS.mSportsActivityMan.getTotalNumberOfRides() == 1) {
            return;
        }
        // Don't notify if you have already notified
        if (mHasNotifiedBestRecord[item]) {
            return;
        }
        mHasNotifiedBestRecord[item] = true;
        notifyStatRecord(icon,text);
    }

    protected void resetNotificationFlags() {
        for (int i = 0; i < mHasNotifiedBestRecord.length;i++) {
            mHasNotifiedBestRecord[i] = false;
        }
    }
}
