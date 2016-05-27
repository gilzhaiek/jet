package com.reconinstruments.reconstats.message;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.reconinstruments.reconstats.R;
import com.reconinstruments.reconstats.StatsActivity.StatsType;
import com.reconinstruments.reconstats.TranscendServiceConnection;

public class StatMessageActivity extends Activity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_stat_message);
		
		
	}


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER ||
		   keyCode== KeyEvent.KEYCODE_ENTER){
			//PhoneUtils.startCall(source, contact, this); //no calling for now!
		}
		return super.onKeyUp(keyCode, event);
	}

	public void setViewData(Bundle data) {
		
	}
}
