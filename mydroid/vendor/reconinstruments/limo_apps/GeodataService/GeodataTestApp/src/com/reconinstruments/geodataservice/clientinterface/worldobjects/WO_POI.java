package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;

import com.reconinstruments.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;

public class WO_POI extends WorldObject  implements Serializable
{
	private final static String TAG = "POI";

// memebers
	public PointXY		mGPSLocation;
	
// constructors
	public WO_POI(WorldObjectTypes _type, String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(_type, name, dataSource);
		mGPSLocation = _gpsLocation;
		
		SetObjectID();
	}

	@Override
	public boolean InGeoRegion(GeoRegion geoRegion) {
		return geoRegion.mBoundingBox.Contains(mGPSLocation.x, mGPSLocation.y);
	}
	
	public void SetObjectID() {	// virtual to be overwritten
		mObjectID = String.format("T%d_%s%011d%011d", mType.ordinal(), (mName+WorldObject.IDNAME_PADDING).substring(0, WorldObject.OBJECTID_NAME_LENGTH), (int)(mGPSLocation.x *1000000), (int)(mGPSLocation.y *1000000));
	}
	
}
