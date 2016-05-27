package com.reconinstruments.applauncher.transcend;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.reconinstruments.applauncher.R;
import com.reconinstruments.notification.ReconNotification;
import com.reconinstruments.utils.ConversionUtil;
import android.content.SharedPreferences;
import com.reconinstruments.utils.stats.StatsUtil;
import java.util.ArrayList;
/*
 * This class handles the attributes related to height for the transcend service.
 */
public class ReconAltitudeManager extends ReconStatsManager{
    private static final String TAG = "ReconAltManager";
    public static final float INVALID_ALT = StatsUtil.INVALID_ALT;
    private static final float INVALID_PRESSURE = 0;
    public static final float SUPER_LOW_PRESSURE = 2;
    public static final String BROADCAST_ACTION_STRING = "RECON_MOD_BROADCAST_ALT";
    public static final int GOOD_NUMBER_OF_SATS = 5;
    public static final int SLOW_WALKING_SPEED = 2;
    private final static float LOW_PASS_FILTER_FACTOR = 0.100f;
    private final int ali = 1;
    // Alt Related:
    private Stat<Float> mAlt = 	new Stat<Float> (INVALID_ALT,"Alt");
    private MaxStat<Float> mMaxAlt =
	new MaxStat<Float> ("MaxAlt",mAlt);
    private MinStat<Float> mMinAlt =
	new MinStat<Float> ("MinAlt",mAlt);
    private MaxStat<Float> mAllTimeMaxAlt =
	new MaxStat<Float> ("AllTimeMaxAlt",mMaxAlt);
    private MinStat<Float> mAllTimeMinAlt =
	new MinStat<Float> ("AllTimeMinAlt",mMinAlt);
    // Pressure Alt related:
    private Stat<Float> mPressureAlt =
	new Stat<Float>(INVALID_ALT,"PressureAlt");
    private PreviousStat<Float> mPreviousPressureAlt = 
	new PreviousStat<Float> ("PreviousPressureAlt",mPressureAlt);
    private LowPassStat<Float> mPressureAlt_LP =
	new LowPassStat<Float>("PressureAlt_LP",mPressureAlt,
			       LOW_PASS_FILTER_FACTOR);
    private PreviousStat<Float> mPreviousPressureAlt_LP =
	new PreviousStat<Float>("PreviousPressureAlt_LP",mPressureAlt_LP);
    // Gps Alt related:
    private float mTempGpsAlt = INVALID_ALT;
    private Stat<Float> mGpsAlt =
	new Stat<Float>(INVALID_ALT,"GpsAlt");
    // Pressure realted: 
    private Stat<Float> mTempPressure =
	new Stat<Float> (INVALID_PRESSURE,"TempPressure");
    private LowPassStat<Float> mPressure =
	new LowPassStat<Float> ("Pressure",mTempPressure,
				LOW_PASS_FILTER_FACTOR);
    // Calibration
    private Stat<Float> mHeightOffsetAvg =
	new Stat<Float>(0f, "HeightOffsetAvg");
    private Stat<Integer> mHeightOffsetN =
	new Stat<Integer> (0,"HeightOffsetN");
    private final static float MAX_HEIGHT_OFFSET_N = 500;
    private boolean mIsCallibrating = true;
    private boolean mIsInitialized = false;
    // Misc:
    private ReconLocationManager mReconLocationManager;
    private AltitudeCalibrator mAltitudeCalibrator =
	new AltitudeCalibrator((int)MAX_HEIGHT_OFFSET_N,0);
    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    @Override
    protected void populateStatLists() {
	addToLists(new Stat[]{mAlt,mMaxAlt,mMinAlt,
			      mAllTimeMaxAlt,mAllTimeMinAlt,
			      mPressureAlt,mPreviousPressureAlt,
			      mPressureAlt_LP, mPreviousPressureAlt_LP,
			      mGpsAlt,mPressure,mHeightOffsetN,
			      mHeightOffsetAvg},
	    new ArrayList[] {mToBeBundled});
	addToLists(new Stat[]{mHeightOffsetAvg,mHeightOffsetN},
		   new ArrayList[]{mToBeAllTimeSaved});
	addToLists(new Stat[]{mPreviousPressureAlt,
			      mPressureAlt_LP,
			      mPreviousPressureAlt_LP},
	    new ArrayList[] {mCurrentValues});
	addToLists(new Stat[]{mMaxAlt,mMinAlt},
		   new ArrayList[] {mToBeSaved,mComparativeValues});
	addToLists(new Stat[]{mAllTimeMinAlt,mAllTimeMaxAlt},
		   new ArrayList[] {mToBeAllTimeSaved,mPostActivityValues});

    }
    public ReconAltitudeManager(ReconTranscendService rts) {
        mReconLocationManager = rts.getReconLocationManager();
        prepare(rts);
    }
    // Getters and setters-------------------------------------------
    public float getDeltaAlt_LP() {
	return (float) mPreviousPressureAlt_LP.getDelta().floatValue();}
    public float getDeltaAlt() {
	return (float) mPreviousPressureAlt.getDelta().floatValue();}
    public int getDeltaAltInt(){
	return (int)getDeltaAlt();}
    public float getPressure() {
        return mPressure.getValue().floatValue();}
    public void setPressure(float val) {
         mPressure.setValue(val);}
    public void setTempPressure(float p) {
        mTempPressure.setValue(p);}
    public float getAlt() {return mAlt.getValue().floatValue();}
    public float getMaxAlt() {return mMaxAlt.getValue().floatValue();}
    public float getHeightOffsetAvg() {
	return mHeightOffsetAvg.getValue().floatValue();}
    public float getGpsAlt() {return mGpsAlt.getValue().floatValue();}
    public float getPressureAlt() {
	return mPressureAlt.getValue().floatValue();}
    public void setPressureAlt(float val) {
	mPressureAlt.setValue(val);
    }
    public float getPreviousPressureAlt() {
	return mPreviousPressureAlt.getValue().floatValue();}
    public void setTempGpsAlt(float gpsAlt) {
	mTempGpsAlt = gpsAlt;
    }
    //-----------------------------------------------------------------
    @Override
    public void updateCurrentValues() {
        if (!mRTS.USE_MOCK_DATA) {
	    // Verfiy last known pressure
	    if (mTempPressure.getValue().floatValue()
		< SUPER_LOW_PRESSURE) {
		mTempPressure.setValue(INVALID_PRESSURE);
	    }
	    // Update filtered pressuer
	    mPressure.update();
        }
        else {                  // using mock location
            mHeightOffsetN.setValue((int) MAX_HEIGHT_OFFSET_N);
            mHeightOffsetAvg.setValue(0f);
        }
	// Update pressure alt;
	mPressureAlt
	    .setValue(JumpAnalyzer.calculate_altitude(mPressure.getValue()
						      .floatValue())/
		      (float) ConversionUtil.DECIMETERS_IN_METER);
	// Upate Gps Alt;
	mGpsAlt.setValue(mTempGpsAlt);
	// Update Calibration:
	goThroughCalibrationCircus();
	if (mHeightOffsetN.getValue().intValue() >= MAX_HEIGHT_OFFSET_N) {
	    // ^has at least calibrated once
	    // Set Alt:
	    mAlt.setValue(mPressureAlt.getValue().floatValue() -
			  mHeightOffsetAvg.getValue().floatValue());
	}
	// Update dependent stats:
	super.updateCurrentValues();
    }

