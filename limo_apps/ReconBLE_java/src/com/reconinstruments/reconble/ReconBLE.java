package com.reconinstruments.reconble;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import android.util.Log;
import android.os.Environment;

/* This is Recon BLE API class
 *   Java client uses services of this class (and this class only) in order to use BLE functionality
 *   Internal implementation (Native/JNI, Async Threads, etc) are shielded from the client who is only
 *   notified of async BLE events -- see "IReconBLEEventListener" interface
 *   
 *   -- Native library is in libs/armeabi/libRECON-ble.so
 *   --  Android will unpack it from APK to /data/data/<app namespace>/lib/libRECON-ble.so
 *       We can also push it to /system/lib/, and pack ReconBLE as JAR file.
 *   
 * */
public class ReconBLE
{
    private static final String TAG = ReconBLE.class.getSimpleName();
	 
    // error codes
    public  static final int    ERR_SUCCESS     =  0;         // generic success
    public  static final int    ERR_FAIL        = -0x01;      // generic failure
	 
    public  static final int    ERR_NO_SESSION  = -0x02;      // no BLE session (shared libary failed, device failure, etc)
    public  static final int    ERR_SLAVE       = -0x03;      // could not put device to slave mode
    public  static final int    ERR_MASTER      = -0x04;      // could not put device to master mode
    public  static final int    ERR_PAIR        = -0x05;      // master/slave pair status error
    public  static final int    ERR_IN_PROGRESS = -0x06;      // async task already in progress
    public  static final int    ERR_WRITE       = -0x07;      // Tx - Write error
    public  static final int    ERR_READ        = -0x08;      // Tx - Read  error
	
    // device mode enumeration
    public enum BLEMode
    {
	DEVICE_UNKNOWN,
	    DEVICE_MASTER,
	    DEVICE_SLAVE
	    }
	 
    // state machine status values: Maps to values from cc2540_ble.h
    public class BLEStatus
    {
	public static final  int    STATUS_INIT          = 0;       // BLE_INIT
	public static final  int    STATUS_IDLE          = 1;       // BLE_IDLE
	public static final  int    STATUS_DISCONNECTED  = 2;       // BLE_DISCONNECT
	public static final  int    STATUS_CONNECTED     = 3;       // BLE_CONNECT
	public static final  int    STATUS_FAIL_ACTIVE   = 5;       // BLE_FAIL_ACTIVE
	public static final  int    STATUS_SPI_FAIL      = 6;       // BLE_SPI_FAIL
	public static final  int    STATUS_REMOTE_LIST   = 10;      // REMOTE_LIST_READY
	public static final  int    STATUS_UNPAIR        = 11;      // BLE_UNPAIR
	public static final  int    STATUS_CANCEL_BOND   = 12;       // BLE_CANCEL_BOND
	public static final  int    STATUS_RECEIVE_NOEOR = 15;      // BLE_READY_NOEOR
	public static final  int    STATUS_RECEIVE_EOR   = 16;      // BLE_EOR
	public static final  int    STATUS_SWITCH_TO_TX  = 17;      // BLE_SWITH_TO_TX
	public static final  int    STATUS_SWITCH_RX_SHIP= 18;      // BLE_SWITCH_RX_SHIP
	public static final  int    STATUS_SEND_NOEOT    = 20;      // BLE_SEND_NOEOT
	public static final  int    STATUS_SEND_EOT      = 21;      // BLE_SEND_EOT
	public static final  int    STATUS_RECEIVE_START = 22;      // BLE_READY_RECEIVE
	public static final  int    STATUS_RECEIVE_START_NR = 23;   // BLE_NOTREADY_RECEIVE
	public static final  int    STATUS_MAC_READY = 24;          // BLE_MAC_READY
	public static final  int    STATUS_IRK_READY = 25;      // BLE_IRK_READY
	public static final  int    STATUS_MISC_DATA_READY = 26;      // BLE_MISC_DATA_READY
	
		
		
