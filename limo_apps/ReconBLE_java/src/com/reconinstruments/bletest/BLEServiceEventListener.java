package com.reconinstruments.bletest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.reconinstruments.nativetest.R;
import com.reconinstruments.reconble.*;
import com.reconinstruments.reconble.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.IOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
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
import android.os.Handler;
import android.os.Environment;

import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.modlivemobile.utils.FileUtils.FilePath.PathType;
//import com.reconinstruments.connect.apps.ConnectHelper;
    
public class BLEServiceEventListener  implements IReconBLEEventListener {

    private BLETestService mTheService = null;
    private static final String TAG = "BLEServiceEventListener";
    
    public BLEServiceEventListener (BLETestService theService) {
	mTheService = theService;
    }
    

    /* IReconBLEEventListener Implementation -- handle Async BLE Events */
    // Master - Slave pair completed callback
    public void onPairChanged(int status) {
	String strStatus = new String();
	if (status == ReconBLE.BLEStatus.STATUS_CONNECTED)   {
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_CONNECTED");
	    solidBlueNotification();
	    // We want only one tryout after 
	    mTheService.mDisconnectionCounter = BLETestService.MAXIMUM_CONNECTION_TRIAL;
	    Intent myi = new Intent(BLETestService.com_reconinstruments_ble_connected);
	    myi.addFlags(32); 
	    mTheService.sendBroadcast(myi);

	    if (!mTheService.isMaster) {//We are in slave mode
		// Only update device list if after disconnect or unpair
		if (mTheService.mStatus != ReconBLE.BLEStatus.STATUS_CONNECTED) {
		    Log.v(TAG,"Broadcasting Connected message the message");
		    updateLastDeviceConnected();
		    notifyiOSConnectionState(true);
		    // Now let iOS know that we are paired:
		    mTheService.mBLE.SendControlByte((byte)0x12);
		    mDelayedAction.postDelayed(mSend0x12,2000); // Send after two seconds
		    // We send after connect info.
		    sendAfterConnectInfo();
		}
		    
		mTheService.xmlCanCome = true;

		
	    } else {		// We are in master mode
		// We know that the remote is connected to us so
		// obviousely it is not connected to iOS
		mTheService.miOSRemoteStatus = BLETestService.IOS_REMOTE_STATUS_DISCONNECTED;
		if (mTheService.fullRemoteMacAddressIsNeeded == true) {
		    Log.d(TAG,"need to put full MAC address");
		    byte[] remote = mTheService.mBLE.GetRemoteMac(0); //0: no version
		    if (remote.length == BLETestService.MAC_ADDRESS_SIZE) {
			BLETestService.writeToBLEFile(remote);
			mTheService.fullRemoteMacAddressIsNeeded = false;
		    } else {
			Log.w(TAG,"mac address of the remote is not 6 bytes");
		    }
		}
	    }
	}

	//	TODO: New status for remote reset
	else if (status == 0xFE || status == 0xF0)   {//Switched mode either slave or master
	    BLELog.d(TAG,"Mode confirmed");
	    mTheService.miOSRemoteStatus = BLETestService.IOS_REMOTE_STATUS_UNKNOWN;

	    if (status == 0xFE) {//Means switched to slave
		mTheService.mBLE.mUnifiedTask.ClearBuffers();
		Intent myi = new Intent(BLETestService.com_reconinstruments_ble_switched_to_slave);
		//myi.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES); 
		myi.addFlags(32); 
		mTheService.sendBroadcast(myi);
	    }
	    else {//Switched to master
		notifyiOSConnectionState(false);
		Intent myi = new Intent(BLETestService.com_reconinstruments_ble_switched_to_master);
		//		myi.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES); 
		myi.addFlags(32); 
		mTheService.sendBroadcast(myi);

	    }
	    mTheService.mDisconnectionCounter = 0;
	    solidRedNotification();
	    mTheService.pairDevice();
	}
	else if (status == ReconBLE.BLEStatus.STATUS_REMOTE_LIST)  {
	    BLELog.d(TAG, "STATUS_REMOTE_LIST");
	    byte[] remotes = getRemoteList();
	    BLELog.d(TAG, "Have mac address:" + remotes);
	    if (remotes != null) {
		if (mTheService.isMaster) {
		    Intent i = new Intent("RECON_BLE_PAIRING_ACTIVITY");
		    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    i.putExtra("remotes",remotes);
		    mTheService.startActivity(i);
		}
		else {	// We are slave
			// We don't really care about mac address
			// although in the future we can just show
			// the mac address to the user. But in
			// reality you just go directly for irk
		    BLELog.d(TAG, "Calling security");
		    // toast("Asking for irk");
		    mTheService.mBLE.RequestSecurity();
		}
	    }
	    else {
		strStatus = "Failed to retrieve Remote Master List...";
	    }
	}
	else if (status == ReconBLE.BLEStatus.STATUS_IRK_READY) {
	    BLELog.d(TAG,"Connected");
	    LinkedList<byte []> lstIrk = new LinkedList<byte []> ();
	    int stat = mTheService.mBLE.GetIrk(lstIrk);
	    saveIrk(lstIrk.getFirst());
	    strStatus = "connected";
	    
	}
	else if (status == ReconBLE.BLEStatus.STATUS_MAC_READY) {
	    //	    Log.v(TAG,"STATUS_MAC_READY");
	    
	    // This means the remote is connected and you can
	    // fetch the mac addres of the remote.

	    // Although we already have the mac address due to user
	    // selection or our own BLEE file. We still need this in
	    // order to find the remote version.
	    byte[] remote = null;
	    
	    remote = mTheService.mBLE.GetRemoteMac(1); //1:version needed
	    if (remote != null) {
		Log.v(TAG,"Remote Version is "+remote[6]);
		mTheService.mRemoteControlVersion = remote[6];
	    } else {
		Log.e(TAG,"Remote mac and version is null");
	    }
	}
	else if (status == ReconBLE.BLEStatus.STATUS_DISCONNECTED ||
		 status == ReconBLE.BLEStatus.STATUS_UNPAIR) {
	    mTheService.canSendIncrementalRib = false;
	    BLELog.v(TAG,"disconnect counter = "+mTheService.mDisconnectionCounter);
	    mTheService.mDisconnectionCounter++;
	    //Log.v(TAG,"mDisconnectionCounter: "+mTheService.mDisconnectionCounter);
	    if (!mTheService.isMaster && mTheService.mDisconnectionCounter > BLETestService.MAXIMUM_CONNECTION_TRIAL) {//We are slave and have retried enough
		BLELog.d(TAG,"retrying...");
		BLELog.d(TAG,"Too many trials...Switch back to master");
		// Switch to master:
		mTheService.setMaster();
		//Let them know that there will be no iOS connection
		notifyiOSConnectionState(false);
		BLELog.d(TAG, "Not calling pairDevice");
		// pairDevice();
		// launch the fail activity
		Intent i = new Intent("com.reconinstruments.connectdevice.IOS_RETRY");
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mTheService.startActivity(i);

		    
	    }
	    else if (!mTheService.isMaster){//We are slave and haven't retried enough
		BLELog.d(TAG,"retrying...");
		//NOTE: I really don't want to rest device but it
		//seems that it doesn't work if I don't  reset
		//mTheService.resetDevice();
		//After testing it seems that it works
		mTheService.mBLE.mUnifiedTask.ClearBuffers();
		mTheService.pairDevice();
	    }

	    solidRedNotification();

	    Intent myi = new Intent(BLETestService.com_reconinstruments_ble_connected);
	    myi.addFlags(32); 
	    mTheService.sendBroadcast(myi);

	}
	else if (status ==ReconBLE.BLEStatus.STATUS_CANCEL_BOND ) {
	    //It means that the user pressed left and right button on remote
	    if (mTheService.isMaster) {// isMaster
		BLELog.d(TAG,"STATUS_CANCEL_BOND in master mode");
		deleteBLEFile();
		solidRedNotification();
		mTheService.resetDevice();
	    }
	}
	else {

	}
	mTheService.mStatus = status;
	//	toast(strStatus);
    }
    // Receive (Rx) "tick" update
    public void onReceiveUpdate(int bytes) {
	// HACK: note that this function is only called when updates
	// are from file transfer and not XML.
	Log.d(TAG, "In file trasnfer");
	Log.d(TAG,"mTheService.xmlCanCome = false");
    }
	
