package com.reconinstruments.geodataservice.datasourcemanager.DynamicData;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.reconinstruments.geodataservice.datasourcemanager.DataSourceManager;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.DynamicAttributeDataCapability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;

public class UserTranscoder extends DynamicDataTranscoder implements LocationListener		// transcodes data from RECON buddy server into geodata server objects
{
// constants 
	private final static String TAG = "UserTranscoder";
	public  final  static String 	GEODATASERVICE_BROADCAST_NEW_DYNAMIC_USER_DATA = "com.reconinstruments.geodataservice.new_dynamic_user_data";
	
// members 
	int							mNumRegisteredClients = 0;
	PointXY						mUserLocation = null;
	float						mUserHeading = 0.0f;
	protected static Context 	mParentContext = null;
	
// methods
	public UserTranscoder(DataSourceManager dsm, Context context,DevTestingState _devTestingState) {
		super(Capability.DataSources.DEVICE_USER_POSITION_SENSORS, dsm, context, _devTestingState); // set up mSourceID and other variables
		mParentContext = context;
	}
	
	public void init() throws Exception {
		super.init();
		

//		if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
//			 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.USER_TRANSCODER_CREATE_ERROR.ordinal()) == 1) ) {
//				throw new Exception ("Forced USERTRANSCODER creation error.");
//		}
		
	}

//============================================
//
	@Override
	public void AddCapabilities(ArrayList<Capability> capabilitiesArray) {
		DynamicAttributeDataCapability newDADCap = new DynamicAttributeDataCapability(Capability.CapabilityTypes.DYNAMIC_USER_POSITION, mSourceID);
		capabilitiesArray.add(newDADCap);
	}

	@Override
	public void RegisterForCapability() {
		mNumRegisteredClients ++;
	}
	
	@Override
	public void UnregisterForCapability() {
		if(mNumRegisteredClients > 0) {
			mNumRegisteredClients --;
		}
	}
	
	public void SetLocation(float longitude, float latitude) {
		//if(signifcant change) {
			mUserLocation = new PointXY(longitude, latitude);
			broadcastUserPosition();
		//}
	}

	public void SetHeading(float heading) {
		//if(signifcant change) {
			mUserHeading = heading;
			broadcastUserPosition();
		//}
	}

	public void broadcastUserPosition() {
 		if(mNumRegisteredClients > 0) {    	// broadcast new user info to registered clients
 		 	Intent newIntent = new Intent();
 		 	newIntent.setAction(GEODATASERVICE_BROADCAST_NEW_DYNAMIC_USER_DATA);
 		 	newIntent.addCategory(Intent.CATEGORY_DEFAULT);
 		 	newIntent.putExtra("Location", mUserLocation);			// use getIntent().getSerializableExtra("Location");  to retrieve in listener
 		 	newIntent.putExtra("Heading", mUserHeading);			// use getIntent().getExtra("Heading");  to retrieve in listener
			mParentContext.sendBroadcast(newIntent); 
 		}
	}


//=============== Location Services protocol handlers

	public void onLocationChanged(Location location) {
				//Log.d(TAG, "onLocationChanged");
			System.out.println("---ON LOCATION CHANGED");
		
		if(!(mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
		    mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.SERVICE_NO_GPS.ordinal()) == 1) ) {	 // if not blocked by testing configuration
			
			// TODO check if position has changed significantly
// 			if(significantChange) {
				mUserLocation = new PointXY((float)location.getLongitude(), (float)location.getLatitude());
				broadcastUserPosition();
				// need to talk to service and adjust mState...
//			}
		}

	}

	public void onProviderDisabled(String provider) {

		// need to talk to service and adjust mState...
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}

}
