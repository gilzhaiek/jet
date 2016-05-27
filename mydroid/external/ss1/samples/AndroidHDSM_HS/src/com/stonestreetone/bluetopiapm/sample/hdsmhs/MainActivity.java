/*
 * < MainActivity.java >
 * Copyright 2011 - 2014 Stonestreet One. All Rights Reserved.
 *
 * Headset Profile Device Sample for Stonestreet One Bluetooth Protocol Stack
 * Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.hdsmhs;

import java.util.EnumSet;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.text.InputType;
import android.util.Log;


import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.HDSM.HeadsetServerManager;
import com.stonestreetone.bluetopiapm.HDSM.HeadsetServerManager.HeadsetEventCallback;
import com.stonestreetone.bluetopiapm.HDSM.HeadsetServerManager.LocalConfiguration;
import com.stonestreetone.bluetopiapm.HDSM.ConnectionFlags;
import com.stonestreetone.bluetopiapm.HDSM.ConnectionStatus;
import com.stonestreetone.bluetopiapm.HDSM.DisconnectedReason;
import com.stonestreetone.bluetopiapm.HDSM.IncomingConnectionFlags;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.CheckboxValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.ChecklistValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.TextValue;
import com.stonestreetone.bluetopiapm.sample.util.SS1SampleActivity;

public class MainActivity extends SS1SampleActivity {
    private static final String LOG_TAG = "HFRM_AG_Sample";

    /*package*/ HeadsetServerManager serverManager;

    @Override
    protected boolean profileEnable() {
        synchronized(this) {

            try {
                serverManager = new HeadsetServerManager(true, eventCallback);
                return true;
            } catch(Exception e) {
                /*
                 * BluetopiaPM server couldn't be contacted.
                 * This should never happen if Bluetooth was
                 * successfully enabled.
                 */
                showToast(this,e.toString());
                //showToast(this, R.string.errorBTPMServerNotReachableToastMessage);
                //BluetoothAdapter.getDefaultAdapter().disable();
                return false;
            }
        }
    }

    @Override
    protected void profileDisable() {
        synchronized(MainActivity.this) {
            if(serverManager != null) {
                serverManager.dispose();
                serverManager = null;
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

    private final HeadsetEventCallback eventCallback = new HeadsetEventCallback() {
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
        public void ringIndication(BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.ringIndicationEventLabel)).append(": ");
            sb.append(remoteDeviceAddress.toString());

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

            if(serverManager != null) {
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

                result = serverManager.connectRemoteDevice(bluetoothAddress, remotePort, connectionFlags, waitForConnectionParameter.value);

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

            if(serverManager != null) {
                int result;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = serverManager.disconnectDevice(bluetoothAddress);

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

            if(serverManager != null) {
                acceptConnParameter = getCommandParameterView(0).getValueCheckbox();

                acceptConnection = acceptConnParameter.value;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = serverManager.connectionRequestResponse(bluetoothAddress, acceptConnection);

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

            if(serverManager != null) {
                devices = serverManager.queryConnectedDevices();

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

            if(serverManager != null) {
                config = serverManager.queryCurrentConfiguration();

                if(config != null) {
                    StringBuilder sb = new StringBuilder();

                    sb.append("Local Configuration:");
                    sb.append("\n     Incoming Connection Flags:");

                    for(IncomingConnectionFlags flag : config.getIncomingConnectionFlags()) {
                        sb.append(" ").append(flag);
                    }

                    sb.append("\n     Supported Features:");

                    for(HeadsetServerManager.SupportedFeatures feature : config.getSupportedFeatures()) {
                        sb.append(" ").append(feature);
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

            if(serverManager != null) {
                Log.d(LOG_TAG, "Piece of Shit");

                incomingFlagParameter = getCommandParameterView(0).getValueChecklist();

                flagSet = EnumSet.noneOf(IncomingConnectionFlags.class);
                if(incomingFlagParameter.checkedItems[0]) {
                    Log.d(LOG_TAG, "Authorization");
                    flagSet.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
                }
                if(incomingFlagParameter.checkedItems[1]) {
                    Log.d(LOG_TAG, "Authentication");
                    flagSet.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
                }
                if(incomingFlagParameter.checkedItems[2]) {
                    Log.d(LOG_TAG, "Encryption");
                    flagSet.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);
                }

                result = serverManager.changeIncomingConnectionFlags(flagSet);

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

    private final CommandHandler setSpeakerGain_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;
            TextValue        gainParameter;
            int              speakerGain;

            if(serverManager != null) {
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

                result = serverManager.setRemoteSpeakerGain(bluetoothAddress, speakerGain);

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

            if(serverManager != null) {
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

                result = serverManager.setRemoteMicrophoneGain(bluetoothAddress, microphoneGain);

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

            if(serverManager != null) {
                result = serverManager.registerSCODataProvider();

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
            if(serverManager != null) {
                serverManager.releaseSCODataProvider();

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
    
    private final CommandHandler sendButtonPress_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(serverManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                result = serverManager.sendButtonPress(bluetoothAddress);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.sendButtonPressCommandName) + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
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

    
    private final Command[] commandList = new Command[] {
            new Command("Connect Device", connectDevice_Handler),
            new Command("Disconnect Device", disconnectDevice_Handler),
            new Command("Connection Request Response", connectionRequestResponse_Handler),
            new Command("Query Connected Devices", queryConnectedDevices_Handler),
            new Command("Query Curernt Configuration", queryLocalConfiguration_Handler),
            new Command("Change Incoming Connection Flags", changeIncomingConnectionFlags_Handler),
            new Command("Set Speaker Gain", setSpeakerGain_Handler),
            new Command("Set Microphone Gain", setMicrophoneGain_Handler),
            new Command("Register for Audio Data", registerAudioData_Handler),
            new Command("Release Audio Data Registration", unregisterAudioData_Handler),
            new Command("Send Audio Data", sendAudioData_Handler),
            new Command("Send Button Press", sendButtonPress_Handler)
    };

}
