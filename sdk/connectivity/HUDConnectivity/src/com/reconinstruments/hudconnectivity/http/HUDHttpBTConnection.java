package com.reconinstruments.hudconnectivity.http;

import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.reconinstruments.hudconnectivity.IHUDConnectivity;
import com.reconinstruments.hudconnectivity.IHUDConnectivity.NetworkEvent;
import com.reconinstruments.hudconnectivity.bluetooth.HUDBTHeaderFactory;
import com.reconinstruments.hudconnectivity.bluetooth.HUDBTService;
import com.reconinstruments.hudconnectivity.bluetooth.HUDBTService.ConnectionState;
import com.reconinstruments.hudconnectivity.bluetooth.HUDBTService.TransactionType;
import com.reconinstruments.hudconnectivity.bluetooth.IHUDBTConsumer;

public class HUDHttpBTConnection extends BroadcastReceiver implements IHUDBTConsumer {
    private final String TAG = this.getClass().getSimpleName();

    private static final boolean DEBUG = true;

    private Context mContext = null;

    private HUDBTService mHUDBTService = null;
    private IHUDConnectivity mHUDConnectivity = null;

    private boolean mHasLocalWebConnection = false;
    private boolean mHasRemoteWebConnection = false;

    private boolean mIsHUD = true;

    public HUDHttpBTConnection(Context context, IHUDConnectivity hudConnectivity, boolean isHUD, HUDBTService hudBTService) {
        if (hudBTService == null || hudConnectivity == null) {
            throw new NullPointerException("HUDHttpBTConnection hudConnectivity or hudBTService can't have null values");
        }

        mIsHUD = isHUD;
        mContext = context;
        mHUDConnectivity = hudConnectivity;

        mHUDBTService = hudBTService;

        mHUDBTService.addConsumer(this);
    }

