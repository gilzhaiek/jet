package com.reconinstruments.connectdevice.ios;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.TextView;

import com.reconinstruments.connectdevice.BTPropertyReader;
import com.reconinstruments.connectdevice.ChooseDeviceActivity;
import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.DisconnectDeviceActivity;
import com.reconinstruments.connectdevice.ConnectionActivity.DeviceType;
import com.reconinstruments.connectdevice.PreferencesUtils;
import com.reconinstruments.connectdevice.R;
import com.reconinstruments.connectdevice.ios.BtNotificationThirdActivity;
import com.reconinstruments.connectdevice.ios.FirstConnectActivity;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.commonwidgets.TwoOptionsJumpFixer;
import com.reconinstruments.utils.DeviceUtils;

public class MfiReconnectActivity extends ConnectionActivity {
	protected static final String TAG = "MfiReconnectActivity";

	private boolean fails = false;
	private ProgressDialog progressDialog;
	private TextView titleTV;
	private TextView textTV;
	private View cancelItem;
	private TwoOptionsJumpFixer twoOptionsJumpFixer;
	private int tryAgainTimes = 0;
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.activity_reconnect_jet);
		titleTV = (TextView) findViewById(R.id.title);
		textTV = (TextView) findViewById(R.id.activity_disconnected_text);
		
		Bundle extras = getIntent().getExtras();
		fails = false;
		if (extras != null) {
			fails = extras.getBoolean("fails");
		}
		if(fails){
			titleTV.setText("FAILED TO RECONNECT");
			if(PreferencesUtils.getLastPairedDeviceType(MfiReconnectActivity.this) == 0){
				textTV.setText(Html
						.fromHtml("Open the Engage app on your Android phone and try again.\n"));
			}else{
				
			textTV
			.setText
			(Html
					.fromHtml(
							"Go to <b>Settings > Bluetooth</b> on your phone and ensure Bluetooth is &nbsp<img src=\"on_switch.png\" align=\"middle\"> Then try again.",
							new ImageGetter() {
								@Override
								public Drawable getDrawable(String source) {
									int id;
									if (source
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
			
			}
			
		}else{
		    if(DeviceUtils.isLimo()){
				textTV.setText(Html
						.fromHtml("Your MOD Live was previously connected to <b>"
						+ PreferencesUtils.getLastPairedDeviceName(this)
						+ "</b>, Would you like to connect to this device again?"));
			}else{
				textTV.setText(Html
						.fromHtml("Your Snow2 was previously connected to <b>"
						+ PreferencesUtils.getLastPairedDeviceName(this)
						+ "</b>, Would you like to connect to this device again?"));
			}
		}
		
		
		final View disconnectItem = findViewById(R.id.disconnect);
		cancelItem = findViewById(R.id.cancel);
		if(fails){
			((TextView)cancelItem).setText("TRY AGAIN");
		}
		disconnectItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				disconnectItem.setFocusable(false);
				cancelItem.setFocusable(false);
				progressDialog = new ProgressDialog(MfiReconnectActivity.this);
				progressDialog.setIndeterminate(true);
				progressDialog.setCancelable(false);
				progressDialog.show();
				progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
				TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
				textTv.setText("Unpairing, please wait...");
				int deviceType = BTPropertyReader.getBTConnectedDeviceType(MfiReconnectActivity.this);
				if(deviceType == 0){ // android device
					if(hudService != null){
						MfiReconnectActivity.this.disconnect(DeviceType.ANDROID);
					}
				}else{
					//TODO do something extra for ios
					if(hudService != null){
						MfiReconnectActivity.this.disconnect(DeviceType.IOS);
					}
				}
				new CountDownTimer(3 * 1000, 1000) {
					public void onTick(long millisUntilFinished) {
					}
					public void onFinish() {
						if(progressDialog != null && progressDialog.isShowing()){
							progressDialog.dismiss();
						}
						unpairDevice();
						PreferencesUtils.setLastPairedDeviceName(MfiReconnectActivity.this, "");
						PreferencesUtils.setBTDeviceName(MfiReconnectActivity.this, "");
						PreferencesUtils.setDeviceAddress(MfiReconnectActivity.this, "");
						PreferencesUtils.setDeviceName(MfiReconnectActivity.this, "");
						PreferencesUtils.setLastPairedDeviceAddress(MfiReconnectActivity.this, "");
						PreferencesUtils.setLastPairedDeviceType(MfiReconnectActivity.this, 0);
						PreferencesUtils.setReconnect(MfiReconnectActivity.this, false);
						startActivity(new Intent(MfiReconnectActivity.this,ChooseDeviceActivity.class)
						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY));
					finish();
					}
				}.start();
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
//				int deviceType = BTPropertyReader.getBTConnectedDeviceType(MfiReconnectActivity.this);
//				if(deviceType == 0){ // android device
//				}else{
					if(hudService != null){
					    if(!DeviceUtils.isLimo()){
							progressDialog = new ProgressDialog(MfiReconnectActivity.this);
							progressDialog.setIndeterminate(true);
							progressDialog.setCancelable(false);
							progressDialog.show();
							progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
							TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
							textTv.setText("Reconnecting, please wait...");
						}
						
						PreferencesUtils.setReconnect(MfiReconnectActivity.this, true);
						if(PreferencesUtils.getLastPairedDeviceType(MfiReconnectActivity.this) == 1){
						    if(DeviceUtils.isLimo()){
								Intent intent = new Intent(MfiReconnectActivity.this,
										WaitingActivity.class)
										.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								startActivity(intent);
								finish();
							}else{
								MfiReconnectActivity.this.connect(PreferencesUtils.getLastPairedDeviceAddress(MfiReconnectActivity.this), DeviceType.IOS);
							}
						}else{
							// the following tryAgainTimes logic is to prevent the system from bad bluetooth state. 
							// BluetoothSocket.connect() will block until a connection is made or the connection fails according to SDK document.
							// but sometimes it returns neither a connection nor an IOException and just block it on this method call
							// the other components can't release the bluetooth resource any more unless restarting the bluetooth module or rebooting the device.
							// thus we need an option to kill the HUDService and restart it completely. We think here is the better place.
							if(tryAgainTimes > 1){
								Log.d(TAG, "resarting hud service and then try to reconnect to the phone.");
								tryAgainTimes = 0;
								MfiReconnectActivity.this.killHUDService();
								new CountDownTimer(1000, 1000) {
									@Override
									public void onFinish() {
										MfiReconnectActivity.this.getApplicationContext().startService(new Intent("RECON_HUD_SERVICE"));
									}
									@Override
									public void onTick(long millisUntilFinished) {
									}
									
								}.start();
								new CountDownTimer(5000, 1000) {
									@Override
									public void onFinish() {
										MfiReconnectActivity.this.connect(PreferencesUtils.getLastPairedDeviceAddress(MfiReconnectActivity.this), DeviceType.ANDROID);
									}
									@Override
									public void onTick(long millisUntilFinished) {
									}
									
								}.start();
							}else{
								MfiReconnectActivity.this.connect(PreferencesUtils.getLastPairedDeviceAddress(MfiReconnectActivity.this), DeviceType.ANDROID);
							}
						}
						
						new CountDownTimer(15 * 1000, 1000) {
							public void onTick(long millisUntilFinished) {
							}
							public void onFinish() {
								if(progressDialog != null){
									progressDialog.dismiss();
								}
								fails = true;
								if(PreferencesUtils.getLastPairedDeviceType(MfiReconnectActivity.this) == 1){
									textTV.setText(Html
											.fromHtml(
													"Go to <b>Settings > Bluetooth</b> on your phone and ensure Bluetooth is &nbsp<img src=\"on_switch.png\" align=\"middle\"> Then try again.",
													new ImageGetter() {
														@Override
														public Drawable getDrawable(String source) {
															int id;
															if (source
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
								}else{
									textTV.setText(Html
											.fromHtml("Open the Engage app on your Android phone and try again.\n"));
								}
								((TextView)cancelItem).setText("TRY AGAIN");
								twoOptionsJumpFixer = new TwoOptionsJumpFixer(disconnectItem, cancelItem);
								twoOptionsJumpFixer.start();
								tryAgainTimes ++;
							}
						}.start();
					}
//				}
					
					
//				MfiReconnectActivity.this.finish();
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
		twoOptionsJumpFixer = new TwoOptionsJumpFixer(disconnectItem, cancelItem);
		twoOptionsJumpFixer.start();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		if(progressDialog != null && progressDialog.isShowing()){
			progressDialog.dismiss();
			progressDialog = null;
		}
		super.onDestroy();
	}
}