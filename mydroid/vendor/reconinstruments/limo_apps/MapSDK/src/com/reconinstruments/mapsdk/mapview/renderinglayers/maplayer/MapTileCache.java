package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoTile;
import com.reconinstruments.mapsdk.mapview.WO_drawings.GeoTileDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.POIDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;

public class MapTileCache {
	private final static String TAG = "MapTileCache";
	private static final int NUM_SEMAPHORE_PERMITS = 50;
	private final Semaphore mAccessToChangeResources = new Semaphore(NUM_SEMAPHORE_PERMITS, true);

	// for Garbage Collection (GC)
	private static final int GARBAGE_COLLECTION_MAX_TILES_POST_GC = 50; // tile stays in cache if close to user or up to this time (in min)
	private static final long TILE_TEST_STALE_TIME_THRESHOLD_IN_MINUTES = 60; // tile stays in cache if close to user or up to this time (in min)
	private static final long TILE_TEST_STALE_DISTANCE_IN_KMS = 60; //  tile stays in cache if close to user or up to this time (in min)
	public static final double MERIDONIAL_CIRCUMFERENCE = 40007860.0; // m  - taken from wikipedia/Earth
	public static final double EQUITORIAL_CIRCUMFERENCE = 40075017.0; // m  - taken from wikipedia/Earth


	public TreeMap<Integer, GeoTileDrawing> 	mLoadedTiles = new TreeMap<Integer, GeoTileDrawing>();	// 
//	public TreeMap<Integer, Integer> 			mLoadingTiles = new TreeMap<Integer, Integer>();				// TODO modified and in future can change to more efficient data structure
//	public ArrayList<WorldObjectDrawing> mPOIDrawingObjects = new ArrayList<WorldObjectDrawing>();		// stores POI drawings separate from background image related drawing objects
//	public ArrayList<WorldObjectDrawing> mImageDrawingObjects = new ArrayList<WorldObjectDrawing>();
	public TreeMap<String, WorldObjectDrawing> 	mMasterDrawingsList	= new TreeMap<String, WorldObjectDrawing>();	// only used when newGeoTileDrawing() and GC

	public void AddGeoTile(Context context, GeoTile geoTile, World2DrawingTransformer world2DrawingTransformer, Date loadTimeStamp, boolean TrailLabelCapitalization) {
		if(geoTile != null) {
			GeoTileDrawing newGeoTileDrawing = null;

			try { mAccessToChangeResources.acquire(); } catch 	(InterruptedException e) {} { // if system fail on semaphore acquire, try anyway as it conflicts with CG are rare
				// multi-thread access, but all actions are additive to (atomic) map.get and put actions so no x-thread conflict
				newGeoTileDrawing = new GeoTileDrawing(context, geoTile, mMasterDrawingsList, world2DrawingTransformer, loadTimeStamp, TrailLabelCapitalization); 
				if(newGeoTileDrawing.mTileBoundary != null) {
					mLoadedTiles.put(geoTile.mTileIndex, newGeoTileDrawing);
				}
				else {
					Log.e(TAG, "Tile#" + geoTile.mTileIndex + " failed to load into the cache.");
				}
//				mLoadingTiles.remove(geoTile.mTileIndex);
			} mAccessToChangeResources.release();
			
		}
		
	}
	
	public ArrayList<Integer> CheckTiles(ArrayList<Integer> requiredList, Date loadTimeStamp) {
		ArrayList<Integer> missingList = new ArrayList<Integer>();

		try { mAccessToChangeResources.acquire(); } catch 	(InterruptedException e) {} { // if system fail on semaphore acquire, try anyway as it conflicts with CG are rare
			for(Integer tileIndex : requiredList) {
				boolean found = false;
				GeoTileDrawing tileDrawing = mLoadedTiles.get(tileIndex);
				if(tileDrawing == null) {
					missingList.add(tileIndex);
				}
				else {
					tileDrawing.mLastUsed = loadTimeStamp;		// touch time stamp of previously loaded tiles
				}
			}
		} mAccessToChangeResources.release();
		return missingList;
	}
	

