package com.reconinstruments.mapImages.mapview;

import java.util.ArrayList;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.Log;

import com.reconinstruments.mapImages.drawings.MapDrawings;
import com.reconinstruments.mapImages.drawings.POIDrawing;
import com.reconinstruments.mapImages.drawings.RenderSchemeManager;
import com.reconinstruments.mapImages.objects.POI;
import com.reconinstruments.mapImages.prim.PointD;

public class DrawingSet {
	private final static String TAG = "DrawingSet";
	
//	public 	RectF					mRegionBoundsGPSCoords = null;
	public 	RectF					mRegionBoundsDrawingCoords = new RectF();
	public 	RectF					mRegionTestBoundsInGPS = new RectF();
	public 	RectF					mResortBounds = new RectF();
	public 	Bitmap					mBackgroundImage = null;
	public 	Canvas 					mBackgroundCanvas = null;
	public 	ArrayList<POIDrawing>	mPOIs = new ArrayList<POIDrawing>();
			Paint 					mResortBBPaint = new Paint();
			boolean					mLoaded = false;
			float					mDrawingScale = 0.0f;
			int						mImageSize = 0;	
			Matrix 					mLoadTransform = new Matrix();
			Matrix 					mRotateOffsetTransform = new Matrix();
			Matrix 					mBGDrawTransform = new Matrix();
			Matrix 					mPOIDrawTransform = new Matrix();
	public  boolean					mCancelLoad = false;
			PointD 					mRegionCenter = new PointD();
			float[] 				mOffset = new float[2];
	public	double					mLoadedMapHeading = 0.0;

//	public DrawingSet(int imageSize) {
//		// TODO Auto-generated constructor stub
//		NewDrawingCanvas(imageSize);
//	}
//
	public DrawingSet() {
		// TODO Auto-generated constructor stub
	}

//	public void UnloadResources() {
//		if(mBackgroundImage != null)  {
//			mBackgroundImage.recycle();
//			mBackgroundCanvas = null;
//		}
//		mLoaded = false;
//	}

	public boolean InitDrawingCanvas(int imageSize) {
		try {
			if(mBackgroundImage == null || mBackgroundCanvas == null) {
				ReleaseImageResources();	// remove old bits if they exist
				Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
				mBackgroundImage = Bitmap.createBitmap(imageSize, imageSize, conf); // this creates a MUTABLE bitmap
				mBackgroundCanvas = new Canvas(mBackgroundImage);
			}
		}
		catch (Exception e) {
			ReleaseImageResources();
			return false; // fail
		}
		return true; // success 
	}
	
	public void ReleaseImageResources() {
		if(mBackgroundImage != null) mBackgroundImage.recycle();
		mBackgroundImage = null;
		mBackgroundCanvas = null;
		mLoaded = false;
	}

	public boolean IsLoaded() {
		return mLoaded;
	}

	public int ImageSize() {
		return mImageSize;
	}

