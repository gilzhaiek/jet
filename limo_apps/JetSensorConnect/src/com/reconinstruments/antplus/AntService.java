
package com.reconinstruments.antplus;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import com.reconinstruments.utils.SettingsUtil;

public class AntService extends Service implements IAntContext {
    private static final String TAG = "AntService";
    
    public static final String ACTION_ANT_SERVICE = "RECON_ANT_SERVICE";
    public static final String EXTRA_ANT_SERVICE_CONNECT = "connect";
    public static final String EXTRA_SENSOR_ID = "deviceId";
    public static final String EXTRA_SENSOR_PROFILE = "profile";
    public static final String EXTRA_SHOW_PASSIVE_NOTIFICATION = "show_passive_notification";
    public static final String EXTRA_ANT_SERVICE_DISCONNECT_ALL = "disconnect_all";
    public static final String EXTRA_ANT_SERVICE_START_ATTEMPT_CONNECT = "start_attempt_connect";
    public static final String EXTRA_ANT_SERVICE_CONNECT_RETRY = "connect_retry";
    public static final String EXTRA_ANT_SERVICE_STOP_ATTEMPT_CONNECT = "stop_attempt_connect";

    public static final String EXTRA_ANT_SERVICE_BIKE_CALIBRATION_REQUEST = "calibration_request";

    private HRManager mHRManager; // heart rate manager
    private BCManager mBCManager; // bike cadence manager
    private BSManager mBSManager; // bike speed manager
    private BSCManager mBSCManager; // bike speed cadence combined manager
    private BWManager mBWManager; // bike power manager
    private AntPlusManager mAntPlusManager;

    public static boolean mShowPassiveNotification = true; // option to show passive notification or not, true to show by default
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate()");
        mAntPlusManager = AntPlusManager.getInstance(this.getApplicationContext());
        mHRManager = new HRManager(this);
        mBCManager = new BCManager(this);
        mBSManager = new BSManager(this);
        mBSCManager = new BSCManager(this);
        mBWManager = new BWManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        //if not ANT mode just return
        if(SettingsUtil.getBleOrAnt() != SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT) return START_STICKY;
        