	public TreeMap<Integer,ArrayList<WorldObjectDrawing>> GetImageObjectsFromCacheInGeoRegion(GeoRegion geoRegion, TreeMap<WorldObjectDrawing.WorldObjectDrawingTypes, ArrayList<Integer>> mapCompositionIndices) {

		TreeMap<String, WorldObjectDrawing> objectsInGR = new TreeMap<String, WorldObjectDrawing>();
		TreeMap<Integer,ArrayList<WorldObjectDrawing>> result = new TreeMap<Integer, ArrayList<WorldObjectDrawing>>();
		
		try { mAccessToChangeResources.acquire(); } catch 	(InterruptedException e) {} { // if system fail on semaphore acquire, try anyway as it conflicts with CG are rare
			ArrayList<Integer> requiredTiles =  GeoTile.GetTileListForGeoRegion(geoRegion, null);
			for(Integer tileIndex : requiredTiles) {
				GeoTileDrawing geoTileDrawing = mLoadedTiles.get(tileIndex);
				if(geoTileDrawing != null) {
					geoTileDrawing.mLastUsed = new Date();
					for(WorldObjectDrawing drawingObject : geoTileDrawing.mImageDrawingObjects) {
						if(objectsInGR.get(drawingObject.mDataObject.mObjectID) == null) {
							if(drawingObject.mDataObject.InGeoRegion(geoRegion)) {							// only take objects within tiles that intersect geoRegion
								objectsInGR.put(drawingObject.mDataObject.mObjectID, drawingObject);				//    and that haven't been added already from other tiles (tracked in objectsInGR)
								
								ArrayList<Integer> indexArray = mapCompositionIndices.get(drawingObject.mType);		//    place new objects in result structure based on their mapComposition index
								if(indexArray != null) {
									for(Integer index : indexArray) {		// usually only one object per array or object/name pair in array
										// put object in various arrays
										ArrayList<WorldObjectDrawing> curList = result.get(index);
										if(curList == null) {
											curList = new ArrayList<WorldObjectDrawing>();
											result.put(index, curList);
										}
										curList.add(drawingObject);
									}
								}
							}
						}
					}
				}
			}
		} mAccessToChangeResources.release();

		return result;
	}
	
	public TreeMap<Integer,ArrayList<POIDrawing>> GetPOIObjectsFromCacheInGeoRegion(TreeMap<WorldObjectDrawing.WorldObjectDrawingTypes, ArrayList<Integer>> mapCompositionIndices, GeoRegion geoRegion) {
		TreeMap<String, POIDrawing> objectsInGR = new TreeMap<String, POIDrawing>();
		TreeMap<Integer,ArrayList<POIDrawing>> result = new TreeMap<Integer,ArrayList<POIDrawing>>();
		
		try { mAccessToChangeResources.acquire(); } catch 	(InterruptedException e) {} { // if system fail on semaphore acquire, try anyway as it conflicts with CG are rare
			ArrayList<Integer> requiredTiles =  GeoTile.GetTileListForGeoRegion(geoRegion, null);
			for(Integer tileIndex : requiredTiles) {
				GeoTileDrawing geoTileDrawing = mLoadedTiles.get(tileIndex);
				if(geoTileDrawing != null) {
					geoTileDrawing.mLastUsed = new Date();
					for(WorldObjectDrawing object : geoTileDrawing.mPOIDrawingObjects) {
//						if(object.mDataObject.mName.equalsIgnoreCase("Whistler Village Gondola")) {
//							Log.e(TAG, "====  disappearing lift for tile#: " + tileIndex + " || " + object.mDataObject.mObjectID + " - " + ((WO_POI)object.mDataObject).mGPSLocation.x+ ", " + ((WO_POI)object.mDataObject).mGPSLocation.y);
//							Log.e(TAG, "              georegion: " + geoRegion.mBoundingBox.left + ","+ geoRegion.mBoundingBox.right + ","+ geoRegion.mBoundingBox.top + ","+ geoRegion.mBoundingBox.bottom);
//						}
						if(objectsInGR.get(object.mDataObject.mObjectID) == null) {
							if(object.mDataObject.InGeoRegion(geoRegion)) {							// only take objects within tiles that intersect geoRegion
								objectsInGR.put(object.mDataObject.mObjectID, (POIDrawing)object);				//    and that haven't been added already
		
								ArrayList<Integer> indexArray = mapCompositionIndices.get(object.mType);		//    place new POIs in result structure based on their mapComposition index
								if(indexArray != null) {
									for(Integer index : indexArray) {
										// put object in various arrays
										ArrayList<POIDrawing> curList = result.get(index);
										if(curList == null) {
											curList = new ArrayList<POIDrawing>();
											result.put(index, curList);
										}
										curList.add((POIDrawing)object);
									}
								}
		
							}
						}
					}
				}
			}
		} mAccessToChangeResources.release();
		return result;
	}

