package com.reconinstruments.phone;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.applauncher.phone.PhoneLogProvider;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;

public class SMSReplyActivity extends Activity
{
    private static final String SMS_REPLY_MSG_BEGIN = "<recon intent=\"RECON_PHONE_CONTROL\"><action type=\"send_sms\">";
    private static final String SMS_REPLY_MSG_END = "</action></recon>";
    private static final String REPLY_CANDIDATES_FILE = "reply_candidates.txt";
        
    private static final String TAG = "SMSReplyActivity";
    private Typeface mUIFont = null;
    private String mSource = null; //the phone number to reply the SMS to
    private Uri smsUri = null;
    
    private String[] mReplyItems = null;
    private boolean phoneConnected = false;
    private BroadcastReceiver btReceiver;
    private View mainView, noConnectionView;
    
	/*
	 * private class for defining an ArrayAdapter that has it own view of list item
	 * 
	 */
	protected class SMSItemsAdapter extends ArrayAdapter<Object> {
		private String[] mSMSItems;
		
		public SMSItemsAdapter( Context context, int resourceId, int textViewResourceId, String[] menuItems) {
			super( context, resourceId, textViewResourceId, menuItems );
			mSMSItems = menuItems;
		}
		
		@Override
		public View getView( int position, View convertView, ViewGroup parent ) {
			View v = convertView;
			if( v == null ) {
				//create a new view from the poiCategoryitem_layout
				LayoutInflater inflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate( R.layout.sms_item, null);						
				
				TextView title = (TextView)v;
				title.setTypeface(mUIFont);
			}	
			
			TextView title = (TextView)v;
			title.setText( mSMSItems[position] );
			return v;
		}
		
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
      	Intent i = getIntent();
    	Bundle b = i.getExtras();
    	mSource = b.getString("source");
    	smsUri = Uri.parse(b.getString("uri"));
    	
    	btReceiver = new PhoneConnectionReceiver();
    	registerReceiver(btReceiver, new IntentFilter(BTCommon.MSG_STATE_UPDATED));
    	
    	LayoutInflater inflater = LayoutInflater.from(this);
    	mainView = inflater.inflate(R.layout.reply_sms_dialog, null);
    	noConnectionView = inflater.inflate(R.layout.no_connection, null);
    	
    	setContentView(BTCommon.isConnected(this) ? mainView : noConnectionView);
    }
    
    public void onStart() {
    	super.onStart();
    	
    	mUIFont = Typeface.createFromAsset(this.getAssets(), "fonts/Eurostib.ttf");
    	
    	TextView noPhoneText = (TextView) noConnectionView.findViewById(R.id.text);
    	noPhoneText.setTypeface(mUIFont);
    	
    	//load the default replies
    	loadReplyCandidates();
    	
    	
    	SMSItemsAdapter itemsAdapter = new SMSItemsAdapter( this, R.layout.sms_item, R.id.sms_reply_item, mReplyItems );
    	
	    /* Get all UI Elements. */
	    ListView smsItems  = (ListView) mainView.findViewById(R.id.smsReplies);
	    smsItems.setAdapter( itemsAdapter );
	    
	    smsItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        //the code to remove the divider of the listview
	    smsItems.setDivider(null);
	    smsItems.setDividerHeight(0);

	    smsItems.setOnItemClickListener(mSMSItemSelectedListener);
    }
     
    public void onDestroy() {
    	unregisterReceiver(btReceiver);
    	super.onDestroy();
    }
    
    /**
     * load the reply candidates
     * for user to choose
     */
    protected void loadReplyCandidates() {
    	String path = Environment.getExternalStorageDirectory() + "/ReconApps/Phone/" + REPLY_CANDIDATES_FILE;
    	File file = new File( path );
    	ArrayList<String> candidates = null;
    	if( file.exists() && file.isFile() ) {
    		try {
    			candidates = new ArrayList<String>();
    			
    			// Open the file that is the first command line parameter
    			FileInputStream fstream = new FileInputStream( path);
    			
    			// Get the object of DataInputStream
    			DataInputStream in = new DataInputStream(fstream);
    			BufferedReader br = new BufferedReader(new InputStreamReader(in));
    			String strLine;
    			
    			//Read File Line By Line
    			while ((strLine = br.readLine()) != null) {
    				candidates.add(strLine);
    			}
    			
    			//Close the input stream
    			in.close();
    		} catch (Exception e) {
    			//Catch exception if any
    			e.printStackTrace();    			  
    		}    		 
    	}    
    	
    	//there is no candidates defined at the disk
    	//load it from resources
    	if( candidates == null || candidates.size() == 0 ) {
        	//load the default replies
        	mReplyItems = this.getResources().getStringArray(R.array.default_sms_replies);
    	} else {
    		mReplyItems = candidates.toArray( new String[0]);
    	}
    }
    
    protected OnItemClickListener mSMSItemSelectedListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if(phoneConnected) {
				String msg = SMS_REPLY_MSG_BEGIN + "<num>" + mSource + "</num>"  + "<body>" +  SMSReplyActivity.this.mReplyItems[position] + "</body>";
				msg += SMS_REPLY_MSG_END;
				
				//send a message through the BT pipe
				Intent myi = new Intent();
				myi.setAction("RECON_SMARTPHONE_CONNECTION_MESSAGE");
				myi.putExtra("message", msg);
				SMSReplyActivity.this.sendBroadcast(myi);
				
				// mark sms as replied to
			    ContentResolver cr = getContentResolver();
			    ContentValues values = new ContentValues();
			    values.put(PhoneLogProvider.KEY_REPLIED, 1);
			    
			    long smsid = ContentUris.parseId(smsUri);
		    	cr.update(PhoneLogProvider.CONTENT_URI, values, "_id = " + smsid, null);
				
				SMSReplyActivity.this.finish();
			}
		}
    	
    }; 
    
    class PhoneConnectionReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v("ReconPhone", "action: "+ intent.getAction());
			if(intent.getAction().equals(BTCommon.MSG_STATE_UPDATED)) {
				phoneConnected = intent.getBooleanExtra("bt_connected", false);
				SMSReplyActivity.this.setContentView(phoneConnected ? mainView : noConnectionView);
				Log.v("ReconPhone", phoneConnected ? "Phone Connected" : "Phone Not Connected");
			}
		}
		
	}
}
