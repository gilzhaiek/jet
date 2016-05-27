package com.reconinstruments.dashlauncher.radar.maps;

import java.util.concurrent.locks.ReentrantLock;

import com.reconinstruments.dashlauncher.radar.maps.bo.MapDataBO;
import com.reconinstruments.dashlauncher.radar.maps.bundlers.MapBundle;
import com.reconinstruments.dashlauncher.radar.maps.common.MapsHandler;
import com.reconinstruments.dashlauncher.radar.maps.controllers.MapViewController;
import com.reconinstruments.dashlauncher.radar.maps.drawings.MapDrawings;
import com.reconinstruments.dashlauncher.radar.maps.helpers.LocationTransformer;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortData;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortsInfo;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

public class MapsManager extends MapsHandler  {
	protected static final String TAG = "MapsManager";
	
	private static MapDataBO			mMapDataBO = null;
	private static boolean				mRunning = false;
	private static Context				mContext = null;
	private static MapViewController 	mMapViewController = null;
	
	private static ResortsInfo 			mResortsInfo = null;
	private static ResortData 			mResortData = null;
	private static MapDrawings			mMapDrawings = null;
	private static MapBundle			mMapBundle = null;

	protected static final ReentrantLock mLock = new ReentrantLock();
	
	private static boolean				mMainThreadRunning = false;
	
	public MapsManager(Context context) {
		mContext = context;
	}
	
	public Bundle GetResortIDBundle() {
		if(mResortData == null)
			return null;
		
		Bundle bundle = new Bundle();
		bundle.putInt("ResortID", mResortData.mResortInfo.ResortID);
		
		return bundle;
	}
	
	public Bundle GetMapBundle() {
		if(mMainThreadRunning) return null;
		
		mMainThreadRunning = true;
		
		Bundle retBundle = null;
		try {
			if(mMapBundle != null){
				Log.v(TAG, "mMapBundle != null");
				if(mMapBundle.mBundle != null) {
					Log.v(TAG, "mMapBundle.mBundle != null");
					retBundle = mMapBundle.mBundle;
				} else {
					mMapBundle = null;
				}
			} 			
			
			if(mMapBundle != null){
			}
			else if(mMapDrawings != null) {
				//Log.v(TAG, "mMapDrawings != null");
				GenerateBundle(mMapDrawings);
			}
			else if(mResortData != null) {
			    //Log.v(TAG, "mResortData != null");
				SuccessResortData(mResortData); 
			}		
			else if(mResortsInfo != null) {
				//Log.v(TAG, "mResortsInfo != null");
				SuccessResortsInfo(mResortsInfo);
			}
		}
		catch (Exception e) { }
		finally {
			mMainThreadRunning = false;
		}
		return retBundle;
	}
	
	public void onLocationChanged(Location location) {
		if(mMapDataBO == null) {
			return;
		}
		
		// Checking Resorts Info
		if(mMapDataBO.IsLoadingResortsInfo()) // Loading Resorts Info
			return;
	
		if(mResortsInfo == null) {
			return;
		}

		// Checking Resort Data
		if(mMapDataBO.IsLoadingResortData()) {// Loading Resort Data
			return;
		}
		
		float longitude = (float)location.getLongitude() + (float)LocationTransformer.DEBUG_OFFSET_LONG;
		float latitude = (float)location.getLatitude() + (float)LocationTransformer.DEBUG_OFFSET_LAT;
		
		//Log.v(TAG,"Longitude="+longitude+" Latitude="+latitude);
		
		// Is the current resort ok?
		if(mResortData != null) {
			if(mResortData.mResortInfo != null) {
				if(mResortData.mResortInfo.BoundingBox.contains(longitude, latitude)) {
					return;
				}
			}
		}
		
		// Find a good resort
		for(int i = 0; i < mResortsInfo.mResorts.size(); i++) {
			if(mResortsInfo.mResorts.get(i).BoundingBox.contains(longitude, latitude)) {
				try {
					mMapDataBO.LoadResortDataTask(this, mResortsInfo.mResorts.get(i));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return;
			}
		}
	}
	
    public void onStart() {
    	mRunning = true;
    	
    	if(mMapDataBO == null) {
    		mMapDataBO = new MapDataBO(mContext);
    	}
    	    
    	mMapDataBO.LoadResortInfoTask(this);
    } 
    
    public void onStop() {
    	mRunning = false;
    	mMapDataBO.onStop();
    }
    
    @Override
    public void SuccessDone(Object object) {
    	if(!mRunning) return;
    	if(object == null)
    		return;
    
    	if(ResortsInfo.IsMyInstance(object)) 
    		SuccessResortsInfo((ResortsInfo)object);
    	else if(ResortData.IsMyInstance(object))
    		SuccessResortData((ResortData)object);
    	else if(MapDrawings.IsMyInstance(object))
    		SuccessResortDrawing((MapDrawings)object);
    	else if(MapBundle.IsMyInstance(object)) {
    		SuccessMapBundle((MapBundle)object);
    	}
    }
    	
    protected void SuccessResortsInfo(ResortsInfo resortsInfo) {
    	mResortsInfo = resortsInfo;
    }
    
    protected void SuccessResortData(ResortData resortData) {
    	mResortData = resortData;

    	if(mMapViewController == null) {
    		mMapViewController = new MapViewController(mContext);
    	}
    	
    	mMapViewController.TransformResortDataToMapDrawings(resortData, this);	
    }
    
    protected void SuccessResortDrawing(MapDrawings mapDrawings){
    	Log.v(TAG, "SuccessResortDrawing mapDrawings");
    	mMapDrawings = mapDrawings;
    	GenerateBundle(mapDrawings);
    }
    
    protected void SuccessMapBundle(MapBundle mapBundle) {
    	Log.v(TAG, "SuccessMapBundle mapBundle");
    	mMapBundle = mapBundle;
    }

    protected void GenerateBundle(MapDrawings mapDrawings){
    	if(mMapViewController == null) return;
    	if(mapDrawings == null) return; 
    	if(mResortData == null) return;
    	
    	mMapViewController.GenerateBundle(mapDrawings, this);
    }
    
}
