/*
 * < MainActivity.java >
 * Copyright 2011 - 2014 Stonestreet One. All Rights Reserved.
 *
 * Audio/Video Remote Control Profile Sample for Stonestreet One Bluetooth
 * Protocol Stack Platform Manager for Android.
 *
 * Author: Greg Hensley
 */
package com.stonestreetone.bluetopiapm.sample.avrcp;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;

import com.stonestreetone.bluetopiapm.AVRCP;
import com.stonestreetone.bluetopiapm.AVRCP.BatteryStatus;
import com.stonestreetone.bluetopiapm.AVRCP.CapabilityID;
import com.stonestreetone.bluetopiapm.AVRCP.CommandErrorCode;
import com.stonestreetone.bluetopiapm.AVRCP.CommandFailureStatus;
import com.stonestreetone.bluetopiapm.AVRCP.CompanyID;
import com.stonestreetone.bluetopiapm.AVRCP.ConnectionFlags;
import com.stonestreetone.bluetopiapm.AVRCP.ConnectionStatus;
import com.stonestreetone.bluetopiapm.AVRCP.ElementAttribute;
import com.stonestreetone.bluetopiapm.AVRCP.ElementAttributeID;
import com.stonestreetone.bluetopiapm.AVRCP.EventID;
import com.stonestreetone.bluetopiapm.AVRCP.GroupNavigationType;
import com.stonestreetone.bluetopiapm.AVRCP.IncomingConnectionFlags;
import com.stonestreetone.bluetopiapm.AVRCP.PlayStatus;
import com.stonestreetone.bluetopiapm.AVRCP.PlayerApplicationSettingText;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlAlreadyRegisteredException;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlButtonState;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlCommandType;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlControllerManager;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlControllerManager.BaseControllerEventCallback;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlDisconnectReason;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlPassThroughOperationID;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlResponseCode;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlSubunitID;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlSubunitType;
import com.stonestreetone.bluetopiapm.AVRCP.SystemStatus;
import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.ServerNotReachableException;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.CheckboxValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.ChecklistValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.OnSpinnerItemChangedListener;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.SeekbarValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.SpinnerValue;
import com.stonestreetone.bluetopiapm.sample.util.ParameterView.TextValue;
import com.stonestreetone.bluetopiapm.sample.util.SS1SampleActivity;
import com.stonestreetone.bluetopiapm.sample.util.Utils;


public class MainActivity extends SS1SampleActivity
{
    //TODO: Update this as desired. The first time a device registers
    //for notification, we will include this in an interim response, and
    //the volume will be changed.
    private static final float DEFAULT_VOLUME = .5f;

    private static final String LOG_TAG = "AVRCP_Sample";


    /*package*/ RemoteControlControllerManager rcManager;
    /*package*/ float currentVolume = DEFAULT_VOLUME;

    @Override
    protected boolean profileEnable() {
        synchronized(this)
        {

            try
            {
                rcManager = new RemoteControlControllerManager(rcControllerEventCallback);
                //supportedEvents = EnumSet.noneOf(EventID.class);
                return true;

            }
            catch(ServerNotReachableException e)
            {

                /*
                 * BluetopiaPM server couldn't be contacted.
                 * This should never happen if Bluetooth was
                 * successfully enabled.
                 */
                showToast(this, R.string.errorBTPMServerNotReachableToastMessage);
                return false;

            }
            catch(RemoteControlAlreadyRegisteredException e) {
                showToast(this, "Remote Control Role is already registered");
                return false;
            }
        }
    }

    @Override
    protected void profileDisable() {
        synchronized(MainActivity.this)
        {

            if(rcManager != null)
            {

                rcManager.dispose();
                rcManager = null;

            }
        }
        
    }

    @Override
    protected Command[] getCommandList() {
        return mCommands;
    }

    @Override
    protected int getNumberProfileParameters() {
        return 0;
    }
    
    @Override
    protected int getNumberCommandParameters() {
        return 6;
    }
    

