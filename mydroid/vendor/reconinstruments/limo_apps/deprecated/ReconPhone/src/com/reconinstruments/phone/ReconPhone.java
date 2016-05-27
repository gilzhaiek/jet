package com.reconinstruments.phone;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.reconinstruments.applauncher.phone.PhoneLogProvider;
import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.phone.tabs.PhoneTabView;

public class ReconPhone extends Activity {
	/** Called when the activity is first created. */
	
	PhoneTabView mPhoneTabView;
	PhoneLogContentObserver mPhoneLogContentObserver;
	BroadcastReceiver btReceiver;
	boolean phoneConnected = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mPhoneLogContentObserver = new PhoneLogContentObserver(new Handler());
		
		ContentResolver cr = getContentResolver();
		cr.registerContentObserver(PhoneLogProvider.CONTENT_URI, true, mPhoneLogContentObserver);
		
		btReceiver = new PhoneConnectionReceiver();
		registerReceiver(btReceiver, new IntentFilter(BTCommon.MSG_STATE_UPDATED));
		
		mPhoneTabView = new PhoneTabView(this);
		updateLists();
		
		setContentView(mPhoneTabView);
	}
	
	public void onStart() {
		super.onStart();
		
		mPhoneTabView.setPhoneConnectionState(BTCommon.isConnected(this));
	}

	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(btReceiver);
		getContentResolver().unregisterContentObserver(mPhoneLogContentObserver);
	}
	
	public class PhoneLogContentObserver extends ContentObserver {

		public PhoneLogContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			updateLists();
		}
	}
	
	public void updateLists() {		
		mPhoneTabView.setCallHistory(getCalls());
		mPhoneTabView.setSMSHistory(getSMS());
	}
	
	private ArrayList<Bundle> getCalls() {
		ContentResolver cr = getContentResolver();
		
		Uri allCalls = Uri.parse(PhoneLogProvider.CONTENT_URI + "/calls");
		Cursor c = cr.query(allCalls, null, null, null, null);
		ArrayList<Bundle> calls = PhoneLogProvider.cursorToBundleList(c);
		c.close();
		
		return calls;
	}
	
	private ArrayList<Bundle> getSMS() {
		ContentResolver cr = getContentResolver();
		
		Uri allSMS = Uri.parse(PhoneLogProvider.CONTENT_URI+"/sms");
		Cursor c = cr.query(allSMS, null, null, null, null);
		ArrayList<Bundle> sms = PhoneLogProvider.cursorToBundleList(c);
		c.close();
		
		return sms;
	}
	
	class PhoneConnectionReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v("ReconPhone", "action: "+ intent.getAction());
			if(intent.getAction().equals(BTCommon.MSG_STATE_UPDATED)) {
				phoneConnected = intent.getBooleanExtra("bt_connected", false);
				mPhoneTabView.setPhoneConnectionState(phoneConnected);
				Log.v("ReconPhone", phoneConnected ? "Phone Connected" : "Phone Not Connected");
			}
		}
		
	}
}