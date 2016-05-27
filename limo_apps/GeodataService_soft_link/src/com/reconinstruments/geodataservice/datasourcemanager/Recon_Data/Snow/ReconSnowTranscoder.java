package com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow;

import java.util.ArrayList;
import java.util.TreeMap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.reconinstruments.geodataservice.SourcedDataContainer;
import com.reconinstruments.geodataservice.TileLoadingFromNetworkException;
import com.reconinstruments.geodataservice.datasourcemanager.DataSourceManager;
import com.reconinstruments.geodataservice.datasourcemanager.StaticMapDataTranscoder;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow.ReconSnowDataSource;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow.datarecords.ReconSnowAreaRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow.datarecords.ReconSnowDataRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow.datarecords.ReconSnowPOIRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow.datarecords.ReconSnowTrailRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.ReconBaseDataSource;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortInfoResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortRequest;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.StaticMapDataCapability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.CarAccess_Parking;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Chairlift;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail_Black;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail_Blue;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail_DBlack;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail_Green;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail_Red;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DrinkingWater;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Road_Artery_Primary;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Road_Artery_Secondary;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Road_Artery_Tertiary;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Road_Freeway;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Road_Residential;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.SkiResortService_Information;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.SkiResortService_Restaurant;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.SkiResortService_Walkway;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_CityTown;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Land;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Ocean;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Park;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Shrub;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_SkiResort;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Tundra;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Water;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Woods;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Washroom;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Border_National;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Chairlift.BottomOfLift;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DownhillSkiTrail.DownhillDirection;

public class ReconSnowTranscoder extends StaticMapDataTranscoder 
{
// constants 
	private final static String TAG = "ReconSnowTranscoder";
	
// members 
	public  ReconSnowTranscoder	mSelf = this;				// needed in init AsyncTask
	AsyncTask<Void, Void, String> mInitDatabaseTask = null;
	AsyncTask<Void, Void, String> mSourceOSMDataTask = null;
	private TreeMap<WorldObject.WorldObjectTypes, ReconSnowDataSource.BaseDataTypes>	mService2ReconDataMapping = new TreeMap<WorldObject.WorldObjectTypes, ReconSnowDataSource.BaseDataTypes>();		
	private TreeMap<ReconSnowDataSource.BaseDataTypes, WorldObject.WorldObjectTypes>	mReconDataMapping2Service = new TreeMap<ReconSnowDataSource.BaseDataTypes, WorldObject.WorldObjectTypes>();		
	
// methods
	public ReconSnowTranscoder(DataSourceManager dsm, Context context,DevTestingState _devTestingState) {
		super(Capability.DataSources.RECON_SNOW, dsm, context, _devTestingState); 
	}
	
