package com.reconinstruments.dashlauncher.settings;

import android.app.Activity;
import android.limo_i2c.LimoI2CNative;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;

public class BrightnessActivity extends Activity {


	static final int PER_STEP_VALUE = 16;

	SeekBar mBrightnessBar = null;
	// Temporary Fix
	//SeekBar mContrastBar = null;
	int mPreviousBrightness = 0;
	int mCurrentBrightness = 0;

	TextView mTitle = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_brightness_layout);

		mBrightnessBar = (SeekBar)this.findViewById(R.id.brightness_bar);
		//mContrastBar = (SeekBar)this.findViewById(R.id.contrast_bar);

		android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL );
		try
		{
			mPreviousBrightness = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
			int brightnessStep = convertToSteps( mPreviousBrightness );
			mBrightnessBar.setProgress(brightnessStep);

		}
		catch( SettingNotFoundException e )
		{

		}

		mBrightnessBar.setOnSeekBarChangeListener(mDisplayChangedListener);
		//mContrastBar.setOnSeekBarChangeListener(mDisplayChangedListener);

	}

	int convertToSteps( int value )
	{
		/*
    	int step = value/PER_STEP_VALUE;
    	step = step >= 1 ? step - 1 : 0;

    	return step;
		 */
		switch(value) {
		case 20: return 0;
		case 32: return 1;
		case 44: return 2;
		case 56: return 3;
		case 68: return 4;
		case 80: return 5;
		case 92: return 6;
		case 104: return 7;
		case 116: return 8;
		case 128: return 9;
		case 140: return 10;
		case 152: return 11;
		case 164: return 12;
		case 197: return 13;
		case 230: return 14;
		default: return 15;
		}
	}

	int convertFromSteps( int step )
	{
		//int value = (step +1)*PER_STEP_VALUE;

		switch(step) {
		case 0: return 20;
		case 1: return 32;
		case 2: return 44;
		case 3: return 56;
		case 4: return 68;
		case 5: return 80;
		case 6: return 92;
		case 7: return 104;
		case 8: return 116;
		case 9: return 128;
		case 10: return 140;
		case 11: return 152;
		case 12: return 164;
		case 13: return 197;
		case 14: return 230;
		default: return 255;
		}
	}

	
	SeekBar.OnSeekBarChangeListener mDisplayChangedListener = new SeekBar.OnSeekBarChangeListener() {

		public void onStopTrackingTouch(SeekBar seekBar) {

		}

		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
		{
			if( fromUser )
			{
				if( seekBar.getId() == R.id.brightness_bar )
				{
					Log.v("SetDisplayActivity", "progress: " + progress);
					mCurrentBrightness = convertFromSteps( progress );
					Log.v("SetDisplayActivity", "brightness: " + mCurrentBrightness);
					android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, mCurrentBrightness);
					//LimoI2CNative.LimoI2CWrite(3, 0x34, 0x1e, brightness );

					//force the window to be refreshed
					WindowManager.LayoutParams lp = getWindow().getAttributes();
					lp.screenBrightness = (float)mCurrentBrightness/(float)255;
					getWindow().setAttributes(lp);
				}
				else		//constrast adjustment
				{		
					int contrast = convertFromSteps( progress );
					LimoI2CNative.LimoI2CWrite(3, 0x34, 0x14, contrast );
				}

			}

		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode){
		
		case KeyEvent.KEYCODE_DPAD_CENTER :
			// Make sure this activity doesn't close onKeyDown, only onKeyUp
			// This is because if we close onKeyDown, then the menu view behind
			// will receiver onKeyUp and close itself.
			android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, mCurrentBrightness);
			finish();
			return true;
		
		}
		return super.onKeyDown(keyCode, event);
	}


}
