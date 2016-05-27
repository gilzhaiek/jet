package com.reconinstruments.mapsdk.mapview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoTile;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataService;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataServiceResponse;

// TilePreloader is a singleton that manages all tile load requests from the geodataservice  
 
public class TileLoader {
	private final static String TAG = "TileLoader";
	
	private final int MAX_NUMBER_OF_LOADING_TILES = 10;
	private final long RETRY_TIME_AFTER_NETWORK_RETRY_IN_MS = 1000;
	private final long RETRY_TIME_AFTER_FAILED_BINDER_TRANSACTION_IN_MS = 0;
	
	private final Semaphore mAccessToRequestsMap = new Semaphore(1, true);

	public enum LoadState {
		WAITING_TO_LOAD,
		LOADING,
		LOADING_BUT_NOT_NEEDED_ANYMORE,
		LOADED
	}
	
	public class TileLoadRequest {
		public Integer		mTileIndex;
		public Date			mLoadRequestTimeStamp;
		public Date			mAttemptLoadAfter;
		public LoadState	mLoadState;
		public Integer		mLoadPriority;
		
		public TileLoadRequest(int tileIndex, Date loadTimeStamp, LoadState loadState, int loadPriority) {
			mTileIndex = tileIndex;
			mLoadRequestTimeStamp = loadTimeStamp;
			mAttemptLoadAfter = new Date();
			mLoadState = loadState;
			mLoadPriority = loadPriority;
		}
	}

// interfaces
	public interface ITileLoaderCallbacks {
		public void TileLoaded(GeoTile newGeoTile, TileLoadRequest loadRequest);	// called when a tile has been loaded from the geodata service
	}

	ITileLoaderCallbacks				mParent;
	int									mMaxThreads = MAX_NUMBER_OF_LOADING_TILES;
	public int							mNumPriorityLevels = 0;
	public  static IGeodataService		mGeodataServiceInterface  = null;
	public String						mMapCompositionID;
	TreeMap<Integer, TileLoadRequest>   mLoadRequests = new TreeMap<Integer, TileLoadRequest>();  // represents all tiles that haven't been loaded yet
	ArrayList<TileLoadRequest>	 		mLoadRequestList = new ArrayList<TileLoadRequest>();
	
	
	public TileLoader(ITileLoaderCallbacks parent, int maxThreads, int numPriorityLevels) {
		mParent = parent;
		mMaxThreads = maxThreads;
		mGeodataServiceInterface = null;
		mNumPriorityLevels = numPriorityLevels;
		mLoadRequests = new TreeMap<Integer, TileLoadRequest>();
	}
	
	public void SetGeodataServiceInterface(IGeodataService GDSInterface, String mapCompositionID) {
		mGeodataServiceInterface = GDSInterface;
		mMapCompositionID = mapCompositionID;
		
		if(mGeodataServiceInterface == null) {
			// if interface closed/lost for some reason
			Clear();
		}
	}
	

	public void Clear() {	// clear old requests... requests that have started stay and are marked as LOADING_BUT_NOT_NEEDED_ANYMORE
		try { 
			mAccessToRequestsMap.acquire(); 

			ArrayList<Integer> loopIndices = new ArrayList<Integer>(mLoadRequests.keySet());
			for(Integer tileIndex : loopIndices) {
				TileLoadRequest tlr = mLoadRequests.get(tileIndex);
				if(tlr != null) { // 
					if(tlr.mLoadState == LoadState.WAITING_TO_LOAD || tlr.mLoadState == LoadState.LOADED) {
						mLoadRequests.remove(tileIndex);
						tlr=null;
					}
					else {	// in middle of load, so mark as cancelled, leave previously cancelled items in map - until load complete
						tlr.mLoadState = LoadState.LOADING_BUT_NOT_NEEDED_ANYMORE;
					}
				}
			}
			
			mAccessToRequestsMap.release();
		}
		catch (InterruptedException e) { }	 // ignore for now... TODO do something smart here  
	}


