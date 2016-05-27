//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.phone.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;
import com.android.vcard.VCardEntry;
import com.reconinstruments.connect.apps.ConnectHelper;
import com.reconinstruments.messagecenter.MessageDBSchema.MessagePriority;
import com.reconinstruments.messagecenter.ReconMessageAPI.ReconNotification;
import com.reconinstruments.messagecenter.ReconMessageAPI.ReconNotification;
import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.connect.messages.XMLMessage;
import com.reconinstruments.phone.PhoneHelper;
import com.reconinstruments.phone.R;
import com.reconinstruments.utils.SettingsUtil;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.reconinstruments.utils.DeviceUtils;



public class PhoneRelayService extends Service {

    private static final String TAG = "PhoneRelayService";
    PhoneRelayReceiver mPhoneRelayReceiver;
    boolean alreadyInACall = false; // TODO: Call waiting logic still messed up
    boolean shouldHangup = false;

    // Current profile services state. PBAP is connected only on
    // requests, so no need to track it.
    int mMapState = -1;         // deprecated
    int mHfpState = -1;         // deprecated

    // Events log
    ArrayList<String> mEvents = new ArrayList<String>();
    // private ArrayAdapter<String> mAdapter;

    // Remote device Bluetooth address to connect to
    String mSelectedDeviceAddress = ""; // empty

    // System Bluetooth Adapter 
    BluetoothAdapter mBluetoothAdapter = null;

    // Last caller ID and its resolved name
    String mActiveCallerId = null;
    String mActiveCallerName = null;
    Uri mActiveCallUri = null;

    // variables to differentiate between ios ble and ios mfi
    boolean isiOS = true;
    boolean isHfp = true;

    // Remote device Phonebook cache. Can be used to lookup caller ID
    // faster than remote device search query
    ArrayList<VCardEntry> mPhonebook = null;

    // Proxy objects for profile services. Used to track profile
    // service state instead of Intent-based requests Bluetooth
    // Handsfree Profile (Unit role)
    boolean mHfpServiceConnected = false; // deprecated

    //BLE stuff
    BLEServiceConnectionManager mBLEServiceConnectionManager = null;

    // Bluetooth MAP profile (MAS client/MAP server)
    boolean mMapServiceConnected = false; // deprecated

    // Bluetooth PBAP Client
    boolean mPbapClientServiceConnected = false; // deprecated

    // Notes: - notification intent is sent with every RING (+CLIP)
    // unsolicited response from remote device.
    private IncomingCallReceiver mIncomingCallReceiver = new IncomingCallReceiver(this);

    // Handles new SMS message notifications
    private IncomingMessageReceiver mIncomingMessageReceiver =
        new IncomingMessageReceiver(this);

    private CallerNameResolutionReceiver mCallerNameResolutionReceiver =
        new CallerNameResolutionReceiver(this);

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        // Android phone handling
        mPhoneRelayReceiver = new PhoneRelayReceiver();
        mPhoneRelayReceiver.mPhoneRelayService = this;
        IntentFilter phonerelayfilter = new IntentFilter();
        phonerelayfilter.addAction(XMLMessage.PHONE_MESSAGE);
        phonerelayfilter.addAction("HFP_CALL_STATUS_CHANGED");
        Log.v(TAG,"registering phonerelayfilter");
        registerReceiver(mPhoneRelayReceiver, phonerelayfilter);
        registerReceiver(mCallerNameResolutionReceiver, new IntentFilter("CALLER_NAME_RESOLUTION"));

        // iOS Phone Handling Set Up
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();



        if (DeviceUtils.isLimo()) {
            // Limo is now deprecated

        }
        else {                  // We are on jet do our own thing
            // Hfp SS1
            IntentFilter filter = new IntentFilter();
            filter.addAction("SS1_ACTION_INCOMING_CALL");
            registerReceiver(mIncomingCallReceiver, filter);

            // Map SS1
            filter = new IntentFilter();
            filter.addAction("SS1_ACTION_NEW_MESSAGE");
            registerReceiver(mIncomingMessageReceiver, filter);
        }

