package com.reconinstruments.dashlauncher.radar.maps.controllers;

import java.util.concurrent.locks.ReentrantLock;

import com.reconinstruments.dashlauncher.radar.maps.bo.MapDrawingsBO;
import com.reconinstruments.dashlauncher.radar.maps.bundlers.MapBundle;
import com.reconinstruments.dashlauncher.radar.maps.bundlers.MapBundler;
import com.reconinstruments.dashlauncher.radar.maps.common.MapDrawingsHandler;
import com.reconinstruments.dashlauncher.radar.maps.common.MapsHandler;
import com.reconinstruments.dashlauncher.radar.maps.drawings.MapDrawings;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortData;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class MapViewController extends MapDrawingsHandler {
	protected static final String TAG = "MapViewController";
	
	private static Context			mContext = null;
	private static MapDrawingsBO	mMapDrawingsBO = null;
	private static MapsHandler		mMapHandler = null;
	private static final String 	MAP_BITMAP_NAME = "resort";
	private static CreateBundleTask mCreateBundleTask = null;
    
	public MapViewController(Context context) {
		mContext = context;
    }
	
	public void TransformResortDataToMapDrawings(ResortData resortData, MapsHandler mapHandler) {
		if(mMapDrawingsBO == null)
			mMapDrawingsBO = new MapDrawingsBO(this);
		
		mMapHandler = mapHandler;
		
		try {
			mMapDrawingsBO.TransformResortData(resortData, mMapDrawings);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	// onProgressUpdate from mMapDrawingsBO.TransformResortData
	@Override
    public void onProgressUpdate(int progress) {
    	super.onProgressUpdate(progress);
    }
    
	// onPostExecute from mMapDrawingsBO.TransformResortData
	@Override
    public void onPostExecute(MapDrawings mapDrawings) {
    	super.onPostExecute(mapDrawings);
    	mMapHandler.SuccessDone(mapDrawings);
    }
	
	public void GenerateBundle(MapDrawings mapDrawings, MapsHandler mapHandler){
		if(mCreateBundleTask != null)
			return;
		
		mMapHandler = mapHandler;
		
		mCreateBundleTask = new CreateBundleTask();
		mCreateBundleTask.execute(mapDrawings);		
	}
	
	protected static class CreateBundleTask extends AsyncTask<MapDrawings, Void, MapBundle> {
		protected static final ReentrantLock mLock = new ReentrantLock();

		@Override
		protected MapBundle doInBackground(MapDrawings... params) {
			MapBundle mapBundle = new MapBundle();
			try {
				mLock.lock(); 
			
				MapDrawings mapDrawings = params[0];
				
				Log.e(TAG, "doInBackground..");
	    	  		    	
		    	if(mapDrawings == null) {
		    		Log.e(TAG, "mapDrawings is null");
		    	}
		    	
		    	MapBundler.GenerateMapImages(mContext, mapBundle.mBundle, MAP_BITMAP_NAME,mapDrawings);
		    	
		        try {
					MapBundler.GenerateMapBundle(mapBundle.mBundle, mapDrawings);
					Log.v(TAG, "doInBackground - " + mContext.getFilesDir() + "/" + MAP_BITMAP_NAME );
				} catch (Exception e) {
					mapBundle.Release();
					mapBundle = null;
				}
			}
			catch (Exception e) {}
			finally {mLock.unlock();}
			
			return mapBundle;
		}
  
		@Override
		protected void onPostExecute(MapBundle mapBundle) {
			Log.v(TAG, "onPostExecute(mapBundle)");
			if(mapBundle != null) {
				Log.v(TAG, "mMapHandler.SuccessDone(mapBundle)");
				mMapHandler.SuccessDone(mapBundle);
			}
			mCreateBundleTask = null;
		}		
    }		
}
