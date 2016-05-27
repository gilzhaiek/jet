package com.contour.connect;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.TextView;
import android.widget.Toast;

import com.contour.api.CameraComms;
import com.contour.api.CameraSettings;
import com.contour.api.CameraStatus;
import com.contour.api.ConnectionService;
import com.contour.connect.CameraViewFragment.CameraViewUpdateListener;
import com.contour.connect.QuickSettingsFragment.OnQuickSettingsSelectedListener;
import com.contour.connect.SettingsFragment.OnSettingsSelectedListener;
import com.contour.connect.SettingsItemSelectionFragment.OnSettingsItemSelectedListener;
import com.contour.connect.SettingsSliderSelectionFragment.OnSettingsSliderChangedListener;
import com.contour.connect.debug.CLog;
import com.contour.connect.views.ContourToast;
import com.contour.connect.views.RecordButton;
import com.contour.net.NetHelper;
import com.contour.net.NetHelper.NetHelperCallback;
import com.contour.utils.ContourUtils;
import com.contour.utils.SharedPrefHelper;
import com.contour.utils.api.actionbarcompat.ActionBarActivity;
import com.google.analytics.tracking.android.EasyTracker;

public class MainActivity extends ActionBarActivity implements OnSettingsSelectedListener, 
OnSettingsItemSelectedListener, CameraViewUpdateListener, OnSettingsSliderChangedListener, 
FragmentManager.OnBackStackChangedListener, NetHelperCallback, OnQuickSettingsSelectedListener,OnFocusChangeListener {

    public static final String TAG                = "MainActivity";
    private boolean mDebug                 = false;
    
    private CameraViewFragment                                       mCameraViewFrag;
    private NetHelper                                                mNetHelper;
    private BluetoothAdapter                                         mAdapter;
    private BluetoothDevice[]                                        mAllBondedDevices;
    private BluetoothDevice                                          mDevice;
    private Messenger                                                mServiceMessenger;
    private boolean                                                  mServiceBound;
    private boolean                                                  mConnected;
    private boolean                                                  mSettingsChanged;
    private CameraSettings                                           mUpdateSettings;
    private CameraStatus                                             mCameraStatus;
//    private boolean                                                  mShouldLoadCameraView;
    private ScreenReceiver                                           mScreenReceiver;
    private static final int                                         REQUEST_ENABLE_BT  = 1;
    private static final String                                         KEY_DEVICE  = "bluetoothdevice";
    private static final String                                         KEY_LAST_VIDEO_IMAGE = "last_video_image";
//    private static final String                                         KEY_SERVICE_BOUND = "key_service_bound";

    // TextView mStatusMessage;

    // ArrayList<String> mStatusMessageList = new ArrayList<String>(20);

    private String                                                   mCameraModel;
    private static Bitmap                                                   sLastBitmap;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(mDebug) CLog.out(TAG,"onSharedPreferenceChanged",key);
            if(key.equals(SharedPrefHelper.FW_KEY)) {                
                requestLatestFirmware();
            }            
        }
    };
    
    static Handler sHandler = new Handler(); 
    
    static class IncomingHandler extends Handler {
        WeakReference<MainActivity> mActivityRef;

        IncomingHandler(MainActivity activity) {
            mActivityRef = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity sca = mActivityRef.get();
            if(sca == null || sca.isFinishing()) return;
            
            switch (msg.what) {
            // Application registration complete.
            case ConnectionService.STATUS_STATE_CHANGE:
                sca.mConnected = false;
                int state = msg.arg1;
//                String statusMessageStr;
                if (state == ConnectionService.STATE_LISTEN) {
//                    statusMessageStr = sca.getString(R.string.notif_camera_ready);
                    sca.setSettingsActionDisabled(true);
//                    sca.showListeningToast();
                } else if (state == ConnectionService.STATE_CONNECTING) {
//                    statusMessageStr = sca.getString(R.string.notif_connecting);
                } else if (state == ConnectionService.STATE_CONNECTED) {
//                    statusMessageStr = sca.getString(R.string.notif_connected);
                    sca.cameraConnected();

                } else if (state == ConnectionService.STATE_NONE){
//                    statusMessageStr = "State None";
                    sca.setSettingsActionDisabled(true);
                } else if (state == ConnectionService.STATE_DISCONNECTED) {
//                    statusMessageStr = sca.getString(R.string.notif_connection_lost);
                    sca.cameraDisconnected();

                } else {
//                    statusMessageStr = sca.getString(R.string.notif_error);
                }
//                sca.appendStatusMessage(statusMessageStr);
                break;
            case ConnectionService.STATUS_VIDEO_FAME:
                Object obj = msg.obj;
//                CLog.out(TAG,"RECV STATUS_VIDEO_FRAME",obj);
                if(obj != null) {
                    sca.updateLiveVideoImage((Bitmap)obj);
                }
                    break;
            default:
                super.handleMessage(msg);
            }
        }
    };

    Messenger mMessenger = new Messenger(new IncomingHandler(this));
    

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_controller);

//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

