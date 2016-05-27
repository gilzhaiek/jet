package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectStates;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;

public class DownhillSkiTrail extends WO_Polyline implements Serializable
{
	public enum DownhillDirection {
		NO_SLOPE_FLAT,
		LOWEST_POINT_IS_LAST,
		LOWEST_POINT_IS_FIRST
	}
	
	DownhillDirection	mDownhillDirection;
	
	
// constructors
	public DownhillSkiTrail(WorldObjectTypes _type, String name, ArrayList<PointXY> _nodes, DownhillDirection _downhillDir, Capability.DataSources dataSource) {
		super(_type, name, _nodes, dataSource);
		mDownhillDirection = _downhillDir;
	}
	

}
