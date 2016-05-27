//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.ble_ss1;
import com.reconinstruments.utils.SettingsUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.DEVM;
import com.stonestreetone.bluetopiapm.DEVM.ConnectFlags;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceFeature;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFilter;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.GATM.ConnectionInformation;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager;
import com.reconinstruments.utils.DeviceUtils;

public class TheBLEService extends Service {
    private static final String TAG = "TheBLEService";

    static final int  REMOTE_DATA_ATT_HANDLE = 0x0012;
    static final int  REMOTE_NOTIFY_ATT_HANDLE = 0x0013;
    static final byte[] REMOTE_NOTIFY_DATA = new byte[]{0x01,0x00};

    //scan for 5 seconds
    private static final int  SCAN_DURATION =  5000;
    //time before pairing dialog appears
    static public int CONNECTION_TIMEOUT = 5000;
    static final String remotePrefix = "ReconR";

    static final int BTPM_ERROR_CODE_DEVICE_IS_CURRENTLY_CONNECTED = -10045;

    private static final int BLE_NOTIFICATION_ID = 10;
    private static final int BLE_DISCONNECT_NUM_RETRIES = 10;
    private static final int BLE_DISCCONECT_WAIT_MS = 500;

    public static int DISCONNECTED = 0;
    public static int CONNECTING = 1;
    public static int CONNECTED = 2;

    public int state = DISCONNECTED;
    boolean timeoutExpired;


    // The mac address stored in BLE.RIB
    BluetoothAddress ribMac;

    public HashMap<BluetoothAddress, BLEDevice> devices;

    public NotificationManager notificationManager;

    // Managers
    DEVM deviceManager;
    GenericAttributeClientManager genericAttributeClientManager;

    public Handler activityHandler;

    Handler connectionTimeoutHandler;
    Handler inputHandler;

    SS1Callbacks callbacks;
        
    RemoteInputManager inputMan;

    public class BLEDevice{
        public BLEDevice(BluetoothAddress address,String name) {
            this.address = address;
            this.name = name;
        }
        public void setName(String name){
            this.name = name;
        }
        public boolean isRemote(){
            return name!=null&&name.startsWith(remotePrefix);
        }
        public BluetoothAddress address;
        public String name;
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"onCreate");
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        connectionTimeoutHandler = new TimeoutHandler();

        callbacks = new SS1Callbacks(this);
                
        inputMan = new RemoteInputManager(this);