//        if (!isTaskRoot()) {
//            final Intent intent = getIntent();
//            final String intentAction = intent.getAction(); 
//            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
//                Log.w(CLog.TAG, "MainActivity is not the root.  Finishing Main Activity instead of launching.");
//                finish();
//                return;       
//            }
//        }
        mDebug = getResources().getBoolean(R.bool.debug_enabled);
        if(mDebug) CLog.out(TAG, "onCreate", savedInstanceState);

        mNetHelper = NetHelper.createInstance(this);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (mDebug)
            FragmentManager.enableDebugLogging(true);
        
        
        // layout for non-tablets
  
        if (savedInstanceState != null) {
            mDevice = savedInstanceState.getParcelable(KEY_DEVICE);
            sLastBitmap = savedInstanceState.getParcelable(KEY_LAST_VIDEO_IMAGE);
        }
        // Check for Bluetooth availability on the Android platform.
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mDebug) CLog.out(TAG, "onCreate", mAdapter,mAdapter != null ? mAdapter.getState() : -99);

        if (mAdapter == null) {

            ContourToast.showText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG);
            finish();
            return;
        }
        mAllBondedDevices = (BluetoothDevice[]) mAdapter.getBondedDevices().toArray(new BluetoothDevice[0]);

        mServiceBound = false;

        registerReceiver(mAdapterReceiver, initAdapterReceiver());
        registerReceiver(mDeviceReceiver, initDeviceReceiver());
        registerReceiver(mCameraReceiver, initCameraCommsReceiver());
        
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenReceiver = new ScreenReceiver();
        registerReceiver(mScreenReceiver,filter);
        loadSplashScreenFrag(); 

    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mDebug)
            CLog.out(TAG, "onConfigurationChanged", newConfig);
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        if(mDebug) CLog.out(TAG,"onStart", mConnected,mServiceBound);
        SharedPrefHelper.registerCallback(this, this.sharedPrefListener);      
        
        EasyTracker.getInstance().activityStart(this); 
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if(mDebug) CLog.out(TAG,"onResume", mConnected,mServiceBound);
        
