package com.reconinstruments.taptap;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class SensorsThread extends Thread implements SensorEventListener{
    /**
     * SAMPLINGRATE is in micro second
     */
    protected int mSamplingRate = 0;
    protected BlockingQueue<String> mQueue = new LinkedBlockingQueue<String>();
    protected FileLogger mFileLog;
    protected boolean mRunning = false;

    //private final static String TYPE_ACC = "acc";
    private final static String TYPE_ACC = "1";
    //private final static String TYPE_MAG = "mag"; 
    //private final static String TYPE_GYRO = "gyro"; 
    //private final static String TYPE_PRE = "pre"; 
    
//<T
    private final static String TYPE_TAP = "2";
//T>

    private SensorManager mSensorManager = null;
    private Sensor mAccelerometer = null;
    private Sensor mMagnetometer = null;
    private Sensor mGyroscope = null;
    private Sensor mPressure = null;
    private Sensor mTemperature = null;

    public boolean IsRunning(){
        return mRunning;
    }

    @SuppressWarnings("deprecation")
    public SensorsThread(Activity activity){
        mFileLog = new FileLogger();
        if(!mFileLog.Activate("SensorTest.csv")){
            return;
        }
        mFileLog.WriteToFile("Type, time, x, y, z\n");
        mSamplingRate = 1 *1000 *1000 / Main.GetHertz();  
        mSensorManager  = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
//<T
        mAccelerometer  = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//T>
	/*
        if(Main.IsAccChecked()){
            mAccelerometer  = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if(Main.IsMagChecked()){
            mMagnetometer   = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        if(Main.IsGyroChecked()){
            mGyroscope      = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        if(Main.IsPreChecked()){
            mPressure       = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        }
        if(Main.IsTempChecked()){
            mTemperature    = mSensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE);
        }
        */
        InitSensor();
        mRunning = true;
    }

    @Override
    public void run(){
        while(mRunning){
            if(mQueue.size() > 1){
                if(mFileLog.WriteToFile(mQueue.remove()) == false )
                    Main.SetSensorsText("Writting Error");
            }
        }
        if(mSensorManager!= null){
            mSensorManager.unregisterListener(this);
        }
        while(!mQueue.isEmpty()){
            if(mFileLog.WriteToFile(mQueue.poll()) == false)
                Main.SetSensorsText("Writting Error");
        }
        mFileLog.DeActivate();
    }

    private void InitSensor() {
	//<T
	mSensorManager.registerListener(this, mAccelerometer, mSamplingRate);
	//T>
	/*
        if(Main.IsAccChecked()){
            mSensorManager.registerListener(this, mAccelerometer, mSamplingRate);
        }
        if(Main.IsMagChecked()){
            mSensorManager.registerListener(this, mMagnetometer, mSamplingRate);
        }
        if(Main.IsGyroChecked()){
            mSensorManager.registerListener(this, mGyroscope, mSamplingRate);
        }
        if(Main.IsPreChecked()){
            mSensorManager.registerListener(this, mPressure, mSamplingRate);
        }
        if(Main.IsTempChecked()){
            mSensorManager.registerListener(this, mTemperature, mSamplingRate);
        }
        */
    }

    public void StopSensor(){
        mRunning = false;
    }
    
//<T
    public void logTapTapInterrupt(int zInt, int yInt, int xInt) {
	
        mQueue.add(TYPE_TAP + "," + GetTime() + "," + xInt + "," + yInt + "," + zInt + "\n");
    }
//T>

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSensorChanged(SensorEvent event) {
        String sensorType ="";
        switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                sensorType=TYPE_ACC;
                break;
	/*
            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorType=TYPE_MAG;
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorType=TYPE_GYRO;
                break;
            case Sensor.TYPE_PRESSURE:
                sensorType=TYPE_PRE;
                mQueue.add(sensorType + "," + GetTime() + "," + event.values[0] +"\n");
                //mFileLog.WriteToFile(sensorType + "," + GetTime() + "," + event.values[0] +"\n");
                return;
            case Sensor.TYPE_TEMPERATURE:
                Log.i("TemperatureSensor", "temp:" + event.values[0] + " , " + GetTime() + "\n");
                return;
	*/
        }
        mQueue.add(sensorType + "," + GetTime() + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "\n");
        //mQueue.add(sensorType + "," + event.timestamp + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "\n");
        //mFileLog.WriteToFile(sensorType + "," + GetTime() + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "\n");
    }

    private long GetTime() {
        return System.currentTimeMillis();
    }
}
