package com.reconinstruments.sensorstest;
import android.util.Log;

import com.reconinstruments.os.hardware.power.HUDPowerManager;

public class HudOsThread extends Thread{
    public static final String TAG = "SensorsLogHudOsThread";
    protected boolean mRunning = false;
    protected int mIntervalTimeMSec;
    protected FileLogger mFileLog;
    private HUDPowerManager mHUDPowerManager = null;

    public HudOsThread(int interval){        
        this.mIntervalTimeMSec = interval * 1000;
        mHUDPowerManager = HUDPowerManager.getInstance();
        if(mHUDPowerManager == null) {
            Log.e(TAG, "cannot find HUDPowerManager");
            return;
        }
        mHUDPowerManager.setCompassTemperature(true);

        mFileLog = new FileLogger();
        if(!mFileLog.Activate("TemperatureTest.csv")){
            Log.e(TAG, "cannot open file");
            return;
        }
        mFileLog.WriteToFile("time,MainT,CompassT\n");
        this.mRunning = true;
    }

    public boolean IsRunning(){
        return mRunning;
    }

    public void StopHudOsThread(){
        mRunning = false;        
    }

    @Override
    public void run(){
        while(mRunning){
            int MainBoardTemperature = mHUDPowerManager.getBoardTemperature();
            int CompassTemperature = mHUDPowerManager.getCompassTemperature();            
            long t= System.currentTimeMillis();
            boolean status;
            status=mFileLog.WriteToFile(t + "," + MainBoardTemperature 
                    + "," + CompassTemperature +  "\n");
            if(status== false)
                Main.SetTemperatureText("Error");
            try {
                Thread.sleep(mIntervalTimeMSec);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        mHUDPowerManager.setCompassTemperature(false);
        mHUDPowerManager = null;       
        mFileLog.DeActivate();
    }
}
