package com.reconinstruments.mapsdk.mapview;

//  This is the OpenGL implementation of MapView.java
//
//  use the script "changeRenderingMethod.bash opengl" to change the MapSDK to use this version,   OR
//             use "changeRenderingMethod.bash canvas" to change the MapSDK to use the Android.Canvas version


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoDataServiceState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataService;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataServiceResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortInfoResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WO_POI;
import com.reconinstruments.mapsdk.mapfragment.MapFragment;
import com.reconinstruments.mapsdk.mapfragment.subclass.MapFragment_Explore;
import com.reconinstruments.mapsdk.mapview.WO_drawings.POIDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.cameraposition.DynamicCameraPositionInterface.IDynamicCameraPosition;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.focusableitems.DynamicFocusableItemsInterface;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.garbagecollection.DynamicGarbageCollectionInterface.IDynamicGarbageCollection;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.geodataservice.DynamicGeoDataInterface.IDynamicGeoData;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.reticleitems.DynamicReticleItemsInterface;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.userposition.DynamicUserPositionInterface.IDynamicUserPosition;
import com.reconinstruments.mapsdk.mapview.renderinglayers.RenderingLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.buddylayer.BuddyItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.buddylayer.BuddyLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer.CustomAnnotationCache;
import com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer.CustomLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.gridlayer.GridLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer.BackgroundSize;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.ShaderGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.ShaderProgram;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleConfig;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticuleLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.GLText;
import com.reconinstruments.mapsdk.mapview.renderinglayers.usericonlayer.UserIconLayer;
import com.reconinstruments.utils.ConversionUtil;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.SettingsUtil;

/**
 * @author stevenmason
 *
 */
public class MapView extends com.reconinstruments.mapsdk.mapview.GLSurfaceView  {
	
// Constants
	private static final String TAG = "MapView";
    public static final boolean OSM_BASE_MAP = true;
    public static final boolean PRINT_GL_INFO = false;
    public static final boolean GL_TEXT_ENABLED = false;
	private static final boolean WAIT_FOR_DEBUGGER = false;
	private static final boolean FORCE_RESOURCE_MISSING_ERROR = false;
	private static final int USER_ICON_SIZE = 15;
	private static final int PROGRESS_START_VALUE = 5; // pixels
	private static final double BASE_CAMERA_VIEWPORT_WIDTH_IN_METERS = 2000.;
	private static final int RETICULE_DISTANCE = 61;
	private static final float RETICLE_ITEMS_WITHIN_DISTANCE_M = 1000f;
																					// the larger this value, the more memory is used for the two drawingSet background images
	private static final int MESSAGE_WHAT_SET_ROLLOVER_TEXT = 1;
	private static final int MESSAGE_WHATHIDE_ROLLOVER_TEXT = 2;
	
	public enum MapViewState {
		INITIALIZING,
		ERROR_INITIALIZING,			// problem encounter during initialization, should only happen during testing...
		WAITING_FOR_DATA_SERVICE,	// during startup
		WAITING_FOR_LOCATION,		// have binding to service but no location data yet
		PROBLEM_WITH_DATA_SERVICE,
		REQUIRED_DATA_UNAVAILABLE,	// service is running but does does not have desired capabilities to supply data
		OUT_OF_MEMORY,
		ERROR_LOADING_THEMES,		// issue loading rendering themes
		LOADING_DATA,
		ERROR_LOADING_DATA,
		DRAW_LAYERS,				// everything is ok, draw layers
		WAITING_FOR_LAYER_INITIALIZATION
	}
	public enum CameraPanDirection {
		LEFT,
		UP,
		RIGHT,
		DOWN
	}
	
	public enum MapViewMode {
		NORMAL,
		FIND_MODE,
		EXPLORE_MODE
	}

    /**
     * Interface defining a callback method for responding to updated buddies events
     */
    public interface OnBuddiesUpdatedListener{
        /**
         * This method is called when there are new buddies available
         * @param buddiesAvailable whether there are buddies available
         */
        public void onBuddiesUpdated(int numBuddies);
    }

    OnBuddiesUpdatedListener mBuddiesUpdatedListener;

// members
	public Activity 				mParentActivity = null;
	public    String 				mIdentifier = "notDefined";
	public  MapFragment 			mParentFragment = null;
	protected RenderSchemeManager 	mRenderSchemeManager = null;
	public  static IGeodataService	mGeodataServiceInterface  = null;
	public  GeoDataServiceState 	mGeodataServiceState = null;
	public  MapViewState			mViewState = MapViewState.INITIALIZING;
	protected MapViewState			mLastViewState = MapViewState.WAITING_FOR_DATA_SERVICE;
	public World2DrawingTransformer	mWorld2DrawingTransformer= null;
	protected PointXY				mMapViewDimensions= null;
			
	protected CameraViewport		mCameraViewport = null;
	protected ArrayList<ResortInfoResponse> 	mClosestResortList = null;
	protected Map2DLayer			mMapLayer;
	protected CustomLayer			mCustomLayer;
	protected GridLayer				mGridLayer;
	protected BuddyLayer			mBuddyLayer;
	protected ReticuleLayer			mReticleLayer;
	protected UserIconLayer			mUserIconLayer;
	
	protected ReticleConfig			mReticleConfig = null;
	boolean mFirstUpdate = true;

	public ArrayList<RenderingLayer> mRenderingLayers = new ArrayList<RenderingLayer>();
	public HashMap<MapViewState, String>  mModeMessages = new HashMap<MapViewState, String>();
	
	boolean							mCameraIsReady = false;
//	boolean							mMapRotates = false;
	boolean							mDrawingMap = false;
	boolean							mNotEnoughMemoryError = false;
	boolean							mShowGrid = false;
	boolean 						mHaveUserLocation = false;
	boolean							mAllLayersReady = false;
	protected boolean 				mShowClosestItemDescription = false;	
	protected boolean				mMapLoaded = false;
//	boolean 						mNewDrawingSetAvailable = false;
	public boolean					mCameraFollowsUser = false;
	public boolean					mCameraRotateWithUser = false;
	public boolean					mCameraPitchesWithUser = false;
	public volatile boolean			mCameraChangesWithUserVelocity = false;
	int 							mStatusBarHeight = 30;
	
    long 							mFrameIntervalInMS;		// ms per frame - set as 1000/framerate defined in renderscheme manager
	long 							mViewRefreshClockStartTime;
	boolean							doGCOnPause = false;
	
	public float					mMapPreloadMultiplier = 1.0f;		// non-documented API, used by MapFragment 
	protected double		    	mAspectRatio = 0.0;
	protected double 				mUserLatitude = 0.0;
	protected double 				mUserLongitude = 0.0;
	protected float 				mCameraHeading = 0.0f;
	protected float					mCameraPitch = 0.0f;
	protected float 				mUserHeading = 0.0f;
	protected float					mUserPitch = 0.0f;
	protected double 				mUserVelocity = 0.0;
	protected float					mPreviousZoomScale = 0f;
	
	protected volatile MapViewMode	mMapViewMode;
	
	private volatile boolean		mSetViewToThirdPerson = false;

