package com.reconinstruments.phone.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.bletest.IBLEService;

public class BLEServiceConnectionManager {
    public static final String  TAG = "BLEServiceConnectionManager";
    Context mContext;//A copy of application context in helper class
    public BLEServiceConnectionManager(Context c) {
	mContext = c;
    }
    	//////////////////////////////////////////////////////
	// aidl service connection.
	/////////////////////////////////////////////////////
	private IBLEService bleService;
	private BleServiceConnection bleServiceConnection;

	public void initService() {
		if (bleServiceConnection == null) {
			bleServiceConnection = new BleServiceConnection();
			Intent i = new Intent("RECON_BLE_TEST_SERVICE");
			mContext.bindService(i, bleServiceConnection, Context.BIND_AUTO_CREATE);
		}
	}

	public void releaseService() {
	    //Do any cleanup that is needed beforehan
	    
	    //unbind:
	    if (bleServiceConnection != null){
		mContext.unbindService(bleServiceConnection);
		bleServiceConnection = null;
		Log.d(TAG, "unbindService()");
	    }
	}

	class BleServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className, IBinder boundService) {
			Log.d(TAG, "onServiceConnected");
			bleService = IBLEService.Stub.asInterface((IBinder) boundService);
			//Do anything that needs to be done on connection
		}
		public void onServiceDisconnected(ComponentName className){ 
			bleService = null;
			Log.d(TAG, "onServiceDisconnected");
		}
	};
	//////////////////// End of aidl shit///////////////////////

    public boolean isiOSMode() {
	try	{
	    if (bleService != null) {
		return !bleService.getIsMaster();
	    } else {
		return false;
	    }
	}
	catch (RemoteException e)	{
	    e.printStackTrace();
	}
	return false;
    }
}