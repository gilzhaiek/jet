package com.reconinstruments.socialsharing;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;
import com.reconinstruments.hudservice.helper.HUDConnectivityHelper;
import com.reconinstruments.hudservice.helper.BTPropertyReader;
import com.reconinstruments.commonwidgets.TwoOptionsJumpFixer;
import com.reconinstruments.commonwidgets.ReconToast;
import com.reconinstruments.commonwidgets.TwoOptionsItem;
import com.reconinstruments.commonwidgets.TwoOptionsAdapter;
import com.reconinstruments.mobilesdk.social.SocialStatsMessage;
import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.utils.DeviceUtils;

public class ShareDayActivity extends CarouselItemHostActivity {
        
    private static final String TAG = "ShareDayActivity";
    private static final String INTENT = "com.reconinstruments.social.share";
    private static final String SENDER = "com.reconinstruments.socialsharing.ShareDayActivity";
    private PostingReceiver mPostingReceiver = new PostingReceiver();
    private CountDownTimer failedTimer;
    private TextView headerTitleTV;
    private TextView contentTV;
        
    private String category;
    private String title;
    private int stat;
    private long when = System.currentTimeMillis()/1000;
    private String valueAndUnit;
    private boolean backToMainScreen = false;
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sharing);
        registerReceiver(mPostingReceiver, new IntentFilter(SENDER));
    }

    /**
     * prepare the fragements, called by method initPager()
     */
    protected List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();
        fList.add(new SharingFragment(R.layout.title_body_carousel_item, "Cancel", 0, 0));
        fList.add(new SharingFragment(R.layout.title_body_carousel_item, "Share", 0, 1));
        return fList;
    }

    /**
     * display the metric data on the UI
     */
    private void updateUI() {
        initPager();
        mPager.setCurrentItem(0);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        backToMainScreen = getIntent().getBooleanExtra("backt_to_main_screen", false);
        headerTitleTV = (TextView) findViewById(R.id.header_title);
        if(DeviceUtils.isSun()){
            headerTitleTV.setText("SHARE YOUR ACTIVITY");
        }else{
            headerTitleTV.setText("SHARE YOUR DAY");
        }
        contentTV = (TextView) findViewById(R.id.content_text);
        LinearLayout action1LL = (LinearLayout) findViewById(R.id.action_layout_1);
        LinearLayout action2LL = (LinearLayout) findViewById(R.id.action_layout_2);
        ImageView imageView = (ImageView) findViewById(R.id.imageView1);
        if(BTPropertyReader.getBTConnectionState(ShareDayActivity.this) != 2){
            if(DeviceUtils.isSun()){
                imageView.setImageResource(R.drawable.jet_select);
            }else{
                imageView.setImageResource(R.drawable.select_button);
            }
            action1LL.setVisibility(View.GONE);
            action2LL.setVisibility(View.VISIBLE);
            contentTV.setText("You must have a smartphone connected to use this feature.");
        }else{ //connected
            action1LL.setVisibility(View.VISIBLE);
            action2LL.setVisibility(View.GONE);
            if(DeviceUtils.isSun()){
                contentTV.setText(getResources().getString(R.string.share_disclaimer, "activity"));
            }else{
                contentTV.setText(getResources().getString(R.string.share_disclaimer, "day"));
            }
            updateUI();
        }
    }

    private void postXMLMessage() {
	//Log.v(TAG,"postXMLMessage");
        
        Pair pair = new Pair("snow", when);
        SocialStatsMessage.SocialStats stats = new SocialStatsMessage.SocialStats();
        stats.sportsActivity =  pair;
        String xmlMsg = SocialStatsMessage.compose(stats); 
        Log.d(TAG, "xmlMsg = " + xmlMsg);
        sendHUDConnectivityMessage(xmlMsg);
                
        failedTimer = new CountDownTimer(7 * 1000, 1000) {
                public void onTick(long millisUntilFinished) {
		    //Log.v(TAG,"onTick Xml");
                }
                public void onFinish() {
		    //Log.v(TAG,"onFinish Xml");
                    FeedbackDialog.dismissDialog(ShareDayActivity.this);
                    finish();
                }
            };
        failedTimer.start();
    }

    private void sendHUDConnectivityMessage(String xmlMsg){
        HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
        cMsg.setIntentFilter(INTENT);
        cMsg.setRequestKey(0);
        cMsg.setSender(SENDER);
        cMsg.setData(xmlMsg.getBytes());
        HUDConnectivityHelper.getInstance(this).push(cMsg, HUDConnectivityService.Channel.OBJECT_CHANNEL);
    }

    @Override
    protected void onDestroy() {
        if(failedTimer != null){
            failedTimer.cancel();
            failedTimer = null;
        }
        FeedbackDialog.dismissDialog(this);
        try{
            unregisterReceiver(mPostingReceiver);
        }catch(IllegalArgumentException e){
            //ignore
        }
        super.onDestroy();
    }
        
    private class PostingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
	    //Log.v(TAG,"Postingreceiver onReceive");
            if (intent.getAction().equals(SENDER)) {
                boolean result = intent.getBooleanExtra("result", false);
                if(failedTimer != null){
                    failedTimer.cancel();
                    failedTimer = null;
                }
                if(result){
                    if(DeviceUtils.isSun()){
                        FeedbackDialog.showDialog(ShareDayActivity.this, "Sharing", "Your activity will be posted<br>on Facebook shortly.", FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER);
                        }else{
                        FeedbackDialog.showDialog(ShareDayActivity.this, "Sharing", "Your day will be posted<br>on Facebook shortly.", FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER);
                        }
                }else{
                    FeedbackDialog.showDialog(ShareDayActivity.this, "Unable To Share", "Ensure Facebook is linked in your Engage app Settings.",
                                              FeedbackDialog.ICONTYPE.WARNING, FeedbackDialog.HIDE_SPINNER);
                }
                //gives 2 seconds delay to dismiss the dialog
                new CountDownTimer(2 * 1000, 1000) {
                    public void onTick(long millisUntilFinished) {
			//Log.v(TAG,"posting receiver onTick");
                    }
                    public void onFinish() {
			//Log.v(TAG,"posting receiver onFinish");
			goHome();
                        ShareDayActivity.this.finish();
                    }
                }.start();
            }
        }
    }

    private class SharingFragment extends CarouselItemFragment {

        public SharingFragment(int defaultLayout, String defaultText, int defaultImage,
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
            if(BTPropertyReader.getBTConnectionState(this) != 2){ //launch smartphone connection
                startActivity(new Intent("com.reconinstruments.connectdevice.CONNECT"));
            }else{
                Intent intent = null;
                if (mPager.getCurrentItem() == 0) { //Cancel
		    //Log.v(TAG,"canceling");
		    goHome();
		    //Log.v(TAG,"started item host");
                    finish();
		    //Log.v(TAG,"called finish");
                }else{ // Share
                    int socialConnected = 0;
                    try {
                        socialConnected = Settings.System.getInt(ShareDayActivity.this.getContentResolver(), "isFacebookConnected");
                    } catch (SettingNotFoundException e) {
                        e.printStackTrace();
                    }
                    if(socialConnected != 0){
			//Log.v(TAG,"call postxmlmessage");
                        postXMLMessage();
                    }else{
                        FeedbackDialog.showDialog(ShareDayActivity.this, "Unable To Share", "Ensure Facebook is linked in your Engage app Settings.",
                                                  FeedbackDialog.ICONTYPE.WARNING, FeedbackDialog.HIDE_SPINNER);
                            
                        //gives 2 seconds delay to dismiss the dialog
                        new CountDownTimer(2 * 1000, 1000) {
                            public void onTick(long millisUntilFinished) {
				//Log.v(TAG,"feedback dialog ontick");
                            }
                            public void onFinish() {
				//Log.v(TAG,"feedback dialog onFinish()");
				goHome();
                                ShareDayActivity.this.finish();
                            }
                        }.start();
                    }
                }
            }
            return true;
        default:
            return super.onKeyUp(keyCode, event);
        }
    }
    
    @Override
    public void onBackPressed() {
	//Log.v(TAG,"onBackPressed");
        if(backToMainScreen){
	    goHome();
        }
        finish();
    }
    private void goHome() {
	//Log.v(TAG,"goHome");
	startActivity(new Intent("com.reconinstruments.itemhost"));
    }
}
