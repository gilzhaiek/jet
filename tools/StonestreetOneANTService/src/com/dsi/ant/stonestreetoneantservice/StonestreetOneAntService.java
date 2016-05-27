package com.dsi.ant.stonestreetoneantservice;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.dsi.ant.stonestreetoneantservice.LowPowerWirelessModeSettingObserver.ILowPowerWirelessModeChanged;

/** The Android Service that runs in the background to allow the ANT Radio Service to access the 
 * ANT chip through the API for the Stonestreet One stack.
 */
public class StonestreetOneAntService extends Service {
    public static final String TAG = StonestreetOneAntService.class.getSimpleName();
    public static final boolean DEBUG = BuildConfig.DEBUG && true;

    /** The piece which handles detecting if chips are added and removed. */
    private StonestreetOneAntChipDetector mAntChipDetector;

    /** Listen for settings changes. */
    private LowPowerWirelessModeSettingObserver mLowPowerWirelessModeObserver;

    /** 
     * Callback for events when the WiLink mode changes, which forwards them to the chip detector 
     * instance.
     */
    private ILowPowerWirelessModeChanged mLowPowerWirelessModeChangedListener = 
            new ILowPowerWirelessModeChanged() {

        // synchronized to not run at same time as lifecycle changes
        @Override
        public synchronized void onIsLowPowerWirelessModeAntChange(boolean isAnt) {
            if(DEBUG) Log.v(TAG, "onIsLowPowerWirelessModeAntChange: "+ isAnt);

            if(null != mAntChipDetector) {
                mAntChipDetector.onIsLowPowerWirelessModeAntChange(isAnt);
            }
        }

    };

    @Override
    public synchronized IBinder onBind(Intent bindIntent) {
        if(DEBUG) Log.v(TAG, "onBind");

        return mAntChipDetector.getBinder(bindIntent);
    }

    @Override
    public synchronized void onCreate() {
        if(DEBUG) Log.v(TAG, "onCreate");

        super.onCreate();

        if(BuildConfig.DEBUG) Log.w(TAG, "Debug build");

        mLowPowerWirelessModeObserver = 
                new LowPowerWirelessModeSettingObserver(this, new Handler());

        mAntChipDetector = new StonestreetOneAntChipDetector(
                LowPowerWirelessModeSettingObserver.isLowPowerWirelessModeAnt(this));

        mLowPowerWirelessModeObserver.start(mLowPowerWirelessModeChangedListener);
    }

    @Override
    public synchronized void onDestroy() {
        if(DEBUG) Log.v(TAG, "onDestroy");

        mLowPowerWirelessModeObserver.stop();
        mLowPowerWirelessModeObserver.destroy();

        mAntChipDetector.destroy();
        mAntChipDetector = null;

        super.onDestroy();
    }

}
