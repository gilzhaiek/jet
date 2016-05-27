package com.reconinstruments.socialsharing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.KeyEvent;
import android.widget.TextView;

public class ConnectFacebookActivity extends Activity {
	
	private static final String TAG = "ConnectFacebookActivity";
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
		contentTV.setText(Html.fromHtml("You need to connect <b>Facebook</b> to your <b>Engage</b> app to share. Please log out of the <b>Engage</b> app and re-login using <b>Facebook</b>."));
		footerTV = (TextView) findViewById(R.id.footer_text);
		footerTV.setText("CONTINUE");
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch(keyCode){
		    	case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
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
