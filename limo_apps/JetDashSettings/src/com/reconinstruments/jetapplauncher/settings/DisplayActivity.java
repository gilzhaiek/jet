package com.reconinstruments.jetapplauncher.settings;

import com.reconinstruments.jetapplauncher.R;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.SeekBar;

public class DisplayActivity extends Activity {
	
	private SeekBar mBrightnessBar = null;
    private int mPreviousBrightness = 0;
    private int mCurrentBrightness = 0;
    public static final int MAX_BRIGHTNESS = 255;
    
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_brightness_layout);
        
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = LayoutParams.MATCH_PARENT;
        params.height = LayoutParams.MATCH_PARENT;
        window.setAttributes(params);
        window.getAttributes().windowAnimations = R.style.dialog_animation;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        mBrightnessBar = (SeekBar) findViewById(R.id.brightness_bar);
        android.provider.Settings.System.putInt(getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        try {
            mPreviousBrightness = android.provider.Settings
                    .System.getInt(getContentResolver(),
                            android.provider.Settings.System.SCREEN_BRIGHTNESS);
            int brightnessStep = convertToSteps(mPreviousBrightness);
            mBrightnessBar.setProgress(brightnessStep);
        } catch (SettingNotFoundException e) {
            //do nothing
        }
        mBrightnessBar.setOnSeekBarChangeListener(mDisplayChangedListener);
        
	}

    @Override
    public void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.fade_slide_in_bottom,0);
    }

    @Override
    public void onBackPressed() {
        refreshWindow(mPreviousBrightness);
        super.onBackPressed();
    }

    private void refreshWindow(int brightness) {
        //revert brightness to previous state
        android.provider.Settings.System
                .putInt(getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS,
                        brightness);

        // force the window to be refreshed
        LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = (float) brightness / (float) MAX_BRIGHTNESS;
        getWindow().setAttributes(lp);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent keyEvent){
        switch (keyCode){
            case KeyEvent.KEYCODE_DPAD_CENTER:
                finish();
                return true;
        }
        return super.onKeyUp(keyCode, keyEvent);
    }
    
	private int convertToSteps(int value) {
        if (value <= 20)
            return 0;
        if (value <= 32)
            return 1;
        if (value <= 44)
            return 2;
        if (value <= 56)
            return 3;
        if (value <= 68)
            return 4;
        if (value <= 80)
            return 5;
        if (value <= 92)
            return 6;
        if (value <= 104)
            return 7;
        if (value <= 116)
            return 8;
        if (value <= 128)
            return 9;
        if (value <= 140)
            return 10;
        if (value <= 152)
            return 11;
        if (value <= 164)
            return 12;
        if (value <= 197)
            return 13;
        if (value <= 230)
            return 14;
        else
            return 15;// if (value <= 255) return 15;
    }

    private int convertFromSteps(int step) {
        switch (step) {
            case 0:
                return 20;
            case 1:
                return 32;
            case 2:
                return 44;
            case 3:
                return 56;
            case 4:
                return 68;
            case 5:
                return 80;
            case 6:
                return 92;
            case 7:
                return 104;
            case 8:
                return 116;
            case 9:
                return 128;
            case 10:
                return 140;
            case 11:
                return 152;
            case 12:
                return 164;
            case 13:
                return 197;
            case 14:
                return 230;
            case 15:
                return 255;
            default:
                return 104;
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
            if (fromUser) {
                if (seekBar.getId() == R.id.brightness_bar) {
                    mCurrentBrightness = convertFromSteps(progress);
                    refreshWindow(mCurrentBrightness);
                }
                else { // constrast adjustment
                   int contrast = convertFromSteps(progress);
                }
            }
        }
    };
}
