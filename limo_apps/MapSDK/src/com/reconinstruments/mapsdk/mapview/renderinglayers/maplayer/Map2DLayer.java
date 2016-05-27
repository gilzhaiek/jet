package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import org.xmlpull.v1.XmlPullParser;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.util.Xml;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoDataServiceState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoTile;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataService;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataServiceResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability.DataSources;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.StaticMapDataCapability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.ObjectTypeList;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.SourcedObjectType;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;
import com.reconinstruments.mapsdk.mapview.MapView.MapViewMode;
import com.reconinstruments.mapsdk.mapview.TileLoader;
import com.reconinstruments.mapsdk.mapview.TileLoader.ITileLoaderCallbacks;
import com.reconinstruments.mapsdk.mapview.TileLoader.TileLoadRequest;
import com.reconinstruments.mapsdk.mapview.WO_drawings.GeoTileDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.POIDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing.WO_Class;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing.WorldObjectDrawingTypes;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.cameraposition.DynamicCameraPositionInterface.IDynamicCameraPosition;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.focusableitems.DynamicFocusableItemsInterface.IDynamicFocusableItems;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.garbagecollection.DynamicGarbageCollectionInterface.IDynamicGarbageCollection;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.geodataservice.DynamicGeoDataInterface.IDynamicGeoData;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.reticleitems.DynamicReticleItemsInterface.IDynamicReticleItems;
import com.reconinstruments.mapsdk.mapview.renderinglayers.DrawingSet.DSChangeResponseCode;
import com.reconinstruments.mapsdk.mapview.renderinglayers.RenderingLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer.CustomLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition.MapCompositionItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition.MapCompositionNameItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition.MapCompositionObjectItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.GLText;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Map2DLayer extends RenderingLayer implements ITileLoaderCallbacks, IDynamicGeoData, IDynamicCameraPosition, IDynamicReticleItems, IDynamicFocusableItems, IDynamicGarbageCollection {
// constants
	private final static String TAG = "Map2DLayer";
	private static final boolean JET_DEMO = true; // temp hack to do jet demo	
	private static final double ROTATE_RELOAD_THRESHOLD = 30.0; // need this change in degrees before map will reload on rotation	
	private static final int BACKGROUND_IMAGE_SIZE_BASE = 512;
	static final int IMAGE_SCALE_MULTIPLIER = 2;
	private static final float CAMERAVIEWPORT_PRELOAD_GEOREGION_MULTIPLIER = 0.4f;	// the size of GR loaded in the DrawingSet used as for preload testing
    private final static String MAP_DATA_FOLDER = "ReconApps/MapData";
	private final static String MAP_TYPES_FOLDER = "ReconApps/MapData/MapTypes";
	public static final int CREATE_DRAWING_SET_MAX_WAIT_TIME_IN_MS = 180000;
	private static final int CREATE_DRAWING_SET_MIN_WAIT_TIME_IN_MS = 30000;
	private static final int CREATE_DRAWING_SET_BASE_WAIT_TIME_PER_TILE_IN_MS = 1000;
	private static final long GC_TIME_IN_MS = 300000;


	private final Semaphore mAccessToDrawingSetLoad = new Semaphore(1, true);
	private final Semaphore mAccessToWaitingForTilesArray = new Semaphore(1, true);
	private final Semaphore mAccessToDrawingSetLoadHandling = new Semaphore(1, true);

	public enum BackgroundSize {
		NORMAL,
		MINIMAL
	}

	
// members
			CustomLayer				mAnnotationLayer = null;
	private Context					mContext;
			boolean 				mEnabled = true;   // ie, use layer when drawing
			boolean					mCreatingNewDrawingSet = false;
			boolean					mLoadingNewDrawingSet = false;
			MapRLDrawingSet			mMapRLDrawingSet = null;
	public 	MapTileCache			mDrawingObjectCache = new MapTileCache();
			String					mMapCompositionID;
	public  static IGeodataService	mGeodataServiceInterface  = null;
			GeoDataServiceState		mGeodataServiceState = null;
			CameraViewport			mCameraViewport = null;
	private ObjectTypeList 			mGeodataServiceMapComposition = null;
			double					mUserLatitude = 0.;
			double					mUserLongitude = 0 ;
	public	float 					mGeoRegionPreloadMultiplier = 1.0f;
			// predefine arrays used during load to avoid alloc thrashing
	
			Date					mLastCGDate = null;
			boolean 				mGCInProgress = false;
			public TileLoader		mTileLoader = new TileLoader(this, 10, 2);
			int						mMaxTilesPostGC = 0;
			int						mImageScaleMultiplier = IMAGE_SCALE_MULTIPLIER;
			int 					mNumNoDataTilesReturned = 0;
			public long					mCurrentLoadStartTime =0;
	LoadNewDrawingSetTask 			mLoadNewDrawingSetTask 	= null;
	Date							mLoadNewDrawingSetTime = null;
	public World2DrawingTransformer	mWorld2DrawingTransformer= null;
	public  HashMap<Integer, Integer> 	mLoadWaitingForTiles = new HashMap<Integer, Integer>();
	public  ArrayList<MapCompositionItem> mMapComposition = null;
	TreeMap<WorldObjectDrawing.WorldObjectDrawingTypes, ArrayList<Integer>> mLoadDrawingSetIndexLookup = null;
	TreeMap<WorldObjectDrawing.WorldObjectDrawingTypes, ArrayList<Integer>> mLoadPOISetIndexLookup = null;
	boolean 						mCancelTileLoadTask = false;
	boolean 						mCancelLoadNewDrawingSetTask = false;
	boolean 						mMapTypeDefinitionNotLoadedCorrectly = false;
	
	Collection<GeoTileDrawing>	mTileIndiciesToKeepDuringGC = null;

	CountDownTimer 				mLoadDrawingTilesTimer = new CountDownTimer(CREATE_DRAWING_SET_MAX_WAIT_TIME_IN_MS, CREATE_DRAWING_SET_MAX_WAIT_TIME_IN_MS) {
		public void onTick(long millisUntilFinished) { }

		public void onFinish() {
			Log.d(TAG, "======== tile load timed out.  forcing draw with partial data =========");

			try { mAccessToWaitingForTilesArray.acquire(); } 	// only allow one thread to touch drawing set load definition at a time
			catch (InterruptedException e) {} {					// TODO handle catch ? how tbd 
				Log.d(TAG, "  - # missing tiles = " + mLoadWaitingForTiles.size());
				for(Integer tileIndex : mLoadWaitingForTiles.keySet()) {
					Log.d(TAG, "  - generating 'no-data' tile for tile #" + tileIndex);
					GeoTile newGeoTile = new GeoTile(tileIndex);
					newGeoTile.MakeTileNoDataAvailable();
					mDrawingObjectCache.AddGeoTile(mContext, newGeoTile, mWorld2DrawingTransformer, new Date(), mRSM.GetTrailLabelCapitalization());
				}
				mLoadWaitingForTiles.clear();	// if time out, then the loading of tiles has taken too long, force draw with what we have
			} mAccessToWaitingForTilesArray.release();

			CreateNewDrawingSet();		
		}

	};
	
// methods
	public Map2DLayer(String map2Dtype, Activity parentActivity, RenderSchemeManager rsm, World2DrawingTransformer world2DrawingTransformer, String mapCompositionID, float preloadMultiplier, BackgroundSize bkgdSize, CustomLayer annotationLayer) throws Exception {
		super(parentActivity, "Map2DLayer", rsm);
		mMapRLDrawingSet = new MapRLDrawingSet(this, rsm, parentActivity, false);	// create DrawingSet and share transformer
		mWorld2DrawingTransformer = world2DrawingTransformer;
		mContext = (Context)parentActivity;
		mAnnotationLayer = annotationLayer;
		
		switch(bkgdSize) {
			case NORMAL: {
				mImageScaleMultiplier = IMAGE_SCALE_MULTIPLIER;
				break;
			}
			case MINIMAL: {
				mImageScaleMultiplier = 1;
				break;
			}
		}
		if(!mMapRLDrawingSet.Init(world2DrawingTransformer, (int)(BACKGROUND_IMAGE_SIZE_BASE * mImageScaleMultiplier), mAnnotationLayer)) {
			throw new Exception("Error generating MapRenderingLayer.  Issue allocating memory for background images.");
		}
		
		
		mMapCompositionID = mapCompositionID;
		mGeoRegionPreloadMultiplier = preloadMultiplier;
		
		loadMapType(map2Dtype);
		if(mMapComposition == null) throw (new Exception("Corrupt map type file: " + map2Dtype));
		
		// after loading map composition, append NoDataZone objects so no data will be drawn on top of all other objects -looks cleaner
		MapCompositionObjectItem newObjectItem = new MapCompositionObjectItem(WorldObjectDrawingTypes.NO_DATA_ZONE, DataSources.RECON_BASE);
    	mMapComposition.add(newObjectItem);
    	ArrayList<Integer> objIndices = new ArrayList<Integer>();
    	objIndices.add(mMapComposition.size()-1);
    	if(newObjectItem.mObjectClass != WO_Class.POI) {
    		mLoadDrawingSetIndexLookup.put(newObjectItem.mObjectType, objIndices);
    	}
		

		SetState(RenderingLayerState.INITIALIZING);
	}
	
	
	public void Release() {
		super.Release();
	}
	
	@Override
	public boolean CheckForUpdates() {
		if(mEnabled) {
//			Log.i(TAG,"Layer State = " + mLayerState);
			boolean newData = mMapRLDrawingSet.SwitchIfUpdateReady();
			if(newData && mLayerState != RenderingLayerState.READY) {
				SetState(RenderingLayerState.READY);
			}
			return newData;
		}
		return false;
	}
	
	@Override
	public void Resume(){
		Log.d(TAG,"firstRunTextureLoad set on Resume!");
		mMapRLDrawingSet.thisIsTheFirstRun = false;
	}

	private void loadMapType(String map2Dtype) {
		// create file path
		String fileName;
		XmlPullParser parser = Xml.newPullParser();
		File file;
		File path = Environment.getExternalStorageDirectory();
		InputStream is;
		BufferedReader br;
		try {
			file = new File(path, MAP_DATA_FOLDER + "/" + map2Dtype);
			br = new BufferedReader(new FileReader(file));

		    // auto-detect the encoding from the stream
		    parser.setInput(br);

		    boolean done = false;
		    int eventType = parser.getEventType();   // get and process event
		    
		    mMapComposition = new ArrayList<MapCompositionItem>();
		    
		    while (eventType != XmlPullParser.END_DOCUMENT && !done){
		        String name = null;
                
//		        name = parser.getName();
//                if(name == null) name = "null";
//		        Log.e(TAG, "eventType:"+eventType + "-"+ name);
//
		        switch (eventType){
		            case XmlPullParser.START_DOCUMENT:
		                name = parser.getName();
		                break;
		                
		            case XmlPullParser.START_TAG:
		                name = parser.getName();
		                if (name.equalsIgnoreCase("map2D")){
	                		String version = parser.getAttributeValue(0);
		                	Log.d(TAG, "loading map composition file: " + map2Dtype + ".xml  ver:" + version);
		                } 
		                
		                if (name.equalsIgnoreCase("object")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes < 2) {
		                		Log.e(TAG, "Bad object definition while parsing object in map composition file " + map2Dtype + ".xml.  Not enough attributes.");
	               				mMapTypeDefinitionNotLoadedCorrectly = true;
		                	}
		                	else {
		                		String obj = parser.getAttributeValue(0);
		                		String sourceStr = parser.getAttributeValue(1);
		                		Capability.DataSources sourceType = null;
		                		if(sourceStr.equalsIgnoreCase("recon_base")) 	{ sourceType = Capability.DataSources.RECON_BASE; };
		                		if(sourceStr.equalsIgnoreCase("recon_snow")) 	{ sourceType = Capability.DataSources.RECON_SNOW; };
		                		if(sourceStr.equalsIgnoreCase("md_ski")) 		{ sourceType = Capability.DataSources.MD_SKI; };
		                		if(sourceType == null) {
			                		Log.e(TAG, "Bad object definition while parsing object in map composition file " + map2Dtype + ".xml.  Unknown data source type.");
		               				mMapTypeDefinitionNotLoadedCorrectly = true;
		                		}

			                	WorldObjectDrawing.WorldObjectDrawingTypes drawingType = WorldObjectDrawing.DrawingTypeForString(obj);
		                		if(drawingType == null) {
			                		Log.e(TAG, "Bad object definition while parsing object in map composition file " + map2Dtype + ".xml.  Unknown object type.");
		               				mMapTypeDefinitionNotLoadedCorrectly = true;
		                		}
		                		else {
		                			MapCompositionObjectItem newObjectItem = new MapCompositionObjectItem(drawingType, sourceType);
		                		
				                	for(int ni=2; ni<numAttributes; ni++) {
				                		// process conditional rendering controls
				                		String nextAttributeName = parser.getAttributeName(ni);
				                		if(nextAttributeName.equalsIgnoreCase("aboveCameraHeightScale")) {
				                			newObjectItem.mDrawingConstraint.SetScaleMin(Float.parseFloat(parser.getAttributeValue(ni)));
				                		}
				                		if(nextAttributeName.equalsIgnoreCase("belowCameraHeightScale")) {
				                			newObjectItem.mDrawingConstraint.SetScaleMax(Float.parseFloat(parser.getAttributeValue(ni)));
				                		}
				                		
				                	}
//				                	Log.d(TAG, "parsing object type: " + obj);
				                	mMapComposition.add(newObjectItem);
		                		} 
		                	}
		                }
		                	
		                if (name.equalsIgnoreCase("name")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes < 1) {
		                		Log.e(TAG, "Bad object definition while parsing name in map composition file " + map2Dtype + ".xml.  Not enough attributes.");
	               				mMapTypeDefinitionNotLoadedCorrectly = true;
		                	}
		                	else {
		                		String obj = parser.getAttributeValue(0);
			                	WorldObjectDrawing.WorldObjectDrawingTypes drawingType = WorldObjectDrawing.DrawingTypeForString(obj);
			                	if(drawingType == null) {
			                		Log.e(TAG, "Bad object definition while parsing name in map composition file " + map2Dtype + ".xml.  Unknown object type.");
		               				mMapTypeDefinitionNotLoadedCorrectly = true;
		                		}
		                		else {
		                			MapCompositionNameItem newObjectItem = new MapCompositionNameItem(drawingType);
			                		
				                	for(int ni=1; ni<numAttributes; ni++) {
				                		// process conditional rendering controls
				                		String nextAttributeName = parser.getAttributeName(ni);
				                		if(nextAttributeName.equalsIgnoreCase("aboveCameraHeightScale")) {
				                			newObjectItem.mDrawingConstraint.SetScaleMin(Float.parseFloat(parser.getAttributeValue(ni)));
				                		}
				                		if(nextAttributeName.equalsIgnoreCase("belowCameraHeightScale")) {
				                			newObjectItem.mDrawingConstraint.SetScaleMax(Float.parseFloat(parser.getAttributeValue(ni)));
				                		}
				                	}

				                	mMapComposition.add(newObjectItem);
		                		} 
		                	}
		                }
		                break;

		            case XmlPullParser.END_TAG:
		                name = parser.getName();
		                break;
		            
		            case XmlPullParser.TEXT:
		                break;
		            }
		        eventType = parser.next();
		        }
		} 
		catch (FileNotFoundException e) {
		    // TODO
			mMapTypeDefinitionNotLoadedCorrectly = true;
			e.printStackTrace();
		} 
		catch (IOException e) {
		    // TODO
			mMapTypeDefinitionNotLoadedCorrectly = true;
			e.printStackTrace();
		} 
		catch (Exception e){
		    // TODO
			mMapTypeDefinitionNotLoadedCorrectly = true;
			e.printStackTrace();

		}
			
		if(mMapTypeDefinitionNotLoadedCorrectly) {
			mMapComposition = null;
			return;
		}
		
		// initialize inverse index lookup tables to use when loading DrawingSet from cache
		mLoadDrawingSetIndexLookup = new TreeMap<WorldObjectDrawing.WorldObjectDrawingTypes, ArrayList<Integer>>();

		mLoadPOISetIndexLookup = new TreeMap<WorldObjectDrawing.WorldObjectDrawingTypes, ArrayList<Integer>>();
		
		mGeodataServiceMapComposition = new ObjectTypeList();
		Integer curIndex = 0;
