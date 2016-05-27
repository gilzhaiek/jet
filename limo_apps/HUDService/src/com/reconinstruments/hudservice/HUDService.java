//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.hudservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.mobilesdk.hudconnectivity.Constants;
import com.reconinstruments.mobilesdk.hudconnectivity.BTProperty;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.mobilesdk.btconnectivity.BTConnectivityManager;
import com.reconinstruments.mobilesdk.bttransport.BTTransportManager;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityService.DeviceType;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import java.lang.Runnable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import com.reconinstruments.utils.DeviceUtils;

public class HUDService extends HUDConnectivityService {

    private static final String TAG = "HUDService";
    public static final String INTENT_MOD_LIVE_MESSAGE = "RECON_SMARTPHONE_CONNECTION_MESSAGE";
    public static final String INTENT_OLD_API_MESSAGE = "INTENT_OLD_API_MESSAGE";
    public static final String INTENT_REQUEST_DISCONNECT = "com.reconinstruments.mobilesdk.hudconnectivity.request.disconnect";
    public static final String INTENT_REQUEST_KILL = "com.reconinstruments.mobilesdk.hudconnectivity.kill";
    private BtConnectionStateChangeReceiver mBtConnectionStateChangeReceiver =
            new BtConnectionStateChangeReceiver(this);
    private TripSyncReceiver mTripSyncReceiver = new TripSyncReceiver(this);
    private boolean disconnectManually = false; // By default we try to auto connect (at startup)
    private final Handler reconnectHandler = new Handler();
    private final long ONE_MINUTE_DELAY = 60 * 1000;

    // Runnable objects that takes care of reconnecting
    private Runnable reconnecting = new Runnable() {
        @Override
        public void run() {
            do {
                if (disconnectManually)
                    break;
                BTConnectivityManager btConnectivityManager = getBTConnectivityManager();
                if (btConnectivityManager == null)
                    break;
                BTTransportManager btTransportManager = btConnectivityManager
                        .getBTTransportManager();
                if (btTransportManager == null)
                    break;
                if (btTransportManager.getConnectionState() != 0)
                    break; // it's connected or connecting
                int deviceType = BTProperty.getLastPairedDeviceType(HUDService.this);
                String address = BTProperty.getLastPairedDeviceAddress(HUDService.this);
                if (address.length() != 17)
                    break; // Invalid mac address
                BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
                        address);
                if (device == null || BluetoothDevice.BOND_BONDED != device.getBondState())
                    break; // Device not there or not paired
                if (deviceType == 1) {// IOS device
                    if (!Build.PRODUCT.contains("limo")) { // just for jet
                        connect(DeviceType.IOS, address);
                    }
                } else {// Android Device
                    connect(DeviceType.ANDROID, address);
                }
            } while (false);
            // Note we keep polling for connection state. Because
            // when testing with fully event driven implementation
            // (based on acting on disconnect events in the
            // broadcast receiver) there were bugs that would take
            // too long to fix. This is left for future
            // improvement. For now we believe that polling once a
            // minute does not impose significant overhead on the
            // system. Note that the previous implementaiton would
            // also behave like this but with a different timer
            // mechanism.
//            reconnectHandler.postDelayed(reconnecting, ONE_MINUTE_DELAY);
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_MOD_LIVE_MESSAGE);
        filter.addAction(INTENT_OLD_API_MESSAGE);
        registerReceiver(mMODLiveReceiver, filter);
        registerReceiver(mBtConnectionStateChangeReceiver, new IntentFilter("HUD_STATE_CHANGED"));
        registerReceiver(disconnectRequestReceiver, new IntentFilter(INTENT_REQUEST_DISCONNECT));
        registerReceiver(killRequestReceiver, new IntentFilter(INTENT_REQUEST_KILL));
        mTripSyncReceiver.startListening(this);
        setDeviceName();

