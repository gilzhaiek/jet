package com.reconinstruments.connectdevice.android;

import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reconinstruments.connectdevice.ChooseDeviceActivity;
import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.R;
import com.reconinstruments.connectdevice.ios.BtNotificationThirdActivity;
import com.reconinstruments.ifisoakley.OakleyDecider;

public class FirstConnectActivity extends ConnectionActivity {
	protected static final String TAG = "FirstConnectActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_android_first_jet);
		final View next = (View) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			    next.setFocusable(false);
			    startActivity(new Intent(FirstConnectActivity.this,
						     WaitingActivity.class));
			    finish();
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

		if (OakleyDecider.isOakley()) {
			TextView conn = (TextView) findViewById(R.id.activity_android_first_text);
			conn.setText(R.string.android_first_oakley);
		}

	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
		   keyCode == KeyEvent.KEYCODE_ENTER){
		    startActivity(new Intent(FirstConnectActivity.this,
					     WaitingActivity.class));
		    finish();
		    return true;
		}
		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public void onBackPressed() {
	    startActivity(new Intent(FirstConnectActivity.this,
				     ChooseDeviceActivity.class));
	    FirstConnectActivity.this.finish();
	}
}
