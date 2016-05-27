package com.reconinstruments.bletest;
import android.util.Base64;
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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.reconinstruments.nativetest.R;
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

import com.reconinstruments.modlivemobile.bluetooth.BTCommon;
import com.reconinstruments.modlivemobile.utils.FileUtils.FilePath.PathType;
import com.reconinstruments.modlivemobile.utils.FileUtils.*;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.utils.DeviceUtils;

import android.os.Handler;
import android.os.Environment;

public class BLETestService extends Service {

    public static final int MAXIMUM_CONNECTION_TRIAL = 2;//0;
    public static final int MAC_ADDRESS_SIZE = 6;
    public static String BLE_FILE_LOC = Environment.getExternalStorageDirectory().getAbsolutePath()+"/BLE.RIB";
    public static final int BLE_ID = 813;
    public  static final String com_reconinstruments_ble_switched_to_slave =
	"com.reconinstruments.ble.SWITCHED_TO_SLAVE";
    public  static final String com_reconinstruments_ble_switched_to_master =
	"com.reconinstruments.ble.SWITCHED_TO_MASTER";
    public  static final String com_reconinstruments_ble_connected =
	"com.reconinstruments.ble.CONNECTED";
    public static final String RIB_PARENT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ReconApps/TripData/";
    public static final int MAX_FAIL_PUSH = 2;
    
    private static final String TAG = "BLETestService";


    // HACK: So that we can do only one thing at a time. FIXME for duplex
    public boolean mInMusicApp = false;
    // End hack
    
    public NotificationManager mNotificationManager;
    public SharedPreferences mPersistantStats;
    public boolean xmlCanCome = true;
    public boolean canSendIncrementalRib = false; // We can only send
						  // incremental rib
						  // when iphone
						  // requests rib file
    
    // buffer we pass to BLE Rx API; gets filled by Incoming Byte stream
    // can be used during "tick" updates (IReconBLEEventListener::onSendUpdate)
    public ByteArrayOutputStream mIncoming = new ByteArrayOutputStream();
    // BLE Recon API instance
    public ReconBLE mBLE = null;
    public boolean isMaster = true;
    public boolean isMasterReadFromPersistanceAtServiceCreate;
    public int mStatus = ReconBLE.BLEStatus.STATUS_DISCONNECTED;
    public byte mTemperature;
    public int mDisconnectionCounter = 0;
    public byte [] myownmac = new byte[6];
    public int mFailedpush = 0;

    private CommandReceiver mCommandReceiver = new CommandReceiver(this);
    private RIBCommandReceiver mRIBCommandReceiver = new RIBCommandReceiver(this);
    private XmlMessageReceiver msgReceiver = new XmlMessageReceiver(this);
    private BuddyDummyClassReceiver mBuddyDummyClassReceiver = new BuddyDummyClassReceiver();
    private IOSDeviceNameReceiver miOSDeviceNameReceiver = new IOSDeviceNameReceiver(this);
    private IOSRemoteStatusReceiver miOSRemoteStatusReceiver = new IOSRemoteStatusReceiver(this);
    private boolean mReceiveTaskStarted = false;
    private BLEServiceEventListener mBLEEventListener = new BLEServiceEventListener(this);

    public boolean fullRemoteMacAddressIsNeeded = false;
    // The above variable is used when we are dealing with ble files
    // that only contain three bytes. (manufacturing ble files). if
    // this flag is set, upon connection with remote we retrieve the
    // full mac address

    public int mRemoteControlVersion = -1;

    // The following variable stores the name of the iOS device that
    // we have connected to. This variable is usually set by
    // miOSDeviceNameReceiver

    public String miOSDeviceName = null;

    public static final int IOS_REMOTE_STATUS_UNKNOWN = -1;
    public static final int IOS_REMOTE_STATUS_CONNECTED = 2;
    public static final int IOS_REMOTE_STATUS_DISCONNECTED = 0;
    public static final int IOS_REMOTE_STATUS_VIRTUAL = 3;
    public int miOSRemoteStatus = IOS_REMOTE_STATUS_UNKNOWN;



    /** Called when the activity is first created. */
    public void toast(String s)  {
	if (BLEDebugSettings.shouldtoast) {
	    Log.v(TAG,""+BLEDebugSettings.shouldtoast);
	    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
	}
    }

