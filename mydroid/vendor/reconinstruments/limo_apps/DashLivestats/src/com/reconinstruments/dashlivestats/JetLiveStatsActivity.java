//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlivestats;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

import com.reconinstruments.dashlauncher.livestats.IDashFragment;
import com.reconinstruments.dashlauncher.livestats.widgets.ReconPowerWidget;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.stats.StatsUtil;

public class JetLiveStatsActivity extends LiveStatsActivityBase{
    public static final String TAG = "JetLiveStatsActivity";
    private JetDialog dayStartedDialog;
    private JetDialog gpsReadyDialog; // for jet only
    private SearchingGPSDialog searchingGPSDialog;
    private DayPausedDialog dayPausedDialog;
    private boolean mShouldAutoRotate = false;
    private int mAutoRotatePeriod = 0;
    private Handler mPanelRotator = new Handler();
    private CountDownDialog mCountDownDialog;

    public void onCreate(Bundle savedInstanceState) {
        //Debug.waitForDebugger();
        super.onCreate(savedInstanceState);
        dayStartedDialog = new JetDialog(this, R.layout.jet_day_started);
        gpsReadyDialog = new JetDialog(this, R.layout.gps_ready);
        searchingGPSDialog = new SearchingGPSDialog(this, R.layout.jet_searching_gps);
        dayPausedDialog = new DayPausedDialog(this, R.layout.jet_day_paused);
        mCountDownDialog = new CountDownDialog(this, R.layout.calibrate_countdown);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if(!inActivity()) {
            startAttemptConnectAntDevice(); 
        }
        if(currentHaveGps && !pastHaveGps && !inActivity()){ // have gps but activity not in
            showHaveGps();
        }
        setAutoRotateRelatedFields();
    }
    
