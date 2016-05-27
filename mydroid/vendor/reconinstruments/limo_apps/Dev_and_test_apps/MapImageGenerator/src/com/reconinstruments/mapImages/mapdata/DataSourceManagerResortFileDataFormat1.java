package com.reconinstruments.mapImages.mapdata;

import java.util.ArrayList;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.mapImages.MapsManager;
import com.reconinstruments.mapImages.MapsManager.IMapServiceListener;
import com.reconinstruments.mapImages.bundlers.MapBundler;
import com.reconinstruments.mapImages.IMapDataService;


public class DataSourceManagerResortFileDataFormat1 extends DataSourceManager implements IMapServiceListener {
	private static final String TAG = "DataSourceManagerResortFileDataFormat1";
	
	protected boolean mBlockLoadRequest = false;

	public DataSourceManagerResortFileDataFormat1(IMapDataService iMapDataService, double preloadBoundaryRatio, double geoRegionExpansionRatio) {
		super(iMapDataService, preloadBoundaryRatio, geoRegionExpansionRatio);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void SetIMapDataService(IMapDataService iMapDataService) {
		super.SetIMapDataService(iMapDataService);

		try {
			mIMapDataService.registerStateListener((DataSourceManager)this);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	//===========================================================
	// external load data interface routines
	
	// external API to request load of map data for a particular source
    @Override
	public void LoadRegionMapData(MapDataCache mapDataCache, RegionMapData regionMapData) {
    	
    	// currently this is not used 
    	// this code is prototype of what will be used in pull architecture
    	
    	
		if(mBlockLoadRequest) return;	// TODO kludge to block launching extra background tasks while data load completes since UI thread runs higher priority.. several identical can be received before one is processed
		        						// also since each request is currently not being responded to (something to change), we can't block these in the MapDataCache as it may lock in a "waiting" state
		mBlockLoadRequest = true;
//		Log.d(TAG, "processing load data request...");

		while(mLoadMapDataIsRunning) {};	// TODO add timeout and error response

		// spawn thread to handle data request
		mLoadMapDataIsRunning = true;
//		Log.v(TAG, "LoadRegionMapData...");
		new LoadRegionMapDataTask(this, mapDataCache, regionMapData).execute();
		mLoadMapDataIsRunning = false;
	}
	
	protected class LoadRegionMapDataTask extends AsyncTask<Void, Void, String> {
		
		private RegionMapData mRegionMapData = null;
		private MapDataCache mMapDataCache = null;
		private DataSourceManager parentDSM = null;
		
		public LoadRegionMapDataTask(DataSourceManager dsm, MapDataCache mapDataCache, RegionMapData regionMapData) {
			mMapDataCache = mapDataCache;
			mRegionMapData = regionMapData;
			parentDSM = dsm;
		}
		
		protected String doInBackground(Void...voids) {

			try	{
				if(parentDSM.mIMapDataService == null) {
					return "No Service";
				}

				Bundle mapBundle = null;
				if(mRegionMapData.mMapDrawings != null)  {
					Bundle resortIDBundle = parentDSM.mIMapDataService.getResortID();
					if(resortIDBundle == null) {
						return "No resort bundle";
					}
					else {
						if(resortIDBundle.getInt("ResortID") == mRegionMapData.mMapDrawings.mResortInfo.ResortID) {
							Log.v(TAG, "Already have this ResortId="+ mRegionMapData.mMapDrawings.mResortInfo.ResortID);
							return "Have bundle";
						}
					}
				}

				// otherwise different/new data- get bundle
				mapBundle = parentDSM.mIMapDataService.getResortGraphicObjects();

				if(mapBundle == null) {
					return "No new data";					
				}
				else {	// there's new data
					Log.i(TAG, "MapsServiceConnection:mapBundle has data");

					mRegionMapData.mMapDrawings = MapBundler.GetMapDrawings(mapBundle);
					mRegionMapData.mNewData = true;
					
					// set geoRegion to bounding box
					// mRegionMapData.mMapDrawings.mResortInfo.BoundingBox

					mapBundle.clear();  
					return "";
				}
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}		 

			return "error";
		}

		protected void onPostExecute(String errorString) {
			if(errorString.length() > 0) {
				mMapDataCache.LoadMapDataComplete(mRegionMapData, errorString);
//				Log.v(TAG, "LoadRegionMapData... "+ errorString);
			}
			else {
				mMapDataCache.LoadMapDataComplete(mRegionMapData, "");
				Log.v(TAG, "LoadRegionMapData... LOADED!");
			}
//			mIsRunning = false;
			mBlockLoadRequest = false;

		}

	}
    
	@Override
	public Bundle GetBuddiesBundle() {

		if(mIMapDataService == null) {
			return null;
		}

		try	{
			Bundle buddyInfoBundle = mIMapDataService.getListOfBuddies();
			return buddyInfoBundle;
		}
		catch (RemoteException e) { e.printStackTrace(); }
		catch (NullPointerException e) { e.printStackTrace(); }
		return null;
		
	}
	
	@Override
	public void SetClosestResort(String closestResortName, double closestResortLatitude, double closestResortLongitude) {
		mParentMapDataCache.SetClosestResort(closestResortName, closestResortLatitude, closestResortLongitude);
	}
	
	
	@Override
	public void MapServiceStateChangedTo(MapsManager.State newServiceState) {
		super.MapServiceStateChangedTo(newServiceState);
		
		Log.i(TAG, "MapServiceStateChangedTo: "+ newServiceState);

		if(mParentMapDataCache == null) return;
		
		// respond to changes from maps service
	    switch(newServiceState) {
	    case LOADING_RESORT_INFO: 
	    	mParentMapDataCache.DataSourceManagerStateChangedTo(DataSourceManagerResponseCode.SERVICE_AVAILABLE, "");
	    	break;
	    case ERROR_LOADING_RESORT_INFO: 
	    	mParentMapDataCache.DataSourceManagerStateChangedTo(DataSourceManagerResponseCode.ERROR, "Resort data not available.");
	    	break;
	    case WAITING_FOR_LOCATION_TO_LOAD_DATA: 
	    	mParentMapDataCache.DataSourceManagerStateChangedTo(DataSourceManagerResponseCode.WAITING_FOR_LOCATION, "");
	    	break;
	    case LOADING_DATA: 
	    case BUNDLING_DATA: 
	    	mParentMapDataCache.DataSourceManagerStateChangedTo(DataSourceManagerResponseCode.LOADING_DATA, "");
	    	break;
	    case DATA_AVAILABLE:
	    	mParentMapDataCache.DataSourceManagerStateChangedTo(DataSourceManagerResponseCode.NEW_DATA_LOADED, "");
	    	break;
	    case NO_DATA_AVAILABLE_FOR_THIS_LOCATION: 
	    	mParentMapDataCache.DataSourceManagerStateChangedTo(DataSourceManagerResponseCode.NO_DATA_LOADED, "");
	    	break;
	    case ERROR_LOADING_DATA: 
	    	mParentMapDataCache.DataSourceManagerStateChangedTo(DataSourceManagerResponseCode.ERROR, "Map data not available");
	    	break;
	    }
	}

	@Override
	public void BuddiesUpdated() {
		// override this method and respond to changes from maps service
		mParentMapDataCache.BuddiesUpdated();
	}

	@Override
   	public DataSourceManagerResponseCode GetMapData(RegionMapData rmd) {
		try	{
			Bundle mapBundle = null;

			// otherwise different/new data- get bundle
			mapBundle = mIMapDataService.getResortGraphicObjects();

			if(mapBundle == null) {
				return DataSourceManagerResponseCode.ERROR;					// no bundle, outside of any resort area in dbase
			}
			else {	// there's new data

				rmd.mMapImagesInfo = MapBundler.GetMapImagesInfo(mapBundle);
				rmd.mMapDrawings = MapBundler.GetMapDrawings(mapBundle);

				//mapBundle.clear();  // TODO determine if this is really needed
			}
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}		 
		return DataSourceManagerResponseCode.NEW_DATA_LOADED;
    }
	


//	protected class LoadRegionMapDataTaskOld extends AsyncTask<Void, Void, String> {
//		
//		private RegionMapData mRegionMapData = null;
//		private MapDataCache mMapDataCache = null;
//		private DataSourceManager parentDSM = null;
//		private boolean mIsRunning = false;
//		
//		public LoadRegionMapDataTaskOld(DataSourceManager dsm, MapDataCache mapDataCache, RegionMapData regionMapData) {
//			mMapDataCache = mapDataCache;
//			mRegionMapData = regionMapData;
//			parentDSM = dsm;
//		}
//		
//		protected String doInBackground(Void...voids) {
//
//			Log.v(TAG, "Load map data from service...");
//			int attempts = 0;
//			while(parentDSM.mIMapDataService == null && attempts <= 120) {
//				try {
//					Log.v(TAG, "No map data service - blocking 250ms and try again");
//					Thread.sleep(250);
//				} 
//				catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				attempts++;
//			}
//			if(parentDSM.mIMapDataService == null) {
//				Log.v(TAG, "No service - timed out");
//				return "Timed out attempting to connect to Map Data Service.";
//			}
//			
//			
////			attempts = 0;
////			try {
////				while(!parentDSM.mIMapDataService.hasData() && attempts <= 100) {
////					Log.v(TAG, "Waiting for service to generate data - blocking 1s and try again");
////					Thread.sleep(5000);
////					attempts++;
////				}
////			}
////			catch (InterruptedException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			} 
////			catch (RemoteException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}
////		
////			if(parentDSM.mIMapDataService == null) {
////				Log.v(TAG, "No data from map data service - timed out");
////				return "Timed out for Map Data Service to produce data.";
////			}
//
//			
//			mIsRunning = true;
//
//			try	{
//				Bundle mapBundle = null;
//				mRegionMapData.mNewData = false;
//
//				Bundle resortIDBundle = parentDSM.mIMapDataService.getResortID();
//				if(resortIDBundle != null && resortIDBundle.getInt("ResortID") == parentDSM.mLastResortIDLoaded) {
//					Log.v(TAG, "No new data to supply");
//					return "";  // mRegionMapData.newData = false will cause MapView to redraw with what it has
//				}
//
//				// otherwise different/new data- get bundle
//				mapBundle = parentDSM.mIMapDataService.getResortGraphicObjects();
//
//				if(mapBundle == null) {
//					parentDSM.mLastResortIDLoaded = -1;   // no resort data
//					return "";					// no bundle, outside of any resort area in dbase
//				}
//				else {	// there's new data
//					Log.v(TAG, "MapsServiceConnection:mapBundle has data");
//
//					mRegionMapData.mMapImagesInfo = MapBundler.GetMapImagesInfo(mapBundle);
//					mRegionMapData.mMapDrawings = MapBundler.GetMapDrawings(mapBundle);
//					mRegionMapData.mNewData = true;
//
//					// figure out BB, take requested mRegionMapData.geoRegion * expansion   and   resort BB
//					mRegionMapData.mGeoRegion.LoadFromRectF(
//							mRegionMapData.mMapDrawings.mResortInfo.BoundingBox.top,
//							mRegionMapData.mMapDrawings.mResortInfo.BoundingBox.bottom,
//							mRegionMapData.mMapDrawings.mResortInfo.BoundingBox.left,
//							mRegionMapData.mMapDrawings.mResortInfo.BoundingBox.right	) ; //+ expansion factor ...mGeoRegionExpansionRatio
//
//					Log.i(TAG, "POIs: " + mRegionMapData.mMapDrawings.POIDrawings.size()); 
//
//					mLastResortIDLoaded = resortIDBundle.getInt("ResortID");
//					mapBundle.clear();  
//				}
//			}
//			catch (RemoteException e)
//			{
//				e.printStackTrace();
//			}		 
//
//			return "";
//		}
//
//		protected void onPostExecute(String errorString) {
//			if(errorString.length() > 0) {
//				mMapDataCache.LoadMapDataComplete(mRegionMapData, errorString);
//			}
//			else {
//				mMapDataCache.LoadMapDataComplete(mRegionMapData, "");
//			}
//			mIsRunning = false;
//		}
//
//	}
}
