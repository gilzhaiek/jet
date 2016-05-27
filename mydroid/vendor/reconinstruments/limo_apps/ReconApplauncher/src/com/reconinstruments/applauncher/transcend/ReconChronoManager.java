/*
 * Keeps only one lap, the current running lap in memory
 * Whenever a lap is stopped, it is saved to the database
 */


package com.reconinstruments.applauncher.transcend;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

public class ReconChronoManager extends ReconStatsManager {
        
    private static final String TAG = "ReconChronoManager";
    private ReconEventHandler mReconEventHandler;
    private Context mContext;
        
    private Bundle currentLap; // the most recent lap
    private SQLiteDatabase database; // only contains laps older than the most recent lap
        
    public ReconChronoManager(ReconTranscendService rts, ReconEventHandler reh, Context context) {
        // This one doesn't need to run super It is a bit of a different beast
        mContext = context;
        mRTS = rts;
        mReconEventHandler = reh;
        DatabaseHelper dbHelper = new DatabaseHelper(mContext);
        database = dbHelper.getWritableDatabase();
        commonInitActions(TAG);
        initCurrentLap(0, 0, 0, 0, 0);
        generateBundle();
    }
    
    @Override
    public String getBasePersistanceName() {
        return TAG;
    }

    @Override
    public void commonInitActions(String tag) {
        if(mRTS.mTimeMan.inNewDay()) {
            Log.i(tag, "New day, clearing chrono DB");
            resetStats();
        }
    }

    
    public void startStop() {
        // If there is no currentLap or it is paused, start it
        if(currentLap == null || currentLap.getInt(DatabaseHelper.KEY_RUNNING) == 0) {
            start();
        } else { //if there is a currentLap stop
            stop();
        }
    }
        
    public void lapTrial() {
        // If the current lap is stopped -> new trial
        if(currentLap.getInt(DatabaseHelper.KEY_RUNNING) == 0) {
            newTrial();
        } else {
            newLap();
        }
    }
        
    public void start() {
        mReconEventHandler.writeStartChronoEvent(); // save to event file
        start(SystemClock.elapsedRealtime(), System.currentTimeMillis());
    }
        
    private void start(long local_start, long utc_start) {
        // If current lap is paused resume it
        if(currentLap.getInt(DatabaseHelper.KEY_HAS_RUN) == 1 && currentLap.getInt(DatabaseHelper.KEY_RUNNING) != 1) {
            currentLap.putLong("local_start_time", SystemClock.elapsedRealtime());
            currentLap.putInt(DatabaseHelper.KEY_RUNNING, 1);
            currentLap.putInt(DatabaseHelper.KEY_HAS_RUN, 1);
        } 
                
        // Generally on first lap of a trial
        else if(currentLap.getInt(DatabaseHelper.KEY_HAS_RUN) == 0 && currentLap.getInt(DatabaseHelper.KEY_RUNNING) == 0) {
            currentLap.putLong(DatabaseHelper.KEY_ELAPSED_TIME, 0);
            currentLap.putLong("local_start_time", SystemClock.elapsedRealtime());
            currentLap.putLong(DatabaseHelper.KEY_START_UTC, System.currentTimeMillis());
            currentLap.putInt(DatabaseHelper.KEY_HAS_RUN, 1);
            currentLap.putInt(DatabaseHelper.KEY_RUNNING, 1);
        } 
                
        else {
            // otherwise make a new one and start it
            initCurrentLap(SystemClock.elapsedRealtime(), System.currentTimeMillis(), 0, 1, 1);
        }
    }
        
    public void stop() {
        if(currentLap.getInt(DatabaseHelper.KEY_RUNNING) == 1) {
            long elapsed_time = currentLap.getLong(DatabaseHelper.KEY_ELAPSED_TIME)
                + SystemClock.elapsedRealtime()
                - currentLap.getLong("local_start_time");
                        
            currentLap.putInt(DatabaseHelper.KEY_RUNNING, 0);
            //currentLap.putInt(DatabaseHelper.KEY_HAS_RUN, 1);
            currentLap.putLong(DatabaseHelper.KEY_ELAPSED_TIME, elapsed_time);
            // CHECK:
            // Writting to event file
            mReconEventHandler.writeStopChronoEvent();
        }
    }
        
