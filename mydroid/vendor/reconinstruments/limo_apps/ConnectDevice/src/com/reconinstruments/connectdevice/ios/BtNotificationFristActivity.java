package com.reconinstruments.connectdevice.ios;

import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;

import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.PreferencesUtils;
import com.reconinstruments.connectdevice.R;

// JIRA: MODLIVE-772 Implement bluetooth connection wizard on MODLIVE
public class BtNotificationFristActivity extends ConnectionActivity {
	private static final String TAG = "BtNotificationFristActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_ios_bt_1);

		Log.d(TAG, "onCreate");
		View enableButton = (View) findViewById(R.id.enable);
		View cancelButton = (View) findViewById(R.id.cancel);

		enableButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "mapStatus = " + mapStatus
						+ " and hfpStatus = " + hfpStatus);
				if ((hfpStatus == 2) && (PreferencesUtils.getBTDeviceName(BtNotificationFristActivity.this).equals(PreferencesUtils.getDeviceName(BtNotificationFristActivity.this)))) {
					Log.d(TAG,
							"Skip to enable call and sms on Mod Live for iPhone only.");
					startActivity(new Intent(BtNotificationFristActivity.this,
							BtNotificationForthActivity.class)
							.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
									| Intent.FLAG_ACTIVITY_NEW_TASK));
				}else{
					startActivity(new Intent(BtNotificationFristActivity.this,
							BtNotificationSecondActivity.class)
							.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
									| Intent.FLAG_ACTIVITY_NEW_TASK));
				}
				BtNotificationFristActivity.this.finish();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				cancel = true;
				BtNotificationFristActivity.this.finish();
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
		// TODO Auto-generated method stub
		super.onResume();
		}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
}
// End of JIRA: MODLIVE-772