package com.reconinstruments.phone.dialogs;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.reconinstruments.messagecenter.MessageDBSchema.MessagePriority;
import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.phone.PhoneLogProvider;
import com.reconinstruments.phone.PhoneUtils;
import com.reconinstruments.phone.R;
import com.reconinstruments.phone.service.PhoneRelayService;
import com.reconinstruments.utils.UIUtils;
import com.reconinstruments.utils.DeviceUtils;

public class IncomingCallActivity extends Activity {

	private static final String TAG = "IncomingCallActivity";

	TextView fromTV;

	private Uri callUri; // The URI of the incoming call or sms in the database

	private String mSource;
	private String mContact;
	private boolean mIsiOS;
	private boolean mIsHfp;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		Intent i = getIntent();
		Bundle b = i.getExtras();

		if(b!=null){
			// get the row of this incoming call/sms from the db
			callUri = Uri.parse(b.getString("uri"));
			mIsiOS = b.getBoolean("isiOS");
			mIsHfp = b.getBoolean("isHfp");
			long id = ContentUris.parseId(callUri);
			ContentResolver cr = getContentResolver();
			Cursor c = cr.query(PhoneLogProvider.CONTENT_URI, null, "_id = "+id, null, null);

			Bundle cursorBundle = PhoneLogProvider.cursorToBundleList(c).get(0);
			mSource = cursorBundle.getString("source");
			mContact = cursorBundle.getString("contact");
		} 
		getWindow().setBackgroundDrawableResource(android.R.color.transparent); 
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(DeviceUtils.isSun()){
            setContentView(R.layout.activity_call_progress_jet);
        }else{
            setContentView(R.layout.activity_call_progress);
        }
        getWindow().setLayout(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT); 
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount=0.7f;
        getWindow().setAttributes(lp);

		fromTV = (TextView) findViewById(R.id.from);
		if (mContact == null) {
		    mContact = "Unknown";
		}
		if(mContact.equals("Unknown")) {
		    fromTV.setText(mSource);
		}
		else  {
		    fromTV.setText(mContact);
		}
		
		if(DeviceUtils.isSun()){
//    		TextView actionTV= (TextView) findViewById(R.id.alertAction);
//    		Typeface semiboldTypeface = UIUtils.getFontFromRes(getApplicationContext(), R.raw.opensans_semibold);
//    		actionTV.setTypeface(semiboldTypeface);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(phoneStateReceiver, new IntentFilter(PhoneRelayService.INTENT_CALL_STARTED));
		registerReceiver(phoneStateReceiver, new IntentFilter(PhoneRelayService.INTENT_CALL_ENDED));
	}
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(phoneStateReceiver);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER || (keyCode==KeyEvent.KEYCODE_ENTER)){
            PhoneUtils.answerCall(this, mIsiOS, mIsHfp, mSource, mContact);
            finish();
            return true;
        }else if(keyCode==KeyEvent.KEYCODE_BACK){
            PhoneUtils.endCall(this, mIsiOS, mIsHfp,true);
            postPhoneNotification();
            finish();
            return true;
        }
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		PhoneUtils.endCall(this, mIsiOS, mIsHfp,true);
		postPhoneNotification();
		finish();
		
		super.onBackPressed();
	}

	BroadcastReceiver phoneStateReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "got broadcast: "+intent.getAction()+" finishing incoming call activity");
			if (intent.getAction().equals(PhoneRelayService.INTENT_CALL_ENDED)) {
			    // Means it wasn't picked up
			    postPhoneNotification();
			    }
			finish();
		}
	};

    private void postPhoneNotification() {
	// post a notification to message center
	Intent viewCallHistory = new Intent("RECON_CALL_HISTORY");
	viewCallHistory.putExtra("contact", mContact);
	// TODO: After changes to Message Center to show cat in
	// groupView we need to swap contact with source
	// String catName = (mContact.equals("Unknown"))? mContact: mSource;
	// String catDesc = (mContact.equals("Unknown"))? mContact: mSource
	// String msgTxt = (mContact.equals("Unknown"))? mSource: mContact;
	// The above TODO to show cat in groupview is still valid
	String catName = mSource;
	String catDesc = mSource;
	String msgTxt = mContact;

	Log.v(TAG,"new post phone notifiication");
	ReconMessageAPI.ReconNotification rn = new ReconMessageAPI
	    .ReconNotification(this.getApplicationContext(),
			       "com.reconinstruments.calls", // group name
			       "MISSED CALLS",		     // group description
			       R.drawable.phone_icon,   // group icon
			       catName,		     // subgroup name
			       catDesc,		     // subgroup description
			       R.drawable.phone_icon,			     // subgroup icon
			       msgTxt			     // messageText
			       );
	rn.overrideMessageViewer(viewCallHistory);
	rn.setExtra(callUri.toString());
	ReconMessageAPI.postNotification(rn,
					 false, // updateable
					 false	// show interactive
					 );
    }
}
