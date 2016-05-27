package com.reconinstruments.mapsdk.mapview;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoDataServiceState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataService;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataServiceResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.cameraposition.DynamicCameraPositionInterface.IDynamicCameraPosition;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.geodataservice.DynamicGeoDataInterface.IDynamicGeoData;
import com.reconinstruments.mapsdk.mapview.renderinglayers.RenderingLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer.CustomAnnotationCache;
import com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer.CustomLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer.BackgroundSize;
import com.reconinstruments.utils.DeviceUtils;

/**
 * @author stevenmason
 *
 */
public class StaticMapGenerator extends View  {
	
// Constants
	private static final String TAG = "StaticMapGenerator";
	
	public  final  static String GEODATASERVICE_BIND_CLIENT_INTERFACE = "com.reconinstruments.geodataservice.clientinterface";
	private static final long GEODATA_SERVICE_CONNECTION_REQUEST_TIMEOUT = 2000;
	private static final long GEODATA_SERVICE_MAP_LOAD_TIMEOUT = 60000;

	private static final boolean WAIT_FOR_DEBUGGER = false;
	private static final int USER_ICON_SIZE = 15;
	private static final double BASE_CAMERA_VIEWPORT_WIDTH_IN_METERS = 2000.;
	private static final int RETICULE_DISTANCE = 61;
																					// the larger this value, the more memory is used for the two drawingSet background images
	
	public enum GDSState {
		WAITING_FOR_DATA_SERVICE,	// during startup
		PROBLEM_WITH_DATA_SERVICE,
		REQUIRED_DATA_UNAVAILABLE,	// service is running but does does not have desired capabilities to supply data
		WAITING_FOR_LAYER_INITIALIZATION
	}
	public enum CameraPanDirection {
		LEFT,
		UP,
		RIGHT,
		DOWN
	}
	
	public enum MapType {
		STREET
	}

// members
	protected Activity 				mParentActivity = null;
	public    String 				mIdentifier = "notDefined";
	public 	  String				mCurrentRequestID = null;
	
	
	protected RenderSchemeManager 	mRenderSchemeManager = null;
	static GeodataServiceConnection	mGeodataServiceConnection = null;
	public  static IGeodataService	mGeodataServiceInterface  = null;
	
	public  GeoDataServiceState 	mGeodataServiceState = null;
	GDSState						mServiceState = null;
	public World2DrawingTransformer	mWorld2DrawingTransformer= null;
	protected PointXY				mMapViewDimensions= null;
			
	protected CameraViewport		mCameraViewport = null;
	protected Map2DLayer			mMapLayer;
	protected CustomLayer			mCustomLayer;
	
	public ArrayList<RenderingLayer> mRenderingLayers = new ArrayList<RenderingLayer>();
	
	boolean							mCameraIsReady = false;
	boolean							mDrawingMap = false;
	boolean							mNotEnoughMemoryError = false;
	boolean 						mHaveUserLocation = false;
	boolean							mAllLayersReady = false;
	protected boolean 				mShowClosestItemDescription = false;	
	protected boolean				mMapLoaded = false;
	
	public float					mMapPreloadMultiplier = 1.0f;		// non-documented API, used by MapFragment 
	protected double		    	mAspectRatio = 0.0;
	protected double 				mUserLatitude = 0.0;
	protected double 				mUserLongitude = 0.0;
	protected float 				mCameraHeading = 0.0f;
	protected float 				mUserHeading = 0.0f;
   
	boolean							mTickTimeout = false;
	public 	Canvas 				    mBitmapCanvas = null;
	public 	Bitmap					mBitmapImage = null;	
	String							mProcessingError = "";
	
