package com.reconinstruments.mapImages.mapdata;


public class MapLayer {
	private static final String TAG = "MapLayer";

	String		objectType = "";	// each layer holds specific types of GISObjects
	String		uiName = "";		// customizable name for layer, defaults to objectType
	boolean 	isVisible = false;
	boolean		isStatic = false;	// objects, can't be changed ... could be prerendered if static
	MapObject[]	objects = null;
	
	public MapLayer(String name) {
		// TODO Auto-generated constructor stub
		objectType = name;
		uiName = name;
		isVisible = false;
		objects = null;
	}
	
	public int addObject(MapObject object) {
		return 0;
	}
	public int removeObject(MapObject object) {
		return 0;
	}
	public int clearObjects() {
		return 0;
	}
	public boolean isVisible() {
		return isVisible;
	}
	public void showLayer() {
		isVisible = true;
	}
	public void hideLayer() {
		isVisible = false;
	}
	public void onDraw() {
		// loop through all GISObjects in layer and call their onDraw
	}

}