    @Override
    public IBinder onBind (Intent intent)    {
	return binder;
    }

    @Override
    public int onStartCommand(Intent i, int flag, int startId)    {
	BLELog.d(TAG,"Service Started");
	BLELog.d(TAG,"The latest Rib Day = "+ findTheLatestRIB( RIB_PARENT_DIR, true));
	BLELog.d(TAG,"The latest Rib Event = "+ findTheLatestRIB( RIB_PARENT_DIR, false));
	return START_STICKY;
    }

    @Override
    public void onCreate()     {
	BLELog.d(TAG,"Service Created");
        super.onCreate();  
        /***** Initialize BLE API. If this suceeds, you know that you have successfully 
         *     accomplished 2 Tasks:
         *        
         *        1) Native Library  "libRECON-ble.so" has been loaded
         *        2) BLE Device      "/dev/cc2540_ble" has been opened
         *        
         *       If this throws, you get NULL API Reference, because there is 
         *       no need continuing */
	if (DeviceUtils.isLimo()) {
	    try {
		mBLE = ReconBLE.Initialize(); 
	    }
	    catch (Exception ex){
		toast("BLE Could not initialize");
		return; 
	    }
	    int status = mBLE.startUnifiedTask(mBLEEventListener);
	    if (status != ReconBLE.ERR_SUCCESS) {
		toast(String.format("Failed to start BLE Unified Tread [%d]", status) );
		return;
	    }

	    mPersistantStats =
		getSharedPreferences("BLEServicePrefs", Context.MODE_WORLD_WRITEABLE);

	    // Recover the old stat
	    isMasterReadFromPersistanceAtServiceCreate =
		mPersistantStats.getBoolean("IsMaster",true);
	    //HACK
	    isMaster = true;
	    BLELog.d(TAG,"IsMaster is now "+isMaster);
	    // tell we are ok here:
	    BLELog.d(TAG,"Getting ready");
	    resetDevice();
	}
	else {
	    Log.v(TAG,"Not on limo. Start the service in crippled mode");
	    
	}
	
	// Register:
	registerReceiver(mCommandReceiver, new IntentFilter("private_ble_command"));
	registerReceiver(mRIBCommandReceiver, new IntentFilter("PRIVATE_BLE_COMMAND_XML"));
	registerReceiver(mBuddyDummyClassReceiver, new IntentFilter("RECON_FRIENDS_LOCATION_UPDATE"));
	registerReceiver(miOSDeviceNameReceiver,
			 new IntentFilter("com.reconinstruments.IOS_DEVICE_NAME"));
		
	registerReceiver(miOSRemoteStatusReceiver,
			 new IntentFilter("com.reconinstruments.IOS_REMOTE_STATUS"));

	registerReceiver(msgReceiver, new IntentFilter(BTCommon.GEN_MSG));

	

	String ns = Context.NOTIFICATION_SERVICE;
	mNotificationManager = (NotificationManager) getSystemService(ns);

	mBLEEventListener = new BLEServiceEventListener(this);
	
    }
    

