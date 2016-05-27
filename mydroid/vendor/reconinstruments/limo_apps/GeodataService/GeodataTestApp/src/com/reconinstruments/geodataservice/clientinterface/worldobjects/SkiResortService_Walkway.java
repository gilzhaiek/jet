package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;

public class SkiResortService_Walkway extends WO_Polyline  implements Serializable
{
	private final static String TAG = "SkiResortService_Walkway";

	public SkiResortService_Walkway(String name, ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.SKIRESORTSERVICE_WALKWAY, name, _nodes, dataSource);
	}

	
}
