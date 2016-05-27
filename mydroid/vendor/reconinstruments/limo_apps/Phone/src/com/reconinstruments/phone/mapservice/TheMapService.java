package com.reconinstruments.phone.mapservice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.BluetopiaPMException;
import com.stonestreetone.bluetopiapm.MAPM;
import com.stonestreetone.bluetopiapm.MAPM.*;
import com.stonestreetone.bluetopiapm.MAPM.MessageAccessClientManager.ClientEventCallback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;
import java.util.EnumSet;

/**



   /**
   *  MAPM Message Client Equipment (MCE) Sample
   */
public class TheMapService extends Service {

    private static final String LOG_TAG = "TheMapService";
    private static final String TAG = "TheMapService";
    /*package*/ Resources resourceManager;
    // Ali
    BluetoothAddress mRemoteDeviceAddress=null;
    String mRemoteDevicePhoneNumber = null;
    private int mInstanceID = -1;
    private int mServerPort = -1;
    private int mMessageHandle = -1;

    //End Ali

    @Override
    public IBinder onBind (Intent intent)    {
	return null;		// for now
    }
    
    @Override
    public void onCreate() {
	super.onCreate();
	Log.v(TAG,"onCreate");
	BluetoothAdapter bluetoothAdapter;

        /*
         * Register a receiver for a Bluetooth "State Changed" broadcast event.
         */
        registerReceiver(bluetoothBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	registerReceiver(mapCommandReceiver, new IntentFilter("RECON_SS1_MAP_COMMAND"));

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null) {
            if(bluetoothAdapter.isEnabled()) {
                profileEnable();
            } else {
            }
        }
	resourceManager = getResources();

    }

