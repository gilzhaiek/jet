package com.dsi.ant.antplus.pluginsampler.antservice;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataTimestampReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IPage4AddtDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.ICumulativeOperatingTimeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IManufacturerAndSerialReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IVersionAndModelReceiver;
import android.util.Log;
public class AntService extends Service implements IAntContext {
    private static final String TAG = "AntService";
    public HRManager mHRManager;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate(){
	Log.v(TAG,"onCreate()");
	// TODO If not ANT mode just return
	mHRManager = new HRManager(this);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startid){
	Log.v(TAG,"onStartCommand");
	Bundle b = intent.getExtras();
	if (b != null) {
	    int profile = b.getInt("profile",0);
	    int deviceId = b.getInt("deviceId",0);
	    if(0 == profile){ // heart rate profile
	        boolean connect = b.getBoolean("connect"); // true: connect, false: disconnect
	        if(connect){
	            mHRManager.handleReset(deviceId);
	        }else{
	            mHRManager.releaseAccess();
	            Intent i = new Intent("heart_rate_device_disconnected");
	            i.putExtra("profile", 0);
	            i.putExtra("deviceId", deviceId);
	            sendBroadcast(i);
	        }
	    }
	}
	else {
	    mHRManager.handleReset(1); // FIXME 
	}
        return START_STICKY;
    }
    @Override
    public void requestAccessToPcc(){
	// Nothing for now
	return;
    }
    @Override
    public Context getContext() {
	return this;
    }
}