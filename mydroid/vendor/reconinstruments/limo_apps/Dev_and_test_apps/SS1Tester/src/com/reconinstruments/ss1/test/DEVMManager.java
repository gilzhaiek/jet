package com.reconinstruments.ss1.test;

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.BluetopiaPMException;
import com.stonestreetone.bluetopiapm.DEVM;
import com.stonestreetone.bluetopiapm.DEVM.AuthenticationCallback;
import com.stonestreetone.bluetopiapm.DEVM.ConnectFlags;
import com.stonestreetone.bluetopiapm.DEVM.EventCallback;
import com.stonestreetone.bluetopiapm.DEVM.Keypress;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.LocalPropertyField;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.RemotePropertyField;

import java.util.EnumSet;

public class DEVMManager {
    private static final String TAG = DEVMManager.class.getSimpleName();
    
    public static String btAddress = ""; // F0:CB:A1:74:CB:57 Recon Dev's iPhone

    private MainActivity mActivity;
    private static DEVMManager instance;
    private DEVM deviceManager;
    private boolean registeredForAuthentication;
    
    protected DEVMManager(MainActivity activity) {
        mActivity = activity;
        profileEnable();
    }
    
    public static DEVMManager getInstance(MainActivity activity) {
        if(instance == null) {
            instance = new DEVMManager(activity);
        }
        return instance;
    }

