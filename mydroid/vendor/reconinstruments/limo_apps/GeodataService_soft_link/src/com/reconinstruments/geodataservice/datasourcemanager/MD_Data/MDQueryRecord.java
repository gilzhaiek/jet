package com.reconinstruments.geodataservice.datasourcemanager.MD_Data;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;

public class MDQueryRecord {
	private final static String TAG = "MDQueryRecord";

	WorldObject.WorldObjectTypes 	mServiceObjectType;
	MDDataSource.MDBaseDataTypes	mMDType;
	
	public MDQueryRecord(WorldObject.WorldObjectTypes superType, MDDataSource.MDBaseDataTypes mdType) {
		mServiceObjectType = superType;
		mMDType = mdType;
	}		

}