//		Integer objectIndex = 0;
//		Integer poiIndex = 0;
		for(MapCompositionItem compositionItem : mMapComposition) {
			
			//  add entries to inverse index lookup tables to use when loading DrawingSet from cache
			WorldObjectDrawing.WO_Class objClass = WorldObjectDrawing.ClassForDrawingType(compositionItem.mObjectType);
			boolean newItemType = false;
			if(objClass == WorldObjectDrawing.WO_Class.POI) {
//				Log.d(TAG,"adding POI object index");
				ArrayList<Integer> poiIndices = mLoadPOISetIndexLookup.get(compositionItem.mObjectType);
				if(poiIndices == null) {		// ie new object type
					poiIndices = new ArrayList<Integer>();
					mLoadPOISetIndexLookup.put(compositionItem.mObjectType, poiIndices);
					newItemType = true;
				}
				poiIndices.add(curIndex);
				curIndex++;
			}
			else {
//				Log.d(TAG,"adding other object index");
				ArrayList<Integer> objIndices = mLoadDrawingSetIndexLookup.get(compositionItem.mObjectType);
				if(objIndices == null) {		// ie new object type
					objIndices = new ArrayList<Integer>();
					mLoadDrawingSetIndexLookup.put(compositionItem.mObjectType, objIndices);
					newItemType = true;
				}
				objIndices.add(curIndex);
				curIndex++;
			}
			if(compositionItem instanceof MapCompositionObjectItem){
//				Log.d(TAG, ">>>>>>> " + compositionItem.mObjectType.name() + " is a MapCompositionObjectItem!!");
			}
			if(newItemType && compositionItem instanceof MapCompositionObjectItem) {		// if first time processing object type (don't add labels), add item to mGeodataServiceMapComposition, which is used to load data from the GeodataService
				WorldObjectTypes worldObjType = WorldObjectDrawing.GetCorrespondingWorldObjectType(compositionItem.mObjectType);
				if(worldObjType != null) {
					mGeodataServiceMapComposition.mObjectTypes.add(new SourcedObjectType(worldObjType, ((MapCompositionObjectItem) compositionItem).mSource));

					// TODO validate this list with service state

				}
			}
			
		}
