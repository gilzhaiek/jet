package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.ArrayList;

import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.DownhillSkiTrail.DownhillDirection;
import com.reconinstruments.geodataservice.clientinterface.worldobjects.WorldObject.WorldObjectTypes;

public class Chairlift extends WO_Polyline  implements Serializable 
{
	private final static String TAG = "Chairlift";
	
	public enum BottomOfLift {
		IS_FIRST_POINT,
		IS_LAST_POINT
	}

	BottomOfLift	mLiftStationPoint;

	public Chairlift(String name, ArrayList<PointXY> _nodes, BottomOfLift _isFirstLastPoint, Capability.DataSources dataSource) {
		super(WorldObjectTypes.CHAIRLIFT, name, _nodes, dataSource);
		mLiftStationPoint = _isFirstLastPoint;
	}
}
