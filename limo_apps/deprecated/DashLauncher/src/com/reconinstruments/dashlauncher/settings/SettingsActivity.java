package com.reconinstruments.dashlauncher.settings;

import java.util.ArrayList;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.settings.advanced.AdvancedActivity;


public class SettingsActivity extends ListActivity {

	
	
	public static final int BROADCAST_INTENT = 0;
	private TranscendServiceConnection mTranscendConnection = null;
	private ArrayList<Handler> handlerList = new ArrayList<Handler>();

	ArrayList<SettingItem> settingList = new ArrayList<SettingItem>();


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.settings_list_layout);

		settingList.add(new SettingItem(new Intent(this, BrightnessActivity.class), R.drawable.setting_icons_brightness_selectable, "Brightness"));
		settingList.add(new SettingItem(new Intent("com.reconinstruments.connectdevice.CONNECT"), R.drawable.setting_icons_smartphone_selectable, "Smartphone Connection"));
		
		Intent btIntent = new Intent(Intent.ACTION_MAIN);
		btIntent.setClassName("com.android.settings", "com.android.settings.bluetooth.BluetoothSettings");     
		settingList.add(new SettingItem(btIntent, R.drawable.setting_icons_bluetooth_selectable, "Bluetooth"));
		//settingList.add(new SettingItem(new Intent(this, BluetoothActivity.class), R.drawable.setting_icons_bluetooth, "Bluetooth"));
		
		settingList.add(new SettingItem(new Intent(this, DateTimeActivity.class), R.drawable.setting_icons_time_selectable, "Set Time"));
		settingList.add(new SettingItem(new Intent(this, ResetStatsActivity.class), R.drawable.setting_icons_resetstats_selectable, "Reset Stats"));
		settingList.add(new SettingItem(new Intent(this, SetUnitsActivity.class), R.drawable.setting_icons_units_selectable, "Set Units"));
		settingList.add(new SettingItem(new Intent(this, UpdateActivity.class), R.drawable.setting_icons_update_selectable, "Software Update"));
		settingList.add(new SettingItem(new Intent("com.reconinstruments.compass.CALIBRATE"), R.drawable.setting_icons_compass_selectable, "Compass Calibration"));
		settingList.add(new SettingItem(new Intent(this, AboutActivity.class),R.drawable.setting_icons_about_selectable, "About"));
		settingList.add(new SettingItem(new Intent(this, HelpActivity.class), R.drawable.setting_icons_help_selectable, "Help"));
		settingList.add(new SettingItem(new Intent(this, AdvancedActivity.class), R.drawable.setting_icons_advanced_selectable, "Advanced"));
		
		mTranscendConnection = new TranscendServiceConnection( this );            
        boolean connect = bindService( new Intent( "RECON_MOD_SERVICE" ), mTranscendConnection, Context.BIND_AUTO_CREATE );

        Util.TRANSEND_SERVICE = mTranscendConnection;
        
        // Bluetooth intent registering
        IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(mReceiver, intentFilter);
		
		ListAdapter mListAdapter = new SettingMainAdapter(this,settingList);
		
		this.getListView().setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				startActivity(settingList.get(position).intent);
			}
			
		});
		
		setListAdapter(mListAdapter);
		
		
	}

	public void onStart() {
		super.onStart();

	}
	
	@Override 
    protected void onDestroy() {
    	super.onDestroy();
    	        
        //unbind Transcend Service connection
        unbindService( mTranscendConnection );
        
        //unbind broadcast receiver
        unregisterReceiver(mReceiver);
        //unregisterReceiver(BatteryBroadcastReceiver.getInstance());
    }
	
	public void registerIntentHandler(Handler h) {
    	handlerList.add(h);
    }
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			for(Handler h : handlerList) {
				Message m = new Message();
				m.what = BROADCAST_INTENT;
				m.obj = intent;
				h.sendMessage(m);
			}
		}
	};

	private class SettingMainAdapter extends ArrayAdapter<SettingItem>{

		Context context = null;
		ArrayList<SettingItem> settings = null;


		public SettingMainAdapter(Context context, ArrayList<SettingItem> settings) {
			super(context, R.layout.settings_list_item, settings);
			this.context = context;
			this.settings = settings;
		}

		@Override
		public SettingItem getItem(int position) {
			return settings.get(position);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent){

			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.settings_list_item, null);
			ImageView icon = (ImageView) convertView.findViewById(R.id.setting_icon);
			TextView text = (TextView) convertView.findViewById(R.id.setting_text);
			
			icon.setImageResource(settings.get(position).iconId);
			text.setText(settings.get(position).title);
			
			return convertView;
		}

	}
}
