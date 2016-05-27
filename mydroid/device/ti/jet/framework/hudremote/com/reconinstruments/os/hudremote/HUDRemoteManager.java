/*
 * Copyright (C) 2015 Recon Instruments
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reconinstruments.os.hudremote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothGattCallback;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import java.lang.RuntimeException;
/**
 * Class that controls, manages and monitors the BLE remote on the HUD.
 * @hide
 */
public class HUDRemoteManager {
    private static final String TAG = "HUDRemoteManager";
    private static final boolean isModelJet = Build.MODEL.equalsIgnoreCase("JET") ? true : false;
    private static final String REMOTE_SERVICE_NAME = "android.bluetooth.IBluetoothGatt";
    private static final boolean DEBUG = false; // change to true to enable debugging
    private static final int REGISTER_TIMEOUT = 1000;
    private static final int REGISTER_COUNTER = 10;
    private int mRegisterCounter;
    //We only scan for 5 minutes for JET
    private static final long SCAN_PERIOD = 300000;

    public static int IDLE = 0;
    public static int CONNECTING = 1;
    public static int CONNECTED = 2;
    public static int SCANNING = 3;
    public static int DISCONNECTING = 4;
    private int mConnState;
    private final Object mStateLock = new Object();

    private IBluetoothGatt mGatt;
    private final Context mContext;
    private boolean mBound = false;
    private BluetoothGattConnection mConnection;

    private static final int CMD_REGISTER_GATT = 0;
    private static final int CMD_STOP_SCAN = 1;
    private static final int HUD_REMOTE_SCAN = 2;
    private static final int HUD_REMOTE_STATE = 3;
    private static final int HUD_REMOTE_NOTIFY= 4;

    private Handler mHUDRemoteHandler;
    private HUDRemoteListenerTransport mWrapper;

    private List<HUDRemoteListener> mListeners =
        new ArrayList<HUDRemoteListener>();

    /** Success return value. */
    public static final int SUCCESS = 1;

    /** Error return value. */
    public static final int ERROR = 0;
    public static final int TIMEOUT = -1;

    private int mLeHandle;
    //Recon remote address
    private String mRemoteAddress;
    //Recon remote  ATTHandle
    static final int  REMOTE_DATA_ATT_HANDLE = 0x0012;
    static final int  REMOTE_NOTIFY_ATT_HANDLE = 0x0013;
    /**
     * Creates an instance of HUDRemoteManager.
     *
     * @param context The application context.
     */
    public HUDRemoteManager(Context context) {
        mContext = context;
        mGatt = null;
        mWrapper = new HUDRemoteListenerTransport();
        mHUDRemoteHandler = new HUDRemoteHandler();
        mLeHandle = 0;
        mConnection = new BluetoothGattConnection();
    }

    /**
     * Cleanup used to unbind the instance of HUDRemoteManager from the IBluetoothGatt.
     */
    public void cleanup() {
        unregisterGatt();
        if (mBound) {
            mContext.unbindService(mConnection);
            mBound = false;
        }
        mListeners.clear();
        mHUDRemoteHandler = null;
        mConnection = null;
    }

