package com.reconinstruments.mapImages.mapdata;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import android.graphics.RectF;
import android.location.Location;
import android.util.Log;

import com.reconinstruments.mapImages.helpers.LocationTransformer;
import com.reconinstruments.mapImages.mapdata.DataSourceManager.DataSourceManagerResponseCode;
import com.reconinstruments.mapImages.mapview.CameraViewport;
import com.reconinstruments.mapImages.mapview.ReconMapView;

public class MapDataCache implements DataSourceManager.IDataSourceManagerCallback {
	private static final String TAG = "MapDataCache";
//	private static final int MAX_RMDS = 5;

	public enum MapCacheResponseCode {
		WAITING_FOR_DATA_SERVICE,
		WAITING_FOR_LOCATION,
		STARTING_DATA_SERVICE,
		LOADING_DATA,
		DATA_AVAILABLE,
		NO_DATA_AVAILABLE,
		ERROR_WITH_SERVICE
	}

	public enum DataServiceState {
		UNKNOWN,
		AVAILABLE,
		ERROR
	}
	DataServiceState mDataServiceState = DataServiceState.UNKNOWN;
	String mDataServiceErrorMsg = "";
	
	public  DataSourceManager 	mDataSourceManager = null;
	private CameraViewport 		mLastViewGeoRegionTested;
	private boolean 			mCanPreloadData = true;
	private DataSourceManager 	dsm=null;
	private  ReconMapView 		mMapView;
	private Date 				mTimeStampDate = new Date();
	private String				mClosestResortName = null;
	private double 				mClosestResortLatitude = 0.;
	private double 				mClosestResortLongitude = 0.;

	private ArrayList<RegionMapData> rmds = new ArrayList<RegionMapData>();

	
	public MapDataCache(ReconMapView mapView, DataSourceManager dsm) {
		mMapView = mapView;
		mDataSourceManager = dsm;
		mCanPreloadData = dsm.CanPreloadData();
		
		dsm.RegisterParentCache(this);
	}
	
	public void SetPreloadDataRatio(boolean state) {
		mCanPreloadData = state;
	}
	
	public RegionMapData GetBestRMDForBounds(RectF viewPortBB) {	// TODO use geoRegion to determine if rmd is valid
		if(rmds.size() > 0) {
			if(rmds.get(0).mMapDrawings == null) return null;
			return rmds.get(0);
		}
		else {
			Log.e(TAG, "GetBestRMDForBounds called with no data");
			return null;
		}
	}
	
	public void SetClosestResort(String closestResortName, double closestResortLatitude, double closestResortLongitude) {
		mClosestResortName = closestResortName;
		mClosestResortLatitude = closestResortLatitude;
		mClosestResortLongitude = closestResortLongitude;
		Log.d(TAG,"Closest resort: " + mClosestResortName + " | " + mClosestResortLatitude + ", " + mClosestResortLongitude);
	}
	
	public String GetClosestResortName() {
		return mClosestResortName;
	}
	
	public double GetDistanceInMetersToClosestResort(double refLat, double refLong) { 
		if(mClosestResortLatitude != 0. && mClosestResortLongitude != 0.) {
			return  DistanceBetweenGPSCoords(refLat, refLong, mClosestResortLatitude, mClosestResortLongitude);  //  in meters
		}
		return -1.0;
	}
	
	protected double DistanceBetweenGPSCoords(double latitude1, double longitude1, double latitude2, double longitude2) {
		double equitorialCircumference = 40075017.0; // m  - taken from wikipedia/Earth
		double meridionalCircumference = 40007860.0; // m  - taken from wikipedia/Earth

		double hDist = Math.abs(longitude1-longitude2) / 360.0 * (equitorialCircumference*Math.cos(Math.toRadians(latitude1)));
		double vDist = Math.abs(latitude1-latitude2) / 360.0 * meridionalCircumference;
		return Math.sqrt(hDist*hDist + vDist*vDist);
	}

