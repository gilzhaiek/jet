package com.reconinstruments.mapImages;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.mapImages.IMapDataService;
import com.reconinstruments.mapImages.mapdata.DataSourceManager;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.modlivemobile.dto.message.BuddyInfoMessage;
import com.reconinstruments.modlivemobile.dto.message.BuddyInfoMessage.BuddyInfo;
import com.reconinstruments.modlivemobile.dto.message.XMLMessage;

public class MapsService extends Service implements LocationListener {
	private static String TAG = "MAP_SERVICE";
	private static final boolean FAKE_USER_POSITION = true;		// turn on to override gps location and force location to fixed point
	private static final boolean FAKE_USER_MOVEMENT = false;    // turn on to randomly move around faked user position... requires FAKE_USER_POSITION = true
	private static final boolean FAKE_BUDDY = false;			// turn on to override buddy listening and force a single buddy at a set location
	boolean mFakeBuddyOnce = true;							// used with FAKE_BUDDY to only generate single fake buddy report after service starts
	
	double mFakeUserMovementX = 0.0;
	double mFakeUserMovementY = 0.0;

	private long lastBuddyUpdate = 0, lastBuddyPull = 0;
	private Bundle mBuddyListBundle = null;
	public DataSourceManager 			mStateListener = null;  // external object to report service state to

	private HashMap<Integer, BuddyInfo> mBuddyMap;

	private MapsManager mMapsManager			= null; 	
	private LocationManager mLocationManager	= null;
	
	public void NotifyBuddiesUpdated() {
		if(mStateListener != null) {
			mStateListener.BuddiesUpdated();
		}
	}					

   public class LocalBinder extends Binder {
        MapsService getService() {
            return MapsService.this;
        }
    }

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate()
	{
		Log.i(TAG, "onCreate()");
		registerReceiver(buddyInfoReceiver, new IntentFilter(XMLMessage.BUDDY_INFO_MESSAGE));

		mMapsManager = new MapsManager(this);		
	}