//		ShowMapCompositionItems();
		
	}
	
	public void ShowMapCompositionItems() {	
		for(MapCompositionItem compositionItem : mMapComposition) {
			Log.e(TAG, "Map composition item: " + compositionItem.mObjectType + ", " + compositionItem .mObjectClass);
		}
		for(WorldObjectDrawing.WorldObjectDrawingTypes objectType : mLoadDrawingSetIndexLookup.keySet()) {
			ArrayList<Integer> indices = mLoadDrawingSetIndexLookup.get(objectType);
			int size = indices == null ? 0 : indices.size();
			Log.e(TAG, "LoadDrawingSet " + objectType + ", " + size + " object(s)");
		}
		for(WorldObjectDrawing.WorldObjectDrawingTypes objectType : mLoadPOISetIndexLookup.keySet()) {
			ArrayList<Integer> indices = mLoadPOISetIndexLookup.get(objectType);
			int size = indices == null ? 0 : indices.size();
			Log.e(TAG, "LoadPOISet " + objectType + ", " + size + " object(s)");
		}
		for(SourcedObjectType sourceObjectType : mGeodataServiceMapComposition.mObjectTypes) {
			Log.e(TAG, "GeodataServiceComposition " + sourceObjectType.mWorldObjectType );
		}
	}
	
	public boolean allMissingTiles() {
		return mMapRLDrawingSet.allMissingTiles();
	}
	
	public void ResetDrawingState() {
		mMapRLDrawingSet.ResetDrawingState();
	}

	private void StartLoadNewDrawingSetTask() {
		try { mAccessToDrawingSetLoad.acquire();} 
		catch (InterruptedException e) { }	{ // TODO handle this ? how tbd  
//			Log.d(TAG,"In StartLoadNewDrawingSetTask");
			
//			if(mPreloadTileLoader != null && mPreloadTileLoader.IsRunning()) mPreloadTileLoader.StopLoading();	// stop previous background preloading of tiles if running
			mTileLoader.Clear();
			
			mLoadNewDrawingSetTime = new Date();
			mLoadNewDrawingSetTask = new LoadNewDrawingSetTask();
//			mLoadNewDrawingSetTask = new LoadNewDrawingSetTask(this);
		    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		    	mLoadNewDrawingSetTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);// required to allow parallel execution of Asynch tasks in map layer		    }
		    }
		    else {
		    	mLoadNewDrawingSetTask.execute();
		    }
	
		} mAccessToDrawingSetLoad.release();
