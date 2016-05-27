package com.reconinstruments.applauncher.transcend;
import com.reconinstruments.externalsensors.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import com.reconinstruments.utils.stats.StatsUtil;
import java.util.ArrayList;

public class ReconHRManager extends ReconStatsManager implements IExternalHeartrateListener {
    private static final String TAG = "ReconHRManager";
    public static final short INVALID_HR = StatsUtil.INVALID_HR;
    private Stat<Integer>mHR = new Stat<Integer>((int)INVALID_HR,"HeartRate");
    private AverageStat<Integer> mAvgHR =
	new AverageStat<Integer>((int)INVALID_HR,"AverageHeartRate",mHR);
    private ExternalHeartrateReceiver mExternalHeartrateReceiver;
    @Override protected void populateStatLists() {
	addToLists(new Stat[]{mHR,mAvgHR},
		   new ArrayList[]{mToBeBundled});
	addToLists(new Stat[]{mAvgHR},
		   new ArrayList[]{mToBeSaved,mCumulativeValues});
    }
    public ReconHRManager(ReconTranscendService rts) {
        prepare(rts);
        mExternalHeartrateReceiver = new ExternalHeartrateReceiver(mRTS, this);
	mExternalHeartrateReceiver.start();
    }
    public int getHR() {
        return mHR.getValue().intValue();
    }
    public int getAvgHR() {
	return mAvgHR.getValue().intValue();
    }

    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    @Override
    public void onHeartrateChanged(int hr) {
	//Log.v(TAG,"onHeartrateChanged: "+hr);
	if (hr < 0) mHR.setValue(mHR.getInvalidValue());
	else mHR.setValue(hr);
    }
    @Override
    public void onSensorConnected() {
    }
    @Override
    public void onSensorDisconnected() {
	mHR.setValue(mHR.getInvalidValue());
    }
}