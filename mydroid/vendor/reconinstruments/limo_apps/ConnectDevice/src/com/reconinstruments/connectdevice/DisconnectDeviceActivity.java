package com.reconinstruments.connectdevice;

import java.lang.reflect.Method;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.TextView;

import com.reconinstruments.connectdevice.ConnectionActivity.DeviceType;
import com.reconinstruments.connectdevice.ios.BtNotificationThirdActivity;
import com.reconinstruments.connectdevice.ios.FirstConnectActivity;
import com.reconinstruments.connectdevice.ios.MfiReconnectActivity;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.utils.DeviceUtils;

//JIRA: MODLIVE-772 Implement bluetooth connection wizard on MODLIVE
public class DisconnectDeviceActivity extends ConnectionActivity {
	protected static final String TAG = "DisconnectDeviceActivity";
	private ProgressDialog progressDialog;
	@Override
	protected void onCreate(Bundle arg0) {
	    // TODO: there should be no diffrence between MODLIVE and JET here.
		super.onCreate(arg0);
		if(DeviceUtils.isLimo()){
		    // The logic being you are already connected if hud service thinks you are not means you are iOS
			if (BTPropertyReader.getBTConnectionState(this) == 2) { 
				showConnected(false);
			} else {
				showConnected(true);
			}
		}else{
			if (BTPropertyReader.getBTConnectionState(this) == 2) {
				Log.d(TAG, "BTPropertyReader.getBTConnectedDeviceType(this)=" + BTPropertyReader.getBTConnectedDeviceType(this));
				if (BTPropertyReader.getBTConnectedDeviceType(this) == 0) {
					showConnected(false);
				}else{
					showConnected(true);
				}
			}
		}

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(DeviceUtils.isLimo()){
			initService_phone();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(DeviceUtils.isLimo()){
			releaseService_phone();
		}
	}

	private void showConnected(final boolean ios) {
	    this.setContentView(R.layout.activity_disconnect_jet);
		Log.d(TAG, "onServiceDisconnected");
		TextView titleTV = (TextView) findViewById(R.id.title);
		titleTV.setText(ios ? "DISCONNECT CURRENT DEVICE" : "DISCONNECT CURRENT DEVICE");
		String deviceName = BTPropertyReader.getBTConnectedDeviceName(this);
		if(Build.PRODUCT.contains("limo")){
			deviceName = PreferencesUtils.getLastPairedDeviceName(this);
		}
		TextView textTV = (TextView) findViewById(R.id.activity_disconnected_text);
		textTV.setText(Html.fromHtml("You are about to be disconnected from <b>"
					     + deviceName
					     + "</b>. What would you like to do?"));
		View disconnectItem = findViewById(R.id.disconnect);// Pair different device
		View cancelItem = findViewById(R.id.cancel); // Disconnect and continue

		cancelItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
			    if(DeviceUtils.isLimo() && ios){
				    DisconnectDeviceActivity.this.disconnectMap();
				    DisconnectDeviceActivity.this.disconnectHfp();
				    Intent theIntent = new Intent()
					.setAction("private_ble_command");
				    theIntent.putExtra("command", 2);
				    sendBroadcast(theIntent);
				    ConnectionActivity.hfpStatus = 0;
				    ConnectionActivity.mapStatus = 0;
				    if(hudService != null){
				    	DisconnectDeviceActivity.this.disconnect(DeviceType.IOS);
				    }
					forgetDevice();
						
				}else{ // Either limo android or jet
					int deviceType = BTPropertyReader.getBTConnectedDeviceType(DisconnectDeviceActivity.this);
					if(deviceType == 0){ // android device
						if(hudService != null){
							DisconnectDeviceActivity.this.disconnect(DeviceType.ANDROID);
						}
						if(Build.PRODUCT.contains("limo")){
							forgetDevice();
						}
					}else{
						//TODO do something extra for ios
						if(hudService != null){
							DisconnectDeviceActivity.this.disconnect(DeviceType.IOS);
						}
					}
				}
				finish();
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

		disconnectItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
			    if(DeviceUtils.isLimo()&&ios){
				    DisconnectDeviceActivity.this.disconnectMap();
				    DisconnectDeviceActivity.this.disconnectHfp();
				    Intent theIntent = new Intent()
					.setAction("private_ble_command");
				    theIntent.putExtra("command", 2);
				    sendBroadcast(theIntent);
				    ConnectionActivity.hfpStatus = 0;
				    ConnectionActivity.mapStatus = 0;
				    if(hudService != null){
				    	DisconnectDeviceActivity.this.disconnect(DeviceType.IOS);
				    }
					forgetDevice();
				    DisconnectDeviceActivity.this.finish();
				}else{
					progressDialog = new ProgressDialog(DisconnectDeviceActivity.this);
					progressDialog.setIndeterminate(true);
					progressDialog.setCancelable(false);
					progressDialog.show();
					progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
					TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
					textTv.setText("Unpairing, please wait...");
					int deviceType = BTPropertyReader.getBTConnectedDeviceType(DisconnectDeviceActivity.this);
					if(deviceType == 0){ // android device
						if(hudService != null){
							DisconnectDeviceActivity.this.disconnect(DeviceType.ANDROID);
						}
					}else{
						//TODO do something extra for ios
						if(hudService != null){
							DisconnectDeviceActivity.this.disconnect(DeviceType.IOS);
						}
					}
					new CountDownTimer(3 * 1000, 1000) {
						public void onTick(long millisUntilFinished) {
						}
						public void onFinish() {
							try{
								if (progressDialog != null && progressDialog.isShowing()) {
									progressDialog.dismiss();
								}
							}catch(IllegalArgumentException e){
								//do nothing since the activity has been finished.
							}
							unpairDevice();
							forgetDevice();
							startActivity(new Intent(DisconnectDeviceActivity.this,ChooseDeviceActivity.class)
							.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY));
							DisconnectDeviceActivity.this.finish();
						}
					}.start();
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
		
		cancelItem.requestFocus();
	}
	
    private void forgetDevice(){
		PreferencesUtils.setLastPairedDeviceName(DisconnectDeviceActivity.this, "");
		PreferencesUtils.setBTDeviceName(DisconnectDeviceActivity.this, "");
		PreferencesUtils.setDeviceAddress(DisconnectDeviceActivity.this, "");
		PreferencesUtils.setDeviceName(DisconnectDeviceActivity.this, "");
		PreferencesUtils.setLastPairedDeviceAddress(DisconnectDeviceActivity.this, "");
		PreferencesUtils.setLastPairedDeviceType(DisconnectDeviceActivity.this, 0);
		PreferencesUtils.setReconnect(DisconnectDeviceActivity.this, false);
	}


}
// End of JIRA: MODLIVE-772