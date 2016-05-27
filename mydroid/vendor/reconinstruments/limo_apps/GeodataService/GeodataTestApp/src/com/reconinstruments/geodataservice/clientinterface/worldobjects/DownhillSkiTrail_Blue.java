package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.reconinstruments.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectStates;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;

public class DownhillSkiTrail_Blue extends DownhillSkiTrail implements Serializable
{
	private final static String TAG = "DownhillSkiTrail_Blue";

// constructors
	public DownhillSkiTrail_Blue(String name, ArrayList<PointXY> _nodes, DownhillSkiTrail.DownhillDirection _downhillDir, Capability.DataSources dataSource) {
		super(WorldObjectTypes.DOWNHILLSKITRAIL_BLUE, name, _nodes, _downhillDir, dataSource);
		// calc mMainAngleFromNorth
	}
	

}