        Log.v(TAG,"Binding to legacy BLE service");
        mBLEServiceConnectionManager = new BLEServiceConnectionManager(this);
        if (DeviceUtils.isLimo()) {
            // Limo is now deprecated
        }
        // Final step re-enable bluetooth if it is not already enabaled
        BluetoothReenableUtil bru = new BluetoothReenableUtil(this);
        bru.enableBTIfNecessary();

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        // unregister Android call receiver
        unregisterReceiver(mPhoneRelayReceiver);

        // unregister iOS phone receivers
        unregisterReceiver(mCallerNameResolutionReceiver);
        unregisterReceiver(mIncomingCallReceiver);
        unregisterReceiver(mIncomingMessageReceiver);
        mBLEServiceConnectionManager.releaseService();

    }

    @Override
    public void onStart(Intent intent, int startid) {
        Log.d(TAG, "onStart");
    }


    ///////////////////// Hack name resolution
    public void requestCallerNameResolutionFromiPhone(String callerId) {
        String xmlMessage = "<recon intent=\"CALLER_ID_RESOLUTION\"><caller_id>"+
            callerId+"</caller_id></recon>";
        sendBroadcast(new Intent(ConnectHelper.GEN_MSG).putExtra("message",xmlMessage));
    }

    void displayAndSave(String name) {

        Log.d(TAG, "Resolved name is " + name);
        mActiveCallerName = name;

        String contact = mActiveCallerName != null ? mActiveCallerName : mActiveCallerId.length() > 0 ? mActiveCallerId : "Unknown"; 
        mEvents.add(0, String.format("Incoming call from %s", contact));

        gotCall(contact,mActiveCallerId);
    }
    /////////////////////End name resolution ////////////////////////



    // Binding Handling: This section deals with binding handling
    /**
     * The IPhoneRelayService is defined through IDL (aidl)
     */
    private final IPhoneRelayService.Stub binder = new IPhoneRelayService.Stub() {
            public int getHfpStatus() {
                return mHfpState;
            }
            public int getMapStatus() {
                return mMapState;
            }
            public String getBluetoothDeviceName() {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mSelectedDeviceAddress);
                //Log.v(TAG,"BluetoothDevice "+device.toString());
                if (device != null) {
                    return device.getName();
                }
                else {
                    return null;
                }
            }
            public boolean remoteConnectToHfpDevice(String macaddress) {
                Log.v(TAG,"remoteConnectToHfpDevice");
                // Deprecated
                return false;
            }
            public boolean remoteConnectToMapDevice(String macaddress) {
                Log.v(TAG,"remoteConnectToMapDevice");
                // Deprecated
                return false;

            }

            public boolean remoteDisconnectHfpDevice() {
                // Deprecated
                return false;
            }
            public boolean remoteDisconnectMapDevice() {
                // deprecated
                return false;
            }

        };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public static void broadcastBtTelephonyState(Context context, int hfpstate,int mapstate) {
        Intent i = new Intent("com.reconinstruments.BtTelephonyStateChanged");
        i.putExtra("HfpState",hfpstate);
        i.putExtra("MapState",mapstate);
        context.sendBroadcast(i);
    }

    
    public boolean containsLegals(String body) {
        Pattern pattern = Pattern.compile("[\\u0021-\\uFFFF]");
        Matcher matcher = pattern.matcher(body);
        return matcher.find();
    }
        
    public void gotSMS(String contact,String number,String body){
        boolean needReply = true;
        if (number.equals("")) {
            number = contact;
            needReply = false;
        }
        Boolean shouldLaunch =
            SettingsUtil.getCachableSystemIntOrSet(this,SettingsUtil
                                           .SHOULD_NOTIFY_SMS,1) == 1;
        if (!shouldLaunch) return;
            
        Uri smsUri = PhoneHelper.saveSMS(getApplicationContext(), number, contact, body, true);
        // Need to run some test on body to see if it is not empty:
        if (!containsLegals(body)) { // Everything from space to last unicode character
            Log.v(TAG,"Body has no word character");
            body = "[Cannot open message. Please view on phone.]";
                    
        }
        String catName = contact;
        if (contact.equals("Unknown")) {
            catName = number;
        }
        ReconNotification notification =
            new ReconNotification(this, "com.reconinstruments.texts",
                                  "TEXTS", R.drawable.text_icon,
                                  number, catName, R.drawable.text_icon, body);
        //NOTES: disable the reply sms message function here
        notification.setExtra(smsUri.toString());
        Intent viewerIntent = new Intent("com.reconinstruments.messagecenter.message");
        viewerIntent.putExtra("back_to_summary", false);
        notification.overrideMessageViewer(viewerIntent); // open message directly from messageAlert window
        ReconMessageAPI.postNotification(notification,
                                         false,
                                         true);
    }

    public void gotCall(String contact,String number){
        Uri callUri = PhoneHelper.saveCall(this, number, contact, true);
        // Note: postNotification now happens in IncomingCallActivity
        Log.v(TAG,"calling display call. isiOS is "+isiOS+"isHfp"+isHfp);
        Intent dialogIntent = new Intent("RECON_INCOMING_CALL");
        dialogIntent.putExtra("uri", callUri.toString());
        dialogIntent.putExtra("isiOS", isiOS);
        dialogIntent.putExtra("isHfp", isHfp);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Calls activity that generally resides on Phone app.
        Log.d(TAG,"Starting incoming call activity");
        Boolean shouldLaunch =
            SettingsUtil.getCachableSystemIntOrSet(this,SettingsUtil
                                           .SHOULD_NOTIFY_CALLS,1) == 1;
        if (shouldLaunch){
            // We only launch if it is set so in the settings
            ReconMessageAPI.wakeupScreen();
            startActivity(dialogIntent);
        }
    }
    public void doWhenCallStarted() {
        Log.d(TAG, "Phone call Started");
        alreadyInACall = true;
        //Ask Bluetooth Headset to reconnect if in iOS Mode
        if (mBLEServiceConnectionManager.isiOSMode()) {
            Log.d(TAG,"we are in iOS Mode");
            Intent i = new Intent("RECON_IOS_BLUETOOTH_HEADSET_COMMAND");
            i.putExtra("command", 3);//3 = disconnect call: FIXME use static names and etc.
            sendBroadcast(i); 
        }

        Intent startedIntent = new Intent(INTENT_CALL_STARTED);
        sendBroadcast(startedIntent);
    }

    public void doWhenCallEnded() {
        Log.d(TAG, "Phone call ended");
        alreadyInACall = false;
        // Ask Bluetooth Headset to reconnect if in iOS Mode
        if (mBLEServiceConnectionManager.isiOSMode()) {
            Log.d(TAG,"we are in iOS Mode");
            Intent i = new Intent("RECON_IOS_BLUETOOTH_HEADSET_COMMAND");
            i.putExtra("command", 4);//4 = answer call: FIXME use static names and etc.
            sendBroadcast(i);
        }
        Intent i = new Intent("RECON_SS1_HFP_COMMAND");
        i.putExtra("command",501);
        sendBroadcast(i);

        Intent endedIntent = new Intent(INTENT_CALL_ENDED);
        sendBroadcast(endedIntent);
    }
        
    public void doWhenRefreshNeeded() {
        Log.d(TAG,"Refresh needed");
        if (mBLEServiceConnectionManager.isiOSMode()) {
            Log.d(TAG,"we are in iOS Mode");
            Intent i = new Intent("RECON_IOS_BLUETOOTH_HEADSET_COMMAND");
            i.putExtra("command", 5);//3 = disconnect call: FIXME use static names and etc.
            sendBroadcast(i); 
        }
    }
        
    public static String INTENT_CALL_STARTED = "RECON_CALL_STARTED";
    public static String INTENT_CALL_ENDED = "RECON_CALL_ENDED";
}
