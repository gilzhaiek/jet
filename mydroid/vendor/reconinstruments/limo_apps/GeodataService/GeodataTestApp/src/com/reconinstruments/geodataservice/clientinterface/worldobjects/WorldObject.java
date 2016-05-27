package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.Date;

import com.reconinstruments.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.geodataservice.clientinterface.capabilities.Capability;

public class WorldObject implements Serializable 
{
	private final static String TAG = "WorldObject";
	public final static int OBJECTID_NAME_LENGTH = 6;
	public final static String IDNAME_PADDING = "_______________________________";	// needs to be longer than OBJECTID_NAME_LENGTH
	public final static String UNKNOWN_ITEM_NAME = "__unknown__";

	public enum WorldObjectTypes {	// correspond to WorldObject subtypes
		NOT_DEFINED,
		CHAIRLIFT,
		CARACCESS_PARKING,
		CARACCESS_ROADWAY,
		DOWNHILLSKITRAIL_GREEN,
		DOWNHILLSKITRAIL_BLUE,
		DOWNHILLSKITRAIL_BLACK,
		DOWNHILLSKITRAIL_DBLACK,
		DOWNHILLSKITRAIL_RED,
		TERRAIN_SHRUB,
		TERRAIN_WOODS,
		TERRAIN_TUNDRA,
		TERRAIN_PARK,
		SKIRESORTSERVICE_INFO,
		SKIRESORTSERVICE_RESTAURANT,
		SKIRESORTSERVICE_WALKWAY
	}

	public enum WorldObjectStates {
		ACTIVE,
		INACTIVE
	}
	
	public String				mObjectID="";		// unique, reproducable ID based on object data
	public WorldObjectTypes		mType;
	public WorldObjectStates 	mState;
	public String				mName;
	public Capability.DataSources mDataSource;
	public Date					mCreatedOn;
	
//======================================
// constructors

	public WorldObject(WorldObjectTypes _type, String name, Capability.DataSources dataSource) {  
		mCreatedOn = new Date();
		mType = _type;
		mName = name;
		mState = WorldObjectStates.ACTIVE;		// always init active (makes obj creation simpler)
		mDataSource = dataSource;
		
	}
	
	public boolean InGeoRegion(GeoRegion geoRegion) {	
		return false;
	}
	

}
