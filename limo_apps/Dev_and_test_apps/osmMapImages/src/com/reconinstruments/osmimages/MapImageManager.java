package com.reconinstruments.osmimages;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;

import com.reconinstruments.mapsdk.mapview.MapView;
import com.reconinstruments.mapsdk.mapview.StaticMapGenerator;


/**
 * @author simonwang
 *
 */

public class MapImageManager {

	private static final String TAG = "MapImages";
	private OsmActivity mOsmActivity = null;
	private StaticMapGenerator mStaticMapGenerator = null;
	private OsmTileManager mTileManager = null;
	
	private ArrayList<Long> mTileIdList = null;
	
	public MapImageManager(OsmActivity activity, StaticMapGenerator staticMapGenerator) {
		this.mOsmActivity = activity;
		mStaticMapGenerator = staticMapGenerator;
		
		this.mTileManager = new OsmTileManager(null, mStaticMapGenerator);
		mTileIdList = mTileManager.GetTileIdListFromStorage();
		
	}
	
	public  OsmTileManager getOsmTileManager() {
		return this.mTileManager;
	}

	
	
	public Location						mClosestResortLocation = null;
	public String						mClosestResortName = null;
	public int							mClosestResortIndex = -1;
	
	public GenerateOsmImageThread		    mGenerateOsmImageThread = null;
	public boolean 						mGenerageOsmImage = false;
	

    public void StartGenerateOsmImgs() {
    	Log.v(TAG, "StartGenerateOsmImgs START");
    	if (mGenerateOsmImageThread == null){
        	mGenerateOsmImageThread = new GenerateOsmImageThread(mTileManager);
    	}
    	
		if (mGenerateOsmImageThread.isRunning()) {
			Log.v(TAG, "StartGenerateOsmImgs has already started");
			return;
		}
		
       mGenerateOsmImageThread.start();
		
	}//eof 
    
    public void NotifyImageLoaded(){
    	if (mGenerateOsmImageThread != null)
    		mGenerateOsmImageThread.notifyThread();
    }
    
    public void StopGenerateOsmImgs() {
    	Log.v(TAG, "StopGenerateOsmImgs STOP");
    	
    	if (mGenerateOsmImageThread == null){
    		return;
    	}

		mGenerateOsmImageThread.stopThread();
		try {
			mGenerateOsmImageThread.interrupt();
			mGenerateOsmImageThread = null;
		}
		catch (Exception e) {
			e.printStackTrace();
			mGenerateOsmImageThread = null;
		}

    }

    class GenerateOsmImageThread extends Thread {

 
    	private boolean isRunning = false;
    	private boolean isRunNext = false;
		private boolean isTimeout = true;
		
		private OsmTileManager mTileManager = null;
		private Bitmap mBitmap = null; 
    	
    	public GenerateOsmImageThread(OsmTileManager manager){
    		this.mTileManager = manager;
    	}
    	
    	public boolean isRunning() {
    		return isRunning;
    	}
    	