    				// many of these are preallocated for efficiency in time-constrained draw cycle
	float[] 						dl 									= new float[2];
	double  						mMaxDrawingY 						= 0.0;
    ArrayList<POIDrawing> 			mFocusTestList 	= new ArrayList<POIDrawing>();
	PointXY 						mLoadingGeoRegionCenter   			= new PointXY(0.f,0.f);
    PointXY 						mDrawingUserPosition   				= new PointXY(0.f,0.f);
    PointXY 						mDrawingCameraViewportCenter   		= new PointXY(0.f,0.f);
	PointXY 						onDrawCameraViewportCurrentCenter 	= new PointXY(0.f,0.f);
	PointXY							mBackgroundImageCenterInDrawingCoords = new PointXY(0f, 0f);
	PointXY							mLastUserIconPosition				= new PointXY(-1f, -1f);
	RectF							mDrawingCameraViewportBoundary 		= new RectF();
	RectF							mDrawingCameraViewportTestBoundary 	= new RectF();
	RectF  							mDrawingGeoRegionLoadBoundary  		= new RectF();
	RectF   						mDrawingResortTestBoundary  		= new RectF();
	RectF							mLoadingGeoRegionBoundary 			= new RectF();
	RectF							mDrawingReticuleBoundary 			= new RectF();
	RectF							mLoadingBoundsInGPS 				= new RectF();
	RectF 							mDescRect 							= new RectF();
	RectXY 							onDrawCameraViewportBoundary 		= new RectXY(0.f,0.f,0.f,0.f);
	Matrix 							mLoadTransformMatrix 				= new Matrix(); 
	Matrix 							mDrawTransformMatrix 				= new Matrix(); 
	Matrix							mDrawingTransform 					= null;

	int 							mMessageTextX=0;
    int 							mMessageTextY=0;
    TextPaint						mMessageTextPaint;
    TextPaint						mMessageSubTextPaint;
    float							mMessageTextHeight = 0;

    int								mProgressLimit = 0;
    float							mCurrentProgress = 1.0f;
    float							mTargetProgress = 1.0f;
    float							mProgressStepSize = 0.f;
    
    LinearLayout					mRolloverContainer;
    TextView 						mRolloverNameTV;
    TextView 						mRolloverDistanceTV;
    String							mClosestPOIItemID;
    String							mClosestPOIItemName;
    float							mPOIDistanceFromMe 					= 0f;

//	private AssetManager mAssetMan;
	protected Resources mResources;
	private boolean mReloadTexture = false;
    private OverlayDialog mOverlayDialog;

	// redraw clock/timer members
	Handler mHandler = new Handler();
	Runnable mTick = new Runnable() {
	    public void run() {
	    	mCameraViewport.UpdateViewport();	// track target viewport
//	        invalidate();						// force redraw View
	        requestRender();					// force redraw of GLSurface frame
	        
			mHandler.postDelayed(this, mFrameIntervalInMS); 	// queue repeated call to run() in future
	    }
	};

// interfaces
	public interface IReconMapViewCallbacks {
		public void MapViewReadyToConfigure();	// called when mapview camera is ready for configuration
		public void MapViewReadyForAnnotation();  			// called when mapview is ready for annotations calls	- old... removed	
	}


//======================================================================================
// constructor	
	@SuppressLint("NewApi")

	public MapView(Activity parentActivity, String identifier) throws Exception {
		this(parentActivity, identifier, "rendering_schemes.xml");
	}
	
	protected boolean haveAllRequiredRuntimeResources() {
		boolean result = true;
		String rootpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ReconApps/MapData/";

		if(!(new File(rootpath + mResources.getString(R.string.rendering_schemes))).exists()) result = false;
		if(!(new File(rootpath + mResources.getString(R.string.reticle_configuration))).exists()) result = false;
		if(!(new File(rootpath + mResources.getString(R.string.scale_pitch_curves))).exists()) result = false;
		if(!(new File(rootpath + mResources.getString(R.string.velocity_curves))).exists()) result = false;
		if(!(new File(rootpath + mResources.getString(R.string.square_tex))).exists()) result = false;
		if(!(new File(rootpath + mResources.getString(R.string.osm_street_config))).exists()) result = false;
		if(!(new File(rootpath + mResources.getString(R.string.osm_base_config))).exists()) result = false;

		if(FORCE_RESOURCE_MISSING_ERROR) {
			return false;
		}
		return result;
	}
	
	public MapView(Activity parentActivity, String identifier, String rsmXmlFile) throws Exception {
 		super(parentActivity);

        mParentActivity = parentActivity;
        mResources = parentActivity.getResources();

 		if(!haveAllRequiredRuntimeResources()) {
 			throw new Exception("Missing Key Files");
 		}
 		
 		if(rsmXmlFile == null) {
 			rsmXmlFile = mResources.getString(R.string.rendering_schemes);
 		}	

		mIdentifier = identifier;

		String productName = Build.PRODUCT;
		String modelName = Build.MODEL;
	    Log.d(TAG, "Hardware product Name: "+productName + ", model: " + modelName);
	    
	    mRenderSchemeManager = new RenderSchemeManager(mParentActivity, productName, mParentActivity.getResources(), mParentActivity.getPackageName(), rsmXmlFile) ;
		mWorld2DrawingTransformer = new World2DrawingTransformer();
		
		mCameraViewport = new CameraViewport(parentActivity, BASE_CAMERA_VIEWPORT_WIDTH_IN_METERS, mRenderSchemeManager);
		
		double frameRate = mRenderSchemeManager.GetRate(RenderSchemeManager.Rates.FRAME_RATE);
		if(frameRate >= 5.0) {
			mFrameIntervalInMS = (long)(1000.0/frameRate);
		}
        mAllLayersReady = false;

        mMessageTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
        mMessageTextPaint.setColor(Color.WHITE);
        mMessageTextPaint.bgColor = Color.BLACK;
        mMessageTextPaint.setTextSize(24);
        if (mMessageTextHeight == 0) {
            mMessageTextHeight = mMessageTextPaint.getTextSize();
        } else {
            mMessageTextPaint.setTextSize(mMessageTextHeight);
        }

		mModeMessages.put(MapViewState.INITIALIZING, "");
		mModeMessages.put(MapViewState.ERROR_INITIALIZING, mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.ERROR_INITIALIZING));
		mModeMessages.put(MapViewState.WAITING_FOR_DATA_SERVICE, mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.WAITING_FOR_DATA_SERVICE));
		mModeMessages.put(MapViewState.WAITING_FOR_LOCATION, mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.WAITING_FOR_LOCATION));
		mModeMessages.put(MapViewState.PROBLEM_WITH_DATA_SERVICE, mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.PROBLEM_WITH_DATA_SERVICE));
		mModeMessages.put(MapViewState.REQUIRED_DATA_UNAVAILABLE, mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.REQUIRED_DATA_UNAVAILABLE));
		mModeMessages.put(MapViewState.OUT_OF_MEMORY, mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.OUT_OF_MEMORY));
		mModeMessages.put(MapViewState.ERROR_LOADING_THEMES, mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.ERROR_LOADING_THEMES));
		mModeMessages.put(MapViewState.WAITING_FOR_LAYER_INITIALIZATION, mRenderSchemeManager.GetMessage(RenderSchemeManager.Messages.WAITING_FOR_MAP));
		mModeMessages.put(MapViewState.DRAW_LAYERS, "");

        mMessageSubTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
        mMessageSubTextPaint.setColor(Color.WHITE);
        mMessageSubTextPaint.bgColor = Color.BLACK;
        mMessageSubTextPaint.setTextSize(mMessageTextHeight-4);
        
        mClosestPOIItemID = "";

        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        
        setRenderer(new MapViewRenderer(parentActivity));
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        mOverlayDialog = new OverlayDialog(mParentActivity, DeviceUtils.isSun() ? R.layout.overlay_dialog_jet : R.layout.overlay_dialog_snow, false);
	}
	
