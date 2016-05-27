package com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream.PutField;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.reconinstruments.geodataservice.TileLoadingFromNetworkException;
import com.reconinstruments.geodataservice.datasourcemanager.DataSource;
import com.reconinstruments.geodataservice.datasourcemanager.StaticMapDataTranscoder;
import com.reconinstruments.geodataservice.datasourcemanager.SuspendObject;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.AssetDB;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.ZippedRecordDAL;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp.ShpContent;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp.ShpRecord;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp.ShpTools;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.ReconOSBlockingHttpClient;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.datarecords.ReconBaseAreaRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.datarecords.ReconBasePOIRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.datarecords.ReconBaseTrailRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow.datarecords.ReconSnowAreaRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow.datarecords.ReconSnowDataRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow.datarecords.ReconSnowPOIRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow.datarecords.ReconSnowTrailRecord;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoTile;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;
import com.reconinstruments.webapi.ReconHttpRequest;
import com.reconinstruments.webapi.ReconHttpResponse;



public class ReconSnowDataSource extends DataSource		// Mountain Dynamics data source - based mostly around old listOfResortsDAL.java
{
// constants 
	private final static String TAG = "ReconSnowDataSource";

	private static final boolean LOAD_FROM_NETWORK	= false;		// all snow tiles are currently preloaded...
	
	private static final boolean CLEAR_PRELOADED_TILES = false;		// debug flag to delete stored files at startup
	private static final boolean CLEAR_DOWNLOADED_TILES = false;	// debug flag to delete previously downloaded files at startup
	private static final String AWS_GEOTILE_FORMAT_VER_STR = "1";
	private static final long NETWORK_LOAD_FAIL_RETRY_TIME	= 300000;	// 5 minutes between retry loading of tile that failed 
	private static final float LOADEDDATABB_SIZE_IN_KMS	= 30.f;
	private static final float PRELOADBB_FACTOR	= 2.f/3.f;
    static final int RETRY_INTERVAL =  60000; // retry interval
    static final int FETCH_TIME_OUT =  5000; // 10 seconds
	
	private final Semaphore mControlOfCache = new Semaphore(1, true);
	private final Semaphore mAccessToZipFiles = new Semaphore(1, true);
	
	public enum DownloadedTileFileState {
		READING_FILE,
		CREATING_FILE,
		WRITING_FILE
	}
	
	public enum BaseDataTypes {  
		AREA_LAND,     // must match type definitions used in Dev_and_Test_Apps/tileCompress/src/TileCompressor.java as this is what is used to create data
		AREA_OCEAN,
		AREA_CITYTOWN,
		AREA_WOODS,
		AREA_WATER,
		AREA_FIELD,
		AREA_PARK,
		HIGHWAY_MOTORWAY,	// OSM uses highway as British term for transport path (Road, bike path) and Motorway is Freeway
		HIGHWAY_PRIMARY,	// main city through road
		HIGHWAY_SECONDARY,	// main city road
		HIGHWAY_TERTIARY,	// lesser main city road
		HIGHWAY_RESIDENTIAL,	// residential road
		POI_RESTAURANT, 	//Type from MD
		POI_STORE,
		POI_HOSPITAL,
		POI_WASHROOM,
		POI_DRINKINGWATER,
		LINE_NATIONAL_BORDER,
		
		POI_PARKING, 		//Type from MD
		POI_INFORMATION, 	//Type from MD
		
		LINE_SKIRUN_GREEN,	//Type from MD
		LINE_SKIRUN_BLUE,
		LINE_SKIRUN_BLACK,
		LINE_SKIRUN_DBLACK,
		LINE_SKIRUN_RED,
		LINE_CHAIRLIFT,
		LINE_ROADWAY,
		LINE_WALKWAY,       //Type from MD
		AREA_SKIRESORT
	}


	
// members 
	AssetDB 							mResortInfoDB = null;
	
