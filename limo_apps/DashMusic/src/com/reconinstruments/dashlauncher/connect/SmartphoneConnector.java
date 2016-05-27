package com.reconinstruments.dashlauncher.connect;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.bletest.IBLEService;
import com.reconinstruments.connect.apps.ConnectHelper;
import com.reconinstruments.dashmusic.DashLauncherApp;
import com.reconinstruments.hudservice.IHUDService;
import com.reconinstruments.dashlauncher.music.MusicHelper;

/** This object should be initialised in an activitys onPostCreate() method 
 * and onDestroy called in the activities onDestroy()
 * 
 * the activity must implement SmartphoneInterface */
public class SmartphoneConnector 
{
	static final String TAG = "SmartphoneConnector";

	public IBLEService bleService;
	private BLEServiceConnection bleServiceConnection;

	public IHUDService mIHUDService;
	private HUDServiceConnection mIHUDServiceConnection;
    
    
	FrameLayout mainView;
	public View overlay;

	public int CONNECT_REQUEST_CODE = 0;
	public int CONNECT_SUCCESS_CODE = 44;

	public enum DeviceType{
		NONE,ANDROID,IOS
	}
	public static class ConnectionState{
		public ConnectionState(boolean connected, DeviceType lastDeviceType)
		{
			this.connected = connected;
			this.lastDeviceType = lastDeviceType;
		}
		public boolean connected;
		public DeviceType lastDeviceType;
	}
	public ConnectionState currentState;
	SmartphoneInterface context;
	public Activity getActivity(){
		return (Activity)context;
	}

	public SmartphoneConnector(SmartphoneInterface context){
		this.context = context;
		this.mainView = (FrameLayout) getActivity().getWindow().getDecorView().findViewById(android.R.id.content);

		getActivity().registerReceiver(phoneConnectionReceiver, new IntentFilter(ConnectHelper.MSG_STATE_UPDATED));
		getActivity().registerReceiver(phoneConnectionReceiver, new IntentFilter("HUD_STATE_CHANGED"));

		initBLEService();
		initHUDService();
		
	}

