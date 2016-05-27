package com.reconinstruments.reconble;

import java.io.ByteArrayOutputStream;

import android.os.Bundle;
import android.util.Log;

/* Implementation of Rx Thread.
 * Client app would typically trigger this on Startup, then
 * be notified of Rx Events via IReconBLEEventListener
 * 
 * TODO: -- Flip to IRQ mode instead of polling (applies to all async tasks)
 *       -- Client ACK on Transmission Start? (ie allow Rejection; right now we notify client
 *          but start receiving regardless
 *  */
public class BLEReceiveTask extends BLETask
{
    private static final String TAG = BLEReceiveTask.class.getSimpleName();
	 
    private int mReceived = 0;
    protected ByteArrayOutputStream mStream;   // caller output stream
	
    Bundle bCompletion = new Bundle();
    Bundle bProgress = new Bundle();
	
    // thread meat
    public void run()
    {
	return;
    }

    // internal helper -- polls on status (or cancel). Once it returns BLE_READY_RECEIVE, READY_NOEOR
    // parent caller (thread function) can start Read
    private int StartReceive ()
    {
	while (true)
	    {
    		// poll driver for status. We need IP_READY_ACK
		//hack ali
		//    		int status = BLENative.checkStatus(mSession);

		int status = 10000; // hackali
    		
    		Log.d(TAG, String.format("Polling BLE driver. Status: [%d]", status) );
    		
    		// can we handle it ourselves?
    		if ( (status == ReconBLE.BLEStatus.STATUS_RECEIVE_START_NR) ||
		     (status == ReconBLE.BLEStatus.STATUS_IDLE) )  
		    {
    			// clear buffers. Why is this necessary? Driver should
    			// take care of it
    			status = BLENative.flushBuffers(mSession);
    			if (status != ReconBLE.ERR_SUCCESS)
			    {
    				Log.e(TAG, "BLE Device not ready -- could not flush buffers");
    				return status;
			    }
    			
		    }
    		
    		if ( (status == ReconBLE.BLEStatus.STATUS_RECEIVE_START) ||
		     (status == ReconBLE.BLEStatus.STATUS_RECEIVE_NOEOR) ||
		     (status == ReconBLE.BLEStatus.STATUS_RECEIVE_EOR) ) 
    			 
		    return status;   // ok, we want caller to handle it
     		   

	    	synchronized (mMonitor)
		    {
			try  
			    {
				// wait for exit to be signaled, then try again
				mMonitor.wait(ReconBLE.RECEIVE_START_POLL_TMO, 0);   
			    } 
			catch (InterruptedException e) 
			    {
			    }   
				
			// if exit was signaled this was client trigerred, so just quit
			if (bExit == true)
			    {
				Log.i(TAG, "Receive Transaction cancelled by User");
			        return ReconBLE.ERR_READ;   // Tx cancelled -- treat as failure
			    }
		    }
	    }
    }

    // internal helper -- keeps receiving untill error, end of transfer, or cancel
    private int Receive ()
    {
	while (true)
	    {
		// raw data buffer
		byte [] rcv = new byte [ReconBLE.BUFFER_IO_SIZE];
			
    		// poll driver for status. We need RECEIVE_NOEOR or RECEIVE_EOR
		//hack ali
		int status = 0;
		//    		int status = BLENative.checkStatus(mSession);
    		// check failure
    		if ( status == ReconBLE.ERR_FAIL)  
		    {
    			Log.e(TAG, "BLE Device not ready -- could not check status");
    			return status;
		    }
    		
    		// I/O 
    		if ( (status == ReconBLE.BLEStatus.STATUS_RECEIVE_NOEOR) ||
		     (status == ReconBLE.BLEStatus.STATUS_RECEIVE_EOR) )
		    {
    			int iread = BLENative.receiveData(mSession, rcv);
    			
        		// check failure
        		if ( iread == ReconBLE.ERR_FAIL)
			    {
        			Log.e(TAG, "BLE Device not ready -- could not check status");
        			return ReconBLE.ERR_READ;
			    }
        		
        		if (iread > 0)
			    {
	        		// otherwise we have # of bytes read. 
	        		mStream.write (rcv, 0, iread);
	        		
	        		// notify client
	        		mReceived += iread;
	        		Log.d(TAG, String.format("Successfully received [%d] bytes", mReceived) );
	        		
	        		this.SendStatus(bProgress,  BLEMessageHandler.KEY_PROGRESS, mReceived);
			    }
        		else
			    status = ReconBLE.BLEStatus.STATUS_RECEIVE_EOR;
   
		    }
    		
		// check if EOR & byte boundary 
    		// TODO: Handle special case & test
    		if (status == ReconBLE.BLEStatus.STATUS_RECEIVE_EOR)
		    return status;
    		
		// now poll for cancel
		synchronized (mMonitor)
		    {
    			try  
			    {
    				// wait for exit to be signaled, then try again
    				mMonitor.wait(ReconBLE.RECEIVE_PROGRESS_TMO, 0);   
			    } 
    			catch (InterruptedException e) 
			    {
			    }   
    				
    			// if exit was signaled this was client trigerred, so just quit
    			if (bExit == true)
			    {
    				Log.i(TAG, "Receive Transaction cancelled by User");
				return ReconBLE.ERR_READ;   // Tx cancelled -- treat as failure
			    }
		    }
       	    
		// and keep reading...
	    }
    }
}
