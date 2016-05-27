package com.contour.utils;

import java.util.List;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

public class BluetoothUtils
{
    public static final String TAG = "BluetoothUtils";
    private static BluetoothUtils sInstance;

    
    public static BluetoothUtils getInstance() 
    {
        if(sInstance == null)
        {
            sInstance = new BluetoothUtils();
        }
        return sInstance;
    }
    
    private final BluetoothAdapter mBluetoothAdapter;
    
    private BluetoothUtils()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        {
            throw new IllegalStateException("Can't do bluetoothy things on a device without bluetooth!");
        }
    }
    
    public void currentPairedDevices(List<BluetoothDevice> outputDeviceList)
    {
        //cancel bluetooth discovery mode to access the adapter
        mBluetoothAdapter.cancelDiscovery();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices)
        {
            BluetoothClass bluetoothClass = device.getBluetoothClass();
            if (bluetoothClass != null)
            {
                // Not really sure what we want, but I know what we don't want.
                switch (bluetoothClass.getMajorDeviceClass())
                {
                    case BluetoothClass.Device.Major.COMPUTER:
                    case BluetoothClass.Device.Major.PHONE:
                        break;
                    default:
                        outputDeviceList.add(device);
                }
            }
        }
    }
    
    public BluetoothDevice findDevice()
    {
        //cancel bluetooth discovery mode to access the adapter
        mBluetoothAdapter.cancelDiscovery();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices)
        {
            if (device.getBondState() != BluetoothDevice.BOND_BONDED)
            {
                return device;
            }
        }

        return null;
    }
    
    public BluetoothDevice findDevice(String deviceAddress)
    {
      //cancel bluetooth discovery mode to access the adapter
        mBluetoothAdapter.cancelDiscovery();

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device.getBondState() != BluetoothDevice.BOND_BONDED)
        {
            return device;
        }

        return null;
    }
}