	public static final  int    STATUS_READY_SEND    = 28;    // IP_READY_RECEIVE. Used
								  // to
								  // be
								  // IP_READY_ACK
	public static final int STATUS_IP_NACK = 29;
	public static final int STATUS_NEW_SHIP_READY = 30;
	public static final int STATUS_IP_REFUSE_RECEIVE = 31;


	// FIXME: This obsolete, keep it for now. Fix it later
	public static final int STATUS_SLAVE_MODE = 1000;
	public static final int STATUS_MASTER_MODE = 1000;

    }
	 
	
    // native so file name
    private static final String SHARED_LIB = "RECON-ble";    // "libRECON-ble.so"
	 
    // polling/timeout constants, until we switch to IRQ based implementation
    protected static final int PAIR_POLL_TMO          = 500;          // polls each half a second for paired status
	 
    protected static final int TXN_ACK_POLL_TMO       = 100;          // polls each 100 ms for ACK to start Send
    protected static final int TXN_SENT_POLL_TMO      = 200;          // polls each 200 ms for ACK between Sent data chunks
	 
    protected static final int RECEIVE_START_POLL_TMO = 1000;         // polls each second for Start of Incoming I/O
    protected static final int RECEIVE_PROGRESS_TMO   = 200;          // polls each 200 ms between Rx chunks
	 
    // Pairing constants
    protected static final String BLE_PATH   = Environment.getExternalStorageDirectory().getAbsolutePath()+"/BLE.RIB"; 
    protected static final int    MAC_ADDRESS_SIZE = 6;            // Fixed size MAC address -- 6 bytes
    protected static final int    IRK_SIZE = 16;           // Fixed size IRK address -- 6 bytes

    protected static final int    MAC_REMOTE_LIST_NUM = 4;         // max number of entries in mac remote address list

    // FIXME: Don't know where shoudl put this
    protected static final int MAX_MISC_DATA_SIZE = 60; // max size of misc data
	 
    // IO constants
    protected static final int BLE_BLOCK_SIZE = 57;
    protected static final int BLOCK_NUM_MAX  = 18;
    protected static final int BUFFER_IO_SIZE = ReconBLE.BLE_BLOCK_SIZE * ReconBLE.BLOCK_NUM_MAX; 
	 
    // session context pointer. Can be anything -- i.e. native side class. Initially
    // this is only open handle to /dev/ccble_2540 device
    private  int m_context = 0;
	 
    // MAC address we are currently pairing (or being paired) with
    byte []  macAddress = new byte[MAC_ADDRESS_SIZE];
	 
    /* Singleton Pattern */
    private ReconBLE () {;}
    private static ReconBLE mInstance = null;
	 
    // Async Tasks: Connection, Tx, Rx
    public BLEUnifiedTask      mUnifiedTask = null;
    private BLEPairTask         mPairTask    = null;
    private BLESendTask         mSendTask    = null;
    private BLEReceiveTask      mReceiveTask = null;
	 
    // notify handlers used internally to trigger main thread callbacks during Async events
    // TODO: Names are a bit non-intuitive. Rx/Tx are mutually exclusive, but Pair (Connection Monitor)
    //       can run in paralel with either Rx or Tx
    private BLEMessageHandler   mNotifyHandler = new BLEMessageHandler ();   // pairing / tx; mutually exclusive
    private BLEMessageHandler   mRxHandler     = new BLEMessageHandler ();   // rx: Runs all the time potentially
	 
    // **** PUBLIC INTERFACE ****
	 
    /* Constructor -- Singleton Pattern */
    public  static ReconBLE Initialize () throws UnsatisfiedLinkError, IOException
    {
	if (ReconBLE.mInstance == null)
	    {
		// load native shared library. This will automatically throw if libRECON-native.so can not be found
		System.loadLibrary(ReconBLE.SHARED_LIB);
			 
		// now allocate us
		ReconBLE.mInstance = new ReconBLE();
			 
		// open the device file
		ReconBLE.mInstance.m_context = BLENative.openDevice();
		if (ReconBLE.mInstance.m_context == 0)
		    {
			ReconBLE.mInstance = null;
			throw new IOException ("Failed to Establish BLE Session");
		    }
	    }
		 
	return mInstance;
    }

