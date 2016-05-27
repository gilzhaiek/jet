//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.power;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.limopm.LimoPMNative;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.DialogInterface;

import com.reconinstruments.commonwidgets.BreadcrumbView;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemPageAdapter;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.SettingsUtil;


public class ReconPowerMenuActivity extends CarouselItemHostActivity {

    TextView title;
    ListView menuList;

    public static boolean screenOn = true;
    private static final int POWER_MODE_NORMAL = 1;
    private static final int POWER_MODE_DISPLAY_OFF = 2;
    private static final int POWER_MODE_SUSPEND = 3;

    private static final int MENU_ID_DISPLAY_OFF = 0;
    private static final int MENU_ID_STAND_BY = 1;
    private static final int MENU_ID_LOCK_DEVICE = 1;
    private static final int MENU_ID_POWER_OFF = 2;
    private static final String[] MENU_ITEM_TITLES = { "Display Sleep",
            "Standby", "Power Off" };
    private static final String TAG = "ReconPowerMenuActivity";

    protected int mMenuItemViewResource = R.layout.menu_item;
    ArrayList<Object> mMenuItems;

    private Messenger mService = null;

    // These must be in-sync with those in GlanceAppService
    private static final int GLANCE_MSG_REGISTER_CLIENT = 1;
    private static final int GLANCE_MSG_UNREGISTER_CLIENT = 2;
    private static final int GLANCE_MSG_UNREGISTER_GLANCE_DETECT = 4;
    private static final int GLANCE_MSG_UNREGISTERED_SUCCESS = 13;
    private static final int GLANCE_MSG_UNREGISTERED_FAILED = 14;
    private int mPowerMenuActionType = -1;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.carousel_with_text_power_menu);
        if (DeviceUtils.isSnow2()){
            TextView titleView = (TextView) findViewById(R.id.title_text_view);
            titleView.setText("POWER OPTIONS");
        }
        initPager();
        mPager.setCurrentItem(0);
        mPager.setPadding(75,0,75,0);
        mContext = this;

        if (DeviceUtils.isSun() && SettingsUtil.getCachableSystemIntOrSet(this, "GlanceEnabled", 0) == 1) {
            bindService(new Intent("com.reconinstruments.jetapplauncher.settings.service.GlanceAppService"),
                        mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mService != null) {
            try {
                // Unregister to client before unbinding
                Message msg = Message.obtain(null, GLANCE_MSG_UNREGISTER_CLIENT);
                Bundle b = new Bundle();
                b.putString("key", TAG);
                msg.setData(b);
                mService.send(msg);
            } catch (RemoteException e) {
                // There is nothing to do if the service has crashed.
            }
            unbindService(mConnection);
        }
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GLANCE_MSG_UNREGISTERED_FAILED:
                    // Regardless if it fails, we still want to turn off the screen. So just log it and
                    // fall thru
                    Log.e(TAG, "Failed to unregister glance!");
                case GLANCE_MSG_UNREGISTERED_SUCCESS:
                    Log.d(TAG, "Glance unregistered finished: " + mPowerMenuActionType);
                    SettingsUtil.setSystemInt(mContext, "GlanceEnabled", 0);

                    switch (mPowerMenuActionType) {
                        case MENU_ID_DISPLAY_OFF:
                            initDisplayOff();
                            break;
                        case MENU_ID_LOCK_DEVICE:
                            initLockDevice();
                            break;
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            mService = new Messenger(obj);

            try {
                // Once connection is created, register to the server
                Message msg = Message.obtain(null, GLANCE_MSG_REGISTER_CLIENT);
                Bundle b = new Bundle();
                b.putString("key", TAG);
                msg.setData(b);
                msg.replyTo = mMessenger;
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

    private boolean shouldShowLockOption() {
        return (SettingsUtil.getCachableSystemIntOrSet(this,
                                          SettingsUtil.PASSCODE_ENABLED,
                                          0) == 1);
    }

    @Override
    protected List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();
        int i = 0;
        fList.add(new ReconPowerMenuFragment(R.layout.carousel_remote_view,
                "Display Off", 0, i, "displaySleep",
                MENU_ID_DISPLAY_OFF));
        i++;
        if (shouldShowLockOption()) {
            fList.add(new ReconPowerMenuFragment(R.layout.carousel_remote_view,
                                                 "Lock Device", 0, i, "lockDevice",
                                                 MENU_ID_LOCK_DEVICE));
            i++;
        }
        fList.add(new ReconPowerMenuFragment(R.layout.carousel_remote_view,
                "Shutdown", 0, i, "powerOff",
                MENU_ID_POWER_OFF));
        return fList;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                String profileName =
                        ((ReconPowerMenuFragment)((CarouselItemPageAdapter)mPager.getAdapter())
                                .getItem(mPager.getCurrentItem()))
                                .getAssociatedProfileName();
                int sat =
                        ((ReconPowerMenuFragment)((CarouselItemPageAdapter)mPager.getAdapter())
                                .getItem(mPager.getCurrentItem()))
                                .getPowerMenuActionType();
                onPowerMenuOptionSelect(sat);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private void onPowerMenuOptionSelect(int powerMenuActionType){
        mPowerMenuActionType = powerMenuActionType;
        switch( powerMenuActionType ){
            case MENU_ID_DISPLAY_OFF:
                initDisplayOff();
                break;
            case MENU_ID_LOCK_DEVICE:
                initLockDevice();
                break;
            case MENU_ID_POWER_OFF:
                initPowerOff();
                break;
        }
    }

    private void initStandBy() {
        Toast.makeText(this, MENU_ITEM_TITLES[MENU_ID_STAND_BY],
                Toast.LENGTH_LONG);
        // Log.v(TAG,"initStandby: suspend mode");
        // System.setProperty("suspend.mode", "1");
        // Log.v(TAG,"suspend mode is"+System.getProperty("suspend.mode", "1"));
        screenOn = false;

        if (DeviceUtils.isLimo()) {
            LimoPMNative.SetPowerMode(POWER_MODE_SUSPEND);
        } else {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.goToSleep(System.currentTimeMillis()); // add 10 seconds to let
            // other apps go
            // sleeping
        }

        Log.d(TAG, "initStandBy");

    }

    // JIRA: MODLIVE-782 Add Connect to smart phone to power menu
    private void initSmartphoneConnection() {
        Log.d(TAG, "initSmartphoneConnection");
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final List<RunningTaskInfo> recentTasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        boolean moveToFront = false;
        for (int i = 0; i < recentTasks.size(); i++) {
            String recent = recentTasks.get(i).baseActivity.toShortString();
            // bring to front
            if (recent.contains("com.reconinstruments.jetconnectdevice")) {
                Log.d(TAG, "moveToFront = true");
                activityManager.moveTaskToFront(recentTasks.get(i).id, ActivityManager.MOVE_TASK_WITH_HOME);
                moveToFront = true;
                break;
            }
        }
        if(!moveToFront){
            Intent intent = new Intent("com.reconinstruments.connectdevice.CONNECT");
            startActivity(intent);
        }
        finish();
    }

    // End of JIRA: MODLIVE-782

    private void initDisplayOff() {
        Toast.makeText(this, MENU_ITEM_TITLES[MENU_ID_DISPLAY_OFF],
                Toast.LENGTH_LONG);
        // Log.v(TAG,"initDisplayOff: display_off mode");
        // System.setProperty("display_off.mode", "1");
        // Log.v(TAG,"display_off mode is"+System.getProperty("display_off.mode",
        // "1"));

        // If glance is enabled, then turn it off before we can turn the display off
        if (DeviceUtils.isSun() && SettingsUtil.getCachableSystemIntOrSet(this, "GlanceEnabled", 0) == 1) {
            turnOffGlance();
        } else {
            screenOn = false;
            if (DeviceUtils.isLimo()) {
                LimoPMNative.SetPowerMode(POWER_MODE_DISPLAY_OFF);
            } else {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                pm.goToSleep(System.currentTimeMillis()); // add 10 seconds to let
                // other apps go
                // sleeping
                finish();
            }
        }
        Log.d(TAG, "initDisplayOff");
    }

    private void initPowerOff() {
        Log.d(TAG, "initPowerOff");
        finish();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        pm.reboot("BegForShutdown"); // Magic reason that causes shutdown as
        // opposed to reboot

    }
    private void initLockDevice(){
        if (DeviceUtils.isSun() && SettingsUtil.getCachableSystemIntOrSet(this, "GlanceEnabled", 0) == 1) {
            turnOffGlance();
        } else {
            screenOn = false;
            Intent i = new Intent("com.reconinstruments.passcodelock.PASSCODE_MAIN");
            i.putExtra("CALLING_ACTION", "LOCK");
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.goToSleep(System.currentTimeMillis()+3000); // 3-sec delay before screen off
            finish();
        }
    }

    private void turnOffGlance() {
        Log.d(TAG, "Glance is enabled, so turning it off first");
        try {
            Message msg = Message.obtain(null, GLANCE_MSG_UNREGISTER_GLANCE_DETECT);
            Bundle b = new Bundle();
            b.putString("key", TAG);
            msg.setData(b);
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop detection");
        }

        // Make note that we need to re-enable glance whenever we turn on the screen
        SettingsUtil.setSystemInt(this, "GlanceReEnable", 1);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fadeout_faster, 0);
    }
}
