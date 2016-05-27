/*
 * Copyright (C) 2015 Recon Instruments
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

package reconinstruments.HUDBluetoothLowEnergy.cts; 

import reconinstruments.HUDBluetoothLowEnergy.cts.TargetAttributes;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;

import android.content.pm.PackageManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.Context;
import android.os.SystemClock; //sleep(), remove if not used
import android.os.ConditionVariable;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.UUID;
import java.util.List;
import java.lang.Byte;
import java.lang.reflect.Field;

/**
 * Junit / Instrumentation test for JET custom andorid.bluetooth.BluetoothGatt* API
 *
 * Test and validate the behaviour of the JET custom android.bluetooth.BluetoothGatt* library
 */
public class BluetoothLowEnergyTest extends AndroidTestCase
{
    // Debug variables
    private static final String TAG = "HUDBluetoothLowEnergy";
    private static final boolean DEBUG = false;

    // Test definition constants
    private static final long TIMEOUT = 30000;
    private static final boolean AUTO_CONNECT = false;

    // Constant what value to kill BluetoothGattCallback threads
    private static final int KILL_SIG = -1;


    // Test definition constants for MIO_LINK dongle
    // Need to be switched to support TI Dongle 
    // once writing to TI Dongle is figured out
    private static final boolean TEST_MIO = false;
    private static final int TEST_INT = 15234;
    private static final int TEST_MANTISSA  = 4321;
    private static final int TEST_EXPONENT = 10;
    private static final byte TEST_BYTE_0A = (byte) 0x0A;
    private static final byte TEST_BYTE_3F = (byte) 0x3F;
    private static final byte[] TEST_BYTES_NULL = hexStringToByteArray("0000000000000000000000000000000000000000");
    private static final byte[] TEST_BYTES_0A = hexStringToByteArray("0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A");
    private static final byte[] TEST_BYTES_3F = hexStringToByteArray("3F3F3F3F3F3F3F3F3F3F3F3F3F3F3F3F3F3F3F3F");
    private static final UUID writeServiceUUID = UUID.fromString("6c721826-5bf1-4f64-9170-381c08ec57ee");
    private static final UUID writeCharacteristicUUID = UUID.fromString("6c722a0a-5bf1-4f64-9170-381c08ec57ee");
    private static final UUID desWriteCharacteristicUUID = UUID.fromString("6c722a0c-5bf1-4f64-9170-381c08ec57ee");
    private static final UUID writeDescriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Custom UUIDs to test BluetoothGatt Service, Characteristic, Descriptor constructors
    private static final UUID testServiceUUID = UUID.fromString("00009876-0000-1000-8000-00805f9b34fb");
    private static final UUID testCharacteristicUUID = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb");
    private static final UUID testDescriptorUUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb");

    // Boolean variable to check for BLE support
    private boolean hasBluetoothLe = false;

    // Boolean variable to control BLE scan
    private boolean isLeScanning = true;

    // Bluetooth instances to be tested throughout the CTS test
    private BluetoothManager mBluetoothManager = null;
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothGatt mBluetoothGatt = null;

    // Conditional Locks for multi-threading
    private static ConditionVariable thrdLockA = null;
    private static ConditionVariable thrdLockB = null;

