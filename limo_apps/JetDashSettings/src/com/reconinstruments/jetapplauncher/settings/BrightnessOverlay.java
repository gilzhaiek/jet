
package com.reconinstruments.jetapplauncher.settings;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.SeekBar;

import com.reconinstruments.jetapplauncher.R;

public class BrightnessOverlay extends DialogFragment {

    private SeekBar mBrightnessBar = null;
    private int mPreviousBrightness = 0;
    private int mCurrentBrightness = 0;
    
    private Activity mActivity;

    public BrightnessOverlay(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        Window window = getDialog().getWindow();
        LayoutParams params = window.getAttributes();
        params.alpha = 0.8f;
        window.setAttributes((android.view.WindowManager.LayoutParams) params);
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow()
                .getAttributes().windowAnimations = R.style.dialog_animation;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setting_brightness_layout, container);
        mBrightnessBar = (SeekBar) view.findViewById(R.id.brightness_bar);
        android.provider.Settings.System.putInt(mActivity.getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        try {
            mPreviousBrightness = android.provider.Settings
                    .System.getInt(mActivity.getContentResolver(),
                            android.provider.Settings.System.SCREEN_BRIGHTNESS);
            int brightnessStep = convertToSteps(mPreviousBrightness);
            mBrightnessBar.setProgress(brightnessStep);
        } catch (SettingNotFoundException e) {
            //do nothing
        }
        mBrightnessBar.setOnSeekBarChangeListener(mDisplayChangedListener);
        return view;
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
                    android.provider.Settings.System
                            .putInt(mActivity.getContentResolver(),
                                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                                    mCurrentBrightness);

                    // force the window to be refreshed
                    WindowManager.LayoutParams lp = getDialog().getWindow().getAttributes();
                    lp.screenBrightness = (float) mCurrentBrightness / (float) 255;
                    getDialog().getWindow().setAttributes(lp);
                }
                else { // constrast adjustment
                   int contrast = convertFromSteps(progress);
                }
            }
        }
    };
}