    private void newLap() {
        // Stop the current lap
        stop();
                
        // Save the current lap to the database
        int trial_group = saveCurrentLap();
                
        // Start a new lap
        initCurrentLap(SystemClock.elapsedRealtime(), System.currentTimeMillis(), 0, 1, 1, trial_group);
    }
        
    public void newTrial() {
        // Save the current lap to the database
        int trial_group = saveCurrentLap();
                
        // init a new lap, but don't start it
        initCurrentLap(SystemClock.elapsedRealtime(), System.currentTimeMillis(), 0, 0, 0, trial_group + 1);
    }
        
    private int saveCurrentLap() { // returns this laps trial number
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_START_UTC, currentLap.getLong(DatabaseHelper.KEY_START_UTC));
        values.put(DatabaseHelper.KEY_ELAPSED_TIME, currentLap.getLong(DatabaseHelper.KEY_ELAPSED_TIME));
        values.put(DatabaseHelper.KEY_HAS_RUN, currentLap.getInt(DatabaseHelper.KEY_HAS_RUN));
        values.put(DatabaseHelper.KEY_RUNNING, currentLap.getInt(DatabaseHelper.KEY_RUNNING));
        values.put(DatabaseHelper.KEY_TRIAL_GROUP, currentLap.getInt(DatabaseHelper.KEY_TRIAL_GROUP));
        database.insert(DatabaseHelper.TABLE_NAME, null, values);
                