    private final BroadcastReceiver mapCommandReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
		Log.v(TAG,"onReceive");
		Bundle b = intent.getExtras();
		int command = b.getInt("command");
		if (command == 500) { // connect
		    Log.v(TAG,"attempt to connect");
		    String s = b.getString("address");
		    BluetoothAddress ba = new BluetoothAddress(s);
		    connectWithTheDevice(ba);
		}
		else if (command == 600) { // Disconnect
		    Log.v(TAG,"disconnect command");
		    disconnectFromTheDevice();
		}

	    }
	};

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

    @Override
    public int onStartCommand(Intent i, int flag, int startId) {
	return START_STICKY;
    }

    private final BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();

		if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
		    switch(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
		    case BluetoothAdapter.STATE_TURNING_ON:
			showToast(context, "bluetoothEnablingToastMessage");

			break;

		    case BluetoothAdapter.STATE_ON:
			showToast(context, "bluetoothEnabledToastMessage");

			profileEnable();

			break;

		    case BluetoothAdapter.STATE_TURNING_OFF:
			showToast(context, "bluetoothDisablingToastMessage");

			profileDisable();

			break;

		    case BluetoothAdapter.STATE_OFF:
			showToast(context, "bluetoothDisabledToastMessage");
			break;

		    case BluetoothAdapter.ERROR:
		    default:
			showToast(context, "bluetoothUnknownStateToastMessage");
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


    /*
     * ==========================================================
     * Profile-specific content below this line.
     */

    /* package */ MAPM.MessageAccessClientManager messageAccessClientManager;
    private final ClientEventCallback messageAccessClientEventCallback = new ClientEventCallback() {

	    @Override
	    public void disconnectedEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int instanceID) {
		Log.d(LOG_TAG,"disconnectedEvent");
		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("disconnectedEventLabel");
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("connectionTypeLabel").append(": ");
		sb.append(connectionType);
		displayMessage(sb);
	    }

	    // Need this
	    @Override
	    public void connectionStatusEvent(ConnectionType connectionType, BluetoothAddress remoteDeviceAddress, int instanceID, ConnectionStatus connectionStatus) {
		Log.d(LOG_TAG,"connectionStatusEvent");
		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("connectionStatusEventLabel").append(": ");
		sb.append(connectionStatus);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);
		mRemoteDeviceAddress = remoteDeviceAddress;

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);
		mInstanceID = instanceID;

		sb = new StringBuilder();
		sb.append("connectionTypeLabel").append(": ");
		sb.append(connectionType);
		displayMessage(sb);

		// Enable notification;
		setFolderAbsolute_Handler.run();

	    }

	    @Override
	    public void enableNotificationsResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode) {
		Log.d(LOG_TAG,"enableNotificationsResponseEvent");
		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("enableNotificationsResponseEventLabel").append(": ");
		sb.append(responseStatusCode);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);
	    }

	    @Override
	    public void getFolderListingResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, boolean isFinal, byte[] folderListingBuffer) {
		Log.d(LOG_TAG,"getFolderListingResponseEvent");
		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("getFolderListingResponseEventLabel").append(": ");
		sb.append(responseStatusCode);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("isFinalLabel").append(": ");
		sb.append(isFinal);
		displayMessage(sb);

		sb = new StringBuilder();
		if(folderListingBuffer != null) {

		    ByteArrayInputStream folderListingInputStream = new ByteArrayInputStream(folderListingBuffer);
		    BufferedInputStream folderListingBufferedInputStream = new BufferedInputStream(folderListingInputStream);
		    try {
			int bytesRead;

			while((bytesRead = folderListingBufferedInputStream.read(folderListingBuffer)) != -1) {
			    sb.append(new String(folderListingBuffer, 0, bytesRead));
			}
			displayMessage(sb);
		    } catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		}
	    }

	    @Override
	    public void getFolderListingSizeResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, int numberOfFolders) {

		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("getFolderListingSizeResponseEventLabel").append(": ");
		sb.append(responseStatusCode);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("numberOfFoldersLabel").append(": ");
		sb.append(numberOfFolders);
		displayMessage(sb);
	    }

	    @Override
	    public void getMessageListingResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, boolean newMessage, Calendar timeMSE, boolean validUTC, int offsetUTC, boolean isFinal, int numberOfMessages, byte[] messageListingBuffer) {
		Log.d(LOG_TAG,"getMessageListingResponseEvent");
		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("getMessageListingResponseEventLabel").append(": ");
		sb.append(responseStatusCode);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("newMessageLabel").append(": ");
		sb.append(newMessage);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("timeMSELabel").append(": ");
		if(timeMSE != null)
		    sb.append(timeMSE.getTime());
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("validUTCLabel").append(": ");
		sb.append(validUTC);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("offsetUTCLabel").append(": ");
		if(validUTC)
		    if(offsetUTC < 0)
			sb.append("- " + Math.abs(offsetUTC)/60 + ":" + Math.abs(offsetUTC) % 60);
		    else
			sb.append("  " + offsetUTC/60 + ":" + offsetUTC % 60);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("isFinalLabel").append(": ");
		sb.append(isFinal);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("numberOfMessagesLabel").append(": ");
		sb.append(numberOfMessages);
		displayMessage(sb);

		sb = new StringBuilder();
		if(messageListingBuffer != null) {

		    ByteArrayInputStream folderListingInputStream = new ByteArrayInputStream(messageListingBuffer);
		    BufferedInputStream folderListingBufferedInputStream = new BufferedInputStream(folderListingInputStream);
		    try {
			int bytesRead;

			while((bytesRead = folderListingBufferedInputStream.read(messageListingBuffer)) != -1) {
			    sb.append(new String(messageListingBuffer, 0, bytesRead));
			}
			displayMessage(sb);
		    } catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		}
	    }

	    @Override
	    public void getMessageListingSizeResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, boolean newMessage, Calendar timeMSE, boolean validUTC, int offsetUTC, int numberOfMessages) {
		Log.d(LOG_TAG,"getMessageListingSizeResponseEvent");
		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("getMessageListingSizeResponseEventLabel").append(": ");
		sb.append(responseStatusCode);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("newMessageLabel").append(": ");
		sb.append(newMessage);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("timeMSELabel").append(": ");
		if(timeMSE != null)
		    sb.append(timeMSE.getTime());
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("validUTCLabel").append(": ");
		sb.append(validUTC);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("offsetUTCLabel").append(": ");
		if(validUTC)
		    if(offsetUTC < 0)
			sb.append("- " + Math.abs(offsetUTC)/60 + ":" + Math.abs(offsetUTC) % 60);
		    else
			sb.append("  " + offsetUTC/60 + ":" + offsetUTC % 60);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("numberOfMessagesLabel").append(": ");
		sb.append(numberOfMessages);
		displayMessage(sb);
	    }
	    // Need this
	    @Override
	    public void getMessageResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, FractionDeliver fractionDeliver, boolean isFinal, byte[] messageBuffer) {

		// We broadcast the messge:
		Log.v(TAG,"retrieved the message");
		String messageString = new String(messageBuffer);
		Intent i = new Intent ("SS1_ACTION_NEW_MESSAGE");
		i.putExtra("BMSG",messageString);
		Log.v(TAG,"broadcasting the message");
		sendBroadcast(i);
		//====================================================
		// Log.d(LOG_TAG,"getMessageResponseEvent"+new String (messageBuffer)+":"+isFinal);
		// displayMessage("");
		// StringBuilder sb = new StringBuilder();

		// sb.append("bluetoothAddressLabel").append(": ");
		// sb.append(remoteDeviceAddress);
		// displayMessage(sb);

		// sb = new StringBuilder();
		// sb.append("instanceIDLabel").append(": ");
		// sb.append(instanceID);
		// displayMessage(sb);

		// sb = new StringBuilder();
		// sb.append("responseStatusCodeLabel").append(": ");
		// sb.append(responseStatusCode);
		// displayMessage(sb);

		// sb = new StringBuilder();
		// sb.append("fractionDeliverLabel").append(": ");
		// sb.append(fractionDeliver);
		// displayMessage(sb);

		// sb = new StringBuilder();
		// sb.append("isFinalLabel").append(": ");
		// sb.append(isFinal);
		// displayMessage(sb);

		// sb = new StringBuilder();
		// if(messageBuffer != null) {

		//     ByteArrayInputStream folderListingInputStream = new ByteArrayInputStream(messageBuffer);
		//     BufferedInputStream folderListingBufferedInputStream = new BufferedInputStream(folderListingInputStream);
		//     try {
		// 	int bytesRead;

		// 	while((bytesRead = folderListingBufferedInputStream.read(messageBuffer)) != -1) {
		// 	    sb.append(new String(messageBuffer, 0, bytesRead));
		// 	}
		// 	displayMessage(sb);
		//     } catch(IOException e) {
		// 	// TODO Auto-generated catch block
		// 	e.printStackTrace();
		//     }
		// }
	    }

	    // Need this
	    @Override
	    public void notificationIndicationEvent(BluetoothAddress remoteDeviceAddress, int instanceID, boolean isFinal, byte[] eventReportBuffer) {

		// TODO: parse event report buffer and find even of
		// type "NewMessage" and get the handle of that.
		// use getMessage_Handler.run();

		Log.d(LOG_TAG,"notificationIndicationEvent"+new String (eventReportBuffer)+":"+isFinal);
		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);

		Log.v(TAG,"update bluetooth address and instance id in case");
		mRemoteDeviceAddress = remoteDeviceAddress;
		mInstanceID = instanceID;
		Log.v(TAG,"see if we can find a good handle");
		int handle = getHandleIfNewMessage(eventReportBuffer);
		if (handle != -1) { // Means new message report with valid handle
		    Log.v(TAG,"attempt to retrieve the message");
		    mMessageHandle = handle;
		    getMessage_Handler.run();
		}
	    }

	    @Override
	    public void pushMessageResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, String messageHandle) {
		Log.d(LOG_TAG,"pushMessageResponseEvent");
		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("responseStatusCodeLabel").append(": ");
		sb.append(responseStatusCode);
		displayMessage(sb);
	    }

	    // Need this;
	    @Override
	    public void setFolderResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode, String currentPath) {
		Log.d(LOG_TAG,"setFolderResponseEvent");
		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("setFolderResponseEventLabel").append(": ");
		sb.append(responseStatusCode);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("currentPathLabel").append(": ");
		sb.append(currentPath);
		displayMessage(sb);
		// Enable Notification
		Log.v(TAG,"now to enable notification");
		enableNotifications_Handler.run();
	    }

	    @Override
	    public void setMessageStatusResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode) {
		Log.d(LOG_TAG,"setMessageStatusResponseEvent");
		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("responseStatusCodeLabel").append(": ");
		sb.append(responseStatusCode);
		displayMessage(sb);
	    }

	    @Override
	    public void updateInboxResponseEvent(BluetoothAddress remoteDeviceAddress, int instanceID, ResponseStatusCode responseStatusCode) {
		Log.d(LOG_TAG,"updateInboxResponseEvent");
		displayMessage("");
		StringBuilder sb = new StringBuilder();

		sb.append("bluetoothAddressLabel").append(": ");
		sb.append(remoteDeviceAddress);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("instanceIDLabel").append(": ");
		sb.append(instanceID);
		displayMessage(sb);

		sb = new StringBuilder();
		sb.append("responseStatusCodeLabel").append(": ");
		sb.append(responseStatusCode);
		displayMessage(sb);
	    }

	};

    // Need this;
    private final void profileEnable() {
        synchronized(this) {
            try {
		Log.v(TAG,"try to enable the profile");
                messageAccessClientManager = new MessageAccessClientManager(messageAccessClientEventCallback);
            } catch(Exception e) {
                /*
                 * BluetopiaPM server couldn't be contacted. This should never
                 * happen if Bluetooth was successfully enabled.
                 */
                showToast(this, "errorBTPMServerNotReachableToastMessage");
                BluetoothAdapter.getDefaultAdapter().disable();
            }
        }
    }

    // Need this;
    private final void profileDisable() {
        synchronized(TheMapService.this) {
            if(messageAccessClientManager != null) {

                messageAccessClientManager.dispose();
                messageAccessClientManager = null;

            }
        }
    }



    // Need this
    private final CommandHandler connectRemoteDevice_Handler = new CommandHandler() {

	    String[]  connectionFlagLabels = {
		"Authentication",
		"Encryption"
	    };

	    boolean[] connectionFlagValues = new boolean[] {false, false};

	    boolean   waitForConnectionChecked = false;

	    @Override
	    public void run() {

		int result = 0;
		int remotePort = mServerPort;
		int instanceID = mInstanceID;
		EnumSet<ConnectionFlags> connectionFlags;
		BluetoothAddress bluetoothAddress;

		if(messageAccessClientManager != null) {
		    if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
			showToast(TheMapService.this, "errorInvalidBluetoothAddressToastMessage");
			return;
		    }


		    connectionFlags = EnumSet.noneOf(ConnectionFlags.class);
		    // connectionFlags.add(ConnectionFlags.REQUIRE_AUTHENTICATION);
		    // connectionFlags.add(ConnectionFlags.REQUIRE_ENCRYPTION);
		    boolean waitForConnection = false;

		    result = messageAccessClientManager.connectRemoteDevice(bluetoothAddress, remotePort, instanceID, connectionFlags, waitForConnection);

		    displayMessage("");
		    displayMessage("connectRemoteDeviceLabel" + ((result == 0) ? "Success" : "Error") + " (" + result + ")");
		}
	    }

	    @Override
	    public void selected() {
	    }

	    @Override
	    public void unselected() {
		// TODO Auto-generated method stub
	    }
	};


    // Need this
    private final CommandHandler disconnect_Handler = new CommandHandler() {

        @Override
        public void run() {

            int result;
            int instanceID = mInstanceID;
            BluetoothAddress bluetoothAddress = mRemoteDeviceAddress;
            ConnectionCategory connectionCategory = ConnectionCategory.MESSAGE_ACCESS;

            if(messageAccessClientManager != null) {

                result = messageAccessClientManager.disconnect(connectionCategory, bluetoothAddress, instanceID);

                displayMessage("");
                displayMessage("disconnectLabel" + result);
            }

        }

        @Override
        public void selected() {
            // parameter1View.setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
            // parameter2View.setModeHidden();
            // parameter3View.setModeHidden();
            // parameter4View.setModeHidden();
            // parameter5View.setModeHidden();
            // parameter6View.setModeHidden();
            // parameter7View.setModeHidden();
            // parameter8View.setModeHidden();
            // parameter9View.setModeHidden();
        }

        @Override
        public void unselected() {
            // TODO Auto-generated method stub
        }

	};


    // private final CommandHandler queryCurrentFolder_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {

    //         int instanceID;
    //         String result = "";
    //         BluetoothAddress bluetoothAddress;
    //         TextValue instanceIDParameter;

    //         if(messageAccessClientManager != null) {

    //             instanceIDParameter = parameter1View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(MainActivity.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             try {
    //                 instanceID = Integer.valueOf(instanceIDParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             try {
    //                 result = messageAccessClientManager.queryCurrentFolder(bluetoothAddress, instanceID);
    //             } catch(BluetopiaPMException e) {
    //                 // TODO Auto-generated catch block
    //                 e.printStackTrace();
    //             }

    //             displayMessage("");
    //             displayMessage("currentPathLabel" + ": " + ((result == null) ? "" : result));
    //         }
    //     }

    //     @Override
    //     public void selected() {
    //         parameter1View.setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
    //         parameter2View.setModeHidden();
    //         parameter3View.setModeHidden();
    //         parameter4View.setModeHidden();
    //         parameter5View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }

    // };
    // private final CommandHandler abort_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {

    //         int result;
    //         int instanceID;
    //         TextValue instanceIDParameter;
    //         ConnectionCategory connectionCategory = ConnectionCategory.MESSAGE_ACCESS;
    //         BluetoothAddress bluetoothAddress;

    //         if(messageAccessClientManager != null) {

    //             instanceIDParameter = parameter1View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(MainActivity.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             try {
    //                 instanceID = Integer.valueOf(instanceIDParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             result = messageAccessClientManager.abort(connectionCategory, bluetoothAddress, instanceID);

    //             displayMessage("");
    //             displayMessage("abortLabel" + result);

    //         }
    //     }

    //     @Override
    //     public void selected() {

    //         parameter1View.setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
    //         parameter2View.setModeHidden();
    //         parameter3View.setModeHidden();
    //         parameter4View.setModeHidden();
    //         parameter5View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }

    // };

    // Need this
    private final CommandHandler parseRemoteMessageAccessServices_Handler = new CommandHandler() {

	    @Override
	    public void run() {

		BluetoothAddress bluetoothAddress;

		MessageAccessServices[] messageAccessServices = null;

		if(messageAccessClientManager != null) {

		    if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
			showToast(TheMapService.this, "errorInvalidBluetoothAddressToastMessage");
			return;
		    }

		    try {
			messageAccessServices = messageAccessClientManager.parseRemoteMessageAccessServices(bluetoothAddress);

			EnumSet<SupportedMessageType> supportedMessageTypes;

			displayMessage("");

			displayMessage("parseRemoteMessageAccessServicesLabel" );

			for(MessageAccessServices messageAccessService : messageAccessServices) {

			    StringBuilder sb = new StringBuilder();
			    sb.append("serviceNameLabel").append(": ");
			    sb.append(messageAccessService.getServiceName());
			    displayMessage(sb);

			    sb = new StringBuilder();
			    sb.append("serverInstanceIDLabel").append(": ");
			    sb.append(messageAccessService.getInstanceID());
			    displayMessage(sb);

			    sb = new StringBuilder();
			    sb.append("serverPortLabel").append(": ");
			    sb.append(messageAccessService.getServerPort());
			    displayMessage(sb);

			    sb = new StringBuilder();
			    sb.append("supportedMessageTypesLabel").append(": ");
			    displayMessage(sb);

			    supportedMessageTypes = messageAccessService.getSupportedMessageTypes();
			    for(SupportedMessageType supportedMessage : supportedMessageTypes) {
				sb = new StringBuilder();
				sb.append(supportedMessage);
				displayMessage(sb);

				if (supportedMessage.toString().contains("SMS_")) { // Means it is a good one
				    Log.v(TAG,"found a good one");
				    Log.v(TAG,"original instance id was "+mInstanceID);
				    mInstanceID = messageAccessService.getInstanceID();
				    Log.v(TAG,"setting InstanceID to" + mInstanceID);

				    Log.v(TAG,"setting ServerPort");
				    mServerPort = messageAccessService.getServerPort();
				}
			    }
			    supportedMessageTypes.clear();

			    displayMessage("");

			}

		    } catch(BluetopiaPMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		}
	    }

	    @Override
	    public void selected() {
	    }

	    @Override
	    public void unselected() {
		// TODO Auto-generated method stub

	    }
	};



    // Need this;
    private final CommandHandler enableNotifications_Handler = new CommandHandler() {

	    boolean enableChecked = true; // Changed by Ali

	    @Override
	    public void run() {
		int result;
		int instanceID = mInstanceID;
		BluetoothAddress bluetoothAddress;

		if(messageAccessClientManager != null) {

		    //     enableCheckedParameter = parameter1View.getValueCheckbox();
		    //     instanceIDParameter = parameter2View.getValueText();

		    if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
			showToast(TheMapService.this, "errorInvalidBluetoothAddressToastMessage");
			return;
		    }

		    //     enableChecked = enableCheckedParameter.value;

		    //     try {
		    //         instanceID = Integer.valueOf(instanceIDParameter.text.toString());
		    //     } catch(NumberFormatException e) {
		    //         // TODO complain
		    //         return;
		    //     }

		    result = messageAccessClientManager.enableNotifications(bluetoothAddress, instanceID, enableChecked);

		    displayMessage("");
		    displayMessage("enableNotificationsLabel" + result);
		}
	    }

	    @Override
	    public void selected() {

	    }

	    @Override
	    public void unselected() {
		// TODO Auto-generated method stub

	    }
	};

    // private final CommandHandler setFolder_Handler = new CommandHandler() {

    //     String[] pathOptions = {
    //         "Root",
    //         "Up",
    //         "Down"
    //     };

    //     @Override
    //     public void run() {

    //         int result;
    //         int instanceID;
    //         String folderName;
    //         PathOption pathOption;
    //         BluetoothAddress bluetoothAddress;

    //         TextValue instanceIDParameter;
    //         TextValue folderNameParameter;
    //         SpinnerValue pathOptionParameter;

    //         if(messageAccessClientManager != null) {

    //             folderNameParameter = parameter1View.getValueText();
    //             pathOptionParameter = parameter2View.getValueSpinner();
    //             instanceIDParameter = parameter3View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(MainActivity.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             folderName = folderNameParameter.text.toString();

    //             switch(pathOptionParameter.selectedItem) {
    //             case 0:
    //                 pathOption = PathOption.ROOT;
    //                 break;
    //             case 1:
    //                 pathOption = PathOption.UP;
    //                 break;
    //             case 2:
    //                 pathOption = PathOption.DOWN;
    //                 break;
    //             default:
    //                 pathOption = PathOption.DOWN;
    //                 break;
    //             }

    //             try {
    //                 instanceID = Integer.valueOf(instanceIDParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             result = messageAccessClientManager.setFolder(bluetoothAddress, instanceID, pathOption, folderName);

    //             displayMessage("");

    //             displayMessage("setFolderLabel" + result);
    //         }
    //     }

    //     @Override
    //     public void selected() {
    //         parameter1View.setModeText("", "Folder Name: ", InputType.TYPE_CLASS_TEXT);
    //         parameter2View.setModeSpinner("Path Options", pathOptions);
    //         parameter3View.setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
    //         parameter4View.setModeHidden();
    //         parameter5View.setModeHidden();
    //         parameter6View.setModeHidden();
    //         parameter7View.setModeHidden();
    //         parameter8View.setModeHidden();
    //         parameter9View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    // Need this;
    private final CommandHandler setFolderAbsolute_Handler = new CommandHandler() {

	    @Override
	    public void run() {

		int result;
		int instanceID = mInstanceID;
		Log.v(TAG,"instanceID is "+mInstanceID);
		String absolutePath = "/telecom/msg";
		BluetoothAddress bluetoothAddress;


		if(messageAccessClientManager != null) {

		    //     absolutePathParameter = parameter1View.getValueText();
		    //     instanceIDParameter = parameter2View.getValueText();

		    if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
			showToast(TheMapService.this, "errorInvalidBluetoothAddressToastMessage");
			return;
		    }

		    //     absolutePath = absolutePathParameter.text.toString();

		    //     try {
		    //         instanceID = Integer.valueOf(instanceIDParameter.text.toString());
		    //     } catch(NumberFormatException e) {
		    //         // TODO complain
		    //         return;
		    //     }

		    result = messageAccessClientManager.setFolderAbsolute(bluetoothAddress, instanceID, absolutePath);

		    displayMessage("");
		    displayMessage("setFolderAbsoluteLabel" + result);

		}
	    }

	    @Override
	    public void selected() {
	    }

	    @Override
	    public void unselected() {
		// TODO Auto-generated method stub

	    }
	};

    // private final CommandHandler getFolderListing_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {

    //         int result;
    //         int instanceID;
    //         int maxListCount = 1024;
    //         int listStartOffset = 0;
    //         BluetoothAddress bluetoothAddress;

    //         TextValue maxListCountParameter;
    //         TextValue listStartOffsetParameter;
    //         TextValue instanceIDParameter;

    //         if(messageAccessClientManager != null) {

    //             maxListCountParameter = parameter1View.getValueText();
    //             listStartOffsetParameter = parameter2View.getValueText();
    //             instanceIDParameter = parameter3View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(MainActivity.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             try {
    //                 maxListCount = Integer.valueOf(maxListCountParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                     return;
    //             }

    //             try {
    //                 listStartOffset = Integer.valueOf(listStartOffsetParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             try {
    //                 instanceID = Integer.valueOf(instanceIDParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             result = messageAccessClientManager.getFolderListing(bluetoothAddress, instanceID, maxListCount, listStartOffset);

    //             displayMessage("");
    //             displayMessage("getMessageListingLabel" + result);
    //         }
    //     }

    //     @Override
    //     public void selected() {
    //         parameter1View.setModeText("1024", "Max List Count", InputType.TYPE_CLASS_NUMBER);
    //         parameter2View.setModeText("0", "List Start Offset", InputType.TYPE_CLASS_NUMBER);
    //         parameter3View.setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
    //         parameter4View.setModeHidden();
    //         parameter5View.setModeHidden();
    //         parameter6View.setModeHidden();
    //         parameter7View.setModeHidden();
    //         parameter8View.setModeHidden();
    //         parameter9View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };
    // private final CommandHandler getFolderListingSize_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {

    //         int result;
    //         int instanceID;
    //         BluetoothAddress bluetoothAddress;

    //         TextValue instanceIDParameter;

    //         if(messageAccessClientManager != null) {

    //             instanceIDParameter = parameter1View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(MainActivity.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             try {
    //                 instanceID = Integer.valueOf(instanceIDParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             result = messageAccessClientManager.getFolderListingSize(bluetoothAddress, instanceID);

    //             displayMessage("");
    //             displayMessage("getFolderListingSizeLabel" + result);

    //         }
    //     }

    //     @Override
    //     public void selected() {
    //         parameter1View.setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
    //         parameter2View.setModeHidden();
    //         parameter3View.setModeHidden();
    //         parameter4View.setModeHidden();
    //         parameter5View.setModeHidden();
    //         parameter6View.setModeHidden();
    //         parameter7View.setModeHidden();
    //         parameter8View.setModeHidden();
    //         parameter9View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };
    // private final CommandHandler getMessageListing_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {

    //         int result;
    //         int instanceID;
    //         int maxListCount = 1024;
    //         int listStartOffset = 0;
    //         String folderName;
    //         BluetoothAddress bluetoothAddress;

    //         TextValue folderNameParameter;
    //         TextValue maxListCountParameter;
    //         TextValue listStartOffsetParameter;
    //         TextValue instanceIDParameter;

    //         ListingInfo listingInfo = null;

    //         if(messageAccessClientManager != null) {

    //             folderNameParameter = parameter1View.getValueText();
    //             maxListCountParameter = parameter2View.getValueText();
    //             listStartOffsetParameter = parameter3View.getValueText();
    //             instanceIDParameter = parameter4View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(MainActivity.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             folderName = folderNameParameter.text.toString();

    //             try {
    //                 maxListCount = Integer.valueOf(maxListCountParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             try {
    //                 listStartOffset = Integer.valueOf(listStartOffsetParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             try {
    //                 instanceID = Integer.valueOf(instanceIDParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             result = messageAccessClientManager.getMessageListing(bluetoothAddress, instanceID, folderName, maxListCount, listStartOffset, listingInfo);

    //             displayMessage("");
    //             displayMessage("getMessageListingLabel" + result);
    //         }
    //     }

    //     @Override
    //     public void selected() {
    //         parameter1View.setModeText("", "Folder Name", InputType.TYPE_CLASS_TEXT);
    //         parameter2View.setModeText("1024", "Max List Count", InputType.TYPE_CLASS_NUMBER);
    //         parameter3View.setModeText("0", "List Start Offset", InputType.TYPE_CLASS_NUMBER);
    //         parameter4View.setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
    //         parameter5View.setModeHidden();
    //         parameter6View.setModeHidden();
    //         parameter7View.setModeHidden();
    //         parameter8View.setModeHidden();
    //         parameter9View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };
    // private final CommandHandler getMessageListingSize_Handler = new CommandHandler() {

    //     String[]  messageTypeLabels = {
    //         "SMS GSM",
    //         "SMS CDMA",
    //         "Email",
    //         "MMS"
    //     };

    //     boolean[] messageTypeValues = new boolean[] {
    //         false,
    //         false,
    //         false,
    //         true,
    //     };

    //     String[]  readStatusOptions = {
    //         "Read Only",
    //         "Unread Only",
    //     };

    //     String[]  priorityOptions = {
    //         "Non-High Only",
    //         "High Only",
    //     };

    //     @Override
    //     public void run() {

    //         int                  result;
    //         int                  instanceID;
    //         String               folderName = null;
    //         EnumSet<MessageType> messageTypes;
    //         Calendar             periodBegin = Calendar.getInstance();
    //         Calendar             periodEnd = Calendar.getInstance();
    //         FilterReadStatus     readStatus;
    //         String               recipient = null;
    //         String               originator = null;
    //         String				 dateFormat = "ddMMyyyy";
    //         FilterPriority       priority;
    //         BluetoothAddress     bluetoothAddress;

    //         TextValue      folderNameParameter;
    //         TextValue      instanceIDParameter;
    //         ChecklistValue messageTypeParameter;
    //         TextValue      periodBeginParameter;
    //         TextValue      periodEndParameter;
    //         SpinnerValue   readStatusParameter;
    //         TextValue      recipientParameter;
    //         TextValue      originatorParameter;
    //         SpinnerValue   priorityParameter;

    //         if(messageAccessClientManager != null) {

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(MainActivity.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             folderNameParameter  = parameter1View.getValueText();
    //             instanceIDParameter  = parameter2View.getValueText();
    //             messageTypeParameter = parameter3View.getValueChecklist();
    //             periodBeginParameter = parameter4View.getValueText();
    //             periodEndParameter   = parameter5View.getValueText();
    //             readStatusParameter  = parameter6View.getValueSpinner();
    //             recipientParameter   = parameter7View.getValueText();
    //             originatorParameter  = parameter8View.getValueText();
    //             priorityParameter    = parameter9View.getValueSpinner();

    //             folderName = folderNameParameter.text.toString();

    //             try {
    //                 instanceID = Integer.valueOf(instanceIDParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             messageTypes = EnumSet.noneOf(MessageType.class);

    //             if(messageTypeParameter.checkedItems[0]) {
    //                 messageTypes.add(MessageType.SMS_GSM);
    //             }
    //             if(messageTypeParameter.checkedItems[1]) {
    //                 messageTypes.add(MessageType.SMS_CDMA);
    //             }
    //             if(messageTypeParameter.checkedItems[2]) {
    //                 messageTypes.add(MessageType.EMAIL);
    //             }
    //             if(messageTypeParameter.checkedItems[3]) {
    //                 messageTypes.add(MessageType.MMS);
    //             }

    //             SimpleDateFormat beginDateFormat = new SimpleDateFormat(dateFormat);
    //             try {
    //                 periodBegin.setTime(beginDateFormat.parse(periodBeginParameter.text.toString()));
    //             } catch(ParseException e) {
    //                 // TODO Auto-generated catch block
    //                 e.printStackTrace();
    //             }

    //             SimpleDateFormat endDateFormat = new SimpleDateFormat(dateFormat);
    //             try {
    //                 periodEnd.setTime(endDateFormat.parse(periodEndParameter.text.toString()));
    //             } catch(ParseException e) {
    //                 // TODO Auto-generated catch block
    //                 e.printStackTrace();
    //             }

    //             switch(readStatusParameter.selectedItem) {
    //             case 0:
    //                 readStatus = FilterReadStatus.READ_ONLY;
    //                 break;
    //             case 1:
    //             default:
    //                 readStatus = FilterReadStatus.UNREAD_ONLY;
    //                 break;
    //             }

    //             recipient = recipientParameter.text.toString();

    //             originator = originatorParameter.text.toString();


    //             switch(priorityParameter.selectedItem) {
    //             case 0:
    //                 priority = FilterPriority.NON_HIGH_ONLY;
    //                 break;
    //             case 1:
    //             default:
    //                 priority = FilterPriority.HIGH_ONLY;
    //                 break;
    //             }

    //             ListingSizeInfo listingSizeInfo = new ListingSizeInfo(messageTypes, periodBegin, periodEnd, readStatus, recipient, originator, priority);

    //             result = messageAccessClientManager.getMessageListingSize(bluetoothAddress, instanceID, folderName, listingSizeInfo);

    //             displayMessage("");
    //             displayMessage("getMessageListingSizeLabel" + result);
    //         }
    //     }

    //     @Override
    //     public void selected() {
    //         parameter1View.setModeText("", "Folder Name", InputType.TYPE_CLASS_TEXT);
    //         parameter2View.setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
    //         parameter3View.setModeChecklist("Message Types", messageTypeLabels, messageTypeValues);
    //         parameter4View.setModeText("", "Period Begin", InputType.TYPE_CLASS_DATETIME);
    //         parameter5View.setModeText("", "Period End", InputType.TYPE_CLASS_DATETIME);
    //         parameter6View.setModeSpinner("Read Status", readStatusOptions);
    //         parameter7View.setModeText("", "Recipient", InputType.TYPE_CLASS_TEXT);
    //         parameter8View.setModeText("", "Originator", InputType.TYPE_CLASS_TEXT);
    //         parameter9View.setModeSpinner("Priority", priorityOptions);
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }
    // };

    // Need this;
    private final CommandHandler getMessage_Handler = new CommandHandler() {

	    String[] charSetOptions = {
		"Native",
		"UTF8"
	    };

	    boolean  attachmentChecked = false;

	    @Override
	    public void run() {

		int result;
		int instanceID = mInstanceID;
		String messageHandle;
		CharSet charSet = CharSet.UTF8;
		BluetoothAddress bluetoothAddress;

		if(messageAccessClientManager != null) {

		    //attachmentCheckedParameter = parameter2View.getValueCheckbox();
		    if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
		        showToast(TheMapService.this, "errorInvalidBluetoothAddressToastMessage");
		        return;
		    }

		    messageHandle = "" + mMessageHandle;
		    attachmentChecked = false; // No attachment


		    result = messageAccessClientManager.getMessage(bluetoothAddress, instanceID, messageHandle, attachmentChecked, charSet, FractionRequest.FIRST);

		    displayMessage("");
		    displayMessage("getMessageLabel" + result);
		}
	    }

	    @Override
	    public void selected() {
	    }

	    @Override
	    public void unselected() {
		// TODO Auto-generated method stub

	    }
	};

    // private final CommandHandler setMessageStatus_Handler = new CommandHandler() {

    //     String[] statusIndicatorOptions = {
    //         "Read",
    //         "Deleted"
    //     };

    //     @Override
    //     public void run() {

    //         int result;
    //         int instanceID;
    //         String messageHandle;
    //         StatusIndicator statusIndicator;
    //         boolean statusValue = false;
    //         BluetoothAddress bluetoothAddress;

    //         TextValue instanceIDParameter;
    //         TextValue messageHandleParameter;
    //         CheckboxValue statusValueParameter;
    //         SpinnerValue statusIndicatorParameter;

    //         if(messageAccessClientManager != null) {

    //             messageHandleParameter = parameter1View.getValueText();
    //             statusIndicatorParameter = parameter2View.getValueSpinner();
    //             statusValueParameter = parameter3View.getValueCheckbox();
    //             instanceIDParameter = parameter4View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(MainActivity.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             messageHandle = messageHandleParameter.text.toString();

    //             switch(statusIndicatorParameter.selectedItem) {
    //             case 0:
    //                 statusIndicator = StatusIndicator.READ_STATUS;
    //                 break;
    //             case 1:
    //                 statusIndicator = StatusIndicator.DELETED_STATUS;
    //                 break;
    //             default:
    //                 statusIndicator = null;
    //                 break;
    //             }

    //             statusValue = statusValueParameter.value;

    //             try {
    //                 instanceID = Integer.valueOf(instanceIDParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             result = messageAccessClientManager.setMessageStatus(bluetoothAddress, instanceID, messageHandle, statusIndicator, statusValue);

    //             displayMessage("");
    //             displayMessage("setMessageStatusLabel" + result);
    //         }
    //     }

    //     @Override
    //     public void selected() {

    //         parameter1View.setModeText("", "Message Handle", InputType.TYPE_CLASS_TEXT);
    //         parameter2View.setModeSpinner("Status Indicator", statusIndicatorOptions);
    //         parameter3View.setModeCheckbox("Status Value:", false);
    //         parameter4View.setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
    //         parameter5View.setModeHidden();
    //         parameter6View.setModeHidden();
    //         parameter7View.setModeHidden();
    //         parameter8View.setModeHidden();
    //         parameter9View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub
    //     }
    // };
    // private final CommandHandler pushMessage_Handler = new CommandHandler() {

    //     String[]  charSetOptions = {
    //         "UTF8",
    //         "Native"
    //     };

    //     String[]  messageOptions = {
    //         "Save a Copy in Sent Folder",
    //         "Retry Send in Case of Failure",
    //         "Final Packet"
    //     };

    //     boolean[] optionsChecked = {
    //         true,
    //         false,
    //         true
    //     };

    //     @Override
    //     public void run() {

    //         int result;
    //         int instanceID;
    //         byte[] messageBuffer = new byte[1024];
    //         String folderName;
    //         CharSet charSet;
    //         BluetoothAddress bluetoothAddress;

    //         TextValue folderNameParameter;
    //         TextValue instanceIDParameter;
    //         ChecklistValue checkBoxParameter;
    //         SpinnerValue charSetParameter;

    //         if(messageAccessClientManager != null) {

    //             folderNameParameter = parameter1View.getValueText();
    //             checkBoxParameter = parameter2View.getValueChecklist();
    //             charSetParameter = parameter3View.getValueSpinner();
    //             instanceIDParameter = parameter4View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(MainActivity.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             folderName = folderNameParameter.text.toString();

    //             switch(charSetParameter.selectedItem) {
    //             case 0:
    //                 charSet = CharSet.NATIVE;
    //                 break;
    //             case 1:
    //             default:
    //                 charSet = CharSet.UTF8;
    //                 break;
    //             }

    //             try {
    //                 instanceID = Integer.valueOf(instanceIDParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             AssetManager assetManager = getAssets();
    //             try {

    //                 int read;

    //                 InputStream inputStream = assetManager.open("test-message");
    //                 BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

    //                 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    //                 while((read = bufferedInputStream.read(messageBuffer, 0, messageBuffer.length)) != -1) {
    //                     byteArrayOutputStream.write(messageBuffer, 0 , read);
    //                 }

    //                 byteArrayOutputStream.flush();

    //                 messageBuffer = byteArrayOutputStream.toByteArray();

    //             } catch(IOException e) {
    //                 // TODO Auto-generated catch block
    //                 e.printStackTrace();
    //             }

    //             result = messageAccessClientManager.pushMessage(bluetoothAddress, instanceID, folderName, checkBoxParameter.checkedItems[0] ? true : false , checkBoxParameter.checkedItems[1] ? true : false , charSet, messageBuffer, checkBoxParameter.checkedItems[2] ? true : false);

    //             displayMessage("");
    //             displayMessage("pushMessageLabel" + result);
    //         }
    //     }

    //     @Override
    //     public void selected() {
    //         parameter1View.setModeText("", "Folder Name", InputType.TYPE_CLASS_TEXT);
    //         parameter2View.setModeChecklist("Message Options", messageOptions, optionsChecked);
    //         parameter3View.setModeSpinner("Character Set", charSetOptions);
    //         parameter4View.setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
    //         parameter5View.setModeHidden();
    //         parameter6View.setModeHidden();
    //         parameter7View.setModeHidden();
    //         parameter8View.setModeHidden();
    //         parameter9View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub

    //     }

    // };
    // private final CommandHandler updateInbox_Handler = new CommandHandler() {

    //     @Override
    //     public void run() {
    //         int result;
    //         int instanceID;
    //         BluetoothAddress bluetoothAddress;

    //         TextValue instanceIDParameter;

    //         if(messageAccessClientManager != null) {

    //             instanceIDParameter = parameter1View.getValueText();

    //             if((bluetoothAddress = getRemoteDeviceAddress()) == null) {
    //                 showToast(MainActivity.this, "errorInvalidBluetoothAddressToastMessage");
    //                 return;
    //             }

    //             try {
    //                 instanceID = Integer.valueOf(instanceIDParameter.text.toString());
    //             } catch(NumberFormatException e) {
    //                 // TODO complain
    //                 return;
    //             }

    //             result = messageAccessClientManager.updateInbox(bluetoothAddress, instanceID);

    //             displayMessage("");
    //             displayMessage("updateInboxLabel" + result);
    //         }
    //     }

    //     @Override
    //     public void selected() {
    //         parameter1View.setModeText("", "Instance ID", InputType.TYPE_CLASS_NUMBER);
    //         parameter2View.setModeHidden();
    //         parameter3View.setModeHidden();
    //         parameter4View.setModeHidden();
    //         parameter5View.setModeHidden();
    //         parameter6View.setModeHidden();
    //         parameter7View.setModeHidden();
    //         parameter8View.setModeHidden();
    //         parameter9View.setModeHidden();
    //     }

    //     @Override
    //     public void unselected() {
    //         // TODO Auto-generated method stub
    //     }
    // };

    // private final Command[] commandList = new Command[] {
    //     new Command("Connect Remote Device", connectRemoteDevice_Handler),
    //     new Command("Disconnect", disconnect_Handler),
    //     new Command("Query Current Folder", queryCurrentFolder_Handler),
    //     new Command("Abort", abort_Handler),
    //     new Command("Parse Remote Message Access Services", parseRemoteMessageAccessServices_Handler),
    //     new Command("Enable Notifications", enableNotifications_Handler),
    //     new Command("Set Folder", setFolder_Handler),
    //     new Command("Set Folder Absolute", setFolderAbsolute_Handler),
    //     new Command("Get Folder Listing", getFolderListing_Handler),
    //     new Command("Get Folder Listing Size", getFolderListingSize_Handler),
    //     new Command("Get Message Listing", getMessageListing_Handler),
    //     new Command("Get Message Listing Size", getMessageListingSize_Handler),
    //     new Command("Get Message", getMessage_Handler),
    //     new Command("Set Message Status", setMessageStatus_Handler),
    //     new Command("Push Message", pushMessage_Handler),
    //     new Command("Update Inbox", updateInbox_Handler),
    // };

    private void connectWithTheDevice(BluetoothAddress bluetoothaddress) {
	Log.v(TAG,"request to connectWithTheDevice");
	// call connectDeviceHandler
	mRemoteDeviceAddress = bluetoothaddress;
	Log.v(TAG,"updateRemoteDeviceServices");
	// TODO: run parse stuff
	// Find instance ID and server port
	Log.v(TAG,"discover good message");
	parseRemoteMessageAccessServices_Handler.run();
	Log.v(TAG,"connectRemoteDevice");
	connectRemoteDevice_Handler.run();
	//setFolderAbsolute_Handler.run();
	//enableNotifications_Handler.run();
    }

    private void disconnectFromTheDevice() {
	disconnect_Handler.run();
    }

    
    //==================================================================
    // This stuff is pertaining to parsing the XML message.
    private int getHandleIfNewMessage(byte[] stringbuffer) {
	String eventString = new String(stringbuffer);
	Document docu;
	int handle = -1;
	try {
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(eventString));
	    // First generate a document document
	    docu = DocumentBuilderFactory.newInstance()
		.newDocumentBuilder().parse(is);
	    // Now find <event> tags.
	    NodeList eventNodes = docu.getElementsByTagName("event");
	    if (eventNodes == null) {
		Log.i(TAG,"no event tag found");
		return -1;
	    }
	    // go through all event tags and start on first new
	    // message; TODO: Make sure there is only one new message
	    // event from SS1 otherwise there is more work to be done
	    // here.
	    for (int i=0; i< eventNodes.getLength(); i++) {
		Element eventTag = (Element) eventNodes.item(i);
		if (isTypeNewMessage(eventTag)) {
		    // means <event type="NewMessage">
		    handle = getHandle(eventTag);
		    if (handle != -1) {
			return handle;
		    }
		}
	    }
	} catch (Exception e) {
	    Log.w(TAG, "Can't parse new message");
	    e.printStackTrace();
	    return -1;
	}
	return handle;		// which is -1;
    }
    // Helper functions for parsing MAP-event-report XML
    private boolean isTypeNewMessage(Element el) {
	String typeattr = el.getAttribute("type");
	return typeattr.equals("NewMessage");
    }
    private int getHandle(Element el) {
	String handleattr = el.getAttribute("handle");
	int handle;
	if (handleattr == null) {
	    handle = -1;
	}
	else {			// There is something in the handle
	    try {		// convert string to integer
		handle = Integer.valueOf(handleattr);
	    } catch(NumberFormatException e) {
		Log.w(TAG,"handle has bad format");
		handle = -1;
	    }
	}
	return handle;	
    }
    //==============================================

}

