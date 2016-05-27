/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.contour.api;

import java.lang.ref.WeakReference;
import java.util.UUID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import com.contour.api.AcceptThread.AcceptThreadCallback;
import com.contour.api.ConnectThread.ConnectThreadCallback;
import com.contour.connect.MainActivity;
import com.contour.connect.R;
import com.contour.connect.debug.CLog;

public class ConnectionService extends Service implements ConnectThreadCallback,
        CommThreadCallback, CameraCommsCallback, AcceptThreadCallback {
    static final String         TAG                    = "ConnectionService";
    boolean        mDebug = true;
    private NotificationManager mNM;
    BluetoothAdapter            mBluetoothAdapter;

    private Messenger           mClient;

    ConnectThread               mConnectThread;
    private CommThread          mCommThread;
    private BaseCameraComms     mCameraComms;

    private AcceptThread        mSecureAcceptThread;
//    private AcceptThread        mInsecureAcceptThread;

    // Unique UUID for this application
    static final UUID           MY_UUID_SECURE         = // 73f5b5af-f3e6-9e12-cc3d-36da345080eb
                                                         // UUID.fromString("73f5b5af-f3e6-9e12-cc3d-36da345080eb");
                                                       new UUID(0x0000110100001000L, 0x800000805F9B34FBL);
    // private static final UUID MY_UUID_INSECURE = UUID
    // .fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final int     STATUS_STATE_CHANGE    = 106;
    public static final int     STATUS_VIDEO_FAME      = 107;


    public static final int     MSG_START_LISTENING    = 200;
    public static final int     MSG_UNREG_CLIENT       = 201;
    public static final int     MSG_CONNECT_CHANNEL    = 400;
    public static final int     MSG_START_VIDEO        = 500;
    public static final int     MSG_STOP_VIDEO         = 501;
    public static final int     MSG_START_RECORDING     = 502;
    public static final int     MSG_STOP_RECORDING     = 503;
    public static final int     MSG_GET_SETTINGS     = 504;
    public static final int     MSG_SET_SETTINGS     = 505;
    public static final int     MSG_APPLY_SETTINGS     = 506;
    public static final int     MSG_RESET_SETTINGS     = 507;
    public static final int     MSG_STOP_STATUS      = 508;


    // public static final int MESSAGE_READ = 2;
    // public static final int MESSAGE_WRITE = 3;
    // public static final int MESSAGE_DEVICE_NAME = 4;
    // public static final int MESSAGE_TOAST = 5;

    // Constants that indicate the current connection state
    public static final int     STATE_NONE             = 0;
    public static final int     STATE_LISTEN           = 1;
    public static final int     STATE_CONNECTING       = 2;
    public static final int     STATE_CONNECTED        = 3;
    public static final int     STATE_DISCONNECTED        = 4;
    
    public static final int     CONNECTION_MSG_UNKNOWN = 302;
    public static final int     CONNECTION_MSG_REFUSED = 301;
    public static final int     CONNECTION_MSG_ABORTED = 302;

    int                         mState;

    private Notification        mNotification;
    private PendingIntent       mContentIntent;
    final Handler               mMainHandler           = new Handler(Looper.getMainLooper());

    // Handles events sent by {@link HealthHDPActivity}.
    static class IncomingHandler extends Handler {
        WeakReference<ConnectionService> mServiceRef;

        IncomingHandler(ConnectionService service) {
            mServiceRef = new WeakReference<ConnectionService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            ConnectionService cs = mServiceRef.get();
            if(cs == null) return;
            switch (msg.what) {
            // Register UI client to this service so the client can receive
            // messages.
            case MSG_START_LISTENING:
                if(cs.mDebug) CLog.out("MSG_START_LISTENING");
                cs.mClient = msg.replyTo;
                cs.startListening();
                break;
            // Unregister UI client from this service.
            case MSG_UNREG_CLIENT:
                if(cs.mDebug) CLog.out("MSG_UNREG_CLIENT");
                cs.mClient = null;
                break;
            // Connect channel.
            case MSG_CONNECT_CHANNEL:
                if(cs.mDebug) CLog.out("MSG_CONNECT_CHANNEL");
//                cs.connect((BluetoothDevice) msg.obj, false);
                break;
            case MSG_START_VIDEO:
                if(cs.mCameraComms != null) cs.mCameraComms.startVideoStream();
                break;
            case MSG_STOP_VIDEO:
                if(cs.mCameraComms != null) cs.mCameraComms.stopVideoStream();
                break;
            case MSG_START_RECORDING:
                if(cs.mCameraComms != null) cs.mCameraComms.startRecording();
                break;
            case MSG_STOP_RECORDING:
                if(cs.mCameraComms != null) cs.mCameraComms.stopRecording();
                break;
            case MSG_GET_SETTINGS:
                if(cs.mDebug) CLog.out(TAG,"MSG_GET_SETTINGS");
                if(cs.mCameraComms != null) cs.mCameraComms.requestSettings();
                break;
            case MSG_SET_SETTINGS:
                if(cs.mCameraComms != null) {
                    int switchPos = msg.arg1;
                    int settingsGroupIndex = (msg.arg2 >> 16) & 0xFF;
                    int settingsItemIndex = msg.arg2 & 0xFF;
                    if(cs.mDebug) CLog.out(TAG,"MSG_SET_SETTINGS",switchPos,settingsGroupIndex,settingsItemIndex);
                    cs.mCameraComms.updateSettings(switchPos,settingsGroupIndex,settingsItemIndex);
                    cs.mCameraComms.sendSettings();
                }
            break;
            case MSG_APPLY_SETTINGS:
                if(cs.mDebug) CLog.out(TAG,"MSG_APPLY_SETTINGS");
                if(cs.mCameraComms != null) cs.mCameraComms.sendSetCameraConfigure();
            	break;
            case MSG_RESET_SETTINGS:
                if (cs.mCameraComms != null) {
                    cs.mCameraComms.sendDefaultSettings();
                    cs.mCameraComms.sendSetCameraConfigure();
                }
                break;
                
            case MSG_STOP_STATUS: 
                if (cs.mCameraComms != null) {
                    cs.mCameraComms.pauseStatusUpdates();
                }
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }


    Messenger                       mMessenger = new Messenger(new IncomingHandler(this));

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.currentThread().setName("ConnectionServiceThread");
        mDebug = getResources().getBoolean(com.contour.connect.R.bool.debug_enabled);

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            // Bluetooth adapter isn't available. The client of the service is
            // supposed to
            // verify that it is available and activate before invoking this
            // service.
            Log.e(CLog.TAG,mBluetoothAdapter == null ? "Stopping self because Bluetooth adapter is null" : String.valueOf(mBluetoothAdapter.isEnabled()));
            stopSelf();
            return;
        }
        // Display a notification about us starting. We put an icon in the
        // status bar.

        showNotification();
        registerReceiver(this.mCameraReceiver, initCameraReceiver());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(mDebug) CLog.out(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(mDebug) CLog.out(TAG, "onUnbind", intent);
        unregisterReceiver(mCameraReceiver);
        stop();
        mNM.cancel(R.string.app_name);
        mCameraComms = null;
        stopSelf();
        return false;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        mNotification = new Notification(R.drawable.icon, getText(R.string.app_name), System.currentTimeMillis());
        mContentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        mNotification.setLatestEventInfo(this, getString(R.string.app_name), getString(R.string.statusmsgloading), mContentIntent);
//        mNM.notify(R.string.notif_name, mNotification);
    }

    private void updateNotification(final String pTitleText, final int textStringId) {
        final String titleText;
        if(pTitleText == null)
            titleText = getString(R.string.app_name); 
        else
            titleText = pTitleText;
        
        this.mMainHandler.post(new Runnable() {
            public void run() {
                String text = getString(textStringId);
                mNotification.setLatestEventInfo(ConnectionService.this, titleText, text, mContentIntent);
                mNM.notify(R.string.app_name, mNotification);
            }
        });

    }

    private void updateNotification(final String pTitleText, final String textString) {
        this.mMainHandler.post(new Runnable() {
            public void run() {
                mNotification.setLatestEventInfo(ConnectionService.this, pTitleText, textString, mContentIntent);
                mNM.notify(R.string.app_name, mNotification);
            }
        });

    }
    
    
    private void sendMessage(int what, int value) {
        if (mClient == null) {
            if(mDebug) CLog.out(TAG, "No clients registered.");
            return;
        }

        try {
            Message msg = Message.obtain(null, what, value, 0);
            mClient.send(msg);
        } catch (RemoteException e) {
            // Unable to reach client.
            e.printStackTrace();
        }
    }

    private void sendMessage(int what, int value, Object obj) {
        if (mClient == null) {
            if(mDebug) CLog.out(TAG, "No clients registered.");
            return;
        }

        try {
            Message msg = Message.obtain(null, what, value, 0, obj);
            mClient.send(msg);
        } catch (RemoteException e) {
            // Unable to reach client.
            e.printStackTrace();
        }
    }
    
    /**
     * Set the current state of the chat connection
     * 
     * @param state
     *            An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if(mDebug) CLog.out(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        int notifId = -1;
        switch(state) {
//        case STATE_LISTEN:
//            notifId = R.string.notif_camera_ready; break;
//        case STATE_CONNECTING:
//            notifId = R.string.notif_connecting; break;
        case STATE_CONNECTED:
            notifId = R.string.notif_connected; break;
//        case STATE_NONE: 
//            notifId = R.string.notif_connection_lost; break;
        }

        sendMessage(STATUS_STATE_CHANGE, state);
        if(notifId > 0)
            updateNotification(null,notifId);
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void startListening() {
        if(mDebug) CLog.out(TAG, "start");
        //CALLED BY MAIN THREAD
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mCommThread != null) {
            mCommThread.cancel();
            mCommThread = null;
        }

        setState(STATE_LISTEN);

        if (useAcceptThreads()) {
            if (mSecureAcceptThread == null) {
                mSecureAcceptThread = new AcceptThread(this,mBluetoothAdapter, true);
                mSecureAcceptThread.mDebug = this.mDebug;
                mSecureAcceptThread.start();
            }
//            if (mInsecureAcceptThread == null) {
//                mInsecureAcceptThread = new AcceptThread(this, mBluetoothAdapter, false);
//                mInsecureAcceptThread.start();
//            }
        }

    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
    	//CALLED BY MAIN THREAD
        if(mDebug) CLog.out(TAG, "stop");

        if(mCameraComms != null)
            mCameraComms.disconnected();

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mCommThread != null) {
            mCommThread.cancel();
            mCommThread = null;
        }

        if (useAcceptThreads()) {
            if (mSecureAcceptThread != null) {
                mSecureAcceptThread.cancel();
                mSecureAcceptThread = null;
            }

//            if (mInsecureAcceptThread != null) {
//                mInsecureAcceptThread.cancel();
//                mInsecureAcceptThread = null;
//            }
        }

        setState(STATE_NONE);
    }

//    /**
//     * Start the ConnectThread to initiate a connection to a remote device.
//     * 
//     * @param device
//     *            The BluetoothDevice to connect
//     * @param secure
//     *            Socket Security type - Secure (true) , Insecure (false)
//     */
//    public synchronized void connect(BluetoothDevice device, boolean secure) {
////        if(useAcceptThreads()) {
////            CLog.out(TAG, "Cancelling any active AcceptThreads" + device);
////            if (mSecureAcceptThread != null) {
////                mSecureAcceptThread.cancel();
////                mSecureAcceptThread = null;
////            }
////
////            if (mInsecureAcceptThread != null) {
////                mInsecureAcceptThread.cancel();
////                mInsecureAcceptThread = null;
////            }
////        }
//    	//ALWAYS CALLED BY MAIN THREAD
//        CLog.out(TAG, "connect to: " + device);
//        mCameraComms = new CameraCommsOld(this, this);
//
//        if (mCommThread != null) {
//            mCommThread.cancel();
//            mCommThread = null;
//        }
//        
//        // Cancel any thread attempting to make a connection
//        if (mState != STATE_CONNECTING) {
//            if (mConnectThread != null) {
//                mConnectThread.cancel();
//                mConnectThread = null;
//            }
//            setState(STATE_CONNECTING);
//            mConnectThread = new ConnectThread(this, device, secure);
//            mConnectThread.start();
//        }
//
//        // Cancel any thread currently running a connection
//        
//    }

    /**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	public synchronized void connected(final BluetoothSocket socket,
			final BluetoothDevice device, final String socketType) {
		// ALWAYS CALLED BY MAIN THREAD
 
	    if(mDebug) CLog.out(TAG, "connected", socket, device);

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mCommThread != null) {
			mCommThread.cancel();
			mCommThread = null;
		}

		if (useAcceptThreads()) {
			// Cancel the accept thread because we only want to connect
			// to one
			// device
			if (mSecureAcceptThread != null) {
				mSecureAcceptThread.cancel();
				mSecureAcceptThread = null;
			}
//			if (mInsecureAcceptThread != null) {
//				mInsecureAcceptThread.cancel();
//				mInsecureAcceptThread = null;
//			}
		}

		// Start the thread to manage the connection and perform
		// transmissions
		mCommThread = new CommThread(ConnectionService.this, socket);
		((Thread) mCommThread).start();
	}

    @Override
    public void onConnectionFailed(BluetoothDevice device, String socketType, String message) {
        if(mDebug) CLog.out(TAG, "connectionFailed");
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                setState(STATE_DISCONNECTED);
            }
        });
//        synchronized (this) {
//            mConnectThread = null;
//        }
//        // Send a failure message back to the Activity
//        // Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
//        // Bundle bundle = new Bundle();
//        // bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
//        // msg.setData(bundle);
//        // mHandler.sendMessage(msg);
//
//        // Start the service over to restart listening mode
//        int connectionMsg = CONNECTION_MSG_UNKNOWN;
//        if (message.contains("Connection refused"))
//            connectionMsg = CONNECTION_MSG_REFUSED;
//        this.sendMessage(STATUS_CONNECTION_MSG, connectionMsg);
//        startListening();
    }

    @Override
    public void onConnectionSuccess(BluetoothSocket socket, BluetoothDevice device,
            String socketType) {
//        synchronized (this) {
//            mConnectThread = null;
//        }
//        this.connected(socket, device, socketType);
        if (mCommThread != null) {
            mCommThread.cancel();
            mCommThread = null;
        }

        if (useAcceptThreads()) {
            // Cancel the accept thread because we only want to connect
            // to one
            // device
            if (mSecureAcceptThread != null) {
                mSecureAcceptThread.cancel();
                mSecureAcceptThread = null;
            }
//            if (mInsecureAcceptThread != null) {
//                mInsecureAcceptThread.cancel();
//                mInsecureAcceptThread = null;
//            }
        }
        
        mCommThread = new CommThread(ConnectionService.this, socket);
        mCommThread.start();
    }
    
    //Accept Thread Callback
    
	@Override
	public void onAcceptConnected(final BluetoothSocket socket,
			final BluetoothDevice device, final String socketType) {
		// CALLED BY ACCEPT THREAD
		mMainHandler.post(new Runnable() {
			@Override
			public void run() {
				connected(socket, device, socketType);
			}
		});
	}
	
    @Override
    public void onAcceptFailed(boolean cancelled) {
        if (!cancelled) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    stop();
                    startListening();
                }
            });
        }
    }

    //CommThread Callbacks

    @Override
    public void onCameraCommStarted(final boolean useOldProtocol) {
        

    	// CALLED BY  COMMS THREAD
    	mMainHandler.post(new Runnable() {
			@Override
			public void run() {
		        setState(STATE_CONNECTED);
		        if(useOldProtocol) {
		            mCameraComms = new CameraCommsOld(ConnectionService.this,ConnectionService.this);
		        } else {
		            mCameraComms = new CameraComms(ConnectionService.this,ConnectionService.this);
		         }
		        
		        if(mCameraComms != null) mCameraComms.connected();
			}
		});
    }

    @Override
	public void onCameraCommDisconnected() {
    	// CALLED BY  COMMS THREAD
        if(mDebug) CLog.out(TAG, "onCameraCommFinished");
		mMainHandler.post(new Runnable() {
			@Override
			public void run() {
				setState(STATE_DISCONNECTED);
				if(mCameraComms != null) mCameraComms.disconnected();
		        startListening();
			}
		});
	}

    @Override
    public void onReceivedPacket(final BasePacket packet) {
		// CALLED BY  COMMS THREAD
//TODO only if slowdowns exist
//    	synchronized(this) {
//          mCameraComms.receivedPacket(packet);
//    	}
    	mMainHandler.post(new Runnable() {
			@Override
			public void run() {
			    if(mCameraComms != null) mCameraComms.receivedPacket(packet);
			}
		});
    }
    
    //*** CAMERA COMMS CALLBACKS ****///

	@Override
	public void sendData(final BasePacket packet) {
		// CALLED BY UI THREAD
//		mMainHandler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (mState == STATE_CONNECTED && mCommThread != null) {
//					// if (D) CLog.out(TAG, "Sending", packet.toString());
//					mCommThread.write(packet.toByteArray());
//				}
//			}
//		});
        if (mState == STATE_CONNECTED && mCommThread != null) {
            // if (D) CLog.out(TAG, "Sending", packet.toString());
            mCommThread.write(packet.toByteArray());
        }
	}

    @Override
    public void onSettngsReceived(CameraSettings settings, int switchPos) {

    }
    
    @Override
    public void onVideoFrameReceived(Bitmap bitmap) {
        this.sendMessage(STATUS_VIDEO_FAME, 0, bitmap);
    }
    
    private IntentFilter initCameraReceiver() {
        IntentFilter filter = new IntentFilter();
//        filter.addAction(CameraComms.ACTION_RECORDING_STARTED);  
//        filter.addAction(CameraComms.ACTION_RECORDING_STOPPED);
        filter.addAction(CameraComms.ACTION_STATUS);
        return filter;
    }
    
    private boolean useAcceptThreads() {
//        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB);
        return true;
    }
    
    
    
    private final BroadcastReceiver mCameraReceiver  = new BroadcastReceiver() {
        int mLastState = -1;
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
//            CLog.out(TAG,"Received Action",action);
//            if(action.equals(CameraComms.ACTION_RECORDING_STARTED)) {
//            }
//            if(action.equals(CameraComms.ACTION_RECORDING_STOPPED)) {
//            }
            if (action.equals(CameraComms.ACTION_STATUS)) {
                Parcelable p = intent.getParcelableExtra(CameraComms.EXTRA_PARCELABLE);
                if(p != null) {
                    CameraStatus cameraStatus = (CameraStatus) p; 
                    int camState = cameraStatus.cameraState;
                    int battery = cameraStatus.batteryPercentage;
                    String gpsStatus = (cameraStatus.gpsHdop == 0 ? "OFF" : "ON");
                    float memory = cameraStatus.storageKbRemaining / 1024.0f;
//                    if(mLastState != camState) {
                        if(camState != CameraStatus.CAMERA_STATE_RECORDING) {
                            String subText= String.format(getString(R.string.notif_status), battery, gpsStatus,memory);
                            updateNotification(getString(R.string.notif_connected),subText);
                        } else {
                            int elapsedSec = cameraStatus.elapsedRecordSeconds;
                            int elapsedHours = (elapsedSec / (60*60));
                            int elapsedMins = Math.abs(elapsedSec / 60)  % 60;
                            String recText = String.format(getString(R.string.record_time_elapsed), elapsedHours, elapsedMins,elapsedSec%60);
                            String subText= String.format(getString(R.string.notif_status_recording),recText);
                            updateNotification(getString(R.string.notif_title_recording),subText);
                        }
//                    }
                    mLastState = camState;
                }
            }
        }
    };
}
