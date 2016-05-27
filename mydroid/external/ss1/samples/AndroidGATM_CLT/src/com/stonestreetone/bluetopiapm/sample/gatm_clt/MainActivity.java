/*
 * < MainActivity.java >
 * Copyright 2011 - 2014 Stonestreet One. All Rights Reserved.
 *
 * Generic Attribute Profile Client Sample for Stonestreet One Bluetooth
 * Protocol Stack Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.gatm_clt;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.text.InputType;
import android.util.Log;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.DEVM;
import com.stonestreetone.bluetopiapm.DEVM.AdvertisingDataType;
import com.stonestreetone.bluetopiapm.DEVM.AdvertisingFlags;
import com.stonestreetone.bluetopiapm.DEVM.ConnectFlags;
import com.stonestreetone.bluetopiapm.DEVM.EventCallback;
import com.stonestreetone.bluetopiapm.DEVM.LocalDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.LocalPropertyField;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFilter;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceFlags;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.DEVM.RemotePropertyField;
import com.stonestreetone.bluetopiapm.GATM.AttributeProtocolErrorType;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicDefinition;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicDescriptor;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicProperty;
import com.stonestreetone.bluetopiapm.GATM.ConnectionInformation;
import com.stonestreetone.bluetopiapm.GATM.ConnectionType;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager;
import com.stonestreetone.bluetopiapm.GATM.GenericAttributeClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.GATM.RequestErrorType;
import com.stonestreetone.bluetopiapm.GATM.ServiceDefinition;
import com.stonestreetone.bluetopiapm.GATM.ServiceInformation;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.CheckboxValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.ChecklistValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.SpinnerValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.TextValue;
import com.stonestreetone.bluetopiapm.sample.util.SS1SampleActivity;
import com.stonestreetone.bluetopiapm.sample.util.Utils;

/**
 *  GATM Generic Attribute Client Sample
 */
public class MainActivity extends SS1SampleActivity {

    private static final String LOG_TAG         = "GATM_CLT_Sample";

    /* package */ DEVM                          deviceManager;
    /* package */ GenericAttributeClientManager genericAttributeClientManager;

    @Override
    protected boolean profileEnable() {

        synchronized(this) {
            try {
                deviceManager = new DEVM(deviceEventCallback);
                genericAttributeClientManager = new GenericAttributeClientManager(genericAttributeClientEventCallback);
                return true;
            } catch(Exception e) {
                /*
                 * BluetopiaPM server couldn't be contacted. This should never
                 * happen if Bluetooth was successfully enabled.
                 */
                showToast(this, R.string.errorBTPMServerNotReachableToastMessage);
                return false;
            }
        }
    }

    @Override
    protected void profileDisable() {
        synchronized(MainActivity.this) {
            if(genericAttributeClientManager != null) {

                genericAttributeClientManager.dispose();
                genericAttributeClientManager = null;

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
        return 3;
    }

    private final EventCallback deviceEventCallback = new EventCallback() {

        @Override
        public void devicePoweredOnEvent() { }

        @Override
        public void devicePoweringOffEvent(int poweringOffTimeout) { }

        @Override
        public void devicePoweredOffEvent() { }

        @Override
        public void localDevicePropertiesChangedEvent(LocalDeviceProperties localProperties, EnumSet<LocalPropertyField> changedFields) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.localDevicePropertiesChangedEventLabel));

            displayMessage("");
            displayMessage(sb);

            for(LocalPropertyField localPropertyField: changedFields) {
                switch(localPropertyField) {
                    case DISCOVERABLE_MODE:
                        if(localProperties.discoverableMode)
                            displayMessage("Device is Discoverable");
                        else
                            displayMessage("Device is Not Discoverable");

                        displayMessage("Discoverable Mode Timeout: " + localProperties.discoverableModeTimeout);
                        break;
                    case CLASS_OF_DEVICE:
                    case DEVICE_NAME:
                    case CONNECTABLE_MODE:
                    case PAIRABLE_MODE:
                    case DEVICE_FLAGS:
                        break;
                }
            }
        }

