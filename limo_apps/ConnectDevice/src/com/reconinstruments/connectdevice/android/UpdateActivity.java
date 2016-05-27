package com.reconinstruments.connectdevice.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import com.reconinstruments.connectdevice.R;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;

public class UpdateActivity extends Activity {
	protected static final String TAG = "UpdateActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.activity_android_update);

		BTCommon.stopBT(this);
		BTCommon.stopFT(this);
	}
}
