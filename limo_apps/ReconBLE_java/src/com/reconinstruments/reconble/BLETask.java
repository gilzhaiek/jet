package com.reconinstruments.reconble;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;

// factor common functionality for async tasks ReconBLE package supports
public class BLETask extends Thread
{
	// Mutex for async polling
	protected class Monitor {}
	
	protected Monitor mMonitor = new Monitor();
	boolean           bExit    = false;
	
	protected Handler                mNotifyHandler;
	protected int                    mSession;    // session ID

	// Exit signal
	public void NotifyExit ()
	{
		synchronized (mMonitor)
		{
			bExit = true;
			mMonitor.notify();
		}
	}
	
	// internal helper to send client notification
    synchronized protected void SendStatus (Bundle b, String key, int value)
    {
    	b.putInt(key,  value);
	//	Log.v("BLETask","key is" + key+" value is "+value);
    	Message message   = Message.obtain(mNotifyHandler);
    	message.setData(b);
    	message.sendToTarget();
    }
}