    // Handler for thread communication
    Handler callbackHandler = null;
    Handler testHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case KILL_SIG:
                    reliableFail((String)msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * The very first function called in each CTS test
     *
     * Check for BLE support
     * Initialize Conditional Locks and Bluetooth Manager & Adapter
     */
    public void setUp() throws Exception
    {
        super.setUp();

        thrdLockA = new ConditionVariable(true);
        thrdLockB = new ConditionVariable(true);

        hasBluetoothLe = getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE);
        mBluetoothManager = (BluetoothManager) getContext().getSystemService(
                getContext().BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getContext().registerReceiver(pairReceiver, intentFilter);
    }

    /**
     * The very last function called in each CTS test
     *
     * Unregisters broadcast receiver for automatic pairing
     * Checks if mBluetoothGatt is null
     *      If null disconnect device and close the IPC with HUDGattService
     */
    public void tearDown() throws Exception
    {
        super.tearDown();

        if (mBluetoothGatt != null)
        {
            synchronized(thrdLockA)
            {
                mBluetoothGatt.disconnect();
                blockThrdA();
            }

            mBluetoothGatt.close();
            Message msg = callbackHandler.obtainMessage(KILL_SIG);
            callbackHandler.sendMessage(msg);

            if (DEBUG)  Log.d(TAG, "DEBUG: Device connection state from BluetoothManager: "+mBluetoothManager.getConnectionState(mBluetoothDevice, BluetoothProfile.GATT));
        }
        getContext().unregisterReceiver(pairReceiver);
    }

    /**
     * Extra debug function to test writeCharacteristic(), writeDescriptor() and all related function on the MIO_LINK dongle
     */
    public void testMio()
    {
        if (hasBluetoothLe && mBluetoothManager != null && mBluetoothAdapter != null && TEST_MIO)
        {
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice("CE:99:02:B2:FC:FF");

            synchronized(thrdLockA)
            {
                new Thread(new CallBackThrd(getContext())).start();
                blockThrdA();
                assertNotNull("mBluetoothGatt is null... FAIL!", mBluetoothGatt);

                mBluetoothGatt.discoverServices();
                blockThrdA();

                if (DEBUG)
                {
                    for (BluetoothGattService mBluetoothGattService : mBluetoothGatt.getServices())
                    {
                        for (BluetoothGattCharacteristic mBluetoothGattCharacteristic : mBluetoothGattService.getCharacteristics())
                        {
                            for (BluetoothGattDescriptor mBluetoothGattDescriptor : mBluetoothGattCharacteristic.getDescriptors())
                            {
                                Log.d(TAG, "DEBUG: Service: "+mBluetoothGattService.getUuid());
                                Log.d(TAG, "DEBUG: Characteristic: "+mBluetoothGattCharacteristic.getUuid());
                                Log.d(TAG, "DEBUG: Descriptor: "+mBluetoothGattDescriptor.getUuid());
                            }
                        }
                    }
                }

                BluetoothGattCharacteristic testCharacter = mBluetoothGatt.
                    getService(writeServiceUUID).
                    getCharacteristic(writeCharacteristicUUID);

                assertTrue("testCharacter PERMISSION_WRITE == 0... FAIL!", (testCharacter.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0);
                assertTrue("testCharacter PERMISSION_READ == 0... FAIL!", (testCharacter.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);

                mBluetoothGatt.readCharacteristic(testCharacter);
                blockThrdA();
                if(DEBUG)
                {
                    for (Byte data : testCharacter.getValue())
                    {
                        Log.d(TAG, "DEBUG: byteArrayToInt bits: "+String.format("%8s", Integer.toBinaryString(data & 0xff)).replace(' ', '0'));
                        Log.d(TAG, "DEBUG: byteArrayToInt value: "+data);
                    }
                }

                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_BYTES_0A));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(testCharacter));
                blockThrdA();
                for (Byte data : testCharacter.getValue())
                {
                    if(DEBUG)
                    {
                        Log.d(TAG, "DEBUG: byteArrayToInt bits: "+String.format("%8s", Integer.toBinaryString(data & 0xff)).replace(' ', '0'));
                        Log.d(TAG, "DEBUG: byteArrayToInt value: "+data);
                    }
                    assertTrue("BluetoothGattCharacter.getValue/setValue behaviour incorrect... FAIL!", data.equals(TEST_BYTE_0A));
                }

                assertTrue("BluetoothGatt.beginReliableWrite() returned false... FAIL!", mBluetoothGatt.beginReliableWrite());
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_BYTES_3F));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(testCharacter));
                blockThrdA();
                for (Byte data : testCharacter.getValue())
                {
                    if(DEBUG)
                    {
                        Log.d(TAG, "DEBUG: byteArrayToInt bits: "+String.format("%8s", Integer.toBinaryString(data & 0xff)).replace(' ', '0'));
                        Log.d(TAG, "DEBUG: byteArrayToInt value: "+data);
                    }
                    assertTrue("BluetoothGattCharacteristic.getValue()/setValue() behaviour incorrect... FAIL!", data.equals(TEST_BYTE_3F));
                }
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_BYTES_0A));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(testCharacter));
                blockThrdA();
                for (Byte data : testCharacter.getValue())
                {
                    if(DEBUG)
                    {
                        Log.d(TAG, "DEBUG: byteArrayToInt bits: "+String.format("%8s", Integer.toBinaryString(data & 0xff)).replace(' ', '0'));
                        Log.d(TAG, "DEBUG: byteArrayToInt value: "+data);
                    }
                    assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!", data.equals(TEST_BYTE_0A));
                }
                assertTrue("BluetoothGatt.executeReliableWrite() returned false... FAIL!", mBluetoothGatt.executeReliableWrite());

                assertTrue("BluetoothGatt.beginReliableWrite() returned false... FAIL!", mBluetoothGatt.beginReliableWrite());
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_BYTES_3F));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(testCharacter));
                blockThrdA();
                for (Byte data : testCharacter.getValue())
                {
                    if(DEBUG)
                    {
                        Log.d(TAG, "DEBUG: byteArrayToInt bits: "+String.format("%8s", Integer.toBinaryString(data & 0xff)).replace(' ', '0'));
                        Log.d(TAG, "DEBUG: byteArrayToInt value: "+data);
                    }
                    assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!", data.equals(TEST_BYTE_3F));
                }
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_BYTES_0A));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(testCharacter));
                blockThrdA();
                for (Byte data : testCharacter.getValue())
                {
                    if(DEBUG)
                    {
                        Log.d(TAG, "DEBUG: byteArrayToInt bits: "+String.format("%8s", Integer.toBinaryString(data & 0xff)).replace(' ', '0'));
                        Log.d(TAG, "DEBUG: byteArrayToInt value: "+data);
                    }
                    assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!", data.equals(TEST_BYTE_0A));
                }
                mBluetoothGatt.abortReliableWrite(mBluetoothDevice);
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(testCharacter));
                blockThrdA();
                for (Byte data : testCharacter.getValue())
                {
                    if(DEBUG)
                    {
                        Log.d(TAG, "DEBUG: byteArrayToInt bits: "+String.format("%8s", Integer.toBinaryString(data & 0xff)).replace(' ', '0'));
                        Log.d(TAG, "DEBUG: byteArrayToInt value: "+data);
                    }
                    assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!", data.equals(TEST_BYTE_0A));
                }

                String testString = "Testing tEsting teSt";
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_BYTES_NULL));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(testString));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!", testCharacter.getStringValue(0).equals(testString));

                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_BYTES_NULL));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_INT, BluetoothGattCharacteristic.FORMAT_UINT16, 0));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!",
                        testCharacter.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0) == TEST_INT);

                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_BYTES_NULL));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_INT, BluetoothGattCharacteristic.FORMAT_UINT16, 6));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!",
                        testCharacter.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 6) == TEST_INT);

                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_BYTES_NULL));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_MANTISSA, TEST_EXPONENT, BluetoothGattCharacteristic.FORMAT_FLOAT, 0));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!",
                        testCharacter.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0).equals((float)(TEST_MANTISSA*Math.pow(10,TEST_EXPONENT))));

                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_BYTES_NULL));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", testCharacter.setValue(TEST_MANTISSA, TEST_EXPONENT, BluetoothGattCharacteristic.FORMAT_FLOAT, 6));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(testCharacter));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!",
                        testCharacter.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 6).equals((float)(TEST_MANTISSA*Math.pow(10,TEST_EXPONENT))));

                BluetoothGattDescriptor testDes = mBluetoothGatt.
                    getService(writeServiceUUID).
                    getCharacteristic(desWriteCharacteristicUUID).
                    getDescriptor(writeDescriptorUUID);
                assertTrue("BluetoothGatt.readDescriptor() returned false... FAIL!", mBluetoothGatt.readDescriptor(testDes));
                blockThrdA();
                if(DEBUG)
                {
                    for (Byte data : testDes.getValue())
                    {
                        Log.d(TAG, "DEBUG: byteArrayToInt bits: "+String.format("%8s", Integer.toBinaryString(data & 0xff)).replace(' ', '0'));
                        Log.d(TAG, "DEBUG: byteArrayToInt value: "+data);
                    }
                }

                assertTrue("BluetoothGattDescriptor.setValue() returned false... FAIL!", testDes.setValue(new byte[] {TEST_BYTE_0A, TEST_BYTE_0A}));
                assertTrue("BluetoothGatt.writeDescriptor() returned false... FAIL!", mBluetoothGatt.writeDescriptor(testDes));
                blockThrdA();
                assertTrue("BluetoothGatt.readDescriptor() returned false... FAIL!", mBluetoothGatt.readDescriptor(testDes));
                blockThrdA();
                for (Byte data : testDes.getValue())
                {
                    if(DEBUG)
                    {
                        Log.d(TAG, "DEBUG: byteArrayToInt bits: "+String.format("%8s", Integer.toBinaryString(data & 0xff)).replace(' ', '0'));
                        Log.d(TAG, "DEBUG: byteArrayToInt value: "+data);
                    }
                    assertTrue("BluetoothGattDescriptor.getValue/setValue behaviour incorrect... FAIL!", data.equals(TEST_BYTE_0A));
                }

                assertTrue("BluetoothGattDescriptor.setValue() returned false... FAIL!", testDes.setValue(new byte[] {TEST_BYTE_3F, TEST_BYTE_3F}));
                assertTrue("BluetoothGatt.writeDescriptor() returned false... FAIL!", mBluetoothGatt.writeDescriptor(testDes));
                blockThrdA();
                assertTrue("BluetoothGatt.readDescriptor() returned false... FAIL!", mBluetoothGatt.readDescriptor(testDes));
                blockThrdA();
                for (Byte data : testDes.getValue())
                {
                    if(DEBUG)
                    {
                        Log.d(TAG, "DEBUG: byteArrayToInt bits: "+String.format("%8s", Integer.toBinaryString(data & 0xff)).replace(' ', '0'));
                        Log.d(TAG, "DEBUG: byteArrayToInt value: "+data);
                    }
                    assertTrue("BluetoothGattDescriptor.getValue/setValue behaviour incorrect... FAIL!", data.equals(TEST_BYTE_3F));
                }

                mBluetoothGatt.readRemoteRssi();
                blockThrdA();
            }
        }
    }

    /**
     * Extra debug function to obtain all Services, Characteristic, Descriptor UUID
     */
    public void test0_Debug()
    {
        if (hasBluetoothLe && mBluetoothManager != null && mBluetoothAdapter != null && DEBUG)
        {
            connectTestDevice();
            for (BluetoothGattService mBluetoothGattService : mBluetoothGatt.getServices())
            {
                Log.d(TAG, "DEBUG: Service: "+mBluetoothGattService.getUuid());
                for (BluetoothGattCharacteristic mBluetoothGattCharacteristic : mBluetoothGattService.getCharacteristics())
                {
                    Log.d(TAG, "DEBUG: Characteristic: "+mBluetoothGattCharacteristic.getUuid());
                    for (BluetoothGattDescriptor mBluetoothGattDescriptor : mBluetoothGattCharacteristic.getDescriptors())
                    {
                        Log.d(TAG, "DEBUG: Descriptor: "+mBluetoothGattDescriptor.getUuid());
                    }
                }
            }
        }
    }

    /**
     * Tests the andorid.bluetooth.BluetoothGatt library
     *
     * Checks all provided functions for correct value and callbacks
     */
    public void test1_BluetoothGatt()
    {
        if (hasBluetoothLe && mBluetoothManager != null && mBluetoothAdapter != null)
        {
            connectTestDevice();
            synchronized(thrdLockB)
            {
                assertTrue(mBluetoothGatt.connect());
                blockThrdB();
            }
            try{
                mBluetoothGatt.getConnectedDevices();
                fail("BluetoothGatt.getConnectedDevices() did not throw UnsupportedOperationException... FAIL!");
            }
            catch (Exception e)
            {
                assertTrue("BluetoothGatt UnsupportedOperationException no correct... FAIL!",
                        e.getMessage().equals("Use BluetoothManager#getConnectedDevices instead."));
            }
            try{
                mBluetoothGatt.getConnectionState(mBluetoothDevice);
                fail("BluetoothGatt.getConnectionState() did not throw UnsupportedOperationException... FAIL!");
            }
            catch (Exception e)
            {
                assertTrue("BluetoothGatt UnsupportedOperationException no correct... FAIL!",
                        e.getMessage().equals("Use BluetoothManager#getConnectionState instead."));
            }
            try{
                mBluetoothGatt.getDevicesMatchingConnectionStates(new int[] {BluetoothProfile.STATE_CONNECTED});
                fail("BluetoothGatt.getDeviceMatchingConnectionStates() did not throw UnsupportedOperationException... FAIL!");
            }
            catch (Exception e)
            {
                assertTrue("BluetoothGatt UnsupportedOperationException no correct... FAIL!",
                        e.getMessage().equals("Use BluetoothManager#getDevicesMatchingConnectionStates instead."));
            }

            assertTrue("BluetoothGatt().getDevice() behaviour incorrect... FAIL!", mBluetoothGatt.getDevice().equals(mBluetoothDevice));

            BluetoothGattService HeartRateService = mBluetoothGatt.getService(TargetAttributes.serviceUUID.SERVICE_HEART_RATE.toUuid());
            List<BluetoothGattCharacteristic> HeartRateCharacteristics = HeartRateService.getCharacteristics(); 
            for (int i = 0; i < HeartRateCharacteristics.size(); i++)
            {
                assertTrue("BluetoothGattService.getService() behaviour incorrect... FAIL!",
                        TargetAttributes.serviceUUID.getUuid(HeartRateService.getUuid()).checkService(HeartRateCharacteristics.get(i).getUuid(), HeartRateCharacteristics.size(), i));
            }

            List<BluetoothGattService> mBluetoothGattServices = mBluetoothGatt.getServices();
            assertTrue("BluetoothGattService size() does not meet expectation... FAIL!", mBluetoothGattServices.size() == 5);
            for (int i = 0; i < mBluetoothGattServices.size() ; i++)
            {
                UUID serviceUUID = mBluetoothGattServices.get(i).getUuid();
                switch(i) {
                    case 0: assertTrue("BluetoothGattService getUuid() does not meet expectation... FAIL!",
                                    serviceUUID.equals(TargetAttributes.serviceUUID.SERVICE_GENERIC_ACCESS.toUuid()));
                            break;
                    case 1: assertTrue("BluetoothGattService getUuid() does not meet expectation... FAIL!",
                                    serviceUUID.equals(TargetAttributes.serviceUUID.SERVICE_GENERIC_ATTRIBUTE.toUuid()));
                            break;
                    case 2: assertTrue("BluetoothGattService getUuid() does not meet expectation... FAIL!",
                                    serviceUUID.equals(TargetAttributes.serviceUUID.SERVICE_HEART_RATE.toUuid()));
                            break;
                    case 3: assertTrue("BluetoothGattService getUuid() does not meet expectation... FAIL!",
                                    serviceUUID.equals(TargetAttributes.serviceUUID.SERVICE_DEVICE_INFORMATION.toUuid()));
                            break;
                    case 4: assertTrue("BluetoothGattService getUuid() does not meet expectation... FAIL!",
                                    serviceUUID.equals(TargetAttributes.serviceUUID.SERVICE_BATTERY_SERVICE.toUuid()));
                            break;
                    default: fail("BluetoothGattService getUuid() does not meet expectation... FAIL!");
                             break;
                }
            }

            BluetoothGattCharacteristic BatteryLevelCharacteristic = mBluetoothGatt.getService(
                    TargetAttributes.serviceUUID.SERVICE_BATTERY_SERVICE.toUuid()).getCharacteristic(
                    TargetAttributes.characteristicUUID.CHARACTERISTIC_BATTERY_LEVEL.toUuid());
            synchronized(thrdLockA)
            {
                assertTrue("BluetoothGatt().readCharacteristic behaviour incorrect... FAIL!", mBluetoothGatt.readCharacteristic(BatteryLevelCharacteristic));
                blockThrdA();

                assertTrue("BluetoothGatt().readDescriptor behaviour incorrect... FAIL!", mBluetoothGatt.readDescriptor
                        (BatteryLevelCharacteristic.getDescriptor(TargetAttributes.descriptorUUID.REPORT_REFERENCE.toUuid())));
                blockThrdA();

                assertTrue("BluetoothGatt().readRemoteRssi() behaviour incorrect... FAIL!", mBluetoothGatt.readRemoteRssi());
                blockThrdA();

                assertTrue("BluetoothGatt().setCharacteristicNotification() returned false... FAIL!", mBluetoothGatt.setCharacteristicNotification
                        (HeartRateService.getCharacteristic(TargetAttributes.characteristicUUID.CHARACTERISTIC_HEART_RATE_MEASURE.toUuid()), true));
                blockThrdA();
                assertTrue("BluetoothGatt().setCharacteristicNotification() returned false... FAIL!", mBluetoothGatt.setCharacteristicNotification
                        (HeartRateService.getCharacteristic(TargetAttributes.characteristicUUID.CHARACTERISTIC_HEART_RATE_MEASURE.toUuid()), false));
                blockThrdA();

                BluetoothGattCharacteristic HeartRateCtrlPoint = HeartRateService.getCharacteristic(TargetAttributes.characteristicUUID.CHARACTERISTIC_HEART_RATE_CTRL_POINT.toUuid());
                assertTrue("HeartRateCtrlPoint does not have write permission... FAIL!", (HeartRateCtrlPoint.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0);

                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", HeartRateCtrlPoint.setValue(new byte[] {(byte)0xa1}));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(HeartRateCtrlPoint));
                blockThrdB();

                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", HeartRateCtrlPoint.setValue(new byte[] {(byte)0x00}));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(HeartRateCtrlPoint));
                blockThrdA();

                BluetoothGattDescriptor BatteryDescriptor = BatteryLevelCharacteristic.getDescriptor
                    (TargetAttributes.descriptorUUID.CLIENT_CHARACTERISTIC_CONFIGURATION.toUuid());
                assertTrue("BluetoothGattDescriptor.setValue() returned false... FAIL!", BatteryDescriptor.setValue(hexStringToByteArray("0400")));
                assertTrue("BluetoothGatt.writeDescriptor() returned false... FAIL!", mBluetoothGatt.writeDescriptor(BatteryDescriptor));
                blockThrdA();

                assertTrue("BluetoothGatt.beginReliableWrite() returned false... FAIL!", mBluetoothGatt.beginReliableWrite());
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", HeartRateCtrlPoint.setValue(new byte[] {(byte)0x00}));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(HeartRateCtrlPoint));
                blockThrdA();
                assertTrue("BluetoothGatt.executeReliableWrite() returned false... FAIL!", mBluetoothGatt.executeReliableWrite());

                assertTrue("BluetoothGatt.beginReliableWrite() returned false... FAIL!", mBluetoothGatt.beginReliableWrite());
                assertTrue("BluetoothGatt.beginReliableWrite() returned false... FAIL!", HeartRateCtrlPoint.setValue(new byte[] {(byte)0x0a}));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(HeartRateCtrlPoint));
                blockThrdA();
                mBluetoothGatt.abortReliableWrite(mBluetoothDevice);
            }
        }
    }

    /**
     * Tests the android.bluetooth.BluetoothGattService library
     *
     * Checks all provided functions for correct value and behaviour
     */
    public void test2_BluetoothGattService()
    {
        if (hasBluetoothLe && mBluetoothManager != null && mBluetoothAdapter != null)
        {
            connectTestDevice();
            synchronized(thrdLockB)
            {
                assertTrue(mBluetoothGatt.connect());
                blockThrdB();
            }

            for (BluetoothGattService mBluetoothGattService : mBluetoothGatt.getServices())
            {
                assertTrue("BluetoothGattService getType() does not meet expectation... FAIL!",
                        mBluetoothGattService.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY);
                assertTrue("BluetoothGattService.getInstanceId() behaviour incorrect... FAIL!",
                        mBluetoothGattService.getInstanceId() == 0);
                assertTrue("bluetoothGattService.getIncludedServices() behaviour incorrect... FAIL!",
                        mBluetoothGattService.getIncludedServices().size() == 0);

                UUID serviceUUID = mBluetoothGattService.getUuid();
                List<BluetoothGattCharacteristic> mBluetoothGattCharacteristics = mBluetoothGattService.getCharacteristics(); 
                for (int i = 0; i < mBluetoothGattCharacteristics.size(); i++)
                {
                    assertTrue("BluetoothGattService.getCharacteristics() behaviour incorrect... FAIL!",
                            TargetAttributes.serviceUUID.getUuid(serviceUUID).checkService(mBluetoothGattCharacteristics.get(i).getUuid(), mBluetoothGattCharacteristics.size(), i));
                }
            }

            BluetoothGattService GenericAccessService = mBluetoothGatt.
                getService(TargetAttributes.serviceUUID.SERVICE_GENERIC_ACCESS.toUuid());
            BluetoothGattCharacteristic DeviceNameCharacteristic = GenericAccessService.
                getCharacteristic(TargetAttributes.characteristicUUID.CHARACTERISTIC_DEVICE_NAME.toUuid());
            synchronized(thrdLockA)
            {
                assertTrue("BluetoothGatt.readCharacteristic returned false... FAIL!", mBluetoothGatt.readCharacteristic(DeviceNameCharacteristic));
                blockThrdA();
            }

            assertTrue("BluetoothGattService.addService() returned false... FAIL!", GenericAccessService.addService(
                        mBluetoothGatt.getService(TargetAttributes.serviceUUID.SERVICE_DEVICE_INFORMATION.toUuid())));
            List<BluetoothGattService> mIncludedServices = GenericAccessService.getIncludedServices();
            assertTrue("BluetoothGattService.addService() behaviour incorrect... FAIL!", mIncludedServices.size() == 1);
            assertTrue("BluetoothGattService.addService() or .getIncludedServices() behaviour incorrect... FAIL!",
                    mIncludedServices.get(0).getUuid().equals(TargetAttributes.serviceUUID.SERVICE_DEVICE_INFORMATION.toUuid()));

            BluetoothGattService testService = new BluetoothGattService(testServiceUUID, BluetoothGattService.SERVICE_TYPE_SECONDARY);
            assertTrue("BluetoothGattService.addService() returned false... FAIL!", GenericAccessService.addService(testService));
            mIncludedServices = GenericAccessService.getIncludedServices();
            assertTrue("BluetoothGattService.addService() behaviour incorrect... FAIL!", mIncludedServices.size() == 2);
            for (BluetoothGattService mIncludedService : mIncludedServices)
            {
                assertTrue(mIncludedService.getUuid().equals(TargetAttributes.serviceUUID.SERVICE_DEVICE_INFORMATION.toUuid()) ||
                        mIncludedService.getUuid().equals(testServiceUUID));
                if (mIncludedService.getUuid().equals(testServiceUUID))
                {
                    assertTrue(mIncludedService.getType() == BluetoothGattService.SERVICE_TYPE_SECONDARY);
                }
            }

            GenericAccessService.addCharacteristic(mBluetoothGatt.getService(
                        TargetAttributes.serviceUUID.SERVICE_BATTERY_SERVICE.toUuid()).getCharacteristic(
                        TargetAttributes.characteristicUUID.CHARACTERISTIC_BATTERY_LEVEL.toUuid()));
            List<BluetoothGattCharacteristic> GenericAccessCharacteristics = GenericAccessService.getCharacteristics();
            assertTrue("BluetoothGattService.addCharacteristic() behaviour incorrect... FAIL!", GenericAccessCharacteristics.size() == 6);
            for (int i = 0; i < GenericAccessCharacteristics.size(); i++)
            {
                UUID characteristicUUID = GenericAccessCharacteristics.get(i).getUuid();
                switch(i) {
                    case 0: assertTrue("BluetoothGattService.getCharacteristic() does not meet expectation... FAIL!",
                                    characteristicUUID.equals(TargetAttributes.characteristicUUID.CHARACTERISTIC_DEVICE_NAME.toUuid()));
                            break;
                    case 1: assertTrue("BluetoothGattService.getCharacteristic() does not meet expectation... FAIL!",
                                    characteristicUUID.equals(TargetAttributes.characteristicUUID.CHARACTERISTIC_APPEARENCE.toUuid()));
                            break;
                    case 2: assertTrue("BluetoothGattService.getCharacteristic() does not meet expectation... FAIL!",
                                    characteristicUUID.equals(TargetAttributes.characteristicUUID.CHARACTERISTIC_PRIVACY_FLAG.toUuid()));
                            break;
                    case 3: assertTrue("BluetoothGattService.getCharacteristic() does not meet expectation... FAIL!",
                                    characteristicUUID.equals(TargetAttributes.characteristicUUID.CHARACTERISTIC_RECONNECTION_ADDRESS.toUuid()));
                            break;
                    case 4: assertTrue("BluetoothGattService.getCharacteristic() does not meet expectation... FAIL!",
                                    characteristicUUID.equals(TargetAttributes.characteristicUUID.CHARACTERISTIC_PERIPHERAL_PRED.toUuid()));
                            break;
                    case 5: assertTrue("BluetoothGattService.getCharacteristic()/addCharacteristic() does not meet expectation... FAIL!",
                                    characteristicUUID.equals(TargetAttributes.characteristicUUID.CHARACTERISTIC_BATTERY_LEVEL.toUuid()));
                            break;
                    default: fail("BluetoothGattService.getCharacteristic() does not meet expectation... FAIL!");
                             break;
                }
            }
        }
    }

    /**
     * Tests the android.bluetooth.BluetoothGattCharacteristic library
     *
     * Checks all provided functions for correct value and behaviour
     */
    public void test3_BluetoothGattCharacteristic()
    {
        if (hasBluetoothLe && mBluetoothManager != null && mBluetoothAdapter != null)
        {
            connectTestDevice();
            synchronized(thrdLockB)
            {
                assertTrue(mBluetoothGatt.connect());
                blockThrdB();
            }
            for (BluetoothGattService mBluetoothGattService : mBluetoothGatt.getServices())
            {
                for (BluetoothGattCharacteristic mBluetoothGattCharacteristic : mBluetoothGattService.getCharacteristics())
                {
                    List<BluetoothGattDescriptor> mBluetoothGattDescriptors = mBluetoothGattCharacteristic.getDescriptors();
                    for (int i = 0; i < mBluetoothGattDescriptors.size(); i++) {
                        assertTrue("BluetoothGattCharacteristic.getDescriptors() behaviour incorrect... FAIL!",
                                TargetAttributes.characteristicUUID.getUuid(mBluetoothGattCharacteristic.getUuid()).
                                check_getDescriptors(mBluetoothGattDescriptors.get(i).getUuid(), mBluetoothGattDescriptors.size(), i));
                    } 

                    assertTrue("BluetoothGattCharacteristic.getService() behaviour incorrect... FAIL!",
                            mBluetoothGattCharacteristic.getService().getUuid().equals(mBluetoothGattService.getUuid()));
                    assertTrue("BluetoothGattCharacteristic.getProperties() behaviour incorrect... FAIL!",
                            TargetAttributes.characteristicUUID.getUuid(
                                mBluetoothGattCharacteristic.getUuid()).check_getProperties(
                                mBluetoothGattCharacteristic.getProperties(), mBluetoothGattCharacteristic.getInstanceId()));
                    assertTrue("BluetoothGattCharacteristic.getPermission() behaviour incorrect... FAIL!",
                            mBluetoothGattCharacteristic.getPermissions() == 0);
                    assertTrue("BluetoothGattCharacteristic.getWriteType() behaviour incorrect... FAIL!",
                            mBluetoothGattCharacteristic.getWriteType() == 2);
                }
            }

            BluetoothGattCharacteristic PnpIDChar = mBluetoothGatt.getService(
                    TargetAttributes.serviceUUID.SERVICE_DEVICE_INFORMATION.toUuid()).getCharacteristic(
                    TargetAttributes.characteristicUUID.CHARACTERISTIC_PNP_ID.toUuid());
            BluetoothGattCharacteristic testCharacteristic = new BluetoothGattCharacteristic(
                    testCharacteristicUUID,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
            BluetoothGattService BatteryService = mBluetoothGatt.getService(
                    TargetAttributes.serviceUUID.SERVICE_BATTERY_SERVICE.toUuid());
            assertTrue("BluetoothGattCharacteristic.addCharacteristic() returned false... FAIL!", BatteryService.addCharacteristic(testCharacteristic));
            BluetoothGattCharacteristic resultCharacteristic = BatteryService.getCharacteristic(testCharacteristicUUID);
            assertTrue("BluetoothGattCharacteristic Constructor behaviour incorrect... FAIL!",
                    (resultCharacteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED) != 0 &&
                    (resultCharacteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED) != 0 &&
                    (resultCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 &&
                    (resultCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);

            assertTrue("BluetoothGattCharacteristic.addDescriptor() returned false... FAIL!", PnpIDChar.addDescriptor(BatteryService.getCharacteristic(
                            TargetAttributes.characteristicUUID.CHARACTERISTIC_BATTERY_LEVEL.toUuid()).getDescriptor(
                            TargetAttributes.descriptorUUID.REPORT_REFERENCE.toUuid())));
            for(BluetoothGattDescriptor mBluetoothGattDescriptor : PnpIDChar.getDescriptors())
            {
                assertTrue("BluetoothGattCharacteristic.addDescriptor() behaviour incorrect... FAIL!",
                        mBluetoothGattDescriptor.getUuid().equals(TargetAttributes.descriptorUUID.REPORT_REFERENCE.toUuid()));
            }

            String testString = "Testing tEsting teSt";
            synchronized(thrdLockA)
            {
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", PnpIDChar.setValue(TEST_BYTES_NULL));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", PnpIDChar.setValue(testString));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!", PnpIDChar.getStringValue(0).equals(testString));

                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", PnpIDChar.setValue(TEST_BYTES_NULL));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", PnpIDChar.setValue(TEST_INT, BluetoothGattCharacteristic.FORMAT_UINT16, 0));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!",
                        PnpIDChar.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0) == TEST_INT);

                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", PnpIDChar.setValue(TEST_BYTES_NULL));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", PnpIDChar.setValue(TEST_INT, BluetoothGattCharacteristic.FORMAT_UINT16, 6));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!",
                        PnpIDChar.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 6) == TEST_INT);

                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", PnpIDChar.setValue(TEST_BYTES_NULL));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", PnpIDChar.setValue(TEST_MANTISSA, TEST_EXPONENT, BluetoothGattCharacteristic.FORMAT_FLOAT, 0));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!",
                        PnpIDChar.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0).equals((float)(TEST_MANTISSA*Math.pow(10,TEST_EXPONENT))));

                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", PnpIDChar.setValue(TEST_BYTES_NULL));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.setValue() returned false... FAIL!", PnpIDChar.setValue(TEST_MANTISSA, TEST_EXPONENT, BluetoothGattCharacteristic.FORMAT_FLOAT, 6));
                assertTrue("BluetoothGatt.writeCharacteristic() returned false... FAIL!", mBluetoothGatt.writeCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGatt.readCharacteristic() returned false... FAIL!", mBluetoothGatt.readCharacteristic(PnpIDChar));
                blockThrdA();
                assertTrue("BluetoothGattCharacteristic.getValue/setValue behaviour incorrect... FAIL!",
                        PnpIDChar.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 6).equals((float)(TEST_MANTISSA*Math.pow(10,TEST_EXPONENT))));
            }
        }
    }

    /**
     * Tests the android.bluetooth.BluetoothGattDescriptor library
     *
     * Checks all provided functions for correct value and callbacks
     */
    public void test4_BluetoothGattDescriptor()
    {
        if (hasBluetoothLe && mBluetoothManager != null && mBluetoothAdapter != null)
        {
            connectTestDevice();
            synchronized(thrdLockB)
            {
                assertTrue(mBluetoothGatt.connect());
                blockThrdB();
            }
            for (BluetoothGattService mBluetoothGattService : mBluetoothGatt.getServices())
            {
                for (BluetoothGattCharacteristic mBluetoothGattCharacteristic : mBluetoothGattService.getCharacteristics())
                {
                    for(BluetoothGattDescriptor mBluetoothGattDescriptor : mBluetoothGattCharacteristic.getDescriptors())
                    {
                        assertTrue("mBluetoothGattDescriptor.getCharacteristic behaviour incorrect... FAIL!",
                                mBluetoothGattDescriptor.getCharacteristic().equals(mBluetoothGattCharacteristic));
                        assertTrue("mBluetoothGattDescriptor.getPermissions() behaviour incorrect... FAIL!",
                                mBluetoothGattDescriptor.getPermissions() == 0);
                    }
                }
            }

            BluetoothGattCharacteristic BatteryLevelChar = mBluetoothGatt.getService(
                    TargetAttributes.serviceUUID.SERVICE_BATTERY_SERVICE.toUuid()).getCharacteristic(
                    TargetAttributes.characteristicUUID.CHARACTERISTIC_BATTERY_LEVEL.toUuid());
            BluetoothGattDescriptor CharacteristicConfig = BatteryLevelChar.getDescriptor(
                    TargetAttributes.descriptorUUID.CLIENT_CHARACTERISTIC_CONFIGURATION.toUuid());

            synchronized(thrdLockA)
            {
                CharacteristicConfig.setValue(hexStringToByteArray("0400"));
                assertTrue("BluetoothGatt.writeDescriptor returned false... FAIL!", mBluetoothGatt.writeDescriptor(CharacteristicConfig));
                blockThrdA();
                assertTrue("BluetoothGatt.readDescriptor returned false... FAIL!", mBluetoothGatt.readDescriptor(CharacteristicConfig));
                blockThrdA();

                boolean constructorCheck = false;
                BluetoothGattDescriptor testDescriptor = new BluetoothGattDescriptor(testDescriptorUUID,
                        BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
                assertTrue("BluetoothGattCharacteristic.addDescriptor returned false... FAIL!", BatteryLevelChar.addDescriptor(testDescriptor));

                List<BluetoothGattDescriptor> BatteryLevelDes = BatteryLevelChar.getDescriptors();
                assertTrue("BluetoothGattDescriptor contructor or BluetoothGattCharacteristic.addDescriptor behaviour incorrect... FAIL!",
                        BatteryLevelDes.size() == 3);
                for (BluetoothGattDescriptor mBluetoothGattDescriptor : BatteryLevelDes)
                {
                    if (mBluetoothGattDescriptor.equals(testDescriptor))
                    {
                        constructorCheck = true;
                        int permission = mBluetoothGattDescriptor.getPermissions();
                        assertTrue("BluetoothGattDescriptor constructor behaviour incorrect... FAIL!",
                                (permission & BluetoothGattDescriptor.PERMISSION_READ) != 0 &&
                                (permission & BluetoothGattDescriptor.PERMISSION_WRITE) != 0);
                    }
                }
                assertTrue("BluetoothGattDescriptor contructor or BluetoothGattCharacteristic.addDescriptor behaviour incorrect... FAIL!", constructorCheck);
            }
        } 
    }

    /**
     * Private helper function to block calling thread on lock: thrdLockA
     *
     * Fails test when thread block times out
     */
    private void blockThrdA()
    {
        thrdLockA.close();
        if(!thrdLockA.block(TIMEOUT))
        {
            reliableFail("thread lock timed out after "+TIMEOUT+"s... fail!");
        }
    }

    /**
     * Private helper function to unblock calling thread on lock: thrdLockA
     */
    private void unblockThrdA()
    {
        thrdLockA.open();
    }

    /**
     * Private helper function to block calling thread on lock: thrdLockB
     *
     * Fails test when thread block times out
     */
    private void blockThrdB()
    {
        thrdLockB.close();
        if(!thrdLockB.block(TIMEOUT))
        {
            reliableFail("thread lock timed out after "+TIMEOUT+"s... fail!");
        }
    }

    /**
     * Private helper function to unblock calling thread on lock: thrdLockB
     */
    private void unblockThrdB()
    {
        thrdLockB.open();
    }

    /**
     * Private helper function to connect to TI HeartRate BLE dongle and discover all services
     *
     * Fails test when any of the following operation fails or times out
     *      - detect dongle
     *      - establish BluetoothGatt
     *      - connect to dongle
     *      - discover services
     */
    private void connectTestDevice()
    {
        if (hasBluetoothLe && mBluetoothManager != null && mBluetoothAdapter != null) {
            synchronized(thrdLockA)
            {
                isLeScanning = mBluetoothAdapter.startLeScan(mLeScanCallbacks);
                blockThrdA();
                mBluetoothAdapter.stopLeScan(mLeScanCallbacks);
                isLeScanning = false;

                new Thread(new CallBackThrd(getContext())).start();
                blockThrdA();
                assertNotNull("mBluetoothGatt is null... FAIL!", mBluetoothGatt);

                try {
                    if (DEBUG)  Log.d(TAG, "DEBUG: device bond state: "+Class.forName("android.bluetooth.BluetoothDevice").getMethod("getBondState").invoke(mBluetoothDevice));
                    if (!Class.forName("android.bluetooth.BluetoothDevice").getMethod("getBondState").invoke(mBluetoothDevice).equals(BluetoothDevice.BOND_BONDED)) {
                        blockThrdA();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                SystemClock.sleep(3000);
                assertTrue("BluetoothGatt.discoverServices() returned false... FAIL!", mBluetoothGatt.discoverServices());
                blockThrdA();
            }
        }
    }

    /**
     * Private helper function to fail to safely
     *
     * Ensures the following operation is done before test fails and closes
     *      - disconnects from test device
     *      - stops leScan
     *      - closes the IPC with HUDGATTSERVICE
     */
    public void reliableFail(String failMsg)
    {
        getContext().unregisterReceiver(pairReceiver);
        if (mBluetoothManager.getConnectionState(mBluetoothDevice, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED)
            mBluetoothGatt.disconnect();
        if (mBluetoothGatt != null)
            mBluetoothGatt.close();
        if (isLeScanning)
            mBluetoothAdapter.stopLeScan(mLeScanCallbacks);
        if (callbackHandler != null)
        {
            Message msg = callbackHandler.obtainMessage(KILL_SIG);
            callbackHandler.sendMessage(msg);
        }

        fail(failMsg);
    }

    /**
     * Implementation of the BluetoothGattCallback
     *
     * Fails tests when on of the following operation fails
     *      - when the corresponding callback of a BluetoothGatt operation is not provoked
     *      - callback status is not BluetoothGatt.GATT_SUCCESS
     *      - content of the operation is not correct
     */
    private final BluetoothGattCallback mBluetoothGattCallbacks = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic)
        {
            if (DEBUG)  Log.d(TAG, "DEBUG: onCharacteristicChanged() thread ID: "+Thread.currentThread().getId());

            if (characteristic.getUuid().equals(TargetAttributes.characteristicUUID.CHARACTERISTIC_HEART_RATE_MEASURE.toUuid()))
            {
                synchronized(thrdLockA)
                {
                    assertTrue("BluetoothGattCallback.onCharacteristicChanged() behaviour incorrect... FAIL!",
                            TargetAttributes.characteristicUUID.getUuid(characteristic.getUuid()).checkBluetoothGattCallback(characteristic));
                    unblockThrdA();
                }
            }
            else
                reliableFail("BluetoothGattCallback.onCharacteristicChanged() status: GATT_FAILURE... FAIL!");
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status)
        {
            if (DEBUG)  Log.d(TAG, "DEBUG: onCharacteristicRead() thread ID: "+Thread.currentThread().getId()+", status: "+status);

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                synchronized(thrdLockA)
                {
                    if (!characteristic.getUuid().equals(writeCharacteristicUUID))
                    {
                        assertTrue("BluetoothGattCallback.onCharacteristicRead() behaviour incorrect... FAIL!",
                                TargetAttributes.characteristicUUID.getUuid(characteristic.getUuid()).checkBluetoothGattCallback(characteristic));
                    }
                    unblockThrdA();
                }
            }
            else
                reliableFail("BluetoothGattCallback.onCharacteristicRead() status: GATT_FAILURE... FAIL!");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status)
        {
            if (DEBUG)  Log.d(TAG, "DEBUG: onCharacteristicWrite() thread ID: "+Thread.currentThread().getId()+", status: "+status);

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                synchronized(thrdLockA)
                {
                    unblockThrdA();
                }
            }
            else
                reliableFail("BluetoothGattCallback.onCharacteristicWrite() status: GATT_FAILURE... FAIL!");
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if (DEBUG)  Log.d(TAG, "DEBUG: onConnectionStateChange() thread ID: "+Thread.currentThread().getId()+
                    ", status: "+status+", newState: "+newState);

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                switch (newState)
                {
                    case BluetoothProfile.STATE_CONNECTED:
                        synchronized(thrdLockB)
                        {
                            unblockThrdB();
                        }
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        synchronized(thrdLockA)
                        {
                            unblockThrdA();
                        }
                        break;
                    default:
                        break;
                }
            }
            else
                reliableFail("BluetoothGattCallback.onConnectionStateChange() status: GATT_FAILURE... FAIL!");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                int status)
        {
            if (DEBUG)  Log.d(TAG, "DEBUG: onDescriptorRead() thread ID: "+Thread.currentThread().getId());

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                synchronized(thrdLockA)
                {
                    if (!descriptor.getUuid().equals(writeDescriptorUUID))
                    {
                        assertTrue("BluetoothGattCallback.onDescriptorRead() behaviour incorrect... FAIL!",
                                TargetAttributes.descriptorUUID.getUuid(descriptor.getUuid()).checkDescriptor(descriptor));
                    }
                    unblockThrdA();
                }
            }
            else
                reliableFail("BluetoothGattCallback.onDescriptorRead() status: GATT_FAILURE... FAIL!");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                int status)
        {
            if (DEBUG)  Log.d(TAG,"DEBUG: onDescriptorWrite() thread ID: "+Thread.currentThread().getId()+", status: "+status);

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                synchronized(thrdLockA)
                {
                    unblockThrdA();
                }
            }
            else
                reliableFail("BluetoothGattCallback.onDescriptorWrite() status: GATT_FAILURE... FAIL!");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
        {
            if (DEBUG)  Log.d(TAG, "DEBUG: onReadRemoteRssi() thread ID: "+Thread.currentThread().getId());

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                synchronized(thrdLockA)
                {
                    assertTrue("BluetoothGattCallback.onReadRemoteRssi() rssi out of range... FAIL!",
                            rssi == 0);
                    unblockThrdA();
                }
            }
            else
                reliableFail("BluetoothGattCallback.onReadRemoteRssi() status: GATT_FAILURE... FAIL!");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status)
        {
            if (DEBUG)  Log.d(TAG, "DEBUG: onReliableWriteCompleted() thread ID: "+Thread.currentThread().getId());

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                synchronized(thrdLockA)
                {
                    unblockThrdA();
                }
            }
            else
                reliableFail("BluetoothGattCallback.onReliableWriteCompleted() status: GATT_FAILURE... FAIL!");
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if (DEBUG)  Log.d(TAG, "DEBUG: onServicesDiscovered() thread ID: "+Thread.currentThread().getId()+", status: "+status);

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                synchronized(thrdLockA)
                {
                    unblockThrdA();
                }
            }
            else
                reliableFail("BluetoothGattCallback.onServiceDiscovered() status: GATT_FAILURE... FAIL!");
        }
    };

    /**
     * Implementation of the BLE scan
     *
     * Fails tests when TI HeartRate BLE dongle is not detected
     */
    private final BluetoothAdapter.LeScanCallback mLeScanCallbacks = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            if (DEBUG)  Log.d(TAG, "DEBUG: onLeScan BlutoothDevice: "+device.getAddress()+", rssi: "+rssi);

            if (device.getAddress().equals(TargetAttributes.DEVICE_ADDR) && device.getName().equals(TargetAttributes.DEVICE_NAME))
            {
                synchronized(thrdLockA)
                {
                    mBluetoothDevice = device;
                    unblockThrdA();
                }
            }
        }
    };

    /**
     * Private helper function to create byte array
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Implementation of the automatic pair BroadcastReciever
     *
     * Receives a pairing event for the TI Dongle and automates the pairing process
     */
    private final BroadcastReceiver pairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int mType;
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (DEBUG)  Log.d(TAG, "DEBUG: pairReceiver invoke... device name: "+device.getName()+", UUID: "+device.getAddress()+", intent state: "+state);
                if (state == BluetoothDevice.BOND_BONDING && device.getAddress().equals(TargetAttributes.DEVICE_ADDR)) {
                    try {
                        synchronized(thrdLockB) {
                            Class.forName("android.bluetooth.BluetoothDevice").getMethod("setPasskey", int.class).invoke(device, 0000);
                            unblockThrdB();
                        }
                    } catch (Exception e) {
                        if (DEBUG)  Log.d(TAG, "DEBUG: setPin threw exception: "+e.getMessage());
                        e.printStackTrace();
                    }
                } else if (state == BluetoothDevice.BOND_BONDED && device.getAddress().equals(TargetAttributes.DEVICE_ADDR)) {
                    unblockThrdA();
                }
            }
        }
    };

    /**
     * Thread implementation to create BluetoothGattCallback thread
     */
    public class CallBackThrd implements Runnable {
        Context context;
        public CallBackThrd(Context context)
        {
            this.context = context;
        }

        public void run()
        {
            synchronized(thrdLockA)
            {
                synchronized(thrdLockB)
                {
                    mBluetoothGatt = mBluetoothDevice.connectGatt(context, AUTO_CONNECT, mBluetoothGattCallbacks);
                    blockThrdB();
                }
                unblockThrdA();
            }

            Looper.prepare();
            callbackHandler = new Handler()
            {
                @Override
                public void handleMessage(Message msg)
                {
                    switch(msg.what)
                    {
                        case KILL_SIG:
                            Looper mLooper = Looper.myLooper();
                            if(mLooper != null)
                                mLooper.quit();
                            break;
                        default:
                            break;
                    }
                }
            };
            Looper.loop();
        }
    }
}
