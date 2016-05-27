package com.reconinstruments.geodataservice.datasourcemanager.DynamicData;

import java.util.ArrayList;

import android.content.Context;

import com.reconinstruments.geodataservice.datasourcemanager.DSMTranscoder;
import com.reconinstruments.geodataservice.datasourcemanager.DataSourceManager;
import com.reconinstruments.geodataservice.datasourcemanager.StaticMapDataTranscoder;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;

public class DynamicDataTranscoder extends DSMTranscoder {
	// archtype class for all dynamic data transcoders
// constants
	private final static String TAG = "DynamicDataTranscoder";

// members

	
// constructors
	public DynamicDataTranscoder(Capability.DataSources transducerID, DataSourceManager dsm, Context context, DevTestingState _devTestingState) {
		super(transducerID, dsm, context, _devTestingState);
	}

	public void setDevTestingState(DevTestingState _devTestingState) {  
		mDevTestingState = _devTestingState;
	}
	
	public void init() throws Exception {
	}

	public void AddCapabilities(ArrayList<Capability> capabilitiesArray) {
	
	}
	
	public void RegisterForCapability() {
		
	}
	
	public void UnregisterForCapability() {
		
	}
	
//=============================================
// interfaces
    
	public interface IStaticMapDataTranscoderCallbacks {
		public void StaticMapDataTranscoderInitializeComplete(StaticMapDataTranscoder transcoder);
		public void StaticMapDataTranscoderInitializeError(Object component, String errorMsg) ;
	}


// methods

}