//======================================================================================
// lifecycle callbacks - designed to be called from parent activity/fragment lifecycle routines
	public void onCreateView() {
		Log.d(TAG,"onCreate "+System.currentTimeMillis());
		
		if(WAIT_FOR_DEBUGGER) {
			android.os.Debug.waitForDebugger();
		} 

		try {
			mParentFragment.ConfigurePreMapViewInit();		// allows fragment to override base configuration after all objects have been created...
			DefineMapLayers(mRenderingLayers);
			if(mSetViewToThirdPerson){
				setThirdPersonView(true);
			}
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			mNotEnoughMemoryError = true;
			Log.e(TAG, "Error creating/initializing MapRenderingLayer. " + e.getMessage());
		}
		
		mParentFragment.ConfigurePostMapViewInit();		// allows fragment to override base configuration after all objects have been created...

	}
	
	// Override this method when defining a subclass
	public void DefineMapLayers(ArrayList<RenderingLayer> layers) throws Exception {
		BackgroundSize bgSize = BackgroundSize.NORMAL;
//		mGridLayer = new GridLayer(this, mRenderSchemeManager, mWorld2DrawingTransformer);
		mCustomLayer = new CustomLayer(mParentActivity, mRenderSchemeManager, mWorld2DrawingTransformer);
		mMapLayer = new Map2DLayer((OSM_BASE_MAP) ? mResources.getString(R.string.osm_base_config)
                                                  : mResources.getString(R.string.osm_street_config),
                                    mParentActivity, mRenderSchemeManager, mWorld2DrawingTransformer, mIdentifier,
                                    mMapPreloadMultiplier, bgSize, mCustomLayer);
		mBuddyLayer  = new BuddyLayer(mParentActivity, mRenderSchemeManager, mWorld2DrawingTransformer, bgSize);
		mUserIconLayer  = new UserIconLayer(mParentActivity, mRenderSchemeManager, mWorld2DrawingTransformer, bgSize);
//		layers.add(mGridLayer);
		layers.add(mMapLayer);
		layers.add(mCustomLayer);
		layers.add(mBuddyLayer);
		layers.add(mUserIconLayer);
	}
	
	public void SetScreenStatusBarHeight(int height){
		mStatusBarHeight = height;
		mCameraViewport.SetStatusBarHeight(height);
	}
	
	public void SetScreenDimensions(float width, float height){
		mAspectRatio = (double)width/(double)height;		
		mCameraViewport.SetViewPortDimensions(width, height);
		mMapViewDimensions = new PointXY(width,height);
		
		if((mParentActivity instanceof IReconMapViewCallbacks) ) {
			((IReconMapViewCallbacks) mParentActivity).MapViewReadyToConfigure();
		}
	}
	
	public void UserLeavingApp() {
    	doGCOnPause = true;		// only do GC if user leaves app, not during some interruption such as a phone call

    	
//    	// for testing... remove following lines for production release
//		if(doGCOnPause) {
//	    	for(RenderingLayer layer : mRenderingLayers) {
//	    		if(layer instanceof IDynamicGarbageCollection) {
//	    			((IDynamicGarbageCollection)layer).DoGarbageCollection(mCameraFollowsUser);
//	    		}
//	    	}
//		}

	}

	public void onResumeView(){
		mNotEnoughMemoryError = false;

		if(mViewState == MapViewState.INITIALIZING) {
			setViewState(MapViewState.WAITING_FOR_DATA_SERVICE);
		}

    	for(RenderingLayer layer : mRenderingLayers) {
   			layer.Resume();
    	}
    	
		if(mBuddyLayer != null) {		
			if(mGeodataServiceInterface != null) {  // if not ready, will do this when interface is established
				try {
					mGeodataServiceInterface.registerForBuddies(mParentActivity.getPackageName()); 
				} catch (RemoteException e) {
					e.printStackTrace();
					Log.e(TAG, "Error registering for buddy updates");
				} 
			}
		}

    	doGCOnPause = false;
    	onResume();
//    	RespondToCameraViewportChange(mCameraViewport);
    	invalidate();
//    	requestRender();
    	mReloadTexture = true;
		startViewRefreshClock();
		
		if(mViewState == MapViewState.DRAW_LAYERS && mMapLayer != null && mMapLayer.allMissingTiles()) {
			EmptyTileSetWarningMessage();
		}
	}


	private void EmptyTileSetWarningMessage() {
		mParentActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if(!mOverlayDialog.isShowing()) mOverlayDialog.show();
            }
		});
	}

	public void onPauseView(){
		stopViewRefreshClock();
		// as maps is a critical feature/service, avoid release / realloc of large memory chunks until maps service is rewritten and disconnected
		// let system reclaim if from background if necessary.. This decision is due because of delays in Java garbage collection could cause moving into and out of maps quickly to thrash the heap
		// resulting in an Out of Memory error experience for the user.

		if(mBuddyLayer != null) {		
			if(mGeodataServiceInterface != null) {
				try {
					mGeodataServiceInterface.unregisterForBuddies(mParentActivity.getPackageName()); 
				} catch (RemoteException e) {
//					e.printStackTrace();
					Log.e(TAG, "Error unregistering for buddy updates");
				} 
			}
		}

		
		if(doGCOnPause) {
	    	for(RenderingLayer layer : mRenderingLayers) {
	    		if(layer instanceof IDynamicGarbageCollection) {
	    			((IDynamicGarbageCollection)layer).DoGarbageCollection(mCameraFollowsUser);
	    		}
	    	}
		}
		super.onPause();
	}
	
	public void onStopView(){
		Log.d("MapView", "onStopView() called");
	}
	
	public void onDestroy(){
		stopViewRefreshClock();
		
    	for(RenderingLayer layer : mRenderingLayers) {
   			layer.Release();
    	}
	}

//======================================================================================
// other MapSDK API calls 
	
// camera setup
	public void	SetCameraPerspective(int dimensions) {
		if(mCameraViewport != null) {
			// tbd
		}
	}
	
	public void	SetZoomLevels(double[] zoomLevels) {
		if(mCameraViewport != null) {
			mCameraViewport.SetZoomLevels(zoomLevels);  // NOT TESTED YET...  
		}
	}
	
	public void	SetPanStepScale(float scaleFactor) {
		if(mCameraViewport != null) {
			mCameraViewport.SetPanStepScale(scaleFactor);
		}
	}
	