	RectXY								mTestForPreloadBB = null;
	RectXY								mLoadedDataBB = null;
	ArrayList<ReconSnowDataRecord>			mCachedDataRecords = new ArrayList<ReconSnowDataRecord>();
	ArrayList<ReconSnowDataRecord>			mBackgroundCache = new ArrayList<ReconSnowDataRecord>();
	ArrayList<String>					mPreloadedResortNames = new ArrayList<String>();
	protected static ZippedRecordDAL	mZippedRecordDAL = null;
	protected Context					mContext = null;
	AsyncTask<Void, Void, String> 		mPreloadDataIntoCacheTask = null;
	boolean								mPrefetchingData = false;
	PointXY 							mPreloadedUserLocation = null;	// in gps coords
	boolean								mResortsFileLoaded = false;
	boolean   							mRunTest = false;
	TreeMap<Integer, ArrayList<ReconSnowDataRecord>> mTileCache = new TreeMap<Integer, ArrayList<ReconSnowDataRecord>>();;
	TreeMap<Integer, ArrayList<ReconSnowDataRecord>> mRevisedTileCache = new TreeMap<Integer, ArrayList<ReconSnowDataRecord>>();;
	TreeMap<Integer, Integer> mTilesLoadingInCache = new TreeMap<Integer, Integer>();;
	TreeMap<Integer, DownloadedTileFileState> mDownloadedTileFileState = new TreeMap<Integer, DownloadedTileFileState>();	// used to avoid file read/write collisions

//    private ReconOSHttpClient			mHttpClient = null;
    private ReconOSBlockingHttpClient	mBlockingHttpClient = null;
    ConnectivityManager 				mConnectivityManager;
	Date								mQueueStartLoadTime;
	int									mNumTilesToLoadFromNet;
//	int									mCounterTilesToLoadToCache = 0;
	int									mDesiredCacheSize = 0;
//	int									mCurrentNumCacheElements = 0;
	boolean								mSwallowFirstLocation=true; 		
	
	private boolean 					mKeepRetrying = false;
    Map<String, List<String>> 			mWebRequestHeader = new HashMap<String, List<String>>();
    Map<Integer, SuspendObject> 		mSuspendedThreads = new HashMap<Integer, SuspendObject>();
    Map<Integer, URL>					mTilesLoadingFromNetwork = new HashMap<Integer, URL>();
    Map<Integer, Date>					mTilesPreviouslyFailedNetworkLoad = new HashMap<Integer, Date>();
	
// methods
	ReconSnowDataSource(StaticMapDataTranscoder transcoder, Context context, DevTestingState _devTestingState) throws Exception {
		super(transcoder, context, _devTestingState);
		mContext = context;

		
		if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
		     mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.OSM_SOURCE_CREATE_ERROR.ordinal()) == 1) ) {
			throw new Exception ("Forced OSMSOURCE creation error.");
		}
		
