package com.reconinstruments.glide;

import java.text.DecimalFormat;
import java.util.Random;

import com.reconinstruments.modservice.ReconMODServiceMessage;
import com.reconinstruments.reconsettings.ReconSettingsUtil;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.widget.TextView;

public class CalibrationActivity extends Activity {

	private static boolean debug = false;
	
	private MODServiceConnection mMODConnection;
	private Messenger mMODConnectionMessenger;
	
	private TextView rawField, calField;
	
	private Bundle currentAltBundle;
	
	private int altOffset = 0;

	private int units;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration);
        
        // Set up connection to MOD Service
        mMODConnection = new MODServiceConnection(this);
        mMODConnectionMessenger = new Messenger(new MODServiceHandler());
        mMODConnection.addReceiver(mMODConnectionMessenger);
        mMODConnection.doBindService();
        
        // View Hooks and Initialization
        Typeface tf = Typeface.createFromAsset(this.getResources().getAssets(), "fonts/Eurostib_1.TTF");
        
        TextView rawTitle = (TextView) findViewById(R.id.raw_alt_title);
        rawField = (TextView) findViewById(R.id.raw_alt_field);
        
        TextView calTitle = (TextView) findViewById(R.id.cal_alt_title);
        calField = (TextView) findViewById(R.id.cal_alt_field);
        
        TextView title = (TextView) findViewById(R.id.title);
        
        title.setTypeface(tf);
        rawTitle.setTypeface(tf);
        rawField.setTypeface(tf);
        calTitle.setTypeface(tf);
        calField.setTypeface(tf);
    }
	
	public void onResume() {
		super.onResume();
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        altOffset = mPrefs.getInt("altitude_offset", 0);
		units = ReconSettingsUtil.getUnits(this);
	}
	
	public void onDestroy() {
    	super.onDestroy();
    	mMODConnection.doUnBindService();
    }
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
    	Intent myIntent;
    	
    	switch(keyCode) {
    	case KeyEvent.KEYCODE_DPAD_LEFT:
    		myIntent = new Intent(this, DashboardTwoActivity.class);
            startActivity(myIntent);
            finish();
    		return true;
    		
    	default:
    		return super.onKeyUp(keyCode, event);
    	}
    }
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = mPrefs.edit();
		
		switch(keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
			altOffset += 5;
			updateAlt(currentAltBundle, altOffset);
			
			editor.putInt("altitude_offset", altOffset); 
			editor.commit();
			return true;
			
		case KeyEvent.KEYCODE_DPAD_DOWN:
			altOffset -= 5;
			updateAlt(currentAltBundle, altOffset);
			
			editor.putInt("altitude_offset", altOffset); 
			editor.commit();
			return true;
			
		default:
			return super.onKeyDown(keyCode, event);
		}
	}
	
	private void updateFields(Bundle data) {
    	Bundle altBundle = data.getBundle("ALTITUDE_BUNDLE");
    	
    	if(debug) {
    		Random r = new Random();
    		altBundle.putFloat("Alt", r.nextFloat() * 10000);
    		altBundle.putInt("HeightOffsetN", 600);
    	}
    	
    	currentAltBundle = altBundle;
    	
    	updateAlt(altBundle, altOffset);
    }
	
	private void updateAlt(Bundle altBundle, float offset) {
    	DecimalFormat df = new DecimalFormat();
    	df.setMaximumFractionDigits(0);
    	df.setMinimumFractionDigits(0);
    	
    	float alt = altBundle.getFloat("Alt");
    	if(debug) alt = 1000;
    	float altAdjusted = alt + offset;
		boolean isUncertain = altBundle.getInt("HeightOffsetN") < 500;
		
		if(isUncertain) {
			rawField.setText("...");
		} else {
			if(units == ReconSettingsUtil.RECON_UINTS_METRIC) {
				rawField.setText(df.format(alt)+"m");
				calField.setText(df.format(altAdjusted)+"m");
			} else {
				rawField.setText(df.format(ConversionUtil.metersToFeet(alt))+"ft");
				calField.setText(df.format(ConversionUtil.metersToFeet(altAdjusted))+"ft");
			}
		}
    }
	
	class MODServiceHandler extends Handler {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
			case ReconMODServiceMessage.MSG_RESULT:
				if (msg.arg1 == ReconMODServiceMessage.MSG_GET_FULL_INFO_BUNDLE) {
					Bundle data = msg.getData();
					
					updateFields(data);
				}
				break;

			default:
				super.handleMessage(msg);
			}
		}
    }
}
