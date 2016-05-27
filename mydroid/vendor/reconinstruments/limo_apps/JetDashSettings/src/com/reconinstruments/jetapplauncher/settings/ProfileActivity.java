package com.reconinstruments.jetapplauncher.settings;

import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;
import com.reconinstruments.utils.SettingsUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends JetSettingsListActivity {
    
    protected void setupSettingsItems(){
	Intent intent;
        mSettingList.add(new SettingItem(null, "Set Units"));

	intent = new Intent(this, ProfileInfoActivity.class);
        mSettingList.add(new SettingItem(intent , "Profile Info"));
    }
    
    @Override
    protected void settingsItemClicked(int position) {
   }
}
