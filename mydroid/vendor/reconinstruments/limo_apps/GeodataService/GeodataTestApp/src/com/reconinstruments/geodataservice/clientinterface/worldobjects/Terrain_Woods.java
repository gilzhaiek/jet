package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.ArrayList;

import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;

public class Terrain_Woods extends Terrain  implements Serializable 
{
	private final static String TAG = "Terrain_Woods";

	public Terrain_Woods(ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.TERRAIN_SHRUB, _nodes, dataSource);
	}
}