	public void LoadTiles(ArrayList<Integer> newTileIndices, int priority, Date loadRequestTimeStamp) {
		
		assert(priority >= 0 && priority < mNumPriorityLevels);
		if(newTileIndices == null || newTileIndices.size() == 0) return;
		if(loadRequestTimeStamp == null) loadRequestTimeStamp = new Date();
		
		try { 
			mAccessToRequestsMap.acquire(); 

			for(Integer tileIndex : newTileIndices) {
				TileLoadRequest tlr = mLoadRequests.get(tileIndex); // check if already in list waiting or loading
				if(tlr != null) { 									// if it is, update it
					tlr.mLoadRequestTimeStamp = loadRequestTimeStamp;
					tlr.mLoadPriority = priority;
					if(tlr.mLoadState == LoadState.LOADING_BUT_NOT_NEEDED_ANYMORE ) {   		//  if prev cancelled, reinstate it
						tlr.mLoadState = LoadState.LOADING;	
					}
					if(tlr.mLoadState != LoadState.LOADING ) {   		//  if waiting or loaded (done)
						tlr.mLoadState = LoadState.WAITING_TO_LOAD;		//      switch to waiting
						tlr.mAttemptLoadAfter = new Date();// now
					}
				}
				else {												// else, create new tile load request
					mLoadRequests.put(tileIndex, new TileLoadRequest(tileIndex, loadRequestTimeStamp, LoadState.WAITING_TO_LOAD, priority));
				}
			}
			
			mLoadRequestList = new ArrayList<TileLoadRequest>(mLoadRequests.values());		// recreate sorted list for loading
			Collections.sort(mLoadRequestList, new Comparator<TileLoadRequest>() {
				public int compare(TileLoadRequest request1, TileLoadRequest request2) {
				    return request1.mLoadPriority.compareTo(request2.mLoadPriority);	// ascending order 
				}
			});
			
			LoadTiles();
			
			mAccessToRequestsMap.release();
		}
		catch (InterruptedException e) { }	 // ignore for now... TODO do something smart here  
		
	}

	
	public boolean TileLoaded(int tileIndex) {
		boolean tileNeeded = false;
		try { 
			mAccessToRequestsMap.acquire(); 

			TileLoadRequest tlr = mLoadRequests.get(tileIndex);
			if(tlr != null) { // 
				if(tlr.mLoadState == LoadState.LOADING) tileNeeded = true; // not cancelled
				tlr.mLoadState = LoadState.LOADED;		// mark as loaded.. will be cleaned up on next reset
				tlr = null;
			}
			LoadTiles();
			
			mAccessToRequestsMap.release();
		}
		catch (InterruptedException e) { }	 // ignore for now... TODO do something smart here  
	
		return tileNeeded;
	}

	
	public void TileLoadFailedReLoad(int tileIndex, long retryInterval) {
		try { 
			mAccessToRequestsMap.acquire(); 

			TileLoadRequest tlr = mLoadRequests.get(tileIndex);
			if(tlr != null) {
				if(tlr.mLoadState == LoadState.LOADING) { // reload if not cancelled
					tlr.mLoadState = LoadState.WAITING_TO_LOAD;
					tlr.mAttemptLoadAfter.setTime((new Date()).getTime() + retryInterval);
				}
				else {
					tlr.mLoadState = LoadState.LOADED;	// stop reload and mark for cleanup in Clear()
				}
			}
			LoadTiles();
			
			mAccessToRequestsMap.release();
		}
		catch (InterruptedException e) { }	 // ignore for now... TODO do something smart here  
	}


	private void LoadTiles() {
		// sort mLoatRequests and work way through it (refreshing mLoadingTiles as we go)

		int numLoadingReqests = 0;
		TreeMap<Integer, TileLoadRequest> tempLoadingTiles = new TreeMap<Integer, TileLoadRequest>();
		for(TileLoadRequest tlr :mLoadRequestList) {
			if(tlr.mLoadState == LoadState.LOADING || tlr.mLoadState == LoadState.LOADING_BUT_NOT_NEEDED_ANYMORE) {
				numLoadingReqests++;
			}
		}
		
		for(TileLoadRequest tlr :mLoadRequestList) {
			if(numLoadingReqests >= mMaxThreads) break;

			if(tlr.mLoadState == LoadState.WAITING_TO_LOAD && !(tlr.mAttemptLoadAfter.after(new Date())) ) {
				tlr.mLoadState = LoadState.LOADING;
				LaunchThreadToLoadTileWithIndex(tlr);
				numLoadingReqests++;
			}
		}
	}

