//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.jetapplauncher.settings;

import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;
import  com.reconinstruments.utils.SettingsUtil;
import  com.reconinstruments.utils.DeviceUtils;

public class ActivityActivity extends JetSettingsListActivity {
        
    protected void setupSettingsItems(){
        if (DeviceUtils.isSun()) {
            boolean shouldAutoRotate = SettingsUtil
                .getSystemIntOrSet(this,SettingsUtil.SHOULD_AUTO_ROTATE,
                                   SettingsUtil.SHOULD_AUTO_ROTATE_DEFAULT)!= 0;
            mSettingList.add(new SettingItem("Auto-Scroll Stats",shouldAutoRotate));
        }
        mSettingList.add(new SettingItem(null, "Reset Stats"));
        mSettingList.add(new SettingItem(null, "Set Units"));
    }
    
    @Override
    protected void settingsItemClicked(int position) {
        boolean isSnow = DeviceUtils.isSnow2();
        SettingItem item = mSettingList.get(position);
        if (position == 0) { //Auto-Scroll Stats
            if (isSnow) showResetStatsOverlay();
            else {
                item.toggle(this);
                SettingsUtil.setSystemInt(this,SettingsUtil.SHOULD_AUTO_ROTATE,
                                          item.isChecked()?
                                          SettingsUtil.AUTO_ROTATE_PERIOD:0);
            }
        }
        else if(position == 1){ //Reset Stats On Jet
            if (isSnow) showSetUnitsOverlay();
            else showResetStatsOverlay();
        }
        else if(position == 2){ //Set Units On jet
            showSetUnitsOverlay();
        }
        mListAdapter.notifyDataSetChanged();
    }
    
    @Override 
    public void onResume() {
        super.onResume();
        updateUnits();
    }
   
    public void updateUnits(){
        SettingItem item;
        if(DeviceUtils.isSun()){
            item = mSettingList.get(2);
        }else{
            item = mSettingList.get(1);
        }
        int unitSetting = SettingsUtil.getUnits(getBaseContext());
        if (unitSetting ==SettingsUtil.RECON_UNITS_METRIC ) {
            item.subTitle = "Metric";
        } else {
            item.subTitle = "Imperial";
        }
        
        mListAdapter.notifyDataSetChanged();
    }

    public void showResetStatsOverlay() {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Cancel", 0, 0));
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Reset", 0, 1));
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        Fragment frg = fm.findFragmentByTag("reset_stats");
        if (frg == null) {
            ResetStatsOverlay overlay = new ResetStatsOverlay("RESET STATS?", "Max, min, average, and cumulative stats will be reset.", list, R.layout.title_body_carousel, this);
            overlay.show(fm.beginTransaction(), "reset_stats");
        }
    }
    
    public void dismissResetStatsOverlay() {
        Fragment overlay = getSupportFragmentManager().findFragmentByTag("reset_stats");
        if (overlay != null) {
            DialogFragment df = (DialogFragment) overlay;
            df.dismissAllowingStateLoss();
        }
    }

    public void showSetUnitsOverlay() {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
        int unitSetting = SettingsUtil.getUnits(getBaseContext());
        if (unitSetting == SettingsUtil.RECON_UNITS_METRIC ) {
            list.add(new ReconJetDialogFragment(R.layout.title_carousel_icon_item, "Metric", R.drawable.confirm_checkmark, 0));
            list.add(new ReconJetDialogFragment(R.layout.title_carousel_icon_item, "Imperial", 0, 1));
        }else{
            list.add(new ReconJetDialogFragment(R.layout.title_carousel_icon_item, "Metric", 0, 0));
            list.add(new ReconJetDialogFragment(R.layout.title_carousel_icon_item, "Imperial", R.drawable.confirm_checkmark, 1));
        }
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        Fragment frg = fm.findFragmentByTag("set_units");
        if (frg == null) {
            SetUnitsOverlay overlay = new SetUnitsOverlay("SET UNITS", list, R.layout.title_carousel, this);
            overlay.show(fm.beginTransaction(), "set_units");
        }
    }
    
    public void dismissSetUnitsOverlay() {
        Fragment overlay = getSupportFragmentManager().findFragmentByTag("set_units");
        if (overlay != null) {
            DialogFragment df = (DialogFragment) overlay;
            df.dismissAllowingStateLoss();
        }
    }
}
