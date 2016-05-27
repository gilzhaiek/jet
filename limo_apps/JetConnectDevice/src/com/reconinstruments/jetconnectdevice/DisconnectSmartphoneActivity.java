
package com.reconinstruments.jetconnectdevice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.utils.BTHelper;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.commonwidgets.FeedbackDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>DisconnectSmartphoneActivity</code> deal with disconnect logic, it sends disconnect request to HUDService
 */
public class DisconnectSmartphoneActivity extends CarouselItemHostActivity {

    private static final String TAG = DisconnectSmartphoneActivity.class.getSimpleName();
    private static final String RECON_YELLOW_TEXT = ConnectSmartphoneActivity.RECON_YELLOW_TEXT;

    private TextView mTitleTV;
    private TextView mBodyTV;
    private TextView mBodySecondTV;
    private String btName;
    
    private int mBTType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reconnect_title_body_carousel);

        btName = BTHelper.getInstance(getApplicationContext()).getLastPairedDeviceName();
        mBTType = BTHelper.getInstance(getApplicationContext()).getLastPairedDeviceType();

        mTitleTV = (TextView) findViewById(R.id.title);
        mBodyTV = (TextView) findViewById(R.id.body);
        mBodySecondTV = (TextView) findViewById(R.id.body_second);
        mTitleTV.setText(mBTType==0? R.string.title_disconnect_device_android : R.string.title_disconnect_device_iphone);

        mBodyTV.setText(Html.fromHtml("Connected to <font color=\"" + RECON_YELLOW_TEXT + "\">"+btName+"</font>"));
        mBodyTV.setLineSpacing(0f, 1f);
        mBodySecondTV.setText("Would you like to continue using this device?");
        
        //custom the viewPager width to fit the length of text
        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        LayoutParams params=(LayoutParams) viewPager.getLayoutParams();
        params.width=300;
        viewPager.setLayoutParams(params);
        
        updateUI();
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
            //ignore it
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FeedbackDialog.dismissDialog(this);
    }

    /**
     * prepare the fragements, called by method initPager()
     */
    protected List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();
        fList.add(new ConnectSmartphoneFragment(R.layout.title_body_carousel_item, "Continue", 0, 0));
        fList.add(new ConnectSmartphoneFragment(R.layout.title_body_carousel_item, "Disconnect", 0, 1));
        return fList;
    }

    /**
     * display the metric data on the UI
     */
    private void updateUI() {
        initPager();
        mPager.setCurrentItem(0);
    }

    private class ConnectSmartphoneFragment extends CarouselItemFragment {

        public ConnectSmartphoneFragment(int defaultLayout, String defaultText, int defaultImage,
                int item) {
            super(defaultLayout, defaultText, defaultImage, item);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(mDefaultLayout, container, false);
            TextView messageTextView = (TextView) v.findViewById(R.id.text_view);
            messageTextView.setText(mDefaultText);
            if(mItem == 0){ //highlight the first item
                messageTextView.setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
            }else if (mItem == 1) { // initial status , left align item 1
                messageTextView.setGravity(Gravity.LEFT);
                v.setAlpha(0.5f);
            }
            inflatedView = v;
            return v;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mPager.getCurrentItem() == 1) {
                    BTHelper.getInstance(getApplicationContext()).disconnect();
                } else {
                    // 0 disconnected, 1 connecting, 2 connected
                    int connectionState = BTHelper.getInstance(getApplicationContext()).getBTConnectionState();
                    if(connectionState == 1 && mBTType == 1){ //specified to iPhone only
                        Intent intent = new Intent(this, WaitingForJetMobileActivity.class);
                        startActivity(intent);
                    }
		    CommonUtils.launchPrevious(this, null, true);
                    //overridePendingTransition(R.anim.fade_slide_in_left,0);
                }
                return false;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

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
                    Log.i(TAG, "HUD_STATE_CHANGED to 0, HUDService reports: disconnected");
                    FeedbackDialog.showDialog(DisconnectSmartphoneActivity.this, "Disconnected", btName,
                                                                           FeedbackDialog.ICONTYPE.CHECKMARK, false);
                    
                    //gives 2 seconds delay to dismiss the dialog
                    new CountDownTimer(2 * 1000, 1000) {
                        public void onTick(long millisUntilFinished) {
                        }
                        public void onFinish() {
                            FeedbackDialog.dismissDialog(DisconnectSmartphoneActivity.this);
                            DisconnectSmartphoneActivity.this.finish();
                        }
                    }.start();
                    
                } else if (state == 1) {
                    Log.i(TAG, "HUD_STATE_CHANGED to 1, HUDService reports: connecting");
                } else if (state == 2) {
                    Log.i(TAG, "HUD_STATE_CHANGED to 2, HUDService reports: connected");
                }
            }
        }
    };

    private void onHUDServiceDisconnected(){
    }
}
