package com.reconinstruments.mapsdk.mapview.renderinglayers.gridlayer;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapview.MapView;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.cameraposition.DynamicCameraPositionInterface.IDynamicCameraPosition;
import com.reconinstruments.mapsdk.mapview.renderinglayers.RenderingLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.ShaderProgram;

public class GridLayer extends RenderingLayer  implements IDynamicCameraPosition {
// constants
	private final static String TAG = "GridLayer";

// members
	Paint	mLinePaint = new Paint();
	float   mCurrentCameraScale = 0.f;
	
// methods
	public GridLayer(Activity parentActivity, RenderSchemeManager rsm, World2DrawingTransformer world2DrawingTransformer) throws Exception {
		super(parentActivity, "Grid", rsm);
	}

	@Override
	public boolean CheckForUpdates() {
		return false;
	}

	@Override
	public boolean IsReady() {
		return true;
	}
	
	@Override
	public void Draw(Canvas canvas, CameraViewport camera, String focusedObjectID, Resources res) {
		mCurrentCameraScale = camera.mCurrentAltitudeScale;
		
		if(mEnabled && mCurrentCameraScale > 0.f) {
			int backgroundColor = mRSM.GetGridBkgdColor();
			canvas.drawColor(Color.argb(255, backgroundColor, backgroundColor, backgroundColor));

			int i;
			int lineColor = mRSM.GetGridLineColor();
			mLinePaint.setColor(Color.argb(mRSM.GetGridAlpha(), lineColor, lineColor, lineColor));
			mLinePaint.setStrokeWidth(mRSM.GetGridLineWidth());

			int stepSize;
			if(mCurrentCameraScale > 0.f) {
				stepSize = (int)(40 / mCurrentCameraScale);
			}
			else {
				stepSize = 40;
			}
			if(stepSize < 1) stepSize = 1;

			//		double angle = mCameraHeading % 90.0;
			//		if(angle > 45) angle -= 90.0;
			int offsetX = (int)(428 % stepSize / 2) - stepSize/2;
			int offsetY = (int)(240 % stepSize / 2);
			for(i=offsetX;i<428;i+=stepSize) {
				//			canvas.drawLine(i + (float)(Math.sin(angle)*120.0), 0, i- (float)(Math.sin(angle)*120.0), 240 , linePaint);
				canvas.drawLine(i, 0, i, 240, mLinePaint);
			}
			for(i=offsetY;i<240;i+=stepSize) {
				//			canvas.drawLine(0, i + (float)(Math.cos(angle)*214.0), 428, i - (float)(Math.cos(angle)*214.0), linePaint);
				canvas.drawLine(0, i , 428, i , mLinePaint);
			}
		}

	}
	@Override
	public void SetCameraPosition(CameraViewport camera) {		// when camera changes, recalc new drawing center
//		mCurrentCameraScale = camera.mCurrentAltitudeScale;
	}

	@Override
	public void SetCameraHeading(float heading) {
	}
	
	@Override
	public void SetCameraPitch(float pitch){
		
	}
	
	public void Draw(CameraViewport camera, String focusedObjectID, Resources res, ShaderProgram shaderProgram, MeshGL meshData, boolean loadNewTexture) {		// for OpenGL based rendering
		// @Overide in subclass
		if(mEnabled) {
			
		}
	}

	@Override
	public void SetCameraPosition(CameraViewport camera, boolean forceUpdate) {
		// TODO Auto-generated method stub
		
	}
}

