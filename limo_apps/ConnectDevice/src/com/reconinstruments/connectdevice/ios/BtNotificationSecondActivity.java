package com.reconinstruments.connectdevice.ios;

import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;

import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.R;

//JIRA: MODLIVE-772 Implement bluetooth connection wizard on MODLIVE
public class BtNotificationSecondActivity extends ConnectionActivity {
	private static final String TAG = "BtNotificationSecondActivity";
	static BtNotificationSecondActivity activity;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_ios_bt_2);

		Log.d(TAG, "onCreate");
		activity = this;
		View okButton = (View) findViewById(R.id.ok);
		View cancelButton = (View) findViewById(R.id.cancel);

		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(BtNotificationSecondActivity.this,
						BtNotificationThirdActivity.class));
				BtNotificationSecondActivity.this.finish();
				// finish();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				cancel = true;
				BtNotificationSecondActivity.this.finish();
			}
		});

		okButton.setOnFocusChangeListener(new OnFocusChangeListener() {

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
		okButton.setSelected(true);
		okButton.requestFocus();
	}

}
// End of JIRA: MODLIVE-772
