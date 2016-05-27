package com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;

public class CustomLineDrawing extends CustomWorldObjectDrawing {
// constants
	private final static String TAG = "CustomLineDrawing";
	
// members
    protected double			mPathAngle = 0.0;
	protected Path				mPath = null;				// base path at scale 1
	protected Path 				mDrawPath = new Path();		// scale, translated version of mPath
	public Paint				mPaint;
	ArrayList<PointXY> 			mGPSNodes;
	public double				mLineLengthInPixels = -1;
	protected RectXY 			mDrawingBounds = new RectXY(999999.0f,-999999.0f,-999999.0f, 999999.0f);		// saves recreating Rect during each draw cycle
	protected RectXY 			mGPSBounds = new RectXY(999999.0f,-999999.0f,-999999.0f, 999999.0f);		// saves recreating Rect during each draw cycle
    protected int				mCurrentPathIndex = -1;	// -1 == not set - used only during Draw() 
	PointXY 					mPointInDrawingCoord = new PointXY(0.f, 0.f);
	float						mLineWidthInPixels;

	int cnt = 0;

	public CustomLineDrawing(String _lineID, ArrayList<PointXY> _nodes, float _lineWidthInM,  int _color,  int _alpha, World2DrawingTransformer _world2DrawingTransformer) {
		super(_lineID, CustomObjectTypes.TRAIL);

		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setAntiAlias(true);
		mPaint.setColor(_color);
		mPaint.setAlpha(_alpha);
		mLineWidthInPixels = _lineWidthInM/World2DrawingTransformer.DISTANCE_PER_PIXEL;

		mGPSNodes = (ArrayList<PointXY>)(_nodes.clone());
		SetPathAndBounds(mGPSNodes, _world2DrawingTransformer);
	}
	
	public void HandleDrawingTransformerChange(World2DrawingTransformer _world2DrawingTransformer) {
		SetPathAndBounds(mGPSNodes, _world2DrawingTransformer);
	}
	
	public void SetPathAndBounds(ArrayList<PointXY> _nodes, World2DrawingTransformer world2DrawingTransformer){
		
		if(_nodes != null) { 
			mLineLengthInPixels = 0.0;
			int numPoints = _nodes.size();
			
			mPath = new Path();
			mPath.incReserve(numPoints);

			int idx = 0;
			PointXY prevPoint = null;
			PointXY firstPoint = null;
			
			// set up base path
			for( PointXY pointInGPSCoords : _nodes) {
				if(firstPoint == null) firstPoint = pointInGPSCoords;

				if(pointInGPSCoords.x < mGPSBounds.left)   mGPSBounds.left =   pointInGPSCoords.x;
				if(pointInGPSCoords.x > mGPSBounds.right)  mGPSBounds.right =  pointInGPSCoords.x;
				if(pointInGPSCoords.y < mGPSBounds.bottom) mGPSBounds.bottom = pointInGPSCoords.y;
				if(pointInGPSCoords.y > mGPSBounds.top)    mGPSBounds.top =    pointInGPSCoords.y;
				
				mPointInDrawingCoord = world2DrawingTransformer.TransformGPSPointToDrawingPoint(pointInGPSCoords);
				
				if(mPointInDrawingCoord.x < mDrawingBounds.left)   mDrawingBounds.left =   mPointInDrawingCoord.x;
				if(mPointInDrawingCoord.x > mDrawingBounds.right)  mDrawingBounds.right =  mPointInDrawingCoord.x;
				if(mPointInDrawingCoord.y < mDrawingBounds.bottom) mDrawingBounds.bottom = mPointInDrawingCoord.y;
				if(mPointInDrawingCoord.y > mDrawingBounds.top)    mDrawingBounds.top =    mPointInDrawingCoord.y;

				if( idx++ == 0 ){			
					mPath.moveTo(mPointInDrawingCoord.x, mPointInDrawingCoord.y);
					prevPoint = mPointInDrawingCoord;
				}
				else {
					mPath.lineTo(mPointInDrawingCoord.x, mPointInDrawingCoord.y);

					double xDiff = (double)prevPoint.x - (double)mPointInDrawingCoord.x;
					double yDiff = (double)prevPoint.y - (double)mPointInDrawingCoord.y;
					mLineLengthInPixels += Math.sqrt(xDiff*xDiff + yDiff*yDiff);
					prevPoint = mPointInDrawingCoord;
				}
			}
			if(prevPoint.x != firstPoint.x || prevPoint.y != firstPoint.y) {	// if not already closed (ie, last point == first point)
				mPath.lineTo(mPointInDrawingCoord.x, mPointInDrawingCoord.y);

				double xDiff = (double)prevPoint.x - (double)mPointInDrawingCoord.x;
				double yDiff = (double)prevPoint.y - (double)mPointInDrawingCoord.y;
				mLineLengthInPixels += Math.sqrt(xDiff*xDiff + yDiff*yDiff);
			}
		}
	}	

	public void Release() {
		if(mPath != null) {
			mPath.reset();
			mPath = null;
		}
		if(mDrawPath != null) {
			mDrawPath.reset();
			mDrawPath = null;
		}
	}

	public void Draw(Canvas canvas, Matrix transformMatrix, float viewScale) {
		if(mEnabled) {
//			if(cnt == 0) {
//				float[] transformValues = new float[9];
//				transformMatrix.getValues(transformValues);
//				Log.e(TAG, "Draw overlay: " + transformValues[Matrix.MSCALE_X] + ", " + transformValues[Matrix.MSCALE_Y] + ", "+ transformValues[Matrix.MTRANS_X] + ", "+ transformValues[Matrix.MTRANS_Y]);
//			}
//			cnt = ((cnt + 1) % 120);

			mPaint.setStrokeWidth((float)(mLineWidthInPixels * Math.sqrt((double)viewScale)) );

			mDrawPath.set(mPath);
			mDrawPath.transform(transformMatrix);
			canvas.drawPath(mDrawPath, mPaint);
		}
	}

	public boolean InGeoRegion(GeoRegion geoRegion) {
		return mGPSBounds.Intersects(geoRegion.mBoundingBox);
	}


}
