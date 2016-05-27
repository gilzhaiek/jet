package com.reconinstruments.dashlauncher.radar.maps.bo;

import java.io.IOException;

import com.reconinstruments.dashlauncher.MapsCorruptionActivity;
import com.reconinstruments.dashlauncher.radar.maps.common.MapsHandler;
import com.reconinstruments.dashlauncher.radar.maps.dal.ResortDataDAL;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortData;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortElementsList;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortInfo;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortsInfo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.widget.Toast;


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
	
	public MapDataBO(Context context) {
		mContext = context;		
	}
	
	public void LoadResortInfoTask(MapsHandler mapsHandler) {
		if(mResortDBUnusable || mResortInfoLoaded) 
			return;
		
		mLoadResortInfoHandler = mapsHandler;
		if(mLoadResortInfoTask != null) {
			return;
		}
		
		mLoadResortInfoTask = new LoadResortInfoTask();
		mLoadResortInfoTask.execute((Void[])null);
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
			Intent dialog = new Intent(mContext, MapsCorruptionActivity.class);
			dialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(dialog);
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
					return;
				}
				
				mLoadResortDataHandler.SuccessDone(resortData);
			}
			mLoadResortDataHandler = null;		
			mLoadResortDataTask = null;
		}		
    }
}
