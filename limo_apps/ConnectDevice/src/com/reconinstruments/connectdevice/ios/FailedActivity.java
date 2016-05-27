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

public class FailedActivity extends ConnectionActivity {
	protected static final String TAG = "FailedActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_ios_failed);

		Log.d(TAG, "onCreate");
		final View continueButton = (View) findViewById(R.id.try_again);
		final View cancelButton = (View) findViewById(R.id.cancel);
		TextView headerText = (TextView) findViewById(R.id.title);
		TextView contentText = (TextView) findViewById(R.id.content);

		String address = PreferencesUtils.getDeviceAddress(this);
		if (!"unknown".equals(address) && !"".equals(address)) {
			FailedActivity.super.connectToHfp(address);
			FailedActivity.super.connectToMap(address);
		}
		
		if (!"unknown".equals(PreferencesUtils.getDeviceName(this)) && !"".equals(PreferencesUtils.getDeviceName(this))) {
			((TextView)continueButton).setText("RECONNECT");
			((TextView)cancelButton).setText("NOT NOW");
			headerText.setText("RECONNECT SMARTPHONE");
			contentText.setText("Your HUD has been disconnected from "
					+ PreferencesUtils.getDeviceName(this)
					+ ". Would you like to reconnect?");
		}

		continueButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				continueButton.setFocusable(false);
				cancelButton.setFocusable(false);
				startActivity(new Intent(FailedActivity.this,
						WaitingActivity.class)
						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				FailedActivity.from = 2;
				FailedActivity.this.finish();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				continueButton.setFocusable(false);
				cancelButton.setFocusable(false);
				cancel = true;
				FailedActivity.this.finish();
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
