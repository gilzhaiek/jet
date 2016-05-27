package com.reconinstruments.dashlivestats;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.reconinstruments.bletest.IBLEService;
import com.reconinstruments.utils.DeviceUtils;

public class DashLauncherApp extends Application
{
	private final static String TAG = "DashLauncherApp";
	public IBLEService bleService;
	private BLEServiceConnection bleServiceConnection;

    
 
	
	public static DashLauncherApp instance;
	@Override
	public void onCreate()
	{
		instance = this;
		// we only need establish this connection if we are on modlive
		if (DeviceUtils.isLimo()) {
		    initBLEService();
		}
	}
	
	
	
	@Override
	public void onTerminate()
	{
		super.onTerminate();
	}



	/** Static access to a singleton application context */
	public static DashLauncherApp getInstance() {
		return instance;
	}
	public boolean isIPhoneConnected() {
		try{
			return bleService!=null&&bleService.isConnected()&&!bleService.getIsMaster();
		} catch (RemoteException e){
			e.printStackTrace();
			Log.d(TAG, "Remote Exception checking iPhone connected");
			return false;
		}
	}
	void initBLEService() {
		if( bleServiceConnection == null ) {
			bleServiceConnection = new BLEServiceConnection();
			Intent i = new Intent("RECON_BLE_TEST_SERVICE");
			bindService( i, bleServiceConnection, Context.BIND_AUTO_CREATE);
			Log.d( TAG, "bindService()" );
		} 
	}

	void releaseBLEService() {
		if( bleServiceConnection != null ) {
			unbindService( bleServiceConnection );	  
			bleServiceConnection = null;
			Log.d( TAG, "unbindService()" );
		}
	}

	class BLEServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className, 
				IBinder boundService ) {
			bleService = IBLEService.Stub.asInterface((IBinder)boundService);
			Log.d(TAG,"onServiceConnected" );
		}

		public void onServiceDisconnected(ComponentName className) {
			bleService = null;
			Log.d( TAG,"onServiceDisconnected" );
		}
	};
}
