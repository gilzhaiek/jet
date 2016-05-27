
package com.reconinstruments.jetapplauncher.settings;

import android.app.AlarmManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.BTHelper;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;
import com.reconinstruments.hudservice.helper.HUDConnectivityHelper;
import com.reconinstruments.hud_phone_status_exchange.TimesyncRequestMessage;
import com.reconinstruments.hud_phone_status_exchange.TimesyncResponseMessage;
import com.reconinstruments.commonwidgets.ReconToast;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class TimeActivity extends JetSettingsListActivity {

    private boolean mIs24 = false;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void setupSettingsItems() {
        mSettingList.add(new SettingItem("Use 24 Hour Clock", mIs24));
        mSettingList.add(new SettingItem(new Intent(this, TimezoneActivity.class),
                "Select Timezone"));
    }

    @Override
    protected void settingsItemClicked(int position) {
        SettingItem item = mSettingList.get(position);
        if (position == 0) {
            mIs24 = !(mIs24);
            updateTimeState(mIs24);
            // broadcast the time changed intent so that status bar can update
            // the change
            Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
            getBaseContext().sendBroadcast(timeChanged);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mIs24 = DateFormat.is24HourFormat(this);
        updateTimeState(mIs24);
    }

    public void updateTimeState(boolean is24) {
        SettingItem item = mSettingList.get(0);
        item.setIsChecked(is24);
        Settings.System.putString(getBaseContext().getContentResolver(),
                Settings.System.TIME_12_24,
                is24 ? "24" : "12");
        mListAdapter.notifyDataSetChanged();
    }
}
