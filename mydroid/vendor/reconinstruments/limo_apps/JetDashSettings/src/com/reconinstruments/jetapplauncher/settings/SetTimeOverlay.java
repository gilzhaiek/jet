
package com.reconinstruments.jetapplauncher.settings;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextSwitcher;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.SettingsUtil;

public class SetTimeOverlay extends DialogFragment {

    private TimeActivity mActivity;
    private TextSwitcher hourSwitcher;
    private TextSwitcher minSwitcher;
    private TextSwitcher meriSwitcher;

    private LinearLayout hourLayout;
    private LinearLayout minLayout;
    private LinearLayout meriLayout;

    private boolean mIs24;

    private int hr;
    private int baseHr;
    private int baseMnt;

    private int focus;

    public SetTimeOverlay(TimeActivity activity) {
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
        View view = inflater.inflate(R.layout.setting_set_time_layout, container);

        hourSwitcher = (TextSwitcher) view.findViewById(R.id.hour_switcher);
        minSwitcher = (TextSwitcher) view.findViewById(R.id.min_switcher);
        meriSwitcher = (TextSwitcher) view.findViewById(R.id.meri_switcher);

        ImageView iv;
        
        hourLayout = (LinearLayout) view.findViewById(R.id.setting_time_hour);
        iv = (ImageView) hourLayout.findViewById(R.id.hour_up);
        iv.setAlpha(100);
        iv = (ImageView) hourLayout.findViewById(R.id.hour_down);
        iv.setAlpha(100);
        
        minLayout = (LinearLayout) view.findViewById(R.id.setting_time_min);
        iv = (ImageView) minLayout.findViewById(R.id.min_up);
        iv.setAlpha(100);
        iv = (ImageView) minLayout.findViewById(R.id.min_down);
        iv.setAlpha(100);
        
        meriLayout = (LinearLayout) view.findViewById(R.id.setting_time_meridiem);
        iv = (ImageView) meriLayout.findViewById(R.id.meri_up);
        iv.setAlpha(100);
        iv = (ImageView) meriLayout.findViewById(R.id.meri_down);
        iv.setAlpha(100);

        focus = 0;

        hourLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                LinearLayout ll = (LinearLayout) v;

                ImageView up = (ImageView) ll.findViewById(R.id.hour_up);
                ImageView down = (ImageView) ll.findViewById(R.id.hour_down);
                if (hasFocus) {
                    focus = 0;
                    up.setAlpha(255);
                    down.setAlpha(255);
                    up.setVisibility(View.VISIBLE);
                    down.setVisibility(View.VISIBLE);
                    ((TextView)hourSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
                }
                else {
                    ((TextView)hourSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_up));
                    up.setAlpha(100);
                    down.setAlpha(100);
                    up.setVisibility(View.GONE);
                    down.setVisibility(View.GONE);
                }
            }
        });

        minLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                LinearLayout ll = (LinearLayout) v;

                ImageView up = (ImageView) ll.findViewById(R.id.min_up);
                ImageView down = (ImageView) ll.findViewById(R.id.min_down);
                if (hasFocus) {
                    focus = 1;
                    ((TextView)minSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
                    up.setAlpha(255);
                    down.setAlpha(255);
                    up.setVisibility(View.VISIBLE);
                    down.setVisibility(View.VISIBLE);
                }
                else {
                    ((TextView)minSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_up));
                    up.setAlpha(100);
                    down.setAlpha(100);
                    up.setVisibility(View.GONE);
                    down.setVisibility(View.GONE);
                }
            }
        });

        meriLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                LinearLayout ll = (LinearLayout) v;

                ImageView up = (ImageView) ll.findViewById(R.id.meri_up);
                ImageView down = (ImageView) ll.findViewById(R.id.meri_down);

                if (hasFocus) {
                    focus = 2;
                    ((TextView)meriSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
                    up.setVisibility(View.VISIBLE);
                    down.setVisibility(View.VISIBLE);
                    up.setAlpha(255);
                    down.setAlpha(255);
                }
                else {
                    ((TextView)meriSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_up));
                    up.setAlpha(100);
                    down.setAlpha(100);
                    up.setVisibility(View.GONE);
                    down.setVisibility(View.GONE);
                }
            }
        });


        mIs24 = DateFormat.is24HourFormat(mActivity);
        if (mIs24){
            meriLayout.setVisibility(View.GONE);
        }
        
        initTime(mActivity);
        update();

        hourLayout.requestFocus();
        ((TextView)hourSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
       
        setupKeyListener();
        
        return view;
    }

    private void update() {
        DecimalFormat df = new DecimalFormat();
        df.setMinimumIntegerDigits(2);
        if (mIs24){
            if(hr < 10){
                ((TextView)hourSwitcher.getChildAt(0)).setText("0" + String.valueOf(hr));
            }else{
                ((TextView)hourSwitcher.getChildAt(0)).setText(String.valueOf(hr));
            }
            
//          hourSwitcher.setText(String.valueOf(hr));
        }
        else{
            if(baseHr < 10){
                ((TextView)hourSwitcher.getChildAt(0)).setText("0" + String.valueOf(baseHr));
            }else{
                ((TextView)hourSwitcher.getChildAt(0)).setText(String.valueOf(baseHr));
            }
            
//          hourSwitcher.setText(String.valueOf(baseHr));
            if (hr >= 12){
                ((TextView)meriSwitcher.getChildAt(0)).setText("PM");
//              meriSwitcher.setText("PM");
            }
            else{
                ((TextView)meriSwitcher.getChildAt(0)).setText("AM");
//              meriSwitcher.setText("AM");
            }
        }
        if(baseMnt < 10){
            ((TextView)minSwitcher.getChildAt(0)).setText(String.valueOf("0" + baseMnt));
        }else{
            ((TextView)minSwitcher.getChildAt(0)).setText(String.valueOf(baseMnt));
        }
        
//      minSwitcher.setText(df.format(baseMnt));

    }


    private void initTime(Context context) 
    {
        hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        baseHr = Calendar.getInstance().get(Calendar.HOUR);
        if(baseHr == 0){
            baseHr = 12;
        }
        baseMnt = Calendar.getInstance().get(Calendar.MINUTE);
    }



    private void setTime(int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        long when = c.getTimeInMillis();

        boolean set = false;
        if (when / 1000 < Integer.MAX_VALUE) {
            set = SystemClock.setCurrentTimeMillis(when);
        }              

        Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
        mActivity.sendBroadcast(timeChanged);

        SettingsUtil.setTimeAuto(mActivity.getBaseContext(), true); // turn off auto GPS
        // We don't need to call timeUpdated() here because the TIME_CHANGED
        // broadcast is sent by the AlarmManager as a side effect of setting the
        // SystemClock time.
    }
    
    private void setupKeyListener() {
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                //ignore left and right event.
                if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_BACK) { // if back button is presssed
                    getDialog().dismiss();
                    return true;
                  }
                if (event.getAction() != KeyEvent.ACTION_UP) {
                    return true;
                }

                Log.v("focus", ""+focus);
                switch (keyCode){
                case KeyEvent.KEYCODE_DPAD_UP:
                    switch (focus){
                    case 0:
                        if (hr < 23){
                            hr++;
                            if (hr > 12)
                                baseHr = hr - 12;
                            else
                                baseHr = hr;
                        }
                        else{
                            hr -= 23;
                            baseHr = 12;
                        }
                        update();
                        break;
                    case 1:
                        if (baseMnt < 59)
                            baseMnt++;
                        else
                            baseMnt -= 59;
                        update();
                        break;
                    case 2:
                        if (((TextView)meriSwitcher.getChildAt(0)).getText().toString().equalsIgnoreCase("pm")){
                            //meriSwitcher.setText("AM");
                            ((TextView)meriSwitcher.getChildAt(0)).setText("AM");
                            hr -= 12;
                        }
                        else{
                            ((TextView)meriSwitcher.getChildAt(0)).setText("PM");
                            //meriSwitcher.setText("PM");
                            hr += 12;
                        }
                        update();
                        break;
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    Log.v("focus", ""+focus);
                    switch (focus){
                    case 0:
                        if (hr > 0){
                            hr--;
                            if (hr > 12)
                                baseHr = hr - 12;
                            else if(hr==0)
                                baseHr = 12;
                            else
                                baseHr = hr;
                        }
                        else{
                            hr = 23;
                            baseHr = 11;
                        }
                        update();
                        break;
                    case 1:
                        if (baseMnt > 0)
                            baseMnt--;
                        else
                            baseMnt = 59;
                        update();
                        break;
                    case 2:
                        if (((TextView)meriSwitcher.getChildAt(0)).getText().toString().equalsIgnoreCase("pm")){
                            //meriSwitcher.setText("AM");
                            ((TextView)meriSwitcher.getChildAt(0)).setText("AM");
                            hr -= 12;
                        }
                        else{
                            //meriSwitcher.setText("PM");
                            ((TextView)meriSwitcher.getChildAt(0)).setText("PM");
                            hr += 12;
                        }
                        update();
                        break;
                    }
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    setTime(hr,baseMnt);
                    getDialog().dismiss();
//                    mActivity.updateTimeState();
                    return true;

                }
                return false;
            }
        });
    }
}
