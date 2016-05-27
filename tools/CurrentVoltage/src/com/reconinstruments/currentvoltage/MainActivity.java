package com.reconinstruments.currentvoltage;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final boolean DEBUG = CurrentVoltage.DEBUG;
	
	Button mStartButton;
	Button mStartAvgButton;
	Button mStopButton;
	Button mRefreshUpButton;
	Button mRefreshDownButton;
	TextView mRefreshRateTextView;
	
	Intent mOverlayServiceIntent; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mStartButton = (Button) findViewById(R.id.button_start);
		mStartButton.setOnClickListener(this);
		mStartAvgButton = (Button) findViewById(R.id.button_start_avg);
		mStartAvgButton.setOnClickListener(this);
		mStopButton = (Button) findViewById(R.id.button_stop);
		mStopButton.setOnClickListener(this);
		mRefreshUpButton = (Button) findViewById(R.id.button_refresh_up);
		mRefreshUpButton.setOnClickListener(this);
		mRefreshDownButton = (Button) findViewById(R.id.button_refresh_down);
		mRefreshDownButton.setOnClickListener(this);
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		CurrentVoltage.refreshRate = pref.getInt(CurrentVoltage.PREF_KEY_REFRESH_RATE, 5); 
		
		mRefreshRateTextView = (TextView) findViewById(R.id.textview_refresh);
		mRefreshRateTextView.setText(""+CurrentVoltage.refreshRate);
		
	}

	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.button_start_avg:
			CurrentVoltage.averageMode = true;
			mOverlayServiceIntent = new Intent(this, TopOverlayService.class);
			startService(mOverlayServiceIntent);
			break;
		case R.id.button_start:
			CurrentVoltage.averageMode = false;
			mOverlayServiceIntent = new Intent(this, TopOverlayService.class);
			startService(mOverlayServiceIntent);
			break;
		case R.id.button_stop:
			stopService(mOverlayServiceIntent);
			break;
		case R.id.button_refresh_up:
			CurrentVoltage.refreshRate++;
			PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(CurrentVoltage.PREF_KEY_REFRESH_RATE, CurrentVoltage.refreshRate).commit();
			mRefreshRateTextView.setText(""+CurrentVoltage.refreshRate);
			break;
		case R.id.button_refresh_down:
			if (CurrentVoltage.refreshRate > 0)
				CurrentVoltage.refreshRate--;
			PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(CurrentVoltage.PREF_KEY_REFRESH_RATE, CurrentVoltage.refreshRate).commit();
			mRefreshRateTextView.setText(""+CurrentVoltage.refreshRate);
			break;
		}
		
	}

}
