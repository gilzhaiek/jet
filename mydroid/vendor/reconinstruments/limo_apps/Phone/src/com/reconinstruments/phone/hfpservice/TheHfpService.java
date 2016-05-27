/*
 * <TheHfpService.java >
 * Copyright 2011 Stonestreet One. All Rights Reserved.
 *
 * TheHfpService for Recon Instruments. Based on sample code from SS1
 *
 * Author: Ali R. M.
 */
package com.reconinstruments.phone.hfpservice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.stonestreetone.bluetopiapm.*;
import com.stonestreetone.bluetopiapm.DEVM.*;
import com.stonestreetone.bluetopiapm.HFRM.AudioGatewayServerManager.MultipartyFeatures;
import com.stonestreetone.bluetopiapm.HFRM.AudioGatewayServerManager.SupportedFeatures;
import com.stonestreetone.bluetopiapm.HFRM.*;
import com.stonestreetone.bluetopiapm.HFRM.HandsFreeServerManager.HandsFreeEventCallback;
import com.stonestreetone.bluetopiapm.HFRM.HandsFreeServerManager.LocalConfiguration;
import com.stonestreetone.bluetopiapm.SPPM.SerialPortClientManager;
import com.stonestreetone.bluetopiapm.SPPM.ServiceRecordInformation;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;


/**
 * Quick put together of the Hfp Service
 *
 * @author Ali R. M.
 */
public class TheHfpService extends Service {

    private static final String LOG_TAG = "TheHfpService2";
    private static final String TAG = "TheHfpService2";
    private int ringCounter = 0;
    private boolean inCall = false;
    private boolean incomingCall = false;


    private static interface CommandHandler extends Runnable {

        /**
         * Indicate that the command associated with this handler has been
         * selected. The handler should signal the GUI thread to update the user
         * interface as necessary. Usually, this means activating/deactivating
         * input fields, changing input formats and hints, etc.
         */
        public void selected();

        /**
         * Indicate that the command associated with this handler was previously
         * selected but that the current selection is about to change. It is
         * recommended that Handlers can use this notification to save user
         * state.
         */
        public void unselected();
    }

    private static class Command implements Map<String, Object> {
        private final String name;
        private final CommandHandler handler;

        public final static String KEY_NAME = "name";
        public final static String KEY_HANDLER = "handler";

        public Command(String name, CommandHandler handler) {
            this.name    = name;
            this.handler = handler;
        }

        public String getName() {
            return name;
        }

        public CommandHandler getHandler() {
            return handler;
        }

        @Override
        public void clear() {
        }

