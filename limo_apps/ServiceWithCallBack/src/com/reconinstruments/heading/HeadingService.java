package com.reconinstruments.heading;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.modservice.ReconMODServiceMessage;

public class HeadingService extends Service {
	public static final String TAG = "HeadingService";
	final RemoteCallbackList<ICallback> mCallbacks = new RemoteCallbackList<ICallback>();

	private HeadLocation mHeadLocation;
	
	private MODServiceConnection mMODConnection;
	private Messenger mMODConnectionMessenger;

	@Override
	public IBinder onBind (Intent intent) {
		return binder;
	}

	@Override
	public int onStartCommand(Intent i, int flag, int startId) {
		Log.v(TAG,"onStartCommand");
		return START_STICKY;
	}

	@Override
	public void onCreate()     {
		super.onCreate();
		Log.v(TAG,"onCreate");
		mHeadLocation = new HeadLocation(this);
		mHeadLocation.onResume();
		mMODConnection = new MODServiceConnection(this);
		mMODConnectionMessenger = new Messenger(new MODServiceHandler());
		mMODConnection.addReceiver(mMODConnectionMessenger);
		mMODConnection.doBindService();
	}

	@Override
	public void onDestroy()     {
		mHeadLocation.onPause();
		if (mMODConnection != null) {
		    mMODConnection.doUnBindService();
		}
		super.onDestroy();
		    
	}


	private final IHeadingService.Stub binder = new IHeadingService.Stub() {
		public void register(ICallback cb) {
			if(cb!=null){
				Log.d(TAG, "registerCallBack registering");
				mCallbacks.register(cb);
			}
		}
		public void unregister(ICallback cb) {
			if(cb!=null){
				Log.d(TAG, "unregister callback");
				mCallbacks.unregister(cb);
			}
		}
	};

	public synchronized void  callOnLocationHeadingChanged(Bundle b) {
		try {
			int N = mCallbacks.beginBroadcast();
			//Log.d("Heading service", "num clients registered = " + N);
			for (int i = 0; i<N; i++) {
				mCallbacks.getBroadcastItem(i).onLocationHeadingChanged(b);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try{
				mCallbacks.finishBroadcast();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	class MODServiceHandler extends Handler {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
			case ReconMODServiceMessage.MSG_RESULT:
				if (msg.arg1 == ReconMODServiceMessage.MSG_GET_FULL_INFO_BUNDLE) {
					Bundle fullInfoBundle = msg.getData();
					
					Bundle altBundle = (Bundle) fullInfoBundle.get("ALTITUDE_BUNDLE");
					float alt = altBundle.getFloat("Alt");
					mHeadLocation.updateAltitude(alt);
				}
				break;

			default:
				super.handleMessage(msg);
			}
		}
    }
}
