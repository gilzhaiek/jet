package com.reconinstruments.os.analyticsservice.internalmonitors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;
import com.reconinstruments.os.hardware.power.HUDPowerManager;

import com.reconinstruments.os.analyticsservice.AnalyticsServiceApp;

public class BatteryMonitor extends AnalyticsInternalMonitor {
    private final String TAG = this.getClass().getSimpleName();

    private final static boolean DEBUG = false;
    private HUDPowerManager mHUDPowerManager = null;
    private static int mPrevBatteryLevel = -1;

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                recordBatteryEvent(intent);
            }
        }
    };

    public BatteryMonitor(AnalyticsServiceApp analyticsService, String configJSON) {

        super("battery_monitor", analyticsService, configJSON);  // will load event filters from configJSON

        // register for broadcast connection events, shutdown,
        Intent intent = mParentService.registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onDestroy() {
        mParentService.unregisterReceiver(mBatteryInfoReceiver);
    }

    public void setHUDPowerManager(HUDPowerManager powerManager) {
        mHUDPowerManager = powerManager;
    }

    private void recordBatteryEvent(Intent intent) {
        if(mHUDPowerManager != null) {
            String batteryLevel = getBatteryLevel(intent);
            if(batteryLevel != null) {
                String batteryStatus = getBatteryStatus(intent);

                //get battery temperature
                String batteryTemperature = String.format("%d", mHUDPowerManager.getBatteryTemperature_C());
                //get board temperature
                String boardTemperature = String.format("%d", mHUDPowerManager.getBoardTemperature());

                if (DEBUG) {
                    Log.d(TAG, "BatteryMeasurement with level:" + batteryLevel + "  status:" + batteryStatus + "  temp(C):" + batteryTemperature);
                }
                recordEvent("BatteryMeasurement", batteryLevel, batteryStatus, batteryTemperature);
            }
        }
    }

    // Copied shamelessly from com.android.settings.Utils
    private static String getBatteryLevel(Intent batteryChangedIntent) {
        int voltage = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        int level = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if(level != mPrevBatteryLevel) {
            mPrevBatteryLevel = level;
            return (String.valueOf(level * 100 / scale) + "%; " + String.valueOf(voltage) +"mV");
        }
        return null;
    }

    private static String getBatteryStatus(Intent batteryChangedIntent) {
        final Intent intent = batteryChangedIntent;

        int plugType = intent.getIntExtra("plugged", 0);
        int status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
        String statusString;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            statusString = "Charging";
            if (plugType > 0) {
                statusString = statusString + " " + ((plugType == BatteryManager.BATTERY_PLUGGED_AC) ? "AC" : "USB");
            }
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            statusString = "Discharging";
        } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            statusString = "Not Charging";
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            statusString = "Full";
        } else {
            statusString = "Unknown";
        }

        return statusString;
    }
}
