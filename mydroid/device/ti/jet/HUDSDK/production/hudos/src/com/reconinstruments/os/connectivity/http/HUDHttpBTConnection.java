package com.reconinstruments.os.connectivity.http;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.reconinstruments.os.connectivity.IHUDConnectivity;
import com.reconinstruments.os.connectivity.IHUDConnectivity.ConnectionState;
import com.reconinstruments.os.connectivity.IHUDConnectivity.NetworkEvent;
import com.reconinstruments.os.connectivity.bluetooth.HUDBTBaseService.OutputStreamContainer;
import com.reconinstruments.os.connectivity.bluetooth.HUDBTHeaderFactory;
import com.reconinstruments.os.connectivity.bluetooth.HUDBTMessage;
import com.reconinstruments.os.connectivity.bluetooth.HUDBTMessageCollectionManager;
import com.reconinstruments.os.connectivity.bluetooth.IHUDBTConsumer;
import com.reconinstruments.os.connectivity.bluetooth.IHUDBTService;

import java.io.IOException;

/** {@hide}*/
public class HUDHttpBTConnection implements IHUDBTConsumer {
    private final String TAG = this.getClass().getSimpleName();

    private static final boolean DEBUG = true;
    private static final int MAX_WAIT_RESPONSE = 300000; // 300 seconds = 5 minutes

    private Context mContext = null;

    private static IHUDBTService mHUDBTService = null;
    private static IHUDConnectivity mHUDConnectivity = null;

    private static boolean mHasLocalWebConnection = false;
    private static boolean mHasRemoteWebConnection = false;

    private boolean mIsHUD = true;

    public HUDHttpBTConnection(Context context, IHUDConnectivity hudConnectivity, boolean isHUD) {
        if (hudConnectivity == null) {
            throw new NullPointerException("HUDHttpBTConnection hudConnectivity can't be null");
        }

        mIsHUD = isHUD;
        mContext = context;
        mHUDConnectivity = hudConnectivity;
    }

    public void startListening(){
        if (mContext!=null){
            IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            mContext.registerReceiver(mConnectivityChangeBroadcastReceiver, filter);
        }
    }

    public void stopListening(){
        if (mContext!=null){
            mContext.unregisterReceiver(mConnectivityChangeBroadcastReceiver);
        }
    }