    public void broadcastReceivedMessage(String readMessage) {
	// May need to rewrite this
	try {
	    String intentAction = XMLMessage.getMessageIntent(readMessage);
	    Intent myi = new Intent();
	    myi.setAction(intentAction);

	    // MAJOR HACK:
	    // We have old and new stuff mixed together: 
	    // buddy tracking info and music song require HUDConnectivity format
	    // other stuff work based on legacy stuff
	    if (intentAction.contains("RECON_SONG_MESSAGE") ||
		intentAction.contains("RECON_FRIENDS_LOCATION_UPDATE")) {
		Log.v(TAG,"converting to HUDConnectivityMessage");
		HUDConnectivityMessage hcm = new HUDConnectivityMessage();
		hcm.setIntentFilter(intentAction); // otherwise you get null after toByteArray()
		hcm.setData(readMessage.getBytes());
		myi.putExtra("message",hcm.toByteArray());
	    }
	    else {
		myi.putExtra("message", readMessage);//Old legacy code
	    }
	    sendBroadcast(myi);

	    BLELog.d(TAG,"received message! "+readMessage);
	} catch(Exception e){
	    e.printStackTrace();
	}
    }
    public void pairDevice() {
	if (mBLE==null) {
	    return;
	}
	myownmac = mBLE.GetOwnMac();
	BLELog.d(TAG,"Mac Address" + myownmac[0]+myownmac[1]+myownmac[2]+myownmac[3]);
	int status;
	if (isMaster){		// If in master mode
	    byte [] macAddress = readBLEFile();
	    if (macAddress != null) {//These is a BLE.RIB file
		if ((macAddress[3] != 0 && macAddress[4] != 0 && macAddress[5] != 0)
		    || (macAddress[0] == 0 && macAddress[1] == 0 && macAddress[2] == 0
			&& macAddress[3] == 0 && macAddress[4] == 0 && macAddress[5] == 0)) {
		    // We call regular pairing function in two cases:
		    // We have regular mac address or all the bytes
		    // are 0, which usually means that the BLE file
		    // has been corrupted
		    Log.d(TAG,"regular pairing");
		    Log.d(TAG,""+(macAddress[0] +""+  macAddress[1]+""+  macAddress[2]+""+
			       macAddress[3] +""+  macAddress[4] +""+ macAddress[5]));
		    status = mBLE.PairInMasterMode(1, macAddress);
		}
		else {//Manufacturing mac address only: 3 bytes
		    Log.d(TAG,"Manufacturing BLE.RIB");
		    fullRemoteMacAddressIsNeeded = true;
		    status = mBLE.PairInMasterMode(3, macAddress);
		}
	    }
	    else{
		Log.d(TAG,"MacAddress null");
		status = mBLE.PairInMasterMode(0, macAddress);
		}
	}
	else {
	    byte[] irk = loadIrk();
	    if (irk != null){
		status = mBLE.PairInSlaveMode(1, irk);
	    }
	    else {
		status = mBLE.PairInSlaveMode(0, irk);
	    }
	}

	if (status != ReconBLE.ERR_SUCCESS)  {
	    BLELog.d(TAG,String.format("Failed to start Pairing. Status: [%d]", status) );
	    return; 
	}
	toast("Pairing ...");
    }
	
    public synchronized void resetDevice() {
	// hard reset of BLE device. Should not probably be
	// used (or even exposed at all) In these early
	// stages while there are tons of bugs useful so we
	// don't have to restart the app (or use ble_test
	// on the side)
	if (mBLE == null) {
	    return;
	}
	int status = mBLE.ResetDevice();
	if (status == ReconBLE.ERR_SUCCESS) {
	    if (isMaster) {
		setMaster();
		BLELog.d(TAG, "Not calling pairDevice");
	    }
	    else {
		setSlave();
		BLELog.d(TAG, "Not calling pairDevice");
	    }
	}
	if (status != ReconBLE.ERR_SUCCESS) {
	    toast(String.format("Failed to reset device. Status: [%d]", status) );
	    return; 
	}
	toast("Device Reset");
    }
	
    public void unpairDevice() {
	// Demo how to Unpair Connected (paired) device. Not sure when this 
	// will be used in big picture of things; useful for debugging
	if (mBLE == null) {
	    return;
	}
	int status;
	if (isMaster) {
	    status = mBLE.UnpairSlave(0); // FIXME: for now assume it
	    // is remote hence: 0
	}
	else {
	    status = mBLE.UnpairMaster();
	}

	if (status != ReconBLE.ERR_SUCCESS)  {
	    toast(String.format("Failed to unpair device. Status: [%d]", status) );
	    return; 
	}
	toast("Device Unpaired");
    }

