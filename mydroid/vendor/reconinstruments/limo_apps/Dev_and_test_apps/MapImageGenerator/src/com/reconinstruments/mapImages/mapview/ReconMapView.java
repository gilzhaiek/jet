package com.reconinstruments.mapImages.mapview;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.reconinstruments.mapImages.MapsManager;
import com.reconinstruments.mapImages.ReconMapFragment;
import com.reconinstruments.mapImages.bo.MapDrawingsBO;
import com.reconinstruments.mapImages.controllers.MapViewController;
import com.reconinstruments.mapImages.drawings.MapDrawings;
import com.reconinstruments.mapImages.drawings.RenderSchemeManager;
import com.reconinstruments.mapImages.helpers.LocationTransformer;
import com.reconinstruments.mapImages.mapdata.DataSourceManager;
import com.reconinstruments.mapImages.mapdata.MapDataCache;
import com.reconinstruments.mapImages.mapdata.MapLayer;
import com.reconinstruments.mapImages.mapdata.MapObject;
import com.reconinstruments.mapImages.mapdata.MapDataCache.IMapDataCacheCallbacks;
import com.reconinstruments.mapImages.mapdata.MapDataCache.MapCacheResponseCode;
import com.reconinstruments.mapImages.mapview.MapBuddyInfo;
import com.reconinstruments.mapImages.objects.POI;
import com.reconinstruments.mapImages.prim.PointD;
import com.reconinstruments.mapImages.R;
import com.reconinstruments.utils.SettingsUtil;

import java.io.RandomAccessFile;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.Math;
import android.graphics.Bitmap;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.Math;
import android.graphics.Bitmap;
import android.os.Environment;

/**
 * @author stevenmason
 *
 */
//public class ReconMapView extends SurfaceView implements IMapDataManagerLoadDataResult, SurfaceHolder.Callback {
public class ReconMapView extends View implements IMapDataCacheCallbacks {
	private static final String TAG = "ReconMapView";
	private static final int USER_ICON_SIZE = 15;
	private static final int MAX_NUM_OFFSCREEN_BUDDIES = 10;
	private static final int RETICULE_DISTANCE = 61;
//	private static final float PAN_RELOAD_THRESHOLD = 0.20f; // percent of width/height (which are equal)	
	private static final double ROTATE_RELOAD_THRESHOLD = 30.0; // need this change in degrees before map will reload on rotation	

    private static long REMOVE_BUDDY_TIME_MS = 7200000;		// 2hrs
//    private static long REMOVE_BUDDY_TIME_MS = 120000;  // for testing	2 mins
    private static long FADE_OFFLINE_BUDDY_TIME_MS = 300000; //change offline threshold from 1min to 5mins. old value = 60000;


	public Bitmap latestMap1 = null;
	public Bitmap latestMap2 = null;
	
	public enum MapViewState {
	
		EMPTY,
		STARTING_DATA_SERVICE,
		OUT_OF_MEMORY,
		SCHEME_LOAD_ERROR,		// issue loading rendering schemes
		LOADING_DATA,
		WAITING_FOR_LOCATION,
		ERROR_DATALOAD,
		MAP,
		NO_DATA
	}

	private ReconMapFragment mParentFragment = null;
	private RenderSchemeManager mRenderSchemeManager = null;

	private MapViewState	mViewState = MapViewState.EMPTY;
	private boolean 		mShowClosestItemDescription = false;	
	private boolean			mMapLoaded = false;
	private double 			mUserLatitude = 0.0;
	private double 			mUserLongitude = 0.0;
	private float 			mMapHeading = 0.0f;
	private float 			mUserHeading = 0.0f;
	private Matrix 			mUserIconRotationMatrix = new Matrix();
	
	int 					mMapDimension = 2;
	double 					mInitRegionWidthInMeters = -1;
    long 					mFrameIntervalInMS = 1000/15;		// ms per frame 

	private MapLayer		mMarkerLayer = null;		// holds code created markers 
	private MapDataCache 	mMapDataCache=null;
//	private ViewportRegion	mCurrentViewportRegion=null;
	protected boolean		mTrackUser = false;
    private Paint		    mClosestItemBoxPaint;
    private Paint		    mClosestItemBoxOutlinePaint;
    private TextPaint		mClosestItemOutlinePaint;
    private TextPaint		mClosestItemPaint;
    private TextPaint		mTextPaint;
    private TextPaint		mSubTextPaint;
    private float			mTextHeight = 0;
    private int 			mTextX=0;
    private int 			mTextY=0;
	private double		    mAspectRatio = 0.0;
	private Bitmap			mUserIcon = null;
	private Bitmap			mUserReticuleIcon = null;
	private Bitmap			mResortReticuleIcon = null;
	private Bitmap			mBuddyReticuleIcon = null;
	Bitmap					mGridBackdrop =null;
	Bitmap					mScaledGridBackdrop =null;
	public LocationTransformer		mLocationTransformer= null;
	RectF 					mResortBBInView = new RectF();
//	private Timer 			mPanTimer = new Timer();
//	private TimerTask 		mBlockLocUpdatesCallback = GetBlockLocationUpdatesTask();

	DrawingSet[]			mDrawingSet = new DrawingSet[2];
	int						mCurDrawingSetIndex = 0;
	int						mLoadingDrawingSetIndex = 0;
	boolean					mNewDrawingSet = false;			// set true when a new DrawingSet is available
	CameraViewport			mCameraViewport = null;
	boolean					mIsRotatingMap = false;
	boolean					mDrawingMap = false;
	boolean					mNotEnoughMemoryError = false;
	boolean					mShowGrid = false;
	boolean 				mIsGenerateImage = false;
	
	MapDrawings 			mBestMapDrawing = null;
	int						mWaitingForLocationTimer = 0;
	

    ArrayList<MapBuddyInfo>	mCurrentBuddies = new ArrayList<MapBuddyInfo>();

	PointD 	mDrawingUserPosition   		= new PointD();
    PointD 	mDrawingCameraViewportCenter   	= new PointD();
	RectF	mDrawingCameraViewportBoundary 	= new RectF();
	RectF	mDrawingCameraViewportTestBoundary 	= new RectF();
	RectF   mDrawingGeoRegionLoadBoundary  	= new RectF();
	RectF   mDrawingResortTestBoundary  	= new RectF();
	RectF	mLoadingGeoRegionBoundary 	= new RectF();
	PointD 	mLoadingGeoRegionCenter   	= new PointD();
	RectF	mDrawingReticuleBoundary 	= new RectF();
	double  mMaxDrawingY = 0.0;
	LoadDrawingSetTask 	mLoadDrawingTask = null;
	RectF	mLoadingBoundsInGPS = new RectF();
	boolean mProcessingLoadRequest = false;
	
	int		mReticuleRadius = 24;
	Matrix	mDrawingTransform = null;
	float[] dl = new float[2];
	boolean mIsLoadingDrawingSet = false;
	boolean mNewDrawingSetAvailable = false;

	RectF mDescRect = new RectF();
	Matrix mLoadTransformMatrix = new Matrix(); 
	Matrix mDrawTransformMatrix = new Matrix(); 
	Paint linePaint = new Paint();
	RollOverResult	mPOIRollOverResult = new RollOverResult(null, -1);
	RollOverResult	mBuddyRollOverResult = new RollOverResult(null, -1);
    
	private boolean 		mShowPOI = true;  // temp state var for initial demostration of "layer hiding"

	int	mNumLoadTasksStarted = 0;
	int	mNumLoadTasksCompleted = 0;
	int	mNumLoadTasksCancelled = 0;
	
	
	long mViewRefreshClockStartTime;

	
	public Bitmap latesBitmap = null;
	
	Handler mHandler = new Handler();
	Runnable mTick = new Runnable() {
	    public void run() {
	    	mCameraViewport.UpdateViewport();	// track target viewport
	        invalidate();						// force redraw
			mHandler.postDelayed(this, mFrameIntervalInMS); 	// queue repeated call to run() in future
	    }
	};

	void startViewRefreshClock() {
		mViewRefreshClockStartTime = SystemClock.uptimeMillis();
	    mHandler.removeCallbacks(mTick);
	    mHandler.post(mTick);
	}

	void stopViewRefreshClock() {
	    mHandler.removeCallbacks(mTick);
	}
	
	
	public ReconMapView(Context context, AttributeSet attrs) {
 		super(context, attrs);

		mMarkerLayer = new MapLayer("Marker");


		//mGridBackdrop = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.grid_backdrop), 428,240,false);
//		mGridBackdrop = BitmapFactory.decodeResource(getResources(), R.drawable.grid_backdrop);

		mLocationTransformer = new LocationTransformer();
		
		
        mUserIcon = BitmapFactory.decodeResource(getResources(), R.drawable.user_arrow);
		mUserReticuleIcon = BitmapFactory.decodeResource(getResources(), R.drawable.user_reticule_icon);
		mBuddyReticuleIcon =BitmapFactory.decodeResource(getResources(), R.drawable.buddy_reticule_icon);
		mResortReticuleIcon =BitmapFactory.decodeResource(getResources(), R.drawable.resort_reticule_icon);

		mDrawingSet[0] = new DrawingSet();
		mDrawingSet[1] = new DrawingSet();
	
	}

    
    //======================================================================================
	// control / state API
	