    public int StartReceive(){
	return BLENative.startReceive(m_context, true);
    }

    public int StartReceive_noAck(){
	return BLENative.startReceive(m_context, false);
    }
	 

	 
    /* board reset; should be used sparingly */
    public int    ResetDevice ()
    {
	// context check
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;   // no BLE session
	return BLENative.resetDevice(m_context);
    }

    public void FlushBuffers() {
	BLENative.flushBuffers(m_context);
    }

	 
    /* sets device to Master/Slave mode. Completes synchronously */
    public int    SetMode (ReconBLE.BLEMode mode)
    {
	// context check
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;   // no BLE session
		 
	int status = ReconBLE.ERR_SUCCESS;
		 
	if (mode == ReconBLE.BLEMode.DEVICE_MASTER)
	    status = BLENative.setMaster(m_context);
		 
	else if (mode == ReconBLE.BLEMode.DEVICE_SLAVE)
	    status = BLENative.setSlave(m_context);
		 
	else  // handle it by reseting the board
	    status = BLENative.resetDevice(m_context);
		 
	return status;
		 
    }
	 
    /* queries current mode (Master/Slave)
       Implementation is not the cleanest way, but didn't want to carry Enum across JNI because
       it is quite anal how JNI handles it (class marshalling, etc) */

    public ReconBLE.BLEMode GetMode ()
    {
	// Ali: FIXME: current implementation is obsolete
	// context check
	ReconBLE.BLEMode mode = ReconBLE.BLEMode.DEVICE_UNKNOWN;
	if (m_context == 0) return mode;
		 
	int status = BLENative.getMode(m_context);
	if (status == ReconBLE.BLEStatus.STATUS_MASTER_MODE)
	    mode = ReconBLE.BLEMode.DEVICE_MASTER;  
		 
	else if (status == ReconBLE.BLEStatus.STATUS_SLAVE_MODE)
	    mode = ReconBLE.BLEMode.DEVICE_SLAVE;
		 
	return mode;
    }
	 


    public int PairInSlaveMode(int bondStatus, byte[] irk)
    {
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;   // no BLE session 
	int stat = BLENative.pairWithMaster(m_context, bondStatus, irk);
	// tell monitoring task what we are working with
	if (stat == ReconBLE.ERR_SUCCESS)
	    {
		if (mPairTask != null) 
		    mPairTask.setMACAddress(irk);
	    }
		 
	return stat;
    }

    public int PairInMasterMode (int bondStatus, byte[] mac)
    {
	Log.d(TAG,"PairInMasterMode");
	Log.d(TAG,"bond Status" + bondStatus);
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;   // no BLE session 
	int stat = BLENative.pairWithSlave(m_context, bondStatus, mac);
	// tell monitoring task what we are working with
	if (stat == ReconBLE.ERR_SUCCESS)
	    {
		if (mPairTask != null) 
		    mPairTask.setMACAddress(mac);
	    }
	return stat;
    }


    /* retrieves remote mac address list. completes sync */
    public  int   GetRemoteList(LinkedList<byte []> lstMAC)
    {
	// context check
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;   // no BLE session
		 
	byte [] masters = new byte[ReconBLE.MAC_ADDRESS_SIZE * ReconBLE.MAC_REMOTE_LIST_NUM + 1];
	int status = BLENative.getMasterList(m_context, masters);
	if (status == ReconBLE.ERR_SUCCESS)
	    {
		// size is 1st byte
		int numMasters = masters[0]/ReconBLE.MAC_ADDRESS_SIZE;
		for (int i = 0; i < numMasters; i++)
		    {
			byte [] macAddress = new byte[ReconBLE.MAC_ADDRESS_SIZE];
				 
			System.arraycopy(masters, 1 + i * ReconBLE.MAC_ADDRESS_SIZE, 
					 macAddress, 0, ReconBLE.MAC_ADDRESS_SIZE);
				 
			lstMAC.addLast(macAddress);
		    }
	    }
	return status;
    }

    public  int   GetRemoteList(byte [] masters)
    {
	// context check
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;   // no BLE session
		 
	int status = BLENative.getMasterList(m_context,masters);
	return status;
    }


