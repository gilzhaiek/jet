package com.reconinstruments.geodataservice.datasourcemanager.Recon_Data.Base;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;

public class ReconBaseQueryRecord {
	private final static String TAG = "ReconBaseQueryRecord";

	WorldObject.WorldObjectTypes 	mServiceObjectType;
	ReconBaseDataSource.BaseDataTypes	mDataType;
	
	public ReconBaseQueryRecord(WorldObject.WorldObjectTypes superType, ReconBaseDataSource.BaseDataTypes dataType) {
		mServiceObjectType = superType;
		mDataType = dataType;
	}		

}
