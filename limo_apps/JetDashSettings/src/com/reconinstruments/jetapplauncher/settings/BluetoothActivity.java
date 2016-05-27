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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.jetapplauncher.R;

import com.reconinstruments.utils.BTHelper;

public class BluetoothActivity extends JetSettingsListActivity {
    protected void setupSettingsItems(){
	// Have disabled bluetooth enabled/disable: check logs

	// Had stock bluetooth setting here. 
        Intent btIntent = new Intent(Intent.ACTION_MAIN);
        btIntent.setClassName("com.android.settings",
			      "com.android.settings.bluetooth.BluetoothSettings");     
        mSettingList.add(new SettingItem(btIntent, "Pair Device"));
        mSettingList.add(new SettingItem(new Intent("com.reconinstruments.jetunpairdevice.UNPAIR"), "Forget Devices"));
	}
	
    @Override
    protected void settingsItemClicked(int position) {
    }

    @Override
    public void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.fade_slide_in_left,0);
    }
    
}