    /* retrieves irk list. completes sync */
    public  int   GetIrk(LinkedList<byte []> lstIrk)
    {
	// context check
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;   // no BLE session
		 
	byte [] irk = new byte[IRK_SIZE]; // FIXME: 16 - > constant in BLE.h
	int status = BLENative.getIrk(m_context, irk);
	if (status == ReconBLE.ERR_SUCCESS)
	    {
		lstIrk.addLast(irk);

	    }
	return status;
    }

    public  int   GetMiscData(LinkedList<byte []> lstMiscData)
    {
	// context check
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;   // no BLE session
		 
	byte [] misc_data = new byte[MAX_MISC_DATA_SIZE]; // FIXME: 16 - > constant in BLE.h
	int status = BLENative.getMiscData(m_context, misc_data);
	if (status == ReconBLE.ERR_SUCCESS)
	    {
		lstMiscData.addLast(misc_data);

	    }
	return status;
    }



    /* unpairs currently paired master */
    public  int   UnpairMaster ()
    {
	// context check
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;   // no BLE session
	int status = BLENative.unpairMaster(m_context);
	if (status == ReconBLE.ERR_SUCCESS)
	    {
		// FIXME: Some modifications have to be done here
		// remove BLE.RIB as well
		File rib = new File(ReconBLE.BLE_PATH);
		if (rib.exists() == true)
		    rib.delete();
	    }
         
	return status;
    }


    /* unpairs currently paired slave with salve id being arg: one of
     * 0 1 2; 0 is for remote */
    public int UnpairSlave (int arg)
    {
	// context check
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;   // no BLE session
	int status = BLENative.unpairSlave(m_context, arg);
	if (status == ReconBLE.ERR_SUCCESS)
	    {
		// remove BLE.RIB as well
		File rib = new File(ReconBLE.BLE_PATH);
		if (rib.exists() == true)
		    rib.delete();
	    }
         
	return status;
    }


    
    /* Connection Monitoring Task Management. This Task can run independently of Rx/Tx; it notifies
       the client of Pair status changes. It can be started/stopped at any time & has no dependencies */

    public int startUnifiedTask(IReconBLEEventListener cbk)
    {

	Log.d(TAG,"startUnifiedTask");
	// if the task is already active, just return
	if (mUnifiedTask != null && mUnifiedTask.isAlive() == true) return ReconBLE.ERR_IN_PROGRESS;
	// (re)start thread that will keep polling until we get valid BLE session
	mNotifyHandler.mCbk = cbk;
		 
	mUnifiedTask = new BLEUnifiedTask();
		 
	mUnifiedTask.mNotifyHandler = mNotifyHandler;
	mUnifiedTask.mSession       = m_context;
	try
	    {
		mUnifiedTask.start();
	    }
	catch (Exception ex)
	    {
		Log.e(TAG, "Fatal Error: Could Not start Unified monitoring Task ");
		return ReconBLE.ERR_FAIL;
	    }

	Log.d(TAG,"Unified Task Started");

         
	return ReconBLE.ERR_SUCCESS;

    }

    // public int stoptUnifiedTask(IReconBLEEventListener cbk)
    // {
    // 	mUnifiedTask.NotifyExit();
    // }



    public int SetSendPriority(int prio) {
	Log.d(TAG,"SetSendPriority " + prio );
	return BLENative.setSendPriority(m_context,prio);
    }

    public int GetCurrentSendPriority() {
	Log.d(TAG,"GetCurrenSendtPriority ");
	//  bits 7-4 is old prior
	//  bits 3-0 is current prior
	return BLENative.getCurrentSendPriority(m_context)&0x0F;
    }


    public int GetCurrentReceivePriority() {
	Log.d(TAG,"GetCurrenSendtPriority ");
	//  bits 7-4 is old prior
	//  bits 3-0 is current prior
	return (BLENative.getCurrentReceivePriority(m_context))&0x0F;
    }


    public int GetPreviousSendPriority() {
	Log.d(TAG,"GetCurrenSendtPriority ");
	//  bits 7-4 is old prior
	//  bits 3-0 is current prior
	return (BLENative.getCurrentSendPriority(m_context)&0xF0)>>4;
    }