    // Receive (Rx) Full completion status of a file. Rx Monitor is NOT
    // terminated & keeps monitoring
    public void onReceiveCompleted(int status, Bundle theBundle) {
	String strStatus = new String();
		
	if (status == ReconBLE.ERR_SUCCESS)
	    strStatus = "Incoming transmission from iPhone completed";
	else
	    strStatus = "Receive Failure";

	BLELog.d(TAG,strStatus);
	String tempFileName = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ theBundle.getString("tempFileName");
	BLELog.d(TAG,"The filename is "+tempFileName);
	mTheService.generateGoodFile(tempFileName);
	// HACK:
	mTheService.xmlCanCome = true;
    }

    // Send (Tx) "tick" update: Number of bytes sent so far
    public void onSendUpdate(int bytes) {
	String strStatus = String.format("onSendUpdate: Sent [%d] bytes", bytes);
	BLELog.d(TAG,strStatus);
	// So that we don't reset for no reason
	mTheService.resetFailedPushCounter();
    }
    public void onSendCompleted(int status, Bundle theBundle) {
	String strStatus = new String();
	int lastsentprior = theBundle.getInt(BLEMessageHandler.KEY_LAST_SENT_PRIORITY,-1);
	if (lastsentprior == 0) {
	    mTheService.xmlCanCome = true;
	    Log.d(TAG," mTheService.xmlCanCome = true;");
	}
	if (status == ReconBLE.ERR_SUCCESS)
	    strStatus = "Send Completed with priority "+lastsentprior;
	else
	    strStatus = "Send Failure";
	BLELog.d(TAG,strStatus);
	//So that we don't reset for no reason
	mTheService.resetFailedPushCounter();
	//
	// Now we decide on the next element to push:
	for (int i = 3; i >=0; i--) {
	    if (mTheService.mPriorList[i] != null){
		if (!mTheService.mPriorList[i].isEmpty()) {
		    //FIXME: Stupid sleep
		    try {
			long numMillisecondsToSleep = 1; // 1 miliseconseconds
			Thread.sleep(numMillisecondsToSleep);
		    } catch (InterruptedException e) {
		    }
		    mTheService.pushElement(i);
		    return;
		}
	    }
	}
	mTheService.pushNothing(); // Means have nothing to send
    }
    public void onXmlReceived(String xml) {
	BLELog.d(TAG,xml);
	mTheService.broadcastReceivedMessage(xml);
    }