        return currentLap.getInt(DatabaseHelper.KEY_TRIAL_GROUP);
    }
        
    // use current latest lap
    private void initCurrentLap(long local_start_time, long utc_start_time, long elapsed_time, int has_run, int is_running) {
        initCurrentLap(local_start_time, utc_start_time, elapsed_time, has_run, is_running, -1);
    }
        
    private void initCurrentLap(long local_start_time, long utc_start_time, 
                                long elapsed_time, int has_run, int is_running, int trial_num) {
                
        //If trial num is invalid, determine the latest trial num
        if(trial_num < 0) { 
            if(currentLap != null) {
                trial_num = currentLap.getInt(DatabaseHelper.KEY_TRIAL_GROUP);
            } else {
                trial_num = 0;
                                
                                Cursor c = database.rawQuery("SELECT MAX("+DatabaseHelper.KEY_TRIAL_GROUP+") as max_group_num " +
                                                "FROM " + DatabaseHelper.TABLE_NAME, null);
                                if(c.moveToFirst()) {
                                    trial_num = c.getInt(0) + 1;
                                }
                                c.close();
                        }
                }
                
                currentLap = new Bundle();
                currentLap.putInt(DatabaseHelper.KEY_TRIAL_GROUP, trial_num);
                currentLap.putLong("local_start_time", local_start_time);
                currentLap.putLong(DatabaseHelper.KEY_START_UTC, utc_start_time);
                currentLap.putLong(DatabaseHelper.KEY_ELAPSED_TIME, elapsed_time);
                currentLap.putInt(DatabaseHelper.KEY_HAS_RUN, has_run);
                currentLap.putInt(DatabaseHelper.KEY_RUNNING, is_running);
        }
                
        
    public int getCurrentTrialIndex() {
        return currentLap.getInt(DatabaseHelper.KEY_TRIAL_GROUP);
    }
        
    public Bundle getBundle() {
        //generateBundle();
        return mBundle;
    }
        
    @Override
    protected Bundle generateBundle() {
        Bundle chronoBundle = new Bundle();
        ArrayList<Bundle> trials = new ArrayList<Bundle>();
        boolean currentLapAdded = false;
                
        //Get all the trial numbers present
        Cursor cTrialNums = database.rawQuery("SELECT "+ DatabaseHelper.KEY_TRIAL_GROUP +
                                              " FROM " + DatabaseHelper.TABLE_NAME + " GROUP BY " + DatabaseHelper.KEY_TRIAL_GROUP + 
                                              " ORDER BY " + DatabaseHelper.KEY_TRIAL_GROUP + " ASC", null);
                
        //Build the trials list
        if(cTrialNums.moveToFirst()) {
            do {
                ArrayList<Bundle> laps = new ArrayList<Bundle>();
                                
                //Trial variables to build
                long trial_start_time = Long.MAX_VALUE;
                long trial_end_time = Long.MIN_VALUE;
                long trial_elapsed_time = 0;
                boolean trial_running = false;
                boolean trial_has_run = false;
                                
                // Add laps from the database to this trial
                Cursor cTrial = database.query(DatabaseHelper.TABLE_NAME, null, 
                                               DatabaseHelper.KEY_TRIAL_GROUP+"="+cTrialNums.getInt(0), null, null, null, DatabaseHelper.KEY_START_UTC + " ASC");
                if(cTrial.moveToFirst()) {
                    do {
                        Bundle lap = new Bundle();
                        boolean isRunning = cTrial.getInt(cTrial.getColumnIndex(DatabaseHelper.KEY_RUNNING)) == 1;
                                                
                        long elapsedTime = cTrial.getLong(cTrial.getColumnIndex(DatabaseHelper.KEY_ELAPSED_TIME));
                        long startTime = cTrial.getLong(cTrial.getColumnIndex(DatabaseHelper.KEY_START_UTC));
                        long endTime = startTime + elapsedTime;
                                                
                        //lap.putBoolean("IsRunning", isRunning);
                        //lap.putBoolean("HasRun", cTrial.getInt(cTrial.getColumnIndex(DatabaseHelper.KEY_HAS_RUN)) == 1);
                        lap.putBoolean("IsRunning", false);
                        lap.putBoolean("HasRun", true);
                        lap.putLong("ElapsedTime", elapsedTime);
                        lap.putLong("StartTime", startTime);
                        lap.putLong("EndTime", endTime);
                                                
                        laps.add(lap);
                                                
                        trial_elapsed_time += elapsedTime;
                        if(startTime < trial_start_time) trial_start_time = startTime;
                        if(endTime > trial_end_time) trial_end_time = endTime;
                        //if(lap.getBoolean("IsRunning")) trial_running = true;
                        if(lap.getBoolean("HasRun")) trial_has_run = true;
                                                
                    } while (cTrial.moveToNext());
                }
                cTrial.close();
                                
                // If current lap is in this trial, add it
                if(currentLap.getInt(DatabaseHelper.KEY_TRIAL_GROUP) == cTrialNums.getInt(0)) {
                    currentLapAdded = true;
                    Bundle b = new Bundle();
                                        
                    boolean isRunning = currentLap.getInt(DatabaseHelper.KEY_RUNNING) == 1;
                                        
                    long elapsedTime = currentLap.getLong(DatabaseHelper.KEY_ELAPSED_TIME);
                    if(isRunning) {
                        elapsedTime+= SystemClock.elapsedRealtime() - currentLap.getLong("local_start_time");
                        chronoBundle.putBoolean("IsRunning", true);
                    }
                                        
                    long startTime = currentLap.getLong(DatabaseHelper.KEY_START_UTC);
                    long endTime = startTime + elapsedTime;
                    b.putBoolean("IsRunning", isRunning);
                    b.putBoolean("HasRun", currentLap.getInt(DatabaseHelper.KEY_HAS_RUN) == 1);
                    b.putLong("ElapsedTime", elapsedTime);
                    b.putLong("StartTime", startTime);
                    b.putLong("EndTime", endTime);
                                        
                    laps.add(b);
                                        
                    trial_elapsed_time += elapsedTime;
                    if(startTime < trial_start_time) trial_start_time = startTime;
                    if(endTime > trial_end_time) trial_end_time = endTime;
                    if(b.getBoolean("IsRunning")) trial_running = true;
                    if(b.getBoolean("HasRun")) trial_has_run = true;
                }
                                
                if(laps.size() > 0) {
                    Bundle trial = new Bundle();
                                        
                    trial.putBoolean("IsRunning", trial_running);
                    trial.putBoolean("HasRun", trial_has_run);
                    trial.putLong("ElapsedTime", trial_elapsed_time);
                    trial.putLong("StartTime", trial_start_time);
                    trial.putLong("EndTime", trial_end_time);
                    trial.putParcelableArrayList("Laps", laps);
                    trials.add(trial);
                }
                                        
                        } while (cTrialNums.moveToNext());
                } 
                cTrialNums.close();
                
        // Add the current trial if it is not already added (e.g. it is in a completely new lap)
        if(!currentLapAdded) {
            // Otherwise just add the current lap
            ArrayList<Bundle> laps = new ArrayList<Bundle>();
                        
            Bundle lap = new Bundle();
            boolean isRunning = currentLap.getInt(DatabaseHelper.KEY_RUNNING) == 1;
            long elapsedTime = currentLap.getLong(DatabaseHelper.KEY_ELAPSED_TIME);
            if(isRunning) {
                elapsedTime += SystemClock.elapsedRealtime() - currentLap.getLong("local_start_time");
                chronoBundle.putBoolean("IsRunning", true);
            }
            long startTime = currentLap.getLong(DatabaseHelper.KEY_START_UTC);
            long endTime = startTime + elapsedTime;
            lap.putBoolean("IsRunning", isRunning);
            lap.putBoolean("HasRun", currentLap.getInt(DatabaseHelper.KEY_HAS_RUN) == 1);
            lap.putLong("ElapsedTime", elapsedTime);
            lap.putLong("StartTime", startTime);
            lap.putLong("EndTime", endTime);
                        
            laps.add(lap);
                        
            Bundle trial = new Bundle();
            trial.putBoolean("IsRunning", lap.getBoolean("IsRunning"));
            trial.putBoolean("HasRun", lap.getBoolean("HasRun"));
            trial.putLong("ElapsedTime", elapsedTime);
            trial.putLong("StartTime", startTime);
            trial.putLong("EndTime", endTime);
            trial.putParcelableArrayList("Laps", laps);
            trials.add(trial);
        }
                
        chronoBundle.putParcelableArrayList("Trials", trials);
                
        return chronoBundle;
    }
        
    @Override
    /* This stops any current laps and saves state */
        public void saveState() {
        stop();
        // Don't save empty laps
        if(currentLap.getInt(DatabaseHelper.KEY_HAS_RUN, 0) == 1)
            saveCurrentLap();
    }
    @Override
    public void loadLastState() {
        // TODO Auto-generated method stub
                
    }
    @Override
    public void resetStats() {
        database.delete(DatabaseHelper.TABLE_NAME, null, null);
    }
    
    /***********************************
     *  Database Helper
     **********************************/
        
    class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "chronodata";
        private static final int DATABASE_VERSION = 1;
        public static final String TABLE_NAME = "chrono_laps";
                
        public static final String KEY_ID = "_id";
        public static final String KEY_START_UTC = "start_utc";
        public static final String KEY_RUNNING = "running";
        public static final String KEY_HAS_RUN = "has_run";
        public static final String KEY_ELAPSED_TIME = "elapsed_time";
        public static final String KEY_TRIAL_GROUP = "trial_group";
                
        private static final String DATABASE_CREATE = "create table " + TABLE_NAME +" (_id integer primary key autoincrement, " +
            KEY_START_UTC + " int not null, " +
            KEY_RUNNING + " int not null, " +
            KEY_HAS_RUN + " int not null, " +
            KEY_ELAPSED_TIME + " int not null, " +
            KEY_TRIAL_GROUP + " int not null);";
                
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(DatabaseHelper.class.getName(),
                  "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS todo");
            onCreate(db);
        }
                
    }
}