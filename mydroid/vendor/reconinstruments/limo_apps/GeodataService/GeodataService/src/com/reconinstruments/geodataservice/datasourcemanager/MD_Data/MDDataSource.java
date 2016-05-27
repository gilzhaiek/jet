package com.reconinstruments.geodataservice.datasourcemanager.MD_Data;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipFile;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.reconinstruments.geodataservice.datasourcemanager.DataSource;
import com.reconinstruments.geodataservice.datasourcemanager.StaticMapDataTranscoder;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.AssetDB;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.CommonSettings;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.ZippedRecordDAL;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.dbf.DbfContent;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.dbf.DbfRecord;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.dbf.DbfTools;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp.ShpContent;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp.ShpPoint;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp.ShpPolygon;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp.ShpPolyline;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp.ShpRecord;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp.ShpTools;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp.ShpType;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.datarecords.MDAreaRecord;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.datarecords.MDDataRecord;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.datarecords.MDPOIRecord;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.datarecords.MDTrailRecord;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortInfoResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortRequest;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;

public class MDDataSource extends DataSource		// Mountain Dynamics data source - based mostly around old listOfResortsDAL.java
{
// constants 
	private final static String TAG = "MDDataSource";
//	private static final String SQL_COUNTRY_REGION_QUERY	= "SELECT * FROM countryRegion order by id";

	private static final float LOADEDDATABB_SIZE_IN_KMS	= 30.f;
	private static final float PRELOADBB_FACTOR	= 2.f/3.f;
	
	private final Semaphore mControlOfCache = new Semaphore(1, true);
	private final Semaphore mAccessToZipFiles = new Semaphore(1, true);


	public enum MDBaseDataTypes {
		POI_RESTAURANT,
		POI_PARKING,
		POI_INFORMATION,
		AREA_WOODS,
		AREA_SHRUB,
		AREA_TUNDRA,
		AREA_PARK,
		AREA_SKIRESORT,			// not native to MD dataset. Introduced to source resort backgrounds
		TRAIL_SKIRUN_GREEN,
		TRAIL_SKIRUN_BLUE,
		TRAIL_SKIRUN_BLACK,
		TRAIL_SKIRUN_DBLACK,
		TRAIL_SKIRUN_RED,
		TRAIL_CHAIRLIFT,
		TRAIL_ROADWAY,
		TRAIL_WALKWAY
	}

	
// members 
	MDListOfResorts						mListOfResorts = new MDListOfResorts();
	AssetDB 							mResortInfoDB = null;
	
