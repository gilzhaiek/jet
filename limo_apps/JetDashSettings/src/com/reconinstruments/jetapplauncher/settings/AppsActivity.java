package com.reconinstruments.jetapplauncher.settings;

import android.content.Intent;

public class AppsActivity extends JetSettingsListActivity {
    
    protected void setupSettingsItems(){
        mSettingList.add(new SettingItem(new Intent(this, FacebookActivity.class), "Facebook"));
    }
    
    @Override
    protected void settingsItemClicked(int position) {
    }
}