// --------------------- camera absolute positioning and scaling
	public void	SetCameraGPSPosition(double longitude, double latitude, double newVelocity) {
		if(mCameraViewport != null) {
			mCameraViewport.SetGPSPosition(longitude, latitude, true); 
			mCameraViewport.SetVelocity(newVelocity, false);
//			mCameraViewport.UpdateViewport();
			
//			if(!mWorld2DrawingTransformer.isDrawingOriginSet() || mWorld2DrawingTransformer.centerIsFarFrom(longitude, latitude)) {	// TODO in future, will need to flush layer caches if we change drawing transformer so that no old drawing objects exist with old transformation
			if(!mWorld2DrawingTransformer.isDrawingOriginSet()) {
				mWorld2DrawingTransformer.SetDrawingOrigin(longitude, latitude);		// long, lat before we can get here...
				
				if(mCustomLayer != null) {
					mCustomLayer.HandleDrawingTransformerChange(mWorld2DrawingTransformer); // incase custom annotations have been previously added.. they need to be updated
				}
				
				// TODO in future, flush layer drawing caches if virtually teleported to a new location far away - as in a demo (see comment above)
			}
	
			if(mViewState == MapViewState.WAITING_FOR_LOCATION) setViewState(MapViewState.WAITING_FOR_LAYER_INITIALIZATION);	
			
			RespondToCameraViewportChange(mCameraViewport);  
			
		}
	}
	
	// note the mapsdk currently exposes the camera altitude to the client, but (currently) converts this to the older zoom magnification scale used within the mapview and layers
	// the future plan is to convert this to a height (or equiv, region width scaling) instead of a zoom mag scale
	
	public void	SetCameraAltitude(float altitude) {		// absolute altitude setting
		if(mCameraViewport != null) {
			mCameraViewport.SetCameraAltitude(altitude, true);		
			RespondToCameraViewportChange(mCameraViewport);  
		}
	}
	
	public void	SetCameraAltitudeScale(float altitudeScale) {	// relative setting, scale version of camera's base altitude
		if(mCameraViewport != null) {
			mCameraViewport.SetCameraAltitudeScale(altitudeScale, true);		
			RespondToCameraViewportChange(mCameraViewport);  
		}
	}
	
	public void SetCameraToShowGeoRegion(GeoRegion geoRegion, float percentWidthMargin, boolean immediate) {	
		if(mCameraViewport != null) {
			mCameraFollowsUser = false;		// using this method, forces user tracking off
			mCameraViewport.SetCameraToShowGeoRegion(geoRegion, percentWidthMargin, immediate, mCameraRotateWithUser);		

			if(!mWorld2DrawingTransformer.isDrawingOriginSet()) {
				mWorld2DrawingTransformer.SetDrawingOrigin(geoRegion.mCenterPoint.x, geoRegion.mCenterPoint.y);		// long, lat before we can get here...
				if(mCustomLayer != null) {
					mCustomLayer.HandleDrawingTransformerChange(mWorld2DrawingTransformer); // incase custom annotations have been previously added.. they need to be updated
				}
				
				// TODO in future, flush layer drawing caches if virtually teleported to a new location far away - as in a demo (see comment above)
			}
			
			RespondToCameraViewportChange(mCameraViewport);  
		}
	}

	
	public float GetCameraAltitudeScale() {
		if(mCameraViewport != null) {
			return mCameraViewport.GetAltitudeScale();
		}
		return 1.0f;
	}
	
	public GeoRegion GetCameraBoundingGeoRegion() {
		if(mCameraViewport != null) {
			return mCameraViewport.GetBoundingGeoRegion();
		}
		return null;
	}
	
	public GeoRegion GetCameraLowestResolutionBoundingGeoRegion() {
		if(mCameraViewport != null) {
			return mCameraViewport.GetBoundingLowestResolutionGeoRegion();
		}
		return null;
	}
	
// --------------------- camera relative positioning and scaling
	public void	SetCameraZoomIndex(int zoomIndex) {
		if(mCameraViewport != null) {
			mCameraViewport.SetZoomIndex(zoomIndex);  	// NOT TESTED YET...
			RespondToCameraViewportChange(mCameraViewport);  
		}
	}
	
	
	public void	Pan(CameraPanDirection direction) {
		if(mCameraViewport != null) {
			switch(direction) {
			case LEFT:
				mCameraViewport.panLeft();
				break;
			case RIGHT:
				mCameraViewport.panRight();
				break;
			case UP:
				mCameraViewport.panUp();
				break;
			case DOWN:
				mCameraViewport.panDown();
				break;
			}
			RespondToCameraViewportChange(mCameraViewport);						
		}
	}
	
	public void StopPan() { // internal, non-API
		mCameraViewport.FreezePan();	// stop automated position
	}

	public void	ZoomCameraIn() {
		if(mCameraViewport != null) {
			mCameraViewport.ZoomIn();
			RespondToCameraViewportChange(mCameraViewport);						
		}
	}
	
	public void	ZoomCameraOut() {
		if(mCameraViewport != null) {
			mCameraViewport.ZoomOut();
			RespondToCameraViewportChange(mCameraViewport);						
		}
	}
	
	public void FreezePan() { // internal, non-API
		mCameraViewport.FreezePan();	// stop automated panning
	}

	// calculations / filtering
	public ArrayList<PointXY> RemoveRedundantPathPointsForCurrentViewport(ArrayList<PointXY> pathNodes) {
		if(mCameraViewport != null) {
			return mCameraViewport.RemoveRedundantPathPointsForCurrentViewport(pathNodes);
		}
		return null;
	}
//	public ArrayList<PointXY> RemoveRedundantPathPointsForBaseScale(ArrayList<PointXY> pathNodes) {
//		if(mCameraViewport != null) {
//			return mCameraViewport.RemoveRedundantPathPointsForBaseScale(pathNodes);
//		}
//		return null;
//	}
	
// --------------------- camera heading/orientation
	public void SetCameraHeading(float heading) {	// included for completeness - not thoroughly tested
		mCameraHeading = heading;

		for(RenderingLayer layer : mRenderingLayers) {
    		if(layer instanceof IDynamicCameraPosition) {
    			((IDynamicCameraPosition)layer).SetCameraHeading(heading);
    		}
    	}

		if(mCameraViewport != null) {
			mCameraViewport.SetViewAngleRelToNorth(mCameraHeading, false);
			RespondToCameraViewportChange(mCameraViewport);  
		}
	}
		
	public void	setCameraPitch(float pitch) {
		mCameraPitch = pitch;
		
		if(mCameraViewport != null) {
			mCameraViewport.SetRealPitchAngle(pitch);
		}
	}

	

// --------------------- grid backdrop 
	public void ShowGrid(boolean showGrid) {
		mShowGrid = showGrid;
	}
	
// --------------------- user position, rotation, tracking & icon visibility 
	public void ShowUserIcon(boolean showUserIcon) {
		if(mUserIconLayer != null) mUserIconLayer.ShowUserPosition(showUserIcon);
	}
	
	public void SetUserPosition(double newLongitude, double newLatitude, double newVelocity) {
		mUserLatitude = newLatitude;		
		mUserLongitude = newLongitude;
		mUserVelocity = newVelocity;
		
		mHaveUserLocation = true;
    	for(RenderingLayer layer : mRenderingLayers) {
    		if(layer instanceof IDynamicUserPosition) {
    			((IDynamicUserPosition)layer).SetUserPosition((float)newLongitude, (float)newLatitude);
    		}
    	}
    	if(mCameraFollowsUser) {
    		SetCameraGPSPosition((float)newLongitude, (float)newLatitude, newVelocity);
    	}
	}
	
	public void SetCameraToRespondToUserVelocity(boolean cameraChangesWithUserVelocity) {
		mCameraChangesWithUserVelocity = cameraChangesWithUserVelocity && mCameraFollowsUser;		// only works if camera is following user
		mCameraViewport.SetCameraToRespondToUserVelocity(mCameraChangesWithUserVelocity);
		if(mCameraFollowsUser && mHaveUserLocation) {
    		SetCameraGPSPosition((float)mUserLongitude, (float)mUserLatitude, mUserVelocity);
		}
	}
	
	public void SetCameraToFollowUser(boolean cameraFollowsUser) {
		mCameraFollowsUser = cameraFollowsUser;
		if(mCameraFollowsUser && mHaveUserLocation) {
    		SetCameraGPSPosition((float)mUserLongitude, (float)mUserLatitude, mUserVelocity);
		}
	}
	
	public boolean IsMapCenteredOnUser() {
		return mCameraFollowsUser;
	}
	
	
	public void SetUserHeading(float heading) {		// 
		mUserHeading = heading;
    	for(RenderingLayer layer : mRenderingLayers) {
    		if(layer instanceof IDynamicUserPosition) {
    			((IDynamicUserPosition)layer).SetUserHeading(heading);
    		}
    	}

		if(mCameraRotateWithUser) {
	    	SetCameraHeading(mUserHeading);
		}
	}
	
	public void SetUserPitch(float pitch){
		mUserPitch = pitch;
		
		if(mCameraPitchesWithUser){
			setCameraPitch(mUserPitch);
		}
	}
	
	public void SetCameraToRotateWithUser(boolean cameraRotatesWithUser) {
		mCameraRotateWithUser = cameraRotatesWithUser;
		if(mCameraViewport != null) {
			mCameraViewport.SetCameraToRotateWithUser(cameraRotatesWithUser);
		}
		if(mCameraRotateWithUser ) {
	    	SetCameraHeading(mUserHeading);
		}
		else {
	    	SetCameraHeading(mCameraHeading);
		}
	}
	
	public void SetCameraToPitchWithUser(boolean cameraPitchesWithUser) {
		mCameraPitchesWithUser = cameraPitchesWithUser;
		if(mCameraViewport != null) {
			mCameraViewport.SetCameraToPitchWithUser(cameraPitchesWithUser);
		}
		if(mCameraPitchesWithUser) {
			setCameraPitch(mUserPitch);
		}
	}
	
	
	public boolean DoesCameraRotateWithUser() {
		return mCameraRotateWithUser;
	}
	
