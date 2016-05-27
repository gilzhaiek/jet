package com.reconinstruments.heading;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

// Heading manager connects to heading service
public class HeadingManager {
    Context mContext;//A copy of application context in helper class
    HeadingListener mHeadingListener;
    public static final String TAG = "HeadingManager";
    public HeadingManager (Context c, HeadingListener hl){
	mContext = c;
	mHeadingListener = hl;
    }  

    //////////////////////////////////////////////////////
    // aidl service connection.
    /////////////////////////////////////////////////////
    private IHeadingService headingService;
    private HeadingServiceConnection headingServiceConnection;

    public void initService() {
	if (headingServiceConnection == null) {
	    headingServiceConnection = new HeadingServiceConnection();
	    Intent i = new Intent("RECON_HEADING_SERVICE");
	    mContext.bindService(i, headingServiceConnection, Context.BIND_AUTO_CREATE);
	}
    }

    public void releaseService() {
	//unregister:
	try {	
	    if (headingService != null) {
		headingService.unregister(callback);
	    }
	}
	catch (RemoteException e) {
	    e.printStackTrace();
	}
	//unbind:
	if (headingServiceConnection != null){
	    mContext.unbindService(headingServiceConnection);
	    headingServiceConnection = null;
	    Log.d(TAG, "unbindService()");
	}
    }

    class HeadingServiceConnection implements ServiceConnection {
	public void onServiceConnected(ComponentName className, IBinder boundService) {
	    Log.d(TAG, "onServiceConnected");
	    headingService = IHeadingService.Stub.asInterface((IBinder) boundService);
	    try	{
		if (headingService != null) {
		    //register:
		    headingService.register(callback);
		}
	    }
	    catch (RemoteException e)	{
		e.printStackTrace();
	    }
	}
	public void onServiceDisconnected(ComponentName className){ 
	    headingService = null;
	    Log.d(TAG, "onServiceDisconnected");
	}
    };
    //////////////////// End of aidl shit///////////////////////

    ///////////////////// Call back shit ///////////////////////
    private ICallback.Stub callback = new ICallback.Stub() {
	    @Override
	    public void onLocationHeadingChanged(Bundle b) throws RemoteException {
		HeadingEvent he = new HeadingEvent(b);
		mHeadingListener.onHeadingChanged(he);
	    }
	};
    ///////////////  End of Call back shit /////////////////////

    public boolean isCompassCalibrated() {
	return (Settings.Secure.getInt(mContext.getContentResolver(), "hasWrittenMagOffsetsV2", 0)==1);
    }
	

}
