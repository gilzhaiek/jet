//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import com.reconinstruments.utils.ConversionUtil;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.ArrayList;

/*
 * This class handles the attributes related to distance travelled
 */
public class ReconDistanceManager extends ReconStatsManager{
    private static final String TAG = "Distance Manager";
    private ReconTimeManager mTimeMan = null;
    private ReconAltitudeManager mAltMan = null;
    private ReconSpeedManager mSpeedMan = null;
    private Stat<Float> mHorzDistance =
        new Stat<Float>(0f, "HorzDistance");
    private Stat<Float> mVertDistance =
        new Stat<Float>(0f, "VertDistance");
    private Stat<Float> mDistance = new Stat<Float>(0f, "Distance");
    private Stat<Float> mAllTimeDistance =
        new Stat<Float>(0f,"AllTimeDistance");
    private MaxStat<Float> mBestDistance =
        new MaxStat<Float> (0f,"BestDistance", mDistance);
    private final String BROADCAST_ACTION_STRING="RECON_MOD_BROADCAST_DISTANCE";
    @Override protected void populateStatLists() {
        addToLists(new Stat[]{mHorzDistance,mVertDistance,
                              mDistance,mBestDistance,mAllTimeDistance},
            new ArrayList[]{mToBeBundled});
        addToLists(new Stat[]{mHorzDistance,mVertDistance,
                              mDistance},
            new ArrayList[]{mToBeSaved});
        addToLists(new Stat[]{mAllTimeDistance},
                   new ArrayList[]{mToBeAllTimeSaved});
        addToLists(new Stat[]{mBestDistance},
                   new ArrayList[]{mToBeAllTimeSaved,mPostActivityValues});
    }
    public ReconDistanceManager(ReconTranscendService rts){
        mAltMan = rts.getReconAltManager();
        mSpeedMan = rts.getReconSpeedManager();
        mTimeMan = rts.mTimeMan;
        prepare(rts);
        generateBundle();
    }
    @Override
    public String getBasePersistanceName() {
        return TAG;
    }

    private boolean mShouldUpdateDistance;
    @Override public void updateCumulativeValues() {
        float horzspeed = mSpeedMan.getHorzSpeed(); 
        float vertspeed = mSpeedMan.getVertSpeed(); 
        float speed = mSpeedMan.getSpeed();//km/h
        float avgSpeed = mSpeedMan.getAverageSpeed()/
            (float)ConversionUtil.KM_p_H_IN_M_p_S;
        float tmpDistance;
        // Don't update distance if you have invalid speed or not in
        // an activity
        mShouldUpdateDistance = (speed != ReconSpeedManager.INVALID_SPEED);
        if (!mShouldUpdateDistance) return;

        horzspeed = horzspeed/(float)ConversionUtil.KM_p_H_IN_M_p_S;//meters/s
        mHorzDistance.setValue(mHorzDistance.getValue().floatValue() +
                               horzspeed * mRTS.DATA_UPDATE_RATE_IN_SEC);
        mVertDistance.setValue(mVertDistance.getValue().floatValue() +
                               Math.abs(mAltMan.getDeltaAlt()));
        // New scheme to calculate distance
        tmpDistance = avgSpeed * (mRTS.mSportsActivityMan.getDuration() / 1000);
        if (tmpDistance > mDistance.getValue().floatValue()) { 
            mDistance.setValue(tmpDistance);
        }
    }
    @Override
    public void updateComparativeValues() {
        if (!mShouldUpdateDistance) return;
        super.updateComparativeValues();
        if (mBestDistance.isRecord()) {
            notifyBestRecordIfNecessary(0,0,"Best Distance");
        }
    }
    @Override
    public void updatePostActivityValues() {
        super.updatePostActivityValues();
        mAllTimeDistance.setValue(mAllTimeDistance.getValue().floatValue() +
                                  mDistance.getValue().floatValue());
    }
    public float getHorzDistance(){
        return mHorzDistance.getValue().floatValue();
    }
    public float getVertDistance(){
        return mVertDistance.getValue().floatValue();
    }
    public float getDistance(){
        return mDistance.getValue().floatValue();
    }
}
