
package com.reconinstruments.jetconnectdevice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.commonwidgets.FeedbackDialog;
import com.reconinstruments.commonwidgets.ReconJetDialog;
import com.reconinstruments.utils.BTHelper;

/**
 * <code>WaitingForPhoneActivity</code> is the base class to support android and iPhone pairing request.
 */
public abstract class WaitingForPhoneActivity extends FragmentActivity {

    private static final String TAG = WaitingForPhoneActivity.class.getSimpleName();

    protected TextView mTitleTV;
    protected TextView mBody1TV;
    protected TextView mBody2TV;
    private String btName;
    
    protected ReconJetDialog mQuitDialog;
    private boolean mUserWantsToQuit = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title_two_lines);
        mTitleTV = (TextView) findViewById(R.id.title);
        mBody1TV = (TextView) findViewById(R.id.body1);
        mBody2TV = (TextView) findViewById(R.id.body2);
        btName = BTHelper.getInstance(WaitingForPhoneActivity.this.getApplicationContext()).getLastPairedDeviceName();
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(scanModeChangedReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        registerReceiver(btBondStateReceiver, new IntentFilter(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        registerReceiver(hudStateReceiver, new IntentFilter("HUD_STATE_CHANGED"));

        BTHelper.getInstance(getApplicationContext()).ensureBluetoothDiscoverability();
    }

    @Override
    protected void onStop() {
        try {
            this.unregisterReceiver(scanModeChangedReceiver);
            this.unregisterReceiver(btBondStateReceiver);
            this.unregisterReceiver(hudStateReceiver);
        } catch (IllegalArgumentException e) {
            // ignore it
        }
        BTHelper.getInstance(getApplicationContext()).disableBluetoothDiscoverability();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FeedbackDialog.dismissDialog(this);
    }

//    @Override
//    public void onBackPressed() {
//        Intent i = new Intent(this, ConnectSmartphoneActivity.class);
//        startActivity(i);
//        finish();
//    }

    // bluetooth adapter scan mode changed receiver, keep making bluetooth discoverable once it disable 
    // since can't change Android system default scan duration.
    private final BroadcastReceiver scanModeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                        BluetoothAdapter.ERROR);
                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.i(TAG, "ensure bluetooth discoverable");
                        BTHelper.getInstance(getApplicationContext()).ensureBluetoothDiscoverability();
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.w(TAG, "neither discoverable nor connectable from remote Bluetooth devices.");
                        break;
                }
            }
        }
    };

    // bluetooth device bond state receiver, to get the pairing state changed event
    // this receiver listening the pairing state changed event and takes proper action
    private final BroadcastReceiver btBondStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (intent.getExtras().getInt(BluetoothDevice.EXTRA_BOND_STATE) == BluetoothDevice.BOND_BONDED) {
                    Log.i(TAG, "paired with " + device.getName());
                    Log.i(TAG, "connecting to " + device.getName());
                    
                    String deviceName = "<font color=\"#FFFFFF\">" + device.getName() + "</font>";
                    // in the first time pairing phase, it always reports 'Connection unsuccessful' on iPhone side
                    // Mfi can't receive any SPPM callback event on the Jet side.
                    // at this point we should provide the timeout mechanism to recover from the bad status
                    FeedbackDialog.showDialog(WaitingForPhoneActivity.this, "Connecting", deviceName,
                                                                                null, FeedbackDialog.SHOW_SPINNER);
                    onHUDServiceConnecting();
                    
                } else if (intent.getExtras().getInt(BluetoothDevice.EXTRA_BOND_STATE) == BluetoothDevice.BOND_BONDING) {
                    Log.i(TAG, "pairing with " + device.getName());
                } else if (intent.getExtras().getInt(BluetoothDevice.EXTRA_BOND_STATE) == BluetoothDevice.BOND_NONE) {
                    Log.w(TAG, "fail to pair with " + device.getName());
                    FeedbackDialog.dismissDialog(WaitingForPhoneActivity.this);
                }
            }
        }
    };

     // listening the mfi/HUDService connection sate changed event and take proper action.
    private final BroadcastReceiver hudStateReceiver = new BroadcastReceiver() {
        int previousState = -1;
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if ("HUD_STATE_CHANGED".equals(action)) {
                int state = intent.getIntExtra("state", 0);
                if(previousState == state){
                    return;
                }

                if (state == 0) {
                    Log.w(TAG, "HUD_STATE_CHANGED changed to 0, HUDService reports: disconnected");

                    FeedbackDialog.showDialog(WaitingForPhoneActivity.this, "Fail to connect", null,
                                                FeedbackDialog.ICONTYPE.WARNING, FeedbackDialog.HIDE_SPINNER);
                    
                    //gives 2 seconds delay to dismiss the dialog
                    new CountDownTimer(2 * 1000, 1000) {
                        public void onTick(long millisUntilFinished) {
                        }
                        public void onFinish() {
                            FeedbackDialog.dismissDialog(WaitingForPhoneActivity.this);
                        }
                    }.start();
                    
                    onHUDServiceDisconnected();
                } else if (state == 1) {
                    Log.i(TAG, "HUD_STATE_CHANGED changed to 1, HUDService reports: connecting");
                    
                    // paired but waiting for connecting
                    btName = BTHelper.getInstance(WaitingForPhoneActivity.this.getApplicationContext()).getLastPairedDeviceName();
                    String deviceName = "<font color=\"#FFFFFF\">" + btName + "</font>";
                    FeedbackDialog.showDialog(WaitingForPhoneActivity.this, "Connecting", deviceName,
                                                                                null, FeedbackDialog.SHOW_SPINNER);
                    onHUDServiceConnecting();

                } else if (state == 2) {
                    Log.i(TAG, "HUD_STATE_CHANGED to 2, HUDService reports: connected");
                    
                    onHUDServiceConnected();
                    btName = BTHelper.getInstance(WaitingForPhoneActivity.this.getApplicationContext()).getLastPairedDeviceName();
                    String deviceName = "<font color=\"#FFFFFF\">" + btName + "</font>";
                    FeedbackDialog.showDialog(WaitingForPhoneActivity.this, "Connected", deviceName,
                                                FeedbackDialog.ICONTYPE.CHECKMARK, FeedbackDialog.HIDE_SPINNER);
                    //gives 2 seconds delay to dismiss the dialog
                    new CountDownTimer(2 * 1000, 1000) {
                        public void onTick(long millisUntilFinished) {
                        }
                        public void onFinish() {
                        	showTutorialOnItemHost();
                        	WaitingForPhoneActivity.this.finish();
                        }
                    }.start();
                }
            }
        }
    };
    
    @Override
    public void onBackPressed() {
    	Intent intent = new Intent(this, ChooseDeviceActivity.class);
		CommonUtils.launchParent(this, intent, true);
    }
    
    private void showTutorialOnItemHost(){
    	if(ConnectSmartphoneActivity.isCalledFromInitialSetup()){
    		Settings.System.putInt(getContentResolver(), "com.reconinstruments.itemhost.SHOULD_SHOW_COACHMARKS", 1);
    	}
    }

    abstract void onHUDServiceDisconnected();
    abstract void onHUDServiceConnecting();
    abstract void onHUDServiceConnected();
}