	protected OverlayDialog			mMissingTilesOverlay;
	
// timeout clock/timer members
	Handler mHandler = new Handler();
	Runnable mTick = new Runnable() {
	    public void run() {
	    	mTickTimeout = true;
	    }
	};

// interfaces
	public interface IReconMapImageGeneratorCallbacks {
		public void AddAnnotations();  			// called when generator is ready for annotations 
	}

//======================================================================================
// constructor	
	public StaticMapGenerator(Activity parentActivity, String identifier, MapType mapType) throws Exception {	
 		super(parentActivity);
 		mParentActivity = parentActivity;
		mIdentifier = identifier;
		
 		String rsmXmlFile = mParentActivity.getResources().getString(R.string.rendering_schemes);

		String productName = Build.PRODUCT;
	    
	    mRenderSchemeManager = new RenderSchemeManager(parentActivity, productName, mParentActivity.getResources(), mParentActivity.getPackageName(), rsmXmlFile) ;
		mWorld2DrawingTransformer = new World2DrawingTransformer();
		
		mCameraViewport = new CameraViewport(parentActivity, BASE_CAMERA_VIEWPORT_WIDTH_IN_METERS, mRenderSchemeManager);
		
        mAllLayersReady = false;
        
        String mapTypeName = (MapView.OSM_BASE_MAP) ? parentActivity.getResources().getString(R.string.osm_base_config)
                                            : parentActivity.getResources().getString(R.string.osm_street_config);
//        switch(mapType) {  // TODO if supporting other mapTypes
//        	case STREET: {
//        		mapTypeName = "osm_street_map";
//        		break;
//        	}
//        }
		mCustomLayer = new CustomLayer(parentActivity, mRenderSchemeManager, mWorld2DrawingTransformer);
		mMapLayer = new Map2DLayer(mapTypeName, parentActivity, mRenderSchemeManager, mWorld2DrawingTransformer, mIdentifier, 0.f, BackgroundSize.MINIMAL, mCustomLayer);
		
		mRenderingLayers.add(mMapLayer);
		mRenderingLayers.add(mCustomLayer);

		mMissingTilesOverlay = new OverlayDialog(mParentActivity,
                DeviceUtils.isSun() ? R.layout.overlay_dialog_jet : R.layout.overlay_dialog_snow, android.R.style.Theme_Black_NoTitleBar_Fullscreen, true);
		mMissingTilesOverlay.setOnKeyListener(new DialogInterface.OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if(event.getAction() == KeyEvent.ACTION_UP){
					if(keyCode == KeyEvent.KEYCODE_BACK){
						dialog.dismiss();
						mParentActivity.finish();
						return true;
					}
				}
				return false;
			}
		});
	}
	 
	
	public Bitmap GenerateMapImage(int width, int height, GeoRegion geoRegion, float percentWidthMargin, float cameraHeading){  
		// returns null on error of any kind... use GetErrorMessage() to see reason for failure

		if(Looper.myLooper() == Looper.getMainLooper()) {
			Log.e(TAG, "Cannot call StaticMapGenerator:GenerateMapImage() from main UI thread.");
			mProcessingError = "Cannot call StaticMapGenerator:GenerateMapImage() from main UI thread.";
			return null;
		}
		
		// this method blocks while the map is draw so it should be run from a thread
		mCameraHeading = cameraHeading;
		
		if(mGeodataServiceConnection != null) {
			mProcessingError = "Generator is busy, try again later...";
			return null;
		}
		mGeodataServiceConnection = new GeodataServiceConnection();
		mParentActivity.bindService(new Intent(GEODATASERVICE_BIND_CLIENT_INTERFACE), mGeodataServiceConnection, Context.BIND_AUTO_CREATE);
//		Log.d(TAG, "BindMapsService:bindService");

		Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        mBitmapImage = Bitmap.createBitmap(width, height, conf); // this creates a MUTABLE bitmap
		mBitmapCanvas = new Canvas(mBitmapImage);
		if(mBitmapImage == null || mBitmapCanvas == null) {
			mProcessingError = "Not enough memory to allocate bitmap.";
			return null;
		}
		Log.d(TAG, "Bitmap created");

		mHandler.removeCallbacks(mTick);
		mTickTimeout = false;
		mHandler.postDelayed(mTick, GEODATA_SERVICE_CONNECTION_REQUEST_TIMEOUT); 	
		int waitCnt = 0;
		while((mGeodataServiceInterface == null || mServiceState == null) && !mTickTimeout) {
			try {			
				Log.d(TAG, "Waiting for mGeodataServiceConnection and state: " + (waitCnt++ * 20) + "ms");
				Thread.sleep(20);
			}
			catch (Exception e) {
			}
		}
		try {
			if(mGeodataServiceInterface == null || mServiceState == null) {
				throw new Exception("GeodataService interface not available. Try later.");
			}
		
			switch (mServiceState) {
				case WAITING_FOR_DATA_SERVICE: {
					throw new Exception("GeodataService not yet available. Try later.");
				}
				case PROBLEM_WITH_DATA_SERVICE: {
					throw new Exception("GeodataService not available. Internal error. Reboot required");
				}
				case REQUIRED_DATA_UNAVAILABLE: {
					throw new Exception("GeodataService cannot provide data retuired for requested map.  Check map configuration files.");
				}
				case WAITING_FOR_LAYER_INITIALIZATION: {	// only case to continue on 
				}
			}
			
			if(mCameraViewport != null) {
				mAspectRatio = (double)width/(double)height;		
				mCameraViewport.SetViewPortDimensions(width,height);
				mMapViewDimensions = new PointXY(width,height);
				mCameraViewport.SetViewAngleRelToNorth(mCameraHeading, true);
				mCameraViewport.SetCameraToShowGeoRegion(geoRegion, percentWidthMargin, true, false);		
				mWorld2DrawingTransformer.SetDrawingOrigin(geoRegion.mCenterPoint.x, geoRegion.mCenterPoint.y);		// long, lat before we can get here...
			} 
			else {
				throw new Exception("Internal error: map camera is null.  Possibly low memory?");
			}
			
			if(mParentActivity != null && mParentActivity instanceof IReconMapImageGeneratorCallbacks) {
				((IReconMapImageGeneratorCallbacks)mParentActivity).AddAnnotations();  			// callback so caller can add annotations (only after mWorld2DrawingTransformer.SetDrawingOrigin has been set)
			}
			
			for(RenderingLayer layer : mRenderingLayers) {
	    		if(layer instanceof IDynamicCameraPosition) {
	    			((IDynamicCameraPosition)layer).SetCameraHeading(cameraHeading);
	    			((IDynamicCameraPosition)layer).SetCameraPosition(mCameraViewport);		// will trigger data loading - IsReady() is true when everything is loaded
	    		}
	    	}
			
			
		    mHandler.removeCallbacks(mTick);
			mTickTimeout = false;
			mHandler.postDelayed(mTick, GEODATA_SERVICE_MAP_LOAD_TIMEOUT); 	
			mAllLayersReady = false;
			waitCnt = 0;
			while(!mAllLayersReady && !mTickTimeout) {
				Thread.sleep(1000);
				mAllLayersReady = true;
				for(RenderingLayer layer : mRenderingLayers) {
					layer.CheckForUpdates();
					mAllLayersReady = mAllLayersReady && layer.IsReady();
		    	}
			}
			if(!mAllLayersReady) {
				throw new Exception("Unable to draw image in maximum time: " + (GEODATA_SERVICE_MAP_LOAD_TIMEOUT / 1000) + "sec. Image creation aborted.");
			}
			if(mMapLayer.IsMissingTiles()) {
				showMissingTilesOverlay();
				throw new Exception("Unable to draw image - not all tile data is available");
			}
			
			//everything is set, so draw image on canvase
			mBitmapCanvas.drawColor(Color.argb(255, 0x1B, 0x1D, 0x21));
			Resources res = mParentActivity.getResources();
			for(RenderingLayer layer : mRenderingLayers) {
    			layer.Draw(mBitmapCanvas, mCameraViewport, null, res);
	    	}
			
			if (mGeodataServiceConnection != null) {
				mParentActivity.unbindService(mGeodataServiceConnection);
				mGeodataServiceConnection = null;
			}
			
			return mBitmapImage;
			
		} 
		catch (Exception e) {
			mProcessingError = e.getMessage();
			if (mGeodataServiceConnection != null)		{
				mParentActivity.unbindService(mGeodataServiceConnection);
				mGeodataServiceConnection = null;
				Log.d(TAG, "UnbindMapsService:unbindService due to exception...");
			}
			Log.e(TAG, "Exception caught: " + e.getMessage());
			return null;
		}
	}
	
	
	public String GetErrorMessage() {
		return mProcessingError;
	}
	
	// calculations / filtering
	public ArrayList<PointXY> RemoveRedundantPathPointsForCurrentViewport(ArrayList<PointXY> pathNodes) {
		if(mCameraViewport != null) {
			return mCameraViewport.RemoveRedundantPathPointsForCurrentViewport(pathNodes);
		}
		return null;
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

	
	// --------------------- GeodataService
	class GeodataServiceConnection implements ServiceConnection	{
		public void onServiceConnected(ComponentName className, IBinder boundService) {
			Log.d(TAG, "connected to Geodata Service client interface");
			// returns IBinder for service.  Use this to create desired (predefined) interface for service
			mGeodataServiceInterface = IGeodataService.Stub.asInterface((IBinder)boundService);
			if(mGeodataServiceInterface == null) {
				Log.e(TAG, "GeodataService bind error: invalid IGeodataService object");
			}
			else {
				IGeodataServiceResponse response;
				try {
					response = mGeodataServiceInterface.getServiceState();		// go retrieve service state after binding to service
					mGeodataServiceState = response.mServiceState;
					Log.d(TAG, "GDS state= " + mGeodataServiceState);
					SetGeodataServiceState(mGeodataServiceState);				// configure state
					
			    	for(RenderingLayer layer : mRenderingLayers) {
			    		if(layer instanceof IDynamicGeoData) {		// if it support iDynamicGeoData interface
			    			try {
								((IDynamicGeoData)layer).SetGeodataServiceInterface(mGeodataServiceInterface);
							} 
			    			catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								Log.e(TAG,"Cannot access geodata service during initialization... Layer: " + layer);
							}
			    		}
			    	}

				} 
				catch (RemoteException e) {
					e.printStackTrace();			//TODO handle API call failures
				}
			}

		}

		public void onServiceDisconnected(ComponentName className)	{	
		    Log.d(TAG, "Map Data Service interface disconnected");
		    mGeodataServiceInterface = null;
			mGeodataServiceState= null;
	    	for(RenderingLayer layer : mRenderingLayers) {
	    		if(layer instanceof IDynamicGeoData) {		// if it support iDynamicGeoData interface
	    			try {
						((IDynamicGeoData)layer).SetGeodataServiceInterface(mGeodataServiceInterface);
					} 
	    			catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.e(TAG,"error during geodata service disconnect in layer update for Layer: " + layer);
					}
	    		}
	    	}

		}
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
		if(mGeodataServiceState != null && 
		   (mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_STALE_USER_LOCATION ||		
			mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_NO_USER_LOCATION ||		
			mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION)) {
			return true;
		}
		return false;
	}
	
	public void SetGeodataServiceState(GeoDataServiceState geodataServiceState) {
		mGeodataServiceState = geodataServiceState;
		
		if(mGeodataServiceState == null) {
			mServiceState = GDSState.WAITING_FOR_DATA_SERVICE;	// will only happen if geoservice disconnects for some reason
			return;
		}
		if(serviceHasError(mGeodataServiceState)) {
			mServiceState = GDSState.PROBLEM_WITH_DATA_SERVICE;	
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
					mServiceState = GDSState.WAITING_FOR_LAYER_INITIALIZATION;	
				}
				else {
					mServiceState = GDSState.REQUIRED_DATA_UNAVAILABLE;	
					Log.e(TAG, "ERROR map layers have an issue with Geodata Service state " + mGeodataServiceState);
				}
			}
			else {
				Log.d(TAG, "Geodata service is not ready ");
				mServiceState = GDSState.WAITING_FOR_DATA_SERVICE;
			}
		}
	}
	
	public boolean isMissingTiles(){
		return mMapLayer.IsMissingTiles();
	}

	private void showMissingTilesOverlay(){
		mParentActivity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				if(!mMissingTilesOverlay.isShowing()) mMissingTilesOverlay.show();
			}
			
		});
	}
}