	public MapCacheResponseCode HasDataToRenderRegion(CameraViewport geoRegion) {
		
//		GeoRegion.ContainResult	mBestContainsResult = GeoRegion.ContainResult.NO;
//		RegionMapData rmdContainingGeoRegion;
		
		mLastViewGeoRegionTested = geoRegion;

		assert dsm == null: "MapDataCache has no DataSourceManager set";

		// bypass new logic (below) for initial "place holder" deliverable
		RegionMapData curRMD;
//		if(true) {
			if(mDataServiceState == DataServiceState.ERROR) {
				return MapCacheResponseCode.ERROR_WITH_SERVICE;
			}
			
			if(rmds.size()==0) {
				if(mDataServiceState == DataServiceState.UNKNOWN) {
					return MapCacheResponseCode.WAITING_FOR_DATA_SERVICE;
				}
				else {
					return MapCacheResponseCode.LOADING_DATA;
				}
				
//				Log.e(TAG, "requesting data...");
//				curRMD = new RegionMapData(geoRegion, RegionMapData.DataStatus.LOADING, true);
//				mDataSourceManager.LoadRegionMapData(this, curRMD);	  // until map services restructured, this call is does nothing
			} 
			else {
				curRMD = rmds.get(0);
				if(curRMD.mMapDrawings == null) {
					return MapCacheResponseCode.NO_DATA_AVAILABLE;
				}
				else {
					RectF rBB = curRMD.mMapDrawings.mResortInfo.BoundingBox;		// TODO make this more efficient - do once when resort data changes
					LocationTransformer lt = new LocationTransformer();		
					lt.SetBoundingBox(curRMD.mMapDrawings.mResortInfo.BoundingBox);	// needed before transformer used

					curRMD.mGeoRegion = geoRegion;
					curRMD.mGeoRegion.SetMinScaleForResortWidthInKms(lt.DistanceKmFromLngLats(rBB.top,rBB.left,rBB.top,rBB.right));
					
					return MapCacheResponseCode.DATA_AVAILABLE;
				}
				//				RectF rBB = curRMD.mMapDrawings.mResortInfo.BoundingBox;
//				RectF gBB = geoRegion;
//				Log.i(TAG, "---- "+ rBB.left +" - "+ gBB.left +" - "+
//						rBB.right +" - "+ gBB.right );
//				Log.i(TAG, "   - "+ rBB.top +" - "+ gBB.top +" - "+
//						rBB.bottom +" - "+ gBB.bottom );
//				if(RectF.intersects(geoRegion, curRMD.mMapDrawings.mResortInfo.BoundingBox)) {	
//					Log.e(TAG, "have data...");
//					return true;
//				}
//				else {
////					Log.e(TAG, "out of bounds of existing data... requesting new data...");
//					rmds.clear();	// reset to startup state (for now) and ask for new data...
//					curRMD = new RegionMapData(geoRegion, RegionMapData.DataStatus.LOADING, true);
//					mDataSourceManager.LoadRegionMapData(this, curRMD);	  // until map services restructured, just mimic previous use and return
//					return false;     
//				}
			}
//		}
		
		
		// new logic - will never reach here until next version
		
//		mBestContainsResult = GeoRegion.ContainResult.NO;	
//		rmdContainingGeoRegion = null;
//		boolean loadedContainingRMDFound = false;
//		// loop rmds[] 
//		if(rmds.size() > 0) {	// if there are rmds loaded
//			Iterator itr = rmds.iterator();
//			while(itr.hasNext() && !loadedContainingRMDFound){
//				RegionMapData nextRMD = (RegionMapData) itr.next();
//				GeoRegion.ContainResult cs = nextRMD.Contains(geoRegion, mBoundaryRatio);
//				if(cs.compareTo(mBestContainsResult) > 0) {		// if RMD object is preferred to best so far
//					mBestContainsResult = cs;
//					rmdContainingGeoRegion = nextRMD;
//					if(cs == GeoRegion.ContainResult.YES_DATALOADED) loadedContainingRMDFound=true;	// have region with best result, so stop search
//				}
//			}
//		}		
//		boolean updateView;
//		RegionMapData newRMD;
//		boolean rc = false;
//		switch(mBestContainsResult) {
//		case NO:					// then request data load and tell view to wait for data
//			rc = false;
//			newRMD = new RegionMapData(geoRegion, RegionMapData.DataStatus.LOADING, !rc);
//			if(rmds.size() == MAX_RMDS) RemoveOldestRMD();  
//			rmds.add(newRMD);
//			mDataSourceManager.LoadRegionMapData(this, newRMD);	
//			// TODO turn on data load timer
//			break;
//			
//		case YES_INBOUNDARY_DATALOADING:
//			rc = false;  
//			if(mCanPreloadData) {	// add another load request to load better data than what's coming
//				newRMD = new RegionMapData(geoRegion, RegionMapData.DataStatus.LOADING, !rc);	 
//				if(rmds.size() == MAX_RMDS) RemoveOldestRMD();  
//				rmds.add(newRMD);
//				mDataSourceManager.LoadRegionMapData(this, newRMD);
//			}
//			break;
//			
//		case YES_DATALOADING:
//			rc = false; 			// data is coming, so just tell view to wait for data
//			// TODO trap (rmdContainingGeoRegion == null)  shouldn't happen
//			rmdContainingGeoRegion.mViewWaitingForThisData = !rc;  // force view waiting flag in case it's not set
//			break;
//			
//		case YES_INBOUNDARY_DATALOADED:
//			rc = true;
//			if(mCanPreloadData) {  	// have data so tell view to go ahead and redraw, but data is edge case, so load better data in background
//				newRMD = new RegionMapData(geoRegion, RegionMapData.DataStatus.LOADING, !rc);	 
//				if(rmds.size() == MAX_RMDS) RemoveOldestRMD();  
//				rmds.add(newRMD);
//				mDataSourceManager.LoadRegionMapData(this, newRMD);
//			}
//			rmdContainingGeoRegion.mLastUsedTimeStamp = mTimeStampDate.getTime();;
//			break;
//		
//		case YES_DATALOADED:		// have data so tell view to go ahead and redraw
//			rmdContainingGeoRegion.mLastUsedTimeStamp = mTimeStampDate.getTime();;
//			rc = true;
//			break;
//			
//		}
//		return false;	// 	true = have data to render, false = don't have data, wait for it
	}
	