        // Run regardless of device: start the periodic poll
        //reconnectHandler.postDelayed(reconnecting, ONE_MINUTE_DELAY);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public void resetReconnectingTask() {
        disconnectManually = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mMODLiveReceiver);
            unregisterReceiver(disconnectRequestReceiver);
            unregisterReceiver(killRequestReceiver);
            unregisterReceiver(mBtConnectionStateChangeReceiver);
            mTripSyncReceiver.stopListening(this);
        } catch (IllegalArgumentException e) {
            if (!e.getMessage().contains("Receiver not registered")) {
                e.printStackTrace();
            }
        }
    }

    private final BroadcastReceiver disconnectRequestReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_REQUEST_DISCONNECT)) {
                Log.i(TAG, "disconnectRequestReceiver receieved disconnect message");
                if (BTProperty.getLastPairedDeviceType(context) == 1) {
                    if (!DeviceUtils.isLimo()) { // just for jet
                        disconnect(DeviceType.IOS);
                    }
                } else {
                    disconnect(DeviceType.ANDROID);
                }
                disconnectManually = true;
            }
        }
    };

    private final BroadcastReceiver killRequestReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_REQUEST_KILL)) {
                Log.d(TAG, "killRequestReceiver receieved kill message");
                Log.d(TAG, "Killing the HUD service");
                int pid = 0;
                ActivityManager am = (ActivityManager) context
                        .getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> pids = am.getRunningAppProcesses();
                for (int i = 0; i < pids.size(); i++)
                {
                    ActivityManager.RunningAppProcessInfo info = pids.get(i);
                    if (info.processName.equalsIgnoreCase("com.reconinstruments.hudservice")) {
                        pid = info.pid;
                    }
                }
                android.os.Process.killProcess(pid);
            }
        }
    };

    // This broadcast receiver serves two purposes. 1) To support
    // sending Old API (from apps sitting on the HUD) that is based on
    // broadcasting intents that have an xml message inside them and
    // 2) Dealing with incoming bluetooth messages that are coming
    // from the phone but where sent using the old API. It converts
    // the message to a format that is a parsable by the client
    // receivers of the old bluetooth system.
    private final BroadcastReceiver mMODLiveReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            // Sending using the old API
            if (intent.getAction().equals(INTENT_MOD_LIVE_MESSAGE)) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String message = bundle.getString("message");
                    if (message != null) {
                        Log.d(TAG, "mMODLiveReceiver receieved a message: " + message);
                        if (Constants.showToast) {
                            Toast.makeText(context.getApplicationContext(),
                                    "mMODLiveReceiver receieved a message: " + message,
                                    Toast.LENGTH_LONG).show();
                        }
                        HUDConnectivityMessage cMsg = new HUDConnectivityMessage();
                        cMsg.setIntentFilter(INTENT_OLD_API_MESSAGE);
                        cMsg.setSender(TAG);
                        cMsg.setRequestKey(0);
                        cMsg.setData(message.getBytes());
                        if (Constants.showToast) {
                            Toast.makeText(
                                    context.getApplicationContext(),
                                    "mMODLiveReceiver converting this message into HUDConnectivityMessage: "
                                            + cMsg.toString(), Toast.LENGTH_LONG).show();
                        }
                        Log.d(TAG,
                                "mMODLiveReceiver converting this message into HUDConnectivityMessage: "
                                        + cMsg.toString());
                        push(cMsg, Channel.OBJECT_CHANNEL);
                    }
                }
            }
            // Receiving using the old API
            else if (intent.getAction().equals(INTENT_OLD_API_MESSAGE)) {
                Log.d(TAG, "mMODLiveReceiver receieved INTENT_OLD_API_MESSAGE");
                byte[] bytearrayhudconnectivitymsg = intent.getByteArrayExtra("message");
                if (bytearrayhudconnectivitymsg != null) {
                    HUDConnectivityMessage cMsg = new HUDConnectivityMessage(
                            bytearrayhudconnectivitymsg);
                    byte[] data = cMsg.getData();
                    String xml = new String(data);
                    Log.d(TAG, "mMODLiveReceiver receieved a message: " + xml);
                    String ifilter = get_finalIntent(xml);

                    Intent i = new Intent(ifilter);
                    Bundle extras = new Bundle();
                    extras.putString("message", xml);
                    i.putExtras(extras);
                    sendBroadcast(i);
                    Log.d(TAG, "mMODLiveReceiver sent out the message: " + xml);
                    if (Constants.showToast) {
                        Toast.makeText(context.getApplicationContext(),
                                "mMODLiveReceiver sent out the message: " + xml, Toast.LENGTH_LONG)
                                .show();
                    }
                }
                else { //
                    Log.d(TAG, "received cMsg is null");
                }
            }
        }
    };

    // Helper function to parse the incoming xml messages.
    private String get_finalIntent(String message) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(message));

            Document doc = db.parse(is);
            NodeList nodes = doc.getElementsByTagName("recon");
            Node rootNode = nodes.item(0);
            NamedNodeMap nnm = rootNode.getAttributes();
            Node n = nnm.getNamedItem("intent");
            String type = n.getNodeValue();
            return type;

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse xml", e);
        }
        return null;
    }

    /**
     * The IHUDService is defined through IDL (aidl)
     */
    private final IHUDService.Stub binder = new IHUDService.Stub() {
        public int getConnectionState() {
            return HUDService.this.getConnectionState().ordinal();
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    private void setDeviceName() {
        Log.v(TAG, "setDeviceName");
        BluetoothAdapter btad = BluetoothAdapter.getDefaultAdapter();
        if (btad == null) {
            return;
        }
        String name = btad.getName();
        if (DeviceUtils.isLimo()) {
            if (name == null || name.equals("limo")) {
                BluetoothAdapter.getDefaultAdapter().setName("MOD Live " + Build.SERIAL);
            }
        }
        else if (DeviceUtils.isSnow2()) { // It is a snow device
            if (name == null || name.contains("SS1BTPM")) {
                BluetoothAdapter.getDefaultAdapter()
                    .setName("Snow2 " + Build.SERIAL
                             .substring(Math.max(0, Build.SERIAL.length() - 4)));
            }
        }
        else { // it is JET
            if (name == null || name.contains("SS1BTPM")) {
                BluetoothAdapter.getDefaultAdapter()
                    .setName("JET " + Build.SERIAL
                             .substring(Math.max(0, Build.SERIAL.length() - 4)));
                             // So if serial is less than 4 characters
                             // it shows all
            }
        }
    }

    Handler defaultHandler = new Handler();

    Runnable connectMapSS1_runnable = new Runnable() {
        public void run() {
            String btAddress = BtConnectionStateChangeReceiver
                    .getBTConnectedDeviceAddress(HUDService.this);
            BtConnectionStateChangeReceiver.connectMapSS1(HUDService.this, btAddress);
        }
    };

    Runnable disconnectMapSS1_runnable = new Runnable() {
        public void run() {
            BtConnectionStateChangeReceiver.disconnectMapSS1(HUDService.this);
        }
    };
}
