package com.reconinstruments.messagecenter.tester;

import android.app.Activity;
import android.os.Bundle;
import com.reconinstruments.messagecenter.ReconMessageAPI.ReconNotification;
import com.reconinstruments.messagecenter.ReconMessageAPI;

/**
 * Describe class <code>MessageCenterTester</code> here.
 *
 * Test class to test for adding updatable message to message center
 *
 * @author <a href="mailto:ali@reconinstruments.com">ali</a>
 * @version 1.0
 */
public class MessageCenterTester extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    public void onResume(){
	super.onResume();
	ReconNotification notification =
	    new ReconNotification(this, "com.reconinstruments.alitest",
				  "Ali test"+System.currentTimeMillis()/1000,
				  R.drawable.ic_launcher, "AliTest",
				  "AliTest"+System.currentTimeMillis()/1000,
				  R.drawable.ic_launcher, "" +
				  System.currentTimeMillis()/1000 % 100);
	ReconMessageAPI.postNotification(notification, true, false);
	ReconNotification notification2 =
	    new ReconNotification(this, "com.reconinstruments.stats",
				  "STATS MILESTONE",
				  R.drawable.ic_launcher, "AllTimeMaxSpeed",
				  "All time maxspeed", R
				  .drawable.ic_launcher,""+ System.currentTimeMillis() % 100);
	ReconMessageAPI.postNotification(notification2, true, true);

    }
    
}
