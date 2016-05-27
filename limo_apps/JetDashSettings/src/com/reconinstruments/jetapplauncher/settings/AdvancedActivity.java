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
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AdvancedActivity extends JetSettingsListActivity {
    private static final String TAG = AdvancedActivity.class.getSimpleName();
    public static final String WIFI = "WiFi";
    public static final String USB_DEBUGGING = 	"USB Debugging";
    public static final String DIAGNOSTICS = 	"Diagnostics";
    public static final String DELETE_AGPS_DATA = "Delete AGPS Data";
    public static final String SENSOR_TYPE = "Sensor Type";
    public static final String FACTORY_RESET = "Factory Reset";

    // FIXME: Find a better method
    public enum Item_position {
	WIFI, USB_DEBUGGING, DIAGNOSTICS, DELETE_AGPS_DATA,SENSOR_TYPE,FACTORY_RESET
    }
  
    protected void setupSettingsItems(){
	// Warning: The order of the list should follow item_position enum 
        mSettingList.add(new SettingItem(new Intent("android.settings.WIFI_SETTINGS"), WIFI));
        mSettingList.add(new SettingItem(null, USB_DEBUGGING));
        mSettingList.add(new SettingItem(new Intent(this, DiagnosticsActivity.class), DIAGNOSTICS));
        mSettingList.add(new SettingItem(null, DELETE_AGPS_DATA));
        mSettingList.add(new SettingItem(new Intent("com.reconinstruments.jetappsettings.antbleswitch"), SENSOR_TYPE));
        mSettingList.add(new SettingItem(null, FACTORY_RESET));
    }
    
    @Override
    protected void settingsItemClicked(int position) {
    	SettingItem item = mSettingList.get(position);
    	
        if(item.title.equals(USB_DEBUGGING)){ //USB Debugging
            try {
                if(Settings.Secure.getInt(getBaseContext().getContentResolver(), Settings.Secure.ADB_ENABLED) == 1) {
                    // Turn off ADB
                    Settings.Secure.putInt(getBaseContext().getContentResolver(), Settings.Secure.ADB_ENABLED, 0);
                } else {
                    // Turn on ADB
                    Settings.Secure.putInt(getBaseContext().getContentResolver(), Settings.Secure.ADB_ENABLED, 1);
                }
            } catch (SettingNotFoundException e) {
                e.printStackTrace();
            }
            updateADBState();
        }
		else if(item.title.equals(FACTORY_RESET)){	// factory resest
		    showFactoryResetOverlay();
		}
        else if(item.title.equals(DELETE_AGPS_DATA)){    // Delete AGPS Data
            showDeleteAGPSOverlay();
        }
    }
    
    @Override 
    public void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.fade_slide_in_left,0);
        updateADBState();
        checkSensorType();
    }
    
    private void checkSensorType(){
        SettingItem item = mSettingList.get(Item_position.SENSOR_TYPE.ordinal());
        if (SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT == SettingsUtil.getBleOrAnt()) {
            item.subTitle = "ANT+";
        }else{
            item.subTitle = "Bluetooth LE";
        }
        mListAdapter.notifyDataSetChanged();
    }
    
    private void updateADBState(){
        SettingItem item = mSettingList.get(Item_position.USB_DEBUGGING.ordinal());
        try {
            if(Settings.Secure.getInt(getBaseContext().getContentResolver(), Settings.Secure.ADB_ENABLED) == 1) {
                item.subIconId = R.drawable.checkbox_enabled_selectable;
            }else{
                item.subIconId = R.drawable.checkbox_selectable;
            }
            mListAdapter.notifyDataSetChanged();
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void showFactoryResetOverlay() {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Cancel", 0, 0));
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Reset", 0, 1));
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        Fragment frg = fm.findFragmentByTag("factory_reset");
        if (frg == null) {
            FactoryResetOverlay overlay = new FactoryResetOverlay("FACTORY RESET?", "Firmware will be reset to the factory default.", list, R.layout.title_body_carousel, this);
            overlay.show(fm.beginTransaction(), "factory_reset");
        }
    }
    
    public void dismissFactoryResetOverlay() {
        Fragment overlay = getSupportFragmentManager().findFragmentByTag("factory_reset");
        if (overlay != null) {
            DialogFragment df = (DialogFragment) overlay;
            df.dismissAllowingStateLoss();
        }
    }
    
    public void showDeleteAGPSOverlay() {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Cancel", 0, 0));
        list.add(new ReconJetDialogFragment(R.layout.title_body_carousel_item_normal, "Delete", 0, 1));
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        Fragment frg = fm.findFragmentByTag("delete_agps");
        if (frg == null) {
            DeleteAgpsOverlay overlay = new DeleteAgpsOverlay("DELETE AGPS DATA", "This will delete assisted GPS data. Connect a smartphone or to Uplink to get new AGPS.", list, R.layout.title_body_carousel, this);
            overlay.show(fm.beginTransaction(), "delete_agps");
        }
    }
    
    public void dismissDeleteAGPSOverlay() {
        Fragment overlay = getSupportFragmentManager().findFragmentByTag("delete_agps");
        if (overlay != null) {
            DialogFragment df = (DialogFragment) overlay;
            df.dismissAllowingStateLoss();
        }
    }
}