    private BroadcastReceiver mConnectivityChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(DEBUG) Log.d(TAG, "onReceive mConnectivityChangeBroadcastReceiver");
            try {
                updateLocalWebConnection(context);
            } catch (InterruptedException e) {
                Log.e(TAG, "onReceive mConnectivityChangeBroadcastReceiver: Couldn't updateLocalWebConnection", e);
            }
        }
    };

    public void setBTService(IHUDBTService hudBTService) {
        mHUDBTService = hudBTService;
        mHUDBTService.addConsumer(this);
    }

    public void updateLocalWebConnection() throws InterruptedException {
        updateLocalWebConnection(mContext);
    }

    private void waitForResponseComplete(HUDBTMessage btResponse) throws Exception {
        // Waiting for the incoming stream to complete a full HUDBTMessage (response)
        synchronized (btResponse) {
            btResponse.wait(MAX_WAIT_RESPONSE);
        }

        if (!btResponse.isComplete()) {
            HUDBTMessageCollectionManager.recycleIncompleteRequestID(btResponse);
            if(DEBUG){
                int headSize = (btResponse.getHeader() == null ? 0 : btResponse.getHeader().length);
                int payloadSize = (btResponse.getPayload() == null ? 0 : btResponse.getPayload().length);
                int bodySize = (btResponse.getBody() == null ? 0 : btResponse.getBody().length);
                Log.e(TAG,"Incomplete Messsage: HeaderSize:" + headSize + " PayloadSize:" + payloadSize + " BodySize:" + bodySize );
            }
            throw new Exception("HUDBTMessage came back with an incomplete response");
        }
    }

    private HUDHttpResponse getHttpResponse(HUDBTMessage btResponse) throws Exception {
        byte[] responseHeader = btResponse.getHeader();
        if (HUDBTHeaderFactory.getCode(responseHeader) == HUDBTHeaderFactory.CODE__ERROR) {
            throw new IOException("sendWebRequest web request has failed, no data");
        }

        if (DEBUG)
            Log.d(TAG, "Payload Length=" + HUDBTHeaderFactory.getPayloadLength(responseHeader) + " Body Length=" + HUDBTHeaderFactory.getBodyLength(responseHeader));

        HUDHttpResponse hudHTTPResponse = new HUDHttpResponse(btResponse.getPayload());

        if (HUDBTHeaderFactory.hasBody(btResponse.getHeader())) {
            hudHTTPResponse.setBody(btResponse.getBody());
        }

        return hudHTTPResponse;
    }

    private void streamData(byte[] header, byte[] payload, byte[] body) throws Exception {
        if (DEBUG) {
            if (header != null) {
                Log.d(TAG, "header size:" + header.length);
            } else {
                Log.d(TAG, "header is null");
            }

            if (payload != null) {
                Log.d(TAG, "payload size:" + payload.length);
            } else {
                Log.d(TAG, "payload is null");
            }

            if (body != null) {
                Log.d(TAG, "body size:" + body.length);
            } else {
                Log.d(TAG, "body is null");
            }

        }
        if (mHUDBTService == null) {
            throw new Exception("sendData: HUDBTService is null");
        }
        OutputStreamContainer osContainer = mHUDBTService.obtainOutputStreamContainer();

        if(osContainer == null) {
            throw new Exception(TAG + ":Couldn't obtain a new OutputSreamContainer");
        }

        try {
            if(header != null && header.length > 0) {
                // Stream the Transaction Header
                mHUDBTService.write(osContainer, header);	
            }

            if(payload != null && payload.length > 0) {
                // Stream the Request
                mHUDBTService.write(osContainer, payload);
            }

            if(body != null && body.length > 0) {
                // Stream the body
                mHUDBTService.write(osContainer, body);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            mHUDBTService.releaseOutputStreamContainer(osContainer);
        }
    }

    private void streamHeader(byte[] header) throws Exception {
        streamData(header, null, null);
    }

    private HUDBTMessage sendHeader(byte[] header) throws Exception {
        HUDBTMessage btResponse = null;
        if(HUDBTHeaderFactory.isRequest(header)) {
            btResponse = HUDBTMessageCollectionManager.createPendingBTResponse(header);
        }

        streamHeader(header);

        // Do we expect a response?
        if(btResponse == null) {
            return null;
        }

        waitForResponseComplete(btResponse);

        return btResponse;
    }

    private HUDHttpResponse __sendWebRequest(HUDHttpRequest request) throws Exception {
        if(mHUDBTService == null) {
            Log.e(TAG, "__sendWebRequest: HUDBTService is null");
            return null;
        }

        boolean hasResponse = request.getDoInput();

        byte[] payload = request.getByteArray();
        byte[] header = HUDBTHeaderFactory.getInternetRequestHeader(hasResponse, payload.length, request.hasBody() ? request.getBody().length : 0);

        HUDBTMessage btResponse = null;
        if(hasResponse) {
            btResponse = HUDBTMessageCollectionManager.createPendingBTResponse(header);
        }

        streamData(header, payload, request.getBody());

        // Do we expect a response?
        if(!hasResponse || (btResponse == null)) {
            return null;
        }

        waitForResponseComplete(btResponse);

        return getHttpResponse(btResponse);
    }

    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws Exception {
        // Should only be called from inside the HUD
        if(!mIsHUD) {
            Log.e(TAG, "sendWebRequest: Phone should not try to access the network through the HUD");
            return null;
        }

        if(mHUDBTService == null) {
            Log.e(TAG, "sendWebRequest: HUDBTService is null");
            return null;
        }

        HUDHttpResponse hudHTTPResponse;
        try {
            hudHTTPResponse = __sendWebRequest(request);
        } catch (Exception e) {
            throw e;
        }

        return hudHTTPResponse;
    }

    public void sendWebResponse(HUDHttpResponse response, byte requestId) throws Exception {
        // Should only be called from inside the HUD
        if(mIsHUD) {
            Log.e(TAG, "sendWebResponse: Phone should not try to access the network through the HUD");
            return;
        }

        if(mHUDBTService == null) {
            Log.e(TAG, "sendWebResponse: HUDBTService is null");
            return;
        }

        byte[] data = response.getByteArray();
        byte[] header = HUDBTHeaderFactory.getInternetResponseHeader(data.length, response.hasBody() ? response.getBody().length : 0);
        HUDBTHeaderFactory.setRequestID(header, requestId);
        streamData(header, data, response.getBody());

        return;
    }

    public boolean hasLocalWebConnection() {
        return mHasLocalWebConnection;
    }

    public boolean hasRemoteWebConnection() {
        return mHasRemoteWebConnection;
    }

    public boolean hasWebConnection() {
        return hasLocalWebConnection() || hasRemoteWebConnection();
    }

    /**
     * In order to check for remote web connection, we will send a "Check Network Header" to the phone/tablet<br>
     * We will read a response right away and check for "Has Network Code"<br>
     * We will call {@link #setRemoteWebConnection(boolean) setRemoteWebConnection} to update the local variable and any other client
     * @throws InterruptedException
     */
    public void updateRemoteWebConnection() throws InterruptedException {
        if(mHUDBTService == null) {
            Log.e(TAG, "updateRemoteWebConnection: HUDBTService is null");
            return;
        }

        if(mHUDBTService.getState() != ConnectionState.CONNECTED) {
            setRemoteWebConnection(false);
            return;
        }

        // Should only be called from inside the HUD
        if(!mIsHUD) {
            Log.e(TAG, "No Need to check remote web connection on non HUD device");
            return;
        }

        byte[] responseHeader = null;

        try {
            responseHeader = sendHeader(HUDBTHeaderFactory.getCheckNetworkHeader()).getHeader();
        } catch (Exception e) {
            Log.e(TAG, "updateRemoteWebConnection: Couldn't sendHeader", e);
            return;
        }

        try {
            if(responseHeader == null) {
                Log.e(TAG, "updateRemoteWebConnection: Response is null");
                return;
            }

            if(HUDBTHeaderFactory.getMessageType(responseHeader) != HUDBTHeaderFactory.MESSAGE_TYPE__RESPONSE) {
                Log.e(TAG, "updateRemoteWebConnection: Expected a oneway response, got " + HUDBTHeaderFactory.getMessageType(responseHeader));
                return;
            }

            if(HUDBTHeaderFactory.getCmd(responseHeader) !=  HUDBTHeaderFactory.CMD__UPDATE_REMOTE_NETWORK) {
                Log.e(TAG, "updateRemoteWebConnection: Expected Check Network Resonse, got " + HUDBTHeaderFactory.getCmd(responseHeader));
                return;
            }

            setRemoteWebConnection(HUDBTHeaderFactory.getArg1(responseHeader) == HUDBTHeaderFactory.ARG1__HAS_NETWORK);
        } catch  (Exception e)  {
            Log.e(TAG,"updateRemoteWebConnection failed", e);
            return;
        }
    }

    private void setRemoteWebConnection(boolean hasNetwork) {
        if(hasNetwork != mHasRemoteWebConnection) {
            Log.d(TAG, "Network state went from " + mHasRemoteWebConnection + " to " + hasNetwork);
            mHasRemoteWebConnection = hasNetwork;
            mHUDConnectivity.onNetworkEvent(mHasRemoteWebConnection ? NetworkEvent.REMOTE_WEB_GAINED : NetworkEvent.REMOTE_WEB_LOST, hasWebConnection());
        }
    }

    private void updateLocalWebConnection(Context context) throws InterruptedException {
        boolean prevHasLocalConnection = mHasLocalWebConnection;
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        mHasLocalWebConnection = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if(DEBUG && (prevHasLocalConnection != mHasLocalWebConnection)) {
            Log.d(TAG, mHasLocalWebConnection ? "Gained Local Web Network" : "Lost Local Web Network");
        }

        if((prevHasLocalConnection != mHasLocalWebConnection) && (mHUDConnectivity != null)) {
            mHUDConnectivity.onNetworkEvent(mHasLocalWebConnection ? NetworkEvent.LOCAL_WEB_GAINED : NetworkEvent.LOCAL_WEB_LOST, hasWebConnection());
            sendUpdateNetworkHeader();
        }
    }

    private synchronized void sendUpdateNetworkHeader() throws InterruptedException {
        if(mHUDBTService == null) {
            Log.e(TAG, "sendUpdateNetworkRequestHeader: HUDBTService is null");
            return;
        }

        // We send any status, as long as we are connected to a HUD
        if((!mIsHUD) && mHUDBTService.getState() == ConnectionState.CONNECTED) {
            try {
                synchronized(mHUDBTService) {
                    streamData(HUDBTHeaderFactory.getUpdateNetworkHeaderOneWay(mHasLocalWebConnection), null, null);
                }
            } catch (Exception e) {
                Log.e(TAG, "updateRemoteWebConnection: Couldn't writeHeader", e);
                return;
            }
        }
    }

    private synchronized void sendUpdateNetworkHeaderResponse(byte requestID) {
        if(mHUDBTService == null) {
            Log.e(TAG, "sendUpdateNetworkHeaderResponse: HUDBTService is null");
            return;
        }

        // We send any status, as long as we are connected to a HUD
        if((!mIsHUD) && mHUDBTService.getState() == ConnectionState.CONNECTED) {
            try {
                synchronized(mHUDBTService) {
                    streamHeader(HUDBTHeaderFactory.getUpdateNetworkHeaderResponse(mHasLocalWebConnection, requestID));
                }
            } catch (Exception e) {
                Log.e(TAG, "updateRemoteWebConnection: Couldn't writeHeader", e);
                return;
            }
        }
    }

    private boolean consumeApplicationCMD(byte[] header, byte[] payload, byte[] body) {
        try {

            if(HUDBTHeaderFactory.getMessageType(header) == HUDBTHeaderFactory.MESSAGE_TYPE__ONEWAY) {
                if(HUDBTHeaderFactory.getCmd(header) == HUDBTHeaderFactory.CMD__UPDATE_REMOTE_NETWORK) {
                    if(mIsHUD) {
                        setRemoteWebConnection(HUDBTHeaderFactory.getArg1(header) == HUDBTHeaderFactory.ARG1__HAS_NETWORK);
                    } else {
                        Log.w(TAG, "Non-HUD received an Update Remote Network - not needed");
                    }
                    return true;
                }

                return false;
            }

            if(HUDBTHeaderFactory.getMessageType(header) == HUDBTHeaderFactory.MESSAGE_TYPE__REQUEST) {
                if(HUDBTHeaderFactory.getCmd(header) == HUDBTHeaderFactory.CMD__CHECK_REMOTE_NETWORK) {
                    if(!mIsHUD) {
                        sendUpdateNetworkHeaderResponse(HUDBTHeaderFactory.getRequestID(header));
                    } else {
                        Log.w(TAG, "HUD has been requested to send a Check Remote Network - not needed");
                    }
                    return true;
                }

                return false;
            }
        } catch  (Exception e)  {
            Log.e(TAG,"updateRemoteWebConnection failed", e);
            return false;
        }

        return false;
    }

    private void sendErrorHeader() {
        if(mHUDBTService == null) {
            Log.e(TAG, "sendErrorHeader: HUDBTService is null");
            return;
        }

        try {
            streamHeader(HUDBTHeaderFactory.getErrorHeader());
        } catch (Exception e) {
            Log.e(TAG, "Failed to send an error header", e);
        }
    }

    private boolean consumeApplicationWeb(byte[] header, byte[] payload, byte[] body) {
        if(HUDBTHeaderFactory.getMessageType(header) == HUDBTHeaderFactory.MESSAGE_TYPE__REQUEST) {
            HUDHttpRequest request = null;
            try {
                request = new HUDHttpRequest(payload);
                request.setBody(body);

                if(HUDBTHeaderFactory.getMessageType(header) == HUDBTHeaderFactory.MESSAGE_TYPE__ONEWAY) {
                    URLConnectionHUDAdaptor.sendWebRequest(request);
                    return true;
                }
                byte requestId = HUDBTHeaderFactory.getRequestID(header);
                HUDHttpResponse response = URLConnectionHUDAdaptor.sendWebRequest(request);

                sendWebResponse(response, requestId);
            } catch (Exception e) {
                Log.e(TAG,"Failed to create an HUDHttpRequest", e);
                if(HUDBTHeaderFactory.getMessageType(header) == HUDBTHeaderFactory.MESSAGE_TYPE__REQUEST) {
                    sendErrorHeader();
                }
            }

            return true;
        } 

        if(HUDBTHeaderFactory.getMessageType(header) == HUDBTHeaderFactory.MESSAGE_TYPE__RESPONSE) {
            // Nothing to do
            return true;
        }

        return false;
    }

    @Override
    public boolean consumeBTData(byte[] header, byte[] payload, byte[] body) {
        if(header == null) {
            Log.e(TAG, "consumeBTData: Response is null");
            return false;
        }

        if(HUDBTHeaderFactory.getApplication(header) == HUDBTHeaderFactory.APPLICATION__CMD) {
            return consumeApplicationCMD(header, payload, body);
        }

        if((HUDBTHeaderFactory.getApplication(header) == HUDBTHeaderFactory.APPLICATION__WEB) && (payload.length > 0)) {
            return consumeApplicationWeb(header, payload, body);
        }

        return false;
    }
}
