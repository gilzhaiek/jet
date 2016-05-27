package com.reconinstruments.geodataservice.datasourcemanager;

import java.util.ArrayList;

import android.content.Context;

import com.reconinstruments.geodataservice.SourcedDataContainer;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortInfoResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortRequest;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class DSMTranscoder 
{
// constants 
	private final static String TAG = "StaticMapDataTranscoder";
	public enum DataTranscoderState {
		INITIALIZING,
		READY
	}

// members 
	
	public    Capability.DataSources 		mSourceID;
	protected static Context 				mContext = null;
	protected DataSourceManager 			mOwner = null;
	public 	  DataTranscoderState  			mState = null;
	protected DevTestingState   			mDevTestingState = null;
// methods
	
	public DSMTranscoder(Capability.DataSources transducerID, DataSourceManager dsm, Context context, DevTestingState _devTestingState) {
		mState = DataTranscoderState.INITIALIZING;
		mSourceID = transducerID;
		mOwner = dsm;
		mContext = context;
		mDevTestingState = _devTestingState;
	}

	public void setDevTestingState(DevTestingState _devTestingState) {  
		mDevTestingState = _devTestingState;
	}
	
	public void init() throws Exception {
	}

	public void SourceData(SourcedDataContainer sourcedData) throws Exception {
		
	}
	
	public void AddCapabilities(ArrayList<Capability> capabilitiesArray) {
		
	}
	
	public void phoneNetworkIsAvailable(boolean phoneNetworkIsAvailable) {
	}
	
//=============================================
// interfaces
    
	public interface IStaticMapDataTranscoderCallbacks {
		public void StaticMapDataTranscoderInitializeComplete(DSMTranscoder transcoder);
		public void StaticMapDataTranscoderInitializeError(Object component, String errorMsg) ;
//		public void StaticMapDataTranscoderBackgroundSourcingDataComplete(SourcedDataContainer sourceDataContainer);
//		public void StaticMapDataTranscoderBackgroundSourcingDataError(SourcedDataContainer sourceDataContainer, String errorMsg) ;
	}

	public ArrayList<ResortInfoResponse> GetClosestResorts(ResortRequest resortRequest) {
		return new ArrayList<ResortInfoResponse>();
	}


}
