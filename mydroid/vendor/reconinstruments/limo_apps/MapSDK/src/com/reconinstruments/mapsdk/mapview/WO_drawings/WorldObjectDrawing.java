package com.reconinstruments.mapsdk.mapview.WO_drawings;

import java.io.Serializable;
import java.util.Date;

import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;

public class WorldObjectDrawing implements Serializable 
{
	private final static String TAG = "WorldObjectDrawing";
	public final static int OBJECTID_NAME_LENGTH = 6;
	public final static String IDNAME_PADDING = "_______________________________";	// needs to be longer than OBJECTID_NAME_LENGTH
	public final static String UNKNOWN_ITEM_NAME = "__unknown__";

	public enum WO_Class {	// correspond to WorldObject subtypes
		TERRAIN,
		TRAIL,
		POI,
		NO_DATA_ZONE
	}

	public enum WorldObjectDrawingTypes {	// correspond to WorldObject subtypes
		NOT_DEFINED,
		CHAIRLIFT,
		CHAIRLIFT_ACCESS,	
		CARACCESS_PARKING,
		ROAD_FREEWAY,
		ROAD_ARTERY_PRIMARY,
		ROAD_ARTERY_SECONDARY,
		ROAD_ARTERY_TERTIARY,
		ROAD_RESIDENTIAL,
		DOWNHILLSKITRAIL_GREEN,
		DOWNHILLSKITRAIL_BLUE,
		DOWNHILLSKITRAIL_BLACK,
		DOWNHILLSKITRAIL_DBLACK,
		DOWNHILLSKITRAIL_RED,
		TERRAIN_LAND,
		TERRAIN_OCEAN,
		TERRAIN_CITYTOWN,
		TERRAIN_WOODS,
		TERRAIN_PARK,
		TERRAIN_WATER,
		TERRAIN_SHRUB,
		TERRAIN_TUNDRA,
		TERRAIN_SKIRESORT,
		SKIRESORTSERVICE_INFO,
		SKIRESORTSERVICE_RESTAURANT,
		SKIRESORTSERVICE_WALKWAY,
		BUDDY,
		USER,
		WASHROOM,
		DRINKINGWATER,
		NO_DATA_ZONE,
		BORDER_NATIONAL,
		WATERWAY
	}

	public enum WorldObjectDrawingStates {
		NORMAL,
		HAS_FOCUS,
		DISABLED,
		DISABLED_FOCUS
	}
	
	public enum CGState {
		NOT_DEFINED,
		GC,
		KEEP
	}
	
	public WorldObjectDrawingTypes	mType;
	public WorldObjectDrawingStates mState;
	public WorldObject				mDataObject;	// underlying data record for this drawable object
	public RenderSchemeManager.ObjectTypes	mRenderType;
	public int						mRenderTypeVariantIndex;
	public int						mInstances;			// number of loaded tiles referencing this object
	public long						mLastUsedTimeStamp;
	public CGState					mGCState = CGState.NOT_DEFINED;	// used during garbage collection
	
//======================================
// constructors

	public WorldObjectDrawing(WorldObjectDrawingTypes _type, WorldObject dataObject) {  
		mType = _type;
		mState = WorldObjectDrawingStates.NORMAL;		// always init normal (makes obj creation simpler)
		mDataObject = dataObject;
		mInstances = 0;						
		mLastUsedTimeStamp = new Date().getTime();
	}

	public void setRendering(RenderSchemeManager.ObjectTypes renderType, int renderTypeVariantIndex) {
		mRenderType = renderType;
		mRenderTypeVariantIndex = renderTypeVariantIndex;
	}

