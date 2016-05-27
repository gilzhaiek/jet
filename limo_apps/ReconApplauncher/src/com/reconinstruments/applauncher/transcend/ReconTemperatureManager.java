package com.reconinstruments.applauncher.transcend;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import com.reconinstruments.utils.stats.StatsUtil;

public class ReconTemperatureManager extends ReconStatsManager{
    private static final String TAG = "ReconTemperatureManager";
    public static final int INVALID_TEMPERATURE = StatsUtil.INVALID_TEMPERATURE; // -1 K!
    public static final String BROADCAST_ACTION_STRING = "RECON_MOD_BROADCAST_TEMPERATURE";
    private int mMaxTemperature = INVALID_TEMPERATURE;
    private int mMinTemperature = INVALID_TEMPERATURE;
    private int mTemperature =INVALID_TEMPERATURE;
    private int mAllTimeMaxTemperature = INVALID_TEMPERATURE;
    private int mAllTimeMinTemperature = INVALID_TEMPERATURE;
    private int mInvalidTemperatureCounter = 0;
    public static final int MAX_INVALID_IN_A_ROW_TO_TOLERATE = 3;
    public ReconTemperatureManager(ReconTranscendService rts) {
        prepare(rts);
        generateBundle();
        mMinTemperature = -INVALID_TEMPERATURE;//So that the next time temperature will be updated. This line should come AFTER bundle generation
        commonInitActions(TAG);                // run it again.
                
    }
    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    @Override
    public void updateCurrentValues() {
    }
    @Override
    public void updateComparativeValues() {
    }

    @Override
    public void updateMembers(){
        int temperature;
        temperature = mRTS.mBLE.mTemperature;
        //Update results if only have valid temperature
        
        //Note the subtle difference that
        //ReconBLEServiceProvider.INVALID_TEMPERATURE is not
        // necessarily teh same as our INVALID_TEMPERATURE; In fact it isnt
        
        if (temperature != ReconBLEServiceProvider.INVALID_TEMPERATURE) { // valid temperature
            mInvalidTemperatureCounter = 0;
            mTemperature = temperature;
            if (temperature < mMinTemperature) { // Min temp handle
                mMinTemperature = temperature;
                // now check against the all time values:
                if (mAllTimeMinTemperature == INVALID_TEMPERATURE ||
                    mMinTemperature < mAllTimeMinTemperature){ // All time changes
                    mAllTimeMinTemperature = mMinTemperature;
                    //update Bundle
                    generateBundle();
                    broadcastBundle(BROADCAST_ACTION_STRING, "AllTimeMinTemperature");
                    
                }
                else {          // all Time does not change only min changes
                    //update Bundle
                    generateBundle();
                    broadcastBundle(BROADCAST_ACTION_STRING, "MinTemperature");
                }

            } 
            if (temperature > mMaxTemperature) { // Max temp handle
                mMaxTemperature = temperature;
                if (mAllTimeMaxTemperature == INVALID_TEMPERATURE ||
                    mMaxTemperature > mAllTimeMaxTemperature){ // all time changes
                    mAllTimeMaxTemperature = mMaxTemperature;
                    //update Bundle
                    generateBundle();
                    broadcastBundle(BROADCAST_ACTION_STRING, "AllTimeMaxTemperature");
                }
                else {          // all time does not change only max changes
                    //update Bundle
                    generateBundle();
                    broadcastBundle(BROADCAST_ACTION_STRING, "MaxTemperature");
                }
            }
            else {              // not min nor max temperature
                generateBundle();
            }

        }
        else{                   // invalid temperature
            mInvalidTemperatureCounter++;
        }
                
        if (mInvalidTemperatureCounter > MAX_INVALID_IN_A_ROW_TO_TOLERATE){ // A lot of invalid temps
            mTemperature = INVALID_TEMPERATURE;
            generateBundle();
        }

    }
    @Override
    protected Bundle generateBundle(){
        super.generateBundle();
        mBundle.putInt("MaxTemperature", mMaxTemperature);
        mBundle.putInt("MinTemperature", mMinTemperature);
        mBundle.putInt("AllTimeMaxTemperature", mAllTimeMaxTemperature);
        mBundle.putInt("AllTimeMinTemperature", mAllTimeMinTemperature);
        mBundle.putInt("Temperature", mTemperature);
        return mBundle;
    }

    @Override
    public void loadLastState() {
        mMaxTemperature = mPersistantStats.getInt("MaxTemperature",mMaxTemperature);
        mMinTemperature = mPersistantStats.getInt("MinTemperature",mMinTemperature);
        mAllTimeMaxTemperature = mPersistantStats.getInt("AllTimeMaxTemperature", mAllTimeMaxTemperature);
        mAllTimeMinTemperature = mPersistantStats.getInt("AllTimeMinTemperature", mAllTimeMinTemperature);
    }

    @Override
    public void saveState() {
        Log.d(TAG,"Saving state");
        SharedPreferences.Editor editor = mPersistantStats.edit();

        editor.putInt("MaxTemperature",mMaxTemperature);
        editor.putInt("MinTemperature",mMinTemperature);
        editor.putInt("AllTimeMaxTemperature",mAllTimeMaxTemperature);      
        editor.putInt("AllTimeMinTemperature",mAllTimeMinTemperature);      

        editor.commit();

    }

    @Override
    public void resetAllTimeStats() {
        mAllTimeMaxTemperature = mMaxTemperature;
        mAllTimeMinTemperature = mMinTemperature;
        generateBundle();
                
    }

    @Override
    public void resetStats() {
        mMaxTemperature = mTemperature;
        mMinTemperature = mTemperature;
        generateBundle();
                
    }

    @Override
    public void loadAllTimeStats() {
        mAllTimeMaxTemperature = mPersistantStats.getInt("AllTimeMaxTemperature", mMaxTemperature);
        mAllTimeMinTemperature = mPersistantStats.getInt("AllTimeMinTemperature", mMinTemperature);
                
    }

    public int getTemperature(){
        return mTemperature;
    }
    


}
