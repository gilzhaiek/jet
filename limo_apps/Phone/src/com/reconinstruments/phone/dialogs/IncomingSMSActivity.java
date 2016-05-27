package com.reconinstruments.phone.dialogs;

import java.text.SimpleDateFormat;
import java.util.Date;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;


//import com.reconinstruments.connect.apps.ConnectedDevice;
import com.reconinstruments.phone.BLEServiceConnectionManager;
import com.reconinstruments.phone.BLEServiceConnectionListener;
import com.reconinstruments.phone.PhoneLogProvider;
import com.reconinstruments.phone.R;
import com.reconinstruments.phone.R.id;
import com.reconinstruments.phone.R.layout;
import com.reconinstruments.utils.BTHelper;

public class IncomingSMSActivity extends Activity implements BLEServiceConnectionListener {

	private static final String TAG = "IncomingSMSActivity";

	private String mSource;
	private String mBody;
	private String mContact;
	private Date mTime;
	private BLEServiceConnectionManager mBLEServiceConnectionManager;

	private Uri smsUri; // The URI of the incoming call or sms in the database
	private boolean mIsiOS = false;
	private boolean mHaveBluetoothConnection = true; 

	private static SimpleDateFormat shortTimeFormat = new SimpleDateFormat("h:mm aa");

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		Intent i = getIntent();
		Bundle b = i.getExtras();

		// get the row of this incoming call/sms from the db
		smsUri = Uri.parse(b.getString("uri"));
		mIsiOS = b.getBoolean("isiOS",true);
		long id = ContentUris.parseId(smsUri);
		ContentResolver cr = getContentResolver();
		Cursor c = cr.query(PhoneLogProvider.CONTENT_URI, null, "_id = "+id, null, null);

		Bundle cursorBundle = PhoneLogProvider.cursorToBundleList(c).get(0);

		mSource = cursorBundle.getString("source");
		mContact = cursorBundle.getString("contact");
		mBody = cursorBundle.getString("body");
		mTime = new Date(cursorBundle.getLong("date"));

		setContentView(R.layout.activity_incoming_sms);

		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		mBLEServiceConnectionManager = new BLEServiceConnectionManager(this);

	}

	public void onStart() {
		super.onStart();
		mBLEServiceConnectionManager.initService(this);
		mHaveBluetoothConnection = BTHelper.isConnected(this);
		Log.v(TAG,"mHaveBluetoothConnection"+mHaveBluetoothConnection);

		TextView fromTV = (TextView) findViewById(R.id.from);
		TextView bodyTV = (TextView) findViewById(R.id.body);
		TextView timeTV = (TextView) findViewById(R.id.time);
		TextView replyTV = (TextView) findViewById(R.id.reply);

		Log.d(TAG, "mContact: " + mContact.length());

		if(mContact == null || mContact.equals("Unknown")) 
			fromTV.setText(mSource);
		else 
			fromTV.setText(mContact.toUpperCase());

		bodyTV.setText(mBody);


		timeTV.setText(shortTimeFormat.format(mTime));

		if (mIsiOS) {
			replyTV.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onStop() {
		mBLEServiceConnectionManager.releaseService();
		super.onStop();
	}

	@Override
	public void onBLEConnected() {
		TextView replyTV = (TextView) findViewById(R.id.reply);
		mIsiOS = mBLEServiceConnectionManager.isiOSMode();
		if (mIsiOS) {
			replyTV.setVisibility(View.INVISIBLE);
		} else if (mHaveBluetoothConnection) {
			replyTV.setVisibility(View.VISIBLE);
		} else {
			replyTV.setVisibility(View.INVISIBLE);
		}

	}
	@Override
	public void onBLEDisconnected() {

	}




	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
	    if((keyCode==KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) && !mIsiOS && mHaveBluetoothConnection){
			Intent intent = new Intent(IncomingSMSActivity.this, SMSReplyActivity.class);
			intent.putExtra("contact", mContact);
			intent.putExtra("source", mSource);
			intent.putExtra("uri", smsUri.toString());
			startActivity(intent);	
			finish();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}


}
