package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;

public class CarAccess_Parking extends WO_POI  implements Serializable
{
	private final static String TAG = "CarAccess_Parking";

	public CarAccess_Parking(String name, PointXY _gpsLocation, Capability.DataSources dataSource) {
		super(WorldObjectTypes.CARACCESS_PARKING, name, _gpsLocation, dataSource);
	}

	
}
