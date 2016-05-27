package com.reconinstruments.connectdevice.ios;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spannable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.connectdevice.BTPropertyReader;
import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.PreferencesUtils;
import com.reconinstruments.connectdevice.R;
import com.reconinstruments.utils.DeviceUtils;

//JIRA: MODLIVE-772 Implement bluetooth connection wizard on MODLIVE
public class BtNotificationForthActivity extends ConnectionActivity {
	private static final String TAG = "BtNotificationForthActivity";
	private View continueButton, cancelButton;
	private int step = 0;
	private ProgressDialog progressDialog;
	private CountDownTimer connectingMonitor;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		
		if(DeviceUtils.isLimo()){
			this.setContentView(R.layout.activity_ios_enable_notification);
		}else{
			this.setContentView(R.layout.activity_ios_enable_notification_jet);
		}

		continueButton = (View) findViewById(R.id.continue_button);
		cancelButton = (View) findViewById(R.id.cancel);
		final TextView messageText = (TextView) findViewById(R.id.message_text);
		if(DeviceUtils.isLimo()){

		messageText
				.setText(Html
						.fromHtml(
								"From your <b>iPhone Bluetooth Settings</b>, tap the <img src=\"blue_arrow.png\"> beside your connected MOD Live, then set <b>Show Notifications</b> to <img src=\"on_switch.png\">",
								new ImageGetter() {
									@Override
									public Drawable getDrawable(String source) {
										int id;
										if (source.equals("blue_arrow.png")) {
											id = R.drawable.blue_arrow;
										} else if (source
												.equals("on_switch.png")) {
											id = R.drawable.on_switch;
										} else {
											return null;
										}
										Drawable d = getResources()
												.getDrawable(id);
										d.setBounds(0, 0,
												d.getIntrinsicWidth(),
												d.getIntrinsicHeight());
										return d;
									}
								}, null));
		}else{
			messageText
			.setText
			(Html
					.fromHtml(
							"From your <b>iPhone Bluetooth Settings</b>, tap the&nbsp<img src=\"blue_arrow.png\" align=\"middle\"> beside your connected <b>Snow2</b>, then set <b>Show Notifications</b> to&nbsp<img src=\"on_switch.png\" align=\"middle\">",
							new ImageGetter() {
								@Override
								public Drawable getDrawable(String source) {
									int id;
									if (source.equals("blue_arrow.png")) {
										id = R.drawable.blue_arrow;
									} else if (source
											.equals("on_switch.png")) {
										id = R.drawable.on_switch;
									} else {
										return null;
									}
									LevelListDrawable d = new LevelListDrawable();
									Drawable empty = getResources()
											.getDrawable(id);
									d.addLevel(0, 0, empty);
									d.setBounds(0, 0,
											empty.getIntrinsicWidth(),
											empty.getIntrinsicHeight());
									return d;
								}
							}, null));
			
//			messageText
//			.setText(span);
		}

		continueButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "onClick and step=" + step);
				if(DeviceUtils.isLimo()){
					continueButton.setFocusable(false);
					cancelButton.setFocusable(false);
					BtNotificationForthActivity.this.connectPhone();
				}else{

				    Log.v(TAG,"wait is done and we are finishing");
					enableCallAndTextSS1();

					Intent intent = new Intent(
							BtNotificationForthActivity.this,
							BtNotificationFivthActivity.class)
							.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
									| Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					
					BtNotificationForthActivity.this.finish();
				}
				step++;

			}
		});
		
		continueButton.setOnFocusChangeListener(new OnFocusChangeListener() {

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

		if(DeviceUtils.isLimo()){
			cancelButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					continueButton.setFocusable(false);
					cancelButton.setFocusable(false);
					finish();
				}
			});
			cancelButton.setOnFocusChangeListener(new OnFocusChangeListener() {

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
			continueButton.setSelected(true);
			continueButton.requestFocus();
			try {
				registerReceiver(btTelephonyReceiver, new IntentFilter(
						"com.reconinstruments.BtTelephonyStateChanged"));
			} catch (IllegalArgumentException e) {
				Log.i(TAG, "btTelephonyReceiver is already unregistered");
			}
		}else{
			progressDialog = new ProgressDialog(this);
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.show();
			progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
			TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
			textTv.setText("Connecting, please wait...");
//			new Handler().postDelayed(new Runnable() {
//		        @Override
//		        public void run() {
		        	if (progressDialog != null){
		        		progressDialog.show();
		        	}
//		        }
//		    }, 1000);
			
			new CountDownTimer(5 * 1000, 1000) {
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
			}
			}.start();
			registerReceiver(hudServiceBroadcastReceiver, new IntentFilter(
					"HUD_STATE_CHANGED"));
		}
	}

	private void connectPhone() {
		try {
			registerReceiver(btTelephonyReceiver, new IntentFilter(
					"com.reconinstruments.BtTelephonyStateChanged"));
		} catch (IllegalArgumentException e) {
			Log.i(TAG, "btTelephonyReceiver is already unregistered");
		}

		progressDialog = new ProgressDialog(this);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
		progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
		TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
		textTv.setText("Please wait...");
		BluetoothDevice device = BluetoothAdapter.getDefaultAdapter()
				.getRemoteDevice(PreferencesUtils.getDeviceAddress(this));
		Log.d(TAG, "Trying to to see if 'Show Notification' is enabled.");
		if (/*
			 * connectToHfp(device.getAddress()) &&
			 */connectToMap(device.getAddress())) {
			continueButton.setEnabled(false);
		}
		Log.d(TAG, "connectingMonitor is running");
		connectingMonitor = new CountDownTimer(20 * 1000, 1000) {

			public void onTick(long millisUntilFinished) {

			}

			public void onFinish() {
//				try {
//					BtNotificationForthActivity.this
//							.unregisterReceiver(btTelephonyReceiver);
//				} catch (IllegalArgumentException e) {
//					Log.i(TAG, "btTelephonyReceiver is already unregistered");
//				}
				Log.d(TAG,
						"Fail to disconnect and connect to the phone, ask the user try again.");
				continueButton.setEnabled(true);
				continueButton.setSelected(true);
				continueButton.requestFocus();
				continueButton.setFocusable(true);
				cancelButton.setFocusable(true);
				((TextView)continueButton).setText("Try Again");
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
			}
		}.start();
	}

	
	@Override
	public void onBackPressed() {
	    if(DeviceUtils.isLimo()){
			super.onBackPressed();
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    if(DeviceUtils.isLimo()){
		    return super.onKeyUp(keyCode, event);
		}else{
			if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
			   keyCode == KeyEvent.KEYCODE_ENTER){
			    Log.v(TAG,"wait is done and we are finishing");
			    enableCallAndTextSS1();

			    if(BTPropertyReader.getBTConnectionState(BtNotificationForthActivity.this) == 2){
					Intent intent = new Intent(
							BtNotificationForthActivity.this,
							BtNotificationSixthActivity.class)
							.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
									| Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
			    }else{
					Intent intent = new Intent(
							BtNotificationForthActivity.this,
							BtNotificationFivthActivity.class)
							.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
									| Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
			    }
			
			BtNotificationForthActivity.this.finish();
			return true;
			}
		}
//		if(keyCode == KeyEvent.KEYCODE_BACK){
//			return true;
//		}
		return super.onKeyUp(keyCode, event);
	}

	private void sendNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon,
				"iPhone Setup Complete", System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, BtNotificationForthActivity.class), 0);
		notification.setLatestEventInfo(this, "iPhone",
				"iPhone Setup Complete", contentIntent);
		notificationManager.notify(654654, notification);
		notificationManager.cancel(654654);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(DeviceUtils.isLimo()){
			initService_ble();
			initService_phone();
		}else{
			
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(DeviceUtils.isLimo()){
			releaseService_phone();
			releaseService_ble();
			if (progressDialog != null) {
				progressDialog.dismiss();
				progressDialog = null;
			}
			if (connectingMonitor != null) {
				connectingMonitor.cancel();
			}
			Log.d(TAG, "unregisterReceiver for btTelephonyReceiver");
			try {
				this.unregisterReceiver(btTelephonyReceiver);
			} catch (IllegalArgumentException e) {
				Log.i(TAG, "btTelephonyReceiver is already unregistered");
			}
		}else{
			if (progressDialog != null) {
				progressDialog.dismiss();
				progressDialog = null;
			}
			if (connectingMonitor != null) {
				connectingMonitor.cancel();
			}
			try{
				Log.d(TAG, "unregisterReceiver hudServiceBroadcastReceiver");
				unregisterReceiver(hudServiceBroadcastReceiver);
			}catch(IllegalArgumentException e){
				//ignore
			}
		}
	}

	private int priorState = 0;
	private BroadcastReceiver btTelephonyReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (step != 0) {
				// state 2 connected, 1 connecting, 0 disconnected
				// int hfpstate = intent.getIntExtra("HfpState", 0);
				int mapstate = intent.getIntExtra("MapState", 0);
				Log.d(TAG, " mapstate: " + mapstate);
				BtNotificationForthActivity.mapStatus = mapstate;
				BtNotificationForthActivity.super.sendXmlToiPhone();
				// BtNotificationForthActivity.hfpStatus = hfpstate;

				if (mapstate == 2 && (priorState != 0)) {
//					try {
//						BtNotificationForthActivity.this
//								.unregisterReceiver(btTelephonyReceiver);
//					} catch (IllegalArgumentException e) {
//						Log.i(TAG,
//								"btTelephonyReceiver is already unregistered");
//					}
					BluetoothDevice device = BluetoothAdapter.getDefaultAdapter()
							.getRemoteDevice(PreferencesUtils.getDeviceAddress(BtNotificationForthActivity.this));
					PreferencesUtils.setLastPairedDeviceName(BtNotificationForthActivity.this, device.getName());
					PreferencesUtils.setBTDeviceName(BtNotificationForthActivity.this, device.getName());
					PreferencesUtils.setDeviceAddress(BtNotificationForthActivity.this, device.getAddress());
					PreferencesUtils.setDeviceName(BtNotificationForthActivity.this, device.getName());
					PreferencesUtils.setLastPairedDeviceAddress(BtNotificationForthActivity.this, device.getAddress());
					PreferencesUtils.setLastPairedDeviceType(BtNotificationForthActivity.this, 1);
					sendNotification();
					if (progressDialog != null) {
						progressDialog.dismiss();
					}
					Log.d(TAG,
							"Success to disconnect and connect to the phone, done.");
					BtNotificationForthActivity.this.finish();
					System.exit(0);
				} else if ((mapstate == 0 && (priorState != 0))
						|| (mapstate == 1 && (priorState != 0))) {
//					try {
//						BtNotificationForthActivity.this
//								.unregisterReceiver(btTelephonyReceiver);
//					} catch (IllegalArgumentException e) {
//						Log.i(TAG,
//								"btTelephonyReceiver is already unregistered");
//					}
					Log.d(TAG,
							"Fail to disconnect and connect to the phone, ask the user try again.");
					continueButton.setEnabled(true);
					continueButton.setFocusable(true);
					cancelButton.setFocusable(true);
					((TextView)continueButton).setText("Try Again");
					if (progressDialog != null) {
						progressDialog.dismiss();
					}
				}
				priorState = mapstate;
			}
		}
	};
	
	private BroadcastReceiver hudServiceBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			//device connection state changed event
			if(intent.getAction().equals("HUD_STATE_CHANGED")){
				int connectionState = intent.getIntExtra("state", 0);
				if(connectionState == 2){ // 2 connected
				    if(DeviceUtils.isLimo()){
					}else{
						PreferencesUtils.setLastPairedDeviceName(BtNotificationForthActivity.this, BTPropertyReader.getBTConnectedDeviceName(BtNotificationForthActivity.this));
						PreferencesUtils.setLastPairedDeviceAddress(BtNotificationForthActivity.this, BTPropertyReader.getBTConnectedDeviceAddress(BtNotificationForthActivity.this));
						PreferencesUtils.setLastPairedDeviceType(BtNotificationForthActivity.this, 1);
						if(PreferencesUtils.isReconnect(BtNotificationForthActivity.this)){
							Intent enableIntent = new Intent(
									BtNotificationForthActivity.this,
									BtNotificationSixthActivity.class)
									.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
											| Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(enableIntent);
							BtNotificationForthActivity.this.finish();
						}
					}
				}
			}
		}
	};

}
// End of JIRA: MODLIVE-772