	// map  
	public boolean IsMapLoaded () {
		return mMapLoaded;
	}
	// layer control
	public String[] GetAvailableLayers () {
		String[] layers = null;
		return layers;
	}
	public void ShowLayer(String layerID, int order) {
		// find layer in 
	}
	public void HideLayer(String layerID) {
		
	}
	public void EnableLayer(String layerID) {
		 
	}
	public void DisableLayer(String layerID) {
		
	}
	public void BringLayerToFront(String layerID) {
		
	}
	public void SendLayerToBack(String layerID) {
		
	}
	public void MapRotates (boolean state) {
		mIsRotatingMap = state;
	}

	public void SetMapHeading(float heading) {
		mMapHeading = heading;
//		Log.i(TAG, "new map heading - " + heading);
		
		if(mCameraViewport != null) {
			mCameraViewport.SetViewAngleRelToNorth(heading, false);
		}
	}
	
	public void SetUserHeading(float heading) {
		mUserHeading = heading;
//		Log.i(TAG, "new user heading - " + heading);
	}
	
	public void SetMapCenterToUserPosition(double newLatitude, double newLongitude) {
//		Log.e(TAG, "in SetMapCenterToUserPosition");
		mUserLatitude = newLatitude;
		mUserLongitude = newLongitude;
		SetMapCenter(newLatitude,newLongitude);
	}
	
	public void SetUserPosition(double newLatitude, double newLongitude) {
//		Log.e(TAG, "in SetUserPosition");
		mUserLatitude = newLatitude;
		mUserLongitude = newLongitude;
		invalidate();
	}

	public void SetMapCenter(double newLatitude, double newLongitude) {	
		
		mCameraViewport.SetCenter(newLatitude, newLongitude, true); 
		LoadRegion(mCameraViewport);  
	}
	
	public void SetCurrentViewscale(float prevScale) {
//		Log.v(TAG, "setting scale " + prevScale);

		if(mCameraViewport != null) {
			mCameraViewport.SetScale(prevScale, false);
		}
	}

	public float GetCurrentViewscale() {
		if(mCameraViewport != null) {
			return mCameraViewport.GetCurrentScale();
		}
		return 1.0f;
	}


	// markers - copy Google Maps API
	public int AddMapObject(MapObject marker) { 	
		// put marker in mMarkerLayer
		mMarkerLayer.addObject(marker);
		return 0;
	}

	public void ShowClosestItemDescription(boolean enable) {
		mShowClosestItemDescription = enable;
	}
	
	public void ShowGrid(boolean enable) {
		mShowGrid = enable;
	}
	
	public void isGenerateImage(boolean isGenerate) {
		if (mIsGenerateImage == isGenerate)
			return;
		
		mIsGenerateImage = isGenerate;
		invalidate();
	}
	
	public interface IMapView {
		public void MapViewStateChanged(ReconMapView.MapViewState mapViewState);		
		public void LoadMapDataComplete(String link);
	}

    //======================================================================================
	// view port adjustment
	public void FreezePan() {
		mCameraViewport.FreezePan();
	}
	
	public void PanUp(boolean nudge) {
		mCameraViewport.panUp(nudge);
		LoadRegion(mCameraViewport);						
	}
	public void PanDown(boolean nudge) {
		mCameraViewport.panDown(nudge);
		LoadRegion(mCameraViewport);						
	}
	public void PanLeft(boolean nudge) {
		mCameraViewport.panLeft(nudge);
		LoadRegion(mCameraViewport);						
	}
	public void PanRight(boolean nudge) {
		mCameraViewport.panRight(nudge);
		LoadRegion(mCameraViewport);						
	}
	public double ZoomIn() {
		double newScale = mCameraViewport.ZoomIn();
//		mScaledGridBackdrop = Bitmap.createScaledBitmap(mGridBackdrop, iconSize, iconSize, false);

		invalidate();				// instead of LoadRegion under assumption that MapDataCache always loads data for zoom level 0
		return newScale;
	}
	public double ZoomOut() {
		double newScale = mCameraViewport.ZoomOut();
		invalidate();				// instead of LoadRegion under assumption that MapDataCache always loads data for zoom level 0
		return newScale;
	}
	public void TogglePOI() {
		mShowPOI = !mShowPOI;
		invalidate();
	}

