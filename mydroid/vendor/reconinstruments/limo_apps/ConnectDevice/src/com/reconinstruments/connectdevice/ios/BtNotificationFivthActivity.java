package com.reconinstruments.connectdevice.ios;
import com.reconinstruments.utils.DeviceUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.connectdevice.BTPropertyReader;
import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.PreferencesUtils;
import com.reconinstruments.connectdevice.R;

import com.reconinstruments.ifisoakley.OakleyDecider;

public class BtNotificationFivthActivity extends ConnectionActivity {
	protected static final String TAG = "BtNotificationFivthActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_ios_fivth_jet);
		setProgressBarIndeterminateVisibility(true);
		if(DeviceUtils.isLimo()){
				final View next = (View) findViewById(R.id.next);
				next.setFocusable(true);
				next.requestFocus();
			next.setLongClickable(true);
			next.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					Log.d(TAG, "setOnLongClickListener->onLongClick: quittting");
					return true;
				}
				
			});
			next.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					next.setFocusable(false);
					BtNotificationFivthActivity.this.finish();

					Log.d(TAG, "setOnClickListener->onClick: do nothing");
				}
				
			});
			next.setOnFocusChangeListener(new OnFocusChangeListener() {

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
		}else{
			ImageView select = (ImageView) findViewById(R.id.select_button);
		}

		if (OakleyDecider.isOakley()) {
			TextView waiting = (TextView) findViewById(R.id.waiting_for_engage_text_view);
			waiting.setText(R.string.waiting_for_engage_oakley);
			TextView youNeedEngage = (TextView) findViewById(R.id.activity_ios_fivth_text);
			youNeedEngage.setText(R.string.you_need_engage_oakley);
		}

		registerReceiver(hudServiceBroadcastReceiver, new IntentFilter(
				"HUD_STATE_CHANGED"));
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		setProgressBarIndeterminateVisibility(false);
	try{
		Log.d(TAG, "unregisterReceiver hudServiceBroadcastReceiver");
		unregisterReceiver(hudServiceBroadcastReceiver);
	}catch(IllegalArgumentException e){
		//ignore
	}
	}
	
	@Override
	public void onBackPressed() {
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			return true;
		}else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
		    if(!DeviceUtils.isLimo()){
				BtNotificationFivthActivity.this.finish();
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
	
	private BroadcastReceiver hudServiceBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			//device connection state changed event
			if(intent.getAction().equals("HUD_STATE_CHANGED")){
				int connectionState = intent.getIntExtra("state", 0);
				if(connectionState == 2){ // 2 connected
				    if(DeviceUtils.isLimo()){
					}else{
						PreferencesUtils.setLastPairedDeviceName(BtNotificationFivthActivity.this, BTPropertyReader.getBTConnectedDeviceName(BtNotificationFivthActivity.this));
						PreferencesUtils.setLastPairedDeviceAddress(BtNotificationFivthActivity.this, BTPropertyReader.getBTConnectedDeviceAddress(BtNotificationFivthActivity.this));
						PreferencesUtils.setLastPairedDeviceType(BtNotificationFivthActivity.this, 1);
						Intent enableIntent = new Intent(
								BtNotificationFivthActivity.this,
								BtNotificationSixthActivity.class)
								.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
										| Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(enableIntent);
						BtNotificationFivthActivity.this.finish();
					}
				}
			}
		}
	};
}
