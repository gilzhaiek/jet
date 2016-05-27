/*
 * < MainActivity.java >
 * Copyright 2011 - 2014 Stonestreet One. All Rights Reserved.
 *
 * Device Manager API Sample for Stonestreet One Bluetooth Protocol Stack
 * Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.devm;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.text.InputType;
import android.util.Log;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.BluetopiaPMException;
import com.stonestreetone.bluetopiapm.DEVM;
import com.stonestreetone.bluetopiapm.DEVM.AdvertisingDataType;
import com.stonestreetone.bluetopiapm.DEVM.AdvertisingFlags;
import com.stonestreetone.bluetopiapm.DEVM.AuthenticationCallback;
import com.stonestreetone.bluetopiapm.DEVM.BondingType;
import com.stonestreetone.bluetopiapm.DEVM.ConnectFlags;
import com.stonestreetone.bluetopiapm.DEVM.DeviceIDInformation;
import com.stonestreetone.bluetopiapm.DEVM.EIRDataType;
import com.stonestreetone.bluetopiapm.DEVM.EventCallback;
import com.stonestreetone.bluetopiapm.DEVM.IOCapability;
import com.stonestreetone.bluetopiapm.DEVM.Keypress;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceFeature;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.LocalPropertyField;
import com.stonestreetone.bluetopiapm.DEVM.LowEnergyBondingType;
import com.stonestreetone.bluetopiapm.DEVM.LowEnergyIOCapability;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceApplicationData;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFilter;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.RemotePropertyField;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.CheckboxValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.ChecklistValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.SpinnerValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.TextValue;
import com.stonestreetone.bluetopiapm.sample.util.SS1SampleActivity;

public class MainActivity extends SS1SampleActivity {

    private static final String LOG_TAG = "DEVM_Sample";

    /*package*/ DEVM deviceManager;
    /*package*/ boolean registeredForAuthentication;

    @Override
    protected boolean profileEnable() {
        synchronized(this) {
            try {
                deviceManager = new DEVM(deviceEventCallback, deviceAuthenticationCallback);
                registeredForAuthentication   = true;
                showToast(this, "Registered with Authentication Management");

            } catch(BluetopiaPMException e) {

                deviceManager = null;
            }

            if(deviceManager == null) {
                try {

                    deviceManager = new DEVM(deviceEventCallback);
                    registeredForAuthentication    = false;
                    showToast(this, "Registered for Non-authentication Management");

                } catch(BluetopiaPMException e) {
                    /*
                     * BluetopiaPM server couldn't be contacted.
                     * This should never happen if Bluetooth was
                     * successfully enabled.
                     */
                    showToast(this, R.string.errorBTPMServerNotReachableToastMessage);
                    return false;
                }
            }
            
            return true;
        }
    }

    @Override
    protected void profileDisable() {
        synchronized(MainActivity.this) {
            if(deviceManager != null) {

                deviceManager.dispose();
                deviceManager = null;
                registeredForAuthentication = false;
            }
        }
    }

    @Override
    protected Command[] getCommandList() {
        return commandList;
    }

    @Override
    protected int getNumberProfileParameters() {
        return 0;
    }

    @Override
    protected int getNumberCommandParameters() {
        return 4;
    }

    private final EventCallback deviceEventCallback = new EventCallback() {

        @Override
        public void devicePoweredOnEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.devicePoweredOnEventLabel));

            displayMessage("");
            displayMessage(sb);

        }
        @Override
        public void devicePoweringOffEvent(int poweringOffTimeout) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.devicePoweringOffEventLabel)).append(": ");
            sb.append("Timeout Value ").append(poweringOffTimeout);

            displayMessage("");
            displayMessage(sb);

        }
        @Override
        public void devicePoweredOffEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.devicePoweredOffEventLabel));

            displayMessage("");
            displayMessage(sb);

        }
        @Override
        public void localDevicePropertiesChangedEvent(LocalDeviceProperties localProperties, EnumSet<LocalPropertyField> changedFields) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.localDevicePropertiesChangedEventLabel));

            displayMessage("");
            displayMessage(sb);

            for (LocalPropertyField localPropertyField: changedFields)
            {
                switch (localPropertyField)
                {
                    case CLASS_OF_DEVICE:

                        displayMessage("Class of Device: " + Integer.toHexString(localProperties.classOfDevice));
                        break;

                    case DEVICE_NAME:

                        displayMessage("Device Name: " + localProperties.deviceName);
                        break;

                    case DISCOVERABLE_MODE:

                        if (localProperties.discoverableMode)
                        {

                            displayMessage("Device is Discoverable");

                        }
                        else
                        {

                            displayMessage("Device is Not Discoverable");

                        }
                        displayMessage("Discoverable Mode Timeout: " + localProperties.discoverableModeTimeout);
                        break;

                    case CONNECTABLE_MODE:

                        if (localProperties.connectableMode)
                        {

                            displayMessage("Device is Connectable");

                        }
                        else
                        {

                            displayMessage("Device is Not Connectable");

                        }
                        displayMessage("Connectable Mode Timeout: " + localProperties.connectableModeTimeout);
                        break;

                    case PAIRABLE_MODE:

                        if (localProperties.pairableMode)
                        {

                            displayMessage("Device is Pairable");

                        }
                        else
                        {

                            displayMessage("Device is Not Pairable");

                        }
                        displayMessage("Pairable Mode Timeout: " + localProperties.pairableModeTimeout);
                        break;

                    case DEVICE_FLAGS:

                        for (LocalDeviceFlags localDeviceFlags: localProperties.localDeviceFlags)
                        {

                            displayMessage("Device Flag: " + localDeviceFlags);

                        }
                        break;

                }

            }

        }
        @Override
        public void remoteDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDeviceServicesStatusEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            if (success){

                sb.append(" Success");

            }
            else{

                sb.append(" Failure");

            }
            displayMessage("");
            displayMessage(sb);

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
            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void discoveryStartedEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.discoveryStartedEventLabel));
            displayMessage("");
            displayMessage(sb);

        }
        @Override
        public void discoveryStoppedEvent() {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.discoveryStoppedEventLabel));
            displayMessage("");
            displayMessage(sb);

        }
        @Override
        public void remoteDeviceFoundEvent(RemoteDeviceProperties deviceProperties) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDeviceFoundEventLabel));
            displayMessage("");
            displayMessage(sb);

            displayMessage("Device Address : " + deviceProperties.deviceAddress.toString());
            displayMessage("Device Name : " + deviceProperties.deviceName);

            displayMessage("Remote Device Flags : ");
            for(RemoteDeviceFlags remoteDeviceFlag: deviceProperties.remoteDeviceFlags)
                displayMessage(" " + remoteDeviceFlag.toString());

            if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_BR_EDR)) {

                displayMessage("Class of Device : " + Integer.toHexString(deviceProperties.classOfDevice));
                displayMessage("RSSI : " + deviceProperties.rssi);
                displayMessage("Transmit Power : " + deviceProperties.transmitPower);
                displayMessage("Sniff Interval : " + deviceProperties.sniffInterval);

                if((deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.APPLICATION_DATA_VALID)) && (deviceProperties.applicationData != null)) {

                    displayMessage("Friendly Name : " + deviceProperties.applicationData.friendlyName);
                    displayMessage("Application Info : " + deviceProperties.applicationData.applicationInfo);
                }
            }

            if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {

                displayMessage("LE Address Type : " + deviceProperties.lowEnergyAddressType);
                displayMessage("LE RSSI : " + deviceProperties.lowEnergyRSSI);
                displayMessage("LE Transmit Power : " + deviceProperties.lowEnergyTransmitPower);
                displayMessage("LE Device Appearance : " + deviceProperties.lowEnergyTransmitPower);
                displayMessage("LE Prior Resolvable Addr.: " + deviceProperties.priorResolvableAddress.toString());
            }
        }
        @Override
        public void remoteDevicePropertiesStatusEvent(boolean success, RemoteDeviceProperties deviceProperties) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDevicePropertiesStatusEventLabel)).append(": ").append((success ? "success" : "failure"));

            displayMessage("");
            displayMessage(sb);

            displayMessage("Device Address : " + deviceProperties.deviceAddress.toString());
            displayMessage("Device Name : " + deviceProperties.deviceName);

            displayMessage("Remote Device Flags : ");
            for(RemoteDeviceFlags remoteDeviceFlag: deviceProperties.remoteDeviceFlags)
                displayMessage(" " + remoteDeviceFlag.toString());

            if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_BR_EDR)) {

                displayMessage("Class of Device : " + Integer.toHexString(deviceProperties.classOfDevice));
                displayMessage("RSSI : " + deviceProperties.rssi);
                displayMessage("Transmit power : " + (deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.TX_POWER_KNOWN) ? deviceProperties.transmitPower : "Unknown"));
                displayMessage("Sniff Interval : " + deviceProperties.sniffInterval);

                if((deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.APPLICATION_DATA_VALID)) && (deviceProperties.applicationData != null)) {

                    displayMessage("Friendly Name : " + deviceProperties.applicationData.friendlyName);
                    displayMessage("Application Info : " + deviceProperties.applicationData.applicationInfo);
                }
            }

            if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {

                displayMessage("LE Address Type : " + deviceProperties.lowEnergyAddressType);
                displayMessage("LE RSSI : " + deviceProperties.lowEnergyRSSI);
                displayMessage("LE Transmit Power : " + (deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.LE_TX_POWER_KNOWN) ? deviceProperties.lowEnergyTransmitPower : "Unknown"));
                displayMessage("LE Device Appearance : " + deviceProperties.lowEnergyTransmitPower);
                displayMessage("LE Prior Resolvable Addr.: " + deviceProperties.priorResolvableAddress.toString());
            }
        }
        @Override
        public void remoteDevicePropertiesChangedEvent(RemoteDeviceProperties deviceProperties, EnumSet<RemotePropertyField> changedFields) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDevicePropertiesChangedEventLabel)).append(": ");
            sb.append(deviceProperties.deviceAddress.toString());

            if(changedFields.contains(RemotePropertyField.DEVICE_FLAGS)) {

                displayMessage("Remote Device Flags : ");
                for(RemoteDeviceFlags remoteDeviceFlags: deviceProperties.remoteDeviceFlags)
                    displayMessage(" " + remoteDeviceFlags);
            }

            if(changedFields.contains(RemotePropertyField.DEVICE_NAME))
                displayMessage("Device Name : " + deviceProperties.deviceName);

            if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_BR_EDR)) {

                if(changedFields.contains(RemotePropertyField.CLASS_OF_DEVICE))
                    displayMessage("Class of Device : " + Integer.toHexString(deviceProperties.classOfDevice));

                if(changedFields.contains(RemotePropertyField.APPLICATION_DATA)) {

                    if((deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.APPLICATION_DATA_VALID)) && (deviceProperties.applicationData != null)) {

                        displayMessage("Friendly Name :" + deviceProperties.applicationData.friendlyName);
                        displayMessage("Application Info : " + deviceProperties.applicationData.applicationInfo);

                    } else {

                        displayMessage("No Application Data");
                    }
                }

                if(changedFields.contains(RemotePropertyField.RSSI)) {

                    displayMessage("RSSI : " + deviceProperties.rssi);
                    displayMessage("Transmit Power : " + (deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.TX_POWER_KNOWN) ? deviceProperties.transmitPower : "Unknown"));
                }

                if(changedFields.contains(RemotePropertyField.PAIRING_STATE))
                    displayMessage(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.CURRENTLY_PAIRED) ? "Device Paired" : "Device Unpaired");

                if(changedFields.contains(RemotePropertyField.CONNECTION_STATE))
                    displayMessage(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.CURRENTLY_CONNECTED) ? "Device Connected" : "Device Disconnected");

                if(changedFields.contains(RemotePropertyField.ENCRYPTION_STATE))
                    displayMessage(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.LINK_CURRENTLY_ENCRYPTED) ? "Link Encrypted" : "Link Unencrypted");

                if(changedFields.contains(RemotePropertyField.SNIFF_STATE))
                    displayMessage(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.LINK_CURRENTLY_SNIFF_MODE) ? "Link in Sniff Mode" : "Link not in Sniff Mode");

                if(changedFields.contains(RemotePropertyField.SERVICES_STATE))
                    displayMessage(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SERVICES_KNOWN) ? "Services Known" : "Services Unknown");
            }

            if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {

                displayMessage("Address Type : " + deviceProperties.lowEnergyAddressType);
                displayMessage("Mode : " + (deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_BR_EDR) ? "Dual" : "Single"));

                displayMessage("");
                displayMessage(sb);

                if(changedFields.contains(RemotePropertyField.PRIOR_RESOLVABLE_ADDRESS))
                    displayMessage("Prior Resovable Address : " + deviceProperties.priorResolvableAddress.toString());

                if(changedFields.contains(RemotePropertyField.DEVICE_APPEARANCE))
                    displayMessage("Device Appearance : " + deviceProperties.deviceAppearance);

                if(changedFields.contains(RemotePropertyField.LE_RSSI)) {

                    displayMessage("LE RSSI : " + deviceProperties.lowEnergyRSSI);
                    displayMessage("LE TX Power : " + (deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.LE_TX_POWER_KNOWN) ? deviceProperties.lowEnergyTransmitPower : "Unknown"));
                }

                if(changedFields.contains(RemotePropertyField.LE_PAIRING_STATE))
                    displayMessage(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.CURRENTLY_PAIRED_OVER_LE) ? "Device Paired Over LE" : "Device Unpaired Over LE");

                if(changedFields.contains(RemotePropertyField.LE_CONNECTION_STATE))
                    displayMessage(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.CURRENTLY_CONNECTED_OVER_LE) ? "Device Connected Over LE" : "Device Disconnected Over LE");

                if(changedFields.contains(RemotePropertyField.LE_ENCRYPTION_STATE))
                    displayMessage(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.LE_LINK_CURRENTLY_ENCRYPTED) ? "LE Link Encrypted" : "LE Link Unencrypted");

                if(changedFields.contains(RemotePropertyField.LE_SERVICES_STATE))
                    displayMessage(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.LE_SERVICES_KNOWN) ? "LE Services Known" : "LE Services Unknown");
            }
        }
        @Override
        public void remoteDevicePairingStatusEvent(BluetoothAddress remoteDevice, boolean success, int authenticationStatus) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDevicePairingStatusEventLabel)).append(": ");
            if (success)
            {

                sb.append("Success ").append(remoteDevice.toString());

            }
            else
            {

                sb.append("Failure ");

            }
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);
            displayMessage("Authentication Status: " + authenticationStatus);

        }
        @Override
        public void remoteDeviceEncryptionStatusEvent(BluetoothAddress remoteDevice, int status) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDeviceEncryptionStatusEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);
            displayMessage("Encryption Status: " + status);

        }
        @Override
        public void remoteDeviceDeletedEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDeviceDeletedEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);

        }
        @Override
        public void remoteDeviceConnectionStatusEvent(BluetoothAddress remoteDevice, int status) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDeviceConnectionStatusEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);
            displayMessage("Connection Status: " + status);

        }
        @Override
        public void remoteDeviceAuthenticationStatusEvent(BluetoothAddress remoteDevice, int status) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.remoteDeviceAuthenticationStatusEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);
            displayMessage("Authentication Status: " + status);

        }

        @Override
        public void deviceScanStartedEvent() {
            displayMessage("");
            displayMessage("deviceScanStartedEvent");
        }

        @Override
        public void deviceScanStoppedEvent() {
            displayMessage("");
            displayMessage("deviceScanStoppedEvent");
        }

        @Override
        public void remoteLowEnergyDeviceAddressChangedEvent(BluetoothAddress PriorResolvableDeviceAddress, BluetoothAddress CurrentResolvableDeviceAddress) {

            StringBuilder sb = new StringBuilder();

            sb.append("remoteLowEnergyDeviceAddressChangedEvent: \n");
            sb.append("    PriorResolvableDeviceAddress = ");
            sb.append(PriorResolvableDeviceAddress.toString());
            sb.append("\n    CurrentResolvableDeviceAddress = ");
            sb.append(CurrentResolvableDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void deviceAdvertisingStarted() {
            displayMessage("");
            displayMessage("deviceAdvertisingStartedEvent");

        }

        @Override
        public void deviceAdvertisingStopped() {
            displayMessage("");
            displayMessage("deviceAdvertisingStoppedEvent");

        }
    };

    private final AuthenticationCallback deviceAuthenticationCallback = new AuthenticationCallback() {

        @Override
        public void userConfirmationRequestEvent(BluetoothAddress remoteDevice, int passkey) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.userConfirmationRequestEventLabel)).append(": ");
            sb.append(remoteDevice.toString()).append(" ").append(passkey);
            displayMessage("");
            displayMessage(sb);

        }
        @Override
        public void pinCodeRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.pinCodeRequestEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);

        }
        @Override
        public void passkeyRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.passkeyRequestEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);

        }
        @Override
        public void passkeyIndicationEvent(BluetoothAddress remoteDevice, int passkey) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.passkeyIndicationEventLabel)).append(": ");
            sb.append(remoteDevice.toString()).append(" ").append(passkey);
            displayMessage("");
            displayMessage(sb);

          }
        @Override
        public void outOfBandDataRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.outOfBandDataRequestEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);

        }
        @Override
        public void keypressIndicationEvent(BluetoothAddress remoteDevice, Keypress keyPressType) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.outOfBandDataRequestEventLabel)).append(": ");
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

            displayMessage("");
            displayMessage(sb);
        }
        @Override
        public void ioCapabilitiesRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.ioCapabilitiesRequestEventLabel)).append(": ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);

        }
        @Override
        public void authenticationStatusEvent(BluetoothAddress remoteDevice, int status) {

            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.authenticationStatusEventLabel)).append(": ");
            sb.append(remoteDevice.toString()).append(" ").append(status);
            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void lowEnergyUserConfirmationRequestEvent(BluetoothAddress remoteDevice, int passkey) {

            StringBuilder sb = new StringBuilder();

            sb.append("lowEnergyUserConfirmationRequestEvent: ");
            sb.append(remoteDevice.toString()).append(" ").append(passkey);
            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void lowEnergyPasskeyRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append("lowEnergyPasskeyRequestEvent: ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void lowEnergyPasskeyIndicationEvent(BluetoothAddress remoteDevice, int passkey) {

            StringBuilder sb = new StringBuilder();

            sb.append("lowEnergyPasskeyIndicationEvent: ");
            sb.append(remoteDevice.toString()).append(" ").append(passkey);
            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void lowEnergyOutOfBandDataRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append("lowEnergyOutOfBandDataRequestEvent: ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void lowEnergyIOCapabilitiesRequestEvent(BluetoothAddress remoteDevice) {

            StringBuilder sb = new StringBuilder();

            sb.append("lowEnergyIOCapabilitiesRequestEvent: ");
            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);

        }
    };

    private final CommandHandler convertManufacturerIDToString_Handler = new CommandHandler() {

        @Override
        public void run() {

            TextValue manufacturerParameter;

            int manufacturerInteger;

            String manufacturerString;

            if(deviceManager != null) {

                manufacturerParameter = getCommandParameterView(0).getValueText();

                try {
                    manufacturerInteger = Integer.valueOf(manufacturerParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                manufacturerString = DEVM.convertManufacturerIDToString(manufacturerInteger);

                displayMessage("");

                displayMessage("convertManufacturerIDToString() result: " + manufacturerString);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Manufacturer ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler acquireLock_Handler = new CommandHandler() {

        @Override
        public void run()
        {

            if(deviceManager != null)
            {

                if (deviceManager.acquireLock())
                {

                    displayMessage("");
                    displayMessage("acquireLock() result: Success");

                }
                else
                {
                    displayMessage("");
                    displayMessage("acquireLock() result: Failure");

                }

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler releaseLock_Handler = new CommandHandler() {

        @Override
        public void run()
        {

            if(deviceManager != null)
            {

                deviceManager.releaseLock();

                displayMessage("");
                displayMessage("releaseLock() function launched");

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler queryDevicePowerState_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            if(deviceManager != null) {

                result = deviceManager.queryDevicePowerState();

                if (result == 1){

                    displayMessage("");
                    displayMessage("queryDevicePowerState(): Device On");

                } else if (result == 0){

                    displayMessage("");
                    displayMessage("queryDevicePowerState(): Device Off");

                } else if (result < 0){

                    displayMessage("");
                    displayMessage("queryDevicePowerState() result: " + result);

                }

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler acknowledgeDevicePoweringDown_Handler = new CommandHandler() {

        @Override
        public void run() {

            if(deviceManager != null) {

                deviceManager.acknowledgeDevicePoweringDown();

                displayMessage("");
                displayMessage("acknowledgeDevicePoweringDown() function launched");

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler queryLocalDeviceProperties_Handler = new CommandHandler() {

        @Override
        public void run() {

            LocalDeviceProperties deviceProperties;

            if(deviceManager != null) {

                deviceProperties = deviceManager.queryLocalDeviceProperties();

                displayMessage("");
                if(deviceProperties != null) {

                    displayMessage("queryLocalDeviceProperties()");
                    displayMessage("Device Address     : " + deviceProperties.deviceAddress.toString());
                    displayMessage("Class of Device    : " + Integer.toHexString(deviceProperties.classOfDevice));
                    displayMessage("Device Name        : " + deviceProperties.deviceName);
                    displayMessage("HCI Version        : " + deviceProperties.hciVersion);
                    displayMessage("HCI Revision       : " + deviceProperties.hciRevision);
                    displayMessage("LMP Version        : " + deviceProperties.lmpVersion);
                    displayMessage("LMP Subversion     : " + deviceProperties.lmpSubversion);
                    displayMessage("Device Manufacturer: " + DEVM.convertManufacturerIDToString(deviceProperties.deviceManufacturer));

                    if (deviceProperties.discoverableMode)
                    {

                        displayMessage("Device Discoverable");

                    }
                    else
                    {

                        displayMessage("Device Not Discoverable");

                    }
                    displayMessage("Discoverable Mode Timeout: " + deviceProperties.discoverableModeTimeout);

                    if (deviceProperties.connectableMode)
                    {

                        displayMessage("Device Connectable");

                    }
                    else
                    {

                        displayMessage("Device Not Connectable");

                    }
                    displayMessage("Connectable Mode Timeout: " + deviceProperties.connectableModeTimeout);

                    if (deviceProperties.pairableMode)
                    {

                        displayMessage("Device Pairable");

                    }
                    else
                    {

                        displayMessage("Device Not Pairable");

                    }
                    displayMessage("Pairable Mode Timeout: " + deviceProperties.pairableModeTimeout);

                    for (LocalDeviceFlags localDeviceFlag: deviceProperties.localDeviceFlags)
                    {

                        displayMessage("Local Device Flag  : " + localDeviceFlag);

                    }

                }
                else
                {

                    displayMessage("queryLocalDeviceProperties(): null");

                }

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler updateClassOfDevice_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            TextValue classOfDeviceParameter;

            int classOfDeviceInteger;

            if(deviceManager != null) {

                classOfDeviceParameter = getCommandParameterView(0).getValueText();

                try {
                    classOfDeviceInteger = Integer.parseInt(classOfDeviceParameter.text.toString(),16);
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = deviceManager.updateClassOfDevice(classOfDeviceInteger);

                displayMessage("");

                displayMessage("updateClassOfDevice() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Class of Device", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler updateDeviceName_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            TextValue deviceNameParameter;

            String deviceNameString;

            if(deviceManager != null) {

                deviceNameParameter = getCommandParameterView(0).getValueText();

                try {
                    deviceNameString = deviceNameParameter.text.toString();
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = deviceManager.updateDeviceName(deviceNameString);

                displayMessage("");

                displayMessage("updateDeviceName() result: " + result + " " + deviceNameString);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Name of Device", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler updateDiscoverableMode_Handler = new CommandHandler() {

        boolean discoverableChecked;

        @Override
        public void run() {

            int result;

            CheckboxValue discoverableParameter;

            TextValue timeoutParameter;

            int timeoutInteger;

            if(deviceManager != null) {

                discoverableParameter = getCommandParameterView(0).getValueCheckbox();

                timeoutParameter = getCommandParameterView(1).getValueText();

                try {
                    timeoutInteger = Integer.valueOf(timeoutParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = deviceManager.updateDiscoverableMode(discoverableParameter.value, timeoutInteger);

                displayMessage("");

                displayMessage("updateDiscoverableMode() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Device Discoverable", discoverableChecked);
            getCommandParameterView(1).setModeText("", "Discoverable Mode Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler updateConnectableMode_Handler = new CommandHandler() {

        boolean connectableChecked;

        @Override
        public void run() {

            int result;

            TextValue timeoutParameter;

            CheckboxValue connectableParameter;

            int timeoutInteger;

            if(deviceManager != null) {

                connectableParameter = getCommandParameterView(0).getValueCheckbox();

                timeoutParameter = getCommandParameterView(1).getValueText();

                try {
                    timeoutInteger = Integer.valueOf(timeoutParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = deviceManager.updateConnectableMode(connectableParameter.value, timeoutInteger);

                displayMessage("");

                displayMessage("updateConnectableMode() result: " + result);

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeCheckbox("Connectable", connectableChecked);
            getCommandParameterView(1).setModeText("", "Connectable Mode Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler updatePairableMode_Handler = new CommandHandler() {

        boolean pairableChecked;

        @Override
        public void run() {

            int result;

            CheckboxValue pairableParameter;

            TextValue timeoutParameter;

            int timeoutInteger;

            if(deviceManager != null) {

                pairableParameter = getCommandParameterView(0).getValueCheckbox();

                timeoutParameter = getCommandParameterView(1).getValueText();

                try {
                    timeoutInteger = Integer.valueOf(timeoutParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = deviceManager.updatePairableMode(pairableParameter.value, timeoutInteger);

                displayMessage("");

                displayMessage("updatePairableMode() result: " + result);

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeCheckbox("Pairable", pairableChecked);
            getCommandParameterView(1).setModeText("", "Pairable Mode Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler queryLocalDeviceIDInformation_Handler = new CommandHandler() {

        @Override
        public void run() {
            DeviceIDInformation result;

            if(deviceManager != null) {

                result = deviceManager.queryLocalDeviceIDInformation();

                displayMessage("");
                if(result != null) {
                    displayMessage("queryLocalDeviceIDInformation() result: SUCCESS");
                    displayMessage("      Vendor ID:      " + result.VendorID);
                    displayMessage("      Product ID:     " + result.ProductID);
                    displayMessage("      Device Version: " + result.DeviceVersion);
                    displayMessage("      USBVendorID:    " + (result.USBVendorID ? "TRUE" : "FALSE"));
                } else {
                    displayMessage("queryLocalDeviceIDInformation() result: FAILED");
                }
                displayMessage("queryLocalDeviceIDInformation() not yet supported.");
            }

        }

        @Override
        public void unselected() {

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }
    };

    private final CommandHandler enableLocalDeviceFeature_Handler = new CommandHandler() {

        String[] LocalDeviceFeatures = {
            "BLUETOOTH_LOW_ENERGY",
            "ANT_PLUS"
        };

        @Override
        public void run() {
            int                result;
            LocalDeviceFeature feature;

            switch(getCommandParameterView(0).getValueSpinner().selectedItem) {
                case 0:
                default:
                    feature = LocalDeviceFeature.BLUETOOTH_LOW_ENERGY;
                    break;
                case 1:
                    feature = LocalDeviceFeature.ANT_PLUS;
            }

            result = deviceManager.enableLocalDeviceFeature(feature);

            displayMessage("");

            displayMessage("enableLocalDeviceFeature() result: " + result);
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Local Device Feature", LocalDeviceFeatures);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }
    };

    private final CommandHandler disableLocalDeviceFeature_Handler = new CommandHandler() {

        String[] LocalDeviceFeatures = {
                "BLUETOOTH_LOW_ENERGY",
                "ANT_PLUS"
            };

            @Override
            public void run() {
                int                result;
                LocalDeviceFeature feature;

                switch(getCommandParameterView(0).getValueSpinner().selectedItem) {
                    case 0:
                    default:
                        feature = LocalDeviceFeature.BLUETOOTH_LOW_ENERGY;
                        break;
                    case 1:
                        feature = LocalDeviceFeature.ANT_PLUS;
                }

                result = deviceManager.disableLocalDeviceFeature(feature);

                displayMessage("");

                displayMessage("disableLocalDeviceFeature() result: " + result);
            }

            @Override
            public void selected() {
                getCommandParameterView(0).setModeSpinner("Local Device Feature", LocalDeviceFeatures);
                getCommandParameterView(1).setModeHidden();
                getCommandParameterView(2).setModeHidden();
                getCommandParameterView(3).setModeHidden();
            }

            @Override
            public void unselected() {
                // TODO Auto-generated method stub
            }
    };

    private final CommandHandler queryActiveLocalDeviceFeature_Handler = new CommandHandler() {

        String[] LocalDeviceFeatures = {
                "BLUETOOTH_LOW_ENERGY",
                "ANT_PLUS"
            };

        @Override
        public void run() {
            LocalDeviceFeature result;

            result = deviceManager.queryActiveLocalDeviceFeature();

            displayMessage("");

            if(result != null)
                displayMessage("queryActiveLocalDeviceFeature() result: " + LocalDeviceFeatures[result.ordinal()]);
            else
                displayMessage("queryActiveLocalDeviceFeature() result: failure");
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }
    };

    private final CommandHandler startDeviceDiscovery_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            TextValue timeoutParameter;

            int timeoutInteger;

            if(deviceManager != null) {

                timeoutParameter = getCommandParameterView(0).getValueText();

                try {
                    timeoutInteger = Integer.valueOf(timeoutParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = deviceManager.startDeviceDiscovery(timeoutInteger);

                displayMessage("");

                displayMessage("startDeviceDiscovery() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Device Discovery Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler stopDeviceDiscovery_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            if(deviceManager != null) {

                result = deviceManager.stopDeviceDiscovery();

                displayMessage("");

                displayMessage("stopDeviceDiscovery() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler queryRemoteDeviceList_Handler = new CommandHandler() {

        String[] remoteDeviceFilters =
        {
                "All Devices",
                "All Connected Devices",
                "All Paired Devices",
                "All Unpaired Devices"
        };

        @Override
        public void run() {

            BluetoothAddress[] remoteDeviceList;

            SpinnerValue remoteDevicefilterParameter;

            RemoteDeviceFilter remoteDeviceFilter;

            TextValue classOfDeviceMaskParameter;

            int classOfDeviceMaskInteger;

            if(deviceManager != null) {

                remoteDevicefilterParameter = getCommandParameterView(0).getValueSpinner();

                switch(remoteDevicefilterParameter.selectedItem)
                {
                   case 0:
                       default:
                       remoteDeviceFilter = RemoteDeviceFilter.ALL_DEVICES;
                       break;
                   case 1:
                       remoteDeviceFilter = RemoteDeviceFilter.CURRENTLY_CONNECTED;
                       break;
                   case 2:
                       remoteDeviceFilter = RemoteDeviceFilter.CURRENTLY_PAIRED;
                       break;
                   case 3:
                       remoteDeviceFilter = RemoteDeviceFilter.CURRENTLY_UNPAIRED;
                       break;
                }

                classOfDeviceMaskParameter = getCommandParameterView(1).getValueText();

                try {
                    classOfDeviceMaskInteger = Integer.parseInt(classOfDeviceMaskParameter.text.toString(),16);
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                remoteDeviceList = deviceManager.queryRemoteDeviceList(remoteDeviceFilter, classOfDeviceMaskInteger);

                if (remoteDeviceList != null)
                {
                    displayMessage("");
                    displayMessage("queryRemoteDeviceList(): ");

                    for (BluetoothAddress remoteDevice: remoteDeviceList)
                    {

                        displayMessage(remoteDevice.toString());

                    }

                }
                else
                {

                    displayMessage("");
                    displayMessage("queryRemoteDeviceList() result: null");

                }

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Remote Device Filter", remoteDeviceFilters);
            getCommandParameterView(1).setModeText("", "Class of Device Mask", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler queryRemoteDeviceProperties_Handler = new CommandHandler() {

        boolean forceUpdateChecked = false;

        @Override
        public void run() {

            BluetoothAddress bluetoothAddress;

            CheckboxValue forceUpdateParameter;

            RemoteDeviceProperties remoteDeviceProperties;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                     showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                     return;
                }

                forceUpdateParameter = getCommandParameterView(0).getValueCheckbox();

                remoteDeviceProperties = deviceManager.queryRemoteDeviceProperties(bluetoothAddress, forceUpdateParameter.value);

                displayMessage("");
                if(remoteDeviceProperties != null) {

                    displayMessage("queryRemoteDeviceProperties()");

                    displayMessage("Device Address  : " + remoteDeviceProperties.deviceAddress.toString());
                    displayMessage("Class of Device : " + Integer.toHexString(remoteDeviceProperties.classOfDevice));
                    displayMessage("Device Name     : " + remoteDeviceProperties.deviceName);
                    displayMessage("RSSI            : " + remoteDeviceProperties.rssi);
                    displayMessage("Transmit power  : " + remoteDeviceProperties.transmitPower);
                    displayMessage("Sniff Interval  : " + remoteDeviceProperties.sniffInterval);

                    if (remoteDeviceProperties.applicationData != null)
                    {

                        displayMessage("Friendly Name   : " + remoteDeviceProperties.applicationData.friendlyName);
                        displayMessage("Application Info: " + remoteDeviceProperties.applicationData.applicationInfo);

                    }

                    for (RemoteDeviceFlags remoteDeviceFlag: remoteDeviceProperties.remoteDeviceFlags)
                    {

                        displayMessage("Remote Device Flag: " + remoteDeviceFlag.toString());

                    }
                }
                else
                {

                    displayMessage("queryRemoteDeviceProperties() result: null");

                }

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeCheckbox("Force Update", forceUpdateChecked);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler queryRemoteDeviceEIRData_Handler = new CommandHandler() {

        @Override
        public void run() {

            Map<EIRDataType, byte[]> result;

            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.queryRemoteDeviceEIRData(bluetoothAddress);

                displayMessage("");
                displayMessage("queryRemoteDeviceEIRData() result: " + ((result != null) ? result.size() : "<null>") + " entries returned.");

                //TODO Display EIR fields

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler updateRemoteDeviceServices_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.updateRemoteDeviceServices(bluetoothAddress);

                displayMessage("");
                displayMessage("updateRemoteDeviceServices() result: " + result);

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler queryRemoteDeviceServicesRaw_Handler = new CommandHandler() {

        @Override
        public void run() {

            byte[] result;
            BluetoothAddress bluetoothAddress;

            if(deviceManager != null)
            {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.queryRemoteDeviceServicesRaw(bluetoothAddress);

                if (result != null)
                {
                    if (result[0] > 0)
                    {
                        displayMessage("");
                        displayMessage("queryRemoteDeviceServicesRaw() No. of Bytes: " + result.length);
                    }
                    else if (result[0] == 0)
                    {
                        displayMessage("");
                        displayMessage("queryRemoteDeviceServicesRaw() result: None Returned");
                    }
                    else
                    {
                        displayMessage("");
                        displayMessage("queryRemoteDeviceServicesRaw() Error: " + result[0]);
                    }
                }
                else
                {
                    displayMessage("");
                    displayMessage("queryRemoteDeviceServicesRaw() result: null");
                }
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler queryRemoteDeviceSupportedServices_Handler = new CommandHandler() {

        @Override
        public void run() {

            UUID[] result;

            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.queryRemoteDeviceSupportedServices(bluetoothAddress);

                if (result != null){

                    displayMessage("");
                    displayMessage("queryRemoteDeviceSupportedServices() UUID: ");

                    for (int index = 0; index < result.length; index++)
                    {

                        displayMessage(index + ") " + result[index].toString());

                    }

                }
                else{

                    displayMessage("");
                    displayMessage("queryRemoteDeviceSupportedServices(): null");

                }

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler addRemoteDevice_Handler = new CommandHandler() {

        @Override
        public void run() {

            int                         result;
            BluetoothAddress            bluetoothAddress;
            TextValue                   classOfDeviceParameter;
            int                         classOfDeviceInteger;
            TextValue                   friendlyNameParameter;
            String                      friendlyNameString;
            TextValue                   applicationInfoParameter;
            int                         applicationInfoInteger;
            RemoteDeviceApplicationData remoteDeviceApplicationData = new RemoteDeviceApplicationData();

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                classOfDeviceParameter = getCommandParameterView(0).getValueText();

                try {
                    classOfDeviceInteger = Integer.parseInt(classOfDeviceParameter.text.toString(),16);
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                friendlyNameParameter = getCommandParameterView(1).getValueText();

                try {
                    friendlyNameString = friendlyNameParameter.text.toString();
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                applicationInfoParameter = getCommandParameterView(2).getValueText();

                try {
                    applicationInfoInteger = Integer.valueOf(applicationInfoParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                remoteDeviceApplicationData.friendlyName = friendlyNameString;
                remoteDeviceApplicationData.applicationInfo = applicationInfoInteger;

                result = deviceManager.addRemoteDevice(bluetoothAddress, classOfDeviceInteger, remoteDeviceApplicationData);

                displayMessage("");
                displayMessage("addRemoteDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Class of Device", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeText("", "Friendly Name", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(2).setModeText("", "Application Information", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler addRemoteLowEnergyDevice_Handler = new CommandHandler() {

        @Override
        public void run() {

            int                         result;
            BluetoothAddress            bluetoothAddress;
            TextValue                   friendlyNameParameter;
            String                      friendlyNameString;
            TextValue                   applicationInfoParameter;
            int                         applicationInfoInteger;
            RemoteDeviceApplicationData remoteDeviceApplicationData = new RemoteDeviceApplicationData();

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                friendlyNameParameter = getCommandParameterView(0).getValueText();

                try {
                    friendlyNameString = friendlyNameParameter.text.toString();
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                applicationInfoParameter = getCommandParameterView(1).getValueText();

                try {
                    applicationInfoInteger = Integer.valueOf(applicationInfoParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                remoteDeviceApplicationData.friendlyName = friendlyNameString;
                remoteDeviceApplicationData.applicationInfo = applicationInfoInteger;

                result = deviceManager.addRemoteLowEnergyDevice(bluetoothAddress, remoteDeviceApplicationData);

                displayMessage("");
                displayMessage("addRemoteLowEnergyDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Friendly Name", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeText("", "Application Information", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler addRemoteDualModeDevice_Handler = new CommandHandler() {

        @Override
        public void run() {

            int                         result;
            BluetoothAddress            bluetoothAddress;
            TextValue                   friendlyNameParameter;
            String                      friendlyNameString;
            TextValue                   applicationInfoParameter;
            int                         applicationInfoInteger;
            RemoteDeviceApplicationData remoteDeviceApplicationData = new RemoteDeviceApplicationData();

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                friendlyNameParameter = getCommandParameterView(0).getValueText();

                try {
                    friendlyNameString = friendlyNameParameter.text.toString();
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                applicationInfoParameter = getCommandParameterView(1).getValueText();

                try {
                    applicationInfoInteger = Integer.valueOf(applicationInfoParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                remoteDeviceApplicationData.friendlyName = friendlyNameString;
                remoteDeviceApplicationData.applicationInfo = applicationInfoInteger;

                result = deviceManager.addRemoteDualModeDevice(bluetoothAddress, remoteDeviceApplicationData);

                displayMessage("");
                displayMessage("addRemoteDualModeDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Friendly Name", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeText("", "Application Information", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler deleteRemoteDevice_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.deleteRemoteDevice(bluetoothAddress);

                displayMessage("");
                displayMessage("deleteRemoteDevice() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler updateRemoteDeviceApplicationData_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            TextValue friendlyNameParameter;

            String friendlyNameString;

            TextValue applicationInfoParameter;

            int applicationInfoInteger;

            RemoteDeviceApplicationData remoteDeviceApplicationData = new RemoteDeviceApplicationData();

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                friendlyNameParameter = getCommandParameterView(0).getValueText();

                try {
                    friendlyNameString = friendlyNameParameter.text.toString();
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                applicationInfoParameter = getCommandParameterView(1).getValueText();

                try {
                    applicationInfoInteger = Integer.valueOf(applicationInfoParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                remoteDeviceApplicationData.friendlyName = friendlyNameString;
                remoteDeviceApplicationData.applicationInfo = applicationInfoInteger;

                result = deviceManager.updateRemoteDeviceApplicationData(bluetoothAddress, remoteDeviceApplicationData);

                displayMessage("");
                displayMessage("updateRemoteDeviceApplicationData() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Friendly Name", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeText("", "Application Information", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler deleteRemoteDevices_Handler = new CommandHandler() {

        String[] remoteDeviceFilters =
        {
                "All Devices",
                "All Connected Devices",
                "All Paired Devices",
                "All Unpaired Devices"
        };

        @Override
        public void run() {

            int result;

            SpinnerValue remoteDevicefilterParameter;

            RemoteDeviceFilter remoteDeviceFilter;

            if(deviceManager != null) {

                remoteDevicefilterParameter = getCommandParameterView(0).getValueSpinner();

                switch(remoteDevicefilterParameter.selectedItem)
                {
                   case 0:
                       default:
                       remoteDeviceFilter = RemoteDeviceFilter.ALL_DEVICES;
                       break;

                   case 1:
                       remoteDeviceFilter = RemoteDeviceFilter.CURRENTLY_CONNECTED;
                       break;

                   case 2:
                       remoteDeviceFilter = RemoteDeviceFilter.CURRENTLY_PAIRED;
                       break;

                   case 3:
                       remoteDeviceFilter = RemoteDeviceFilter.CURRENTLY_UNPAIRED;
                       break;

                }

                result = deviceManager.deleteRemoteDevices(remoteDeviceFilter);

                displayMessage("");
                displayMessage("deleteRemoteDevices() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Remote Device Filter", remoteDeviceFilters);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler pairWithRemoteDevice_Handler = new CommandHandler() {

        boolean forcePairChecked = false;

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            CheckboxValue    forcePairParameter;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                forcePairParameter = getCommandParameterView(0).getValueCheckbox();

                result = deviceManager.pairWithRemoteDevice(bluetoothAddress, forcePairParameter.value);

                displayMessage("");
                displayMessage("pairWithRemoteDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Force Pairing:", forcePairChecked);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler cancelPairWithRemoteDevice_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.cancelPairWithRemoteDevice(bluetoothAddress);

                displayMessage("");
                displayMessage("cancelPairWithRemoteDevice() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler unpairRemoteDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            int result;
            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.unpairRemoteDevice(bluetoothAddress);

                displayMessage("");
                displayMessage("unpairRemoteDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler authenticateRemoteDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            int result;
            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.authenticateRemoteDevice(bluetoothAddress);

                displayMessage("");
                displayMessage("authenticateRemoteDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler encryptRemoteDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            int result;
            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.encryptRemoteDevice(bluetoothAddress);

                displayMessage("");
                displayMessage("encryptRemoteDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler connectWithRemoteDevice_Handler = new CommandHandler() {

        String[] connectFlagLabels = {
                "Authentication",
                "Encryption"
        };

        boolean[] connectFlagValues = new boolean[] {false, false};

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            ChecklistValue connectionFlagsParameter;

            EnumSet<ConnectFlags> connectionFlags;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                connectionFlagsParameter = getCommandParameterView(0).getValueChecklist();

                connectionFlags = EnumSet.noneOf(ConnectFlags.class);

                if(connectionFlagsParameter.checkedItems[0]) {
                    connectionFlags.add(ConnectFlags.AUTHENTICATE);
                }

                if(connectionFlagsParameter.checkedItems[1]) {
                    connectionFlags.add(ConnectFlags.ENCRYPT);
                }

                result = deviceManager.connectWithRemoteDevice(bluetoothAddress, connectionFlags);

                displayMessage("");
                displayMessage("connectWithRemoteDevice() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeChecklist("Connect Flags", connectFlagLabels, connectFlagValues);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler disconnectRemoteDevice_Handler = new CommandHandler() {

        boolean forceDisconnectChecked = false;

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            CheckboxValue forceDisconnectParameter;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                forceDisconnectParameter = getCommandParameterView(0).getValueCheckbox();

                result = deviceManager.disconnectRemoteDevice(bluetoothAddress, forceDisconnectParameter.value);

                displayMessage("");
                displayMessage("disconnectRemoteDevice() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Force Disconnect:", forceDisconnectChecked);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler setRemoteDeviceLinkActive_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.setRemoteDeviceLinkActive(bluetoothAddress);

                displayMessage("");
                displayMessage("setRemoteDeviceLinkActive() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler sendPinCode_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            TextValue pinCodeParameter;

            String pinCodeString;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                pinCodeParameter = getCommandParameterView(0).getValueText();

                try {
                    pinCodeString = pinCodeParameter.text.toString();
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = deviceManager.sendPinCode(bluetoothAddress, pinCodeString.getBytes());

                displayMessage("");
                displayMessage("sendPinCode() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Pin Code", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler sendUserConfirmation_Handler = new CommandHandler() {

        boolean acceptChecked = true;

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            CheckboxValue acceptParameter;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                acceptParameter = getCommandParameterView(0).getValueCheckbox();

                result = deviceManager.sendUserConfirmation(bluetoothAddress, acceptParameter.value);

                displayMessage("");
                displayMessage("sendUserConfirmation() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Send Confirmation", acceptChecked);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler sendPasskey_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            TextValue passkeyParameter;

            int passkeyInteger;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                passkeyParameter = getCommandParameterView(0).getValueText();

                try {
                    passkeyInteger = Integer.valueOf(passkeyParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = deviceManager.sendPasskey(bluetoothAddress, passkeyInteger);

                displayMessage("");

                displayMessage("sendPasskey() result: " + result);

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeText("", "Pass Key", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler sendOutOfBandData_Handler = new CommandHandler() {

        @Override
        public void run() {

            int              result;
            byte[]           hash;
            byte[]           randomizer;
            BluetoothAddress bluetoothAddress;
            TextValue        simplePairingHashParameter;
            String           simplePairingHashString;
            TextValue        simplePairingRandomizerParameter;
            String           simplePairingRandomizerString;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                simplePairingHashParameter = getCommandParameterView(0).getValueText();

                try {
                    simplePairingHashString = simplePairingHashParameter.text.toString();
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                simplePairingRandomizerParameter = getCommandParameterView(0).getValueText();

                try {
                    simplePairingRandomizerString = simplePairingRandomizerParameter.text.toString();
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                if(simplePairingHashString != null)
                    hash = Arrays.copyOfRange(simplePairingHashString.getBytes(), 0, 16);
                else
                    hash = new byte[16];

                if(simplePairingRandomizerString != null)
                    randomizer = Arrays.copyOfRange(simplePairingRandomizerString.getBytes(), 0, 16);
                else
                    randomizer = new byte[16];


                result = deviceManager.sendOutOfBandData(bluetoothAddress, hash, randomizer);

                displayMessage("");
                displayMessage("sendOutOfBandData() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Simple Pairing Hash", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeText("", "Simple Pairing Randomizer", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler sendIOCapabilities_Handler = new CommandHandler() {

        String[] IOCapabilities = {
                "Display Only",
                "Display Yes-No",
                "Keyboard Only",
                "No Input - No Output"
                 };

        String[] bondingTypes = {
                "No Bonding",
                "Dedicated Bonding",
                "General Bonding"
                 };

        boolean mitmRequiredChecked = false;

        boolean outOfBandDataChecked = false;

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            SpinnerValue ioCapabilityParameter;

            IOCapability ioCapability;

            SpinnerValue bondingTypeParameter;

            BondingType bondingType;

            CheckboxValue mitmRequiredParameter;

            CheckboxValue outOfBandDataParameter;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                ioCapabilityParameter = getCommandParameterView(0).getValueSpinner();

                switch(ioCapabilityParameter.selectedItem)
                {
                   case 0:
                       default:
                       ioCapability = IOCapability.DISPLAY_ONLY;
                       break;
                   case 1:
                       ioCapability = IOCapability.DISPLAY_YES_NO;
                       break;
                   case 2:
                       ioCapability = IOCapability.KEYBOARD_ONLY;
                       break;
                   case 3:
                       ioCapability = IOCapability.NO_INPUT_NO_OUTPUT;
                       break;
                }

                bondingTypeParameter = getCommandParameterView(1).getValueSpinner();

                switch(bondingTypeParameter.selectedItem)
                {
                   case 0:
                       default:
                       bondingType = BondingType.NO_BONDING;
                       break;
                   case 1:
                       bondingType = BondingType.DEDICATED_BONDING;
                       break;
                   case 2:
                       bondingType = BondingType.GENERAL_BONDING;
                       break;
                }

                mitmRequiredParameter = getCommandParameterView(2).getValueCheckbox();

                outOfBandDataParameter = getCommandParameterView(3).getValueCheckbox();

                result = deviceManager.sendIOCapabilities(bluetoothAddress, ioCapability, outOfBandDataParameter.value, mitmRequiredParameter.value, bondingType);

                displayMessage("");
                displayMessage("sendIOCapabilities() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Input-Output Capability", IOCapabilities);
            getCommandParameterView(1).setModeSpinner("Discoverable Mode", bondingTypes);
            getCommandParameterView(2).setModeCheckbox("MITM Protection Required", mitmRequiredChecked);
            getCommandParameterView(3).setModeCheckbox("Out Of Band Data Present", outOfBandDataChecked);
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler startLowEnergyDeviceScan_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            TextValue timeoutParameter;

            int timeoutInteger;

            if(deviceManager != null) {

                timeoutParameter = getCommandParameterView(0).getValueText();

                try {
                    timeoutInteger = Integer.valueOf(timeoutParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = deviceManager.startLowEnergyDeviceScan(timeoutInteger);

                displayMessage("");

                displayMessage("startLowEnergyDeviceScan() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Device Scan Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler stopLowEnergyDeviceScan_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            if(deviceManager != null) {

                result = deviceManager.stopLowEnergyDeviceScan();

                displayMessage("");

                displayMessage("stopLowEnergyDeviceScan() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler startLowEnergyAdvertising_Handler = new CommandHandler() {

        private final String flags[] = {
            "User Public Address",
            "Discoverable",
            "Connectable",
            "Advertise Device Name",
            "Advertise Tx Power",
            "Advertise Appearance"
        };

        private final boolean values[] = {
            false,
            false,
            false,
            false,
            false,
            false
        };

        @Override
        public void run() {

            int result;

            TextValue timeoutParameter;
            ChecklistValue flagsParameter;

            int timeoutInteger;

            if(deviceManager != null) {

                timeoutParameter = getCommandParameterView(0).getValueText();
                flagsParameter = getCommandParameterView(1).getValueChecklist();

                EnumSet<AdvertisingFlags> flags = EnumSet.noneOf(AdvertisingFlags.class);

                for(AdvertisingFlags flag : AdvertisingFlags.values()) {
                    if(flagsParameter.checkedItems[flag.ordinal()])
                        flags.add(flag);
                }

                try {
                    timeoutInteger = Integer.valueOf(timeoutParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                Map<AdvertisingDataType, byte[]> data;
                data = new HashMap<AdvertisingDataType, byte[]>(1);

                byte[] iBeaconAd = new byte[] {

                    (byte)0x4C, (byte)0x00,
                    (byte)0x02,
                    (byte)0x15,
                    (byte)0xE2, (byte)0xC5, (byte)0x6D, (byte)0xB5, (byte)0xDF, (byte)0xFB, (byte)0x48, (byte)0xD2, (byte)0xB0, (byte)0x60, (byte)0xD0, (byte)0xF5, (byte)0xA7, (byte)0x10, (byte)0x96, (byte)0xE0,
                    (byte)0x00, (byte)0x01,
                    (byte)0x00, (byte)0x01,
                    (byte)0xC5
                };

                //We don't have a service to advertise in DEVM, so set some test data as example
                data.put(AdvertisingDataType.MANUFACTURER_SPECIFIC, iBeaconAd);

                result = deviceManager.startLowEnergyAdvertising(flags, timeoutInteger, data);

                displayMessage("");

                displayMessage("startLowEnergyAdvertising() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Advertising Duration", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeChecklist("Advertising Flags", flags, values);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler stopLowEnergyAdvertising_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            if(deviceManager != null) {

                result = deviceManager.stopLowEnergyAdvertising();

                displayMessage("");

                displayMessage("stopLowEnergyAdvertising() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler queryRemoteLowEnergyDeviceProperties_Handler = new CommandHandler() {

        boolean forceUpdateChecked = false;

        @Override
        public void run() {

            BluetoothAddress bluetoothAddress;

            CheckboxValue forceUpdateParameter;

            RemoteDeviceProperties remoteDeviceProperties;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                     showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                     return;
                }

                forceUpdateParameter = getCommandParameterView(0).getValueCheckbox();

                remoteDeviceProperties = deviceManager.queryRemoteLowEnergyDeviceProperties(bluetoothAddress, forceUpdateParameter.value);

                displayMessage("");
                if(remoteDeviceProperties != null) {

                    displayMessage("queryRemoteDeviceProperties()");

                    displayMessage("Device Address  : " + remoteDeviceProperties.deviceAddress.toString());
                    displayMessage("Class of Device : " + Integer.toHexString(remoteDeviceProperties.classOfDevice));
                    displayMessage("Device Name     : " + remoteDeviceProperties.deviceName);
                    displayMessage("RSSI            : " + remoteDeviceProperties.rssi);
                    displayMessage("Transmit power  : " + remoteDeviceProperties.transmitPower);
                    displayMessage("Sniff Interval  : " + remoteDeviceProperties.sniffInterval);

                    if (remoteDeviceProperties.applicationData != null)
                    {

                        displayMessage("Friendly Name   : " + remoteDeviceProperties.applicationData.friendlyName);
                        displayMessage("Application Info: " + remoteDeviceProperties.applicationData.applicationInfo);

                    }

                    for (RemoteDeviceFlags remoteDeviceFlag: remoteDeviceProperties.remoteDeviceFlags)
                    {

                        displayMessage("Remote Device Flag: " + remoteDeviceFlag.toString());

                    }

                    displayMessage("Low Energy Address Type: " );//public AddressType                 lowEnergyAddressType;
                    displayMessage("Low Energy RSSI:  :" + remoteDeviceProperties.lowEnergyRSSI);
                    displayMessage("Low Energy Transmit Power:" + remoteDeviceProperties.lowEnergyTransmitPower);
                 //XXX Clarify?
                    displayMessage("Low Energy Appearance:" + remoteDeviceProperties.deviceAppearance);
                    displayMessage("Prior Resolvable Address: " + ((remoteDeviceProperties.priorResolvableAddress != null) ? remoteDeviceProperties.priorResolvableAddress.toString() : "none"));
                    //XXX

                }
                else
                {

                    displayMessage("queryRemoteDeviceProperties() result: null");

                }

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeCheckbox("Force Update", forceUpdateChecked);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler queryRemoteLowEnergyDeviceAdvertisingData_Handler = new CommandHandler() {

        @Override
        public void run() {
            Map<AdvertisingDataType, byte[]> result;
            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                //XXX
                result = deviceManager.queryRemoteLowEnergyDeviceAdvertisingData(bluetoothAddress);

                displayMessage("");
                displayMessage("queryRemoteLowEnergyDeviceAdvertisingData() result: " + ((result != null) ? result.size() : "<null>") + " entries returned.");

                //TODO Display AD fields
            }
        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler queryRemoteLowEnergyDeviceScanResponseData_Handler = new CommandHandler() {

        @Override
        public void run() {

            Map<AdvertisingDataType, byte[]> result;

            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                //XXX
                result = deviceManager.queryRemoteLowEnergyDeviceScanResponseData(bluetoothAddress);

                displayMessage("");
                displayMessage("queryRemoteLowEnergyDeviceScanResponseData() result: " + ((result != null) ? result.size() : "<null>") + " entries returned.");

                //TODO Display AD fields

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler updateRemoteLowEnergyDeviceServices_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.updateRemoteLowEnergyDeviceServices(bluetoothAddress);

                displayMessage("");
                displayMessage("updateRemoteLowEnergyDeviceServices() result: " + result);

            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler queryRemoteLowEnergyDeviceServicesRaw_Handler = new CommandHandler() {

        @Override
        public void run() {

            byte[] result;
            BluetoothAddress bluetoothAddress;

            if(deviceManager != null)
            {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.queryRemoteLowEnergyDeviceServicesRaw(bluetoothAddress);

                if (result != null)
                {
                    displayMessage("");
                    displayMessage("queryRemoteLowEnergyDeviceServicesRaw() No. of Bytes: " + result.length);
                }
                else
                {
                    displayMessage("");
                    displayMessage("queryRemoteLowEnergyDeviceServicesRaw() result: null");
                }
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler queryRemoteDeviceSupportedLowEnergyServices_Handler = new CommandHandler() {

        @Override
        public void run() {

            UUID[] result;

            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.queryRemoteDeviceSupportedLowEnergyServices(bluetoothAddress);

                if (result != null){

                    displayMessage("");
                    displayMessage("queryRemoteDeviceSupportedLowEnergyServices() UUID: ");

                    for (int index = 0; index < result.length; index++)
                    {

                        displayMessage(index + ") " + result[index].toString());

                    }

                }
                else{

                    displayMessage("");
                    displayMessage("queryRemoteDeviceSupportedLowEnergyServices(): null");

                }

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler pairWithRemoteLowEnergyDevice_Handler = new CommandHandler() {

        boolean forcePairChecked = false;

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            CheckboxValue    forcePairParameter;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                forcePairParameter = getCommandParameterView(0).getValueCheckbox();

                result = deviceManager.pairWithRemoteLowEnergyDevice(bluetoothAddress, forcePairParameter.value);

                displayMessage("");
                displayMessage("pairWithRemoteLowEnergyDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Force Pairing:", forcePairChecked);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler unpairRemoteLowEnergyDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            int result;
            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.unpairRemoteLowEnergyDevice(bluetoothAddress);

                displayMessage("");
                displayMessage("unpairRemoteLowEnergyDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler authenticateRemoteLowEnergyDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            int result;
            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.authenticateRemoteLowEnergyDevice(bluetoothAddress);

                displayMessage("");
                displayMessage("authenticateRemoteLowEnergyDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler encryptRemoteLowEnergyDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            int result;
            BluetoothAddress bluetoothAddress;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = deviceManager.encryptRemoteLowEnergyDevice(bluetoothAddress);

                displayMessage("");
                displayMessage("encryptRemoteLowEnergyDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler connectWithRemoteLowEnergyDevice_Handler = new CommandHandler() {

        String[] connectFlagLabels = {
                "Authentication",
                "Encryption"
        };

        boolean[] connectFlagValues = new boolean[] {false, false};

        @Override
        public void run() {

            int result;
            BluetoothAddress bluetoothAddress;
            ChecklistValue connectionFlagsParameter;
            EnumSet<ConnectFlags> connectionFlags;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                connectionFlagsParameter = getCommandParameterView(0).getValueChecklist();
                connectionFlags = EnumSet.noneOf(ConnectFlags.class);

                if(connectionFlagsParameter.checkedItems[0]) {
                    connectionFlags.add(ConnectFlags.AUTHENTICATE);
                }

                if(connectionFlagsParameter.checkedItems[1]) {
                    connectionFlags.add(ConnectFlags.ENCRYPT);
                }

                result = deviceManager.connectWithRemoteLowEnergyDevice(bluetoothAddress, connectionFlags);

                displayMessage("");
                displayMessage("connectWithRemoteLowEnergyDevice() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeChecklist("Connect Flags", connectFlagLabels, connectFlagValues);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler disconnectRemoteLowEnergyDevice_Handler = new CommandHandler() {

        boolean forceDisconnectChecked = false;

        @Override
        public void run() {
            int result;
            BluetoothAddress bluetoothAddress;
            CheckboxValue forceDisconnectParameter;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                forceDisconnectParameter = getCommandParameterView(0).getValueCheckbox();

                result = deviceManager.disconnectRemoteLowEnergyDevice(bluetoothAddress, forceDisconnectParameter.value);

                displayMessage("");
                displayMessage("disconnectRemoteLowEnergyDevice() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Force Disconnect:", forceDisconnectChecked);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler sendLowEnergyUserConfirmation_Handler = new CommandHandler() {

        boolean acceptChecked = true;

        @Override
        public void run() {

            int result;

            BluetoothAddress bluetoothAddress;

            CheckboxValue acceptParameter;

            if(deviceManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                acceptParameter = getCommandParameterView(0).getValueCheckbox();

                result = deviceManager.sendLowEnergyUserConfirmation(bluetoothAddress, acceptParameter.value);

                displayMessage("");
                displayMessage("sendLowEnergyUserConfirmation() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Send Confirmation", acceptChecked);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler sendLowEnergyPasskey_Handler = new CommandHandler() {

        @Override
        public void run() {
            int result;
            BluetoothAddress bluetoothAddress;
            TextValue passkeyParameter;
            int passkeyInteger;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                passkeyParameter = getCommandParameterView(0).getValueText();

                try {
                    passkeyInteger = Integer.valueOf(passkeyParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = deviceManager.sendLowEnergyPasskey(bluetoothAddress, passkeyInteger);

                displayMessage("");
                displayMessage("sendPasskey() result: " + result);
            }
        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeText("", "Pass Key", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler sendLowEnergyOutOfBandData_Handler = new CommandHandler() {

        @Override
        public void run() {

            int              result;
            byte[]           encryptionKey;
            BluetoothAddress bluetoothAddress;
            TextValue        simplePairingKeyParameter;
            String           simplePairingKeyString;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                simplePairingKeyParameter = getCommandParameterView(0).getValueText();

                try {
                    simplePairingKeyString = simplePairingKeyParameter.text.toString();
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                if(simplePairingKeyString != null)
                    encryptionKey = Arrays.copyOfRange(simplePairingKeyString.getBytes(), 0, 16);
                else
                    encryptionKey = new byte[16];

                result = deviceManager.sendLowEnergyOutOfBandData(bluetoothAddress, encryptionKey);

                displayMessage("");
                displayMessage("sendLowEnergyOutOfBandData() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Simple Pairing Encryption Key", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
        }

        @Override
        public void unselected() {

        }

    };

    private final CommandHandler sendLowEnergyIOCapabilities_Handler = new CommandHandler() {

        String[] IOCapabilities = {
                "Display Only",
                "Display Yes-No",
                "Keyboard Only",
                "No Input - No Output",
                "Keyboard and Display"
                 };

        String[] bondingTypes = {
                "No Bonding",
                "Bonding",
                 };

        boolean mitmRequiredChecked = false;

        boolean outOfBandDataChecked = false;

        @Override
        public void run() {
            int result;
            BluetoothAddress bluetoothAddress;
            SpinnerValue ioCapabilityParameter;
            LowEnergyIOCapability ioCapability;
            SpinnerValue bondingTypeParameter;
            LowEnergyBondingType bondingType;
            CheckboxValue mitmRequiredParameter;
            CheckboxValue outOfBandDataParameter;

            if(deviceManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                ioCapabilityParameter = getCommandParameterView(0).getValueSpinner();

                switch(ioCapabilityParameter.selectedItem)
                {
                   case 0:
                       default:
                       ioCapability = LowEnergyIOCapability.DISPLAY_ONLY;
                       break;
                   case 1:
                       ioCapability = LowEnergyIOCapability.DISPLAY_YES_NO;
                       break;
                   case 2:
                       ioCapability = LowEnergyIOCapability.KEYBOARD_ONLY;
                       break;
                   case 3:
                       ioCapability = LowEnergyIOCapability.NO_INPUT_NO_OUTPUT;
                       break;
                   case 4:
                       ioCapability = LowEnergyIOCapability.KEYBOARD_DISPLAY;
                       break;
                }

                bondingTypeParameter = getCommandParameterView(1).getValueSpinner();

                switch(bondingTypeParameter.selectedItem)
                {
                   case 0:
                       default:
                       bondingType = LowEnergyBondingType.NO_BONDING;
                       break;
                   case 1:
                       bondingType = LowEnergyBondingType.BONDING;
                       break;
                }

                mitmRequiredParameter = getCommandParameterView(2).getValueCheckbox();

                outOfBandDataParameter = getCommandParameterView(3).getValueCheckbox();

                result = deviceManager.sendLowEnergyIOCapabilities(bluetoothAddress, ioCapability, outOfBandDataParameter.value, mitmRequiredParameter.value, bondingType);

                displayMessage("");
                displayMessage("sendLowEnergyIOCapabilities() result: " + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Input-Output Capability", IOCapabilities);
            getCommandParameterView(1).setModeSpinner("Discoverable Mode", bondingTypes);
            getCommandParameterView(2).setModeCheckbox("MITM Protection Required", mitmRequiredChecked);
            getCommandParameterView(3).setModeCheckbox("Out Of Band Data Present", outOfBandDataChecked);
        }

        @Override
        public void unselected() {

        }

    };

    private final Command[] commandList = new Command[] {

        new Command("Manufacturer ID to String", convertManufacturerIDToString_Handler),
        new Command("Acquire Lock", acquireLock_Handler),
        new Command("Release Lock", releaseLock_Handler),
        new Command("Query Device Power State", queryDevicePowerState_Handler),
        new Command("Acknowledge Device Powering Down", acknowledgeDevicePoweringDown_Handler),
        new Command("Query Local Device Properties", queryLocalDeviceProperties_Handler),
        new Command("Update Class Device", updateClassOfDevice_Handler),
        new Command("Update Device Name", updateDeviceName_Handler),
        new Command("Update Discoverable Mode", updateDiscoverableMode_Handler),
        new Command("Update Connectable Mode", updateConnectableMode_Handler),
        new Command("Update Pairable Mode", updatePairableMode_Handler),
        new Command("Query Local Device ID Information", queryLocalDeviceIDInformation_Handler),
        new Command("Enable Local Device Feature", enableLocalDeviceFeature_Handler),
        new Command("Disable Local Device Feature", disableLocalDeviceFeature_Handler),
        new Command("Query Active Local Device Feature", queryActiveLocalDeviceFeature_Handler),
        new Command("Start Device Discovery", startDeviceDiscovery_Handler),
        new Command("Stop Device Discovery", stopDeviceDiscovery_Handler),
        new Command("Query Remote Device List", queryRemoteDeviceList_Handler),
        new Command("Query Remote Device Properties", queryRemoteDeviceProperties_Handler),
        new Command("Query Remote Device EIR Data", queryRemoteDeviceEIRData_Handler),
        new Command("Update Remote Device Services", updateRemoteDeviceServices_Handler),
        new Command("Query Remote Device Services Raw", queryRemoteDeviceServicesRaw_Handler),
        new Command("Query Remote Device Supported Services", queryRemoteDeviceSupportedServices_Handler),
        new Command("Add Remote Device", addRemoteDevice_Handler),
        new Command("Add Remote Low Energy Device", addRemoteLowEnergyDevice_Handler),
        new Command("Add Remote Dual Mode Device", addRemoteDualModeDevice_Handler),
        new Command("Delete Remote Device", deleteRemoteDevice_Handler),
        new Command("Update Remote Device Application Data", updateRemoteDeviceApplicationData_Handler),
        new Command("Delete Remote Devices", deleteRemoteDevices_Handler),
        new Command("Pair With Remote Device", pairWithRemoteDevice_Handler),
        new Command("Cancel Pair With Remote Device", cancelPairWithRemoteDevice_Handler),
        new Command("Unpair Remote Device", unpairRemoteDevice_Handler),
        new Command("Authenticate Remote Device", authenticateRemoteDevice_Handler),
        new Command("Encrypt Link To Remote Device", encryptRemoteDevice_Handler),
        new Command("Connect With Remote Device", connectWithRemoteDevice_Handler),
        new Command("Disconnect Remote Device", disconnectRemoteDevice_Handler),
        new Command("Set Remote Device Link to Active", setRemoteDeviceLinkActive_Handler),
        new Command("Send Pin Code", sendPinCode_Handler),
        new Command("Send User Confirmation", sendUserConfirmation_Handler),
        new Command("Send Pass Key", sendPasskey_Handler),
        new Command("Send Out Of Band Data", sendOutOfBandData_Handler),
        new Command("Send Input-Output Capabilities", sendIOCapabilities_Handler),
        new Command("Start Low Energy Device Scan", startLowEnergyDeviceScan_Handler),
        new Command("Stop Low Energy Device Scan", stopLowEnergyDeviceScan_Handler),
        new Command("Start Low Energy Advertising", startLowEnergyAdvertising_Handler),
        new Command("Stop Low Energy Advertising", stopLowEnergyAdvertising_Handler),
        new Command("Query Remote Low Energy Device Properties", queryRemoteLowEnergyDeviceProperties_Handler),
        new Command("Query Remote Low Energy Device Advertising Data", queryRemoteLowEnergyDeviceAdvertisingData_Handler),
        new Command("Query Remote Low Energy Device Scan Response Data", queryRemoteLowEnergyDeviceScanResponseData_Handler),
        new Command("Update Remote Low Energy Device Services", updateRemoteLowEnergyDeviceServices_Handler),
        new Command("Query Remote Low Energy Device Services Raw", queryRemoteLowEnergyDeviceServicesRaw_Handler),
        new Command("Query Remote Device Supported Low Energy Services", queryRemoteDeviceSupportedLowEnergyServices_Handler),
        new Command("Pair With Remote Low Energy Device", pairWithRemoteLowEnergyDevice_Handler),
        new Command("Unpair Remote Low Energy Device", unpairRemoteLowEnergyDevice_Handler),
        new Command("Authenticate Remote Low Energy Device", authenticateRemoteLowEnergyDevice_Handler),
        new Command("Encrypt Link To Remote Low Energy Device", encryptRemoteLowEnergyDevice_Handler),
        new Command("Connect With Remote Low Energy Device", connectWithRemoteLowEnergyDevice_Handler),
        new Command("Disconnect Remote Low Energy Device", disconnectRemoteLowEnergyDevice_Handler),
        new Command("Send Low Energy User Confirmation", sendLowEnergyUserConfirmation_Handler),
        new Command("Send Low Energy Pass Key", sendLowEnergyPasskey_Handler),
        new Command("Send Low Energy Out Of Band Data", sendLowEnergyOutOfBandData_Handler),
        new Command("Send Low Energy Input-Output Capabilities", sendLowEnergyIOCapabilities_Handler),

    };

}

