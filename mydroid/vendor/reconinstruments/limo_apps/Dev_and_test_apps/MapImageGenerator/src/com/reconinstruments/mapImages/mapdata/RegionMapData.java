package com.reconinstruments.mapImages.mapdata;

import android.location.Location;

import com.reconinstruments.mapImages.drawings.MapDrawings;
import com.reconinstruments.mapImages.mapview.CameraViewport;
import com.reconinstruments.mapImages.objects.MapImagesInfo;


public class RegionMapData {
	private static final String TAG = "RegionMapData";
	
	public CameraViewport 	mGeoRegion  = null;		// initially loaded with requested region, data source manager may modify and return larger region
	private DataStatus 		mStatus = DataStatus.EMPTY;		
	public boolean 			mViewWaitingForThisData = false;
	public MapLayer 		layers[] = null;	// null = no data
	public long 			mLastUsedTimeStamp = -1;
	// TODO add load timeout timer and timeout handler
	
	// temp to accompodate initial prototype
	public boolean 			mNewData = false;
	public MapDrawings 		mMapDrawings = null;
	public MapImagesInfo 	mMapImagesInfo = null;
	public Location 		mClosestResortLocation = null;		// used to store closest 

	public enum DataStatus {
		EMPTY,
		LOADING,
		LOADED
	}
	
	public boolean IsLoading() {
		return (mStatus == DataStatus.LOADING);
	}
	
	public RegionMapData(CameraViewport geoRegion, DataStatus status, boolean viewWaiting) {
		mGeoRegion = geoRegion;
		mStatus = status;
		mViewWaitingForThisData = viewWaiting;
	}
	
	public void SetStatus(DataStatus status) {
		mStatus = status;
	}
	
	public CameraViewport.ContainResult Contains(CameraViewport geoRegion, float boundaryRatio) {
		return mGeoRegion.Contains(geoRegion, boundaryRatio);	
	}

}