        // Register a receiver for a Bluetooth "State Changed" broadcast event.
        registerReceiver(bluetoothBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public int onStartCommand(Intent i, int flag, int startId) {

        Log.d(TAG,"onStartCommand");
        // Terminate if ANT mode
        if (SettingsUtil.getBleOrAnt() == SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT) {
            Log.d(TAG,"ANT mode, stopping BLE service");
            stopSelf();
            return START_NOT_STICKY;
        } else {
            Log.d(TAG,"BLE mode");
        }

        Log.d(TAG,"Attempt to read BLE RIB File");
        byte[] bleRib = BLEUtils.readBLEFile();
        if(bleRib!=null){
            Log.d(TAG,BLEUtils.byteArrayToHex(bleRib)+" in BLE RIB");
            ribMac = new BluetoothAddress(bleRib);
        }
        else
            Log.d(TAG,"no mac address in BLE RIB"); 


        if(deviceManager==null){
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(bluetoothAdapter != null) {
                if(bluetoothAdapter.isEnabled()) {
                    profileEnable();
                } else {
                    bluetoothAdapter.enable();
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        profileDisable();
        // TOOD unregister broadcast receivers?
        unregisterReceiver(bluetoothBroadcastReceiver);
        inputMan.onDestroy();
    }

    // The important part here is profileEnable and profileDisable calls
    private final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    switch(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    case BluetoothAdapter.STATE_ON:
                        profileEnable();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        profileDisable();
                        break;
                    }
                }
            }
        };
    private final void profileEnable() {
        Log.d(TAG,"profileEnable");
        if (SettingsUtil.getBleOrAnt() == SettingsUtil.LOW_POWER_WIRELESS_MODE_ANT) {
            Log.v(TAG,"Ant mode don't enable profile");
            return;
        }
        synchronized(this) {
            try {
                if(deviceManager==null){
                    deviceManager = new DEVM(callbacks.deviceEventCallback);
                    try {
                        if (deviceManager.acquireLock()) {
                            int res = deviceManager.enableLocalDeviceFeature(LocalDeviceFeature.BLUETOOTH_LOW_ENERGY);
                        } else {
                            throw new RuntimeException("Cannot acquire SS1 DEVM lock");
                        }
                    } finally {
                        deviceManager.releaseLock();
                    }
                    if(genericAttributeClientManager==null)
                        genericAttributeClientManager = new GenericAttributeClientManager(callbacks.genericAttributeClientEventCallback);

                    onReady();
                }
            } catch(Exception e) {
                e.printStackTrace();
                // BluetopiaPM server couldn't be contacted. This should never
                // happen if Bluetooth was successfully enabled.
                Toast.makeText(this, "ERROR: Could not connect to the BluetopiaPM service", Toast.LENGTH_LONG).show();
                BluetoothAdapter.getDefaultAdapter().disable();
            }
        }
    }
    private final void profileDisable() {
        Log.d(TAG,"profileDisable");
        synchronized(this) {
            if(genericAttributeClientManager != null) {
                genericAttributeClientManager.dispose();
                genericAttributeClientManager = null;
            }

            if(deviceManager != null) {
                BluetoothAddress[] connectedDevices = deviceManager.queryRemoteDeviceList(RemoteDeviceFilter.CURRENTLY_CONNECTED, 0);
                for(BluetoothAddress address:connectedDevices){
                    Log.d(TAG, "Disconnected from connected device: " + address);
                    deviceManager.disconnectRemoteLowEnergyDevice(address, true);
                }

                try {
                    if (deviceManager.acquireLock()) {
                        int counter = 0;
                        // Retry disabling local feature a few times as we need to wait for the disconnect from above to finish
                        while (counter < BLE_DISCONNECT_NUM_RETRIES) {
                            try {
                                Thread.sleep(BLE_DISCCONECT_WAIT_MS);
                            } catch (InterruptedException ex) {
                            }

                            if (deviceManager.disableLocalDeviceFeature(LocalDeviceFeature.BLUETOOTH_LOW_ENERGY) == 0) {
                                break;
                            }
                            counter++;
                        }
                    } else {
                        throw new RuntimeException("Cannot aquire SS1 DEVM lock");
                    }
                } finally {
                    deviceManager.releaseLock();
                }
                deviceManager.dispose();
                deviceManager = null;
            }
        }
        if(state!=CONNECTED)
            showDisconnectedNotification();
    }
    //called when profile connected and bluetooth adapter on
    private void onReady(){
        Log.d(TAG, "onReady");

        devices = new HashMap<BluetoothAddress,BLEDevice>();

        state = DISCONNECTED;
        showDisconnectedNotification();

        // query existing remote device information
        // we need this in case the service is restarted, because we won't discover devices that have been discovered already
        BluetoothAddress[] cachedDevices = deviceManager.queryRemoteDeviceList(RemoteDeviceFilter.ALL_DEVICES, 0);
        for(BluetoothAddress address:cachedDevices){
            RemoteDeviceProperties deviceProperties = deviceManager.queryRemoteDeviceProperties(address,false);

            BLEDevice device = new BLEDevice(deviceProperties.deviceAddress,deviceProperties.deviceName);
            devices.put(address, device);

            if(matchBleRIB(device.address)){
                connect(device.address);
            }
        }

        if(state==DISCONNECTED){ // if not connected or connecting
            ConnectionInformation[] connections = genericAttributeClientManager.queryConnectedDevices();
            for(ConnectionInformation connection:connections){
                if(matchBleRIB(connection.remoteDeviceAddress)){
                    Log.d(TAG, "connected to remote "+connection.remoteDeviceAddress);
                    onConnect();
                    break;
                }
            }
        }

        if(state==DISCONNECTED){
            startScan(SCAN_DURATION);

            Log.d(TAG, "lookingForRemote");
            timeoutExpired = false;
            connectionTimeoutHandler.sendEmptyMessageDelayed(0, CONNECTION_TIMEOUT);
            Log.d(TAG, "not connected, looking for remote");
        }
    }

    private void remoteDiscovered(BLEDevice device){
        if(timeoutExpired==true&&state!=CONNECTED){
            Log.d(TAG, "remotes found, not connected, showing pairing screen");
            startActivity(new Intent("RECON_BLE_PAIRING_ACTIVITY_SS1").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }
        // pairing activity is visible
        if(activityHandler!=null){
            Log.d(TAG, "notifying activity of remote");
            Message msg = new Message();
            msg.obj = device;
            activityHandler.sendMessage(msg);
        }
    }

    class TimeoutHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "TimeoutHandler");

            timeoutExpired = true;
            // if remotes have been found, show pairing dialog, otherwise wait until we find one
            if(!getRemotes().isEmpty()&&state!=CONNECTED){
                Log.d(TAG, "timeout, remotes found, not connected, showing pairing screen");
                startActivity(new Intent("RECON_BLE_PAIRING_ACTIVITY_SS1").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));
            }
        }
    };

    public void resetTimer(){
        Log.d(TAG, "resetTimer");

        connectionTimeoutHandler.removeMessages(0);
        connectionTimeoutHandler.sendEmptyMessageDelayed(0, CONNECTION_TIMEOUT);
    }


    @Override
    public IBinder onBind (Intent intent) {
        return mBinder;
    }
    private final IBinder mBinder = new MBinder();
    public class MBinder extends Binder {
        public TheBLEService getService() {
            return TheBLEService.this;
        }
    }

    // methods available to bound activity
    public ArrayList<BLEDevice> getRemotes(){
        ArrayList<BLEDevice> remotes = new ArrayList<BLEDevice>();

        Log.d(TAG, "checking if "+devices.size()+" devices are remotes");

        for(BLEDevice device:getDevices()){
            Log.d(TAG, "checking device "+device.name);
            if(device.isRemote())
                remotes.add(device);
        }
        return remotes;
    }

    public ArrayList<BLEDevice> getDevices() {
        return new ArrayList<BLEDevice>(devices.values());
    }

    public boolean matchBleRIB(BluetoothAddress mac){
        if(ribMac==null) return false;

        byte[] macBytes = mac.toByteArray();
        byte[] ribBytes = ribMac.toByteArray();

        return  macBytes[3]==ribBytes[3]&&
            macBytes[4]==ribBytes[4]&&
            macBytes[5]==ribBytes[5];
    }
    public void saveBleRIB(BluetoothAddress address){

        ribMac = address;
        BLEUtils.writeToBLEFile(ribMac.toByteArray());
    }

    void startScan(int duration){
        if(deviceManager!=null){
            int result = deviceManager.startLowEnergyDeviceScan(duration);
            Log.d(TAG,"startLowEnergyDeviceScan result: "+result);
            if(result!=0){
                Log.d(TAG,"Error initiating scan, might already be scanning");
            }
        }
    }
    public void connect(BluetoothAddress address){
        Log.d(TAG,"attempting to connect to "+address);
        saveBleRIB(address);
        if(deviceManager==null) {
            Log.d(TAG, "can't connect, ss1 device manager disabled");
            return;
        }
        int result = deviceManager.connectWithRemoteLowEnergyDevice(address, EnumSet.noneOf(ConnectFlags.class));
        if(result!=0){
            Log.e(TAG,"error trying to connect, error code: "+result);
            if(result==BTPM_ERROR_CODE_DEVICE_IS_CURRENTLY_CONNECTED){
                onConnect();
            }
        } else {
            // reset pairing request timeout
            state = CONNECTING;
        }
    }
    public void disconnect(BluetoothAddress address) {
        Log.d(TAG,"disconnecting with "+address);

        if(deviceManager==null) return;
                
        int result = deviceManager.disconnectRemoteLowEnergyDevice(address, false);
        if(result!=0){
            Log.e(TAG,"error trying to disconnect, error code: "+result);
        }
    }
    public void sendData(BluetoothAddress address, int notifyHandle, byte[] data) {
        Log.d(TAG, "sendData");
        if(deviceManager!=null){
            Log.d(TAG,"sending data");
            int result = genericAttributeClientManager.writeValueWithoutResponse(address, notifyHandle, data);
            Log.d(TAG,"result is "+result);
        }
    }

    public void onConnect(){
        Log.d(TAG, "onConnect");

        state = CONNECTED;
        showConnectedNotification();

        Log.d(TAG,"enabling remote notification");
        sendData(ribMac,REMOTE_NOTIFY_ATT_HANDLE,REMOTE_NOTIFY_DATA);

        sendBroadcast(new Intent("RECON_BLE_REMOTE_CONNECTED")); //tell pairing activity to go away

        inputMan.onConnect();
    }
    public void onDisconnect(){
        Log.d(TAG, "onDisconnect");

        state = DISCONNECTED;
        showDisconnectedNotification();

        onReady();      // start the same procedure as before. TODO: only reconnect or rescan
                
        inputMan.onDisconnect();
    }
    private void showConnectedNotification() {
            
        showNotification(R.drawable.remote_blue);
    }
    private void showDisconnectedNotification() {
        if (DeviceUtils.isSnow2()) {
            showNotification(R.drawable.remote_red);
        }
        else {
            notificationManager.cancel(BLE_NOTIFICATION_ID);
        }
    }

    @SuppressWarnings("deprecation")
    private void showNotification(int drawable) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification(drawable, null, System.currentTimeMillis());
        n.setLatestEventInfo(this, "", "", contentIntent);
        n.flags |= Notification.FLAG_ONGOING_EVENT;
        n.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(BLE_NOTIFICATION_ID, n);
    }

    // Callbacks from SS1 GATT
    public void gattConnected(BluetoothAddress remoteDeviceAddress) {
        if(matchBleRIB(remoteDeviceAddress)){
            onConnect();
        }
    }
    public void gattDisconnected(BluetoothAddress remoteDeviceAddress) {
        if(matchBleRIB(remoteDeviceAddress)){
            onDisconnect();
        }
    }
    public void receivedGattData(BluetoothAddress remoteDeviceAddress,int attributeHandle, byte[] attributeValue) {
        if(!matchBleRIB(remoteDeviceAddress)) return;

        if (attributeHandle==REMOTE_DATA_ATT_HANDLE) {
            // disconnect remote
            if(attributeValue[0]== (RemoteInputManager.RECON_KEY_LEFT|RemoteInputManager.RECON_KEY_RIGHT)) {
                Log.d(TAG,"forgetting remote");
                BLEUtils.clearBLEFile();
                ribMac = null;

                onDisconnect();
            }
            else {
                inputMan.sendKeyEvent(attributeValue[0]);

                // we must be connected to this remote
                if(state==DISCONNECTED){
                    saveBleRIB(remoteDeviceAddress);
                    onConnect();
                }
            }
        }
    }

    // Callbacks from SS1 Bluetooth
    public void discoveryStopped() {
        if(state!=CONNECTED){
            startScan(SCAN_DURATION);
        }
    }

    public void newBleDeviceFound(RemoteDeviceProperties deviceProperties) {

        if(!devices.containsKey(deviceProperties.deviceAddress)){

            BLEDevice device = new BLEDevice(deviceProperties.deviceAddress,deviceProperties.deviceName);
            devices.put(deviceProperties.deviceAddress, device);
                
            if(state==DISCONNECTED&&matchBleRIB(device.address)){
                connect(device.address);
            }
            else if(device.isRemote()){
                remoteDiscovered(device);
            }
            // if the device name is not there, we need to know it to find remotes
            else if(!deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.NAME_KNOWN)){
                Log.d(TAG, "no device name, querying device properties for "+device.address);
                deviceManager.queryRemoteDeviceProperties(device.address, true);
            }
        } else {
            BLEDevice device = devices.get(deviceProperties.deviceAddress);
            Log.d(TAG, "found device that already exists: "+device.address);
        }
    }

    public void devicePropertyChanged(RemoteDeviceProperties deviceProperties) {

        BLEDevice device;
        if(devices.containsKey(deviceProperties.deviceAddress)){
            device = devices.get(deviceProperties.deviceAddress);
            if(!device.name.equals(deviceProperties.deviceName)){
                device.setName(deviceProperties.deviceName);
                if(device.isRemote()){
                    remoteDiscovered(device);
                }
            }
        } else {
            Log.d(TAG, "new device found");
            device = new BLEDevice(deviceProperties.deviceAddress,deviceProperties.deviceName);
            devices.put(deviceProperties.deviceAddress, device);

            if(matchBleRIB(device.address)){
                Log.d(TAG,"Found device matches ble rib address");
                connect(device.address);
            } else {
                if(device.isRemote()){
                    remoteDiscovered(device);
                }
            }
        }
    }

    public void bleConnectionStatusChanged(BluetoothAddress remoteDevice,int status) {

        if(status!=0&&matchBleRIB(remoteDevice)){//error connecting
            Log.d(TAG,"Error connecting to remote, error code:"+status);
            state = DISCONNECTED;
                        
            onDisconnect(); // calls onReady() which calls connect and retries, TODO: clean up this logic
        }
    }
}