    //======================================================================================
	// lifecycle callbacks - designed to be called from parent activity/fragment lifecycle routines
	public void onCreate(int dimensions, double initRegionWidthInMeters, DataSourceManager dsm, boolean trackUser, ReconMapFragment parentFragment,RenderSchemeManager rsm){
		Log.i(TAG,"onCreate "+System.currentTimeMillis());
		
		this.setDrawingCacheEnabled(true);
		
		mMapDimension = dimensions;
		mMapDataCache = new MapDataCache(this, dsm);
		mTrackUser = trackUser;
		mInitRegionWidthInMeters = initRegionWidthInMeters;
		mCameraViewport = new CameraViewport(initRegionWidthInMeters, rsm.GetZoomLevels(), rsm.GetMinZoomLevel(),
				(float)rsm.GetRate(RenderSchemeManager.Rates.SCALE_RATE), 
				(float)rsm.GetRate(RenderSchemeManager.Rates.ROTATION_RATE), 
				rsm.GetRate(RenderSchemeManager.Rates.PAN_RATE));		// set geoRegion scale in world distance units (meters)
		mParentFragment = parentFragment;
		mRenderSchemeManager = rsm;
		
		mClosestItemBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
		mClosestItemBoxPaint.setStyle(Style.FILL) ;
		mClosestItemBoxPaint.setColor(mRenderSchemeManager.GetPanRolloverBoxBGColor());		
		mClosestItemBoxPaint.setAlpha(mRenderSchemeManager.GetPanRolloverBoxBGAlpha());
		mClosestItemBoxPaint.setAntiAlias(true);
		
		mClosestItemBoxOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
		mClosestItemBoxOutlinePaint.setStyle(Style.STROKE) ;
		mClosestItemBoxOutlinePaint.setColor(mRenderSchemeManager.GetPanRolloverBoxOutlineColor());		
		mClosestItemBoxOutlinePaint.setAlpha(mRenderSchemeManager.GetPanRolloverBoxOutlineAlpha());
		mClosestItemBoxOutlinePaint.setStrokeWidth(mRenderSchemeManager.GetPanRolloverBoxOutlineWidth());
		mClosestItemBoxOutlinePaint.setAntiAlias(true);
		
		mClosestItemPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
        mClosestItemPaint.setTextSize(mRenderSchemeManager.GetPanRolloverTextSize());
		mClosestItemPaint.setAntiAlias(true);
		mClosestItemPaint.setTextAlign(Align.CENTER);
		mClosestItemPaint.setTypeface(Typeface.SANS_SERIF);
		mClosestItemPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));		
		mClosestItemPaint.setColor(mRenderSchemeManager.GetPanRolloverTextColor());		
		mClosestItemPaint.setAlpha(255);

		mClosestItemOutlinePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
		mClosestItemOutlinePaint.setTextSize(mRenderSchemeManager.GetPanRolloverTextSize());
		mClosestItemOutlinePaint.setTextAlign(Align.CENTER);
		mClosestItemOutlinePaint.setAntiAlias(true);
		mClosestItemOutlinePaint.setTypeface(Typeface.SANS_SERIF);
		mClosestItemOutlinePaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));		
		mClosestItemOutlinePaint.setColor(mRenderSchemeManager.GetPanRolloverTextOutlineColor());		
		mClosestItemOutlinePaint.setStyle(Paint.Style.STROKE);
		mClosestItemOutlinePaint.setStrokeWidth(mRenderSchemeManager.GetPanRolloverTextOutlineWidth());
		mClosestItemOutlinePaint.setAlpha(255);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.bgColor = Color.BLACK;
        mTextPaint.setTextSize(24);
        if (mTextHeight == 0) {
            mTextHeight = mTextPaint.getTextSize();
        } else {
            mTextPaint.setTextSize(mTextHeight);
        }
        mSubTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
        mSubTextPaint.setColor(Color.WHITE);
        mSubTextPaint.bgColor = Color.BLACK;
        mSubTextPaint.setTextSize(mTextHeight-4);
        
        mFrameIntervalInMS = (long)(1000.0/mRenderSchemeManager.GetRate(RenderSchemeManager.Rates.FRAME_RATE));
        
	}
	
	public void onResume(){
		if(mAspectRatio == 0.0) {
//			mAspectRatio = (double)getWidth()/(double)getHeight();
			mAspectRatio = 428./240.;
			mCameraViewport.SetViewPortDimensions(getWidth(),getHeight());
		}
		mWaitingForLocationTimer = 0;
		int imageSize = 980;
		mNotEnoughMemoryError = false;
		if(!mDrawingSet[0].InitDrawingCanvas(imageSize)) {
			mNotEnoughMemoryError = true;
			mDrawingSet[0].ReleaseImageResources();		
		}
		if(!mDrawingSet[1].InitDrawingCanvas(imageSize)) {
			mNotEnoughMemoryError = true;
			mDrawingSet[0].ReleaseImageResources();
			mDrawingSet[1].ReleaseImageResources();
		}
		
		startViewRefreshClock();
	}

	public void onPause(){
		stopViewRefreshClock();
//		mDrawingSet[0].ReleaseImageResources();	// as maps is a critical feature/service, avoid release / realloc of large memory chunks until maps service is rewritten and disconnected
												// let system reclaim if from background if necessary.. This decision is due because of delays in Java garbage collection could cause moving into and out of maps quickly to thrash the heap
												// resulting in an Out of Memory error experience for the user.
//		mDrawingSet[1].ReleaseImageResources();
	}
	
	public void onDestroy(){
		stopViewRefreshClock();
		mDrawingSet[0].ReleaseImageResources();
		mDrawingSet[1].ReleaseImageResources();
	}

	
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
//		mTextX = w/2;
//		mTextY = h/2+5;
		mTextX = 428/2;
		mTextY = 240/2+5;
		mAspectRatio = (double)mTextX/(double)mTextY;
		if(mCameraViewport != null) {
			mCameraViewport.SetViewPortDimensions(mTextX,mTextY);
		}
		invalidate();
	}
	
	public class RollOverResult {
		public String mDescription=null;
		public double mDistance = -1;
		
		public RollOverResult(String desc, double dist) {
			mDescription = desc;
			mDistance = dist;
		}
	}
	
	private void drawText(Canvas canvas) {
		
		String text = "Press 'UP' key to generate images.      " ; 
		float xoffset = mTextPaint.measureText(text)/2;
		canvas.drawText(text, mTextX-xoffset, mTextY-80, mTextPaint);
		
		text =        "Press 'DOWN' key to stop generation."; 
		xoffset = mTextPaint.measureText(text)/2;
		canvas.drawText(text, mTextX-xoffset, mTextY-60, mTextPaint);
		
		
		String status;
		if (mIsGenerateImage)
			status = "Status: Generating Images...";
		else
			status = "Status: Not Generating Images.";
		
		xoffset = mSubTextPaint.measureText(status)/2;
		canvas.drawText(status, mTextX-xoffset, mTextY+30, mSubTextPaint);
		
	}
	@Override
	public void onDraw(Canvas canvas){
		super.onDraw(canvas);
		this.setDrawingCacheEnabled(true);
		
//		Log.i(TAG,"DSs: "+mDrawingSet[0].IsLoaded() + " | "+mDrawingSet[1].IsLoaded());
		
		// draw background gird...
		float xoffset=0;
		String text = "";
		
		canvas.drawColor(Color.argb(255, 0x1B, 0x1D, 0x21));
//		Log.e(TAG,"----------" + backgroundColor);
		
		if(mRenderSchemeManager.mErrorSchemesNotLoadedCorrectly) {
			mViewState = MapViewState.SCHEME_LOAD_ERROR;	// will only happen if render scheme file is corrupted or missing
		}
		if(mNotEnoughMemoryError) {
			mViewState = MapViewState.OUT_OF_MEMORY;	// will only happen if couldn't access enough memory for drawing sets
		}
		

				
		switch(mViewState) {
		case EMPTY:
			// clear canvas;
			break;
		case STARTING_DATA_SERVICE:
			// clear canvas;
			text = mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.STARTUP);
			xoffset = mTextPaint.measureText(text)/2;
			canvas.drawText(text, mTextX-xoffset, mTextY-20, mTextPaint);
			break;
		case OUT_OF_MEMORY:
			// clear canvas;
			text = mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.OUT_OF_MEMORY);
			xoffset = mTextPaint.measureText(text)/2;
			canvas.drawText(text, mTextX-xoffset, mTextY-20, mTextPaint);
			break;
			
		case SCHEME_LOAD_ERROR:
			// show loading message/icon
			text = mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.SCHEME_ERROR);
			xoffset = mTextPaint.measureText(text)/2;
			canvas.drawText(text, mTextX-xoffset, mTextY-20, mTextPaint);
			break;
		case WAITING_FOR_LOCATION:
			// show loading message/icon
			/*if(mWaitingForLocationTimer < 30) {
				mWaitingForLocationTimer ++;
				text = mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.STARTUP);
			}
			else {
				text = mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.WAITING_FOR_LOCATION);
			}
			xoffset = mTextPaint.measureText(text)/2;
			canvas.drawText(text, mTextX-xoffset, mTextY-20, mTextPaint);*/
			break;
		case LOADING_DATA:
			// show loading message/icon
			text = mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.LOADING_DATA);
			xoffset = mTextPaint.measureText(text)/2;
			canvas.drawText(text, mTextX-xoffset, mTextY-20, mTextPaint);
			if(mMapDataCache != null) {
				String rName = mMapDataCache.GetClosestResortName();
				if(rName != null) {
					String trName;
					if(rName.length() > 40) trName = rName.substring(0, 36) + "...";	// limit long names
					else trName = rName;
//					text = "<resort name here tbd>";
					xoffset = mSubTextPaint.measureText(trName)/2;
					canvas.drawText(trName, mTextX-xoffset, mTextY+20, mSubTextPaint);
				}
			}
			break;
		case NO_DATA:
			// show loading message/icon
			text = mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.NO_DATA);
			xoffset = mTextPaint.measureText(text)/2;
			canvas.drawText(text, mTextX-xoffset, mTextY-40, mTextPaint);
			if(mMapDataCache != null) {
				String rName = mMapDataCache.GetClosestResortName();
				if(rName != null) {
					double rDist = mMapDataCache.GetDistanceInMetersToClosestResort(mUserLatitude, mUserLongitude);
					String distStr = "";
					if(rDist >= 0.) {
					    // We branch out based on units used
					    int units = SettingsUtil.getUnits(mParentFragment.getActivity());
					    if ( units == SettingsUtil.RECON_UNITS_METRIC) {
						if(rDist < 1000.) distStr = ", " + (int)rDist + "m";
						else distStr = ", " + (int)(rDist/1000.) + "km";
					    }
					    else { // Imperial
						distStr = ", "+((double)((int)(rDist/160.93)))/10. + "Mi";
					    }
					}
					String trName;
					if(rName.length() > 32) trName = rName.substring(0, 28) + "..." + distStr;	// limit long names
					else trName = rName + distStr;

					text = mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.CLOSEST_RESORT) ;
					xoffset = mSubTextPaint.measureText(text)/2;
					canvas.drawText(text, mTextX-xoffset, mTextY+0, mSubTextPaint);
					
					xoffset = mSubTextPaint.measureText(trName)/2;
					canvas.drawText(trName, mTextX-xoffset, mTextY+34, mSubTextPaint);

				}
			}
			break;
		case ERROR_DATALOAD:
			// show error message
			text = mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.ERROR_LOADING_DATA);
			xoffset = mTextPaint.measureText(text)/2;
			canvas.drawText(text, mTextX-xoffset, mTextY-20, mTextPaint);
			break;
		case MAP:
			//Log.e(TAG,"$$$$$$$$$$$$ ReconMApView.onDraw().map_view");
