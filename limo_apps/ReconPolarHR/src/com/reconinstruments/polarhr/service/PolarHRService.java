package com.reconinstruments.polarhr.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class PolarHRService extends Service {

	public static final int REGISTER_HANDLER = 30;
	
	public static final int RECON_POLAR_BROADCAST_HR = 0;
	
	public static final int MSG_GET_POLAR_BUNDLE = 1;
	public static final int MSG_POLAR_BUNDLE = 2;
	
	// Debugging
    private static final String TAG = "PolarHRService";
    private static final boolean D = true;
    
    // Well known SPP UUID (will *probably* map to RFCOMM channel 1 (default) if not in use)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    // Member fields
    private BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private Handler retryHandler = new RetryConnectionHandler();;
    
    private PolarHRStatus currentState;
	
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    
    /*
     * Handles incoming request to this service
     */
    class IncomingHandler extends Handler {

		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            	case MSG_GET_POLAR_BUNDLE:
            		// reply with current heart rate
            		try {
    					Message lamsg = Message.obtain(null, MSG_POLAR_BUNDLE, msg.what, 0);
    					Log.v(TAG, PolarHRService.this.currentState.toString());
    					lamsg.setData(PolarHRService.this.currentState.getBundle());
    					msg.replyTo.send(lamsg);
    				} catch (RemoteException e) {
    					// The client is dead.
    				}
            		break;
            }
		}
    }
    
    @Override
    public void onCreate() {
    	mState = PolarHRStatus.STATE_NONE;
    	mAdapter = BluetoothAdapter.getDefaultAdapter();
    	currentState = new PolarHRStatus();
    	
    	Log.d(TAG,"onCreate()");
    }
    
    @Override
    public void onDestroy() {
    	this.stop();
    }
    
	@Override
	public IBinder onBind(Intent arg0) {
		return mMessenger.getBinder();
	}
	
	public void broadcastHRUpdate() {
		Intent myi = new Intent();
		myi.setAction("POLAR_BROADCAST_HR");
		myi.putExtra("POLAR_BUNDLE", currentState.getBundle());
		Context c = getApplicationContext();
		c.sendBroadcast(myi);
		
		File path = Environment.getExternalStorageDirectory();
		File file = new File(path, "ReconApps/PolarHR/hrdata.txt");
		
		try {
			(file.getParentFile()).mkdirs();
			BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
			out.write(System.currentTimeMillis() + "," + currentState.getAvgHeartRate() + "," + currentState.getHeartRateInterval());
			out.newLine();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
     * Set the current state of the connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        currentState.setConnectionState(mState);
    }
    
    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }
    
    /**
     * Start the service.
     */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
        	mConnectThread.cancel();
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
        	mConnectedThread.cancel();
        	mConnectedThread = null;
        }
        
        setState(PolarHRStatus.STATE_NONE);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        if(mState == PolarHRStatus.STATE_NONE) {
        	this.stop();
	        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	        String polarMAC = mPrefs.getString("POLAR_MAC_ADDRESS", "");
	    	
	        if(polarMAC.length() == 17) {
	        	try {
	        		BluetoothDevice polarBTDevice = mAdapter.getRemoteDevice(polarMAC);
	        		this.connect(polarBTDevice);
	        	} catch(Exception e) {
	        		e.printStackTrace();
	        	}
	        }
        }
        return START_STICKY;
    }
    
    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == PolarHRStatus.STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(PolarHRStatus.STATE_CONNECTING);
    }
    
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        
        currentState.setDeviceName(device.getName());
        broadcastHRUpdate();

        setState(PolarHRStatus.STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {
        	mConnectThread.cancel();
        	mConnectThread = null;
        }
        if (mConnectedThread != null) {
        	mConnectedThread.cancel();
        	mConnectedThread = null;
        }
        setState(PolarHRStatus.STATE_NONE);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
    	setState(PolarHRStatus.STATE_NONE);
    	
        // Send a failure message back to the Activity
        broadcastHRUpdate();
        retryHandler.sendMessageDelayed(new Message(), 30000); // Retry connection after 20s
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
    	setState(PolarHRStatus.STATE_NONE);
    	
        // Send a failure message back to the Activity
    	broadcastHRUpdate();
    	retryHandler.sendMessageDelayed(new Message(), 30000); // Retry connection after 20s
    }
    
    class RetryConnectionHandler extends Handler {
    	public void handleMessage(Message msg) {
    		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(PolarHRService.this);
 	        String polarMAC = mPrefs.getString("POLAR_MAC_ADDRESS", "");
 	    	
 	    	BluetoothDevice polarBTDevice = mAdapter.getRemoteDevice(polarMAC);
 	    	PolarHRService.this.connect(polarBTDevice);
        }
    }
	
	/**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                PolarHRService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (PolarHRService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        //private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");

            // Keep listening to the InputStream while connected
            int badctr = 0, rrctr = 0, minctr = 0, lasthrbin = 0, batused = 0;
            boolean good_packet;
            while (true) {
                try {
                    // Read from the InputStream
                	int start = mmInStream.read();
                    int ilen = 4;
                    good_packet = false;
                    
                    if(start == 254) {
                    	ilen = mmInStream.read();
                    	int ilen_compl = mmInStream.read();
                    	int check = 255 - ilen;
                    	if (ilen > 7 && ilen < 17 && ilen_compl == check) {
                    		good_packet = true;
                    	} else {
                    		good_packet = false;
                    	}
                    }
                    
                    if(good_packet) {
                    	//Log.v(TAG, "GOOD PACKET");
                    	
                    	//PolarHRStatus status = new PolarHRStatus();
                    	
                    	// Read rest of the HR packet
                    	byte[] msg2 = new byte[ilen - 3];
                    	mmInStream.read(msg2);
                    	
                    	int bincounter = (int) msg2[0]; //counter
                    	int stabin = (int) msg2[1]; //status
                    	int hrbin = (int) msg2[2]; //Filtered good AVG HR (for 1.28 sec interval)
                    	
                    	PolarHRService.this.currentState.setAvgHeartRate(hrbin);
                    	
                    	String response = "";
                    	if ((stabin & 0x10) == 0) {
                    		badctr = badctr + 1; // CONTACT LOST DETECTED (contact_ok_bit = 0)
                    		Log.v(TAG, "CONTACT LOST DETECTED");
                    	} else {
                    		badctr = 0;
                    	}
                    	if(badctr > 4) {
                    		Log.e(TAG, "5th contact lost bit detected! (badctr < 4), closing connection!");
                    		//close connection and Send a failure message back to the Activity
                            Log.v(TAG, "Connection lost");
                    		broadcastHRUpdate();
                            cancel();
                    	}
                    	if ((stabin & 0x60) == 0) {
                    		PolarHRService.this.currentState.setBatteryState(PolarHRStatus.BATTERY_DEAD);
                    	}
                    	if ((stabin & 0x60) == 0x60) {
                    		PolarHRService.this.currentState.setBatteryState(PolarHRStatus.BATTERY_FULL);
                    	}
                    	if ((stabin & 0x60) == 0x40) {
                    		PolarHRService.this.currentState.setBatteryState(PolarHRStatus.BATTERY_OK);
                    	}
                    	if ((stabin & 0x60) == 0x20) {
                    		PolarHRService.this.currentState.setBatteryState(PolarHRStatus.BATTERY_WEAK);
                    	}
                    	int rr1H, rr1L;
                    	if (hrbin == 0) {
                    		if (rrctr == 0) {
                    			rr1H = (int) msg2[3];
                    			rr1L = (int) msg2[4];
                    			int batused_temp = 256*rr1H +rr1L;
                    			Log.v(TAG, "batused_temp:" + Integer.toString(batused_temp) + " batused:" + Integer.toString(batused));
                    			if (batused_temp > batused)
                    				batused = batused_temp;
                    			rrctr = 1;
                    		}
                    		
                    	} else {
                    		for(int i=1; i < ((ilen / 2) - 2); i++) {
                    			rr1H = (int) msg2[2*i +1];
                    			rr1L = (int) msg2[2*i +2];
                    			int rr = 256*rr1H + rr1L;
                    			rrctr = rrctr + rr;
                    			if(rrctr > 60000) {
                    				rrctr -= 6000;
                    				minctr = minctr + 1;
                    			}
                    			PolarHRService.this.currentState.setHeartRateInterval(rr);
                    		}
                    	}
                    	
                    	PolarHRService.this.currentState.setMinutesUsed(batused);
                    	lasthrbin = hrbin;
                    	
                    	//Send HR status to UI activity
                    	if (hrbin > 0) // Ignore first couple 0bpm readings
                    		PolarHRService.this.broadcastHRUpdate();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
