package com.reconinstruments.voltagelogger;

import java.io.FileOutputStream;
import java.util.ArrayList;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.reconinstruments.os.hardware.power.HUDPowerManager;


public class BattLogService extends Service {
    public static final String TAG = BattLogService.class.getSimpleName();

    private static final int DEFAULT_INTERVAL_TIME_MSEC = 100*1000;

    public boolean mLogEnable;    
    public RunningMode mRunningMode;
    public int mIntervalTimeMSec;

    String Timestamp = "";

    private HUDPowerManager mHUDPowerManager = null;
    private IBinder mBinder = new LocalBinder();
    private BatteryStatus mTmpBattStatus = new BatteryStatus();
    
    private ArrayList<BattLogListener> mListeners = new ArrayList<BattLogListener>();

    public class LocalBinder extends Binder {
        BattLogService getService() {
            return BattLogService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service binding");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service unbinding");
        return false;
    }

    @Override
    public void onCreate ()   {
        Log.d(TAG, "VoltageLogger Service created");

        mLogEnable = false;
        mRunningMode = RunningMode.Normal;
        mIntervalTimeMSec = DEFAULT_INTERVAL_TIME_MSEC;

        registerReceiver(this.batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if(mHUDPowerManager == null) {
            mHUDPowerManager = HUDPowerManager.getInstance();
            if(mHUDPowerManager == null) {
                Log.e(TAG, "cannot find HUDPowerManager");
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        if(mRunningMode.equals(RunningMode.Normal)) {
            unregisterReceiver(batteryInfoReceiver);
        }
    }

    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // this is where we deal with the data sent from the battery.
            if (intent.getAction().equals("android.intent.action.BATTERY_CHANGED"))
            {
                mTmpBattStatus.voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                mTmpBattStatus.percentage = (int) ( (float) 100 * ((float) level / (float)scale));

                if (mHUDPowerManager != null) {
                    mTmpBattStatus.currentAverage = mHUDPowerManager.getCurrent(HUDPowerManager.AVERAGE_CURRENT_UPDATE_INTERVAL_10_SEC);
                    mTmpBattStatus.currentNow = mHUDPowerManager.getCurrent(HUDPowerManager.INSTANT_CURRENT_UPDATE_INTERVAL_10_SEC);
                    mTmpBattStatus.temperature = mHUDPowerManager.getBatteryTemperature_C();
                }

                for(BattLogListener listener : mListeners) {
                   listener.onBatteryStatusChanged(mTmpBattStatus);
                }

                if(mLogEnable) {
                    writefile(mTmpBattStatus);
                }
            }
        }
    };

    public synchronized void setRunningMode(boolean enableLog, RunningMode runningMode, int intervalTimeSec){
        mLogEnable = enableLog;
        mIntervalTimeMSec = intervalTimeSec*1000;

        if(!mRunningMode.equals(runningMode)){
            mRunningMode = runningMode;
            if(mRunningMode.equals(RunningMode.Polling)) {
                Log.d(TAG, "Change to polling mode");
                unregisterReceiver(batteryInfoReceiver);
                Thread th = new Thread() {
                    @Override
                    public void run(){
                        pollingModeLog();
                    }
                };
                th.start();
            } else {
                Log.d(TAG, "Change to normal mode");
                registerReceiver(this.batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            }
        }
    }

    public void registerHandler(BattLogListener battLogListener) {
       if(!mListeners.contains(battLogListener))
          mListeners.add(battLogListener);
    }

    public void unregisterHandler(BattLogListener battLogListener) {
       if(mListeners.contains(battLogListener))
          mListeners.remove(battLogListener);
    }

    private void writefile(BatteryStatus battStatus)
    {
        if (Timestamp == "") {
            Timestamp = (String) (android.text.format.DateFormat.format("yyyy-MM-dd_hhmmss", new java.util.Date()));
        }

        String filename = "batt." + Timestamp + ".csv";
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_APPEND);
            outputStream.write((""+ System.currentTimeMillis() + "," +
                    battStatus.voltage + "," +
                    battStatus.currentAverage + "," +
                    battStatus.percentage + "," +
                    battStatus.currentNow + "," +
                    battStatus.temperature + "\n").getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pollingModeLog()
    {
        while(mRunningMode.equals(RunningMode.Polling)){
            //Log.d(TAG,"update");
            if (mHUDPowerManager != null) {
                mTmpBattStatus.percentage = mHUDPowerManager.getBatteryPercentage();
                mTmpBattStatus.voltage = mHUDPowerManager.getBatteryVoltage();
                mTmpBattStatus.currentAverage = mHUDPowerManager.getCurrent(HUDPowerManager.AVERAGE_CURRENT_UPDATE_INTERVAL_10_SEC);
                mTmpBattStatus.currentNow = mHUDPowerManager.getCurrent(HUDPowerManager.INSTANT_CURRENT_UPDATE_INTERVAL_10_SEC);
                mTmpBattStatus.temperature = mHUDPowerManager.getBatteryTemperature_C();

                for(BattLogListener listener : mListeners) {
                   listener.onBatteryStatusChanged(mTmpBattStatus);
                }

                if(mLogEnable) {
                    writefile(mTmpBattStatus);
                }
            }

            try {
                Thread.sleep(mIntervalTimeMSec);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
