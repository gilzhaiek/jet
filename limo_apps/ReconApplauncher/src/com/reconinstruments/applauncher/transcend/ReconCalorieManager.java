package com.reconinstruments.applauncher.transcend;

import java.text.DecimalFormat;
import java.util.Calendar;

import com.reconinstruments.applauncher.R;
import com.reconinstruments.notification.ReconNotification;
import com.reconinstruments.externalsensors.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.stats.StatsUtil;
import com.reconinstruments.utils.UserInfo;
import java.util.ArrayList;


/**
 *  <code>ReconCalorieManager</code> Keeps track of the calories
 *  burned by the user. It draws on a variety of sources to give and
 *  estimate of the calorie burn. If we external sensors such as
 *  Heartrate or Power to rely on we will use them, otherwise we use
 *  generic formulae. We also need information on user's weight have a
 *  more accurate estimation.
 *
 *  The implementation of Calories is based on Php implementation of
 *  Engage. 
 *
 *
 */
public class ReconCalorieManager extends ReconStatsManager {
    private static final String TAG = "ReconCalorieManager";
    public static final float INVALID_CALORIES = StatsUtil.INVALID_CALORIES;
    
    private WindowStat<Float> mTotalCalories = 
	new WindowStat<Float>(0f,"TotalCalories",INVALID_CALORIES,
			      new Stat<Float> (),2) {
	@Override
	public void reset() {
            setValue(0f);
            super.reset();
	}
    };
    private Stat<Float>  mAllTimeTotalCalories =
        new Stat<Float>(0f,"AllTimeTotalCalories",INVALID_CALORIES); 
    private MaxStat<Float> mBestCalorieBurn =
        new MaxStat<Float>(0f,"BestCalorieBurn",mTotalCalories);
  
    private float mWeight = 70f;
    private float mHeight = 180f;
    private float mAge = 31;
    private int mGender = 0;    // 0 male 1 female
    private float mBMR;
    @Override protected void populateStatLists() {
	addToLists(new Stat[]{mTotalCalories,mAllTimeTotalCalories,
			      mBestCalorieBurn},
	    new ArrayList[]{mToBeBundled});
        addToLists(new Stat[]{mTotalCalories},
                   new ArrayList[]{mToBeSaved});
	addToLists(new Stat[]{mAllTimeTotalCalories},
		   new ArrayList[]{mToBeAllTimeSaved});
	addToLists(new Stat[]{mBestCalorieBurn},
		   new ArrayList[]{mToBeAllTimeSaved,mPostActivityValues});
    }
    public ReconCalorieManager(ReconTranscendService rts) {
	mTotalCalories.setFollowed(mTotalCalories);
        prepare(rts);
        setTheProfileInfo();
    }

    private void setTheProfileInfo() {
        UserInfo userInfo = UserInfo.getACopyOfSystemUserInfo();
        // I miss lisp macros
        String temp = userInfo.get(UserInfo.WEIGHT);
        if (!temp.equals("")) {
            mWeight = Float.parseFloat(temp);
        }
        temp = userInfo.get(UserInfo.HEIGHT);
        if (!temp.equals("")) {
            mHeight = Float.parseFloat(temp);
        }
        temp = userInfo.get(UserInfo.BIRTH_YEAR);
        if (!temp.equals("")) {
            float year = (float)Calendar.getInstance().get(Calendar.YEAR);
            float birthYear = Float.parseFloat(temp);
            mAge = year - birthYear;        
            
        }
        temp = userInfo.get(UserInfo.GENDER);
        mGender = (temp.equals(UserInfo.FEMALE))? 1 : 0;

        // Now lets go to more complicated stuff
        if (mGender == 1) {     // female
            mBMR =  (9.56f * mWeight) + (1.85f * mHeight) - (4.68f * mAge) + 655f;
        } else {                // male
            mBMR = (13.75f * mWeight) + (5f * mHeight) - (6.76f * mAge) + 66f;
        }
    }

    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    @Override
    public void updateCumulativeValues() {
	mTotalCalories.setValue(updateCalories());
	mTotalCalories.update();
        mAllTimeTotalCalories
	    .setValue(mTotalCalories.getValue().floatValue() +
		      (mTotalCalories.getValue().floatValue() -
		       mTotalCalories.readPrevious(1).floatValue()));
    }
    @Override
    public void updateComparativeValues() {
        super.updateComparativeValues();
        if (mBestCalorieBurn.isRecord()) {
            notifyBestRecordIfNecessary(0,0,"Most Calories Burned");
        }
    }
    private float updateCalories() {
        float tripHours = (float)((mRTS.mSportsActivityMan.getDuration())
                                  /3600000.0); // miliseconds
        int avgHRint = mRTS.mHRMan.getAvgHR();
        float avgHR = (float) avgHRint;
        float tcal;                                  // for total calories
        if (avgHRint != ReconHRManager.INVALID_HR) {    // have HR
            if (mGender == 1) { // female
                tcal = ((-20.4022f + (0.4472f * avgHR) - (0.1263f * mWeight) +
                  (0.074f * mAge))/4.184f) * 60f * tripHours;   
            }
            else {              // male
                tcal = ((-55.0969f + (0.6309f * avgHR) + (0.1988f * mWeight) +
                                   (0.2017f * mAge))/4.184f) * 60f * tripHours;
            }
        }
        else {                  // Don't have HR
            tcal = (mBMR / 24f) * getMet() * tripHours;
        }
        if (tcal < 0) tcal = INVALID_CALORIES;
        return tcal;
    }

    private float getMet() {
        float avgSpeed = mRTS.mSpeedMan.getAverageSpeed();
        int activityType = mRTS.mSportsActivityMan.getType();
        if (activityType == ActivityUtil.SPORTS_TYPE_CYCLING) { 
            if(32 < avgSpeed) {return 16f;}
            else if(25 < avgSpeed) {return 12f;}
            else if(22 < avgSpeed) {return 10f;}
            else if(19 < avgSpeed) {return 8f;}
            else {return 6f;}
        }
        else if (activityType == ActivityUtil.SPORTS_TYPE_RUNNING) { 
            if(17.7 < avgSpeed) {return 18f;}
            else if(14.5 < avgSpeed) {return 15f;}
            else if(12 < avgSpeed) {return 12.5f;}
            else if(9.5 < avgSpeed) {return 10f;}
            else if(8 < avgSpeed) {return 8f;}
            else if(6.5 < avgSpeed) {return 5f;}
            else if(5 < avgSpeed) {return 3.3f;}
            else if(4 < avgSpeed) {return 2.5f;}
            else {return 2f;}
        }
        else {                  // any other activity
            return 0;           // crap value
        }
    }
}