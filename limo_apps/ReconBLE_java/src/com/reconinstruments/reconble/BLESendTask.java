package com.reconinstruments.reconble;

import java.io.IOException;
import java.io.InputStream;

import android.os.Bundle;
import android.util.Log;

/* Asynchronous Send Task. Transmits chunks of data from InputStream created by
 * caller (ReconBLE)
 * */
public class BLESendTask extends BLETask
{
    private static final String TAG = BLESendTask.class.getSimpleName();
	 
    // IO buffer, actual send size might be less
    protected byte []  sendBuffer = new byte [ReconBLE.BUFFER_IO_SIZE];  

    // input stream -- file or buffer, allocated by caller. We don't care at this level
    // what is in, we simply send it
    public InputStream        m_input      = null;  
	
    // thread meat -- single Tx transaction
    // We don't do any control checks i.e. connection -- only strict Tx protocol
    // We can't be pre-empted by other I/O transactions either, otherwise we will fail
    // Details of this management -- TBD; likely concept is some sort of system-wide singleton
    // (Service?) that keeps set of IO channels & manages Transactions
    public void run ()
    {
	return;
    }
    
    // internal helper that closes input stream on exit. Hyper anal java throws exceptions
    private void CloseInput ()
    {
    	try 
	    {
		m_input.close();
	    } 
    	catch (IOException e)
	    {
		Log.w(TAG, "Exception thrown while closing Input Stream: " + e.getMessage() + " but we don't really care");
	    }
    }
   
    
}