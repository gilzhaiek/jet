package com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer;

import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapview.MapView.MapViewMode;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.cameraposition.DynamicCameraPositionInterface.IDynamicCameraPosition;
import com.reconinstruments.mapsdk.mapview.renderinglayers.RenderingLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.ShaderProgram;
import com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.GLText;

public class CustomLayer extends RenderingLayer implements  IDynamicCameraPosition {
// constants
	private final static String TAG = "CustomLayer";
	private static final boolean JET_DEMO = true; // temp hack to do jet demo	
	private static final int BACKGROUND_IMAGE_SIZE_BASE = 512;
	private static final float CAMERAVIEWPORT_PRELOAD_GEOREGION_MULTIPLIER = 0.4f;	// the size of GR loaded in the DrawingSet used as for preload testing
	private static final float CAMERAVIEWPORT_GEOREGION_LOAD_MULTIPLIER = 2.0f;		// the amount of extra data around the camera viewport to preload in the drawing set background image... calibrate to optimize UI smoothness  vs  memory usage
	

	
// members
	CameraViewport			mCameraViewport = null;
	World2DrawingTransformer mWorld2DrawingTransformer= null;
	CustomAnnotationCache	mCustomAnnotationCache = null;
	CustomLayerDrawingSet	mCustomLayerDrawingSet = null;

	

// methods
	public CustomLayer(Activity parentActivity, RenderSchemeManager rsm, World2DrawingTransformer world2DrawingTransformer) throws Exception {
		super(parentActivity, "Custom", rsm);
		mCustomLayerDrawingSet = new CustomLayerDrawingSet(rsm);	// create DrawingSet and share transformer
		mCustomAnnotationCache = new CustomAnnotationCache();
		mWorld2DrawingTransformer = world2DrawingTransformer;
		if(!mCustomLayerDrawingSet.Init(world2DrawingTransformer, (int)(BACKGROUND_IMAGE_SIZE_BASE * CAMERAVIEWPORT_GEOREGION_LOAD_MULTIPLIER))) {
			throw new Exception("Error generating MapRenderingLayer.  Issue allocating memory for background images.");
		}
	
		SetState(RenderingLayerState.READY);
	}
	
	
	public void Release() {
		super.Release();
	}
	
	public CustomAnnotationCache.AnnotationErrorCode RemoveAllAnnotations() {
		return mCustomAnnotationCache.RemoveAllAnnotations();
	}
	
	public CustomAnnotationCache.AnnotationErrorCode AddPointAnnotation(String poiID, PointXY poiLocation, Bitmap image, int alpha) {
		CustomAnnotationCache.AnnotationErrorCode rc = mCustomAnnotationCache.AddPointAnnotation(poiID, poiLocation, image, alpha, mWorld2DrawingTransformer);
//		if(rc == CustomAnnotationCache.AnnotationErrorCode.NO_ERROR) SetCameraPosition(mCameraViewport, true);	// force update through setCameraPosition()
		return rc;
	}
	
	public CustomAnnotationCache.AnnotationErrorCode AddLineAnnotation(String lineID, ArrayList<PointXY> nodes, float lineWidthInM,  int color,  int alpha) {
//		for(PointXY node : nodes) {
//			Log.d(TAG, "Node: " + node.x + ", " + node.y);
//		}
		CustomAnnotationCache.AnnotationErrorCode rc =  mCustomAnnotationCache.AddLineAnnotation(lineID, nodes, lineWidthInM, color, alpha, mWorld2DrawingTransformer);
//		if(rc == CustomAnnotationCache.AnnotationErrorCode.NO_ERROR) SetCameraPosition(mCameraViewport, true);	// force update through setCameraPosition()
//		else {
//			Log.e(TAG, ">>>>>> ERROR: could not add line annotation!");
//		}
		return rc;
	}
	
	public CustomAnnotationCache.AnnotationErrorCode AddOverlayAnnotation(String overlayID, ArrayList<PointXY> nodes, int color,  int alpha) {
		CustomAnnotationCache.AnnotationErrorCode rc =  mCustomAnnotationCache.AddOverlayAnnotation(overlayID,  nodes, color,  alpha, mWorld2DrawingTransformer);
//		if(rc == CustomAnnotationCache.AnnotationErrorCode.NO_ERROR) SetCameraPosition(mCameraViewport, true);	// force update through setCameraPosition()
		return rc;
	}
	
	public CustomAnnotationCache.AnnotationErrorCode RemovePointAnnotation(String objectID) {
		return mCustomAnnotationCache.RemovePointAnnotation(objectID);
	}

	public CustomAnnotationCache.AnnotationErrorCode RemoveLineAnnotation(String objectID) {
		return mCustomAnnotationCache.RemoveLineAnnotation(objectID);
	}

	public CustomAnnotationCache.AnnotationErrorCode RemoveOverlayAnnotation(String objectID) {
		return mCustomAnnotationCache.RemoveOverlayAnnotation(objectID);
	}

	
	@Override
	public boolean CheckForUpdates() {
		if(mEnabled) {
			boolean newData = mCustomLayerDrawingSet.SwitchIfUpdateReady();
			if(newData && mLayerState != RenderingLayerState.READY) {
				SetState(RenderingLayerState.READY);
			}
			return newData;
		}
		return false;
	}
	