	public static WorldObject.WorldObjectTypes GetCorrespondingWorldObjectType(WorldObjectDrawingTypes type) {
		switch(type) {
			case CHAIRLIFT: 			return WorldObject.WorldObjectTypes.CHAIRLIFT; 
			case CARACCESS_PARKING: 	return WorldObject.WorldObjectTypes.CARACCESS_PARKING; 
			case ROAD_FREEWAY: 			return WorldObject.WorldObjectTypes.ROAD_FREEWAY; 
			case ROAD_ARTERY_PRIMARY: 	return WorldObject.WorldObjectTypes.ROAD_ARTERY_PRIMARY; 
			case ROAD_ARTERY_SECONDARY: return WorldObject.WorldObjectTypes.ROAD_ARTERY_SECONDARY; 
			case ROAD_ARTERY_TERTIARY: 	return WorldObject.WorldObjectTypes.ROAD_ARTERY_TERTIARY; 
			case ROAD_RESIDENTIAL: 		return WorldObject.WorldObjectTypes.ROAD_RESIDENTIAL; 
			case DOWNHILLSKITRAIL_GREEN:return WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_GREEN; 
			case DOWNHILLSKITRAIL_BLUE: return WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_BLUE; 
			case DOWNHILLSKITRAIL_BLACK:return WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_BLACK; 
			case DOWNHILLSKITRAIL_DBLACK: 	return WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_DBLACK; 
			case DOWNHILLSKITRAIL_RED: 	return WorldObject.WorldObjectTypes.DOWNHILLSKITRAIL_RED; 
			case TERRAIN_LAND: 			return WorldObject.WorldObjectTypes.TERRAIN_LAND; 
			case TERRAIN_OCEAN: 		return WorldObject.WorldObjectTypes.TERRAIN_OCEAN; 
			case TERRAIN_CITYTOWN: 		return WorldObject.WorldObjectTypes.TERRAIN_CITYTOWN; 
			case TERRAIN_WOODS: 		return WorldObject.WorldObjectTypes.TERRAIN_WOODS; 
			case TERRAIN_PARK: 			return WorldObject.WorldObjectTypes.TERRAIN_PARK; 
			case TERRAIN_WATER: 		return WorldObject.WorldObjectTypes.TERRAIN_WATER; 
			case TERRAIN_SHRUB: 		return WorldObject.WorldObjectTypes.TERRAIN_SHRUB; 
			case TERRAIN_TUNDRA: 		return WorldObject.WorldObjectTypes.TERRAIN_TUNDRA; 
			case TERRAIN_SKIRESORT: 	return WorldObject.WorldObjectTypes.TERRAIN_SKIRESORT; 
			case SKIRESORTSERVICE_INFO: return WorldObject.WorldObjectTypes.SKIRESORTSERVICE_INFO; 
			case SKIRESORTSERVICE_RESTAURANT: return WorldObject.WorldObjectTypes.SKIRESORTSERVICE_RESTAURANT; 
			case SKIRESORTSERVICE_WALKWAY: return WorldObject.WorldObjectTypes.SKIRESORTSERVICE_WALKWAY; 
			case WASHROOM: 				return WorldObject.WorldObjectTypes.WASHROOM; 
			case DRINKINGWATER: 		return WorldObject.WorldObjectTypes.DRINKINGWATER; 
			case NO_DATA_ZONE: 			return WorldObject.WorldObjectTypes.NO_DATA_ZONE; 
			case BORDER_NATIONAL: 		return WorldObject.WorldObjectTypes.BORDER_NATIONAL; 
			case WATERWAY: 				return WorldObject.WorldObjectTypes.WATERWAY; 
	
			default: return null;
		}
	}

	public static WO_Class ClassForDrawingType(WorldObjectDrawingTypes type) {
		switch(type) {
			case CHAIRLIFT: 			return WO_Class.TRAIL; 
			case CHAIRLIFT_ACCESS: 		return WO_Class.POI; 	
			case CARACCESS_PARKING: 	return WO_Class.POI; 
			case ROAD_FREEWAY: 			return WO_Class.TRAIL; 
			case ROAD_ARTERY_PRIMARY: 	return WO_Class.TRAIL; 
			case ROAD_ARTERY_SECONDARY: return WO_Class.TRAIL; 
			case ROAD_ARTERY_TERTIARY: 	return WO_Class.TRAIL; 
			case ROAD_RESIDENTIAL: 		return WO_Class.TRAIL; 
			case DOWNHILLSKITRAIL_GREEN:return WO_Class.TRAIL; 
			case DOWNHILLSKITRAIL_BLUE: return WO_Class.TRAIL; 
			case DOWNHILLSKITRAIL_BLACK:return WO_Class.TRAIL; 
			case DOWNHILLSKITRAIL_DBLACK: 	return WO_Class.TRAIL; 
			case DOWNHILLSKITRAIL_RED: 	return WO_Class.TRAIL; 
			case TERRAIN_LAND: 			return WO_Class.TERRAIN; 
			case TERRAIN_OCEAN: 		return WO_Class.TERRAIN; 
			case TERRAIN_CITYTOWN: 		return WO_Class.TERRAIN; 
			case TERRAIN_WOODS: 		return WO_Class.TERRAIN; 
			case TERRAIN_PARK: 			return WO_Class.TERRAIN; 
			case TERRAIN_WATER: 		return WO_Class.TERRAIN; 
			case TERRAIN_SHRUB: 		return WO_Class.TERRAIN; 
			case TERRAIN_TUNDRA: 		return WO_Class.TERRAIN; 
			case TERRAIN_SKIRESORT: 	return WO_Class.TERRAIN; 
			case SKIRESORTSERVICE_INFO: return WO_Class.POI; 
			case SKIRESORTSERVICE_RESTAURANT: return WO_Class.POI; 
			case SKIRESORTSERVICE_WALKWAY: return WO_Class.TRAIL; 
			case BUDDY: 				return WO_Class.POI; 
			case WASHROOM: 				return WO_Class.POI; 
			case DRINKINGWATER:			return WO_Class.POI; 
			case NO_DATA_ZONE: 			return WO_Class.NO_DATA_ZONE; 
			case BORDER_NATIONAL: 		return WO_Class.TRAIL; 
			case WATERWAY:		 		return WO_Class.TRAIL; 
	
			default: return WO_Class.TERRAIN;
		}
	}
	