// --------------------- Annotations

	public CustomAnnotationCache.AnnotationErrorCode AddPointAnnotation(String poiID, PointXY poiLocation, Bitmap image, int alpha) {
		return mCustomLayer.AddPointAnnotation(poiID, poiLocation, image, alpha);
	}

	public CustomAnnotationCache.AnnotationErrorCode AddLineAnnotation(String lineID, ArrayList<PointXY> nodes, float lineWidthInM,  int color,  int alpha) {
		return mCustomLayer.AddLineAnnotation(lineID, nodes, lineWidthInM, color, alpha);
	}

	public CustomAnnotationCache.AnnotationErrorCode AddOverlayAnnotation(String overlayID, ArrayList<PointXY> nodes, int color,  int alpha) {
		return mCustomLayer.AddOverlayAnnotation(overlayID,  nodes, color,  alpha);
	}

	public CustomAnnotationCache.AnnotationErrorCode RemovePointAnnotation(String objectID) {
		return mCustomLayer.RemovePointAnnotation(objectID);
	}

	public CustomAnnotationCache.AnnotationErrorCode RemoveLineAnnotation(String objectID) {
		return mCustomLayer.RemoveLineAnnotation(objectID);
	}

	public CustomAnnotationCache.AnnotationErrorCode RemoveOverlayAnnotation(String objectID) {
		return mCustomLayer.RemoveOverlayAnnotation(objectID);
	}

	public CustomAnnotationCache.AnnotationErrorCode RemoveAllAnnotations() {
		CustomAnnotationCache.AnnotationErrorCode rc =  mCustomLayer.RemoveAllAnnotations();
		return rc;
	}

	

	
//	
////======================================================================================
//// layer management
//	public void loadRenderingLayers() {
//		
//	}
	
//======================================================================================
// methods
	
	public void	SetPreloadMultiplier(float preloadMultiplier) {
		mMapPreloadMultiplier = preloadMultiplier;
	}
	
	boolean serviceHasError(GeoDataServiceState serviceState) {
		if(mGeodataServiceState != null && 
		   (mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.ERROR_DURING_SERVICE_INITIALIZATION ||		
			mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.ERROR_WITH_SERVICE)) {
			return true;
		}
		return false;
	}

	boolean serviceIsReady(GeoDataServiceState serviceState) {
		if(mCameraFollowsUser) {
			if(mGeodataServiceState != null && 
				(mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_STALE_USER_LOCATION ||		
				mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION)) {
				return true;
			}
		}
		else {
			if(mGeodataServiceState != null && 
				(mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_STALE_USER_LOCATION ||
				mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_NO_USER_LOCATION ||		
				mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA ||		
				mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION)) {
				return true;
			}
		}
		return false;
	}
	
	void startViewRefreshClock() {
		mViewRefreshClockStartTime = SystemClock.uptimeMillis();
	    mHandler.removeCallbacks(mTick);
	    mHandler.post(mTick);
	}

	void stopViewRefreshClock() {
	    mHandler.removeCallbacks(mTick);
	}
	
//    public boolean AllRenderingLayersInitialized() {
//    	boolean rc = true;
//    	for(RenderingLayer layer : mRenderingLayers) {
//    		if(!layer.IsReady()) {
//    			rc = false;
//    			break;
//    		}
//    	}
//    	return rc;
//    }
//
	public void SetIGeodataService(IGeodataService	geodataServiceInterface)  {
		mGeodataServiceInterface = geodataServiceInterface;
		
		if(mBuddyLayer != null) {		// assumes this mapview is created/initialized before binding to geodataservice in mapfragment
			if(mGeodataServiceInterface != null) {
				try {
					mGeodataServiceInterface.registerForBuddies(mParentActivity.getPackageName()); 
				} catch (RemoteException e) {
					e.printStackTrace();
					Log.e(TAG, "Error registering for buddy updates");
				} 
			}
		}

		
    	for(RenderingLayer layer : mRenderingLayers) {
    		if(layer instanceof IDynamicGeoData) {		// if it support iDynamicGeoData interface
    			try {
					((IDynamicGeoData)layer).SetGeodataServiceInterface(geodataServiceInterface);
				} 
    			catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.e(TAG,"Cannot access geodata service during layer initialization... Layer: " + layer);
				}
    		}
    	}
 	}
	
	public void SetGeodataServiceState(GeoDataServiceState geodataServiceState) {
		mGeodataServiceState = geodataServiceState;
		
		if(mGeodataServiceState == null) {
			setViewState(MapViewState.WAITING_FOR_DATA_SERVICE);	// will only happen if geoservice disconnects for some reason
			return;
		}
		if(serviceHasError(mGeodataServiceState)) {
			setViewState(MapViewState.PROBLEM_WITH_DATA_SERVICE);	
		}
		else {
			if(serviceIsReady(mGeodataServiceState)) { 
				Log.d(TAG, "Geodata service is now ready... have state ");

				// for all layers needing GeodataServiceState..., pass it to them
				boolean serviceHasCapabilitiesRequiredByAllLayers = true;
		    	for(RenderingLayer layer : mRenderingLayers) {
		    		if(layer instanceof IDynamicGeoData) {
		    			serviceHasCapabilitiesRequiredByAllLayers = serviceHasCapabilitiesRequiredByAllLayers && ((IDynamicGeoData)layer).SetGeodataServiceState(mGeodataServiceState);
		    		}
		    	}
		    	
				if(serviceHasCapabilitiesRequiredByAllLayers) {
					if(!mCameraViewport.IsReady())	{	// if cameraViewport not set (expected on first time) it's due to lack of user position, so go get it and setup cameraViewport
						try {
							IGeodataServiceResponse rc = mGeodataServiceInterface.getUserLocation();	// causes service to broadcast user location
							if(rc.mResponseCode == IGeodataServiceResponse.ResponseCodes.ERROR_WITH_REQUEST) {
								Log.e(TAG, "Error triggering User Position response from geodata service: " + rc.mErrorMessage);
							}
	
						} 
						catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Log.e(TAG, "Exception thrown while triggering User Position response from geodata service: " + e.getMessage());
						}
						setViewState(MapViewState.WAITING_FOR_LOCATION);	// the reason the camera is not yet initialized
					}
					else {
						setViewState(MapViewState.WAITING_FOR_LAYER_INITIALIZATION);	
					}
				}
				else {
					
					setViewState(MapViewState.REQUIRED_DATA_UNAVAILABLE);	
					Log.e(TAG, "ERROR map layers have an issue with Geodata Service state " + mGeodataServiceState);
				}
			}
			else {
				Log.d(TAG, "Geodata service is not ready ");
				if(mGeodataServiceState.mState ==  GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_NO_USER_LOCATION) {
					setViewState(MapViewState.WAITING_FOR_LOCATION);	
				}
				else {
					setViewState(MapViewState.WAITING_FOR_DATA_SERVICE);
				}
			}
		}
		Log.d(TAG, "ViewState = " + mViewState);
	}
	