//			if(!mDrawingMap) {
//				Log.d(TAG, "in Drawing map...");
				if(mRenderSchemeManager.mLoadingSettingsFile) return;	// don't try and draw map if rendering scheme data is being (re)loaded 

				mDrawingMap = true;
				if(mNewDrawingSetAvailable) {	// if new DrawingSet flagged, switch to it
					mCurDrawingSetIndex = mCurDrawingSetIndex == 0 ? 1 : 0;
					Log.i(TAG,"New drawing set available: " + mCurDrawingSetIndex);
					mNewDrawingSetAvailable = false;
				}


				if(mShowGrid) {
					int backgroundColor = mRenderSchemeManager.GetGridBkgdColor();
					canvas.drawColor(Color.argb(255, backgroundColor, backgroundColor, backgroundColor));
					
					int i;
					int lineColor = mRenderSchemeManager.GetGridLineColor();
					linePaint.setColor(Color.argb(mRenderSchemeManager.GetGridAlpha(), lineColor, lineColor, lineColor));
					linePaint.setStrokeWidth(mRenderSchemeManager.GetGridLineWidth());

					int stepSize;
					if(mCameraViewport != null) {
						stepSize = (int)(40 * mCameraViewport.GetCurrentScale());
					}
					else {
						stepSize = 40;
					}
//					double angle = mMapHeading % 90.0;
//					if(angle > 45) angle -= 90.0;
					int offsetX = (int)(428 % stepSize / 2) - stepSize/2;
					int offsetY = (int)(240 % stepSize / 2);
					for(i=offsetX;i<428;i+=stepSize) 
//						canvas.drawLine(i + (float)(Math.sin(angle)*120.0), 0, i- (float)(Math.sin(angle)*120.0), 240 , linePaint);
						canvas.drawLine(i, 0, i, 240, linePaint);
					for(i=offsetY;i<240;i+=stepSize) 
//						canvas.drawLine(0, i + (float)(Math.cos(angle)*214.0), 428, i - (float)(Math.cos(angle)*214.0), linePaint);
						canvas.drawLine(0, i , 428, i , linePaint);
				}

				if(!mDrawingSet[mCurDrawingSetIndex].IsLoaded()) {

//					text = mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.CACHE_DATA_LOADED);
//					xoffset = mTextPaint.measureText(text)/2;
//					canvas.drawText(text, mTextX-xoffset, mTextY-20, mTextPaint);
//					text = "<resort name here tbd>";
//					xoffset = mSubTextPaint.measureText(text)/2;
//					canvas.drawText(text, mTextX-xoffset, mTextY+20, mSubTextPaint);

					if(!mIsLoadingDrawingSet) {
						LoadDrawingSet(mCurDrawingSetIndex == 0 ? 1 : 0, false); // using viewport target
					}
				}
				else {

					if(mMapDataCache.GetBestRMDForBounds(mCameraViewport) != null) {

						float viewScale = mCameraViewport.GetCurrentScale();


						mDrawingCameraViewportCenter.x = mLocationTransformer.TransformLongitude(mCameraViewport.mCurrentLatitude, mCameraViewport.mCurrentLongitude);
						mDrawingCameraViewportCenter.y = mMaxDrawingY - mLocationTransformer.TransformLatitude(mCameraViewport.mCurrentLatitude, mCameraViewport.mCurrentLongitude);// LocationTransformer does flip of Y axis...

						RefreshDrawingSet(mCurDrawingSetIndex, mCameraViewport.mCurrentLatitude, mCameraViewport.mCurrentLongitude, mCameraViewport.mTargetScale);

						mDrawingCameraViewportBoundary.left =   (float)(mLocationTransformer.TransformLongitude(mCameraViewport.mBB.top, mCameraViewport.mBB.left));
						mDrawingCameraViewportBoundary.right =  (float)(mLocationTransformer.TransformLongitude(mCameraViewport.mBB.top, mCameraViewport.mBB.right));
						mDrawingCameraViewportBoundary.bottom = (float)mMaxDrawingY - (float)(mLocationTransformer.TransformLatitude(mCameraViewport.mBB.top, mCameraViewport.mBB.left));   // top and bottom flipped as GPS are positive up and graphics is positive down
						mDrawingCameraViewportBoundary.top =    (float)mMaxDrawingY - (float)(mLocationTransformer.TransformLatitude(mCameraViewport.mBB.bottom, mCameraViewport.mBB.left));

						float borderScale = 1.2f;		// provide border that's slightly larger to handle rotation and POIs on border
						mDrawingCameraViewportTestBoundary.left = mDrawingCameraViewportBoundary.left / borderScale;
						mDrawingCameraViewportTestBoundary.right = mDrawingCameraViewportBoundary.right * borderScale;
						mDrawingCameraViewportTestBoundary.bottom = mDrawingCameraViewportBoundary.bottom * borderScale;
						mDrawingCameraViewportTestBoundary.top = mDrawingCameraViewportBoundary.top / borderScale;


						//					Log.d(TAG,"ondraw()");


						// calc user position in drawing coordinates
						mDrawingUserPosition.x = mLocationTransformer.TransformLongitude(mUserLatitude, mUserLongitude);
						mDrawingUserPosition.y = mMaxDrawingY - mLocationTransformer.TransformLatitude(mUserLatitude, mUserLongitude);
						//					Log.e(TAG, "user position x,y: "+ mDrawingUserPosition.x +" - "+ mDrawingUserPosition.y) ;

						UpdateBuddies(mMapDataCache.mDataSourceManager, (float) mMaxDrawingY);	// go poll for buddies

						Bitmap tempBitmap = mRenderSchemeManager.GetPOIBitmap(6,0, viewScale);
						double iconRadius = (double)(tempBitmap.getWidth()/2.0 / viewScale);
						//				Log.i(TAG,"icon radius: " + iconRadius);
						double radius = iconRadius *0.80 ;

						// if in Explore mode, calc closest item to screen center
						String itemDescription = null;
						if(mShowClosestItemDescription)	{ 	
							mDrawingReticuleBoundary.left = (float) (mDrawingCameraViewportCenter.x - radius);
							mDrawingReticuleBoundary.right = (float) (mDrawingCameraViewportCenter.x + radius);
							mDrawingReticuleBoundary.bottom = (float) (mDrawingCameraViewportCenter.y + radius);   
							mDrawingReticuleBoundary.top = (float) (mDrawingCameraViewportCenter.y - radius);

							double closestDistance= 100000.0;
							mDrawingSet[mCurDrawingSetIndex].GetClosestItem(mPOIRollOverResult, mDrawingCameraViewportCenter, mDrawingReticuleBoundary, radius);

							GetClosestBuddy(mBuddyRollOverResult, mDrawingCameraViewportCenter, mDrawingReticuleBoundary, radius, mMaxDrawingY, mPOIRollOverResult.mDistance) ;

							//					Log.v(TAG, "closest decision: " + mBuddyRollOverResult.mDescription + ", " + mPOIRollOverResult.mDescription + ", " + mBuddyRollOverResult.mDistance + ", " + mPOIRollOverResult.mDistance );
							if(mBuddyRollOverResult.mDescription != null) {
								//						Log.v(TAG, "choosing buddy");
								itemDescription = mBuddyRollOverResult.mDescription;
								mBestMapDrawing.clearClosestItem();
							}
							else {
								itemDescription = mPOIRollOverResult.mDescription;
							}

						}
						else {
							mDrawingSet[mCurDrawingSetIndex].ResetPOIStatus();
						}


						// draw map after finding closest item/buddy so that item will be drawn properly
						mDrawingSet[mCurDrawingSetIndex].Draw(canvas, mDrawingCameraViewportTestBoundary, mRenderSchemeManager, 
								(float)mDrawingCameraViewportCenter.x, (float)mDrawingCameraViewportCenter.y, 
								(float)mCameraViewport.mScreenWidthInPixels/2.0f, (float)mCameraViewport.mScreenHeightInPixels/2.0f, viewScale, -mCameraViewport.mCurrentRotationAngle, getResources());


						if(itemDescription != null) {	// put description of object with focus on map
							xoffset = mClosestItemPaint.measureText(itemDescription)/2;
							mDescRect.left = (float)(mCameraViewport.mScreenWidthInPixels/2-(xoffset*1.2));
							mDescRect.top = (float)(mCameraViewport.mScreenHeightInPixels-20-26);
							mDescRect.right = (float)(mCameraViewport.mScreenWidthInPixels/2+(xoffset*1.2)); 
							mDescRect.bottom = (float)(mCameraViewport.mScreenHeightInPixels-20+15);
							canvas.drawRect(mDescRect, mClosestItemBoxPaint);
							canvas.drawRect(mDescRect, mClosestItemBoxOutlinePaint);
							canvas.drawText(itemDescription, mCameraViewport.mScreenWidthInPixels/2, mCameraViewport.mScreenHeightInPixels-20, mClosestItemOutlinePaint);
							canvas.drawText(itemDescription, mCameraViewport.mScreenWidthInPixels/2, mCameraViewport.mScreenHeightInPixels-20, mClosestItemPaint);
						}

						// for buddies and user icon
						mDrawTransformMatrix.setTranslate(-(float)mDrawingCameraViewportCenter.x,-(float)mDrawingCameraViewportCenter.y);
						mDrawTransformMatrix.postScale(viewScale,viewScale);
						mDrawTransformMatrix.postRotate(-mCameraViewport.mCurrentRotationAngle);
						mDrawTransformMatrix.postTranslate((float)mCameraViewport.mScreenWidthInPixels/2.0f, (float)mCameraViewport.mScreenHeightInPixels/2.0f);

						float[] vcp = new float[2];
						vcp[0] = (float)mDrawingCameraViewportCenter.x;
						vcp[1] = (float)mDrawingCameraViewportCenter.y;
						mDrawTransformMatrix.mapPoints(vcp);

						DrawBuddies(canvas, mBestMapDrawing.mResortInfo.BoundingBox, mDrawingCameraViewportBoundary, mRenderSchemeManager, mMaxDrawingY, mDrawTransformMatrix, viewScale, vcp);

						float margin = RETICULE_DISTANCE;
						mDrawingResortTestBoundary.left =   (float)(mLocationTransformer.TransformLongitude(mBestMapDrawing.mResortInfo.BoundingBox.top, mBestMapDrawing.mResortInfo.BoundingBox.left)) - RETICULE_DISTANCE/viewScale;
						mDrawingResortTestBoundary.right =  (float)(mLocationTransformer.TransformLongitude(mBestMapDrawing.mResortInfo.BoundingBox.top, mBestMapDrawing.mResortInfo.BoundingBox.right)) + RETICULE_DISTANCE/viewScale;
						mDrawingResortTestBoundary.bottom = (float)mMaxDrawingY - (float)(mLocationTransformer.TransformLatitude(mBestMapDrawing.mResortInfo.BoundingBox.top, mBestMapDrawing.mResortInfo.BoundingBox.left)) + RETICULE_DISTANCE/viewScale;   // top and bottom flipped as GPS are positive up and graphics is positive down
						mDrawingResortTestBoundary.top =    (float)mMaxDrawingY - (float)(mLocationTransformer.TransformLatitude(mBestMapDrawing.mResortInfo.BoundingBox.bottom, mBestMapDrawing.mResortInfo.BoundingBox.left)) - RETICULE_DISTANCE/viewScale;

						
						// if out of resort bounds, draw resort icon on reticule
						float[] rcp = new float[2];  // resort center position
						rcp[0] = (float)(mDrawingResortTestBoundary.right/2.0);	
						rcp[1] = (float)(mDrawingResortTestBoundary.bottom/2.0);
						mDrawTransformMatrix.mapPoints(rcp); // transformed relative to viewport
						
//						if(!mDrawingResortBoundary.contains((float)mDrawingCameraViewportCenter.x, (float)mDrawingCameraViewportCenter.y) && // simple test first avoids more complex test
//						   !IsOverlapping(mDrawingCameraViewportBoundary, mDrawingResortBoundary, mDrawTransformMatrix)) {
						if(!mDrawingResortTestBoundary.contains((float)mDrawingCameraViewportCenter.x, (float)mDrawingCameraViewportCenter.y)) {
							//						Log.i(TAG,"Resort on reticule");
							float diffX = rcp[0]-vcp[0];
							float diffY = rcp[1]-vcp[1];
							float mag = (float) Math.sqrt(diffX*diffX + diffY*diffY);
							float rx = diffX/mag * RETICULE_DISTANCE + vcp[0];
							float ry = diffY/mag * RETICULE_DISTANCE + vcp[1];
							canvas.drawBitmap(mResortReticuleIcon, rx- mResortReticuleIcon.getWidth()/2, ry-mResortReticuleIcon.getHeight()/2, null);				// fixed user icon
						}
						
						// last, draw user icon
						float[] up = new float[2];
						up[0] = (float)mDrawingUserPosition.x;
						up[1] = (float)mDrawingUserPosition.y;
						mDrawTransformMatrix.mapPoints(up);

						if(mCameraViewport.mUserTestBB.contains(up[0], up[1]))  {
							mUserIconRotationMatrix.setTranslate(-mUserIcon.getWidth()/2, -mUserIcon.getHeight()/2);
							mUserIconRotationMatrix.postRotate(mUserHeading-mMapHeading);
							mUserIconRotationMatrix.postTranslate(up[0], up[1]);
							canvas.drawBitmap(mUserIcon, mUserIconRotationMatrix, null);	// rotated user icon
						}
						else {
							//						Log.i(TAG,"User on reticule");
							float diffX = up[0]-vcp[0];
							float diffY = up[1]-vcp[1];
							float mag = (float) Math.sqrt(diffX*diffX + diffY*diffY);
							float rx = diffX/mag * RETICULE_DISTANCE + vcp[0];
							float ry = diffY/mag * RETICULE_DISTANCE + vcp[1];
							canvas.drawBitmap(mUserReticuleIcon, rx- mUserReticuleIcon.getWidth()/2, ry-mUserReticuleIcon.getHeight()/2, null);				// fixed user icon
						}

					}
					else {
						//				canvas.drawColor(Color.RED);
						text = mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.ERROR_LOADING_DATA);
						xoffset = mTextPaint.measureText(text)/2;
						canvas.drawText(text, mTextX-xoffset, mTextY-20, mTextPaint);
					}
					