    public void start() {
        updateLocalWebConnection(mContext);
        mContext.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public HUDHttpResponse sendWebRequest(HUDHttpRequest request) throws Exception {
        // Should only be called from inside the HUD
        if (!mIsHUD) {
            Log.e(TAG, "sendWebRequest: Phone should not try to access the network through the HUD");
            return null;
        }

        synchronized (mHUDBTService) {
            byte[] data = request.getByteArray();
            byte[] header = HUDBTHeaderFactory.getInternetRequestHeader(request.getDoInput(), data.length, request.hasBody() ? request.getBody().length : 0);
            // Stream the Transaction Header
            try {
                mHUDBTService.writeHeader(header, TransactionType.REQUEST);
            } catch (IOException e) {
                Log.e(TAG, "sendWebRequest: updateRemoteWebConnection: Couldn't writeHeader", e);
                return null;
            }

            // Stream the Request
            mHUDBTService.write(data, TransactionType.REQUEST);

            // Stream the body
            if (request.hasBody()) {
                mHUDBTService.write(request.getBody(), TransactionType.REQUEST);
            }

            // Do we expect a response?
            if (!request.getDoInput()) {
                return null;
            }

            header = mHUDBTService.read(HUDBTHeaderFactory.HEADER_LENGTH);

            if (header.length != HUDBTHeaderFactory.HEADER_LENGTH) {
                throw new IOException("sendWebRequest Header expected to have " + HUDBTHeaderFactory.HEADER_LENGTH + " bytes, but got " + header.length + " bytes");
            }

            if (HUDBTHeaderFactory.getCode(header) == HUDBTHeaderFactory.CODE__ERROR) {
                throw new IOException("sendWebRequest web request has failed, no data");
            }

            if (DEBUG)
                Log.d(TAG, "Payload Length=" + HUDBTHeaderFactory.getPayloadLength(header) + " Body Length=" + HUDBTHeaderFactory.getBodyLength(header));

            data = mHUDBTService.read(HUDBTHeaderFactory.getPayloadLength(header));
            HUDHttpResponse hudHTTPResponse = new HUDHttpResponse(data);

            if (HUDBTHeaderFactory.hasBody(header)) {
                data = mHUDBTService.read(HUDBTHeaderFactory.getBodyLength(header));
                if (data.length != HUDBTHeaderFactory.getBodyLength(header)) {
                    Log.e(TAG, "Read " + data.length + " expected " + HUDBTHeaderFactory.getBodyLength(header) + " bytes");
                }
                hudHTTPResponse.setBody(data);
                data = null;
            }

            return hudHTTPResponse;
        }
    }

    public void sendWebResponse(HUDHttpResponse response) throws Exception {
        // Should only be called from inside the HUD
        if (mIsHUD) {
            Log.e(TAG, "sendWebResponse: Phone should not try to access the network through the HUD");
            return;
        }

        synchronized (mHUDBTService) {
            byte[] data = response.getByteArray();
            byte[] header = HUDBTHeaderFactory.getInternetResponseHeader(data.length, response.hasBody() ? response.getBody().length : 0);
            // Stream the Transaction Header
            try {
                mHUDBTService.writeHeader(header, TransactionType.RESPONSE);
            } catch (IOException e) {
                Log.e(TAG, "sendWebRequest: updateRemoteWebConnection: Couldn't writeHeader", e);
                return;
            }

            // Stream the Request
            mHUDBTService.write(data, TransactionType.RESPONSE);

            // Stream the body
            if (response.hasBody()) {
                mHUDBTService.write(response.getBody(), TransactionType.RESPONSE);
            }

            return;
        }
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
     */
    public void updateRemoteWebConnection() {
        if (mHUDBTService.getState() != ConnectionState.CONNECTED) {
            setRemoteWebConnection(false);
            return;
        }

        // Should only be called from inside the HUD
        if (!mIsHUD) {
            Log.e(TAG, "No Need to check remote web connection on non HUD device");
            return;
        }

        synchronized (mHUDBTService) {
            byte[] responseHeader = null;

            try {
                responseHeader = mHUDBTService.writeHeader(HUDBTHeaderFactory.getCheckNetworkHeader(), TransactionType.REQUEST);
            } catch (IOException e) {
                Log.e(TAG, "updateRemoteWebConnection: Couldn't writeHeader", e);
                return;
            }

            try {
                if (responseHeader == null) {
                    Log.e(TAG, "updateRemoteWebConnection: Response is null");
                    return;
                }

                if (HUDBTHeaderFactory.getRequestHeaderType(responseHeader) != HUDBTHeaderFactory.REQUEST_HDR__ONEWAY) {
                    Log.e(TAG, "updateRemoteWebConnection: Expected a oneway response, got " + HUDBTHeaderFactory.getRequestHeaderType(responseHeader));
                    return;
                }

                if (HUDBTHeaderFactory.getCmd(responseHeader) != HUDBTHeaderFactory.CMD__UPDATE_REMOTE_NETWORK) {
                    Log.e(TAG, "updateRemoteWebConnection: Expected Check Network Resonse, got " + HUDBTHeaderFactory.getCmd(responseHeader));
                    return;
                }

                setRemoteWebConnection(HUDBTHeaderFactory.getArg1(responseHeader) == HUDBTHeaderFactory.ARG1__HAS_NETWORK);
            } catch (Exception e) {
                Log.e(TAG, "updateRemoteWebConnection failed", e);
                return;
            }
        }
    }

    private void setRemoteWebConnection(boolean hasNetwork) {
        if (hasNetwork != mHasRemoteWebConnection) {
            Log.d(TAG, "Network state went from " + mHasRemoteWebConnection + " to " + hasNetwork);
            mHasRemoteWebConnection = hasNetwork;
            mHUDConnectivity.onNetworkEvent(mHasRemoteWebConnection ? NetworkEvent.REMOTE_WEB_GAINED : NetworkEvent.REMOTE_WEB_LOST, hasWebConnection());
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        updateLocalWebConnection(context);
    }

    private void updateLocalWebConnection(Context context) {
        boolean prevHasLocalConnection = mHasLocalWebConnection;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        mHasLocalWebConnection = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (DEBUG && (prevHasLocalConnection != mHasLocalWebConnection)) {
            Log.d(TAG, mHasLocalWebConnection ? "Gained Local Web Network" : "Lost Local Web Network");
        }

        if (prevHasLocalConnection != mHasLocalWebConnection) {
            mHUDConnectivity.onNetworkEvent(mHasLocalWebConnection ? NetworkEvent.LOCAL_WEB_GAINED : NetworkEvent.LOCAL_WEB_LOST, hasWebConnection());
            sendUpdateNetworkHeader(TransactionType.REQUEST);
        }
    }

    private synchronized void sendUpdateNetworkHeader(TransactionType transactionType) {
        // We send any status, as long as we are connected to a HUD
        if ((!mIsHUD) && mHUDBTService.getState() == ConnectionState.CONNECTED) {
            try {
                synchronized (mHUDBTService) {
                    mHUDBTService.writeHeader(HUDBTHeaderFactory.getUpdateNetworkHeader(mHasLocalWebConnection), transactionType);
                }
            } catch (IOException e) {
                Log.e(TAG, "updateRemoteWebConnection: Couldn't writeHeader", e);
                return;
            }
        }
    }

    private boolean consumeApplicationCMD(byte[] header, byte[] payload, byte[] body) {
        try {
            if (HUDBTHeaderFactory.getRequestHeaderType(header) == HUDBTHeaderFactory.REQUEST_HDR__ONEWAY) {
                if (HUDBTHeaderFactory.getCmd(header) == HUDBTHeaderFactory.CMD__UPDATE_REMOTE_NETWORK) {
                    if (mIsHUD) {
                        setRemoteWebConnection(HUDBTHeaderFactory.getArg1(header) == HUDBTHeaderFactory.ARG1__HAS_NETWORK);
                    } else {
                        Log.w(TAG, "Non-HUD received an Update Remote Network - not needed");
                    }
                    return true;
                }

                return false;
            }

            if (HUDBTHeaderFactory.getRequestHeaderType(header) == HUDBTHeaderFactory.REQUEST_HDR__RESPONSE) {
                if (HUDBTHeaderFactory.getCmd(header) == HUDBTHeaderFactory.CMD__CHECK_REMOTE_NETWORK) {
                    if (!mIsHUD) {
                        sendUpdateNetworkHeader(TransactionType.RESPONSE);
                    } else {
                        Log.w(TAG, "HUD has been requested to send a Check Remote Network - not needed");
                    }
                    return true;
                }

                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "updateRemoteWebConnection failed", e);
            return false;
        }

        return false;
    }

    private void sendErrorHeader() {
        try {
            mHUDBTService.writeHeader(HUDBTHeaderFactory.getErrorHeader(), TransactionType.RESPONSE);
        } catch (IOException e) {
            Log.e(TAG, "Failed to send an error header", e);
        }
    }

    private boolean consumeApplicationWeb(byte[] header, byte[] payload, byte[] body) {
        HUDHttpRequest request = null;
        try {
            request = new HUDHttpRequest(payload);
            request.setBody(body);

            if (HUDBTHeaderFactory.getRequestHeaderType(header) == HUDBTHeaderFactory.REQUEST_HDR__ONEWAY) {
                URLConnectionHUDAdaptor.sendWebRequest(request);
                return true;
            }

            HUDHttpResponse response = URLConnectionHUDAdaptor.sendWebRequest(request);

            sendWebResponse(response);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create an HUDHttpRequest", e);
            if (HUDBTHeaderFactory.getRequestHeaderType(header) == HUDBTHeaderFactory.REQUEST_HDR__RESPONSE) {
                sendErrorHeader();
            }
        }

        return true;
    }

    @Override
    public boolean consumeBTData(byte[] header, byte[] payload, byte[] body) {
        if (header == null) {
            Log.e(TAG, "consumeBTData: Response is null");
            return false;
        }

        if (HUDBTHeaderFactory.getApplication(header) == HUDBTHeaderFactory.APPLICATION__CMD) {
            return consumeApplicationCMD(header, payload, body);
        }

        if ((HUDBTHeaderFactory.getApplication(header) == HUDBTHeaderFactory.APPLICATION__WEB) && (payload.length > 0)) {
            return consumeApplicationWeb(header, payload, body);
        }

        return false;
    }
}
