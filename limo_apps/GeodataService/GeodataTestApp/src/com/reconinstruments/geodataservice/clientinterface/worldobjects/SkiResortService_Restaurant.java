package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;

public class SkiResortService_Restaurant extends WO_POI  implements Serializable
{
	private final static String TAG = "SkiResortService_Restaurant";

	public SkiResortService_Restaurant(String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(WorldObjectTypes.SKIRESORTSERVICE_RESTAURANT,  name, _gpsLocation, dataSource);
	}

	
}