	private void RemoveOldestRMD() {
		RegionMapData oldestRMD = null;
		long testTime = mTimeStampDate.getTime();
		Iterator itr = rmds.iterator();
		while(itr.hasNext()){
			RegionMapData curRMD = (RegionMapData) itr.next();
			if(curRMD.mLastUsedTimeStamp < testTime) {
				oldestRMD = curRMD;
				testTime = curRMD.mLastUsedTimeStamp;
			}
		}
		rmds.remove(oldestRMD);
	}
	
	// results interface definition
	public interface IMapDataCacheCallbacks {
		public void MapCacheStateChangedTo(MapCacheResponseCode rc);
		public void ErrorLoadingCache(String errorMsg);
		public void BuddiesUpdated();
	}
	
	public void BuddiesUpdated() {
		mMapView.BuddiesUpdated();
	}
	
	// callback from DataSourceManager - IDataSourceManagerCallback interface
	public void DataSourceManagerStateChangedTo(DataSourceManager.DataSourceManagerResponseCode resCode, String errorMsg) {
    	mDataServiceErrorMsg = "";
		RegionMapData curRMD;

	    switch(resCode) {
	    case SERVICE_AVAILABLE: 
	    	mDataServiceState = DataServiceState.AVAILABLE;
			if(rmds.size() > 0) {	// if there are rmds loaded
				rmds.clear();
			}
			mMapView.MapCacheStateChangedTo(MapCacheResponseCode.STARTING_DATA_SERVICE);
	    	break;
	    case WAITING_FOR_LOCATION: 
	    	mDataServiceState = DataServiceState.AVAILABLE;
			if(rmds.size() > 0) {	// if there are rmds loaded
				rmds.clear();
			}
			mMapView.MapCacheStateChangedTo(MapCacheResponseCode.WAITING_FOR_LOCATION);
	    	break;
	    case LOADING_DATA: 
	    	mDataServiceState = DataServiceState.AVAILABLE;
			if(rmds.size() > 0) {	// if there are rmds loaded
				rmds.clear();
			}
			mMapView.MapCacheStateChangedTo(MapCacheResponseCode.LOADING_DATA);
	    	break;
	    case NEW_DATA_LOADED:
	    case NO_DATA_LOADED: 
	    	mDataServiceState = DataServiceState.AVAILABLE;
			if(rmds.size() > 0) {	// if there are rmds loaded
				curRMD = rmds.get(0);
			}
			else {
				curRMD = new RegionMapData(mLastViewGeoRegionTested, RegionMapData.DataStatus.LOADED, true);
				rmds.add(curRMD);
			}
			if(resCode == DataSourceManager.DataSourceManagerResponseCode.NEW_DATA_LOADED) {
				if(mDataSourceManager.GetMapData(curRMD) == DataSourceManagerResponseCode.ERROR) {		// fill curRMD with data if successful
				   	mMapView.ErrorLoadingCache("ERROR - can't access map data.");
				}
				else {
					Log.i(TAG, "new data available...");
					mMapView.MapCacheStateChangedTo(MapCacheResponseCode.DATA_AVAILABLE);
				}
			}
			else {
				Log.i(TAG, "no data available...");
				curRMD.mMapDrawings = null;
				curRMD.mMapImagesInfo = null;
				mMapView.MapCacheStateChangedTo(MapCacheResponseCode.NO_DATA_AVAILABLE);
			}
	    	break;
	    case ERROR: 
	    	mDataServiceState = DataServiceState.ERROR;
	    	mDataServiceErrorMsg = errorMsg;
	    	mMapView.ErrorLoadingCache(errorMsg);
	    	break;
	    }
	}