	public void init() throws Exception {
		super.init();
		mDataSource = new ReconSnowDataSource(mSelf, mContext, mDevTestingState);

		mInitDatabaseTask = new InitDatabaseTask();
		mInitDatabaseTask.execute();		// transcoder waits in background task for ReconDataSource to be created and initialize

		if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
			 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.OSM_TRANSCODER_CREATE_ERROR.ordinal()) == 1) ) {
				throw new Exception ("Forced RECONTRANSCODER creation error.");
		}
		
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.CHAIRLIFT, 					ReconSnowDataSource.BaseDataTypes.LINE_CHAIRLIFT);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.CARACCESS_PARKING, 			ReconSnowDataSource.BaseDataTypes.POI_PARKING);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.ROAD_ARTERY_TERTIARY, 		ReconSnowDataSource.BaseDataTypes.LINE_ROADWAY);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_GREEN, 		ReconSnowDataSource.BaseDataTypes.LINE_SKIRUN_GREEN);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_BLUE, 		ReconSnowDataSource.BaseDataTypes.LINE_SKIRUN_BLUE);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_BLACK, 		ReconSnowDataSource.BaseDataTypes.LINE_SKIRUN_BLACK);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_DBLACK, 	ReconSnowDataSource.BaseDataTypes.LINE_SKIRUN_DBLACK);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_RED, 		ReconSnowDataSource.BaseDataTypes.LINE_SKIRUN_RED);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_WOODS, 				ReconSnowDataSource.BaseDataTypes.AREA_WOODS);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_PARK, 				ReconSnowDataSource.BaseDataTypes.AREA_PARK);
//		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_SHRUB, 				ReconSnowDataSource.BaseDataTypes.AREA_SHRUB);
//		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_TUNDRA, 				ReconSnowDataSource.BaseDataTypes.AREA_TUNDRA);
//		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_SKIRESORT,			ReconSnowDataSource.BaseDataTypes.AREA_SKIRESORT);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.SKIRESORTSERVICE_INFO, 		ReconSnowDataSource.BaseDataTypes.POI_INFORMATION);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.SKIRESORTSERVICE_RESTAURANT,	ReconSnowDataSource.BaseDataTypes.POI_RESTAURANT);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.SKIRESORTSERVICE_WALKWAY, 	ReconSnowDataSource.BaseDataTypes.LINE_WALKWAY);

		for(WorldObject.WorldObjectTypes woType : mService2ReconDataMapping.keySet()) {
			mReconDataMapping2Service.put(mService2ReconDataMapping.get(woType), woType);
		}
	}
	
	@Override
	public void AddCapabilities(ArrayList<Capability> capabilitiesArray) {
		for(WorldObject.WorldObjectTypes woType : mService2ReconDataMapping.keySet()) {
			StaticMapDataCapability newDRCap = new StaticMapDataCapability(woType, mSourceID);
			capabilitiesArray.add(newDRCap);
		}
//		InformationRetrievalCapability newIRCap = new InformationRetrievalCapability(InformationRetrievalCapability.InfoRetrievalTypes.CLOSEST_RESORTS, mSourceID);
//		capabilitiesArray.add(newIRCap);
		
	}

	public void setDevTestingState(DevTestingState _devTestingState) {  // to propagate run-time changes to object
		super.setDevTestingState(_devTestingState);
		if(mDataSource != null) mDataSource.setDevTestingState(_devTestingState);
	}
	


	
	protected class InitDatabaseTask extends AsyncTask<Void, Void, String> {
		
		public InitDatabaseTask() {
		}
		
		protected String doInBackground(Void...voids) {
			try	{
				return mDataSource.init();	// wait until OSMDataSource wakes up and initializes
			}
			catch (Exception e) {
				return e.toString();
			}		 
		}

		protected void onPostExecute(String errorString) {
			if(errorString != null) { // error
				Log.e(TAG,"InitDatabaseTask error: " + errorString);
				mOwner.StaticMapDataTranscoderInitializeError(mSelf, TAG + " error:" + errorString);
			}
			else {
				Log.i(TAG,"InitDatabaseTask complete");
				mOwner.StaticMapDataTranscoderInitializeComplete(mSelf);
			}
		}

	}
	
	
	@Override
	public void SourceData(SourcedDataContainer sourcedDataContainer) throws Exception {
		// fills sourcedDataContainer with WorldObjects retrieved from the OSMDataSource for tile index sourcedDataContainer.mRequestedGeoTileIndex

		
//		Log.e(TAG, "in OSMTranscoder-SourceData");
		if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
				 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.OSM_TRANSCODER_SOURCING_DATA_ERROR.ordinal()) == 1) ) {
					throw new Exception ("Forced OSMTRANSCODER sourcing data error.");
		}

		
		try	{
			// build up array of query records
//				Log.e(TAG,"OSMTranscoderSourceData 2");
			ArrayList<ReconSnowQueryRecord> dataQueryRecords = new ArrayList<ReconSnowQueryRecord>();
			ArrayList<WorldObject.WorldObjectTypes> testArray = sourcedDataContainer.mDataTypes;
			for(ReconSnowDataSource.BaseDataTypes dataType : mReconDataMapping2Service.keySet()) {
				WorldObject.WorldObjectTypes wodataType = mReconDataMapping2Service.get(dataType);
				boolean found = false;
				for(WorldObject.WorldObjectTypes type : testArray) {
					if(type == wodataType) {
						found = true;
						break;
					}
				}
				if(found) {
					ReconSnowQueryRecord newDataQueryRecord = new ReconSnowQueryRecord(wodataType, dataType);
					dataQueryRecords.add(newDataQueryRecord);
				}
				
			}

//			for(WorldObject.WorldObjectTypes wodataType : sourcedDataContainer.mDataTypes) {
//				OSMDataSource.ReconBaseDataTypes osmType = mService2ReconDataMapping.get(wodataType);
//				if(wodataType != null && osmType != null) {
//					OSMQueryRecord newDataQueryRecord = new OSMQueryRecord(wodataType, osmType);
//					dataQueryRecords.add(newDataQueryRecord);
//				}
//			}

			if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
					 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.OSM_SOURCE_SOURCING_DATA_ERROR.ordinal()) == 1) ) {
						throw new Exception ("Forced RECON DATA sourcing data error.");
			}
			
//				Log.e(TAG,"OSMTranscoderSourceData 3");
			
			// next line assume Geodata Service tile ID =  tile ID  - TODO handle mismatch case in future
			ArrayList<ReconSnowDataRecord> ReconDataRecords = ((ReconSnowDataSource)mDataSource).SourceDataWithQuery(dataQueryRecords, sourcedDataContainer.mRequestedGeoTileIndex);

//				Log.e(TAG,"OSMTranscoderSourceData 4 with " + ReconDataRecords.size() + " objects");
			// now decode ReconDataRecords arraylist into arraylist of WorldObjects
			if(ReconDataRecords == null) throw new Exception("Null record array returned from ReconDataSource");  // it's okay to get no data back, but not a null array
			if(sourcedDataContainer.mRetrievedWorldObjects == null) {
				sourcedDataContainer.mRetrievedWorldObjects = new ArrayList<WorldObject>();
			}
			sourcedDataContainer.mRetrievedWorldObjects.removeAll(ReconDataRecords);
			for(ReconSnowDataRecord dataRecord : ReconDataRecords) {	
//				Log.i(TAG, "OSMDataRecord: " + dataRecord.mName + ", " + dataRecord.mOSMType);
				sourcedDataContainer.mRetrievedWorldObjects.add(TranslateDataRecordIntoWO(dataRecord, mSourceID));
//				Log.e(TAG,"OSMTranscoderSourceData 4b");
			}	
