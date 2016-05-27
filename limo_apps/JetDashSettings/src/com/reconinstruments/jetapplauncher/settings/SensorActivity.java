package com.reconinstruments.jetapplauncher.settings;
import android.content.Intent;
public class SensorActivity extends JetSettingsListActivity {
    
    protected void setupSettingsItems(){
        mSettingList.add(new SettingItem(null, "Add Sensors"));
        mSettingList.add(new SettingItem(null, "Sensor Type"));
        mSettingList.add(new SettingItem(new Intent(this, AntBleSwitchActivity.class),
                                         "ANT/BLE"));
    }

    @Override
    protected void settingsItemClicked(int position) {
        // No special treatment
    }
}
