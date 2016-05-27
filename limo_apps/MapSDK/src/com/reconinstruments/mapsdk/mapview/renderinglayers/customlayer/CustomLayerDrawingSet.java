package com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer;

import java.util.ArrayList;
import java.util.TreeMap;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;
import com.reconinstruments.mapsdk.mapview.WO_drawings.NoDataZoneDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.POIDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.WO_drawings.TerrainDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.TrailDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.renderinglayers.DrawingSet;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition.MapCompositionItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition.MapCompositionNameItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition.MapCompositionObjectItem;

public class CustomLayerDrawingSet extends DrawingSet {
// constants
	private final static String 	TAG = "CustomLayerDrawingSet";
	
// members
	public  World2DrawingTransformer	mWorld2DrawingTransformer = null;
	
	// data - all data is stored in paired buffers: current and alternate.  The value of super.mCurIndex defines the current set, and alternate is !current set
	public  float[]					mLoadedHeading = new float[2];		// info regarding next GeoRegion to load
	public  float[]					mLoadedScale = new float[2];
	public  GeoRegion[] 			mLoadedGeoRegion = new GeoRegion[2];						// used to draw data at current zoom scale
	public  GeoRegion[] 			mLoadedLowestResolutionGeoRegion = new GeoRegion[2];		// used to load data from geodata service at lowest resolution - speeds up scale redrawing

	public 	ArrayList<CustomPOIDrawing>	mPOI0 = new ArrayList<CustomPOIDrawing>(); // POI data
	public 	ArrayList<CustomPOIDrawing>	mPOI1 = new ArrayList<CustomPOIDrawing>(); 

	public 	ArrayList<CustomLineDrawing>	mLine0 = new ArrayList<CustomLineDrawing>(); // line data
	public 	ArrayList<CustomLineDrawing>	mLine1 = new ArrayList<CustomLineDrawing>(); 

	public 	ArrayList<CustomOverlayDrawing>	mOverlay0 = new ArrayList<CustomOverlayDrawing>(); // overlay data
	public 	ArrayList<CustomOverlayDrawing>	mOverlay1 = new ArrayList<CustomOverlayDrawing>(); 

	public 	float[]					mLatitude = new float[2];			// user position information
	public 	float[]					mLongitude = new float[2];	
	public 	float[]					mHeading = new float[2];		

	// predefined classes to speed up rendering
	PointXY					onDrawCameraViewportCurrentCenter = new PointXY(0.f, 0.f);
	PointXY 				mDrawingCameraViewportCenter =  new PointXY(0.f, 0.f);
	RectF 					mDrawingCameraViewportBoundary = new RectF(); 
	Matrix 					mPOIDrawTransform = new Matrix();
	Matrix 					mBGDrawTransform = new Matrix();
			
	int	cnt = 0;
			
// creator	/ init / release	
	public CustomLayerDrawingSet(RenderSchemeManager rsm) {
		super(rsm);
	}
	
	public boolean Init(World2DrawingTransformer world2DrawingTransformer, int imageSize) {
		mWorld2DrawingTransformer = world2DrawingTransformer;
		
		for(int i=0; i<2; i++) {
			mLoadState[i] = DrawingSetLoadState.INACTIVE;
			mLoadedScale[i] = 0.f;
			mLoadedHeading[i] = 0.f;
			mLoadedGeoRegion[i] = new GeoRegion();
			mLoadedLowestResolutionGeoRegion[i] = new GeoRegion();
		}

		return true;
	}
	
	public void release() {
	}

// Overridden methods
	@Override
	public void CancelLoad(boolean cancelLoad) {
		super.CancelLoad(cancelLoad);
	}
	
	@Override
	public void ResetAllParamters() {
		super.ResetAllParamters();
		mCurIndex = 0;
		mPOI0 = new ArrayList<CustomPOIDrawing>(); // POI data
		mPOI1 = new ArrayList<CustomPOIDrawing>(); 

		mLine0 = new ArrayList<CustomLineDrawing>(); // line data
		mLine1 = new ArrayList<CustomLineDrawing>(); 

		mOverlay0 = new ArrayList<CustomOverlayDrawing>(); // overlay data
		mOverlay1 = new ArrayList<CustomOverlayDrawing>(); 

	}
	
	public void HandleDrawingTransformerChange(World2DrawingTransformer world2DrawingTransformer) {
		
		mWorld2DrawingTransformer = world2DrawingTransformer; 
		for(CustomPOIDrawing drawing : mPOI0) {
			drawing.HandleDrawingTransformerChange(world2DrawingTransformer);
		}
		for(CustomPOIDrawing drawing : mPOI1) {
			drawing.HandleDrawingTransformerChange(world2DrawingTransformer);
		}
		for(CustomLineDrawing drawing : mLine0) {
			drawing.HandleDrawingTransformerChange(world2DrawingTransformer);
		}
		for(CustomLineDrawing drawing : mLine1) {
			drawing.HandleDrawingTransformerChange(world2DrawingTransformer);
		}
		for(CustomOverlayDrawing drawing : mOverlay0) {
			drawing.HandleDrawingTransformerChange(world2DrawingTransformer);
		}
		for(CustomOverlayDrawing drawing : mOverlay1) {
			drawing.HandleDrawingTransformerChange(world2DrawingTransformer);
		}
	}

	public void Draw(Canvas canvas, CameraViewport cameraViewport, String focusedObjectID, Resources res) { // called from mapview - do nothing.. changed Oct 2014 to draw as part of map2DLayer
	}

