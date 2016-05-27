package com.reconinstruments.mapsdk.mapview.WO_drawings;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

import android.content.Context;
import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoTile;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.NoDataZone;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WO_POI;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WO_Polyline;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;

public class GeoTileDrawing {

	// constants
		private final static String TAG = "GeoTileDrawing";

	// members
		public int	mTileIndex = -1;
		public GeoRegion 	mTileBoundary;
		public ArrayList<WorldObjectDrawing> mPOIDrawingObjects = null;
		public ArrayList<WorldObjectDrawing> mImageDrawingObjects = null;
		public boolean mNoDataPlaceholder = true;
		public Date 	mLastUsed = null;
		WorldObject		mCurWorldObject = null;
		
	// constructors
		public GeoTileDrawing(Context context, GeoTile geoTile, TreeMap<String, WorldObjectDrawing> masterDrawingsList, World2DrawingTransformer world2DrawingTransformer, Date loadTimeStamp, boolean TrailLabelCapitalization) {
			try {
				
				mTileIndex = geoTile.mTileIndex;
				mTileBoundary = GeoTile.GetGeoRegionFromTileIndex(mTileIndex);		// used for data lookup

				mPOIDrawingObjects = new ArrayList<WorldObjectDrawing>();
				mImageDrawingObjects = new ArrayList<WorldObjectDrawing>();

				mLastUsed = loadTimeStamp;
//				if(geoTile.mWorldObjectArray.size() != 0) {
//					Log.d(TAG, "creating new GeoTileDrawing tile index#" + mTileIndex + " | " + geoTile.mWorldObjectArray.size() + " objects");
//				}
//				else {
//					Log.d(TAG, "creating new GeoTileDrawing tile index#" + mTileIndex + " | empty");
//				}
				
				mNoDataPlaceholder = geoTile.mNoDataPlaceholder;
				for(WorldObject worldObject : geoTile.mWorldObjectArray) {
					mCurWorldObject = worldObject;
					
					WorldObjectDrawing existingWODrawing = masterDrawingsList.get(worldObject.mObjectID);		// masterDrawingList used to minimize duplication of drawing objects
					if(existingWODrawing != null) {
						switch(existingWODrawing.mType) {
							case CARACCESS_PARKING: 
							case CHAIRLIFT_ACCESS:
							case SKIRESORTSERVICE_INFO:
							case SKIRESORTSERVICE_RESTAURANT: 
							case WASHROOM:
							case DRINKINGWATER: {
								mPOIDrawingObjects.add(existingWODrawing);
								existingWODrawing.mInstances ++;
								break;
							}
							case CHAIRLIFT: {
								mImageDrawingObjects.add(existingWODrawing);
								existingWODrawing.mInstances ++;
								
								WO_POI clRecord = new WO_POI(WorldObject.WorldObjectTypes.CHAIRLIFT_ACCESS, worldObject.mName, ((WO_Polyline) worldObject).mPolylineNodes.get(0), worldObject.mDataSource);
								
								existingWODrawing = masterDrawingsList.get(clRecord.mObjectID);		// have to add chairlift access points to all tiles... this should exist 
								if(existingWODrawing != null) {
									mPOIDrawingObjects.add(existingWODrawing);
									existingWODrawing.mInstances ++;
								}								
								break;
							}
							case WATERWAY:
							case BORDER_NATIONAL:
							case DOWNHILLSKITRAIL_GREEN:
							case DOWNHILLSKITRAIL_BLUE:
							case DOWNHILLSKITRAIL_BLACK:
							case DOWNHILLSKITRAIL_DBLACK:
							case DOWNHILLSKITRAIL_RED:
							case ROAD_FREEWAY:
							case ROAD_ARTERY_PRIMARY:
							case ROAD_ARTERY_SECONDARY:
							case ROAD_ARTERY_TERTIARY:
							case ROAD_RESIDENTIAL:
							case SKIRESORTSERVICE_WALKWAY:
							case TERRAIN_LAND:
							case TERRAIN_OCEAN:
							case TERRAIN_CITYTOWN:
							case TERRAIN_SKIRESORT:
							case TERRAIN_TUNDRA:
							case TERRAIN_PARK:
							case TERRAIN_SHRUB:
							case TERRAIN_WOODS:
							case NO_DATA_ZONE:{
								mImageDrawingObjects.add(existingWODrawing);
								existingWODrawing.mInstances ++;
								break;
							}
						} // end switch

					}
					else {	// new world object... create drawing object
						POIDrawing newPOIObj = null;
						PolylineDrawing newPolylineObj = null;
						
						switch(worldObject.mType) {
							case CARACCESS_PARKING: {
								newPOIObj = new POIDrawing(context, WorldObjectDrawing.WorldObjectDrawingTypes.CARACCESS_PARKING, (WO_POI)worldObject, world2DrawingTransformer);
								break;
							}
							case SKIRESORTSERVICE_INFO:{
								newPOIObj = new  POIDrawing(context, WorldObjectDrawing.WorldObjectDrawingTypes.SKIRESORTSERVICE_INFO, (WO_POI)worldObject, world2DrawingTransformer);
								break;
							}
							case SKIRESORTSERVICE_RESTAURANT: {
								newPOIObj = new  POIDrawing(context, WorldObjectDrawing.WorldObjectDrawingTypes.SKIRESORTSERVICE_RESTAURANT, (WO_POI)worldObject, world2DrawingTransformer);
								break;
							}
							case WASHROOM: {
								worldObject.mName = "Washroom";  // force consistent name
								newPOIObj = new  POIDrawing(context, WorldObjectDrawing.WorldObjectDrawingTypes.WASHROOM, (WO_POI)worldObject, world2DrawingTransformer);
								break;
							}
							case DRINKINGWATER: {
								worldObject.mName = "Drinking Water";
								newPOIObj = new  POIDrawing(context, WorldObjectDrawing.WorldObjectDrawingTypes.DRINKINGWATER, (WO_POI)worldObject, world2DrawingTransformer);
								break;
							}
		
							case CHAIRLIFT: {
								// add chairlift access at end of chairlift
								WO_POI clRecord = new WO_POI(WorldObject.WorldObjectTypes.CHAIRLIFT_ACCESS, worldObject.mName, ((WO_Polyline) worldObject).mPolylineNodes.get(0), worldObject.mDataSource);
								newPOIObj = new  POIDrawing(context, WorldObjectDrawing.WorldObjectDrawingTypes.CHAIRLIFT_ACCESS, clRecord, world2DrawingTransformer);
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.CHAIRLIFT, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
		
							case WATERWAY: {
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.WATERWAY, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
							case DOWNHILLSKITRAIL_GREEN:{
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.DOWNHILLSKITRAIL_GREEN, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
							case DOWNHILLSKITRAIL_BLUE:{
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.DOWNHILLSKITRAIL_BLUE, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
							case DOWNHILLSKITRAIL_BLACK:{
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.DOWNHILLSKITRAIL_BLACK, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
							case DOWNHILLSKITRAIL_DBLACK:{
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.DOWNHILLSKITRAIL_DBLACK, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
							case DOWNHILLSKITRAIL_RED:{
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.DOWNHILLSKITRAIL_RED, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
							
							case ROAD_FREEWAY:{
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.ROAD_FREEWAY, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
							case ROAD_ARTERY_PRIMARY:{
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.ROAD_ARTERY_PRIMARY, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
							case ROAD_ARTERY_SECONDARY:{
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.ROAD_ARTERY_SECONDARY, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
							case ROAD_ARTERY_TERTIARY:{
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.ROAD_ARTERY_TERTIARY, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
							case ROAD_RESIDENTIAL:{
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.ROAD_RESIDENTIAL, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
							
							case SKIRESORTSERVICE_WALKWAY: {
								newPolylineObj = new TrailDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.SKIRESORTSERVICE_WALKWAY, (WO_Polyline) worldObject, world2DrawingTransformer);
								break;
							}
		
							case TERRAIN_SKIRESORT:{
								newPolylineObj = new TerrainDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_SKIRESORT, (Terrain) worldObject, world2DrawingTransformer);
								break;
							}
							case TERRAIN_TUNDRA:{
								newPolylineObj = new TerrainDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_TUNDRA, (Terrain) worldObject, world2DrawingTransformer);
								break;
							}
							case TERRAIN_PARK:{
								newPolylineObj = new TerrainDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_PARK, (Terrain) worldObject, world2DrawingTransformer);
								break;
							}
							case TERRAIN_SHRUB:{
								newPolylineObj = new TerrainDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_SHRUB, (Terrain) worldObject, world2DrawingTransformer);
								break;
							}
							case TERRAIN_WOODS:{
								newPolylineObj = new TerrainDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_WOODS, (Terrain) worldObject, world2DrawingTransformer);
								break;
							}
							case TERRAIN_LAND:{
								newPolylineObj = new TerrainDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_LAND, (Terrain) worldObject, world2DrawingTransformer);
								break;
							}
							case TERRAIN_OCEAN:{
								newPolylineObj = new TerrainDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_OCEAN, (Terrain) worldObject, world2DrawingTransformer);
								break;
							}
//							case TERRAIN_CITYTOWN:{
//								newPolylineObj = new TerrainDrawing(WorldObjectDrawing.WorldObjectDrawingTypes.TERRAIN_CITYTOWN, (Terrain) worldObject, world2DrawingTransformer);
//								break;
//							}
							case NO_DATA_ZONE:{
								newPolylineObj = new NoDataZoneDrawing((NoDataZone) worldObject, world2DrawingTransformer);
								break;
							}
						} // end switch
						
						if(newPOIObj != null) {
							masterDrawingsList.put(newPOIObj.mDataObject.mObjectID, newPOIObj);
							mPOIDrawingObjects.add(newPOIObj);
							newPOIObj.mInstances = 1;
						}
						if(newPolylineObj != null) {
							if(TrailLabelCapitalization) {
								worldObject.mName = abbreviateStreetNames(worldObject.mName).toUpperCase(Locale.getDefault());
							} 
							else {
								worldObject.mName = abbreviateStreetNames(worldObject.mName);
							}
							masterDrawingsList.put(newPolylineObj.mDataObject.mObjectID, newPolylineObj);
							mImageDrawingObjects.add(newPolylineObj);
							newPolylineObj.mInstances = 1;
						}
					}
				}

			}
			catch (Exception e) {
				if(mPOIDrawingObjects != null) mPOIDrawingObjects.clear();
				if(mImageDrawingObjects != null) mImageDrawingObjects.clear();
				mTileBoundary = null;
				Log.e(TAG,  "Error decoding tile#"+ mTileIndex + " for object type " + mCurWorldObject.mType);
				mTileIndex = -1;		// mark as junk for GC
			}
		}

		/**
		 * Abbbreviates road names. <br><br>
		 * e.g. "Granville Street" becomes "Granville St"<br><br>
		 * 
		 * NOTE: The changes in street names here will be reflected
		 * in all future drawing events, since this is stored in cache
		 * @param streetName Name of the street to abbreviate
		 * @return the new abbreviated road name
		 */
		private String abbreviateStreetNames(String streetName){

			if(streetName.matches(".*[Ss]treet.*")) 			streetName = streetName.replaceAll("[Ss]treet", "St");
			else if(streetName.matches(".*[Bb]oulevard.*")) 	streetName = streetName.replaceAll("[Bb]oulevard", "Bvld");
			else if(streetName.matches(".*[Aa]venue.*")) 	streetName = streetName.replaceAll("[Aa]venue", "Ave");
			else if(streetName.matches(".*[Hh]ighway.*")) 	streetName = streetName.replaceAll("[Hh]ighway", "Hwy");
			else if(streetName.matches(".*[Aa]lley.*")) 	streetName = streetName.replaceAll("[Aa]lley", "Aly");
			else if(streetName.matches(".*[Aa]nex.*")) 	streetName = streetName.replaceAll("[Aa]nex", "Anx");
			else if(streetName.matches(".*[Rr]oad.*")) 	streetName = streetName.replaceAll("[Rr]oad", "Rd");

			return streetName;
		}
}
