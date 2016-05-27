package com.reconinstruments.dashlauncher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

public class MapsCorruptionActivity extends Activity {

	public static final String TAG = "MapsCorruptionActivity";
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.maps_corruption_popup_layout);
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.v(TAG, "onKeyUp");
		if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||  keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_BACK) {
			startActivity(new Intent(getApplicationContext(), com.reconinstruments.dashlauncher.livestats.LiveStatsActivity.class));
			overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			return true;
		}
		
		return super.onKeyUp(keyCode, event);
	}
	
	public void onBackPressed() {
	    
		return;
	}
}
