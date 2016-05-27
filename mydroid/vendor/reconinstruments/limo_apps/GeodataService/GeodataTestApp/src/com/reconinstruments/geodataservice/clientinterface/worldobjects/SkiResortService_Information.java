package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;

public class SkiResortService_Information extends WO_POI  implements Serializable
{
	private final static String TAG = "SkiResortService_Information";

	public SkiResortService_Information(String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(WorldObjectTypes.SKIRESORTSERVICE_INFO, name, _gpsLocation, dataSource);
	}

	
}
