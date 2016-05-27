package com.reconinstruments.connectdevice.ios;

import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.TextView;

import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.PreferencesUtils;
import com.reconinstruments.connectdevice.R;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;

// JIRA: MODLIVE-772 Implement bluetooth connection wizard on MODLIVE
public class BtReconnectActivity extends ConnectionActivity {
	private static final String TAG = "BtReconnectActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_ios_bt_reconnect);

		Log.d(TAG, "onCreate");
		Intent intent = getIntent();
		int from = intent.getIntExtra("from", 0);
		// final String address = intent.getStringExtra("address");
		String deviceName = PreferencesUtils.getDeviceName(this);
		TextView textView = (TextView) findViewById(R.id.message);
		// if (from == 0) {
		// textView.setText("Your HUD has been disconnected from \""
		// + deviceName + "\". Would you like to reconnect?");
		// } else {
		textView.setText("Your HUD was previously connected from \""
				+ deviceName + "\". Would you like to connect again?");
		// }
		final View enableButton = (View) findViewById(R.id.enable);
		final View cancelButton = (View) findViewById(R.id.cancel);
		
		String address = PreferencesUtils.getDeviceAddress(this);
		if (!"unknown".equals(address)) {
			BtReconnectActivity.super.connectToHfp(address);
			BtReconnectActivity.super.connectToMap(address);
		}

		enableButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enableButton.setFocusable(false);
				cancelButton.setFocusable(false);
				Log.d(TAG, "reconnecting...");
				Intent intent = new Intent(BtReconnectActivity.this,
						WaitingActivity.class)
						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				// BTCommon.stopBT(BtReconnectActivity.this);
				// BTCommon.stopFT(BtReconnectActivity.this);
				ConnectionActivity.from = 1;
				BtReconnectActivity.this.finish();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enableButton.setFocusable(false);
				cancelButton.setFocusable(false);
				cancel = true;
				BtReconnectActivity.this.finish();
			}
		});

		enableButton.setOnFocusChangeListener(new OnFocusChangeListener() {

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
		enableButton.setSelected(true);
		enableButton.requestFocus();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// initService_phone();
	}

	protected void onDestroy() {
		super.onDestroy();
		// releaseService_phone();
	}

}
// End of JIRA: MODLIVE-772