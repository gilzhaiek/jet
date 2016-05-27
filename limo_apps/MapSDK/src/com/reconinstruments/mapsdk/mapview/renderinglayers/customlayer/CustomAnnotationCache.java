package com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer;

import java.util.ArrayList;
import java.util.TreeMap;

import android.graphics.Bitmap;
import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;

public class CustomAnnotationCache {
	private final static String TAG = "CustomAnnotationCache";
	
//	private final Semaphore mAccessToMasterDrawingsList = new Semaphore(1, true);

	public TreeMap<String, CustomPOIDrawing> mPointObjects = new TreeMap<String, CustomPOIDrawing>();
	public TreeMap<String, CustomLineDrawing> mLineObjects = new TreeMap<String, CustomLineDrawing>();
	public TreeMap<String, CustomOverlayDrawing> mOverlayObjects = new TreeMap<String, CustomOverlayDrawing>();
	
	public enum AnnotationErrorCode {	
		NO_ERROR,
		IMAGE_FILE_ACCESS_ERROR,
		DUPLICATE_ID_ERROR
	}

	public AnnotationErrorCode RemoveAllAnnotations() {
		mPointObjects.clear();
		mLineObjects.clear();
		mOverlayObjects.clear();
		return AnnotationErrorCode.NO_ERROR;
	}
//	public TreeMap<Integer, GeoTileDrawing> mLoadedTiles = new TreeMap<Integer, GeoTileDrawing>();	// 
//	public TreeMap<Integer, Integer> mLoadingTiles = new TreeMap<Integer, Integer>();				// TODO modified and in future can change to more efficient data structure
//	public ArrayList<WorldObjectDrawing> mPOIDrawingObjects = new ArrayList<WorldObjectDrawing>();		// stores POI drawings separate from background image related drawing objects
//	public ArrayList<WorldObjectDrawing> mImageDrawingObjects = new ArrayList<WorldObjectDrawing>();
//	public TreeMap<String, WorldObjectDrawing> mMasterDrawingsList	= new TreeMap<String, WorldObjectDrawing>();	// only used when newGeoTileDrawing() and GC
//	
	
	public AnnotationErrorCode AddPointAnnotation(String pointID, PointXY poiLocation, Bitmap image, int alpha, World2DrawingTransformer world2DrawingTransformer) {
		if(mPointObjects.get(pointID) != null) {
			return AnnotationErrorCode.DUPLICATE_ID_ERROR;
		}
		CustomPOIDrawing newPOI = new CustomPOIDrawing(pointID, poiLocation, image, alpha, world2DrawingTransformer);
		if(newPOI == null) {
			return AnnotationErrorCode.IMAGE_FILE_ACCESS_ERROR;
		}
		mPointObjects.put(pointID, newPOI);
//		Log.e(TAG, "Annotations: points="+mPointObjects.size() + ", overlays="+mOverlayObjects.size() + ", lines="+mLineObjects.size());
		return AnnotationErrorCode.NO_ERROR;
	}
	
	public AnnotationErrorCode RemovePointAnnotation(String objectID) {
		mPointObjects.remove(objectID);
		return AnnotationErrorCode.NO_ERROR;
	}
	
	public AnnotationErrorCode AddLineAnnotation(String lineID, ArrayList<PointXY> nodes, float lineWidthInM,  int color,  int alpha, World2DrawingTransformer world2DrawingTransformer) {
		if(mLineObjects.get(lineID) != null) {
			return AnnotationErrorCode.DUPLICATE_ID_ERROR;
		}
		CustomLineDrawing newLine = new CustomLineDrawing(lineID, nodes, lineWidthInM, color, alpha, world2DrawingTransformer);
		mLineObjects.put(lineID, newLine);
//		Log.e(TAG, "Annotations: points="+mPointObjects.size() + ", overlays="+mOverlayObjects.size() + ", lines="+mLineObjects.size());
		return AnnotationErrorCode.NO_ERROR;
	}
	
	public AnnotationErrorCode RemoveLineAnnotation(String objectID) {
		mLineObjects.remove(objectID);
		return AnnotationErrorCode.NO_ERROR;
	}
	
	public AnnotationErrorCode AddOverlayAnnotation(String overlayID, ArrayList<PointXY> nodes, int color,  int alpha, World2DrawingTransformer world2DrawingTransformer) {
		if(mOverlayObjects.get(overlayID) != null) {
			return AnnotationErrorCode.DUPLICATE_ID_ERROR;
		}
		CustomOverlayDrawing newOverlay = new CustomOverlayDrawing(overlayID,  nodes, color,  alpha, world2DrawingTransformer);
		mOverlayObjects.put(overlayID, newOverlay);
//		Log.e(TAG, "Annotations: points="+mPointObjects.size() + ", overlays="+mOverlayObjects.size() + ", lines="+mLineObjects.size());
		return AnnotationErrorCode.NO_ERROR;
	}
	