        @Override
        public void remoteDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) { }

        @Override
        public void remoteLowEnergyDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) {
            StringBuilder sb = new StringBuilder();

            sb.append("remoteLowEnergyDeviceServicesStatusEvent: ");
            sb.append(remoteDevice.toString());
            if(success)
                sb.append(" Success");
            else
                sb.append(" Failure");

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void discoveryStartedEvent() { }

        @Override
        public void discoveryStoppedEvent() { }

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
            if(success)
                sb.append("Success ").append(remoteDevice.toString());
            else
                sb.append("Failure ");

            sb.append(remoteDevice.toString());
            displayMessage("");
            displayMessage(sb);
            displayMessage("Authentication Status: " + (authenticationStatus & 0x7FFFFFFF) + (((authenticationStatus & 0x80000000) != 0) ? " (LE)" : "(BR/EDR)"));
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
        public void remoteDeviceDeletedEvent(BluetoothAddress remoteDevice) { }

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
        public void deviceAdvertisingStarted() {
            displayMessage("");
            displayMessage("deviceAdvertisingStartedEvent");

        }

        @Override
        public void deviceAdvertisingStopped() {
            displayMessage("");
            displayMessage("deviceAdvertisingStoppedEvent");

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
    };

    private final ClientEventCallback genericAttributeClientEventCallback = new ClientEventCallback() {

        @Override
        public void connectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int MTU) {

            displayMessage("");
            StringBuilder sb;

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.connectedEventLabel));
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.connectionTypeLabel)).append(": ");
            sb.append(connectionType);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.MTULabel)).append(": ");
            sb.append(MTU);
            displayMessage(sb);
        }

        @Override
        public void disconnectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress) {

            displayMessage("");
            StringBuilder sb;

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.disconnectedEventLabel));

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.connectionTypeLabel)).append(": ");
            sb.append(connectionType);
            displayMessage(sb);
        }

        @Override
        public void connectionMTUUpdateEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int MTU) {

            displayMessage("");
            StringBuilder sb;

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.connectionMTUUpdateEventLabel)).append(": ");
            sb.append(MTU);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.connectionTypeLabel)).append(": ");
            sb.append(connectionType);
            displayMessage(sb);
         }

        @Override
        public void handleValueEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, boolean handleValueIndication, int attributeHandle, byte[] attributeValue) {

            displayMessage("");
            StringBuilder sb;

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.handleValueEventLabel));
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.connectionTypeLabel)).append(": ");
            sb.append(connectionType);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.handleValueIndicationLabel)).append(": ");
            sb.append(handleValueIndication);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.attributeHandleLabel)).append(": ");
            sb.append("0x").append(Integer.toHexString(attributeHandle)).append(" (").append(attributeHandle).append(")");
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.attributeValueLabel)).append(": ");
            displayMessage(sb);
            
            if(attributeValue != null) {

                sb = new StringBuilder();
                char[] hexCharacters = Utils.bytesToCharArray(attributeValue);
                for(int i = 0; i < hexCharacters.length; i = i + 2) {
                
                    sb.append("0x").append(hexCharacters[i]).append(hexCharacters[i + 1]);
                    
                    if((i + 1) < hexCharacters.length)
                        sb.append(" ");
                }
                
                displayMessage(sb);            
                                
                boolean printCharacters = true;

                sb = new StringBuilder();
                for(int i = 0; i < attributeValue.length && printCharacters; i++) {
                
                    if((attributeValue[i] >= 0x20) && (attributeValue[i] <= 0x7F))
                        sb.append((char)attributeValue[i]);
                    else
                        printCharacters = false;
                }
                
                if(printCharacters)
                    displayMessage(sb);
                
                displayMessage("");
            
            } else {
                
                displayMessage("attributeValue is null");           
                displayMessage("");
            }  
         }

        @Override
        public void readResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle, boolean isFinal, byte[] value) {

            displayMessage("");
            StringBuilder sb;

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.readResponseEventLabel));
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.connectionTypeLabel)).append(": ");
            sb.append(connectionType);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.transactionIDLabel)).append(": ");
            sb.append(transactionID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.handleLabel)).append(": ");
            sb.append("0x").append(Integer.toHexString(handle)).append(" (").append(handle).append(")");
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.isFinalLabel)).append(": ");
            sb.append(isFinal);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.attributeValueLabel)).append(": ");
            displayMessage(sb);
            
            if(value != null) {

                sb = new StringBuilder();
                char[] hexCharacters = Utils.bytesToCharArray(value);
                for(int i = 0; i < hexCharacters.length; i = i + 2) {
                
                    sb.append("0x").append(hexCharacters[i]).append(hexCharacters[i + 1]);
                    
                    if((i + 1) < hexCharacters.length)
                        sb.append(" ");
                }
                
                displayMessage(sb);            
                                
                boolean printCharacters = true;

                sb = new StringBuilder();
                for(int i = 0; i < value.length && printCharacters; i++) {
                
                    if((value[i] >= 0x20) && (value[i] <= 0x7F))
                        sb.append((char)value[i]);
                    else
                        printCharacters = false;
                }
                
                if(printCharacters)
                    displayMessage(sb);
                
                displayMessage("");
            
            } else {
                
                displayMessage("attributeValue is null");           
                displayMessage("");
            }  
        }

        @Override
        public void writeResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle) {

            displayMessage("");
            StringBuilder sb;

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.writeResponseEventLabel)).append(": ");
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.connectionTypeLabel)).append(": ");
            sb.append(connectionType);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.transactionIDLabel)).append(": ");
            sb.append(transactionID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.handleLabel)).append(": ");
            sb.append("0x").append(Integer.toHexString(handle)).append(" (").append(handle).append(")");
            displayMessage(sb);
        }

        @Override
        public void errorResponseEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int transactionID, int handle, RequestErrorType requestErrorType, AttributeProtocolErrorType attributeProtoccolErrorType) {

            displayMessage("");
            StringBuilder sb;

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.errorResponseEventLabel)).append(": ");
            sb.append(requestErrorType);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.connectionTypeLabel)).append(": ");
            sb.append(connectionType);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.transactionIDLabel)).append(": ");
            sb.append(transactionID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.handleLabel)).append(": ");
            sb.append(handle);
            displayMessage(sb);

            if(requestErrorType == RequestErrorType.ERROR_RESPONSE) {

                sb = new StringBuilder();
                sb.append(resourceManager.getString(R.string.attributeProtoccolErrorTypeLabel)).append(": ");
                sb.append(attributeProtoccolErrorType);
                displayMessage(sb);
            }
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

                //We don't have a service to advertise in DEVM, so set some test data as example
                data.put(AdvertisingDataType.MANUFACTURER_SPECIFIC, new String("test data").getBytes());

                result = deviceManager.startLowEnergyAdvertising(flags, timeoutInteger, data);

                displayMessage("");

                displayMessage("startLowEnergyAdvertising() result: " + result);

            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Device Scan Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeChecklist("Advertising Flags", flags, values);
            getCommandParameterView(2).setModeHidden();
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
                if(result != null) {
                    for(AdvertisingDataType type:result.keySet()) {
                        
                        StringBuilder sb = new StringBuilder(result.get(type).length * 2);
                        for(byte b:result.get(type))
                            sb.append(String.format("%02x",b & 0xFF));
                        displayMessage("type is " + type + " value is " + sb.toString());
                    }

                }
            }
        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
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
                if(result != null) {
                    for(AdvertisingDataType type:result.keySet()) {
                        
                        StringBuilder sb = new StringBuilder(result.get(type).length * 2);
                        for(byte b:result.get(type))
                            sb.append(String.format("%02x",b & 0xFF));
                        displayMessage("type is " + type + " value is " + sb.toString());
                    }
                }
            }

        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
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
                    
                    StringBuilder sb = new StringBuilder(result.length * 2);
                    for(byte b:result)
                        sb.append(String.format("%02x",b & 0xFF));
                    displayMessage(sb.toString());
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
        }

        @Override
        public void unselected() {

        }
    };

    private final CommandHandler queryNumberOfConnectedDevices_Handler = new CommandHandler() {


        @Override
        public void run() {

            int result;

            if(genericAttributeClientManager != null) {

                result = genericAttributeClientManager.queryNumberOfConnectedDevices();

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.queryNumberOfConnectedDevicesLabel) + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }
    };
    private final CommandHandler queryConnectedDevices_Handler = new CommandHandler() {

        @Override
        public void run() {

            ConnectionInformation[] connectionInformation;
            TextValue maximumEntriesParameter;

            if(genericAttributeClientManager != null) {
                connectionInformation = genericAttributeClientManager.queryConnectedDevices();

                displayMessage("");

                displayMessage(resourceManager.getString(R.string.queryConnectedDevicesLabel) );

                for(ConnectionInformation connectionInfo : connectionInformation) {

                    StringBuilder sb = new StringBuilder();
                    sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
                    sb.append(connectionInfo.remoteDeviceAddress);
                    displayMessage(sb);

                    sb = new StringBuilder();
                    sb.append(resourceManager.getString(R.string.connectionTypeLabel)).append(": ");
                    sb.append(connectionInfo.connectionType);
                    displayMessage(sb);

                    displayMessage("");
                }

                displayMessage("");
            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }

    };

    private final CommandHandler queryRemoteDeviceServices_Handler = new CommandHandler() {

        @Override
        public void run() {

            BluetoothAddress    remoteDeviceAddress;
            ServiceDefinition[] services;

            if(genericAttributeClientManager != null) {

                if((remoteDeviceAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                services = genericAttributeClientManager.queryRemoteDeviceServices(remoteDeviceAddress);

                displayMessage("");
                displayMessage("queryRemoteDeviceServices() result: " + (services == null ? "error" : "success"));

                if(services != null) {
                    displayMessage("Results: ");

                    for(ServiceDefinition service : services) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Service UUID:          ").append(service.serviceDetails.uuid.toString()).append('\n');
                        sb.append("Service ID:          ").append(Integer.toString(service.serviceDetails.serviceID)).append('\n');
                        sb.append("Starting Handle:       ").append("0x").append(Integer.toHexString(service.serviceDetails.startHandle))
                          .append(" (").append(service.serviceDetails.startHandle).append(")").append('\n');
                        sb.append("Ending Handle:         ").append("0x").append(Integer.toHexString(service.serviceDetails.endHandle))
                          .append(" (").append(service.serviceDetails.endHandle).append(")").append('\n');

                        sb.append("Included Services:\n");
                        if(service.includedServices.length > 0) {
                            for(ServiceInformation includedService : service.includedServices) {
                                sb.append("    * UUID:            ").append(includedService.uuid.toString()).append('\n');
                                sb.append("      Starting Handle: ").append("0x").append(Integer.toHexString(includedService.startHandle))
                                  .append(" (").append(includedService.startHandle).append(")").append('\n');
                                sb.append("      Ending Handle:   ").append("0x").append(Integer.toHexString(includedService.endHandle))
                                  .append(" (").append(includedService.endHandle).append(")").append('\n');
                            }
                        } else {
                            sb.append("     -none-\n");
                        }

                        sb.append("Characteristics:\n");
                        if(service.characteristics.length > 0) {
                            for(CharacteristicDefinition characteristic : service.characteristics) {
                                sb.append("    * UUID:            ").append(characteristic.uuid.toString()).append('\n');
                                sb.append("      Handle:          ").append("0x").append(Integer.toHexString(characteristic.handle))
                                  .append(" (").append(characteristic.handle).append(")").append('\n');

                                sb.append("      Properties:\n");
                                if(characteristic.properties.isEmpty()) {
                                    sb.append("           -none-\n");
                                } else {
                                    for(CharacteristicProperty prop : characteristic.properties)
                                        sb.append("           ").append(prop.name()).append('\n');
                                }

                                sb.append("      Descriptors:\n");
                                if(characteristic.descriptors.length > 0) {
                                    for(CharacteristicDescriptor descriptor : characteristic.descriptors) {
                                        sb.append("          * UUID:      ").append(descriptor.uuid.toString()).append('\n');
                                        sb.append("            Handle:    ").append("0x").append(Integer.toHexString(descriptor.handle))
                                          .append(" (").append(descriptor.handle).append(")").append('\n');
                                    }
                                } else {
                                    sb.append("           -none-\n");
                                }
                            }
                        } else {
                            sb.append("     -none-\n");
                        }

                        displayMessage(sb);
                        displayMessage("");
                    }
                }
            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }
    };

    private final CommandHandler readValue_Handler = new CommandHandler() {

        boolean readAll = false;

        @Override
        public void run() {

            int              result;
            BluetoothAddress remoteDeviceAddress;
            int              attributeHandle;
            int              offset;
            boolean          readAll;
            TextValue        attributeHandleParameter;
            TextValue        offsetParameter;
            CheckboxValue    readAllParameter;

            if(genericAttributeClientManager != null) {

                if((remoteDeviceAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                attributeHandleParameter = getCommandParameterView(0).getValueText();

                try {
                    attributeHandle = Integer.valueOf(attributeHandleParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                offsetParameter = getCommandParameterView(1).getValueText();

                try {
                    offset = Integer.valueOf(offsetParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                readAllParameter = getCommandParameterView(2).getValueCheckbox();
                readAll = readAllParameter.value;

                result = genericAttributeClientManager.readValue(remoteDeviceAddress, attributeHandle, offset, readAll);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.readValueLabel) + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Attribute Handle", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeText("", "Offset", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeCheckbox("Read All:", readAll);
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };
    private final CommandHandler writeValue_Handler = new CommandHandler() {


        @Override
        public void run() {

            int              result;
            BluetoothAddress remoteDeviceAddress;
            int              attributeHandle;
            TextValue        attributeHandleParameter;
            TextValue        attributeDataParameter;
            String           value;
            byte[]           valueBytes;

            if(genericAttributeClientManager != null) {

                if((remoteDeviceAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                attributeHandleParameter = getCommandParameterView(0).getValueText();

                try {
                    attributeHandle = Integer.valueOf(attributeHandleParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                attributeDataParameter = getCommandParameterView(1).getValueText();

                value = attributeDataParameter.text.toString();

                if(getCommandParameterView(2).getValueCheckbox().value) {
                    if(value.startsWith("0x"))
                        value = value.substring(2);

                    if((valueBytes = Utils.hexToByteArray(value)) != null)
                        result = genericAttributeClientManager.writeValue(remoteDeviceAddress, attributeHandle, valueBytes);
                    else
                        result = -1;
                }
                else
                    result = genericAttributeClientManager.writeValue(remoteDeviceAddress, attributeHandle,value.getBytes());

                displayMessage("");

                if(result == -1)
                    displayMessage(resourceManager.getString(R.string.writeValueLabel) + " Invalid hex string");
                else
                    displayMessage(resourceManager.getString(R.string.writeValueLabel) + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Attribute Handle", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeText("", "Value", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(2).setModeCheckbox("Parse As Hex", false);
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler writeValueWithoutResponse_Handler = new CommandHandler() {

        @Override
        public void run() {

            int              result;
            BluetoothAddress remoteDeviceAddress;
            int              attributeHandle;
            TextValue        attributeHandleParameter;
            TextValue        attributeDataParameter;
            String           value;
            byte[]           valueBytes;

            if(genericAttributeClientManager != null) {

                if((remoteDeviceAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                attributeHandleParameter = getCommandParameterView(0).getValueText();

                try {
                    attributeHandle = Integer.valueOf(attributeHandleParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                attributeDataParameter = getCommandParameterView(1).getValueText();

                value = attributeDataParameter.text.toString();
                if(getCommandParameterView(2).getValueCheckbox().value) {
                    if(value.startsWith("0x"))
                        value = value.substring(2);

                    if((valueBytes = Utils.hexToByteArray(value)) != null)
                        result = genericAttributeClientManager.writeValueWithoutResponse(remoteDeviceAddress, attributeHandle, valueBytes);
                    else
                        result = -1;
                }
                else
                    result = genericAttributeClientManager.writeValueWithoutResponse(remoteDeviceAddress, attributeHandle,value.getBytes());

                displayMessage("");

                if(result == -1)
                    displayMessage(resourceManager.getString(R.string.writeValueLabel) + " Invalid hex string");
                else
                    displayMessage(resourceManager.getString(R.string.writeValueLabel) + result);
            }
        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeText("", "Attribute Handle", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeText("", "Value", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(2).setModeCheckbox("Parse As Hex", false);
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final Command[] commandList = new Command[] {
            new Command("Update Discoverable Mode", updateDiscoverableMode_Handler),
            new Command("Start Low Energy Device Scan", startLowEnergyDeviceScan_Handler),
            new Command("Stop Low Energy Device Scan", stopLowEnergyDeviceScan_Handler),
            new Command("Start Low Energy Advertising", startLowEnergyAdvertising_Handler),
            new Command("Stop Low Energy Advertising", stopLowEnergyAdvertising_Handler),
            new Command("Query Remote Device List", queryRemoteDeviceList_Handler),
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

            new Command("Query Number of Connected Devices", queryNumberOfConnectedDevices_Handler),
            new Command("Query Connected Devices", queryConnectedDevices_Handler),
            new Command("Query Remote Device Services", queryRemoteDeviceServices_Handler),
            new Command("Read Value", readValue_Handler),
            new Command("Write Value", writeValue_Handler),
            new Command("Write Value Without Response", writeValueWithoutResponse_Handler)
    };
}


