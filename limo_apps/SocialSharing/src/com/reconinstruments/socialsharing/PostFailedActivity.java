package com.reconinstruments.socialsharing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

public class PostFailedActivity extends Activity {
	
	private static final String TAG = "PostFailedActivity";
	private TextView titleTV;
	private TextView contentTV;
	private TextView footerTV;
	
	private String category;
	private String title;
	private String valueAndUnit;
	private boolean tryAgain = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.error);

		Intent intent = getIntent();
		category = intent.getStringExtra("category");
		title = intent.getStringExtra("title");
		valueAndUnit = intent.getStringExtra("valueAndUnit");
		tryAgain = intent.getBooleanExtra("tryAgain", false);

		titleTV = (TextView) findViewById(R.id.title);
		titleTV.setText("FACEBOOK POST FAILED");
		contentTV = (TextView) findViewById(R.id.content_text);
		contentTV.setText("There was an error posting to Facebook. Ensure you have the Engage app open and try again.");
		footerTV = (TextView) findViewById(R.id.footer_text);
		footerTV.setText("TRY AGAIN");
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch(keyCode){
		case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				Intent intent = new Intent(this,SharingActivity.class);
				intent.putExtra("category", category);
				intent.putExtra("title", title);
				intent.putExtra("valueAndUnit", valueAndUnit);
				intent.putExtra("tryAgain", tryAgain);
				startActivity(intent);
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
