package com.reconinstruments.phone;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.reconinstruments.modlivemobile.bluetooth.BTCommon;

public class DisplaySMSActivity extends Activity {

    String mSource, mBody, mContact;
    Date mTime;
    ScrollView messageScrollView;
    Uri smsUri;
    BroadcastReceiver btReceiver;
    boolean phoneConnected = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_display);
        
        Intent i = getIntent();
    	Bundle b = i.getExtras();
    	if (b != null) {
        	mSource = b.getString("source");
        	mBody = b.getString("body");
        	mContact = b.getString("contact");
        	smsUri = Uri.parse(b.getString("uri"));
        	//mTime = b.getString("time");
        	mTime = new Date(b.getLong("time"));
    	}
    	
    	btReceiver = new PhoneConnectionReceiver();
    	registerReceiver(btReceiver, new IntentFilter(BTCommon.MSG_STATE_UPDATED));
	}
    public void onStart() {
		super.onStart();
		
		View selectIcon = findViewById(R.id.selectReplyIcon);
		
		TextView mTitle = (TextView) findViewById(R.id.dialogTitle);
		TextView mDate = (TextView) findViewById(R.id.dialogDate);
		TextView mText = (TextView) findViewById(R.id.dialogTxt);
		TextView replyTxt = (TextView) findViewById(R.id.replyTxt);
		TextView dismissTxt = (TextView) findViewById(R.id.dismissTxt);
		TextView seeMoreTxt = (TextView) findViewById(R.id.seeMoreTxt);
		
		Typeface tf = Typeface.createFromAsset(this.getAssets(), "fonts/Eurostib.ttf");
		mTitle.setTypeface(tf);
		mDate.setTypeface(tf);
		mText.setTypeface(tf);
		replyTxt.setTypeface(tf);
		dismissTxt.setTypeface(tf);
		seeMoreTxt.setTypeface(tf);
		
		try {
		    //if (mContact != null && mContact.length() > 0) mTitle.setText(mContact);
		    //else mTitle.setText(mSource);
		    
			if(mContact == null || mContact.equals("Unknown")) 
				mTitle.setText(mSource);
			else 
				mTitle.setText(mContact.toUpperCase());
			
		    mText.setText(mBody);
		    //mDate.setText(mTime);
		    
		    long currentDateInMillis = (new Date()).getTime();
			long eventDateInMillis = mTime.getTime();
			
			int diffInDays = (int) Math.floor((currentDateInMillis - eventDateInMillis) / (24 * 60 * 60 * 1000));
			if(diffInDays == 0) {
				SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa", Locale.US);
				mDate.setText(sdf.format(mTime));
			} else {
				SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.yy h:mm aa", Locale.US);
				mDate.setText(sdf.format(mTime));
			}
		} catch (NullPointerException e) {
		    // broken.
		}
		
		if(BTCommon.isConnected(getApplicationContext())){
			replyTxt.setVisibility(View.VISIBLE);
			selectIcon.setVisibility(View.VISIBLE);
		} else {
			replyTxt.setVisibility(View.GONE);
			selectIcon.setVisibility(View.GONE);
		}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	unregisterReceiver(btReceiver);
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
    	
    	/* Once everything has been rendered, check to see if
    	 * the arrow needs to displayed and display it. 
    	 */
    	displayArrow();
    }
    
    @Override
    public void onUserInteraction() {
    	super.onUserInteraction();
    	
    	/* This is a hack
    	 * ScrollView does not provide a way to listen for
    	 * scroll events. This listens for any user interaction
    	 * with this activity, and recalculates whether or not
    	 * to show the arrow below the text.
    	 */
    	displayArrow();
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
		    Intent i = new Intent(this, SMSReplyActivity.class);
		    i.putExtra("source", mSource);
		    i.putExtra("contact", mContact);
		    i.putExtra("uri", smsUri.toString());
		    startActivity(i);
	        return true;
		}
		return super.onKeyUp(keyCode, event);
    }
    
    /**
     * Calculates whether or not an arrow should be displayed beneath a message
     * and shows or hides it accordingly.
     */
    private void displayArrow() {
    	View bottomScrollView = findViewById(R.id.scrollViewBottom);
    	ScrollView scroll = (ScrollView) findViewById(R.id.dialogScrollContainer);
    	
    	Rect bounds = new Rect();
    	bottomScrollView.getHitRect(bounds);

        Rect scrollBounds = new Rect();
    	scroll.getDrawingRect(scrollBounds);

        if(Rect.intersects(scrollBounds, bounds)) {
            (findViewById(R.id.showMore)).setVisibility(View.GONE);
            (findViewById(R.id.replyDismiss)).setVisibility(View.VISIBLE);
        } else {
        	(findViewById(R.id.showMore)).setVisibility(View.VISIBLE);
        	(findViewById(R.id.replyDismiss)).setVisibility(View.GONE);
        }
        
        
    }
    
    class PhoneConnectionReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v("DisplaySMSActivity", "action: "+ intent.getAction());
			if(intent.getAction().equals(BTCommon.MSG_STATE_UPDATED)) {
				phoneConnected = intent.getBooleanExtra("bt_connected", false);
				
				Log.v("DisplaySMSActivity", phoneConnected ? "Phone Connected" : "Phone Not Connected");
				
				View selectIcon = findViewById(R.id.selectReplyIcon);
				View replyTxt = findViewById(R.id.replyTxt);
				
				if(phoneConnected) {
					replyTxt.setVisibility(View.VISIBLE);
					selectIcon.setVisibility(View.VISIBLE);
				} else {
					replyTxt.setVisibility(View.GONE);
					selectIcon.setVisibility(View.GONE);
				}
			}
		}
		
	}
    
}
