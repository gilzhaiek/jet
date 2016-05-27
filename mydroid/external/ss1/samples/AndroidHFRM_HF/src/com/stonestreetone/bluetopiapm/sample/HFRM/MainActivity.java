/*
 * < MainActivity.java >
 * Copyright 2011 - 2014 Stonestreet One. All Rights Reserved.
 *
 * Hands-free Profile Sample for Stonestreet One Bluetooth Protocol Stack
 * Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.HFRM;

import java.util.EnumSet;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.text.InputType;
import android.util.Log;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.BluetopiaPMException;
import com.stonestreetone.bluetopiapm.HFRM;
import com.stonestreetone.bluetopiapm.HFRM.AudioGatewayServerManager.MultipartyFeatures;
import com.stonestreetone.bluetopiapm.HFRM.AudioGatewayServerManager.SupportedFeatures;
import com.stonestreetone.bluetopiapm.HFRM.CallHoldMultipartyHandlingType;
import com.stonestreetone.bluetopiapm.HFRM.CallState;
import com.stonestreetone.bluetopiapm.HFRM.ConfigurationIndicatorEntry;
import com.stonestreetone.bluetopiapm.HFRM.ConnectionFlags;
import com.stonestreetone.bluetopiapm.HFRM.ConnectionStatus;
import com.stonestreetone.bluetopiapm.HFRM.CurrentCallListEntry;
import com.stonestreetone.bluetopiapm.HFRM.DisconnectedReason;
import com.stonestreetone.bluetopiapm.HFRM.ExtendedResult;
import com.stonestreetone.bluetopiapm.HFRM.HandsFreeServerManager;
import com.stonestreetone.bluetopiapm.HFRM.HandsFreeServerManager.HandsFreeEventCallback;
import com.stonestreetone.bluetopiapm.HFRM.HandsFreeServerManager.LocalConfiguration;
import com.stonestreetone.bluetopiapm.HFRM.IncomingConnectionFlags;
import com.stonestreetone.bluetopiapm.HFRM.NetworkMode;
import com.stonestreetone.bluetopiapm.HFRM.SubscriberNumberInformation;
import com.stonestreetone.bluetopiapm.ServerNotReachableException;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.CheckboxValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.ChecklistValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.SpinnerValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.TextValue;
import com.stonestreetone.bluetopiapm.sample.util.SS1SampleActivity;

/**
 * Primary Activity for this sample application.
 *
 * @author Greg Hensley
 */
public class MainActivity extends SS1SampleActivity {

    private static final String LOG_TAG = "HFRM_HF_Sample";

    /*package*/ HFRM.HandsFreeServerManager handsFreeServerManager;