	public void Load(MapDrawings mapDrawing, RectF mResortBBInView, float imageSize, double mapHeading, RectF drawingGeoRegionBoundary, PointD loadingGeoRegionCenter, RenderSchemeManager rsm, Matrix mLoadTransform, float viewScale, Resources res, boolean showAllNames) {
		// draw DrawingSet background on bitmap
		// first draw backdrop for resort
		mRegionBoundsDrawingCoords.set(drawingGeoRegionBoundary);	// save copy of region depicted by boundary
		mRegionCenter.x = loadingGeoRegionCenter.x;
		mRegionCenter.y = loadingGeoRegionCenter.y;
		mDrawingScale = viewScale;
		mLoadedMapHeading = mapHeading;
//		if(mBackgroundImage == null || mBackgroundCanvas == null) {		// on first time, create bitmap and canvas... 
//			mImageSize = (int) imageSize;
//			NewDrawingCanvas(mImageSize);
//			Log.i(TAG, "New image "+ (int)imageSize);
//		}

		mBackgroundCanvas.drawColor( 0, PorterDuff.Mode.CLEAR); 
//		mBackgroundCanvas.drawColor(Color.YELLOW); 
		
		//first draw white background {
		mResortBBPaint.setColor(rsm.GetMapBGColor());
		mResortBBPaint.setStyle(Paint.Style.FILL);
		mResortBBPaint.setAlpha(0xff);
		mResortBounds.set(mResortBBInView);
		if(mResortBounds.intersect(drawingGeoRegionBoundary)) {
			mLoadTransform.mapRect(mResortBounds);
			mBackgroundCanvas.drawRect(mResortBounds, mResortBBPaint);
		}
		
		if(!mCancelLoad) mapDrawing.DrawBackground(mBackgroundCanvas, drawingGeoRegionBoundary, mapHeading, rsm, mLoadTransform, viewScale, res, showAllNames, this);

		if(!mCancelLoad) mapDrawing.LoadPOIArray(mPOIs, drawingGeoRegionBoundary, this);
		
		if(!mCancelLoad) {
			mLoaded = true;
//			Log.i(TAG, "Background image info: "+ mBackgroundImage + ", " +mBackgroundImage.getWidth() + ", " +mBackgroundImage.getHeight());
//			Log.i(TAG, "mPOIs: "+ mPOIs.size());
		}
	}
	
	public void Draw(Canvas canvas, RectF viewPortTestBoundary, RenderSchemeManager rsm, float geoCenterX, float geoCenterY, float screenOffsetX, float screenOffsetY, float viewScale, float heading, Resources res) {  // draw all objects within bounds
		// draw background first
//		float xTrans2 = -mBackgroundImage.getWidth()/2.f;
//		float yTrans2 = -mBackgroundImage.getWidth()/2.f;
//		float xTrans2 = (mRegionBoundsDrawingCoords.left-geoCenterX)*viewScale;
//		float yTrans2 = (mRegionBoundsDrawingCoords.top -geoCenterY)*viewScale;
//		Log.i(TAG, "draw bg: "+ xTrans2 + ", " + mRegionBoundsDrawingCoords.left + ", " + geoCenterX);
//		Log.i(TAG, "       : "+ yTrans2 + ", " + mRegionBoundsDrawingCoords.top  + ", " + geoCenterY);
//		Log.i(TAG, "       : "+ viewScale + ", " + mDrawingScale);
		
		//Log.e(TAG,"$$$$$$$$$$$$ DrawingSet.Draw()");
		
		mBGDrawTransform.reset();
		mBGDrawTransform.setTranslate(-mBackgroundImage.getWidth()/2.f,-mBackgroundImage.getWidth()/2.f);
		mBGDrawTransform.postScale(viewScale/mDrawingScale,viewScale/mDrawingScale);
		mBGDrawTransform.postRotate(heading);
		mRotateOffsetTransform.setRotate(heading);
		mOffset[0] = (float)(mRegionCenter.x-geoCenterX)*viewScale;
		mOffset[1] = (float)(mRegionCenter.y-geoCenterY)*viewScale;
		mRotateOffsetTransform.mapPoints(mOffset);
		mBGDrawTransform.postTranslate(mOffset[0] + screenOffsetX, mOffset[1] + screenOffsetY);

//		mBGDrawTransform.setScale(0.25f, 0.25f);
		canvas.drawBitmap(mBackgroundImage, mBGDrawTransform, null);  
		
		// then overlay POIs that are within viewport (oriented to face user)
		mPOIDrawTransform.setTranslate(-geoCenterX,-geoCenterY);
		mPOIDrawTransform.postScale(viewScale,viewScale);
		mPOIDrawTransform.postRotate(heading);
		mPOIDrawTransform.postTranslate(screenOffsetX, screenOffsetY);
		for(POIDrawing poiDrawing : mPOIs) {
			if(poiDrawing.mPoi.Type != POI.POI_TYPE_BUDDY && poiDrawing.mState == MapDrawings.State.DISABLED)	{		// draw disabled, non-focused items first
				if(viewPortTestBoundary.contains((float)poiDrawing.mLocation.x, (float)poiDrawing.mLocation.y)) {
					poiDrawing.Draw(canvas, res, rsm, mPOIDrawTransform, viewScale);
				}
			}
		}
		for(POIDrawing poiDrawing : mPOIs) {
			if(poiDrawing.mPoi.Type != POI.POI_TYPE_BUDDY && poiDrawing.mState == MapDrawings.State.NORMAL)	{			// draw enabled, non-focused items second
				if(viewPortTestBoundary.contains((float)poiDrawing.mLocation.x, (float)poiDrawing.mLocation.y)) {
					poiDrawing.Draw(canvas, res, rsm, mPOIDrawTransform, viewScale);
				}
			}
		}
		for(POIDrawing poiDrawing : mPOIs) {
			if(poiDrawing.mPoi.Type != POI.POI_TYPE_BUDDY && poiDrawing.mState == MapDrawings.State.DISABLED_FOCUS)	{	// draw disabled, focused item third (if one exists and should only be one)
				if(viewPortTestBoundary.contains((float)poiDrawing.mLocation.x, (float)poiDrawing.mLocation.y)) {
					poiDrawing.Draw(canvas, res, rsm, mPOIDrawTransform, viewScale);
				}
			}
		}
		for(POIDrawing poiDrawing : mPOIs) {
			if(poiDrawing.mPoi.Type != POI.POI_TYPE_BUDDY && poiDrawing.mState == MapDrawings.State.HAS_FOCUS)	{		// draw enabled, focused item last (if one exists and should only be one)
				if(viewPortTestBoundary.contains((float)poiDrawing.mLocation.x, (float)poiDrawing.mLocation.y)) {
					poiDrawing.Draw(canvas, res, rsm, mPOIDrawTransform, viewScale);
				}
			}
		}
	}
	public void ResetPOIStatus() {
		for(POIDrawing poiDrawing : mPOIs) {
			poiDrawing.mState = MapDrawings.State.NORMAL;
		}		
	}
	