	public void Draw(Canvas canvas, float scale, Matrix bgTransform, Matrix poiTransform, String focusedObjectID, Resources res) { // called from mapRLDrawingSet:CreateNextImageFromObjects()
		
		float borderScale = 1.2f;		// provide border that's slightly larger to handle rotation and POIs on border
		RectF mDrawingCameraViewportTestBoundary = new RectF(mDrawingCameraViewportBoundary.left / borderScale, mDrawingCameraViewportBoundary.top / borderScale, mDrawingCameraViewportBoundary.right * borderScale, mDrawingCameraViewportBoundary.bottom * borderScale);

		float invScale = scale;
		
		mBGDrawTransform = bgTransform;
		
		// draw overlays, than lines, then points (rotated to user)
		for(CustomOverlayDrawing overlayDrawing : GetCurrentOverlay()) {
//			Log.i(TAG,"in overlay drawing");
			overlayDrawing.Draw(canvas, mBGDrawTransform, invScale);
		}

		for(CustomLineDrawing lineDrawing : GetCurrentLine()) {
//			Log.e(TAG,"in line drawing");
			lineDrawing.Draw(canvas, mBGDrawTransform, invScale);
		}
		
		mPOIDrawTransform = poiTransform;
		for(CustomPOIDrawing poiDrawing : GetCurrentPOI()) {
//			Log.i(TAG,"in point drawing");
			poiDrawing.Draw(canvas, mPOIDrawTransform, invScale);
		}
		

	}

// methods unique to DS subclass
// set value methods
	public void DefineNextMapToLoad(GeoRegion nextGeoRegion, GeoRegion nextLowestResolutionGeoRegion, float nextScale, float nextHeading) {
		SetNextGeoRegion(nextGeoRegion);
		SetNextLowestResolutionGeoRegion(nextLowestResolutionGeoRegion);
		SetNextScale(nextScale);
		SetNextHeading(nextHeading);
		SetLoadingIndicator(true);  // mark alternate set as "loading"
	}
	
	public void SetUserHeading(float userHeading) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		mHeading[nextIndex] = userHeading;
	}

	public void SetNextGeoRegion(GeoRegion nextGR) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		mLoadedGeoRegion[nextIndex] = new GeoRegion(nextGR);
	}
	
	public void SetNextLowestResolutionGeoRegion(GeoRegion nextGRLR) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		mLoadedLowestResolutionGeoRegion[nextIndex] = new GeoRegion(nextGRLR);
	}
	
	public void SetNextScale(float nextScale) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		mLoadedScale[nextIndex] = nextScale;
	}
	
	public void SetNextHeading(float nextHeading) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		mLoadedHeading[nextIndex] = nextHeading;
	}
	
	public void SetNextState(DrawingSetLoadState newState) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		mLoadState[nextIndex] = newState;
	}
	
	
	@Override
	public boolean SwitchIfUpdateReady() {
		if(mUpdateAvailable) {
			mLoadState[mCurIndex] = DrawingSetLoadState.INACTIVE;
			mCurIndex = (mCurIndex == 0) ? 1 : 0;
			mLoadState[mCurIndex] = DrawingSetLoadState.LOADED;
			mInitialized = true;			// true after any data is loaded and stays true forever
			mUpdateAvailable = false;
//			Log.d(TAG,"  Drawing set switched...");
			
			return true;
		}
		return false;
	}

// state query methods
	public boolean IsLoadingData() {
		return IsNextLoading();
	}

	
	public boolean IsNextLoading() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return (mLoadState[nextIndex] == DrawingSetLoadState.LOADING);
	}
		

	
// get value methods
	public ArrayList<CustomPOIDrawing> GetCurrentPOI() {
		if(mCurIndex == 0) {
			return mPOI0;
		}
		else {
			return mPOI1;
		}
	}

	public ArrayList<CustomLineDrawing> GetCurrentLine() {
		if(mCurIndex == 0) {
			return mLine0;
		}
		else {
			return mLine1;
		}
	}

	public ArrayList<CustomOverlayDrawing> GetCurrentOverlay() {
		if(mCurIndex == 0) {
			return mOverlay0;
		}
		else {
			return mOverlay1;
		}
	}

	public DrawingSetLoadState GetNextLoadState() {
		return GetNextState();
	}
	
	public GeoRegion GetLoadedGeoRegion() {
		return mLoadedGeoRegion[mCurIndex];
	}
	
	public float GetLoadedScale() {
		return mLoadedScale[mCurIndex];
	}
	
	public float GetLoadedHeading() {
		return mLoadedHeading[mCurIndex];
	}
	
	public GeoRegion GetNextGeoRegion() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return mLoadedGeoRegion[nextIndex];
	}
	
	public GeoRegion GetNextLowestResolutionGeoRegion() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return mLoadedLowestResolutionGeoRegion[nextIndex];
	}

	public float GetNextScale() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return mLoadedScale[nextIndex];
	}
	
	public float GetNextHeading() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return mLoadedHeading[nextIndex];
	}
	
	public DrawingSetLoadState GetNextState() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return mLoadState[nextIndex];
	}
	
	public PointXY GetUserLocation() {
		return (new PointXY(mLongitude[mCurIndex], mLatitude[mCurIndex]) );
	}
	
	public float GetHeading() {
		return mHeading[mCurIndex];
	}
	
	public ArrayList<WorldObjectDrawing> GetFocusableObjects() {		
		ArrayList<WorldObjectDrawing> resultList = new ArrayList<WorldObjectDrawing>();
		return resultList;

	}


	

}
