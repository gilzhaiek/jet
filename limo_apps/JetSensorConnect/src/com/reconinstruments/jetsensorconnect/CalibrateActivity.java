
package com.reconinstruments.jetsensorconnect;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

import com.reconinstruments.antplus.AntPlusManager;
import com.reconinstruments.antplus.AntPlusSensor;
import com.reconinstruments.antplus.AntService;
import com.reconinstruments.antplus.BWManager;
import com.reconinstruments.antplus.PowerSensor;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.commonwidgets.FeedbackDialog.ICONTYPE;
import com.reconinstruments.utils.SettingsUtil;

/**
 * <code>CalibrateActivity</code> deal with the ant+ power sensor calibration.
 */
public class CalibrateActivity extends FragmentActivity {

    private static final String TAG = CalibrateActivity.class.getSimpleName();

    private TextView mTitleTV;
    private TextView mBody1TV;
    private TextView mBody2TV;
    private TextView mButtonTV;
    private ImageView mButtonIV;
    
    private Handler mHandler = new Handler(); // a handler to control delayed runnable
    private String mTorque = null; // latest torque
    private String mOffset = null; // latest zero offset
    private boolean mShown = false; //indicates if the overlay shown or not, if shown, don't need to show again.

    private boolean mFromSettings = true; //indicates if the request comes from settings or not, it may come from dashboard
    private PowerSensor mSensor;
    
    public static final String EXTRA_CALIBRATION_FROM_SETTINGS="calibration_from_settings";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkIfNeedsCalibrate(); // check if needs calibrate
        
