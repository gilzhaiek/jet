package com.reconinstruments.mapImages.mapdata;

// base for all MapObject types...GISObjects, Markers... things that can be added to a map background
// MapLayers hold groups of similar MapObjects

public class MapObject {
	private static final String TAG = "MapObject";

	public enum MapType {
		MAP_TYPE_NONE,
		MAP_TYPE_DRAWING,
		MAP_TYPE_SATELLITE,
		MAP_TYPE_HYBRID,
		MAP_TYPE_TERRAIN
	}

	public MapObject() {
		// TODO Auto-generated constructor stub
	}

}
