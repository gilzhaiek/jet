package com.reconinstruments.applauncher.transcend;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.applauncher.R;
import com.reconinstruments.notification.ReconNotification;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.DeviceUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

public class ReconRunManager extends ReconStatsManager {
    private static final String TAG = "ReconRunManager";
    public static final String BROADCAST_ACTION_STRING = "RECON_MOD_BROADCAST_RUN";
    public  final static String RUNINFO_FILE = "runinfo";
    final private int SKI_RUN_SIGN_MIN = -5;
    final private int SKI_RUN_SIGN_MAX= 5;
    final private int SKI_RUN_MAX_ASCEND = 5;
    final private int SKI_RUN_MAX_DESCEND = 5;
    final private int SKI_RUN_IDLE_MAX_SPEED = 5;
    final private int SKI_RUN_MAX_IDLE_DURATION = 300;
    final private int SKI_RUN_MIN_IDLE_DURATION = 4;

    //Ski run detection variables
    private int ski_run_sign_total;
    private int ski_run_idle_duration_total;
    /* This section involves ski run detection variables */
    private  int ski_run_descend;
    private  int ski_run_ascend;
    private  boolean in_a_ski_run;
    private  int time_of_start_of_ski_run;      /* not used for now */
    private int dist_of_start_of_ski_run;       /* not used for now */
    private  int total_number_of_ski_runs;
    private int mAllTimeTotalNumberOfSkiRuns; // used in the bundle
                                              // that's why a
                                              // different name
                                              // convention is used
    private  float last_run_max_speed;
    private  float last_run_max_altitude;
    private  float last_run_temp_avg_speed;
    private  float last_run_avg_speed;
    private  float last_run_vrt;
    private  float last_run_dist;
    private  int total_time_in_last_run;
    private int total_time_in_all_runs; // this is used for
                                        // calculating all itme
                                        // average
    private float all_runs_avg_speed;
    private float all_runs_temp_avg_speed;
        
    private int height_diff;
    //End of ski run detection variables

    //hack no vert till run
    public boolean hasBeenInARun = false;
    // We only start counting vertical when a run starts in other
    // words vertical uses this. make sure that we don't use vertical
    // here. just alt
        
    private ReconTimeManager mReconTimeManager;
    private ReconAltitudeManager mReconAltitudeManager;
    private ReconSpeedManager mReconSpeedManager;
    private ReconLocationManager mReconLocationManager;
        
    public ArrayList<ReconRun> mRuns;
        
    private ReconRun mTempRun;
        
    public ReconRunManager(ReconTranscendService rts) {
        mReconTimeManager = rts.getReconTimeManager();
        mReconAltitudeManager = rts.getReconAltManager();
        mReconSpeedManager = rts.getReconSpeedManager();
        mReconLocationManager = rts.getReconLocationManager();
        mRuns = new ArrayList<ReconRun>();
        prepare(rts);
        generateBundle();
    }

    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
        
    protected Bundle generateBundle(){
        super.generateBundle();
        //Generate the bundle array
        ArrayList<Bundle> runs = new ArrayList<Bundle>();
        for (int i=0; i<mRuns.size(); i++) {
            runs.add(mRuns.get(i).getBundle());
        }
        mBundle.putParcelableArrayList("Runs",runs);
        // One more 
        mBundle.putInt("AllTimeTotalNumberOfSkiRuns",
                 mAllTimeTotalNumberOfSkiRuns);
        return mBundle;
    }

    public void updateMembers() {
	super.updateMembers();
    }

