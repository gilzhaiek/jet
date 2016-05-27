package com.reconinstruments.connectdevice.ios;

import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.TextView;

import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.R;

public class IosBtToggleActivity extends ConnectionActivity {
	private static final String TAG = "IosBtToggleActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_ios_bt_toggle);

		Log.d(TAG, "onCreate");
		TextView textView = (TextView) findViewById(R.id.message);
		textView.setText(Html
				.fromHtml("Please go to <b>Settings</b> > <b>Bluetooth</b> on your <b>iPhone</b> and turn bluetooth off and on to reset it."));
		View okButton = (View) findViewById(R.id.ok);

		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				cancel = true;
				IosBtToggleActivity.this.finish();
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
		okButton.setSelected(true);
		okButton.requestFocus();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	protected void onDestroy() {
		super.onDestroy();
		finish();
	}

}