package com.reconinstruments.musiccontrol;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.TextView;

import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;


public class MainActivity extends Activity {

	public static final String TAG = "MusicControlTest";
	public static final String MUSIC_INFO_INTENT = "RECON_SONG_MESSAGE";
	public static final String MUSIC_CONTROL_INTENT = "RECON_MUSIC_CONTROL";
	public static final String mPath = "com.reconinstruments.musiccontrol.MainActivity";

	private TextView textTV = null;
	private TextView textReplyTV = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		registerReceiver(controlReceiver, new IntentFilter(MUSIC_INFO_INTENT));
		registerReceiver(controlReceiver, new IntentFilter(mPath));
		init();

	}

	private void init() {
		startService(new Intent("RECON_HUD_SERVICE"));
		initButtons();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		unregisterReceiver(controlReceiver);
	}

	private void initButtons() {
		textTV = (TextView) findViewById(R.id.textView1);
		textReplyTV = (TextView) findViewById(R.id.textView2);
	}


	@Override
	public boolean onKeyUp(int code, KeyEvent event){
		if (event.getAction() == KeyEvent.ACTION_UP) {
			switch (code) {
			case KeyEvent.KEYCODE_BACK:
				finish();
				return true;
			case KeyEvent.KEYCODE_DPAD_CENTER:
				textTV.setText("pressed TOGGLE");
				sendMusicCommand(5);
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				textTV.setText("pressed PREVIOUS");
				sendMusicCommand(2);
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				textTV.setText("pressed NEXT");
				sendMusicCommand(1);
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				textTV.setText("pressed VOLUME DOWN");
				sendMusicCommand(4);
				return true;
			case KeyEvent.KEYCODE_DPAD_UP:
				textTV.setText("pressed VOLUME UP");
				sendMusicCommand(3);
				return true;
			}
		}
		return true;
	}

	private void sendMusicCommand(int command){
		HUDConnectivityMessage msg = new HUDConnectivityMessage();
		msg.setIntentFilter(MUSIC_CONTROL_INTENT);
		msg.setRequestKey(0);
		msg.setSender(mPath);
		String s = Integer.toString(command);
		msg.setData(s.getBytes());
		push(msg);
		Log.e(TAG,"pushed message with command: "+command);
	}


	public void push(HUDConnectivityMessage cMsg){
		Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.channel.command");

		if(i != null && cMsg != null){
			i.putExtra(HUDConnectivityMessage.TAG, cMsg.ToByteArray());
			//			i.putExtra(HUDConnectivityMessage.TAG, cMsg);
			sendBroadcast(i);
			Log.d(TAG, "sent out the message " + cMsg.toString());
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private BroadcastReceiver controlReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.e(TAG,"received broadcast with intent action: "+intent.getAction());
			if(intent.getAction().equals(mPath))
			{
				boolean result = intent.getBooleanExtra("result", false);
				Log.e(TAG,"message sent succesfully: "+result);
			}
			else if(intent.getAction().equals(MUSIC_INFO_INTENT)){
				Log.e(TAG,"received music information about a song");
				Bundle bundle = intent.getExtras();
				
				if(bundle!=null){
					String msgBytes = bundle.getString("message");
					Log.e(TAG,"message parsed: "+msgBytes);
					if(msgBytes!=null){
						Log.e(TAG,"message not null");
						textReplyTV.setText(msgBytes);
					}
				}else{
					Log.e(TAG,"bundle was NULL");
				}
			}
			else{
				Log.e(TAG,"something went really wrong");
			}
		}

	};

}