	public boolean isConnected(){
	    MusicHelper.logFunctionName(TAG);

		if (mIHUDService==null || bleService== null){
			return false;
		}
		boolean androidConnected = false;
		boolean oldIOSConnected = false;
		try {
			androidConnected = mIHUDService.getConnectionState()==2;
			oldIOSConnected = isIPhoneConnected(bleService);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (androidConnected||oldIOSConnected);

		//return (BTCommon.isConnected(getActivity())||DashLauncherApp.getInstance().isIPhoneConnected());
	}

	public boolean isIPhoneConnected(IBLEService bleService) {
	    MusicHelper.logFunctionName(TAG);

		try{
			return bleService!=null&&bleService.isConnected()&&!bleService.getIsMaster();
		} catch (RemoteException e){
			e.printStackTrace();
			Log.d(TAG, "Remote Exception checking iPhone connected");
			return false;
		}
	}
	public static DeviceType lastDevice(){
	    MusicHelper.logFunctionName(TAG);
			    

		//if(true) return DeviceType.NONE;

		String lastDevice = Settings.System.getString(DashLauncherApp.instance.getContentResolver(), "LastDeviceConnected");

		//Log.d(TAG,"lastDevice: "+lastDevice);

		return lastDevice==null?DeviceType.NONE:(lastDevice.equals("Android")?DeviceType.ANDROID:DeviceType.IOS);
	}
	public void onDestroy()
	{
		releaseHUDService();
		releaseBLEService();
		getActivity().unregisterReceiver(phoneConnectionReceiver);
	}

	private void updateConnectionState(ConnectionState state)
	{
	    MusicHelper.logFunctionName(TAG);

		if(currentState!=null){
			if(state.connected!=currentState.connected){
				currentState = state;
				if(currentState.connected)
					onConnect();
				else
					onDisconnect();
			}
		}else{
			currentState = state;
			if(currentState.connected)
				onConnect();
			else
				onDisconnect();
		}
	}
	/** called only after being disconnected */
	private void onConnect(){
	    	    	    MusicHelper.logFunctionName(TAG);

		hideOverlay();
		context.onConnect();
	}
	public void hideOverlay(){
	    	    	    MusicHelper.logFunctionName(TAG);

	    MusicHelper.logFunctionName(TAG);
		if(overlay!=null){
		    Log.v(TAG,"overlay is not null");
			mainView.removeView(overlay);
			overlay = null;
		} else {
		    Log.v(TAG,"overlay is null");
		}
	}
	public void onDisconnect()
	{	
		showNewConnectOverlay();
		context.onDisconnect();
	}

    // This broadcast receiver is for connection state chagen messagses
	BroadcastReceiver phoneConnectionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ConnectHelper.MSG_STATE_UPDATED)) 
			{
				boolean connected = intent.getBooleanExtra("connected", false);
				Log.d(TAG,"connected "+connected);
				DeviceType type = DeviceType.ANDROID;
				if(intent.hasExtra("device")){
					Log.d(TAG,"device: "+intent.getStringExtra("device"));
					type = intent.getStringExtra("device").equalsIgnoreCase("ios")?DeviceType.IOS:DeviceType.ANDROID;
				}
				DeviceType lastType = lastDevice();
				// hack, this will ignore disconnect messages from a device that isn't connected
				if(lastType==type||connected==true){
					ConnectionState state = new ConnectionState(connected,type);
					updateConnectionState(state);
				}
			}
			else if(intent.getAction().equals("HUD_STATE_CHANGED")){
				boolean connected = intent.getExtras().getInt("state")==2;
				Log.d(TAG,"connected "+connected);
				DeviceType type = DeviceType.ANDROID;
				ConnectionState state = new ConnectionState(connected,type);
				updateConnectionState(state);
			}
		}
	};
	
	public void showNewConnectOverlay(){
	    MusicHelper.logFunctionName(TAG);
		hideOverlay();
		overlay = context.getNoConnectOverlay();
		mainView.addView(overlay, new LinearLayout.LayoutParams(mainView.getLayoutParams().width, mainView.getLayoutParams().height));

		View setupItem = context.getNoConnectSetupButton(overlay);
		setupItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				switch(currentState.lastDeviceType){
				case IOS:
					getActivity().startActivityForResult(new Intent("com.reconinstruments.connectdevice.CONNECT_IOS"),CONNECT_REQUEST_CODE);
					break;
				default:
					Settings.System.putString(getActivity().getContentResolver(), "DisableSmartphone", "false");
					getActivity().startActivityForResult(new Intent("com.reconinstruments.connectdevice.CONNECT"),CONNECT_REQUEST_CODE);
					break;
				}
			}
		});
	}

	class BLEServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className, 
				IBinder boundService ) {
			bleService = IBLEService.Stub.asInterface((IBinder)boundService);
			Log.d(TAG,"onServiceConnected" );
			if (mIHUDService != null) {
				Log.d(TAG,"setting current state");
				setupCurrentState();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			bleService = null;
			Log.d( TAG,"onServiceDisconnected" );
		}
	};

	class HUDServiceConnection implements ServiceConnection{
		// Called when the connection with the service is established
		public void onServiceConnected(ComponentName className, IBinder service) {
			// Following the example above for an AIDL interface,
			// this gets an instance of the IRemoteInterface, which we can use to call on the service
			mIHUDService = IHUDService.Stub.asInterface((IBinder)service);
			Log.d(TAG,"ihudservice onServiceConnected" );

			if (bleService != null) {
				Log.d(TAG,"setting current state");
				setupCurrentState();
			}
		}



		// Called when the connection with the service disconnects unexpectedly
		public void onServiceDisconnected(ComponentName className) {
			Log.e(TAG, "IHUDService has unexpectedly disconnected");
			mIHUDService = null;
		}
	}
	private void setupCurrentState() {
	    	    	    MusicHelper.logFunctionName(TAG);

		currentState = new ConnectionState(isConnected(),lastDevice());
		Log.d(TAG,"last connected device was: "+lastDevice().name());
		if(currentState.connected)
			onConnect();
		else
			onDisconnect();
	}

	void initHUDService(){
		if( mIHUDServiceConnection == null ) {
			mIHUDServiceConnection = new HUDServiceConnection();
			Intent i = new Intent("RECON_HUD_SERVICE");
			boolean b = getActivity().bindService( i, mIHUDServiceConnection, Context.BIND_AUTO_CREATE);
			Log.d( TAG, "hud service bindService() result: "+b );
		} 
	}

	void initBLEService() {
		if( bleServiceConnection == null ) {
			bleServiceConnection = new BLEServiceConnection();
			Intent i = new Intent("RECON_BLE_TEST_SERVICE");
			getActivity().bindService( i, bleServiceConnection, Context.BIND_AUTO_CREATE);
			Log.d( TAG, "bindService()" );
		} 
	}
	void releaseHUDService() {
		if( mIHUDServiceConnection != null ) {
			getActivity().unbindService( mIHUDServiceConnection );	  
			mIHUDServiceConnection = null;
			Log.d( TAG, "unbindService()" );
		}
	}
	void releaseBLEService() {
		if( bleServiceConnection != null ) {
			getActivity().unbindService( bleServiceConnection );	  
			bleServiceConnection = null;
			Log.d( TAG, "unbindService()" );
		}
	}

}
