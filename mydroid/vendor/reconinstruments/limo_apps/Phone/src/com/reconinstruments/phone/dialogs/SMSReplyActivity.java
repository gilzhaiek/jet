package com.reconinstruments.phone.dialogs;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.ListActivity;
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

import com.reconinstruments.connect.apps.ConnectHelper;
import com.reconinstruments.connect.messages.PhoneMessage;
import com.reconinstruments.phone.PhoneHelper;
import com.reconinstruments.phone.R;
import com.reconinstruments.phone.R.id;
import com.reconinstruments.phone.R.layout;
import com.reconinstruments.utils.BTHelper;

/**
 This file is deprecated. Use Reply Message Activity
 **/
@Deprecated
public class SMSReplyActivity extends ListActivity
{
    //private static final String SMS_REPLY_MSG_BEGIN = "<recon intent=\"RECON_PHONE_CONTROL\"><action type=\"send_sms\">";
    //private static final String SMS_REPLY_MSG_END = "</action></recon>";
    private static final String REPLY_CANDIDATES_FILE = "reply_candidates.txt";

    private static final String TAG = "SMSReplyActivity";
    private Typeface mUIFont = null;
    private String mContact = null; //the phone number to reply the SMS to
    private String mSource = null; //the phone number to reply the SMS to
    //private Uri smsUri = null;

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
        mContact = b.getString("contact");
        mSource = b.getString("source");
        Log.v(TAG, "contact "+mContact + "source "+mSource);
        //smsUri = Uri.parse(b.getString("uri"));

        btReceiver = new PhoneConnectionReceiver();
        registerReceiver(btReceiver, new IntentFilter(ConnectHelper.MSG_STATE_UPDATED));

        LayoutInflater inflater = LayoutInflater.from(this);
        mainView = inflater.inflate(R.layout.activity_canned_sms, null);
        noConnectionView = inflater.inflate(R.layout.no_connection, null);

        setContentView(BTHelper.isConnected(this) ? mainView : noConnectionView);
    }

    public void onStart() {
        super.onStart();
        TextView fromTV = (TextView) findViewById(R.id.from);
        String from = mContact.equals("Unknown")? mSource:mContact; //show number if no contact, otherwise show contact
        fromTV.setText("To: "+from);

        TextView noPhoneText = (TextView) noConnectionView.findViewById(R.id.text);
        noPhoneText.setTypeface(mUIFont);

        //load the default replies
        loadReplyCandidates();


        SMSItemsAdapter itemsAdapter = new SMSItemsAdapter( this, R.layout.sms_item, R.id.sms_reply_item, mReplyItems );


        this.setListAdapter( itemsAdapter );

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        //the code to remove the divider of the listview
        getListView().setDivider(null);
        getListView().setDividerHeight(0);

        getListView().setOnItemClickListener(mSMSItemSelectedListener);
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

            sendSMS(SMSReplyActivity.this.mReplyItems[position]);
        }
    };

    public void sendSMS(String body){
        Log.v(TAG,"sendSMS");
        PhoneMessage msg = new PhoneMessage(PhoneMessage.Control.SENDSMS,mSource,null,null,body);

        //String msg = SMS_REPLY_MSG_BEGIN + "<num>" + mSource + "</num>"  + "<body>" +  SMSReplyActivity.this.mReplyItems[position] + "</body>";
        //msg += SMS_REPLY_MSG_END;

        //send a message through the BT pipe
        Intent myi = new Intent();
        myi.setAction(ConnectHelper.GEN_MSG);
        myi.putExtra("message", msg.toXML());
        SMSReplyActivity.this.sendBroadcast(myi);

        finish();

        PhoneHelper.saveSMS(this,mSource,mContact,body,false);
    }

    class PhoneConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("ReconPhone", "action: "+ intent.getAction());
            if(intent.getAction().equals(ConnectHelper.MSG_STATE_UPDATED)) {
                phoneConnected = intent.getBooleanExtra("connected", false);
                SMSReplyActivity.this.setContentView(phoneConnected ? mainView : noConnectionView);
                Log.v("ReconPhone", phoneConnected ? "Phone Connected" : "Phone Not Connected");
            }
        }

    }
}
