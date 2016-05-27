//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.battery.logger;

import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import com.reconinstruments.os.hardware.power.HUDPowerManager;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;
import android.os.Build;

public class BatteryLogService extends Service {
    public static final String TAG = BatteryLogService.class.getSimpleName();
    public static final boolean DEBUG = false;
    private static final String BAT_LOG_FOLDER = "BatteryLog";
    static final String PRODUCT_MODEL = Build.MODEL.equalsIgnoreCase("JET") ? "JET" : "SNOW";
    
    private int mLogMode;
    private int mLogInterval;//ToDo: for pooling mode later
    private boolean mLogEnable;
    private HUDPowerManager mHUDPowerManager = null;
    private PollingThread mThread;
    private BatteryStatus mBattStatus = new BatteryStatus();

    String mTimestamp = "";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        disableLog();
        unregisterReceiver(shutdownReceiver);
        super.onDestroy();
    }

    /**
     * The standard implementation for the Android Service class
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG,"onStartCommand");
        if (intent != null) {
            mLogMode = intent.getIntExtra("logMode", BatteryLogBootReceiver.NORMAL_MODE);
            mLogInterval = intent.getIntExtra("logInterval",100);
            Log.d(TAG, "mLogMode="+ mLogMode + ",mLogInterval=" + mLogInterval);
        }

        mBattStatus.clear();
        if(mHUDPowerManager == null) {
            //mHUDPowerManager= (HUDPowerManager) this.getSystemService("RECON_HUD_SERVICE");
            mHUDPowerManager= HUDPowerManager.getInstance();
            if(mHUDPowerManager == null)
                Log.e(TAG, "cannot find HUDPowerManager");
        }
        
        if (mLogMode == BatteryLogBootReceiver.NORMAL_MODE){
            registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            
        } else {
            mThread = new PollingThread();
            mThread.start();
        }
        
        mLogEnable = true;
        //Check shutdown event
        registerReceiver(shutdownReceiver, new IntentFilter(Intent.ACTION_SHUTDOWN));
        
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class PollingThread extends Thread{
        @Override
        public void run(){
            Log.v(TAG,"Polling thread start");
            try {
                pollingModeLog();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // this is where we deal with the data sent from the battery.
            if (intent.getAction().equals("android.intent.action.BATTERY_CHANGED")) {
                mBattStatus.voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                mBattStatus.percentage = (int) ( (float) 100 * ((float) level / (float)scale));
                //Log.d(TAG, "VoltageLogger","level = " + level + ", scale = " + scale + ", status (l/s) = " + batteryPct + "%");
                if (mHUDPowerManager != null){
                    mBattStatus.currentAverage = mHUDPowerManager.getCurrent(HUDPowerManager.AVERAGE_CURRENT_UPDATE_INTERVAL_10_SEC);
                    mBattStatus.currentNow = mHUDPowerManager.getCurrent(HUDPowerManager.INSTANT_CURRENT_UPDATE_INTERVAL_10_SEC);
                    mBattStatus.temperature = mHUDPowerManager.getBatteryTemperature_C();
                }

                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,-1);
                boolean extralog = false;
                //Update plug type and battery charge/discharge status if changed
                if(plugged != mBattStatus.plugged || status != mBattStatus.status)
                {
                    mBattStatus.plugged = plugged;
                    mBattStatus.status = status;
                    extralog = true;
                }

                try {
                    writefile(mBattStatus, extralog);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    };

    private BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent){
            disableLog();
        }
    };
    
    private void writefile(BatteryStatus battStatus, boolean extralog) throws IOException
    {
        boolean is_log_firsttime = false;
        String filename = Environment.getExternalStorageDirectory().getPath() + 
                            "/" + BAT_LOG_FOLDER;
        if (mTimestamp.equals("")) {
            mTimestamp = (String) (android.text.format.DateFormat.format("yyyy-MM-dd_hhmmss", new java.util.Date()));

            File file = new File(filename);
            if( !file.exists() || !file.isDirectory()) {
                file.mkdirs();
            }
            is_log_firsttime = true;
        }

        filename += "/" + PRODUCT_MODEL + "." +mTimestamp + ".csv";
        FileOutputStream outputStream = null;

        long currentTime = System.currentTimeMillis();

        if(DEBUG)
            Log.d(TAG, currentTime + ": " +
                       " voltage=" + battStatus.voltage +
                       " currentAvg=" + battStatus.currentAverage +
                       " percentage=" + battStatus.percentage);

        //Append to the file already exists; Create the file if not exists.
        try {
            outputStream = new FileOutputStream(filename, true);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "fail to open file");
            e.printStackTrace();
        }

        String logdata="";
        if(is_log_firsttime == true){
            //Add title for csv file
            logdata = "hhmmss,time,V,Average I,Current,Capacity,Temp,plug type,charge status\n";
        }

        logdata += (String)(android.text.format.DateFormat.format("hhmmss", new java.util.Date())) + "," +
                                currentTime + "," +
                                battStatus.voltage + "," +
                                battStatus.currentAverage + "," +
                                battStatus.currentNow + "," +
                                battStatus.percentage + "," +
                                battStatus.temperature;
        if(extralog)
            logdata += ("," + battStatus.plugged + "," + battStatus.status);
        logdata +="\n";

        try {
            outputStream.write(logdata.getBytes());
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.e(TAG, "fail to write file");
            e.printStackTrace();
            if(outputStream != null) {
                outputStream.close(); // Might throw an expection but we should throw it up if this happens
            }
        }
    }

    private void pollingModeLog() throws IOException
    {
        while( mLogMode == BatteryLogBootReceiver.POLLING_MODE ){
            
            if (mHUDPowerManager != null) {
                mBattStatus.percentage = mHUDPowerManager.getBatteryPercentage();
                mBattStatus.voltage = mHUDPowerManager.getBatteryVoltage();
                mBattStatus.currentAverage = mHUDPowerManager.getCurrent(HUDPowerManager.AVERAGE_CURRENT_UPDATE_INTERVAL_10_SEC);
                mBattStatus.currentNow = mHUDPowerManager.getCurrent(HUDPowerManager.INSTANT_CURRENT_UPDATE_INTERVAL_10_SEC);
                mBattStatus.temperature = mHUDPowerManager.getBatteryTemperature_C();

                writefile(mBattStatus,false);
            }

            try {
                Thread.sleep(mLogInterval*1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void disableLog()
    {
        if(mLogEnable == false)
            return;
        Log.d(TAG,"Disable Log");
        if (mLogMode == BatteryLogBootReceiver.NORMAL_MODE)
            unregisterReceiver(batteryInfoReceiver);
        else
            mLogMode=BatteryLogBootReceiver.NORMAL_MODE;//return default state
        mLogEnable = false;
    }
    
    class BatteryStatus {
        int voltage;
        int currentAverage;
        int percentage;
        int currentNow;
        int temperature;
        int plugged;
        int status;

        public BatteryStatus() {
            clear();
        }

        public void clear() {
            voltage = HUDPowerManager.BAD_VALUE;
            currentAverage = HUDPowerManager.BAD_VALUE;
            percentage = HUDPowerManager.BAD_VALUE;
            currentNow = HUDPowerManager.BAD_VALUE;
            temperature = HUDPowerManager.BAD_VALUE;
            plugged = HUDPowerManager.BAD_VALUE;
            status = HUDPowerManager.BAD_VALUE;
        }
    }
}
