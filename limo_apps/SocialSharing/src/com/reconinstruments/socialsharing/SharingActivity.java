package com.reconinstruments.socialsharing;

import java.util.ArrayList;

import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;
import com.reconinstruments.hudservice.helper.HUDConnectivityHelper;
import com.reconinstruments.hudservice.helper.BTPropertyReader;
import com.reconinstruments.commonwidgets.TwoOptionsJumpFixer;
import com.reconinstruments.commonwidgets.ReconToast;
import com.reconinstruments.commonwidgets.TwoOptionsItem;
import com.reconinstruments.commonwidgets.TwoOptionsAdapter;
import com.reconinstruments.mobilesdk.social.SocialStatsMessage;

public class SharingActivity extends ListActivity {
	
	private static final String TAG = "SharingActivity";
	private static final String INTENT = "com.reconinstruments.social.share";
	private static final String SENDER = "com.reconinstruments.socialsharing.SharingActivity";
	private ProgressDialog progressDialog;
	private TwoOptionsJumpFixer twoOptionsJumpFixer;
	private PostingReceiver mPostingReceiver = new PostingReceiver();
	private CountDownTimer failedTimer;
	private TextView headerTitleTV;
	private TextView contentTV;
	
	private String category;
	private String title;
	private int stat;
	private float airStat;
	private long when;
	private String valueAndUnit;
	private boolean tryAgain = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
		setContentView(R.layout.sharing);

		registerReceiver(mPostingReceiver, new IntentFilter(SENDER));

		Intent intent = getIntent();
		category = intent.getStringExtra("category");
		title = intent.getStringExtra("title");
		valueAndUnit = intent.getStringExtra("valueAndUnit");
		tryAgain = intent.getBooleanExtra("tryAgain", false);
		stat = intent.getIntExtra("stat", 0);
		airStat = intent.getFloatExtra("airStat", 0.0f);
		when = intent.getLongExtra("when", (long)(System.currentTimeMillis()/1000));

		headerTitleTV = (TextView) findViewById(R.id.header_title);
		headerTitleTV.setText("SHARE TO FACEBOOK");
		contentTV = (TextView) findViewById(R.id.content_text);
			setHtmlText(title, valueAndUnit);

