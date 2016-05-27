package com.rxnetworks.sntp;

import android.os.SystemClock;	// added for SystemClock.elapsedRealtime() 	
import android.util.Log;

public class SNTPTime {
	private String host;
	//private int port;
	public long currentTime = 0; /* in milliseconds */
	public long clockOffset = 0; /* in milliseconds */

	private static final String TAG = "RXNServices.SNTPTime";
   
 	public SNTPTime(String host, int port) {
		this.host = host;
		//this.port = port;
	}

	public void getTime()
	{
		 int timeout = 3000;
		 SntpClient client = new SntpClient();
		 if (client.requestTime(host, timeout)) {
			 currentTime = (client.getNtpTime() + SystemClock.elapsedRealtime() - client.getNtpTimeReference()) / 1000;
		 }
	
		 Log.i(TAG, "SNTP currentTime is " + currentTime);
	}
}