        @Override
        public boolean containsKey(Object key) {
            String stringKey = key.toString();

            if((stringKey == KEY_NAME) || (stringKey == KEY_HANDLER)) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean containsValue(Object value) {
            if((value instanceof String) && (name.equals(value.toString()))) {
                return true;
            } else if(handler.equals(value)) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            HashSet<Entry<String,Object>> set = new HashSet<Entry<String,Object>>(2);

            set.add(new SimpleImmutableEntry<String,Object>(KEY_NAME, name));
            set.add(new SimpleImmutableEntry<String,Object>(KEY_HANDLER, handler));

            return set;
        }

        @Override
        public Object get(Object key) {
            if(KEY_NAME.equals(key)) {
                return name;
            } else if(KEY_HANDLER.equals(key)) {
                return handler;
            } else {
                return null;
            }
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Set<String> keySet() {
            HashSet<String> set = new HashSet<String>(2);

            set.add(KEY_NAME);
            set.add(KEY_HANDLER);

            return set;
        }

        @Override
        public Object put(String key, Object value) {
            if(KEY_NAME.equals(key)) {
                return name;
            } else if(KEY_HANDLER.equals(key)) {
                return handler;
            } else {
                return null;
            }
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> map) {
        }

        @Override
        public Object remove(Object key) {
            if(KEY_NAME.equals(key)) {
                return name;
            } else if(KEY_HANDLER.equals(key)) {
                return handler;
            } else {
                return null;
            }
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public Collection<Object> values() {
            ArrayList<Object> array = new ArrayList<Object>(2);

            array.add(name);
            array.add(handler);

            return array;
        }
    }

    /*package*/ Resources resourceManager;

    // Ali
    BluetoothAddress mRemoteDeviceAddress=null;
    String mRemoteDevicePhoneNumber = null;
    //End Ali
    @Override
    public IBinder onBind (Intent intent)    {
        return null;		// for now
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG,"onCreate");
        BluetoothAdapter bluetoothAdapter;

        /*
         * Register a receiver for a Bluetooth "State Changed" broadcast event.
         */
        registerReceiver(bluetoothBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(hfpCommandReceiver, new IntentFilter("RECON_SS1_HFP_COMMAND"));

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null) {
            if(bluetoothAdapter.isEnabled()) {
                profileEnable();
            } else {
            }
        }
        resourceManager = getResources();

    }

    @Override
    public int onStartCommand(Intent i, int flag, int startId)    {
        return START_STICKY;

    }

    private final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                switch(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    case BluetoothAdapter.STATE_ON:
                        profileEnable();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        profileDisable();
                        break;
                }
            }
        }
    };

    /*package*/ BluetoothAddress getRemoteDeviceAddress() {
        return mRemoteDeviceAddress;
    }

    /*package*/ void displayMessage(CharSequence string) {
        //        Message.obtain(uiThreadMessageHandler, MESSAGE_DISPLAY_MESSAGE, string).sendToTarget();
        Log.v(TAG,string.toString());
    }

    /*package*/ static void showToast(Context context, int resourceID) {
        //Toast.makeText(context, resourceID,1000).show();
    } // This is UI shit;

    /*package*/ static void showToast(Context context, String message) {
        //Toast.makeText(context, message, 1000).show();
        Log.v(TAG,message);
    } // This is UI Shit


    /*==========================================================
     * Profile-specific content below this line.
     */

    /*package*/ HFRM.HandsFreeServerManager handsFreeServerManager;

    DEVM  deviceManager;
    // Need this:
    private final void profileEnable() {
        Log.v(TAG,"profileEnable");
        synchronized(this) {

            try {
                deviceManager = new DEVM(deviceEventCallback);
                mSPCM = new SerialPortClientManager(null);
            }
            catch (ServerNotReachableException e) {
                Log.e(TAG,"ServerNotReachableException");
                deviceManager = null;
                mSPCM = null;

            }

            if(handsFreeServerManager != null) {
                handsFreeServerManager.dispose();
            }

            try {
                // TODO The request for a "controlling" manager (first parameter) is not guaranteed. Handle the case where it fails.
                handsFreeServerManager = new HandsFreeServerManager(true, handsFreeEventCallbackHandler);
                //showToast(this, "Registered as the Controller Manager");
                Log.v(TAG, "Registered as the Controller Manager");
            } catch(BluetopiaPMException e) {
                handsFreeServerManager = null;
            }

            if(handsFreeServerManager == null) {
                try {
                    handsFreeServerManager = new HandsFreeServerManager(false, handsFreeEventCallbackHandler);
                    //showToast(this, "Registered as a non-controller Manager");
                    Log.v(TAG, "Registered as a non-controller Manager");
                } catch(ServerNotReachableException e) {
                    /*
                     * BluetopiaPM server couldn't be contacted.
                     * This should never happen if Bluetooth was
                     * successfully enabled.
                     */
                    //showToast(this, R.string.errorBTPMServerNotReachableToastMessage);
                    Log.w(TAG, "errorBTPMServerNotReachableToastMessage");
                    BluetoothAdapter.getDefaultAdapter().disable();
                }
            }
        }
    }

    // Need this
    private final void profileDisable() {
        synchronized(TheHfpService.this) {
            if(handsFreeServerManager != null) {
                handsFreeServerManager.dispose();
                handsFreeServerManager = null;
            }
        }
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

    void callHandle(boolean value) {
        if (!value) {
            Log.v(TAG,"sending call ended");
            // TODO: send the broadcast that call was ended.
            Intent i = new Intent ("HFP_CALL_STATUS_CHANGED");
            i.putExtra("event","CALL_ENDED");
            sendBroadcast(i);
            inCall = false;
            incomingCall = false;

            AudioManager audioManager;
            if((audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE)) != null) {
                audioManager.setMode(AudioManager.MODE_NORMAL);
                audioManager.setBluetoothScoOn(false);
                displayMessage("Disabled Bluetooth SCO audio routing in AudioManager.");
            }
        }
        else  {
            // TODO: indiscate call connected:
            Log.v(TAG,"sending call started");
            Intent i = new Intent ("HFP_CALL_STATUS_CHANGED");
            i.putExtra("event","CALL_STARTED");
            sendBroadcast(i);
            // hack Ali: Potentially move this
            Log.v(TAG,"releasing Audio");
            releaseSCOAudioConnection_Handler.run();
            inCall = true;
            incomingCall = false;

        }
        mRemoteDevicePhoneNumber = null;
        ringCounter = 0;

    }

    private final HandsFreeEventCallback handsFreeEventCallbackHandler = new HandsFreeEventCallback() {

        @Override
        public void voiceRecognitionIndicationEvent(BluetoothAddress remoteDeviceAddress, boolean voiceRecognitionActive) {
            StringBuilder sb = new StringBuilder();

            sb.append("voiceRecognitionIndicationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(voiceRecognitionActive);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void speakerGainIndicationEvent(BluetoothAddress remoteDeviceAddress, int speakerGain) {
            StringBuilder sb = new StringBuilder();

            sb.append("speakerGainIndicationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Gain: ").append(speakerGain);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void microphoneGainIndicationEvent(BluetoothAddress remoteDeviceAddress, int microphoneGain) {
            StringBuilder sb = new StringBuilder();

            sb.append("microphoneGainIndicationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Gain: ").append(microphoneGain);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void incomingConnectionRequestEvent(BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();

            sb.append("incomingConnectionRequestEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);
        }


        // Need this:
        @Override
        public void incomingCallStateIndicationEvent(BluetoothAddress remoteDeviceAddress, CallState callState) {
            StringBuilder sb = new StringBuilder();

            sb.append("incomingCallStateIndicationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(callState);

            displayMessage("");
            displayMessage(sb);
        }

        // Need this;
        @Override
        public void disconnectedEvent(BluetoothAddress remoteDeviceAddress, DisconnectedReason reason) {
            StringBuilder sb = new StringBuilder();

            sb.append("disconnectedEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(reason);

            displayMessage("");
            displayMessage(sb);

            broadcastConnectionStateChange(remoteDeviceAddress, STATE_DISCONNECTED, STATE_CONNECTED);
        }

        // Need this;
        @Override
        public void connectionStatusEvent(BluetoothAddress remoteDeviceAddress, ConnectionStatus connectionStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append("connectionStatusEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(connectionStatus);

            displayMessage("");
            displayMessage(sb);
        }

        // Need this
        @Override
        public void connectedEvent(BluetoothAddress remoteDeviceAddress) {
            Log.v(TAG,"connectedEvent");
            StringBuilder sb = new StringBuilder();

            sb.append("connectedEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);

            broadcastConnectionStateChange(remoteDeviceAddress, STATE_CONNECTED, STATE_DISCONNECTED);

        }

        @Override
        public void audioDisconnectedEvent(BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();
            AudioManager  audioManager;

            sb.append("audioDisconnectedEvent").append(": ");
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

            sb.append("audioDataEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(audioData.length).append(" bytes");

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void audioConnectionStatusEvent(BluetoothAddress remoteDeviceAddress, boolean successful) {
            StringBuilder sb = new StringBuilder();

            sb.append("audioConnectionStatusEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Successful: ").append(successful);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void audioConnectedEvent(BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();
            AudioManager  audioManager;

            sb.append("audioConnectedEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());

            displayMessage("");
            displayMessage(sb);

            if((audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE)) != null) {
                displayMessage("Setting audio mode to IN_CALL");
                audioManager.setMode(AudioManager.MODE_IN_CALL);

                displayMessage("Enabling Bluetooth SCO audio routing");
                audioManager.setBluetoothScoOn(true);
            }

            Log.v(TAG,"Not releasing audio now");
            if (inCall) {
                // hack Ali: Potentially move this
                releaseSCOAudioConnection_Handler.run();
            }

        }

        @Override
        public void voiceTagRequestConfirmationEvent(BluetoothAddress remoteDeviceAddress, String phoneNumber) {
            StringBuilder sb = new StringBuilder();
            sb.append("voiceTagRequestConfirmationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(phoneNumber);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void subscriberNumberInformationConfirmationEvent(BluetoothAddress remoteDeviceAddress, SubscriberNumberInformation subscriberNumber) {
            StringBuilder sb = new StringBuilder();

            sb.append("subscriberNumberInformationConfirmationEvent").append(": ");
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

            sb.append("serviceLevelConnectionEstablishedEvent").append(": ");
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
            // Ali:
            Log.v(TAG,"setting the remote device address");
            mRemoteDeviceAddress = remoteDeviceAddress;
            Log.v(TAG,"enabling caller Id");

            enableRemoteCallerIDNotification_Handler.run();

        }

        // Need this
        @Override
        public void ringIndicationEvent(BluetoothAddress remoteDeviceAddress) {
            StringBuilder sb = new StringBuilder();

            sb.append("ringIndicationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());

            incomingCall = true;

            displayMessage("");
            displayMessage(sb);
            // broadcast ring event
            Intent i = new Intent("SS1_ACTION_INCOMING_CALL");
            // Only send broadcast once you have the phone number:
            if (mRemoteDevicePhoneNumber != null) {
                i.putExtra("EXTRA_CALLER_ID",mRemoteDevicePhoneNumber);
                sendBroadcast(i);
                releaseSCOAudioConnection_Handler.run();
                Log.v(TAG,"Sent the braodcast");
            } else {		// no caller Id
                if (ringCounter < 3) {
                    ringCounter++;
                    Log.w(TAG,"No caller Id");
                    Log.w(TAG,"Not sending broadcast");
                }
                else {		// Sending null caller Id
                    i.putExtra("EXTRA_CALLER_ID","");
                    sendBroadcast(i);
                    releaseSCOAudioConnection_Handler.run();
                    Log.w(TAG,"Sending null caller Id");
                }
            }
        }

        @Override
        public void responseHoldStatusConfirmationEvent(BluetoothAddress remoteDeviceAddress, CallState responseHoldStatus) {
            StringBuilder sb = new StringBuilder();

            sb.append("responseHoldStatusConfirmationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(responseHoldStatus);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void networkOperatorSelectionConfirmationEvent(BluetoothAddress remoteDeviceAddress, NetworkMode mode, String operator) {
            StringBuilder sb = new StringBuilder();

            sb.append("networkOperatorSelectionConfirmationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Mode: ").append(mode);
            sb.append(", Operator: ").append(operator);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void incomingCallStateConfirmationEvent(BluetoothAddress remoteDeviceAddress, CallState callState) {
            StringBuilder sb = new StringBuilder();

            sb.append("incomingCallStateConfirmationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(callState);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void inBandRingToneSettingIndicationEvent(BluetoothAddress remoteDeviceAddress, boolean enabled) {
            StringBuilder sb = new StringBuilder();

            sb.append("inBandRingToneSettingIndicationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", Enabled: ").append(enabled);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void currentCallsListConfirmationEvent(BluetoothAddress remoteDeviceAddress, CurrentCallListEntry currentCall) {
            StringBuilder sb = new StringBuilder();

            sb.append("currentCallsListConfirmationEvent").append(": ");
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

        // Need this
        @Override
        public void controlIndicatorStatusIndicationEvent(BluetoothAddress remoteDeviceAddress, String name, int value, int rangeMinimum, int rangeMaximum) {
            StringBuilder sb = new StringBuilder();
            Log.v(TAG,"controlIndicatorStatusIndicationEvent");
            sb.append("controlIndicatorStatusIndicationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(name).append(" = ").append(value);
            sb.append(" ([").append(rangeMinimum).append(",").append(rangeMaximum).append("])");

            displayMessage("");
            displayMessage(sb);
            if (name.equals("CALL")) {
                if (value==1) { // I don't know about other values that's why I don't go callHandle(value==1)
                    callHandle(true);
                }
                else if (value==0) {
                    callHandle(false);
                }
            }
            else if (name.equals("CALLSETUP")) {
                if (value == 0 && incomingCall==true) {
                    // means ended before picked up:
                    callHandle(false);
                }
            }
            else {
                Log.v(TAG,"not interested");
            }

            mRemoteDevicePhoneNumber = null;
            ringCounter = 0;
        }

        // Need this
        @Override
        public void controlIndicatorStatusIndicationEvent(BluetoothAddress remoteDeviceAddress, String name, boolean value) {

            StringBuilder sb = new StringBuilder();
            sb.append("controlIndicatorStatusIndicationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(name).append(" = ").append(value);

            displayMessage("");
            displayMessage(sb);

            if (name.equals("CALL")) {
                callHandle(value);

            }
            else {
                Log.v(TAG,"not interested");
            }

            mRemoteDevicePhoneNumber = null;
            ringCounter = 0;



        }

        @Override
        public void controlIndicatorStatusConfirmationEvent(BluetoothAddress remoteDeviceAddress, String name, int value, int rangeMinimum, int rangeMaximum) {
            StringBuilder sb = new StringBuilder();

            sb.append("controlIndicatorStatusConfirmationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(name).append(" = ").append(value);
            sb.append(" ([").append(rangeMinimum).append(",").append(rangeMaximum).append("])");

            displayMessage("");
            displayMessage(sb);

            if (name.equals("CALL") && (value==0)) {
                callHandle(false);

            }
            else if (name.equals("CALL") && (value==1)) {
                callHandle(true);

            }
            else {
                Log.v(TAG,"not interested");
            }

            mRemoteDevicePhoneNumber = null;
            ringCounter = 0;

        }

        @Override
        public void controlIndicatorStatusConfirmationEvent(BluetoothAddress remoteDeviceAddress, String name, boolean value) {
            StringBuilder sb = new StringBuilder();

            sb.append("controlIndicatorStatusConfirmationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(name).append(" = ").append(value);

            displayMessage("");
            displayMessage(sb);

        }

        @Override
        public void commandResultEvent(BluetoothAddress remoteDeviceAddress, ExtendedResult resultType, int resultValue) {
            StringBuilder sb = new StringBuilder();

            sb.append("commandResultEvent").append(": ");
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

            sb.append("callWaitingNotificationIndicationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(phoneNumber);

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void callLineIdentificationNotificationIndicationEvent(BluetoothAddress remoteDeviceAddress, String phoneNumber) {
            StringBuilder sb = new StringBuilder();

            sb.append("callLineIdentificationNotificationIndicationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(phoneNumber);

            mRemoteDevicePhoneNumber = phoneNumber;

            displayMessage("");
            displayMessage(sb);
        }

        @Override
        public void callHoldMultipartySupportConfirmationEvent(BluetoothAddress remoteDeviceAddress, EnumSet<MultipartyFeatures> callHoldSupportMask) {
            StringBuilder sb = new StringBuilder();

            sb.append("callHoldMultipartySupportConfirmationEvent").append(": ");
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

            sb.append("arbitraryResponseIndicationEvent").append(": ");
            sb.append(remoteDeviceAddress.toString());
            sb.append(", ").append(responseData);

            displayMessage("");
            displayMessage(sb);
        }
    };

    // Need htis
    // Commenting this out until we know how to connect
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
            //TextValue                portNumberParameter;
            //ChacklistValue           connectionFlagsParameter;
            //CheckboxValue            waitForConnectionParameter;
            BluetoothAddress         bluetoothAddress;
            int                      remotePort;
            EnumSet<ConnectionFlags> connectionFlags;

            if(handsFreeServerManager != null) {
                //                portNumberParameter = parameter1View.getValueText();
                //                connectionFlagsParameter = parameter2View.getValueChecklist();
                //                waitForConnectionParameter = parameter3View.getValueCheckbox();

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMe");
                    return;
                }
                remotePort = mRFCOMMportNo;
                Log.v(TAG,"port number is "+remotePort);
                if (remotePort == -1) {
                    Log.w(TAG,"bad port");
                    return;
                }

                connectionFlags = EnumSet.noneOf(ConnectionFlags.class);
                // Enable authentication and encryption.
                //connectionFlags.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
                //connectionFlags.add(ConnectionFlags.REQUIRE_ENCRYPTION);

                boolean waitForConnection = false;//true;
                result = handsFreeServerManager.connectRemoteDevice(bluetoothAddress, remotePort, connectionFlags,waitForConnection);

                displayMessage("");
                displayMessage("connectDeviceCommand" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeText("", "Port number", InputType.TYPE_CLASS_NUMBER);
            //            //parameter2View.setModeChecklist("Connection Flags", connectionFlagLabels, connectionFlagValues);
            //            //parameter3View.setModeCheckbox("Wait for connection:", waitForConnectionChecked);
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // Need this
    private final CommandHandler disconnectDevice_Handler = new CommandHandler() {

        @Override
        public void run() {
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                int result;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMe");
                    return;
                }

                result = handsFreeServerManager.disconnectDevice(bluetoothAddress);

                displayMessage("");
                displayMessage("disconnectDeviceComman" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // private final CommandHandler connectionRequestResponse_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         int              result;
    //         BluetoothAddress bluetoothAddress;
    //         //CheckboxValue    acceptConnParameter;
    //         boolean          acceptConnection;

    //         if(handsFreeServerManager != null) {
    // 		//                acceptConnParameter = parameter1View.getValueCheckbox();

    //             acceptConnection = acceptConnParameter.value;

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMe");
    //                 return;
    //             }

    //             result = handsFreeServerManager.connectionRequestResponse(bluetoothAddress, acceptConnection);

    //             displayMessage("");
    //             displayMessage("connectionRequestResponseComman" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            parameter1View.setModeCheckbox("Accept Connection", false);
    // 	    //            parameter2View.setModeHidden();
    // 	    //            parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

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
                    displayMessage("queryConnectedDevicesComman" + ": Error");
                }
            }
        }

        @Override
        public void selected() {
            //            parameter1View.setModeHidden();
            //            parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
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
                    displayMessage("queryLocalConfigurationCommandName" + ": Error");
                }
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // private final CommandHandler changeIncomingConnectionFlags_Handler = new CommandHandler() {
    //     private final String[] flags = {
    //             "Require Authorization",
    //             "Require Authentication",
    //             "Require Encryption"
    //     };

    //     private final boolean[] values = {
    //             false,
    //             false,
    //             false
    //     };

    //     @Override
    //     public void run() {
    //         int                              result;
    //         //ChacklistValue                   incomingFlagParameter;
    //         EnumSet<IncomingConnectionFlags> flagSet;

    //         if(handsFreeServerManager != null) {

    // 		//                incomingFlagParameter = parameter1View.getValueChecklist();

    //             flagSet = EnumSet.noneOf(IncomingConnectionFlags.class);
    //             if(incomingFlagParameter.checkedItems[0]) {
    //                 flagSet.add(IncomingConnectionFlags.REQUIRE_AUTHORIZATION);
    //             }
    //             if(incomingFlagParameter.checkedItems[1]) {
    //                 flagSet.add(IncomingConnectionFlags.REQUIRE_AUTHENTICATION);
    //             }
    //             if(incomingFlagParameter.checkedItems[2]) {
    //                 flagSet.add(IncomingConnectionFlags.REQUIRE_ENCRYPTION);
    //             }

    //             result = handsFreeServerManager.changeIncomingConnectionFlags(flagSet);

    //             displayMessage("");
    //             displayMessage("changeIncomingConnectionFlagsCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeChecklist("Incoming Connection Flags", flags, values);
    // 	    //            //parameter2View.setModeHidden();
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    private final CommandHandler disableRemoteSoundEnhancement_Handler = new CommandHandler() {

        @Override
        public void run() {
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                int result;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.disableRemoteEchoCancellationNoiseReduction(bluetoothAddress);

                displayMessage("");
                displayMessage("disableRemoteSoundEnhancementCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // private final CommandHandler setRemoteVoiceRecognitionActivation_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         int              result;
    //         BluetoothAddress bluetoothAddress;
    //         //CheckboxValue    voiceRecogParameter;

    //         if(handsFreeServerManager != null) {
    // 		//                voiceRecogParameter = parameter1View.getValueCheckbox();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             result = handsFreeServerManager.setRemoteVoiceRecognitionActivation(bluetoothAddress, voiceRecogParameter.value);

    //             displayMessage("");
    //             displayMessage("setRemoteVoiceRecognitionActivationCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeCheckbox("Voice Recognition Active", false);
    // 	    //            //parameter2View.setModeHidden();
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    // private final CommandHandler setSpeakerGain_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         int              result;
    //         BluetoothAddress bluetoothAddress;
    //         //TextValue        gainParameter;
    //         int              speakerGain;

    //         if(handsFreeServerManager != null) {
    // 		//                gainParameter = parameter1View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             try {
    //                 speakerGain = Integer.valueOf(gainParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             result = handsFreeServerManager.setRemoteSpeakerGain(bluetoothAddress, speakerGain);

    //             displayMessage("");
    //             displayMessage("setSpeakerGainCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeText("", "Speaker Gain", (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));
    // 	    //            //parameter2View.setModeHidden();
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    // private final CommandHandler setMicrophoneGain_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         int              result;
    //         BluetoothAddress bluetoothAddress;
    //         //TextValue        gainParameter;
    //         int              microphoneGain;

    //         if(handsFreeServerManager != null) {
    // 		//                gainParameter = parameter1View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             try {
    //                 microphoneGain = Integer.valueOf(gainParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             result = handsFreeServerManager.setRemoteMicrophoneGain(bluetoothAddress, microphoneGain);

    //             displayMessage("");
    //             displayMessage("setMicrophoneGainCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeText("", "Microphone Gain", (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));
    // 	    //            //parameter2View.setModeHidden();
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    private final CommandHandler registerAudioData_Handler = new CommandHandler() {

        @Override
        public void run() {
            int result;

            if(handsFreeServerManager != null) {
                result = handsFreeServerManager.registerSCODataProvider();

                displayMessage("");
                displayMessage("registerAudioDataCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
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
                displayMessage("unregisterAudioDataCommandName" + ": Success");
            }
        }

        @Override
        public void unselected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
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
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.setupSCOAudioConnection(bluetoothAddress);

                displayMessage("");
                displayMessage("setupSCOAudioConnectionCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }
    };

    private final CommandHandler releaseSCOAudioConnection_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.releaseSCOAudioConnection(bluetoothAddress);

                displayMessage("");
                displayMessage("releaseSCOAudioConnectionCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");

            }
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }
    };

    private final CommandHandler sendAudioData_Handler = new CommandHandler() {

        @Override
        public void run() {
            // FIXME
            showToast(TheHfpService.this, "This command is not currently supported by the sample app.");
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();

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
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.queryRemoteControlIndicatorStatus(bluetoothAddress);

                displayMessage("");
                displayMessage("queryRemoteIndicatorStatusCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // private final CommandHandler enableRemoteIndicatorNotification_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         BluetoothAddress bluetoothAddress;
    //         //CheckboxValue    enableNotifParameter;

    //         if(handsFreeServerManager != null) {
    //             int result;

    // 		//                enableNotifParameter = parameter1View.getValueCheckbox();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             result = handsFreeServerManager.enableRemoteIndicatorEventNotification(bluetoothAddress, enableNotifParameter.value);

    //             displayMessage("");
    //             displayMessage("enableRemoteIndicatorNotificationCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeCheckbox("Enable Event Notification", false);
    // 	    //            //parameter2View.setModeHidden();
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    private final CommandHandler queryRemoteCallHoldSupport_Handler = new CommandHandler() {

        @Override
        public void run() {
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                int result;

                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.queryRemoteCallHoldingMultipartyServiceSupport(bluetoothAddress);

                displayMessage("");
                displayMessage("queryRemoteCallHoldSupportCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // private final CommandHandler sendCallHoldSelection_Handler = new CommandHandler() {
    //     private final String[] spinnerStrings = {
    //             "Add held call to conversation",
    //             "Connect two calls and disconnect",
    //             "Hold all active and accept the other",
    //             "Private consultation",
    //             "Release all active, accept incoming",
    //             "Release all held calls",
    //             "Release specified index"
    //     };

    //     @Override
    //     public void run() {
    //         int                            result;
    //         BluetoothAddress               bluetoothAddress;
    //         SpinnerValue                   handlingTypeParameter;
    // 	    //TextValue                      indexParameter;
    //         CallHoldMultipartyHandlingType handlingType;
    //         int                            index;

    //         if(handsFreeServerManager != null) {
    // 		//                handlingTypeParameter = parameter1View.getValueSpinner();
    // 		//                indexParameter = parameter2View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             switch(handlingTypeParameter.selectedItem) {
    //                 case 0:
    //                     handlingType = CallHoldMultipartyHandlingType.ADD_A_HELD_CALL_TO_CONVERSATION;
    //                     break;
    //                 case 1:
    //                     handlingType = CallHoldMultipartyHandlingType.CONNECT_TWO_CALLS_AND_DISCONNECT;
    //                     break;
    //                 case 2:
    //                     handlingType = CallHoldMultipartyHandlingType.PLACE_ALL_ACTIVE_CALLS_ON_HOLD_ACCEPT_THE_OTHER;
    //                     break;
    //                 case 3:
    //                     handlingType = CallHoldMultipartyHandlingType.PRIVATE_CONSULTATION_MODE;
    //                     break;
    //                 case 4:
    //                     handlingType = CallHoldMultipartyHandlingType.RELEASE_ALL_ACTIVE_CALLS_ACCEPT_WAITING_CALL;
    //                     break;
    //                 case 5:
    //                     handlingType = CallHoldMultipartyHandlingType.RELEASE_ALL_HELD_CALLS;
    //                     break;
    //                 case 6:
    //                     handlingType = CallHoldMultipartyHandlingType.RELEASE_SPECIFIED_CALL_INDEX;
    //                     break;
    //                 default:
    //                     //TODO Complain
    //                     return;
    //             }

    //             try {
    //                 index = Integer.valueOf(indexParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             result = handsFreeServerManager.sendCallHoldingMultipartySelection(bluetoothAddress, handlingType, index);

    //             displayMessage("");
    //             displayMessage("sendCallHoldSelectionCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeSpinner("Call Hold Handling Type", spinnerStrings);
    // 	    //            //parameter2View.setModeText("", "Index", InputType.TYPE_CLASS_NUMBER);
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    // private final CommandHandler enableRemoteCallWaitingNotification_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         int              result;
    //         BluetoothAddress bluetoothAddress;
    //         //CheckboxValue    enableNotifParameter;

    //         if(handsFreeServerManager != null) {
    // 		//                enableNotifParameter = parameter1View.getValueCheckbox();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             result = handsFreeServerManager.enableRemoteCallWaitingNotification(bluetoothAddress, enableNotifParameter.value);

    //             displayMessage("");
    //             displayMessage("enableRemoteCallWaitingNotificationCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeCheckbox("Enable Notification", false);
    // 	    //            //parameter2View.setModeHidden();
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    // Need this
    private final CommandHandler enableRemoteCallerIDNotification_Handler = new CommandHandler() {

        @Override
        public void run() {
            Log.v(TAG,"attempt to enableRemoteCallerID");
            int              result;
            BluetoothAddress bluetoothAddress=mRemoteDeviceAddress;
            //CheckboxValue    enableNotifParameter;

            if(handsFreeServerManager != null) {
                //		//                enableNotifParameter = parameter1View.getValueCheckbox();
                //Ali:
                if(mRemoteDeviceAddress == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                //                result = handsFreeServerManager.enableRemoteCallLineIdentificationNotification(bluetoothAddress, enableNotifParameter.value);
                result = handsFreeServerManager.enableRemoteCallLineIdentificationNotification(bluetoothAddress, true);


                displayMessage("");
                displayMessage("enableRemoteCallerIDNotificationCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeCheckbox("Enable Notification", false);
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // Later need this
    private final CommandHandler dialNumber_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result=0; // Hack for now
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                //                result = handsFreeServerManager.dialPhoneNumber(bluetoothAddress, parameter1View.getValueText().text.toString());

                displayMessage("");
                displayMessage("dialNumberCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeText("", "Phone Number", InputType.TYPE_CLASS_PHONE);
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // private final CommandHandler memoryDial_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         int              result=0; // hack
    //         BluetoothAddress bluetoothAddress;
    //         int              memoryLocation;

    //         if(handsFreeServerManager != null) {
    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             try {
    // 		    //                    memoryLocation = Integer.valueOf(parameter1View.getValueText().text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             result = handsFreeServerManager.dialPhoneNumberFromMemory(bluetoothAddress, memoryLocation);

    //             displayMessage("");
    //             displayMessage("memoryDialCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeText("", "Memory Location Index", InputType.TYPE_CLASS_NUMBER);
    // 	    //            //parameter2View.setModeHidden();
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    private final CommandHandler reDialLastNumber_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.redialLastPhoneNumber(bluetoothAddress);

                displayMessage("");
                displayMessage("reDialLastNumberCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // Need this
    private final CommandHandler answerCall_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.answerIncomingCall(bluetoothAddress);

                displayMessage("");
                displayMessage("answerCallCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // Need htis
    private final CommandHandler hangupCall_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.hangUpCall(bluetoothAddress);

                displayMessage("");
                displayMessage("hangupCallCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // private final CommandHandler sendDTMF_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         int              result;
    //         BluetoothAddress bluetoothAddress;

    //         if(handsFreeServerManager != null) {
    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    // 		//                if(parameter1View.getValueText().text.toString().length() < 1) {
    //                 // TODO complain
    // 		//                    return;
    // 		//                }

    // 	    //                result = handsFreeServerManager.transmitDTMFCode(bluetoothAddress, parameter1View.getValueText().text.toString().charAt(0));

    //             displayMessage("");
    //             displayMessage("sendDTMFCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeText("", "DTMF Code (0-9, *, #, A-D)");
    // 	    //            //parameter2View.setModeHidden();
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    private final CommandHandler sendVoiceTagRequest_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.voiceTagRequest(bluetoothAddress);

                displayMessage("");
                displayMessage("sendVoiceTagRequestCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
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
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.queryRemoteCurrentCallsList(bluetoothAddress);

                displayMessage("");
                displayMessage("queryCallListCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
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
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.setNetworkOperatorSelectionFormat(bluetoothAddress);

                displayMessage("");
                displayMessage("setOperatorFormatCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
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
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.queryRemoteNetworkOperatorSelection(bluetoothAddress);

                displayMessage("");
                displayMessage("queryOperatorCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    private final CommandHandler enableErrorReports_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result=0; // hack
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                //                result = handsFreeServerManager.enableRemoteExtendedErrorResult(bluetoothAddress, parameter1View.getValueCheckbox().value);

                displayMessage("");
                displayMessage("enableErrorReportsCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeCheckbox("Enable Extended Error Results:", false);
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
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
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.querySubscriberNumberInformation(bluetoothAddress);

                displayMessage("");
                displayMessage("queryPhoneNumberCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
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
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                result = handsFreeServerManager.queryResponseHoldStatus(bluetoothAddress);

                displayMessage("");
                displayMessage("queryResponseHoldStatusCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // private final CommandHandler setIncomingCallState_Handler = new CommandHandler() {

    //     String[] callStateLabels = {
    //             "Accept",
    //             "Place on Hold",
    //             "Reject",
    //             "Ignore",
    //     };

    //     @Override
    //     public void run() {
    //         int              result;
    //         BluetoothAddress bluetoothAddress;
    //         SpinnerValue     callStateValue;
    //         CallState        callState;

    //         if(handsFreeServerManager != null) {
    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    // 		//                callStateValue = parameter1View.getValueSpinner();

    //             switch(callStateValue.selectedItem) {
    //             case 0:
    //                 callState = CallState.ACCEPT;
    //                 break;
    //             case 1:
    //                 callState = CallState.HOLD;
    //                 break;
    //             case 2:
    //                 callState = CallState.REJECT;
    //                 break;
    //             case 3:
    //             default:
    //                 callState = CallState.NONE;
    //                 break;
    //             }

    //             result = handsFreeServerManager.setIncomingCallState(bluetoothAddress, callState);

    //             displayMessage("");
    //             displayMessage("setIncomingCallStateCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeSpinner("Incoming Call State", callStateLabels);
    // 	    //            //parameter2View.setModeHidden();
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    // private final CommandHandler sendArbitraryCommand_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         int              result=0;
    //         String           command;
    //         BluetoothAddress bluetoothAddress;

    //         if(handsFreeServerManager != null) {
    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    // 		//                if(parameter2View.getValueCheckbox().value == true) {
    // 		//                    command = parameter1View.getValueText().text.toString() + '\r';
    // 		//                } else {
    // 		//                    command = parameter1View.getValueText().text.toString();
    // 		//                }

    //             result = handsFreeServerManager.sendArbitraryCommand(bluetoothAddress, command);

    //             displayMessage("");
    //             displayMessage("sendArbitraryCommandCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	//            //parameter1View.setModeText("", "Arbitrary Command");
    // 	//            //parameter2View.setModeCheckbox("Terminate with CR", true);
    // 	//            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    private final CommandHandler setSMSToText_Handler = new CommandHandler() {

        @Override
        public void run() {
            int              result;
            String           command;
            BluetoothAddress bluetoothAddress;

            if(handsFreeServerManager != null) {
                if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
                    showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
                    return;
                }

                command =  "AT+CMGF=1" + '\r';

                result = handsFreeServerManager.sendArbitraryCommand(bluetoothAddress, command);

                displayMessage("");
                displayMessage("command");
                displayMessage("setSMSToTextCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
            }
        }

        @Override
        public void selected() {
            //            //parameter1View.setModeHidden();
            //            //parameter2View.setModeHidden();
            //            //parameter3View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub

        }
    };

    // private final CommandHandler sendSMSNumber_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         int              result=0; // hack
    //         String           command;
    //         BluetoothAddress bluetoothAddress;

    //         if(handsFreeServerManager != null) {
    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    // 		//                command = "AT+CMGS=" + '\"' + parameter1View.getValueText().text.toString() + '\"' + '\r';

    //             result = handsFreeServerManager.sendArbitraryCommand(bluetoothAddress, command);

    //             displayMessage("");
    //             displayMessage(command);
    //             displayMessage("sendSMSNumberCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeText("", "Phone Number");
    // 	    //            //parameter2View.setModeHidden();
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };
    // private final CommandHandler sendSMSMessage_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         int              result=0;
    //         String           command;
    //         BluetoothAddress bluetoothAddress;

    //         if(handsFreeServerManager != null) {
    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(TheHfpService.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    // 		//                command = parameter1View.getValueText().text.toString() + (char)26;

    //             result = handsFreeServerManager.sendArbitraryCommand(bluetoothAddress, command);

    //             displayMessage("");
    //             displayMessage("sendSMSMessageCommandName" + ": " + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
    //         }
    //     }

    //     @Override
    //     public void selected() {
    // 	    //            //parameter1View.setModeText("", "Message");
    // 	    //            //parameter2View.setModeHidden();
    // 	    //            //parameter3View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    private final Command[] commandList = new Command[] {
            // new Command("Connect Device", connectDevice_Handler),
            new Command("Disconnect Device", disconnectDevice_Handler),
            // new Command("Connection Request Response", connectionRequestResponse_Handler),
            new Command("Query Connected Devices", queryConnectedDevices_Handler),
            new Command("Query Local Configuration", queryLocalConfiguration_Handler),
            // new Command("Change Incoming Connection Flags", changeIncomingConnectionFlags_Handler),
            new Command("Disable Remote Sound Enhancement", disableRemoteSoundEnhancement_Handler),
            // new Command("Set Remote Voice Recognition Activation", setRemoteVoiceRecognitionActivation_Handler),
            // new Command("Set Speaker Gain", setSpeakerGain_Handler),
            // new Command("Set Microphone Gain", setMicrophoneGain_Handler),
            new Command("Register for Audio Data", registerAudioData_Handler),
            new Command("Release Audio Data Registration", unregisterAudioData_Handler),
            new Command("Setup SCO Audio Connection", setupSCOAudioConnection_Handler),
            new Command("Release SCO Audio Connection", releaseSCOAudioConnection_Handler),
            new Command("Send Audio Data", sendAudioData_Handler),
            new Command("Query Remote Indicator Status", queryRemoteIndicatorStatus_Handler),
            // new Command("Enable Remote Indicator Notification", enableRemoteIndicatorNotification_Handler),
            new Command("Query Remote Call Hold Support", queryRemoteCallHoldSupport_Handler),
            // new Command("Send Call Hold Selection", sendCallHoldSelection_Handler),
            // new Command("Enable Remote Call Waiting Notification", enableRemoteCallWaitingNotification_Handler),
            new Command("Enable Remote Caller ID Notification", enableRemoteCallerIDNotification_Handler),
            new Command("Dial Number", dialNumber_Handler),
            // new Command("Dial Number From Memory", memoryDial_Handler),
            new Command("Re-dial Last Number", reDialLastNumber_Handler),
            new Command("Answer Call", answerCall_Handler),
            new Command("Hang-up Call", hangupCall_Handler),
            // new Command("Send DTMF Code", sendDTMF_Handler),
            new Command("Send Voice Tag Request", sendVoiceTagRequest_Handler),
            new Command("Query Current Call List", queryCallList_Handler),
            new Command("Set Operator Format", setOperatorFormat_Handler),
            new Command("Query Operator", queryOperator_Handler),

            new Command("Enable Error Reports", enableErrorReports_Handler),
            new Command("Query Local Phone Number", queryPhoneNumber_Handler),
            new Command("Query Response/Hold Status", queryResponseHoldStatus_Handler),
            // new Command("Set Incoming Call State", setIncomingCallState_Handler),

            // new Command("Send Arbitrary Command", sendArbitraryCommand_Handler),
            new Command("Set SMS Message to Text", setSMSToText_Handler),
            // new Command("Send Phone Number for SMS", sendSMSNumber_Handler),
            // new Command("Send SMS Message", sendSMSMessage_Handler),
    };

    // Ali Stuff.
    private final BroadcastReceiver hfpCommandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG,"onReceive");
            Bundle b = intent.getExtras();
            int command = b.getInt("command");
            if (command == 1) {//answercall
                pickupTheCall();
            }
            else if (command == 2) {//Hangup
                hangupTheCall();
            }
            else if (command == 500) { // connect
                Log.v(TAG,"attempt to connect");
                String s = b.getString("address");
                BluetoothAddress ba = new BluetoothAddress(s);
                connectWithTheDevice(ba);
            }
            else if (command == 501) { // set in call to zero
                Log.v(TAG,"set inCall to zero");
                inCall = false;
            }
            else if (command == 600) { // disconnect
                Log.v(TAG,"disconnect command");
                disconnectFromTheDevice();
            }

        }
    };

    private void pickupTheCall() {
        // Need to disable audio when it is established
        Log.v(TAG,"releasing Audio");
        releaseSCOAudioConnection_Handler.run();

        Log.v(TAG,"pickupTheCall");
        // Call answerCall_Handler
        answerCall_Handler.run();
        // Disable audio
        inCall = true;

    }

    private void hangupTheCall() {
        // Call hangupCall_Handler
        hangupCall_Handler.run();
        callHandle(false);
    }

    private void connectWithTheDevice(BluetoothAddress bluetoothaddress) {
        Log.v(TAG,"request to connectWithTheDevice");
        // call connectDeviceHandler
        mRemoteDeviceAddress = bluetoothaddress;

        // skip updateRemoteDeviceServices because we are assuming hfp is supported
        getRFCOMMportNo();
        Log.v(TAG,"attempt to connect");
        connectDevice_Handler.run();
    }

    private void disconnectFromTheDevice() {
        disconnectDevice_Handler.run();
    }


    // All this shit is needed for establishing connection from this device.
    private final DEVM.EventCallback deviceEventCallback = new DEVM.EventCallback() {

        @Override
        public void devicePoweredOnEvent() {Log.v(TAG,"devicePoweredOnEvent");}
        @Override
        public void devicePoweringOffEvent(int poweringOffTimeout) {Log.v(TAG,"devicePoweringOffEvent");}
        @Override
        public void devicePoweredOffEvent() {Log.v(TAG,"devicePoweredOffEvent");}

        @Override
        public void localDevicePropertiesChangedEvent(LocalDeviceProperties localProperties, EnumSet<LocalPropertyField> changedFields) {
            Log.v(TAG,"localDevicePropertiesChangedEvent");

            for(LocalPropertyField localPropertyField: changedFields) {
                switch(localPropertyField) {
                    case DISCOVERABLE_MODE:
                        if(localProperties.discoverableMode)
                            Log.v(TAG,"Device is Discoverable");
                        else
                            Log.v(TAG,"Device is Not Discoverable");

                        Log.v(TAG,"Discoverable Mode Timeout: " + localProperties.discoverableModeTimeout);
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
        public void remoteDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) {
            Log.v(TAG,"remoteDeviceServicesStatusEvent");
        }

        @Override
        public void remoteLowEnergyDeviceServicesStatusEvent(BluetoothAddress remoteDevice, boolean success) {
            Log.v(TAG,"remoteLowEnergyDeviceServicesStatusEvent: "+(success?"Success":"Failure"));}
        @Override
        public void discoveryStartedEvent() {Log.v(TAG,"discoveryStartedEvent");}
        @Override
        public void discoveryStoppedEvent() {Log.v(TAG,"discoveryStoppedEvent");}

        @Override
        public void remoteDeviceFoundEvent(RemoteDeviceProperties deviceProperties) {
            // if we are looking new devices mBondStatus == 0,
            // then we dont care about new found devices. We will
            // perform pairing at remote Device property change.
            Log.d(TAG,"remoteDeviceFoundEvent");
        }

        @Override
        public void remoteDevicePropertiesStatusEvent(boolean success, RemoteDeviceProperties deviceProperties) {
            Log.d(TAG,"remoteDevicePropertiesStatusEvent: "+(success?"Success":"Failure"));

            if(deviceProperties.remoteDeviceFlags.contains(RemoteDeviceFlags.SUPPORTS_LOW_ENERGY)) {

                Log.d(TAG,"Device Address    : " + deviceProperties.deviceAddress.toString());
                Log.v(TAG,"Class of Device   : " + Integer.toHexString(deviceProperties.classOfDevice));
                Log.d(TAG,"Device Name       : " + deviceProperties.deviceName);
                Log.v(TAG,"RSSI              : " + deviceProperties.rssi);
                Log.v(TAG,"Transmit power    : " + deviceProperties.transmitPower);
                Log.v(TAG,"Sniff Interval    : " + deviceProperties.sniffInterval);

                if(deviceProperties.applicationData != null) {
                    Log.v(TAG,"Friendly Name     : " + deviceProperties.applicationData.friendlyName);
                    Log.v(TAG,"Application Info  : " + deviceProperties.applicationData.applicationInfo);
                }

                for(RemoteDeviceFlags remoteDeviceFlag: deviceProperties.remoteDeviceFlags)
                    Log.v(TAG,"Remote Device Flag: " + remoteDeviceFlag.toString());
            }
        }

        // We need this one.
        @Override
        public void remoteDevicePropertiesChangedEvent(RemoteDeviceProperties deviceProperties, EnumSet<RemotePropertyField> changedFields) {
            Log.d(TAG,"remoteDevicePropertiesChangedEvent: "+deviceProperties.deviceAddress.toString());

            for(RemotePropertyField remotePropertyField: changedFields) {
                Log.d(TAG,"remoteProperty Changed: "+remotePropertyField);
            }
        }

        @Override
        public void remoteDevicePairingStatusEvent(BluetoothAddress remoteDevice, boolean success, int authenticationStatus) {
            Log.v(TAG,"remoteDevicePairingStatusEvent");

            Log.v(TAG,(success?"Success":"Failure"));
            Log.v(TAG,remoteDevice.toString());
            Log.v(TAG,"Authentication Status: " + (authenticationStatus & 0x7FFFFFFF) + (((authenticationStatus & 0x80000000) != 0) ? " (LE)" : "(BR/EDR)"));
        }

        @Override
        public void remoteDeviceEncryptionStatusEvent(BluetoothAddress remoteDevice, int status) {
            Log.v(TAG,"remoteDeviceEncryptionStatusEvent");
            Log.v(TAG,remoteDevice.toString());
            Log.v(TAG,"Encryption Status: " + status);
        }

        @Override
        public void remoteDeviceDeletedEvent(BluetoothAddress remoteDevice) {
            Log.v(TAG,"remoteDeviceDeletedEvent");}

        @Override
        public void remoteDeviceConnectionStatusEvent(BluetoothAddress remoteDevice, int status) {
            Log.v(TAG,"remoteDeviceConnectionStatusEvent");
            Log.v(TAG,remoteDevice.toString());
            Log.v(TAG,"Connection Status: " + status);
        }

        @Override
        public void remoteDeviceAuthenticationStatusEvent(BluetoothAddress remoteDevice, int status) {
            Log.v(TAG,"remoteDeviceAuthenticationStatusEvent");
            Log.v(TAG,remoteDevice.toString());
            Log.v(TAG,"Authentication Status: " + status);
        }

        @Override
        public void deviceScanStartedEvent() {
            Log.v(TAG,"deviceScanStartedEvent");
        }

        @Override
        public void deviceScanStoppedEvent() {
            Log.v(TAG,"deviceScanStoppedEvent");
        }

        @Override
        public void remoteLowEnergyDeviceAddressChangedEvent(BluetoothAddress PriorResolvableDeviceAddress, BluetoothAddress CurrentResolvableDeviceAddress) {
            Log.v(TAG,"remoteLowEnergyDeviceAddressChangedEvent");
            Log.v(TAG,"remoteLowEnergyDeviceAddressChangedEvent: \n"+
                    "    PriorResolvableDeviceAddress = "+
                    PriorResolvableDeviceAddress.toString()+
                    "\n    CurrentResolvableDeviceAddress = "+
                    CurrentResolvableDeviceAddress.toString());
        }

        @Override
        public void deviceAdvertisingStarted() {
            // TODO Auto-generated method stub

        }

        @Override
        public void deviceAdvertisingStopped() {
            // TODO Auto-generated method stub

        }
    };

    SerialPortClientManager mSPCM;
    int mRFCOMMportNo = -1;

    private int getRFCOMMportNo() {
        Log.v(TAG,"getRFCOMMportNo");
        if (mSPCM == null) {
            Log.w(TAG,"mSPCM is null");
            return -1;
        }
        SPPM.ServiceRecordInformation[] sris= mSPCM.queryRemoteDeviceServices(mRemoteDeviceAddress);
        if (sris==null){
            return -1;
        }
        for (ServiceRecordInformation sri: sris) {
            if (sri.serviceClassID.equals(UUID.fromString("0000111F-0000-1000-8000-00805F9B34FB"))) {
                mRFCOMMportNo = sri.rfcommPortNumber;
                Log.v(TAG,"successfully set the RFCOMM port number");
                return mRFCOMMportNo;
            }
        }
        return -1;
    }
}