	RectXY								mTestForPreloadBB = null;
	RectXY								mLoadedDataBB = null;
	ArrayList<MDDataRecord>				mCachedDataRecords = new ArrayList<MDDataRecord>();
	ArrayList<MDDataRecord>				mBackgroundCache = new ArrayList<MDDataRecord>();
	ArrayList<String>					mPreloadedResortNames = new ArrayList<String>();
	protected static ZippedRecordDAL	mZippedRecordDAL = null;
	protected Context					mContext = null;
	protected TreeMap<String, MDBaseDataTypes>		mAmericasTypeNameToTypeIndex = new TreeMap<String, MDBaseDataTypes>();
	protected TreeMap<String, MDBaseDataTypes>		mNonAmericasTypeNameToTypeIndex = new TreeMap<String, MDBaseDataTypes>();
	AsyncTask<Void, Void, String> 		mPreloadDataIntoCacheTask = null;
	boolean								mPrefetchingData = false;
	  PointXY 	mPreloadedUserLocation = null;	// in gps coords
	  boolean	mResortsFileLoaded = false;
	
// methods
	MDDataSource(StaticMapDataTranscoder transcoder, Context context, DevTestingState _devTestingState) throws Exception {
		super(transcoder, context, _devTestingState);
		mContext = context;
		mResortInfoDB = new AssetDB(mContext);
		
		if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
		     mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.MD_SOURCE_CREATE_ERROR.ordinal()) == 1) ) {
			throw new Exception ("Forced MDSOURCE creation error.");
		}
		
		if(mZippedRecordDAL == null) {
			mZippedRecordDAL = new ZippedRecordDAL();
		}
		
		// define GRMNTypeFieldString mappings to datatype enums for america and non-america resorts
		mAmericasTypeNameToTypeIndex.put("GREEN_TRAIL", 	MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_GREEN); 
		mAmericasTypeNameToTypeIndex.put("GREEN_TRUNK", 	MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_GREEN); 
		mAmericasTypeNameToTypeIndex.put("BLUE_TRAIL", 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_BLUE); 
		mAmericasTypeNameToTypeIndex.put("BLUE_TRUNK", 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_BLUE); 
		mAmericasTypeNameToTypeIndex.put("BLACK_TRAIL", 	MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_BLACK); 
		mAmericasTypeNameToTypeIndex.put("BLACK_TRUNK", 	MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_BLACK); 
		mAmericasTypeNameToTypeIndex.put("DBLBLCK_TRAIL", 	MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_DBLACK); 
		mAmericasTypeNameToTypeIndex.put("DBLBLCK_TRUNK", 	MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_DBLACK); 
		mAmericasTypeNameToTypeIndex.put("SKI_LIFT", 		MDDataSource.MDBaseDataTypes.TRAIL_CHAIRLIFT); 
		mAmericasTypeNameToTypeIndex.put("TRAIL", 			MDDataSource.MDBaseDataTypes.TRAIL_WALKWAY); 
		mAmericasTypeNameToTypeIndex.put("CHWY_RESID", 		MDDataSource.MDBaseDataTypes.TRAIL_ROADWAY); 
		mAmericasTypeNameToTypeIndex.put("RESTAURANT_AMERICAN", MDDataSource.MDBaseDataTypes.POI_RESTAURANT); 
		mAmericasTypeNameToTypeIndex.put("PARKING", 		MDDataSource.MDBaseDataTypes.POI_PARKING); 
		mAmericasTypeNameToTypeIndex.put("INFORMATION", 	MDDataSource.MDBaseDataTypes.POI_INFORMATION); 
		mAmericasTypeNameToTypeIndex.put("WOODS", 			MDDataSource.MDBaseDataTypes.AREA_WOODS); 
		mAmericasTypeNameToTypeIndex.put("SCRUB", 			MDDataSource.MDBaseDataTypes.AREA_SHRUB); 
		mAmericasTypeNameToTypeIndex.put("TUNDRA", 			MDDataSource.MDBaseDataTypes.AREA_TUNDRA); 
		mAmericasTypeNameToTypeIndex.put("URBAN_PARK", 		MDDataSource.MDBaseDataTypes.AREA_PARK); 
		
		mNonAmericasTypeNameToTypeIndex.put("GREEN_TRAIL", 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_GREEN); 
		mNonAmericasTypeNameToTypeIndex.put("GREEN_TRUNK", 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_GREEN); 
		mNonAmericasTypeNameToTypeIndex.put("BLUE_TRAIL", 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_BLUE); 
		mNonAmericasTypeNameToTypeIndex.put("BLUE_TRUNK", 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_BLUE); 
		mNonAmericasTypeNameToTypeIndex.put("BLACK_TRAIL", 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_BLACK); 
		mNonAmericasTypeNameToTypeIndex.put("BLACK_TRUNK", 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_BLACK); 
		mNonAmericasTypeNameToTypeIndex.put("RED_TRAIL", 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_RED); 
		mNonAmericasTypeNameToTypeIndex.put("RED_TRUNK", 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_RED); 
		mNonAmericasTypeNameToTypeIndex.put("SKI_LIFT", 		MDDataSource.MDBaseDataTypes.TRAIL_CHAIRLIFT); 
		mNonAmericasTypeNameToTypeIndex.put("TRAIL", 			MDDataSource.MDBaseDataTypes.TRAIL_WALKWAY); 
		mNonAmericasTypeNameToTypeIndex.put("CHWY_RESID", 		MDDataSource.MDBaseDataTypes.TRAIL_ROADWAY); 
		mNonAmericasTypeNameToTypeIndex.put("RESTAURANT_AMERICAN", MDDataSource.MDBaseDataTypes.POI_RESTAURANT); 
		mNonAmericasTypeNameToTypeIndex.put("PARKING", 			MDDataSource.MDBaseDataTypes.POI_PARKING); 
		mNonAmericasTypeNameToTypeIndex.put("INFORMATION", 		MDDataSource.MDBaseDataTypes.POI_INFORMATION); 
		mNonAmericasTypeNameToTypeIndex.put("WOODS", 			MDDataSource.MDBaseDataTypes.AREA_WOODS); 
		mNonAmericasTypeNameToTypeIndex.put("SCRUB", 			MDDataSource.MDBaseDataTypes.AREA_SHRUB); 
		mNonAmericasTypeNameToTypeIndex.put("TUNDRA", 			MDDataSource.MDBaseDataTypes.AREA_TUNDRA); 
		mNonAmericasTypeNameToTypeIndex.put("URBAN_PARK", 		MDDataSource.MDBaseDataTypes.AREA_PARK); 

	}
	
 
	public String init() {
		try {
			// do everything needed to get data source ready
			
			// verify existence of zip file
			File mapDataZipFile = new File(CommonSettings.GetMapDataZipFileName());

			String errorMsg= null;
			if( mapDataZipFile.exists() == false || 
				(mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
				 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.MD_SOURCE_MDDATA_ZIP_NO_FILE.ordinal()) == 1) ) 	{
				errorMsg = "Resort Zip file named " + CommonSettings.GetMapDataZipFileName() + " doesn't exist";
				Log.e(TAG, errorMsg);
				return errorMsg;
			}
			else if( mapDataZipFile.canRead() == false || 
					(mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
					 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.MD_SOURCE_MDDATA_ZIP_READ_ERROR.ordinal()) == 1)  ) {
				errorMsg = "Resort Zip file cannot be read" + CommonSettings.GetMapDataZipFileName();
				Log.e(TAG, errorMsg);
				return errorMsg;
			}

			
			// confirm zip file okay by trying to load list of resorts
			LoadListOfResorts(mListOfResorts); //  fails by throwing exceptions
			mResortsFileLoaded = true;
			return null;
		}		
		catch (Exception e) {
			String errorMsg = "MDDataSource initialization error: " + e.toString();
			Log.e(TAG, errorMsg);
			return errorMsg;
		}

	}

	@Override
	public ArrayList<Integer> GetCachedDataStats() throws Exception {
		ArrayList<Integer> result = new ArrayList<Integer>();
		result.add(1);	// TODO make this work after MD data converted to tiles...
		if(mPrefetchingData) {
			result.add(0);
		}
		else {
			result.add(1);
		}
		return result;
	}
	
	@Override
	public void SetUserLocation(float longitude, float latitude)  throws Exception {
		
		if(!mResortsFileLoaded) return;
		
		boolean prefetchData = false;
		
		if(mTestForPreloadBB == null) {		// if data not already preloaded, preload
			prefetchData = true;
		}
		else {	
			if(!mTestForPreloadBB.Contains(longitude, latitude)) {	// if new point outside of preloadBB - which is a internal boundary to the preloaded georegion
				prefetchData = true;
			}
		}
			
		if(prefetchData) {
			PrefetchData(longitude, latitude);  // replace existing data and boundaries mLoadedDataBB && mTestForPreloadBB
		}
	}

	public ArrayList<String> GetResortNames() {
		return mPreloadedResortNames;
	}
 

	private void PrefetchData(float longitude, float latitude)  throws Exception {		// cache data records around location(long, lat)
			if(mPrefetchingData) {
//				Log.d(TAG, "redundant request for prefetch - prefetch in progress - request ignored");
				return ;	
			}
			
			mPrefetchingData = true;	// ignore all redundant attempts to prefetch
			Log.d(TAG, "prefetching data around point " + longitude + ", " + latitude);
			
			mAccessToZipFiles.acquire();

			mPreloadDataIntoCacheTask = new PreloadDataIntoCacheTask(longitude, latitude);
			mPreloadDataIntoCacheTask.execute();		// allows call to return (driven by GPS onLocationChanged() in service) while data is loaded
		}
	
	protected class PreloadDataIntoCacheTask extends AsyncTask<Void, Void, String> {
		
		float mLongitude;
		float mLatitude;
		ArrayList<String> mResortNames = new ArrayList<String>();
		RectXY mNewBB = null;
		float mLongAngle;
		float mLatAngle;
		ArrayList<MDDataRecord>	mBackgroundCache = null;
		
		public PreloadDataIntoCacheTask(float longitude, float latitude) {
			mLongitude = longitude;
			mLatitude = latitude;

			mLongAngle = CalcLongAngle(LOADEDDATABB_SIZE_IN_KMS, mLatitude, mLongitude);
			mLatAngle = CalcLatAngle(LOADEDDATABB_SIZE_IN_KMS, mLatitude, mLongitude);
			mNewBB = new RectXY(mLongitude-mLongAngle/2.f, mLatitude+mLatAngle/2.f, mLongitude+mLongAngle/2.f, mLatitude-mLatAngle/2.f);

		}
		
		protected String doInBackground(Void...voids) {

//			Log.d(TAG, "Starting background task to prefetch data...");
			try	{
				mBackgroundCache = new ArrayList<MDDataRecord>();
				
				mBackgroundCache.clear();
				mResortNames.clear();
				for(MDResortInfo resort : mListOfResorts.mResorts) {
					if(mNewBB.Intersects(resort.mBoundingBox)) {
						// first add terrain record to define resort boundary box
						Log.i(TAG, "NEW RESORT - " + resort.mName);
						
						// then add features within the resort
						LoadResortDataIntoCache(resort, mBackgroundCache, true);	// true marks records as user-location-dependent, ie, not to be removed by GC
						mResortNames.add(resort.mName);
					}
				}
				return ""; 
			}
			catch (Exception e) {
				return e.toString();
			}		 
		}

		protected void onPostExecute(String errorString) {

			if(!errorString.equalsIgnoreCase("")) { // error
//				ReportException(errorString);
				Log.e(TAG, "Error prefetching data into cache: " + errorString);
				// TODO some other form of handling ??  -- report back up to service...
			}
			else{
				try {
					mControlOfCache.acquire();			 // wait a moment until cache access is finished
				} 
				catch (InterruptedException e) {
					// allows semaphore to time out... because it's not been released somewhere else - if this happens... swap it anyhow... unlikely to cause problems as it's loaded infrequently
				}					
				mCachedDataRecords.clear();
				mCachedDataRecords.addAll(mBackgroundCache);		// copy items to cache as mBackgroundCache will disappear when task ends
				mControlOfCache.release();
				
				mPreloadedResortNames.clear();
				mPreloadedResortNames.addAll(mResortNames);
	
				if(mPreloadedResortNames.size() > 0) {
					Log.d(TAG, "Prefetched resorts...");
					for(String name : mPreloadedResortNames) {
						Log.d(TAG, "   " + name);
					}
					Log.d(TAG, "records in cache: " + mCachedDataRecords.size());
				}
				mLoadedDataBB = mNewBB; // set after data loaded so requests on the cache do not try to load 
										// set preloadBB as factor of loadedDataBB size -- value > 0.5 < 1.0f recommended
				mTestForPreloadBB = new RectXY(mLongitude-(PRELOADBB_FACTOR*mLongAngle/2.f), mLatitude+(PRELOADBB_FACTOR*mLatAngle/2.f), mLongitude+(PRELOADBB_FACTOR*mLongAngle/2.f), mLatitude-(PRELOADBB_FACTOR*mLatAngle/2.f));
				
			}
			mAccessToZipFiles.release();
			mPrefetchingData = false;

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

	public void LoadListOfResorts(MDListOfResorts listOfResorts) throws Exception {

		SQLiteDatabase resortDB = null;

		try {
			resortDB = mResortInfoDB.GetDB();		// get handle to DB
			if(resortDB == null || 
					(mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
					 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.MD_SOURCE_RESORTINFO_DB_NO_FILE.ordinal()) == 1)) {
				throw new Exception ("Could not access the MD database ResortInfo.db.");
			}
			
			listOfResorts.SetCorrupted(mResortInfoDB.IsMapDataCorrupted());	// record if DB is corrupted
			
//			LoadCountryRegions(resortDB, listOfResorts);  // needed??
			LoadResorts(resortDB, listOfResorts);
			Log.i(TAG, "Resort zip file exists and ResortList has been loaded..");
		}
		catch (Exception exception) {
			if(resortDB != null) {
				if(resortDB.isOpen()) {
					resortDB.close();
				}
			}
			Log.e(TAG, exception.toString());
			throw new Exception (exception.toString()); 
		}
		finally {
			if(resortDB != null) {
				if(resortDB.isOpen()) {
					resortDB.close();
				}
			}
		}
	}
	
	protected void LoadResorts(SQLiteDatabase resortDB, MDListOfResorts listOfResorts) throws Exception {
		resortDB.beginTransaction();
		Cursor cursor = null;
		try
		{		
			if(mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
			   mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.MD_SOURCE_RESORTINFO_DB_READ_ERROR.ordinal()) == 1) {
				throw new Exception ("forced LoadResorts read to crash.");
				
			}
			cursor = resortDB.rawQuery("SELECT * FROM resortLocation where mapVersion=="+mResortInfoDB.GetDBVersion()+" order by countryID", new String[] {});
			resortDB.setTransactionSuccessful();
			resortDB.endTransaction();

			int idxId			= cursor.getColumnIndex("id");
			int idxName			= cursor.getColumnIndex("name");
			int idxCRId			= cursor.getColumnIndex("countryID");
			int idxlat			= cursor.getColumnIndex("hubLatitude");
			int idxlng			= cursor.getColumnIndex("hubLongitude");
			int idxMinLat		= cursor.getColumnIndex("minLatitude");
			int idxMaxLat		= cursor.getColumnIndex("maxLatitude");
			int idxMinLng		= cursor.getColumnIndex("minLongitude");
			int idxMaxLng		= cursor.getColumnIndex("maxLongitude");
			int idxMapVersion	= cursor.getColumnIndex("mapVersion");
			 
			int rowCount = cursor.getCount();
			for( int i = 0; i < rowCount; ++i )
			{
				cursor.moveToPosition(i); 
				 
				RectXY bbox			= new RectXY((float)cursor.getDouble(idxMinLng),
					 						(float)cursor.getDouble(idxMaxLat),			
					 						(float)cursor.getDouble(idxMaxLng),
					 						(float)cursor.getDouble(idxMinLat));
				 
				PointXY location	= new PointXY((float)cursor.getDouble(idxlng), (float)cursor.getDouble(idxlat));
				 
				int crID 			= cursor.getInt(idxCRId);
				int mapVersion		= cursor.getInt(idxMapVersion);
				int resortID		= cursor.getInt(idxId);
				String resortName	= cursor.getString(idxName);
				 
				MDResortInfo resortInfo = null;
//				resortInfo = new ResortInfo(resortID , resortName, crID, location, mapVersion, bbox, resortHolder.ParentHolder.Name, resortHolder.Name);
				resortInfo = new MDResortInfo(resortID , resortName, crID, location, mapVersion, bbox, null, null);  // not loading country/region for now... if desired, load up countryRegion table into hash and use that to fill name values.  Avoid the Holder idea used before

				listOfResorts.AddResort(resortInfo);
			}		
			 
			cursor.close();			 
		}
		catch (Exception exception)
		{
			if(resortDB.inTransaction()) {
				resortDB.endTransaction();
			}
			Log.e(TAG, "Failed during load of resorts:" + exception.toString());
			throw new Exception("Failed during load of resorts:" + exception.toString());
		} 
		finally 
		{
			if(resortDB.inTransaction()) {
				resortDB.endTransaction();
			}
		}		
	}
	
	