    public void onMiscData (byte [] miscData) {
	//	BLELog.v(TAG,"miscData");
	byte length = miscData[0];
	byte type = miscData[1];
	BLELog.v(TAG,"Length "+length+" type "+type);
	if (type == -71) {
	    BLELog.v(TAG,"It is temperature");
	    BLELog.v(TAG, "Temperature is "+ miscData[2]);
	    mTheService.mTemperature = miscData[2];
	}
    }


    private void solidBlueNotification(){
	PendingIntent contentIntent = PendingIntent.getActivity(mTheService, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
	Notification n;
	if (mTheService.isMaster) {
	    n = new Notification(R.drawable.remote_blue, null, System.currentTimeMillis());
	}
	else {
	    n = new Notification(R.drawable.ios_blue, null, System.currentTimeMillis());
	}
	n.setLatestEventInfo(mTheService, "", "", contentIntent);
	n.flags |= Notification.FLAG_ONGOING_EVENT;
	n.flags |= Notification.FLAG_NO_CLEAR;
	mTheService.mNotificationManager.notify(BLETestService.BLE_ID, n);
    }

    private void solidRedNotification() {
	PendingIntent contentIntent = PendingIntent.getActivity(mTheService, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
	Notification n;
	if (mTheService.isMaster) {
	    n = new Notification(R.drawable.remote_red, null, System.currentTimeMillis());
	}
	else {
	    n = new Notification(R.drawable.ios_red, null, System.currentTimeMillis());
	}
	n.setLatestEventInfo(mTheService, "", "", contentIntent);
	n.flags |= Notification.FLAG_ONGOING_EVENT;
	n.flags |= Notification.FLAG_NO_CLEAR;
	mTheService.mNotificationManager.notify(BLETestService.BLE_ID, n);
    }

    private void notifyiOSConnectionState(boolean stat) {
	Intent devi = new Intent(BTCommon.MSG_STATE_UPDATED);
	devi.putExtra("connected",stat);
	devi.putExtra("device","ios");
	devi.addFlags(32); 
	mTheService.sendBroadcast(devi);
    }


    // Bunch of helper functions
    private void updateLastDeviceConnected() {
	Settings.System.putString(mTheService.getContentResolver(), "LastDeviceConnected", "iOS");
	Settings.System.putString(mTheService.getContentResolver(), "DisableSmartphone", "false");
    }
    private void deleteBLEFile() {
	BLELog.d(TAG,"Attempt to delete BLE file");
	File srcFile = new File(Environment.getExternalStorageDirectory(),"BLE.RIB");
	srcFile.delete();
    }
    private void saveIrk(byte[] irk) {
	BLELog.d(TAG,"saveIrk");
	SharedPreferences.Editor editor = mTheService.mPersistantStats.edit();
	editor.putString("Irk",Base64.encodeToString(irk,0));
	editor.commit();
    }

    private byte[] loadIrk() {
	String irkString = (mTheService.mPersistantStats.getString("Irk",null));
	byte [] irk= null;
	if (irkString != null){
	    irk = Base64.decode(irkString,0);
	}
	return irk;
    }

    private byte[] getRemoteList() {
	byte[] remotes = null;
	byte[] tempremotelist = new byte[BLETestService.MAC_ADDRESS_SIZE*4  +1];
	int stat = mTheService.mBLE.GetRemoteList(tempremotelist);
	if (stat == ReconBLE.ERR_SUCCESS) {
	    BLELog.d(TAG, "Have remote list");
	    //		toast( "Have remote list");
	    int numRemotes = tempremotelist[0]/BLETestService.MAC_ADDRESS_SIZE;
	    BLELog.d(TAG,"num remotes "+numRemotes);
	    remotes = new byte[numRemotes*BLETestService.MAC_ADDRESS_SIZE];
	    System.arraycopy(tempremotelist, 1, remotes, 0, numRemotes* BLETestService.MAC_ADDRESS_SIZE);
	    BLELog.d(TAG, "Have mac address:" + remotes);
	}
	return remotes;
    }

    private void sendAfterConnectInfo() {
	// This functions creates an xml of crucial info which we call
	// "AfterconnectInfo" on the MODLive side that iOS might need
	// and sends it to iOS This function is called after
	// connection.
	

	//Read the remote contorl
	byte[] remoteAddress =  BLETestService.readBLEFile();
	//
	String address = "unknown";
	if (remoteAddress != null) {
	    address  = String.format("%02X %02X %02X %02X %02X %02X",
				     remoteAddress[0],
				     remoteAddress[1],
				     remoteAddress[2],
				     remoteAddress[3],
				     remoteAddress[4],
				     remoteAddress[5]);
	}

	// Put the remote control and firmware version into the string

	String xmlMessage = "<recon intent=\"after_connect\"><remote_address>"+
	    address+"</remote_address><firmware_version>"+getVersionName()+"</firmware_version></recon>";
	mTheService.addToPriorList(xmlMessage, 1);
	
    }

    private String getVersionName(){
	
	String versionName="unknown";
	try {
	    PackageInfo pInfo =
		mTheService.getPackageManager().getPackageInfo(mTheService.getPackageName(),  PackageManager.GET_META_DATA);
	    versionName = pInfo.versionName;
	} catch (NameNotFoundException e1) {
	    Log.e(mTheService.getClass().getSimpleName(), "Name not found", e1);
	}
	return versionName;
    }

    private Handler mDelayedAction = new Handler();
    private final  Runnable mSend0x12=new Runnable()	{
	    public void run()   {
		mTheService.mBLE.SendControlByte((byte)0x12);
	    }
	};

}