package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition;

import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing.WO_Class;

public class MapCompositionItem {



	public WorldObjectDrawing.WorldObjectDrawingTypes 	mObjectType = WorldObjectDrawing.WorldObjectDrawingTypes.NOT_DEFINED;
	public MapCompositionItemConstraint					mDrawingConstraint = null;
	public WO_Class										mObjectClass = null;
	
	public MapCompositionItem(WorldObjectDrawing.WorldObjectDrawingTypes objType) {
		mObjectType = objType;
		mDrawingConstraint = new MapCompositionItemConstraint();
		mObjectClass = WorldObjectDrawing.ClassForDrawingType(objType);
	}
}