    private void showToast(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mActivity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
   
    public void stop(){
        profileDisable();
    }

    private final void profileEnable() {
        synchronized(this) {
            try {
                deviceManager = new DEVM(deviceEventCallback, deviceAuthenticationCallback);
                registeredForAuthentication = true;
                Log.d(TAG, "Registered with Authentication Management");
            } catch(BluetopiaPMException e) {
                deviceManager = null;
            }

            if(deviceManager == null) {
                try {
                    deviceManager = new DEVM(deviceEventCallback);
                    registeredForAuthentication = false;
                    Log.d(TAG, "Registered for Non-authentication Management");
                } catch(Exception e) {
                    /*
                     * BluetopiaPM server couldn't be contacted.
                     * This should never happen if Bluetooth was
                     * successfully enabled.
                     */
                    Log.w(TAG, "ERROR: Could not connect to the BluetopiaPM service.");
                    BluetoothAdapter.getDefaultAdapter().disable();
                }
            }
        }
    }

    private final void profileDisable() {
        synchronized(mActivity) {
            if(deviceManager != null) {
                deviceManager.dispose();
                deviceManager = null;
                registeredForAuthentication = false;
            }
        }
    }

    private final EventCallback deviceEventCallback = new EventCallback() {

        @Override
        public void devicePoweredOnEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("Device Powered On");

            Log.d(TAG, sb.toString());

        }
        @Override
        public void devicePoweringOffEvent(int poweringOffTimeout) {

            StringBuilder sb = new StringBuilder();

            sb.append("Device Powering Off").append(": ");
            sb.append("Timeout Value ").append(poweringOffTimeout);

            Log.d(TAG, sb.toString());

        }
        @Override
        public void devicePoweredOffEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("Device Powered Off");

            Log.d(TAG, sb.toString());

        }
        @Override
        public void localDevicePropertiesChangedEvent(LocalDeviceProperties localProperties, EnumSet<LocalPropertyField> changedFields) {

            StringBuilder sb = new StringBuilder();

            sb.append("Local Device Property Change");

            Log.d(TAG, sb.toString());

            for (LocalPropertyField localPropertyField: changedFields)
            {
                switch (localPropertyField)
                {
                    case CLASS_OF_DEVICE:

                        Log.d(TAG, "Class of Device: " + Integer.toHexString(localProperties.classOfDevice));
                        break;

                    case DEVICE_NAME:

                        Log.d(TAG, "Device Name: " + localProperties.deviceName);
                        break;

                    case DISCOVERABLE_MODE:

                        if (localProperties.discoverableMode)
                        {

                            Log.d(TAG, "Device is Discoverable");

                        }
                        else
                        {

                            Log.d(TAG, "Device is Not Discoverable");

                        }
                        Log.d(TAG, "Discoverable Mode Timeout: " + localProperties.discoverableModeTimeout);
                        break;

                    case CONNECTABLE_MODE:

                        if (localProperties.connectableMode)
                        {

                            Log.d(TAG, "Device is Connectable");

                        }
                        else
                        {

                            Log.d(TAG, "Device is Not Connectable");

                        }
                        Log.d(TAG, "Connectable Mode Timeout: " + localProperties.connectableModeTimeout);
                        break;

                    case PAIRABLE_MODE:

                        if (localProperties.pairableMode)
                        {

                            Log.d(TAG, "Device is Pairable");

                        }
                        else
                        {

                            Log.d(TAG, "Device is Not Pairable");

                        }
                        Log.d(TAG, "Pairable Mode Timeout: " + localProperties.pairableModeTimeout);
                        break;

                    case DEVICE_FLAGS:

                        for (LocalDeviceFlags localDeviceFlags: localProperties.localDeviceFlags)
                        {

                            Log.d(TAG, "Device Flag: " + localDeviceFlags);

                        }
                        break;

                }

            }

        }
        @Override
        public void remoteDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) {

            StringBuilder sb = new StringBuilder();

            sb.append("Remote Device Services Status").append(": ");
            sb.append(remoteDevice.toString());
            if (success){

                sb.append(" Success");

            }
            else{

                sb.append(" Failure");

            }
            Log.d(TAG, sb.toString());

        }

        @Override
        public void remoteLowEnergyDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) {

            StringBuilder sb = new StringBuilder();

            sb.append("remoteLowEnergyDeviceServicesStatusEvent: ");
            sb.append(remoteDevice.toString());
            if (success){

                sb.append(" Success");

            }
            else{

                sb.append(" Failure");

            }
            Log.d(TAG, sb.toString());

        }

        @Override
        public void discoveryStartedEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("Discovery Started");
            Log.d(TAG, sb.toString());

        }
        @Override
        public void discoveryStoppedEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append("Discovery Stopped");
            Log.d(TAG, sb.toString());

        }
        @Override
        public void remoteDeviceFoundEvent(RemoteDeviceProperties deviceProperties) {

            StringBuilder sb = new StringBuilder();

            sb.append("Remote Device Found");
            Log.d(TAG, sb.toString());

            Log.d(TAG, "Device Address  : " + deviceProperties.deviceAddress.toString());
            Log.d(TAG, "Class of Device : " + Integer.toHexString(deviceProperties.classOfDevice));
            Log.d(TAG, "Device Name     : " + deviceProperties.deviceName);
            Log.d(TAG, "RSSI            : " + deviceProperties.rssi);
            Log.d(TAG, "Transmit power  : " + deviceProperties.transmitPower);
            Log.d(TAG, "Sniff Interval  : " + deviceProperties.sniffInterval);

            if (deviceProperties.applicationData != null)
            {

                Log.d(TAG, "Friendly Name   : " + deviceProperties.applicationData.friendlyName);
                Log.d(TAG, "Application Info: " + deviceProperties.applicationData.applicationInfo);

            }

            for (RemoteDeviceFlags remoteDeviceFlag: deviceProperties.remoteDeviceFlags)
            {

                Log.d(TAG, "Remote Device Flag: " + remoteDeviceFlag.toString());

            }

        }
        @Override
        public void remoteDevicePropertiesStatusEvent(boolean success, RemoteDeviceProperties deviceProperties) {

            StringBuilder sb = new StringBuilder();

            sb.append("Remote Device Properties Status").append(": ");
            if (success)
            {

                sb.append("success");

            }
            else
            {

                sb.append("failure");

            }

            Log.d(TAG, sb.toString());

            Log.d(TAG, "Device Address    : " + deviceProperties.deviceAddress.toString());
            Log.d(TAG, "Class of Device   : " + Integer.toHexString(deviceProperties.classOfDevice));
            Log.d(TAG, "Device Name       : " + deviceProperties.deviceName);
            Log.d(TAG, "RSSI              : " + deviceProperties.rssi);
            Log.d(TAG, "Transmit power    : " + deviceProperties.transmitPower);
            Log.d(TAG, "Sniff Interval    : " + deviceProperties.sniffInterval);

            if (deviceProperties.applicationData != null)
            {

                Log.d(TAG, "Friendly Name     : " + deviceProperties.applicationData.friendlyName);
                Log.d(TAG, "Application Info  : " + deviceProperties.applicationData.applicationInfo);

            }

            for (RemoteDeviceFlags remoteDeviceFlag: deviceProperties.remoteDeviceFlags)
            {

                Log.d(TAG, "Remote Device Flag: " + remoteDeviceFlag.toString());

            }

        }
        @Override
        public void remoteDevicePropertiesChangedEvent(RemoteDeviceProperties deviceProperties, EnumSet<RemotePropertyField> changedFields) {

            boolean foundChange = false;

            boolean foundFlag = false;

            StringBuilder sb = new StringBuilder();

            sb.append("Remote Device Property Change").append(": ");
            sb.append(deviceProperties.deviceAddress.toString());

            Log.d(TAG, sb.toString());

            for (RemotePropertyField remotePropertyField: changedFields)
            {

                switch (remotePropertyField)
                {

                    case DEVICE_FLAGS:

                        for (RemoteDeviceFlags remoteDeviceFlags: deviceProperties.remoteDeviceFlags)
                        {

                            Log.d(TAG, "Remote Device Flag: " + remoteDeviceFlags);

                        }
                        break;

                    case CLASS_OF_DEVICE:

                        Log.d(TAG, "Class of Device: " + Integer.toHexString(deviceProperties.classOfDevice));
                        foundChange = true;
                        break;

                    case DEVICE_NAME:

                        Log.d(TAG, "Device Name: " + deviceProperties.deviceName);
                        foundChange = true;
                        break;

                    case APPLICATION_DATA:

                        if (deviceProperties.applicationData != null)
                        {

                            Log.d(TAG, "Friendly Name   : " + deviceProperties.applicationData.friendlyName);
                            Log.d(TAG, "Application Info: " + deviceProperties.applicationData.applicationInfo);

                        }
                        else
                        {

                            Log.d(TAG, "Application Data is null");

                        }
                        foundChange = true;
                        break;

                    case RSSI:

                        Log.d(TAG, "RSSI: " + deviceProperties.rssi);
                        foundChange = true;
                        break;

                    case PAIRING_STATE:

                        for (RemoteDeviceFlags searchPaired: deviceProperties.remoteDeviceFlags)
                        {
                            if (searchPaired == RemoteDeviceFlags.CURRENTLY_PAIRED)
                            {

                                Log.d(TAG, "Device Paired");
                                showToast("Device Paired");
                                foundFlag = true;
                                break;

                            }
                        }

                        if (foundFlag == false)
                        {
                            
                            Log.d(TAG, "Device Unpaired");
                            showToast("Device Unpaired");
                        }
                        else
                        {

                            foundFlag = false;

                        }
                        foundChange = true;
                        break;

                    case CONNECTION_STATE:

                        for (RemoteDeviceFlags searchConnected: deviceProperties.remoteDeviceFlags)
                        {
                            if (searchConnected == RemoteDeviceFlags.CURRENTLY_CONNECTED)
                            {

                                Log.d(TAG, "Device Connected");
                                showToast("Device Connected");
                                foundFlag = true;
                                break;

                            }
                        }

                        if (foundFlag == false)
                        {

                            Log.d(TAG, "Device Unconnected");
                            showToast("Device Unconnected");
                        }
                        else
                        {

                            foundFlag = false;

                        }
                        foundChange = true;
                        break;

                    case ENCRYPTION_STATE:

                        for (RemoteDeviceFlags searchEncrypted: deviceProperties.remoteDeviceFlags)
                        {
                            if (searchEncrypted == RemoteDeviceFlags.LINK_CURRENTLY_ENCRYPTED)
                            {

                                Log.d(TAG, "Device Link Encrypted");
                                foundFlag = true;
                                break;

                            }
                        }

                        if (foundFlag == false)
                        {

                            Log.d(TAG, "Device Link Unencrypted");

                        }
                        else
                        {

                            foundFlag = false;

                        }
                        foundChange = true;
                        break;

                    case SNIFF_STATE:

                        for (RemoteDeviceFlags searchSniff: deviceProperties.remoteDeviceFlags)
                        {
                            if (searchSniff == RemoteDeviceFlags.LINK_CURRENTLY_SNIFF_MODE)
                            {

                                Log.d(TAG, "Device Link in Sniff Mode");
                                foundFlag = true;
                                break;

                            }
                        }

                        if (foundFlag == false)
                        {

                            Log.d(TAG, "Device Link not in Sniff Mode");

                        }
                        else
                        {

                            foundFlag = false;

                        }
                        foundChange = true;
                        break;

                    case SERVICES_STATE:

                        for (RemoteDeviceFlags searchServices: deviceProperties.remoteDeviceFlags)
                        {
                            if (searchServices == RemoteDeviceFlags.SERVICES_KNOWN)
                            {

                                Log.d(TAG, "Services are Known");
                                foundFlag = true;
                                break;

                            }
                        }

                        if (foundFlag == false)
                        {

                            Log.d(TAG, "Services are not Known");

                        }
                        else
                        {

                            foundFlag = false;

                        }
                        foundChange = true;
                        break;

                }

                if (foundChange)
                {

                    break;

                }

            }

        }
        @Override
        public void remoteDevicePairingStatusEvent(BluetoothAddress remoteDevice, boolean success, int authenticationStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append("remote Device Pairing Status").append(": ");
            if (success)
            {

                sb.append("Success ").append(remoteDevice.toString());

            }
            else
            {

                sb.append("Failure ");

            }
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());
            Log.d(TAG, "Authentication Status: " + authenticationStatus);

        }
        @Override
        public void remoteDeviceEncryptionStatusEvent(BluetoothAddress remoteDevice, int status) {

            StringBuilder sb = new StringBuilder();

            sb.append("Remote Device Encryption Status").append(": ");
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());
            Log.d(TAG, "Encryption Status: " + status);

        }
        @Override
        public void remoteDeviceDeletedEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append("Remote Device Deleted").append(": ");
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());

        }
        @Override
        public void remoteDeviceConnectionStatusEvent(BluetoothAddress remoteDevice, int status) {

            StringBuilder sb = new StringBuilder();

            sb.append("Remote Device Connection Status").append(": ");
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());
            Log.d(TAG, "Connection Status: " + status);

        }
        @Override
        public void remoteDeviceAuthenticationStatusEvent(BluetoothAddress remoteDevice, int status) {

            StringBuilder sb = new StringBuilder();

            sb.append("Remote Device Authentication Status").append(": ");
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());
            Log.d(TAG, "Authentication Status: " + status);

        }

        @Override
        public void deviceScanStartedEvent() {
            Log.d(TAG, "deviceScanStartedEvent");
        }

        @Override
        public void deviceScanStoppedEvent() {
            Log.d(TAG, "deviceScanStoppedEvent");
        }

        @Override
        public void remoteLowEnergyDeviceAddressChangedEvent(BluetoothAddress PriorResolvableDeviceAddress, BluetoothAddress CurrentResolvableDeviceAddress) {

            StringBuilder sb = new StringBuilder();

            sb.append("remoteLowEnergyDeviceAddressChangedEvent: \n");
            sb.append("    PriorResolvableDeviceAddress = ");
            sb.append(PriorResolvableDeviceAddress.toString());
            sb.append("\n    CurrentResolvableDeviceAddress = ");
            sb.append(CurrentResolvableDeviceAddress.toString());

            Log.d(TAG, sb.toString());

        }
    };

    private final AuthenticationCallback deviceAuthenticationCallback = new AuthenticationCallback() {

        @Override
        public void userConfirmationRequestEvent(BluetoothAddress remoteDevice, int passkey) {

            StringBuilder sb = new StringBuilder();

            sb.append("User Confirmation Request").append(": ");
            sb.append(remoteDevice.toString()).append(" ").append(passkey);
            Log.d(TAG, sb.toString());

        }
        @Override
        public void pinCodeRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append("Pin Code Request").append(": ");
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());

        }
        @Override
        public void passkeyRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append("Pass Key Request").append(": ");
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());

        }
        @Override
        public void passkeyIndicationEvent(BluetoothAddress remoteDevice, int passkey) {

            StringBuilder sb = new StringBuilder();

            sb.append("Pass Key Indication").append(": ");
            sb.append(remoteDevice.toString()).append(" ").append(passkey);
            Log.d(TAG, sb.toString());

          }
        @Override
        public void outOfBandDataRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append("Out Of Band Data Request").append(": ");
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());

        }
        @Override
        public void keypressIndicationEvent(BluetoothAddress remoteDevice, Keypress keyPressType) {

            StringBuilder sb = new StringBuilder();

            sb.append("Out Of Band Data Request").append(": ");
            sb.append(remoteDevice.toString());

            switch (keyPressType)
            {
                case ENTRY_STARTED:
                    sb.append(" Entry Started");
                    break;

                case DIGIT_ENTERED:
                    sb.append(" Digit Entered");
                    break;

                case DIGIT_ERASED:
                    sb.append(" Digit Erased");
                    break;

                case CLEARED:
                    sb.append(" Cleared");
                    break;

                case ENTRY_COMPLETED:
                    sb.append(" Entry Completed");
                    break;
            }

            Log.d(TAG, sb.toString());
        }
        @Override
        public void ioCapabilitiesRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append("Input-Output Request").append(": ");
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());

        }
        @Override
        public void authenticationStatusEvent(BluetoothAddress remoteDevice, int status) {

            StringBuilder sb = new StringBuilder();

            sb.append("Authentication Status").append(": ");
            sb.append(remoteDevice.toString()).append(" ").append(status);
            Log.d(TAG, sb.toString());

        }

        @Override
        public void lowEnergyUserConfirmationRequestEvent(BluetoothAddress remoteDevice, int passkey) {

            StringBuilder sb = new StringBuilder();

            sb.append("lowEnergyUserConfirmationRequestEvent: ");
            sb.append(remoteDevice.toString()).append(" ").append(passkey);
            Log.d(TAG, sb.toString());

        }

        @Override
        public void lowEnergyPasskeyRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append("lowEnergyPasskeyRequestEvent: ");
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());

        }

        @Override
        public void lowEnergyPasskeyIndicationEvent(BluetoothAddress remoteDevice, int passkey) {

            StringBuilder sb = new StringBuilder();

            sb.append("lowEnergyPasskeyIndicationEvent: ");
            sb.append(remoteDevice.toString()).append(" ").append(passkey);
            Log.d(TAG, sb.toString());

        }

        @Override
        public void lowEnergyOutOfBandDataRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append("lowEnergyOutOfBandDataRequestEvent: ");
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());

        }

        @Override
        public void lowEnergyIOCapabilitiesRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append("lowEnergyIOCapabilitiesRequestEvent: ");
            sb.append(remoteDevice.toString());
            Log.d(TAG, sb.toString());

        }
    };
    

    public void updateDiscoverableMode() {

        new Thread() {
            @Override
            public void run() {
    
                int result;
    
                int timeoutInteger = 120;
    
                if(deviceManager != null) {
    
                    result = deviceManager.updateDiscoverableMode(true, timeoutInteger);
    
                    showToast("updateDiscoverableMode() result: " + result);
    
                }
    
            }
        }.start();
    }
    
    public void startDeviceDiscovery() {

        new Thread() {
        @Override
            public void run() {
    
                int result;
    
                int timeoutInteger = 120;
    
                if(deviceManager != null) {
    
                    result = deviceManager.startDeviceDiscovery(timeoutInteger);
    
                    showToast("startDeviceDiscovery() result: " + result);
    
                }
    
            }
        }.start();
    }

    public void connectWithRemoteDevice() {

        new Thread() {
            @Override
            public void run() {
    
                int result;
    
                BluetoothAddress bluetoothAddress = new BluetoothAddress(mActivity.getBTAddress());
    
                EnumSet<ConnectFlags> connectionFlags;
    
                if(deviceManager != null) {
    
                    connectionFlags = EnumSet.noneOf(ConnectFlags.class);
    
                    connectionFlags.add(ConnectFlags.AUTHENTICATE);
                    connectionFlags.add(ConnectFlags.ENCRYPT);
    
                    result = deviceManager.connectWithRemoteDevice(bluetoothAddress, connectionFlags);
    
                    showToast("connectWithRemoteDevice() result: " + result);
    
                }
    
            }
        }.start();
    }

    public void disconnectRemoteDevice() {

        new Thread() {
            @Override
            public void run() {
    
                int result;
    
                BluetoothAddress bluetoothAddress = new BluetoothAddress(mActivity.getBTAddress());
    
                if(deviceManager != null) {
    
                    result = deviceManager.disconnectRemoteDevice(bluetoothAddress, false);
    
                    showToast("disconnectRemoteDevice() result: " + result);
    
                }
    
            }
        }.start();
    }

    public void unpairRemoteDevice() {

        new Thread() {
            @Override
            public void run() {
                int result;
                BluetoothAddress bluetoothAddress = new BluetoothAddress(mActivity.getBTAddress());
    
                if(deviceManager != null) {
                    result = deviceManager.unpairRemoteDevice(bluetoothAddress);
    
                    showToast("unpairRemoteDevice() result: " + result);
                }
            }
        }.start();
    }
    
    public void deleteRemoteDevice() {

        new Thread() {
            @Override
            public void run() {
    
                int result;
    
                BluetoothAddress bluetoothAddress = new BluetoothAddress(mActivity.getBTAddress());
    
                if(deviceManager != null) {
    
                    result = deviceManager.deleteRemoteDevice(bluetoothAddress);
    
                    showToast("deleteRemoteDevice() result: " + result);
    
                }
    
            }
        }.start();
    };

}