    @Override
    protected boolean profileEnable() {
        synchronized(this) {
            if(handsFreeServerManager != null) {
                handsFreeServerManager.dispose();
            }

            try {
                // TODO The request for a "controlling" manager (first parameter) is not guaranteed. Handle the case where it fails.
                handsFreeServerManager = new HandsFreeServerManager(true, handsFreeEventCallbackHandler);
                showToast(this, "Registered as the Controller Manager");
            } catch(BluetopiaPMException e) {
                handsFreeServerManager = null;
            }

            if(handsFreeServerManager == null) {
                try {
                    handsFreeServerManager = new HandsFreeServerManager(false, handsFreeEventCallbackHandler);
                    showToast(this, "Registered as a non-controller Manager");
                } catch(ServerNotReachableException e) {
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
            if(handsFreeServerManager != null) {
                handsFreeServerManager.dispose();
                handsFreeServerManager = null;
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


    /*
     * Duplicate constants from non-public android.bluetooth.BluetootHeadset API
     */
    // State
    public static final int STATE_ERROR        = -1;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING   = 1;
    public static final int STATE_CONNECTED    = 2;

    /*
     * This notifies AudioService that the Bluetooth Hands Free device is
     * connected and that the audio path may be enabled in the future.
     */
    private void broadcastConnectionStateChange(BluetoothAddress remoteDeviceAddress, int newState, int previousState) {
        BluetoothDevice androidBluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(remoteDeviceAddress.toString());

        Intent intent = new Intent("android.bluetooth.headset.action.STATE_CHANGED");           // BluetoothHeadset.ACTION_STATE_CHANGED
        intent.putExtra("android.bluetooth.headset.extra.PREVIOUS_STATE", previousState);       // BluetoothHeadset.EXTRA_PREVIOUS_STATE
        intent.putExtra("android.bluetooth.headset.extra.STATE", newState);                     // BluetoothHeadset.EXTRA_STATE

        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, androidBluetoothDevice);                  // BluetoothDevice.EXTRA_DEVICE

        sendBroadcast(intent, android.Manifest.permission.BLUETOOTH);
    }

    private final HandsFreeEventCallback handsFreeEventCallbackHandler = new HandsFreeEventCallback() {

        @Override
        public void voiceRecognitionIndicationEvent(BluetoothAddress remoteDeviceAddress, boolean voiceRecognitionActive) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.voiceRecognitionIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(voiceRecognitionActive);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void speakerGainIndicationEvent(BluetoothAddress remoteDeviceAddress, int speakerGain) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.speakerGainIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Gain: ").append(speakerGain);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void microphoneGainIndicationEvent(BluetoothAddress remoteDeviceAddress, int microphoneGain) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.microphoneGainIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Gain: ").append(microphoneGain);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void incomingConnectionRequestEvent(BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.incomingConnectionRequestEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void incomingCallStateIndicationEvent(BluetoothAddress remoteDeviceAddress, CallState callState) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.incomingCallStateIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(callState);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void disconnectedEvent(BluetoothAddress remoteDeviceAddress, DisconnectedReason reason) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.disconnectedEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(reason);

            displayMessage("");
            displayMessage(sb);

            broadcastConnectionStateChange(remoteDeviceAddress, STATE_DISCONNECTED, STATE_CONNECTED);
        }

        @Override
        public void connectionStatusEvent(BluetoothAddress remoteDeviceAddress, ConnectionStatus connectionStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.connectionStatusEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(connectionStatus);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void connectedEvent(BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.connectedEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);

            broadcastConnectionStateChange(remoteDeviceAddress, STATE_CONNECTED, STATE_DISCONNECTED);
        }

        @Override
        public void audioDisconnectedEvent(BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();
            AudioManager  audioManager;

            sb.append(resourceManager.getString(R.string.audioDisconnectedEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);

            if((audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE)) != null) {
                audioManager.setMode(AudioManager.MODE_NORMAL);
                audioManager.setBluetoothScoOn(false);
                displayMessage("Disabled Bluetooth SCO audio routing in AudioManager.");
            }
        }

        @Override
        public void audioDataEvent(BluetoothAddress remoteDeviceAddress, byte[] audioData) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.audioDataEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(audioData.length).append(" bytes");

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void audioConnectionStatusEvent(BluetoothAddress remoteDeviceAddress, boolean successful) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.audioConnectionStatusEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Successful: ").append(successful);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void audioConnectedEvent(BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();
            AudioManager  audioManager;

            sb.append(resourceManager.getString(R.string.audioConnectedEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);

            if((audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE)) != null) {
                displayMessage("Setting audio mode to IN_CALL");
                audioManager.setMode(AudioManager.MODE_IN_CALL);

                displayMessage("Enabling Bluetooth SCO audio routing");
                audioManager.setBluetoothScoOn(true);
            }
        }

        @Override
        public void voiceTagRequestConfirmationEvent(BluetoothAddress remoteDeviceAddress, String phoneNumber) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.voiceTagRequestConfirmationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(phoneNumber);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void subscriberNumberInformationConfirmationEvent(BluetoothAddress remoteDeviceAddress, SubscriberNumberInformation subscriberNumber) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.subscriberNumberInformationConfirmationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Number Format: ").append(subscriberNumber.numberFormat);
            sb.append(", Phone Number: ").append(subscriberNumber.phoneNumber);
            sb.append(", Service Type: ").append(subscriberNumber.serviceType);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void serviceLevelConnectionEstablishedEvent(BluetoothAddress remoteDeviceAddress, EnumSet<SupportedFeatures> remoteSupportedFeatures, EnumSet<MultipartyFeatures> remoteCallHoldMultipartySupport) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.serviceLevelConnectionEstablishedEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());

            sb.append(",\n    Supported Features:");
            for(SupportedFeatures feature : remoteSupportedFeatures) {
                sb.append(" ").append(feature);
            }

            sb.append(",\n    Multiparty Features:");
            for(MultipartyFeatures feature : remoteCallHoldMultipartySupport) {
                sb.append(" ").append(feature);
            }

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void ringIndicationEvent(BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.ringIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void responseHoldStatusConfirmationEvent(BluetoothAddress remoteDeviceAddress, CallState responseHoldStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.responseHoldStatusConfirmationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(responseHoldStatus);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void networkOperatorSelectionConfirmationEvent(BluetoothAddress remoteDeviceAddress, NetworkMode mode, String operator) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.networkOperatorSelectionConfirmationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Mode: ").append(mode);
            sb.append(", Operator: ").append(operator);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void incomingCallStateConfirmationEvent(BluetoothAddress remoteDeviceAddress, CallState callState) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.incomingCallStateConfirmationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(callState);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void inBandRingToneSettingIndicationEvent(BluetoothAddress remoteDeviceAddress, boolean enabled) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.inBandRingToneSettingIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Enabled: ").append(enabled);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void currentCallsListConfirmationEvent(BluetoothAddress remoteDeviceAddress, CurrentCallListEntry currentCall) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.currentCallsListConfirmationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(",\n    Index: ").append(currentCall.getIndex());
            sb.append(",\n    Direction: ").append(currentCall.getCallDirection());
            sb.append(",\n    Status: ").append(currentCall.getCallStatus());
            sb.append(",\n    Mode: ").append(currentCall.getCallMode());
            sb.append(",\n    Multiparty:").append(currentCall.isMultiparty());
            sb.append(",\n    Format: ").append(currentCall.getNumberFormat());
            sb.append(",\n    Phone Number: ").append(currentCall.getPhoneNumber());

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void controlIndicatorStatusIndicationEvent(BluetoothAddress remoteDeviceAddress, String name, int value, int rangeMinimum, int rangeMaximum) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.controlIndicatorStatusIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(name).append(" = ").append(value);
            sb.append(" ([").append(rangeMinimum).append(",").append(rangeMaximum).append("])");

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void controlIndicatorStatusIndicationEvent(BluetoothAddress remoteDeviceAddress, String name, boolean value) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.controlIndicatorStatusIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(name).append(" = ").append(value);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void controlIndicatorStatusConfirmationEvent(BluetoothAddress remoteDeviceAddress, String name, int value, int rangeMinimum, int rangeMaximum) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.controlIndicatorStatusConfirmationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(name).append(" = ").append(value);
            sb.append(" ([").append(rangeMinimum).append(",").append(rangeMaximum).append("])");

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void controlIndicatorStatusConfirmationEvent(BluetoothAddress remoteDeviceAddress, String name, boolean value) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.controlIndicatorStatusConfirmationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(name).append(" = ").append(value);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void commandResultEvent(BluetoothAddress remoteDeviceAddress, ExtendedResult resultType, int resultValue) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.commandResultEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(resultType);

            if(resultType == ExtendedResult.RESULT_CODE) {
                sb.append(" = ").append(resultValue);
            }

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void callWaitingNotificationIndicationEvent(BluetoothAddress remoteDeviceAddress, String phoneNumber) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.callWaitingNotificationIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(phoneNumber);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void callLineIdentificationNotificationIndicationEvent(BluetoothAddress remoteDeviceAddress, String phoneNumber) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.callLineIdentificationNotificationIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(phoneNumber);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void callHoldMultipartySupportConfirmationEvent(BluetoothAddress remoteDeviceAddress, EnumSet<MultipartyFeatures> callHoldSupportMask) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.callHoldMultipartySupportConfirmationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Features:");

            if(callHoldSupportMask != null) {
                if(!callHoldSupportMask.isEmpty()) {
                    for(MultipartyFeatures feature : callHoldSupportMask) {
                        sb.append(" ").append(feature);
                    }
                } else {
                    sb.append(" NONE - Feature bitmask is valid but empty");
                }
            } else {
                sb.append(" NONE - Feature bitmask is not valid");
            }

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void arbitraryResponseIndicationEvent(BluetoothAddress remoteDeviceAddress, String responseData) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.arbitraryResponseIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(responseData);

            displayMessage("");
            displayMessage(sb);
        }
    };

    private final CommandHandler connectDevice_Handler = new CommandHandler() {

        String[] connectionFlagLabels = {
                "Authentication",
                "Encryption"
        };

        boolean[] connectionFlagValues = new boolean[] {false, false};

        boolean waitForConnectionChecked = false;

        @Override
        public void run() {
            int                      result;
            TextValue                portNumberParameter;
            ChecklistValue           connectionFlagsParameter;
            CheckboxValue            waitForConnectionParameter;
            BluetoothAddress         bluetoothAddress;
            int                      remotePort;
            EnumSet<ConnectionFlags> connectionFlags;

            if(handsFreeServerManager != null) {
                portNumberParameter = getCommandParameterView(0).getValueText();
                connectionFlagsParameter = getCommandParameterView(1).getValueChecklist();
                waitForConnectionParameter = getCommandParameterView(2).getValueCheckbox();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    remotePort = Integer.valueOf(portNumberParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                connectionFlags = EnumSet.noneOf(ConnectionFlags.class);
                if(connectionFlagsParameter.checkedItems[0]) {
                    connectionFlags.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
                }
                if(connectionFlagsParameter.checkedItems[1]) {
                    connectionFlags.add(ConnectionFlags.REQUIRE_ENCRYPTION);
                }

                result = handsFreeServerManager.connectRemoteDevice(bluetoothAddress, remotePort, connectionFlags, waitForConnectionParameter.value);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.connectDeviceCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Port number", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeChecklist("Connection Flags", connectionFlagLabels, connectionFlagValues);
            getCommandParameterView(2).setModeCheckbox("Wait for connection:", waitForConnectionChecked);
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler disconnectDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                int result;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.disconnectDevice(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.disconnectDeviceCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler connectionRequestResponse_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            CheckboxValue    acceptConnParameter;
            boolean          acceptConnection;

            if(handsFreeServerManager != null) {
                acceptConnParameter = getCommandParameterView(0).getValueCheckbox();

                acceptConnection = acceptConnParameter.value;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.connectionRequestResponse(bluetoothAddress, acceptConnection);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.connectionRequestResponseCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Accept Connection", false);
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
            BluetoothAddress[] devices;

            if(handsFreeServerManager != null) {
                devices = handsFreeServerManager.queryConnectedDevices();

                if(devices != null) {
                    displayMessage("");

                    if(devices.length > 0) {
                        displayMessage("Connected devices (" + devices.length + "):");

                        for(BluetoothAddress bdaddr : devices) {
                            displayMessage("    " + bdaddr);
                        }
                    } else {
                        displayMessage("No devices connected");
                    }
                } else {
                    displayMessage("");
                    displayMessage(resourceManager.getString(R.string.queryConnectedDevicesCommandName) + ": Error");
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

    private final CommandHandler queryLocalConfiguration_Handler = new CommandHandler() {

        @Override
        public void run() {
            LocalConfiguration config;

            if(handsFreeServerManager != null) {
                config = handsFreeServerManager.queryLocalConfiguration();

                if(config != null) {
                    StringBuilder sb = new StringBuilder();

                    sb.append("Local Configuration:");
                    sb.append("\n     Incoming Connection Flags:");

                    for(IncomingConnectionFlags flag : config.getIncomingConnectionFlags()) {
                        sb.append(" ").append(flag);
                    }

                    sb.append("\n     Supported Features:");

                    for(HandsFreeServerManager.SupportedFeatures feature : config.getSupportedFeatures()) {
                        sb.append(" ").append(feature);
                    }

                    sb.append("\n     Network Type: ").append(config.getNetworkType());

                    sb.append("\n     Additional Indicators:");

                    for(ConfigurationIndicatorEntry indicator : config.getAdditionalIndicators()) {
                        sb.append(" ").append(indicator);
                    }

                    displayMessage(sb.toString());
                } else {
                    displayMessage("");
                    displayMessage(resourceManager.getString(R.string.queryLocalConfigurationCommandName) + ": Error");
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

    private final CommandHandler changeIncomingConnectionFlags_Handler = new CommandHandler() {
        private final String[] flags = {
                "Require Authorization",
                "Require Authentication",
                "Require Encryption"
        };

        private final boolean[] values = {
                false,
                false,
                false
        };

        @Override
        public void run() {
            int                              result;
            ChecklistValue                   incomingFlagParameter;
            EnumSet<IncomingConnectionFlags> flagSet;

            if(handsFreeServerManager != null) {

                incomingFlagParameter = getCommandParameterView(0).getValueChecklist();

                flagSet = EnumSet.noneOf(IncomingConnectionFlags.class);
                if(incomingFlagParameter.checkedItems[0]) {
                    flagSet.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
                }
                if(incomingFlagParameter.checkedItems[1]) {
                    flagSet.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                }
                if(incomingFlagParameter.checkedItems[2]) {
                    flagSet.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);
                }

                result = handsFreeServerManager.changeIncomingConnectionFlags(flagSet);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.changeIncomingConnectionFlagsCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeChecklist("Incoming Connection Flags", flags, values);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler disableRemoteSoundEnhancement_Handler = new CommandHandler() {

        @Override
        public void run() {
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                int result;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.disableRemoteEchoCancellationNoiseReduction(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.disableRemoteSoundEnhancementCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler setRemoteVoiceRecognitionActivation_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            CheckboxValue    voiceRecogParameter;

            if(handsFreeServerManager != null) {
                voiceRecogParameter = getCommandParameterView(0).getValueCheckbox();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.setRemoteVoiceRecognitionActivation(bluetoothAddress, voiceRecogParameter.value);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.setRemoteVoiceRecognitionActivationCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Voice Recognition Active", false);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler setSpeakerGain_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            TextValue        gainParameter;
            int              speakerGain;

            if(handsFreeServerManager != null) {
                gainParameter = getCommandParameterView(0).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    speakerGain = Integer.valueOf(gainParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = handsFreeServerManager.setRemoteSpeakerGain(bluetoothAddress, speakerGain);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.setSpeakerGainCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Speaker Gain", (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler setMicrophoneGain_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            TextValue        gainParameter;
            int              microphoneGain;

            if(handsFreeServerManager != null) {
                gainParameter = getCommandParameterView(0).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    microphoneGain = Integer.valueOf(gainParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = handsFreeServerManager.setRemoteMicrophoneGain(bluetoothAddress, microphoneGain);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.setMicrophoneGainCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Microphone Gain", (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler registerAudioData_Handler = new CommandHandler() {

        @Override
        public void run() {
            int result;

            if(handsFreeServerManager != null) {
                result = handsFreeServerManager.registerSCODataProvider();

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.registerAudioDataCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler unregisterAudioData_Handler = new CommandHandler() {

        @Override
        public void run() {
            if(handsFreeServerManager != null) {
                handsFreeServerManager.releaseSCODataProvider();

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.unregisterAudioDataCommandName) + ": Success");
            }
        }

        @Override
        public void unselected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void selected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler setupSCOAudioConnection_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.setupSCOAudioConnection(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.setupSCOAudioConnectionCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }
    };

    private final CommandHandler releaseSCOAudioConnection_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.releaseSCOAudioConnection(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.releaseSCOAudioConnectionCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }
    };

    private final CommandHandler sendAudioData_Handler = new CommandHandler() {

        @Override
        public void run() {
            // FIXME
            showToast(MainActivity.this, "This command is not currently supported by the sample app.");
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();

            displayMessage("The \"Send Audio Data\" command is not currently supported in the sample app.");
        }
    };

    private final CommandHandler queryRemoteIndicatorStatus_Handler = new CommandHandler() {

        @Override
        public void run() {
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                int result;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.queryRemoteControlIndicatorStatus(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.queryRemoteIndicatorStatusCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler enableRemoteIndicatorNotification_Handler = new CommandHandler() {

        @Override
        public void run() {
            BluetoothAddress bluetoothAddress;
            CheckboxValue    enableNotifParameter;

            if(handsFreeServerManager != null) {
                int result;

                enableNotifParameter = getCommandParameterView(0).getValueCheckbox();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.enableRemoteIndicatorEventNotification(bluetoothAddress, enableNotifParameter.value);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.enableRemoteIndicatorNotificationCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Enable Event Notification", false);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler queryRemoteCallHoldSupport_Handler = new CommandHandler() {

        @Override
        public void run() {
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                int result;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.queryRemoteCallHoldingMultipartyServiceSupport(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.queryRemoteCallHoldSupportCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler sendCallHoldSelection_Handler = new CommandHandler() {
        private final String[] spinnerStrings = {
                "Add held call to conversation",
                "Connect two calls and disconnect",
                "Hold all active and accept the other",
                "Private consultation",
                "Release all active, accept incoming",
                "Release all held calls",
                "Release specified index"
        };

        @Override
        public void run() {
            int                            result;
            BluetoothAddress               bluetoothAddress;
            SpinnerValue                   handlingTypeParameter;
            TextValue                      indexParameter;
            CallHoldMultipartyHandlingType handlingType;
            int                            index;

            if(handsFreeServerManager != null) {
                handlingTypeParameter = getCommandParameterView(0).getValueSpinner();
                indexParameter = getCommandParameterView(1).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                switch(handlingTypeParameter.selectedItem) {
                    case 0:
                        handlingType = CallHoldMultipartyHandlingType.ADD_A_HELD_CALL_TO_CONVERSATION;
                        break;
                    case 1:
                        handlingType = CallHoldMultipartyHandlingType.CONNECT_TWO_CALLS_AND_DISCONNECT;
                        break;
                    case 2:
                        handlingType = CallHoldMultipartyHandlingType.PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER;
                        break;
                    case 3:
                        handlingType = CallHoldMultipartyHandlingType.PRIVATE_CONSULTATION_MODE;
                        break;
                    case 4:
                        handlingType = CallHoldMultipartyHandlingType.RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL;
                        break;
                    case 5:
                        handlingType = CallHoldMultipartyHandlingType.RELEASE_ALL_HELD_CALLS;
                        break;
                    case 6:
                        handlingType = CallHoldMultipartyHandlingType.RELEASE_SPECIFIED_CALL_INDEX;
                        break;
                    default:
                        //TODO Complain
                        return;
                }

                try {
                    index = Integer.valueOf(indexParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = handsFreeServerManager.sendCallHoldingMultipartySelection(bluetoothAddress, handlingType, index);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.sendCallHoldSelectionCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Call Hold Handling Type", spinnerStrings);
            getCommandParameterView(1).setModeText("", "Index", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler enableRemoteCallWaitingNotification_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            CheckboxValue    enableNotifParameter;

            if(handsFreeServerManager != null) {
                enableNotifParameter = getCommandParameterView(0).getValueCheckbox();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.enableRemoteCallWaitingNotification(bluetoothAddress, enableNotifParameter.value);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.enableRemoteCallWaitingNotificationCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Enable Notification", false);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler enableRemoteCallerIDNotification_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            CheckboxValue    enableNotifParameter;

            if(handsFreeServerManager != null) {
                enableNotifParameter = getCommandParameterView(0).getValueCheckbox();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.enableRemoteCallLineIdentificationNotification(bluetoothAddress, enableNotifParameter.value);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.enableRemoteCallerIDNotificationCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Enable Notification", false);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler dialNumber_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.dialPhoneNumber(bluetoothAddress, getCommandParameterView(0).getValueText().text.toString());

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.dialNumberCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Phone Number", InputType.TYPE_CLASS_PHONE);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler memoryDial_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            int              memoryLocation;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    memoryLocation = Integer.valueOf(getCommandParameterView(0).getValueText().text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = handsFreeServerManager.dialPhoneNumberFromMemory(bluetoothAddress, memoryLocation);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.memoryDialCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Memory Location Index", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler reDialLastNumber_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.redialLastPhoneNumber(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.reDialLastNumberCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler answerCall_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.answerIncomingCall(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.answerCallCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler hangupCall_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.hangUpCall(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.hangupCallCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler sendDTMF_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                if(getCommandParameterView(0).getValueText().text.toString().length() < 1) {
                    // TODO complain
                    return;
                }

                result = handsFreeServerManager.transmitDTMFCode(bluetoothAddress, getCommandParameterView(0).getValueText().text.toString().charAt(0));

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.sendDTMFCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "DTMF Code (0-9, *, #, A-D)");
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler sendVoiceTagRequest_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.voiceTagRequest(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.sendVoiceTagRequestCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler queryCallList_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.queryRemoteCurrentCallsList(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.queryCallListCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler setOperatorFormat_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.setNetworkOperatorSelectionFormat(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.setOperatorFormatCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler queryOperator_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.queryRemoteNetworkOperatorSelection(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.queryOperatorCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler enableErrorReports_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.enableRemoteExtendedErrorResult(bluetoothAddress, getCommandParameterView(0).getValueCheckbox().value);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.enableErrorReportsCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Enable Extended Error Results:", false);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler queryPhoneNumber_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.querySubscriberNumberInformation(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.queryPhoneNumberCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler queryResponseHoldStatus_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = handsFreeServerManager.queryResponseHoldStatus(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.queryResponseHoldStatusCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler setIncomingCallState_Handler = new CommandHandler() {

        String[] callStateLabels = {
                "Accept",
                "Place on Hold",
                "Reject",
                "Ignore",
        };

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            SpinnerValue     callStateValue;
            CallState        callState;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                callStateValue = getCommandParameterView(0).getValueSpinner();

                switch(callStateValue.selectedItem) {
                case 0:
                    callState = CallState.ACCEPT;
                    break;
                case 1:
                    callState = CallState.HOLD;
                    break;
                case 2:
                    callState = CallState.REJECT;
                    break;
                case 3:
                default:
                    callState = CallState.NONE;
                    break;
                }

                result = handsFreeServerManager.setIncomingCallState(bluetoothAddress, callState);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.setIncomingCallStateCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeSpinner("Incoming Call State", callStateLabels);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler sendArbitraryCommand_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            String           command;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                if(getCommandParameterView(1).getValueCheckbox().value == true) {
                    command = getCommandParameterView(0).getValueText().text.toString() + '\r';
                } else {
                    command = getCommandParameterView(0).getValueText().text.toString();
                }

                result = handsFreeServerManager.sendArbitraryCommand(bluetoothAddress, command);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.sendArbitraryCommandCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Arbitrary Command");
            getCommandParameterView(1).setModeCheckbox("Terminate with CR", true);
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler setSMSToText_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            String           command;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                command =  "AT+CMGF=1" + '\r';

                result = handsFreeServerManager.sendArbitraryCommand(bluetoothAddress, command);

                displayMessage("");
                displayMessage("command");
                displayMessage(resourceManager.getString(R.string.setSMSToTextCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    private final CommandHandler sendSMSNumber_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            String           command;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                command = "AT+CMGS=" + '\"' + getCommandParameterView(0).getValueText().text.toString() + '\"' + '\r';

                result = handsFreeServerManager.sendArbitraryCommand(bluetoothAddress, command);

                displayMessage("");
                displayMessage(command);
                displayMessage(resourceManager.getString(R.string.sendSMSNumberCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Phone Number");
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler sendSMSMessage_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            String           command;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                command = getCommandParameterView(0).getValueText().text.toString() + (char)26;

                result = handsFreeServerManager.sendArbitraryCommand(bluetoothAddress, command);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.sendSMSMessageCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Message");
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final Command[] commandList = new Command[] {
            new Command("Connect Device", connectDevice_Handler),
            new Command("Disconnect Device", disconnectDevice_Handler),
            new Command("Connection Request Response", connectionRequestResponse_Handler),
            new Command("Query Connected Devices", queryConnectedDevices_Handler),
            new Command("Query Local Configuration", queryLocalConfiguration_Handler),
            new Command("Change Incoming Connection Flags", changeIncomingConnectionFlags_Handler),
            new Command("Disable Remote Sound Enhancement", disableRemoteSoundEnhancement_Handler),
            new Command("Set Remote Voice Recognition Activation", setRemoteVoiceRecognitionActivation_Handler),
            new Command("Set Speaker Gain", setSpeakerGain_Handler),
            new Command("Set Microphone Gain", setMicrophoneGain_Handler),
            new Command("Register for Audio Data", registerAudioData_Handler),
            new Command("Release Audio Data Registration", unregisterAudioData_Handler),
            new Command("Setup SCO Audio Connection", setupSCOAudioConnection_Handler),
            new Command("Release SCO Audio Connection", releaseSCOAudioConnection_Handler),
            new Command("Send Audio Data", sendAudioData_Handler),
            new Command("Query Remote Indicator Status", queryRemoteIndicatorStatus_Handler),
            new Command("Enable Remote Indicator Notification", enableRemoteIndicatorNotification_Handler),
            new Command("Query Remote Call Hold Support", queryRemoteCallHoldSupport_Handler),
            new Command("Send Call Hold Selection", sendCallHoldSelection_Handler),
            new Command("Enable Remote Call Waiting Notification", enableRemoteCallWaitingNotification_Handler),
            new Command("Enable Remote Caller ID Notification", enableRemoteCallerIDNotification_Handler),
            new Command("Dial Number", dialNumber_Handler),
            new Command("Dial Number From Memory", memoryDial_Handler),
            new Command("Re-dial Last Number", reDialLastNumber_Handler),
            new Command("Answer Call", answerCall_Handler),
            new Command("Hang-up Call", hangupCall_Handler),
            new Command("Send DTMF Code", sendDTMF_Handler),
            new Command("Send Voice Tag Request", sendVoiceTagRequest_Handler),
            new Command("Query Current Call List", queryCallList_Handler),
            new Command("Set Operator Format", setOperatorFormat_Handler),
            new Command("Query Operator", queryOperator_Handler),

            new Command("Enable Error Reports", enableErrorReports_Handler),
            new Command("Query Local Phone Number", queryPhoneNumber_Handler),
            new Command("Query Response/Hold Status", queryResponseHoldStatus_Handler),
            new Command("Set Incoming Call State", setIncomingCallState_Handler),

            new Command("Send Arbitrary Command", sendArbitraryCommand_Handler),
            new Command("Set SMS Message to Text", setSMSToText_Handler),
            new Command("Send Phone Number for SMS", sendSMSNumber_Handler),
            new Command("Send SMS Message", sendSMSMessage_Handler),
    };

}