    private BaseControllerEventCallback rcControllerEventCallback = new BaseControllerEventCallback() {

        @Override
        public void incomingConnectionRequestEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();
            
            sb.append("Connection Request: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void remoteControlConnectedEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();
            
            sb.append("Remote Control Connected: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void remoteControlConnectionStatusEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress, ConnectionStatus connectionStatus) {
            StringBuilder sb = new StringBuilder();
            
            sb.append("Remote Control Connection Status: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    Status: ").append(connectionStatus);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void remoteControlDisconnectedEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress, RemoteControlDisconnectReason disconnectReason) {
            StringBuilder sb = new StringBuilder();

            sb.append("Remote Control Disconnected: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    Reason: ").append(disconnectReason);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void passthroughResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, RemoteControlPassThroughOperationID operationID, RemoteControlButtonState buttonState, byte[] operationData) {
            StringBuilder sb = new StringBuilder();

            sb.append("Pass Through Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Operation ID:").append(operationID);
            sb.append("\n    Button State: ").append(buttonState);
            
            if(operationData != null && operationData.length > 0) {
                sb.append("\n    Operation Data: ");
                
                for(int i=0;i<operationData.length;i++)
                    sb.append(String.format("%0x02X ", operationData[i]));
            }
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void vendorDependentResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, RemoteControlSubunitType subunitType, RemoteControlSubunitID subunitID, CompanyID companyID, byte[] data) {
            StringBuilder sb = new StringBuilder();

            sb.append("Vendor Dependent Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Subunit Type: ").append(subunitType);
            sb.append("\n    Subunit ID: ").append(subunitID);
            sb.append("\n    Company ID: ").append(companyID);
            
            if(data != null && data.length > 0) {
                sb.append("\n    Response Data: ");
                
                for(int i=0;i<data.length;i++)
                    sb.append(String.format("%0x02X ", data[i]));
            }

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void groupNavigationResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, RemoteControlButtonState buttonState) {
            StringBuilder sb = new StringBuilder();

            sb.append("Group Navigation Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Button State: ").append(buttonState);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void getCompanyIDCapabilitiesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, CompanyID[] ids) {
            StringBuilder sb = new StringBuilder();

            sb.append("Company ID Capabilities Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    CompanyIDs:");
            
            for(CompanyID id : ids)
                sb.append("\n        ").append(id);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void getEventCapabilitiesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, EnumSet<EventID> ids) {
            StringBuilder sb = new StringBuilder();

            sb.append("Supported Events Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Supported Events:");
            
            for(EventID id : ids)
                sb.append("\n        ").append(id);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void listPlayerApplicationSettingAttributesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, byte[] attributeIDs) {
            StringBuilder sb = new StringBuilder();

            sb.append("List Player Application Setting Attributes Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Attribute IDs:");
            
            if(attributeIDs != null) {
                for(byte id : attributeIDs)
                    sb.append("\n        ").append(id);
            }
            
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void listPlayerApplicationSettingValuesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, byte[] values) {
            StringBuilder sb = new StringBuilder();

            sb.append("List Player Application Setting Values Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Value IDs:");
            
            if(values != null) {
                for(byte id : values)
                    sb.append("\n        ").append(id);
            }
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void getCurrentPlayerApplicationSettingValueResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, Map<Byte, Byte> currentValues) {
            StringBuilder sb = new StringBuilder();

            sb.append("Get Current Player Application Setting Values Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Current Values: ");
            
            for(byte attribute : currentValues.keySet())
                sb.append("\n        ").append(attribute).append(": ").append(currentValues.get(attribute));
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void setPlayerApplicationSettingValueResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Set Player Application Setting Value Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void getPlayerApplicationSettingAttributeTextResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, PlayerApplicationSettingText[] textEntries) {
            StringBuilder sb = new StringBuilder();

            sb.append("Get Player Application Setting Attribute Text Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Text Entries:");
            
            for(PlayerApplicationSettingText entry : textEntries) {
                sb.append("\n        Attribute ID: ").append(entry.id);
                sb.append("\n        Character Set: ").append(entry.characterSet);
                
                String text = entry.decodeTextData();
                
                if(text != null)
                    sb.append("\n        Text: ").append(text);
                else
                    sb.append("\n        Text: Unknown character set");
                
                sb.append("\n");
            }
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void getPlayerApplicationSettingValueTextResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, PlayerApplicationSettingText[] textEntries) {
            StringBuilder sb = new StringBuilder();

            sb.append("Get Player Application Setting Value Text Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            
            sb.append("\n    Text Entries:");
            
            for(PlayerApplicationSettingText entry : textEntries) {
                sb.append("\n        Value ID: ").append(entry.id);
                sb.append("\n        Character Set: ").append(entry.characterSet);
                
                String text = entry.decodeTextData();
                
                if(text != null)
                    sb.append("\n        Text: ").append(text);
                else
                    sb.append("\n        Text: Unknown character set");
                
                sb.append("\n");
            }
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void informDisplayableCharacterSetResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Inform Displayable Character Set Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void informBatteryStatusResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Inform Battery Status Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void getElementAttributesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, ElementAttribute[] attributes) {
            StringBuilder sb = new StringBuilder();

            sb.append("Get Element Attributes Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Element Attributes:");
            
            for(ElementAttribute attribute : attributes) {
                sb.append("\n        ID: ").append(attribute.attributeID);
                sb.append("\n        Character Set: ").append(attribute.characterSet);
                
                String text = attribute.decodeAttributeData();
                
                if(text != null)
                    sb.append("\n        Text: ").append(text);
                else
                    sb.append("\n        Text: Unknown character set");
                
                sb.append("\n");
            }
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void getPlayStatusResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, int length, int position, PlayStatus playStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append("Get Play Status Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Length: ").append(length);
            sb.append("\n    Position: ").append(position);
            sb.append("\n    Status: ").append(playStatus);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void playbackStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, PlayStatus playStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append("Playback Status Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Status: ").append(playStatus);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void trackChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, long identifier) {
            StringBuilder sb = new StringBuilder();

            sb.append("Track Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Identifier: ").append(identifier);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void trackReachedEndNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Track Reached End Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void trackReachedStartNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Track Reached Start Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void playbackPositionChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, int position) {
            StringBuilder sb = new StringBuilder();

            sb.append("Playback Position Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Position: ").append(position);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void batteryStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, BatteryStatus batteryStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append("Battery Status Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Status: ").append(batteryStatus);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void systemStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, SystemStatus systemStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append("System Status Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Status: ").append(systemStatus);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void playerApplicationSettingChangedNotifications(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, Map<Byte, Byte> changedValues) {
            StringBuilder sb = new StringBuilder();

            sb.append("Player Application Setting Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Changed Values: ");
            
            for(byte attribute : changedValues.keySet())
                sb.append("\n        ").append(attribute).append(": ").append(changedValues.get(attribute));
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void volumeChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, float volume) {
            StringBuilder sb = new StringBuilder();

            sb.append("Volume Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Volume: ").append(volume);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void setAbsoluteVolumeResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, float volume) {
            StringBuilder sb = new StringBuilder();

            sb.append("Set Absolute Volume Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Volume: ").append(volume);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void commandRejectedResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, CommandErrorCode errorCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Command Rejected Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Error Code: ").append(errorCode);
            
            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void commandFailureNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, CommandFailureStatus status) {
            StringBuilder sb = new StringBuilder();

            sb.append("Command Failure Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Status: ").append(status);
            
            
            displayMessage("");
            displayMessage(sb);
        }
        
    };
    
    private final CommandHandler connectionRequestResponse_Handler = new CommandHandler() {

        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                result = rcManager.connectionRequestResponse(bluetoothAddress, getCommandParameterView(0).getValueCheckbox().value);
                
                displayMessage("");
                displayMessage("connectionRequestResponse() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeCheckbox("Accept", false);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler connectRemoteControl_Handler = new CommandHandler() {

        final String[] flags = Utils.getEnumStringSet(ConnectionFlags.values());
        final boolean[] values = new boolean[flags.length];
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            EnumSet<ConnectionFlags> connectionFlags;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                ChecklistValue flagsParameter = getCommandParameterView(0).getValueChecklist();
                
                connectionFlags = EnumSet.noneOf(ConnectionFlags.class);
                
                for(int i=0;i<flags.length;i++) {
                    if(flagsParameter.checkedItems[i])
                        connectionFlags.add(ConnectionFlags.valueOf(ConnectionFlags.class, flags[i]));
                }

                
                result = rcManager.connectRemoteControl(bluetoothAddress, connectionFlags, getCommandParameterView(1).getValueCheckbox().value);
                
                displayMessage("");
                displayMessage("connectRemoteControl() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeChecklist("Connection Flags", flags, values);
            getCommandParameterView(1).setModeCheckbox("Wait for connection", false);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler changeIncomingConnectionFlags_Handler = new CommandHandler() {
        
        private String[] flags = Utils.getEnumStringSet(IncomingConnectionFlags.values());
        private boolean[] values = new boolean[flags.length];
        
        @Override
        public void run() {
            int                      result;
            EnumSet<IncomingConnectionFlags> connectionFlags;
            
            if(rcManager != null) {
                ChecklistValue flagsParameter = getCommandParameterView(0).getValueChecklist();
                
                connectionFlags = EnumSet.noneOf(IncomingConnectionFlags.class);
                
                for(int i=0;i<flags.length;i++) {
                    if(flagsParameter.checkedItems[i])
                        connectionFlags.add(IncomingConnectionFlags.valueOf(IncomingConnectionFlags.class, flags[i]));
                }
                
                result = rcManager.changeIncomingConnectionFlags(connectionFlags);
                
                displayMessage("");
                displayMessage("changeIncomingConnectionFlags_Handler() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeChecklist("Incoming Connection Flags", flags, values);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler disconnectRemoteControl_Handler = new CommandHandler() {

        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                result = rcManager.disconnectRemoteControl(bluetoothAddress);
                
                displayMessage("");
                displayMessage("disconnectRemoteControl_Handler() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler queryConnectedDevices_Handler = new CommandHandler() {

        @Override
        public void run() {
            BluetoothAddress[] addressList;

            if(rcManager != null) {
                addressList = rcManager.queryConnectedRemoteControlDevices();
                
                if(addressList != null) {
                    displayMessage("Query Connected Devices Success");
                    displayMessage("Addresses: ");
                    for(BluetoothAddress address : addressList)
                        displayMessage("    " + address);
                }
                else
                    displayMessage("Query Connected Devices Failure");
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeHidden();
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
            
        }
        
    };
    
    private final CommandHandler passthroughCommand_Handler = new CommandHandler() {
        final String[] operations = Utils.getEnumStringSet(RemoteControlPassThroughOperationID.values());
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            RemoteControlButtonState buttonState;
            RemoteControlPassThroughOperationID opID;
            byte[] commandData;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                    
                    if(getCommandParameterView(3).getValueText().text.toString().length() > 0)
                        commandData = Utils.hexToByteArray(getCommandParameterView(3).getValueText().text);
                    else
                        commandData = null;
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                catch(IllegalArgumentException e) {
                    displayMessage("Invalid Hex String");
                    return;
                }
                
                opID = RemoteControlPassThroughOperationID.valueOf(operations[getCommandParameterView(1).getValueSpinner().selectedItem]);
                buttonState = getCommandParameterView(2).getValueCheckbox().value?RemoteControlButtonState.PRESSED:RemoteControlButtonState.RELEASED;
                
                result = rcManager.sendPassthroughCommand(bluetoothAddress, timeout, opID, buttonState, commandData);
                
                displayMessage("");
                displayMessage("sendPassthroughCommand() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeSpinner("Operation ID", operations);
            getCommandParameterView(2).setModeCheckbox("Button Pressed", false);
            getCommandParameterView(3).setModeText("", "Command Data (hex)", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler vendorDependentCommand_Handler = new CommandHandler() {
        final String[] commandTypes = Utils.getEnumStringSet(RemoteControlCommandType.values());
        final String[] subunitTypes = Utils.getEnumStringSet(RemoteControlSubunitType.values());
        final String[] subunitIDs = Utils.getEnumStringSet(RemoteControlSubunitID.values());
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            int timeout;
            byte[] companyID;
            byte[] commandData;
            
            RemoteControlCommandType commandType;
            RemoteControlSubunitType subunitType;
            RemoteControlSubunitID subunitID;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                try {
                    companyID = Utils.hexToByteArray(getCommandParameterView(4).getValueText().text);
                    
                    if(companyID.length != 3)
                        displayMessage("Company ID must be 3 bytes");
                    
                    if(getCommandParameterView(5).getValueText().text.toString().length() > 0)
                        commandData = Utils.hexToByteArray(getCommandParameterView(5).getValueText().text);
                    else
                        commandData = null;
                } catch(IllegalArgumentException e) {
                    displayMessage("Invalid Hex String");
                    return;
                }
                
                
                
                commandType = RemoteControlCommandType.valueOf(commandTypes[getCommandParameterView(1).getValueSpinner().selectedItem]);
                subunitType = RemoteControlSubunitType.valueOf(subunitTypes[getCommandParameterView(2).getValueSpinner().selectedItem]);
                subunitID = RemoteControlSubunitID.valueOf(subunitIDs[getCommandParameterView(3).getValueSpinner().selectedItem]);
                
                result = rcManager.vendorDependentGenericCommand(bluetoothAddress, timeout, commandType, subunitType, subunitID, new CompanyID(companyID[0], companyID[1], companyID[2]), commandData);
                
                displayMessage("");
                displayMessage("vendorDependentGenericCommand() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeSpinner("Command Type", commandTypes);
            getCommandParameterView(2).setModeSpinner("Subunit Type", subunitTypes);
            getCommandParameterView(3).setModeSpinner("Subunit ID", subunitIDs);
            getCommandParameterView(4).setModeText("", "Company ID (hex)", InputType.TYPE_CLASS_TEXT);
            getCommandParameterView(5).setModeText("", "Command Data (hex)", InputType.TYPE_CLASS_TEXT);
            
        }

        @Override
        public void unselected() {
            getCommandParameterView(5).setModeHidden();
        }
        
    };
    
    private final CommandHandler groupNavigationCommand_Handler = new CommandHandler() {
        final String[] navTypes = Utils.getEnumStringSet(GroupNavigationType.values());
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            GroupNavigationType navType;
            RemoteControlButtonState buttonState;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                buttonState = getCommandParameterView(1).getValueCheckbox().value?RemoteControlButtonState.PRESSED:RemoteControlButtonState.RELEASED;
                
                navType = GroupNavigationType.valueOf(navTypes[getCommandParameterView(1).getValueSpinner().selectedItem]);
                
                result = rcManager.groupNavigation(bluetoothAddress, timeout, buttonState, navType);
                
                displayMessage("");
                displayMessage("groupNavigation() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeCheckbox("Button Pressed", false);
            getCommandParameterView(2).setModeSpinner("Navigation Type", navTypes);
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler getCapabilitiesCommand_Handler = new CommandHandler() {

        final String[] capabilities = Utils.getEnumStringSet(CapabilityID.values());
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                result = rcManager.getRemoteCapabilities(bluetoothAddress, timeout, CapabilityID.valueOf(capabilities[getCommandParameterView(1).getValueSpinner().selectedItem]));
                
                displayMessage("");
                displayMessage("getRemoteCapabilities() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeSpinner("Capability ID", capabilities);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler listPlayerApplicationSettingAttributesCommand_Handler = new CommandHandler() {

        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                result = rcManager.listPlayerApplicationSettingAttributes(bluetoothAddress, timeout);
                
                displayMessage("");
                displayMessage("listPlayerApplicationSettingAttributes() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler listPlayerApplicationSettingValuesCommand_Handler = new CommandHandler() {

        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            byte attributeID;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                    attributeID = Byte.parseByte(getCommandParameterView(1).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Integer Value");
                    return;
                }
                
                result = rcManager.listPlayerApplicationSettingValues(bluetoothAddress, timeout, attributeID);
                
                displayMessage("");
                displayMessage("listPlayerApplicationSettingValues() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeText("", "Attribute ID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    //XXX
    private final CommandHandler getCurrentPlayerApplicationSettingValueCommand_Handler = new CommandHandler() {

        final String[] hints = {
                "Attribute ID"
        };
        
        final int[] inputTypes = {
                InputType.TYPE_CLASS_NUMBER
        };
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            
            byte[] attributeIDs;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                String[][] textData = getCommandParameterView(1).getValueMultitext().textData;
                
                if(textData != null) {
                    attributeIDs = new byte[textData.length];
                    for(int i=0;i<textData.length;i++) {
                        try {
                            attributeIDs[i] = Byte.parseByte(textData[i][0]);
                            
                        } catch(NumberFormatException e) {
                            displayMessage("Invalid attribute ID");
                            return;
                        }
                    }
                }
                else {
                    displayMessage("No attributes entered");
                    return;
                }
                
                result = rcManager.getCurrentPlayerApplicationSettingValue(bluetoothAddress, timeout, attributeIDs);
                
                displayMessage("");
                displayMessage("getCurrentPlayerApplicationSettingValue() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeMultitext("AttributeIDs", 1, hints, inputTypes);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
  //XXX
    private final CommandHandler setPlayerApplicationSettingValueCommand_Handler = new CommandHandler() {
        final String[] hints = {
                "Attribute ID",
                "Value"
        };
        
        final int[] inputTypes = {
                InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_CLASS_NUMBER
        };
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            
            Map<Byte, Byte> attributeValues;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                String[][] textData = getCommandParameterView(1).getValueMultitext().textData;
                
                if(textData != null) {
                    attributeValues = new HashMap<Byte, Byte>();
                    
                    for(int i=0;i<textData.length;i++) {
                        try {
                            byte attributeID = Byte.parseByte(textData[i][0]);
                            byte value = Byte.parseByte(textData[i][1]);
                            
                            attributeValues.put(attributeID, value);
                            
                        } catch(NumberFormatException e) {
                            displayMessage("Invalid attribute ID");
                            return;
                        }
                    }
                }
                else {
                    displayMessage("No attributes entered");
                    return;
                }
                
                result = rcManager.setPlayerApplicationSettingValue(bluetoothAddress, timeout, attributeValues);
                
                displayMessage("");
                displayMessage("setPlayerApplicationSettingValue() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeMultitext("Attribute Values", 2, hints, inputTypes);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
  //XXX
    private final CommandHandler getPlayerApplicationSettingAttributeTextCommand_Handler = new CommandHandler() {
        final String[] hints = {
                "Attribute ID"
        };
        
        final int[] inputTypes = {
                InputType.TYPE_CLASS_NUMBER
        };
        
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            byte[] attributeIDs;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                String[][] textData = getCommandParameterView(1).getValueMultitext().textData;
                
                if(textData != null) {
                    attributeIDs = new byte[textData.length];
                    for(int i=0;i<textData.length;i++) {
                        try {
                            attributeIDs[i] = Byte.parseByte(textData[i][0]);
                            
                        } catch(NumberFormatException e) {
                            displayMessage("Invalid attribute ID");
                            return;
                        }
                    }
                }
                else {
                    displayMessage("No attributes entered");
                    return;
                }
                
                result = rcManager.getPlayerApplicationSettingAttributeText(bluetoothAddress, timeout, attributeIDs);
                
                displayMessage("");
                displayMessage("getPlayerApplicationSettingAttributeText() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeMultitext("Attribute IDs", 1, hints, inputTypes);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
  //XXX
    private final CommandHandler getPlayerApplicationSettingValueTextCommand_Handler = new CommandHandler() {
        final String[] hints = {
                "Value ID"
        };
        
        final int[] inputTypes = {
                InputType.TYPE_CLASS_NUMBER
        };
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            byte attributeID;
            byte[] valueIDs;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                    attributeID = Byte.parseByte(getCommandParameterView(1).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Number");
                    return;
                }
                
                String[][] textData = getCommandParameterView(1).getValueMultitext().textData;
                
                if(textData != null) {
                    valueIDs = new byte[textData.length];
                    for(int i=0;i<textData.length;i++) {
                        try {
                            valueIDs[i] = Byte.parseByte(textData[i][0]);
                            
                        } catch(NumberFormatException e) {
                            displayMessage("Invalid attribute ID");
                            return;
                        }
                    }
                }
                else {
                    displayMessage("No attributes entered");
                    return;
                }
                
                result = rcManager.getPlayerApplicationSettingValueText(bluetoothAddress, timeout, attributeID, valueIDs);
                
                displayMessage("");
                displayMessage("getPlayerApplicationSettingValueText() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeText("", "AttributeID", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(2).setModeMultitext("Value IDs", 1, hints, inputTypes);
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
  //XXX
    private final CommandHandler informDisplayableCharacterSetCommand_Handler = new CommandHandler() {
        final String[] hints = {
                "Character Set"
        };
        
        final int[] inputTypes = {
                InputType.TYPE_CLASS_NUMBER
        };
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            short[] charsets;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                String[][] textData = getCommandParameterView(1).getValueMultitext().textData;
                
                if(textData != null) {
                    charsets = new short[textData.length];
                    for(int i=0;i<textData.length;i++) {
                        try {
                            charsets[i] = Short.parseShort(textData[i][0]);
                            
                        } catch(NumberFormatException e) {
                            displayMessage("Invalid attribute ID");
                            return;
                        }
                    }
                }
                else {
                    displayMessage("No attributes entered");
                    return;
                }
                
                result = rcManager.informDisplayableCharacterSet(bluetoothAddress, timeout, charsets);
                
                displayMessage("");
                displayMessage("informDisplayableCharacterSet() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeMultitext("Character Sets", 1, hints, inputTypes);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler informBatteryStatusCommand_Handler = new CommandHandler() {

        final String[] statuses = Utils.getEnumStringSet(BatteryStatus.values());
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                result = rcManager.informBatteryStatusOfController(bluetoothAddress, timeout, BatteryStatus.valueOf(statuses[getCommandParameterView(1).getValueSpinner().selectedItem]));
                
                displayMessage("");
                displayMessage("informBatteryStatusOfController() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeSpinner("Battery Status", statuses);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler getElementAttributesCommand_Handler = new CommandHandler() {

        final String[] attributes = Utils.getEnumStringSet(ElementAttributeID.values());
        final boolean[] values = new boolean[attributes.length];
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            
            EnumSet<ElementAttributeID> attributeIDs;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                attributeIDs = EnumSet.noneOf(ElementAttributeID.class);
                
                for(int i=0;i<attributes.length;i++) {
                    if(getCommandParameterView(1).getValueChecklist().checkedItems[i])
                        attributeIDs.add(ElementAttributeID.valueOf(attributes[i]));
                }
                
                result = rcManager.getElementAttributes(bluetoothAddress, timeout, AVRCP.MEDIA_INDENTIFIER_CURRENTLY_PLAYING, attributeIDs);
                
                displayMessage("");
                displayMessage("getElementAttributes() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeChecklist("Attributes", attributes, values);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler getPlayStatusCommand_Handler = new CommandHandler() {

        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                result = rcManager.getPlayStatus(bluetoothAddress, timeout);
                
                displayMessage("");
                displayMessage("getPlayStatus() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeHidden();
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
        }

        @Override
        public void unselected() {
        }
        
    };
    
    private final CommandHandler registerNotificationCommand_Handler = new CommandHandler() {
        final String[] events = Utils.getEnumStringSet(EventID.values());
        
        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            EventID eventID;
            int interval;
            
            if(rcManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Timeout");
                    return;
                }
                
                eventID = EventID.valueOf(events[getCommandParameterView(1).getValueSpinner().selectedItem]);
                
                if(eventID.equals(EventID.PLAYBACK_POSITION_CHANGED))
                {
                    try {
                        interval = Integer.parseInt(getCommandParameterView(2).getValueText().text.toString());
                    } catch(NumberFormatException e) {
                        displayMessage("Invalid interval");
                        return;
                    }
                }
                else
                    interval = 0;
                
                result = rcManager.registerNotification(bluetoothAddress, timeout, eventID, interval);
                
                displayMessage("");
                displayMessage("registerNotification() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeSpinner("Event ID", events);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
            
            getCommandParameterView(2).setOnSpinnerItemChangedListener(new OnSpinnerItemChangedListener() {
                
                @Override
                public void onSpinnerItemChange(int newItem) {
                    if(newItem > 0) {
                        if(EventID.valueOf(events[newItem]).equals(EventID.PLAYBACK_POSITION_CHANGED))
                            getCommandParameterView(2).setModeText("", "Playback Interval", InputType.TYPE_CLASS_NUMBER);
                        else
                            getCommandParameterView(2).setModeHidden();
                    }
                    
                }
            });
            
        }

        @Override
        public void unselected() {
            getCommandParameterView(2).setOnSpinnerItemChangedListener(null);
        }
        
    };
    
    private final CommandHandler setAbsoluteVolumeCommand_Handler = new CommandHandler() {

        @Override
        public void run() {
            int                      result;
            BluetoothAddress         bluetoothAddress;
            
            int timeout;
            float volume;
            
            if(rcManager != null) {
                SeekbarValue volumeParameter;
                
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(MainActivity.this, R.string.errorInvalidBluetoothAddressToastMessage);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(getCommandParameterView(0).getValueText().text.toString());
                }
                catch(NumberFormatException e) {
                    displayMessage("Invalid Number Format");
                    return;
                }
                
                volumeParameter = getCommandParameterView(1).getValueSeekbar();
                
                volume = (float)volumeParameter.position/(float)volumeParameter.max;
                
                result = rcManager.setAbsoluteVolume(bluetoothAddress, timeout, volume);
                
                displayMessage("");
                displayMessage("setAbsoluteVolume() result: " + result);
            }
            
        }

        @Override
        public void selected() {
            getCommandParameterView(0).setModeText("", "Response Timeout", InputType.TYPE_CLASS_NUMBER);
            getCommandParameterView(1).setModeSeekbar("Volume", /* AVRCP max volume value*/0x7F, 0);
            getCommandParameterView(2).setModeHidden();
            getCommandParameterView(3).setModeHidden();
            getCommandParameterView(4).setModeHidden();
        }

        @Override
        public void unselected() {
        }
        
    };

    private final Command[] mCommands = new Command[]
    {
            new Command("Connection Request Response", connectionRequestResponse_Handler),
            new Command("Connect Remote Control", connectRemoteControl_Handler),
            new Command("Change Incoming Connection Flags", changeIncomingConnectionFlags_Handler),
            new Command("Disconnect Remote Control", disconnectRemoteControl_Handler),
            new Command("Query Connected Devices", queryConnectedDevices_Handler),
            new Command("Passthrough Command", passthroughCommand_Handler),
            new Command("Vendor Dependent Generic Command", vendorDependentCommand_Handler),
            new Command("Group Navigation Command", groupNavigationCommand_Handler),
            new Command("Get Capabilities Command", getCapabilitiesCommand_Handler),
            new Command("List Player Application Setting Attributes Comamnd", listPlayerApplicationSettingAttributesCommand_Handler),
            new Command("List Player Application Setting Values Command", listPlayerApplicationSettingValuesCommand_Handler),
            new Command("Get Current Player Application Setting Value Command", getCurrentPlayerApplicationSettingValueCommand_Handler),
            new Command("Set Player Application Setting Value Command", setPlayerApplicationSettingValueCommand_Handler),
            new Command("Get Player Application Setting Attribute Text Command", getPlayerApplicationSettingAttributeTextCommand_Handler),
            new Command("Get Player Application Setting Value Text Command", getPlayerApplicationSettingValueTextCommand_Handler),
            new Command("Inform Displayable Character Set Command", informDisplayableCharacterSetCommand_Handler),
            new Command("Inform Battery Status Command", informBatteryStatusCommand_Handler),
            new Command("Get Element Attributes Command", getElementAttributesCommand_Handler),
            new Command("Get Play Status Command", getPlayStatusCommand_Handler),
            new Command("Register Notification Command", registerNotificationCommand_Handler),
            new Command("Set Absolute Volume Command", setAbsoluteVolumeCommand_Handler)
    };


    
    

}
