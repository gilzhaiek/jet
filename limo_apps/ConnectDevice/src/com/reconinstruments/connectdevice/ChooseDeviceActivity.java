package com.reconinstruments.connectdevice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.TextView;

import com.reconinstruments.bletest.IBLEService;
import com.reconinstruments.connectdevice.ios.BtNotificationFivthActivity;
import com.reconinstruments.connectdevice.ios.BtNotificationFristActivity;
import com.reconinstruments.connectdevice.ios.BtReconnectActivity;
import com.reconinstruments.connectdevice.ios.MfiReconnectActivity;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.commonwidgets.TwoOptionsJumpFixer;
import com.reconinstruments.utils.DeviceUtils;

public class ChooseDeviceActivity extends ConnectionActivity {
	protected static final String TAG = "ChooseDeviceActivity";
	private View androidItem, iphoneItem;
	private TwoOptionsJumpFixer chooseDeviceFixer;
	private TwoOptionsJumpFixer connectedDeviceFixer;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		
		if(Build.PRODUCT.contains("limo")){
			if (BTPropertyReader.getBTConnectionState(this) == 2) {
				showConnected(false);
			} else {
				if(!"".equals(PreferencesUtils.getLastPairedDeviceName(this)) && (PreferencesUtils.getLastPairedDeviceType(this) == 0)){
					startReconnect();
				}else{
					initService();
				}
			}
		}else{
			if (BTPropertyReader.getBTConnectionState(this) == 2) {
				Log.d(TAG, "BTPropertyReader.getBTConnectedDeviceType(this)=" + BTPropertyReader.getBTConnectedDeviceType(this));
				if (BTPropertyReader.getBTConnectedDeviceType(this) == 0) {
					showConnected(false);
				}else{
					showConnected(true);
				}

			} else {
				if((BTPropertyReader.getBTConnectionState(this) == 1) || !"".equals(PreferencesUtils.getLastPairedDeviceName(this))){
					startReconnect();
				}else{
					showChooseDevice();
				}
			}
		}		
	}

	private void startReconnect(){
			if((BTPropertyReader.getBTConnectionState(this) == 1) && (PreferencesUtils.getLastPairedDeviceType(this) == 1)){
				startActivity(new Intent(this, BtNotificationFivthActivity.class)
				.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_NEW_TASK));
			}else{
				startActivity(new Intent(this, MfiReconnectActivity.class)
				.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_NEW_TASK));
			}
			finish();
	}
	
	// ////////////////////////////////////////////////////
	// aidl service connection.
	// ///////////////////////////////////////////////////
	private IBLEService bleService;
	private BLEServiceConnection bleServiceConnection;

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(chooseDeviceFixer != null){
			chooseDeviceFixer.stop();
		}
		if(connectedDeviceFixer != null){
			connectedDeviceFixer.stop();
		}
		releaseService();
	}

	void initService() {
	    if(DeviceUtils.isLimo()){
			if (bleServiceConnection == null) {
				bleServiceConnection = new BLEServiceConnection();
				Intent i = new Intent("RECON_BLE_TEST_SERVICE");
				bindService(i, bleServiceConnection, Context.BIND_AUTO_CREATE);
				Log.d(TAG, "bindService()");
			}
		}
	}

	void releaseService() {
	    if(DeviceUtils.isLimo()){
			if (bleServiceConnection != null) {
				unbindService(bleServiceConnection);
				bleServiceConnection = null;
				Log.d(TAG, "unbindService()");
			}
		}
	}

	class BLEServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			bleService = IBLEService.Stub.asInterface((IBinder) boundService);
			Log.d(TAG, "onServiceConnected");
			try {
				if (bleService != null) {
					serviceConnected(bleService);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			bleService = null;
			Log.d(TAG, "onServiceDisconnected");
		}
	};

	public void serviceConnected(IBLEService bleService) throws RemoteException {

		boolean isConnected = !bleService.getIsMaster()
				&& bleService.isConnected();
		if (!isConnected) {
			if(!"".equals(PreferencesUtils.getLastPairedDeviceName(this))){
				startReconnect();
			}else{
				showChooseDevice();
			}
		} else {
			showConnected(true);
		}
	}

	public void showChooseDevice() {
	    this.setContentView(R.layout.activity_choose_device_jet);
		androidItem = findViewById(R.id.android_item);
		iphoneItem = findViewById(R.id.iphone_item);
		Settings.System.putString(getContentResolver(), "DisableSmartphone",
				"false");

		androidItem.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				iphoneItem.setFocusable(false);
				androidItem.setFocusable(false);
				startActivity(new Intent(
						ChooseDeviceActivity.this,
						com.reconinstruments.connectdevice.android.FirstConnectActivity.class));
				finish();
			}
		});

		androidItem.setOnFocusChangeListener(new OnFocusChangeListener() {

			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v
						.getBackground();
				if (hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});

		iphoneItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				iphoneItem.setFocusable(false);
				androidItem.setFocusable(false);
				if(DeviceUtils.isLimo()){
					// TODO: add remote version logic
					int remoteControlVersionNumber = 1;
					try {
						remoteControlVersionNumber = bleService
								.getRemoteControlVersionNumber();
						Log.v(TAG, "remote Version is "
								+ remoteControlVersionNumber);
					} catch (RemoteException e) {
						e.printStackTrace();
					}

					if (remoteControlVersionNumber < 1) {

						startActivity(new Intent(
								ChooseDeviceActivity.this,
								com.reconinstruments.connectdevice.ios.RemoteWarningActivity.class));
					} else {
						startActivity(new Intent(
								ChooseDeviceActivity.this,
								com.reconinstruments.connectdevice.ios.FirstConnectActivity.class));
					}
				}else{
					PreferencesUtils.setReconnect(ChooseDeviceActivity.this, false);
					startActivity(new Intent(
							ChooseDeviceActivity.this,
							com.reconinstruments.connectdevice.ios.FirstConnectActivity.class));
				}
				finish();
			}
		});

		iphoneItem.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v
						.getBackground();
				if (hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});
		chooseDeviceFixer = new TwoOptionsJumpFixer(androidItem, iphoneItem);
		chooseDeviceFixer.start();
	}

	private void showConnected(final boolean ios) {
	    this.setContentView(R.layout.activity_already_connected_jet);

		Log.d(TAG, "showConnected");
		TextView titleTV = (TextView) findViewById(R.id.title);
		titleTV.setText(ios ? "PAIRED WITH IPHONE" : "PAIRED WITH ANDROID");
		TextView textTV = (TextView) findViewById(R.id.activity_already_connected_text);
        TextView textSecondTV = (TextView) findViewById(R.id.activity_already_connected_second_text);
		String deviceName = getDeviceName();
		textTV.setText(Html.fromHtml("You are currently paired with <b>" + deviceName + "</b>"));
        textSecondTV.setText("Would you like to continue using this device?");
		final View disconnectItem = findViewById(R.id.disconnect);
		final View cancelItem = findViewById(R.id.cancel);

		disconnectItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				disconnectItem.setFocusable(false);
				cancelItem.setFocusable(false);
				startActivity(new Intent(ChooseDeviceActivity.this,
						DisconnectDeviceActivity.class)
				.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				ChooseDeviceActivity.this.finish();
			}
		});

		disconnectItem.setOnFocusChangeListener(new OnFocusChangeListener() {

			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v
						.getBackground();
				if (hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});

		cancelItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				disconnectItem.setFocusable(false);
				cancelItem.setFocusable(false);
				if(DeviceUtils.isLimo() && ios){
					ChooseDeviceActivity.this.enableCallAndText();
				}else{
					finish();
				}
			}
		});

		cancelItem.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v
						.getBackground();
				if (hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});
		connectedDeviceFixer = new TwoOptionsJumpFixer(disconnectItem, cancelItem);
		connectedDeviceFixer.start();
	}

	private void enableCallAndText() {
		Log.d(TAG, "mapStatus = " + getMapStatus() + " and hfpStatus = "
				+ getHfpStatus());
		if ((getHfpStatus() == 2)
				&& (getMapStatus() == 2)
				&& (PreferencesUtils.getBTDeviceName(this)
						.equals(PreferencesUtils.getDeviceName(this)))) {
			Log.d(TAG,
					"Skip to enable call and sms on Mod Live for iPhone only.");
			ChooseDeviceActivity.super.sendXmlToiPhone();
			finish();
		} else {
			Log.d(TAG,
					"Go into the next step to enable call and sms on Mod Live for iPhone only.");
			startActivity(new Intent(this, BtNotificationFristActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
							| Intent.FLAG_ACTIVITY_NEW_TASK));
			finish();
		}
	}

    private String getDeviceName() {
	String deviceName;
	if(DeviceUtils.isLimo()){
	    deviceName = PreferencesUtils.getLastPairedDeviceName(this);
	}else{
	     deviceName = BTPropertyReader.getBTConnectedDeviceName(this);
	}
	return deviceName;
    }

}