	public AnnotationErrorCode RemoveOverlayAnnotation(String objectID) {
		mOverlayObjects.remove(objectID);
		return AnnotationErrorCode.NO_ERROR;
	}
	
	
	public void HandleDrawingTransformerChange(World2DrawingTransformer world2DrawingTransformer) {
		for(CustomPOIDrawing drawing : mPointObjects.values()) {
			drawing.HandleDrawingTransformerChange(world2DrawingTransformer);
		}
		for(CustomLineDrawing drawing : mLineObjects.values()) {
			drawing.HandleDrawingTransformerChange(world2DrawingTransformer);
		}
		for(CustomOverlayDrawing drawing : mOverlayObjects.values()) {
			drawing.HandleDrawingTransformerChange(world2DrawingTransformer);
		}
	}
	
	public void LoadDrawingsInGeoRegion(CustomLayerDrawingSet customLayerDrawingSet) {
		GeoRegion loadingGeoRegion = customLayerDrawingSet.GetNextGeoRegion();	
//		float loadingScale = customLayerDrawingSet.GetNextScale();	
		
//		RectXY gr = loadingGeoRegion.mBoundingBox;
//		Log.d(TAG,"      LoadDrawingsInGeoRegion region = : " + gr.left + " "+ gr.right + " : "+ gr.top + " "+ gr.bottom);
		int nextIndex = (customLayerDrawingSet.mCurIndex == 0) ? 1 : 0;
		if(nextIndex == 0) {
			customLayerDrawingSet.mPOI0 = GetPOIObjectsFromCacheInGeoRegion(loadingGeoRegion);
			customLayerDrawingSet.mLine0 = GetLineObjectsFromCacheInGeoRegion(loadingGeoRegion);
			customLayerDrawingSet.mOverlay0 = GetOverlayObjectsFromCacheInGeoRegion(loadingGeoRegion);
//			Log.e(TAG, "Loaded annotations (index 0): points="+customLayerDrawingSet.mPOI0.size() + ", overlays="+customLayerDrawingSet.mOverlay0.size() + ", lines="+customLayerDrawingSet.mLine0.size());
		}
		else {
			customLayerDrawingSet.mPOI1 = GetPOIObjectsFromCacheInGeoRegion(loadingGeoRegion);
			customLayerDrawingSet.mLine1 = GetLineObjectsFromCacheInGeoRegion(loadingGeoRegion);
			customLayerDrawingSet.mOverlay1 = GetOverlayObjectsFromCacheInGeoRegion(loadingGeoRegion);
//			Log.e(TAG, "Loaded annotations (index 1): points="+customLayerDrawingSet.mPOI1.size() + ", overlays="+customLayerDrawingSet.mOverlay1.size() + ", lines="+customLayerDrawingSet.mLine1.size());
		}
	}
	
	public ArrayList<CustomPOIDrawing> GetPOIObjectsFromCacheInGeoRegion(GeoRegion geoRegion) {
		ArrayList<CustomPOIDrawing> objectsInGR = new ArrayList<CustomPOIDrawing>();
		
		for(CustomPOIDrawing object : mPointObjects.values()) {
			if(object.InGeoRegion(geoRegion)) {		
				objectsInGR.add(object);			
			}
		}
		return objectsInGR;
	}

	public ArrayList<CustomLineDrawing> GetLineObjectsFromCacheInGeoRegion(GeoRegion geoRegion) {
		ArrayList<CustomLineDrawing> objectsInGR = new ArrayList<CustomLineDrawing>();
		
		for(CustomLineDrawing object : mLineObjects.values()) {
			if(object.InGeoRegion(geoRegion)) {		
				objectsInGR.add(object);			
			}
		}
		return objectsInGR;
	}

	public ArrayList<CustomOverlayDrawing> GetOverlayObjectsFromCacheInGeoRegion(GeoRegion geoRegion) {
		ArrayList<CustomOverlayDrawing> objectsInGR = new ArrayList<CustomOverlayDrawing>();
		
		for(CustomOverlayDrawing object : mOverlayObjects.values()) {
			if(object.InGeoRegion(geoRegion)) {		
				objectsInGR.add(object);			
			}
		}
		return objectsInGR;
	}



}































