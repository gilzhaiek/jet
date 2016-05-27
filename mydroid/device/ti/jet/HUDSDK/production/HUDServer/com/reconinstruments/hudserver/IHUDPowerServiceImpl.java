package com.reconinstruments.hudserver;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.RemoteException;
import android.os.RemoteException;
import android.os.PowerManager;
import android.os.BatteryManager;
import android.os.ServiceManager;
import android.util.Log;
import com.reconinstruments.os.hardware.power.IHUDPowerService;
import com.reconinstruments.lib.hardware.HUDPower;
import com.reconinstruments.os.hardware.power.HUDPowerManager;
import android.app.ActivityManagerNative;

class IHUDPowerServiceImpl extends IHUDPowerService.Stub {
    private static final String TAG = "IHUDPowerServiceImpl";
    private static final boolean DEBUG = false;
    private final Context context;
    private final HUDPower mHUDPower;

    private String mLastShutdownReason = "NULL";

    IHUDPowerServiceImpl(Context context) {
        this.context = context;
        this.mHUDPower = new HUDPower();

        PowerManager pm = (PowerManager) this.context.getSystemService(Context.POWER_SERVICE);
        this.mLastShutdownReason = pm.getLastShutdownReason();

        Intent intent = new Intent(Intent.ACTION_SHUTDOWN_REASON);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(HUDPowerManager.EXTRA_REASON, getLastShutdownReason());
        intent.putExtra(HUDPowerManager.EXTRA_REASON_STR, this.mLastShutdownReason);

        ActivityManagerNative.broadcastStickyIntent(intent, null);

        if(DEBUG) {
            Log.d(TAG, "lastShutdownReason: " + this.mLastShutdownReason + ":" + getLastShutdownReason());
        }

        this.context.registerReceiver(mBatteryDeadReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public int getBatteryVoltage() {
        if (DEBUG) Log.d(TAG, "Getting Battery Voltage in mV");
        return this.mHUDPower.getBatteryVoltage_uV()/1000;
    }

    public int getAverageCurrent() {
        if (DEBUG) Log.d(TAG, "Getting Average Current in mA");
        return this.mHUDPower.getAverageCurrent_uA()/1000;
    }

    public int getCurrent() {
        if (DEBUG) Log.d(TAG, "Getting Current in mA");
        return this.mHUDPower.getCurrent_uA()/1000;
    }

    public int getBatteryPercentage() {
        if (DEBUG) Log.d(TAG, "Getting Battery percentage");
        return this.mHUDPower.getBatteryPercentage();
    }

    public int getBatteryTemperature_C() {
        if (DEBUG) Log.d(TAG, "Getting Battery Temperature in Celsius");
        return this.mHUDPower.getBatteryTemperature_C10th()/10;
    }

    public int setCompassTemperature(boolean enable_disable) {
        if (DEBUG) Log.d(TAG, "Enable/Disable Compass temerature check");
        return this.mHUDPower.setCompassTemperature(enable_disable);
    }

    public int getCompassTemperature() {
        if (DEBUG) Log.d(TAG, "Getting Compass Temperature raw data");
        return this.mHUDPower.getCompassTemperature();
    }

    public int getBoardTemperature() {
        if (DEBUG) Log.d(TAG, "Getting Mainboard Temperature");
        return this.mHUDPower.getBoardTemperature_C();
    }

    public int getLastShutdownReason() {
        if(this.mLastShutdownReason.equals("BatteryDead")) return HUDPowerManager.SHUTDOWN_BATT_REMOVED;
        if(this.mLastShutdownReason.equals("Reboot")) return HUDPowerManager.SHUTDOWN_GRACEFUL;
        if(this.mLastShutdownReason.equals("Shutdown")) return HUDPowerManager.SHUTDOWN_GRACEFUL;
        if(this.mLastShutdownReason.equals("BOOT")) return HUDPowerManager.SHUTDOWN_ABRUPT;
        return HUDPowerManager.SHUTDOWN_GRACEFUL; // for now..
    }

    public int setFreqScalingGovernor(int governor) {
        if (DEBUG) Log.d(TAG, "Setting frequency scaling governor: " + governor);
        return this.mHUDPower.setFreqScalingGovernor(governor);
    }

    BroadcastReceiver mBatteryDeadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "Battery status changed");

            int health = intent.getIntExtra("health", BatteryManager.BATTERY_HEALTH_UNKNOWN);
            if (health == BatteryManager.BATTERY_HEALTH_DEAD) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                Log.d(TAG, "Battery is removed, reboot the device");
                pm.reboot("BatteryDead");
            }
        }
    };

}
