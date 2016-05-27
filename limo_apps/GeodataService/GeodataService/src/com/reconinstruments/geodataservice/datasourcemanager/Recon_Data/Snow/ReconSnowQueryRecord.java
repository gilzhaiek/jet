package com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Snow;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;

public class ReconSnowQueryRecord {
	private final static String TAG = "ReconBaseQueryRecord";

	WorldObject.WorldObjectTypes 	mServiceObjectType;
	ReconSnowDataSource.BaseDataTypes	mDataType;
	
	public ReconSnowQueryRecord(WorldObject.WorldObjectTypes superType, ReconSnowDataSource.BaseDataTypes dataType) {
		mServiceObjectType = superType;
		mDataType = dataType;
	}		

}
