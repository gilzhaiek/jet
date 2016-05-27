package com.reconinstruments.mapsdk.mapview.renderinglayers;

import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Canvas;

import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.MapView.MapViewMode;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.ShaderProgram;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.GLText;

public class RenderingLayer {
// constants
	private final static String TAG = "RenderingLayer";
	
	public enum RenderingLayerState {
		INITIALIZING,
		ERROR_INITIALIZING,	
		WAITING_FOR_DATA_SERVICE,
		WAITING_FOR_LOCATION,
		PROBLEM_WITH_DATA_SERVICE,
		REQUIRED_DATA_UNAVAILABLE,	// service is running but does does not have desired capabilities to supply data
		OUT_OF_MEMORY,
		LOADING_DATA,
		ERROR_LOADING_DATA,
		READY
	}
	
// members
	public		RenderingLayerState 	mLayerState = 	RenderingLayerState.INITIALIZING;
	protected	boolean 				mEnabled = true;
	protected 	RenderSchemeManager 	mRSM = null;
	protected 	DrawingSet 				mDrawingSet = null;
	public		Activity				mParentActivity = null;
	public		String					mName=null;
	
	
// interfaces
	public interface ILayerStateFeedback {
		public void LayerStateChanged(RenderingLayer layer, RenderingLayerState layerState);		
	}
	
// methods
	public RenderingLayer(Activity parent, String layerName, RenderSchemeManager rsm) {
		mParentActivity = parent;
		mRSM = rsm;
		mName = layerName;
	}
	
	public void Create() {
		
	}
	public void Resume() {
		
	}
	public void Pause() {
		
	}
	
	public void Init(DrawingSet drawingSet) {
		// @Overide in subclass but call super.init
		mDrawingSet = drawingSet;
	}
	
	public void Release() {
		// @Overide in subclass but call super.release
	}
	
	public void Enable() {
		mEnabled = true;
	}
	
	public void Disable() {
		mEnabled = false;
	}
	
	public boolean IsEnabled() {
		return mEnabled;
	}
	
	public boolean IsReady() {
		return mLayerState == RenderingLayerState.READY;
	}
	
	public void ResetDrawingData() {
	
	}
	
	public boolean CheckForUpdates() {
		
		// override in subclass... something like...
		if(mEnabled) {
			boolean newData = mDrawingSet.SwitchIfUpdateReady();
			if(newData && mLayerState != RenderingLayerState.READY) {
				SetState(RenderingLayerState.READY);
			}
			return newData;
		}
		return false;
	}
	
	
	protected void SetState(RenderingLayerState newState) {
		mLayerState = newState;
	}
	

	
	
// api support
	public void Draw(Canvas canvas, CameraViewport camera, String focusedObjectID, Resources res) {
		// @Overide in subclass
		if(mEnabled) {
			
		}
	}
	

	public void Draw(RenderSchemeManager rsm, Resources res, CameraViewport camera, String focusedObjectID, boolean loadNewTexture, MapViewMode viewMode, GLText glText) {		// for OpenGL based rendering
		// @Overide in subclass
		if(mEnabled) {
			
		}
	}
		
// dynamic data apis
		// implement any dynamic data apis supported by renderingla
	
}
