package com.reconinstruments.jetapplauncher.settings;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.os.BatteryManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.commonwidgets.R;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;

public class DeviceActivity extends JetSettingsListActivity {
    private static final String TAG = "DeviceActivity";
    
    protected void setupSettingsItems(){
        mSettingList.add(new SettingItem(new Intent("com.reconinstruments.compass.CALIBRATE").putExtra("startFromBeginning", true), "Calibrate Compass"));
        if (DeviceUtils.isSun()) {
            mSettingList.add(new SettingItem(new Intent(this, PasscodeActivity.class), "Passcode Lock"));
	}
	if (DeviceUtils.isSun()) {
	    mSettingList.add(new SettingItem(new Intent(this, SoundsActivity.class), "Sounds"));
	}
        mSettingList.add(new SettingItem(new Intent(this, TimeActivity.class), "Time"));
        mSettingList.add(new SettingItem(new Intent(this, ProfileInfoActivity.class), "User Profile"));        
        mSettingList.add(new SettingItem(new Intent(this, AboutAndHelpActivity.class), "About & Help"));

        
    }
    
    @Override
    protected void settingsItemClicked(int position) {
	mSettingList.get(position).toggle(this);
	mListAdapter.notifyDataSetChanged();
    }


	      
}