	@Override
	public int onStartCommand(Intent i, int flag, int startId)
	{
		mMapsManager.onStart();

		if(mLocationManager == null) {
			//			Log.i(TAG, "starting LocationManager GPS service");
			mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, this);
		}    	
		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		Log.i(TAG, "onDestroy()");
		super.onDestroy();
		this.unregisterReceiver(buddyInfoReceiver);
	}	

	/**
	 * The IMapsService is defined through IDL (aidl)
	 */
	private final IMapDataService.Stub mBinder = new IMapDataService.Stub()
	{
		public Bundle getResortGraphicObjects()
		{
			return mMapsManager.GetMapBundle(); 
		}

		public Bundle getListOfBuddies() 
		{
			if(FAKE_BUDDY) {
				if(mFakeBuddyOnce) {
					mFakeBuddyOnce = false;
					fakeBuddy();
				}
			}
			if(lastBuddyPull < lastBuddyUpdate)
			{
				lastBuddyPull = lastBuddyUpdate;
				return mBuddyListBundle;
			}
			return null;
		}

		public Bundle getResortID() throws RemoteException {
			return mMapsManager.GetResortIDBundle();
		}

		public void updateLocation (Bundle bundle) {
			//onLocationChanged((Location)bundle.getParcelable("Location"));
		}		

		public void debugForceLocation (Location location) {
			onLocationChanged(location);
		}		

		public void registerStateListener(DataSourceManager dsm) {
			mStateListener = dsm;
			mMapsManager.registerStateListener(dsm);
		}
		
		public boolean hasData() {	
			return mMapsManager.mDataAvailable;	// will be true if have decoded one location update
		}
		/*public MapsService getMapsService() {
			return MapsService.this;
		}*/
		public void startGenerateImage() {
			if (mMapsManager != null)
				mMapsManager.StartGenerateResortImgs();
		}
		
		public void stopGenerateImage() {
			if (mMapsManager != null)
				mMapsManager.StopGenerateResortImgs();
		}

	};


	// Patrick or Chris:
	// 0) Put BuddyInfoReceiver here: once the buddies are received 
	// 1) update the array list of all existing buddies.mBuddyList
	// TODO 2) Construct the bundle using constructBundleFromBuddyInfo
	// TODO 3) save the output to mBuddyListBundle;
	// --------
	// ReconRadar reads mBuddyListBundle;
	// If it is the first time construct the texture objects
	// else: Just move the texture objects around.
	//END to do:

	private static final Bundle BuddyInfoToBundle (BuddyInfoMessage.BuddyInfo bi) {
		Bundle b = new Bundle();
		b.putInt("id", bi.localId);
		b.putString("email", bi.email);
		b.putString("name", bi.name);
		b.putParcelable("location",bi.location);
		return b;

	}
	private static final Bundle constructBundleFromBuddyInfo(HashMap<Integer, BuddyInfoMessage.BuddyInfo> buddyMap) {

		ArrayList<Bundle> blist = new ArrayList<Bundle>();


		for (BuddyInfo bi : buddyMap.values()) {
			if (bi != null){
				Bundle bib = BuddyInfoToBundle(bi);
				blist.add(bib);	
			}
		}
		Bundle b = new Bundle();
		b.putParcelableArrayList("BuddyInfoBundle",blist);
		return b;
	}

	public void fakeBuddy()
	{
		if(mBuddyMap == null)
		{
			BuddyInfo fakeBuddy  = new BuddyInfo(99,"faker",50.085902,-122.965105);	// fake buddy for now..

			mBuddyMap = new HashMap<Integer, BuddyInfoMessage.BuddyInfo>();
			mBuddyMap.clear();
			mBuddyMap.put(fakeBuddy.localId, fakeBuddy);

			mBuddyListBundle = constructBundleFromBuddyInfo(mBuddyMap);
		}
		lastBuddyUpdate = System.currentTimeMillis();		
	}

	BroadcastReceiver buddyInfoReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Bundle bundle = intent.getExtras();
			if(bundle!=null){
				byte[] bytes = bundle.getByteArray("message");
				HUDConnectivityMessage cMsg = new HUDConnectivityMessage(bytes);
				String buddyInfo = new String(cMsg.getData());

				BuddyInfoMessage message = new BuddyInfoMessage();

				@SuppressWarnings("unchecked")
				ArrayList<BuddyInfo> buddyList = (ArrayList<BuddyInfo>) message.parse(buddyInfo);

				//			Log.i(TAG, "onReceive()");
				//			Log.i(TAG, "buddy count: " + buddyList.size());
				int index = 0;
				//			for (BuddyInfo buddy : buddyList)
				//			{
				//				Log.v(TAG, "buddy[" + (index++) + "]: id=" + buddy.localId + ", name=" + buddy.name + ", email=" + buddy.email + 
				//						   ", location=" + buddy.location.getLatitude() + " " + buddy.location.getLongitude());
				//			}

				if(mBuddyMap == null)
				{
					mBuddyMap = new HashMap<Integer, BuddyInfoMessage.BuddyInfo>();
				}

				mBuddyMap.clear();

				for (BuddyInfo buddy : buddyList)
				{ 
					mBuddyMap.put(buddy.localId, buddy);
				}			

				mBuddyListBundle = constructBundleFromBuddyInfo(mBuddyMap);
				lastBuddyUpdate = System.currentTimeMillis();	

				NotifyBuddiesUpdated();
			}
		}
	};

	public void onLocationChanged(Location location) {
		//		Log.d(TAG, "onLocationChanged");
		if(mMapsManager != null) {
			if(FAKE_USER_POSITION) {
				if(FAKE_USER_MOVEMENT) { 
					mFakeUserMovementX -=  0.0005;
					mFakeUserMovementY +=  0.000;
				}
				else {
					mFakeUserMovementX = 0.0;
					mFakeUserMovementY = 0.0;
				}
				location.setLongitude(-122.963705 + mFakeUserMovementX);	// fake user location for now..
				location.setLatitude(50.085702 + mFakeUserMovementY);
			}
			mMapsManager.onLocationChanged(location);
		}
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	
}
