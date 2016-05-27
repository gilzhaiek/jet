//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.applauncher.transcend;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.content.Intent;
public class ReconLocationManager extends ReconStatsManager{
    private static final String TAG = "ReconLocationManager";
    private Location mLocation = null;
    private Location mPreviousLocation = null;
    private SimpleLocationLogger mSimpleLocationLogger = null;
    private int mNumberOfSatellites = 0;
    public  long mTimeOfLastUpdate = 0;
    private Intent mGpsStateChange =
        new Intent("com.reconinstruments.applauncer.transcend.GPS_FIX_CHANGED");
    public static final int ACCURACY_IN_HDOP = 10;
    // ^I am assuming that HDOP is accuracy / 10.
    private boolean mIsGpsFix = false;
    private boolean mWasGpsFix = false;
    private boolean mEverHadGps = false;
    private static final long TIME_TRESHHOLD_FOR_NO_FIX = 3000; //milisecons
    @Override
    public String getBasePersistanceName() {
        return TAG;
    }
    public void updateMembers(Location loc,int numSats){
        mPreviousLocation = mLocation;
        mLocation = loc;
        mNumberOfSatellites = numSats;
        mTimeOfLastUpdate = SystemClock.elapsedRealtime();
        if (shouldUpdateComparativeValues()) {
            mSimpleLocationLogger.push(loc);
        } 
    }
    public Location getLocation () {
        return mLocation;
    }
    public Location getPreviousLocation () {
        return mPreviousLocation;
    }
    public void setLocation(Location l) {
        mLocation = l;
    }
    public SimpleLocationLogger getSimpleLocationLogger() {
        return mSimpleLocationLogger;
    }
    public boolean bundleValid() {
        return isGPSFix();
    }
    @Override
    protected Bundle generateBundle() {
        super.generateBundle();
        mBundle.putParcelable("Location", mLocation);
        mBundle.putParcelable("PreviousLocation",mPreviousLocation );
        mBundle.putBoolean("IsGPSFix",isGPSFix());
        mBundle.putBoolean("EverHadGps",mEverHadGps);
        return mBundle;
    }
    public ReconLocationManager(ReconTranscendService rts) {
        mSimpleLocationLogger = new SimpleLocationLogger();
        prepare(rts);
        generateBundle();
    }
    public void setMockSpeed(float s) {
        mLocation.setSpeed(s);
    }
    @Override public void saveToTempState() {
        mSimpleLocationLogger.dumpBuffersToFile();
    }
    @Override public void saveState() {
        int type = mRTS.getSportsActivityManager().getType();
        SimpleLocationLogger.saveTempFile(type);        
    }
    @Override
    public void resetStats() {
        mSimpleLocationLogger.reset();
    }
    public int getNumberOfSatellites() {
        return mNumberOfSatellites;
    }
    public boolean isGPSFix() { // GPS fix as for having speed
        return ((SystemClock.elapsedRealtime() - mTimeOfLastUpdate) <
                 TIME_TRESHHOLD_FOR_NO_FIX);
    }
    private void announceGpsFixStateChange() {
        mGpsStateChange.putExtra("isGpsFix",mIsGpsFix);
        mRTS.sendStickyBroadcast(mGpsStateChange);
    }
    @Override public void updateCurrentValues() {
        mWasGpsFix = mIsGpsFix;
        mIsGpsFix = isGPSFix();
        if (mWasGpsFix ^ mIsGpsFix) { // state chagne "^" is exclusive or
            announceGpsFixStateChange();
        }
        if (mIsGpsFix) mEverHadGps = true;
    }
}
