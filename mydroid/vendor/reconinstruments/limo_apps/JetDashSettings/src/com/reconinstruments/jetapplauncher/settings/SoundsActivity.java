package com.reconinstruments.jetapplauncher.settings;
import com.reconinstruments.utils.SettingsUtil;
import android.provider.Settings;
import android.view.View;
import android.widget.ListView;

import com.reconinstruments.jetapplauncher.R;

public class SoundsActivity extends JetSettingsListActivity {
    
    protected void setupSettingsItems(){
        mSettingList.add(new SettingItem(this, "Menu Sounds",
					 Settings.System.SOUND_EFFECTS_ENABLED,
					 true));
        mSettingList.add(new SettingItem(this, "Alert Sounds",
					 SettingsUtil.SHOULD_NOTIFICATION_SOUNDS,
					 true));
    }
    
    @Override
    protected void settingsItemClicked(int position) {
        SettingItem item = mSettingList.get(position);
	switch (position) {
	case 0:
	case 1:
	    item.toggle(this);
	    break;
	}
	mListAdapter.notifyDataSetChanged();
    }
}
