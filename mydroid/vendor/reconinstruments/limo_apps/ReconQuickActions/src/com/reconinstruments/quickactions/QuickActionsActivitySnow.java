package com.reconinstruments.quickactions;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ImageView;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import com.reconinstruments.commonwidgets.CommonUtils;

/**
 * <code>ReconQuickActionsActivity</code> is designed to launch a quick
 * actions menu for SNOW2. The layout contains a Wrist Remote image surrounded
 * by different navigation options. These options are (in order, CW starting from top):
 * - Home
 * - Smartphone
 * - Power
 * - Notifications
 */
public class QuickActionsActivitySnow extends Activity {

    private static final String TAG = QuickActionsActivity.class.getSimpleName();

    private ImageView leftView;
    private ImageView rightView;
    private ImageView topView;
    private ImageView bottomView;
    private ImageView onTopCenterView;

    private static android.os.Handler handler = new android.os.Handler();
    private static final int SCALE_ANIM_TIME = 60; // in MS


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recon_quick_actions);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(125,0,0,0)));

        overridePendingTransition(R.anim.dock_bottom_enter, 0);

        leftView = (ImageView) findViewById(R.id.leftView);
        rightView = (ImageView) findViewById(R.id.rightView);
        topView = (ImageView) findViewById(R.id.topView);
        bottomView = (ImageView) findViewById(R.id.bottomView);
        onTopCenterView = (ImageView) findViewById(R.id.onTopCenterView);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Intent intent;

        switch (keyCode) {
            // Music
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                CommonUtils.scaleAnimate(onTopCenterView);
                intent = new Intent("com.reconinstruments.jetmusic");
                // post delayed intent for scale animation to finish
                handler.postDelayed(makeRunnable(intent), SCALE_ANIM_TIME);
                return true;

            // Notifications
            case KeyEvent.KEYCODE_DPAD_LEFT:
                CommonUtils.scaleAnimate(leftView);
                intent = new Intent("com.reconinstruments.messagecenter.frontend");
                handler.postDelayed(makeRunnable(intent), SCALE_ANIM_TIME);
                return true;

            // Smartphone
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                CommonUtils.scaleAnimate(rightView);
                intent = new Intent("com.reconinstruments.connectdevice.CONNECT");
                handler.postDelayed(makeRunnable(intent), SCALE_ANIM_TIME);
                return true;

            // Home
            case KeyEvent.KEYCODE_DPAD_UP:
                CommonUtils.scaleAnimate(topView);
                intent = new Intent("com.reconinstruments.itemhost");
                handler.postDelayed(makeRunnable(intent), SCALE_ANIM_TIME);
                return true;

            // Power Options
            case KeyEvent.KEYCODE_DPAD_DOWN:
                CommonUtils.scaleAnimate(bottomView);
                intent = new Intent("RECON_POWER_MENU");
                handler.postDelayed(makeRunnable(intent), SCALE_ANIM_TIME);
                return true;

            default:
                super.onKeyUp(keyCode, event);
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0,R.anim.fadeout_faster);
    }

    /**
     * <code>makeRunnable</code> method makes a <code>CommonUtils.launchNew</code>
     * runnable using the argument intent
     *
     * @param intent an <code>Intent</code> value
     */
    public Runnable makeRunnable(final Intent intent){
        return new Runnable() {
            @Override
            public void run() {
                CommonUtils.launchNew(QuickActionsActivitySnow.this, intent, true);
            }
        };
    }
}