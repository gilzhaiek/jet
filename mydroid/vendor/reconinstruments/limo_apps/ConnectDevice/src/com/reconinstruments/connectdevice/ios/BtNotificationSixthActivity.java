package com.reconinstruments.connectdevice.ios;

import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.connectdevice.BTPropertyReader;
import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.PreferencesUtils;
import com.reconinstruments.connectdevice.R;
import com.reconinstruments.commonwidgets.ReconToast;

import com.reconinstruments.ifisoakley.OakleyDecider;
import com.reconinstruments.utils.DeviceUtils;

public class BtNotificationSixthActivity extends ConnectionActivity {
	protected static final String TAG = "BtNotificationSixthActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_ios_sixth_jet);
		if(DeviceUtils.isLimo()){
			final View next = (View) findViewById(R.id.next);
			next.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					next.setFocusable(false);
			    	(new ReconToast(getApplicationContext(), com.reconinstruments.commonwidgets.R.drawable.checkbox_icon, BTPropertyReader.getBTConnectedDeviceName(BtNotificationSixthActivity.this) + " Setup Complete")).show();
					BtNotificationSixthActivity.this.finish();
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
		}

		if (OakleyDecider.isOakley()) {
			TextView conn = (TextView) findViewById(R.id.activity_ios_first_text);
			conn.setText(R.string.ios_first_oakley);
		}

		if(PreferencesUtils.isReconnect(BtNotificationSixthActivity.this)){
	    	(new ReconToast(BtNotificationSixthActivity.this, com.reconinstruments.commonwidgets.R.drawable.checkbox_icon, BTPropertyReader.getBTConnectedDeviceName(BtNotificationSixthActivity.this) + " Setup Complete")).show();
			BtNotificationSixthActivity.this.finish();
		}
	}
	
	@Override
	public void onBackPressed() {
	}

	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
		   keyCode == KeyEvent.KEYCODE_ENTER){
		    if(!DeviceUtils.isLimo()){
		    	(new ReconToast(BtNotificationSixthActivity.this, com.reconinstruments.commonwidgets.R.drawable.checkbox_icon, BTPropertyReader.getBTConnectedDeviceName(BtNotificationSixthActivity.this) + " Setup Complete")).show();
				BtNotificationSixthActivity.this.finish();
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
}
