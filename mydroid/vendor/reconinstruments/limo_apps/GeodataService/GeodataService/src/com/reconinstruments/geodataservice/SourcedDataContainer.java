package com.reconinstruments.geodataservice;

import java.util.ArrayList;
import java.util.Date;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;

public class SourcedDataContainer {
// constants
	private final static String TAG = "SourcedDataContainer";
//	public enum SourcedDataStates {
//		UNDEFINED,
//		INITIALIZED,
//		LOADING,
//		SYSTEM_PRELOADING,
//		LOADED,
//		ERROR
//	}

// members
//	public SourcedDataStates		mState = SourcedDataStates.UNDEFINED;
	public Date						mCreatedTimeStamp = null;
	public Date						mLoadStartTimeStamp = null;
	public Date						mLoadEndTimeStamp = null;
	public Date						mLastAccessedTimeStamp = null;
	public int						mRequestedGeoTileIndex = 0;
//	public GeoRegion				mLoadedGeoRegion = null;
	public ArrayList<WorldObject>  	mRetrievedWorldObjects = null;
	public ArrayList<WorldObject.WorldObjectTypes> 		mDataTypes = null;	// requested data types to load
	public Capability.DataSources mSource = null;		// source/transducer to use for this data
	public TileDataRequest 				mParentDataRequest;
	public String	 				mParentRequestID;
	
// constructors
	
	public SourcedDataContainer(Capability.DataSources source) {
//		mState = SourcedDataStates.INITIALIZED;
		mCreatedTimeStamp = new Date();
		mSource = source;
		mParentDataRequest = null;
		mParentRequestID = "";	
		mRequestedGeoTileIndex = -1;
//		mLoadedGeoRegion = mRequestedGeoRegion;		
		mDataTypes = new ArrayList<WorldObject.WorldObjectTypes>();
	}
	
	public SourcedDataContainer(Capability.DataSources source, int _geoTileIndex, TileDataRequest parent) {
//		mState = SourcedDataStates.INITIALIZED;
		mCreatedTimeStamp = new Date();
		mSource = source;
		mParentDataRequest = parent;				// Note, this released (set to null) after data loaded or error with loading so parent DataRequest can be released while this obj lives on in GeodataService cache
		mParentRequestID = parent.mRequestID;	
		mRequestedGeoTileIndex = _geoTileIndex;
//		mLoadedGeoRegion = mRequestedGeoRegion;		// set equal here but may be changed in DSM transcoders...
		mDataTypes = new ArrayList<WorldObject.WorldObjectTypes>();
	}
	
	
	public SourcedDataContainer CreateCopyFor(TileDataRequest parent, int _geoTileIndex) {
		SourcedDataContainer newSDC = new SourcedDataContainer(mSource);
		newSDC.mCreatedTimeStamp = new Date();
		newSDC.mParentDataRequest = parent;			// Note, this released (set to null) after data loaded or error with loading so parent DataRequest can be released while this obj lives on in GeodataService cache
		newSDC.mParentRequestID = parent.mRequestID;	
		newSDC.mRequestedGeoTileIndex = _geoTileIndex;
//		newSDC.mRequestedGeoRegion = newSDC.mRequestedGeoRegion;  // set equal here but may be changed in DSM transcoders...
		
		for(WorldObject.WorldObjectTypes woType : mDataTypes) {
			newSDC.mDataTypes.add(woType);
		}
		return newSDC;
	}
	
	
// methods
	public void AddDataType(WorldObject.WorldObjectTypes worldObjectType) {
		if(mDataTypes == null) {
			mDataTypes = new ArrayList<WorldObject.WorldObjectTypes>();
		}
		mDataTypes.add(worldObjectType);
	}


	
		
}
