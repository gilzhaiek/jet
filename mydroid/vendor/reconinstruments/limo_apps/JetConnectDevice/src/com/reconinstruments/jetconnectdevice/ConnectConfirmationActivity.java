//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.jetconnectdevice;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.utils.DeviceUtils;

public class ConnectConfirmationActivity extends CarouselItemHostActivity {

	public static final String TAG = ConnectConfirmationActivity.class.getSimpleName();
	public static final String EXTRA_FROM = "from";
	public static final int FROM_NORMAL = 0;
	public static final int FROM_RECONNECT = 1;
    
    private TextView mTitleTV;
    private TextView mBodyTV;
    private int mFrom = FROM_NORMAL;
    
    private JetConnectHelpDialog mNavigateHelpDialog;
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DeviceUtils.isSun()) {
            setContentView(R.layout.connect_confirmation);
        }else{
            setContentView(R.layout.title_body_carousel);
        }
        mTitleTV = (TextView) findViewById(R.id.title);
        mBodyTV = (TextView) findViewById(R.id.body);
        mTitleTV.setText(R.string.title_choose_device);
        if(DeviceUtils.isSun()) {
            mBodyTV.setText(R.string.desc_choose_device_jet);
        }
        else {
            mBodyTV.setText(R.string.desc_choose_device_snow2);
        }
        
        updateUI();
    }
        
    /**
     * prepare the fragements, called by method initPager()
     */
    protected List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();
        fList.add(new ConnectSmartphoneFragment(R.layout.title_body_carousel_item, "Connect", 0, 0));
        fList.add(new ConnectSmartphoneFragment(R.layout.title_body_carousel_item, "Not Now", 0, 1));
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
    	showTutorialOnItemHost();
        switch (keyCode) {
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER:
            if (mPager.getCurrentItem() == 0) {
                setResult(ConnectSmartphoneActivity.RESULT_PROCEED_CONNECTION);
                finish();
            }
            else {
            	finish();
            }
            return true;
        default:
            return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFrom = getIntent().getIntExtra(EXTRA_FROM, FROM_NORMAL);
        if(ConnectSmartphoneActivity.isCalledFromInitialSetup()){
        	showNavigateHelpDialog();
        }
    }

    @Override
    public void onBackPressed() {
    	showTutorialOnItemHost();
        if(mFrom != FROM_NORMAL){
            Intent intent = new Intent(this, ReconnectSmartphoneActivity.class);
            CommonUtils.launchPrevious(this,intent,true);
        }else{
            super.onBackPressed();
            CommonUtils.launchPrevious(this,null,false);
        }
    }
    
    private void showNavigateHelpDialog(){
        mNavigateHelpDialog = JetConnectHelpDialog.newInstance();
        mNavigateHelpDialog.show(getSupportFragmentManager(), "show_navigation_dialog");
    }
    
    
    private void showTutorialOnItemHost(){
    	if(ConnectSmartphoneActivity.isCalledFromInitialSetup()){
    		Settings.System.putInt(getContentResolver(), "com.reconinstruments.itemhost.SHOULD_SHOW_COACHMARKS", 1);
    	}
    }
}
