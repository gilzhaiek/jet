package com.reconinstruments.mapImages.bo;

import java.io.IOException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.reconinstruments.mapImages.MapsManager;
import com.reconinstruments.mapImages.MapsManager.State;
import com.reconinstruments.mapImages.common.MapsHandler;
import com.reconinstruments.mapImages.dal.ResortDataDAL;
import com.reconinstruments.mapImages.objects.ResortData;
import com.reconinstruments.mapImages.objects.ResortElementsList;
import com.reconinstruments.mapImages.objects.ResortInfo;
import com.reconinstruments.mapImages.objects.ResortsInfo;


public class MapDataBO {
protected static final String TAG = "MapDataBO";
	
	private static Context 				mContext = null;

	private static ResortDataDAL		mResortDataDAL = null;
	private static ResortElementsList 	mResortElementsList = null;
	private static ResortsInfoBO 		mResortInfoBO = null;
	private static ResortsInfo			mResortsInfo = null;
	private static MapsHandler			mLoadResortInfoHandler = null;
	private static MapsHandler			mLoadResortDataHandler = null;
	private static LoadResortInfoTask	mLoadResortInfoTask = null;
	private static LoadResortDataTask	mLoadResortDataTask = null;
	
	private static ResortInfo			mResortInfo = null;
	
	private static boolean				mResortDBUnusable	= false; 
	private static boolean				mResortInfoLoaded 	= false;
	
	public static MapsManager			mOwner = null;
	
	public ResortsInfoBO GetResortsInfoBO(){
		return mResortInfoBO;
	}
	
	public MapDataBO(Context context) {
		mContext = context;		
	}
	
	public void LoadResortInfoTask(MapsHandler mapsHandler) {
		if(mResortDBUnusable || mResortInfoLoaded) 
			return;
		
		if(mLoadResortInfoTask != null) {
			return;
		}
		mLoadResortInfoHandler = mapsHandler;
		
		mLoadResortInfoTask = new LoadResortInfoTask();
		mLoadResortInfoTask.execute((Void[])null);
    	
    	mOwner.SetState(State.LOADING_RESORT_INFO);

	}
	
	public void onStop(){
		if(mLoadResortDataTask != null) {
			mLoadResortDataTask.cancel(true);
		}
	}
	
	public boolean IsLoadingResortsInfo() {return mLoadResortInfoHandler != null;}
	public boolean IsLoadingResortData() {return mLoadResortDataHandler != null;}
	public boolean IsResortDBCorrupted() {return mResortDBUnusable;}
	
	public void LoadResortDataTask(MapsHandler mapsHandler, ResortInfo resortInfo) throws Exception {
		if(mResortDBUnusable) 
			return;
		
		if(mLoadResortDataTask != null)
			return;
		
//		Log.e(TAG, "in here");
		mResortInfo = resortInfo;
		
		mLoadResortDataHandler = mapsHandler;
		mLoadResortDataTask = new LoadResortDataTask();
		mLoadResortDataTask.execute();
	}
	
    protected static class LoadResortInfoTask extends AsyncTask<Void, Void, ResortsInfo> {

		@Override
		protected ResortsInfo doInBackground(Void... params) {
    	  	if(mResortInfoBO == null) {
        		mResortInfoBO = new ResortsInfoBO(mContext);
        	}
    	  	
        	if(mResortsInfo == null) {
        		mResortsInfo = new ResortsInfo();
        		try {
    				mResortInfoBO.Load(mResortsInfo);
    				
    			} catch (Exception e) {
    				e.printStackTrace();

    				return null;
    			}
        	}
        	
        	return mResortsInfo;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			mLoadResortInfoHandler = null;
			mLoadResortInfoTask = null;
		}	
		
		private void StartResortCorruptedDialog() {
//			Intent dialog = new Intent(mContext, MapsCorruptionActivity.class);
//			dialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			mContext.startActivity(dialog);
		}
		
		@Override
		protected void onPostExecute(ResortsInfo resortsInfo) {
			if(resortsInfo == null) {
				mResortDBUnusable = true;
				StartResortCorruptedDialog();
			}
			else if(resortsInfo.IsCorrupted()) {
				StartResortCorruptedDialog();
			}

			
			mResortInfoLoaded = true;
			mLoadResortInfoHandler.SuccessDone(resortsInfo);
			mLoadResortInfoHandler = null;
			mLoadResortInfoTask = null;
		
			if(resortsInfo == null) {
				mOwner.SetState(MapsManager.State.ERROR_LOADING_RESORT_INFO);
			}
			else {
				mOwner.SetState(MapsManager.State.WAITING_FOR_LOCATION_TO_LOAD_DATA);
			}
		}		
    }    
    
    protected static class LoadResortDataTask extends AsyncTask<Void, Void, ResortData> {
    	
		@Override
		protected ResortData doInBackground(Void... params) {
        	if(mResortElementsList == null) {
        		mResortElementsList = new ResortElementsList();
        		ResortElementsListBO.Initialize(mResortElementsList);
        		mResortDataDAL = new ResortDataDAL(mContext, mResortElementsList);
        	}
        	
        	if(isCancelled()) return null;
        	
        	ResortData resortData = new ResortData();
        	
        	try {
				mResortDataDAL.LoadResortData(this, resortData, mResortInfo);
				mResortDataDAL.FixBoundingBox(resortData, mResortInfo);
			} catch (IOException e) {
				resortData = null;
				e.printStackTrace();
			}

        	if(isCancelled()) return null;
        	
			return resortData;
		}

		 @Override
	    protected void onCancelled() {
			super.onCancelled();
			mLoadResortDataHandler = null;		
			mLoadResortDataTask = null;
	    }		

		@Override
		protected void onPostExecute(ResortData resortData) {
			if(!isCancelled()) {
				if(mLoadResortDataHandler == null)
				{
					Log.v(TAG, "mLoadResortDataHandler is null");
					mLoadResortDataHandler = null;		
					mLoadResortDataTask = null;
					return;
				}
				//mOwner.mDataAvailable = true;
				
				mLoadResortDataHandler.SuccessDone(resortData);
			}
			
			if(resortData == null) {
				mOwner.SetState(MapsManager.State.ERROR_LOADING_DATA);
			}
			else {
				if(resortData.POIs == null && resortData.Areas == null && resortData.Trails == null) {
					mOwner.SetState(MapsManager.State.NO_DATA_AVAILABLE_FOR_THIS_LOCATION);
				}
				else {
					mOwner.SetState(MapsManager.State.BUNDLING_DATA);
				}
			}
			mLoadResortDataHandler = null;		
			mLoadResortDataTask = null;
		}		
    }
}