//		mHttpClient = new ReconOSHttpClient(mContext, mWebRequestCallback);
		mBlockingHttpClient = new ReconOSBlockingHttpClient(mContext);


		// make sure tile folders on disk exist...
	    String preloadedTileFolder = GetPreloadedTileFolder();
	    File folder = new File(preloadedTileFolder);
	    if(!folder.exists()) {
	    	folder.mkdirs();
	    }
	    String downloadedTileFolder = GetDownloadedTileFolder();
	    Log.d(TAG, "Downloaded files are stored at " + downloadedTileFolder);
	    folder = new File(downloadedTileFolder);
	    if(!folder.exists()) {
	    	folder.mkdirs();
	    }
	    
	    if(CLEAR_PRELOADED_TILES) {  // to remove preloaded files during testing
			File dir = new File(preloadedTileFolder);
			for (File child : dir.listFiles()) {
				child.delete();
			}
		}
	    if(CLEAR_DOWNLOADED_TILES) {  // to test network download
			File dir = new File(downloadedTileFolder);
			for (File child : dir.listFiles()) {
				child.delete();
			}
		}

	    mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

	    // TODO check downloadedTileFolder and empty oldest to keep usage below size limit

	}
	
	@Override
	public String init() {
		try {
			// do everything needed to get data source ready
			
			String errorMsg= null;
			if((mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
				 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.OSM_SOURCE_CREATE_ERROR.ordinal()) == 1) ) 	{
				errorMsg = "Forced OSM Data source init error";
				Log.e(TAG, errorMsg);
				return errorMsg;
			}
			return errorMsg;
		}		
		catch (Exception e) {
			String errorMsg = "OSMDataSource initialization error: " + e.toString();
			Log.e(TAG, errorMsg);
			return errorMsg;
		}
	}
	
	@Override
	public ArrayList<Integer> GetCachedDataStats() throws Exception {
		ArrayList<Integer> result = new ArrayList<Integer>();
		result.add(mDesiredCacheSize);
		result.add(mDesiredCacheSize - mTilesLoadingInCache.size());
//		Log.e(TAG,"SNOWDATA - getcachedDataStats: " + mDesiredCacheSize + ", "+ mTilesLoadingInCache.size());
		return result;
	}

	@Override
	public void SetUserLocation(float longitude, float latitude)  throws Exception {

		if(mSwallowFirstLocation) {
			mSwallowFirstLocation=false;		// used to avoid conflicts between FakeGPS and real at startup
			return;
		}
		boolean prefetchData = false;
				
		if(mTestForPreloadBB == null) {		// if data not already preloaded, preload
			prefetchData = true;
		}
		else {	
			if(!mTestForPreloadBB.Contains(longitude, latitude)) {	// if new point outside of preloadBB 
//				Log.e(TAG, "preload needed for " + longitude + ": " + mTestForPreloadBB.left +", "+ mTestForPreloadBB.right +" || "+ latitude + ": "+ mTestForPreloadBB.top +", "+ mTestForPreloadBB.bottom);
				prefetchData = true;
			}
		}

		if(prefetchData && !mPrefetchingData && mTilesLoadingInCache.size() == 0) {
			mPrefetchingData = true;	// can be interrupted by new location data... block so all requests are made before new request arrives
			GeoRegion tileGeoRegionContainingUser = GeoTile.GetGeoRegionFromTileIndex(GeoTile.GetTileIndex(longitude, latitude));
			mTestForPreloadBB = tileGeoRegionContainingUser.mBoundingBox;	   		// set preload region so when user leaves boundary of current tile, prefetch is done again 
			mTestForPreloadBB.top += 0.002;	// add some hysteresis to test boundary so locations on tile boudaries don't thrash reloading the cache
			mTestForPreloadBB.right += 0.002;
			mTestForPreloadBB.left -= 0.002;
			mTestForPreloadBB.bottom -= 0.002;
			
			GeoRegion preloadRegion = tileGeoRegionContainingUser.ScaledCopy(6);	// 6x6 tiles - roughly 33x33km   
			// get tile list
			ArrayList<Integer> tileList = GeoTile.GetTileListForGeoRegion(preloadRegion, null);
			ArrayList<ReconSnowDataRecord> tileRecords = null;
			
			mRevisedTileCache.clear();
			mQueueStartLoadTime = new Date();
//			mNumTilesToLoadFromNet = 0;
			mDesiredCacheSize = tileList.size();
			mTilesLoadingInCache.clear();
			for(Integer tileIndex : tileList) {		// copy over existing tiles
				if((tileRecords = mTileCache.get(tileIndex)) != null) {
					mRevisedTileCache.put(tileIndex, tileRecords);
//					mCurrentNumCacheElements++;
				}
			}
			mTileCache = mRevisedTileCache; 		// auto cleans cache of old tiles ... no need for more advanced GC
//			Log.d(TAG, "SNOW DATA: Updated tile cache with " + mRevisedTileCache.keySet().size() +" tiles");
			
			ArrayList<Integer> iterator = new ArrayList<Integer>();
			for(Integer tileIndex : tileList) {		// first create tracking list BEFORE starting any threads, as threads change tracking list and can screw it up
				if((tileRecords = mTileCache.get(tileIndex)) == null) {
					mTilesLoadingInCache.put(tileIndex, tileIndex);
					iterator.add(tileIndex);
//					mNumTilesToLoadFromNet++;;
				}
			}
			for(Integer tileIndex : iterator) {		// load missing tiles by spawning load thread for each required tile - simplifies network loading logic
				new Thread(new PrefetchTileToCache(tileIndex, mContext)).start();
			}
			//			mCounterTilesToLoadToCache = mNumTilesToLoadFromNet;	// store max
			
			mPrefetchingData = false;

		}
	}
	
	private class PrefetchTileToCache implements Runnable {

		int mTileIndex = 0;
		Context	mParentContext;
		
		public PrefetchTileToCache(int tileIndex, Context context) {
			mTileIndex = tileIndex;
			mParentContext = context;
//	        Log.d(TAG, "SNOW DATA: creating cache load thread for tile#" + mTileIndex);
		}
		@Override
		public void run() {
			
			ArrayList<ReconSnowDataRecord> tileRecords = null;
			try {
				tileRecords = loadTileFromDiskOrNetwork(mParentContext, mTileIndex, null, true);
//		        Log.e(TAG, "finished tile#" + mTileIndex + "  load# " + mTilesLoadingInCache.get(mTileIndex) + "  num left = " + mTilesLoadingInCache.size());
			} 
			catch (Exception e) {  // can happen a lot if not in region with snow tiles
				mTilesLoadingInCache.remove(mTileIndex);
//		        Log.e(TAG, "SNOW DATA: ignoring failed cache load for tile#" + mTileIndex + " : load# " + mTilesLoadingInCache.get(mTileIndex) + "  num left = " + mTilesLoadingInCache.size());
			}  
		} 
	}

	private void ReportException(String errorString) throws Exception {
		throw new Exception(errorString);
	}

	public float CalcDistanceInKmBetween(PointXY point1, PointXY point2) {
		float meridionalCircumference = 40007860.0f; // m  - taken from wikipedia/Earth
		float equitorialCircumference = 40075017.0f; // m  - taken from wikipedia/Earth

		float hAngleDiff = Math.abs(point1.x - point2.x);
		if(hAngleDiff > 180.f) hAngleDiff = 360.f - hAngleDiff;	
		
		float hDistInKm = (hAngleDiff / 360.f * (float)(equitorialCircumference*Math.cos(Math.toRadians((point1.y + point2.y)/2.f))))/1000.f; 
		float vDistInKm = ((point1.y - point2.y) / 360.f * meridionalCircumference)/1000.f; 
		return (float) Math.sqrt(hDistInKm*hDistInKm + vDistInKm*vDistInKm) ; 
	}
	
	public float CalcLatAngle(float distance, float latitude, float longitude) {
		float meridionalCircumference = 40007860.0f; // m  - taken from wikipedia/Earth

		return Math.abs(distance*1000.f / meridionalCircumference * 360.f); 
	}

	public float CalcLongAngle(float distance, float latitude, float longitude) { 
		float equitorialCircumference = 40075017.0f; // m  - taken from wikipedia/Earth

		return Math.abs(distance*1000.f / (float)(equitorialCircumference*Math.cos(Math.toRadians(latitude))) * 360.f); 
	}

	

	public String GetPreloadedTileFolder() {
		return Environment.getExternalStorageDirectory().getPath() + "/ReconApps/GeodataService/PreloadedOSMTiles/Snow/";
	}
	
	public String GetDownloadedTileFolder() {
		return Environment.getExternalStorageDirectory().getPath() + "/ReconApps/GeodataService/DownloadedOSMTiles/Snow/";
	}

	public String GetCachedTileFolder() {
		return mContext.getCacheDir() + "/CachedOSMTiles/";
	}
	
	private ArrayList<ReconSnowDataRecord> loadTileFromDiskOrNetwork(Context mContext, int requestedTileIndex, TreeMap<BaseDataTypes, WorldObject.WorldObjectTypes> inDataQuery, boolean putInCache) throws Exception {
		// check downloaded storage
		ArrayList<ReconSnowDataRecord> loadedDataRecords = new ArrayList<ReconSnowDataRecord>();	
		String extension = ".rgz";
//		if(!USE_COMPRESSED_FILES) {
//			extension = ".xml";
//		}

		File downloadFilePath = new File(GetDownloadedTileFolder(), String.format("%09d", requestedTileIndex) + extension); 

		if(downloadFilePath.exists()) {
			Date startTime = new Date();
			loadedDataRecords = loadTile(downloadFilePath, requestedTileIndex, inDataQuery);
			Date endTime = new Date();
			Log.d(TAG, "SNOW DATA: Downloaded tile# "+ requestedTileIndex + " load time: " +(endTime.getTime() - startTime.getTime()) + " for " + loadedDataRecords.size() + " objects" + "  num left = " + mTilesLoadingInCache.size());
			if(putInCache) {
				if(loadedDataRecords != null) {
					mTileCache.put(requestedTileIndex, loadedDataRecords);  // add retrieved records to cache
				}
				mTilesLoadingInCache.remove(requestedTileIndex);
			}
			return loadedDataRecords;
		}
		else {
			// check preloaded storage
			File preloadFilePath = new File(GetPreloadedTileFolder(), String.format("%09d", requestedTileIndex) + extension); 

			if(preloadFilePath.exists()) {
				Date startTime = new Date();
				loadedDataRecords = loadTile(preloadFilePath, requestedTileIndex, inDataQuery);
				Date endTime = new Date();
				Log.d(TAG, "SNOW DATA: Preloaded tile# "+ requestedTileIndex + " load time: " + (endTime.getTime() - startTime.getTime()) + " for " + loadedDataRecords.size() + " objects" + "  num left = " + mTilesLoadingInCache.size());
				if(putInCache) {
					if(loadedDataRecords != null) {
						mTileCache.put(requestedTileIndex, loadedDataRecords);  // add retrieved records to cache
					}
					mTilesLoadingInCache.remove(requestedTileIndex);
				}
				return loadedDataRecords;

			}
			else {
				// get tile from network server onto disk, return with TileLoadingFromNetworkException and let client retry later (improves efficiency)
				if(!LOAD_FROM_NETWORK) {
//					Log.d(TAG,"SNOW DATA: Network load for tile#"+ requestedTileIndex + " blocked in code");
					if(putInCache) 	{
						mTilesLoadingInCache.remove(requestedTileIndex);
					}
					throw new Exception("Network load for tile#"+ requestedTileIndex +" blocked in code");

				}

				// if no network, fail
				if(!mPhoneNetworkIsAvailable) {
					if(putInCache) 	{
						mTilesLoadingInCache.remove(requestedTileIndex);
					}
					throw new Exception("SNOW DATA: Network load for tile#"+ requestedTileIndex +" failed.  Network unavailable");
				}
				
				// if previously failed, fail unless NETWORK_LOAD_FAIL_RETRY_TIME ms have passed
				Date lastFailTime = mTilesPreviouslyFailedNetworkLoad.get(requestedTileIndex);
				if(lastFailTime != null) {
					long now = (new Date()).getTime();
					if( lastFailTime.after(new Date(now - NETWORK_LOAD_FAIL_RETRY_TIME) )) {
						if(putInCache) 	{
							mTilesLoadingInCache.remove(requestedTileIndex);
						}
						throw new Exception("SNOW DATA: Network load for tile#"+ requestedTileIndex +" failed.  Load previously failed in network assumed unavilable. Will allow another try after " + (int)(now - NETWORK_LOAD_FAIL_RETRY_TIME) + "ms.");
					}
					mTilesPreviouslyFailedNetworkLoad.remove(requestedTileIndex);	// retry time expired, try again
				}

				// if not already loading from network (due to previous call), start thread to load from network
				if(mTilesLoadingFromNetwork.get(requestedTileIndex) == null) {
					// start new load thread
					LoadTileFromNetworkThread newNetworkLoadThread = new LoadTileFromNetworkThread();
					newNetworkLoadThread.mLoadTileIndex = requestedTileIndex;
					newNetworkLoadThread.mPutInCache = putInCache;
					newNetworkLoadThread.mInDataQuery = inDataQuery;
					newNetworkLoadThread.mFilePath = downloadFilePath;
					newNetworkLoadThread.mURL = new URL("https://s3.amazonaws.com/geotilebucket/"+ AWS_GEOTILE_FORMAT_VER_STR +"/snow/" + String.format("%09d", requestedTileIndex) + extension);  // open requests to anyone... doesn't need security
					mTilesLoadingFromNetwork.put(requestedTileIndex, newNetworkLoadThread.mURL);
					newNetworkLoadThread.start();
				}

				// use special exception to "fail" (for now) - this is interpreted by DSM as try again later
				throw new TileLoadingFromNetworkException("tile#"+ requestedTileIndex);		
			}
		}
	}
	
	public class LoadTileFromNetworkThread extends Thread {		// blocks with HTTP until data or failure, data gets written to disk
		public int 		mLoadTileIndex;
		public URL		mURL;
		public String   mExtension = ".xml";
		public boolean  mPutInCache;
		public File 	mFilePath;
		ReconHttpRequest mTileRequest;
		ReconHttpResponse mHttpResponse;
		TreeMap<BaseDataTypes,WorldObject.WorldObjectTypes> mInDataQuery;
		
	    @Override
	    public void run() {
	    	Log.d(TAG, "SNOW DATA: LoadTileFromNetworkThread - loading tile " + mLoadTileIndex + " from network...");
	    	Date startTime = new Date();

	    	mHttpResponse = null;
	    	try {
	    		// built data request
	    		Map<String, List<String>> 	mWebRequestHeader = new HashMap<String, List<String>>();
	    		//		mWebRequestHeader.put("Content-Type", Arrays.asList(new String[] { "text/xml" }));
	    		mTileRequest = new ReconHttpRequest("GET", mURL, FETCH_TIME_OUT, mWebRequestHeader, null);
	    		Log.d(TAG,"SNOW DATA: Fetch tile request built and sent for URL:" + mURL.toExternalForm());

	    		// send request to tile server and wait for response (assumes this routine is called from non-UI thread
	    		mHttpResponse = mBlockingHttpClient.sendBlockingRequest(mTileRequest);
	    	}
	    	catch(Exception e) {
	    		mHttpResponse = null;
	    	}
	    	
	    	if (mHttpResponse == null ) {  // error case
	    		//mBlockingHttpClient.GetErrorType();  // see Android_MobileSDK/WebAPI for IReconHTTPCallback.java
	    		// case: network error , ie, something in network is screwing up
	    		if(!mPutInCache) mTilesPreviouslyFailedNetworkLoad.put(mLoadTileIndex, new Date());	
    			Log.e(TAG,"SNOW DATA: Network load for tile#"+ mLoadTileIndex +" failed in HTTP client with error type " + mBlockingHttpClient.GetErrorType() + " on GET request: " + mURL);
	    		//			throw new Exception("Network error during load of tile#"+ mLoadTileIndex + ". Tile load abandoned");
	    	}
	    	else {
	    		if(mHttpResponse.getResponseCode() == 200) {	// successful data retrieval
		    		// save response to file then read contents -- TODO in future, explore way of loading from memory contents instead of file - faster load!!
	    			String extension = ".rgz";
//	    			if(!USE_COMPRESSED_FILES) {
//	    				extension = ".xml";
//	    			}
//		    		File filePath = new File(GetDownloadedTileFolder(), mLoadTileIndex + extension); 
		    		//		filePath = new File(GetPreloadedTileFolder(), requestedTileIndex+".xml"); 
		    		if(writeTileToDisk(mFilePath, mHttpResponse.getBody())) {
		    			if(!mFilePath.exists()) {		// may be redundant...
		    				// case: uncaught FileOutputStream.write error -  for some reason data not found on disk after writing
		    				if(!mPutInCache) mTilesPreviouslyFailedNetworkLoad.put(mLoadTileIndex, new Date());	
		    				Log.e(TAG,"SNOW DATA: Network load for tile#"+ mLoadTileIndex +" failed to write data from disk (uncaught FileOutputStream.write error), file " + mFilePath);
		    				//					throw new Exception("Network load for tile#"+ mLoadTileIndex +" failed - file is corrupt, file " + filePath);
		    			}
		    			else {
		    				long timeDiff = (new Date()).getTime() - startTime.getTime();
		    		    	Log.d(TAG,"SNOW DATA: Fetch tile request succeeded for URL:" + mURL.toExternalForm() + " in " + timeDiff/1000.f + "sec (concurrent with other tasks)" );
				    		if(mPutInCache) {
				    			ArrayList<ReconSnowDataRecord> loadedDataRecords;
								try {
									loadedDataRecords = loadTile(mFilePath, mLoadTileIndex, mInDataQuery);
								} 
								catch (Exception e) {
									loadedDataRecords = null;
								}
				    			if(loadedDataRecords != null) {
				    				mTileCache.put(mLoadTileIndex, loadedDataRecords);  // add retrieved records to cache
				    			}
				    		}
		    			}
		    		}
		    		else {
		    			// case: failed to write data to disk
		    			if(!mPutInCache) mTilesPreviouslyFailedNetworkLoad.put(mLoadTileIndex, new Date());	
		    			Log.e(TAG,"SNOW DATA: Network load for tile#"+ mLoadTileIndex +" failed to write data to disk, file " + mFilePath);
		    			//				throw new Exception("Network load for tile#"+ mLoadTileIndex +" failed to write data to disk, file " + filePath);
		    		}
		    	}
		    	else {
		    		// case: URL or service is not available
		    		if(!mPutInCache) mTilesPreviouslyFailedNetworkLoad.put(mLoadTileIndex, new Date());	
		    		Log.e(TAG,"SNOW DATA: Network load for tile#"+ mLoadTileIndex +" failed. Response code:"+mHttpResponse.getResponseCode());
		    		//			throw new Exception("Network load for tile#"+ mLoadTileIndex +" failed. Response code:"+httpResponse.getResponseCode());
 		    	}
	    	}
	    	if(mPutInCache) {
	    		mTilesLoadingInCache.remove(mLoadTileIndex);   // record cache load as done for this tile
	    	}

	    	if(mTilesLoadingInCache.size() == 0) {
				long timeDiff = (new Date()).getTime() - mQueueStartLoadTime.getTime();
		    	Log.d(TAG,"SNOW DATA: Total preload of " + mNumTilesToLoadFromNet + " tiles in " + timeDiff/1000.f + "sec, Average " + (mNumTilesToLoadFromNet/(timeDiff/1000.f)));
	    	}
	    	else {
	    		Log.d(TAG,"SNOW DATA: == tile countdown: "+ mTilesLoadingInCache.size());
	    	}
	    	if(mTilesLoadingFromNetwork.get(mLoadTileIndex) != null) {
	    		mTilesLoadingFromNetwork.remove(mLoadTileIndex);
	    	}

	    }
	    
		private boolean writeTileToDisk(File filePath, byte[] fileContents) {
			try {
				FileOutputStream fos = new FileOutputStream(filePath);
			    fos.write(fileContents);
			    fos.close();
			    return true;
			} 
			catch(FileNotFoundException ex) {
			    Log.e(TAG,"FileNotFoundException : " + ex);
			    return false;
			} 
			catch(IOException ioe) {
			    Log.e(TAG,"IOException : " + ioe);
			    return false;
			}
		}
	    
	}
	


	
	//=============================================================================================
	
	public ArrayList<ReconSnowDataRecord> SourceDataWithQuery(ArrayList<ReconSnowQueryRecord> dataQueryRecords, int requestedTileIndex) throws Exception {
		// must be thread safe  

		ArrayList<ReconSnowDataRecord> loadedDataRecords = new ArrayList<ReconSnowDataRecord>();	// re=init
		TreeMap<BaseDataTypes, WorldObject.WorldObjectTypes> inDataQuery = new TreeMap<BaseDataTypes, WorldObject.WorldObjectTypes>();
		for(ReconSnowQueryRecord queryRecord : dataQueryRecords) {
			inDataQuery.put(queryRecord.mDataType, queryRecord.mServiceObjectType);		// the query data types are built into a map to simplify query data type lookup during processing loop
		}

		// look for tile file in TileCache (internal disk), Preloaded Tiles (external mem) and on network service (in that order).  If not found, throw an exception
		ArrayList<ReconSnowDataRecord> allRecordsInCachedTile = null;	

		allRecordsInCachedTile = mTileCache.get(requestedTileIndex);		
		if(allRecordsInCachedTile != null) {	// tile records in cache
			for(ReconSnowDataRecord record : allRecordsInCachedTile) {
				if(inDataQuery.get(record.mDataType) != null) {		// only return records matching types defined in query
					loadedDataRecords.add(record);
				}
			}
			Log.d(TAG, "SNOW DATA: Loaded " + loadedDataRecords.size() + " records for tile " + requestedTileIndex + " from  tile cache");
		}
		else {
			try {
				loadedDataRecords = loadTileFromDiskOrNetwork(mContext, requestedTileIndex, inDataQuery, false);
			}
			catch (IOException e) {
				throw e;	// fail on file read io error
			}
			catch (Exception e) {
				loadedDataRecords = new ArrayList<ReconSnowDataRecord>();  // return null list other (file-not-found) errors for now as files expected to be loaded onto sdcard
			}
		}
		return loadedDataRecords;


		// for testing just read file from preload storage
//		Log.d(TAG, "SNOW DATA: starting tile load");
	}

	
	

