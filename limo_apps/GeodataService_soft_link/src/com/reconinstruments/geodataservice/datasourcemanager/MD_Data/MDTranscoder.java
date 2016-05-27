package com.reconinstruments.geodataservice.datasourcemanager.MD_Data;

import java.util.ArrayList;
import java.util.TreeMap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.reconinstruments.geodataservice.SourcedDataContainer;
import com.reconinstruments.geodataservice.datasourcemanager.DataSourceManager;
import com.reconinstruments.geodataservice.datasourcemanager.StaticMapDataTranscoder;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.datarecords.MDAreaRecord;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.datarecords.MDDataRecord;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.datarecords.MDPOIRecord;
import com.reconinstruments.geodataservice.datasourcemanager.MD_Data.datarecords.MDTrailRecord;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoTile;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortInfoResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortRequest;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.InformationRetrievalCapability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.StaticMapDataCapability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.CarAccess_Parking;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Chairlift;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Chairlift.BottomOfLift;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail.DownhillDirection;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail_Black;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail_Blue;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail_DBlack;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail_Green;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail_Red;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Road_Artery_Tertiary;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.SkiResortService_Information;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.SkiResortService_Restaurant;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.SkiResortService_Walkway;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Park;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Shrub;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_SkiResort;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Tundra;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Woods;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;

public class MDTranscoder extends StaticMapDataTranscoder 
{
// constants 
	private final static String TAG = "MDTranscoder";
	
// members 
	public  MDTranscoder	mSelf = this;				// needed in AsyncTask
	AsyncTask<Void, Void, String> mInitMDDatabaseTask = null;
	AsyncTask<Void, Void, String> mSourceMDDataTask = null;
	private TreeMap<WorldObject.WorldObjectTypes, MDDataSource.MDBaseDataTypes>	mService2MDDataMapping = new TreeMap<WorldObject.WorldObjectTypes, MDDataSource.MDBaseDataTypes>();		
	private TreeMap<MDDataSource.MDBaseDataTypes, WorldObject.WorldObjectTypes>	mMDData2ServiceMapping = new TreeMap<MDDataSource.MDBaseDataTypes, WorldObject.WorldObjectTypes>();		
	
// methods
	public MDTranscoder(DataSourceManager dsm, Context context,DevTestingState _devTestingState) {
		super(Capability.DataSources.MD_SKI, dsm, context, _devTestingState); 
	}
	
	public void init() throws Exception {
		super.init();
		mInitMDDatabaseTask = new InitMDDatabaseTask();
		mInitMDDatabaseTask.execute();		// transcoder waits in background task for MDDataSource to be created and initialize

		if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
			 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.MD_TRANSCODER_CREATE_ERROR.ordinal()) == 1) ) {
				throw new Exception ("Forced MDTRANSCODER creation error.");
		}
		
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.CHAIRLIFT, 					MDDataSource.MDBaseDataTypes.TRAIL_CHAIRLIFT);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.CARACCESS_PARKING, 			MDDataSource.MDBaseDataTypes.POI_PARKING);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.ROAD_ARTERY_TERTIARY, 		MDDataSource.MDBaseDataTypes.TRAIL_ROADWAY);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_GREEN, 	MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_GREEN);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_BLUE, 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_BLUE);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_BLACK, 	MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_BLACK);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_DBLACK, 	MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_DBLACK);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_RED, 		MDDataSource.MDBaseDataTypes.TRAIL_SKIRUN_RED);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_SHRUB, 				MDDataSource.MDBaseDataTypes.AREA_SHRUB);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_WOODS, 				MDDataSource.MDBaseDataTypes.AREA_WOODS);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_TUNDRA, 			MDDataSource.MDBaseDataTypes.AREA_TUNDRA);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_PARK, 				MDDataSource.MDBaseDataTypes.AREA_PARK);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_SKIRESORT,			MDDataSource.MDBaseDataTypes.AREA_SKIRESORT);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.SKIRESORTSERVICE_INFO, 		MDDataSource.MDBaseDataTypes.POI_INFORMATION);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.SKIRESORTSERVICE_RESTAURANT,MDDataSource.MDBaseDataTypes.POI_RESTAURANT);
		mService2MDDataMapping.put(WorldObject.WorldObjectTypes.SKIRESORTSERVICE_WALKWAY, 	MDDataSource.MDBaseDataTypes.TRAIL_WALKWAY);

		for(WorldObject.WorldObjectTypes woType : mService2MDDataMapping.keySet()) {
			mMDData2ServiceMapping.put(mService2MDDataMapping.get(woType), woType);
		}
	}
	
	@Override
	public void AddCapabilities(ArrayList<Capability> capabilitiesArray) {
		for(WorldObject.WorldObjectTypes woType : mService2MDDataMapping.keySet()) {
			StaticMapDataCapability newDRCap = new StaticMapDataCapability(woType, mSourceID);
			capabilitiesArray.add(newDRCap);
		}
		InformationRetrievalCapability newIRCap = new InformationRetrievalCapability(InformationRetrievalCapability.InfoRetrievalTypes.CLOSEST_RESORTS, mSourceID);
		capabilitiesArray.add(newIRCap);
		
	}

	public void setDevTestingState(DevTestingState _devTestingState) {  // to propagate run-time changes to object
		super.setDevTestingState(_devTestingState);
		if(mDataSource != null) mDataSource.setDevTestingState(_devTestingState);
	}
	

