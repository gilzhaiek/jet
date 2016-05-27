//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlivestats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import com.reconinstruments.dashlauncher.livestats.IDashFragment;
import com.reconinstruments.utils.stats.ActivityUtil;

public class SnowLiveStatsActivity extends LiveStatsActivityBase{
    public static final String TAG = "SnowLiveStatsActivity";
    private JetDialog dayStartedDialog;
    private SearchingGPSDialog searchingGPSDialog;
    private DayPausedDialog dayPausedDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The dialogues
        dayStartedDialog = new JetDialog(this, R.layout.day_started);
        searchingGPSDialog = new SearchingGPSDialog(this, R.layout.searching_gps);
        dayPausedDialog = new DayPausedDialog(this, R.layout.day_paused);

    }
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER:
            ActivityUtil.pauseActivity(this);
            showDayPausedDialog();
            return true;
        default:
            return super.onKeyUp(keyCode,event);
        }
    }
    ///////////////////////////////////////////////////////////////
    // New paradigm for accessing data from Transcend service
    ///////////////////////////////////////////////////////////////
    @Override
    protected BroadcastReceiver getTranscendReceiver() {
        return transcendReceiver;
    }
    BroadcastReceiver transcendReceiver = new BroadcastReceiver () {
            @Override
            public void onReceive(Context c, Intent i) {
                sFullInfo = i.getBundleExtra("FullInfo");
                if (shouldShowOverlay()) {
                    showOverLayIfNotShown();
                }
                else {
                    hideOverLayIfNotHidden();
                    startActivityIfNotStarted();
                    if (ActivityUtil.getActivityState(sFullInfo)==ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED){
                        showDayPausedDialog();
                    }
                    // update views:
                    ((IDashFragment)(dashFragments.get(currentPosition)))
                    .updateFields(sFullInfo, inActivity());
                }
            }
        };
    ///////////////////////////////////////////////////////////////

    private boolean shouldShowOverlay() {
        return (!sFullInfo.getBoolean("LOCATION_BUNDLE_VALID", false)
                && (ActivityUtil.getActivityState(sFullInfo) ==
                    ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY));
    }
    private void showOverLayIfNotShown() {
        if(!searchingGPSDialog.isShowing()) searchingGPSDialog.show();
    }
    private void hideOverLayIfNotHidden() {
        if(searchingGPSDialog.isShowing()) searchingGPSDialog.cancel();
    }
    private void startActivityIfNotStarted() {
        //Log.v(TAG,"startActivityIfNotStarted");
        if (ActivityUtil.getActivityState(sFullInfo) ==
            ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY) {
            ActivityUtil.startActivity(this);

            // show the dayStartedDialog for 2 seconds
            dayStartedDialog.show();
            new CountDownTimer(2 * 1000, 1000) {
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    dayStartedDialog.cancel();
                }
            }.start();
        }
    }
    private void showDayPausedDialog() {
        if(!dayPausedDialog.isShowing()) dayPausedDialog.show();
    }
    public void postSetDashboard(){
        //do nothing on snow2
    }
}
