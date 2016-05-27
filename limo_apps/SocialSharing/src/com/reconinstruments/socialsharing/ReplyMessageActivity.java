package com.reconinstruments.socialsharing;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import com.reconinstruments.hudservice.helper.BTPropertyReader;
import com.reconinstruments.mobilesdk.social.SocialStatsMessage;
import com.reconinstruments.hudservice.helper.HUDConnectivityHelper;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;

public class ReplyMessageActivity extends Activity{
	
	private static final String TAG = "ReplyMessageActivity";
	private static final String INTENT = "com.reconinstruments.social.facebook.tophone.message";
	private static final String SENDER = "com.reconinstruments.socialsharing.ReplyMessageActivity";
	private static final int REQ_KEY = 0;
	
	private ProgressDialog progressDialog;
	private CountDownTimer failedTimer;
	private ReplyMessageReceiver mReplyMessageReceiver = new ReplyMessageReceiver();
	
	private String[] cannedItems;
	private String threadId;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recon_canned_list);
        
        threadId = getIntent().getStringExtra("thread_id");
        
        registerReceiver(mReplyMessageReceiver, new IntentFilter(SENDER));
        ListView cannedListView = (ListView) findViewById(android.R.id.list);
        cannedItems = getResources().getStringArray(R.array.default_social_reply_message);
        ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.recon_canned_item, cannedItems);
        cannedListView.setAdapter(adapter);
        
        cannedListView.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(BTPropertyReader.getBTConnectionState(ReplyMessageActivity.this) != 2){
					startActivity(new Intent(ReplyMessageActivity.this,ConnectPhoneActivity.class));
					finish();
				}else{
					postXMLMessage(cannedItems[position]);
				}
			}
        });
	}
	
	private void postXMLMessage(String msg) {
		progressDialog = new ProgressDialog(ReplyMessageActivity.this);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
		progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
		TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
		textTv.setText("Sending message...");

		Pair pair = new Pair(101, System.currentTimeMillis());
		SocialStatsMessage.SocialStats stats = new SocialStatsMessage.SocialStats();
		stats.allTimeMaxSpeed =  pair;
		String xmlMsg = SocialStatsMessage.compose(stats); 
		Log.d(TAG, "xmlMsg = " + xmlMsg);
		sendHUDConnectivityMessage(xmlMsg);
		
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
