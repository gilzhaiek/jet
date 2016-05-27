package com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base;

import java.util.ArrayList;
import java.util.TreeMap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.reconinstruments.geodataservice.SourcedDataContainer;
import com.reconinstruments.geodataservice.TileLoadingFromNetworkException;
import com.reconinstruments.geodataservice.datasourcemanager.DataSourceManager;
import com.reconinstruments.geodataservice.datasourcemanager.StaticMapDataTranscoder;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.datarecords.ReconBaseAreaRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.datarecords.ReconBaseDataRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.datarecords.ReconBasePOIRecord;
import com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base.datarecords.ReconBaseTrailRecord;
import com.reconinstruments.geodataservice.devinterface.DevTestingState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortInfoResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortRequest;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.StaticMapDataCapability;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.DrinkingWater;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Road_Artery_Primary;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Road_Artery_Secondary;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Road_Artery_Tertiary;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Road_Freeway;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Road_Residential;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_CityTown;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Land;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Ocean;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Park;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Water;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain_Woods;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Washroom;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Border_National;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Waterway;

public class ReconBaseTranscoder extends StaticMapDataTranscoder 
{
// constants 
	private final static String TAG = "ReconBaseTranscoder";
	
// members 
	public  ReconBaseTranscoder	mSelf = this;				// needed in init AsyncTask
	AsyncTask<Void, Void, String> mInitDatabaseTask = null;
	AsyncTask<Void, Void, String> mSourceOSMDataTask = null;
	private TreeMap<WorldObject.WorldObjectTypes, ReconBaseDataSource.BaseDataTypes>	mService2ReconDataMapping = new TreeMap<WorldObject.WorldObjectTypes, ReconBaseDataSource.BaseDataTypes>();		
	private TreeMap<ReconBaseDataSource.BaseDataTypes, WorldObject.WorldObjectTypes>	mReconDataMapping2Service = new TreeMap<ReconBaseDataSource.BaseDataTypes, WorldObject.WorldObjectTypes>();		
//	private TreeMap<ReconDataSource.ReconBaseDataTypes, WorldObject.WorldObjectTypes>	mOSMData2ServiceMapping = new TreeMap<ReconDataSource.ReconBaseDataTypes, WorldObject.WorldObjectTypes>();		
	
// methods
	public ReconBaseTranscoder(DataSourceManager dsm, Context context,DevTestingState _devTestingState) {
		super(Capability.DataSources.RECON_BASE, dsm, context, _devTestingState); 
	}
	