	public void LaunchThreadToLoadTileWithIndex(TileLoadRequest loadRequest) {
		LoadGeoTileThread newTileLoadThread = new LoadGeoTileThread();
		newTileLoadThread.mLoadRequest = loadRequest;
		int LoadPriority;
		if(mNumPriorityLevels == 1) {
			LoadPriority = Thread.MAX_PRIORITY;
		}
		else {
			LoadPriority = Thread.MAX_PRIORITY - (int) ((Thread.MAX_PRIORITY-Thread.MIN_PRIORITY) * (loadRequest.mLoadPriority/(mNumPriorityLevels-1)) );
		}
//		Log.d(TAG, "loading tile#" + loadRequest.mTileIndex + " priority=" + loadRequest.mLoadPriority + " task priority=" + LoadPriority);
		newTileLoadThread.setPriority(LoadPriority);
		newTileLoadThread.start();
	}
	
	public class LoadGeoTileThread extends Thread {
		IGeodataServiceResponse rc;
		public TileLoadRequest mLoadRequest;

	    @Override
	    public void run() {
			GeoTile newGeoTile = null;
			String startStr = "load";
			String endStr = "loading";
			int tryCnt = 0;
			try {
//				if(mLoadRequest.mLoadPriority == 0) {
//					Log.e(TAG, "loading tile#" + mLoadRequest.mTileIndex + " from Geodata Service...");
//				}
//				else {
//					Log.i(TAG, "loading tile#" + mLoadRequest.mTileIndex + " from Geodata Service...");
//				}
				rc = mGeodataServiceInterface.getMapTileData(mLoadRequest.mTileIndex, mMapCompositionID);  // response rc holds retrieved GeoTile
//				Log.d(TAG, "   done tile#" + mLoadRequest.mTileIndex);

				if(rc.mResponseCode == IGeodataServiceResponse.ResponseCodes.TILE_LOADING_FROM_NETWORK_TRY_LATER) {
					TileLoadFailedReLoad(mLoadRequest.mTileIndex, RETRY_TIME_AFTER_NETWORK_RETRY_IN_MS);
				}
				else {
					if(rc.mResponseCode == IGeodataServiceResponse.ResponseCodes.ERROR_DURING_REQUEST) {	// tile failed to load - make tile a No Data Available zone
						//					Log.d(TAG, "  error during tile load for #" + mLoadTileIndex + " missing tile generated");
						newGeoTile = new GeoTile(mLoadRequest.mTileIndex);
						newGeoTile.MakeTileNoDataAvailable();
					}
					else {
						newGeoTile = rc.mGeoTile;
					}
					if(TileLoaded(mLoadRequest.mTileIndex)) {
						mParent.TileLoaded(newGeoTile, mLoadRequest);
					}
				}
			}
			catch (Exception e) {
				tryCnt ++;
				if(tryCnt <= 4) {
					Log.e(TAG, "failed tile load caught for tile #" + mLoadRequest.mTileIndex + " - Reloading (attempt " + tryCnt + ") ..." );	// trap for FAILD BINDER TRANSACTION and other system errors
					TileLoadFailedReLoad(mLoadRequest.mTileIndex, RETRY_TIME_AFTER_FAILED_BINDER_TRANSACTION_IN_MS);

				}
				else {
					Log.e(TAG, "multiple errors caught during tile load for #" + mLoadRequest.mTileIndex + " - Generating no-data tile..." );	
					newGeoTile = new GeoTile(mLoadRequest.mTileIndex);
					newGeoTile.MakeTileNoDataAvailable();
					if(TileLoaded(mLoadRequest.mTileIndex)) {
						mParent.TileLoaded(newGeoTile, mLoadRequest);
					}
				}
			}

	    }


	}
	
}
