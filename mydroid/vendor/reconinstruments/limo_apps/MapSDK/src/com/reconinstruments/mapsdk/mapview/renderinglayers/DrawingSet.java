package com.reconinstruments.mapsdk.mapview.renderinglayers;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.Log;

import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;

public class DrawingSet {
// a drawingSet holds two set of data used by a rendering layer's draw() method.  One set of data is active and the alternate is used for background loading.  
// The drawingset is continually polled to see if there is new data loaded in the alternate set.  If so, the sets switch roles, alternate = active and active = alternate.
	
	private final static String TAG = "DrawingSet";
	
//	private final static String LAYER_STATIC_POIS = "Static POIs";
//	private final static String LAYER_BUDDIES = "Buddies";
//	private final Semaphore mAccessToWaitingForTilesArray = new Semaphore(1, true);
	
	public enum DSChangeResponseCode {
		UNDEFINED,
		ERROR_DURING_DATA_LOAD,
		NO_DATA_AVAILABLE,
		DATA_LOADING,
		DATA_LOADED
	}
	public enum DrawingSetLoadState {
		INACTIVE,
		LOADED,
		LOADING
	}

	public  boolean					mInitialized = false;						// true after first data is loaded
	public	int						mCurIndex = 0;
    public  DrawingSetLoadState[] 	mLoadState = new DrawingSetLoadState[2];
	public  boolean					mCancelLoad = false;						// set true if canceling background/asynch load of alternate drawing set
	public  boolean					mIgnoreCurrentLoad = false;					// set true if you want background task to ignore the current load, ie, not set mUpdateAvailable
	public 	boolean					mUpdateAvailable = false;
	public 	RenderSchemeManager		mRSM=null;


	public DrawingSet(RenderSchemeManager rsm) {
		mRSM = rsm;
		for(int i=0; i<2; i++) {
			mLoadState[i] = DrawingSetLoadState.INACTIVE;
		}
	}
	
	public void IgnoreCurrentLoad() {
		mIgnoreCurrentLoad = true;
	}
	
	public boolean SwitchIfUpdateReady() {
		Log.e(TAG, "SwitchIfUpdateReady: " +  mUpdateAvailable);
		if(mUpdateAvailable) {
			mCurIndex = (mCurIndex == 0) ? 1 : 0;
			mUpdateAvailable = false;
			return true;
		}
		return false;
	}

	public void CancelLoad(boolean cancelLoad) {  // @Override in subclasses if needed
		mCancelLoad = cancelLoad;		// skips unfinished pieces of load 
		mIgnoreCurrentLoad = false;
		SetLoadingIndicator(false);
	}
	public void ResetLoadParamters() {
		mCancelLoad = false;
		mIgnoreCurrentLoad = false;
	}
	public void ResetAllParamters() {
		mCancelLoad = false;
		mIgnoreCurrentLoad = false;
		mInitialized = false;
		mUpdateAvailable = false;
	}
	public boolean IsCancellingPrevLoadTask() {
		return mCancelLoad;
	}
	
	public boolean IsInitializedWithData() {
		return mInitialized;
	}
	
	public void SetLoadingIndicator(boolean value) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		if(value) {
			mLoadState[nextIndex] = DrawingSetLoadState.LOADING;
		}
		else {
			mLoadState[nextIndex] = DrawingSetLoadState.INACTIVE;
		}
	}


// to Override in subclasses	
	public void Draw(Canvas canvas, CameraViewport camera, String focusedObjectID, Resources res) {
		
	}  
	
}
