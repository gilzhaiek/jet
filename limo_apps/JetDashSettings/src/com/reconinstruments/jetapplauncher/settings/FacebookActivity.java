package com.reconinstruments.jetapplauncher.settings;

import android.view.View;
import android.widget.ListView;

import com.reconinstruments.jetapplauncher.R;

public class FacebookActivity extends JetSettingsListActivity {
    
    protected void setupSettingsItems(){
        mSettingList.add(new SettingItem(null, "Enable Facebook", R.drawable.checkbox_enabled_selectable));
        mSettingList.add(new SettingItem(null, "Messages", R.drawable.checkbox_enabled_selectable));
        mSettingList.add(new SettingItem(null, "Comments", R.drawable.checkbox_enabled_selectable));
        mSettingList.add(new SettingItem(null, "Likes", R.drawable.checkbox_enabled_selectable));
    }
    
    @Override
    protected void settingsItemClicked(int position) {
    }
}
