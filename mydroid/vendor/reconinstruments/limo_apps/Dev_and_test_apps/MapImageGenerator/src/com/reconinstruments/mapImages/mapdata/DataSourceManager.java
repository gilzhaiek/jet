/**
 * 
 */


/*
 * dev stages: 1-dummy data, 2- file data, 3- file or net data
 */
package com.reconinstruments.mapImages.mapdata;

/**
 * @author stevenmason
 *
 */

import android.location.Location;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.mapImages.MapsManager;
import com.reconinstruments.mapImages.IMapDataService;



public class DataSourceManager implements Parcelable 
{	// singleton	
	private static final String TAG = "DataSourceManager";

	protected boolean mCanPreloadData = false;
	protected double mPreloadBoundaryRatio= 1.0;     // margin inside requested geoView where preloading happens, suggested values
	protected double mGeoRegionExpansionRatio= 2.0;  // amount of data returned relative to what is asked for
	protected static boolean mLoadMapDataIsRunning = false;
	public IMapDataService mIMapDataService = null;

	MapDataCache	mParentMapDataCache = null;
	protected int mLastResortIDLoaded = -1;

	public enum DataSourceManagerResponseCode {
		SERVICE_AVAILABLE,
		WAITING_FOR_LOCATION,
		LOADING_DATA,
		NEW_DATA_LOADED,
		NO_DATA_LOADED,
		ERROR
	}

	
	//===========================================================
	public DataSourceManager(IMapDataService iMapDataService, double preloadBoundaryRatio, double geoRegionExpansionRatio) {   // Restrict the constructor from being instantiated - singleton - see getInstance()
		// TODO Auto-generated constructor stub
		Log.d(TAG, "constructor");
		mGeoRegionExpansionRatio = geoRegionExpansionRatio;
		mPreloadBoundaryRatio = preloadBoundaryRatio;
		mIMapDataService = iMapDataService;
	}
	public boolean CanPreloadData() {
		return mCanPreloadData;
	}
	
	public void RegisterParentCache(MapDataCache mdc) {
		mParentMapDataCache = mdc;
	}
	
	public void RemoveIMapDataService() {
		if(mIMapDataService != null) {
			try {
				mIMapDataService.registerStateListener(null);	// turn it off any callbacks in the service... TODO, this won't help if app crashes and service still has callback obj registered  better to wrap callback in try/catch
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
	
	public Bundle GetBuddiesBundle() {
		return null;
	}
	
	public void SetIMapDataService(IMapDataService iMapDataService) {
		mIMapDataService = iMapDataService;
		if(mIMapDataService == null) {
			// TODO abort any existing calls to service to get data as service has been disconnected
			Log.d(TAG, "Setting IMapDataService = null");
		}
		else {
			Log.d(TAG, "Setting IMapDataService");
		}
	}
	
	public IMapDataService GetIMapDataService() {
		return mIMapDataService;
	}
	
	public void SetClosestResort(String closestResortName, double closestResortLatitude, double closestResortLongitude) {
	}
	
	public void MapServiceStateChangedTo(MapsManager.State mState) {
		// override this method and respond to changes from maps service
	}
	
	public void BuddiesUpdated() {
		// override this method and respond to changes from maps service
	}

	public DataSourceManagerResponseCode GetMapData(RegionMapData rmd) {
   		return DataSourceManagerResponseCode.NO_DATA_LOADED;		// expect this to be overridden
	}


	//===========================================================
	// external load data interface routines
	
	// results interface definition
	public interface IDataSourceManagerCallback {
//		public void LoadMapDataComplete(RegionMapData regionMapData, String errorMsg);
		public void DataSourceManagerStateChangedTo(DataSourceManagerResponseCode resCode, String errorMsg);
		public void BuddiesUpdated();
	}

	// external API to request load of map data for a particular source
	public void LoadRegionMapData(MapDataCache mapDataCache, RegionMapData regionMapData) {
	}

	
	//===========================================================
	private String username;
	private String password;


	public static final Parcelable.Creator<DataSourceManager> CREATOR = new Parcelable.Creator<DataSourceManager>() {

		public DataSourceManager createFromParcel(Parcel in) {
			return new DataSourceManager(in);
		}

		public DataSourceManager[] newArray(int size) {
			return null;
		}

	};
	
	private DataSourceManager(Parcel in) {
	    readFromParcel(in);
	}

	public int describeContents() {
	    return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
	    out.writeString(username);
	out.writeString(password);
	}
	public void readFromParcel(Parcel in) {
	    username = in.readString();
	    password = in.readString();
	}


}