//			  Log.e(TAG,"OSMTranscoderSourceData 5 with " + sourcedDataContainer.mRetrievedWorldObjects.size() + " objects");
			return;
		}
		catch (TileLoadingFromNetworkException e) {
			throw e;
		}		 
		catch (Exception e) {
//			e.printStackTrace();
//		Log.d(TAG,"unable to source data for query");
			return;
		}		 
	}

	protected WorldObject TranslateDataRecordIntoWO(ReconSnowDataRecord dataRecord, Capability.DataSources sourceID) throws Exception {
		WorldObject result = null; 
		
		
		switch(dataRecord.mDataType) {
//		case AREA_SKIRESORT: {
//			return new Terrain_SkiResort(((ReconSnowAreaRecord)dataRecord).mPolylineNodes, sourceID);
//		}
		case AREA_WOODS: {
			return new Terrain_Woods(((ReconSnowAreaRecord)dataRecord).mPolylineNodes, sourceID);
		}
//		case AREA_SHRUB: {
//			return new Terrain_Shrub(((ReconSnowAreaRecord)dataRecord).mPolylineNodes, sourceID);
//		}
//		case AREA_TUNDRA: {
//			return new Terrain_Tundra(((ReconSnowAreaRecord)dataRecord).mPolylineNodes, sourceID);
//		}
		case AREA_PARK: {
			return new Terrain_Park(((ReconSnowAreaRecord)dataRecord).mPolylineNodes, sourceID);
		}
		case LINE_SKIRUN_GREEN: {
			return new DownhillSkiTrail_Green(dataRecord.mName, ((ReconSnowTrailRecord)dataRecord).mPolylineNodes, DownhillDirection.LOWEST_POINT_IS_FIRST, sourceID);
		}
		case LINE_SKIRUN_BLUE: {
			return new DownhillSkiTrail_Blue(dataRecord.mName, ((ReconSnowTrailRecord)dataRecord).mPolylineNodes, DownhillDirection.LOWEST_POINT_IS_FIRST, sourceID);
		}
		case LINE_SKIRUN_BLACK: {
			return new DownhillSkiTrail_Black(dataRecord.mName, ((ReconSnowTrailRecord)dataRecord).mPolylineNodes, DownhillDirection.LOWEST_POINT_IS_FIRST, sourceID);
		}
		case LINE_SKIRUN_DBLACK: {
			return new DownhillSkiTrail_DBlack(dataRecord.mName, ((ReconSnowTrailRecord)dataRecord).mPolylineNodes, DownhillDirection.LOWEST_POINT_IS_FIRST, sourceID);
		}
		case LINE_SKIRUN_RED: {
			return new DownhillSkiTrail_Red(dataRecord.mName, ((ReconSnowTrailRecord)dataRecord).mPolylineNodes, DownhillDirection.LOWEST_POINT_IS_FIRST, sourceID);
		}
		case LINE_CHAIRLIFT: {
			return new Chairlift(dataRecord.mName, ((ReconSnowTrailRecord)dataRecord).mPolylineNodes, BottomOfLift.IS_FIRST_POINT, sourceID);
		}
		case LINE_ROADWAY: {  // duplicates base roadways so blocked
//			return new Road_Artery_Tertiary(dataRecord.mName, ((ReconSnowTrailRecord)dataRecord).mPolylineNodes, sourceID);
		}
		case LINE_WALKWAY: {
			return new SkiResortService_Walkway(dataRecord.mName, ((ReconSnowTrailRecord)dataRecord).mPolylineNodes, sourceID);
		}
		case POI_RESTAURANT: {
			return new SkiResortService_Restaurant(dataRecord.mName, ((ReconSnowPOIRecord)dataRecord).mLocation, sourceID);
		}
		case POI_INFORMATION: {
			return new SkiResortService_Information(dataRecord.mName, ((ReconSnowPOIRecord)dataRecord).mLocation, sourceID);
		}
		case POI_PARKING: {
			return new CarAccess_Parking(dataRecord.mName, ((ReconSnowPOIRecord)dataRecord).mLocation, sourceID);
		}
		default:
			Log.e(TAG, "Unknown ReconSnowDataRecord (type:" + dataRecord.mDataType + ") returned from ReconSnowDataSource.");
			throw new Exception("Unknown ReconSnowDataRecord (type:" + dataRecord.mDataType + ") returned from ReconSnowDataSource.");
		}
	}
	
	@Override
	public ArrayList<ResortInfoResponse> GetClosestResorts(ResortRequest resortRequest) {
		return mDataSource.GetClosestResorts(resortRequest);
	}

}
