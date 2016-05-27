package com.reconinstruments.applauncher.transcend;
import com.reconinstruments.externalsensors.*;
import android.content.Context;
import android.os.Build;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import com.reconinstruments.utils.stats.StatsUtil;
import java.util.ArrayList;
public class ReconCadenceManager extends ReconStatsManager implements IExternalCadenceListener {
    private static final String TAG = "ReconCadenceManager";
    public static final int INVALID_CADENCE = StatsUtil.INVALID_CADENCE;
    private Stat<Integer> mCadence = new Stat<Integer>(INVALID_CADENCE,"Cadence");
    private AverageStat<Integer> mAvgCadence =
        new AverageStat<Integer>(0,"AverageCadence",mCadence);
    private ExternalCadenceReceiver mExternalCadenceReceiver;
    @Override protected void populateStatLists() {
	addToLists(new Stat[]{mCadence,mAvgCadence},new ArrayList[]{mToBeBundled});
        addToLists(new Stat[]{mAvgCadence},
                   new ArrayList[] {mToBeSaved,mCumulativeValues});
    }
    public ReconCadenceManager(ReconTranscendService rts) {
        prepare(rts);
        mExternalCadenceReceiver = new ExternalCadenceReceiver(mRTS, this);
        mExternalCadenceReceiver.start();
    }
    public int getCadence() {
        return mCadence.getValue().intValue();
    }
    public int getAvgCadence() {
        return mAvgCadence.getValue().intValue();
    }
    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    public void onCadenceChanged(int cadence){
        if (cadence < 0) {
            mCadence.setValue(mCadence.getInvalidValue());
        }
        else {
            mCadence.setValue(cadence);
        }
    }
    public void onSensorConnected() {
    }
    public void onSensorDisconnected() {
        mCadence.setValue(mCadence.getInvalidValue());
    }
}