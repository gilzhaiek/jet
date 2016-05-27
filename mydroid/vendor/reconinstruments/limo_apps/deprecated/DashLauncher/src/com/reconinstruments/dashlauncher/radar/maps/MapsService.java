package com.reconinstruments.dashlauncher.radar.maps;

import java.util.ArrayList;
import java.util.HashMap;

import com.reconinstruments.modlivemobile.dto.message.BuddyInfoMessage;
import com.reconinstruments.modlivemobile.dto.message.XMLMessage;
import com.reconinstruments.modlivemobile.dto.message.BuddyInfoMessage.BuddyInfo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class MapsService extends Service implements LocationListener {
	private static String TAG = "MAP_SERVICE";
	
	private long lastBuddyUpdate = 0, lastBuddyPull = 0;
	private Bundle mBuddyListBundle = null;
	
	private HashMap<Integer, BuddyInfo> mBuddyMap;
	
	private MapsManager mMapsManager			= null; 	
	private LocationManager mLocationManager	= null;

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
	private final IMapsService.Stub mBinder = new IMapsService.Stub()
	{
		public Bundle getResortGraphicObjects()
		{
			return mMapsManager.GetMapBundle(); 
		}

		public Bundle getListOfBuddies() 
		{
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

	BroadcastReceiver buddyInfoReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String buddyInfo = intent.getStringExtra("message");

			BuddyInfoMessage message = new BuddyInfoMessage();

			@SuppressWarnings("unchecked")
			ArrayList<BuddyInfo> buddyList = (ArrayList<BuddyInfo>) message.parse(buddyInfo);

			Log.i(TAG, "onReceive()");
			Log.i(TAG, "buddy count: " + buddyList.size());
			int index = 0;
			for (BuddyInfo buddy : buddyList)
			{
				Log.i(TAG, "buddy[" + (index++) + "]: id=" + buddy.localId + ", name=" + buddy.name + ", email=" + buddy.email + 
						   ", location=" + buddy.location.getLatitude() + " " + buddy.location.getLongitude());
			}
			
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
		}
	};

	public void onLocationChanged(Location location) {
		if(mMapsManager != null) {
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
