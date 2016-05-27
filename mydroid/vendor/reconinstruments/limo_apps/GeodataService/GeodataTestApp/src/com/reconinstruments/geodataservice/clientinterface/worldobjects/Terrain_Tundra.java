package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.ArrayList;

import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;

public class Terrain_Tundra extends Terrain  implements Serializable 
{
	private final static String TAG = "Terrain_Tundra";

	public Terrain_Tundra( ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(WorldObjectTypes.TERRAIN_TUNDRA, _nodes, dataSource);
	}
}