    private void goThroughCalibrationCircus() {
        mIsCallibrating = (mReconLocationManager.isGPSFix() &&
                           mHeightOffsetN.getValue().intValue() <
			   MAX_HEIGHT_OFFSET_N);
        if (!mReconLocationManager.isGPSFix()) return;
	// conditions for going on with callibration:
	// 1) Number of Satellites > 4
	// 2) We are stationary (to prevent patent violation)
	int num_sats = mReconLocationManager.getNumberOfSatellites();
	double speed = mReconLocationManager.getLocation().getSpeed();
	if (!((num_sats >= GOOD_NUMBER_OF_SATS) &&
	      (speed < SLOW_WALKING_SPEED))){return;}
	int offset = (int)(mPressureAlt.getValue().intValue() -
			   mGpsAlt.getValue().intValue());
	int weight = 1;
	int iii;
	for (iii = 1; iii < num_sats-GOOD_NUMBER_OF_SATS;iii++){
	    weight *= 2;
	}
	mAltitudeCalibrator.push(offset,weight);
	if (mIsCallibrating) {
	    mHeightOffsetN.setValue(mHeightOffsetN.getValue().intValue() +
				    weight);
	    mHeightOffsetAvg
		.setValue(mHeightOffsetAvg.getValue().floatValue() +
			  ((float)(offset * weight))/MAX_HEIGHT_OFFSET_N);
	}
	else {
	    // Regular calibration is done going to continuous
	    // calibration
	    mHeightOffsetAvg.setValue(mAltitudeCalibrator.mAverage);
	}
     }
    @Override
    public void updateComparativeValues() {
	// Don't update if not calibrated
	if (mHeightOffsetN.getValue().intValue() < MAX_HEIGHT_OFFSET_N) return;
	super.updateComparativeValues();
	if (mMinAlt.wasRecord()){
            broadcastBundle(BROADCAST_ACTION_STRING,"MinAlt");
        } else if (mMaxAlt.wasRecord()){
            broadcastBundle(BROADCAST_ACTION_STRING,"MaxAlt");
        } if (mAllTimeMaxAlt.isRecord()){
            broadcastBundle(BROADCAST_ACTION_STRING,"MaxAllTimeAlt");
        }
    }
}
