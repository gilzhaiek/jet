package com.reconinstruments.jetapplauncher.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.jetapplauncher.settings.advanced.UninstallActivity;
import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class BatterySavingActivity extends JetSettingsListActivity {
    private static final String TAG = BatterySavingActivity.class.getSimpleName();
    private String mBatteryLevel = "";
    private String mBatteryStatus = "";
    private SettingItem mBatteryItem = null; // Need it for future reference
    private SettingItem mVideoLengthItem = null;
    private TextView mTextView;
    private static SparseArray<String> mItems = new SparseArray<String>();
    
    public static final String GPS_ALWAYS_ON = "GPS Always on";
    public static final String RECORD_LENGTH = "Record Length";
    public static final String BATTERY = "Battery";

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel = getBatteryPercentage(intent);
                mBatteryStatus= getBatteryStatus(intent);
                updateBatteryIndication();
            }
        }
    };
    
    public void onCreate(Bundle savedInstanceState) {
        Intent i = registerReceiver(mBatteryInfoReceiver,
             new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i != null) {
            Log.v(TAG,"some level");
            mBatteryLevel = getBatteryPercentage(i);
            mBatteryStatus= getBatteryStatus(i);
            Log.v(TAG,"mBatteryStatus "+mBatteryStatus);
            Log.v(TAG,"mBatteryLevel "+mBatteryLevel);
        } else {
            Log.v(TAG,"no level");
        }
        super.onCreate(savedInstanceState);
    }

    protected void setupSettingsItems(){
        //initialize the items
        if(mItems.size() == 0){
            int j = 0;
            mItems.put(j++, GPS_ALWAYS_ON);
            if (DeviceUtils.isSun()) {
                mItems.put(j++, RECORD_LENGTH);
            }
            mItems.put(j, BATTERY);
        }
        
        //append the textview on the bottom
        LinearLayout lin = (LinearLayout)findViewById(R.id.base_lin_layout);
        mTextView = new TextView(getApplicationContext());
        mTextView.setPadding(0, 0, 0, 12);
        mTextView.setGravity(Gravity.CENTER | Gravity.TOP);
        mTextView.setTextSize(22);
        mTextView.setTextColor(Color.WHITE);
        lin.addView(mTextView);
        
        for(int i = 0; i < mItems.size(); i++){
            if(mItems.get(i).equals(GPS_ALWAYS_ON)){
                mSettingList.add(new SettingItem(this, mItems.get(i), "RECON_GPS_ALWAYS_ON", false));
            }else if(mItems.get(i).equals(RECORD_LENGTH)){
                // Video Capture
                int videocap = SettingsUtil
                        .getCachableSystemIntOrSet(this, SettingsUtil.VIDEO_RECORD_DURATION,
                                SettingsUtil.DEFAULT_VIDEO_RECORD_DURATION);
                String subTitle = "1 min";
                if (videocap != 60) {
                    subTitle = videocap + " sec";
                }
                mVideoLengthItem = new SettingItem(null, mItems.get(i), subTitle);
                mSettingList.add(mVideoLengthItem);
            }else if(mItems.get(i).equals(BATTERY)){
                mBatteryItem = new SettingItem(null, mItems.get(i), getFullBatteryString());
                mSettingList.add(mBatteryItem);
            }
        }
    }

    @Override
    protected void settingsItemSelected(int position) {
        // Update the text
        int defalutListHeight = 180;
        TextView fakeTextView = new TextView(getApplicationContext());
        if (mItems.get(position).equals(GPS_ALWAYS_ON)) {
            mTextView.setText("Search for GPS before activities");
            mTextView.setLineSpacing(fakeTextView.getLineSpacingExtra(), 1.0f);
        } else if (mItems.get(position).equals(RECORD_LENGTH)) {
            mTextView.setText("Video camera record length\nLonger videos use more battery");
            mTextView.setLineSpacing(fakeTextView.getLineSpacingExtra() - 5f, 1.0f);
            defalutListHeight = 160;
        } else if (mItems.get(position).equals(BATTERY)) {
            mTextView.setText("Current battery level");
            mTextView.setLineSpacing(fakeTextView.getLineSpacingExtra(), 1.0f);
        }
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)mListView.getLayoutParams();
        lp.height = defalutListHeight;
        mListView.setLayoutParams(lp);
    }

    @Override
    protected void settingsItemClicked(int position) {
        SettingItem item = mSettingList.get(position);
        if(RECORD_LENGTH.equals(item.title)){
            showVideoCapOverlay();
        }else if (GPS_ALWAYS_ON.equals(item.title)) { // GPS always on
            item.toggle(this);
            mListAdapter.notifyDataSetChanged();
        }
   }
    public void updateVideoCap(){
        if(mVideoLengthItem == null) return;
        int videocap = SettingsUtil
                .getCachableSystemIntOrSet(this, SettingsUtil.VIDEO_RECORD_DURATION,
                               SettingsUtil.DEFAULT_VIDEO_RECORD_DURATION);
        if(videocap == 60){
            mVideoLengthItem.subTitle = "1 min";
        }else{
            mVideoLengthItem.subTitle = videocap + " sec";
        }
        
        mListAdapter.notifyDataSetChanged();
    }

    public void showVideoCapOverlay() {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
    int videocap = SettingsUtil
        .getCachableSystemIntOrSet(this, SettingsUtil.VIDEO_RECORD_DURATION,
                       SettingsUtil.DEFAULT_VIDEO_RECORD_DURATION);

    List<Integer> options = new ArrayList<Integer>();
    options.add(15);
    options.add(30);
    options.add(60);
    List<String> labels = new ArrayList<String>();
    labels.add("15 seconds");
    labels.add("30 seconds");
    labels.add("1 minute");


    int i = options.indexOf(videocap);
    if (i < 0) i = 0;
    for(int j = 0; j<options.size(); j++){
        if(i == j){
            list.add(new ReconJetDialogFragment(R.layout.title_carousel_icon_item,
                    labels.get(i),
                    R.drawable.confirm_checkmark, i));
        }else{
            list.add(new ReconJetDialogFragment(R.layout.title_carousel_icon_item, labels.get(j), 0, j));
        }
    }
    
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        Fragment frg = fm.findFragmentByTag("set_video_cap");
        if (frg == null) {
            VideoCapOverlay overlay = new VideoCapOverlay("VIDEO LENGTH", list, R.layout.title_carousel, this);
            overlay.show(fm.beginTransaction(), "set_video_cap");
        }
    }
    
    public void dismissVideoCapOverlay() {
        Fragment overlay = getSupportFragmentManager().findFragmentByTag("set_units");
        if (overlay != null) {
            DialogFragment df = (DialogFragment) overlay;
            df.dismissAllowingStateLoss();
        }
    }

    // Copied shamelessly from com.android.settings.Utils
    public static String getBatteryPercentage(Intent batteryChangedIntent) {
        int level = batteryChangedIntent.getIntExtra("level", 0);
        int scale = batteryChangedIntent.getIntExtra("scale", 100);
        return String.valueOf(level * 100 / scale) + "%";
    }

    public static String getBatteryStatus(Intent batteryChangedIntent) {
        final Intent intent = batteryChangedIntent;

        int plugType = intent.getIntExtra("plugged", 0);
        int status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
        String statusString;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            statusString = "Charging";
            if (plugType > 0) {
                statusString = statusString
                        + " "
            + ((plugType == BatteryManager.BATTERY_PLUGGED_AC)
                                ? "AC"
               : "USB");
            }
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            statusString = "Discharging";
        } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            statusString = "Not Charging";
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
        statusString = "Full";
        } else {
            statusString = "Unknown";
        }

        return statusString;
    }
    @Override
    public void onDestroy() {
    unregisterReceiver(mBatteryInfoReceiver);
    super.onDestroy();
    }
    private String getFullBatteryString() {
    if (DeviceUtils.isSun()) {
        return (mBatteryStatus+"-"+mBatteryLevel);
    }
    else {
        return (mBatteryStatus);
    }
    }
    private void updateBatteryIndication() {
    if (mBatteryItem == null) return;
    mBatteryItem.subTitle = getFullBatteryString();
    mListAdapter.notifyDataSetChanged();
    }
}
