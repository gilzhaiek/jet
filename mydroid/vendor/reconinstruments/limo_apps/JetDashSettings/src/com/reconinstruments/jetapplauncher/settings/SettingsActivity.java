package com.reconinstruments.jetapplauncher.settings;

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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.BTHelper;
import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.utils.DeviceUtils;


public class SettingsActivity extends JetSettingsListActivity {
	
	private BrightnessOverlay mBrightnessDialog = null;
	
	@Override 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        // Bluetooth intent registering
        IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("HUD_STATE_CHANGED");
		
		registerReceiver(mReceiver, intentFilter);
	}
    
    protected void setupSettingsItems(){
        mSettingList.add(new SettingItem(new Intent(this, DisplayMenuActivity.class), R.drawable.setting_icons_display_selectable, "Display"));
        int state = BTHelper.getInstance(this.getApplicationContext()).getBTConnectionState();
        if(state ==2){
            mSettingList.add(new SettingItem(new Intent("com.reconinstruments.connectdevice.CONNECT"), R.drawable.setting_icons_smartphone_selectable, "Smartphone", "connected"));
        }else{
            mSettingList.add(new SettingItem(new Intent("com.reconinstruments.connectdevice.CONNECT"), R.drawable.setting_icons_smartphone_selectable, "Smartphone"));
        }
        mSettingList.add(new SettingItem(new Intent(this, BluetoothActivity.class), R.drawable.setting_icons_bluetooth_selectable, "Bluetooth"));
		if (DeviceUtils.isSun()) {
		    mSettingList.add(new SettingItem(new Intent("com.reconinstruments.jetsensorconnect"), R.drawable.setting_icons_sensors_selectable, "Sensors"));
		}
        mSettingList.add(new SettingItem(new Intent(this, ActivityActivity.class), R.drawable.setting_icons_activity_selectable, "Activities"));
        mSettingList.add(new SettingItem(new Intent(this, DeviceActivity.class), R.drawable.setting_icons_device_selectable, "Device"));
        mSettingList.add(new SettingItem(new Intent(this, NotificationsActivity.class), R.drawable.setting_icons_notifications_selectable, "Notifications"));
        mSettingList.add(new SettingItem(new Intent(this, BatterySavingActivity.class), R.drawable.setting_icons_battery_selectable, "Battery Saving"));
        mSettingList.add(new SettingItem(new Intent(this, AdvancedActivity.class), R.drawable.setting_icons_advanced_selectable, "Advanced"));
    }

    @Override 
    public void onResume() {
        super.onResume();
        // deleted - animate on back pressed inside the called activity instead
        updateHUDServiceState(-1);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
	CommonUtils.launchParent(this);
    }
	
    //update the bluetooth connection state
    private void updateHUDServiceState(int state){
        if(state == -1){ //query the state
            state = BTHelper.getInstance(this.getApplicationContext()).getBTConnectionState();
        }
        SettingItem item = mSettingList.get(1); // Smartphone
        if(state == 2){
            item.subTitle = "connected";
        }else{
            item.subTitle = null;
        }
        mListAdapter.notifyDataSetChanged();
    }
    
	@Override 
    protected void onDestroy() {
    	super.onDestroy();
    	        
        //unbind broadcast receiver
        unregisterReceiver(mReceiver);
    }
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
		    String action = intent.getAction();

            if ("HUD_STATE_CHANGED".equals(action)) {
                int state = intent.getIntExtra("state", 0);
                updateHUDServiceState(state);
            }
		}
	};
	
	@Override
	protected void settingsItemClicked(int position) {
	}
}
