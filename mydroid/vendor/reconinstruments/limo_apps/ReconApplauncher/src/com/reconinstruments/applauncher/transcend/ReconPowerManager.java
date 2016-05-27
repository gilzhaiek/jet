package com.reconinstruments.applauncher.transcend;
import com.reconinstruments.externalsensors.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.reconinstruments.utils.stats.StatsUtil;
import java.util.ArrayList;

public class ReconPowerManager extends ReconStatsManager
    implements IExternalBikePowerListener {
    private static final String TAG = "ReconPowerManager";
    public static final int INVALID_POWER = StatsUtil.INVALID_POWER;
    private Stat<Integer> mPower = new Stat<Integer>(INVALID_POWER,"Power");
    private Stat<Integer> mLeftPower = new Stat<Integer>(INVALID_POWER,"LeftPower");
    private Stat<Integer> mRightPower = new Stat<Integer>(INVALID_POWER,"RightPower");
    private AverageStat<Integer> mAvgPower =
	new AverageStat<Integer> (0,"AveragePower",mPower);
    private MaxStat<Integer> mMaxPower =
	new MaxStat<Integer>("MaxPower",mPower);
    private MaxStat<Integer> mAllTimeMaxPower =
	new MaxStat<Integer>("AllTimeMaxPower",mMaxPower);
    private MaxStat<Integer> mBestAveragePower =
	new MaxStat<Integer>(0,"BestAveragePower",mAvgPower);
    private WindowAverageStat<Integer> m3sAverage =
	new WindowAverageStat("3sAveragePower", mPower,3);
    private WindowAverageStat<Integer> m10sAverage =
	new WindowAverageStat("10sAveragePower",mPower,10);
    private WindowAverageStat<Integer> m30sAverage =
	new WindowAverageStat("30sAveragePower",mPower,30);
    private ExternalBikePowerReceiver mExternalBikePowerReceiver;
    @Override protected void populateStatLists() {
	addToLists(new Stat[]{mPower,mLeftPower,mRightPower,mAvgPower,
			      mMaxPower,mAllTimeMaxPower,mBestAveragePower,
			      m3sAverage,m10sAverage,m30sAverage},
		   new ArrayList[] {mToBeBundled});
	addToLists(new Stat[]{mAvgPower},
		   new ArrayList[] {mToBeSaved,mCumulativeValues});
	addToLists(new Stat[]{mMaxPower},
		   new ArrayList[] {mToBeSaved,mComparativeValues});
	addToLists(new Stat[]{mAllTimeMaxPower},
		   new ArrayList[] {mToBeAllTimeSaved,mPostActivityValues});
	addToLists(new Stat[]{mBestAveragePower},
		   new ArrayList[] {mToBeAllTimeSaved,mPostActivityValues});
	addToLists(new Stat[]{m3sAverage,m10sAverage,m30sAverage},
		   new ArrayList[] {mCurrentValues});

    }
    public ReconPowerManager(ReconTranscendService rts) {
        prepare(rts);
	mExternalBikePowerReceiver = new ExternalBikePowerReceiver(mRTS, this);
	mExternalBikePowerReceiver.start();
    }
    public int getPower() {
        return mPower.getValue();
    }
    public int getLeftPower() {
        return mLeftPower.getValue();
    }
    public int getRightPower() {
        return mRightPower.getValue();
    }
    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    @Override
    public void onPowerChanged(int power) {
	if (power < 0) {	// invalid value
	    mPower.setValue(mPower.getInvalidValue());
	    mRightPower.setValue(mPower.getInvalidValue());
	    mLeftPower.setValue(mPower.getInvalidValue());
	}
	else {
	    mPower.setValue(power);
	    mLeftPower.setValue(power);
	    mRightPower.setValue(power);
	}
    }
    @Override
    public void onSensorConnected() {
    }
    @Override
    public void onSensorDisconnected() {
	mPower.setValue(mPower.getInvalidValue());
	mRightPower.setValue(mPower.getInvalidValue());
	mLeftPower.setValue(mPower.getInvalidValue());
    }
}