    public int GetPrevioustReceivePriority() {
	Log.d(TAG,"GetCurrenSendtPriority ");
	//  bits 7-4 is old prior
	//  bits 3-0 is current prior
	return (BLENative.getCurrentReceivePriority(m_context)&0xF0)>>4;
    }

    public byte[] GetOwnMac() {
	Log.d(TAG, "GetOwnMac");
	byte[] ownMac = new byte[6];
	BLENative.getOwnMac(m_context,ownMac);
	Log.d(TAG,"ownMac"+ownMac[0]+ownMac[1]);
	return ownMac;
    }

    public byte[] GetRemoteMac(int needVersion) {
	Log.d(TAG, "GetRemoteMac");
	byte[] remoteMac = new byte[6+needVersion];
	BLENative.getRemoteMac(m_context,remoteMac,needVersion); // 0 means no version needed
	return remoteMac;
    }


    public int SendControlByte(byte ctrl) {
	return BLENative.sendControlByte(m_context,ctrl);
    }

    public int SendSpecialCommand(byte theCommand) {
	return BLENative.sendSpecialCommand(m_context,theCommand);
    }


    public int    StartPairMonitor (IReconBLEEventListener cbk)
    {
	// if the task is already active, just return
	if (mPairTask != null && mPairTask.isAlive() == true) return ReconBLE.ERR_IN_PROGRESS;
		 
	// (re)start thread that will keep polling until we get valid BLE session
	mNotifyHandler.mCbk = cbk;
		 
	mPairTask = new BLEPairTask();
		 
	mPairTask.mNotifyHandler = mNotifyHandler;
	mPairTask.mSession       = m_context;
	mPairTask.setMACAddress(macAddress);
         
	try
	    {
		mPairTask.start();
	    }
	catch (Exception ex)
	    {
		Log.e(TAG, "Fatal Error: Could Not start Pair monitoring Task ");
		return ReconBLE.ERR_FAIL;
	    }
         
	return ReconBLE.ERR_SUCCESS;
    }

    public  void  StopPairMonitor ()
    {
	if (mPairTask != null)
	    {
		mPairTask.NotifyExit();   // simply signal pair task
		mPairTask = null;
	    }
    }
	 
    /* Incoming Transmission Task Management. It can be started only once
       connection is established (status check performed). If not running, this effectively
       shuts down the Receiver & Incoming Byte stream will never be pulled from BLE chip
	
       Client will typically manage (start/stop) Rx task based on received callbacks
       from PairMonitor Task
	
       Note the Client Provided Buffer ("ByteArrayOutputStream"). During "tick" status
       updates it will always contain currently received byte stream. On completion
       it will contain full Rx-ed message */
    public int    StartReceiving (ByteArrayOutputStream strm, IReconBLEEventListener cbk)
    {
	Log.d(TAG,"StartReceiving()");
	return this.startRxMonitor(cbk, strm);
    }
	
    public void   StopReceiving ()
    {
	if (mReceiveTask != null)
	    mReceiveTask.NotifyExit();
    }
	 
    /* Tx API set: Buffer, String or File. They all sync to Tx Task with configured
       Input Byte Stream. It runs only while transaction is in progress & will automatically
       shut down when finished. This is fundamently different from Pair/Rx Tasks which require
       Client start/stop management
	 
       It can be started only if there is valid connection (status check performed) */
    public  int   SendBuffer (byte [] buffer, IReconBLEEventListener cbk)
    {
	// spawn async tx task
	return this.startTxMonitor(cbk, buffer, null);
    }
	 
    // Obsolete
    public int SendTestString(IReconBLEEventListener cbk)
    {
	//	return mUnifiedTask.NotifySend(null);
	return -1;
    }

    public  int   SendString (String strData, IReconBLEEventListener cbk)
    {
	Log.d(TAG,"SendString()");
	byte [] buffer = strData.getBytes();
	return this.startTxMonitor(cbk, buffer, null );
    }
	 