    @Override
    public void onStop() {
        super.onStop();
        stopAttemptConnectAntDevice();
    }
    
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER:
            if (inActivity()) { // Already in activity
                Log.v(TAG,"in Activity");
                pauseActivitySequence();
            }
            else if (haveGps()) { // Not in activity and have gps
                preStartActivitySequence();
            }
            else {              // not in activity and not gps
                showOverLayIfNotShown();
            }
            return true;
        default:
            return super.onKeyUp(keyCode,event);
        }
    }
    private boolean haveGps() {
        return currentHaveGps;
    }
    /**
     * Describe <code>pauseActivitySequence</code> a sequence of
     * actions to be done once the user pauses their sports activity
     *
     */
    private void pauseActivitySequence() {
        ActivityUtil.pauseActivity(this);
        showDayPausedDialog();
        setAutoRotateRelatedFields();
    }
    private void preStartActivitySequence() {
        //if there is power widget, call calibrate activity first
        if(hasWidget(ReconPowerWidget.class)){
            Intent intent = new Intent("com.reconinstruments.jetsensorconnect.calibrate");
            intent.putExtra("calibration_from_settings", false);
            startActivityForResult(intent, 1);
        }else{
            startActivitySequence();
        }
    }
    
    public void resumeActivitySequence() {
        // try to reconnect to sensors before resume the activity
        if (SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT == SettingsUtil.getBleOrAnt()) {
            startAttemptConnectAntDevice(); 
            new CountDownTimer(5* 60 * 1000, 1000) { // stop attempting after 5 minutes
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    stopAttemptConnectAntDevice();
                }
            }.start();
        }
        ActivityUtil.resumeActivity(this);
    }
    
    private void startActivitySequence(){
        ActivityUtil.startActivity(this);
        setAutoRotateRelatedFields();
        // show the dayStartedDialog for 5 seconds
        dayStartedDialog.show();
        new CountDownTimer(5 * 1000, 1000) { // show 5 second
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                dayStartedDialog.cancel();
            }
        }.start();
        
        new CountDownTimer(5* 60 * 1000, 1000) { // stop attempting after 5 minutes
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                stopAttemptConnectAntDevice();
            }
        }.start();
    }
    
    //handle result from calibrating activity
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                mCountDownDialog.show();
                new CountDownTimer(4* 1000, 1000) { // show 4 seconds
                    public void onTick(long millisUntilFinished) {
                        int countdown = (int)millisUntilFinished/1000;
                        if(countdown < 4){
                            mCountDownDialog.setText(String.valueOf(countdown));
                            MediaPlayer mp = MediaPlayer.create(JetLiveStatsActivity.this, R.raw.tick);
                            mp.start(); 
                        }
                    }
                    public void onFinish() {
                        mCountDownDialog.cancel();
                        startActivitySequence();
                    }
                }.start();
            }else{
                startActivitySequence();
            }
        }
    }
    private void showHaveGps() {
        hideOverLayIfNotHidden(); //if gps ready, make sure searching gps overlay has been dismiss
        gpsReadyDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                        
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if(event.getAction() == KeyEvent.ACTION_UP){
                        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER){
                            preStartActivitySequence();
                            dialog.cancel();
                            return true;
                        }
                        else return false;
                    }
                    return false;
                }
            });
        gpsReadyDialog.show();
        MediaPlayer mp = MediaPlayer.create(this, R.raw.not9);
        mp.start(); 
        new CountDownTimer(5 * 1000, 1000) { // show 5 second
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                gpsReadyDialog.cancel();
            }
        }.start();
    }
    ///////////////////////////////////////////////////////////////
    // New paradigm for accessing data from Transcend service
    ///////////////////////////////////////////////////////////////
    @Override
    protected BroadcastReceiver getTranscendReceiver() {
        return transcendReceiver;
    }
    boolean currentHaveGps = false;
    boolean pastHaveGps = false;
    int previousActivityState = ActivityUtil.SPORTS_ACTIVITY_STATUS_ERROR;
    BroadcastReceiver transcendReceiver = new BroadcastReceiver () {
            @Override
            public void onReceive(Context c, Intent i) {
                sFullInfo = i.getBundleExtra("FullInfo");
                if(sFullInfo == null) return; //avoid null pointer exception
                int currentActivityState = ActivityUtil.getActivityState(sFullInfo);
                Integer sportType = new Integer(ActivityUtil.getCurrentSportsType(sFullInfo));
                if(sportType.intValue() != mCurrentSport.intValue()){
                    mCurrentSport = sportType;
                    buildContentView(null);
                }
                if (ActivityUtil.getActivityState(sFullInfo)==ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED){
                    showDayPausedDialog();
                }
                else {
                    pastHaveGps = currentHaveGps;
                    currentHaveGps = sFullInfo.getBoolean("LOCATION_BUNDLE_VALID", false);
                    if (currentHaveGps && !pastHaveGps && !inActivity()) showHaveGps();
                    // update views:
                    ((IDashFragment)(dashFragments.get(currentPosition)))
                    .updateFields(sFullInfo, inActivity());
                }
                if (previousActivityState != currentActivityState){
                    setAutoRotateRelatedFields(); // set when the state changes
                }
                previousActivityState = currentActivityState;
            }
        };
    ///////////////////////////////////////////////////////////////

    private void showOverLayIfNotShown() {
        if(!searchingGPSDialog.isShowing()) searchingGPSDialog.show();
    }
    private void hideOverLayIfNotHidden() {
        if(searchingGPSDialog.isShowing()) searchingGPSDialog.cancel();
    }
    private void showDayPausedDialog() {
        dayPausedDialog.setSportType(mCurrentSport);
        if(!dayPausedDialog.isShowing()) dayPausedDialog.show();
    }
    
    public void postSetDashboard(){
        setAutoRotateRelatedFields();
    }
    
    private void audoRotateDashboardIfNeed(){
        if (mShouldAutoRotate) {
            mPanelRotator.removeCallbacks(mRotateToNext);
            mPanelRotator.postDelayed(mRotateToNext,mAutoRotatePeriod);
        }
    }
    
    public void nextDashboard(int increment) {
        int size = dashFragments.size();
        if (size <= 0) return;
        currentPosition = (currentPosition + increment + size) % size;
        setDashboard(currentPosition, mShouldAutoRotate);
    }

    private Runnable mRotateToNext = new Runnable() {
            @Override
            public void run() {
                nextDashboard(1);
            }
        };

    private void setAutoRotateRelatedFields() {
        mAutoRotatePeriod = SettingsUtil
            .getCachableSystemIntOrSet(this,
                                       SettingsUtil.SHOULD_AUTO_ROTATE,
                                       SettingsUtil.SHOULD_AUTO_ROTATE_DEFAULT)
            * 1000; // Make it seconds
        mShouldAutoRotate = (mAutoRotatePeriod != 0);
        
        //don't auto rotate if the activity hasn't been started
        if(!inActivity()){
            mShouldAutoRotate = false;
            return;
        }
        
        audoRotateDashboardIfNeed();
    }
   
    private void startAttemptConnectAntDevice(){
        Intent intent = new Intent("RECON_ANT_SERVICE");
        intent.putExtra("start_attempt_connect", true);
        startService(intent); 
    }
    
    private void stopAttemptConnectAntDevice(){
        Intent intent = new Intent("RECON_ANT_SERVICE");
        intent.putExtra("stop_attempt_connect", true);
        startService(intent); 
    }
    
    //the countdown overylay to show countdown number
    private class CountDownDialog extends Dialog {

        private TextView countDownTV;
        
        public CountDownDialog(Context context, int layout) {
            super(context);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(layout);
            countDownTV = (TextView) findViewById(R.id.body);
            Window window = getWindow();
            WindowManager.LayoutParams params = window.getAttributes();
            window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            window.setAttributes(params);
            window.getAttributes().windowAnimations = R.style.dialog_animation;
            window.setBackgroundDrawable(new ColorDrawable(Color.argb(180,0,0,0)));
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
        
        public void setText(String text){
            countDownTV.setText(text);
        }
    }
}
