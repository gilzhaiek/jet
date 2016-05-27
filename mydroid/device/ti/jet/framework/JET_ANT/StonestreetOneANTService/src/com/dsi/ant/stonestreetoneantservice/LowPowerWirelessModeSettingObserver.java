package com.dsi.ant.stonestreetoneantservice;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Listens to changes to the 'low power wireless mode' setting and triggers matching events.
 */
public class LowPowerWirelessModeSettingObserver extends ContentObserver {
    public static final String TAG = LowPowerWirelessModeSettingObserver.class.getSimpleName();
    public static final boolean DEBUG = false;

    // TODO These values need to be used in Settings.Secure (part of platform set up).
    /** The WiLink is in ANT mode. */
    public static final String LOW_POWER_WIRELESS_MODE_ANT = "0";
    /** The WiLink is in BLE mode. */
    public static final String LOW_POWER_WIRELESS_MODE_BLE = "1";
    /**
     * The low power wireless mode to use if the setting does not exist.<br/>
     * DEFAULT is BLE since the first ever boot will default to BLE
     */
    public static final String LOW_POWER_WIRELESS_MODE_DEFAULT = LOW_POWER_WIRELESS_MODE_BLE;
    /** The key for reading the low power wireless mode setting from 'Settings.Secure'. */
    public static final String LOW_POWER_WIRELESS_MODE = "low_power_wireless_mode";

    /**
     * The events triggered by the {@link LowPowerWirelessModeSettingObserver}.
     */
    public interface ILowPowerWirelessModeChanged {
        /**
         * The WiLink 'low power wireless mode' has changed.
         * 
         * @param isAnt If the new setting is in ANT mode.
         */
        public void onIsLowPowerWirelessModeAntChange(boolean isAnt);
    }

    /** The low power wireless mode until the setting is changed. */
    private boolean mLastKnownIsLowPowerWirelessModeAnt;

    /** The context where the settings apply. */
    private Context mContext;

    /** Where to send mode changed events. */
    private ILowPowerWirelessModeChanged mCallback = null;

    private static final File RADIO_CONFIG_FILE = new File("/data/misc/recon/BT.conf");

    /**
     * Creates a new setting observer. Will only be running between {@link start()} and 
     * {@link stop()}. {@link destroy()} must be called when finished to prevent a context leak.
     * 
     * @param context The context the settings apply to.
     * @param handler The handler the observer will run on.
     */
    public LowPowerWirelessModeSettingObserver(Context context, Handler handler) {
        super(handler);

        if(null == context) {
            throw new IllegalArgumentException("Context must be provided");
        }
        mContext = context;

        mLastKnownIsLowPowerWirelessModeAnt = isLowPowerWirelessModeAnt(context);
    }

    /**
     * Clean up. This instance will no longer be usable after this is called.
     */
    public void destroy() {
        if(DEBUG) Log.v(TAG, "destroy");

        stop();
        mContext = null;
    }

    @Override
    public void onChange(boolean selfChange) {
        if(DEBUG) Log.v(TAG, "onChange");

        super.onChange(selfChange);

        boolean currentIsModeAnt = isLowPowerWirelessModeAnt(mContext);

        if(currentIsModeAnt != mLastKnownIsLowPowerWirelessModeAnt) {
            if(DEBUG) Log.i(TAG, "Mode setting has changed. Is ANT: "+ currentIsModeAnt);

            mLastKnownIsLowPowerWirelessModeAnt = currentIsModeAnt;

            ILowPowerWirelessModeChanged callback = mCallback;
            if(null != callback) {
                callback.onIsLowPowerWirelessModeAntChange(currentIsModeAnt);
            } else {
                if(DEBUG) Log.w(TAG, "No mode changed event sent as no callback set.");
            }
        }
    }

    /**
     * Read the current low power wireless mode from the config file.
     * 
     * @param context The context where the settings apply.
     * 
     * @return true if the setting is currently 'ANT mode'.
     */
    public static boolean isLowPowerWirelessModeAnt(Context context) {
        String firstLine = null;
        BufferedReader reader = null;
        boolean isAnt = false;
        try {
            reader = new BufferedReader(new FileReader(RADIO_CONFIG_FILE));
            firstLine = reader.readLine();
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read from " + RADIO_CONFIG_FILE);
            Log.e(TAG, "Exception: " + e);
        }

        if (firstLine != null) {
            isAnt = (firstLine.contains("mode=ANT"));
        }

        if(DEBUG) Log.i(TAG, "isLowPowerWirelessModeAnt = "+ isAnt);

        return isAnt;
    }

    /**
     * Begin listening for changes to the setting.
     *  
     * @param callback where to send events when a change occurs.
     */
    public void start(ILowPowerWirelessModeChanged callback) {
        if(DEBUG) Log.v(TAG, "start");

        mCallback = callback;

        mContext.getApplicationContext().getContentResolver().registerContentObserver(Settings.Secure.CONTENT_URI, true, this);
    }

    /**
     * Cancel listening for changes to the setting.
     */
    public void stop() {
        if(DEBUG) Log.v(TAG, "stop");

        mCallback = null;
        mContext.getApplicationContext().getContentResolver().unregisterContentObserver(this);
    }
}