//	//======================================
//
//		public void setMountainDynamicsID(WorldObject worldObject) {
//			switch(mType) {
//				case AREA: {
//					Terrain area = (Terrain) worldObject;
//					float sumX = 0.f;
//					float sumY = 0.f;
//					float cnt = 0.f;
//					for(PointXY point : area.mPolygonNodes) {
//						sumX += point.x;
//						sumY += point.y;
//						cnt ++;
//					}
//					area.mObjectID = String.format("MD_AREA%011d%011d", (int)(sumX/cnt *1000000), (int)(sumY/cnt *1000000));
//					Log.d(TAG, area.mObjectID);
//					break;
//				}
//				case TRAIL: {
//					Trail trail = (Trail) worldObject;
//					float sumX = 0.f;
//					float sumY = 0.f;
//					float cnt = 0.f;
//					for(PointXY point : trail.mTrailNodes) {
//						sumX += point.x;
//						sumY += point.y;
//						cnt ++;
//					}
//					trail.mObjectID = String.format("MD_TRAIL%011d%011d", (int)(sumX/cnt *1000000), (int)(sumY/cnt *1000000));
//					Log.d(TAG, trail.mObjectID);
//					break;
//				}
//				case POI: {
//					POI poi = (POI) worldObject;
//					poi.mObjectID = String.format("MD_POI%011d%011d", (int)(poi.mGPSLocation.x*1000000),  (int)(poi.mGPSLocation.y*1000000));
//					Log.d(TAG, poi.mObjectID);
//					break;
//				}
//			}
//		}
//		

	
	
	
	protected class InitMDDatabaseTask extends AsyncTask<Void, Void, String> {
		
//		private MDTranscoder mParentTranscoder = null;
//		private Context mContext = null;
//		DevTestingState mDevTestingState;
//		private RegionMapData mRegionMapData = null;
//		private MapDataCache mMapDataCache = null;
		
		public InitMDDatabaseTask() {
//			mParentTranscoder = parentTranscoder;
//			mContext = context;
//			mDevTestingState = _devTestingState;

//		  public InitMDDatabase(MDTranscoder parentTranscoder, MapDataCache mapDataCache, RegionMapData regionMapData) {
//			mMapDataCache = mapDataCache;
//			mRegionMapData = regionMapData;
		}
		
		protected String doInBackground(Void...voids) {

			try	{
				mDataSource = new MDDataSource(mSelf, mContext, mDevTestingState);
				return mDataSource.init();	// wait until MDDataSource wakes up and initializes
			}
			catch (Exception e) {
//				e.printStackTrace();
				return e.toString();
			}		 
		}

		protected void onPostExecute(String errorString) {
			if(errorString != null) { // error
				Log.e(TAG,"InitMDDatabaseTask error: " + errorString);
				mOwner.StaticMapDataTranscoderInitializeError(mSelf, TAG + " error:" + errorString);
			}
			else {
				Log.i(TAG,"InitMDDatabaseTask complete");
				mOwner.StaticMapDataTranscoderInitializeComplete(mSelf);
			}
		}

	}
	
	
	@Override
	public void SourceData(SourcedDataContainer sourcedDataContainer) throws Exception {
		// fills sourcedDataContainer with WorldObjects retrieved from the MDDataSource for tile index sourcedDataContainer.mRequestedGeoTileIndex

		
//		Log.e(TAG, "in MDTranscoder-SourceData");
		if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
				 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.MD_TRANSCODER_SOURCING_DATA_ERROR.ordinal()) == 1) ) {
					throw new Exception ("Forced MDTRANSCODER sourcing data error.");
		}

		
		//TODO remove background task.... all sourcing is now based on client background task
		