//		Log.d(TAG,"Out StartLoadNewDrawingSetTask");
	}
	
	protected class LoadNewDrawingSetTask extends AsyncTask<Void, Void, String> {
//		Map2DLayer mParent = null;
		
//		public LoadNewDrawingSetTask(Map2DLayer parent) {
		public LoadNewDrawingSetTask() {
			mMapRLDrawingSet.ResetLoadParamters();	// reset flags in DS
			mCancelLoadNewDrawingSetTask = false;
//			mParent = parent;
		}

		protected String doInBackground(Void...voids)  {
			
			ArrayList<Integer> 				mRequiredTiles = null;
			ArrayList<Integer> 				mMissingTiles = new ArrayList<Integer>();

			mLoadingNewDrawingSet = true;
			DSChangeResponseCode rc = DSChangeResponseCode.ERROR_DURING_DATA_LOAD; 
			try {
				Log.d(TAG, "starting LoadNewDrawingSetTask - doInBackground - loading new drawing set");
	
				GeoRegion loadingGeoRegion = mMapRLDrawingSet.GetNextGeoRegion().ScaledCopy(mImageScaleMultiplier);		// scale region to match background image scaling
				GeoRegion loadingLowestResolutionGeoRegion = mMapRLDrawingSet.GetNextLowestResolutionGeoRegion().ScaledCopy(mImageScaleMultiplier);	
				float loadingScale = mMapRLDrawingSet.GetNextScale();	
				float loadingHeading = mMapRLDrawingSet.GetNextHeading();	
				
				RectXY gtBB = loadingGeoRegion.mBoundingBox;
				Log.d(TAG,"Loading GR bounding box: " + gtBB.left + ", "+ gtBB.top + ", "+ gtBB.right + ", "+ gtBB.bottom );
				gtBB = loadingLowestResolutionGeoRegion.mBoundingBox;
				Log.d(TAG,"Loading LowRes GR bounding box: " + gtBB.left + ", "+ gtBB.top + ", "+ gtBB.right + ", "+ gtBB.bottom );

				
				mLoadWaitingForTiles.clear();		// stops new drawing set trigger from cache load
				mNumNoDataTilesReturned = 0;
				
				mRequiredTiles = GeoTile.GetTileListForGeoRegion(loadingGeoRegion, null);
				Log.d(TAG, "    LoadingGeoRegion: # tiles found = " + mRequiredTiles.size());
				
				mMissingTiles = mDrawingObjectCache.CheckTiles(mRequiredTiles, mLoadNewDrawingSetTime);
				if(mMissingTiles.size() == 0) {
					CreateNewDrawingSet(loadingGeoRegion, loadingScale, loadingHeading);
					rc = DSChangeResponseCode.DATA_LOADED;
				}
				else {
					for(Integer tileIndex : mMissingTiles) {
						mLoadWaitingForTiles.put(tileIndex, tileIndex);	// needs to be done before mTileLoader.LoadTiles
					}
					
					mTileLoader.LoadTiles(mMissingTiles, 0, mLoadNewDrawingSetTime);	// load tiles with priority 0 
					
					if(mLoadWaitingForTiles.size() > 0) {  // TODO... this may be redundant
						if(mLoadDrawingTilesTimer != null) {
							mLoadDrawingTilesTimer.cancel(); // is this redundant?? or required to reset previous timer 
							mLoadDrawingTilesTimer.start(); // timeout timer to reset things if something goes wrong with tile loading
							mCurrentLoadStartTime = System.currentTimeMillis();
						}
//						int timeoutTime = mLoadWaitingForTiles.size() * CREATE_DRAWING_SET_BASE_WAIT_TIME_PER_TILE_IN_MS;
//						if(timeoutTime < CREATE_DRAWING_SET_MIN_WAIT_TIME_IN_MS) timeoutTime = CREATE_DRAWING_SET_MIN_WAIT_TIME_IN_MS;
//						if(timeoutTime > CREATE_DRAWING_SET_MAX_WAIT_TIME_IN_MS) timeoutTime = CREATE_DRAWING_SET_MAX_WAIT_TIME_IN_MS;
//						Log.d(TAG, "Timeout time = " + timeoutTime);

					}
					 
					// will run CreateNewDS once all tiles in mLoadWaitingForTiles have been loaded
					rc = DSChangeResponseCode.DATA_LOADING;
				}
				Log.d(TAG, "    - number of required tiles " + mRequiredTiles.size());
				
				if(mGeoRegionPreloadMultiplier >= 1.0) {
					// get preload tile indices (in order, closest to region center first) 
					ArrayList<Integer> tileIndicesToPreload = GeoTile.GetTileListForGeoRegion(loadingLowestResolutionGeoRegion.ScaledCopy(mGeoRegionPreloadMultiplier), mRequiredTiles);
	
					Log.d(TAG, "    - number of preload tiles " + tileIndicesToPreload.size());
	
					if(mMaxTilesPostGC == 0) mMaxTilesPostGC = (int)((float)(mRequiredTiles.size() + mRequiredTiles.size()) * 1.2f);
					
					mMissingTiles = mDrawingObjectCache.CheckTiles(tileIndicesToPreload, mLoadNewDrawingSetTime);
					if(mMissingTiles.size() != 0) {	// check cache, return and needed tiles in mMissingTiles
						Log.d(TAG, "    - number of missing preload tiles " + mMissingTiles.size());
						mTileLoader.LoadTiles(mMissingTiles, 1, mLoadNewDrawingSetTime);	// preload surrounding tiles with priority 1
					}
				}
				else {
					Log.d(TAG, "    - preloading of tiles is disabled");
				}
				
			}
			catch (Exception e) {
				Log.e(TAG,"Something wrong during LoadNewDrawingSetTask");
			}
			finally {
				mLoadingNewDrawingSet = false;
			}

			switch(rc) {
			case NO_DATA_AVAILABLE: {
				return "NODATA";
			}
			case DATA_LOADED: {
				return "LOADED";
			}
			case DATA_LOADING: {
				return "LOADING";
			}
			default:
				return "ERROR";
			}
		}
		
		@Override
	    protected void onCancelled(String endString) {	// reached here after cancelled task has completed
			Log.d(TAG, "finished LoadNewDrawingSetTask - cancelled");
			mMapRLDrawingSet.ResetLoadParamters();		// reset flags in DS
			mCancelLoadNewDrawingSetTask = false;
			mLoadNewDrawingSetTask = null;
			SetCameraPosition(mCameraViewport);		// after load cancelled, see if there's another load pending (otherwise code would have to wait for another sensor trigger or user input)
	    }

		protected void onPostExecute(String endString) {
			if(endString != null && endString.length() > 0) {
				if(endString.equalsIgnoreCase("ERROR")) {
					mMapRLDrawingSet.ResetLoadParamters();	
					Log.e(TAG,"ERROR: error during data load (Map2DLayer-LoadDrawingSetTask");
				}
				else {
					if(endString.equalsIgnoreCase("LOADING")) {
						if(mLayerState != RenderingLayerState.READY) {
							SetState(RenderingLayerState.LOADING_DATA);
						}
						Log.d(TAG, "finished LoadNewDrawingSetTask - waiting for required tiles to load");
					}
					else {
						Log.d(TAG, "finished LoadNewDrawingSetTask - load complete");
					}

				}
			}
			else {  // only get here in old ver of Android where system sets String = null on cancel
			}
			
			mLoadNewDrawingSetTask = null;
		}
	}

	@Override	// callback from TileLoader
	public void TileLoaded(GeoTile newGeoTile, TileLoadRequest loadRequest) {
		mDrawingObjectCache.AddGeoTile(mContext, newGeoTile, mWorld2DrawingTransformer, loadRequest.mLoadRequestTimeStamp, mRSM.GetTrailLabelCapitalization());
		
		try { mAccessToWaitingForTilesArray.acquire(); } 	// only allow one thread to touch drawing set load definition at a time
		catch (InterruptedException e) {} {					// TODO handle catch ? how tbd 

			if(loadRequest.mLoadPriority == 0) {
				if(newGeoTile.mNoDataPlaceholder) {
					Log.d(TAG, "missing tile #" + loadRequest.mTileIndex + " - waiting for " + (mLoadWaitingForTiles.size()-1) + " tiles");
				}
				else {
					Log.d(TAG, "loaded tile #" + loadRequest.mTileIndex + " - waiting for " + (mLoadWaitingForTiles.size()-1) + " tiles");
				}
			}
			else {
				if(newGeoTile.mNoDataPlaceholder) {
					Log.d(TAG, "missing preload tile #" + loadRequest.mTileIndex);
				}
				else {
					Log.d(TAG, "preloaded tile #" + loadRequest.mTileIndex);
				}
			}
			if(mLoadWaitingForTiles.get(loadRequest.mTileIndex) != null) {
				if(newGeoTile.mNoDataPlaceholder) {
					mNumNoDataTilesReturned ++;
				}
				mLoadWaitingForTiles.remove(loadRequest.mTileIndex);

				if(mLoadWaitingForTiles.size() == 0) {
					CreateNewDrawingSet();
				}
			}
		} mAccessToWaitingForTilesArray.release();
			
	}

	
	public void CreateNewDrawingSet() {
		
		GeoRegion loadingGeoRegion = mMapRLDrawingSet.GetNextGeoRegion().ScaledCopy(mImageScaleMultiplier);		// scale region to match background image scaling
		float loadingScale = mMapRLDrawingSet.GetNextScale();			// used for retrieving rendering scheme resources... not scaled to match background	
		float loadingHeading = mMapRLDrawingSet.GetNextHeading();	

		CreateNewDrawingSet(loadingGeoRegion, loadingScale, loadingHeading);

	}
	
	public void CreateNewDrawingSet(GeoRegion loadingGeoRegion, float loadingScale, float loadingHeading) {	// note, on call, georegion, scale and heading are pre-scaled to match background image scaling

		if(mLoadDrawingTilesTimer != null) {
			mLoadDrawingTilesTimer.cancel(); // 
			mCurrentLoadStartTime = 0;
		}
		mLoadDrawingTilesTimer = null;
		
		
//		for(MapCompositionItem item : mMapComposition) {
//			Log.e(TAG, "mapcomposition item " + item.mObjectType + ", " + item.mObjectClass );
//		}
//		ShowMapCompositionItems();

		mCreatingNewDrawingSet = true;
		Date now = new Date();
		try {
			Log.d(TAG, "In CreateNewDrawingSet...");
			Date subtaskStartTime = new Date();
			TreeMap<Integer, ArrayList<WorldObjectDrawing>> imageObjectsFromCache = null;
			if(!mCancelLoadNewDrawingSetTask) {
				imageObjectsFromCache = mDrawingObjectCache.GetImageObjectsFromCacheInGeoRegion(loadingGeoRegion, mLoadDrawingSetIndexLookup);		// called after load from geodata service
				now = new Date();
				int numImageObjects=0;
				if(imageObjectsFromCache != null) {
					for(Integer index : imageObjectsFromCache.keySet()) {
						numImageObjects += imageObjectsFromCache.get(index).size();
					}
				}
				else {
					numImageObjects = 0;
				}
				Log.d(TAG, "    C1 - retrieved " + numImageObjects + " image objects   -  time: " + (now.getTime() - subtaskStartTime.getTime()) + "ms");
			}
			
			subtaskStartTime = new Date();
			if(!mCancelLoadNewDrawingSetTask) {
				mMapRLDrawingSet.CreateNextImageFromObjects(imageObjectsFromCache, mMapComposition, loadingGeoRegion, loadingScale, loadingHeading, mWorld2DrawingTransformer, mRSM);
			}
			
			now = new Date();
			Log.d(TAG, "    C2 - draw image   " + (now.getTime() - subtaskStartTime.getTime()) + "ms");
			
			subtaskStartTime = new Date();
			if(!mCancelLoadNewDrawingSetTask) {
				Log.d(TAG, ">>>>>>>>> Adding POI Objects nearby... ");
				mMapRLDrawingSet.AddPOIObjects(mDrawingObjectCache.GetPOIObjectsFromCacheInGeoRegion(mLoadPOISetIndexLookup,loadingGeoRegion), mMapComposition, loadingScale);
			}
			
			if(!mCancelLoadNewDrawingSetTask) {
				mMapRLDrawingSet.mUpdateAvailable = true;
			}
			now = new Date();
//			Log.d(TAG, "    C3 - " + (now.getTime() - subtaskStartTime.getTime()) + "ms to retreive POIs");
		}
		catch (Exception e) {
			if(e != null)
				Log.e("Map2DLayer", "Exception in CreateNewDrawingSet: " + e.getMessage());
			else
				Log.e("Map2DLayer", "Exception in CreateNewDrawingSet: " + "e is null");
			e.printStackTrace();
		}
		finally {
			mCreatingNewDrawingSet = false;
		}
	}

	public boolean IsMissingTiles() {
		return (mNumNoDataTilesReturned > 0);
	}
    	
	private ArrayList<Integer> RemoveTilesFromList(ArrayList<Integer> tileIndiciesToRemove, ArrayList<Integer> tileList) {
		ArrayList<Integer> resultList = new ArrayList<Integer>();
//		Log.d(TAG, "In RemoveTilesFromList: " + tileList.size() + "x" + tileIndiciesToRemove.size() + "=" + tileList.size() * tileIndiciesToRemove.size() );
		for(Integer index : tileList) {
			boolean found = false;
			for(Integer removeIndex : tileIndiciesToRemove) {
				if(index.compareTo(removeIndex) == 0) {
					found=true;
					break;
				}
			}
			if(!found) {
				resultList.add(index);
			}
		}
//		Log.d(TAG, "         result size " + resultList.size() );
		return resultList;
	}
	


	