	@Override
	public void SetCameraHeading(float heading) {
		
	}
	
	@Override
	public void SetCameraPitch(float pitch){
		
	}
	
	@Override
	public void SetCameraPosition(CameraViewport cameraViewport) {		// ReconMapView camera has changed... do we need to load new data for drawing??
		SetCameraPosition(cameraViewport, false);
	}

	// dynamic camera position interface routines
	public void SetCameraPosition(CameraViewport cameraViewport, boolean forceUpdate) {
		// implement logic to see if we need to update the DS?
		// Note, this method is called continuously from ReconMapView.onDraw() as the view is continuously redrawing at a fixed refresh rate

		if(mEnabled) {
			mCameraViewport = cameraViewport;
			if(mCustomLayerDrawingSet != null && cameraViewport != null){
				
				if(mCustomLayerDrawingSet.IsCancellingPrevLoadTask()) return;		// ignore camera movement if in middle of canceling prev load (ie, waiting for LoadDrawingSetTask.onCancelled() to be called)
				
				if(forceUpdate || !mCustomLayerDrawingSet.IsInitializedWithData()) {		
					if(!mCustomLayerDrawingSet.IsLoadingData()) {
//						Log.e(TAG, "in SetCameraPosition 3");
						UpdateDrawingSet(cameraViewport);
					}
				}
				else {
					float loadedScale = mCustomLayerDrawingSet.GetLoadedScale();
					GeoRegion preloadGeoRegion = mCustomLayerDrawingSet.GetLoadedGeoRegion().ScaledCopy(CAMERAVIEWPORT_PRELOAD_GEOREGION_MULTIPLIER);
					
					boolean test1 = !preloadGeoRegion.Contains((float)cameraViewport.mCurrentLongitude, (float)cameraViewport.mCurrentLatitude);
					boolean test2 = loadedScale < 0.97*cameraViewport.mTargetAltitudeScale || loadedScale > 1.1*cameraViewport.mTargetAltitudeScale;

					if(forceUpdate || test1 || test2 ){		// need to load new drawing set
//						Log.e(TAG, " loaded scale=" + loadedScale + ", " + cameraViewport.mTargetAltitudeScale);
				    	mCustomLayerDrawingSet.CancelLoad(true);		// stop any previous load
						UpdateDrawingSet(cameraViewport);
					}
					else {
						// nothing to do, current drawing set is suitable for viewport
					}
				}
			}
		}
	}

	public void HandleDrawingTransformerChange(World2DrawingTransformer world2DrawingTransformer) {
		mWorld2DrawingTransformer = world2DrawingTransformer; 
		mCustomLayerDrawingSet.HandleDrawingTransformerChange(world2DrawingTransformer);
		mCustomAnnotationCache.HandleDrawingTransformerChange(world2DrawingTransformer);
	}
	
	private void UpdateDrawingSet(CameraViewport cameraViewport) {
    	mCustomLayerDrawingSet.ResetLoadParamters();
		mCustomLayerDrawingSet.DefineNextMapToLoad(cameraViewport.mGeoRegionToSupportTargetView.ScaledCopy(CAMERAVIEWPORT_GEOREGION_LOAD_MULTIPLIER), 
				 cameraViewport.mGeoRegionToSupportTargetViewLowestResolution.ScaledCopy(CAMERAVIEWPORT_GEOREGION_LOAD_MULTIPLIER), 
				 cameraViewport.mTargetAltitudeScale, cameraViewport.mTargetRotationAngle);	

		mCustomAnnotationCache.LoadDrawingsInGeoRegion(mCustomLayerDrawingSet);
		
		mCustomLayerDrawingSet.mUpdateAvailable = true;

	}

	@Override
	public void Draw(Canvas canvas, CameraViewport camera, String focusedObjectID, Resources res) {
		// @Overide in subclass
		if(mEnabled) {
//			Log.e(TAG, "in Draw...........................");
			mCustomLayerDrawingSet.Draw(canvas, camera, focusedObjectID, res);
		}
	}
	public void Draw(Canvas canvas, float scale, Matrix transform, Matrix poiTransform, String focusedObjectID, Resources res) {
		// @Overide in subclass
		if(mEnabled) {
//			Log.e(TAG, "in Draw...........................");
			mCustomLayerDrawingSet.Draw(canvas, scale, transform, poiTransform, focusedObjectID, res);
		}
	}

	@Override
	public void Draw(RenderSchemeManager rsm, Resources res, CameraViewport camera, String focusedObjectID, boolean loadNewTexture, MapViewMode viewMode, GLText glText) {		// for OpenGL based rendering
		// @Overide in subclass
		return;	// now done in map layer
//		if(mEnabled) {
//			mCustomLayerDrawingSet.Draw(rsm, res, camera, focusedObjectID, loadNewTexture, exploreMode, glText);
//		}
	}
		

}
