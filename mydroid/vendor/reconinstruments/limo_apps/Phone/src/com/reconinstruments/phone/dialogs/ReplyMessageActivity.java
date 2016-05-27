package com.reconinstruments.phone.dialogs;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.util.Pair;
import android.util.Log;

import com.reconinstruments.commonwidgets.ReconToast;

import com.reconinstruments.connect.apps.ConnectHelper;
import com.reconinstruments.hudservice.helper.BTPropertyReader;
import com.reconinstruments.hudservice.helper.HUDConnectivityHelper;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;
import com.reconinstruments.phone.R;
import com.reconinstruments.phone.PhoneHelper;
import com.reconinstruments.utils.BTHelper;
import com.reconinstruments.connect.messages.PhoneMessage;

public class ReplyMessageActivity extends Activity{

    private static final String TAG = "SMSReplyActivity";
    private static final String INTENT = "RECON_SMARTPHONE_CONNECTION_MESSAGE";
    private static final String SENDER = "com.reconinstruments.phone.dialogs.SMSReplyActivity";
    private static final int REQ_KEY = 0;
    private static final String REPLY_CANDIDATES_FILE = "reply_candidates.txt";

    private ProgressDialog progressDialog;
    private CountDownTimer failedTimer;
    private ReplyMessageReceiver mReplyMessageReceiver = new ReplyMessageReceiver();

    private String[] cannedItems;
    private String threadId;

    private String mContact = null; //the phone number to reply the SMS to
    private String mSource = null; //the phone number to reply the SMS to
    //private Uri smsUri = null;

    private String[] mReplyItems = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recon_canned_list);

        Intent i = getIntent();
        Bundle b = i.getExtras();
        mContact = b.getString("contact");
        mSource = b.getString("source");
        //smsUri = Uri.parse(b.getString("uri"));
        Log.v(TAG,"mContact "+mContact + " mSource "+mSource);

        registerReceiver(mReplyMessageReceiver, new IntentFilter(SENDER));

        TextView fromTV = (TextView) findViewById(R.id.title);
        String from = mContact.equals("Unknown")? mSource:mContact; //show number if no contact, otherwise show contact
        fromTV.setText("To: "+from);

        //load the default replies
        loadReplyCandidates();


        ListView cannedListView = (ListView) findViewById(android.R.id.list);
        cannedItems = getResources().getStringArray(R.array.default_social_reply_message);
        ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.recon_canned_item, cannedItems);
        cannedListView.setAdapter(adapter);

        cannedListView.setOnItemClickListener(new OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if(!BTHelper.isConnected(ReplyMessageActivity.this)) {
                    startActivity((new Intent("com.reconinstruments.connectdevice.CONNECT")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }else{
                    if(BTPropertyReader.getBTConnectedDeviceType(ReplyMessageActivity.this) == 0){
                        //postXMLMessage(cannedItems[position]);
                        sendSMS(cannedItems[position]);

                    }else{
                        (new ReconToast(ReplyMessageActivity.this, com.reconinstruments.commonwidgets.R.drawable.error_icon, "Can not reply sms to iPhone.")).show();
                    }
                    finish();
                }
            }
        });
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

    private void sendSMS(String body){
        PhoneMessage msg = new PhoneMessage(PhoneMessage.Control.SENDSMS,mSource,null,null,body);
        //send a message through the BT pipe
        Intent myi = new Intent();
        myi.setAction(ConnectHelper.GEN_MSG);
        myi.putExtra("message", msg.toXML());
        this.sendBroadcast(myi);
        (new ReconToast(ReplyMessageActivity.this, "Sending Message...")).show();

        PhoneHelper.saveSMS(this,mSource,mContact,body,false);
    }

    private void postXMLMessage(String msg) {
        progressDialog = new ProgressDialog(ReplyMessageActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
        progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
        TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
        textTv.setText("Sending message...");

        PhoneMessage xmlMsg = new PhoneMessage(PhoneMessage.Control.SENDSMS,mSource,null,null,msg);

        PhoneHelper.saveSMS(this,mSource,mContact,msg,false);
        sendHUDConnectivityMessage(xmlMsg.toXML());

        failedTimer = new CountDownTimer(7 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                if(progressDialog != null && progressDialog.isShowing()){
                    progressDialog.dismiss();
                }
                finish();
            }
        };
        failedTimer.start();
    }

    private void sendHUDConnectivityMessage(String xmlMsg){
        HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
        cMsg.setIntentFilter(INTENT);
        cMsg.setRequestKey(REQ_KEY);
        cMsg.setSender(SENDER);
        cMsg.setData(xmlMsg.getBytes());
        HUDConnectivityHelper.getInstance(this).push(cMsg, HUDConnectivityService.Channel.OBJECT_CHANNEL);
    }

    @Override
    protected void onDestroy() {
        if(progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        if(progressDialog != null){
            failedTimer.cancel();
        }
        try{
            unregisterReceiver(mReplyMessageReceiver);
        }catch(IllegalArgumentException e){
            //ignore
        }
        super.onDestroy();
    }

    private class ReplyMessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SENDER)) {
                boolean result = intent.getBooleanExtra("result", false);
                if(result){
                    if(progressDialog != null && progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    (new ReconToast(ReplyMessageActivity.this, com.reconinstruments.commonwidgets.R.drawable.checkbox_icon, "Message sent!")).show();
                    finish();
                }else{
                    replyFailed();
                }
            }
        }
    }


    private void replyFailed() {
        if(progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        if(progressDialog != null){
            failedTimer.cancel();
        }
        (new ReconToast(ReplyMessageActivity.this, com.reconinstruments.commonwidgets.R.drawable.error_icon, "Message failed to send!")).show();
        finish();
    }

}