////		Log.e(TAG,"MDTranscoderSourceData 1");
//		mSourceMDDataTask = new SourceMDDataTask(sourcedDataContainer);
//		mSourceMDDataTask.execute();		// transcoder waits in background task for MDDataSource to be created and initialize
//
//	}
//
//	protected class SourceMDDataTask extends AsyncTask<Void, Void, String> {
//		
//		SourcedDataContainer mSourcedDataContainer; 
//		public SourceMDDataTask(SourcedDataContainer sourcedDataContainer) {
//			mSourcedDataContainer = sourcedDataContainer;
//		}
//		
//		protected String doInBackground(Void...voids) {
//
			try	{
				// build up array of query records
//				Log.e(TAG,"MDTranscoderSourceData 2");
				ArrayList<MDQueryRecord> dataQueryRecords = new ArrayList<MDQueryRecord>();
				for(WorldObject.WorldObjectTypes wodataType : sourcedDataContainer.mDataTypes) {
					MDDataSource.MDBaseDataTypes mdType = mService2MDDataMapping.get(wodataType);
					if(wodataType != null && mdType != null) {
						MDQueryRecord newDataQueryRecord = new MDQueryRecord(wodataType,  mdType);
						dataQueryRecords.add(newDataQueryRecord);
					}
				}

				if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
						 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.MD_SOURCE_SOURCING_DATA_ERROR.ordinal()) == 1) ) {
							throw new Exception ("Forced MDSOURCE sourcing data error.");
				}
				
//				Log.e(TAG,"MDTranscoderSourceData 3");
				
										// TODO, change next 2 lines to translate GeoTile index into MD tile index format (retrieved from MDDataSource.get)
				GeoRegion tileGR = GeoTile.GetGeoRegionFromTileIndex(sourcedDataContainer.mRequestedGeoTileIndex); // convert to source request data, ie geoRegion
				ArrayList<MDDataRecord> MDDataRecords = ((MDDataSource)mDataSource).SourceDataWithQuery(dataQueryRecords, tileGR);

//				Log.e(TAG,"MDTranscoderSourceData 4");
				// now decode MDDataRecords arraylist into arraylist of WorldObjects
				if(MDDataRecords == null) throw new Exception("Null record array returned from MDDataSource");  // it's okay to get no data back, but not a null array
				if(sourcedDataContainer.mRetrievedWorldObjects == null) {
					sourcedDataContainer.mRetrievedWorldObjects = new ArrayList<WorldObject>();
				}
				sourcedDataContainer.mRetrievedWorldObjects.removeAll(MDDataRecords);
				for(MDDataRecord dataRecord : MDDataRecords) {	
//					Log.i(TAG, "MDDataRecord: " + dataRecord.mName + ", " + dataRecord.mMDType);
					sourcedDataContainer.mRetrievedWorldObjects.add(TranslateDataRecordIntoWO(dataRecord, mSourceID));
				}	