	// for future implementations
	public void LoadMapDataComplete(RegionMapData regionMapData, String errorMsg) {
		if(errorMsg.length() == 0) {	// no error

			// only accept data that suits last requested geoRegion
			if(RectF.intersects(mLastViewGeoRegionTested, regionMapData.mMapDrawings.mResortInfo.BoundingBox)) {	
				regionMapData.mLastUsedTimeStamp = new Date().getTime();
				regionMapData.SetStatus(RegionMapData.DataStatus.LOADED);	
				
				RectF rBB = regionMapData.mMapDrawings.mResortInfo.BoundingBox;
				
				LocationTransformer lt = new LocationTransformer();
				lt.SetBoundingBox(regionMapData.mMapDrawings.mResortInfo.BoundingBox);	// needed before transformer used

				regionMapData.mGeoRegion.SetMinScaleForResortWidthInKms(lt.DistanceKmFromLngLats(rBB.top,rBB.left,rBB.top,rBB.right));
				
				rmds.add(regionMapData);
				Log.v(TAG, "new data added...");
				
				// TODO turn off data load timer
				mMapView.LoadMapDataComplete("");
			}
//			else {
//				rmds.remove(regionMapData);	// no new data added to set so remove empty rmd; mapview will have to draw with what it has avaiable
//			}
		}
		else {
			// TODO handle error
//			rmds.remove(regionMapData);	// remove bad data object from cache
//			mMapView.LoadMapDataComplete(errorMsg);
		}
	}

	
}
