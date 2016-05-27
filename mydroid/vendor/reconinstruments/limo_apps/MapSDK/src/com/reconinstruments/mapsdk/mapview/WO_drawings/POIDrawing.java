package com.reconinstruments.mapsdk.mapview.WO_drawings;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WO_POI;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.GLHelper;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.ShaderGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.ShaderProgram;

public class POIDrawing extends WorldObjectDrawing {
	private static final long serialVersionUID = 1L;
	private final static String TAG = "POIDrawing";

	public PointXY  mLocation = new PointXY(0.f, 0.f);
	private static float[] mInverseVPMat = new float[16];
	private float[] mModelViewMat;
	private float[] mLocationWorld;
	private float[] mTransformedLocation;
	private float[] tempLocation;
	private int[] mViewport;
	private float[] mLocationWithOffset;
	private float mScaleSize;
	
	public MeshGL mMesh;
	private boolean mTextureLoaded = false;
	
	private float mRandZ;
	private boolean thisIsTheFirstRun = true;
	
	public POIDrawing(Context context, WorldObjectDrawingTypes type, WO_POI dataObject, World2DrawingTransformer world2DrawingTransformer) {
		super(type, (WorldObject)dataObject);
		
		if(world2DrawingTransformer != null) {
			mLocation = world2DrawingTransformer.TransformGPSPointToDrawingPoint(((WO_POI)mDataObject).mGPSLocation);
		}
		int	renderTypeVariantIndex = 0;
		switch(type) {
			case BUDDY: {
				renderTypeVariantIndex = RenderSchemeManager.BitmapTypes.BUDDY.ordinal();
				break;
			}
			case WASHROOM: {
				renderTypeVariantIndex = RenderSchemeManager.BitmapTypes.WASHROOM.ordinal();
				break;
			}			
			case DRINKINGWATER: {
				renderTypeVariantIndex = RenderSchemeManager.BitmapTypes.DRINKINGWATER.ordinal();
				break;
			}
			case CARACCESS_PARKING: {
				renderTypeVariantIndex = RenderSchemeManager.BitmapTypes.CARPARK.ordinal();
				break;
			}
			case CHAIRLIFT_ACCESS: {
				renderTypeVariantIndex = RenderSchemeManager.BitmapTypes.CHAIRLIFT.ordinal();
				break;
			}
			case SKIRESORTSERVICE_INFO: {
				renderTypeVariantIndex = RenderSchemeManager.BitmapTypes.INFORMATION.ordinal();
				break;
			}
			case SKIRESORTSERVICE_RESTAURANT: {
				renderTypeVariantIndex = RenderSchemeManager.BitmapTypes.RESTAURANT.ordinal();
				break;
			}
		}
		RenderSchemeManager.ObjectTypes renderType = RenderSchemeManager.ObjectTypes.BITMAP;
		super.setRendering(renderType, renderTypeVariantIndex);
		mLocationWorld = new float[4];
		mTransformedLocation = new float[4];
		tempLocation = new float[4];
		mViewport = new int[4];
		mModelViewMat = new float[16];
		mLocationWithOffset = new float[4];
	}
	
	public void setRendering(RenderSchemeManager.ObjectTypes renderType, int renderTypeVariantIndex) {
		super.setRendering(renderType, renderTypeVariantIndex);
	}
		
	public void Release(){
	}
	
	public void Draw(Canvas canvas, Resources res, RenderSchemeManager rsm, Matrix transformMatrix, float viewScale, boolean itemHasMapViewFocus, Resources res2) {

		float[] dl = new float[2];
		dl[0] = mLocation.x;
		dl[1] = mLocation.y;
		transformMatrix.mapPoints(dl);
		int bitmapVersionIndex = mState.ordinal();
		if(itemHasMapViewFocus) {
			bitmapVersionIndex ++;
		}
		Bitmap poiIcon = rsm.GetPOIBitmap(mRenderTypeVariantIndex, bitmapVersionIndex, viewScale);
		double bitmapVertOffsetPercent = (double)rsm.GetPOIBitmapOffsetPercent(mRenderTypeVariantIndex, bitmapVersionIndex, viewScale)/100.0;
//		if(itemHasMapViewFocus) {
//			Log.e(TAG,"bitmap offset: " + mType + " | " + bitmapVersionIndex + " | " + bitmapVertOffsetPercent);
//		}
		if(poiIcon != null) {
			if(itemHasMapViewFocus) {
				canvas.drawBitmap(poiIcon, dl[0] - poiIcon.getWidth()/2+1, dl[1] - (int)(poiIcon.getHeight() * (0.5 + bitmapVertOffsetPercent))+1, null);
			}
			else {
				canvas.drawBitmap(poiIcon, dl[0] - poiIcon.getWidth()/2+1, dl[1] - poiIcon.getHeight()/2+1, null);
			}
		}
	}
	