// focusable api support
	public ArrayList<POIDrawing> GetFocusableItems() {		// retrieve all objects that can be focused, used for x-layer decision making such as find closest focusable item
		// @Overide in subclass
		ArrayList<POIDrawing> resultList = new ArrayList<POIDrawing>();
		if(mEnabled) {
			resultList = mMapRLDrawingSet.GetFocusableObjects();
		}
		return resultList;
	}
	
	@Override
	public void Draw(Canvas canvas, CameraViewport camera, String focusedObjectID, Resources res) {
		// @Overide in subclass
		if(mEnabled) {
			mMapRLDrawingSet.Draw(canvas, camera, focusedObjectID, res);
		}
	}
	
// dynamic reticle support
	@Override
	public ArrayList<ReticleItem> GetReticleItems(CameraViewport camera, float withinDistInM) {  /// assumed to be called after Draw()
		// @Overide in subclass
		if(mEnabled) {
			return mMapRLDrawingSet.GetReticleItems(camera, withinDistInM);
		}
		return null;
	}
	
	@Override
	public void Draw(RenderSchemeManager rsm, Resources res, CameraViewport camera, String focusedObjectID, boolean loadNewTexture, MapViewMode viewMode, GLText glText) {		// for OpenGL based rendering
		// @Overide in subclass
		if(mEnabled) {
			mMapRLDrawingSet.Draw(rsm, res, camera, focusedObjectID, loadNewTexture, viewMode, glText);
		}
	}
	
