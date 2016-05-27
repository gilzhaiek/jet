package com.reconinstruments.messagecenter.frontend;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;

import com.reconinstruments.messagecenter.R;

import com.reconinstruments.commonwidgets.ProgressDialog;
import com.reconinstruments.commonwidgets.CommonUtils;

public class NoUnreadActivity extends FragmentActivity{
    
    private Handler mHandler = new Handler();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.no_unread_message);
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                ProgressDialog.showProgressDialog(NoUnreadActivity.this, "All Notifications");
                mHandler.postDelayed(dismissFeedback, 1000);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    public Runnable dismissFeedback = new Runnable() {
        public void run() {
            ProgressDialog.dismissProgressDialog(NoUnreadActivity.this);
            startActivity((new Intent(NoUnreadActivity.this, SummaryActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
