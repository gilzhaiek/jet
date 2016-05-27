
package com.reconinstruments.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetooth;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>BTHelper</code> provides the APIs to access the
 * Bluetooth/MFi/HUDService component and the properties related with.
 */
public class BTHelper {
    private static final String TAG = BTHelper.class.getSimpleName();
    private static BTHelper instance = null;
    private Context mContext;
    public static final int BT_STATE_CONNECTED = 2;
    public static final int BT_STATE_CONNECTING = 1;
    public static final int BT_STATE_DISCONNECTED = 0;
    
    public static final String BLUETOOTH_DONT_AUTO_RECONNECT = "BluetoothDontAutoReconnect";

    protected BTHelper(Context context) {
        mContext = context;
    }

    public static BTHelper getInstance(Context context) {
        if (instance == null) {
            instance = new BTHelper(context);
        }
        return instance;
    }

    private IBluetooth getIBluetooth() {
        IBluetooth ibt = null;
        try {
            Class c2 = Class.forName("android.os.ServiceManager");
            Method m2 = c2.getDeclaredMethod("getService", String.class);
            IBinder b = (IBinder) m2.invoke(null, "bluetooth");
            Class c3 = Class.forName("android.bluetooth.IBluetooth");
            Class[] s2 = c3.getDeclaredClasses();
            Class c = s2[0];
            Method m = c.getDeclaredMethod("asInterface", IBinder.class);
            m.setAccessible(true);
            ibt = (IBluetooth) m.invoke(null, b);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ibt;
    }

    public enum DeviceType {
        ANDROID, IOS
    }

    /**
     * unpair the device
     * @param address
     */
    public boolean unpairDevice(String address){
        String notReconnecting = Settings.System.getString(mContext.getContentResolver(), BLUETOOTH_DONT_AUTO_RECONNECT);
        if(notReconnecting != null && notReconnecting.contains(address)){ //remove the address from  NOTRECONNECTING string
            notReconnecting = notReconnecting.replace("," + address, "");
            Settings.System.putString(mContext.getContentResolver(), BLUETOOTH_DONT_AUTO_RECONNECT, notReconnecting);
        }
        try {
            IBluetooth mBtService = getIBluetooth();
            return mBtService.removeBond(address);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean shouldAutoReconnect(){
        int launchReconnect = 0;
        String notReconnecting = null;
        
        if(getBTConnectionState() != 0){
            return false;
        }
        
        try{
            launchReconnect = Settings.System.getInt(mContext.getContentResolver(), "LAUNCH_RECONNECT");
            notReconnecting = Settings.System.getString(mContext.getContentResolver(), BLUETOOTH_DONT_AUTO_RECONNECT);
        }catch (SettingNotFoundException e){
            return false;
        }
        
        if(launchReconnect == 0){
            return false;
        }
        
        String btAddress = getLastPairedDeviceAddress();
        if("".equals(btAddress)){
            return false;
        }
        if(notReconnecting != null && notReconnecting.contains(btAddress)){
            return false;
        }
        
        //disable LAUNCH_RECONNECT
        Settings.System.putInt(mContext.getContentResolver(), "LAUNCH_RECONNECT", 0);
        
        return true;
    }
    
    /**
     * get paired device list
     */
    public List<BluetoothDevice> getPairedDevice(){
        return new ArrayList<BluetoothDevice>(BluetoothAdapter.getDefaultAdapter().getBondedDevices());
    }
    
    /**
     * get the jet name
     */
    public String getJetName() {
        return BluetoothAdapter.getDefaultAdapter().getName();
    }

    //enable or disable bluetooth
    public void setBluetooth(boolean enable) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            restartHUDService();
            bluetoothAdapter.enable();
        }
        else if(!enable && isEnabled) {
             bluetoothAdapter.disable();
        }
    }

    /**
     * enable bluetooth discoverability
     */
    public void ensureBluetoothDiscoverability() {
        try {
            IBluetooth mBtService = getIBluetooth();
            if (mBtService.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                // default mode is BluetoothAdapter.SCAN_MODE_CONNECTABLE
                try {
                    // mBtService.setDiscoverableTimeout(120);
                    mBtService
                            .setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 120);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * disable bluetooth discoverability
     */
    public void disableBluetoothDiscoverability() {
        try {
            IBluetooth mBtService = getIBluetooth();
            if (mBtService.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE) {
                try {
                    mBtService.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * get HUDService connection state 0 disconnected, 1 connecting, 2 connected
     */
    public int getBTConnectionState() {
        return getBTConnectionState(mContext);
    }

    public static int getBTConnectionState(Context context) {
        int res = 0;
        try {
            res = Settings.System.getInt(context.getContentResolver(), "BTConnectionState");
        } catch (SettingNotFoundException e) {
        }
        return res;
    }

    public static boolean isConnected(Context context) {
        return getBTConnectionState(context)==2;
    }

    /**
     * get the most recent paired device type 0 android, 1 ios
     */
    public int getLastPairedDeviceType() {
        int res = 0;
        try {
            res = Settings.System.getInt(mContext.getContentResolver(), "LastPairedDeviceType");
        } catch (SettingNotFoundException e) {
        }
        return res;
    }

    /**
     * get the most recent paird device address 0 android, 1 ios
     */
    public String getLastPairedDeviceAddress() {
        String deviceAddress = Settings.System.getString(mContext.getContentResolver(),
                "LastPairedDeviceAddress");
        if (deviceAddress == null) {
            deviceAddress = "";
        }
        return deviceAddress;
    }

    /**
     * get the most recent paired device name
     */
    public String getLastPairedDeviceName() {
        String deviceName = Settings.System.getString(mContext.getContentResolver(),
                "LastPairedDeviceName");
        if (deviceName == null)
            deviceName = "";
        return deviceName;
    }

    /**
     * set the most recent paired device type 0 for android , 1 for ios
     */
    public void setLastPairedDeviceType(int deviceType) {
        Settings.System.putInt(mContext.getContentResolver(), "LastPairedDeviceType", deviceType);
    }

    /**
     * set the most recent paired device address
     * 
     * @param deviceAddress
     */
    public void setLastPairedDeviceAddress(String deviceAddress) {
        Settings.System.putString(mContext.getContentResolver(), "LastPairedDeviceAddress",
                deviceAddress);
    }

    /**
     * set the most recent paired device name
     * 
     * @param deviceName
     */
    public void setLastPairedDeviceName(String deviceName) {
        Settings.System
                .putString(mContext.getContentResolver(), "LastPairedDeviceName", deviceName);
    }

    /**
     * disconnect the most recent device
     */
    public void disconnect() {
        Log.i(TAG, "Send disconnect request to HUDService");
        Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.request.disconnect");
        int deviceType = getLastPairedDeviceType();
        if (deviceType == 1) { // ios connected
            i.putExtra("deviceType", DeviceType.IOS.name());
        } else {
            i.putExtra("deviceType", DeviceType.ANDROID.name());
        }
        mContext.sendBroadcast(i);
    }

    /**
     * reconnect the most recent device
     */
    public void reconnect() {
        Log.i(TAG, "Send connect request to HUDService");
        Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.connect");
        i.putExtra("address", getLastPairedDeviceAddress());
        int deviceType = getLastPairedDeviceType();
        if (deviceType == 1) { // ios connected
            i.putExtra("deviceType", DeviceType.IOS.name());
        } else {
            i.putExtra("deviceType", DeviceType.ANDROID.name());
        }
        i.putExtra("attempts", 0); // don't need attempt in reconnecting process
        mContext.sendBroadcast(i);
    }

    /**
     * restart the HUDService completely by killing the HUDService process
     */
    public void restartHUDService() {
        Log.i(TAG, "Send kill request to HUDService");
        Intent i = new Intent("com.reconinstruments.mobilesdk.hudconnectivity.kill");
        mContext.sendBroadcast(i);
    }
    
    public void reEnableMapSS1() {
        disconnectMapSS1();
        connectMapSS1_delayed();
    }
    
    private void disconnectMapSS1() {
        Log.i(TAG, "disconnectMapSS1");
        Intent i = new Intent("RECON_SS1_MAP_COMMAND");
        i.putExtra("command", 600);
        mContext.sendBroadcast(i);
    }
    
    private void connectMapSS1_delayed() {
        Log.i(TAG, "connecting with Map with a delay of 5 seconds");
        mapHandler.removeCallbacks(connectMapSS1_runnable);
        mapHandler.postDelayed(connectMapSS1_runnable, 5000);
    }

    private Handler mapHandler = new Handler();

    private Runnable connectMapSS1_runnable = new Runnable() {
        public void run() {
            Log.i(TAG, "connectMapSS1");
            Intent i = new Intent("RECON_SS1_MAP_COMMAND");
            i.putExtra("command", 500);
            i.putExtra("address", getLastPairedDeviceAddress());
            mContext.sendBroadcast(i);
        }
    };

}