	public void Draw(RenderSchemeManager rsm, Matrix transformMatrix, CameraViewport camera, float pitch, float[] locationVectorMatrix, float[] offset, float mapImageSize, boolean itemHasMapViewFocus, boolean loadNewTexture){
		float angle = -camera.mCurrentRotationAngle;
		float viewScale = camera.mCurrentAltitudeScale;
		float[] viewMatrix = camera.mCamViewMatrix;
		float[] projMatrix = camera.mCamProjMatrix;
		mScaleSize = mapImageSize/8f;
		int bitmapVersionIndex = mState.ordinal();
		if(itemHasMapViewFocus) {
			bitmapVersionIndex ++;
//			mScaleSize = (camera.mTargetAltitudeScale == camera.getMaxZoomScale()) ? mapImageSize/12f : mScaleSize;
		}
		float bitmapVertOffsetPercent = (float)rsm.GetPOIBitmapOffsetPercent(mRenderTypeVariantIndex, bitmapVersionIndex, viewScale)/100f;
		
		mMesh = rsm.getPOIMesh(mRenderTypeVariantIndex, bitmapVersionIndex, viewScale);
		
		if (thisIsTheFirstRun) {
//			mRandZ = mMesh.getMeshBoundingRadius() + GLHelper.clamp((float) Math.random(), 0f, 0.1f);
			mRandZ = 0.02f;
		}
		mTransformedLocation[0] = mLocation.x;
		mTransformedLocation[1] = mLocation.y;
		mTransformedLocation[2] = 0;
		android.opengl.Matrix.multiplyMV(mTransformedLocation, 0, locationVectorMatrix, 0, mTransformedLocation, 0);
		android.opengl.Matrix.setIdentityM(mMesh.mModelMatrix, 0);
		android.opengl.Matrix.translateM(mMesh.mModelMatrix, 0, offset[0], offset[1], offset[2]);
		android.opengl.Matrix.rotateM(mMesh.mModelMatrix, 0, -angle, 0, 0, 1);
		android.opengl.Matrix.translateM(mMesh.mModelMatrix, 0, mTransformedLocation[0], -mTransformedLocation[1], 0f);
		android.opengl.Matrix.rotateM(mMesh.mModelMatrix, 0, angle, 0, 0, 1);
		android.opengl.Matrix.scaleM(mMesh.mModelMatrix, 0, mScaleSize, mScaleSize, mScaleSize);
		
		mLocationWithOffset[0] = mTransformedLocation[0] + offset[0];
		mLocationWithOffset[1] = mTransformedLocation[1] + offset[1];
		mLocationWithOffset[2] = mTransformedLocation[2] + offset[2];
		
		//offset along +Z so it wont intersect map mesh
		android.opengl.Matrix.translateM(mMesh.mModelMatrix, 0, 0, bitmapVertOffsetPercent, mRandZ);
		android.opengl.Matrix.rotateM(mMesh.mModelMatrix, 0, pitch, 1, 0, 0);
		
//		Log.d(TAG, "mesh bounding radius: " + mMesh.getMeshBoundingRadius());
//		if(loadNewTexture) Log.d(TAG, "mLocation : " + Float.toString(mTransformedLocation[0]) + ", " + Float.toString(mTransformedLocation[1]) );
		mMesh.enableAlphaTesting = true;
		mMesh.drawMesh(viewMatrix, projMatrix);
		if(thisIsTheFirstRun) thisIsTheFirstRun = false;
	}
	
	public float[] getTransformedLocation(){
		return mTransformedLocation;
	}
	
	/**
	 * This returns the location of the POI taking into account the offset due to
	 * panning in MapExplore mode.
	 * @return 
	 */
	public float[] getLocationWithOffset(){
		return mLocationWithOffset;
	}
	
	/**
	 * 
	 * @return the scale value used to transform this POI.
	 */
	public float getScaleSize(){
		return mScaleSize; 
	}
}
