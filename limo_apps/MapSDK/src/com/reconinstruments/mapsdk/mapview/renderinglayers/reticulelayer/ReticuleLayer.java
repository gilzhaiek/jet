package com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer;

import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.Log;

import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WO_POI;
import com.reconinstruments.mapsdk.mapview.MapView.MapViewMode;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.cameraposition.DynamicCameraPositionInterface.IDynamicCameraPosition;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.reticleitems.DynamicReticleItemsInterface;
import com.reconinstruments.mapsdk.mapview.renderinglayers.RenderingLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer.BackgroundSize;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.GLText;

public class ReticuleLayer extends RenderingLayer implements IDynamicCameraPosition {
// constants
	private final static String TAG = "ReticuleLayer";
	protected static final int MAX_NUM_OFFSCREEN_BUDDIES = 10;
	protected static final int RETICULE_DISTANCE = 61;

// members
	protected Bitmap			mUserIcon = null;
	protected Bitmap			mBuddyIcon = null;
	protected Bitmap			mReticuleImage = null;
	public boolean 				mRolloverNamingEnabled = false;
	protected Paint		    	mClosestItemBoxPaint;				// TODO remove when these items moved to their own layers
    protected Paint		    	mClosestItemBoxOutlinePaint;
    protected TextPaint			mClosestItemOutlinePaint;
    protected TextPaint			mClosestItemPaint;
    protected RollOverResult	mRollOverResult = null;
	protected int				mReticuleRadius = 24;
	protected Matrix			mDrawTransformMatrix = new Matrix();
	protected ArrayList<ReticleItem> 			mReticleItems = new ArrayList<ReticleItem>();
	protected float[] 			mDrawingViewPortCenter = new float[2];
	public  World2DrawingTransformer	mWorld2DrawingTransformer = null;
	protected RectF 			mDescRect = new RectF();
	protected PointXY			mReticleLocation = null;	// predefined var to avoid recalc during draw
	
