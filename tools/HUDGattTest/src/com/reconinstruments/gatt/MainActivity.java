
package com.reconinstruments.gatt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String TAG = "GattTestActivity";
    IBluetoothGatt mService = null;
    private boolean mIsBound;
    private int mLeScanHandle;
    private final UUID[] mScanFilter = null;
    /*
    private final UUID[] mScanFilter = {
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    };
    */
    private static final boolean DBG = true;

    /**
     * GATT communication
     */
    private int mClientIf;// Follow BluetoothGatt name
    private String mBleAdrress = "90:D7:EB:B2:66:DE";
    //private String mBleAdrress = "CE:99:02:B2:FC:FF";
    private BluetoothDevice mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
            mBleAdrress);
    private List<BluetoothGattService> mServices = new ArrayList<BluetoothGattService>();
    private final UUID CLIENT_CONFIGURATION_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final UUID HEART_RATE_MEASUREMENT_UUID = UUID
            .fromString("00002a37-0000-1000-8000-00805f9b34fb");

    private int mConnState;
    private final Object mStateLock = new Object();
    private static final int CONN_STATE_IDLE = 0;
    private static final int CONN_STATE_CONNECTING = 1;
    private static final int CONN_STATE_ENCRYPTING = 5;
    private static final int CONN_STATE_CONNECTED = 2;
    private static final int CONN_STATE_DISCONNECTING = 3;
    private static final int CONN_STATE_CLOSED = 4;

    BluetoothGattService getServiceFromList(BluetoothDevice device, int starthandle,
            int endhandle, List<BluetoothGattService> services) {
        for (BluetoothGattService svc : services) {
            if (svc.getDevice().equals(device) &&
                    svc.getStartHandle() <= starthandle &&
                    svc.getEndHandle() >= endhandle) {
                return svc;
            }
        }
        return null;
    }

    BluetoothGattService getServiceFromList(BluetoothDevice device, UUID uuid,
            int instanceId, int starthandle, int endhandle,
            List<BluetoothGattService> services) {
        if (services.isEmpty())
            return null;
        for (BluetoothGattService svc : services) {
            if (svc.getDevice().equals(device) &&
                    svc.getInstanceId() == instanceId &&
                    svc.getUuid().equals(uuid) &&
                    svc.getStartHandle() == starthandle &&
                    svc.getEndHandle() == endhandle) {
                return svc;
            }
        }
        return null;
    }

    /**
     * Mainly used to get BluetoothGattService based on the ATThandle ragnes
     * 
     * @param device
     * @param starthandle
     * @param endhandle
     * @return
     */
    BluetoothGattService getGAttService(BluetoothDevice device, int starthandle,
            int endhandle) {
        return getServiceFromList(device, starthandle,
                endhandle, mServices);
    }

    /**
     * Mainly used to get BluetoothGattService to check if it's the same service
     * 
     * @param device
     * @param uuid
     * @param instanceId
     * @param starthandle
     * @param endhandle
     * @return
     */
    BluetoothGattService getGAttService(BluetoothDevice device, UUID uuid,
            int instanceId, int starthandle, int endhandle) {
        return getServiceFromList(device, uuid, instanceId,
                starthandle, endhandle, mServices);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        Button button1 = (Button) findViewById(R.id.Bind);
        button1.setOnClickListener(mBindListener);

        button1 = (Button) findViewById(R.id.Unbind);
        button1.setOnClickListener(mUnbindListener);

        button1 = (Button) findViewById(R.id.startScan);
        button1.setOnClickListener(mStartScanListener);
        button1 = (Button) findViewById(R.id.StopScan);
        button1.setOnClickListener(mStopScanListener);

        button1 = (Button) findViewById(R.id.Gatconnect);
        button1.setOnClickListener(mGatconnectListener);

        button1 = (Button) findViewById(R.id.Gatdisconnect);
        button1.setOnClickListener(mGatdisconnecttListener);

        button1 = (Button) findViewById(R.id.EnableNotify);
        button1.setOnClickListener(mGatListener);

        button1 = (Button) findViewById(R.id.DisableNotify);
        button1.setOnClickListener(mGatListener);

        button1 = (Button) findViewById(R.id.Write);
        button1.setOnClickListener(mGatListener);

        button1 = (Button) findViewById(R.id.Read);
        button1.setOnClickListener(mGatListener);
    }

    private IBluetoothGattCallback mScanCallback = new IBluetoothGattCallback.Stub() {
        public void onClientRegistered(int status, int clientIf) {
            Log.d(TAG, "onClientRegistered() - status=" + status +
                    " clientIf=" + clientIf);
            if (status == 0) {
                mLeScanHandle = clientIf;
                try {
                    if (mScanFilter == null) {
                        mService.startScan(mLeScanHandle, false);
                    } else {
                        ParcelUuid[] uuids = new ParcelUuid[mScanFilter.length];
                        for (int i = 0; i != uuids.length; ++i) {
                            uuids[i] = new ParcelUuid(mScanFilter[i]);
                        }
                        mService.startScanWithUuids(mLeScanHandle, false, uuids);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "fail to start le scan: " + e);
                    mLeScanHandle = -1;
                }
                if (mLeScanHandle == -1) {
                    // registration succeeded but start scan failed
                    if (mService != null) {
                        try {
                            mService.unregisterClient(mLeScanHandle);
                        } catch (RemoteException e) {
                            Log.e(TAG, "fail to unregister callback: " + mLeScanHandle +
                                    " error: " + e);
                        }
                    }
                }
            } else {
                // registration failed
                mLeScanHandle = -1;
            }

        }

        @Override
        public void onScanResult(String address, int rssi, byte[] advData) throws RemoteException {
            // TODO Auto-generated method stub
            Log.d(TAG, "onScanResult() - Device=" + address + " RSSI=" + rssi);
            StringBuilder sb = new StringBuilder(advData.length * 2);
            for (byte b : advData)
                sb.append(String.format("%02X", b & 0xFF));
            Log.d(TAG, "ad data = " + sb.toString());
        }

        @Override
        public void onClientConnectionState(int status, int clientIf, boolean connected,
                String address) throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void onGetService(String address, int srvcInstId, ParcelUuid srvcUuid,
                int startHandle, int endHandle) throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void onGetIncludedService(String address, int inclSrvcInstId,
                ParcelUuid inclSrvcUuid, int startHandle, int endHandle) throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void onGetCharacteristic(String address, int AttHandle, ParcelUuid charUuid,
                int charProps) throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void onGetDescriptor(String address, int AttHandle, ParcelUuid descrUuid)
                throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSearchComplete(String address, int status) throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void onNotify(String address, int AttHandle, byte[] values) throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void onWriteResponse(String address, int AttHandle, int status)
                throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void onReadResponse(String address, int AttHandle, int status, byte[] values)
                throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void onReadRemoteRssi(String address, int rssi, int status) throws RemoteException {
            // TODO Auto-generated method stub

        }
    };

    private OnClickListener mBindListener = new OnClickListener() {
        public void onClick(View v) {
            // Establish a couple connections with the service, binding
            // by interface names. This allows other applications to be
            // installed that replace the remote service by implementing
            // the same interface.
            bindService(new Intent(IBluetoothGatt.class.getName()),
                    mConnection, Context.BIND_AUTO_CREATE);
            mIsBound = true;
            Log.d(TAG, "Binding " + IBluetoothGatt.class.getName());
        }
    };

    private OnClickListener mUnbindListener = new OnClickListener() {
        public void onClick(View v) {
            if (mIsBound) {
                // If we have received the service, and hence registered with
                // it, then now is the time to unregister.
                if (mService != null) {
                    try {
                        mService.unregisterClient(mLeScanHandle);
                    } catch (RemoteException e) {
                        // There is nothing special we need to do if the service
                        // has crashed.
                    }
                }

                // Detach our existing connection.
                unbindService(mConnection);
                mIsBound = false;
            }
        }
    };

    private OnClickListener mStartScanListener = new OnClickListener() {
        public void onClick(View v) {
            if (mIsBound) {
                // If we have received the service, and hence registered with
                // it, then now is the time to unregister.
                if (mService != null) {
                    try {
                        UUID uuid = UUID.randomUUID();
                        mService.registerClient(new ParcelUuid(uuid), mScanCallback);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Cannot get service");
                    }
                }
            }
        }
    };

    private OnClickListener mStopScanListener = new OnClickListener() {
        public void onClick(View v) {
            if (mIsBound) {
                // If we have received the service, and hence registered with
                // it, then now is the time to unregister.
                if (mService != null) {
                    try {
                        mService.stopScan(mLeScanHandle, false);
                        mService.unregisterClient(mLeScanHandle);
                    } catch (RemoteException e) {
                        // There is nothing special we need to do if the service
                        // has crashed.
                    }
                    mLeScanHandle = -1;
                }
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Conect service");
            mService = IBluetoothGatt.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.d(TAG, "Disconect service");

        }

    };

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    /**
     * Gat connect investigation
     */
    private OnClickListener mGatconnectListener = new OnClickListener() {
        public void onClick(View v) {
            if (mIsBound) {
                if (mService == null)
                    return;
                try {
                    synchronized (mStateLock) {
                        mConnState = CONN_STATE_IDLE;
                    }
                    UUID uuid = UUID.randomUUID();
                    mService.registerClient(new ParcelUuid(uuid), mGATTCallback);

                } catch (RemoteException e) {
                    Log.e(TAG, "Cannot get service");
                }
            }
        }
    };

    private OnClickListener mGatdisconnecttListener = new OnClickListener() {
        public void onClick(View v) {
            if (mIsBound) {
                if (mService == null || mClientIf <= 0)
                    return;

                try {
                    mService.clientDisconnect(mClientIf, mDevice.getAddress());
                    mService.unregisterClient(mClientIf);
                    mClientIf = 0;
                } catch (RemoteException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    };

    private OnClickListener mGatListener = new OnClickListener() {

        public void onClick(View v) {
            int state = 0;
            switch (v.getId()) {
                case R.id.EnableNotify:
                    state = 1;
                    break;
                case R.id.DisableNotify:
                    state = 0;
                    break;
                case R.id.Write:
                    state = 2;
                    break;
                case R.id.Read:
                    state = 3;
                    break;
            }
            for (BluetoothGattService service : mServices) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    if (characteristic.getUuid().equals(HEART_RATE_MEASUREMENT_UUID)) {
                        Log.d(TAG, "Find HEART_RATE_MEASUREMENT_UUID");
                        switch (state) {
                            case 0:
                                setCharacteristicNotification(characteristic, false);
                                break;
                            case 1:
                                setCharacteristicNotification(characteristic, true);
                                break;
                            case 2:
                                try {
                                    int handle = 19;// 18, 23 for write
                                    byte[] values = {
                                            0
                                    };
                                    mService.writeAttValue(mClientIf, mDevice.getAddress(),
                                            handle, values, true);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "", e);
                                }
                                break;
                            case 3:
                                try {
                                    int handle = 9;// 19 for drscriptors //28
                                                   // for read
                                    mService.readAttValue(mClientIf, mDevice.getAddress(),
                                            handle);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "", e);
                                }
                                break;
                        }
                        return;
                    }
                }
            }
        }

    };

    private IBluetoothGattCallback mGATTCallback = new IBluetoothGattCallback.Stub() {
        @Override
        public void onClientRegistered(int status, int clientIf) {
            Log.d(TAG, "onClientRegistered() - status=" + status +
                    " clientIf=" + clientIf);
            if (status == 0) {
                mClientIf = clientIf;
                try {
                    synchronized (mStateLock) {
                        if (mConnState != CONN_STATE_IDLE) {
                            throw new IllegalStateException("Not idle");
                        }
                        mConnState = CONN_STATE_CONNECTING;
                    }
                    mService.clientConnect(mClientIf, mBleAdrress, false);
                } catch (RemoteException e) {
                    Log.e(TAG, "fail to start Gatt: " + e);
                    mClientIf = -1;
                }

            } else {
                // registration failed
                mClientIf = -1;
            }

        }

        @Override
        public void onScanResult(String address, int rssi, byte[] advData) {
            // TODO Auto-generated method stub
            Log.d(TAG, "onScanResult?");
        }

        @Override
        public void onClientConnectionState(int status, int clientIf, boolean connected,
                String address) throws RemoteException {
            Log.d(TAG, "Gatt connection: " + connected + "; status=" + status
                    + " clientIf=" + clientIf + " device=" + address);
            // Deal with mConnState
            boolean report = true;
            synchronized (mStateLock) {
                if (DBG)
                    Log.d(TAG, " mConnState=" + mConnState);
                switch (mConnState) {
                    case CONN_STATE_CONNECTING:
                        if (connected) {
                            if (status == 0x100) {
                                mConnState = CONN_STATE_ENCRYPTING;
                                report = false;
                                try {
                                    mService.encryptRemoteDevice(clientIf, address);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "", e);
                                }
                            } else {
                                mConnState = CONN_STATE_CONNECTED;
                            }
                        }// else still stay in CONNECTING state
                        break;
                    case CONN_STATE_ENCRYPTING:
                        if (connected) {
                            mConnState = CONN_STATE_CONNECTED;
                        } else {
                            report = false;
                            mConnState = CONN_STATE_DISCONNECTING;
                            mService.clientConnect(clientIf, address, false);
                        }
                        break;
                    default:
                        if (connected) {
                            mConnState = CONN_STATE_CONNECTED;
                        } else {
                            mConnState = CONN_STATE_IDLE;
                        }
                        break;
                }
                if (DBG)
                    Log.d(TAG, " change mConnState=" + mConnState);
            }

            if (connected == true && status != 0x101 && report == true) {
                mServices.clear();
                try {
                    mService.discoverServices(clientIf, address);
                } catch (RemoteException e) {
                    Log.e(TAG, "", e);
                }
            }
        }

        @Override
        public void onGetService(String address, int srvcInstId, ParcelUuid srvcUuid,
                int startHandle, int endHandle) throws RemoteException {
            if (DBG)
                Log.d(TAG, "onGetService() - Device=" + address + " UUID=" + srvcUuid);
            if (!address.equals(mDevice.getAddress())) {
                return;
            }

            /*
             * Services cleared already BluetoothGattService service =
             * getService(mDevice, srvcUuid.getUuid(), srvcInstId,startHandle,
             * endHandle); if(service != null) { Log.e(TAG,
             * "Service already added!"); return; }
             */
            BluetoothGattService service = new BluetoothGattService(mDevice, srvcUuid.getUuid(),
                    srvcInstId, BluetoothGattService.SERVICE_TYPE_PRIMARY, startHandle, endHandle);
            // The main service is always primary service from SS1 stack

            mServices.add(service);
        }

        @Override
        public void onGetIncludedService(String address, int inclSrvcInstId,
                ParcelUuid inclSrvcUuid, int startHandle, int endHandle) {
            if (DBG)
                Log.d(TAG, "onGetIncludedService() - Device=" + address
                        + " Included=" + inclSrvcUuid);

            if (!address.equals(mDevice.getAddress())) {
                return;
            }

            BluetoothGattService service = getGAttService(mDevice,
                    startHandle, endHandle);
            if (service == null) {
                Log.e(TAG, "Main Service not found!");
                return;
            }
            /*
             * Services cleared already List<BluetoothGattService>
             * IncludedServices = service.getIncludedServices();
             * BluetoothGattService includedService =
             * getServiceFromList(mDevice, inclSrvcUuid.getUuid(),
             * inclSrvcInstId,startHandle, endHandle, IncludedServices);
             * if(includedService != null ) { Log.e(TAG,
             * "Included Service already added!"); return; }
             */
            // The included service is always secondary service from SS1 stack
            BluetoothGattService includedService = new BluetoothGattService(mDevice,
                    inclSrvcUuid.getUuid(),
                    inclSrvcInstId, BluetoothGattService.SERVICE_TYPE_SECONDARY, startHandle,
                    endHandle);
            service.addIncludedService(includedService);
        }

        @Override
        public void onGetCharacteristic(String address, int AttHandle, ParcelUuid charUuid,
                int charProps) {
            if (DBG)
                Log.d(TAG, "onGetCharacteristic() - Device=" + address + " UUID=" +
                        charUuid + " Handle=" + AttHandle);

            if (!address.equals(mDevice.getAddress()))
                return;

            BluetoothGattService service = getGAttService(mDevice, AttHandle, AttHandle);
            if (service == null) {
                Log.e(TAG, "Main Service for Characteristic not found!");
                return;
            }

            UUID uuid = charUuid.getUuid();
            /*
             * Services cleared already BluetoothGattCharacteristic
             * characteristic = service.getCharacteristic(uuid, AttHandle);
             * if(characteristic != null ) { Log.e(TAG,
             * "Characteristic already added!"); return; }
             */
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                    service, uuid, AttHandle, charProps, 0);
            service.addCharacteristic(characteristic);
        }

        @Override
        public void onGetDescriptor(String address, int AttHandle, ParcelUuid descUuid) {
            if (DBG)
                Log.d(TAG, "onGetDescriptor() - Device=" + address + " UUID=" + descUuid
                        + " Handle=" + AttHandle);

            if (!address.equals(mDevice.getAddress())) {
                return;
            }
            BluetoothGattService service = getGAttService(mDevice, AttHandle, AttHandle);
            if (service == null)
                return;

            BluetoothGattCharacteristic characteristic = service.
                    getCharacteristicFromDescHandle(AttHandle);
            if (characteristic == null) {
                Log.e(TAG, "Characteristic for the Descriptor not found!");
                return;
            }

            UUID uuid = descUuid.getUuid();
            /*
             * Services cleared already BluetoothGattDescriptor descriptor =
             * characteristic.getDescriptor(uuid); if( descriptor != null ) {
             * Log.e(TAG, "Descriptor already added!"); return; }
             */
            BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                    characteristic, uuid, AttHandle, 0);
            characteristic.addDescriptor(descriptor);
        }

        @Override
        public void onSearchComplete(String address, int status) throws RemoteException {
            Log.d(TAG, "onSearchComplete");
            // TODO: onServicesDiscovered
        }

        @Override
        public void onNotify(String address, int AttHandle, byte[] values) {
            if (DBG)
                Log.d(TAG, "onNotify() - Device=" + address + " handle=" + AttHandle);

            if (!address.equals(mDevice.getAddress())) {
                return;
            }

            BluetoothGattService service = getGAttService(mDevice, AttHandle, AttHandle);
            if (service == null) {
                Log.e(TAG, "Main Service for Characteristic not found!");
                return;
            }

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(AttHandle);
            if (characteristic == null) {
                Log.e(TAG, "Characteristic not found!");
                return;
            }
            characteristic.setValue(values);
            // TODO: onCharacteristicChanged
            if (DBG) {
                StringBuilder sb = new StringBuilder(values.length * 2);
                for (byte b : values)
                    sb.append(String.format("%02X", b & 0xFF));
                Log.d(TAG, "att values = " + sb.toString());
            }
        }

        @Override
        public void onWriteResponse(String address, int AttHandle, int status) {
            if (DBG)
                Log.d(TAG, "onWriteResponse() - Device=" + address + " handle=" + AttHandle);

            if (!address.equals(mDevice.getAddress())) {
                return;
            }

            BluetoothGattService service = getGAttService(mDevice, AttHandle, AttHandle);
            if (service == null) {
                Log.e(TAG, "Main Service for Characteristic not found!");
                return;
            }

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(AttHandle);
            if (characteristic == null) {// Check if it's discriptors
                characteristic = service.
                        getCharacteristicFromDescHandle(AttHandle);
                if (characteristic == null) {
                    Log.e(TAG, "Characteristic for the Descriptor not found!");
                    return;
                }
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(AttHandle);
                if (descriptor == null)
                    return;
                Log.d(TAG, "onDescriptorWrite status=" + status);
                // TODO:
                // mCallback.onDescriptorWrite(BluetoothGatt.this, descriptor,
                // status);
            }
            else {// Characteristic found
                Log.d(TAG, "onCharacteristicWrite status=" + status);
                // TODO:
                // mCallback.onCharacteristicWrite(BluetoothGatt.this,
                // characteristic, status);;
            }
        }

        @Override
        public void onReadResponse(String address, int AttHandle, int status, byte[] values) {
            if (DBG) {
                Log.d(TAG, "onReadResponse() - Device=" + address + " handle=" + AttHandle);
                if (status == 0) {
                    StringBuilder sb = new StringBuilder(values.length * 2);
                    for (byte b : values)
                        sb.append(String.format("%02X", b & 0xFF));
                    Log.d(TAG, "values = " + sb.toString());
                }
            }

            if (!address.equals(mDevice.getAddress())) {
                return;
            }

            BluetoothGattService service = getGAttService(mDevice, AttHandle, AttHandle);
            if (service == null) {
                Log.e(TAG, "Main Service for Characteristic not found!");
                return;
            }

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(AttHandle);
            if (characteristic == null) {// Check if it's discriptors
                characteristic = service.
                        getCharacteristicFromDescHandle(AttHandle);
                if (characteristic == null) {
                    Log.e(TAG, "Characteristic for the Descriptor not found!");
                    return;
                }
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(AttHandle);
                if (descriptor == null)
                    return;
                if (DBG)
                    Log.d(TAG, "onDescriptorRead status=" + status);
                if (status == 0)
                    descriptor.setValue(values);

                // TODO:
                // mCallback.onDescriptorRead(BluetoothGatt.this, descriptor,
                // status);
            }
            else {// Characteristic found
                if (DBG)
                    Log.d(TAG, "onCharacteristicRead status=" + status);
                if (status == 0)
                    characteristic.setValue(values);
                // TODO:
                // mCallback.onCharacteristicWrite(BluetoothGatt.this,
                // characteristic, status);;
            }

        }

        @Override
        public void onReadRemoteRssi(String address, int rssi, int status) throws RemoteException {
            // TODO Auto-generated method stub

        }
    };

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
                && (characteristic.getProperties() &
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0)
            return false;
        if (DBG)
            Log.d(TAG, "writeCharacteristic() - uuid: " + characteristic.getUuid());
        if (mService == null || mClientIf == 0)
            return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null)
            return false;

        BluetoothDevice device = service.getDevice();
        if (device == null)
            return false;

        boolean response;
        if (characteristic.getWriteType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            response = false;
        } else {
            response = true;
        }

        try {
            mService.writeAttValue(mClientIf, device.getAddress(),
                    characteristic.getInstanceId(), characteristic.getValue(), response);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
        return true;
    }

    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        if (DBG)
            Log.d(TAG, "writeDescriptor() - uuid: " + descriptor.getUuid());
        if (mService == null || mClientIf == 0)
            return false;

        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (characteristic == null)
            return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null)
            return false;

        BluetoothDevice device = service.getDevice();
        if (device == null)
            return false;

        boolean response;
        if (characteristic.getWriteType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            response = false;
        } else {
            response = true;
        }

        try {
            mService.writeAttValue(mClientIf, device.getAddress(),
                    descriptor.getHandle(), descriptor.getValue(), response);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
        return true;
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
            boolean enable) {
        if (DBG)
            Log.d(TAG, "setCharacteristicNotification() - uuid: " + characteristic.getUuid()
                    + " enable: " + enable);
        if (mService == null || mClientIf == 0)
            return false;

        boolean isNotification;
        int property = characteristic.getProperties();

        if ((property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            isNotification = true;
        } else if ((property & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            isNotification = false;
        }
        else {
            Log.e(TAG, "characteristic don't support notifications/indications");
            return false;
        }

        BluetoothGattService service = characteristic.getService();
        if (service == null)
            return false;

        BluetoothDevice device = service.getDevice();
        if (device == null)
            return false;

        boolean response;
        if (characteristic.getWriteType() == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            response = false;
        } else {
            response = true;
        }

        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        for (BluetoothGattDescriptor descriptor : descriptors) {
            if (descriptor.getUuid().equals(CLIENT_CONFIGURATION_UUID)) {
                if (enable) {
                    if (isNotification) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    } else {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    }
                } else {
                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }

                try {
                    mService.writeAttValue(mClientIf, device.getAddress(),
                            descriptor.getHandle(), descriptor.getValue(), response);
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, "", e);
                    return false;
                }
            }
        }

        Log.e(TAG, "Didn't find CLIENT_CONFIGURATION_UUID!");
        return false;
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0)
            return false;
        if (DBG)
            Log.d(TAG, "readCharacteristic() - uuid: " + characteristic.getUuid());
        if (mService == null || mClientIf == 0)
            return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null)
            return false;

        BluetoothDevice device = service.getDevice();
        if (device == null)
            return false;

        try {
            mService.readAttValue(mClientIf, device.getAddress(), characteristic.getInstanceId());
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }

        return true;
    }

    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        if (DBG)
            Log.d(TAG, "readDescriptor() - uuid: " + descriptor.getUuid());
        if (mService == null || mClientIf == 0)
            return false;

        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (characteristic == null)
            return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null)
            return false;

        BluetoothDevice device = service.getDevice();
        if (device == null)
            return false;

        try {
            mService.readAttValue(mClientIf, device.getAddress(), descriptor.getHandle());
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
        return true;
    }
}