	public void  GetClosestItem(ReconMapView.RollOverResult ror, PointD mDrawingGeoRegionCenter, RectF mDrawingReticuleBoundary, double reticuleRadius) {
		String closestItemDescription = null;
		double sqDistToCenter, diffX, diffY;
		double closestSqPOIDist = 10000000.0;
		double closestPOIDist = -1;
		POIDrawing closestPOI = null;
		double sqReticuleRadius = reticuleRadius*reticuleRadius;
		
		for(POIDrawing poiDrawing : mPOIs) {
			poiDrawing.mState = MapDrawings.State.NORMAL;
			if(mDrawingReticuleBoundary.contains((float)poiDrawing.mLocation.x, (float)poiDrawing.mLocation.y)) {
//				Log.e(TAG,"POI "+(int)(poiDrawing.mLocation.x)+", "+ (int)(poiDrawing.mLocation.y) +" : "+poiDrawing.mPoi.Name);
				diffX = mDrawingGeoRegionCenter.x - poiDrawing.mLocation.x;
				diffY = mDrawingGeoRegionCenter.y - poiDrawing.mLocation.y;
				
				sqDistToCenter = diffX*diffX + diffY*diffY;
				if(sqDistToCenter < closestSqPOIDist && sqDistToCenter < sqReticuleRadius) {
					closestPOI = poiDrawing;
					closestSqPOIDist = sqDistToCenter;
				}
			}
		}
		if(closestPOI != null) {
			closestPOIDist = Math.sqrt(closestSqPOIDist);
			closestItemDescription = closestPOI.mPoi.Name;
			closestPOI.mState = MapDrawings.State.HAS_FOCUS;
		}
		else {
			closestPOIDist = 10000000.;
		}
		
//		for(TrailDrawing trailDrawing : TrailDrawings) {
//			if(RectF.intersects(trailDrawing.GetBoundingBox(),mDrawingReticuleBoundary)) {
////					Log.e(TAG,"trail "+trailDrawing.mTrail.Name);
//				trailDrawing.CalcDistToRectCenter(canvas, rsm, drawingTransform, viewScale);
//			}
//		}
//
		ror.mDescription = closestItemDescription;
		ror.mDistance = closestPOIDist;
		
	}

}
