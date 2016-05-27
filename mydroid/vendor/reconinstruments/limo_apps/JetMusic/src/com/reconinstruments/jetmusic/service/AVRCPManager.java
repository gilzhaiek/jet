package com.reconinstruments.jetmusic.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.stonestreetone.bluetopiapm.AVRCP;
import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.ServerNotReachableException;
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
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlDisconnectReason;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlPassThroughOperationID;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlResponseCode;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlSubunitID;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlSubunitType;
import com.stonestreetone.bluetopiapm.AVRCP.SystemStatus;
import com.stonestreetone.bluetopiapm.AVRCP.RemoteControlControllerManager.BaseControllerEventCallback;

import java.util.EnumSet;
import java.util.Map;

import com.reconinstruments.utils.BTHelper;

public class AVRCPManager {
    
    private static final String TAG = AVRCPManager.class.getSimpleName();
    
    public static final String ACTION_MUSIC_SERVICE_CHANGED = "com.reconinstruments.jetmusic.ACTION_MUSIC_SERVICE_CHANGED";
    public static final String ACTION_MUSIC_PLAY_CHANGED = "com.reconinstruments.jetmusic.ACTION_MUSIC_PLAY_CHANGED";
    public static final String ACTION_MUSIC_CONTENT_CHANGED = "com.reconinstruments.jetmusic.ACTION_MUSIC_CONTENT_CHANGED";
    public static final String EXTRA_MUSIC_STATE = "STATE";
    public static final int MUSIC_STATE_DISCONNECTED = 0;
    public static final int MUSIC_STATE_CONNECTED = 1;
    public static final String EXTRA_MUSIC_PLAY_STATE = "PLAY_STATE";
    public static final int PLAY_STATE_STOP = 0;
    public static final int PLAY_STATE_PLAY = 1;
    public static final int PLAY_STATE_PAUSE = 2;
    public static final String EXTRA_MUSIC_TITLE = "TITLE";
    public static final String EXTRA_MUSIC_ARTIST = "ARTIST";
    public static final String EXTRA_MUSIC_ALBUM = "ALBUM";
    
    private static AVRCPManager instance = null;
    private Context mContext = null;
    private BTHelper mBTHelper;
    
    //cache the most recent status
    private PlayStatus mPlayStatus;
    private String mMostRecentTitle = "";
    private String mMostRecentArtist = "";
    private String mMostRecentAlbum = "";
    // the signal to indicate if the command has been processed
    private int mPassthroughID = -1; 
    
    private Handler mHandler = new Handler();
    
    private RemoteControlControllerManager rcManager;
    
    protected AVRCPManager(Context context) {
        mContext = context;
        mBTHelper = BTHelper.getInstance(mContext);
        if(mBTHelper.getBTConnectionState() == 2){ //connected
            boolean profileEnabled = profileEnable();
            Log.d(TAG, "profileEnabled = " + profileEnabled);
        }
    }

    public static AVRCPManager getInstance(Context context) {
        if (instance == null) {
            instance = new AVRCPManager(context);
        }
        return instance;
    }

    public boolean profileEnable() {
        synchronized(this) {
            try {
                if(rcManager == null) {
                    rcManager = new RemoteControlControllerManager(rcControllerEventCallback);
                }
                cleanup();
                connectRemoteControl();
                return true;
            } catch(ServerNotReachableException e) {
                return false;
            }
            catch(RemoteControlAlreadyRegisteredException e) {
                return false;
            }
        }
    }
    
    public void profileDisable() {
        synchronized(this) {
            if(rcManager != null) {
                
                //broadcast music disconnected event
                Intent intent = new Intent(ACTION_MUSIC_SERVICE_CHANGED);
                intent.putExtra(EXTRA_MUSIC_STATE, MUSIC_STATE_DISCONNECTED);
                mContext.sendBroadcast(intent);
                mHandler.postDelayed(new Runnable() { // put the command to the queue
                    public void run() {
                        disconnectRemoteControl();
                    }
                }, 300);
                
            }
        }
    }

    private int getNumberProfileParameters() {
        return 0;
    }
    
