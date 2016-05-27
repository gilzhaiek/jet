
package com.reconinstruments.jetsensorconnect;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SoundEffectConstants;
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

import com.reconinstruments.antplus.AntPlusManager;
import com.reconinstruments.antplus.AntPlusProfileManager;
import com.reconinstruments.antplus.AntPlusSensor;
import com.reconinstruments.antplus.BikeSensor;
import com.reconinstruments.antplus.PowerSensor;
import com.reconinstruments.antplus.AntService;
import com.reconinstruments.antplus.BWManager;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.commonwidgets.FeedbackDialog.ICONTYPE;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

public class WheelSizeOverlay extends DialogFragment {

    private SensorConnectActivity mActivity;
    private TextSwitcher firstNumSwitcher;
    private TextSwitcher secondNumSwitcher;
    private TextSwitcher thirdNumSwitcher;
    private TextSwitcher forthNumSwitcher;

    private LinearLayout firstNumLayout;
    private LinearLayout secondNumLayout;
    private LinearLayout thirdNumLayout;
    private LinearLayout forthNumLayout;

    private int firstNum = 0;
    private int secondNum = 0;
    private int thirdNum = 0;
    private int forthNum = 0;

    private int focus;

    private AntPlusSensor mDevice;
    private View mView;
    private boolean mSingleStep; // there is next step on adding power device

    public WheelSizeOverlay(SensorConnectActivity activity, AntPlusSensor device, boolean singleStep) {
        mActivity = activity;
        mDevice = device;
        mSingleStep = singleStep;
        firstNum = ((BikeSensor)device).getCircumference() / 1000;
        secondNum = (((BikeSensor)device).getCircumference() % 1000) / 100;
        thirdNum = (((BikeSensor)device).getCircumference() - firstNum * 1000 - secondNum * 100) / 10;
        forthNum = (((BikeSensor)device).getCircumference() - firstNum * 1000 - secondNum * 100) % 10;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wheel_size, container);

        firstNumSwitcher = (TextSwitcher) view.findViewById(R.id.first_num_switcher);
        secondNumSwitcher = (TextSwitcher) view.findViewById(R.id.second_num_switcher);
        thirdNumSwitcher = (TextSwitcher) view.findViewById(R.id.third_num_switcher);
        forthNumSwitcher = (TextSwitcher) view.findViewById(R.id.forth_num_switcher);

        ImageView iv;

        firstNumLayout = (LinearLayout) view.findViewById(R.id.first_num);
        iv = (ImageView) firstNumLayout.findViewById(R.id.first_num_up);
        iv = (ImageView) firstNumLayout.findViewById(R.id.first_num_down);

        secondNumLayout = (LinearLayout) view.findViewById(R.id.second_num);
        iv = (ImageView) secondNumLayout.findViewById(R.id.second_num_up);
        iv = (ImageView) secondNumLayout.findViewById(R.id.second_num_down);

        thirdNumLayout = (LinearLayout) view.findViewById(R.id.third_num);
        iv = (ImageView) thirdNumLayout.findViewById(R.id.third_num_up);
        iv = (ImageView) thirdNumLayout.findViewById(R.id.third_num_down);

        forthNumLayout = (LinearLayout) view.findViewById(R.id.forth_num);
        iv = (ImageView) forthNumLayout.findViewById(R.id.forth_num_up);
        iv = (ImageView) forthNumLayout.findViewById(R.id.forth_num_down);

        focus = 0;

        firstNumLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                LinearLayout ll = (LinearLayout) v;

