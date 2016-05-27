package com.reconinstruments.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryUtil {
    /**
     * 
     * Returns battery Level 0 <= lvl <= 1
     * 
     * @param context
     * @return
     */
    public static float getBatteryLevel(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level < 0 || scale < 0) { // Something is wrong, battery level not detected. Failsafe
            return -1;
        }
        float batteryPct = level / (float)scale;
        
        return batteryPct;
    }
}
