package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.ArrayList;

import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;

public class Terrain_Park extends Terrain  implements Serializable 
{
	private final static String TAG = "Terrain_Park";

	public Terrain_Park(ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.TERRAIN_PARK, _nodes, dataSource);
	}
}