    /**
     * ServiceConnection used to monitor the state of the connection to the IBluetoothGatt.
     */
    private class BluetoothGattConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected: " + className.getClassName());
            mGatt = IBluetoothGatt.Stub.asInterface(service);
            mBound = true;
            mHUDRemoteHandler.sendMessage(mHUDRemoteHandler.obtainMessage(CMD_REGISTER_GATT));
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected: " + className.getClassName());
            mGatt = null;
            mBound = false;
        }
    };

    /**
     * Send conneciton command for a HUD remote.
     *
     * @return {@link #SUCCESS} on success. Otherwise, failure.
     * @throws RuntimeException if HUDRemoteManager cannot start a BLE connect for a HUD remote.
     */
    public int connectRemote(String address) {
        if (!isConnectedService() || mLeHandle <= 0) return ERROR;
        return connectRemote(mLeHandle, address);
    }

    /**
     * Delete BLE File.
     */
    public void clearBLEFile() {
        synchronized(mStateLock) {
            mRemoteAddress = null;
            HUDRemoteUtils.clearBLEFile();
            mConnState = DISCONNECTING;//Allow to scan again
        }
    }

    /**
     * Register a HUD remote listener to receive events from IBluetoothGatt.
     *
     * @param listener An instance of {@link com.reconinstruments.os.hudremote.HUDRemoteListener}.
     * @return {@link #SUCCESS} on successful registration. Otherwise failure"
     */
    public int registerHUDRemoteListener(HUDRemoteListener listener) {
        int result = ERROR;
        if (listener != null) {
            synchronized(mListeners) {
                if (mListeners.contains(listener)) {
                    Log.w(TAG, "Already registered listener: " + listener);
                } else {
                    mListeners.add(listener);
                    mRegisterCounter = 0;
                    Message message = mHUDRemoteHandler.obtainMessage(CMD_REGISTER_GATT);

                    if (isConnectedService()) {
                        Log.v(TAG, "REGISTER_GATT)");
                        mHUDRemoteHandler.sendMessage(message);
                    } else {
                        Log.v(TAG, "bindservice");
                        // Bind to the service
                        if (!mContext.bindService(new Intent(REMOTE_SERVICE_NAME), mConnection, Context.BIND_AUTO_CREATE)) {
                            Log.e(TAG, "Failed to bind to: " + REMOTE_SERVICE_NAME);
                            mListeners.remove(listener);
                            mHUDRemoteHandler = null;
                            return ERROR;
                        }
                    }
                    result = SUCCESS;
                }
            }
        }
        return result;
    }

    /**
     * Unregister a HUD remote listener from HUDRemoteService.
     *
     * @param listener An instance of {@link com.reconinstruments.os.hudremote.HUDRemoteListener}
     * that was previously used to register for HUD remote events.
     * @return {@link SUCCESS} if registration successful; ERROR otherwise.
     */
    public int unregisterHUDRemoteListener(HUDRemoteListener listener) {
        int rc = ERROR;
        if (listener != null && mBound) {
            synchronized (mListeners) {
                if (!mListeners.contains(listener)) {
                    Log.e(TAG, "Not registered: " + listener);
                } else {
                    if (mListeners.remove(listener)) {
                        if (mListeners.isEmpty())
                            rc = unregisterGatt();
                    } else
                        Log.e(TAG, "Fail to remove " + listener);
                }
            }
        }
        return rc;
    }

    /**
     * Helper function to check whether the HUDRemoteManager has connectted the service.
     */
    private boolean isConnectedService() {
        return (mGatt != null && mBound);
    }

    private int connectRemote(int clientIf, String address) {
        int result = SUCCESS;
        //Cancel scan if possible
        synchronized(mStateLock) {
            Log.d(TAG, "connect " + address + ", state=" + mConnState);
            if (mConnState == SCANNING) {
                if (isModelJet) {
                    mHUDRemoteHandler.removeMessages(CMD_STOP_SCAN);
                }
                try {
                    mGatt.stopScan(clientIf, false);
                } catch (RemoteException e) {
                    Log.e(TAG,"",e);
                    result = ERROR;
                }
            }

            mConnState = CONNECTING;
            //Update BLE file
            if (address == null) {
                mRemoteAddress = null;
                HUDRemoteUtils.clearBLEFile();
                Log.w(TAG, "null address detected!");
                mConnState = IDLE;
                return result;
            }

            if (mRemoteAddress == null ||
                !mRemoteAddress.equals(address)) {
                    mRemoteAddress = address;

                    byte[] byte_address = HUDRemoteUtils.getBytesFromAddress(address);
                    HUDRemoteUtils.writeToBLEFile(byte_address);
            }

        }
        //Connect to the remote
        try {
            mGatt.clientConnect(clientIf, address,false);
        } catch (RemoteException e) {
            Log.e(TAG,"",e);
            result = ERROR;
            mConnState = IDLE;
        }
        return result;
    }

    private int startRemoteScan(int clientIf) {
        try {
            mGatt.startScan(clientIf, false);
            synchronized(mStateLock) {
                Log.d(TAG, "startRemoteScan, state=" + mConnState);
                mConnState = SCANNING;
            }
            if (isModelJet) {//Jet will only scan for SCAN_PERIOD
                mHUDRemoteHandler.sendMessageDelayed(
                    mHUDRemoteHandler.obtainMessage(CMD_STOP_SCAN), SCAN_PERIOD);
            }
           return SUCCESS;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to start scan: " + e);
            return ERROR;
        }
    }

    private int enableRemoteNotification(int clientIf, String address) {
        try {
            mGatt.writeAttributeValue(clientIf, address,
                    REMOTE_NOTIFY_ATT_HANDLE, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, false);
            return SUCCESS;
        } catch (RemoteException e) {
            Log.e(TAG,"enableRemoteNotification Failed " + e);
            return ERROR;
        }
    }

    private int unregisterGatt() {
        int result = SUCCESS;
        synchronized(mStateLock) {
            if (mLeHandle>0) {
                try {
                    if (DEBUG) Log.d(TAG, "unregisterGatt");
                    mGatt.unregisterClient(mLeHandle);
                } catch (RemoteException e) {
                    result = ERROR;
                    throw new RuntimeException("unregisterGatt fail: " + e);
                }
                finally {
                    mLeHandle = 0;
                    mConnState = IDLE;
                }
            }
        }
        return result;
    }
    /**
     * Send to clients
     */
    private void sendHUDRemoteRegisterState(int state) {
        synchronized (mListeners) {
            for (HUDRemoteListener listener : mListeners) {
                if (DEBUG) Log.d(TAG, "sendHUDRemoteRegisterState");
                listener.onHUDRemoteRegistered(state);
            }
        }
    }

    private void sendHUDRemoteConnectionState(int state) {
        synchronized (mListeners) {
            for (HUDRemoteListener listener : mListeners) {
                if (DEBUG) Log.d(TAG, "sendHUDRemoteConnectionState");
                listener.onHUDRemoteConnectionChange(state);
            }
        }
    }

    private void sendHUDRemoteScan (String address) {
        for (HUDRemoteListener listener : mListeners) {
            if (DEBUG) Log.d(TAG, "sendHUDRemoteScan");
            listener.onHUDRemoteScan(address);
        }
    }

    private void sendHUDRemoteNotify(int value) {
        for (HUDRemoteListener listener : mListeners) {
            if (DEBUG) Log.d(TAG, "sendHUDRemoteNotify");
            listener.onHUDRemoteNotify(value);
        }
    }
    /**
     * HUDRemoteHandler
     */
    class HUDRemoteHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch ((msg.what)) {
                case CMD_REGISTER_GATT:
                    registerGatt();
                break;
                case CMD_STOP_SCAN:
                    stopScan();
                break;
                case HUD_REMOTE_SCAN:
                    sendHUDRemoteScan((String)msg.obj);
                break;
                case HUD_REMOTE_STATE:
                    sendHUDRemoteConnectionState(msg.arg1);
                break;
                case HUD_REMOTE_NOTIFY:
                    sendHUDRemoteNotify(msg.arg1);
                break;
                default:
                break;
            }
        }

        private void registerGatt() {
            if (DEBUG) Log.d(TAG,"mRegisterCounter=" + mRegisterCounter);
            if (isConnectedService()) {
                if (mLeHandle <= 0) {
                    try {
                        if (mGatt.registerReconRemote(mWrapper) == true ) {
                            Log.d(TAG, "registerGatt");
                            return;
                        }
                    } catch (RemoteException e) {
                        throw new RuntimeException("registerGatt fail: " + e);
                    }
                } else
                    Log.e(TAG, "Only allow one client to register");
            } else {
                Log.e(TAG, "No Serivce found");
            }

            if (mRegisterCounter < REGISTER_COUNTER) {
                mRegisterCounter++;
                mHUDRemoteHandler.sendMessageDelayed(mHUDRemoteHandler.obtainMessage(CMD_REGISTER_GATT), REGISTER_TIMEOUT);
            } else
                sendHUDRemoteRegisterState(TIMEOUT);
        }

        private void stopScan() {
            try {
                mGatt.stopScan(mLeHandle, false);
                synchronized(mStateLock) {
                    Log.d(TAG, "stopRemoteScan, state=" + mConnState);
                    if (mConnState == SCANNING)
                        mConnState = IDLE;//leave it to idle state if nothing happen
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to stop scan: " + e);
            }
        }
    };

    /**
     * Helper class to receive events from IBluetoothGattCallback and route them to the listeners
     */
    private class HUDRemoteListenerTransport extends IBluetoothGattCallback.Stub  {

        public void onClientRegistered(int status, int clientIf) {
            if (DEBUG) Log.d(TAG, "onClientRegistered() - status: " + status + " clientIf: " + clientIf);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mLeHandle = clientIf;

                //Attempt to read BLE RIB File
                synchronized(mStateLock) {
                    byte[] address = HUDRemoteUtils.readBLEFile();
                    if (address != null) {
                        mRemoteAddress = HUDRemoteUtils.getAddressStringFromByte(address);
                        if (DEBUG) Log.d(TAG, mRemoteAddress +" in BLE RIB");
                        if (connectRemote(clientIf, mRemoteAddress) == ERROR) {
                            Log.e(TAG, "connect fail");
                            mLeHandle = -1;
                        }
                    }
                    else {
                        mRemoteAddress = null;
                        Log.d(TAG,"no mac address in BLE RIB");
                        if (startRemoteScan(clientIf) == ERROR) {
                            Log.e(TAG, "scan fail");
                            mLeHandle = -1;
                        }
                    }
                }

                if (mLeHandle == -1) {
                    // registration succeeded but connect or start scan failed
                    if (mGatt != null) {
                        try {
                            mGatt.unregisterClient(mLeHandle);
                        } catch (RemoteException e) {
                            Log.e(TAG, "fail to unregister callback: " + mLeHandle +
                                  " error: " + e);
                        }
                    }
                }
            } else {
                mLeHandle = -1;
                Log.e(TAG, "Registration failed: " + status);
            }
            if (mLeHandle <= 0)
                sendHUDRemoteRegisterState(ERROR);
            else
                sendHUDRemoteRegisterState(SUCCESS);
        }

        public void onClientConnectionState(int status, int clientIf, boolean connected, String address) {
            //the failure is due to the recon remote is inacitve, then neeed to scan the channel
            if (status == BluetoothGatt.GATT_FAILURE) {
                startRemoteScan(clientIf);
            } else {
                int state = connected ? 1:0;
                mHUDRemoteHandler.sendMessage(
                    mHUDRemoteHandler.obtainMessage(HUD_REMOTE_STATE,state,0));

                synchronized(mStateLock) {
                    if (DEBUG) Log.d(TAG, "connection=" + connected +" prestate="+ mConnState);
                    if (connected) {
                        mConnState = CONNECTED;
                        if (enableRemoteNotification(clientIf, address) == ERROR) {
                            Log.e(TAG, "cannot enable  Notification");
                        }
                    }
                    else {
                        if ( mConnState == DISCONNECTING) {
                            startRemoteScan(clientIf);
                        } else {
                            connectRemote(clientIf, address);
                        }
                    }
                }
            }
        }

        public void onScanResult(String address, int rssi, byte[] advData) {
            if (DEBUG) Log.d(TAG, "onScanResult() - device: " + address + " RSSI: " + rssi);
            synchronized(mStateLock) {
                if (mLeHandle <= 0) return;
                if (mRemoteAddress != null) {
                    if (mRemoteAddress.equals(address)) {
                        connectRemote(mLeHandle, address);
                        return;
                    }
                }
            }

            mHUDRemoteHandler.sendMessage(
                mHUDRemoteHandler.obtainMessage(HUD_REMOTE_SCAN,address));
        }

        public void onGetService(String address, int srvcInstId, ParcelUuid srvcUuid,
                int startHandle, int endHandle) {
            // no op
        }

        public void onGetIncludedService(String address, int inclSrvcInstId,
                ParcelUuid inclSrvcUuid, int startHandle, int endHandle) {
            // no op
        }

        public void onGetCharacteristic(String address, int attHandle, ParcelUuid charUuid,
                int charProps) {
            // no op
        }

        public void onGetDescriptor(String address, int attHandle, ParcelUuid descrUuid) {
            // no op
        }

        public void onSearchComplete(String address, int status) {
            // no op
        }

        public void onNotify(String address, int attHandle, byte[] values)  {
            if (attHandle == REMOTE_DATA_ATT_HANDLE) {
                mHUDRemoteHandler.sendMessage(
                mHUDRemoteHandler.obtainMessage(HUD_REMOTE_NOTIFY,(int)values[0],0));
            }
        }

        public void onWriteResponse(String address, int attHandle,int status) {
            // no op
        }

        public void onReadResponse(String address, int attHandle,
                                   int status, byte[] values) {
            // no op
        }

        public void onReadRemoteRssi(String address, int rssi, int status) {
            // no op
        }
    }
}

