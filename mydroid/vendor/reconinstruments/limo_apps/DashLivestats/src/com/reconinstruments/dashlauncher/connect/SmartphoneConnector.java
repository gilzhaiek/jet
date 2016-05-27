package com.reconinstruments.dashlauncher.connect;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.TransitionDrawable;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.reconinstruments.connect.apps.ConnectHelper;
import com.reconinstruments.dashlivestats.DashLauncherApp;
import com.reconinstruments.hudservice.helper.HUDConnectivityHelper;
import com.reconinstruments.utils.BTHelper;

/** This object should be initialised in an activitys onPostCreate() method 
 * and onDestroy called in the activities onDestroy()
 *
 * the activity must implement SmartphoneInterface */
public class SmartphoneConnector
{
	static final String TAG = "SmartphoneConnector";

	FrameLayout mainView;
	View overlay;

	int CONNECT_REQUEST_CODE = 0;
	int CONNECT_SUCCESS_CODE = 44;

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
	ConnectionState currentState;
	SmartphoneInterface context;
	public Activity getActivity(){
		return (Activity)context;
	}

	public SmartphoneConnector(SmartphoneInterface context){
		this.context = context;
		this.mainView = (FrameLayout) getActivity().getWindow().getDecorView().findViewById(android.R.id.content);

        getActivity().registerReceiver(phoneConnectionReceiver, new IntentFilter(ConnectHelper.MSG_STATE_UPDATED));

		currentState = new ConnectionState(isConnected(),lastDevice());

		if(currentState.connected)
			onConnect();
		else
			onDisconnect();
	}
	public boolean isConnected(){
		return (BTHelper.isConnected(getActivity())||DashLauncherApp.getInstance().isIPhoneConnected());
	}
	public static DeviceType lastDevice(){

		//if(true) return DeviceType.NONE;

		String lastDevice = Settings.System.getString(DashLauncherApp.instance.getContentResolver(), "LastDeviceConnected");

		//Log.d(TAG,"lastDevice: "+lastDevice);

		return lastDevice==null?DeviceType.NONE:(lastDevice.equals("Android")?DeviceType.ANDROID:DeviceType.IOS);
	}
	public void onDestroy()
	{
		getActivity().unregisterReceiver(phoneConnectionReceiver);
	}

	private void updateConnectionState(ConnectionState state)
	{
		if(state.connected!=currentState.connected){
			currentState = state;
			if(currentState.connected)
				onConnect();
			else
				onDisconnect();
		}
	}
	/** called only after being disconnected */
	private void onConnect(){
		hideOverlay();
		context.onConnect();
	}
	private void hideOverlay(){
		if(overlay!=null){
			mainView.removeView(overlay);
			overlay = null;
		}
    }
	private void onDisconnect()
	{
		switch(currentState.lastDeviceType){
		case NONE:
			showNewConnectOverlay();
			break;
		case ANDROID:
			showAndroidOverlay();
			break;
		case IOS:
			if(context.requiresAndroid())
				showNewConnectOverlay();
			else
				showIOSOverlay();
			break;
		}
		context.onDisconnect();
	}

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
		}
	};
	public void showAndroidOverlay(){
		hideOverlay();
		overlay = context.getAndroidOverlay();
		mainView.addView(overlay, new LinearLayout.LayoutParams(mainView.getLayoutParams().width, mainView.getLayoutParams().height));
	}
	private void showIOSOverlay(){
		hideOverlay();
		overlay = context.getIOSOverlay();
		mainView.addView(overlay, new LinearLayout.LayoutParams(mainView.getLayoutParams().width, mainView.getLayoutParams().height));
		View connectButton = context.getIOSConnectButton(overlay);
		connectButton.setOnClickListener(new OnClickListener(){
			public void onClick(View v)
			{
				getActivity().startActivityForResult(new Intent("com.reconinstruments.connectdevice.CONNECT_IOS"),CONNECT_REQUEST_CODE);
			}
		});
	}
	private void showNewConnectOverlay(){
		hideOverlay();
		overlay = context.getNoConnectOverlay();
		mainView.addView(overlay, new LinearLayout.LayoutParams(mainView.getLayoutParams().width, mainView.getLayoutParams().height));

		View setupItem = context.getNoConnectSetupButton(overlay);
		setupItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Settings.System.putString(getActivity().getContentResolver(), "DisableSmartphone", "false");
				getActivity().startActivityForResult(new Intent("com.reconinstruments.connectdevice.CONNECT"),CONNECT_REQUEST_CODE);
			}
		});
		setupItem.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v.getBackground();
				if(hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});
		setupItem.setSelected(true);
		setupItem.requestFocus();

		View noshowItem = context.getNoConnectNoShowButton(overlay);
		if(noshowItem!=null){
			noshowItem.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Settings.System.putString(getActivity().getContentResolver(), "DisableSmartphone", "true");
					getActivity().finish();
				}
			});
			noshowItem.setOnFocusChangeListener(new OnFocusChangeListener() {
				public void onFocusChange(View v, boolean hasFocus) {
					TransitionDrawable transition = (TransitionDrawable) v.getBackground();
					if(hasFocus) {
						transition.startTransition(300);
					} else {
						transition.resetTransition();
					}
				}
			});
		}
	}
}
