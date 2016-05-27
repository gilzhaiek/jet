package com.reconinstruments.connectdevice.ios;

import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;

import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.R;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;

public class ConnectActivity extends ConnectionActivity {
	private static final String TAG = "ConnectActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_ios_connect);

		final View continueButton = (View) findViewById(R.id.continue_button);
		final View cancelButton = (View) findViewById(R.id.cancel);

		continueButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				continueButton.setFocusable(false);
				cancelButton.setFocusable(false);
				// start the waiting activity with a flag that tells the system
				// it was intentionally started
				Intent intent = new Intent(ConnectActivity.this,
						WaitingActivity.class);
				ConnectActivity.from = 0;
				startActivity(intent);
				finish();
				// BTCommon.stopBT(ConnectActivity.this);
				// BTCommon.stopFT(ConnectActivity.this);
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				continueButton.setFocusable(false);
				cancelButton.setFocusable(false);
				ConnectActivity.this.finish();
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

	}
}
