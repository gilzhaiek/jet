package com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects;

import java.util.ArrayList;

public class NoDataZone extends WO_Polyline   
{
	private final static String TAG = "NoDataAvailable";

	
	public NoDataZone(String ndaID, ArrayList<PointXY> _nodes) {
		super(WorldObjectTypes.NO_DATA_ZONE, ndaID, _nodes, null);		
	}

    
//======================================
// methods

}