                ImageView up = (ImageView) ll.findViewById(R.id.first_num_up);
                ImageView down = (ImageView) ll.findViewById(R.id.first_num_down);
                if (hasFocus) {
                    focus = 0;
                    up.setVisibility(View.VISIBLE);
                    down.setVisibility(View.VISIBLE);
                    ((TextView)firstNumSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
                }
                else {
                    ((TextView)firstNumSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_up));
                    up.setVisibility(View.GONE);
                    down.setVisibility(View.GONE);
                }
            }
        });

        secondNumLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                LinearLayout ll = (LinearLayout) v;

                ImageView up = (ImageView) ll.findViewById(R.id.second_num_up);
                ImageView down = (ImageView) ll.findViewById(R.id.second_num_down);
                if (hasFocus) {
                    focus = 1;
                    ((TextView)secondNumSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
                    up.setVisibility(View.VISIBLE);
                    down.setVisibility(View.VISIBLE);
                }
                else {
                    ((TextView)secondNumSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_up));
                    up.setVisibility(View.GONE);
                    down.setVisibility(View.GONE);
                }
            }
        });

        thirdNumLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                LinearLayout ll = (LinearLayout) v;

                ImageView up = (ImageView) ll.findViewById(R.id.third_num_up);
                ImageView down = (ImageView) ll.findViewById(R.id.third_num_down);

                if (hasFocus) {
                    focus = 2;
                    ((TextView)thirdNumSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
                    up.setVisibility(View.VISIBLE);
                    down.setVisibility(View.VISIBLE);
                }
                else {
                    ((TextView)thirdNumSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_up));
                    up.setVisibility(View.GONE);
                    down.setVisibility(View.GONE);
                }
            }
        });

        forthNumLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                LinearLayout ll = (LinearLayout) v;

                ImageView up = (ImageView) ll.findViewById(R.id.forth_num_up);
                ImageView down = (ImageView) ll.findViewById(R.id.forth_num_down);

                if (hasFocus) {
                    focus = 3;
                    ((TextView)forthNumSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
                    up.setVisibility(View.VISIBLE);
                    down.setVisibility(View.VISIBLE);
                }
                else {
                    ((TextView)forthNumSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_up));
                    up.setVisibility(View.GONE);
                    down.setVisibility(View.GONE);
                }
            }
        });

        update();

        firstNumLayout.requestFocus();
        ((TextView)firstNumSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));

        setupKeyListener();
        view.setBackgroundColor(getResources().getColor(R.color.recon_overlay_bg));
        mView = view;
        return view;
    }

    private void update() {
        ((TextView)firstNumSwitcher.getChildAt(0)).setText(String.valueOf(firstNum));
        ((TextView)secondNumSwitcher.getChildAt(0)).setText(String.valueOf(secondNum));
        ((TextView)thirdNumSwitcher.getChildAt(0)).setText(String.valueOf(thirdNum));
        ((TextView)forthNumSwitcher.getChildAt(0)).setText(String.valueOf(forthNum));
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
                    mActivity.reloadSensors();
                    getDialog().dismiss();
                    if(mDevice.getProfile() == BWManager.EXTRA_BIKE_POWER_PROFILE && !mSingleStep){
                        mActivity.startActivity(new Intent("com.reconinstruments.jetsensorconnect.calibrate"));
                    }
                    return true;
                  }
                if (event.getAction() != KeyEvent.ACTION_UP) {
                    return true;
                }

                switch (keyCode){
                case KeyEvent.KEYCODE_DPAD_UP:
                    mView.playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
                    switch (focus){
                        case 0:
                            firstNum ++;
                            if(firstNum > 9){
                                firstNum = 0;
                            }
                            update();
                            break;
                        case 1:
                            secondNum ++;
                            if(secondNum > 9){
                                secondNum = 0;
                            }
                            update();
                            break;
                        case 2:
                            thirdNum ++;
                            if(thirdNum > 9){
                                thirdNum = 0;
                            }
                            update();
                            break;
                        case 3:
                            forthNum ++;
                            if(forthNum > 9){
                                forthNum = 0;
                            }
                            update();
                            break;
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    mView.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN);
                    switch (focus) {
                        case 0:
                            firstNum --;
                            if(firstNum < 0){
                                firstNum = 9;
                            }
                            update();
                            break;
                        case 1:
                            secondNum --;
                            if(secondNum < 0){
                                secondNum = 9;
                            }
                            update();
                            break;
                        case 2:
                            thirdNum --;
                            if(thirdNum < 0){
                                thirdNum = 9;
                            }
                            update();
                            break;
                        case 3:
                            forthNum --;
                            if(forthNum < 0){
                                forthNum = 9;
                            }
                            update();
                            break;
                    }
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    
                    ((BikeSensor)mDevice).setCircumference(Integer.parseInt("" + firstNum + "" + secondNum + "" + thirdNum + "" + forthNum));
                    
                    //update the speed capability device as well, these device should share the same circumference
                    final AntPlusManager antPlusManager = AntPlusManager.getInstance(mActivity.getApplicationContext());
                    AntPlusSensor sensor = antPlusManager.getMostRecent(AntPlusProfileManager.EXTRA_BIKE_SPEED_PROFILE);
                    if(sensor != null) ((BikeSensor)sensor).setCircumference(Integer.parseInt("" + firstNum + "" + secondNum + "" + thirdNum + "" + forthNum));
                    sensor = antPlusManager.getMostRecent(AntPlusProfileManager.EXTRA_BIKE_SPEED_CADENCE_PROFILE);
                    if(sensor != null) ((BikeSensor)sensor).setCircumference(Integer.parseInt("" + firstNum + "" + secondNum + "" + thirdNum + "" + forthNum));
                    sensor = antPlusManager.getMostRecent(AntPlusProfileManager.EXTRA_BIKE_POWER_PROFILE);
                    if(sensor != null && ((PowerSensor)sensor).hasCombinedSpeed()) ((PowerSensor)sensor).setCircumference(Integer.parseInt("" + firstNum + "" + secondNum + "" + thirdNum + "" + forthNum));
                    
                    antPlusManager.persistRememberedDevice();
                    
                    Intent i = new Intent(AntService.ACTION_ANT_SERVICE);
                    i.putExtra(AntService.EXTRA_SENSOR_PROFILE, mDevice.getProfile());
                    i.putExtra(AntService.EXTRA_ANT_SERVICE_CONNECT, true);
                    i.putExtra(AntService.EXTRA_SHOW_PASSIVE_NOTIFICATION, false);
                    i.putExtra(AntService.EXTRA_SENSOR_ID, mDevice.getDeviceNumber());
                    mActivity.getApplicationContext().startService(i);

                    FeedbackDialog.showDialog(mActivity, "Wheel Size Set", null, FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER);
                    new CountDownTimer(3 * 1000, 1000) {
                        public void onTick(long millisUntilFinished) {}
                        public void onFinish() {
                            mActivity.reloadSensors();
                            FeedbackDialog.dismissDialog(mActivity);
                            getDialog().dismiss();
                            if(mDevice.getProfile() == BWManager.EXTRA_BIKE_POWER_PROFILE && !mSingleStep){
                                mActivity.startActivity(new Intent("com.reconinstruments.jetsensorconnect.calibrate"));
                            }
                        }
                    }.start();
                    
                    
                    return true;

                }
                return false;
            }
        });
    }
}
