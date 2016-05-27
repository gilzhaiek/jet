package com.reconinstruments.reconble;
import java.io.IOException;
import java.io.InputStream;
import android.os.Message;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.os.Environment;
public class BLEUnifiedTask extends BLETask {
    private static final String TAG = "BLEUnifiedTask";
    private static final int NUM_PRIORITIES = 4;
    private static final int MISC_DATA_SIZE = 60;
    private static final int FULL_BLE_STATUS_LENGTH = 6;
    private int mAdaptiveWaitTime  = 1000;
    private int mLastSendStatus;
    private int mLastdReceivedStatus;
    private int mNextSendStep;
    private int mLastOperationResult;
    private int mStatus;
    private byte[] mFullStatus = new byte[FULL_BLE_STATUS_LENGTH];
    private Bundle b = new Bundle();
    private final int TX_MODE = 2;
    private final int RX_MODE = 1;
    private final int NO_TX_RX_MODE = 0;

    private static final int SPI_FAIL = 0x2000;
    private static final int EOR_BIT_REPEAT = 0x1000;
    private static final int TX_RETRY = 0x800;
    private static final int IP_NACK = 0x400;
    private static final int IOS_ACK = 0x200;//iOS ack goggle's Tx data
    private static final int TX_TO = 0x100;
    private static final int EOR_BIT = 0x80;
    private static final int NOEOR_BIT = 0x40;
    private static final int Rx_BIT = 0x20;
    private static final int EOT_ACK = 0x10;
    private static final int EOT_BIT = 0x8;
    private static final int NOEOT_ACK = 0x4;
    private static final int GOGGLE_ACK_NEEDED = 0x2;
    private static final int Tx_BIT = 0x1;

    private boolean mNeedTxHandshake = true;
    private int shouldPoll = 0;

    // for send:
    private int mSendPriority;
    private int mPreviousSendPriority;
    private boolean[] mIsSendings = new boolean [NUM_PRIORITIES];
    private int [] iSents = new int [NUM_PRIORITIES];
    private int iToSend = 0;
    private byte[]  sendBuffer = new byte [ReconBLE.BUFFER_IO_SIZE];
    InputStream [] m_inputs = new InputStream [NUM_PRIORITIES];

    // For receive:
    byte [] rcv = new byte [ReconBLE.BUFFER_IO_SIZE];


    // A 2-D array that we use This shit has to do with a double
    // buffer temp receive byte array. The reason we are doing that is
    // we may need to further manipulate the bytes that we were using
    // in the previous received chunk. and we don't want to do that when we have
    // alraedy written them to the output stream
    private byte [][] doubleRcv = new byte[2][ReconBLE.BUFFER_IO_SIZE];
    private int doubleRcvIndex = 0;
    private int [] double_iread = new int[2];

    
    
    private boolean [] mIsReceivings = new boolean[NUM_PRIORITIES];
    private int[] mReceiveds = new int[NUM_PRIORITIES];
    private int mReceivePriority=0;
    private int mPreviousReceivePriority=0;
    protected ByteArrayOutputStream[] mOutputStreams =
	new ByteArrayOutputStream[NUM_PRIORITIES];
    ReconMessageValidator xmlValidator = new ReconMessageValidator();

    private File tempFile = null;

    private void init(){
	Log.v(TAG, "flushing buffer");
	BLENative.flushBuffers(mSession);
	Log.v(TAG,"Initializing receive streams");
	for (int i=0;i<NUM_PRIORITIES;i++){
	    //init receive shit
	    mIsReceivings[i] = false;
	    mReceiveds[i] = 0;
	    mOutputStreams[i] = null;
	    // init send shit
	    mIsSendings[i] = false;
	    iSents[i] = 0;
	    m_inputs[i] = null;
	}
	mReceivePriority = BLENative.getCurrentReceivePriority(mSession)&0x0f;
	mSendPriority = BLENative.getCurrentSendPriority(mSession)&0x0f;
	Log.v(TAG,"mReceivePriority "+mReceivePriority);
	Log.v(TAG,"mSendPriority "+mSendPriority);
    }


