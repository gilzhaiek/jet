package com.reconinstruments.phone.dialogs;

import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reconinstruments.phone.PhoneHelper;
import com.reconinstruments.phone.PhoneUtils;
import com.reconinstruments.phone.R;
import com.reconinstruments.utils.UIUtils;
import com.reconinstruments.utils.DeviceUtils;

public class CallProgressActivity extends Activity {

	private boolean mIsiOS;
	private boolean mIsHfp;
	private static final String TAG = "CallProgressActivity";

	boolean calling;

	private String mSource;
	private String mContact;

	LinearLayout titleBar;
	TextView fromTV;
	TextView titleTV;
	TextView durationTV;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent i = getIntent();
		Bundle b = i.getExtras();

		mSource = b.getString("source");
		mContact = b.getString("contact");
		mIsiOS = b.getBoolean("isiOS");
		mIsHfp = b.getBoolean("isHfp");

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

		titleBar = (LinearLayout) findViewById(R.id.titleBar);
		fromTV = (TextView) findViewById(R.id.from);
		titleTV = (TextView) findViewById(R.id.title);
		durationTV = (TextView) findViewById(R.id.duration);

		durationTV.setVisibility(View.GONE);

		Log.d(TAG, "mContact: " + mContact.length());
		if(mContact == null || mContact.equals("Unknown")) 
			fromTV.setText(mSource);
		else 
			fromTV.setText(mContact);

		calling = b.getBoolean("calling");

		if(calling){
			titleTV.setText("CALLING...");

		} else {
			titleTV.setText("ANSWERING...");
			// if call started isn't received in 3 seconds, exit the activity
			pickupHandler.sendEmptyMessageDelayed(0, 3000);
		}

		registerReceiver(phoneStateReceiver, new IntentFilter("RECON_CALL_STARTED"));
		registerReceiver(phoneStateReceiver, new IntentFilter("RECON_CALL_ENDED"));
        
		RelativeLayout actionView= (RelativeLayout) findViewById(R.id.action_view);
		if(DeviceUtils.isSun()){
	        TextView textView1= (TextView) findViewById(R.id.textView1);
	        TextView textView2= (TextView) findViewById(R.id.textView2);
	        TextView textView3= (TextView) findViewById(R.id.textView3);
	        ImageView image1IV= (ImageView) findViewById(R.id.image1);
	        textView1.setVisibility(View.GONE);
	        textView2.setVisibility(View.GONE);
	        textView3.setVisibility(View.VISIBLE);
	        image1IV.setVisibility(View.VISIBLE);
	        actionView.setGravity(Gravity.LEFT);
		}else{
	        TextView textView1= (TextView) findViewById(R.id.textView1);
	        TextView textView2= (TextView) findViewById(R.id.textView2);
	        ImageView image1IV= (ImageView) findViewById(R.id.imageView1);
	        ImageView image2IV= (ImageView) findViewById(R.id.imageView2);
	        textView1.setVisibility(View.VISIBLE);
	        textView2.setVisibility(View.GONE);
	        image1IV.setVisibility(View.VISIBLE);
	        image2IV.setVisibility(View.GONE);
	        image1IV.setImageResource(R.drawable.back);
	        textView1.setText("HANGUP");
	        actionView.setGravity(Gravity.LEFT);
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		unregisterReceiver(phoneStateReceiver);
	}

	@Override
	public void onBackPressed()
	{
	    PhoneUtils.endCall(this, mIsiOS, mIsHfp, false);
		cancelled = true;
        finish();
		super.onBackPressed();
	}

	BroadcastReceiver phoneStateReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals("RECON_CALL_STARTED")) {
				callStartedActions(context);
			}
			else if(intent.getAction().equals("RECON_CALL_ENDED")) {
				callEndedActions(context);
			}
		}
	};

	// Helper funciton
	private void callStartedActions(Context context) {
		if(!calling){
			titleTV.setText("CONNECTED");
			titleTV.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
			titleBar.setBackgroundResource(R.color.call_active);
			
			durationTV.setVisibility(View.VISIBLE);
			Typeface semiboldTypeface = UIUtils.getFontFromRes(getApplicationContext(), R.raw.opensans_semibold);
			durationTV.setTypeface(semiboldTypeface);

			if (!cancelled && !callDurationThread.isAlive()) {
				callDurationThread.start();
			}
			PhoneHelper.saveCall(context,mSource,mContact,false);
		}
		pickupHandler.removeMessages(0);
	}
	private void callEndedActions(Context context) {
		cancelled = true;
		finish();
	}

	boolean cancelled = false;
	Thread callDurationThread = new Thread(){
		@Override
		public void run(){
			super.run();
			int duration = 0;
			while(!cancelled){
				Message msg = new Message();
				msg.obj = timeString(duration);
				handler.sendMessage(msg);
				duration++;
				try{
					sleep(1000);
				} catch (InterruptedException e){
					e.printStackTrace();
					cancelled = true;
					return;
				}
			}
		}	
	};
	
	final Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			durationTV.setText((String)msg.obj);
		}
	};
	final Handler pickupHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			finish();
		}
	};
	public String timeString(int seconds){
		return String.format("%02d:%02d", 
				TimeUnit.SECONDS.toMinutes(seconds),
				TimeUnit.SECONDS.toSeconds(seconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds))
				);
	}
}