	public void init() throws Exception {
		super.init();
		mDataSource = new ReconBaseDataSource(mSelf, mContext, mDevTestingState);

		mInitDatabaseTask = new InitDatabaseTask();
		mInitDatabaseTask.execute();		// transcoder waits in background task for ReconDataSource to be created and initialize

		if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
			 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.OSM_TRANSCODER_CREATE_ERROR.ordinal()) == 1) ) {
				throw new Exception ("Forced RECONTRANSCODER creation error.");
		}
		
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.ROAD_FREEWAY, 				ReconBaseDataSource.BaseDataTypes.HIGHWAY_MOTORWAY);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.ROAD_ARTERY_PRIMARY, 		ReconBaseDataSource.BaseDataTypes.HIGHWAY_PRIMARY);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.ROAD_ARTERY_SECONDARY,		ReconBaseDataSource.BaseDataTypes.HIGHWAY_SECONDARY);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.ROAD_ARTERY_TERTIARY, 		ReconBaseDataSource.BaseDataTypes.HIGHWAY_TERTIARY);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.ROAD_RESIDENTIAL,	 		ReconBaseDataSource.BaseDataTypes.HIGHWAY_RESIDENTIAL);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_LAND, 				ReconBaseDataSource.BaseDataTypes.AREA_LAND);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_OCEAN,				ReconBaseDataSource.BaseDataTypes.AREA_OCEAN);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_CITYTOWN, 			ReconBaseDataSource.BaseDataTypes.AREA_CITYTOWN);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_WOODS, 				ReconBaseDataSource.BaseDataTypes.AREA_WOODS);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_PARK, 				ReconBaseDataSource.BaseDataTypes.AREA_PARK);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.TERRAIN_WATER, 				ReconBaseDataSource.BaseDataTypes.AREA_WATER);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.WASHROOM,	 				ReconBaseDataSource.BaseDataTypes.POI_WASHROOM);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.DRINKINGWATER,	 			ReconBaseDataSource.BaseDataTypes.POI_DRINKINGWATER);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.BORDER_NATIONAL,	 		 	ReconBaseDataSource.BaseDataTypes.LINE_NATIONAL_BORDER);
		mService2ReconDataMapping.put(WorldObject.WorldObjectTypes.WATERWAY,		 		 	ReconBaseDataSource.BaseDataTypes.WATERWAY);

		for(WorldObject.WorldObjectTypes woType : mService2ReconDataMapping.keySet()) {
			mReconDataMapping2Service.put(mService2ReconDataMapping.get(woType), woType);
		}
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.HIGHWAY_MOTORWAY, 		WorldObject.WorldObjectTypes.ROAD_FREEWAY);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.HIGHWAY_PRIMARY, 		WorldObject.WorldObjectTypes.ROAD_ARTERY_PRIMARY);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.HIGHWAY_SECONDARY, 	WorldObject.WorldObjectTypes.ROAD_ARTERY_SECONDARY);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.HIGHWAY_TERTIARY, 		WorldObject.WorldObjectTypes.ROAD_ARTERY_TERTIARY); 
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.HIGHWAY_RESIDENTIAL, 	WorldObject.WorldObjectTypes.ROAD_RESIDENTIAL); 
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.AREA_LAND, 			WorldObject.WorldObjectTypes.TERRAIN_LAND);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.AREA_OCEAN, 			WorldObject.WorldObjectTypes.TERRAIN_OCEAN);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.AREA_CITYTOWN,			WorldObject.WorldObjectTypes.TERRAIN_CITYTOWN);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.AREA_WOODS, 			WorldObject.WorldObjectTypes.TERRAIN_WOODS);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.AREA_PARK,				WorldObject.WorldObjectTypes.TERRAIN_PARK);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.AREA_WATER,			WorldObject.WorldObjectTypes.TERRAIN_WATER);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.AREA_FIELD,			WorldObject.WorldObjectTypes.TERRAIN_PARK);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.POI_WASHROOM,			WorldObject.WorldObjectTypes.WASHROOM);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.POI_DRINKINGWATER,		WorldObject.WorldObjectTypes.DRINKINGWATER);
//		mReconDataMapping2Service.put(ReconDataSource.ReconBaseDataTypes.BORDER_NATIONAL,		WorldObject.WorldObjectTypes.BORDER_NATIONAL);
//
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
			ArrayList<ReconBaseQueryRecord> dataQueryRecords = new ArrayList<ReconBaseQueryRecord>();
			ArrayList<WorldObject.WorldObjectTypes> testArray = sourcedDataContainer.mDataTypes;
			for(ReconBaseDataSource.BaseDataTypes dataType : mReconDataMapping2Service.keySet()) {
				WorldObject.WorldObjectTypes wodataType = mReconDataMapping2Service.get(dataType);
				boolean found = false;
				for(WorldObject.WorldObjectTypes type : testArray) {
					if(type == wodataType) {
						found = true;
						break;
					}
				}
				if(found) {
					ReconBaseQueryRecord newDataQueryRecord = new ReconBaseQueryRecord(wodataType, dataType);
					dataQueryRecords.add(newDataQueryRecord);
				}
				
			}

			if( (mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.ENABLED.ordinal()) == 1 && 
					 mDevTestingState.mTestingConditionState.get(DevTestingState.TestingConditions.OSM_SOURCE_SOURCING_DATA_ERROR.ordinal()) == 1) ) {
						throw new Exception ("Forced RECON DATA sourcing data error.");
			}
			
			// next line assume Geodata Service tile ID = tile ID  - TODO handle mismatch case in future
			ArrayList<ReconBaseDataRecord> ReconDataRecords = ((ReconBaseDataSource)mDataSource).SourceDataWithQuery(dataQueryRecords, sourcedDataContainer.mRequestedGeoTileIndex);

			// now decode ReconDataRecords arraylist into arraylist of WorldObjects
			if(ReconDataRecords == null) throw new Exception("Null record array returned from ReconBaseDataSource");  // it's okay to get no data back, but not a null array
			if(sourcedDataContainer.mRetrievedWorldObjects == null) {
				sourcedDataContainer.mRetrievedWorldObjects = new ArrayList<WorldObject>();
			}
			sourcedDataContainer.mRetrievedWorldObjects.removeAll(ReconDataRecords);
			for(ReconBaseDataRecord dataRecord : ReconDataRecords) {	
				sourcedDataContainer.mRetrievedWorldObjects.add(TranslateDataRecordIntoWO(dataRecord, mSourceID));
			}	
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

	protected WorldObject TranslateDataRecordIntoWO(ReconBaseDataRecord dataRecord, Capability.DataSources sourceID) throws Exception {
		WorldObject result = null; 
		
		
		switch(dataRecord.mDataType) {
			case AREA_LAND: {
				return new Terrain_Land(((ReconBaseAreaRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case AREA_OCEAN: {
				return new Terrain_Ocean(((ReconBaseAreaRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case AREA_CITYTOWN: {
				return new Terrain_CityTown(((ReconBaseAreaRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case AREA_WOODS: {
				return new Terrain_Woods(((ReconBaseAreaRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case AREA_WATER: {
				return new Terrain_Water(((ReconBaseAreaRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case AREA_FIELD: 
			case AREA_PARK: {
				return new Terrain_Park(((ReconBaseAreaRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case LINE_NATIONAL_BORDER: {
				return new Border_National(dataRecord.mName, ((ReconBaseTrailRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case WATERWAY: {
				return new Border_National(dataRecord.mName, ((ReconBaseTrailRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case HIGHWAY_MOTORWAY: {
				return new Road_Freeway(dataRecord.mName, ((ReconBaseTrailRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case HIGHWAY_PRIMARY: {
				return new Road_Artery_Primary(dataRecord.mName, ((ReconBaseTrailRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case HIGHWAY_SECONDARY: {
				return new Road_Artery_Secondary(dataRecord.mName, ((ReconBaseTrailRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case HIGHWAY_TERTIARY: {
				return new Road_Artery_Tertiary(dataRecord.mName, ((ReconBaseTrailRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case HIGHWAY_RESIDENTIAL: {
				return new Road_Residential(dataRecord.mName, ((ReconBaseTrailRecord)dataRecord).mPolylineNodes, sourceID);
			}
			case POI_WASHROOM: {
				return new Washroom(dataRecord.mName, ((ReconBasePOIRecord)dataRecord).mLocation, sourceID);
			}
			case POI_DRINKINGWATER: {
				return new DrinkingWater(dataRecord.mName, ((ReconBasePOIRecord)dataRecord).mLocation, sourceID);
			}
			default:
				Log.e(TAG, "Unknown ReconDataRecord (type:" + dataRecord.mDataType + ") returned from ReconDataSource.");
				throw new Exception("Unknown OSMDataRecord (type:" + dataRecord.mDataType + ") returned from ReconDataSource.");
		}
		
	}
	
	@Override
	public ArrayList<ResortInfoResponse> GetClosestResorts(ResortRequest resortRequest) {
		return mDataSource.GetClosestResorts(resortRequest);
	}

}
