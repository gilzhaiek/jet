
package com.reconinstruments.jetconnectdevice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.TextUtils;
import android.text.Spanned;
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
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.utils.BTHelper;
import com.reconinstruments.commonwidgets.FeedbackDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>ReconnectSmartphoneActivity</code> deal with reconnect logic,
 * it sends connect request to HUDService
 */
public class ReconnectSmartphoneActivity extends CarouselItemHostActivity {

    private static final String TAG = ReconnectSmartphoneActivity.class.getSimpleName();
    private static final String RECON_YELLOW_TEXT = ConnectSmartphoneActivity.RECON_YELLOW_TEXT;

    private TextView mTitleTV;
    private TextView mBodyTV;
    private TextView mBodySecondTV;
    private String btName;
    
    private boolean mShouldAutoReconnect = false;
    
    private Handler mHandler = new Handler();
    
    private int mBTType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reconnect_title_body_carousel);

        mShouldAutoReconnect = getIntent().getBooleanExtra("SHOULD_AUTO_RECONNECT",false);

        btName = BTHelper.getInstance(getApplicationContext()).getLastPairedDeviceName();
        mBTType = BTHelper.getInstance(getApplicationContext()).getLastPairedDeviceType();

        mTitleTV = (TextView) findViewById(R.id.title);
        mBodyTV = (TextView) findViewById(R.id.body);
        mBodySecondTV = (TextView) findViewById(R.id.body_second);
        mTitleTV.setText(mBTType==0 ? R.string.title_reconnect_device_android
                                        : R.string.title_reconnect_device_iphone);
        mBodyTV.setLines(2);
        mBodyTV.setLineSpacing(0f, 1f);
        mBodyTV.setText(Html.fromHtml("Would you like to reconnect to <font color=\"" + RECON_YELLOW_TEXT + "\">" + btName + "</font>?"));
        mBodySecondTV.setVisibility(View.GONE);
        //custom the viewPager width to fit the length of text
        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        LayoutParams params=(LayoutParams) viewPager.getLayoutParams();
        params.width=320;
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
        mHandler.removeCallbacks(reconnectFailedRunnable);
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        FeedbackDialog.dismissDialog(this);
    }

    /**
     * prepare the fragements, called by method initPager()
     */
    protected List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();
        if(!mShouldAutoReconnect){ // manually reconnect
            fList.add(ConnectSmartphoneFragment.newInstance(R.layout.title_body_carousel_item, "Reconnect", 0, 0));
            fList.add(ConnectSmartphoneFragment.newInstance(R.layout.title_body_carousel_item, "Connect Other", 0, 1));
            // requires a maximum padding of 65
        }else{ // auto reconnect
            fList.add(ConnectSmartphoneFragment.newInstance(R.layout.title_body_carousel_item, "Reconnect", 0, 0));
            fList.add(ConnectSmartphoneFragment.newInstance(R.layout.title_body_carousel_item, "Later", 0, 1));
            fList.add(ConnectSmartphoneFragment.newInstance(R.layout.title_body_carousel_item, "Don't Ask", 0, 2));
        }
        return fList;
    }

    /**
     * display the metric data on the UI
     */
    private void updateUI() {
        initPager();
        mPager.setCurrentItem(0);
        if(!mShouldAutoReconnect) mPager.setPadding(60,0,60,0); // change padding for 'Connect Other'
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                Intent intent = null;
                if (mPager.getCurrentItem() == 0) {
                    
                    // 0 disconnected, 1 connecting, 2 connected
                    int connectionState = BTHelper.getInstance(getApplicationContext()).getBTConnectionState();
                    if(connectionState == 1 && mBTType == 1){ //specified to iPhone only
                        intent = new Intent(this, WaitingForJetMobileActivity.class);
			CommonUtils.launchNext(this,intent,true);
                    }else{
                        FeedbackDialog.showDialog(this, "Reconnecting", btName, null, true, false);
                        BTHelper.getInstance(getApplicationContext()).reconnect();
                        //stop and report failure if it's over 10 seconds for some reasons such as out of range or weird state, etc.
                        mHandler.postDelayed(reconnectFailedRunnable, 10*1000); 
                    }
                } else if (mPager.getCurrentItem() == 1) {
                    if(!mShouldAutoReconnect){ // connect other for manually reconnecting
                        intent = new Intent(this, ChooseDeviceActivity.class);
                        intent.putExtra(ChooseDeviceActivity.EXTRA_FROM, ChooseDeviceActivity.FROM_RECONNECT);
			CommonUtils.launchNext(this,intent,false);
                        overridePendingTransition(R.anim.fade_slide_in_bottom,0);
                    }else{ // later for auto reconnecting
                        // don't need to anything here
                    }
                    finish();
                } else{ // Don't ask again for auto reconnecting
                    // save this device to 'don't ask again' list
                    String btAddress = BTHelper.getInstance(getApplicationContext()).getLastPairedDeviceAddress();
                    String notReconnecting = Settings.System.getString(getApplicationContext().getContentResolver(),
                            BTHelper.BLUETOOTH_DONT_AUTO_RECONNECT);
                    if(notReconnecting == null){
                        Settings.System.putString(getApplicationContext().getContentResolver(),
                                BTHelper.BLUETOOTH_DONT_AUTO_RECONNECT, "," + btAddress);
                    }else if(!notReconnecting.contains(btAddress)){
                        Settings.System.putString(getApplicationContext().getContentResolver(),
                                BTHelper.BLUETOOTH_DONT_AUTO_RECONNECT, notReconnecting + "," + btAddress);
                    }
                    finish();
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
                previousState = state;
                if (state == 0) {
                    Log.w(TAG, "HUD_STATE_CHANGED to 0, HUDService reports: disconnected");
                    reconnectFailed();
                    mHandler.removeCallbacks(reconnectFailedRunnable);
                } else if (state == 1) {
                    Log.i(TAG, "HUD_STATE_CHANGED to 1, HUDService reports: connecting");
                    //the following code is specified for iPhone
                    int deviceType = BTHelper.getInstance(ReconnectSmartphoneActivity.this.getApplicationContext())
                                        .getLastPairedDeviceType();
                    if(deviceType == 1){
                        FeedbackDialog.dismissDialog(ReconnectSmartphoneActivity.this);
                        Intent i = new Intent(ReconnectSmartphoneActivity.this, WaitingForJetMobileActivity.class);

			CommonUtils.launchNext(ReconnectSmartphoneActivity.this,
					       i,
					       true);
                        overridePendingTransition(R.anim.fade_slide_in_bottom,0);

                    }
                } else if (state == 2) {
                    Log.i(TAG, "HUD_STATE_CHANGED to 2, HUDService reports: connected");
                    mHandler.removeCallbacks(reconnectFailedRunnable);
                    FeedbackDialog.dismissDialog(ReconnectSmartphoneActivity.this);
                    FeedbackDialog.showDialog(ReconnectSmartphoneActivity.this,
                                                    "Connected", btName, FeedbackDialog.ICONTYPE.CHECKMARK, false);
                    
                    //gives 2 seconds delay to dismiss the dialog
                    new CountDownTimer(2 * 1000, 1000) {
                        public void onTick(long millisUntilFinished) {
                        }
                        public void onFinish() {
                            FeedbackDialog.dismissDialog(ReconnectSmartphoneActivity.this);
                            ReconnectSmartphoneActivity.this.finish();
                            overridePendingTransition(R.anim.fade_slide_in_left,0);
                        }
                    }.start();
                    
                }
            }
        }
    };

   @Override
   public void onBackPressed() {
       super.onBackPressed();
       CommonUtils.launchPrevious(this,null,false);
   }
   
   private void reconnectFailed(){
       // dismiss the 'reconnecting...' overlay
       FeedbackDialog.dismissDialog(this);
       //show reconnect failed overlay
       String titleMsg = "Reconnect Failed";
       FeedbackDialog.showDialog(this, titleMsg, null, FeedbackDialog.ICONTYPE.WARNING, false);
       //gives 2 seconds delay to dismiss the overlay
       new CountDownTimer(2 * 1000, 1000) {
           public void onTick(long millisUntilFinished) {
           }
           public void onFinish() {
               FeedbackDialog.dismissDialog(ReconnectSmartphoneActivity.this);
           }
       }.start();
   }
   
   private Runnable reconnectFailedRunnable = new Runnable() {
       @Override
       public void run() {
           reconnectFailed();
       }
    };

}
