package com.reconinstruments.phone.message;

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.widget.TextView;

import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.phone.PhoneLogProvider;
import com.reconinstruments.phone.PhoneUtils;
import com.reconinstruments.phone.R;
import com.reconinstruments.utils.TimeUtils;



public class CallHistoryActivity extends Activity {

	String contact;
	String source;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_call_history);
		
		TextView titleView = (TextView) findViewById(R.id.title);
		TextView historyView = (TextView) findViewById(R.id.history);
		TextView dateView = (TextView) findViewById(R.id.msgDate);
		
		contact = getIntent().getStringExtra("contact");
		titleView.setText(contact);
		
		Uri baseUri = PhoneLogProvider.CONTENT_URI;

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		String select = "(" + PhoneLogProvider.KEY_TYPE + "="+PhoneLogProvider.TYPE_CALL+") AND "
		+PhoneLogProvider.KEY_CONTACT+"='"+contact+"'";

		Cursor c = getContentResolver().query(baseUri, CALLS_PROJECTION, select, null, null);
		
		ArrayList<Date> missedCalls = new ArrayList<Date>();
		
		if(c.moveToFirst()){
			source = c.getString(0);
			do {
				long date = Long.parseLong(c.getString(3));
				boolean missed = c.getInt(1)==1;
				if(missed)
					missedCalls.add(new Date(date));
			} while(c.moveToNext());
		}
		
		String history = "";
		if(missedCalls.size()>0){
			history += "Calls\n";
			for(Date missedCall:missedCalls){
				history += TimeUtils.getTimeString(missedCall)+"\n";
			}

			dateView.setText(TimeUtils.getDateString(missedCalls.get(0)));
		}
		historyView.setText(history);
		
		historyView.setMovementMethod(new ScrollingMovementMethod());
		
		int category_id = getIntent().getIntExtra("category_id",-1);
		if (category_id != -1) {
		    ReconMessageAPI.markAllMessagesInCategoryAsRead(this, category_id);
		}
	}
	
	static final String[] CALLS_PROJECTION = new String[] {
		PhoneLogProvider.KEY_SOURCE,
		PhoneLogProvider.KEY_MISSED,
		PhoneLogProvider.KEY_INCOMING,
		PhoneLogProvider.KEY_DATE,
	};

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER ||
		   keyCode == KeyEvent.KEYCODE_ENTER){
			//PhoneUtils.startCall(source, contact, this); //no calling for now!
		}
		return super.onKeyUp(keyCode, event);
	}
}
