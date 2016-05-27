package com.reconinstruments.quickactions;


import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.text.Html;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Typeface;
import android.graphics.Color;

import com.reconinstruments.utils.UIUtils;
import com.reconinstruments.utils.BTHelper;

/**
 * 
 * <code>QuickActionsActivity</code> is designed to launch a quick
 * actions which can be invoked from any of the activities in Recon
 * JET.  It shows multi layouts depends on the invoking activity's
 * property(home screen or not) It shows the notificatoin summary info
 * by group as well if there is new notification.  It launches the
 * pre-defined app when the user swipe
 * forward/backward/upward/downward.  The other components which need
 * to be launched by this app should implement new intent filter
 * action otherwise it can't go back to original starting point
 */
public class QuickActionsActivity extends Activity {
    
    private static final String TAG = QuickActionsActivity.class.getSimpleName();
    
    public static final String QUICK_ACTIONS_ACTIVITY = "com.reconinstruments.quickactions";
    public static final String EXTRA_INHOME = "inHome";
    public static final int PHONE_CONNECTED = 2;
    public static final int DROPSHADOW_RADIUS = 15;
    public static final int DROPSHADOW_DX = 0;
    public static final int DROPSHADOW_DY = -2;
    public static final int DROPSHADOW_COLOR = Color.BLACK;
    public static final float BG_DIM = 0.7f;

    private View currentIconView;
    private TextView tempTextView;
    private ImageView tempImageView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setGravity(Gravity.CENTER);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setContentView(R.layout.quick_nav_menu);
        Typeface semiboldTypeface = UIUtils.getFontFromRes(getApplicationContext(), R.raw.opensans_semibold);


        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = BG_DIM; // dimAmount between 0.0f and 1.0f, 1.0f is completely dark
        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); 
        
        //TODO override pending transition
        overridePendingTransition(R.anim.dock_bottom_enter, 0);

        //notifications icon
        currentIconView = findViewById(R.id.notifications);
        tempTextView = (TextView) currentIconView.findViewById(R.id.textView);
        tempImageView = (ImageView) currentIconView.findViewById(R.id.iconView);
        tempImageView.setImageResource(R.drawable.notifications_icon);
        String text = "<font color=#ffffff>swipe</font> <font color=#ffb300>back</font>";
        tempTextView.setText(Html.fromHtml(text));
        tempTextView.setTypeface(semiboldTypeface);
        tempTextView.setShadowLayer(DROPSHADOW_RADIUS, DROPSHADOW_DX, DROPSHADOW_DY, DROPSHADOW_COLOR);

        //camera icon
        currentIconView = findViewById(R.id.camera);
        tempTextView = (TextView) currentIconView.findViewById(R.id.textView);
        tempImageView = (ImageView) currentIconView.findViewById(R.id.iconView);
        tempImageView.setImageResource(R.drawable.camera_icon);
        tempTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.jet_select, 0, 0, 0);

        //right icon view (smartphone or music)
        currentIconView = findViewById(R.id.rightView);
        tempTextView = (TextView) currentIconView.findViewById(R.id.textView);
        tempImageView = (ImageView) currentIconView.findViewById(R.id.iconView);

        //check smartphone connection
        tempImageView.setImageResource((BTHelper.isConnected(this)) ? R.drawable.music_icon : R.drawable.smartphone_icon);


        text = "<font color=#ffffff>swipe</font> <font color=#ffb300>forward</font> ";
        tempTextView.setText(Html.fromHtml(text));
        tempTextView.setTypeface(semiboldTypeface);
        tempTextView.setShadowLayer(DROPSHADOW_RADIUS, DROPSHADOW_DX, DROPSHADOW_DY, DROPSHADOW_COLOR);

        tempTextView = (TextView) findViewById(R.id.swipeView);
        tempTextView.setShadowLayer(DROPSHADOW_RADIUS, DROPSHADOW_DX, DROPSHADOW_DY, DROPSHADOW_COLOR);
        tempTextView = (TextView) findViewById(R.id.returnSentence);
        tempTextView.setShadowLayer(DROPSHADOW_RADIUS, DROPSHADOW_DX, DROPSHADOW_DY, DROPSHADOW_COLOR);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        Intent intent = null;

        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                intent = new Intent("com.reconinstruments.camera");
                startActivity(intent);
                finish();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                intent = new Intent("com.reconinstruments.messagecenter.frontend");
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                intent = new Intent((BTHelper.isConnected(this)) ? "com.reconinstruments.jetmusic" : "com.reconinstruments.connectdevice.CONNECT");
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                intent = new Intent("com.reconinstruments.itemhost");
                startActivity(intent);
                finish();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP: // Special case for taking screenshot
                sendBroadcast(new Intent("com.reconinstruments.screenshot.TAKE_SCREENSHOT"));
                finish();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    // to implement a key long press event, has to override the keydown event to start tracking on this key first
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
	    event.startTracking();
	    return true;
	}
	return super.onKeyDown(keyCode, event);
    }
	
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            //TODO launch the power off confirmation pop up screen
            //TODO TBD: override pending transition, depends on the position
            Intent intent = new Intent("com.reconinstruments.connectdevice.CONNECT");
            startActivity(intent);
            overridePendingTransition(0, R.anim.dock_bottom_exit);
            finish();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }
}
