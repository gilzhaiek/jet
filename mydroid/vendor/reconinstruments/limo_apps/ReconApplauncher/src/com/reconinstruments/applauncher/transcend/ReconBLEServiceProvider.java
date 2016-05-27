package com.reconinstruments.applauncher.transcend;
import android.util.Base64;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.reconinstruments.bletest.IBLEService;
import com.reconinstruments.applauncher.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReconBLEServiceProvider {

    private static final String TAG = "ReconBLEServiceProvider";
    public final static int INVALID_TEMPERATURE = -100;

    public int mTemperature;
    public int mStatus;
    private byte[] mBytes = null;
    private byte[] mListOfRemotes = null;
    private int mNumRemotes;
    private ReconTranscendService mRTS;
    public boolean mPastPairingString = false;
    private boolean mBlinkStatus = true;
    private NotificationManager mNotificationManager;

    
    //////////////////////////////////////////////////////
    // aidl service connection.
    /////////////////////////////////////////////////////
    private IBLEService bleService = null;
    private BLEServiceConnection bleServiceConnection;

    private void initService() {
        if( bleServiceConnection == null ) {
            bleServiceConnection = new BLEServiceConnection();
            Intent i = new Intent("RECON_BLE_TEST_SERVICE");
            mRTS.bindService( i, bleServiceConnection, Context.BIND_AUTO_CREATE);
            Log.d( TAG, "bindService()" );
        } 
    }

    private void releaseService() {
	if( bleServiceConnection != null ) {
	    mRTS.unbindService( bleServiceConnection );	  
	    bleServiceConnection = null;
	    Log.d( TAG, "unbindService()" );
        }
    }

    class BLEServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, 
				       IBinder boundService ) {
            bleService = IBLEService.Stub.asInterface((IBinder)boundService);
            Log.d(TAG,"onServiceConnected" );
	    try{
		if (bleService != null) {
		    mTemperature = bleService.getTemperature();
		}
	    }
	    catch (RemoteException e) {
		e.printStackTrace();
	    }
        }

        public void onServiceDisconnected(ComponentName className) {
            bleService = null;
            Log.d( TAG,"onServiceDisconnected" );
        }
    };
    /////////////////// End of aidl shit///////////////////////


    public ReconBLEServiceProvider(boolean parsingStat, ReconTranscendService rts) {
	mPastPairingString = parsingStat;
	mRTS= rts;
		
	String ns = Context.NOTIFICATION_SERVICE;
	mNotificationManager = (NotificationManager) mRTS.getSystemService(ns);

	//JET FIXME: 
	//initService();
	
    }

    public byte[] updateBLE() {
	try{
	    if (bleService != null) {
		mTemperature = bleService.getTemperature();
	    }
	}
	catch (RemoteException e) {
	    e.printStackTrace();
	}
	return null;
    }

    public boolean pushIncrementalRibThroughBLE (byte theType, byte[] bytes) {
	//Log.v(TAG,"pushIncrementalRibThroughBLE");
	//First make sure that it is ios and connected
	try{
	    if (bleService != null) {
		if (bleService.getIsMaster() || !bleService.isConnected()) {
		    //Log.v(TAG,"ble is not connected or is master");
		    //DEBUG
		    return false;
		}
		//Concatenating the bytes: The size is really small so
		//we just use a a regular for loop
		byte [] totalByteArray = new byte[1 + bytes.length];
		totalByteArray[0] =theType;
		for (int i = 1;i<=bytes.length;i++) {
		    totalByteArray[i] = bytes[i-1];
		}
		//Log.v(TAG,"bleService.pushIncrementalRib(totalByteArray);");
		bleService.pushIncrementalRib(Base64.encodeToString(totalByteArray,Base64.DEFAULT));
		return true;
	    } 
	}
	catch (RemoteException e) {
	    e.printStackTrace();
	}
	return false;
    }
}