    private int getNumberCommandParameters() {
        return 6;
    }
    
    private BaseControllerEventCallback rcControllerEventCallback = new BaseControllerEventCallback() {

        @Override
        public void incomingConnectionRequestEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();
            
            sb.append("Connection Request: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void remoteControlConnectedEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();
            
            sb.append("Remote Control Connected: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            
            Log.d(TAG, sb.toString());
            
            //query the capabilities
            getCapabilities(CapabilityID.EVENTS_SUPPORTED);
            
            registerNotification(EventID.PLAYBACK_STATUS_CHANGED);
            registerNotification(EventID.TRACK_CHANGED);
            
            EnumSet<ElementAttributeID> attributeIDs = EnumSet.noneOf(ElementAttributeID.class);
            attributeIDs.add(ElementAttributeID.TITLE);
            attributeIDs.add(ElementAttributeID.ARTIST);
            attributeIDs.add(ElementAttributeID.ALBUM);
            getElementAttributes(attributeIDs);
            
            //broadcast music connected event, give 0.5 second postDelay
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    Intent intent = new Intent(ACTION_MUSIC_SERVICE_CHANGED);
                    intent.putExtra(EXTRA_MUSIC_STATE, MUSIC_STATE_CONNECTED);
                    mContext.sendBroadcast(intent);
                }
            }, 500);

        }

        @Override
        public void remoteControlConnectionStatusEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress, ConnectionStatus connectionStatus) {
            StringBuilder sb = new StringBuilder();
            
            sb.append("Remote Control Connection Status: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    Status: ").append(connectionStatus);
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void remoteControlDisconnectedEvent(AVRCP manager, BluetoothAddress remoteDeviceAddress, RemoteControlDisconnectReason disconnectReason) {
            StringBuilder sb = new StringBuilder();

            sb.append("Remote Control Disconnected: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    Reason: ").append(disconnectReason);
            
            Log.d(TAG, sb.toString());
            
            //broadcast music disconnected event
            Intent intent = new Intent(ACTION_MUSIC_SERVICE_CHANGED);
            intent.putExtra(EXTRA_MUSIC_STATE, MUSIC_STATE_DISCONNECTED);
            mContext.sendBroadcast(intent);
            
            cleanup();
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
            
            Log.d(TAG, sb.toString());
            
            //complete processing the previous command, now is ready to process the next one
            if(getPassthroughID() == transactionID){
                setPassthroughID(-1);
            }
            
            if(operationID == RemoteControlPassThroughOperationID.BACKWARD && buttonState == RemoteControlButtonState.RELEASED){//send button pressed
                forwardOrBackward(RemoteControlPassThroughOperationID.BACKWARD, RemoteControlButtonState.RELEASED, null);
            }
            if(operationID == RemoteControlPassThroughOperationID.FORWARD && buttonState == RemoteControlButtonState.RELEASED){//send button pressed
                forwardOrBackward(RemoteControlPassThroughOperationID.FORWARD, RemoteControlButtonState.RELEASED, null);
            }
            if(operationID == RemoteControlPassThroughOperationID.PLAY && buttonState == RemoteControlButtonState.RELEASED){//send button pressed
                playOrPause(RemoteControlButtonState.RELEASED, null);
            }
            if(operationID == RemoteControlPassThroughOperationID.PAUSE && buttonState == RemoteControlButtonState.RELEASED){//send button pressed
                playOrPause(RemoteControlButtonState.RELEASED, null);
            }
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
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void groupNavigationResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, RemoteControlButtonState buttonState) {
            StringBuilder sb = new StringBuilder();

            sb.append("Group Navigation Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Button State: ").append(buttonState);
            
            Log.d(TAG, sb.toString());
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
            
            Log.d(TAG, sb.toString());
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
            
            Log.i(TAG, sb.toString());
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
            
            Log.d(TAG, sb.toString());
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
            
            Log.d(TAG, sb.toString());
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
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void setPlayerApplicationSettingValueResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Set Player Application Setting Value Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            
            Log.d(TAG, sb.toString());
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
            
            Log.d(TAG, sb.toString());
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
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void informDisplayableCharacterSetResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Inform Displayable Character Set Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void informBatteryStatusResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Inform Battery Status Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void getElementAttributesResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, ElementAttribute[] attributes) {
            StringBuilder sb = new StringBuilder();

            sb.append("Get Element Attributes Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Element Attributes:");
            
            String title = "";
            String artist = "";;
            String album = "";;
            
            for(ElementAttribute attribute : attributes) {
                sb.append("\n        ID: ").append(attribute.attributeID);
                sb.append("\n        Character Set: ").append(attribute.characterSet);
                
                String text = attribute.decodeAttributeData();
                
                if(text != null)
                    sb.append("\n        Text: ").append(text);
                else
                    sb.append("\n        Text: Unknown character set");
                
                sb.append("\n");
                
                if(text != null && attribute.attributeID.name().equals(EXTRA_MUSIC_TITLE) ){
                    title = text;
                    mMostRecentTitle = text;
                }
                if(text != null && attribute.attributeID.name().equals(EXTRA_MUSIC_ARTIST) ){
                    artist = text;
                    mMostRecentArtist = text;
                }
                if(text != null && attribute.attributeID.name().equals(EXTRA_MUSIC_ALBUM) ){
                    album = text;
                    mMostRecentAlbum = text;
                }
                
            }
            
            Intent intent = new Intent(ACTION_MUSIC_CONTENT_CHANGED);
            intent.putExtra(EXTRA_MUSIC_TITLE, title);
            intent.putExtra(EXTRA_MUSIC_ARTIST, artist);
            intent.putExtra(EXTRA_MUSIC_ALBUM, album);
            mContext.sendBroadcast(intent);
            
            Log.d(TAG, sb.toString());
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
            
            Log.d(TAG, sb.toString());
            
        }

        @Override
        public void playbackStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, PlayStatus playStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append("Playback Status Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Status: ").append(playStatus);
            
            Log.d(TAG, sb.toString());
            
            //set current play status
            setCurrentPlayStatus(playStatus);
            if(responseCode == RemoteControlResponseCode.CHANGED){ //register again

                Intent intent = new Intent(ACTION_MUSIC_PLAY_CHANGED);
                if(playStatus == PlayStatus.PAUSED){
                    intent.putExtra(EXTRA_MUSIC_STATE, PLAY_STATE_PAUSE);
                }else if(playStatus == PlayStatus.PLAYING){
                    intent.putExtra(EXTRA_MUSIC_STATE, PLAY_STATE_PLAY);
                }else{
                    intent.putExtra(EXTRA_MUSIC_STATE, PLAY_STATE_STOP);
                }
                
                mContext.sendBroadcast(intent);

                registerNotification(EventID.PLAYBACK_STATUS_CHANGED);
                
                EnumSet<ElementAttributeID> attributeIDs = EnumSet.noneOf(ElementAttributeID.class);
                attributeIDs.add(ElementAttributeID.TITLE);
                attributeIDs.add(ElementAttributeID.ARTIST);
                attributeIDs.add(ElementAttributeID.ALBUM);
                getElementAttributes(attributeIDs);
            }
            
        }

        @Override
        public void trackChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, long identifier) {
            StringBuilder sb = new StringBuilder();

            sb.append("Track Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Identifier: ").append(identifier);
            
            Log.d(TAG, sb.toString());
            
            if(responseCode == RemoteControlResponseCode.CHANGED){ //register again
                registerNotification(EventID.TRACK_CHANGED);
                
                //get current element attr
                EnumSet<ElementAttributeID> attributeIDs = EnumSet.noneOf(ElementAttributeID.class);
                attributeIDs.add(ElementAttributeID.TITLE);
                attributeIDs.add(ElementAttributeID.ARTIST);
                attributeIDs.add(ElementAttributeID.ALBUM);
                getElementAttributes(attributeIDs);
            }
        }

        @Override
        public void trackReachedEndNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Track Reached End Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void trackReachedStartNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Track Reached Start Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void playbackPositionChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, int position) {
            StringBuilder sb = new StringBuilder();

            sb.append("Playback Position Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Position: ").append(position);
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void batteryStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, BatteryStatus batteryStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append("Battery Status Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Status: ").append(batteryStatus);
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void systemStatusChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, SystemStatus systemStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append("System Status Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Status: ").append(systemStatus);
            
            Log.d(TAG, sb.toString());
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
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void volumeChangedNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, float volume) {
            StringBuilder sb = new StringBuilder();

            sb.append("Volume Changed Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Volume: ").append(volume);
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void setAbsoluteVolumeResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, float volume) {
            StringBuilder sb = new StringBuilder();

            sb.append("Set Absolute Volume Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Volume: ").append(volume);
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void commandRejectedResponse(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, RemoteControlResponseCode responseCode, CommandErrorCode errorCode) {
            StringBuilder sb = new StringBuilder();

            sb.append("Command Rejected Response: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Response Code: ").append(responseCode);
            sb.append("\n    Error Code: ").append(errorCode);
            
            Log.d(TAG, sb.toString());
        }

        @Override
        public void commandFailureNotification(RemoteControlControllerManager manager, BluetoothAddress remoteDeviceAddress, int transactionID, CommandFailureStatus status) {
            StringBuilder sb = new StringBuilder();

            sb.append("Command Failure Notification: ");
            sb.append("\n    Address: ").append(remoteDeviceAddress);
            sb.append("\n    TransactionID: ").append(transactionID);
            sb.append("\n    Status: ").append(status);
            
            Log.d(TAG, sb.toString());
            
            //reset the connection when command failure.
            reset();
        }
        
    };
    
    private void cleanup(){
        setPassthroughID(-1);
        mPlayStatus = PlayStatus.ERROR;
        mMostRecentTitle = "";
        mMostRecentArtist = "";
        mMostRecentAlbum = "";
    }
    
    public void reset(){
        cleanup();
        disconnectRemoteControl();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                connectRemoteControl();
            }
        }, 300);
    }
    
    public void connectionRequestResponse(final BluetoothAddress bluetoothAddress){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    // acceptConnection must be true
                    result = rcManager.connectionRequestResponse(bluetoothAddress, true);
                    Log.d(TAG, "connectionRequestResponse() result: " + result);
                }
            }
        }.start();
    }
    
    public void connectRemoteControl(){
        new Thread() {
            @Override
            public void run() {
                int result;
                EnumSet<ConnectionFlags> connectionFlags;
                if(rcManager != null) {
                    connectionFlags = EnumSet.noneOf(ConnectionFlags.class);
                    connectionFlags.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
//                    connectionFlags.add(ConnectionFlags.REQUIRE_ENCRYPTION);
                    // waitForConnection must be true
                    result = rcManager.connectRemoteControl(new BluetoothAddress(mBTHelper.getLastPairedDeviceAddress()), connectionFlags, true);
                    Log.d(TAG, "connectRemoteControl() result: " + result);
                }else{
                    profileEnable();
                }
            }
        }.start();
    }
    
    public void changeIncomingConnectionFlags(final EnumSet<IncomingConnectionFlags> connectionFlags){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.changeIncomingConnectionFlags(connectionFlags);
                    Log.d(TAG, "changeIncomingConnectionFlags() result: " + result);
                }
            }
        }.start();
    }
    
    public void disconnectRemoteControl(){
        mHandler.removeCallbacksAndMessages(null); //clean the message queue when it's disconnected
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    try{
                    result = rcManager.disconnectRemoteControl(new BluetoothAddress(mBTHelper.getLastPairedDeviceAddress()));
                    Log.d(TAG, "disconnectRemoteControl() result: " + result);
                    rcManager.dispose();
                    rcManager = null;
                    }catch(IllegalArgumentException iae){
                        //don't do anything.
                    }
                }
            }
        }.start();
    }
    
    public void queryConnectedDevices(){
        new Thread() {
            @Override
            public void run() {
                BluetoothAddress[] addressList;
                if(rcManager != null) {
                    addressList = rcManager.queryConnectedRemoteControlDevices();
                     if(addressList != null) {
                        Log.d(TAG, "Query Connected Devices Success");
                        Log.d(TAG, "Addresses: ");
                        for(BluetoothAddress address : addressList)
                            Log.d(TAG, "    " + address);
                    }
                    else
                        Log.d(TAG, "Query Connected Devices Failure");
                }
            }
        }.start();
    }
    
    public void playOrPause(final RemoteControlButtonState buttonState, final byte[] commandData){
        new Thread() {
            @Override
            public void run() {
                int result;
                RemoteControlPassThroughOperationID opID;
                if(rcManager != null) {
                    if(getCurrentPlayStatus() == PlayStatus.PLAYING){
                        opID = RemoteControlPassThroughOperationID.PAUSE;
                    }else{
                        opID = RemoteControlPassThroughOperationID.PLAY;
                    }
                    result = rcManager.sendPassthroughCommand(new BluetoothAddress(mBTHelper.getLastPairedDeviceAddress()), 1000, opID, buttonState, commandData);
                    Log.d(TAG, "passthrough() " + opID + " " + buttonState + " result: " + result);
                    
                    if(result > 0){
                        setPassthroughID(result);
                    }else{
                        setPassthroughID(-1);
                    }
                }
            }
        }.start();
    }
    
    public void forwardOrBackward(final RemoteControlPassThroughOperationID opID, final RemoteControlButtonState buttonState, final byte[] commandData){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.sendPassthroughCommand(new BluetoothAddress(mBTHelper.getLastPairedDeviceAddress()), 1000, opID, buttonState, commandData);
                    Log.d(TAG, "forwardOrBackward()  " + opID + " " + buttonState + " result: " + result);
                    
                    if(result > 0){
                        setPassthroughID(result);
                    }else{
                        setPassthroughID(-1);
                    }
                }
            }
        }.start();
    }
    
    public void vendorDependent(final BluetoothAddress bluetoothAddress, final RemoteControlCommandType commandType, final RemoteControlSubunitType subunitType, final RemoteControlSubunitID subunitID, final byte[] companyID, final byte[] commandData){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.vendorDependentGenericCommand(bluetoothAddress, 1000, commandType, subunitType, subunitID, new CompanyID(companyID[0], companyID[1], companyID[2]), commandData);
                    Log.d(TAG, "vendorDependent() result: " + result);
                }
            }
        }.start();
    }
    
    public void groupNavigation(final BluetoothAddress bluetoothAddress, final RemoteControlButtonState buttonState, final GroupNavigationType navType){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.groupNavigation(bluetoothAddress, 1000, buttonState, navType);
                    Log.d(TAG, "groupNavigation() result: " + result);
                }
            }
        }.start();
    }
    
    public void getCapabilities(final CapabilityID capabilityID){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.getRemoteCapabilities(new BluetoothAddress(mBTHelper.getLastPairedDeviceAddress()), 1000, capabilityID);
                    Log.d(TAG, "getCapabilities() result: " + result);
                }
            }
        }.start();
    }
    
    public void listPlayerApplicationSettingAttributes(final BluetoothAddress bluetoothAddress){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.listPlayerApplicationSettingAttributes(bluetoothAddress, 1000);
                    Log.d(TAG, "listPlayerApplicationSettingAttributes() result: " + result);
                }
            }
        }.start();
    }
    
    public void listPlayerApplicationSettingValues(final BluetoothAddress bluetoothAddress, final byte attributeID){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.listPlayerApplicationSettingValues(bluetoothAddress, 1000, attributeID);
                    Log.d(TAG, "listPlayerApplicationSettingValues() result: " + result);
                }
            }
        }.start();
    }
    
    public void getCurrentPlayerApplicationSettingValue(final BluetoothAddress bluetoothAddress, final byte[] attributeIDs){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.getCurrentPlayerApplicationSettingValue(bluetoothAddress, 1000, attributeIDs);
                    Log.d(TAG, "getCurrentPlayerApplicationSettingValue() result: " + result);
                }
            }
        }.start();
    }
    
    public void setPlayerApplicationSettingValue(final BluetoothAddress bluetoothAddress, final Map<Byte, Byte> attributeValues){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.setPlayerApplicationSettingValue(bluetoothAddress, 1000, attributeValues);
                    Log.d(TAG, "setPlayerApplicationSettingValue() result: " + result);
                }
            }
        }.start();
    }
    
    public void getPlayerApplicationSettingAttributeText(final BluetoothAddress bluetoothAddress, final byte[] attributeIDs){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.getPlayerApplicationSettingAttributeText(bluetoothAddress, 1000, attributeIDs);
                    Log.d(TAG, "getPlayerApplicationSettingAttributeText() result: " + result);
                }
            }
        }.start();
    }
    
    public void getPlayerApplicationSettingValueText(final BluetoothAddress bluetoothAddress, final byte attributeID, final byte[] valueIDs){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.getPlayerApplicationSettingValueText(bluetoothAddress, 1000, attributeID, valueIDs);
                    Log.d(TAG, "getPlayerApplicationSettingValueText() result: " + result);
                }
            }
        }.start();
    }
    
    public void informDisplayableCharacterSet(final BluetoothAddress bluetoothAddress, final short[] charsets){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.informDisplayableCharacterSet(bluetoothAddress, 1000, charsets);
                    Log.d(TAG, "informDisplayableCharacterSet() result: " + result);
                }
            }
        }.start();
    }
    
    public void informBatteryStatus(final BluetoothAddress bluetoothAddress, final BatteryStatus batteryStatus){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.informBatteryStatusOfController(bluetoothAddress, 1000, batteryStatus);
                    Log.d(TAG, "informBatteryStatus() result: " + result);
                }
            }
        }.start();
    }
    
    public void getElementAttributes(final EnumSet<ElementAttributeID> attributeIDs){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.getElementAttributes(new BluetoothAddress(mBTHelper.getLastPairedDeviceAddress()), 1000, AVRCP.MEDIA_INDENTIFIER_CURRENTLY_PLAYING, attributeIDs);
                    Log.d(TAG, "getElementAttributes() result: " + result);
                }
            }
        }.start();
    }
    
    public void getPlayStatus(){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.getPlayStatus(new BluetoothAddress(mBTHelper.getLastPairedDeviceAddress()), 1000);
                    Log.d(TAG, "getPlayStatus() result: " + result);
                }
            }
        }.start();
    }
    
    public void registerNotification(final EventID eventID){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    if(!eventID.equals(EventID.PLAYBACK_POSITION_CHANGED)){
                        result = rcManager.registerNotification(new BluetoothAddress(mBTHelper.getLastPairedDeviceAddress()), 1000, eventID, 0);
                    }else{
                        result = rcManager.registerNotification(new BluetoothAddress(mBTHelper.getLastPairedDeviceAddress()), 1000, eventID, 500);
                    }
                    Log.d(TAG, "registerNotification() result: " + result);
                }
            }
        }.start();
    }
    
    public void setAbsoluteVolume(final BluetoothAddress bluetoothAddress, final float volume){
        new Thread() {
            @Override
            public void run() {
                int result;
                if(rcManager != null) {
                    result = rcManager.setAbsoluteVolume(bluetoothAddress, 1000, volume);
                    Log.d(TAG, "setAbsoluteVolume() result: " + result);
                }
            }
        }.start();
    }
    
    public void setCurrentPlayStatus(PlayStatus playStatus){
        synchronized (this) {
            mPlayStatus = playStatus;
        }
    }
    
    public PlayStatus getCurrentPlayStatus(){
        synchronized (this) {
            return mPlayStatus;
        }
    }
    
    public void setPassthroughID(int passthroughID){
        synchronized (this) {
            mPassthroughID = passthroughID;
        }
    }
    
    public int getPassthroughID(){
        synchronized (this) {
            return mPassthroughID;
        }
    }
    
    public boolean isProcessing(){
        return getPassthroughID()>0? true:false;
    }

    public PlayStatus getMostRecentPlayStatus() {
        return mPlayStatus;
    }

    public String getMostRecentTitle() {
        return mMostRecentTitle;
    }

    public String getMostRecentArtist() {
        return mMostRecentArtist;
    }
    public String getMostRecentAlbum() {
        return mMostRecentAlbum;
    }
}