// dynamic geo data interface routines
	
	@SuppressLint("NewApi")
	@Override
	public void SetGeodataServiceInterface(IGeodataService iGeodataService) throws RemoteException {
		mGeodataServiceInterface = iGeodataService;
		
		if(mGeodataServiceInterface != null) {
			IGeodataServiceResponse rc;
			rc = mGeodataServiceInterface.defineMapComposition(mMapCompositionID, mGeodataServiceMapComposition);
			if(rc.mResponseCode == IGeodataServiceResponse.ResponseCodes.ERROR_WITH_REQUEST) {
				Log.e(TAG, "Error setting Map Composition: " + rc.mErrorMessage);
				throw new RemoteException("Error setting Map Composition: " + rc.mErrorMessage);
			}
			SetState(RenderingLayerState.LOADING_DATA);
		}
		else {
			SetState(RenderingLayerState.WAITING_FOR_DATA_SERVICE);
			mMapRLDrawingSet.ResetAllParamters();
		}
		
		mTileLoader.SetGeodataServiceInterface(iGeodataService, mMapCompositionID);
	}

	@Override
	public boolean SetGeodataServiceState(GeoDataServiceState geoDataServiceState) {
		mGeodataServiceState = geoDataServiceState;
		return ServiceCanSupplyRequiredData(geoDataServiceState);  // service state contains the current service capabilities
	}
	
	boolean ServiceCanSupplyRequiredData(GeoDataServiceState serviceState) {
		
		boolean result = true;

		if(mGeodataServiceMapComposition == null) return false;

		for(SourcedObjectType sourcedObjectType : mGeodataServiceMapComposition.mObjectTypes) {
			boolean found = false;
			for(Capability capability : mGeodataServiceState.mCapabilities) {
				if(capability instanceof StaticMapDataCapability) {
//					Log.e(TAG,"checking capabilities: " + ((StaticMapDataCapability)capability).mWorldObjectType + " - " +sourcedObjectType.mWorldObjectType + "  |  "+
//							((StaticMapDataCapability)capability).mDataSource  + " - "+ sourcedObjectType.mDataSource);
					if(((StaticMapDataCapability)capability).mWorldObjectType == sourcedObjectType.mWorldObjectType && 
					   ((StaticMapDataCapability)capability).mDataSource == sourcedObjectType.mDataSource) {
						found = true;
						break;
					}
				}
			}	
			if(!found) {
				result = false;
				SetState(RenderingLayerState.REQUIRED_DATA_UNAVAILABLE);	//
				Log.e(TAG, "Geodata Service can not provide the data needed by the Map Rendering Layer: missing "+sourcedObjectType.mWorldObjectType + " from "+ sourcedObjectType.mDataSource);
				break;
			}
		}

		return result;
	}