// no longer used... remove
	protected void LoadRecordsIntoArray(ZipFile zipFile, TreeMap<BaseDataTypes, WorldObject.WorldObjectTypes> inDataQuery, ArrayList<ReconSnowDataRecord> dataRecords) throws Exception{
		// load all records in zip file that are in the query in to the data record array
		
		ByteBuffer shapeStream = mZippedRecordDAL.CreateByteBuffer(zipFile, "tile.shp", null);   // single file??  or like MD with separate shp file for points, trails, areas and resort info
		if(shapeStream == null) throw new Exception ("shapeStream is null");
		ShpContent shpContent = null;

		try {
			shpContent = ShpTools.ReadRecords( shapeStream );
			if(shpContent == null) throw new Exception ("ShpContent wasn't loaded correctly");
			
						
																				// TODO potential error if first record is undefined for some reason... can't just use get(0)
			
			int idx = 0;
			for( ShpRecord shpRecord : shpContent.shpRecords )	{
				
			}	
		} 
		catch (Exception e) { 
			
		}
		finally {
			if(shpContent != null) {
				shpContent.Release();
			}
		}
	}


	void runTestPoints(Context context)  {
		Resources res = context.getResources();
			
		try   {
			String path = GetPreloadedTileFolder();
			FileInputStream fstream = new FileInputStream(path + "geopointtest.txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String strLine;

			while ((strLine = br.readLine()) != null) {
				String[] token = strLine.split(",");
				double longitude = Double.parseDouble(token[0]);
				double latitude = Double.parseDouble(token[1]);
				Integer tileIndex =	GeoTile.GetTileIndex(longitude, latitude); 
				if(tileIndex.intValue() == Integer.parseInt(token[2])) {
					Log.d(TAG,"  > "+ token[0] + "|"+ longitude + ", " + token[1]+ "|" + latitude + " maps to same tile#" + tileIndex );
				}
				else {
					Log.d(TAG,"  > "+ token[0] + "|"+ longitude + ", " + token[1]+ "|" + latitude + " maps to tile#" + tileIndex + " which is different from Simon's tile #" + token[2] );
				}
			}
			in.close();
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}		

		

	}

	ArrayList<ReconSnowDataRecord> loadTile(File filepath, int tileIndex, TreeMap<BaseDataTypes, WorldObject.WorldObjectTypes> inDataQuery) throws Exception {
		return loadRGZTile(filepath, tileIndex, inDataQuery);
	}

    private static ArrayList<ReconSnowDataRecord> loadRGZTile(File cmpFilePath, int tileIndex, TreeMap<BaseDataTypes, WorldObject.WorldObjectTypes> inDataQuery) throws Exception {		// data out (encoding)
     	ArrayList<ReconSnowDataRecord> records = new ArrayList<ReconSnowDataRecord>();

		int recordCount = 0;
		try {
	
	        int data = 0;
	     	byte[] fileData = null;
			Date startTime = new Date();
	     	
	     	FileInputStream inputFile = new FileInputStream(cmpFilePath);
	     	GZIPInputStream zipStream = new GZIPInputStream(new BufferedInputStream(inputFile));  // casting fileinputstream into ZipInputStream
	     	
	        ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
	        while( ( data = zipStream.read() ) != - 1 )   {
	        	baOutput.write( data );
	        }
	        fileData = baOutput.toByteArray();
	        baOutput.close();
	     	zipStream.close(); 
	     	inputFile.close();
	     	
			ByteBuffer packedTileData = ByteBuffer.wrap(fileData);
			int numRecords = (int)packedTileData.getShort();	// get number of records in this compressed file
			
//			Log.d(TAG, "# records in tile " + tileIndex + ".rgz is " + numRecords);
			for(int ii = 0; ii < numRecords; ii++) {		
				int nextTypeOrdinal = (int)packedTileData.get();
//				Log.d(TAG, "object ordinal " + nextTypeOrdinal );
				BaseDataTypes objBaseType = BaseDataTypes.values()[nextTypeOrdinal];// get new object type identifier
	
				boolean addObject = (inDataQuery == null || inDataQuery.get(objBaseType) != null);
				ReconSnowDataRecord currentRecord = null;
				if(objBaseType != null) {
					// create new record with empty point data
//					Log.d(TAG, "In tile#" + tileIndex + ": " + objBaseType);
					switch(objBaseType) {
						case POI_RESTAURANT:
						case POI_PARKING:
						case POI_INFORMATION: {
							ReconSnowPOIRecord currentPOIRecord = new ReconSnowPOIRecord(objBaseType, "", new PointXY(0.f,0.f), false);	// create base record
							if(currentPOIRecord != null) {
								currentPOIRecord.UnpackFromBuf(packedTileData);	// unpack compressed data into base record
								if(addObject) {
									records.add(currentPOIRecord);					// add to list of records
									recordCount ++;
								}
							}
							break;
						}
						case LINE_SKIRUN_GREEN:
						case LINE_SKIRUN_BLUE:
						case LINE_SKIRUN_BLACK:
						case LINE_SKIRUN_DBLACK:
						case LINE_SKIRUN_RED:
						case LINE_CHAIRLIFT:
						case LINE_ROADWAY:
						case LINE_WALKWAY:  {
							ReconSnowTrailRecord currentTrailRecord = new ReconSnowTrailRecord(objBaseType, "", new ArrayList<PointXY>(), 0, false, false);
							if(currentTrailRecord != null) {
								currentTrailRecord.UnpackFromBuf(packedTileData);
								if(addObject) {
									records.add(currentTrailRecord);					// add to list of records
									recordCount ++;
								}
							}
							break;
						}

						case AREA_WOODS: 
						case AREA_PARK:  {
							ReconSnowAreaRecord currentAreaRecord = new ReconSnowAreaRecord(objBaseType, "", new ArrayList<PointXY>(), false);
							if(currentAreaRecord != null) {
								currentAreaRecord.UnpackFromBuf(packedTileData);
								if(addObject) {
									records.add(currentAreaRecord);
									recordCount ++;
								}
							}
							break;
						}
						default: {	
							break;
						}

					}
				}
		
			}
			Date endTime = new Date();
//			Log.d(TAG, "SNOW DATA: LoadRGZTile load time for tile#" + tileIndex + ": " + (endTime.getTime() - startTime.getTime()) + "ms for " + recordCount + " objects");
		}
		catch (IOException e) {
			boolean deleted = cmpFilePath.delete();
			if(deleted) {
				Log.e(TAG, "SNOW DATA: Unable to load tile#" +tileIndex+".rgz file due to system file error:" + e.getMessage() + ". Removing bad file.");
				throw new Exception("Unable to load tile#" +tileIndex+".rgz file: " + e.getMessage() + ". Removing bad file.");
			}
			else {
				Log.e(TAG, "SNOW DATA: Unable to load tile#" +tileIndex+".rgz file due to system file error:" + e.getMessage() + ". Unable to remove this bad file.");
				throw new Exception("Unable to load tile#" +tileIndex+".rgz file: " + e.getMessage() + ". Unable to remove this bad file.");
			}
		}
		catch (Exception e) {
			Log.e(TAG, "SNOW DATA: Unable to load tile#" +tileIndex+".rgz file after " + recordCount + " records:" + e.getMessage());
			throw new Exception("Unable to load tile#" +tileIndex+".rgz file: " + e.getMessage());
		}
 

     	return records;
 	}

	

}
