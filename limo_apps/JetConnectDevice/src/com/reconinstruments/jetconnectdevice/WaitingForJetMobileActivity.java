
package com.reconinstruments.jetconnectdevice;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.commonwidgets.ReconJetDialog;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;
import com.reconinstruments.utils.DeviceUtils;

/**
 * <code>WaitingForJetMobileActivity</code> waiting for the MFi session open event and then finish the smartphone connection.
 */
public class WaitingForJetMobileActivity extends FragmentActivity {

    private static final String TAG = WaitingForJetMobileActivity.class.getSimpleName();
    private static final String RECON_YELLOW_TEXT = ConnectSmartphoneActivity.RECON_YELLOW_TEXT;

    private TextView mTitleTV;
    private TextView mBody1TV;
    private TextView mBody2TV;
    private TextView mButtonTV;
    private ImageView mButtonIV;
    
    private CustomQuitDialog mQuitDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title_two_lines_button);
        mTitleTV = (TextView) findViewById(R.id.title);
        mBody1TV = (TextView) findViewById(R.id.body1);
        mBody2TV = (TextView) findViewById(R.id.body2);
        mButtonTV = (TextView) findViewById(R.id.text_view);
        mButtonIV = (ImageView) findViewById(R.id.image_view);
        mTitleTV.setText(R.string.title_waiting_engage);
        mBody1TV.setText(Html.fromHtml("Log in to the <font color=\"" + RECON_YELLOW_TEXT + "\">Recon Engage</font> app on your iPhone. "
        		+ "It is available for download from the <font color=\"" + RECON_YELLOW_TEXT + "\">App Store</font>."));
        mBody1TV.setLineSpacing(0f, 0.9f);
        mButtonTV.setText("DO THIS LATER");
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
            //ignore it
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
        Intent intent = new Intent(this, EnableNotificationsActivity.class);
        CommonUtils.launchPrevious(this,intent,true);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            	List<Fragment> fragList = new ArrayList<Fragment>();
    	        fragList.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Cancel", 0, 0));
    	        fragList.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Quit", 0, 1));
    	        
    	        String title = getResources().getString(R.string.title_quit_confirm);
    	        String desc = null;
    	        if(DeviceUtils.isSun()){
    	        	desc = getResources().getString(R.string.desc_quit_confirm_jet);
    	        }
    	        else {
    	        	desc = getResources().getString(R.string.desc_quit_confirm_snow2);
    	        }
    	        
            	FragmentManager fm = getSupportFragmentManager(); 
            	Fragment frag = fm.findFragmentByTag("quit_dialog");
            	if(frag == null) {
            		mQuitDialog = new CustomQuitDialog(this, title, desc, fragList, R.layout.title_body_carousel);
            		mQuitDialog.show(fm.beginTransaction(), "quit_dialog");
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
    
    //only for iphone
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
                    Log.w(TAG, "HUD_STATE_CHANGED to 0, HUDService reports: disconnected");
                } else if (state == 1) {// in this case, bluetooth paired and connected but mfi session doesn't open yet
                    Log.i(TAG, "HUD_STATE_CHANGED to 1, HUDService reports: connecting");
                } else if (state == 2) {
                    Log.i(TAG, "HUD_STATE_CHANGED to 2, HUDService reports: connected");
                    FeedbackDialog.showDialog(WaitingForJetMobileActivity.this, "Connected", null,
                                                FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER);
                    
                    //gives 2 seconds delay to dismiss the dialog
                    new CountDownTimer(2 * 1000, 1000) {
                        public void onTick(long millisUntilFinished) {
                        }
                        public void onFinish() {
                            WaitingForJetMobileActivity.this.finish();
                            //force close the app to clean some activities in the history stack
//                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    }.start();
                    
                }
            }
        }
    };
}