    public  int   SendFile   (String strPath, IReconBLEEventListener cbk)
    {
	// validate path
	File file = new File(strPath);
	if ( (!file.exists() ) || (!file.isFile() ) )
	    {
		Log.e(TAG, String.format("Passed [%s] is not valid File", strPath) );
		return ReconBLE.ERR_FAIL;
	    }
		 
	return this.startTxMonitor (cbk, null, strPath);
		 
    }

    // cancel send in progress
    public void   StopSend ()
    {
	if (mSendTask != null)
	    mSendTask.NotifyExit();
    }


    public int RequestSecurity ()
    {
	Log.d("ReconBLE", "RequestSecurity");
	return BLENative.slaveRequestSecurity(m_context);
    }

    public int ResetRemote (byte[] irk)
    {
	Log.d("ReconBLE", "RequestSecurity");
	return BLENative.resetRemote(m_context, irk);
    }

    public int TelliOSToClear()
    {
	Log.d("ReconBLE", "TelliOSToClear");
	return BLENative.telliOSToClear(m_context);

    }
	 
    // **** END PUBLIC INTERFACE ****
	 
    // internal helpers that spawn Tx/Rx threads
    private int  startTxMonitor   (IReconBLEEventListener cbk, byte [] dataBuffer, String strPath)
    {
	Log.d(TAG,"startTxMonitor()");
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;         // no BLE session 
		 
	// check if we are paired. This completes sync
	if (BLENative.checkStatus(m_context,0) !=  ReconBLE.BLEStatus.STATUS_CONNECTED)
	    return ReconBLE.ERR_PAIR; 

	Log.d(TAG,"startTxMonitor: We are paired... continue");
	// try to allocate input stream
	InputStream input = null;
	try 
	    {
		if (strPath != null)
		    input = new FileInputStream (strPath);
		else
		    input = new ByteArrayInputStream (dataBuffer);
	    } 
		 
	catch (Exception e)
	    { 
		Log.e(TAG, "Could not access Input Data: " + e.getMessage() );
		return ReconBLE.ERR_FAIL;
	    }
		   
	// (re)start thread that will keep sending and reporting progress until data is transmitted
	mNotifyHandler.mCbk = cbk;
		 
	mSendTask = new BLESendTask();
		 
	mSendTask.mNotifyHandler   = mNotifyHandler;
	mSendTask.mSession         = m_context;
         
	// set data input stream
	mSendTask.m_input    = input;
         
	try
	    {
		mSendTask.start();
	    }
	catch (Exception ex)
	    {
		Log.e(TAG, "Fatal Error: Could Not start Tx monitoring Task: " + ex.getMessage() );
		return ReconBLE.ERR_FAIL;
	    }
         
	return ReconBLE.ERR_SUCCESS;
    }

    private int  startRxMonitor   (IReconBLEEventListener cbk, ByteArrayOutputStream strm)
    {
	Log.d(TAG,"startRxMonitor()");
	if (m_context == 0) return ReconBLE.ERR_NO_SESSION;         // no BLE session 
	Log.d(TAG,"startRxMonitor(): There is a session");
	// check if we are paired. This completes sync
	if (BLENative.checkStatus(m_context,0) !=  ReconBLE.BLEStatus.STATUS_CONNECTED)
	    {
		Log.d(TAG, "startRxMonitor(): BLE Not connected");
		return ReconBLE.ERR_PAIR; }

	Log.d(TAG, "startRxMonitor(): There si a connection");
	// (re)start thread that will keep listening and receiving
	mRxHandler.mCbk = cbk;
		 
	mReceiveTask = new BLEReceiveTask();
		 
	mReceiveTask.mNotifyHandler   = mRxHandler;
	mReceiveTask.mSession         = m_context;
         
	// set received data output stream
	mReceiveTask.mStream          = strm;
         
	try
	    {
		mReceiveTask.start();
	    }
	catch (Exception ex)
	    {
		Log.e(TAG, "Fatal Error: Could Not start Rx monitoring Task: " + ex.getMessage() );
		return ReconBLE.ERR_FAIL;
	    }
	return ReconBLE.ERR_SUCCESS;
    }


}