// map  
	public boolean IsMapLoaded () {
		return mMapLoaded;
	}

	
	public void ShowClosestItemDescription(boolean enable) {
		mShowClosestItemDescription = enable;
	}
	
	public interface IMapView {
		public void ConfigurePreMapViewInit();		
		public void ConfigurePostMapViewInit();		
		public void MapViewStateChanged(MapView.MapViewState mapViewState);		
		public void LoadMapDataComplete(String link);
		public void LoadingMapWithProgress(int progValue, int timeoutVal);	// called when initial map loading progress is changing
		public void ActiveLoadingMapWithProgress(int progValue);	// called when map loading progress is changing when map displayed
		
	}

	private void RespondToCameraViewportChange(CameraViewport cameraViewport) {
		if(cameraViewport.IsReady() && mGeodataServiceState != null){		// if camera ready and geodata service available
			// pass new camera position to rendering layers...
			for(RenderingLayer layer : mRenderingLayers) {
	    		if(layer instanceof IDynamicCameraPosition) {
	    			((IDynamicCameraPosition)layer).SetCameraPosition(cameraViewport);
	    		}
			}
		}
	}
	public void ForceRedraw() {
		if(mCameraViewport.IsReady() && mGeodataServiceState != null){		// if camera ready and geodata service available
			// pass new camera position to rendering layers...
			for(RenderingLayer layer : mRenderingLayers) {
	    		if(layer instanceof IDynamicCameraPosition) {
	    			((IDynamicCameraPosition)layer).SetCameraPosition(mCameraViewport, true);
	    		}
			}
		}
	}
	
//==========================================================	
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		mMessageTextX = w/2;
		mMessageTextY = h/2+5;
	}
	
	public void setViewState(MapViewState newViewState) {
		if(mViewState != newViewState) {
			mViewState = newViewState;
			mParentFragment.MapViewStateChanged(newViewState);  
		}
	}
	
	
	public void setCameraChangeWithUserVelocity(boolean changeWithVelocity){
		mCameraChangesWithUserVelocity = changeWithVelocity;
	}
	
	public void setExploreModeEnabled(boolean exploreMode){
		mMapViewMode = (exploreMode) ? MapViewMode.EXPLORE_MODE : MapViewMode.NORMAL;
	}
	
	public void setFindModeEnabled(boolean findMode){
		mMapViewMode = (findMode) ? MapViewMode.FIND_MODE : MapViewMode.NORMAL;
		if(findMode) mPreviousZoomScale = mCameraViewport.mCurrentAltitudeScale;
		else mCameraViewport.SetCameraAltitudeScale(mPreviousZoomScale, true);
	}
	
	class MapViewRenderer implements android.opengl.GLSurfaceView.Renderer {
		
		private Context context;
		
		// Shaders for OpenGL rendering
        private ShaderProgram[] mShaderProgram = new ShaderProgram[MeshGL.MeshType.values().length];
		private GLText glText;
		private boolean firstRun = true;
		
		public MapViewRenderer(Context context){
			this.context = context;
		}
				
		@Override
	    public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
			String lastVertFileName = "", lastFragFileName = "";
			ShaderGL vert = null, frag = null;
			ShaderProgram lastShaderProgram = null;

            if(PRINT_GL_INFO) {
                int maxTexUnits[] = new int[3];
                int depthBits[] = new int[2];
                String gpuVendor = GLES20.glGetString(GLES20.GL_VENDOR);
                String gpuRenderer = GLES20.glGetString(GLES20.GL_RENDERER);
                String gpuGLVersion = GLES20.glGetString(GLES20.GL_VERSION);
                GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, maxTexUnits, 0);
                GLES20.glGetIntegerv(GLES20.GL_DEPTH_BITS, depthBits, 0);
                Log.i(TAG, "GPU Vendor: " + gpuVendor + "\nRenderer: " + gpuRenderer + "\nGLES version: " + gpuGLVersion);
                Log.i(TAG, "GLES20 Maximum Texture Image Units supported: " + maxTexUnits[0]);
                Log.i(TAG, "Maximum Depth Bits supported: " + depthBits[0]);
            }
                       
			//Create and load shaders
			for (int meshType = 0; meshType < mRenderSchemeManager.getNumMeshTypes(); meshType++) {
                  String vertFile = mRenderSchemeManager.getVertShaderFile(meshType),
                                  fragFile = mRenderSchemeManager.getFragShaderFile(meshType);
                  if (!lastVertFileName.equals(vertFile) || !lastFragFileName.equals(fragFile)) {
                          if (!lastVertFileName.equals(vertFile)) {
                                  vert = new ShaderGL(mParentActivity, mResources, vertFile, GLES20.GL_VERTEX_SHADER);
                                  lastVertFileName = vertFile;
                          }
                          if (!lastFragFileName.equals(fragFile)) {
                                  frag = new ShaderGL(mParentActivity, mResources, fragFile, GLES20.GL_FRAGMENT_SHADER);
                                  lastFragFileName = fragFile;
                          }
                          mShaderProgram[meshType] = lastShaderProgram = new ShaderProgram(vert, frag);
                  }
                  else {
                          mShaderProgram[meshType] = lastShaderProgram;
                  }
                  mRenderSchemeManager.setMeshes(meshType, new MeshGL(MeshGL.MeshType.values()[meshType], mShaderProgram[meshType]));
          }

            if(GL_TEXT_ENABLED) {
                glText = new GLText(context, mShaderProgram[0].getVertShader().getFilename(),
                        mShaderProgram[0].getFragShader().getFilename(), mShaderProgram[0]);

                // Load the font from file (set size + padding), creates the texture
                // NOTE: after a successful call to this the font is ready for rendering!
                glText.load("eurostib_1", 18, 2, 2);
            }
			
