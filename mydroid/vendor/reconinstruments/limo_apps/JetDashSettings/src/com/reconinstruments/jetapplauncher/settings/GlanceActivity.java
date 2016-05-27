package com.reconinstruments.jetapplauncher.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.ReconJetDialogFragment;
import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.jetapplauncher.settings.service.GlanceAppService;
import com.reconinstruments.os.hardware.glance.HUDGlanceManager;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.commonwidgets.CommonUtils;

import java.util.ArrayList;
import java.util.List;


public class GlanceActivity extends JetSettingsListActivity
{
    private static final String TAG = "GlanceActivity";

    public static final String REQUEST = "com.reconinstruments.glancedetectionapp.REQ";
    public static final String RESULT = "com.reconinstruments.glancedetectionapp.RES";
    public static final String EVENT = "com.reconinstruments.glancedetectionapp.EVT";

    private Context mContext;
    private Messenger mService = null;
    private static boolean mGlanceOnResume = false;
    private SettingItem mCalSettingItem = null;
    private SettingItem mScreenDelayItem = null;
    private TextView mTextView;
    private static boolean mOnCreate = true;

    protected void setupSettingsItems(){
        boolean enabled = (SettingsUtil.getCachableSystemIntOrSet(this, "GlanceEnabled", 0) == 1);
        mSettingList.add(new SettingItem(this, "Enable Glance",
                     "GlanceEnabled", enabled));
    }

    @Override
    protected void settingsItemClicked(int position) {
        SettingItem item = mListAdapter.getItem(position);
        switch (position) {
            case 0:
                int enable = SettingsUtil.getCachableSystemIntOrSet(this, "GlanceEnabled", 0);
                Log.d(TAG, "Enabled: " + enable);
                if (SettingsUtil.getCachableSystemIntOrSet(mContext, "GlanceEnabled", 0) == 0) {
                    // Force the user to calibrate everytime before starting glance detection.
                    mGlanceOnResume = true;
                    startCalibration();
                } else {
                    stopGlanceDetection();
                }
                break;
            case 1:
                showScreenDelayOverlay();
                break;
            case 2:
                startCalibration();
                break;
        }
    }

    @Override
    protected void settingsItemSelected(int position) {
        // Ignore the first item selected callback
        if (mOnCreate) {
            mOnCreate = false;
        } else {
            // Update the text
            if (position == 0) {
                mTextView.setText("Experimental feature");
            } else if (position == 1) {
                mTextView.setText("Set screen off delay time");
            } else if (position == 2) {
                mTextView.setText("Re-calibrate Glance Detection");
            }
        }
    }

    public void toggleGlance(boolean enabled) {
        int pos = -1;
        for (int i = 0; i < mSettingList.size(); i++){
            if ("Enable Glance".equals(mSettingList.get(i).title)) {
                pos = i;
            }
        }

        if (pos == -1) return;
        SettingItem item = mSettingList.get(pos);
	boolean wasChecked = item.isChecked();
        item.setItemChecked(this, enabled);

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)mListView.getLayoutParams();

        // If glance is enabled, then add the two items
        if (enabled && !wasChecked) {
            // Get the currently set delay
            int delay = SettingsUtil.getCachableSystemIntOrSet(this, "ScreenDelay", 2);
            String subTitle;
            if (delay == 0) {
                subTitle = "None";
            } else {
                subTitle = delay + " sec";
            }
	    mScreenDelayItem = new SettingItem(null, "Delay Time", subTitle);
	    mSettingList.add(mScreenDelayItem);
	    mCalSettingItem = new SettingItem("Calibrate");
	    mSettingList.add(mCalSettingItem);

            lp.height = 180; // modify list layout height to accomodate text below
            mTextView.setText("Experimental feature");
        } else if (!enabled) {
            mSettingList.remove(mScreenDelayItem);
            mSettingList.remove(mCalSettingItem);

            lp.height = 160; // modify list layout height to accomodate text below
            mTextView.setSingleLine(false);
            mTextView.setText(getResources().getString(R.string.GlanceDetectionInstructions));
        }
        mListView.setLayoutParams(lp);