//	protected void LoadCountryRegions(SQLiteDatabase resortDB, listOfResorts listOfResorts) throws Exception {
//		resortDB.beginTransaction();
//		Cursor cursor = null;
//		try
//		{		
//			cursor = resortDB.rawQuery(SQL_COUNTRY_REGION_QUERY, new String[] {});
//			resortDB.setTransactionSuccessful();
//			resortDB.endTransaction();
//
//			int idxName = cursor.getColumnIndex( "name" );
//			int idxId = cursor.getColumnIndex( "id" );
//			
//			for (int i = 0; i < cursor.getCount(); ++i)
//			{
//				cursor.moveToPosition(i);
//				
//				String countryRegionName	= cursor.getString(idxName).trim();
//				int countryRegionID 		= cursor.getInt(idxId);
//				
//				String regionName = null;
//				String countryName = null;
//				if(HasRegion(countryRegionName)) {
//					String regionName = ExtractRegionName( countryRegionName );
//					String countryName = ExtractCountryName( countryRegionName );
//				} 
//				else {
//					countryName = countryRegionName; 
//				}
//				// load country and region names into hashMap for processing 
//			}
//			cursor.close();			 
//		}
//		catch (Exception exception)
//		{
//			Log.e("Failed to LoadCountriesRegions", exception.toString());
//			throw new Exception("Failed to LoadCountriesRegions");
//		} 
//		finally 
//		{
//			if(resortDB.inTransaction()) {
//				resortDB.endTransaction();
//			}
//		}
//	}
//
//	// Check if a name is a country name or a combination of countryRegion
//	private boolean HasRegion( String name ) {
//		return name.contains("/");
//	}
//	
//	// Get the country name from a string that might mix with both country and region name
//	private String ExtractCountryName( String name ) {
//		return (HasRegion( name )) ? name.substring(0, name.lastIndexOf("/")) : name; 
//	}
//
//	private String ExtractRegionName( String name ) {
//		return (HasRegion( name )) ? name.substring(name.lastIndexOf("/") + 1, name.length()) : null;
//	}
//	
	
	
	public void PrintAvailMem(){
		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(memoryInfo);
		Log.i(TAG, " memoryInfo.availMem " + memoryInfo.availMem + "\n" );
	}
	
	
	public void LoadResortDataIntoCache(MDResortInfo resort, ArrayList<MDDataRecord> dataRecordCache, boolean isUserLocationDependent) throws Exception {
		ZipFile zippedMap = null;
		
		try{
//			Log.v(TAG, "Loading " + resort.GetParentName() +"/"+ resort.mName + "..." + isUserLocationDependent); 
//			PrintAvailMem();
			
			File mapDataZipFile = new File(CommonSettings.GetMapDataZipFileName());
			if( mapDataZipFile.exists() == false) {
				Log.e(TAG, "MDDataSource - " + CommonSettings.GetMapDataZipFileName() + " Doesn't Exist");
				throw new Exception("MDDataSource (LoadResortDataIntoCache) - " + CommonSettings.GetMapDataZipFileName() + " Doesn't Exist");
			}
			else if( mapDataZipFile.canRead() == false) {
				Log.e(TAG, "MDDataSource - Failed to read " + CommonSettings.GetMapDataZipFileName());
				throw new Exception("MDDataSource (LoadResortDataIntoCache) - Failed to read " + CommonSettings.GetMapDataZipFileName());
			}
			
			zippedMap = new ZipFile( mapDataZipFile );
			
	    	RectXY resortTrueBB = new RectXY(200.f, -200.f, -200.f, 200.f);					// needed because MD data is sometimes screwed up for resort boundaries...
			LoadRecordsIntoCache(zippedMap, "lines/" + resort.mAssetBaseName, dataRecordCache, isUserLocationDependent, resortTrueBB);
			LoadRecordsIntoCache(zippedMap, "areas/" + resort.mAssetBaseName, dataRecordCache, isUserLocationDependent, resortTrueBB);
			LoadRecordsIntoCache(zippedMap, "points/" + resort.mAssetBaseName, dataRecordCache, isUserLocationDependent, resortTrueBB);
			
			if(resort.mBoundingBox.left != resortTrueBB.left || resort.mBoundingBox.right != resortTrueBB.right || 
			   resort.mBoundingBox.top != resortTrueBB.top || resort.mBoundingBox.bottom != resortTrueBB.bottom) {
				Log.e(TAG, "Supplied boundary of resort " + resort.mName + " is inaccurate - being modified");
				Log.e(TAG, "       " + resort.mBoundingBox.left + "|" + resortTrueBB.left + ", " + resort.mBoundingBox.right + "|" + resortTrueBB.right + ", " + 
						   resort.mBoundingBox.top + "|" + resortTrueBB.top + ", " + resort.mBoundingBox.bottom + "|" + resortTrueBB.bottom);
			}
			else {
				Log.d(TAG, "Supplied boundary of resort " + resort.mName + " is accurate");
			}
			ArrayList<PointXY> resortBoundaryPoints = new ArrayList<PointXY>();
//	    	resortBoundaryPoints.add( new PointXY( resort.mBoundingBox.left, resort.mBoundingBox.top ));
//	    	resortBoundaryPoints.add( new PointXY( resort.mBoundingBox.right, resort.mBoundingBox.top ));
//	    	resortBoundaryPoints.add( new PointXY( resort.mBoundingBox.right, resort.mBoundingBox.bottom ));
//	    	resortBoundaryPoints.add( new PointXY( resort.mBoundingBox.left, resort.mBoundingBox.bottom ));
	    	resortBoundaryPoints.add( new PointXY( resortTrueBB.left, resortTrueBB.top ));
	    	resortBoundaryPoints.add( new PointXY( resortTrueBB.right, resortTrueBB.top ));
	    	resortBoundaryPoints.add( new PointXY( resortTrueBB.right, resortTrueBB.bottom ));
	    	resortBoundaryPoints.add( new PointXY( resortTrueBB.left, resortTrueBB.bottom ));

	    	dataRecordCache.add(new MDAreaRecord(MDBaseDataTypes.AREA_SKIRESORT, resort.mName, resortBoundaryPoints, true));
//			Log.v(TAG, "Finished Loading " + resort.mName); 
//			PrintAvailMem();
			
		}
		catch (Exception e) {
			if(zippedMap != null) {
				zippedMap.close();
			}
			Log.e(TAG, "Failed LoadResortDataIntoCache: "+e.getMessage());
			throw new Exception(e.getMessage());
		}
		finally {
			if(zippedMap != null) {
				zippedMap.close();
			}
		}
	}
	


	protected void LoadRecordsIntoCache(ZipFile zippedRecords, String assetName, ArrayList<MDDataRecord> dataRecordCache, boolean isUserLocationDependent, RectXY resortTrueBB) throws Exception{

		ByteBuffer shapeStream = mZippedRecordDAL.CreateByteBuffer(zippedRecords, assetName + ".shp", assetName + ".SHP");
		if(shapeStream == null) throw new Exception ("shapeStream is null");
		ShpContent shpContent = null;

		ByteBuffer dbStream = mZippedRecordDAL.CreateByteBuffer(zippedRecords, assetName + ".dbf", null);
		if(dbStream == null) throw new Exception ("dbStream is null");
		DbfContent dbfContent = null;

		try {
			shpContent = ShpTools.ReadRecords( shapeStream );
			if(shpContent == null) throw new Exception ("ShpContent wasn't loaded correctly");
			
			dbfContent = DbfTools.ReadRecords( dbStream );
			if(dbfContent == null) throw new Exception ("dbfContent wasn't loaded correctly");
						
																				// TODO potential error if first record is undefined for some reason... can't just use get(0)
			boolean isInAmericas = IsInAmericas(shpContent.shpRecords.get(0));  // use first record coordinates to determine if we are in the americas
			
			int idx = 0;
			for( ShpRecord shpRecord : shpContent.shpRecords )	{
				DbfRecord dbfRecord = dbfContent.dbfRecords[idx++];	// follow along..
				if(dbfRecord.HasGRMNType) {
					if(shpRecord.shapeType == ShpType.SHAPE_POINT) {
						AddPOIToCache((ShpPoint)shpRecord.shape, dbfRecord, isInAmericas, dataRecordCache, isUserLocationDependent, resortTrueBB);
					}
					else if(shpRecord.shapeType == ShpType.SHAPE_POLYLINE) {
						AddTrailToCache((ShpPolyline)shpRecord.shape, dbfRecord, isInAmericas, dataRecordCache, isUserLocationDependent, resortTrueBB);
					}
					else if(shpRecord.shapeType == ShpType.SHAPE_POLYGON) {
						AddAreaToCache((ShpPolygon)shpRecord.shape, dbfRecord, isInAmericas, dataRecordCache, isUserLocationDependent, resortTrueBB);
					}
				}
			}	
		} 
		catch (Exception e) { 
			
		}
		finally {
			if(shpContent != null) {
				shpContent.Release();
			}
			if(dbfContent != null) {
				dbfContent.Release();
			}
		}
	}
	protected boolean IsInAmericas(ShpRecord record){
		float tmpLongitude = -90; // Americas
		  
		switch( record.shapeType ) 	{                
			case ShpType.SHAPE_POINT:    tmpLongitude = (float)(((ShpPoint)   record.shape).x); break;
			case ShpType.SHAPE_POLYLINE: tmpLongitude = (float)(((ShpPolyline)record.shape).box.left); break;
			case ShpType.SHAPE_POLYGON:  tmpLongitude = (float)(((ShpPolygon) record.shape).box.left); break;
		}
		return (tmpLongitude < -34);
	}

	
	protected void AddPOIToCache(ShpPoint shpPoint, DbfRecord dbfRecord, boolean isInAmericas, ArrayList<MDDataRecord> dataRecordCache, boolean isUserLocationDependent, RectXY resortTrueBB){
		MDDataSource.MDBaseDataTypes poiType = null;
		if(isInAmericas) {
			poiType = mAmericasTypeNameToTypeIndex.get(dbfRecord.GRMNTypeFieldString);
		}
		else {
			poiType = mNonAmericasTypeNameToTypeIndex.get(dbfRecord.GRMNTypeFieldString);
		}
		if(poiType == null) return;		// not a valid geodata service data type
		
		String poiName = dbfRecord.GetName();
		if(poiName.isEmpty()) poiName = WorldObject.UNKNOWN_ITEM_NAME;
		dataRecordCache.add(new MDPOIRecord(poiType, poiName, new PointXY((float) shpPoint.x, (float) shpPoint.y ), isUserLocationDependent));
		
		if((float)shpPoint.x > resortTrueBB.right)  resortTrueBB.right = (float)shpPoint.x;
		if((float)shpPoint.x < resortTrueBB.left)   resortTrueBB.left = (float)shpPoint.x;
		if((float)shpPoint.y > resortTrueBB.top)    resortTrueBB.top = (float)shpPoint.y;
		if((float)shpPoint.y < resortTrueBB.bottom) resortTrueBB.bottom = (float)shpPoint.y;
	}
	
	
	protected void AddTrailToCache(ShpPolyline shpPolyline, DbfRecord dbfRecord, boolean isInAmericas, ArrayList<MDDataRecord> dataRecordCache, boolean isUserLocationDependent, RectXY resortTrueBB){
		MDDataSource.MDBaseDataTypes trailType = null;
		if(isInAmericas) {
			trailType = mAmericasTypeNameToTypeIndex.get(dbfRecord.GRMNTypeFieldString);
		}
		else {
			trailType = mNonAmericasTypeNameToTypeIndex.get(dbfRecord.GRMNTypeFieldString);
		}

		if(trailType == null) return;	// not a valid geodata service data type

		ArrayList<PointXY> trailPoints = new ArrayList<PointXY>();
		for( ArrayList<ShpPoint> shpPointsArray : shpPolyline.rings )  	{
    		for( ShpPoint shpPoint : shpPointsArray )	{
    			trailPoints.add( new PointXY( (float)shpPoint.x, (float)shpPoint.y ));

    			if((float)shpPoint.x > resortTrueBB.right)  resortTrueBB.right = (float)shpPoint.x;
    			if((float)shpPoint.x < resortTrueBB.left)   resortTrueBB.left = (float)shpPoint.x;
    			if((float)shpPoint.y > resortTrueBB.top)    resortTrueBB.top = (float)shpPoint.y;
    			if((float)shpPoint.y < resortTrueBB.bottom) resortTrueBB.bottom = (float)shpPoint.y;
    		}
		}
		if(trailPoints.size() == 0) return;
		
		String trailName = CleanTrailName(dbfRecord.GetName());
		if(trailName.isEmpty()) trailName = WorldObject.UNKNOWN_ITEM_NAME;
		dataRecordCache.add(new MDTrailRecord(trailType, trailName, trailPoints, dbfRecord.GetSpeedLimit(), dbfRecord.IsOneWay(), isUserLocationDependent));
    

	}
	
	protected String CleanTrailName(String name) {
		int idx = name.indexOf('(');
		return (idx == 0) ? "" : ((idx < 0) ? name : name.substring(0, idx - 1));
	}

		
	
	protected void AddAreaToCache(ShpPolygon shpPolygon, DbfRecord dbfRecord, boolean isInAmericas, ArrayList<MDDataRecord> dataRecordCache, boolean isUserLocationDependent, RectXY resortTrueBB){
		MDDataSource.MDBaseDataTypes areaType = null;
		if(isInAmericas) {
			areaType = mAmericasTypeNameToTypeIndex.get(dbfRecord.GRMNTypeFieldString);
		}
		else {
			areaType = mNonAmericasTypeNameToTypeIndex.get(dbfRecord.GRMNTypeFieldString);
		}
		    	
		if(areaType == null) return;	// not a valid geodata service data type

		ArrayList<PointXY> areaPoints = new ArrayList<PointXY>();
		for( ArrayList<ShpPoint> shpPointsArray : shpPolygon.rings )  	{
    		for( ShpPoint shpPoint : shpPointsArray )	{
    			areaPoints.add( new PointXY( (float)shpPoint.x, (float)shpPoint.y ));

    			if((float)shpPoint.x > resortTrueBB.right)  resortTrueBB.right = (float)shpPoint.x;
    			if((float)shpPoint.x < resortTrueBB.left)   resortTrueBB.left = (float)shpPoint.x;
    			if((float)shpPoint.y > resortTrueBB.top)    resortTrueBB.top = (float)shpPoint.y;
    			if((float)shpPoint.y < resortTrueBB.bottom) resortTrueBB.bottom = (float)shpPoint.y;
    		}
		}
		if(areaPoints.size() == 0) return;
		
		String areaName = dbfRecord.GetName();
		if(areaName.isEmpty()) areaName = WorldObject.UNKNOWN_ITEM_NAME;
		dataRecordCache.add(new MDAreaRecord(areaType, areaName, areaPoints, isUserLocationDependent));
	}


	
	
	public ArrayList<MDDataRecord> SourceDataWithQuery(ArrayList<MDQueryRecord> dataQueryRecords, GeoRegion geoRegionToLoad) throws Exception {
		// must be thread safe  
//		Log.e(TAG,"MDSourceSourceData 1");

		mAccessToZipFiles.acquire();	// will block if not available, ie, the cache is being loaded

		ArrayList<MDDataRecord> recordsToUse = mCachedDataRecords;
		if(mLoadedDataBB == null || !mLoadedDataBB.Contains(geoRegionToLoad.mBoundingBox)) {	// if requested GR not in preloaded region (set when GPS present) - possibly region not preloaded yet or looking for data away from user location
			recordsToUse = new ArrayList<MDDataRecord>();							// create new tempCache
			for(MDResortInfo resort : mListOfResorts.mResorts) {					// then load data from files into cache...  noting that Transcoders generally ask for more data (ie, larger GR) than geodata service clients actually request (so don't need to add extra here)
				RectXY gBB = geoRegionToLoad.mBoundingBox;
				RectXY rBB = resort.mBoundingBox;
				
				if(gBB.Intersects(rBB)) {
//					Log.e(TAG, "MDDataSource: " + resort.mName + ": " + gBB.left + "|" + rBB.left + " : " + gBB.right + "|" + rBB.right + " : " + gBB.top + "|" + rBB.top + " : " + gBB.bottom + "|" + rBB.bottom);
					LoadResortDataIntoCache(resort, recordsToUse, false);	// false marks records as not user-location-dependent, ie, can be removed by GC 
																			// currently not being used as these records are not added to the permanent cache - which alleviates need for GC in MDDataSource
				}
			}
		
		}
		mAccessToZipFiles.release();
//		Log.e(TAG,"MDSourceSourceData 2");

		// note the  dataQueryRecords input defines the type of data records to return	
		TreeMap<MDBaseDataTypes, WorldObject.WorldObjectTypes> inDataQuery = new TreeMap<MDBaseDataTypes, WorldObject.WorldObjectTypes>();
		for(MDQueryRecord queryRecord : dataQueryRecords) {
			
//			Log.e(TAG,"Queried types: " + queryRecord.mMDType + ", "+ queryRecord.mServiceObjectType);
//			
			inDataQuery.put(queryRecord.mMDType, queryRecord.mServiceObjectType);		// the query data types are built into a map to simplify query data type lookup during processing loop
		}
		
//		Log.e(TAG,"MDSourceSourceData 3");
		ArrayList<MDDataRecord> loadedMDDataRecords = new ArrayList<MDDataRecord>();	// prepare array for results
		
//		Log.e(TAG,"MDSourceSourceData 4");
		if(recordsToUse == mCachedDataRecords) {	// is sourcing from cache...
			mControlOfCache.acquire();			 // wait a moment until cache access is finished
			try{
		
				for(MDDataRecord dataRecord : recordsToUse) {
//					Log.d(TAG, "MDDataRecord testing: " + dataRecord.mName + ", " + dataRecord.mMDType) ;
					if(dataRecord != null && inDataQuery.get(dataRecord.mMDType) != null ) {
						if(dataRecord.ContainedInGR(geoRegionToLoad.mBoundingBox)) {
							loadedMDDataRecords.add(dataRecord);
						}
					}
				}
				mControlOfCache.release();
			}
			catch (Exception e) {
				mControlOfCache.release();
				throw e;
			}
		}
		else {										// source for temp mini-cache if primary cache (which is centered on user) is not ready ... 
			for(MDDataRecord dataRecord : recordsToUse) {
//				Log.d(TAG, "MDDataRecord: " + dataRecord.mName + ", " + dataRecord.mMDType) ;
				if(dataRecord != null && inDataQuery.get(dataRecord.mMDType) != null && dataRecord.ContainedInGR(geoRegionToLoad.mBoundingBox)) {
					loadedMDDataRecords.add(dataRecord);
				}
			}
		}

		return loadedMDDataRecords;
	}


	public ArrayList<ResortInfoResponse> GetClosestResorts(ResortRequest resortRequest) { 
		TreeMap<Float, MDResortInfo> resultMap = new TreeMap<Float, MDResortInfo>();
		for(MDResortInfo resort : mListOfResorts.mResorts) {					// then load data from files into cache...  noting that Transcoders generally ask for more data (ie, larger GR) than geodata service clients actually request (so don't need to add extra here)
			float distToResort = CalcDistanceInKmBetween(resort.mResortLocation, resortRequest.mLocation);
//			Log.d(TAG, "closest resort calc: " + resort.mName + ": " + distToResort + " | " + resort.mResortLocation.x + ", " + resort.mResortLocation.y);
//			if(resort.mBoundingBox.Contains(resortRequest.mLocation.x, resortRequest.mLocation.y)) distToResort = 0.f;
			if(distToResort <= resortRequest.mMaxDistanceInKm) {
				resultMap.put(distToResort, resort);
			}
		}
		
		ArrayList<ResortInfoResponse> result = new ArrayList<ResortInfoResponse>();
		for(Float distance : resultMap.keySet()) {
//			if(resort.mBoundingBox.Contains(resortRequest.mLocation.x, resortRequest.mLocation.y)) distToResort = 0.f;
			MDResortInfo resort = resultMap.get(distance);
			ResortInfoResponse nextResortResponse = new ResortInfoResponse(resort.mName, (float)distance, resort.mResortLocation, resort.mBoundingBox, false);
			if(resort.mBoundingBox.Contains(resortRequest.mLocation.x, resortRequest.mLocation.y)) nextResortResponse.mTargetPointWithinResortBoundingBox = true;
			result.add(nextResortResponse);
			if(result.size() >= resortRequest.mResultLimit) break;
		}
		return result;
	}
}
