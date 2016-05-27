package com.reconinstruments.phone;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.reconinstruments.applauncher.phone.PhoneLogProvider;

public class IncomingDialogActivity extends Activity {

    private String mSource;
    private String mBody;
    private String mContact;
    private int mType;
    private Uri callSmsUri; // The URI of the incoming call or sms in the database
    
    private static final String ANSWER_CALL_MSG = "<recon intent=\"RECON_PHONE_CONTROL\"><action type=\"answer_incoming_call\" /></recon>";
    private static final String IGNORE_CALL_MSG = "<recon intent=\"RECON_PHONE_CONTROL\"><action type=\"ignore_incoming_call\" /></recon>";
    //private static final String CALL_ENDED_MSG = "RECON_INCOMING_CALL_ENDED";
    
    //CallEndedReceiver mCallEndedReceiver = null;
    
    // Dialog types.  Don't use zero because the Bundle getInt method returns 0 on failure.
    public static final int TYPE_SMS = 1;
    public static final int TYPE_CALL = 2;
    
    private static final String TAG = "NotificationDialogActivity";
    
    @Override
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    	
    	Intent i = getIntent();
    	Bundle b = i.getExtras();
    	
    	// get the row of this incoming call/sms from the db
    	callSmsUri = Uri.parse(b.getString("uri"));
    	long id = ContentUris.parseId(callSmsUri);
    	ContentResolver cr = getContentResolver();
		Cursor c = cr.query(PhoneLogProvider.CONTENT_URI, null, "_id = "+id, null, null);
		
		Bundle cursorBundle = PhoneLogProvider.cursorToBundleList(c).get(0);
		mType = cursorBundle.getInt("type");
		mSource = cursorBundle.getString("source");
		mContact = cursorBundle.getString("contact");
		mBody = cursorBundle.getString("body");
    	
		// set up view for this type of incoming call/sms
    	switch(mType) {
    	    case TYPE_SMS:
    	    	Log.d(TAG,"SMS type");
    	    	setContentView(R.layout.dialog_sms);
    		break;
    		
    	    case TYPE_CALL:
    	    	Log.d(TAG," Call type");
    	    	setContentView(R.layout.dialog_call);
    		break;
    	    
    	    default:
    	    	this.finish();
    	}
    	
    	getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

    }
    
    public void onStart() {
		super.onStart();	
		try {
		    TextView callFromTxt = (TextView) findViewById(R.id.callFromText);
		    TextView dialogTitle = (TextView) findViewById(R.id.dialogTitle);
		    TextView answerTxt = (TextView) findViewById(R.id.answerTxt);
		    TextView dismissTxt = (TextView) findViewById(R.id.dismissTxt);
		    
		    Typeface tf = Typeface.createFromAsset(this.getAssets(), "fonts/Eurostib.ttf");
		    if(callFromTxt != null) callFromTxt.setTypeface(tf);
		    if(dialogTitle != null) dialogTitle.setTypeface(tf);
		    if(answerTxt != null) answerTxt.setTypeface(tf);
		    if(dismissTxt != null) dismissTxt.setTypeface(tf);
		    
		    Log.d(TAG, "mContact: " + mContact.length());
		    
		    switch (mType) {
				case TYPE_SMS:
					/*
				    if (mContact != null && !(mContact.length() < 1))
				    	callFromTxt.setText(mContact.toUpperCase());
				    else 
				    	callFromTxt.setText(mSource);
				    */
					if(mContact == null || mContact.equals("Unknown")) 
						callFromTxt.setText(mSource);
					else 
						callFromTxt.setText(mContact.toUpperCase());
					
				    TextView dialogTxt = (TextView) findViewById(R.id.dialogTxt);
					dialogTxt.setTypeface(tf);
				    dialogTxt.setText(mBody);
				    break;
				    
				case TYPE_CALL:
					/*
				    if (mContact != null && !(mContact.length() < 1)) 
				    	callFromTxt.setText(mContact);
				    else 
				    	callFromTxt.setText(mSource);
				    	*/
					if(mContact == null || mContact.equals("Unknown")) 
						callFromTxt.setText(mSource);
					else 
						callFromTxt.setText(mContact.toUpperCase());
				    //mCallEndedReceiver = new CallEndedReceiver(this);
				    //registerReceiver(mCallEndedReceiver, new IntentFilter(CALL_ENDED_MSG));
				    break;
		    }
		    
		} catch (NullPointerException e) {
		    Log.e(TAG, "Error manipulating UI elements.  Did you change the XML?");
		    Log.e(TAG, e.toString());
		    e.printStackTrace();
		}
    }
     
    public void onDestroy() {
    	super.onDestroy();
    	//if (mCallEndedReceiver != null) unregisterReceiver(mCallEndedReceiver);
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	switch (keyCode) {
	    	case KeyEvent.KEYCODE_BACK:
	    		if(mType == TYPE_CALL)
	    			return dismissCall();
	    		else if (mType == TYPE_SMS) {
	    			return dismissSMS();
	    		}
	    		
	    	case KeyEvent.KEYCODE_DPAD_CENTER:
	    		if(mType == TYPE_CALL)
	    			return answerCall();
	    		else if (mType == TYPE_SMS) {
	    			return replySMS();
	    		}
	    }
    	return false;
    }
    
    private boolean answerCall() {
    	// Tell phone to answer call
    	Intent i = new Intent();
	    i.setAction("RECON_SMARTPHONE_CONNECTION_MESSAGE");
	    i.putExtra("message", ANSWER_CALL_MSG);
	    IncomingDialogActivity.this.sendBroadcast(i); 
	    
	    // Update this call as answered
	    ContentResolver cr = getContentResolver();
	    ContentValues values = new ContentValues();
	    values.put(PhoneLogProvider.KEY_MISSED, 0);
	    long callid = ContentUris.parseId(callSmsUri);
    	cr.update(PhoneLogProvider.CONTENT_URI, values, "_id = "+callid, null);
	    
    	// End this activity
	    IncomingDialogActivity.this.finish();
	    
	    return true;
    }
    
    private boolean dismissCall() {
    	// Tell phone to dismiss this call
    	Intent i = new Intent();
	    i.setAction("RECON_SMARTPHONE_CONNECTION_MESSAGE");
	    i.putExtra("message", IGNORE_CALL_MSG);
	    IncomingDialogActivity.this.sendBroadcast(i);
	    
	    // End this activity
		IncomingDialogActivity.this.finish();
		
		return true;
    }
    
    private boolean replySMS() {
    	Intent intent = new Intent(IncomingDialogActivity.this, SMSReplyActivity.class);
		intent.putExtra("source", mSource);
    	intent.putExtra("uri", callSmsUri.toString());
	    IncomingDialogActivity.this.startActivity(intent);	
	    IncomingDialogActivity.this.finish();
	    
    	return true;
    }
    
    private boolean dismissSMS() {
    	IncomingDialogActivity.this.finish();
    	return true;
    }
}
