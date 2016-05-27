package com.contour.api;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.contour.connect.debug.CLog;

class ConnectThread extends Thread {
    static final String TAG = "ConnectThread";
    static final boolean USE_SOCKET_HACK = false;
    private BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private String mSocketType;
    private ConnectThreadCallback mCallback;
    
    public ConnectThread(ConnectThreadCallback callback, BluetoothDevice device, boolean secure) {
        mCallback = callback;
        mmDevice = device;
        mSocketType = secure ? "Secure" : "Insecure";
        BluetoothSocket tmp = null;

        // Get a BluetoothSocket for a connection with the
        // given BluetoothDevice
        if (USE_SOCKET_HACK) {
            try {

                Method m = mmDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
                tmp = (BluetoothSocket) m.invoke(mmDevice, 1);
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (secure) {
                    tmp = mmDevice
                            .createRfcommSocketToServiceRecord(ConnectionService.MY_UUID_SECURE);
                } else {
                    tmp = mmDevice
                            .createInsecureRfcommSocketToServiceRecord(ConnectionService.MY_UUID_SECURE);
                }
            } catch (IOException e) {
                CLog.out(TAG,"Socket Type: " + mSocketType + "create() failed", e);
            }

        }
        mmSocket = tmp;
    }

    public void run() {
        setName("ConnectThread");

        CLog.out(TAG,"BEGIN mConnectThread", mSocketType);
        // Process process;
        // BluetoothSocket tmp = null;

        // try {
        // process = Runtime.getRuntime().exec("sdptool records " +
        // mmDevice.getAddress());
        // process.waitFor();
        // } catch (IOException e1) {
        // // TODO Auto-generated catch block
        // CLog.out(TAG,"Failed to get process for device", e1);
        // } catch (InterruptedException e) {
        // CLog.out(TAG,"Failed to get process for device", e);
        // }
        //

        // mmSocket = tmp;
        // Always cancel discovery because it will slow down a connection
        // mBluetoothAdapter.cancelDiscovery();

        // Make a connection to the BluetoothSocket
        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            mmSocket.connect();
        } catch (IOException e) {
            CLog.out(TAG,"unable to connect to socket", e);
            if(!USE_SOCKET_HACK) socketHackConnect();
            return;
         }

        CLog.out(TAG,"mConnectThread Connected!");

        // Start the connected thread
        mCallback.onConnectionSuccess(mmSocket, mmDevice, mSocketType);
    }

    private void socketHackConnect()
    {
        BluetoothSocket tmp = null;

        try {

            Method m = mmDevice.getClass().getMethod("createInsecureRfcommSocket",
                    new Class[] { int.class });
            tmp = (BluetoothSocket) m.invoke(mmDevice, 1);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mmSocket = tmp;
        
        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            mmSocket.connect();
        } catch (IOException e) {
            CLog.out(TAG,"socket hack unable to connect to socket", e);
            mCallback.onConnectionFailed(mmDevice, mSocketType,e.getMessage());
            return;
         }

        CLog.out(TAG,"socket hack mConnectThread Connected!");
        mCallback.onConnectionSuccess(mmSocket, mmDevice, mSocketType);
    }
    
    private void failedToConnect()
    {
        try {
            mmSocket.close();
        } catch (IOException e2) {
            CLog.out(TAG,"unable to close() socket during connection failure", e2);
        }
    }
    
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            CLog.out(TAG,"close() of connect socket failed", e);
        }
    }
    
    public interface ConnectThreadCallback {
        public void onConnectionFailed(BluetoothDevice device, String socketType,String message);
        public void onConnectionSuccess(BluetoothSocket socket, BluetoothDevice device, String socketType);

    }
}