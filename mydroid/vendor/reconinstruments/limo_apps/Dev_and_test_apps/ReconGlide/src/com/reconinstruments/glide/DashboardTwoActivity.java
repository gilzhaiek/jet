package com.reconinstruments.glide;

import java.text.DecimalFormat;
import java.util.Random;

import com.reconinstruments.modservice.ReconMODServiceMessage;
import com.reconinstruments.polarhr.service.PolarHRStatus;
import com.reconinstruments.reconsettings.ReconSettingsUtil;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DashboardTwoActivity extends Activity {
    
	private static boolean debug = false;
	
	private MODServiceConnection mMODConnection;
	private Messenger mMODConnectionMessenger;
	private PolarBroadcastReceiver mPolarBroadcastReceiver;
	
	private TextView speedField, speedUnitField, glideField, glideTitle, altField, hrField;
	private View glideView;
	private ImageView gaugeImageView, glideIcon;
	
	private static int INVALID_SPEED = -1; // from ReconSpeedManager
	private static int GAUGE_TOP_END = 260;
	
	private int altOffset = 0;
	
	private int units = ReconSettingsUtil.RECON_UINTS_METRIC;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2);
        
        // Set up connection to MOD Service
        mMODConnection = new MODServiceConnection(this);
        mMODConnectionMessenger = new Messenger(new MODServiceHandler());
        mMODConnection.addReceiver(mMODConnectionMessenger);
        mMODConnection.doBindService();
        
        // Listen for Polar broadcasts
        mPolarBroadcastReceiver = new PolarBroadcastReceiver();
        registerReceiver(mPolarBroadcastReceiver, new IntentFilter("POLAR_BROADCAST_HR"));
        
        // View hooks & init
        Typeface tf = Typeface.createFromAsset(this.getResources().getAssets(), "fonts/Eurostib_1.TTF");
        
        glideView = findViewById(R.id.glide);
        speedField = (TextView) this.findViewById(R.id.speed_field);
        speedUnitField = (TextView) this.findViewById(R.id.speed_unit);
        TextView altTitle = (TextView) this.findViewById(R.id.alt_title);
        altField = (TextView) this.findViewById(R.id.alt_field);
        glideTitle = (TextView) this.findViewById(R.id.glide_title);
        glideField = (TextView) this.findViewById(R.id.glide_field);
        hrField = (TextView) this.findViewById(R.id.hr_field);
        gaugeImageView = (ImageView) this.findViewById(R.id.gauge);
        glideIcon = (ImageView) this.findViewById(R.id.glide_icon);
        
        speedField.setTypeface(tf);
        speedUnitField.setTypeface(tf);
        altTitle.setTypeface(tf);
        altField.setTypeface(tf);
        glideTitle.setTypeface(tf);
        glideField.setTypeface(tf);
        hrField.setTypeface(tf);
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
    	unregisterReceiver(mPolarBroadcastReceiver);
    }
    
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	Intent myIntent;
    	
    	switch(keyCode) {
    	case KeyEvent.KEYCODE_DPAD_LEFT:
    		myIntent = new Intent(this, DashboardOneActivity.class);
            startActivity(myIntent);
            finish();
    		return true;
    		
    	case KeyEvent.KEYCODE_DPAD_RIGHT:
    		myIntent = new Intent(this, CalibrationActivity.class);
    		startActivity(myIntent);
    		finish();
    		return true;
    		
    	default:
    		return super.onKeyUp(keyCode, event);
    	}
    }
    
    private void updateFields(Bundle data) {
    	Bundle speedBundle = data.getBundle("SPEED_BUNDLE");
    	Bundle altBundle = data.getBundle("ALTITUDE_BUNDLE");
    	
    	if(debug) {
    		Random r = new Random();
    		speedBundle.putFloat("HorzSpeed", r.nextFloat() * 100);
    		speedBundle.putFloat("VertSpeed", r.nextFloat() * 100);
    		speedBundle.putFloat("Speed", r.nextFloat() * 300);
    		altBundle.putFloat("Alt", r.nextFloat() * 10000);
    		altBundle.putInt("HeightOffsetN", 600);
    	}
    	
    	updateGlide(speedBundle);
    	
    	updateSpeed(speedBundle);
    	
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
			altField.setText("...");
		} else {
			altField.setText(df.format(altAdjusted)+"m");
			if(units == ReconSettingsUtil.RECON_UINTS_METRIC) {
				altField.setText(df.format(altAdjusted)+"m");
			} else {
				// write imperial units
				altField.setText(df.format(ConversionUtil.metersToFeet(altAdjusted)) + "ft");
			}
		}
    }
    
    private void updateGlide(Bundle speedBundle) {
    	DecimalFormat df = new DecimalFormat();
    	df.setMaximumFractionDigits(1);
    	df.setMinimumFractionDigits(1);
    	
    	// Glide
    	float horzSpeed = speedBundle.getFloat("HorzSpeed");
    	float vertSpeed = speedBundle.getFloat("VertSpeed");

    	// Speed
    	float speed = speedBundle.getFloat("Speed");
    	
    	if(!(vertSpeed == INVALID_SPEED || horzSpeed == INVALID_SPEED || vertSpeed == 0) && speed > 20) {
    		float glideRatio = Math.abs(horzSpeed / vertSpeed);
    		
	    	if(glideRatio > 10)
	    		glideField.setText("10.0");
	    	else
	    		glideField.setText(df.format(glideRatio));
	    	
	    	if(glideRatio > 3) {
	    		glideView.setBackgroundResource(R.drawable.border_green);
	    		glideIcon.setImageResource(R.drawable.glide_green_20);
	    		glideTitle.setTextColor(0xFF19a94b);
	    	} else if (glideRatio < 3 && glideRatio > 2) {
	    		glideView.setBackgroundResource(R.drawable.border_yellow);
	    		glideIcon.setImageResource(R.drawable.glide_yellow_20);
	    		glideTitle.setTextColor(0xFFFCD116);
	    	} else {
	    		glideView.setBackgroundResource(R.drawable.border_red);
	    		glideIcon.setImageResource(R.drawable.glide_red_20);
	    		glideTitle.setTextColor(Color.RED);
	    	}
	    	
    	} else {
    		glideField.setText("--");
    		glideView.setBackgroundResource(R.drawable.border_grey);
    		glideTitle.setTextColor(0XFFCCCCCC);
    		glideIcon.setImageResource(R.drawable.glide_grey_20);
    	}
    }
    
    private void updateSpeed(Bundle speedBundle) {
    	// Speed
    	float speed = speedBundle.getFloat("Speed");
    	
    	DecimalFormat df = new DecimalFormat();
    	df.setMinimumFractionDigits(0);
    	df.setMaximumFractionDigits(0);
    	
    	// Set unit
    	if(units == ReconSettingsUtil.RECON_UINTS_IMPERIAL) {
			speedUnitField.setText("mph");
		} else {
			speedUnitField.setText("km/h");
		}
    	
    	// Set speed fields
    	if(speed != INVALID_SPEED) {
    		speedField.setText(df.format(speed));
    		
    		if(units == ReconSettingsUtil.RECON_UINTS_IMPERIAL) {
    			speedField.setText(df.format(ConversionUtil.kmsToMiles(speed)));
    		} else {
    			speedField.setText(df.format(speed));
    		}
    		
    		float pctOfTopSpeed = speed / GAUGE_TOP_END;
    		
    		if (Float.compare(pctOfTopSpeed, (float) 0.0625) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_0_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.125) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_1_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.1875) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_2_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.25) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_3_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.3125) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_4_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.375) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_5_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.4375) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_6_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.5) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_7_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.5625) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_8_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.625) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_9_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.6875) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_10_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.75) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_11_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.8125) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_12_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.875) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_13_lrg);
    		} else if (Float.compare(pctOfTopSpeed, (float) 0.9375) < 0) {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_14_lrg);
    		} else {
    			gaugeImageView.setImageResource(R.drawable.speed_gauge_15_lrg);
    		}
    	} else {
    		speedField.setText("--");
    		gaugeImageView.setImageResource(R.drawable.speed_gauge_0_lrg);
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

	class PolarBroadcastReceiver extends BroadcastReceiver {
	
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals("POLAR_BROADCAST_HR")) {
				Bundle hrBundle = intent.getExtras().getBundle("POLAR_BUNDLE");
				
				int connectionState = hrBundle.getInt("ConnectionState");
				if(connectionState == PolarHRStatus.STATE_CONNECTED	&& hrBundle.getInt("AvgHR") > 0)
					hrField.setText(Integer.toString(hrBundle.getInt("AvgHR")));
				else if(connectionState == PolarHRStatus.STATE_CONNECTING) {
					hrField.setText("...");
				}
				
				else if(connectionState == PolarHRStatus.STATE_NONE) {
					hrField.setText("--");
				}
			}
		}
		
	}
}