        Log.v(TAG, "onStartCommand");
        ArrayList<AntPlusSensor> devices = mAntPlusManager.getRememberedDevice();
        if (intent == null || intent.getExtras() == null)  {
            if(devices != null && devices.size() > 0){
                // connect all of the most recent device
                Log.d(TAG, "connecting to all of the most recent device.");
                attemptConnect();
            }
            return START_STICKY;
        }
        Bundle b = (intent != null) ? intent.getExtras() : null;
        if (b != null) {
            // start attempting connect to the most recent devices
            if (b.getBoolean(EXTRA_ANT_SERVICE_START_ATTEMPT_CONNECT)) {
                if(devices == null || devices.size() == 0) return START_STICKY; // no devices
                Log.d(TAG, "start attempting to connect the most recent devices.");
                attemptConnect();
                return START_STICKY;
            }
            // stop attempting connect to the most recent devices
            if (b.getBoolean(EXTRA_ANT_SERVICE_STOP_ATTEMPT_CONNECT)) {
                Log.d(TAG, "stop attempting to connect the most recent devices.");
                mShowPassiveNotification = true;
                mAttempConnectHandler.removeCallbacksAndMessages(null);
                return START_STICKY;
            }
            if (b.getBoolean(EXTRA_ANT_SERVICE_DISCONNECT_ALL)) {
                mAttempConnectHandler.removeCallbacksAndMessages(null);
                // Disconnect all
                for (AntPlusSensor sensor : devices) {
                    Log.v(TAG, "Disconnecting ANT sensor " + sensor.getDeviceNumber());
                    sensor.setAutoReconnect(true);
                    mShowPassiveNotification = false; // disconnect silently
                    connectOrDisconnect(sensor.getProfile(), sensor.getDeviceNumber(), false);
                }
                return START_STICKY;
            }
            if (b.getBoolean(EXTRA_ANT_SERVICE_BIKE_CALIBRATION_REQUEST)) { // for bike power calibration
                mBWManager.requestManualCalibration();
                return START_STICKY;
            }
            int profile = b.getInt(EXTRA_SENSOR_PROFILE,
                    HRManager.EXTRA_HEART_RATE_PROFILE);
            int deviceId = b.getInt(EXTRA_SENSOR_ID, -1);
            boolean connect = b.getBoolean(EXTRA_ANT_SERVICE_CONNECT); // true: connect, false: disconnect
            mShowPassiveNotification = b.getBoolean(EXTRA_SHOW_PASSIVE_NOTIFICATION, true);
            
            if(!connect){
                AntPlusSensor device = mAntPlusManager.getDevice(profile, deviceId);
                if(device != null) device.setAutoReconnect(false);
            }
            connectOrDisconnect(profile, deviceId, connect);
        }
        return START_STICKY;
    }
    
    private Handler mAttempConnectHandler = new Handler();
    private void attemptConnect(){
        mAttempConnectHandler.postDelayed(new Runnable() {
            int profile = HRManager.EXTRA_HEART_RATE_PROFILE;
            int delayTime = 1 * 1000;
            @Override
            public void run() {
                if(profile > BSCManager.EXTRA_BIKE_POWER_PROFILE) {
                    profile = HRManager.EXTRA_HEART_RATE_PROFILE;
                    delayTime = 1 * 1000; // 1 seconds delay
                }else if(profile > AntPlusProfileManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE){
                    delayTime = 5 * 1000; // 5 seconds delay
                }
                AntPlusSensor sensor = mAntPlusManager.getMostRecent(profile);
                profile ++;
                if(sensor != null && !sensor.isConnected() && sensor.isAutoReconnect()){
                    mShowPassiveNotification = false;
                    connectOrDisconnect(sensor.getProfile(), sensor.getDeviceNumber(), true);
                }
                mAttempConnectHandler.postDelayed(this, delayTime); // 1 seconds delay to next sensor, 5 seconds delay to next reconnecting
            }
        }, 0);
    }
    
    private void connectOrDisconnect(int profile, int deviceId, boolean connect){
        if(connect){
            disconnect(profile, deviceId); //disconnect the devices in this profile except itself first
        }
        AntPlusSensor device = mAntPlusManager.getDevice(profile, deviceId);
        if (HRManager.EXTRA_HEART_RATE_PROFILE == profile) { // heart rate profile
            if (connect) {
                if(device != null && !device.isConnected()){//avoid to reconnect this device
                    mAntPlusManager.setMostRecent(profile, deviceId);
                    mHRManager.handleReset(deviceId);
                }
            } else {
                mHRManager.releaseAccess();
                mHRManager.broadcastDisconnectedMessage(deviceId, 0);
            }
        }else if(BCManager.EXTRA_BIKE_CADENCE_PROFILE == profile){ // bike cadence profile
            if (connect) {
                if(device != null && !device.isConnected()){//avoid to reconnect this device
                    mAntPlusManager.setMostRecent(profile, deviceId);
                    mBCManager.handleReset(deviceId);
                }
                // Ignore Combined device b/c we already have cadence only
                BSCManager.ignoreCadence(true); 
                BWManager.ignoreCadence(true);
            } else {
                mBCManager.releaseAccess();
                AntPlusSensor sensor = mAntPlusManager.getMostRecent(AntPlusProfileManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE);
                if(sensor!= null && sensor.isConnected()){ // if spd/cad is connected, then use spd/cad data
                    BSCManager.ignoreCadence(false);
                }else{
                    BWManager.ignoreCadence(false);
                }
                mBCManager.broadcastDisconnectedMessage(deviceId, 0);
            }
        }else if(BSManager.EXTRA_BIKE_SPEED_PROFILE == profile){ // bike speed profile
            if (connect) {
                if(device != null && !device.isConnected()){//avoid to reconnect this device
                    mAntPlusManager.setMostRecent(profile, deviceId);
                    mBSManager.handleReset(deviceId);
                    mBSManager.setCircumference(((BikeSensor)device).getCircumference()/1000f);
                }
                // Ignore Combined device b/c we already have speed only
                BSCManager.ignoreSpeed(true);
                BWManager.ignoreSpeed(true);
            } else {
                mBSManager.releaseAccess();
                AntPlusSensor sensor = mAntPlusManager.getMostRecent(AntPlusProfileManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE);
                if(sensor!= null && sensor.isConnected()){ // if spd/cad is connected, then use spd/cad data
                    BSCManager.ignoreSpeed(false);
                }else{
                    BWManager.ignoreSpeed(false);
                }
                mBSManager.broadcastDisconnectedMessage(deviceId, 0);
            }
        }else if(BSCManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE == profile){ // bike speed + cadence profile
            if (connect) {
                if(device != null && !device.isConnected()){//avoid to reconnect this device
                    mAntPlusManager.setMostRecent(profile, deviceId);
                    mBSCManager.setCircumference(((BikeSensor)device).getCircumference()/1000f);
                    mBSCManager.handleReset(deviceId);
                    BWManager.ignoreSpeed(true);
                    BWManager.ignoreCadence(true);
               }
            } else {
                mBSCManager.releaseAccess();
                AntPlusSensor sensor = mAntPlusManager.getMostRecent(AntPlusProfileManager.EXTRA_BIKE_SPEED_PROFILE);
                if(sensor== null || !sensor.isConnected()){ // only if spd is disconnected
                    BWManager.ignoreSpeed(false);
                }
                sensor = mAntPlusManager.getMostRecent(AntPlusProfileManager.EXTRA_BIKE_CADENCE_PROFILE);
                if(sensor== null || !sensor.isConnected()){ // only if cad is disconnected
                    BWManager.ignoreCadence(false);
                }
                mBSCManager.broadcastDisconnectedMessage(deviceId, 0);
            }
        }else if(BWManager.EXTRA_BIKE_POWER_PROFILE == profile){ // bike power profile
            if (connect) {
                if(device != null && !device.isConnected()){//avoid to reconnect this device
                    mAntPlusManager.setMostRecent(profile, deviceId);
                    mBWManager.setCircumference(((PowerSensor)device).getCircumference()/1000f);
                    mBWManager.handleReset(deviceId);
                }
            } else {
                mBWManager.releaseAccess();
                mBWManager.broadcastDisconnectedMessage(deviceId, 0);
            }
        }
    }
    
    // ant plus just allow one device connected for one profile, so
    // disconnect all of the devices under this profile except itself and broadcast message out before connect to a new device
    private void disconnect(int profile, int deviceId){
        List<AntPlusSensor> devices = mAntPlusManager.getConnectedDevices(profile);
        for(AntPlusSensor device : devices){
            if(device.getDeviceNumber() != deviceId)
                connectOrDisconnect(profile, device.getDeviceNumber(), false);
        }
    }
    
    @Override
    public void requestAccessToPcc() {
        // Nothing for now
        return;
    }

    @Override
    public Context getContext() {
        return this;
    }
}
