//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.jetapplauncher.settings;

import com.reconinstruments.jetapplauncher.R;

import android.app.Activity;
//import android.limo_i2c.LimoI2CNative;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_brightness_layout);


        mBrightnessBar = (SeekBar)this.findViewById(R.id.brightness_bar);
        //mContrastBar = (SeekBar)this.findViewById(R.id.contrast_bar);

        android.provider.Settings
	    .System.putInt(getContentResolver(),
			   android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
			   android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        try {
	    mPreviousBrightness = android.provider.Settings
		.System.getInt(getContentResolver(),
			       android.provider.Settings.System.SCREEN_BRIGHTNESS);
	    int brightnessStep = convertToSteps(mPreviousBrightness);
	    mBrightnessBar.setProgress(brightnessStep);

	}
        catch(SettingNotFoundException e) {

	}

        mBrightnessBar.setOnSeekBarChangeListener(mDisplayChangedListener);
        //mContrastBar.setOnSeekBarChangeListener(mDisplayChangedListener);

        Window mainWindow = getWindow();
        int dimAmt = 60;
        mainWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = mainWindow.getAttributes();
        params.dimAmount = dimAmt / 100f;
        mainWindow.setAttributes(params);
    }

    int convertToSteps(int value) {
        /*
          int step = value/PER_STEP_VALUE;
          step = step >= 1 ? step - 1 : 0;

          return step;
        */
	// We use if values with (<=) instead of a switch statement so
	// that if the value is set to something in between we are not
	// screwed up.
	if (value <= 20) return 0;
	if (value <= 32) return 1;
	if (value <= 44) return 2;
	if (value <= 56) return 3;
	if (value <= 68) return 4;
	if (value <= 80) return 5;
	if (value <= 92) return 6;
	if (value <= 104) return 7;
	if (value <= 116) return 8;
	if (value <= 128) return 9;
	if (value <= 140) return 10;
	if (value <= 152) return 11;
	if (value <= 164) return 12;
	if (value <= 197) return 13;
	if (value <= 230) return 14;
	else return 15;//if (value <= 255) return 15;


    }

    int convertFromSteps(int step) {
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
        case 15: return 255;
        default: return 104;
        }
    }

        
    SeekBar.OnSeekBarChangeListener mDisplayChangedListener =
	new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
		return;
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
		return;
            }
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
		    if(seekBar.getId() == R.id.brightness_bar) {
			//Log.v("SetDisplayActivity", "progress: " + progress);
			mCurrentBrightness = convertFromSteps(progress);
			//Log.v("SetDisplayActivity", "brightness: " + mCurrentBrightness);
			android.provider.Settings.System
			    .putInt(getContentResolver(),
				    android.provider.Settings.System.SCREEN_BRIGHTNESS,
				    mCurrentBrightness);
			// Potentially add IPowerManager and etc.

			//LimoI2CNative.LimoI2CWrite(3, 0x34, 0x1e, brightness);

			//force the window to be refreshed
			WindowManager.LayoutParams lp = getWindow().getAttributes();
			lp.screenBrightness = (float)mCurrentBrightness/(float)255;
			getWindow().setAttributes(lp);
		    }
		    else {            //constrast adjustment

			int contrast = convertFromSteps(progress);
			//LimoI2CNative.LimoI2CWrite(3, 0x34, 0x14, contrast);
		    }
		}
            }
        };
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER :
            // Make sure this activity doesn't close onKeyDown, only onKeyUp
            // This is because if we close onKeyDown, then the menu view behind
            // will receiver onKeyUp and close itself.
            finish();
            return true;
                
        }
        return super.onKeyDown(keyCode, event);
    }
}
