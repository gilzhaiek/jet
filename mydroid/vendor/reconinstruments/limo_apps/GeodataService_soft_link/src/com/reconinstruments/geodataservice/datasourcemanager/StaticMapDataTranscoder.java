package com.reconinstruments.geodataservice.datasourcemanager;

import java.util.ArrayList;

import android.content.Context;

import com.reconinstruments.geodataservice.SourcedDataContainer;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class StaticMapDataTranscoder extends DSMTranscoder
{
// constants 
	private final static String TAG = "StaticMapDataTranscoder";

// members 
	
	public    StaticMapDataTranscoder		mSelf = this;				// needed in AsyncTask
	protected DataSource					mDataSource = null;
	
// methods
	public StaticMapDataTranscoder(Capability.DataSources transducerID, DataSourceManager dsm, Context context, DevTestingState _devTestingState) {
		super(transducerID, dsm, context, _devTestingState);
	}

	public void setDevTestingState(DevTestingState _devTestingState) {  
		mDevTestingState = _devTestingState;
		if(mDataSource != null) mDataSource.setDevTestingState(_devTestingState);  // to propagate run-time DevTestingState changes to data source
	}
	
	public void init() throws Exception {
	}

	public void SourceData(SourcedDataContainer sourcedData) throws Exception {
		
	}
	
	public void AddCapabilities(ArrayList<Capability> capabilitiesArray) {
		
	}
	
	public void SetUserLocation(float longitude, float latitude) throws Exception {
		if(mDataSource != null) {
			mDataSource.SetUserLocation(longitude, latitude);
		}
	}
		
	public ArrayList<Integer> GetCachedDataStats() throws Exception {
		if(mDataSource != null) {
			return mDataSource.GetCachedDataStats();
		}
		return null;
	}
	
	public void phoneNetworkIsAvailable(boolean phoneNetworkIsAvailable) {
		mDataSource.phoneNetworkIsAvailable(phoneNetworkIsAvailable);
	}

//=============================================
// interfaces
    
	public interface IStaticMapDataTranscoderCallbacks {
		public void StaticMapDataTranscoderInitializeComplete(StaticMapDataTranscoder transcoder);
		public void StaticMapDataTranscoderInitializeError(Object component, String errorMsg) ;
		public void StaticMapDataTranscoderBackgroundSourcingDataComplete(SourcedDataContainer sourceDataContainer);
		public void StaticMapDataTranscoderBackgroundSourcingDataError(SourcedDataContainer sourceDataContainer, String errorMsg) ;
	}

//	public Serializable GetClosestResorts(ResortRequest resortRequest) {
//		return null;
//	}


}
