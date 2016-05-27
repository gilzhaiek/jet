/*
 * < MainActivity.java >
 * Copyright 2013 - 2014 Stonestreet One. All Rights Reserved.
 *
 * Message Access Profile Client Sample for Stonestreet One Bluetooth Protocol
 * Stack Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.mapm_mce;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.EnumSet;

import android.content.res.AssetManager;
import android.text.InputType;
import android.util.Log;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.BluetopiaPMException;
import com.stonestreetone.bluetopiapm.MAPM;
import com.stonestreetone.bluetopiapm.MAPM.CharSet;
import com.stonestreetone.bluetopiapm.MAPM.ConnectionCategory;
import com.stonestreetone.bluetopiapm.MAPM.ConnectionFlags;
import com.stonestreetone.bluetopiapm.MAPM.ConnectionStatus;
import com.stonestreetone.bluetopiapm.MAPM.ConnectionType;
import com.stonestreetone.bluetopiapm.MAPM.FilterPriority;
import com.stonestreetone.bluetopiapm.MAPM.FilterReadStatus;
import com.stonestreetone.bluetopiapm.MAPM.FractionDeliver;
import com.stonestreetone.bluetopiapm.MAPM.FractionRequest;
import com.stonestreetone.bluetopiapm.MAPM.ListingInfo;
import com.stonestreetone.bluetopiapm.MAPM.ListingSizeInfo;
import com.stonestreetone.bluetopiapm.MAPM.MessageAccessClientManager;
import com.stonestreetone.bluetopiapm.MAPM.MessageAccessClientManager.ClientEventCallback;
import com.stonestreetone.bluetopiapm.MAPM.MessageAccessServices;
import com.stonestreetone.bluetopiapm.MAPM.MessageType;
import com.stonestreetone.bluetopiapm.MAPM.PathOption;
import com.stonestreetone.bluetopiapm.MAPM.ResponseStatusCode;
import com.stonestreetone.bluetopiapm.MAPM.StatusIndicator;
import com.stonestreetone.bluetopiapm.MAPM.SupportedMessageType;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.CheckboxValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.ChecklistValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.SpinnerValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.TextValue;
import com.stonestreetone.bluetopiapm.sample.util.SS1SampleActivity;

/**
 *  MAPM Message Client Equipment (MCE) Sample
 */
public class MainActivity extends SS1SampleActivity {

    private static final String LOG_TAG                          = "MAPM_Sample";

    /* package */ MAPM.MessageAccessClientManager messageAccessClientManager;