        mListAdapter.notifyDataSetChanged();
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int enabled = 0;
            switch (msg.what) {
                case GlanceAppService.MSG_REGISTERED_SUCCESS:
                    enabled = 1;
                case GlanceAppService.MSG_UNREGISTERED_SUCCESS:
                    Log.d(TAG, "Registered/unregistered success " + enabled);
                    toggleGlance(enabled == 1);
                    SettingsUtil.setSystemInt(mContext, "GlanceEnabled", enabled);
                    break;
                case GlanceAppService.MSG_REGISTERED_FAILED:
                case GlanceAppService.MSG_UNREGISTERED_FAILED:
                    Log.e(TAG, "Failed to start/stop glance detection");
                    break;
                default:
                    Log.d(TAG, "Unknown message");
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mIncomingMessenger = new Messenger(new IncomingHandler());

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            Log.d(TAG, "Connected to service");
            mService = new Messenger(obj);

            try {
                Log.d(TAG, "Registering client to service!");
                Message msg = Message.obtain(null, GlanceAppService.MSG_REGISTER_CLIENT);
                Bundle b = new Bundle();
                b.putString("key", TAG);
                msg.setData(b);
                msg.replyTo = mIncomingMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                Log.d(TAG, "GlanceAppService has crashed");
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            Log.d(TAG, "Disconnected from service");
            mService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout lin = (LinearLayout)findViewById(R.id.base_lin_layout);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)mListView.getLayoutParams();

        mTextView = new TextView(getApplicationContext());
        if (SettingsUtil.getCachableSystemIntOrSet(this, "GlanceEnabled", 0) == 1) {
            Log.d(TAG, "Glance enabled");
            // Get the currently set delay
            int delay = SettingsUtil.getCachableSystemIntOrSet(this, "ScreenDelay", 2);
            String subTitle;
            if (delay == 0) {
                subTitle = "None";
            } else {
                subTitle = delay + " sec";
            }
            mScreenDelayItem = new SettingItem(null, "Delay Time", subTitle);
            mSettingList.add(mScreenDelayItem);

            mCalSettingItem = new SettingItem("Calibrate");
            mSettingList.add(mCalSettingItem);

            lp.height = 180; // modify list layout height to accomodate text below
            mTextView.setText("Experimental feature");
        } else {
            Log.d(TAG, "Glance not enabled");
            lp.height = 160; // modify list layout height to accomodate text below
            mTextView.setSingleLine(false);
            mTextView.setText(getResources().getString(R.string.GlanceDetectionInstructions));
        }
        mListView.setLayoutParams(lp);

        mTextView.setPadding(0, 0, 0, 12);
        mTextView.setGravity(Gravity.CENTER | Gravity.BOTTOM);
        mTextView.setTextSize(22);
        mTextView.setTextColor(Color.WHITE);
        lin.addView(mTextView);

        mContext = this;
        mOnCreate = true;

        /* Bind to the glance service for this app */
        bindService(new Intent(GlanceActivity.this, GlanceAppService.class), mConnection, Context.BIND_AUTO_CREATE);
    }


    public void updateScreenDelay(int delay) {
        int pos = -1;
        for (int i = 0; i < mSettingList.size(); i++){
            if ("Delay Time".equals(mSettingList.get(i).title)) {
                pos = i;
            }
        }

        if (pos == -1) return;
        Message msg = Message.obtain(null, GlanceAppService.MSG_SET_DELAY, delay, 0);
        try {
            Log.d(TAG,"Sending message to service to set delay: " + delay);
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to start detection");
        }

        SettingsUtil.setSystemInt(mContext, "ScreenDelay", delay);
        SettingItem item = mSettingList.get(pos);
        if (delay == 0) {
            item.subTitle = "None";
        } else {
            item.subTitle = delay + " sec";
        }
        mListAdapter.notifyDataSetChanged();
    }

    private void showScreenDelayOverlay() {
        List<android.support.v4.app.Fragment> list = new ArrayList<android.support.v4.app.Fragment>();
        List<Integer> options = new ArrayList<Integer>();
        options.add(0);
        options.add(1);
        options.add(2);
        options.add(3);
        List<String> labels = new ArrayList<String>();
        labels.add("None");
        labels.add("1 sec");
        labels.add("2 sec");
        labels.add("3 sec");

        // Get currently set delay
        int delay = SettingsUtil.getCachableSystemIntOrSet(this, "ScreenDelay", 2);
        int i = options.indexOf(delay);
        if (i < 0) i = 0;
        for (int j = 0; j < options.size(); j++) {
            if (i == j) {
                list.add(new ReconJetDialogFragment(R.layout.title_carousel_icon_item,
                    labels.get(i),
                    R.drawable.confirm_checkmark, i));
            } else {
                list.add(new ReconJetDialogFragment(R.layout.title_carousel_icon_item, labels.get(j), 0, j));
            }
        }

        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        Fragment frg = fm.findFragmentByTag("set_glance_delay");
        if (frg == null) {
            ScreenDelayOverlay overlay = new ScreenDelayOverlay("DELAY TIME", list, R.layout.title_carousel, this);
            overlay.show(fm.beginTransaction(), "set_glance_delay");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((SettingsUtil.getCachableSystemIntOrSet(this, "GlanceCalibrated", 0) == 1) &&
            ((SettingsUtil.getCachableSystemIntOrSet(this, "GlanceEnabled", 0) == 1) || mGlanceOnResume) &&
            mService != null) {
            mGlanceOnResume = false;
            startGlanceDetection();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            try {
                Message msg = Message.obtain(null, GlanceAppService.MSG_UNREGISTER_CLIENT);
                Bundle b = new Bundle();
                b.putString("key", TAG);
                msg.setData(b);
                mService.send(msg);
            } catch (RemoteException e) {
            }
            unbindService(mConnection);
        }
    }

    public void startGlanceDetection() {
        Message msg = Message.obtain(null, GlanceAppService.MSG_REGISTER_GLANCE_DETECT);
        Bundle b = new Bundle();
        b.putString("key", TAG);
        msg.setData(b);
        try {
            Log.d(TAG,"Sending message to service to start");
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to start detection");
        }
    }

    public void stopGlanceDetection() {
        Message msg = Message.obtain(null, GlanceAppService.MSG_UNREGISTER_GLANCE_DETECT);
        Bundle b = new Bundle();
        b.putString("key", TAG);
        msg.setData(b);
        try {
            Log.d(TAG,"Sending message to service to stop");
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop detection");
        }

    }

    public void startCalibration() {
        // Unset calibrated system flag
        SettingsUtil.setSystemInt(mContext, "GlanceCalibrated", 0);

        Intent calibrateIntent = new Intent(this, CalibrateActivity.class);
        CommonUtils.launchNew(this, calibrateIntent, false);
    }
}
