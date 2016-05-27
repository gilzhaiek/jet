package com.reconinstruments.bletest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

@SuppressLint("NewApi")
public class MainActivity extends Activity
{
    private static final String TAG = "BLETest";
    private Context mContext = null;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler = new Handler();
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mbluetoothGatt;

    // Stops scanning after 10 seconds
    private static final long SCAN_PERIOD = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.main);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Log.e(TAG, "BT Adapter is null or not enabled!");
            }
        } else {
            Log.e(TAG, "Unable to retrieve BluetoothManager");
        }
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice("CE:99:02:B2:FC:FF");
    }

    @Override
    public void onDestroy() {
        if (mbluetoothGatt != null) {
            Log.d(TAG, "onDestroy");
            mbluetoothGatt.close();
        }
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        super.onDestroy();
    }

    public void startScan(View v) {
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            StringBuilder sb = new StringBuilder(scanRecord.length*2);
            for (byte b1 : scanRecord)
                sb.append(String.format("%02x", b1 & 0xFF));
            Log.d(TAG, "onLeScan: " + sb.toString());
        }
    };

    public void  startConnect(View v) {
        if ( mBluetoothDevice != null ) {
            Log.d(TAG, "connect");
            mbluetoothGatt = mBluetoothDevice.connectGatt(this, false, mCallback);
        }

    }

    public void  disconnect(View v) {
        if ( mbluetoothGatt != null ) {
            mbluetoothGatt.disconnect();
        }
    }

    private final BluetoothGattCallback mCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
            Log.d(TAG, "onConnectionStateChange status=" + status + " newState=" + newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED)
                    mbluetoothGatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.d(TAG, "onServicesDiscovered status=" + status);
        }
    };
}
