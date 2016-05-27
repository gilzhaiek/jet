
package com.reconinstruments.jetconnectdevice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.utils.BTHelper;
import com.reconinstruments.utils.DeviceUtils;

/**
 * <code>EnableNotificationsActivity</code> ask to enable the notification, it listens the HUDService state changed event and take proper action as well.
 */
public class EnableNotificationsActivity extends FragmentActivity {

    private static final String TAG = EnableNotificationsActivity.class.getSimpleName();
    private static final int DELAY_TO_DISMISS_DIALOG = 2000; // milliseconds
    private static final String RECON_YELLOW_TEXT = ConnectSmartphoneActivity.RECON_YELLOW_TEXT;
    private TextView mTitleTV;
    private TextView mBodyTV;
    private TextView mButtonTV;
    private String btName;
    private ImageView mButtonIV;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title_body_button);
        btName = BTHelper.getInstance(getApplicationContext()).getLastPairedDeviceName();
        mTitleTV = (TextView) findViewById(R.id.title);
        mBodyTV = (TextView) findViewById(R.id.body);
        mButtonTV = (TextView) findViewById(R.id.text_view);
        mButtonIV = (ImageView) findViewById(R.id.image_view);
        mTitleTV.setText(R.string.title_enable_notification);
        mButtonTV.setText("CONTINUE");
        mBodyTV.setText
        (Html
                .fromHtml(
                        "Tap the&nbsp<img src=\"iphone_info_icon.png\" align=\"middle\"> next to your connected device, then set <font color=\"" + RECON_YELLOW_TEXT + "\">Show Notifications</font> to&nbsp<img src=\"iphone_switch.png\" align=\"middle\">",
                        new ImageGetter() {
                            @Override
                            public Drawable getDrawable(String source) {
                                int id;
                                if (source
                                        .equals("iphone_info_icon.png")) {
                                    id = R.drawable.iphone_info_icon;
                                } else if (source
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
        mButtonIV.setImageResource((DeviceUtils.isSun()) ? R.drawable.select_btn : R.drawable.snow_select);
        adjustMargins();
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(hudStateReceiver, new IntentFilter("HUD_STATE_CHANGED"));
    }

    @Override
    protected void onStop() {
        try {
            this.unregisterReceiver(hudStateReceiver);
        } catch (IllegalArgumentException e) {
            // ignore it
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FeedbackDialog.dismissDialog(this);
    }

    @Override
    public void onBackPressed() {
        //do nothing to prevent user go back
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                BTHelper btHelper = BTHelper.getInstance(this.getApplicationContext());
                btHelper.reEnableMapSS1(); // try to reconnect map profile
                if(btHelper.getBTConnectionState() == 2){ //connected already at this point
                    FeedbackDialog.showDialog(EnableNotificationsActivity.this, "Connected", btName,
                                                FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER);
                    
                    //gives 2 seconds delay to dismiss the dialog
                    new CountDownTimer(DELAY_TO_DISMISS_DIALOG, 1000) {
                        public void onTick(long millisUntilFinished) {
                        }
                        public void onFinish() {
                            FeedbackDialog.dismissDialog(EnableNotificationsActivity.this);
                            EnableNotificationsActivity.this.finish();
                            overridePendingTransition(R.anim.fade_slide_in_left,0);
                        }
                    }.start();
                }else if(btHelper.getBTConnectionState() == 1){ // go to next step
                    Intent intent = new Intent(this, WaitingForJetMobileActivity.class);
                    CommonUtils.launchNext(this, intent, true);
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
    
    private void adjustMargins(){
    	View view = findViewById(R.id.bottom_layout);
    	RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
    	params.leftMargin = 20;
    	params.bottomMargin = 20;
    }
    
    // listening the mfi/HUDService connection sate changed event and take proper action.
    private final BroadcastReceiver hudStateReceiver = new BroadcastReceiver() {
        int previousState = -1;
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if ("HUD_STATE_CHANGED".equals(action)) {
                int state = intent.getIntExtra("state", 0);
                if(previousState == state){
                    return;
                }
                if (state == 0) {
                    Log.w(TAG, "HUD_STATE_CHANGED changed to 0, HUDService reports: disconnected");
                    FeedbackDialog.showDialog(EnableNotificationsActivity.this, "Failed to connect", null,
                                                FeedbackDialog.ICONTYPE.WARNING, FeedbackDialog.HIDE_SPINNER);
                    
                    //gives 2 seconds delay to dismiss the dialog
                    new CountDownTimer(2 * 1000, 1000) {
                        public void onTick(long millisUntilFinished) {
                        }
                        public void onFinish() {
                            FeedbackDialog.dismissDialog(EnableNotificationsActivity.this);
                            Intent intent = new Intent(EnableNotificationsActivity.this, WaitingForIphoneActivity.class);
                            CommonUtils.launchPrevious(EnableNotificationsActivity.this,
						       intent,true);
                        }
                    }.start();
                }
                previousState = state;
            }
        }
    };
}