    public synchronized void setSlave() {
	if (mBLE == null) {
	    return;
	}
	/**** Set Device to Slave Mode. Completes Sync. Will block a bit
	 *    in Kernel, but this is probably OK. Going further, should
	 *    be moved to async as well */

	BLELog.v(TAG,"resetting counter");
	mDisconnectionCounter = 0;
	// We try to reset the remote in case it is connected
	BLELog.d(TAG,"resetting remote");
	byte [] irk = null;
	// Note per new spec we always reset the remote
	if (irk == null) {
	    BLELog.d(TAG,"Don't have irk fully reset remote");
	}
	int remotestatus = mBLE.ResetRemote(irk);
	try{
	    //do what you want to do before sleeping
	    Thread.currentThread().sleep(500);//sleep for 500 ms
	    //do what you want to do after sleeptig
	}
	catch(Exception ie){
	    //If this thread was intrrupted by nother thread
	}
	int status = mBLE.SetMode(ReconBLE.BLEMode.DEVICE_SLAVE);
	toast("setSlave");
		    
	if (status != ReconBLE.ERR_SUCCESS)  {
	    toast("Failed to set Slave");
	    BLELog.d(TAG,"Failed to set Device to Slave mode. Status: "+ status);
	    return;
	}
	else {
	    isMaster = false;
	    BLELog.d(TAG,"Set To slave mode");
	    toast("Success in set Slave");
	    SharedPreferences.Editor editor = mPersistantStats.edit();
	    editor.putBoolean("IsMaster",isMaster);
	    editor.commit();
	}
    }

    public synchronized void setMaster() {
	if (mBLE==null) {
	    return;
	}
	toast("Set Master");
	/**** Set Device to Slave Mode. Completes Sync. Will block a bit
	 *    in Kernel, but this is probably OK. Going further, should
	 *    be moved to async as well */
	int status = mBLE.SetMode(ReconBLE.BLEMode.DEVICE_MASTER); 
	if (status != ReconBLE.ERR_SUCCESS) {
	    toast("failed in set master");
	    BLELog.d(TAG,"Failed to set Device to Master mode. Status: "+ status);
	    return;
	}
	else {
	    isMaster = true;
	    BLELog.d(TAG,"Set To master mode");
	    toast("Sucess in set master");
	    //Save the change in persistant stats:
	    SharedPreferences.Editor editor = mPersistantStats.edit();
	    editor.putBoolean("IsMaster",isMaster);
	    editor.commit();
	}
	// Reset the disconnection counter:
	mDisconnectionCounter = 0;
    }

    private class BuddyDummyClassReceiver extends BroadcastReceiver {
	@Override
	public void onReceive (Context c, Intent i) {
	    BLELog.d(TAG,"Got buddy tracking shit");
	}
    }

    // On the following array of arraylists: There are four priority
    // lists that contain elements that are pushed BLEUnifiedTask.
    // priority 0 is for file transfer and the corresponding array
    // list stores a list of file names. When this service decides to
    // push the file to BLEUnifiedTask it generates a fileinputstream
    // and pushes it.
    public ArrayList<String>[] mPriorList = (ArrayList<String>[]) new ArrayList[4];
    // private void putFileInQue();
    // private void putXmlInQue();
    public void addToPriorList(String message, int prior) {
	if ((mPriorList[prior])==null) {
	    BLELog.v(TAG,"Array list is empty creating one");
	    mPriorList[prior] = new ArrayList<String>();
	}
	BLELog.v(TAG,"Now adding message to prior list");
	BLELog.d(TAG,"sending "+ message+" Priority is "+prior);
	mPriorList[prior].add(message);
	pushElement(prior);
    }