	public static WorldObjectDrawing.WorldObjectDrawingTypes DrawingTypeForString(String typeNameStr) {

		WorldObjectDrawing.WorldObjectDrawingTypes	drawingType = null;
		if(typeNameStr.equalsIgnoreCase("land")) 				{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_LAND; };
		if(typeNameStr.equalsIgnoreCase("ocean")) 				{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_OCEAN; };
		if(typeNameStr.equalsIgnoreCase("city")) 				{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_CITYTOWN; };
		if(typeNameStr.equalsIgnoreCase("woods")) 				{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_WOODS; };
		if(typeNameStr.equalsIgnoreCase("park")) 				{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_PARK; };
		if(typeNameStr.equalsIgnoreCase("water")) 				{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_WATER; };
		if(typeNameStr.equalsIgnoreCase("shrub")) 				{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_SHRUB; };
		if(typeNameStr.equalsIgnoreCase("tundra")) 				{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_TUNDRA; };
		if(typeNameStr.equalsIgnoreCase("skiresort")) 			{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_SKIRESORT; };
		if(typeNameStr.equalsIgnoreCase("ski-info"))			{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.SKIRESORTSERVICE_INFO; };
		if(typeNameStr.equalsIgnoreCase("ski-restaurant"))		{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.SKIRESORTSERVICE_RESTAURANT; };
		if(typeNameStr.equalsIgnoreCase("ski-walkway"))			{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.SKIRESORTSERVICE_WALKWAY; };
		if(typeNameStr.equalsIgnoreCase("skitrail-green")) 		{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.DOWNHILLSKITRAIL_GREEN; };
		if(typeNameStr.equalsIgnoreCase("skitrail-blue")) 		{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.DOWNHILLSKITRAIL_BLUE; };
		if(typeNameStr.equalsIgnoreCase("skitrail-black")) 		{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.DOWNHILLSKITRAIL_BLACK; };
		if(typeNameStr.equalsIgnoreCase("skitrail-dblack")) 	{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.DOWNHILLSKITRAIL_DBLACK; };
		if(typeNameStr.equalsIgnoreCase("skitrail-red")) 		{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.DOWNHILLSKITRAIL_RED; };
		if(typeNameStr.equalsIgnoreCase("highway-motorway"))	{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.ROAD_FREEWAY; };
		if(typeNameStr.equalsIgnoreCase("highway-primary")) 	{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.ROAD_ARTERY_PRIMARY; };
		if(typeNameStr.equalsIgnoreCase("highway-secondary")) 	{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.ROAD_ARTERY_SECONDARY; };
		if(typeNameStr.equalsIgnoreCase("highway-tirtiary")) 	{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.ROAD_ARTERY_TERTIARY; };
		if(typeNameStr.equalsIgnoreCase("highway-residential")) { drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.ROAD_RESIDENTIAL; };
		if(typeNameStr.equalsIgnoreCase("parking_lot")) 		{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.CARACCESS_PARKING; };
		if(typeNameStr.equalsIgnoreCase("chairlift_access")) 	{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.CHAIRLIFT_ACCESS; };
		if(typeNameStr.equalsIgnoreCase("chairlift")) 			{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.CHAIRLIFT; };
		if(typeNameStr.equalsIgnoreCase("washroom")) 			{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.WASHROOM; };
		if(typeNameStr.equalsIgnoreCase("drinkingwater")) 		{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.DRINKINGWATER; };
		if(typeNameStr.equalsIgnoreCase("border-national")) 		{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.BORDER_NATIONAL; };
		if(typeNameStr.equalsIgnoreCase("waterway"))	 		{ drawingType = WorldObjectDrawing.WorldObjectDrawingTypes.WATERWAY; };
	
		return drawingType;
	}
}
