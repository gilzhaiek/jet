package com.reconinstruments.jetapplauncher.settings;

import android.content.Intent;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;
import com.reconinstruments.utils.SettingsUtil;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends JetSettingsListActivity {
    
    protected void setupSettingsItems(){
        mSettingList.add(new SettingItem(this, "Enable Calls",
					 SettingsUtil.SHOULD_NOTIFY_CALLS, true));
        mSettingList.add(new SettingItem(this, "Enable Texts",
					 SettingsUtil.SHOULD_NOTIFY_SMS, true));
        // mSettingList.add(new SettingItem(new Intent(this, AppsActivity.class), "Apps"));
        mSettingList.add(new SettingItem("Clear Notifications"));
    }
    
    @Override
    protected void settingsItemClicked(int position) {
	SettingItem item = mListAdapter.getItem(position);
	switch (position) {
	case 0:
	case 1:
	    item.toggle(this);
	    break;
	case 2:
	    showClearNotificationsOverlay();
	    
	}
	mListAdapter.notifyDataSetChanged();
	return;
    }

    public void showClearNotificationsOverlay() {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Cancel", 0, 0));
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Clear", 0, 1));
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        Fragment frg = fm.findFragmentByTag("clear_notifications");
        if (frg == null) {
            ClearNotificationsOverlay overlay = new ClearNotificationsOverlay("CLEAR NOTIFICATIONS?", "All items in Notification Center will be deleted.", list, R.layout.title_body_carousel, this);
            overlay.show(fm.beginTransaction(), "clear_notifications");
        }
    }
    
    public void dismissClearNotificationsOverlay() {
        Fragment overlay = getSupportFragmentManager().findFragmentByTag("clear_notifications");
        if (overlay != null) {
            DialogFragment df = (DialogFragment) overlay;
            df.dismissAllowingStateLoss();
        }
    }
}