    public synchronized void pushNothing() {
	//This is a special function telling BLEUnified task that
	//there is nothing from the upper level to be sent.
	if (mBLE==null) return;
	mBLE.mUnifiedTask.upperLevelHasNohtingToSend();
    }
    public synchronized boolean pushElement(int prior) {
	if (mBLE == null) return false;
	int sendResult = 10;
	BLELog.v(TAG,"Trying to push element");
	try {
	    long numMillisecondsToSleep = 1; // 1 milisecond
	    Thread.sleep(numMillisecondsToSleep);
	} catch (InterruptedException e) {
	}
	// Make sure that the list is not empty
	if (!(mPriorList[prior] != null && !mPriorList[prior].isEmpty())) {
	    Log.w(TAG,"No element or no list exists. Returning from function");
	    return false;
	}
	if (prior==0) {		// It is a ifle
	    BLELog.d(TAG,"Pushing with priority 0");
	    try {
		String thePath = mPriorList[prior].get(0);
		if (thePath == null) {
		    return false;
		}
		File theFile = new File(thePath);
		if (theFile == null) {
		    return false;
		}
		FileInputStream fis = new FileInputStream (mPriorList[prior].get(0));
		// FIXME: Risky
		String metaFileName = RIB_PARENT_DIR+"tmpFile.txt";
		FileOutputStream output = new FileOutputStream (metaFileName);
		String filename = mPriorList[prior].get(0);
		String fileSize = Long.toString((new File(filename)).length());
		byte[] filenamebytes = filename.getBytes();
		byte[] fileSizebytes = fileSize.getBytes();
		byte[] zero = new byte[1];
		zero[0] = 0;
		output.write(filenamebytes,0,filenamebytes.length);
		output.write(zero,0,1);
		output.write(fileSizebytes,0,fileSizebytes.length);
		output.write(zero,0,1);
		output.close();
		FileInputStream  filemetadata = new FileInputStream (metaFileName);
		BLELog.d(TAG,filemetadata.available()+" bytes available from meta data");
		BLELog.d(TAG,fis.available()+" bytes available from actual file");
		SequenceInputStream mfis = new  SequenceInputStream(filemetadata, fis);
		BLELog.d(TAG,mfis.available()+" Bytes availabe for the entire file");
		sendResult = mBLE.mUnifiedTask.InsertToTheQue(fis,prior);
	    }
	    catch (IOException e)  {
		Log.w(TAG, "Can't generated input stream " + e.getMessage() + " but we don't really care");
	    } 
	}
	else if (prior ==1 || prior ==2) {// It is an XML
	    byte [] buffer = mPriorList[prior].get(0).getBytes();
	    try {
		if (buffer != null) {
		    ByteArrayInputStream bais =   new ByteArrayInputStream (buffer);
		    sendResult = mBLE.mUnifiedTask.InsertToTheQue(bais,prior);
		}
	    }
	    catch (Exception e) {
		Log.w(TAG, "Can't generate bytearray inputstream. reason: " + e.getMessage());	    
	    }
	}
	else {//TODO: prior 3 goes here
	    byte [] buffer = Base64.decode(mPriorList[prior].get(0),Base64.DEFAULT);
	    try {
		if (buffer != null) {
		    ByteArrayInputStream bais =   new ByteArrayInputStream (buffer);
		    sendResult = mBLE.mUnifiedTask.InsertToTheQue(bais,prior);
		}
	    }
	    catch (Exception e) {
		Log.w(TAG, "Can't generate bytearray inputstream. reason: " + e.getMessage());	    
	    }
	}
	BLELog.v(TAG,"sendResult = "+sendResult);
	if (sendResult == ReconBLE.ERR_SUCCESS) {
	    BLELog.v(TAG,"Successfully pushed the file so removing it from backlog");
	    mPriorList[prior].remove(0);
	}
	return (sendResult == ReconBLE.ERR_SUCCESS);

    }