//					if(mNewDrawingSetAvailable) {	// if new DrawingSet flagged, switch to it
//						mNewDrawingSetAvailable = false;
////						Log.e(TAG,"Releasing DS["+(mCurDrawingSetIndex == 0 ? 1 : 0)+"]");
////						mDrawingSet[mCurDrawingSetIndex == 0 ? 1 : 0].UnloadResources();	// release resource from old DS
//					}
//					mDrawingMap = false;	// release semaphone

					Bitmap bitmap = this.getDrawingCache();
					latestMap2 = bitmap;
					//latestMap1 = bitmap.copy(bitmap.getConfig(), true);
					
					//Log.v(TAG,  "+++++lastetMap1=" + ((latestMap1==null)? "null":"ok"));
					
					//Log.v(TAG,  "+++++lastetMap2=" + ((latestMap2==null)? "null":"ok"));
					//GenerateResortMapImg(latestMap2);
					
					break;
				}	// end if cur drawing set is loaded
//			} // if mDrawingMap

				// end draw map case
		}	// end switch

		drawText(canvas);
	}

//	private boolean IsOverlapping(RectF viewportBB, RectF testBB, Matrix testTransform) {
//		
//		float[] tl = new float[2];
//		tl[0] = (float)testBB.left;
//		tl[1] = (float)testBB.top;
//		testTransform.mapPoints(tl);
//		float[] tr = new float[2];
//		tr[0] = (float)testBB.left;
//		tr[1] = (float)testBB.top;
//		testTransform.mapPoints(tr);
//		float[] bl = new float[2];
//		bl[0] = (float)testBB.left;
//		bl[1] = (float)testBB.top;
//		testTransform.mapPoints(bl);
//		float[] br = new float[2];
//		br[0] = (float)testBB.left;
//		br[1] = (float)testBB.top;
//		testTransform.mapPoints(br);
//		
//		if(LineIntersectsLine(viewportBB.left,  viewportBB.top,    viewportBB.right, viewportBB.top,    tl[0], tl[1], tr[0], tr[1])) return true;
//		if(LineIntersectsLine(viewportBB.right, viewportBB.top,    viewportBB.right, viewportBB.bottom, tl[0], tl[1], tr[0], tr[1])) return true;
//		if(LineIntersectsLine(viewportBB.right, viewportBB.bottom, viewportBB.left,  viewportBB.bottom, tl[0], tl[1], tr[0], tr[1])) return true;
//		if(LineIntersectsLine(viewportBB.left,  viewportBB.bottom, viewportBB.left,  viewportBB.top,    tl[0], tl[1], tr[0], tr[1])) return true;
//
//		if(LineIntersectsLine(viewportBB.left,  viewportBB.top,    viewportBB.right, viewportBB.top,    tr[0], tr[1], br[0], br[1])) return true;
//		if(LineIntersectsLine(viewportBB.right, viewportBB.top,    viewportBB.right, viewportBB.bottom, tr[0], tr[1], br[0], br[1])) return true;
//		if(LineIntersectsLine(viewportBB.right, viewportBB.bottom, viewportBB.left,  viewportBB.bottom, tr[0], tr[1], br[0], br[1])) return true;
//		if(LineIntersectsLine(viewportBB.left,  viewportBB.bottom, viewportBB.left,  viewportBB.top,    tr[0], tr[1], br[0], br[1])) return true;
//
//		if(LineIntersectsLine(viewportBB.left,  viewportBB.top,    viewportBB.right, viewportBB.top,    br[0], br[1], bl[0], bl[1])) return true;
//		if(LineIntersectsLine(viewportBB.right, viewportBB.top,    viewportBB.right, viewportBB.bottom, br[0], br[1], bl[0], bl[1])) return true;
//		if(LineIntersectsLine(viewportBB.right, viewportBB.bottom, viewportBB.left,  viewportBB.bottom, br[0], br[1], bl[0], bl[1])) return true;
//		if(LineIntersectsLine(viewportBB.left,  viewportBB.bottom, viewportBB.left,  viewportBB.top,    br[0], br[1], bl[0], bl[1])) return true;
//
//		if(LineIntersectsLine(viewportBB.left,  viewportBB.top,    viewportBB.right, viewportBB.top,    bl[0], bl[1], tl[0], tl[1])) return true;
//		if(LineIntersectsLine(viewportBB.right, viewportBB.top,    viewportBB.right, viewportBB.bottom, bl[0], bl[1], tl[0], tl[1])) return true;
//		if(LineIntersectsLine(viewportBB.right, viewportBB.bottom, viewportBB.left,  viewportBB.bottom, bl[0], bl[1], tl[0], tl[1])) return true;
//		if(LineIntersectsLine(viewportBB.left,  viewportBB.bottom, viewportBB.left,  viewportBB.top,    bl[0], bl[1], tl[0], tl[1])) return true;
//
//		return false;
//	}
//
//    private boolean LineIntersectsLine(float Ax1, float Ay1, float Ax2, float Ay2, float Bx1, float By1, float Bx2, float By2)
//    {
//        float q = (Ay1 - By1) * (Bx2 - Bx1) - (Ax1 - Bx1) * (By2 - By1);
//        float d = (Ax2 - Ax1) * (By2 - By1) - (Ay2 - Ay1) * (Bx2 - Bx1);
//
//        if( d == 0 ) { return false; } 
//
//        float r = q / d;
//
//        q = (Ay1 - By1) * (Ax2 - Ax1) - (Ax1 - Bx1) * (Ay2 - Ay1);
//        float s = q / d;
//
//        if( r < 0 || r > 1 || s < 0 || s > 1 )  { return false; }
//
//        return true;
//    }

	
	private void RefreshDrawingSet(int curDrawingSetIndex, double centerLatitude, double centerLongitude, double targetScale) {
		// Note, this method is called continuously from onDraw() as the view is continuously redrawing at a fixed refresh rate
		
		
		if(mDrawingSet != null && mDrawingSet[curDrawingSetIndex] != null) {
//			if(mDrawingSet[curDrawingSetIndex].mDrawingScale != targetScale || 
//			  !mDrawingSet[curDrawingSetIndex].mRegionTestBoundsInGPS.contains((float)centerLongitude, (float)centerLatitude) ||
//			   Math.abs(mDrawingSet[curDrawingSetIndex].mLoadedMapHeading - mCameraViewport.mCurrentRotationAngle) > ROTATE_RELOAD_THRESHOLD ){
			boolean test1 = mDrawingSet[curDrawingSetIndex].mDrawingScale < 0.97*targetScale || mDrawingSet[curDrawingSetIndex].mDrawingScale > 1.1*targetScale;
//			boolean test1 = mDrawingSet[curDrawingSetIndex].mDrawingScale != targetScale;
			boolean test2 = !mDrawingSet[curDrawingSetIndex].mRegionTestBoundsInGPS.contains((float)centerLongitude, (float)centerLatitude);
			boolean test3 = Math.abs(mDrawingSet[curDrawingSetIndex].mLoadedMapHeading - mCameraViewport.mCurrentRotationAngle) > ROTATE_RELOAD_THRESHOLD;
			//test3 = false;
			if(test1 || test2 || test3 ){
				// need to load new drawing set
				if(mIsLoadingDrawingSet) {		// if already loading, check if that data set is suitable.  If not cancel and restart
					boolean ltest1 = mDrawingSet[mLoadingDrawingSetIndex].mDrawingScale < 0.97*targetScale || mDrawingSet[mLoadingDrawingSetIndex].mDrawingScale > 1.1*targetScale;
					boolean ltest2 = !mDrawingSet[mLoadingDrawingSetIndex].mRegionTestBoundsInGPS.contains((float)centerLongitude, (float)centerLatitude);
					boolean ltest3 = Math.abs(mDrawingSet[mLoadingDrawingSetIndex].mLoadedMapHeading - mCameraViewport.mCurrentRotationAngle) > ROTATE_RELOAD_THRESHOLD;
					//ltest3 = false;
					if(ltest1 || ltest2 || ltest3 ){
//					if(mDrawingSet[mLoadingDrawingSetIndex].mDrawingScale != targetScale || 
//					  !mDrawingSet[mLoadingDrawingSetIndex].mRegionTestBoundsInGPS.contains((float)centerLongitude, (float)centerLatitude) ||
//					  Math.abs(mDrawingSet[mLoadingDrawingSetIndex].mLoadedMapHeading - mCameraViewport.mCurrentRotationAngle) > ROTATE_RELOAD_THRESHOLD){
//						RectF r = mDrawingSet[mLoadingDrawingSetIndex].mRegionTestBoundsInGPS;
//						Log.i(TAG,"cancel :" +mDrawingSet[mLoadingDrawingSetIndex].mDrawingScale + " - " + mDrawingSet[mLoadingDrawingSetIndex].mDrawingScale + "-" + targetScale+ "-" + mDrawingSet[mLoadingDrawingSetIndex].mLoadedMapHeading);
//						Log.i(TAG,"       : " + r.left + " " + centerLongitude + " "+r.right + " : "+r.top + " " + centerLatitude + " "+r.bottom);
//						Log.i(TAG,"canceling prev load:" + ltest1 + ", "+ ltest2 + ", "+ ltest3);
						
						mLoadDrawingTask.cancel(true);
						mIsLoadingDrawingSet = false;
//						Log.e(TAG, "Asynch tasks1: " + mNumLoadTasksStarted + " | "+ mNumLoadTasksCompleted + " | "+ mNumLoadTasksCancelled);
//						LoadDrawingSet(curDrawingSetIndex == 0 ? 1 : 0, test1);
					}
					else {
//						Log.d(TAG, "already loading.. do nothing");
					}
				}
				else {
					if(!mProcessingLoadRequest && mNumLoadTasksStarted == mNumLoadTasksCompleted + mNumLoadTasksCancelled) {
//						Log.i(TAG,"load new drawing set: " + test1 + ", "+ test2 + ", "+ test3);
//						RectF c = mDrawingSet[curDrawingSetIndex].mRegionTestBoundsInGPS;
//						Log.i(TAG,"loading condition:" + mDrawingSet[curDrawingSetIndex].mDrawingScale + " - " + targetScale);
////						Log.i(TAG,"                 : " + c.left + " "+c.right + " "+c.top + " "+c.bottom + " " + centerLongitude+ " " + centerLatitude);
//						Log.i(TAG,"                 : " + c.left + " " + centerLongitude + " "+c.right + " : "+c.top + " " + centerLatitude + " "+c.bottom);
////						Log.i(TAG,"                 : " + mDrawingSet[curDrawingSetIndex].mRegionTestBoundsInGPS.contains((float)centerLongitude, (float)centerLatitude));
//						Log.e(TAG, "Asynch tasks2: " + mNumLoadTasksStarted + " | "+ mNumLoadTasksCompleted + " | "+ mNumLoadTasksCancelled);
						LoadDrawingSet(curDrawingSetIndex == 0 ? 1 : 0, test1);
					}
				}
			}
			else {
				// nothing to do, current drawing set is suitable for viewport
			}
		}

	}

	private void LoadDrawingSet(int drawingSetIndexToLoad, boolean mscaleChangeReload) {
		if(mCameraViewport != null) {
			if(!mProcessingLoadRequest) {
				mProcessingLoadRequest = true;
				
				mLoadingDrawingSetIndex = drawingSetIndexToLoad;

				mCameraViewport.FreezePan();	// lock target to current viewport
//				mCameraViewport.CalcTargetBoundingBox();
				mLoadingBoundsInGPS.set(mCameraViewport.mTargetBB);
				double vertMargin = 0.5 * (double)(mLoadingBoundsInGPS.top - mLoadingBoundsInGPS.bottom );
				double horzMargin = 0.5 * (double)(mLoadingBoundsInGPS.right - mLoadingBoundsInGPS.left );
				mLoadingBoundsInGPS.left -= horzMargin;
				mLoadingBoundsInGPS.right += horzMargin;
				mLoadingBoundsInGPS.top += vertMargin;
				mLoadingBoundsInGPS.bottom -= vertMargin;
				
				RectF c = mLoadingBoundsInGPS;
//				Log.i(TAG,"mLoadingBoundsInGPS    (" +drawingSetIndexToLoad + "):  "+ c.left + " "+c.right + " "+c.top + " "+c.bottom);

				mDrawingSet[drawingSetIndexToLoad].mRegionTestBoundsInGPS.set(mCameraViewport.mTargetBB);
//				double vertMargin2 = 0.5 * (double)(mLoadingBoundsInGPS.top - mLoadingBoundsInGPS.bottom );
//				double horzMargin2 = 0.5 * (double)(mLoadingBoundsInGPS.right - mLoadingBoundsInGPS.left );
//				mLoadingBoundsInGPS.left -= horzMargin;
//				mLoadingBoundsInGPS.right += horzMargin;
//				mLoadingBoundsInGPS.top += vertMargin;
//				mLoadingBoundsInGPS.bottom -= vertMargin;

				
				RectF d = mDrawingSet[drawingSetIndexToLoad].mRegionTestBoundsInGPS;
//				Log.i(TAG,"mRegionTestBoundsInGPS (" +drawingSetIndexToLoad + "):  "+ d.left + " "+d.right + " "+d.top + " "+d.bottom);
				
				mDrawingSet[drawingSetIndexToLoad].mDrawingScale = mCameraViewport.mTargetScale ;
				mDrawingSet[drawingSetIndexToLoad].mCancelLoad = false;

				mIsLoadingDrawingSet = true;

				mLoadDrawingTask = new LoadDrawingSetTask(mDrawingSet[drawingSetIndexToLoad], mscaleChangeReload);
				mLoadDrawingTask.execute();
				mNumLoadTasksStarted ++;
				
				mProcessingLoadRequest = false;
			}
		}
	}
	
	protected class LoadDrawingSetTask extends AsyncTask<Void, Void, String> {
		
		private DrawingSet mLoadingDrawingSet = null;
		private boolean mScaleChangeReload = false;
		
		public LoadDrawingSetTask(DrawingSet drawingSet, boolean mscaleChangeReload) {
			mLoadingDrawingSet = drawingSet;
			mScaleChangeReload =mscaleChangeReload;
		}

		@Override
	    protected void onCancelled() {
	    	mLoadingDrawingSet.mCancelLoad = true;
	    }

		protected String doInBackground(Void...voids)  {

			if(mMapDataCache.GetBestRMDForBounds(mCameraViewport) != null) {
				
//				Log.i(TAG,"In LoadDrawingSetTask...");
				float viewScale = (float)mCameraViewport.mTargetScale;

				mBestMapDrawing = mMapDataCache.GetBestRMDForBounds(mCameraViewport.mTargetBB).mMapDrawings;
				mLocationTransformer.SetBoundingBox(mBestMapDrawing.getLocationTransformerBoundingBox());	// needed before transformer used
				mMaxDrawingY = mLocationTransformer.TransformLatitude(mBestMapDrawing.mResortInfo.BoundingBox.bottom, mBestMapDrawing.mResortInfo.BoundingBox.left);
						// calc geoRegion center and boundary in drawing coordinates
				mLoadingGeoRegionCenter.x = mLocationTransformer.TransformLongitude(mCameraViewport.mTargetLatitude, mCameraViewport.mTargetLongitude);
				mLoadingGeoRegionCenter.y = mMaxDrawingY - mLocationTransformer.TransformLatitude(mCameraViewport.mTargetLatitude, mCameraViewport.mTargetLongitude);// LocationTransformer does flip of Y axis...
				
//				Log.e(TAG, "viewport Target BB: "+ (int)mCameraViewport.mTargetBB.left +" - "+ (int)mCameraViewport.mTargetBB.right +" ; "+ (int)mCameraViewport.mTargetBB.bottom +" - "+ (int)mCameraViewport.mTargetBB.top);
				mLoadingGeoRegionBoundary.left = (float)(mLocationTransformer.TransformLongitude(mLoadingBoundsInGPS.top, mLoadingBoundsInGPS.left));
				mLoadingGeoRegionBoundary.right = (float)(mLocationTransformer.TransformLongitude(mLoadingBoundsInGPS.top, mLoadingBoundsInGPS.right));
				mLoadingGeoRegionBoundary.bottom = (float)mMaxDrawingY -(float)(mLocationTransformer.TransformLatitude(mLoadingBoundsInGPS.top, mLoadingBoundsInGPS.left));   // top and bottom flipped as GPS are positive up and graphics is positive down
				mLoadingGeoRegionBoundary.top = (float)mMaxDrawingY - (float)(mLocationTransformer.TransformLatitude(mLoadingBoundsInGPS.bottom, mLoadingBoundsInGPS.left));
//					Log.e(TAG, "++++ "+ mDrawingGeoRegionBoundary.left +" - "+ mDrawingGeoRegionBoundary.right +" ; "+ mDrawingGeoRegionBoundary.top +" - "+ mDrawingGeoRegionBoundary.bottom);

				mResortBBInView.setEmpty();	// by def drawing 0,0 coordinates at resortBB top left
				mResortBBInView.right = (float)(mLocationTransformer.TransformLongitude(mBestMapDrawing.mResortInfo.BoundingBox.bottom, mBestMapDrawing.mResortInfo.BoundingBox.right));
				mResortBBInView.bottom = (float)(mLocationTransformer.TransformLatitude(mBestMapDrawing.mResortInfo.BoundingBox.bottom, mBestMapDrawing.mResortInfo.BoundingBox.right));
				
//				Log.i(TAG, "Boundary height " + (mLoadingGeoRegionBoundary.bottom - mLoadingGeoRegionBoundary.top));
//				float borderScale = 1.0f;		// provide border that's larger to avoid constant reloading during panning
//				mDrawingGeoRegionLoadBoundary.left = mLoadingGeoRegionBoundary.left - borderScale * (mLoadingGeoRegionBoundary.right - mLoadingGeoRegionBoundary.left);
//				mDrawingGeoRegionLoadBoundary.right = mLoadingGeoRegionBoundary.right + borderScale * (mLoadingGeoRegionBoundary.right - mLoadingGeoRegionBoundary.left);
//				mDrawingGeoRegionLoadBoundary.bottom = mLoadingGeoRegionBoundary.bottom + borderScale * (mLoadingGeoRegionBoundary.bottom - mLoadingGeoRegionBoundary.top);
//				mDrawingGeoRegionLoadBoundary.top = mLoadingGeoRegionBoundary.top - borderScale * (mLoadingGeoRegionBoundary.bottom - mLoadingGeoRegionBoundary.top);

				RectF c = mLoadingGeoRegionBoundary;
//				Log.i(TAG,"mLoadingGeoRegionBoundary: " + c.left + " "+c.right + " "+c.top + " "+c.bottom + " " + mLoadingGeoRegionCenter.x+ " " + mLoadingGeoRegionCenter.y);

//				float imageSize = (float)(mLoadingGeoRegionBoundary.right - mLoadingGeoRegionBoundary.left + 1)*viewScale; // desired for 
				float imageSize = 980.0f; 

				float drawScale = 1.0f;		// TODO fix this hack to get all scales to map onto single size bitmap to avoid memory issues
				drawScale = (float)((double)imageSize / (double) (mLoadingGeoRegionBoundary.right - mLoadingGeoRegionBoundary.left + 1));
//				mCameraViewport.AdjustClosestZoomScale(drawScale);	// TODO refactor how to keep constant bit map size
				//mCameraViewport.mTargetScale = drawScale;
//
//				if(mLoadingDrawingSet.IsLoaded()) {
////					// bit of hack to avoid recreating bitmaps and canvases in drawing set... which sometimes led to memory overflow errors
//					drawScale = (float)((double)mLoadingDrawingSet.ImageSize() / (double) (mLoadingGeoRegionBoundary.right - mLoadingGeoRegionBoundary.left + 1));
//					mCameraViewport.AdjustClosestZoomScale(drawScale);	// TODO refactor how to keep constant bit map size
//					mCameraViewport.mTargetScale = drawScale;
//				}
//				else {
//					drawScale = viewScale;
//				}
//				Log.i(TAG,"Scale comparison, desired: "+ drawScale + " | revised to fit bitmap " + drawScale + " | imageSizes desired " + imageSize +  " vs actual " + mLoadingDrawingSet.ImageSize() );
				mLoadTransformMatrix.setTranslate((float)(- mLoadingGeoRegionCenter.x),(float)(-mLoadingGeoRegionCenter.y));
				mLoadTransformMatrix.postScale(drawScale,drawScale);
				mLoadTransformMatrix.postTranslate(imageSize/2.0f, imageSize/2.0f);


				mLoadingDrawingSet.Load(mBestMapDrawing, mResortBBInView, imageSize, mCameraViewport.mTargetRotationAngle, mLoadingGeoRegionBoundary, mLoadingGeoRegionCenter, mRenderSchemeManager, mLoadTransformMatrix, drawScale, getResources(), mCameraViewport.showPathNames());


			}			
			return "";		 
		}

		protected void onCancelled(String errorString) {
			mIsLoadingDrawingSet = false;
			mNumLoadTasksCancelled ++;
		}

		protected void onPostExecute(String errorString) {
			if(errorString.length() > 0) {

			}
			else {
				if(!mLoadingDrawingSet.mCancelLoad) {
					mLoadingDrawingSet.mLoaded = true;
					mNewDrawingSetAvailable = true;
				}
//				Log.v(TAG, "New DrawingSet... LOADED!");
			}
			mIsLoadingDrawingSet = false;
			mNumLoadTasksCompleted ++;

		}

	}
    	
		// load current cameraViewport target 
		

	public void BuddiesUpdated() {
		Log.i(TAG,"Buddies update signaled");
		invalidate();
	}

	private void UpdateBuddies(DataSourceManager dsm, float maxDrawingY) {
		// update buddies list if new buddies data
		
		Bundle buddyBundle = dsm.GetBuddiesBundle();
		
		if(buddyBundle != null) {	// replace buddies list if new buddies data
			
			ArrayList<Bundle> buddyInfoBundleList = (ArrayList<Bundle>)( (ArrayList)buddyBundle.getParcelableArrayList("BuddyInfoBundle"));
			
		    for(Bundle buddyInfoBundle : buddyInfoBundleList)
			{
				Location nextBuddyLocation = (Location) buddyInfoBundle.getParcelable("location");
				
				if(nextBuddyLocation != null) {
					String nextBuddyName = buddyInfoBundle.getString("name");
					int	nextBuddyID = buddyInfoBundle.getInt("id");

//					Log.v(TAG,  "buddy "+nextBuddy.name);
//					//Log.i(TAG, "buddyInfoID=" + buddyInfo.localId + ", name=" + buddyInfo.name +  ", location=" + buddyInfo.location.getLatitude() + " " + buddyInfo.location.getLongitude());

					boolean notInCurrentBuddies = true;
					if(mCurrentBuddies.size() > 0) {
						for(MapBuddyInfo curBuddy : mCurrentBuddies)	{	
							if(curBuddy != null && curBuddy.mName != null) {  
								if (curBuddy.mName.equals(nextBuddyName) )	{	// if yes, update info... assumes later data is in array
									curBuddy.mLatitude = nextBuddyLocation.getLatitude();
									curBuddy.mLongitude = nextBuddyLocation.getLongitude();
									curBuddy.mLocationTimeStamp = nextBuddyLocation.getTime();
									curBuddy.mLastUpdateTime = System.currentTimeMillis();
									curBuddy.mOnline = true;

									curBuddy.mDrawingX = (float)mLocationTransformer.TransformLongitude(curBuddy.mLatitude, curBuddy.mLongitude);;
									curBuddy.mDrawingY = maxDrawingY - (float)mLocationTransformer.TransformLatitude(curBuddy.mLatitude, curBuddy.mLongitude);

//									Log.v(TAG, "updating buddy=" + nextBuddyName + "," + curBuddy.mLongitude +  ", " + curBuddy.mLatitude + " - " + curBuddy.mDrawingX +  ", " + curBuddy.mDrawingY);

									notInCurrentBuddies = false;
									break;	
								}
							}
						}
					}
					if(notInCurrentBuddies) {					// otherwise add it
						MapBuddyInfo curBuddy = new MapBuddyInfo(nextBuddyID, nextBuddyName, nextBuddyLocation.getLatitude(), nextBuddyLocation.getLongitude(), nextBuddyLocation.getTime(), System.currentTimeMillis(),maxDrawingY, mLocationTransformer);
						mCurrentBuddies.add(curBuddy);
						curBuddy.mOnline = true;

//						Log.d(TAG, "update adding buddy=" + nextBuddyName + "," + curBuddy.mLongitude +  ", " + curBuddy.mLatitude + " - " + curBuddy.mDrawingX +  ", " + curBuddy.mDrawingY);
					}
				
				}
			}
			
			
		}
		
		// update all buddy states based on timeout thresholds...   // bundle above may not contain all buddies so this has to be done in a separate loop
		for(int i=mCurrentBuddies.size()-1; i>=0; i--)	{		// reverse through array so removals don't effect processing
			MapBuddyInfo curBuddy = mCurrentBuddies.get(i);
			if((System.currentTimeMillis() - curBuddy.mLocationTimeStamp) > REMOVE_BUDDY_TIME_MS) {	// remove offline buddy after time limit of no updates
				curBuddy = null;
				mCurrentBuddies.remove(i);
			}
			else {
				if((System.currentTimeMillis() - curBuddy.mLocationTimeStamp) > FADE_OFFLINE_BUDDY_TIME_MS) curBuddy.mOnline = false;	// mark buddy as offline after time limit of no updates
				else curBuddy.mOnline = true;	// mark buddy as online
			}
		}
	}
	
	private void DrawBuddies(Canvas canvas, RectF resortBoundary, RectF viewPortBoundary, RenderSchemeManager rsm, double maxDrawingY, Matrix transformMatrix, double viewScale, float[] vcp) 
	{
//		Log.v(TAG, "drawing "+mCurrentBuddies.size() + " buddies");

//		int nextOffScreenBuddyIndex = 0;
		for(MapBuddyInfo buddy : mCurrentBuddies)	{	
			if(buddy != null) {
//				Log.v(TAG, "drawing buddy: "+buddy.mName + "- " + buddy.mDrawingX + " , " + buddy.mDrawingY);

				if(viewPortBoundary.contains(buddy.mDrawingX, buddy.mDrawingY)) {
//					Log.v(TAG, "drawing on-screen buddy: "+buddy.mName);
					dl[0]=buddy.mDrawingX;
					dl[1]=buddy.mDrawingY;
					transformMatrix.mapPoints(dl);

					//			Bitmap buddyIcon = rsm.GetPOIBitmap(POI.POI_TYPE_BUDDY, mState.ordinal(), viewScale);
					Bitmap buddyIcon = rsm.GetPOIBitmap(POI.POI_TYPE_BUDDY, buddy.mState.ordinal(), viewScale);
					if(buddyIcon != null) {
						//.e(TAG,"Buddy State: " + buddy.mState.ordinal() + ", start time: "+ buddy.mLocationTimeStamp);
						if(buddy.mState == MapDrawings.State.HAS_FOCUS || buddy.mState == MapDrawings.State.DISABLED_FOCUS ) {
							canvas.drawBitmap(buddyIcon, dl[0] - buddyIcon.getWidth()/2+1, dl[1] - buddyIcon.getHeight()+1, null);
						}
						else {
							canvas.drawBitmap(buddyIcon, dl[0] - buddyIcon.getWidth()/2+1, dl[1] - buddyIcon.getHeight()/2+1, null);
						}
					}
				}
				else {	// put buddies on reticule  within certain limits  - what values
//					Log.v(TAG, "drawing off-screen buddy: "+buddy.mName);
					if(resortBoundary.contains((float)buddy.mLongitude, (float)buddy.mLatitude)) {
						// last, draw user icon
						float[] bp = new float[2];
						bp[0] = (float)buddy.mDrawingX;
						bp[1] = (float)buddy.mDrawingY;
						transformMatrix.mapPoints(bp);	// transform buddy to viewport coords

//						Bitmap nextBuddyIcon = mBuddyReticuleIconArray.get(nextOffScreenBuddyIndex++);
						float diffX = bp[0]-vcp[0];
						float diffY = bp[1]-vcp[1];
						float mag = (float) Math.sqrt(diffX*diffX + diffY*diffY);
						float rx = diffX/mag * RETICULE_DISTANCE + vcp[0];
						float ry = diffY/mag * RETICULE_DISTANCE + vcp[1];
						canvas.drawBitmap(mBuddyReticuleIcon, rx- mBuddyReticuleIcon.getWidth()/2, ry-mBuddyReticuleIcon.getHeight()/2, null);	
					}

				}
			}
		}
//		for(int i=nextOffScreenBuddyIndex; i< MAX_NUM_OFFSCREEN_BUDDIES; i++) {
//			
//		}
	}
	
	public void GetClosestBuddy(ReconMapView.RollOverResult ror, PointD mDrawingGeoRegionCenter, RectF mDrawingReticuleBoundary, double reticuleRadius, double maxDrawingY, double distance) 
	{
		String closestItemDescription = null;
		double sqDistToCenter, diffX, diffY;
		double closestSqBuddyDist = distance*distance*1.2;  //1.2
		double closestBuddyDist = -1;
		MapBuddyInfo closestBuddy = null;
		
		for(MapBuddyInfo buddy : mCurrentBuddies)	{	
			if(buddy.mOnline) {
				buddy.mState = MapDrawings.State.NORMAL;
			}
			else {
				buddy.mState = MapDrawings.State.DISABLED;
			}

			if(mDrawingReticuleBoundary.contains(buddy.mDrawingX, buddy.mDrawingY)) {
				diffX = mDrawingGeoRegionCenter.x - buddy.mDrawingX;
				diffY = mDrawingGeoRegionCenter.y - buddy.mDrawingY;
				sqDistToCenter = diffX*diffX + diffY*diffY;
//				Log.e(TAG,"Buddy "+(int)(buddy.mDrawingX)+", "+ (int)(buddy.mDrawingY) +" : " + buddy.mName + ": "+ diffX + ", " + diffY+ ": "+ sqDistToCenter + ", " + closestSqBuddyDist);
				
				if(sqDistToCenter < closestSqBuddyDist) {
					closestBuddy = buddy;
					closestSqBuddyDist = sqDistToCenter;
				}
			}
		}
		if(closestBuddy != null) {
			closestBuddyDist = Math.sqrt(closestSqBuddyDist);
			closestItemDescription = "buddy: " + closestBuddy.mName; 
			if(closestBuddy.mOnline) {
				closestBuddy.mState = MapDrawings.State.HAS_FOCUS;
			}
			else {
				closestBuddy.mState = MapDrawings.State.DISABLED_FOCUS;
			}
		}
		else {
			closestBuddyDist = 100000.;
		}

		ror.mDescription = closestItemDescription;
		ror.mDistance = closestBuddyDist;
	}

	
	private void LoadRegion(CameraViewport geoRegion) {
		if(geoRegion.BoundingBoxDefined()) {
			MapDataCache.MapCacheResponseCode mrc = mMapDataCache.HasDataToRenderRegion(geoRegion);
		    switch(mrc) {
		    case WAITING_FOR_LOCATION: 
				mViewState = MapViewState.WAITING_FOR_LOCATION;
		    	break;
		    case LOADING_DATA: 
				mViewState = MapViewState.LOADING_DATA;
		    	break;
		    case DATA_AVAILABLE:
				mViewState = MapViewState.MAP;
		    	break;
		    case NO_DATA_AVAILABLE: 
				mViewState = MapViewState.NO_DATA;
		    	break;
		    case ERROR_WITH_SERVICE: 
				mViewState = MapViewState.ERROR_DATALOAD;
		    	break;
		    }		
//			invalidate();	// now done through timer
//			mParentFragment.MapViewStateChanged(mViewState);  // pass new state up to fragment
			
		}
	}
	//======================================================
	//  callbacks

    
	
	// IMapDataCacheLoadDataResult routine(s)
	public void MapCacheStateChangedTo(MapCacheResponseCode rc){
		Log.i(TAG, "MapCacheStateChangedTo: " + rc);
	    switch(rc) {
	    case DATA_AVAILABLE:
	    	LoadRegion(mCameraViewport);
	    	break;
	    case STARTING_DATA_SERVICE: 
			mViewState = MapViewState.STARTING_DATA_SERVICE;
			break;
	    case WAITING_FOR_LOCATION: 
			mViewState = MapViewState.WAITING_FOR_LOCATION;
			break;
	    case LOADING_DATA: 
			mViewState = MapViewState.LOADING_DATA;
	    	break;
	    case NO_DATA_AVAILABLE: 
			mViewState = MapViewState.NO_DATA;
	    	break;
	    case ERROR_WITH_SERVICE: 
			mViewState = MapViewState.ERROR_DATALOAD;
	    	break;
	    }		
		mParentFragment.MapViewStateChanged(mViewState);  // pass new state up to fragment
	}
	
	public void ErrorLoadingCache(String errorMsg) {
		mViewState = MapViewState.ERROR_DATALOAD;
		invalidate();
		Log.e(TAG," Error with map service/data: " + errorMsg);   
	}
	
	public void LoadMapDataComplete(String errorMsg) {		// interface callback for MapDataManager 
		// when siteMapData finished loading for map
		
		if(errorMsg.length() > 0) {	// error
			mViewState = MapViewState.ERROR_DATALOAD;
			invalidate();
			Log.e(TAG," Error loading map data: " + errorMsg);   // eg, no data service available...
		}
		else {
			mViewState = MapViewState.MAP;
			invalidate();		// force draw of map
		}
		
		mParentFragment.LoadMapDataComplete(errorMsg);
	}
	    
}
