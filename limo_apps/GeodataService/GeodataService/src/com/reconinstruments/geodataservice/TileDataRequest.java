package com.reconinstruments.geodataservice;

import java.util.ArrayList;
import java.util.Date;

public class TileDataRequest {
// constants
	private final static String TAG = "TileDataRequest";
//	public enum DataRequestStates {
//		UNDEFINED,
//		INITIALIZED,
//		LOADING,
//		SYSTEM_PRELOADING,
//		LOADED,
//		ERROR
//	}

// members
//	public DataRequestStates mState = DataRequestStates.UNDEFINED;
	String				    mClientID = "";		// supplied by client, used in broadcast notifications after asynch data loads
	String					mRequestID = "";	// unique ID for client, used to coordinate asynch data loads
	Date					mCreatedTimeStamp = null;
	Date					mLoadStartTimeStamp = null;
	Date					mLoadEndTimeStamp = null;
	Date					mLastAccessedTimeStamp = null;
	public int				mRequestedGeoTileIndex = -1;
//	public GeoRegion		mLoadedGeoRegion = null;
	public MapComposition   mMapComposition = null;
	public ArrayList<SourcedDataContainer>  mSourcedDataContainers = null;
	public ArrayList<SourcedDataContainer>  mStillToLoad = null;
	
// constructors
	public TileDataRequest(int _geoTileIndex, MapComposition mapComposition) {
		mClientID = mapComposition.mClientID;
		mRequestID = new Date().toString();
		mCreatedTimeStamp = new Date();
		mRequestedGeoTileIndex = _geoTileIndex;
//		mLoadedGeoRegion = mRequestedGeoRegion;
		mMapComposition = mapComposition;

//		Log.e(TAG,"DataRequest1");
		mSourcedDataContainers = new ArrayList<SourcedDataContainer>();
		mStillToLoad = new ArrayList<SourcedDataContainer>();
		for(SourcedDataContainer templateSDC : mapComposition.mSourcedDataContainers) {
			SourcedDataContainer newSDC = templateSDC.CreateCopyFor(this, _geoTileIndex);
			mSourcedDataContainers.add(newSDC);	// 
			mStillToLoad.add(newSDC);	// used by DSM as a checklist to determine if DataRequest has been fully sourced
		}
//		Log.e(TAG,"DataRequest5");
	}
	
// methods
	public boolean IsLoaded() {
		return (mStillToLoad != null && mStillToLoad.size() == 0) ? true : false;
	}

//	public boolean Contains(DataRequest newDataRequest) {
//		// pretty simple tests, but should work well give clients continually ask for the same type of data and the Transducers generally return larger LoadedGeoRegions then the RequestedGeoRegions.
//		
//		if(mLoadedGeoRegion.mBoundingBox.Contains(newDataRequest.mRequestedGeoRegion.mBoundingBox)) return true;	
//		
//		return false;
//	}
//	
	public void Clean() {
		for(SourcedDataContainer sourcedDataContainer : mSourcedDataContainers) {
			sourcedDataContainer.mRetrievedWorldObjects = null;
			sourcedDataContainer.mDataTypes = null;	
		}
		mSourcedDataContainers = null;

	}

}
