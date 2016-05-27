package com.reconinstruments.socialsharing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

public class ConnectPhoneActivity extends Activity {
	
	private static final String TAG = "ConnectPhoneActivity";
	private TextView titleTV;
	private TextView contentTV;
	private TextView footerTV;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.error);

		titleTV = (TextView) findViewById(R.id.title);
		titleTV.setText("FACEBOOK POST FAILED");
		contentTV = (TextView) findViewById(R.id.content_text);
		contentTV.setText("You need to connect a smartphone to share to Facebook. Would you like to connect now?");
		footerTV = (TextView) findViewById(R.id.footer_text);
		footerTV.setText("CONNECT SMARTPHONE");
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch(keyCode){
		case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				startActivity((new Intent("com.reconinstruments.connectdevice.CONNECT")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));	
				finish();	
				return true;
			default:
				return super.onKeyUp(keyCode, event);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
