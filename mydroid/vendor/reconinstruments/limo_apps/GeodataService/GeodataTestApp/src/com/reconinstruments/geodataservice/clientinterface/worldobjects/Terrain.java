package com.reconinstruments.geodataservice.clientinterface.worldobjects;

import java.io.Serializable;
import java.util.ArrayList;

import com.reconinstruments.geodataservice.clientinterface.capabilities.DataRetrievalCapability;

public class Terrain extends WO_Polyline  implements Serializable 
{
	private final static String TAG = "Terrain";

	int	mColor;
	
	public Terrain(WorldObjectTypes _type, ArrayList<PointXY> _nodes, Capability.DataSources dataSource) {
		super(_type, "terrain", _nodes, dataSource);		// terrains don't have names by default
	}
}
