package com.reconinstruments.reconble;

import java.io.FileOutputStream;

import android.os.Bundle;
import android.util.Log;

/* Context thread for Monitoring of BLE connection
 * Will NOT be triggered manually during invocation of Pairing API -- client must start it explicitly 
 * As long as this thread is running, client should be notified of connection status changes */
public class BLEPairTask extends BLETask
{
    private static final String TAG = BLEPairTask.class.getSimpleName();
	 
    // mac address we are currently being paired with; set by parent
    // we keep a copy here because on success we write BLE.RIB
    // (self note: Not sure BLE.RIB concept is valid -- seems to cause complexity with no real benefit)
    byte [] macAddress = new byte [ReconBLE.MAC_ADDRESS_SIZE];
	
    // current status; on flip we notify client
    private int mStatus = ReconBLE.BLEStatus.STATUS_DISCONNECTED;
	
    Bundle b = new Bundle();   // status change notification message
	
    // setting of MAC address from outside. TODO: synchronize (guard with Mutex)
    public void setMACAddress (byte[] newaddress)
    {
	if (newaddress != null)
	    {
		System.arraycopy(newaddress, 0, 
				 macAddress, 0, ReconBLE.MAC_ADDRESS_SIZE);
	    }
    }
	
    public void run()
    {
	// Here we keep polling BLE status indefinitely until we get cancelled
	// We report connection status change
	// *** NOTE: When the driver supports real async mode (i.e. poll(fd)
	//           Change this code. Right now we have to sleep in tight loop and keep checking 
	//           which is very inefficient due to user/kernel context switch ****
		
	// clear exit signal
	bExit = false;  
		
	// prepare return message bundle
	b.putInt(BLEMessageHandler.KEY_TYPE, BLEMessageHandler.TYPE_PAIR);
	int status = ReconBLE.ERR_SUCCESS;
		
	//
	// now loop until we are told to quit : exit set or internal error (ERR_FAIL)
	// We report status change:
	//
	// -- connected             : status == ReconBLE.BLEStatus.STATUS_CONNECTED
	// -- disconnected          : status == ReconBLE.BLEStatus.STATUS_DISCONNECTED
	// -- remote list available : status == ReconBLE.BLEStatus.STATUS_REMOTE_LIST
	//
	while (true)
	    {
		// check status 
		//status = BLENative.checkStatus(mSession,false);  
			
		// check generic failure in which case we quit
		if (status == ReconBLE.ERR_FAIL) 
		    {
			Log.e(TAG, "BLE Device not ready -- could not check status");
			this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
				
			// break loop
			break;
		    }
	
		// process this status
		this.ProcessStatus(status);
		 
	        // wait for exit, then poll again
		synchronized (mMonitor)
		    {
			try 
			    {
				mMonitor.wait(ReconBLE.PAIR_POLL_TMO, 0);   // wait a second
			    } 
			catch (InterruptedException e)
			    {
			    }   
				
			// if exit was signaled this was client trigerred, so just quit
			if (bExit == true)  
			    {
				Log.i(TAG, "BLE Connection Monitoring cancelled by User");
					
				status = ReconBLE.ERR_PAIR;        
				break;  
			    }
		    }
			
	    }

	// and we are done!
	Log.d(TAG, "Terminating!");  
    }
	
    // internal helper to write BLE.rib with paired master mac address
    private void WritePair ()
    {
	try 
	    {
		FileOutputStream fs = new FileOutputStream (ReconBLE.BLE_PATH);
		fs.write(macAddress, 0, ReconBLE.MAC_ADDRESS_SIZE);
			
		fs.close();
	    } 
		 
	catch (Exception ex) 
	    {
		Log.e(TAG, "Failed to save Paired Master MAC Address in " + ReconBLE.BLE_PATH);
	    }
    }
	
    // private helper to report status change & flip internal state if required
    private void ProcessStatus (int status)
    {
	if (mStatus == status) return;     // no internal change
		
	if ( (status != ReconBLE.BLEStatus.STATUS_CONNECTED) &&
	     (status != ReconBLE.BLEStatus.STATUS_DISCONNECTED) &&
	     (status != ReconBLE.BLEStatus.STATUS_REMOTE_LIST) )    return;   // not interested
		
	// now time to flip!
	Log.i (TAG, String.format ("Connection status changed. Old [%d], new [%d]", mStatus, status) );
	mStatus = status;
		
	// internal post-processing
	if (status == ReconBLE.BLEStatus.STATUS_CONNECTED)  
	    {
		// here we (re)write BLE.rib with mac address
		if (macAddress[0] != 0x00 && macAddress[1] != 0x00)
		    this.WritePair(); 
	    }

	// inform client
	this.SendStatus (b, BLEMessageHandler.KEY_STATUS, status);
    }
}