	protected boolean			thisIsTheFirstRun = true;
	protected int				mMapImageSize = -1;
	protected int				mImageScaleMultiplier = -1;
	protected float				mReticuleScaleFactor = 0f;
	
// methods
	public ReticuleLayer(Activity parentActivity, RenderSchemeManager rsm, World2DrawingTransformer world2DrawingTransformer, boolean rolloverNamingEnabled, BackgroundSize bkgdSize) throws Exception {
		super(parentActivity, "Reticule", rsm);
		
		switch (bkgdSize) {
			case NORMAL: {
				mImageScaleMultiplier = 2;
				break;
			}
			case MINIMAL: {
				mImageScaleMultiplier = 1;
				break;
			}
		}
		mMapImageSize = 512 * mImageScaleMultiplier;
		
		mRolloverNamingEnabled = rolloverNamingEnabled;
		mReticuleImage = BitmapFactory.decodeResource(mParentActivity.getResources(), R.drawable.reticule);
		mWorld2DrawingTransformer = world2DrawingTransformer;
		
		// == TODO move the following to Rollover layer
		mClosestItemBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
		mClosestItemBoxPaint.setStyle(Style.FILL) ;
		mClosestItemBoxPaint.setColor(mRSM.GetPanRolloverBoxBGColor());		
		mClosestItemBoxPaint.setAlpha(mRSM.GetPanRolloverBoxBGAlpha());
		mClosestItemBoxPaint.setAntiAlias(true);
		
		mClosestItemBoxOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
		mClosestItemBoxOutlinePaint.setStyle(Style.STROKE) ;
		mClosestItemBoxOutlinePaint.setColor(mRSM.GetPanRolloverBoxOutlineColor());		
		mClosestItemBoxOutlinePaint.setAlpha(mRSM.GetPanRolloverBoxOutlineAlpha());
		mClosestItemBoxOutlinePaint.setStrokeWidth(mRSM.GetPanRolloverBoxOutlineWidth());
		mClosestItemBoxOutlinePaint.setAntiAlias(true);
		
		mClosestItemPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
        mClosestItemPaint.setTextSize(mRSM.GetPanRolloverTextSize());
		mClosestItemPaint.setAntiAlias(true);
		mClosestItemPaint.setTextAlign(Align.CENTER);
		mClosestItemPaint.setTypeface(Typeface.SANS_SERIF);
		mClosestItemPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));		
		mClosestItemPaint.setColor(mRSM.GetPanRolloverTextColor());		
		mClosestItemPaint.setAlpha(255);

		mClosestItemOutlinePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
		mClosestItemOutlinePaint.setTextSize(mRSM.GetPanRolloverTextSize());
		mClosestItemOutlinePaint.setTextAlign(Align.CENTER);
		mClosestItemOutlinePaint.setAntiAlias(true);
		mClosestItemOutlinePaint.setTypeface(Typeface.SANS_SERIF);
		mClosestItemOutlinePaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));		
		mClosestItemOutlinePaint.setColor(mRSM.GetPanRolloverTextOutlineColor());		
		mClosestItemOutlinePaint.setStyle(Paint.Style.STROKE);
		mClosestItemOutlinePaint.setStrokeWidth(mRSM.GetPanRolloverTextOutlineWidth());
		mClosestItemOutlinePaint.setAlpha(255);
		
		mDrawingViewPortCenter[0] = 0.f;
		mDrawingViewPortCenter[1] = 0.f;
	}
	
	public void ClearItems() {
		mReticleItems.clear();
	}
	
	public void AddItems(ArrayList<ReticleItem> itemList) {
		if(itemList != null) {
			mReticleItems.addAll(itemList);
		}
	}
	
	public class RollOverResult {
		public String mDescription=null;
		public double mDistance = -1;
		
		public RollOverResult(String desc, double dist) {
			mDescription = desc;
			mDistance = dist;
		}
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
		if(mEnabled) {
			// draw reticle
			mDrawTransformMatrix.setTranslate(mReticleLocation.x, mReticleLocation.y);
			canvas.drawBitmap(mReticuleImage, mDrawTransformMatrix, null);	
			
			// draw reticle items if they exist
			for(ReticleItem item : mReticleItems) {
				float diffX = item.mDrawingLocation.x-mDrawingViewPortCenter[0];
				float diffY = item.mDrawingLocation.y-mDrawingViewPortCenter[1];
				float mag = (float) Math.sqrt(diffX*diffX + diffY*diffY);
				float rx = diffX/mag * RETICULE_DISTANCE + mDrawingViewPortCenter[0];
				float ry = diffY/mag * RETICULE_DISTANCE + mDrawingViewPortCenter[1];
				canvas.drawBitmap(item.mImage, rx- item.mImage.getWidth()/2, ry-item.mImage.getHeight()/2, null);				// fixed user icon
			}
			
			// show rollover if enabled and an item in focus
			if(mRollOverResult != null) {	// put description of object with focus on map
				float xoffset = mClosestItemPaint.measureText(mRollOverResult.mDescription)/2.f;
				mDescRect.left = (float)(camera.mScreenWidthInPixels/2-(xoffset*1.2));
				mDescRect.top = (float)(camera.mScreenHeightInPixels-20-26);
				mDescRect.right = (float)(camera.mScreenWidthInPixels/2+(xoffset*1.2)); 
				mDescRect.bottom = (float)(camera.mScreenHeightInPixels-20+15);
				canvas.drawRect(mDescRect, mClosestItemBoxPaint);
				canvas.drawRect(mDescRect, mClosestItemBoxOutlinePaint);
				canvas.drawText(mRollOverResult.mDescription, camera.mScreenWidthInPixels/2, camera.mScreenHeightInPixels-20, mClosestItemOutlinePaint);
				canvas.drawText(mRollOverResult.mDescription, camera.mScreenWidthInPixels/2, camera.mScreenHeightInPixels-20, mClosestItemPaint);
			}

		}
	}
	
	public void SetRollover(WO_POI closestPOI, float distance) {
		if(closestPOI == null) {
			mRollOverResult = null;
		}
		else {
			mRollOverResult = new RollOverResult(closestPOI.mName, distance);
		}
	}
	@Override
	public void Draw(RenderSchemeManager rsm, Resources res, CameraViewport camera, String focusedObjectID, boolean loadNewTexture, MapViewMode viewMode, GLText glText) {		// for OpenGL based rendering
		// @Overide in subclass
		if(mEnabled) {
			if(thisIsTheFirstRun) Log.d(TAG, ">>>>>> Entering ReticleLayer!!");
			MeshGL reticleMesh = rsm.getMeshOfType(MeshGL.MeshType.RETICLE.ordinal());
			mReticuleScaleFactor = camera.getMaxZoomScale() * mMapImageSize/12;
			
			// draw reticle items if they exist
			for(ReticleItem item : mReticleItems) {
				float diffX = item.mDrawingLocation.x-mDrawingViewPortCenter[0];
				float diffY = item.mDrawingLocation.y-mDrawingViewPortCenter[1];
				float mag = (float) Math.sqrt(diffX*diffX + diffY*diffY);
				float rx = diffX/mag * mReticuleScaleFactor;
				float ry = diffY/mag * mReticuleScaleFactor;
				//need a mesh for reticle item
				item.mMesh = rsm.getMeshOfType(MeshGL.MeshType.RETICLE_ITEM.ordinal());
				android.opengl.Matrix.setIdentityM(item.mMesh.mModelMatrix, 0);
				item.mMesh.loadMeshTexture(item.mImage, loadNewTexture, 3);
				item.mMesh.drawMesh(camera.mCamViewMatrix, camera.mCamProjMatrix);
			}
						
			android.opengl.Matrix.setIdentityM(reticleMesh.mModelMatrix, 0);
			android.opengl.Matrix.translateM(reticleMesh.mModelMatrix, 0, 0, 0, 0.05f);
			android.opengl.Matrix.scaleM(reticleMesh.mModelMatrix, 0, mReticuleScaleFactor, mReticuleScaleFactor, mReticuleScaleFactor);
			reticleMesh.loadMeshTexture(mReticuleImage, thisIsTheFirstRun, 1);
			reticleMesh.drawMesh(camera.mCamViewMatrix, camera.mCamProjMatrix);
			
			if(thisIsTheFirstRun) thisIsTheFirstRun = false;
		}
	}
	
	@Override
	public void SetCameraPosition(CameraViewport camera) {		// when camera changes, recalc new drawing center
		PointXY drawingCameraViewportCenter = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(new PointXY((float)camera.mCurrentLongitude, (float)camera.mCurrentLatitude));
		
		mDrawTransformMatrix.setTranslate(-drawingCameraViewportCenter.x,-drawingCameraViewportCenter.y);
		mDrawTransformMatrix.postTranslate(camera.mScreenWidthInPixels/2.0f, camera.mScreenHeightInPixels/2.0f);

		mDrawingViewPortCenter[0] = (float)drawingCameraViewportCenter.x;
		mDrawingViewPortCenter[1] = (float)drawingCameraViewportCenter.y;
		mDrawTransformMatrix.mapPoints(mDrawingViewPortCenter);
		
		mReticleLocation = new PointXY(((float)camera.mScreenWidthInPixels-mReticuleImage.getWidth())/2.0f,((float)camera.mScreenHeightInPixels-mReticuleImage.getHeight())/2.0f);
	}
	
	@Override
	public void SetCameraHeading(float heading) {
	}
	
	@Override
	public void SetCameraPitch(float pitch){
		
	}

	@Override
	public void SetCameraPosition(CameraViewport camera, boolean forceUpdate) {
		// TODO Auto-generated method stub
		
	}
}
