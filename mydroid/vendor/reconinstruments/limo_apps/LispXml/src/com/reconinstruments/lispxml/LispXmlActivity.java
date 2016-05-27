package com.reconinstruments.lispxml;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.content.ServiceConnection;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import com.reconinstruments.lispxml.LispXmlService.LocalBinder;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import android.os.Environment;


public class LispXmlActivity extends Activity
{
    public static final String TAG = "LispXmlActivity";
    LispXmlService mService;
    boolean mBound = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

    }
    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, LispXmlService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onStop();
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

	    @Override
	    public void onServiceConnected(ComponentName className,
					   IBinder service) {
		// We've bound to LocalService, cast the IBinder and get LocalService instance
		LocalBinder binder = (LocalBinder) service;
		mService = binder.getService();
		mBound = true;
	    }
	    
	    @Override
	    public void onServiceDisconnected(ComponentName arg0) {
		mBound = false;
	    }
	};

    /**
     * Helper function to read and write the contents of a file into a string.
     * 
     * @param f
     *            a <code>File</code> value
     * @return a <code>String</code> value
     * @exception java.io.IOException
     *                if an error occurs
     */
    public static String readFileAsString(File f) throws java.io.IOException {
	return readFileAsString(f.getAbsolutePath());
    }

    /**
     * Helper function to read and write the contents of a file into a string.
     * 
     * @param filePath
     *            a <code>String</code> value
     * @return a <code>String</code> value
     * @exception java.io.IOException
     *                if an error occurs
     */
    public static String readFileAsString(String filePath)
	throws java.io.IOException {
	BufferedReader reader = new BufferedReader(new FileReader(filePath));
	String line, results = "";
	while ((line = reader.readLine()) != null) {
	    results += line;
	}
	reader.close();
	//Log.d(TAG, "results=" + results);
	return results;
    }
    

}