    	//it is for the MapSDK API's GetBitmap
    	public void run(){
			Log.i(TAG,"GenerateOsmImageThread started.");
			
			
			isRunning = true;
			isRunNext = true;
			OsmBoundingBox rBB = null;
			
			int goodLoaded = 0;
			int badLoaded = 0;
			OsmTile tile = null;
			long id = 0;
			int counter = 0;
			
			Log.v(TAG, "*****total_tileList_size=" + mTileIdList.size());
			
			
			for (Long tileId : mTileIdList) {
				

				
				tile = new OsmTile(tileId);
				rBB = tile.mBound;
				id = tileId.longValue();
				
				
				
				synchronized (this)
				{
					try {
						isTimeout = true;
						Log.v(TAG, "LoadmapDataTask");
						mBitmap = mTileManager.GetBitMapImg(tileId);
						//mTileManager.RenderTile(tileId);
						//Log.v(TAG, "wait 2 mins max to wait loading completion.");
						//this.wait(2*60*1000);
						sleep(100);
					}
					catch(InterruptedException ie){
						Log.v(TAG, "Loading map, interrrupted Exception....");
						ie.printStackTrace();
						++badLoaded;
						mOsmActivity.showErrorMsg();
						continue;
					}
					catch (Exception e) {
						Log.v(TAG, "Loading map tileId=" + tileId + ", Exception....");
						e.printStackTrace();
						mOsmActivity.showErrorMsg();
						++badLoaded;
						continue;
					}
				}//eof synchronized
					
				
				if (mTileManager.isNotGetBitmap()){
					mOsmActivity.showErrorMsg();
					//Log.v(TAG, "Loading map, Timeout.");
					isTimeout = true;
					++badLoaded;
					
				}
				else{
					mOsmActivity.showImg(mBitmap);
					++goodLoaded;

				}
				
				Log.v(TAG, "map loaded, total=" + (goodLoaded+badLoaded) + ", good=" +goodLoaded+", bad=" +badLoaded+", good%=" + goodLoaded*100/(goodLoaded+badLoaded) +", notifiled=" + ((isTimeout==false)? "1" : "0"));
				
				

			

			Log.v(TAG, "sleep 0.1 second to save map....");
			try {

				//wait for loading image
				sleep(100);
			}
			catch (Exception e) {
				Log.v(TAG, "Loading map, sleep Exception....");
			}

			
			mTileManager.SaveCurrentBitmap(tileId);
			
			if (isRunNext) {
				Log.v(TAG, "sleep 0.01 second to next tile...");
				try {
	
					//wait for loading image
					sleep(10);
				}
				catch (Exception e) {
					Log.v(TAG, "Loading map, sleep Exception....");
				}
			
			}else {
				break;
			}
			
			}//eof for
			
			
			isRunning = false;
			isRunNext = false;
			Log.v(TAG, "Completed osm loading, total=" + (goodLoaded+badLoaded) + ", good=" +goodLoaded+", bad=" +badLoaded+", good%=" + goodLoaded*100/(goodLoaded+badLoaded));
			mOsmActivity.showIntialScreen();
			
		}//EOF RUN

    	
    	/*public void run(){
			Log.i(TAG,"GenerateOsmImageThread started.");
			
			
			isRunning = true;
			isRunNext = true;
			OsmBoundingBox rBB = null;
			
			int goodLoaded = 0;
			int badLoaded = 0;
			OsmTile tile = null;
			long id = 0;
			int counter = 0;
			
			Log.v(TAG, "*****total_tileList_size=" + mTileIdList.size());
			
			
			for (Long tileId : mTileIdList) {
				

				
				tile = new OsmTile(tileId);
				rBB = tile.mBound;
				id = tileId.longValue();
				
	
				synchronized (this)
				{
					try {
						isTimeout = true;
						Log.v(TAG, "LoadmapDataTask");
						mTileManager.RenderTile(tileId);
						Log.v(TAG, "wait 2 mins max to wait loading completion.");
						this.wait(2*60*1000);
					}
					catch(InterruptedException ie){
						Log.v(TAG, "Loading map, interrrupted Exception....");
						ie.printStackTrace();
						++badLoaded;
						continue;
					}
					catch (Exception e) {
						Log.v(TAG, "Loading map, Exception....");
						e.printStackTrace();
						++badLoaded;
						continue;
					}
				}//eof synchronized
					
				if (isTimeout){
					
					Log.v(TAG, "Loading map, Timeout.");
					isTimeout = true;
					++badLoaded;
					
				}
				else{
					++goodLoaded;
				}
				
				Log.v(TAG, "map loaded, total=" + (goodLoaded+badLoaded) + ", good=" +goodLoaded+", bad=" +badLoaded+", good%=" + goodLoaded*100/(goodLoaded+badLoaded) +", notifiled=" + ((isTimeout==false)? "1" : "0"));
				
				

			
			if (isRunNext) {
				Log.v(TAG, "sleep 2 second to save map....");
				try {

					//wait for loading image
					sleep(2*1000);
				}
				catch (Exception e) {
					Log.v(TAG, "Loading map, sleep Exception....");
				}
			}
			
			mTileManager.SaveCurrentTileImage(tileId);
			
			}//eof for
			
			
			isRunning = false;
			isRunNext = false;
			Log.v(TAG, "Completed osm loading, total=" + (goodLoaded+badLoaded) + ", good=" +goodLoaded+", bad=" +badLoaded+", good%=" + goodLoaded*100/(goodLoaded+badLoaded));
			
		}//EOF RUN*/
    	
       public void notifyThread(){
    	   Log.v(TAG, "notifyThread");
    	   synchronized (this){
    		   Log.v(TAG, "notify1.");
    		   isTimeout = false;
    		   this.notify();
    	   }
    	   
       }
       public void stopThread(){
    	   isRunNext = false;
    	   notifyThread();
       }

    }//eof class 

    
	
}//eof MapImageManager