// dynamic camera position interface routines
	public void SetCameraPosition(CameraViewport cameraViewport) {		// ReconMapView camera has changed... do we need to load new data for drawing??
		SetCameraPosition(cameraViewport, false);
	}

	// dynamic camera position interface routines
	public void SetCameraPosition(CameraViewport cameraViewport, boolean forceUpdate) {

//		Log.d(TAG, "SetCameraPosition called! mEnabled = " + String.valueOf(mEnabled));
//		mUserLatitude = (double)latitude;
//		mUserLongitude = (double)longitude;
//		mMapRLDrawingSet.SetUserLocation(latitude, longitude);
		
		// implement logic to see if we need to update the DS?
		// Note, this method is called continuously from ReconMapView.onDraw() as the view is continuously redrawing at a fixed refresh rate

		if(mEnabled) {
			mCameraViewport = cameraViewport;
//			Log.d(TAG, "GeodataService State = " + mGeodataServiceState);
			if(mMapRLDrawingSet != null && cameraViewport != null && mGeodataServiceState != null &&
					(mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_STALE_USER_LOCATION || 
					mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_NO_USER_LOCATION || 
					mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION_LOADING_DATA || 
					mGeodataServiceState.mState == GeoDataServiceState.ServiceStates.SERVICE_READY_WITH_USER_LOCATION)) {
				
				if(mCancelLoadNewDrawingSetTask) {
//					Log.d(TAG, "am cancelling prev load task...");
					return;		// ignore camera movement if in middle of canceling prev load (ie, waiting for LoadDrawingSetTask.onCancelled() to be called)
				}
				
				if(forceUpdate || !mMapRLDrawingSet.IsInitializedWithData()) {		
					if(!mMapRLDrawingSet.IsLoadingData()) {
						cameraViewport.FreezePan();	// lock target to current viewport
//						RectXY gr = cameraViewport.mGeoRegionToSupportTargetViewLowestResolution.ScaledCopy(mGeoRegionPreloadMultiplier).mBoundingBox;
//						Log.d(TAG,"      init region = : " + gr.left + " "+ gr.right + " : "+ gr.top + " "+ gr.bottom);
						mMapRLDrawingSet.DefineNextMapToLoad(cameraViewport.mGeoRegionToSupportTargetView, 
	 							 							 cameraViewport.mGeoRegionToSupportTargetViewLowestResolution, 
	 							 							 cameraViewport.mTargetAltitudeScale, cameraViewport.mTargetRotationAngle, mMapCompositionID);	
//						Log.e(TAG,"Starting map load task");
						StartLoadNewDrawingSetTask();
					}
				}
				else {
					float loadedScale = mMapRLDrawingSet.GetLoadedScale();
					float loadedHeading = mMapRLDrawingSet.GetLoadedHeading();
					GeoRegion preloadGeoRegion = mMapRLDrawingSet.GetLoadedGeoRegion().ScaledCopy(CAMERAVIEWPORT_PRELOAD_GEOREGION_MULTIPLIER);
					
					boolean test1 = !preloadGeoRegion.Contains((float)cameraViewport.mCurrentLongitude, (float)cameraViewport.mCurrentLatitude);
					boolean test2 = loadedScale < 0.97*cameraViewport.mTargetAltitudeScale || loadedScale > 1.1*cameraViewport.mTargetAltitudeScale;
//					Log.d(TAG,"testing cameraviewport: " + test2 + " | "+ loadedScale + " | "+ cameraViewport.mTargetAltitudeScale );
					float testLoadedMapHeading = loadedHeading;
					float testCameraViewportHeading = cameraViewport.mTargetRotationAngle;
					if(testLoadedMapHeading < 180.0f && testCameraViewportHeading > 180.0f) {
						testLoadedMapHeading += 360.0f;
					}
					if(testLoadedMapHeading > 180.0f && testCameraViewportHeading < 180.0f) {
						testCameraViewportHeading += 360.0f;
					}
					boolean test3 = Math.abs(testLoadedMapHeading - testCameraViewportHeading) > ROTATE_RELOAD_THRESHOLD;
					
	//				test3 = false;
					
//					Log.d(TAG,"testing cameraviewport: " + test1 + ", "+ test2 + ", "+ test3 + " | "+ mMapRLDrawingSet.IsLoadingData() + " | "+ mLoadNewDrawingSetTask );
					if(test1 || test2 || test3 ){		// need to load new drawing set
//						Log.d(TAG,"1st test : " + loadedScale + "-" + cameraViewport.mTargetAltitudeScale + " - " + loadedHeading + "-" + cameraViewport.mTargetRotationAngle);
//						RectXY r = preloadGeoRegion.mBoundingBox;
//						Log.d(TAG,"         : " + r.left + " " + cameraViewport.mCurrentLongitude + " "+ r.right + " : "+ r.top + " " + cameraViewport.mCurrentLatitude + " "+ r.bottom);
						
						if(mMapRLDrawingSet.IsLoadingData()) {		// if already loading, check if that data set is suitable.  If not cancel and restart
//							Log.e(TAG, "IsLoadingData == true and loading task " + (mLoadNewDrawingSetTask != null));
							float loadingScale = mMapRLDrawingSet.GetNextScale();
							float loadingHeading = mMapRLDrawingSet.GetNextHeading();
							GeoRegion loadingGeoRegion = mMapRLDrawingSet.GetNextGeoRegion();
	
							boolean ltest1 = !loadingGeoRegion.Contains((float)cameraViewport.mCurrentLongitude, (float)cameraViewport.mCurrentLatitude);
							boolean ltest2 = loadingScale < 0.97*cameraViewport.mTargetAltitudeScale || loadingScale > 1.1*cameraViewport.mTargetAltitudeScale;
							testLoadedMapHeading = loadingHeading;
							testCameraViewportHeading = cameraViewport.mTargetRotationAngle;
							if(testLoadedMapHeading < 180.0f && testCameraViewportHeading > 180.0f) {
								testLoadedMapHeading += 360.0f;
							}
							if(testLoadedMapHeading > 180.0f && testCameraViewportHeading < 180.0f) {
								testCameraViewportHeading += 360.0f;
							}
							boolean ltest3 = Math.abs(testLoadedMapHeading - testCameraViewportHeading) > ROTATE_RELOAD_THRESHOLD;
//							ltest3 = false;
							
							if(ltest1 || ltest2 || ltest3 ){
//								Log.d(TAG,"1st test : " + loadedScale + "-" + cameraViewport.mTargetScale + " - " + loadedHeading + "-" + cameraViewport.mTargetRotationAngle);
//								RectXY r = loadedGeoRegion.mBoundingBox;
//								Log.d(TAG,"         : " + r.left + " " + cameraViewport.mCurrentLongitude + " "+ r.right + " : "+ r.top + " " + cameraViewport.mCurrentLatitude + " "+ r.bottom);
//								Log.d(TAG,"2nd test	: " + ltest1 + ", "+ ltest2 + ", "+ ltest3);
//								Log.d(TAG,"	cancel prev load: " + test1 + ", "+ test2 + ", "+ test3);
								if(mLoadNewDrawingSetTask != null) {		// if BG task still running
									mLoadNewDrawingSetTask.cancel(true); 	// mark BG task as cancelled 
									mCancelLoadNewDrawingSetTask = true;	// this line allows cancelled tasks to finish quicker
									Log.d(TAG,"cancelling previous load task");
								}	
								
								mLoadWaitingForTiles.clear();			// stops incoming tiles from data service from triggering DS load/creation
								
								mMapRLDrawingSet.CancelLoad(true);		// mark dataset as not loading...
								
//								Log.e(TAG, "after cancel - IsLoadingData == " + mMapRLDrawingSet.IsLoadingData());
							}
							else {
		//						Log.d(TAG, "already loading suitable drawing set.. do nothing");
							}
						}
						else {
//							Log.d(TAG,"	 loading new drawing set: " + test1 + ", "+ test2 + ", "+ test3);
							if(mLoadNewDrawingSetTask == null) {	// all previous task have been completed or cancelled
								Log.d(TAG,"spawning new load task");
								cameraViewport.FreezePan();	// lock target to current viewport
								mMapRLDrawingSet.DefineNextMapToLoad(cameraViewport.mGeoRegionToSupportTargetView, 
			 							 							 cameraViewport.mGeoRegionToSupportTargetViewLowestResolution, 
			 							 							 cameraViewport.mTargetAltitudeScale, cameraViewport.mTargetRotationAngle, mMapCompositionID);	
								StartLoadNewDrawingSetTask();
	
							}
						}
					}
					else {
						// nothing to do, current drawing set is suitable for viewport
					}
				}
			}
		}

	}

	@Override
	public void SetCameraHeading(float heading) {
//		mMapRLDrawingSet.SetUserHeading(heading);  // ?? now stored in cameraviewport?
	}
	
	@Override
	public void SetCameraPitch(float pitch){
		
	}
	
	public void setThirdPersonView(boolean setThirdPersonView){
		mMapRLDrawingSet.setThirdPersonView(setThirdPersonView);
	}

	@Override
	public void DoGarbageCollection(boolean mapCameraTrackingUser) {
		Log.d(TAG,"Garbage collection of Map layer!");
		boolean doGC = false;
		if(mLastCGDate == null ) {
			doGC = true;
		}
		else {
			long nowInMS = (new Date()).getTime();
			if((nowInMS - mLastCGDate.getTime()) > GC_TIME_IN_MS) {
				doGC = true;
			}
		}
		if(doGC && !mGCInProgress) {
			mGCInProgress = true;
			mLastCGDate = new Date();
			mDrawingObjectCache.CollectGarbage(mMaxTilesPostGC);
//			mDrawingObjectCache.CollectGarbage((float)mUserLongitude, (float)mUserLatitude, mMaxTilesPostGC);
			mGCInProgress = false;
		}
	}

	public Bitmap getBackgroundImage() {
		return mMapRLDrawingSet.GetImage();
	}
	
	public int getImageScaleMult(){
		return IMAGE_SCALE_MULTIPLIER;
	}
	
	public PointXY getBackgroundImageCenterDrawingCoords(){
		return mMapRLDrawingSet.mBackgroundImageCenterInDrawingCoords;
	}
	
	public float[] getPanOffset(){
		return mMapRLDrawingSet.getPanOffset();
	}
	
	/**
	 * 
	 * @return A reference to the MapRLDrawingSet object
	 */
	public MapRLDrawingSet getMapDrawingSet(){
		return mMapRLDrawingSet;
	}
}