//			GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//			GLES20.glDepthFunc(GLES20.GL_LEQUAL);
			GLES20.glEnable(GLES20.GL_BLEND);
			GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
	    }

		@Override
	    public void onSurfaceChanged(GL10 gl, int w, int h) {
	        gl.glViewport(0, 0, w, h);
	        if(GL_TEXT_ENABLED) glText.setScreenWidthHeight(w, h);
	        android.opengl.Matrix.perspectiveM(mCameraViewport.mCamProjMatrix, 0, 60, ((float)w/(float)h), mCameraViewport.setZNear(0.01f), 10000f);
	    }

		@Override
	    public void onDrawFrame(GL10 gl) {
	    	GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
	    	GLES20.glClearColor(0f, 0f, 0f, 1f);
	    	// draw background grid...

	    	if(mRenderSchemeManager.mLoadingSettingsFile) return;	// don't try and draw map if rendering scheme data is being (re)loaded 

	    	if(mLastViewState != mViewState) {
	    		Log.d(TAG, "===== State Changed to: " + mViewState);
	    	}
	    	mLastViewState = mViewState;

	    	if(mRenderSchemeManager.mErrorSchemesNotLoadedCorrectly) {
	    		setViewState(MapViewState.ERROR_LOADING_THEMES);	// will only happen if render scheme file is corrupted or missing
	    	}
	    	if(mNotEnoughMemoryError) {
	    		setViewState(MapViewState.OUT_OF_MEMORY);	// will only happen if render scheme file is corrupted or missing
	    	}

			// messages in orthoganal projection
			// scenes in perspective

		switch(mViewState) {
		case INITIALIZING:
		case ERROR_INITIALIZING:
		case PROBLEM_WITH_DATA_SERVICE:
		case REQUIRED_DATA_UNAVAILABLE:
		case OUT_OF_MEMORY:
		case ERROR_LOADING_THEMES:
			break;		// don't do anything... fragment will draw appropriate message

		case WAITING_FOR_DATA_SERVICE:
		case WAITING_FOR_LOCATION:
			if(!mCameraIsReady) {	// first time camera is ready
				if(mCameraViewport.IsReady()) {
					mCameraIsReady = true;
					
					RespondToCameraViewportChange(mCameraViewport);  // force all camera changes to layers (catches early parm configuration)
				}
			}
			else {		// to avoid getting stuck in WAITING_FOR_LOCATION if mCameraIsReady accidentally set true
				RespondToCameraViewportChange(mCameraViewport);  // force all camera changes to layers (catches early parm configuration)
				if(mViewState == MapViewState.WAITING_FOR_LOCATION) {
					setViewState(MapViewState.WAITING_FOR_LAYER_INITIALIZATION);	
				}
			}
			break;
		
		case WAITING_FOR_LAYER_INITIALIZATION:		// don't get here until geodata service and camera are ready
			if(!mCameraIsReady) {	// first time camera is ready
				if(mCameraViewport.IsReady()) {
					mCameraIsReady = true;
					
					RespondToCameraViewportChange(mCameraViewport);  // force all camera changes to layers (catches early parm configuration)
				}
			}
			boolean allLayersAreReady = true;
			for(RenderingLayer layer : mRenderingLayers) {
				layer.CheckForUpdates();			// check for new data on all layers... if available switch to it for this draw cycle
				Boolean layerIsReady = layer.IsReady();
				allLayersAreReady = allLayersAreReady && layerIsReady;
			}
			if(allLayersAreReady) { 
				if((mParentActivity instanceof IReconMapViewCallbacks)) {
					((IReconMapViewCallbacks) mParentActivity).MapViewReadyForAnnotation();
				}
				mFirstUpdate = true;
				setViewState(MapViewState.DRAW_LAYERS);	
			}
			
			if(mMapLayer != null && mMapLayer.mLoadWaitingForTiles != null) {
				int tileLoadProgress = mMapLayer.mLoadWaitingForTiles.size();
				if(tileLoadProgress > mProgressLimit) {
					mProgressLimit = tileLoadProgress;
					mCurrentProgress = 0.f;
					mProgressStepSize = 1.0f/(float) mProgressLimit * 3;
				}
				mTargetProgress = ((float)(mProgressLimit-tileLoadProgress) /(float)mProgressLimit);
			
				if(mCurrentProgress != mTargetProgress) {
					if( (mTargetProgress-mCurrentProgress) > mProgressStepSize && mTargetProgress < 0.95) {
						mCurrentProgress += mProgressStepSize;
					}
					else {
						mCurrentProgress = mTargetProgress;
					}
				}
				int progressValue = (int)(((mCurrentProgress * (mMapViewDimensions.x-(float)PROGRESS_START_VALUE)) + PROGRESS_START_VALUE) / mMapViewDimensions.x * 100);
				int timeoutValue = (int)((float)(System.currentTimeMillis() - mMapLayer.mCurrentLoadStartTime)/(float)mMapLayer.CREATE_DRAWING_SET_MAX_WAIT_TIME_IN_MS * 100);
				if(timeoutValue > 120) timeoutValue = 0;  // handles case of mCurrentLoadStartTime not set 
				mParentFragment.LoadingMapWithProgress(progressValue, timeoutValue);
			}
			break;


	    	case DRAW_LAYERS:							// don't get here until geodata service, camera and layers are ready
	    		assert mWorld2DrawingTransformer.isDrawingOriginSet();		// origin set after drawingSet loaded and shouldn't get here until drawingSet is loaded
	    		boolean updateTexture = false;

	    		mDrawingMap = true;
	    		
	    		// for the very first draw call, skip checking for updates again 
	    		// since the update has been consumed and force update the map texture
	    		if(firstRun){
	    			firstRun = false;
	    			updateTexture = true;
	    		}
	    		else {
	    			
	    			if(mReticleLayer != null) {
		    			if(mReticleLayer.mRolloverNamingEnabled) {
		    				if(mMapViewMode == MapViewMode.EXPLORE_MODE){
		    					FindPOIClosestToCenter();
		    					setRolloverTextView(mClosestPOIItemName, mPOIDistanceFromMe);
		    				}
		    				else {
		    					setRolloverTextView("", 0);
		    				}
		    			}		
			    		mReticleLayer.ClearItems();
		    		}
	    			
		    		for(RenderingLayer layer : mRenderingLayers) {
		    			if(layer.CheckForUpdates() && layer.IsReady()){		// check for new data on all layers... if available switch to it for this draw cycle
		    				updateTexture = true;
		    				if(mFirstUpdate && mMapLayer.allMissingTiles()) {
		    					EmptyTileSetWarningMessage();
		    					mFirstUpdate = false;
		    				}
		    			}	
		    			
		    			if(mReticleLayer != null) {
		    				if(mReticleLayer.IsEnabled() && layer instanceof DynamicReticleItemsInterface.IDynamicReticleItems){
		    					ArrayList<ReticleItem> reticleItems = ((DynamicReticleItemsInterface.IDynamicReticleItems) layer).GetReticleItems(mCameraViewport, 0f);
		    					mReticleLayer.AddItems(reticleItems);
		    				}
		    			}
		    		}
	    		}
	    		//			UpdateBuddies((float) mMaxDrawingY);	// go poll for buddies
	    		
	    		for(RenderingLayer layer : mRenderingLayers) {
	    			boolean isNormalViewMode = (mMapViewMode == MapViewMode.NORMAL);
	    			layer.Draw(mRenderSchemeManager, mParentActivity.getResources(), mCameraViewport, (isNormalViewMode) ? "" : mClosestPOIItemID, updateTexture, mMapViewMode, glText);
	    		}
	    		
	    		break;
	    	}	// end switch
	    }
	}
	
	/**
	 * Sets the overlaying TextView on maps, intended to be used in Explore mode. 
	 * @param rolloverText Text to show on overlay
	 * @param distanceFromMe Distance to show on overlay. Input values are in meteres or feet.
	 */
	public void setRolloverTextView(final String rolloverText, final float distanceFromMe){
		mParentActivity.runOnUiThread(new Runnable(){
			@Override
			public void run(){
				if((mParentFragment instanceof MapFragment_Explore) && rolloverText != null && !rolloverText.equals("")){
					mRolloverContainer = ((MapFragment_Explore)mParentFragment).getRolloverContainer();
					mRolloverNameTV = ((MapFragment_Explore)mParentFragment).getRolloverTextView();
					mRolloverDistanceTV = ((MapFragment_Explore)mParentFragment).getRolloverDistance();
					mRolloverNameTV.setText(rolloverText);
					if(SettingsUtil.getUnits(mParentActivity.getBaseContext()) == SettingsUtil.RECON_UNITS_METRIC){
						if(distanceFromMe >= ConversionUtil.KM_TO_METERS_RATIO){
							mRolloverDistanceTV.setText(String.format("%.1fkm", ConversionUtil.metersToKm(distanceFromMe)));
						}
						else {
							mRolloverDistanceTV.setText(String.format("%.0fm", distanceFromMe));
						}
					}
					else {
						if(distanceFromMe >= ConversionUtil.MILES_TO_FEET_RATIO){ // mile = 5280 feet
							mRolloverDistanceTV.setText(String.format("%.1fmi", ConversionUtil.feetToMiles(distanceFromMe)));
						}
						else {
							// else show feet
							mRolloverDistanceTV.setText(String.format("%.1fft", distanceFromMe));
						}
					}
					mRolloverContainer.setVisibility(View.VISIBLE);
				}
				else {
					mRolloverContainer = ((MapFragment_Explore)mParentFragment).getRolloverContainer();
					mRolloverNameTV = ((MapFragment_Explore)mParentFragment).getRolloverTextView();
					mRolloverDistanceTV = ((MapFragment_Explore)mParentFragment).getRolloverDistance();
					mRolloverNameTV.setText("");
					mRolloverDistanceTV.setText("");
					mRolloverContainer.setVisibility(View.INVISIBLE);
				}
			}
		});
	}


	public Bitmap getBackgroundImage() {
		return mMapLayer.getBackgroundImage();
	}
	
	private void FindPOIClosestToCenter() {	// assumes only called if mReticleLayer!= null
		// loop all rendering layers...
		mFocusTestList.clear();
		for(RenderingLayer layer : mRenderingLayers) {
			if(layer instanceof DynamicFocusableItemsInterface.IDynamicFocusableItems) {
				mFocusTestList.addAll(((DynamicFocusableItemsInterface.IDynamicFocusableItems)layer).GetFocusableItems());
			}
		}

//		Log.e(TAG, "FocusTestList size " + mFocusTestList.size());

		// create list of focused objects using some criteria... closest to screen center within X pixels...
//		Bitmap tempBitmap = mRenderSchemeManager.GetPOIBitmap(RenderSchemeManager.BitmapTypes.BUDDY.ordinal(),0, mCameraViewport.mCurrentAltitudeScale);		// get example icon at scale for dimension calculation
//		double iconRadius = (double)(tempBitmap.getWidth()/2.0);
//		POIDrawing tempIcon = mFocusTestList.get(0);
		double detectionRadius = 0;
		
//		mMapLayer.getMapDrawingSet().mImageSize;
		//				Log.e(TAG, "closest detection radius " + radius); // 91.2

		
		mDrawingCameraViewportCenter = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(new PointXY((float)mCameraViewport.mCurrentLongitude,(float) mCameraViewport.mCurrentLatitude));
		
		float closestSqDistance = 999999999.f; 
//		float distFromScreenCenterInM = 0.f;
		String closestItemID = null;
		String closestItemName = null;
		WO_POI closestItem = null;
		double chosenLat = 0;
		double chosenLong = 0;
		for(POIDrawing item : mFocusTestList) {
			detectionRadius = 0.5 * item.getScaleSize() * (mCameraViewport.mCurrentAltitudeScale / mCameraViewport.getMaxZoomScale());
			mDrawingReticuleBoundary.left = (float) (mDrawingCameraViewportCenter.x - detectionRadius);
			mDrawingReticuleBoundary.right = (float) (mDrawingCameraViewportCenter.x + detectionRadius);
			mDrawingReticuleBoundary.bottom = (float) (mDrawingCameraViewportCenter.y + detectionRadius);   
			mDrawingReticuleBoundary.top = (float) (mDrawingCameraViewportCenter.y - detectionRadius);
			
			if(mDrawingReticuleBoundary.contains(item.mLocation.x, item.mLocation.y)) {
				float diffX = mDrawingCameraViewportCenter.x - item.mLocation.x;
				float diffY = mDrawingCameraViewportCenter.y - item.mLocation.y;
				float sqDistToCenter = diffX*diffX + diffY*diffY;
				if(sqDistToCenter < closestSqDistance) {
//
//				
//				distFromScreenCenterInM = World2DrawingTransformer.DistanceBetweenGPSPoints((float)mCameraViewport.mCurrentLongitude,  (float)mCameraViewport.mCurrentLatitude, 
//						((WO_POI)item.mDataObject).mGPSLocation.x, ((WO_POI)item.mDataObject).mGPSLocation.y );
//				if(distFromScreenCenterInM < closestDistance) {
//					Log.d(TAG, "closest so far... " + (String) ((WO_POI)item.mDataObject).mName);
					closestSqDistance = sqDistToCenter;
					closestItem = (WO_POI)item.mDataObject;
					closestItemID = item.mDataObject.mObjectID;
					closestItemName = item.mDataObject.mName;
					chosenLong = ((WO_POI)item.mDataObject).mGPSLocation.x;
					chosenLat = ((WO_POI)item.mDataObject).mGPSLocation.y;
				}
			}
			// find closest ------- String focusedItemID = ...canvas;
		}
		if(closestItemID == null || closestItemName == null){
			mClosestPOIItemID = "";
			mClosestPOIItemName = "";
			mPOIDistanceFromMe = 0;
		}
		else {
			PointXY currentUserArrowPosition = mUserIconLayer.getCurrentUserPosition();
			if(mClosestPOIItemID.equalsIgnoreCase(closestItemID)){
				if((mLastUserIconPosition.x == currentUserArrowPosition.x)
				        && (mLastUserIconPosition.y == currentUserArrowPosition.y)){
					mReticleLayer.SetRollover(closestItem, closestSqDistance);
					return;
				}
			}
			mClosestPOIItemID = closestItemID;
			mClosestPOIItemName = closestItemName;
			
			//get distance in meters
			mPOIDistanceFromMe = World2DrawingTransformer.DistanceBetweenGPSPoints(
                                                                currentUserArrowPosition.x, currentUserArrowPosition.y,
                                                                (float)chosenLong, (float)chosenLat);
			// conversion to feet if User wants Imperial units
			if(SettingsUtil.getUnits(mParentActivity.getBaseContext()) == SettingsUtil.RECON_UNITS_IMPERIAL) {
			    mPOIDistanceFromMe = (float) ConversionUtil.metersToFeet(mPOIDistanceFromMe);
			}
			mLastUserIconPosition = currentUserArrowPosition;
		}
		mReticleLayer.SetRollover(closestItem, closestSqDistance);
	}
			

	public void HandleNewBuddies() {
		if(mBuddyLayer != null) {
			mBuddyLayer.HandleNewBuddies();
            if(mBuddiesUpdatedListener != null){
                mBuddiesUpdatedListener.onBuddiesUpdated(mBuddyLayer.getNumBuddies());
            }
            mBuddyLayer.CheckForUpdates();
			invalidate();
		}
	}
	
	public PointXY getBackgroundImageCenterDrawingCoords(){
		return mMapLayer.getBackgroundImageCenterDrawingCoords();
	}