        mFromSettings = getIntent().getBooleanExtra(EXTRA_CALIBRATION_FROM_SETTINGS, true);
        if(mFromSettings){
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.title_two_lines_button);
            mTitleTV = (TextView) findViewById(R.id.title);
            mBody1TV = (TextView) findViewById(R.id.body1);
            mBody2TV = (TextView) findViewById(R.id.body2);
            mButtonTV = (TextView) findViewById(R.id.text_view);
            mButtonIV = (ImageView) findViewById(R.id.image_view);
            mTitleTV.setText("CALIBRATE POWER");
            mBody1TV.setText(Html.fromHtml("Stop pedaling and unclip before\ncalibrating Power Sensor."));
            mButtonTV.setText("CALIBRATE");
            mButtonIV.setImageResource(R.drawable.select_btn);
        }else{
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.calibrate_outside);
            mTitleTV = (TextView) findViewById(R.id.title);
            mBody1TV = (TextView) findViewById(R.id.body1);
            mTitleTV.setText("CALIBRATE POWER");
            mBody1TV.setText(Html.fromHtml("Power sensor detected. Unclip\nfrom pedals before calibrating."));
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        //reset
        mTorque = null;
        mOffset = null;
        mShown = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            this.unregisterReceiver(calibrationReceiver);
        } catch (IllegalArgumentException e) { }
    }

    private void checkIfNeedsCalibrate(){
        if(SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT != SettingsUtil.getBleOrAnt()) finish();
        mSensor = (PowerSensor)(AntPlusManager.getInstance(this).getMostRecent(BWManager.EXTRA_BIKE_POWER_PROFILE));
        if(mSensor == null || !mSensor.canCalibrate()){ // can't do calibration
            finish();
        }
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                registerReceiver(calibrationReceiver, new IntentFilter(BWManager.ACTION_BIKE_POWER_CALIBRATION));
                if(mFromSettings)
                    FeedbackDialog.showDialog(CalibrateActivity.this, "Calibrating", "Do not pedal while calibrating", null, FeedbackDialog.SHOW_SPINNER);
                else
                    FeedbackDialog.showDialog(CalibrateActivity.this, "Calibrating", "Do not pedal while calibrating", null, FeedbackDialog.SHOW_SPINNER, true, false);
                mHandler.postDelayed(calibrationFailedRunnable, 20 * 1000);
		// ^give 20 second timeout to calibrate: this should
		// almost never reach even if calibration fails
                Intent i = new Intent(AntService.ACTION_ANT_SERVICE);
                i.putExtra(AntService.EXTRA_ANT_SERVICE_BIKE_CALIBRATION_REQUEST, true);
                startService(i);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
    
    private void dismissFeedbackDialogAndFinishActivity(){
        FeedbackDialog.dismissDialog(CalibrateActivity.this);
        if(!mFromSettings){
            setResult(RESULT_OK, new Intent());
        }
        CalibrateActivity.this.finish();
    }
    
    private Runnable dismissFeedbackDialogAndFinishActivityRunnable=new Runnable(){
        public void run(){
            dismissFeedbackDialogAndFinishActivity();
        }
    };
    
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED, new Intent());
    }

    private Runnable dismissFeedbackDialogRunnable=new Runnable(){
        public void run(){
            FeedbackDialog.dismissDialog(CalibrateActivity.this);
        }
    };
    
    private Runnable calibrationFailedRunnable=new Runnable(){
        public void run(){
            mHandler.removeCallbacksAndMessages(null);
            FeedbackDialog.dismissDialog(CalibrateActivity.this); //dismiss the previous if needs
            if(mFromSettings)
                FeedbackDialog.showDialog(CalibrateActivity.this, "Calibration Failed", null, FeedbackDialog.ICONTYPE.WARNING, FeedbackDialog.HIDE_SPINNER);
            else
                FeedbackDialog.showDialog(CalibrateActivity.this, "Calibration Failed", null, FeedbackDialog.ICONTYPE.WARNING, FeedbackDialog.HIDE_SPINNER, false, false);
            mHandler.postDelayed(dismissFeedbackDialogRunnable, 2 * 1000); //give 2 second to dismiss
            try {
                CalibrateActivity.this.unregisterReceiver(calibrationReceiver);
            } catch (IllegalArgumentException e) { }
        }
    };
    
    private final BroadcastReceiver calibrationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == BWManager.ACTION_BIKE_POWER_CALIBRATION){
                boolean result = intent.getBooleanExtra(BWManager.EXTRA_BIKE_POWER_CALIBRATION_RESULT, false); // indicates request send or not
                String offset = intent.getStringExtra(BWManager.EXTRA_BIKE_POWER_CALIBRATION_OFFSET);
                String torque = intent.getStringExtra(BWManager.EXTRA_BIKE_POWER_CALIBRATION_TORQUE);
                
                if(offset != null) mOffset = offset;
                if(torque != null) mTorque = torque;
                if(mShown) return; //skip since it showed already, just update offset and torque
                if(result){
                    if((mOffset != null && mTorque != null && !mShown && mSensor.hasOffsetAndTorque()) || (mOffset != null && !mShown && !mSensor.hasOffsetAndTorque())){ // calibrated
                        mShown = true; // just show one time
                        mHandler.removeCallbacksAndMessages(null);
                        FeedbackDialog.dismissDialog(CalibrateActivity.this); //dismiss the previous if needs
                        
                        if(mSensor.hasOffsetAndTorque()){
                            if(mFromSettings)
                                FeedbackDialog.showDialog(CalibrateActivity.this, "Calibrated", "Offset " + mOffset + " - Torque " + mTorque, FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER, true);
                            else
                                FeedbackDialog.showDialog(CalibrateActivity.this, "Calibrated", "Offset " + mOffset + " - Torque " + mTorque, FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER, true, false);
                            new CountDownTimer(5* 1000, 1000) {
                                public void onTick(long millisUntilFinished) { //try to update torque value every 1 second
                                    if(mFromSettings)
                                        FeedbackDialog.updateDialog(CalibrateActivity.this, null, "Offset " + mOffset + " - Torque " + mTorque);
                                    else
                                        FeedbackDialog.updateDialog(CalibrateActivity.this, null, "Offset " + mOffset + " - Torque " + mTorque);
                                }
                                public void onFinish() {
                                    try {
                                        CalibrateActivity.this.unregisterReceiver(calibrationReceiver);
                                    } catch (IllegalArgumentException e) { }
                                    dismissFeedbackDialogAndFinishActivity();
                                }
                            }.start();
                        }else{
                            if(mFromSettings)
                                FeedbackDialog.showDialog(CalibrateActivity.this, "Calibrated", null, FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER, true);
                            else
                                FeedbackDialog.showDialog(CalibrateActivity.this, "Calibrated", null, FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER, true, false);
                            mHandler.postDelayed(dismissFeedbackDialogAndFinishActivityRunnable, 5 * 1000); //give 5 second to dismiss and finish the activity
                            try {
                                CalibrateActivity.this.unregisterReceiver(calibrationReceiver);
                            } catch (IllegalArgumentException e) { }
                        }
                    }
                }else{ // request failed
                    mHandler.postDelayed(calibrationFailedRunnable, 0);
                }
            }
        }
    };
}