	public void CollectGarbage(int maxTilesPostGC) {
//	public void CollectGarbage(float userPositionLong, float userPositionLat, int maxTilesPostGC) {
		
		Log.d(TAG, "in map GC........");
//		if(mLoadedTiles.size() <= maxTilesPostGC) return;	// nothing to do
		
		try { 
			mAccessToChangeResources.acquire(NUM_SEMAPHORE_PERMITS);	// grab all permits, prevent other threads from modifying adding/removing objects from the list during GC
																		// given this is called during activity onPause, it's only previously loading objects that will get blocked
			
			ArrayList<Integer> drawingTilesToGC = new ArrayList<Integer>();
			ArrayList<String> drawingObjectsToGC = new ArrayList<String>();

			for(WorldObjectDrawing woDrawing : mMasterDrawingsList.values()) {
				woDrawing.mGCState = WorldObjectDrawing.CGState.GC;				// mark all drawing objects for GC, and below reset objects to keep (allows for many tiles to one drawing object)
			}
			
			ArrayList<GeoTileDrawing> tilelist = new ArrayList<GeoTileDrawing>(mLoadedTiles.values());	// create list of tiles and sort in ascending time order... last used tiles are last in list
			Collections.sort(tilelist, new Comparator<GeoTileDrawing>() {
				public int compare(GeoTileDrawing tile1, GeoTileDrawing tile2) {
				    return tile1.mLastUsed.compareTo(tile2.mLastUsed);	
				}
			});
			
			int numTiles = tilelist.size();
			int tileCnt = 0;
			for(GeoTileDrawing drawingTile : tilelist) {
				tileCnt++;
				if(numTiles <= maxTilesPostGC ) {										// for tiles to keep, mark all objects in tile as "keep"
					Log.d(TAG, " " + tileCnt + ": keeping tile#" + drawingTile.mTileIndex + " in cache" + " | lastUsed= " + drawingTile.mLastUsed);
					for(WorldObjectDrawing woDrawing : drawingTile.mPOIDrawingObjects) {
						woDrawing.mGCState = WorldObjectDrawing.CGState.KEEP;				
					}
					for(WorldObjectDrawing woDrawing : drawingTile.mImageDrawingObjects) {
						woDrawing.mGCState = WorldObjectDrawing.CGState.KEEP;
					}
				}
				else {
					Log.d(TAG, "  removing tile#" + drawingTile.mTileIndex + " from cache" + " | lastUsed= " + drawingTile.mLastUsed);
					drawingTilesToGC.add(drawingTile.mTileIndex);						// otherwise mark tile to discard (objects have previously been marked as GC unless kept by another tile)
					numTiles --;
				}
			}

																						// find all objects in master list still marked as GC
			for(WorldObjectDrawing woDrawing : mMasterDrawingsList.values()) {
				if(woDrawing.mGCState == WorldObjectDrawing.CGState.GC)	{
					drawingObjectsToGC.add(woDrawing.mDataObject.mObjectID);
				}
			}

																						// remove marked, old tiles
			for(Integer tileIndex : drawingTilesToGC) {
				mLoadedTiles.remove(tileIndex);
			}

																						// remove GC'd drawing objects
			for(String objectID : drawingObjectsToGC) {
				mMasterDrawingsList.remove(objectID);
			}
		
		} catch (InterruptedException e) {}	
		finally {
			mAccessToChangeResources.release(NUM_SEMAPHORE_PERMITS);
		}
	}
	
	public static float DistanceBetweenGPSPoints(float longitude1, float latitude1, float longitude2, float latitude2) {
		double horizDist = (double)((((double)longitude1-longitude2)/360.0) * EQUITORIAL_CIRCUMFERENCE*Math.cos(Math.toRadians((double)latitude1))); 
		double vertDist  = (double)(-((double)latitude1-latitude2)/360.0 * MERIDONIAL_CIRCUMFERENCE) ;  // == pixel coordinate  (-ve because in Android drawing/graphics Y is opposite real-world (GPS) Y direction);
		float  distInM   = (float) Math.sqrt(horizDist * horizDist + vertDist * vertDist);
		
		return distInM;
		
	}

}