//	
//	public void LayerStateChanged(RenderingLayer reportingLayer, RenderingLayerState layerState) {
////		if(!mAllLayersReady) {
////			mAllLayersReady = true;
////			for(RenderingLayer layer : mRenderingLayers) {
////				mAllLayersReady = mAllLayersReady && layer.IsInitialized();
////			}
////		}
////		if(mAllLayersReady) {
//			if(reportingLayer instanceof Map2DLayer && reportingLayer.mLayerState == RenderingLayerState.REQUIRED_DATA_UNAVAILABLE) {
//				setViewState(MapViewState.REQUIRED_DATA_UNAVAILABLE);
//			}
////			else {
////				setViewState(MapViewState.DRAW_LAYERS);
////			}
////		}
//	}

	public ArrayList<BuddyItem> GetSortedBuddyList() {
		return mBuddyLayer.GetSortedBuddyList();
	}

	/**
	 * If true, the pitch will have gradually less influence from the user
	 * based on the user's current velocity
	 * @param taperPitch
	 */
	public void setForceTaperPitchWhileMoving(boolean taperPitch){
		mCameraViewport.setForceTaperPitchWhileMoving(taperPitch);
	}
	
	public void setThirdPersonView(boolean setThirdPersonView){
		if(mMapLayer != null){
			mMapLayer.setThirdPersonView(setThirdPersonView);
		}
		else {
			mSetViewToThirdPerson = true;
		}
	}
	
	public void setHighlightedBuddy(String objectID){
		if(mReticleLayer == null) {
			mClosestPOIItemID = objectID;
		}
	}

    public void setOnBuddiesUpdatedListener(OnBuddiesUpdatedListener listener){
        mBuddiesUpdatedListener = listener;
    }

    /**
     * Gets one of the 4 scale levels defined in rendering_schemes.xml, based on what the
     * current altitude scale is.
     * @return the scale level
     */
    public int getCurrentScaleLevel(){
        return mRenderSchemeManager.getScaleRange(GetCameraAltitudeScale());
    }

}