    /*
      The following set of functions are used to parse the 6 bytes are status
     */
    public int getConnectionStatus() {
	return mFullStatus[0];
    }
    public int getRole(){
	//	Log.v(TAG,""+mFullStatus[1]);
	return mFullStatus[1];
    }
    public int getTxIndex() {
	byte[] bs = new byte[2];
	BLENative.noninvasiveCheckStatus(mSession, bs);
	return bs[0];
    }
    public int getRxIndex() {
	byte[] bs = new byte[2];
	BLENative.noninvasiveCheckStatus(mSession, bs);
    	return bs[1];
    }
    public int getStreamFlags() {
	int temp = mFullStatus[5];
	temp = (temp << 8)&0xFF00;
	temp |= (mFullStatus[4]&0xFF);
	return temp;
    }
    /*
      End of set of functions that we truely love
     */
	
        
    public void run() {
	init();
	bExit = false;
	int status;
	while (true) {
	    status = (BLENative.newCheckStatus(mSession,1,mFullStatus) & 0xFF);
	    if (status == ReconBLE.ERR_FAIL) {
		Log.e(TAG, "BLE Device not ready -- could not check status");
		this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
		break;
	    }

	    this.ProcessStatus(status);
	    this.ProcessStreamStatus(getStreamFlags());
	    mAdaptiveWaitTime = calculateAdaptiveWaitTime();
	    synchronized (mMonitor) {
		try  {
		    mMonitor.wait(mAdaptiveWaitTime, 0);   // wait some itme
		} 
		catch (InterruptedException e) {
		}   
		// if exit was signaled this was client trigerred, so just quit
		if (bExit == true) {
		    Log.i(TAG, "BLE Connection Monitoring cancelled by User");
		    status = ReconBLE.ERR_PAIR;        
		    break;  
		}
	    }
	}
	Log.v(TAG, "Terminating!");  
    }

    private void ProcessStatus (int status)   {
	//Log.v(TAG,"Role is " + getRole()+ " connection status is " + status);
	if (mStatus == status && shouldPoll == 0) return;     // no internal change
	shouldPoll = 1;
	switch (status ) {
	case ReconBLE.BLEStatus.STATUS_INIT:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_INIT:");
	    break;
	case ReconBLE.BLEStatus.STATUS_IDLE:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_IDLE:");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
	    break;
	case ReconBLE.BLEStatus.STATUS_DISCONNECTED:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_DISCONNECTED:");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
	    break;
	case ReconBLE.BLEStatus.STATUS_CONNECTED:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_CONNECTED:");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
	    break;
	case ReconBLE.BLEStatus.STATUS_FAIL_ACTIVE:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_FAIL_ACTIVE:");
	    break;
	case ReconBLE.BLEStatus.STATUS_SPI_FAIL:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_SPI_FAIL:");
	    BLENative.sendSpecialCommand(mSession,(byte)0xfc); // That is for F*cking clear
	    break;
	case ReconBLE.BLEStatus.STATUS_REMOTE_LIST:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_REMOTE_LIST:");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
	    break;
	case ReconBLE.BLEStatus.STATUS_UNPAIR:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_UNPAIR:");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
	    break;
	case ReconBLE.BLEStatus.STATUS_CANCEL_BOND:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_CANCEL_BOND:");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
	    break;
	case 0xF0://Changed mode to master
	    Log.v(TAG,"ReconBLE.BLEStatus.0XF0:");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
	    break;
	case 0xFE:		// Changed mode to slave
	    Log.v(TAG,"ReconBLE.BLEStatus.0XFE:");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
	    break;
	case ReconBLE.BLEStatus.STATUS_MISC_DATA_READY:
	    //Log.v(TAG,"ReconBLE.BLEStatus.STATUS_MISC_DATA_READY:");
	    handleMiscDataReady();
	    break;
	case ReconBLE.BLEStatus.STATUS_MAC_READY:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_MAC_READY:");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    Log.v(TAG,"status is "+status);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS,ReconBLE.BLEStatus.STATUS_MAC_READY);
	    break;
	case ReconBLE.BLEStatus.STATUS_IRK_READY:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_IRK_READY:");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
	    break;
	}
	if (isJustConnected(status)) {
	    //	    Log.v(TAG,"isJustConnected");
	    //	    Log.v(TAG,"status is "+status+" mStatus is"+mStatus);
	    // Some times connected is implied and not specifically
	    // stated so we infer it and report upstrais Note that we
	    // call the function with new Bundle() to prevent racing
	    // condition from the above stast
	    Bundle tempB = new Bundle();
	    tempB.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (tempB, BLEMessageHandler.KEY_STATUS, ReconBLE.BLEStatus.STATUS_CONNECTED);
	}
	    mStatus = status;
    }