//        boolean showingSettings = showingSettingsFragment();
//        mShouldLoadCameraView =  (showingSettings == false);
  

    }
    
    @Override
    protected void onResumeFragments ()
    {
        super.onResumeFragments();
        PowerManager pm =(PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isScreenOn();
        // If Bluetooth is not on, request that it be enabled.
        boolean bluetoothDisabled = !mAdapter.isEnabled();
        if(mDebug) CLog.out(TAG, "onResumeFragments", bluetoothDisabled,mServiceBound,isScreenOn);    
        if(isScreenOn) {
            if (bluetoothDisabled) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else if (!mServiceBound) {
                startAndBindService();
            } else if (mConnected && mServiceBound) {          
    //            this.loadSplashScreenFrag();
                this.sendMessageWithDevice(ConnectionService.MSG_GET_SETTINGS);
            }
        }
    }

    
    @Override
    protected void onPause() {
        super.onPause();
        if(mDebug) CLog.out(TAG,"onPause",mConnected,mServiceBound);

        if(mConnected && mServiceBound) {
            sendMessageWithDevice(ConnectionService.MSG_STOP_VIDEO);
//            sendMessageWithDevice(ConnectionService.MSG_STOP_STATUS);
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if(mDebug) CLog.out(TAG,"onStop",mConnected,mServiceBound);
        SharedPrefHelper.unregisterCallback(this, this.sharedPrefListener);
        
        //Google Analytics
        EasyTracker.getInstance().activityStop(this);
    }

    /**
     * Ensures user has turned on Bluetooth on the Android device.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(mDebug) CLog.out(TAG,"onActivityResult",requestCode,resultCode,data);

        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                if(!mServiceBound) {
                    startAndBindService();
                } else if(mConnected) 
                    sendMessageWithDevice(ConnectionService.MSG_GET_SETTINGS);
            } else {
                if(mDebug) CLog.out(TAG,"onActivityResult CANCELLED",requestCode,resultCode,data);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if(mDebug) CLog.out(TAG,"onDestroy");
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
        if(mNetHelper != null)
            mNetHelper.cancelActiveTasks();
        unregisterReceiver(mAdapterReceiver);
        unregisterReceiver(mDeviceReceiver);
        unregisterReceiver(mCameraReceiver);
        unregisterReceiver(mScreenReceiver);
        doUnbindService();
        super.onDestroy();
    }
    
    @Override
    protected void onSaveInstanceState (Bundle outState) {
        if(mDebug) CLog.out(TAG,"onSaveInstanceState",mDevice,sLastBitmap);

        if(mDevice != null)
            outState.putParcelable(KEY_DEVICE, mDevice);
        if(sLastBitmap != null)
            outState.putParcelable(KEY_LAST_VIDEO_IMAGE, sLastBitmap);
        
//        outState.putBoolean(KEY_SERVICE_BOUND, mServiceBound);
        super.onSaveInstanceState(outState);
    }

//    @Override
//    public void onWindowFocusChanged (boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if(mDebug) CLog.out(TAG,"onWindowFocusChanged",hasFocus);
//    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);

        // Calling super after populating the menu is necessary here to ensure
        // that the
        // action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // CLog.out(TAG,"onPrepareOptionsMenu",menu.getItem(0).getTitle(),menu.getItem(1).getTitle(),menu.getItem(2).getTitle());

        if (!mConnected) {
            this.setSettingsActionDisabled(true);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            break;

        case R.id.menu_close:
            this.onActionCloseButtonClicked();
            break;
        case R.id.menu_settings:
            if(this.mConnected) {
                if(mUpdateSettings == null) {
                    setSettingsActionLoadingEnabled(true);
                    sendMessageWithDevice(ConnectionService.MSG_GET_SETTINGS);
                    break;
                }

                sendMessageWithDevice(ConnectionService.MSG_GET_SETTINGS);
                
                FragmentManager fm = getSupportFragmentManager();
                Fragment f = fm.findFragmentByTag(QuickSettingsFragment.TAG);
                if (f == null) {
                    f = QuickSettingsFragment.newInstance((int)mUpdateSettings.switchPosition,mUpdateSettings,mDebug);
                } 
                if(f.isResumed() == false) {
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.replace(R.id.fragment_container, f,QuickSettingsFragment.TAG);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    ft.addToBackStack(null);
                    ft.commit();
                    setSettingsActionDisabled(true);
                }
            } else {
                Log.w(CLog.TAG, "Attempt to invoke settings without a camera connection");
            }
            break;

        case R.id.menu_register:
            break;

        case R.id.menu_about:
            loadAboutFragment();
            break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onLowMemory () {
        Log.w(CLog.TAG, "MainActivity::onLowMemory called!");
    }
    
    public boolean isConnected() {
    	return mConnected;
    }
    
    public CameraSettings getLastCameraSettings() {
        if(mUpdateSettings == null)
            return null;
        try {
            return (CameraSettings) this.mUpdateSettings.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return mUpdateSettings;
        }
    }
    
//    private void loadAboutFragment() {
//        Fragment f = AboutFragment.newInstance();
//        ((AboutFragment)f).setOnTouchListener(mGestureListener);
//        FragmentManager fm = getSupportFragmentManager();
//        FragmentTransaction ft = fm.beginTransaction();
//        ft.setCustomAnimations(R.anim.anim_swipe_in_from_right, R.anim.anim_swipe_out_from_right,R.anim.anim_swipe_in_from_left, R.anim.anim_swipe_out_from_left);
//        ft.replace(R.id.fragment_container, f);
//        ft.addToBackStack(null);
//        ft.commit();
//    }     

    private void loadAboutFragment() {
        startActivity(new Intent(this,AboutFragmentActivity.class));
    }

    // Intent filter and broadcast receive to handle Bluetooth on event.
    private IntentFilter initAdapterReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        return filter;
    }

    private IntentFilter initDeviceReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        return filter;
    }

    private IntentFilter initCameraCommsReceiver() {
        IntentFilter filter = new IntentFilter(CameraComms.ACTION_SETTINGS);
        filter.addAction(CameraComms.ACTION_RECORDING_STARTED);  
        filter.addAction(CameraComms.ACTION_RECORDING_STOPPED);
        filter.addAction(CameraComms.ACTION_CAMERA_CONFIGURE_DISABLED);
        filter.addAction(CameraComms.ACTION_CAMERA_CONFIGURE_ENABLED);
        filter.addAction(CameraComms.ACTION_STATUS);
        return filter;
    }
    
    
    private void startAndBindService() {
        if(mDebug) CLog.out(TAG,"startAndBindService");
        Intent intent = new Intent(this, ConnectionService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    
    private void doUnbindService() {
        if(mDebug) CLog.out(TAG, "doUnbindService", mServiceBound);
        if (mServiceBound) {
            unbindService(mConnection);
            mServiceMessenger = null;
            mServiceBound = false;
        }
    }
    
    private void loadSplashScreenFrag() {
    	FragmentManager fm = getSupportFragmentManager();

        Fragment f = fm.findFragmentByTag(SplashScreenFragment.TAG);
        if(f == null) {
            f = SplashScreenFragment.newInstance();
        }   
        fm.beginTransaction().replace(R.id.fragment_container,f, SplashScreenFragment.TAG).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
    }
    
    public void cameraConnected() {
        mConnected = true;
    }
    
    public void cameraDisconnected() {
        
        if(mDebug) CLog.out(TAG,"cameraDisconnected");
//        mShouldLoadCameraView = true; 
        mUpdateSettings = null;
        mCameraStatus = null;
        if (!isFinishing()) {
            setLoadingTitleBarEnabled(false);
            loadDisconnectedFrag();
            setSettingsActionLoadingEnabled(false);
            setSettingsActionDisabled(true);
        }
    }

    public void loadDisconnectedFrag() {
        if(this.isFinishing()) return; 

        FragmentManager fm = getSupportFragmentManager();

        Fragment f = fm.findFragmentByTag(DisconnectedFragment.TAG);
        if(mDebug) CLog.out(TAG,"loadDisconnectedFrag",f,f==null?"null":f.isAdded(),mCameraViewFrag,mCameraViewFrag==null?"null":mCameraViewFrag.isAdded());

        if (f == null) {
            f = DisconnectedFragment.newInstance();
        } else {
            if (f.isAdded())
                return;
        }
        FragmentTransaction ft = fm.beginTransaction();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (mCameraViewFrag != null) {
//            CLog.out(TAG,"Removing Fragment",mCameraViewFrag.getTag());
            ft.remove(mCameraViewFrag);
            ft.commit();
            fm.executePendingTransactions();
            mCameraViewFrag = null;
        }
        fm.beginTransaction().replace(R.id.fragment_container, f, DisconnectedFragment.TAG).commit();
        fm.executePendingTransactions();

        int N = fm.getBackStackEntryCount();
//        CLog.out(TAG,"Back stack entry count",N);

        this.setSettingsActionDisabled(true);
        this.setSettingsActionClosed(false);
        this.setSettingsActionLoadingEnabled(false);
        this.setLoadingTitleBarEnabled(false);
    }

    public void loadFirmwareUpgradeFrag() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentByTag(FirmwareUpgradeFragment.TAG);
        if(f == null) {
            f = FirmwareUpgradeFragment.newInstance();
        } else {
            if(f.isAdded()) return;
        }
        fm.beginTransaction().replace(R.id.fragment_container,f, FirmwareUpgradeFragment.TAG).commit();
        setSettingsActionDisabled(true);
     }
    
    public void loadCameraViewFrag() {
        boolean showingCameraViewFrag = this.showingCameraViewFragment();
        boolean showingSettingsFrag = this.showingSettingsFragment();
        if (mDebug)
            CLog.out(TAG, "loadCameraViewFrag", showingCameraViewFrag, showingSettingsFrag, mCameraViewFrag);
        if (!showingCameraViewFrag && !showingSettingsFrag) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment f = null;
            if(mCameraViewFrag != null) {
                f = mCameraViewFrag;
            }
            if(f == null) {
                f = fm.findFragmentByTag(CameraViewFragment.TAG);
            }
            if (f == null) {
                f = CameraViewFragment.newInstance(mDebug);
            }
            mCameraViewFrag = (CameraViewFragment) f;
            fm.beginTransaction().replace(R.id.fragment_container, f, CameraViewFragment.TAG).commit();
        } else if (showingCameraViewFrag) {
            mCameraViewFrag = (CameraViewFragment) getSupportFragmentManager().findFragmentByTag(CameraViewFragment.TAG);
            onStartVideoStream();
        }
    }
    
    public void requestLatestFirmware() {
         
        if(mNetHelper.isNetworkAvailable()) {
            if(mDebug) CLog.out(TAG,"Requesting latest firmware version numbers from the web");
            mNetHelper.getVersioningData();
        } else {
            if(mDebug) CLog.out(TAG,"Checking if camera has latest firmware using hard coded values (network is unavailable)");
            this.doFirmwareCheck(mUpdateSettings.getModelMajorVersionMin(),mUpdateSettings.getModelMinorVersionMin(),mUpdateSettings.getModelBuildVersionMin());
        }
    }
    
    private void loadAppInfoFragment() {
        Fragment dbf = AppInfoFragment.newInstance(mDevice);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction frt = fm.beginTransaction();
        frt.replace(R.id.fragment_container, dbf,AppInfoFragment.TAG);
        frt.addToBackStack(null);
        frt.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        frt.commit();
    }

    public void setDevice(BluetoothDevice device) {
        mDevice = device;
    }

//    private void connectChannel() {
//        if(mAdapter.isDiscovering())
//            mAdapter.cancelDiscovery();
//        CLog.out(TAG, "Cancel Discovery");
//        if(mDevice == null) {
//            Object[] devices =mAdapter.getBondedDevices().toArray();
//            if(devices == null || devices.length < 1) {
//                ContourToast.showText(this, "No devices paired, pair your camera!", Toast.LENGTH_LONG);
//            } else if (devices.length > 1) {
//                ContourToast.showText(this, "Too many devices paired, unpair all devices except camera!", Toast.LENGTH_LONG);
//            } else { 
//                mDevice = (BluetoothDevice) devices[0];
//            }
//        }
//       if(mDevice != null) {
//           sendMessageWithDevice(ConnectionService.MSG_CONNECT_CHANNEL);
//       }
//    }

    private void sendMessage(int what, int switchPos, int settingsGroupIndex, int settingsItemIndex) {
        if (mServiceMessenger == null) {
            Log.w(CLog.TAG,"Contour Service not connected.");
            return;
        }

        int settingsArgValue = (settingsGroupIndex << 16) | (settingsItemIndex & 0xFF);
        try {
            mServiceMessenger.send(Message.obtain(null, what, switchPos, settingsArgValue));
        } catch (RemoteException e) {
            Log.wtf(CLog.TAG,"Unable to reach service.");
            e.printStackTrace();
        }
    }
    
    private void sendMessageWithDevice(int what) {
    	 if (mServiceMessenger == null) {
    	     Log.w(CLog.TAG,"Contour Service not connected.");
             return;
         }

         try {
             mServiceMessenger.send(Message.obtain(null, what,mDevice));
         } catch (RemoteException e) {
             Log.wtf(CLog.TAG,"Unable to reach service.");
             e.printStackTrace();
         }    }
    
   
    
    private ServiceConnection mConnection = new ServiceConnection() {
                                              public void onServiceConnected(ComponentName name,
                                                      IBinder service) {
                                                  if(mDebug) CLog.out(TAG,"onServiceConnected", name, service);
                                                  mServiceBound = true;
                                                  Message msg = Message.obtain(null, ConnectionService.MSG_START_LISTENING);
                                                  msg.replyTo = mMessenger;
                                                  mServiceMessenger = new Messenger(service);
                                                  try {
                                                      mServiceMessenger.send(msg);
                                                  } catch (RemoteException e) {
                                                      CLog.out("Unable to register client to service.");
                                                      e.printStackTrace();
                                                  }
                                              }

                                              public void onServiceDisconnected(ComponentName name) {
                                                  if(mDebug) CLog.out(TAG,"onServiceDisconnected", name);
                                                  mServiceMessenger = null;
                                                  mServiceBound = false;
                                              }
                                          };

//    public static class SelectDeviceDialogFragment extends DialogFragment {
//        MainActivity mmActivity;
//
//        public static MainActivity.SelectDeviceDialogFragment newInstance(String[] names,
//                int position, MainActivity activity) {
//            MainActivity.SelectDeviceDialogFragment frag = new SelectDeviceDialogFragment();
//            Bundle args = new Bundle();
//            args.putStringArray("names", names);
//            args.putInt("position", position);
//            frag.setArguments(args);
//            frag.mmActivity = activity;
//            return frag;
//        }
//
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState) {
//            String[] deviceNames = getArguments().getStringArray("names");
//            int position = getArguments().getInt("position", -1);
//            if (position == -1)
//                position = 0;
//            return new AlertDialog.Builder(getActivity()).setTitle(R.string.select_device).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    mmActivity.connectChannel();
//                }
//            }).setSingleChoiceItems(deviceNames, position, new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    mmActivity.setDevice(which);
//                }
//            }).create();
//        }
//    }

    private final BroadcastReceiver mAdapterReceiver = new BroadcastReceiver() {
                                                         @Override
                                                         public void onReceive(Context context,
                                                                 Intent intent) {
                                                             final String action = intent.getAction();
                                                             if(mDebug) CLog.out("mAdapterReceiver onReceive action", action);
                                                             if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                                                                 int extra = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                                                                 if(mDebug) CLog.out("mAdapterReceiver onReceive extra", extra);
                                                                 if (extra == BluetoothAdapter.STATE_ON) {

                                                                 }
                                                             }
                                                         }
                                                     };

    private final BroadcastReceiver mDeviceReceiver  = new BroadcastReceiver() {
                                                         @Override
                                                         public void onReceive(Context context,
                                                                 Intent intent) {
                                                             final String action = intent.getAction();
                                                             if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                                                                 BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                                                  setDevice(device);
                                                                  if(mDebug) CLog.out("mDeviceReceiver connected", device);
                                                                  if(!mConnected) {
                                                                      Fragment f = getSupportFragmentManager().findFragmentByTag(DisconnectedFragment.TAG);
                                                                      if(f != null && f.isResumed()) {
                                                                          ((DisconnectedFragment)f).setTitleTextConnecting(true);
                                                                      }
                                                                  }
                                                             }

                                                             if (BluetoothDevice.ACTION_UUID.equals(action)) {
                                                                 BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                                                 Parcelable[] uuid = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

                                                                 if(mDebug) CLog.out("ACTION_UUID", device, uuid);

                                                             }

                                                             if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                                                                 BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                                                 if(mDebug) CLog.out("mDeviceReceiver disconnect requested", device);
//                                                                 appendStatusMessage("Low-level ACL disconnect requested by remote device");
                                                             }

                                                             if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                                                                 BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                                                 if(mDebug) CLog.out("mDeviceReceiver disconnected", device);
                                                                 if (!mConnected) {
                                                                     Fragment f = getSupportFragmentManager().findFragmentByTag(DisconnectedFragment.TAG);
                                                                     if (f != null && f.isResumed()) {
                                                                         ((DisconnectedFragment) f).setTitleTextConnecting(false);
                                                                     }
                                                                 }
                                                             }
                                                         }
                                                     };

    // public void appendStatusMessage(String message) {
//        mStatusMessageList.add(message);
//        if (mStatusMessageList.size() > 20) {
//            mStatusMessageList.remove(0);
//        }
//        String msg = "";
//        for (String s : mStatusMessageList) {
//            msg = msg.concat(s).concat("\n");
//        }
//        mStatusMessage.setText(msg);
//    }
    
    private void updateLiveVideoImage(Bitmap bitmap) {
        sLastBitmap = bitmap;
        if(this.mCameraViewFrag != null) {
            mCameraViewFrag.setLiveVideoImage(bitmap);
        }
    }
    
    private boolean showingCameraViewFragment() {
        Fragment f = getSupportFragmentManager().findFragmentByTag(CameraViewFragment.TAG);
        return (f != null && f.isResumed());
    }
    
    private boolean showingSettingsFragment() {
        Fragment f = getSupportFragmentManager().findFragmentByTag(SettingsFragment.TAG);
        Fragment f2 = getSupportFragmentManager().findFragmentByTag(QuickSettingsFragment.TAG);
        Fragment f3 = getSupportFragmentManager().findFragmentByTag(SettingsItemSelectionFragment.TAG);

        return (f != null && f.isAdded()) || (f2 != null && f2.isAdded() || (f3 != null && f3.isAdded()));
    }
    
    private void updateSettingsFragment(CameraSettings cameraSettings) {

        Fragment f;
        
//        f = getSupportFragmentManager().findFragmentByTag(SettingsItemSelectionFragment.TAG);
//        if(mDebug) CLog.out(TAG,"updateSettingsFragment",SettingsItemSelectionFragment.TAG,f != null ? f.isAdded() : "null");
//
//        if (f != null && f.isAdded()) {
//            getSupportFragmentManager().popBackStackImmediate();
//        }
        
        f= getSupportFragmentManager().findFragmentByTag(SettingsFragment.TAG);
        if(mDebug) CLog.out(TAG,"updateSettingsFragment",SettingsFragment.TAG,f != null ? f.isAdded() : "null");

        if (f != null) {
            ((SettingsFragment)f).setSettings(cameraSettings);
            this.setSettingsActionLoadingEnabled(false);
            this.setSettingsActionClosed(true);
        }
        
        f = getSupportFragmentManager().findFragmentByTag(QuickSettingsFragment.TAG);
        if(mDebug) CLog.out(TAG,"updateSettingsFragment",QuickSettingsFragment.TAG,f != null ? f.isAdded() : "null");

        if (f != null) {
            ((QuickSettingsFragment)f).setSettings(cameraSettings,this.mCameraStatus.switchPos);
            this.setSettingsActionLoadingEnabled(false);
            this.setSettingsActionClosed(true);
        }
        
       
    }
    
    public void setSettingsActionLoadingEnabled(boolean enabled) {
        getActionBarHelper().setRefreshActionItemState(enabled);
    }
    
    public void setSettingsActionDisabled(boolean disabled) {
        getActionBarHelper().setDisabledActionItemState(disabled);  
    }
    
    public void setSettingsActionClosed(boolean closed) {
        if(mDebug) CLog.out(TAG,"setSettingsActionClosed",closed);
        getActionBarHelper().setCloseActionItemState(closed);  
    }
    
    public void onCameraConfigureEnabled() {
        setSettingsActionLoadingEnabled(false);
        setSettingsActionDisabled(false);
        setLoadingTitleBarEnabled(false);
    }
    
    public void onCameraConfigureDisabled() {

        Fragment f = getSupportFragmentManager().findFragmentByTag(SettingsItemSelectionFragment.TAG);
        if(mDebug) CLog.out(TAG,"onCameraConfigureDisabled",SettingsItemSelectionFragment.TAG,f != null ? f.isAdded() : "null");

        if (f != null && f.isAdded()) {
            getSupportFragmentManager().popBackStackImmediate();
        }
        
        setLoadingTitleBarEnabled(true);
        setSettingsActionLoadingEnabled(true);
        setSettingsActionClosed(false);
    }
    
    TextView mLoadTextView;
    View mLastTitleView;
    boolean mLoadingTitleEnabled;
    private void setLoadingTitleBarEnabled(boolean enabled, int stringId) {
        if(mDebug) CLog.out(TAG,"setLoadingTitleBarEnabled",enabled,mLastTitleView);

        if(enabled) {            
            if(mLastTitleView == null) {
                mLastTitleView = getActionBarHelper().getTitleView();
                if(mLoadTextView == null) {
                    mLoadTextView = (TextView)getActionBarHelper().setTitleView(R.layout.text_title_bar);
                    mLoadTextView.setText(stringId);
                } else {
                    getActionBarHelper().setTitleView(mLoadTextView);
                }
                mLoadingTitleEnabled = true;
            }
        } else {

            if(mLastTitleView != null) {
                getActionBarHelper().setTitleView(mLastTitleView);
                mLastTitleView = null;
            }
            mLoadingTitleEnabled = false;
        }
    }
    
    private void setLoadingTitleBarEnabled(boolean enabled) {
        setLoadingTitleBarEnabled(enabled,R.string.statusmsgloading);
    }

    
    @Override
    protected void onActionCloseButtonClicked() {
        if(mDebug) CLog.out(TAG,"onActionCloseButtonClicked");
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack();
//        setSettingsActionClosed(false);
    }
    
    // OnSettingsSelectedListener

    @Override
    public void onSettingSelected(CameraSettings settings, int position, long id, String[] items) {
        String[] settingsItems = items;
        int currentItemPosition = 0;
        switch(position) {
            case SettingsFragment.SETTINGS_ITEM_MODE: 
                currentItemPosition = settings.getVideoResIndex();
                break;
            case SettingsFragment.SETTINGS_ITEM_QUALITY:
                currentItemPosition = settings.getQuality();
                break;
            case SettingsFragment.SETTINGS_ITEM_PHOTO_FREQUENCY:
                currentItemPosition = settings.getPhotoModeIndex();
                break;
            case SettingsFragment.SETTINGS_ITEM_METERING:
                currentItemPosition = settings.getMeteringMode();
                break;
            case SettingsFragment.SETTINGS_ITEM_WHITE_BALANCE:
                currentItemPosition = settings.getWhiteBalance();
                break;
            case SettingsFragment.SETTINGS_ITEM_CAMERA_BEEPS:
            	this.sendMessage(ConnectionService.MSG_SET_SETTINGS,settings.switchPosition, position,0);
                mSettingsChanged = true;
            	Log.i("DARRELLCONTOUR","SETTINGS_ITEM_CAMERA_BEEPS");
            	return;
            case SettingsFragment.SETTINGS_ITEM_GPS:
                this.sendMessage(ConnectionService.MSG_SET_SETTINGS,settings.switchPosition,position,0);
                mSettingsChanged = true;
                Log.i("DARRELLCONTOUR","SETTINGS_ITEM_GPS");
                return;
            case SettingsFragment.SETTINGS_ITEM_VIDEO_FORMAT: 
                currentItemPosition = settings.getVideoFormat();
                break;
            case SettingsFragment.SETTINGS_ITEM_MICROPHONE:
            case SettingsFragment.SETTINGS_ITEM_ADVANCED: 
            {
                SettingsSliderSelectionFragment f = SettingsSliderSelectionFragment.newInstance(position,settings);
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction frt = fm.beginTransaction();
                frt.replace(R.id.fragment_container, f,SettingsItemSelectionFragment.TAG);
                frt.addToBackStack(null);
                frt.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                frt.commit();
            }
                return;
            case SettingsFragment.SETTINGS_ITEM_GPS_UPDATE_RATE:
                currentItemPosition = settings.getGpsRateIndex();
                break;
        }
        if(settingsItems != null) {
            SettingsItemSelectionFragment f = SettingsItemSelectionFragment.newInstance(settings.switchPosition,position,currentItemPosition,settingsItems);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction frt = fm.beginTransaction();
            frt.replace(R.id.fragment_container, f,SettingsItemSelectionFragment.TAG);
            frt.addToBackStack(null);
            frt.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            frt.commit();
        }
    }
    
    //OnSettingsItemSelectedListener
    @Override
    public void onSettingsItemSelected(int switchPos, int categoryPosition, int itemPosition)
    {
        Fragment f = getSupportFragmentManager().findFragmentByTag(SettingsItemSelectionFragment.TAG);
        if(f != null) {
//        	getSupportFragmentManager().beginTransaction().remove(f).commit();
        	getSupportFragmentManager().popBackStack();
        } else {
        	return;
        }
        
        this.sendMessage(ConnectionService.MSG_SET_SETTINGS,switchPos, categoryPosition,itemPosition);
        mSettingsChanged = true;
    }
    
    //OnSettingsSliderChangedListener
    @Override
    public void onSettingsSliderChanged(int switchPos, int categoryPos, int itemPosition) {
//        Fragment f = getSupportFragmentManager().findFragmentByTag(SettingsItemSelectionFragment.TAG);
//        if(f != null) {
//            getSupportFragmentManager().beginTransaction().remove(f).commit();
//        } else {
//            return;
//        }        
        if(mDebug) CLog.out(TAG,"onSettingsSliderChanged",categoryPos,itemPosition);
        this.sendMessage(ConnectionService.MSG_SET_SETTINGS,switchPos,categoryPos,itemPosition);
        mSettingsChanged = true;
    }
    
    @Override
    public void onSettingsSliderCancelled(CameraSettings unchangedSettings) {
        this.mUpdateSettings = unchangedSettings;
        if(mDebug) CLog.out(TAG,"onSettingsSliderCancelled",mUpdateSettings);
        sendMessageWithDevice(ConnectionService.MSG_GET_SETTINGS);
    }
    
    @Override
	public void onSendSettingsToCamera(CameraSettings settings) {
//		this.sendMessage(ConnectionService.MSG_SET_SETTINGS, settings);
	}

    @Override
    public void onApplySettings() {
//        setSettingsActionLoadingEnabled(true);
        if (mSettingsChanged && mConnected) {
//            sendMessageWithDevice(ConnectionService.MSG_START_VIDEO);
            this.sendMessageWithDevice(ConnectionService.MSG_APPLY_SETTINGS);
            mSettingsChanged = false;
        }
    }
    
    @Override
    public void onQuickSettingSelected(int position, long id, String[] items) {
        String[] settingsItems = items;
        int currentItemPosition = 0;
        int titleCategoryPosition = 0;
        switch (position) {
        case QuickSettingsFragment.SETTINGS_ITEM_RESET_TO_DEFAULTS:
            // String msg =
            // String.format(getString(R.string.dialog_msg_reset_defaults),
            // this.mSwitchPosition);
            String msg = getString(R.string.settingsresetdefaultmsg);
            new AlertDialog.Builder(this).setMessage(msg).setTitle(R.string.settingsresetdefaultalerttitle).setPositiveButton(getString(R.string.ok), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sendMessage(ConnectionService.MSG_RESET_SETTINGS,0, 0, 0);
                    dialog.dismiss();
                }
            }).setNegativeButton(getString(R.string.alertbuttoncancel), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).create().show();

            return;
        case QuickSettingsFragment.SETTINGS_ITEM_ABOUT:
            loadAboutFragment();
            return;
        case QuickSettingsFragment.SETTINGS_ITEM_STATUS:
            loadAppInfoFragment();
            return;
        case QuickSettingsFragment.SETTINGS_ITEM_VIDEO_FORMAT: 
            currentItemPosition = mUpdateSettings.getVideoFormat();
            //TODO - Fix this shitty hack. 
            titleCategoryPosition = SettingsFragment.SETTINGS_ITEM_VIDEO_FORMAT;
            break;
        case QuickSettingsFragment.SETTINGS_ITEM_SWITCH_ONE: 
        case QuickSettingsFragment.SETTINGS_ITEM_SWITCH_TWO:
            FragmentManager fm = getSupportFragmentManager();
            Fragment f = fm.findFragmentByTag(SettingsFragment.TAG);
            if (f == null) {
                f = SettingsFragment.newInstance(position,mUpdateSettings);
            } 
            if(f.isResumed() == false) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.replace(R.id.fragment_container, f,SettingsFragment.TAG);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.addToBackStack(null);
                ft.commit();
                setSettingsActionDisabled(true);
            }
            return;
        }
        if(settingsItems != null) {
            SettingsItemSelectionFragment f = SettingsItemSelectionFragment.newInstance(mUpdateSettings.switchPosition,titleCategoryPosition,currentItemPosition,settingsItems);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction frt = fm.beginTransaction();
            frt.replace(R.id.fragment_container, f,SettingsItemSelectionFragment.TAG);
            frt.addToBackStack(null);
            frt.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            frt.commit();
        }
    }
    
    //CameraViewUpdateListener
    @Override
    public void onStartVideoStream() {
//        if(mConnected)
        if(mDebug) CLog.out(TAG,"onStartVideoStream");
            sendMessageWithDevice(ConnectionService.MSG_START_VIDEO);
    }
    
    @Override
    public void onStartRecordRequested() {
        this.sendMessageWithDevice(ConnectionService.MSG_START_RECORDING);
    }

    @Override
    public void onStopRecordRequested() {
        this.sendMessageWithDevice(ConnectionService.MSG_STOP_RECORDING);
    }
    
    @Override
    public CameraStatus onCameraViewStarted(CameraViewFragment fragment) {
        if(mDebug) CLog.out(TAG,"onCameraViewStarted",sLastBitmap);
        if(mConnected)
            this.setLoadingTitleBarEnabled(true);
        if(sLastBitmap != null)
            fragment.setLiveVideoImage(sLastBitmap);
        return mCameraStatus;
    }
    
    @Override
    public void onCameraViewStopped() {
        mCameraViewFrag = null;        
    }

    public class ScreenReceiver extends BroadcastReceiver {

        private boolean wasScreenOn = true;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                // do whatever you need to do here
                wasScreenOn = false;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                // and do whatever you need to do here
                wasScreenOn = true;
            }
        }

        public boolean wasScreenOn() {
            return wasScreenOn;
        }
    };

    
    private final BroadcastReceiver mCameraReceiver  = new BroadcastReceiver() {
        public void onReceive(Context context,
                Intent intent) {
            String action = intent.getAction();
            if (action.equals(CameraComms.ACTION_SETTINGS)) {
                CameraSettings cameraSettings = (CameraSettings)intent.getParcelableExtra(CameraComms.EXTRA_PARCELABLE);
                onCameraSettingsReceived(cameraSettings);
            } else if (action.equals(CameraComms.ACTION_CAMERA_CONFIGURE_ENABLED)) {
                if(mDebug) CLog.out(TAG,"Camera Receiver received config enabled");
        		onCameraConfigureEnabled();
            } else if (action.equals(CameraComms.ACTION_CAMERA_CONFIGURE_DISABLED)) {
                if(mDebug) CLog.out(TAG,"Camera Receiver received config disabled");
                onCameraConfigureDisabled();
        	} else if(action.equals(CameraComms.ACTION_STATUS)) {
                CameraStatus cs = (CameraStatus)intent.getParcelableExtra(CameraComms.EXTRA_PARCELABLE);
                onCameraStatusReceived(cs);
            }  else if(action.equals(CameraComms.ACTION_RECORDING_STARTED)) {
                if(mDebug) CLog.out(TAG,"Camera Receiver received action recording started");
                if(showingCameraViewFragment())
                    setSettingsActionDisabled(true);

            } else if(action.equals(CameraComms.ACTION_RECORDING_STOPPED)) {
                if(mDebug) CLog.out(TAG,"Camera Receiver received action recording stopped");
                if(showingCameraViewFragment())
                    setSettingsActionDisabled(false);
            }
        }
    };
    
    
    private void onCameraSettingsReceived(CameraSettings cameraSettings) {
        if(mDebug) CLog.out(TAG,"Camera Receiver received settings",cameraSettings);
        
        boolean justConnected = mUpdateSettings == null;
        try {
            mUpdateSettings = (CameraSettings) cameraSettings.clone();
        } catch (CloneNotSupportedException e) {
            mUpdateSettings = cameraSettings;
            e.printStackTrace();
        }
        updateSettingsFragment(mUpdateSettings);
        dispatchAnalytics(justConnected);
        if(justConnected) {
            saveCameraModelAndFwVersion();
        } else {
            if(mUpdateSettings.cameraModel != CameraComms.MODEL_PLUS_2) {
                sendMessageWithDevice(ConnectionService.MSG_STOP_VIDEO);
                sHandler.postDelayed(new Runnable() {
                    public void run() {
                        sendMessageWithDevice(ConnectionService.MSG_START_VIDEO);
                    }
                },1000);
            }
        }
    }
    
    private int mUserFocusedSwitchPosition = -1;

    private void onCameraStatusReceived(CameraStatus cs) {
        if(mDebug) CLog.out(TAG,"Camera Receiver received action status");
        mCameraStatus = cs;
         if(mUpdateSettings != null && mLoadingTitleEnabled) {

             if (mCameraViewFrag != null && mCameraViewFrag.isVisible()) {
                 mLoadingTitleEnabled = false;
                 mLastTitleView = null;
                 getActionBarHelper().setTitleView(mCameraViewFrag.mStatusBar);
             } else {
                 setLoadingTitleBarEnabled(false);
             }
            if(cs.cameraState != CameraStatus.CAMERA_STATE_RECORDING)
                setSettingsActionDisabled(false);
        }

         int newSwitchPos = cs.switchPos;
         if(mUserFocusedSwitchPosition != -1 && mUserFocusedSwitchPosition != newSwitchPos) {
             if(mUpdateSettings != null && mUpdateSettings.cameraModel != CameraComms.MODEL_PLUS_2) {
                 mUpdateSettings.switchPosition = (byte) newSwitchPos;
                 
                 Fragment f = getSupportFragmentManager().findFragmentByTag(SettingsItemSelectionFragment.TAG);

                 if (f != null && f.isAdded()) {
                     getSupportFragmentManager().popBackStackImmediate();
                 }
                 
                 updateSettingsFragment(mUpdateSettings);
             }
         }
         mUserFocusedSwitchPosition = newSwitchPos;
    }

    // FragmentManager.OnBackStackChangedListener
    @Override
    public void onBackStackChanged() {
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
       if(mDebug) CLog.out(TAG,"BACK STACK COUNT",backStackCount);
       if(backStackCount < 1) {
           this.onApplySettings();
           setSettingsActionClosed(false);
       }
    }

    // NetHelperCallback
    
    @Override
    public void onCameraVersionDownloaded(int cameraModel, int major, int minor, int build) {
        if(this.isFinishing()) return;

        if(mUpdateSettings != null) {
            if (mUpdateSettings.cameraModel == cameraModel) {
                if (mDebug)
                    CLog.out(TAG, "onCameraVersionDownloaded", "FOUND MATCH", mUpdateSettings.cameraModel, mUpdateSettings.majorVersion, mUpdateSettings.minorVersion, mUpdateSettings.buildVersion, cameraModel, major, minor, build);
                doFirmwareCheck(major, minor, build);
            } else {
                if (mDebug)
                    CLog.out(TAG, "onCameraVersionDownloaded", "NO MATCH", mUpdateSettings.cameraModel, mUpdateSettings.majorVersion, mUpdateSettings.minorVersion, mUpdateSettings.buildVersion, cameraModel, major, minor, build);
            }
        } else {
            setSettingsActionLoadingEnabled(true);
            sendMessageWithDevice(ConnectionService.MSG_GET_SETTINGS);
            loadCameraViewFrag();
        }
    }
    
    @Override
    public void onCameraVersionDownloadedFailed() {
        if(this.isFinishing()) return;
        if(mUpdateSettings != null)
            this.doFirmwareCheck(mUpdateSettings.getModelMajorVersionMin(),mUpdateSettings.getModelMinorVersionMin(),mUpdateSettings.getModelBuildVersionMin());   
        else {
            setSettingsActionLoadingEnabled(true);
            sendMessageWithDevice(ConnectionService.MSG_GET_SETTINGS);
            loadCameraViewFrag();
        }
    }
    
    public void saveCameraModelAndFwVersion() {
        SharedPrefHelper.saveFwVersion(this, mUpdateSettings.cameraModel, mUpdateSettings.majorVersion, mUpdateSettings.minorVersion, mUpdateSettings.buildVersion);
    }
    
    public void dispatchAnalytics(boolean firstConnection) {
        if(this.isFinishing() || mDevice == null || mUpdateSettings == null) return;

        if(mCameraModel == null)
            mCameraModel = SharedPrefHelper.checkDeviceAddress(this, mDevice.getAddress());
        if(mCameraModel == null) {
            mCameraModel = ContourUtils.getCameraModel(this, mUpdateSettings.cameraModel);
            SharedPrefHelper.saveDeviceAddress(this, mDevice.getAddress(), mCameraModel);
        }
        if(firstConnection)
            EasyTracker.getTracker().trackEvent(Constants.GA_CATEGORY_CONNECTION, Constants.GA_ACTION_CONNECTED, mCameraModel, -1L);
        EasyTracker.getTracker().trackEvent(Constants.GA_CATEGORY_SETTINGS, mUpdateSettings.getGps() ? Constants.GA_ACTION_GPS_ENABLED : Constants.GA_ACTION_GPS_DISABLED, mCameraModel,-1L);
    }
    
    public void doFirmwareCheck(int minMajor, int minMinor, int minBuild) {
        int fwVersionVal = SharedPrefHelper.getFwVersionVal(this);
//        int modelCurr = (fwVersionVal >> 24) & 0xFF;
        int majorCurr = (fwVersionVal >> 16) & 0xFF;
        int minorCurr = (fwVersionVal >> 8) & 0xFF;
        int buildCurr = fwVersionVal & 0xFF;
        
        boolean firmwareIsOld = isFirmwareVersionOld(majorCurr,minorCurr,buildCurr,minMajor,minMinor,minBuild);
        if(mDebug) CLog.out(TAG,"doFirmwareCheck",firmwareIsOld);
        if (firmwareIsOld) {
            if (mDebug)
                loadCameraViewFrag();
            else
                loadFirmwareUpgradeFrag();
        } else {
            loadCameraViewFrag();
        }
    }
    
    public boolean isFirmwareVersionOld(int major, int minor, int build, int minMajor, int minMinor, int minBuild)
    {
        if (major < minMajor) {
            return true;
        } else if (major == minMajor) {
            if (minor < minMinor) {
                return true;
            } else if (minor == minMinor) {
                if (build < minBuild) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public String getCameraModel()
    {
        return this.mCameraModel;
    }
    public void onClickedRecord(View view) {
    	Log.i("CAMERAVIEWFRAGMENT","onClickedRecord!");
//    	MotionEvent event = new MotionEvent();
//    	RecordButton.onTouchEvent(MotionEvent.this);
    	RecordButton recordButton = (RecordButton)findViewById(R.id.record_button);
    	recordButton.onClickEvent();
    }

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		Log.i("MAINACTIVITY","ONFOCUS!");
		
	}
}