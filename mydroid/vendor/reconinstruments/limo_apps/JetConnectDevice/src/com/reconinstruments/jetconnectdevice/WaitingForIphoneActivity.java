
package com.reconinstruments.jetconnectdevice;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.util.Log;

import com.reconinstruments.utils.BTHelper;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.commonwidgets.FeedbackDialog;

/**
 * <code>WaitingForIphoneActivity</code> enables the bluetooth discoverability and listen for HUDService state changed event and takes proper actions.
 */
public class WaitingForIphoneActivity extends WaitingForPhoneActivity {

    private static final String TAG = WaitingForIphoneActivity.class.getSimpleName();
    private static final String RECON_YELLOW_TEXT = ConnectSmartphoneActivity.RECON_YELLOW_TEXT;

    private CountDownTimer mWaitForMapServiceTimer;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitleTV.setText(R.string.title_waiting_device_iphone);

        mBody1TV.setText
        (Html
                .fromHtml(
                        "Go to <font color=\"" + RECON_YELLOW_TEXT + "\">Settings &gt; Bluetooth</font> on your iPhone and ensure Bluetooth is set to&nbsp<img src=\"iphone_switch.png\" align=\"middle\">",
                        new ImageGetter() {
                            @Override
                            public Drawable getDrawable(String source) {
                                int id;
                                if (source
                                        .equals("iphone_switch.png")) {
                                    id = R.drawable.iphone_switch;
                                } else {
                                    return null;
                                }
                                LevelListDrawable d = new LevelListDrawable();
                                Drawable empty = getResources()
                                        .getDrawable(id);
                                d.addLevel(0, 0, empty);
                                d.setBounds(0, 0,
                                        empty.getIntrinsicWidth(),
                                        empty.getIntrinsicHeight());
                                return d;
                            }
                        }, null));
        mBody2TV.setText
        (Html
                .fromHtml(
                        "Tap <font color=\"" + RECON_YELLOW_TEXT + "\">" + BTHelper.getInstance(getApplicationContext()).getJetName() + "</font>",
                        new ImageGetter() {
                            @Override
                            public Drawable getDrawable(String source) {
                                int id;
                                if (source
                                        .equals("iphone_switch.png")) {
                                    id = R.drawable.iphone_switch;
                                } else {
                                    return null;
                                }
                                LevelListDrawable d = new LevelListDrawable();
                                Drawable empty = getResources()
                                        .getDrawable(id);
                                d.addLevel(0, 0, empty);
                                d.setBounds(0, 0,
                                        empty.getIntrinsicWidth(),
                                        empty.getIntrinsicHeight());
                                return d;
                            }
                        }, null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        if(mWaitForMapServiceTimer == null){
            mWaitForMapServiceTimer = new CountDownTimer(5 * 1000, 1000) {
                public void onTick(long millisUntilFinished) {
                }
    
                public void onFinish() {
                    Intent i = new Intent(WaitingForIphoneActivity.this,
                            EnableNotificationsActivity.class);
 //                   i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
		    CommonUtils.launchNew(WaitingForIphoneActivity.this,i,true);
                }
            };
        }

    }

    @Override
    protected void onStop() {
        if(mWaitForMapServiceTimer != null){
            mWaitForMapServiceTimer.cancel();
        }
        mWaitForMapServiceTimer = null;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FeedbackDialog.dismissDialog(this);
    }

    @Override
    protected void onHUDServiceDisconnected() {
        mWaitForMapServiceTimer.cancel();
    }

    @Override
    protected void onHUDServiceConnecting() {
        Log.i(TAG, "waiting for map service ready after 10 seconds");
        mWaitForMapServiceTimer.cancel();
        mWaitForMapServiceTimer.start();
    }

    @Override
    protected void onHUDServiceConnected() {
        mWaitForMapServiceTimer.cancel();
        finish();
    }

    
}
