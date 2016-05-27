package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.ArrayList;

import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;

public class Terrain_Shrub extends Terrain  implements Serializable 
{
	public Terrain_Shrub(ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.TERRAIN_PARK, _nodes, dataSource);
	}
}