    private boolean isJustConnected(int status) {
	return ((status == ReconBLE.BLEStatus.STATUS_MAC_READY ||
		 status == ReconBLE.BLEStatus.STATUS_IRK_READY ||
		 status ==  ReconBLE.BLEStatus.STATUS_MISC_DATA_READY)
		&&
		!(mStatus == ReconBLE.BLEStatus.STATUS_MAC_READY ||
		  mStatus == ReconBLE.BLEStatus.STATUS_IRK_READY ||
		  mStatus == ReconBLE.BLEStatus.STATUS_MISC_DATA_READY)); 
    }

    private void ProcessStreamStatus (int status)   {
	/*
	#define SPI_FAIL       0x2000
	    #define EOR_BIT_REPEAT        0x1000
	    #define TX_RETRY              0x800
	    #define IP_NACK               0x400
	    #define IOS_ACK               0x200//iOS ack goggle's Tx data
	    #define TX_TO                 0x100
	    #define EOR_BIT               0x80
	    #define NOEOR_BIT             0x40
	    #define Rx_BIT                0x20
	    #define EOT_ACK               0x10
	    #define EOT_BIT               0x8
	    #define NOEOT_ACK             0x4
	    #define GOGGLE_ACK_NEEDED     0x2
	    #define Tx_BIT                0x1
	*/
	Log.v(TAG,"status is "+status);
	if((status&SPI_FAIL) != 0) {
	    BLENative.sendSpecialCommand(mSession,(byte)0xfc); // That is for F*cking clear
	}
	
	if ((status&NOEOR_BIT) !=  0) {
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_RECEIVE_NOEOR:");
	    receiveNextChunk(ReconBLE.BLEStatus.STATUS_RECEIVE_NOEOR);
	}
	else if ((status&EOR_BIT) != 0) {
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_RECEIVE_NOEOR:");
	    receiveNextChunk(ReconBLE.BLEStatus.STATUS_RECEIVE_EOR);
	}
	if ((status&NOEOT_ACK) != 0) {
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_SEND_NOEOT:");
	    sendNextChunk(ReconBLE.BLEStatus.STATUS_SEND_NOEOT);
	}
	else if((status&EOT_ACK) != 0) {
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_SEND_EOT:");
	    NotifyStopSend();
	}
	if ((status&TX_TO) != 0) {
	    Log.v(TAG,"ReconBLE.BLEStatus.TX_TO:");
	    Log.v(TAG,"sending disconnect command");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS,ReconBLE.BLEStatus.STATUS_DISCONNECTED);


	}
    }
    /*	    
	switch (status ) {
	case ReconBLE.BLEStatus.STATUS_RECEIVE_NOEOR:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_RECEIVE_NOEOR:");
	    receiveNextChunk(status);
	    break;
	case ReconBLE.BLEStatus.STATUS_RECEIVE_EOR:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_RECEIVE_EOR:");
	    receiveNextChunk(status);
	    break;
	case ReconBLE.BLEStatus.STATUS_SEND_NOEOT:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_SEND_NOEOT:");
	    sendNextChunk(status);
	    break;
	case ReconBLE.BLEStatus.STATUS_SEND_EOT:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_SEND_EOT:");
	    sendNextChunk(status);
	    break;
	case ReconBLE.BLEStatus.STATUS_RECEIVE_START:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_RECEIVE_START:");
	    receiveNextChunk(status);
	    break;
	case ReconBLE.BLEStatus.STATUS_RECEIVE_START_NR:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_RECEIVE_START_NR:");
	    // It means ios is trying to send something without
	    // handshake while we are in Idle mode
	    BLENative.startReceive(mSession,false);
	    break;
	case ReconBLE.BLEStatus.STATUS_MAC_READY:
	    //Log.v(TAG,"ReconBLE.BLEStatus.STATUS_MAC_READY:");
	    break;
	case ReconBLE.BLEStatus.STATUS_IRK_READY:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_IRK_READY:");
	    b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	    this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
	    break;
	case ReconBLE.BLEStatus.STATUS_MISC_DATA_READY:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_MISC_DATA_READY:");
	    handleMiscDataReady();
	    break;
	case ReconBLE.BLEStatus.STATUS_READY_SEND:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_READY_SEND:");
	    sendNextChunk(status);
	    break;
	case ReconBLE.BLEStatus.STATUS_NEW_SHIP_READY:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_NEW_SHIP_READY:");
	    Log.v(TAG,"Send Ship status changed");
	    sendNextChunk(status);
	    break;
	case ReconBLE.BLEStatus.STATUS_SWITCH_TO_TX:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_SWITCH_TO_TX:");
	    Log.v(TAG,"Switch to tx");
	    handleSwithToTxMode(status);
	    break;
	case ReconBLE.BLEStatus.STATUS_SWITCH_RX_SHIP:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_SWITCH_RX_SHIP:");
	    Log.v(TAG,"Receive Ship status changed");
	    receiveNextChunk(status);
	    break;
	case ReconBLE.BLEStatus.STATUS_IP_REFUSE_RECEIVE:
	    Log.v(TAG,"ReconBLE.BLEStatus.STATUS_IP_REFUSE_RECEIVE");
	    Log.v(TAG,"We go back to receive mode");
	    BLENative.startReceive(mSession,true);
	    break;
	}
	mStatus = status;
    */

    private int handleMiscDataReady() {
	//Log.v(TAG,"handle misc data");
	byte[] miscbuffer = new byte[MISC_DATA_SIZE];
	BLENative.getMiscData(mSession,miscbuffer);
	sendoutMiscData(miscbuffer);
	shouldPoll = 1;
	return 1;
    }

    private int findPriorityForSendingNextChunk() {
	for (int i=NUM_PRIORITIES-1;i>=0;i--) {
	    if (m_inputs[i] != null) {
		return i;
	    }
	}
	return -1;	
    }
    
    private void sendNextChunk(int passedStat) {
	Log.v(TAG,"Sending next chunk");
	// Update the pirority status of the system. Note that the
	// function doesn't set any values just querries the chip and
	// records the result.
	fixSendPriority();
	//	Log.v(TAG,"Priority "+mSendPriority);
	int status;
	// If we have to finish the current transmission we do it here
	// if (passedStat == ReconBLE.BLEStatus.STATUS_SEND_EOT) {
	//     Log.v(TAG,"BLEStatus.STATUS_SEND_EOT");
	//     NotifyStopSend ();
	//     this.SendStatus(b, BLEMessageHandler.KEY_STATUS, passedStat);
	//     return;
	// }
	int nextSendPriority = findPriorityForSendingNextChunk();
	//	Log.v(TAG,"nextSendPriority is "+nextSendPriority);
	if (nextSendPriority == -1) {
	    Log.v(TAG,"Nothing to send");
	    tellKernelWeHaveNothingToSend();
	    return;
	}
	BLENative.setSendPriority(mSession,nextSendPriority);
	fixSendPriority();
	//Sanity check:
	if (nextSendPriority != mSendPriority) {
	    Log.w(TAG,"Priority did not change... Not sending");
	    return;
	}
	// Now sanity check that we are not trying to send empty sh*t.
	if (m_inputs[mSendPriority] == null) {
	    Log.w(TAG,"Trying to send empty ... exiting the function");
	    return;
	}

	// If it is the start of transmimssion then we fix the flag
	if (!mIsSendings[mSendPriority]) {
	    mIsSendings[mSendPriority] = true;
	}

	// Ok: At this stage are the preliminary sh*t is taken care
	// of. we can send some real data
	try  {
	    iToSend = m_inputs[mSendPriority].read(sendBuffer, 0, ReconBLE.BUFFER_IO_SIZE); 
	}
	catch (IOException e)  {
	    Log.e(TAG, "Input Buffer read Error: " + e.getMessage() );
	    status = ReconBLE.ERR_FAIL;
	    return;
	}
	// boundary case; If 0 bytes, we have whole number of full
	// ReconBLE.BLE_BLOCK_SIZE, so we need to mannually send EOT
	if (iToSend == 0)  {
	    Log.i(TAG, "Manually sending EOT -- whole number of full data chunks");
	    status = BLENative.endTransmission(mSession,mSendPriority,57);
	    if (status != ReconBLE.ERR_SUCCESS){
		Log.e(TAG, "Transmission End Failure");
	    }
	    // quit at each case. TODO: Wait for ACK here??
	    return;
	}
	else if (iToSend == -1) {
	    Log.v(TAG,"Seems that kernel has missed sending EOT because it was interrupted");
	    int goodbytes = iSents[mSendPriority] % 57;
	    if (goodbytes == 0) goodbytes = 57;
	    status = BLENative.endTransmission(mSession,mSendPriority,goodbytes);
	    if (status != ReconBLE.ERR_SUCCESS){
		Log.e(TAG, "Transmission End Failure");
	    }
	    return;
	}
	// transmit using Native layer, waiting for ACK we can keep sending:
	status = BLENative.sendData(mSession, sendBuffer, iToSend); 
	// update progress: Total # of bytes sent:
	iSents[mSendPriority] += iToSend;
	// TODO
	b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_tx_PROGRESS);
	this.SendStatus(b, BLEMessageHandler.KEY_PROGRESS, iSents[mSendPriority]);
	// check sent size. If less than MAX, we are done:
	if (iToSend < ReconBLE.BUFFER_IO_SIZE) {
	    status = ReconBLE.ERR_SUCCESS;
	    return;
	}
    }

    private int receiveNextChunk (int passedStat) {
	doubleRcvIndex = (doubleRcvIndex+1)%2;
	Log.v(TAG,"new receiveNextChunk");
	int streamFlags = getStreamFlags();
	// 1) Get Ship type
	mReceivePriority = BLENative.getCurrentReceivePriority(mSession);
	// 2) read from the buffer
	double_iread[doubleRcvIndex] = BLENative.receiveData(mSession, doubleRcv[doubleRcvIndex]);
	// 3) Handle shit after read buffer: That is write the bytes
	// to the input stream
	int handlereturn = new_handleShitAfterReceiveData(double_iread,mReceivePriority,
							  doubleRcv,0);
	if (handlereturn!=ReconBLE.ERR_SUCCESS){
	    return handlereturn;	    
	}
	// 4) Notify end of receive if EOR received
	if (passedStat == ReconBLE.BLEStatus.STATUS_RECEIVE_EOR) {
	    NotifyStopReceive();
	}
	return ReconBLE.ERR_SUCCESS;

	
    }

    /**
       This funciton sorts through the received bytse and dumps them
       ino the correct output stream. It also detects extra padded
       bytes and discards them.
     */
    private int new_handleShitAfterReceiveData
	(int [] iread, int prior, byte[][] double_rcv_buf, int start) {
	Log.v(TAG,"new_handleShitAfterReceiveData");
	int streamFlags = getStreamFlags();
	Log.v(TAG,"doubleRcvIndex = "+doubleRcvIndex);
	if (mIsReceivings[prior] == false) {//Start of a new receive
	    Log.v(TAG,"start of a new receive");
	    if (iread[doubleRcvIndex] == 0) {//there is nothing to read. Duh!
		Log.v(TAG,"there is nothing to read. Duh!");
	    }
	    else {// There is something to read:
		// initialize streams
		Log.v(TAG,"there is something to read!");
		mOutputStreams[prior]= new ByteArrayOutputStream();
		mReceiveds[prior] = 0;
		mIsReceivings[prior] = true;
		Log.v(TAG,"mOutputStreams[prior].size = " + mOutputStreams[prior].size());
		if ((streamFlags&EOR_BIT)!=0) {//entire message is in one
					  //chunk, eor included
		    Log.v(TAG,"entire message is in one chunk");
		    //Dump the current buffer into output stream
		    mOutputStreams[prior].write
			(double_rcv_buf[doubleRcvIndex], start, iread[doubleRcvIndex]);
		}
		else { //entire message is not in one chunk: in this
		       //case we lag writing into the output stream by
		       //one chunk, so that if we need to manipulate
		       //the previous received chunk before dumping it
		       //into outputstream, we are able to do so.
		    Log.v(TAG,"entire message is NOT in one chunk");
		}
	    }
	}
	else { //Have been receiving in this priority.
	    Log.v(TAG,"Have been receiving in this priority");
	    int pIndex = (doubleRcvIndex+1)%2;//for previous index
	    Log.v(TAG,"pIndex is"+pIndex);
	    if ((streamFlags&EOR_BIT)!= 0) {		//It is the final chunk:
		Log.v(TAG,"it is the final chunk");
		if (iread[doubleRcvIndex] > 0) {//regular case
		    Log.v(TAG,"regular case: not padded to be exactly 1026");
		    //Dump the previous chunk and current chunk into
		    //the outputstream. No hassle is needed
		    Log.v(TAG,"mOutputStreams[prior].size"+ mOutputStreams[prior].size());
		    mOutputStreams[prior].write
			(double_rcv_buf[pIndex], start, iread[pIndex]);
		    Log.v(TAG,"mOutputStreams[prior].size"+ mOutputStreams[prior].size());
		    mOutputStreams[prior].write
			(double_rcv_buf[doubleRcvIndex], start,
			 iread[doubleRcvIndex]);
		}
		else if (iread[doubleRcvIndex] == 0) { //Special case:
		    Log.v(TAG,"special case");
		    // We need to throw away some bytes from the
		    // previous chunk before we dump it into output
		    // stream.

		    //How much to keep:
		    int howmuch = iread[pIndex] - 57 + double_rcv_buf[pIndex][0];
		    if (howmuch > 0) { // Dump the correct amount of
				       // the previous chunk into
				       // output stream
			Log.v(TAG,"The correct amount to dump is "+howmuch);
			mOutputStreams[prior].write
			    (double_rcv_buf[pIndex], start, howmuch);
		    }
		    //Because iread[doubleRcvIndex] == 0 no need to
		    //dump anything from the current chunk into output 
		    
		}
	    }
	    else {// Not the final chunk
		Log.v(TAG,"not the final chunk");
		// Write the previous data into into stream
		Log.v(TAG,"Write the previous data into into stream");
		Log.v(TAG,"Sanity check");
		// String s = new String(double_rcv_buf[pIndex]);
		// Log.v(TAG,"Previous content is "+s);
		Log.v(TAG,"iread[pIndex] =  " + iread[pIndex]);
		Log.v(TAG,"start =  " + start);
		mOutputStreams[prior].write
		    (double_rcv_buf[pIndex], start, iread[pIndex]);
	    }
	}

	// notify client:
	try {
	    if (mOutputStreams[prior] != null) {
		mOutputStreams[prior].flush();
	    }
	} catch (IOException e) {
	    Log.e(TAG, "Could not write to buffer");
	}
	mReceiveds[prior] += iread[doubleRcvIndex];
	Log.v(TAG, String.format("Successfully received [%d] bytes", mReceiveds[prior]) );
	this.SendStatus(b,  BLEMessageHandler.KEY_PROGRESS, mReceiveds[prior]);
	
	return ReconBLE.ERR_SUCCESS;
    }

    public int NotifyReceive () {
	Log.v(TAG,"Start NotifyReceive");
	Log.v(TAG,"We still don't know teh receive priorioty");

	//	fixReceivePriority();
	//Log.v(TAG,"mReceivePriority" + mReceivePriority);
	//	mIsReceivings[mReceivePriority] = true;
	//	mReceiveds[mReceivePriority] = 0;
	//	try {
	//	    mOutputStreams[mReceivePriority] = new ByteArrayOutputStream();
	//	}
	//	catch (Exception e) {
		
	//	}
	Log.v(TAG,"Attempt to Flushd buffer");
	int status = BLENative.flushBuffers(mSession);
	if (status == ReconBLE.ERR_SUCCESS)  {
	    Log.v(TAG,"Flushed buffer");
	}
	return status;
    }

    public void NotifyStopReceive () {
	Log.v(TAG,"NotifyStopReceive");
	Log.v(TAG,"Current Receive Priority = " + mReceivePriority);
	mIsReceivings[mReceivePriority] = false;
	mReceiveds[mReceivePriority] = 0;
	this.HandleAndCloseReceiveChannel(mReceivePriority);
	//getReadyForPotentialNewSendOrReceive(false) ; // Means after receive
    }

    public void upperLevelHasNohtingToSend() {
	//So we send our own cached
	Log.v(TAG,"upperLevelHasNohtingToSend()");
	synchronized (mMonitor)   {
	    sendNextChunk(ReconBLE.BLEStatus.STATUS_SEND_NOEOT);
	    mMonitor.notify();
	}
    }
    public int InsertToTheQue (InputStream inputstream, int prior) {
	Log.v(TAG,"InsertToTheQue");
	synchronized (mMonitor)   {
	    if (m_inputs[prior] != null) {
		Log.v(TAG,"Insertion rejected");
		return ReconBLE.ERR_FAIL;
	    }
	    else {
		m_inputs[prior] = inputstream;
		Log.v(TAG,"Insertion accepted");
		Log.v(TAG,"Tx Index is "+ getTxIndex());
	    }
	    if (getTxIndex() == 0) {
		//Means we are not busy sending another packet so
		//we can send
		Log.v(TAG,"Can immediately send");
		sendNextChunk(ReconBLE.BLEStatus.STATUS_SEND_NOEOT);		    
	    } else {
		Log.v(TAG,"System is busy sending other stuff");
	    }
	    mMonitor.notify();
	}
	return ReconBLE.ERR_SUCCESS;
    }
    public int NotifySendNew ()  {
	Log.v(TAG,"NotifySendNew");
	synchronized (mMonitor)   {
	    if (mIsSendings[mSendPriority]) {
		return ReconBLE.ERR_FAIL;
	    }
	    Log.v(TAG,"Start Handshake");
	    int status = BLENative.beginTransmission(mSession);
	    if (status == ReconBLE.ERR_SUCCESS){
		Log.v(TAG,"Handshake was success");
		Log.v(TAG,"Flushing buffer");
		status = BLENative.flushBuffers(mSession);
	    }
	    if (status != ReconBLE.ERR_SUCCESS) return status;
	    mIsSendings[mSendPriority] = true;
	    mMonitor.notify();
	}
	return ReconBLE.ERR_SUCCESS;
    }
    
    // TODO: combone the two 'notifyStopSend's into one. 
    public void NotifyStopSend (int prior) {
	Log.v(TAG,"NotifyStopSend");
	synchronized (mMonitor) {
	    if (prior > 3) {
		Log.w(TAG,"mSendPriority > 3");
		return;
	    }
	    mIsSendings[prior] = false;
	    iSents[prior] = 0;
	    this.CloseInput();
	    mMonitor.notify();
	}
	reportSendComplete();
    }

    public void NotifyStopSend () {
	Log.v(TAG,"NotifyStopSend");
	synchronized (mMonitor) {
	    fixSendPriority();
	    if (mSendPriority > 3) {
		Log.w(TAG,"mSendPriority > 3");
		return;
	    } else {
		Log.v(TAG,"send priority is "+mSendPriority);
		mIsSendings[mSendPriority] = false;
		iSents[mSendPriority] = 0;
 		this.CloseInput();
	    }
	    mMonitor.notify();
	}
	reportSendComplete();
    }

    public void ClearBuffers () {
	Log.v(TAG,"NotifyStopSend");
	synchronized (mMonitor) {
	    init();
	    mMonitor.notify();
	}
    }

    // internal helper that closes input stream on exit. Hyper anal java throws exceptions
    private void CloseInput ()
    {
	Log.v(TAG,"CloseInput. mSendPriority "+mSendPriority);
	try  {
	    if (m_inputs[mSendPriority] != null)
		m_inputs[mSendPriority].close();
	    m_inputs[mSendPriority] = null;
	} 
	catch (IOException e)	    {
	    Log.w(TAG, "Exception thrown while closing Input Stream: " + e.getMessage() + " but we don't really care");
	}
    }

    private void HandleAndCloseReceiveChannel (int prior) {
	// Here we handle what we want to do with different priorities:
	// prior 0: write to file.
	// prior 1: send to xml message validator
	// prior 2: send to xml message validator
	// prior 3: special handling: used for incremental rib for now
    	Log.v(TAG, "HandleAndCloseReceiveChannel(prior="+prior);
	if (prior == 0 || prior == 3) { // FIXME: prior 3 should not be file like
	    Log.v(TAG,"The file size to be written is "+mOutputStreams[prior].size());
	    writeStreamToFile(mOutputStreams[prior]);
	    reportFileReceived();
	}
	else if (prior == 1 || prior == 2) {	// XML validation
	    Log.v(TAG,"xml received. validating");
	    String xmlmessage = writeStreamToStringAndValidate(mOutputStreams[prior]);
	    if (xmlmessage != null) {
		sendoutXml(xmlmessage);
	    }
	    else {
		Log.v(TAG,"XML was not validated");
	    }
	}
	if (mOutputStreams[prior] != null) {
	    // close the channel:
	    try {
		mOutputStreams[prior].close();
		mOutputStreams[prior] = null;
	    }
	    catch (IOException e) {
		Log.w(TAG, "Exception thrown while closing Output Stream: " +
		      e.getMessage() + " but we don't really care");
	    }
	}
    }
    private String writeStreamToStringAndValidate(ByteArrayOutputStream ops) {
	Log.v(TAG,"writeStreamToStringAndValidate");
	if (ops != null) {
	    Log.v(TAG,"outputstream is not null");
	    String xmlMessage = null;
	    xmlMessage = ops.toString();
	    Log.d(TAG,"received "+xmlMessage);
	    ReconMessageValidator xmlValidator = new ReconMessageValidator();
	    xmlValidator.appendString(xmlMessage);
	    String newxmlmessage = xmlValidator.validate();
	    return newxmlmessage;
	}
	return null;
    }

    private void writeStreamToFile(ByteArrayOutputStream ops) {
	Log.v(TAG,"writeStreamToFile");
	if (ops != null) {
	    // 1) Find good file name:
	    for (int i = 1;i<=10000;i++) {
		tempFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/tmpFile_BLE_Receive_"+
				    mReceivePriority+"--"+i+".txt");
		if (!tempFile.exists()) {
		    break;
		}
	    }
	    Log.v(TAG,"Good temp file name "+ tempFile.getName());
	    try  {
		//Log.v(TAG,mOutputStream.toString());
		Log.i(TAG,"writing to file");
		FileOutputStream outputStream =
		    new FileOutputStream (tempFile);
		ops.writeTo(outputStream);
		outputStream.close();
	    } 
	    catch (IOException e) {
		Log.w(TAG, "Exception thrown while closing Output Stream: " +
		      e.getMessage() + " but we don't really care");
	    }
	}
	else {
	    Log.w(TAG,"The received file empty");
	}
    }

    private int calculateAdaptiveWaitTime() {
	if (mIsSendings[0] ||mIsSendings[1] ||
	    mIsSendings[2] ||mIsSendings[3] ||
	    mIsReceivings[0] ||mIsReceivings[1] ||
	    mIsReceivings[2] || mIsReceivings[3]) {
	    return 10;
	    //	    return 1;
	}
	else {
	    return 20;
	    //	    return 2;
	}
    }
    private boolean fixReceivePriority() {
	int crpr = BLENative.getCurrentReceivePriority(mSession);
	if ((crpr & 0x80) == 0x80) {	// Priority change
	    mPreviousReceivePriority = (crpr&0x70)>>4;
	    mReceivePriority = crpr&0x0f;
	    return true;
	}
	mReceivePriority = crpr&0x0f;
	return false;
    }
    private void sendoutXml(String xml) {
	Log.v(TAG,"sendoutXml");
	Bundle tmpB = new Bundle();
	tmpB.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_XML_READY);
	tmpB.putString("xml",xml);
	Message message   = Message.obtain(mNotifyHandler);
    	message.setData(tmpB);
    	message.sendToTarget();
    }

    private void sendoutMiscData(byte[] miscData) {
	//Log.v(TAG,"SendoutMiscData");
	Bundle tmpB = new Bundle();
	tmpB.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_MISC_DATA_READY);
	tmpB.putByteArray("misc_data",miscData);
	Message message   = Message.obtain(mNotifyHandler);
    	message.setData(tmpB);
    	message.sendToTarget();
    }

    private void reportSendComplete() {
	Log.v(TAG,"reportSendComplete");
	Bundle tmpB = new Bundle();
	tmpB.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_tx_END);
	tmpB.putInt(BLEMessageHandler.KEY_LAST_SENT_PRIORITY,mSendPriority);
	Message message   = Message.obtain(mNotifyHandler);
    	message.setData(tmpB);
    	message.sendToTarget();
    }
    private int tellKernelWeHaveNothingToSend() {
	Log.v(TAG,"tellKernelWeHaveNothingToSend");
	//This is a special magic argument command meaning that there is nothing to send
	return BLENative.endTransmission(mSession,0x80,0x0);
    }
    private void reportFileReceived() {
	Log.v(TAG,"reportReceiveComplete");
	Bundle tmpB = new Bundle();
	if (tempFile == null) {
	    Log.v(TAG,"Not gonna operate on empty file");
	    return ;
	}
	tmpB.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_rx_END);
	tmpB.putString("tempFileName",tempFile.getName());
	Message message   = Message.obtain(mNotifyHandler);
    	message.setData(tmpB);
    	message.sendToTarget();
    }
    private void fixSendPriority() {
	int csps = BLENative.getCurrentSendPriority(mSession);
	mPreviousSendPriority = (csps&0xf0)>>4;
	mSendPriority = csps&0x0f;
    }
    private int findHighestPriorityForRemainingReceive() {
	for (int i=NUM_PRIORITIES-1;i>=0;i--) {
	    if (mOutputStreams[i] != null) {
		return i;
	    }
	}
	return -1;	
    }
}
class ReconMessageValidator {
    StringBuilder sb = new StringBuilder();
    // DOTALL lets us include newline characters in a message, useful for text messages
    Pattern mRegexPattern = Pattern.compile("(<recon.+?</recon>)",Pattern.DOTALL);
    Matcher mRegexMatcher;
    public void reset() {
	sb = new StringBuilder();
    }
    public void appendString(String s) {
	sb.append(s);
    }
    public void appendString(byte[] b, int len) {
	String s = new String(b, 0, len);
	sb.append(s);
    }
    public String validate() {
	mRegexMatcher = mRegexPattern.matcher(sb.toString());
	if (mRegexMatcher.find()) {
	    String s = mRegexMatcher.group(0);
	    sb.delete(mRegexMatcher.start(), mRegexMatcher.end());
	    return s;
	}
	else {
	    return null;
	}
    }
}