    @Override
    protected boolean profileEnable() {

        synchronized(this) {
            try {
                messageAccessClientManager = new MessageAccessClientManager(messageAccessClientEventCallback);
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
            if(messageAccessClientManager != null) {

                messageAccessClientManager.dispose();
                messageAccessClientManager = null;

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
        return 9;
    }

    private final ClientEventCallback messageAccessClientEventCallback = new ClientEventCallback() {

        @Override
        public void disconnectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int instanceID) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.disconnectedEventLabel));
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.connectionTypeLabel)).append(": ");
            sb.append(connectionType);
            displayMessage(sb);
        }

        @Override
        public void connectionStatusEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int instanceID, ConnectionStatus connectionStatus) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.connectionStatusEventLabel)).append(": ");
            sb.append(connectionStatus);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.connectionTypeLabel)).append(": ");
            sb.append(connectionType);
            displayMessage(sb);
         }

        @Override
        public void enableNotificationsResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.enableNotificationsResponseEventLabel)).append(": ");
            sb.append(responseStatusCode);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);
        }

        @Override
        public void getFolderListingResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, boolean isFinal, byte[] folderListingBuffer) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.getFolderListingResponseEventLabel)).append(": ");
            sb.append(responseStatusCode);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.isFinalLabel)).append(": ");
            sb.append(isFinal);
            displayMessage(sb);

            sb = new StringBuilder();
            if(folderListingBuffer != null) {

                ByteArrayInputStream folderListingInputStream = new ByteArrayInputStream(folderListingBuffer);
                BufferedInputStream folderListingBufferedInputStream = new BufferedInputStream(folderListingInputStream);
                try {
                    int bytesRead;

                    while((bytesRead = folderListingBufferedInputStream.read(folderListingBuffer)) != -1) {
                        sb.append(new String(folderListingBuffer, 0, bytesRead));
                    }
                    displayMessage(sb);
                } catch(IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void getFolderListingSizeResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, int numberOfFolders) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.getFolderListingSizeResponseEventLabel)).append(": ");
            sb.append(responseStatusCode);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.numberOfFoldersLabel)).append(": ");
            sb.append(numberOfFolders);
            displayMessage(sb);
        }

        @Override
        public void getMessageListingResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, boolean newMessage, Calendar timeMSE, boolean validUTC, int offsetUTC, boolean isFinal, int numberOfMessages, byte[] messageListingBuffer) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.getMessageListingResponseEventLabel)).append(": ");
            sb.append(responseStatusCode);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.newMessageLabel)).append(": ");
            sb.append(newMessage);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.timeMSELabel)).append(": ");
            if(timeMSE != null)
                sb.append(timeMSE.getTime());
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.validUTCLabel)).append(": ");
            sb.append(validUTC);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.offsetUTCLabel)).append(": ");
            if(validUTC)
                if(offsetUTC < 0)
                    sb.append("- " + Math.abs(offsetUTC)/60 + ":" + Math.abs(offsetUTC) % 60);
                else
                    sb.append("  " + offsetUTC/60 + ":" + offsetUTC % 60);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.isFinalLabel)).append(": ");
            sb.append(isFinal);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.numberOfMessagesLabel)).append(": ");
            sb.append(numberOfMessages);
            displayMessage(sb);

            sb = new StringBuilder();
            if(messageListingBuffer != null) {

                ByteArrayInputStream folderListingInputStream = new ByteArrayInputStream(messageListingBuffer);
                BufferedInputStream folderListingBufferedInputStream = new BufferedInputStream(folderListingInputStream);
                try {
                    int bytesRead;

                    while((bytesRead = folderListingBufferedInputStream.read(messageListingBuffer)) != -1) {
                        sb.append(new String(messageListingBuffer, 0, bytesRead));
                    }
                    displayMessage(sb);
                } catch(IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void getMessageListingSizeResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, boolean newMessage, Calendar timeMSE, boolean validUTC, int offsetUTC, int numberOfMessages) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.getMessageListingSizeResponseEventLabel)).append(": ");
            sb.append(responseStatusCode);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.newMessageLabel)).append(": ");
            sb.append(newMessage);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.timeMSELabel)).append(": ");
            if(timeMSE != null)
                sb.append(timeMSE.getTime());
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.validUTCLabel)).append(": ");
            sb.append(validUTC);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.offsetUTCLabel)).append(": ");
            if(validUTC)
                if(offsetUTC < 0)
                    sb.append("- " + Math.abs(offsetUTC)/60 + ":" + Math.abs(offsetUTC) % 60);
                else
                    sb.append("  " + offsetUTC/60 + ":" + offsetUTC % 60);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.numberOfMessagesLabel)).append(": ");
            sb.append(numberOfMessages);
            displayMessage(sb);
        }

        @Override
        public void getMessageResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, FractionDeliver fractionDeliver, boolean isFinal, byte[] messageBuffer) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.responseStatusCodeLabel)).append(": ");
            sb.append(responseStatusCode);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.fractionDeliverLabel)).append(": ");
            sb.append(fractionDeliver);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.isFinalLabel)).append(": ");
            sb.append(isFinal);
            displayMessage(sb);

            sb = new StringBuilder();
            if(messageBuffer != null) {

                ByteArrayInputStream folderListingInputStream = new ByteArrayInputStream(messageBuffer);
                BufferedInputStream folderListingBufferedInputStream = new BufferedInputStream(folderListingInputStream);
                try {
                    int bytesRead;

                    while((bytesRead = folderListingBufferedInputStream.read(messageBuffer)) != -1) {
                        sb.append(new String(messageBuffer, 0, bytesRead));
                    }
                    displayMessage(sb);
                } catch(IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void notificationIndicationEvent(BluetoothAddress remoteDeviceAddress, int instanceID, boolean isFinal, byte[] eventReportBuffer) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);
        }

        @Override
        public void pushMessageResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, String messageHandle) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.responseStatusCodeLabel)).append(": ");
            sb.append(responseStatusCode);
            displayMessage(sb);
        }

        @Override
        public void setFolderResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, String currentPath) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.setFolderResponseEventLabel)).append(": ");
            sb.append(responseStatusCode);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.currentPathLabel)).append(": ");
            sb.append(currentPath);
            displayMessage(sb);
        }

        @Override
        public void setMessageStatusResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.responseStatusCodeLabel)).append(": ");
            sb.append(responseStatusCode);
            displayMessage(sb);
        }

        @Override
        public void updateInboxResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode) {

            displayMessage("");
            StringBuilder sb = new StringBuilder();

            sb.append(resourceManager.getString(R.string.bluetoothAddressLabel)).append(": ");
            sb.append(remoteDeviceAddress);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.instanceIDLabel)).append(": ");
            sb.append(instanceID);
            displayMessage(sb);

            sb = new StringBuilder();
            sb.append(resourceManager.getString(R.string.responseStatusCodeLabel)).append(": ");
            sb.append(responseStatusCode);
            displayMessage(sb);
        }

    };

    private final CommandHandler connectRemoteDevice_Handler = new CommandHandler() {

        String[]  connectionFlagLabels = {
            "Authentication",
            "Encryption"
        };

        boolean[] connectionFlagValues = new boolean[] {false, false};

        boolean   waitForConnectionChecked = false;

        @Override
        public void run() {

            int result = 0;
            int remotePort;
            int instanceID;
            EnumSet<ConnectionFlags> connectionFlags;
            BluetoothAddress bluetoothAddress;

            TextValue portNumberParameter;
            TextValue instanceIDParameter;
            ChecklistValue connectionFlagsParameter;
            CheckboxValue waitForConnectionParameter;

            if(messageAccessClientManager != null) {

                portNumberParameter = getCommandParameterView(0).getValueText();
                instanceIDParameter = getCommandParameterView(1).getValueText();
                connectionFlagsParameter = getCommandParameterView(2).getValueChecklist();
                waitForConnectionParameter = getCommandParameterView(3).getValueCheckbox();

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

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
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

                result = messageAccessClientManager.connectRemoteDevice(bluetoothAddress, remotePort, instanceID, connectionFlags, waitForConnectionParameter.value);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.connectRemoteDeviceLabel) + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Port number", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeChecklist("Connection Flags", connectionFlagLabels, connectionFlagValues);
            getCommandParameterView(3).setModeCheckbox("Wait for Connection:", waitForConnectionChecked);
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }
    };
    private final CommandHandler disconnect_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;
            int instanceID;
            TextValue instanceIDParameter;
            BluetoothAddress bluetoothAddress;
            ConnectionCategory connectionCategory = ConnectionCategory.MESSAGE_ACCESS;

            if(messageAccessClientManager != null) {

                instanceIDParameter = getCommandParameterView(0).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = messageAccessClientManager.disconnect(connectionCategory, bluetoothAddress, instanceID);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.disconnectLabel) + result);
            }

        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }

    };
    private final CommandHandler queryCurrentFolder_Handler = new CommandHandler() {

        @Override
        public void run() {

            int instanceID;
            String result = "";
            BluetoothAddress bluetoothAddress;
            TextValue instanceIDParameter;

            if(messageAccessClientManager != null) {

                instanceIDParameter = getCommandParameterView(0).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                try {
                    result = messageAccessClientManager.queryCurrentFolder(bluetoothAddress, instanceID);
                } catch(BluetopiaPMException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.currentPathLabel) + ": " + ((result == null) ? "" : result));
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };
    private final CommandHandler abort_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;
            int instanceID;
            TextValue instanceIDParameter;
            ConnectionCategory connectionCategory = ConnectionCategory.MESSAGE_ACCESS;
            BluetoothAddress bluetoothAddress;

            if(messageAccessClientManager != null) {

                instanceIDParameter = getCommandParameterView(0).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = messageAccessClientManager.abort(connectionCategory, bluetoothAddress, instanceID);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.abortLabel) + result);

            }
        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };
    private final CommandHandler parseRemoteMessageAccessServices_Handler = new CommandHandler() {

        @Override
        public void run() {

            BluetoothAddress bluetoothAddress;

            MessageAccessServices[] messageAccessServices = null;

            if(messageAccessClientManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    messageAccessServices = messageAccessClientManager.parseRemoteMessageAccessServices(bluetoothAddress);

                    EnumSet<SupportedMessageType> supportedMessageTypes;

                    displayMessage("");

                    displayMessage(resourceManager.getString(R.string.parseRemoteMessageAccessServicesLabel) );

                    for(MessageAccessServices messageAccessService : messageAccessServices) {

                        StringBuilder sb = new StringBuilder();
                        sb.append(resourceManager.getString(R.string.serviceNameLabel)).append(": ");
                        sb.append(messageAccessService.getServiceName());
                        displayMessage(sb);

                        sb = new StringBuilder();
                        sb.append(resourceManager.getString(R.string.serverInstanceIDLabel)).append(": ");
                        sb.append(messageAccessService.getInstanceID());
                        displayMessage(sb);

                        sb = new StringBuilder();
                        sb.append(resourceManager.getString(R.string.serverPortLabel)).append(": ");
                        sb.append(messageAccessService.getServerPort());
                        displayMessage(sb);

                        sb = new StringBuilder();
                        sb.append(resourceManager.getString(R.string.supportedMessageTypesLabel)).append(": ");
                        displayMessage(sb);

                        supportedMessageTypes = messageAccessService.getSupportedMessageTypes();
                        for(SupportedMessageType supportedMessage : supportedMessageTypes) {
                            sb = new StringBuilder();
                            sb.append(supportedMessage);
                            displayMessage(sb);
                        }
                        supportedMessageTypes.clear();

                        displayMessage("");

                    }

                } catch(BluetopiaPMException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
         }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler enableNotifications_Handler = new CommandHandler() {

        boolean enableChecked = false;

        @Override
        public void run() {
            int result;
            int instanceID;
            BluetoothAddress bluetoothAddress;
            TextValue instanceIDParameter;
            CheckboxValue enableCheckedParameter;

            if(messageAccessClientManager != null) {

                enableCheckedParameter = getCommandParameterView(0).getValueCheckbox();
                instanceIDParameter = getCommandParameterView(1).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                enableChecked = enableCheckedParameter.value;

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = messageAccessClientManager.enableNotifications(bluetoothAddress, instanceID, enableChecked);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.enableNotificationsLabel) + result);
            }
        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeCheckbox("Enable:", enableChecked);
            getCommandParameterView(1).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler setFolder_Handler = new CommandHandler() {

        String[] pathOptions = {
            "Root",
            "Up",
            "Down"
        };

        @Override
        public void run() {

            int result;
            int instanceID;
            String folderName;
            PathOption pathOption;
            BluetoothAddress bluetoothAddress;

            TextValue instanceIDParameter;
            TextValue folderNameParameter;
            SpinnerValue pathOptionParameter;

            if(messageAccessClientManager != null) {

                folderNameParameter = getCommandParameterView(0).getValueText();
                pathOptionParameter = getCommandParameterView(1).getValueSpinner();
                instanceIDParameter = getCommandParameterView(2).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                folderName = folderNameParameter.text.toString();

                switch(pathOptionParameter.selectedItem) {
                case 0:
                    pathOption = PathOption.ROOT;
                    break;
                case 1:
                    pathOption = PathOption.UP;
                    break;
                case 2:
                    pathOption = PathOption.DOWN;
                    break;
                default:
                    pathOption = PathOption.DOWN;
                    break;
                }

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = messageAccessClientManager.setFolder(bluetoothAddress, instanceID, pathOption, folderName);

                displayMessage("");

                displayMessage(resourceManager.getString(R.string.setFolderLabel) + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Folder Name: ", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeSpinner("Path Options", pathOptions);
            getCommandParameterView(2).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler setFolderAbsolute_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;
            int instanceID;
            String absolutePath;
            BluetoothAddress bluetoothAddress;

            TextValue absolutePathParameter;
            TextValue instanceIDParameter;

            if(messageAccessClientManager != null) {

                absolutePathParameter = getCommandParameterView(0).getValueText();
                instanceIDParameter = getCommandParameterView(1).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                absolutePath = absolutePathParameter.text.toString();

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = messageAccessClientManager.setFolderAbsolute(bluetoothAddress, instanceID, absolutePath);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.setFolderAbsoluteLabel) + result);

            }
        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeText("", "Absolute Path", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler getFolderListing_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;
            int instanceID;
            int maxListCount = 1024;
            int listStartOffset = 0;
            BluetoothAddress bluetoothAddress;

            TextValue maxListCountParameter;
            TextValue listStartOffsetParameter;
            TextValue instanceIDParameter;

            if(messageAccessClientManager != null) {

                maxListCountParameter = getCommandParameterView(0).getValueText();
                listStartOffsetParameter = getCommandParameterView(1).getValueText();
                instanceIDParameter = getCommandParameterView(2).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    maxListCount = Integer.valueOf(maxListCountParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                        return;
                }

                try {
                    listStartOffset = Integer.valueOf(listStartOffsetParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = messageAccessClientManager.getFolderListing(bluetoothAddress, instanceID, maxListCount, listStartOffset);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.getFolderListingLabel) + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("1024", "Max List Count", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeText("0", "List Start Offset", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler getFolderListingSize_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;
            int instanceID;
            BluetoothAddress bluetoothAddress;

            TextValue instanceIDParameter;

            if(messageAccessClientManager != null) {

                instanceIDParameter = getCommandParameterView(0).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = messageAccessClientManager.getFolderListingSize(bluetoothAddress, instanceID);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.getFolderListingSizeLabel) + result);

            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler getMessageListing_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;
            int instanceID;
            int maxListCount = 1024;
            int listStartOffset = 0;
            String folderName;
            BluetoothAddress bluetoothAddress;

            TextValue folderNameParameter;
            TextValue maxListCountParameter;
            TextValue listStartOffsetParameter;
            TextValue instanceIDParameter;

            ListingInfo listingInfo = null;

            if(messageAccessClientManager != null) {

                folderNameParameter = getCommandParameterView(0).getValueText();
                maxListCountParameter = getCommandParameterView(1).getValueText();
                listStartOffsetParameter = getCommandParameterView(2).getValueText();
                instanceIDParameter = getCommandParameterView(3).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                folderName = folderNameParameter.text.toString();

                try {
                    maxListCount = Integer.valueOf(maxListCountParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                try {
                    listStartOffset = Integer.valueOf(listStartOffsetParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = messageAccessClientManager.getMessageListing(bluetoothAddress, instanceID, folderName, maxListCount, listStartOffset, listingInfo);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.getMessageListingLabel) + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Folder Name", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeText("1024", "Max List Count", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeText("0", "List Start Offset", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(3).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler getMessageListingSize_Handler = new CommandHandler() {

        String[]  messageTypeLabels = {
            "SMS GSM",
            "SMS CDMA",
            "Email",
            "MMS"
        };

        boolean[] messageTypeValues = new boolean[] {
            false,
            false,
            false,
            true,
        };

        String[]  readStatusOptions = {
            "Read Only",
            "Unread Only",
        };

        String[]  priorityOptions = {
            "Non-High Only",
            "High Only",
        };

        @Override
        public void run() {

            int                  result;
            int                  instanceID;
            String               folderName = null;
            EnumSet<MessageType> messageTypes;
            Calendar             periodBegin = Calendar.getInstance();
            Calendar             periodEnd = Calendar.getInstance();
            FilterReadStatus     readStatus;
            String               recipient = null;
            String               originator = null;
            String				 dateFormat = "ddMMyyyy";
            FilterPriority       priority;
            BluetoothAddress     bluetoothAddress;

            TextValue      folderNameParameter;
            TextValue      instanceIDParameter;
            ChecklistValue messageTypeParameter;
            TextValue      periodBeginParameter;
            TextValue      periodEndParameter;
            SpinnerValue   readStatusParameter;
            TextValue      recipientParameter;
            TextValue      originatorParameter;
            SpinnerValue   priorityParameter;

            if(messageAccessClientManager != null) {

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                folderNameParameter  = getCommandParameterView(0).getValueText();
                instanceIDParameter  = getCommandParameterView(1).getValueText();
                messageTypeParameter = getCommandParameterView(2).getValueChecklist();
                periodBeginParameter = getCommandParameterView(3).getValueText();
                periodEndParameter   = getCommandParameterView(4).getValueText();
                readStatusParameter  = getCommandParameterView(5).getValueSpinner();
                recipientParameter   = getCommandParameterView(6).getValueText();
                originatorParameter  = getCommandParameterView(7).getValueText();
                priorityParameter    = getCommandParameterView(8).getValueSpinner();

                folderName = folderNameParameter.text.toString();

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                messageTypes = EnumSet.noneOf(MessageType.class);

                if(messageTypeParameter.checkedItems[0]) {
                    messageTypes.add(MessageType.SMS_GSM);
                }
                if(messageTypeParameter.checkedItems[1]) {
                    messageTypes.add(MessageType.SMS_CDMA);
                }
                if(messageTypeParameter.checkedItems[2]) {
                    messageTypes.add(MessageType.EMAIL);
                }
                if(messageTypeParameter.checkedItems[3]) {
                    messageTypes.add(MessageType.MMS);
                }

                SimpleDateFormat beginDateFormat = new SimpleDateFormat(dateFormat);
                try {
                    periodBegin.setTime(beginDateFormat.parse(periodBeginParameter.text.toString()));
                } catch(ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                SimpleDateFormat endDateFormat = new SimpleDateFormat(dateFormat);
                try {
                    periodEnd.setTime(endDateFormat.parse(periodEndParameter.text.toString()));
                } catch(ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                switch(readStatusParameter.selectedItem) {
                case 0:
                    readStatus = FilterReadStatus.READ_ONLY;
                    break;
                case 1:
                default:
                    readStatus = FilterReadStatus.UNREAD_ONLY;
                    break;
                }

                recipient = recipientParameter.text.toString();

                originator = originatorParameter.text.toString();


                switch(priorityParameter.selectedItem) {
                case 0:
                    priority = FilterPriority.NON_HIGH_ONLY;
                    break;
                case 1:
                default:
                    priority = FilterPriority.HIGH_ONLY;
                    break;
                }

                ListingSizeInfo listingSizeInfo = new ListingSizeInfo(messageTypes, periodBegin, periodEnd, readStatus, recipient, originator, priority);

                result = messageAccessClientManager.getMessageListingSize(bluetoothAddress, instanceID, folderName, listingSizeInfo);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.getMessageListingSizeLabel) + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Folder Name", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeChecklist("Message Types", messageTypeLabels, messageTypeValues);
            getCommandParameterView(3).setModeText("", "Period Begin", InputType.TYPE_CLASS_DATETIME);
            getCommandParameterView(4).setModeText("", "Period End", InputType.TYPE_CLASS_DATETIME);
            getCommandParameterView(5).setModeSpinner("Read Status", readStatusOptions);
            getCommandParameterView(6).setModeText("", "Recipient", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(7).setModeText("", "Originator", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(8).setModeSpinner("Priority", priorityOptions);
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler getMessage_Handler = new CommandHandler() {

        String[] charSetOptions = {
            "Native",
            "UTF8"
        };

        boolean  attachmentChecked = false;

        @Override
        public void run() {

            int result;
            int instanceID;
            String messageHandle;
            CharSet charSet;
            BluetoothAddress bluetoothAddress;

            TextValue instanceIDParameter;
            TextValue messageHandleParameter;
            CheckboxValue attachmentCheckedParameter;
            SpinnerValue charSetParameter;

            if(messageAccessClientManager != null) {

                messageHandleParameter = getCommandParameterView(0).getValueText();
                attachmentCheckedParameter = getCommandParameterView(1).getValueCheckbox();
                charSetParameter = getCommandParameterView(2).getValueSpinner();
                instanceIDParameter = getCommandParameterView(3).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                messageHandle = messageHandleParameter.text.toString();
                attachmentChecked = attachmentCheckedParameter.value;

                switch(charSetParameter.selectedItem) {
                case 0:
                    charSet = CharSet.NATIVE;
                    break;
                case 1:
                default:
                    charSet = CharSet.UTF8;
                    break;
                }

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = messageAccessClientManager.getMessage(bluetoothAddress, instanceID, messageHandle, attachmentChecked, charSet, FractionRequest.FIRST);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.getMessageLabel) + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Message Handle", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeCheckbox("Attachment:", attachmentChecked);
            getCommandParameterView(2).setModeSpinner("Character Set", charSetOptions);
            getCommandParameterView(3).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };
    private final CommandHandler setMessageStatus_Handler = new CommandHandler() {

        String[] statusIndicatorOptions = {
            "Read",
            "Deleted"
        };

        @Override
        public void run() {

            int result;
            int instanceID;
            String messageHandle;
            StatusIndicator statusIndicator;
            boolean statusValue = false;
            BluetoothAddress bluetoothAddress;

            TextValue instanceIDParameter;
            TextValue messageHandleParameter;
            CheckboxValue statusValueParameter;
            SpinnerValue statusIndicatorParameter;

            if(messageAccessClientManager != null) {

                messageHandleParameter = getCommandParameterView(0).getValueText();
                statusIndicatorParameter = getCommandParameterView(1).getValueSpinner();
                statusValueParameter = getCommandParameterView(2).getValueCheckbox();
                instanceIDParameter = getCommandParameterView(3).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                messageHandle = messageHandleParameter.text.toString();

                switch(statusIndicatorParameter.selectedItem) {
                case 0:
                    statusIndicator = StatusIndicator.READ_STATUS;
                    break;
                case 1:
                    statusIndicator = StatusIndicator.DELETED_STATUS;
                    break;
                default:
                    statusIndicator = null;
                    break;
                }

                statusValue = statusValueParameter.value;

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = messageAccessClientManager.setMessageStatus(bluetoothAddress, instanceID, messageHandle, statusIndicator, statusValue);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.setMessageStatusLabel) + result);
            }
        }

        @Override
        public void selected() {

            getCommandParameterView(0).setModeText("", "Message Handle", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeSpinner("Status Indicator", statusIndicatorOptions);
            getCommandParameterView(2).setModeCheckbox("Status Value:", false);
            getCommandParameterView(3).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }
    };
    private final CommandHandler pushMessage_Handler = new CommandHandler() {

        String[]  charSetOptions = {
            "UTF8",
            "Native"
        };

        String[]  messageOptions = {
            "Save a Copy in Sent Folder",
            "Retry Send in Case of Failure",
            "Final Packet"
        };

        boolean[] optionsChecked = {
            true,
            false,
            true
        };

        @Override
        public void run() {

            int result;
            int instanceID;
            byte[] messageBuffer = new byte[1024];
            String folderName;
            CharSet charSet;
            BluetoothAddress bluetoothAddress;

            TextValue folderNameParameter;
            TextValue instanceIDParameter;
            ChecklistValue checkBoxParameter;
            SpinnerValue charSetParameter;

            if(messageAccessClientManager != null) {

                folderNameParameter = getCommandParameterView(0).getValueText();
                checkBoxParameter = getCommandParameterView(1).getValueChecklist();
                charSetParameter = getCommandParameterView(2).getValueSpinner();
                instanceIDParameter = getCommandParameterView(3).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                folderName = folderNameParameter.text.toString();

                switch(charSetParameter.selectedItem) {
                case 0:
                    charSet = CharSet.NATIVE;
                    break;
                case 1:
                default:
                    charSet = CharSet.UTF8;
                    break;
                }

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                AssetManager assetManager = getAssets();
                try {

                    int read;

                    InputStream inputStream = assetManager.open("test-message");
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                    while((read = bufferedInputStream.read(messageBuffer, 0, messageBuffer.length)) != -1) {
                        byteArrayOutputStream.write(messageBuffer, 0 , read);
                    }

                    byteArrayOutputStream.flush();

                    messageBuffer = byteArrayOutputStream.toByteArray();

                } catch(IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                result = messageAccessClientManager.pushMessage(bluetoothAddress, instanceID, folderName, checkBoxParameter.checkedItems[0] ? true : false , checkBoxParameter.checkedItems[1] ? true : false , charSet, messageBuffer, checkBoxParameter.checkedItems[2] ? true : false);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.pushMessageLabel) + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Folder Name", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(1).setModeChecklist("Message Options", messageOptions, optionsChecked);
            getCommandParameterView(2).setModeSpinner("Character Set", charSetOptions);
            getCommandParameterView(3).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }

    };
    private final CommandHandler updateInbox_Handler = new CommandHandler() {

        @Override
        public void run() {
            int result;
            int instanceID;
            BluetoothAddress bluetoothAddress;

            TextValue instanceIDParameter;

            if(messageAccessClientManager != null) {

                instanceIDParameter = getCommandParameterView(0).getValueText();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }

                try {
                    instanceID = Integer.valueOf(instanceIDParameter.text.toString());
                } catch(NumberFormatException e) {
                    // TODO complain
                    return;
                }

                result = messageAccessClientManager.updateInbox(bluetoothAddress, instanceID);

                displayMessage("");
                displayMessage(resourceManager.getString(R.string.updateInboxLabel) + result);
            }
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            getCommandParameterView(5).setModeHidden();
            getCommandParameterView(6).setModeHidden();
            getCommandParameterView(7).setModeHidden();
            getCommandParameterView(8).setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }
    };

    private final Command[] commandList = new Command[] {
        new Command("Connect Remote Device", connectRemoteDevice_Handler),
        new Command("Disconnect", disconnect_Handler),
        new Command("Query Current Folder", queryCurrentFolder_Handler),
        new Command("Abort", abort_Handler),
        new Command("Parse Remote Message Access Services", parseRemoteMessageAccessServices_Handler),
        new Command("Enable Notifications", enableNotifications_Handler),
        new Command("Set Folder", setFolder_Handler),
        new Command("Set Folder Absolute", setFolderAbsolute_Handler),
        new Command("Get Folder Listing", getFolderListing_Handler),
        new Command("Get Folder Listing Size", getFolderListingSize_Handler),
        new Command("Get Message Listing", getMessageListing_Handler),
        new Command("Get Message Listing Size", getMessageListingSize_Handler),
        new Command("Get Message", getMessage_Handler),
        new Command("Set Message Status", setMessageStatus_Handler),
        new Command("Push Message", pushMessage_Handler),
        new Command("Update Inbox", updateInbox_Handler),
    };
}
