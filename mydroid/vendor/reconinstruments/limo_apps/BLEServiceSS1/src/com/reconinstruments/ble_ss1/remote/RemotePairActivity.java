package com.reconinstruments.ble_ss1.remote;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ListView;

import com.reconinstruments.ble_ss1.R;
import com.reconinstruments.ble_ss1.TheBLEService;
import com.reconinstruments.ble_ss1.TheBLEService.BLEDevice;
import com.reconinstruments.commonwidgets.CenterListView;
import com.reconinstruments.utils.DeviceUtils;

public class RemotePairActivity extends ListActivity {
	private static final String TAG = "RemotePairActivity";
	public static final String SELECT = "SELECT";
	public static final String POWER = "POWER";
	public static String sTheButton;
	ImageView remoteIcon;
	TextView titleText;
	TextView timerText;
	TextView centerText;
	TextView whiteText;
	TextView grayText;
	
	CenterListView list;

	int timer;
	boolean pairing = false; //false = ..DETECTED, true = PAIRING... 

	private TheBLEService service;
	private RemoteAdapter remoteListAdapter; 

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate"); 
		setContentView(R.layout.ble_pairing_dialog);
		sTheButton = DeviceUtils.isSnow2()? POWER: SELECT;

		remoteIcon = (ImageView) findViewById(R.id.remote);
		titleText = (TextView) findViewById(com.reconinstruments.ble_ss1.R.id.title_text);
		timerText = (TextView) findViewById(R.id.timer_text);
		centerText = (TextView) findViewById(R.id.center_text);
		whiteText = (TextView) findViewById(R.id.white_text);
		grayText = (TextView) findViewById(R.id.gray_text);
		
		list = (CenterListView) getListView();

		registerReceiver(connectReceiver, new IntentFilter("RECON_BLE_REMOTE_CONNECTED"));

		if(service==null){
			Intent bindIntent = new Intent(this,TheBLEService.class);
			bindService(bindIntent,sConnection,0);
		}
	}
	public void onDestroy(){
		if(service!=null){
			service.activityHandler = null;
			this.unbindService(sConnection);
		}
		timerHandler.removeMessages(0);
		unregisterReceiver(connectReceiver);

		super.onDestroy();
	}

	ServiceConnection sConnection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			Log.d(TAG, "Service: " + name + " connected");
			TheBLEService.MBinder mbinder = ((TheBLEService.MBinder)binder);
			service = mbinder.getService();

			// if already connected, go away
			if(service.state==TheBLEService.CONNECTED){
				finish();
			} else {
				foundRemotes(service.getRemotes());
				service.activityHandler = deviceHandler;
			}
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			service.activityHandler = null;
			service = null;
			finish();
		}
	};
	Handler deviceHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			foundNewRemote((BLEDevice)msg.obj);
		}
	};
	BroadcastReceiver connectReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			finish();
		}
	};
	// called when activity starts
	private void foundRemotes(ArrayList<BLEDevice> remotes){
		
		Log.d(TAG, "connected to service with "+remotes.size()+" remotes");

		remoteListAdapter = new RemoteAdapter (this, remotes);
		setListAdapter(remoteListAdapter);

		showRemotes();

		timerText.setVisibility(View.GONE);
	}
	// called when remote is detected after activity starts
	private void foundNewRemote(BLEDevice remote){

		remoteListAdapter.add(remote);
		if(!pairing){ // must now be more than one remote
			showRemotes();
		}
		if(pairing&&getNumRemotes()>1){
			whiteText.setText(Html.fromHtml ("PRESS <font color=\"#ffb300\">"+sTheButton+" BUTTON</font> TO CHANGE"));
			whiteText.setVisibility(View.VISIBLE);
		}
	}

	private void showRemotes(){
		if(getNumRemotes()==1){
			titleText.setText("REMOTE DETECTED");
	
			list.setVisibility(View.VISIBLE);
			
			list.setFocusable(false);
	
			centerText.setVisibility(View.GONE);
			remoteIcon.setVisibility(View.VISIBLE);
			whiteText.setText(Html.fromHtml("PRESS <font color=\"#ffb300\">"+sTheButton+" BUTTON</font> TO PAIR"));
			grayText.setVisibility(View.GONE);
		} else {
			titleText.setText(getNumRemotes()+" REMOTES DETECTED");	
	
			list.setVisibility(View.GONE);
	
			centerText.setVisibility(View.VISIBLE);	
			whiteText.setVisibility(View.VISIBLE);
			remoteIcon.setVisibility(View.VISIBLE);
			whiteText.setText(Html.fromHtml("PRESS <font color=\"#ffb300\">"+sTheButton+" BUTTON</font> TO PAIR"));
			grayText.setVisibility(View.GONE);
		}
	}
	// can't get from list or adapter directly because it is circular
	private int getNumRemotes(){
		return remoteListAdapter.getRealCount();
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.d(TAG, "onKeyUp, keycode: "+keyCode);
		if (keyCode == KeyEvent.KEYCODE_POWER) {
			powerButtonPressed();
			return true;
		} 
		else if (DeviceUtils.isSun() && keyCode != KeyEvent.KEYCODE_BACK) {
	          powerButtonPressed();
	          return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	private void powerButtonPressed(){
		if(!pairing){
		    
			list.setVisibility(View.VISIBLE);
			list.setFocusable(true);
			//if we set a listView invisible then visible, unless we request focus, selectors never appear
			list.requestFocus();

            centerText.setVisibility(View.GONE);
			
			timerText.setVisibility(View.VISIBLE);
			titleText.setText("PAIRING REMOTE IN:");
			remoteIcon.setVisibility(View.GONE);
			
			if(getNumRemotes()>1){
				whiteText.setText(Html.fromHtml("PRESS <font color=\"#ffb300\">"+sTheButton+" BUTTON</font> TO CHANGE"));
				whiteText.setVisibility(View.VISIBLE);
				grayText.setVisibility(View.GONE);
			} else {
                whiteText.setText(Html.fromHtml("PRESS <font color=\"#ffb300\">"+sTheButton+" BUTTON</font> TO"));
                whiteText.setVisibility(View.VISIBLE);
                grayText.setText(Html.fromHtml("RESTART TIMER"));
                grayText.setVisibility(View.VISIBLE);
			}
		}
		pairing = true;
		
		list.selectNext();

		timer = 10;
		timerHandler.removeMessages(0);
		timerHandler.sendEmptyMessage(0);
	}
	Handler timerHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(timer>0){
				timer--;
				String text = String.format("%02d", timer);
				timerText.setText(text);
				this.sendEmptyMessageDelayed(0, 1000);
			} else {
				selectRemote();
				this.removeMessages(0);
			}
		}
	};

	public void selectRemote(){
		
		BLEDevice selected = remoteListAdapter.getItem(list.getSelectedItemPosition());
		service.connect(selected.address);
		service.resetTimer();

		finish();
	}
}