//				Log.e(TAG,"MDTranscoderSourceData 5");
				return;
			}
			catch (Exception e) {
//				e.printStackTrace();
				return;
			}		 
		}

		protected WorldObject TranslateDataRecordIntoWO(MDDataRecord dataRecord, Capability.DataSources sourceID) throws Exception {
			WorldObject result = null; 
			
			switch(dataRecord.mMDType) {
				case AREA_SKIRESORT: {
//					Log.i(TAG, "NEW RESORT BACKGROUND - " + dataRecord.mName);
					return new Terrain_SkiResort(((MDAreaRecord)dataRecord).mPolylineNodes, sourceID);
				}
				case AREA_WOODS: {
					return new Terrain_Woods(((MDAreaRecord)dataRecord).mPolylineNodes, sourceID);
				}
				case AREA_SHRUB: {
					return new Terrain_Shrub(((MDAreaRecord)dataRecord).mPolylineNodes, sourceID);
				}
				case AREA_TUNDRA: {
					return new Terrain_Tundra(((MDAreaRecord)dataRecord).mPolylineNodes, sourceID);
				}
				case AREA_PARK: {
					return new Terrain_Park(((MDAreaRecord)dataRecord).mPolylineNodes, sourceID);
				}
				case TRAIL_SKIRUN_GREEN: {
					return new DownhillSkiTrail_Green(dataRecord.mName, ((MDTrailRecord)dataRecord).mPolylineNodes, DownhillDirection.LOWEST_POINT_IS_FIRST, sourceID);
				}
				case TRAIL_SKIRUN_BLUE: {
					return new DownhillSkiTrail_Blue(dataRecord.mName, ((MDTrailRecord)dataRecord).mPolylineNodes, DownhillDirection.LOWEST_POINT_IS_FIRST, sourceID);
				}
				case TRAIL_SKIRUN_BLACK: {
					return new DownhillSkiTrail_Black(dataRecord.mName, ((MDTrailRecord)dataRecord).mPolylineNodes, DownhillDirection.LOWEST_POINT_IS_FIRST, sourceID);
				}
				case TRAIL_SKIRUN_DBLACK: {
					return new DownhillSkiTrail_DBlack(dataRecord.mName, ((MDTrailRecord)dataRecord).mPolylineNodes, DownhillDirection.LOWEST_POINT_IS_FIRST, sourceID);
				}
				case TRAIL_SKIRUN_RED: {
					return new DownhillSkiTrail_Red(dataRecord.mName, ((MDTrailRecord)dataRecord).mPolylineNodes, DownhillDirection.LOWEST_POINT_IS_FIRST, sourceID);
				}
				case TRAIL_CHAIRLIFT: {
					return new Chairlift(dataRecord.mName, ((MDTrailRecord)dataRecord).mPolylineNodes, BottomOfLift.IS_FIRST_POINT, sourceID);
				}
				case TRAIL_ROADWAY: {
					return new Road_Artery_Tertiary(dataRecord.mName, ((MDTrailRecord)dataRecord).mPolylineNodes, sourceID);
				}
				case TRAIL_WALKWAY: {
					return new SkiResortService_Walkway(dataRecord.mName, ((MDTrailRecord)dataRecord).mPolylineNodes, sourceID);
				}
				case POI_RESTAURANT: {
					return new SkiResortService_Restaurant(dataRecord.mName, ((MDPOIRecord)dataRecord).mLocation, sourceID);
				}
				case POI_INFORMATION: {
					return new SkiResortService_Information(dataRecord.mName, ((MDPOIRecord)dataRecord).mLocation, sourceID);
				}
				case POI_PARKING: {
					return new CarAccess_Parking(dataRecord.mName, ((MDPOIRecord)dataRecord).mLocation, sourceID);
				}
				default:
					throw new Exception("Unknown MDDataRecord (type:" + dataRecord.mMDType + ") returned from MDDataSource.");
			}
			
		}
		
//		
//		protected void onPostExecute(String errorString) {
//			if(!errorString.equalsIgnoreCase("")) { // error
//				Log.e(TAG,"SourceMDDataTask error: " + errorString);
//				mSourcedDataContainer.mState = SourcedDataStates.LOADED;
//				mOwner.StaticMapDataTranscoderBackgroundSourcingDataError(mSourcedDataContainer, TAG + " error:" + errorString);
//			}
//			else {
//				Log.i(TAG,"SourceMDDataTask complete");
//				mSourcedDataContainer.mState = SourcedDataStates.ERROR;
//				mOwner.StaticMapDataTranscoderBackgroundSourcingDataComplete(mSourcedDataContainer);
//			}
//
//		}
//
//	}

	@Override
	public ArrayList<ResortInfoResponse> GetClosestResorts(ResortRequest resortRequest) {
		return mDataSource.GetClosestResorts(resortRequest);
	}

}