		ArrayList<TwoOptionsItem> options = new ArrayList<TwoOptionsItem>();
		options.add(new TwoOptionsItem("POST TO FACEBOOK" ));
		options.add(new TwoOptionsItem("CANCEL" ));
		setListAdapter(new TwoOptionsAdapter(this, options));
		if(tryAgain){
			if(twoOptionsJumpFixer != null){
				twoOptionsJumpFixer.stop();
			}
			postXMLMessage();
		}else{
			twoOptionsJumpFixer = new TwoOptionsJumpFixer(getListView());
			twoOptionsJumpFixer.start();
			getListView().setOnItemClickListener(new OnItemClickListener(){

				public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
					
					if(twoOptionsJumpFixer != null){
						twoOptionsJumpFixer.stop();
					}
					if (position == 0){
						if(BTPropertyReader.getBTConnectionState(SharingActivity.this) != 2){
							startActivity(new Intent(SharingActivity.this,ConnectPhoneActivity.class));
						}else{
							int socialConnected = 0;
							try {
								socialConnected = Settings.System.getInt(SharingActivity.this.getContentResolver(), "isFacebookConnected");
							} catch (SettingNotFoundException e) {
								e.printStackTrace();
							}
							if(socialConnected != 0){
								postXMLMessage();
							}else{
								startActivity(new Intent(SharingActivity.this,ConnectFacebookActivity.class));
							}
						}
					}
					else{
						finish();	
					}				
				}
			});
		}
	}

	@Override
	protected void onResume() {
		Intent intent = getIntent();
		category = intent.getStringExtra("category");
		title = intent.getStringExtra("title");
		valueAndUnit = intent.getStringExtra("valueAndUnit");
		tryAgain = intent.getBooleanExtra("tryAgain", false);
		stat = intent.getIntExtra("stat", 0);
		airStat = intent.getFloatExtra("airStat", 0.0f);
		
		when = intent.getLongExtra("when", (long)(System.currentTimeMillis()/1000));
		super.onResume();
	}

	private void postXMLMessage() {
		progressDialog = new ProgressDialog(SharingActivity.this);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		progressDialog.show();
		progressDialog.setContentView(com.reconinstruments.commonwidgets.R.layout.recon_progress);
		TextView textTv = (TextView) progressDialog.findViewById(R.id.text);
		textTv.setText("Posting to Facebook...");
		
		Pair pair = new Pair(stat, (long)when);
		SocialStatsMessage.SocialStats stats = new SocialStatsMessage.SocialStats();
		
		if(StatActivity.CATEGORY_VERTICAL.equals(category)){
			stats.allTimeVertical =  pair;
		}else if(StatActivity.CATEGORY_DISTANCE.equals(category)){
			stats.allTimeDistance =  pair;
		}else if(StatActivity.CATEGORY_ALTITUDE.equals(category)){
			stats.allTimeMaxAltitude =  pair;
		}else if(StatActivity.CATEGORY_AIR.equals(category)){
			pair = new Pair(airStat, (long)when);
			stats.allTimeAir =  pair;
		}else if(StatActivity.CATEGORY_SPEED.equals(category)){
			stats.allTimeMaxSpeed =  pair;
		}else{
			Log.w(TAG, "skip to invalid category:" + category);
			return;
		}
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

	private void setHtmlText(String title, String value) {
		contentTV.setText(Html.fromHtml("Are you sure you want to post your <b>"+ title + "</b> of <b>" + value + "</b> to your Facebook Wall?"));
	}
	
	private void sendHUDConnectivityMessage(String xmlMsg){
		HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
		cMsg.setIntentFilter(INTENT);
		cMsg.setRequestKey(0);
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
		if(twoOptionsJumpFixer != null){
			twoOptionsJumpFixer.stop();
		}
		try{
			unregisterReceiver(mPostingReceiver);
		}catch(IllegalArgumentException e){
			//ignore
		}
		super.onDestroy();
	}
	
	private class PostingReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(SENDER)) {
				boolean result = intent.getBooleanExtra("result", false);
				if(result){
					if(progressDialog != null && progressDialog.isShowing()){
						progressDialog.dismiss();
					}
					postStatusToStatusBar(SharingActivity.this, "Posting", "Posting to Facebook...");
					finish();
				}else{
					postFailed();
				}
			}
		}
	}
	
    private void postStatusToStatusBar(Context context, String title, String text){
		NotificationManager notificationManager = (NotificationManager) 
				context.getSystemService(Context.NOTIFICATION_SERVICE); 
		PendingIntent pIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);
		Notification n  = new Notification.Builder(context)
        .setContentTitle(title)
        .setContentText(text)
        .setSmallIcon(R.drawable.facebook_sharing)
        .setAutoCancel(true).build();
		notificationManager.notify(SocialMessageReceiver.NOTIFICATION_POSTING, n); 
    }

	private void postFailed() {
		if(progressDialog != null && progressDialog.isShowing()){
			progressDialog.dismiss();
		}
		if(progressDialog != null){
			failedTimer.cancel();
		}
		if(twoOptionsJumpFixer != null){
			twoOptionsJumpFixer.stop();
		}
		Intent intent = new Intent(this,PostFailedActivity.class);
		intent.putExtra("category", category);
		intent.putExtra("title", title);
		intent.putExtra("valueAndUnit", valueAndUnit);
		intent.putExtra("stat", stat);
		intent.putExtra("when", when);
		intent.putExtra("tryAgain", true);
		startActivity(intent);
		finish();	
	}
}
