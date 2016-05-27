package com.reconinstruments.connectdevice.android;

import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.R;
import com.reconinstruments.ifisoakley.OakleyDecider;
public class FailedActivity extends ConnectionActivity {
	protected static final String TAG = "FailedActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_android_failed_jet);

		final View continueButton = (View) findViewById(R.id.try_again);
		final View cancelButton = (View) findViewById(R.id.cancel);

		continueButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				continueButton.setFocusable(false);
				cancelButton.setFocusable(false);
				startActivity(new Intent(FailedActivity.this,
						WaitingActivity.class)
				.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				finish();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				continueButton.setFocusable(false);
				cancelButton.setFocusable(false);
				cancel = true;
				finish();
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
//		continueButton.setSelected(true);
//		continueButton.requestFocus();

		if (OakleyDecider.isOakley()) {
			// TextView conn = (TextView)
			// findViewById(R.id.activity_android_first_text);
			// conn.setText(R.string.android_first);
		}

	}
}