    @Override
    public void updateCurrentValues() {
	return;
    }
    @Override
    public void updateCumulativeValues() {
	// Don't update run detection if you haven't had. bureaucrat 
        if (mReconLocationManager.getLocation() == null) {
            return;
        }
	if (!(mRTS.mSportsActivityMan.getType() == ActivityUtil.SPORTS_TYPE_SKI ||
	      DeviceUtils.isSnow2())) {
	    return;
	}
        //updateSki runs
        height_diff = mReconAltitudeManager.getDeltaAltInt();
        RunDebug.recordState("heightdiff "+height_diff); // DEBUG:
        if (height_diff < 0) {//We are going down
            ski_run_idle_duration_total = 0;
            // case going down for not long:
            if (ski_run_sign_total > SKI_RUN_SIGN_MIN ){
                ski_run_sign_total--;                                                   
            }
            ski_run_descend -= height_diff;
            RunDebug.recordState("Ski run descend"+ski_run_descend); // DEBUG
            // case have been going down for long enough: (note the
            // fact that I have not put "else" in the next statement
            // is intentional
            if (ski_run_sign_total == SKI_RUN_SIGN_MIN) {
                ski_run_ascend = 0;
                if (!in_a_ski_run && ski_run_descend > SKI_RUN_MAX_DESCEND){
                    in_a_ski_run = true;//Start a new run
                    hasBeenInARun = true;
                    //reset_last_run_info:
                    resetLastRunTempVariables();
                    mTempRun = new ReconRun();
                    // Possible enhancement: We can add teh start of
                    // the run here.
                    mTempRun.mNumber = mRuns.size()+1;
                    total_number_of_ski_runs++;
                    mTempRun.mMaxAltitude=mReconAltitudeManager.getAlt();
                    mAllTimeTotalNumberOfSkiRuns++;
                    RunDebug.recordState("Start of run "+mTempRun.mNumber); // DEBUG:
                }
            }
        }
        else if (height_diff > 0){ // We are going up
            if (ski_run_sign_total < SKI_RUN_SIGN_MAX) { // not going up for long enough
                ski_run_sign_total++;
            }
            ski_run_ascend += height_diff;
            RunDebug.recordState("Ski run ascend"+ski_run_ascend); // DEBUG
            if (ski_run_sign_total == SKI_RUN_SIGN_MAX) { // going up for long enough
                ski_run_descend = 0; 
            }
            if (in_a_ski_run && (ski_run_ascend > SKI_RUN_MAX_ASCEND)) { // Went up enough!
                in_a_ski_run = false; // End of run
                if (ski_run_idle_duration_total > SKI_RUN_MIN_IDLE_DURATION){ 
                    mReconSpeedManager.setAverageSpeed(mReconSpeedManager.getTempAverageSpeed());
                    last_run_avg_speed = last_run_temp_avg_speed;
                }
		finalizeRun();
                //Broadcast run
                broadcastBundle(BROADCAST_ACTION_STRING);
                if (mRuns.size() == 1 ) { // First run of the day
                    mRTS.sendBroadcast(new Intent("com.reconinstruments.applauncher.transcend.FIRST_RUN_OF_THE_DAY"));
                }
                Bundle notifExtraBundle = new Bundle();
                notifExtraBundle.putInt("position", 0);
            }
        }
        else if (height_diff == 0){               // Going flat
            if (mReconSpeedManager.getSpeed()==0)/* stop*/{
                if (ski_run_idle_duration_total == 0){
                    //Save Average Speed
                    mReconSpeedManager.setTempAverageSpeed(mReconSpeedManager.getAverageSpeed());
                    last_run_temp_avg_speed =last_run_avg_speed;
                    
                    //FIXME
                    //              mReconSpeedManager.setTempAllTimeAverageSpeed(mReconSpeedManager.getAllTimeAverageSpeed());
                    //              all_runs_temp_avg_speed =all_runs_avg_speed;
                    
                    
                }
                ski_run_idle_duration_total++;
            }
        }
                
        //Update current_run information
        if (in_a_ski_run){
            float current_speed = mReconSpeedManager.getSpeed(); // km/h
                        
            //Max speed
            if (mReconSpeedManager.getSpeed() > last_run_max_speed){
                last_run_max_speed = current_speed;
            }
            //Distance
            if (current_speed > 0){
                //distance. Note that time interval is in seconds
                last_run_dist += current_speed / 3.6f * (mReconTimeManager.getTimeInterval());
                //average speed
                last_run_avg_speed  = ((last_run_avg_speed * total_time_in_last_run) + current_speed)/(total_time_in_last_run + 1);
                total_time_in_last_run++;
                                                        
                // alltime average speed
                // FIXME
                //              all_runs_avg_speed = ((all_runs_avg_speed * total_time_in_all_runs) + current_speed)/(total_time_in_all_runs + 1);

                if (mReconAltitudeManager.getDeltaAlt() < 0)
                    last_run_vrt -=  mReconAltitudeManager.getDeltaAlt(); 
            } 
        }

                

    }

    public void loadStatsFromBundle(Bundle b){
        mRuns = new ArrayList<ReconRun>();
        //Get the list of runs from bundle
        ArrayList<Bundle> runs = b.getParcelableArrayList("Runs");
                
        //Go through bundle (individual run bundles) and construct runs and add to list
        for (int i=0; i<runs.size();i++){
            mRuns.add(new ReconRun(runs.get(i)));
        }

        mAllTimeTotalNumberOfSkiRuns = b.getInt("AllTimeTotalNumberOfSkiRuns");
        // note that the above is also stored in
        // mPersistantStats. Look at saveState() for logic explanation
    }
        
