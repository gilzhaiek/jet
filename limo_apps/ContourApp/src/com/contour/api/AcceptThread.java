package com.contour.api;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.contour.connect.debug.CLog;

/**
 * This thread runs while listening for incoming connections. It behaves like a
 * server-side client. It runs until a connection is accepted (or until
 * cancelled).
 */
class AcceptThread extends Thread {
    static final String                 TAG        = "AcceptThread";
    public boolean mDebug = true;
    // The local server socket
    private final BluetoothServerSocket mmServerSocket;
    private String                      mSocketType;
    private final AcceptThreadCallback  mCallback;
    private volatile boolean            mCancelled = false;
    
    public interface AcceptThreadCallback {
        public void onAcceptConnected(BluetoothSocket socket, BluetoothDevice device,
                final String socketType);
        public void onAcceptFailed(boolean cancelled);
    }

    public AcceptThread(AcceptThreadCallback callback, BluetoothAdapter btAdapter, boolean secure) {
        mCallback = callback;
        BluetoothServerSocket tmp = null;
        mSocketType = secure ? "Secure" : "Insecure";

        // Create a new listening server socket
        try {
            if (secure) {
//                tmp = btAdapter.listenUsingRfcommWithServiceRecord("PnP Information", ConnectionService.MY_UUID_SECURE);
                tmp = btAdapter.listenUsingInsecureRfcommWithServiceRecord("PnP Information", ConnectionService.MY_UUID_SECURE);
//                Method meth_getChannel = tmp.getClass().getDeclaredMethod("getChannel", new Class[] {});
//                meth_getChannel.setAccessible(true);
//                mChannel = (Integer) meth_getChannel.invoke(tmp, new Object[] {});
//                CLog.out(TAG, "RFCOMM LISTENING ON CHANNEL", mChannel);
                // tmp =
                // InsecureBluetooth.listenUsingRfcommWithServiceRecord(btAdapter,
                // "PnP Information", ConnectionService.MY_UUID_SECURE,true);
            } else {
                tmp = InsecureBluetooth.listenUsingRfcomm(1, true);
            }
        } catch (IOException e) {
            Log.e(CLog.TAG,TAG + ":: Socket Type: " + mSocketType + " listen() failed", e);
        } catch (IllegalArgumentException e) {
            Log.e(CLog.TAG,TAG + ":: Socket Type: " + mSocketType + " listen() failed", e);
        }
        mmServerSocket = tmp;
    }

    public void run() {
        if(mDebug) CLog.out(TAG, "Begin " + mSocketType + " AcceptThread");
        setName("AcceptThread" + mSocketType);

        if(mmServerSocket == null) {
            mCallback.onAcceptFailed(mCancelled);
            return;
        }
        BluetoothSocket socket = null;
        // if(mmServerSocket == null) return;
        // Listen to the server socket if we're not connected
        // while (this.connectionService.mState !=
        // ConnectionService.STATE_CONNECTED) {
//        while (!mCancelled) {

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                if(mDebug) CLog.out(TAG, "Try " + mSocketType + " Socket.accept()");
//                socket = mmServerSocket.accept(5000);
              socket = mmServerSocket.accept();

            } catch (IOException e) {
                if(mDebug) CLog.out(TAG, "Socket Type: " + mSocketType + " accept() failed", e);
                mCallback.onAcceptFailed(mCancelled);
                return;
            }
//            if (socket != null)
//                break;
//        }
//        if (mCancelled) {
//            CLog.out(TAG, "Socket Cancelled " + mSocketType);
//            return;
//        }
            if(mDebug) CLog.out(TAG, "Socket Connected " + mSocketType, socket);
        
        mCallback.onAcceptConnected(socket, socket.getRemoteDevice(), mSocketType);
    }

    public void cancel() {
        mCancelled = true;

        if(mDebug) CLog.out(TAG, "Socket Type " + mSocketType + " cancel ");
        try {
            
            if (mmServerSocket != null) {
                mmServerSocket.close();
            }
        } catch (IOException e) {
            Log.w(CLog.TAG, TAG + ":: Socket Type " + mSocketType + " close() of server failed", e);
        }
    }
}