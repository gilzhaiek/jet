package com.reconinstruments.reconble;

/* Native Proxy; bridge between Java world and JNI
 * This is PURE INTERFACE with no logic at this level
 * Essentially a "C header file" */

public class BLENative 
{	 
    // Session built / teardown (c-tor / d-tor)
    protected   static native int openDevice   ();               // called at init: Opens device file ("/dev/cc_ble2540")
    protected   static native int closeDevice  (int session);    // close BLE device
     
    // Full board reset
    protected   static native int resetDevice   (int session);
     
    // Mode management (master/slave) 
    protected   static native int setMaster      (int session);        // sets device to master mode
    protected   static native int setSlave       (int session);        // sets device to slave mode
     
    protected   static native int getMode      (int session);          // retrieves current device mode status
     
    // Pairing with Master in Slave Mode
    protected   static native int pairMaster      (int session, byte [] mac);   // tries to pair with passed MAC address; also advertises (new search)
    protected   static native int getMasterList   (int session, byte [] list);  // retrieves broadcasted master list
    protected   static native int bondMaster      (int session, byte [] mac);   // tries to bond with passed MAC address; effective connection attempt
    protected   static native int newSearch       (int session);                // starts new master search
    protected   static native int unpairMaster    (int session);                // unpairs with Paired master
    protected   static native int unpairSlave    (int session, int arg);                // unpairs with Paired slave
    protected   static native int slaveRequestSecurity (int session); // Request security key
    protected   static native int getIrk (int session, byte [] irk); // Request security key
    protected   static native int getMiscData (int session, byte [] miscdata); // Request security key


    // New Ali functions
    protected static native int pairWithSlave (int session, int bondstatus, byte[] mac);
    protected static native int pairWithMaster(int session, int bondstatus, byte[] irk);
    
     
    // Generic Status Retrieval. (Initially used to support async model)
    protected   static native int checkStatus (int session,int withPolling);

     
    // Buffer Flush
    protected   static native int flushBuffers (int session);
     
    // Tx
    protected   static native int beginTransmission (int session);                                // starts send Transaction
    protected   static native int sendData          (int session, byte [] buffer, int numBytes);  // sends raw byte buffer  -- transmit binary objects
    protected   static native int endTransmission   (int session, int prior, int numBytes);                                // ends send Transaction: Necessary only on 1026 packet boundary
    // numBytes by default 57
     

    
    protected   static native int startReceive (int session, boolean shouldAck);                // Go to receive mode

    
    // Rx
    protected   static native int receiveData       (int session, byte [] buffer);                // data read. Returns # of bytes read, -1 on error

    protected static native int setSendPriority (int session, int prio); // 0 1 2 3 (3 highers)

    protected static native int getCurrentSendPriority(int session);
    protected static native int getCurrentReceivePriority(int session);
    protected static native int getSendOrReceiveMode(int session);
    protected static native int sendAck(int session);
    protected static native int resetRemote(int session, byte[] irk);
    protected static native int getOwnMac(int session, byte [] buffer);
    protected static native int getRemoteMac(int session, byte [] buffer, int needVersion);
    protected static native int telliOSToClear(int session);
    protected static native int sendControlByte(int session,byte ctrl);
    protected static native int sendSpecialCommand(int session,byte theByte);

    protected   static native int newCheckStatus (int session,int withPolling, byte[] theStatus);
    protected   static native int noninvasiveCheckStatus (int session, byte[] theStatus);
}
