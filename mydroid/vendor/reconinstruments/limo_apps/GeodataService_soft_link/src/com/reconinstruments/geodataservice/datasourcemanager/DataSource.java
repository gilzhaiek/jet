package com.reconinstruments.geodataservice.datasourcemanager;

import java.util.ArrayList;

import android.content.Context;

import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortInfoResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortRequest;

public class DataSource
{
// constants 
	private final static String TAG = "DataSource";
	
// members 
	protected DevTestingState   mDevTestingState = null;
	protected StaticMapDataTranscoder		mOwner = null;
	protected static Context 	mContext = null;
	protected boolean mPhoneNetworkIsAvailable = false;

// methods
	protected DataSource(StaticMapDataTranscoder transcoder, Context context, DevTestingState _devTestingState) {
		mDevTestingState = _devTestingState;
		mOwner = transcoder;
		mContext = context;
	}
	
	public void setDevTestingState(DevTestingState _devTestingState) {  // to propagate run-time changes to object
		mDevTestingState = _devTestingState;
	}
	
	public String init() {
		// override this
		return null;
	}

	public ArrayList<Integer> GetCachedDataStats() throws Exception {
		return null;
	}

	public void SetUserLocation(float longitude, float latitude)  throws Exception {
	}

	public ArrayList<ResortInfoResponse> GetClosestResorts(ResortRequest resortRequest) {
		return new ArrayList<ResortInfoResponse>();
	}

	public void phoneNetworkIsAvailable(boolean phoneNetworkIsAvailable) {
		mPhoneNetworkIsAvailable = phoneNetworkIsAvailable;
		
	}


}
