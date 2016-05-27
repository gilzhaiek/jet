package com.reconinstruments.dashlauncher.settings.advanced;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.settings.SettingAdapter;
import com.reconinstruments.dashlauncher.settings.SettingItem;
import com.reconinstruments.dashsettings.R;

public class AdvancedActivity extends ListActivity {
	private static final String TAG = "AdvancedActivity";
	
	private static final int POS_UNINST = 0;
	private static final int POS_PHONE_RESET = 1;
	private static final int POS_FACT = 2;
	private static final int POS_USB = 3;
	private static final int POS_SHOW_TUTORIAL = 4;
	
	public static final String SETTING_KEY_IN_STORE_DEMO = "IN_STORE_DEMO";
	public static final String SETTING_KEY_PLAY_INTRO_ON_START = "INTRO_VIDEO_ALWAYS_PLAY";
	
	private ArrayList<SettingItem> advancedList;
	private SettingAdapter advancedAdapter;
	
	private Handler mHandler = new Handler();
	private boolean fiveSecDelayStarted = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.setting_layout);
		
		ImageView headerIcon = (ImageView) findViewById(R.id.setting_icon);
		headerIcon.setBackgroundResource(R.drawable.advanced_white);
		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("ADVANCED");
				
		advancedList = new ArrayList<SettingItem>();
		advancedList.add(new SettingItem(new Intent(this, UninstallActivity.class), "Uninstall App" ));
		advancedList.add(new SettingItem(new Intent(this, CLearPhoneHistoryActivity.class), "Clear Message History" ));
		advancedList.add(new SettingItem(new Intent(this, FactoryResetActivity.class), "Factory Reset" ));
		
		SettingItem item = new SettingItem("USB Debugging");
		int adbOn;
		try {
			adbOn = Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.ADB_ENABLED);
		} catch (SettingNotFoundException e) {
			Log.e(TAG, e.getLocalizedMessage());
			adbOn = -1;
		}
		item.checkBox=true;
		if (adbOn == 1) item.checkBoxValue = true;
		else if (adbOn == 0) item.checkBoxValue = false;
		advancedList.add(item);
		
		advancedAdapter = new SettingAdapter(this, 0, advancedList);
		
		setListAdapter(advancedAdapter);
		
		this.getListView().setOnItemClickListener(advancedListener);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(!fiveSecDelayStarted 
				&& keyCode == KeyEvent.KEYCODE_DPAD_RIGHT 
				&& this.getListView().getSelectedItemPosition() == POS_SHOW_TUTORIAL) {
			
			mHandler.postDelayed(new Runnable() {
				public void run() {
					// Turn on in store demo mode
					Settings.System.putInt(getContentResolver(), SETTING_KEY_IN_STORE_DEMO, 1);
					Settings.System.putInt(getContentResolver(), SETTING_KEY_PLAY_INTRO_ON_START, 1);
					advancedList.get(POS_SHOW_TUTORIAL).checkBoxValue = true;
					advancedAdapter.notifyDataSetChanged();
					fiveSecDelayStarted = false;
					startService(new Intent("IN_STORE_DEMO_SERVICE"));
				}}, 5000);
			fiveSecDelayStarted = true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			mHandler.removeCallbacks(null);
			fiveSecDelayStarted = false;
		}
		return super.onKeyUp(keyCode, event);
	}
	
	private OnItemClickListener advancedListener = new OnItemClickListener(){

		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
			switch (position){
			case POS_UNINST:
			case POS_FACT:
			//case POS_TUTORIAL:
				startActivity(advancedList.get(position).intent);
				break;
			case POS_USB:
				try {
					if(Settings.Secure.getInt(getBaseContext().getContentResolver(), Settings.Secure.ADB_ENABLED) == 1) {
						// Turn off ADB
						Settings.Secure.putInt(getBaseContext().getContentResolver(), Settings.Secure.ADB_ENABLED, 0);
						
						advancedList.get(POS_USB).checkBoxValue = false;
						advancedAdapter.notifyDataSetChanged();
					} else {
						// Turn on ADB
						Settings.Secure.putInt(getBaseContext().getContentResolver(), Settings.Secure.ADB_ENABLED, 1);
						
						advancedList.get(POS_USB).checkBoxValue = true;
						advancedAdapter.notifyDataSetChanged();
					}
					
				} catch (SettingNotFoundException e) {
					e.printStackTrace();
				}
				break;
			case POS_SHOW_TUTORIAL:
				if(Settings.System.getInt(getContentResolver(), SETTING_KEY_PLAY_INTRO_ON_START, 0) == 1) {
					// turn off video
					stopService(new Intent("IN_STORE_DEMO_SERVICE"));
					Settings.System.putInt(getContentResolver(), SETTING_KEY_IN_STORE_DEMO, 0);
					Settings.System.putInt(getContentResolver(), SETTING_KEY_PLAY_INTRO_ON_START, 0);
					advancedList.get(POS_SHOW_TUTORIAL).checkBoxValue = false;
				} else {
					// turn on video
					Settings.System.putInt(getContentResolver(), SETTING_KEY_PLAY_INTRO_ON_START, 1);
					advancedList.get(POS_SHOW_TUTORIAL).checkBoxValue = true;
				}
				advancedAdapter.notifyDataSetChanged();
				break;
			case POS_PHONE_RESET:				
				startActivity(advancedList.get(position).intent);
				break;
			}
		}
	};

}