    @Override
    public void loadLastState() {
        // Here is the logic: Read the file into bytearray. Generate
        // parcel from bytearray (unmarshal). Generate bundle from
        // parcel. Load stats from bundle

        // Make sure that there is a good file
        File fi = new File(mRTS.getFilesDir(), RUNINFO_FILE);
        int fileSize = (int)fi.length();
        Log.d(TAG,"File size is "+fi.length());
        final Parcel p = Parcel.obtain();
        if (fileSize > 0 ){
            try {
                // read file
                FileInputStream f = mRTS.openFileInput(RUNINFO_FILE);
                byte [] byteArray = new byte[fileSize];
                f.read(byteArray);

                // generate parcel
                p.unmarshall(byteArray,0,fileSize);


                // Generate bundle from parcel
                p.setDataPosition(0);
                Bundle b = p.readBundle();

                // load stats from bundle
                loadStatsFromBundle(b);
                
            }
            catch (IOException e){
                Log.e(TAG,"Couldn't read run info" );
                e.printStackTrace();
            } finally {
                p.recycle();
            }
        }
                
    }

    @Override
    public void saveState() {
        Log.d(TAG,"Saving state");
        // Two step: saving allTimeStats and then saving the rest.

        // First Step: Saving allTime stat.  
        // 
        // Rationala for doing it this way: The CommonInitActions
        // loads the all time info and then if there is a new day
        // loads the state.  That's why although the all time total
        // number of ski runs is saved in the bundle it may not get
        // loaded. we save it separately and if after load it gets
        // overwritten by the same value.

        Log.d(TAG,"Saving all time runs");
        SharedPreferences.Editor editor = mPersistantStats.edit();
        editor.putInt("AllTimeTotalNumberOfSkiRuns",mAllTimeTotalNumberOfSkiRuns);
        editor.commit();

        // Now we go the bulk of the action which is saving all the
        // run info
        
        //All you need to do is save the bundle

        // Here is the logic: Generate parcel from Bundle. Generate
        // file from parcel (marshal). Save file) So serialize and
        // attack
        Log.d(TAG,"saving run info");
        Parcel p = Parcel.obtain();
        mBundle.writeToParcel(p, 0);
        FileOutputStream fos = null;
        try {
            fos = mRTS.openFileOutput(RUNINFO_FILE, Context.MODE_PRIVATE);
            Log.d(TAG,"Attempt to marshal and write");
            fos.write(p.marshall());
            Log.d(TAG,"Attempt to flush");
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            p.recycle();
        }
                
    }

    @Override
    public void resetAllTimeStats() {
        Log.d(TAG, "resetAllTimeStats()");
        mAllTimeTotalNumberOfSkiRuns = mRuns.size();
        
                // MODLIVE-643: all time runs does not get reset
                SharedPreferences.Editor editor = mPersistantStats.edit();
                editor.putInt("AllTimeTotalNumberOfSkiRuns",mAllTimeTotalNumberOfSkiRuns);
                editor.commit();
                generateBundle();
                // End of MODLIVE-643: all time runs does not get reset
    }

    @Override
    public void resetStats() {
        mRuns = new ArrayList<ReconRun>();
        generateBundle();
    }

    @Override
    public void loadAllTimeStats() {
        mAllTimeTotalNumberOfSkiRuns =
            mPersistantStats.getInt("AllTimeTotalNumberOfSkiRuns",
                                      0);
    }

    @Override
    public void updatePostActivityValues(){
	// This is call at the end so we overload it to
	if (in_a_ski_run) {
	    in_a_ski_run = false;
	    finalizeRun();
	}
    }

        
    public void resetLastRunTempVariables() {
        last_run_max_speed = 0;
        last_run_temp_avg_speed = 0;
        last_run_avg_speed = 0;
        last_run_vrt = 0;
        last_run_dist = 0;
        total_time_in_last_run = 0;
    }
    public int getRunNumber() {
        return total_number_of_ski_runs;
    }
    public int getAllTimeTotalNumberOfSkiRuns() {
        return mAllTimeTotalNumberOfSkiRuns;
    }
    @Override
    public void updateComparativeValues() {
    }

    private void finalizeRun() {
	if (mTempRun == null) return; // just making sure
	mTempRun.mAverageSpeed = last_run_avg_speed;
	mTempRun.mMaxSpeed = last_run_max_speed;
	mTempRun.mDistance = last_run_dist;
	mTempRun.mVertical = last_run_vrt;
	RunDebug.recordState("End of run"); // DEBUG
	//Add the shit to the array
	mTempRun.updateBundle();
	mRuns.add(mTempRun);
	generateBundle();
    }
}

class RunDebug{
    public static final boolean DEBUG_MODE = false;
    public static final String RUN_LOG_FILE_NAME = "runlog.txt";

    public static void recordState(String txt){
        if (DEBUG_MODE){
            writeStateChangeToFile(txt);
        }
    }

    public static void writeStateChangeToFile(String txt){
        File f = new File(Environment.getExternalStorageDirectory(),RUN_LOG_FILE_NAME);
        try {
            int size;
            FileWriter os = new FileWriter(f,true);
            os.write(("Time "+System.currentTimeMillis()+txt+"\n"));
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