    static public byte[] readBLEFile(){
	BLELog.d(TAG,"readBLEFile");
	File srcFile = new File(BLE_FILE_LOC);
	if (!srcFile.exists()) {
	    return null;
	}
	InputStream inTape = null;
	byte[] result = null;
	try {
	    inTape = new FileInputStream(srcFile);
	    result  = readTape(MAC_ADDRESS_SIZE, inTape);
	    inTape.close();
	    return result;
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return null;
    }

    public static void writeToBLEFile(byte[] the_bytes) {
	File srcFile = new File(BLE_FILE_LOC);
	OutputStream os = null;

	try {
	    os = new FileOutputStream(srcFile);
	    os.write(the_bytes);
	    os.close();
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }


    // Helper function for  reading BLE file
    static private byte[] readTape(int numBytes, InputStream inTape)
	throws FileNotFoundException, IOException {
	BLELog.d(TAG,"readTape");
	byte[] buf = new byte[numBytes];
	try {
	    int size;
	    size = inTape.read(buf);
	    if (size == numBytes) {
		return buf;
	    }
	} finally {

	}
	return null;
    }

    /**
     * The IBLEService is defined through IDL (aidl)
     */
    private final IBLEService.Stub binder = new IBLEService.Stub() {
	    public int getTemperature() {
		return (int) mTemperature;
	    }

	    public boolean getIsMaster() {
		return isMaster;
	    }
	    
	    public boolean getIsMasterBeforeonCreate() {
		return isMasterReadFromPersistanceAtServiceCreate;
	    }

	    public boolean isConnected() {
		return !(mStatus == ReconBLE.BLEStatus.STATUS_DISCONNECTED ||
			 mStatus == ReconBLE.BLEStatus.STATUS_UNPAIR ||
			 mStatus == ReconBLE.BLEStatus.STATUS_IRK_READY ||
			 mStatus == ReconBLE.BLEStatus.STATUS_MAC_READY ||
			 mStatus == ReconBLE.BLEStatus.STATUS_IDLE);
	    }
	    public byte[] getOwnMacAddress() {
		Log.v(TAG,"get Own mac and and ");
		return  myownmac;
	    }
	    public int getRemoteControlVersionNumber() {
		return  mRemoteControlVersion;
	    }

	    public boolean hasEverConnectedAsSlave() {
		// Check to see if you have an irk
		String irkString = (mPersistantStats.getString("Irk",null));
		return (irkString != null);
	    }
	    public void setInMusicApp(boolean flag) {
		mInMusicApp = flag;
	    }
	    public boolean getInMusicApp() {
		return mInMusicApp;
	    }
	    public boolean ifCanSendXml() {
		return xmlCanCome;
	    }
	    public boolean ifCanSendMusicXml() {
		boolean goodPriorlist = false;
		if (mPriorList[1] == null) {
		    goodPriorlist = true;
		} else if (mPriorList[1].isEmpty()) {
		    goodPriorlist = true;
		}
		Log.d(TAG, "ifCanSendMusicXml() xmlCanCome: "+xmlCanCome+" in music app: "+mInMusicApp+" goodPriorList: "+goodPriorlist);
		return (xmlCanCome && mInMusicApp && goodPriorlist);
	    }
	    public int incrementFailedPushCounter() {
		if (xmlCanCome) {
		    mFailedpush++;
		}
		Log.d(TAG, "mFailedPush "+mFailedpush);
		clearXmlQueIfNecessary();
		return mFailedpush;
	    }

	    public int sendControlByte(byte ctrl) {
		// The values are: 
                // MUSIC_NEXT_TRACK = 0x01;	
		// MUSIC_PREVIOUS_TRACK = 0x02;	
		// MUSIC_VOLUME_UP = 0x03;	
		// MUSIC_VOLUME_DOWN = 0x04;	
		// MUSIC_TOGGLE_PLAY_PAUSE = 0x05;
		// REPORT_REMOTE_STATIS = 0x11;
		// IRK_RECEIVED = 0x12; //It means user paired
		if (mBLE != null) {
		    return mBLE.SendControlByte(ctrl);
		}
		else {
		    return ReconBLE.ERR_FAIL;
		}
	    }

	    public int pushIncrementalRib(String bytesString) {
		Log.v(TAG,"pushIncrementalRib");
		if (canSendIncrementalRib) {
		    addToPriorList(bytesString,3);
		    return 0;	
		} else {
		    return -1;	
		}
	    }

	    public int pushXml(String xmlString) {
		if (isConnected() && !isMaster) {
		    addToPriorList(xmlString,1);
		    return 0;
		} else {
		    Log.v(TAG,"can't push Xml, not Connected or master");
		    return -1;
		}
		
	    }
	    //	    public int otherIncrementalRib(){
	    // 	Log.v(TAG,"otherIncrementalRib");
	    // 	return 1;
	    // }

	    public String getiOSDeviceName() {
		Log.v(TAG,"getiOSDeviceName");
		return miOSDeviceName;
	    }
	    public int getiOSRemoteStatus() {
		// Note: This function does not check to see if it
		// logically makes sense to call it or if we are
		// master or anything. It just exposes the value, so
		// it should be considered the last known state of
		// miOSRemoteStatus. It is up to the API user to
		// manage that.
		Log.v(TAG,"getiOSRemoteStatus");
		return miOSRemoteStatus;
	    }

	};


    // Notification stuff;
    // TODO: put counter shit so tha tdoes not go to red all the time
    public static String findTheLatestRIB( String dirName,boolean isDay) {
	File dir = new File(dirName);
	FilenameFilter theFilenameFilterDAY = new FilenameFilter() { 
		public boolean accept(File dir, String filename) {
		    return (filename.endsWith(".RIB") && filename.contains("DAY"));
		}
	    };
	FilenameFilter theFilenameFilterEVENT = new FilenameFilter() { 
		public boolean accept(File dir, String filename) {
		    return (filename.endsWith(".RIB") && filename.contains("EVENT"));
		}
	    };

	File[] files = null;
	if (isDay) {
	    if (dir.exists()&& dir.list().length>0) {
		files =  dir.listFiles(theFilenameFilterDAY);
	    }
	}
	else {
	    if (dir.exists()&&dir.list().length>0) {
		files =  dir.listFiles(theFilenameFilterEVENT);
	    }
	}
	// So far the list of files.
	// Now find the latest file:
	long latestmodified = 0;
	int goodi = -1;
	if (files != null) {
	    for (int i = 0; i< files.length;i++) {
		long lastmodified = files[i].lastModified();
		if (lastmodified > latestmodified) {
		    latestmodified = lastmodified;
		    goodi = i;
		}
	    }
	}
	String theLatestRIBFile = null ;
	if (goodi >=0) {
	    theLatestRIBFile  = files[goodi].getAbsolutePath();
	}
	return theLatestRIBFile;
    }
    
    public void generateGoodFile(String inputfile) {
	try {
	    File f1 = new File(inputfile);
	    InputStream in = new FileInputStream(f1);
	    byte[] buf = new byte[1024];
	    // First we extract the file name and size
	    int len;
	    len = in.read(buf);
	    int i;
	    int nameend = -1;
	    int sizeend = -1;
	    for (i=0; i<len;i++) {
		if (buf[i] == 0) {
		    if (nameend == -1) {
			nameend = i;
		    }
		    else  {		// sizseend == -1
			sizeend = i;
			break;
		    }
		}
	    }
	    BLELog.d(TAG,"File name ends at "+nameend);
	    BLELog.d(TAG,"File sizse ends at "+sizeend);
	    byte[] namebytes = new byte[nameend];
	    System.arraycopy(buf, 0 , namebytes, 0, nameend);
	    String trueFileName = new String(namebytes);
	    BLELog.d(TAG,"True file name is "+trueFileName);
	    File f2 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+trueFileName);
	    OutputStream out = new FileOutputStream(f2, false); // no append
	    out.write(buf,sizeend+1,len-(sizeend+1));
      
	    while ((len = in.read(buf)) > 0) {
		out.write(buf, 0, len);
	    }
	    in.close();
	    out.close();
	    System.out.println("File copied.");
	    // Notify the apps about the received file 
	    Intent fileTransferComplete = new Intent();
	    fileTransferComplete.setAction(BTCommon.MSG_FILE_FINISH);
	    fileTransferComplete.putExtra("error", "SAVED");
	    FilePath path = new FilePath(trueFileName,PathType.STORAGE);
	    fileTransferComplete.putExtra("savedFile", path);
	    sendBroadcast(fileTransferComplete);
	    
	    
	} catch (Exception ex) {
	    System.out.println(ex);
	}
    }



    public synchronized void clearXmlQueIfNecessary() {
	if (mBLE==null) return;
	if (mFailedpush > BLETestService.MAX_FAIL_PUSH) {
	    mBLE.mUnifiedTask.NotifyStopSend(1);
	    //HACK
	    try{
		//do what you want to do before sleeping
		Thread.currentThread().sleep(500);//sleep for 500 ms
		//do what you want to do after sleeptig
	    }
	    catch(Exception ie){
		//If this thread was intrrupted by nother thread
	    }
	    
	    mBLE.FlushBuffers();
	    mBLE.TelliOSToClear();
	    //HackHack
	    //mPriorList[1] = new ArrayList<String>();
	    //endhackhack
	    //endhack
	    mFailedpush = 0;
	}
    }

    public  void resetFailedPushCounter() {
	mFailedpush = 0;
    }

    private byte[] loadIrk() {
	String irkString = (mPersistantStats.getString("Irk",null));
	byte [] irk= null;
	if (irkString != null){
	    irk = Base64.decode(irkString,0);
	}
	return irk;
    }
}



