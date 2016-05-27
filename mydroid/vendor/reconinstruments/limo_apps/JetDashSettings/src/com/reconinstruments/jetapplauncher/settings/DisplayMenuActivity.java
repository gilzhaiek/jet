package com.reconinstruments.jetapplauncher.settings;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.reconinstruments.jetapplauncher.settings.advanced.UninstallActivity;
import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.DeviceUtils;

public class DisplayMenuActivity extends JetSettingsListActivity {
    private static final String TAG = DisplayMenuActivity.class.getSimpleName();
  
    protected void setupSettingsItems(){
        mSettingList.add(new SettingItem(new Intent(this, DisplayActivity.class), "Brightness"));
        if (DeviceUtils.isSun()) {
            mSettingList.add(new SettingItem(new Intent(this, GlanceActivity.class), "Glance Detection"));
        }
    }
    
    @Override
    protected void settingsItemClicked(int position) {
    }
}
