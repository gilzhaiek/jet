package com.reconinstruments.mapsdk.mapview.WO_drawings;

import java.util.ArrayList;
import java.util.Locale;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WO_Polyline;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.CollisionDetector;

public class PolylineDrawing extends WorldObjectDrawing {
// constants
	private static final long serialVersionUID = 1L;
	private final static String TAG = "PolylineDrawing";
	private final static boolean ShowHiddenLabels = false;

// members
    protected double			mPathAngle = 0.0;
	protected Path[]			mPath = null;				// base path at scale 1
	protected Path 				mDrawPath = new Path();		// scale, translated version of mPath
	protected PointXY			mLabelCenterPoint = null;
	protected float				mLabelAngleFromHorizontalInRads;
//	protected RectF 			mBoundingBox = null;

	public double				mLineLengthInPixels = -1;
	protected Rect 				mLabelBounds = new Rect();		// saves recreating Rect during each draw cycle
    protected int				mCurrentPathIndex = -1;	// -1 == not set - used only during Draw() 
	PointXY mPointInDrawingCoord = new PointXY(0.f, 0.f);
	PointXY mFirstPointInDrawingCoord = new PointXY(0.f, 0.f);
	PointXY mLastPointInDrawingCoord = new PointXY(0.f, 0.f);

	// preallocated arrays to speed up draw
	float[] v = new float[9]; 
	float[] vcp = new float[2];


	public PolylineDrawing(WorldObjectDrawingTypes type, WO_Polyline dataObject, boolean isAngleDependent, World2DrawingTransformer world2DrawingTransformer) {
		super(type, (WorldObject)dataObject);
		SetPaths(isAngleDependent, world2DrawingTransformer);
	}

	public void setRendering(RenderSchemeManager.ObjectTypes renderType, int renderTypeVariantIndex) {
		super.setRendering(renderType, renderTypeVariantIndex);
	}

	public void SetPaths(boolean isAngleDependent, World2DrawingTransformer world2DrawingTransformer){
		WO_Polyline plObj = ((WO_Polyline)mDataObject);
		if(plObj.mPolylineNodes != null) { 
			PointXY prevPoint = null;
			mPath = new Path[2];

			mLineLengthInPixels = 0.0;
			int numPoints = plObj.mPolylineNodes.size();
			int idx = 0;

			mPath[0] = new Path();
			mPath[0].incReserve(numPoints);

			ArrayList<PointXY> pointsInDrawingCoords = new ArrayList<PointXY>();

			// set up base path
			for( PointXY pointInGPSCoords : plObj.mPolylineNodes) {
//				mPointInDrawingCoord = world2DrawingTransformer.TransformGPSPointToDrawingPoint(pointInGPSCoords);
				pointsInDrawingCoords.add(world2DrawingTransformer.TransformGPSPointToDrawingPoint(pointInGPSCoords));
				mPointInDrawingCoord = pointsInDrawingCoords.get(idx);

				if( idx++ == 0 ){			
					mPath[0].moveTo(mPointInDrawingCoord.x, mPointInDrawingCoord.y);
					prevPoint = mPointInDrawingCoord;
				}
				else {
					mPath[0].lineTo(mPointInDrawingCoord.x, mPointInDrawingCoord.y);

					double xDiff = (double)mPointInDrawingCoord.x - (double)prevPoint.x;
					double yDiff = (double)mPointInDrawingCoord.y - (double)prevPoint.y;
					mLineLengthInPixels += Math.sqrt(xDiff*xDiff + yDiff*yDiff);
					prevPoint = mPointInDrawingCoord;
				}
			}

			// calc label center point and angle
			if(pointsInDrawingCoords.size() > 1) {
				double curLineLength = 0;
				double segmentLength = 0;
				double centerLength = mLineLengthInPixels / 2.;
				prevPoint = pointsInDrawingCoords.get(0);
				for( idx = 1; idx < numPoints; idx++ ) {
					mPointInDrawingCoord = pointsInDrawingCoords.get(idx);
					double xDiff = (double)mPointInDrawingCoord.x - (double)prevPoint.x;
					double yDiff = (double)mPointInDrawingCoord.y - (double)prevPoint.y;
					segmentLength = Math.sqrt(xDiff*xDiff + yDiff*yDiff);
					curLineLength += segmentLength;
					if(curLineLength > centerLength) {  // if past halfway
						mLabelCenterPoint = new PointXY((float)(mPointInDrawingCoord.x - ((curLineLength-centerLength)/segmentLength) * xDiff), (float)(mPointInDrawingCoord.y - ((curLineLength-centerLength)/segmentLength) * yDiff) ); 
						mLabelAngleFromHorizontalInRads = (float) Math.atan2(yDiff, xDiff);
						break;
					}
					prevPoint = mPointInDrawingCoord;
				}
			}
			else {
				mLabelCenterPoint = new PointXY(0.f, 0.f);
				mLabelAngleFromHorizontalInRads = 0.f;
			}
//			Log.e(TAG, "PolylineDrawing setup: " + plObj.mName + ": "+ mLabelCenterPoint.x + ", " + mLabelCenterPoint.y  + ", " + mLabelAngleFromHorizontalInRads);

			// calc 2nd path if angleDependent (simplifies mechanism to draw upside down)
			if(!isAngleDependent) {
				mPath[1] = null;
				mPathAngle = 0.0;
			}
			else {
				// set up alternative path
				mPath[1] = new Path();
				mPath[1].incReserve(plObj.mPolylineNodes.size());

				//create the inversed path for rendering name label on it
				for( idx = numPoints - 1; idx >=0; --idx ) {
					PointXY pointInGPSCoords = plObj.mPolylineNodes.get(idx);
//					mPointInDrawingCoord = world2DrawingTransformer.TransformGPSPointToDrawingPoint(pointInGPSCoords);
					mPointInDrawingCoord = pointsInDrawingCoords.get(idx);
					if( idx == numPoints - 1 ) {				
						mPath[1].moveTo(mPointInDrawingCoord.x, mPointInDrawingCoord.y);
					}
					else {
						mPath[1].lineTo(mPointInDrawingCoord.x, mPointInDrawingCoord.y);
					}						
					prevPoint = mPointInDrawingCoord;
				}	

//				PointXY firstPointInGPSCoords = plObj.mPolylineNodes.get(0);
//				PointXY lastPointInGPSCoords = plObj.mPolylineNodes.get(numPoints-1);
//				mFirstPointInDrawingCoord = world2DrawingTransformer.TransformGPSPointToDrawingPoint(firstPointInGPSCoords);
//				mLastPointInDrawingCoord  = world2DrawingTransformer.TransformGPSPointToDrawingPoint(lastPointInGPSCoords);
				mFirstPointInDrawingCoord = pointsInDrawingCoords.get(0);
				mLastPointInDrawingCoord = pointsInDrawingCoords.get(numPoints-1);

				double diffX = mFirstPointInDrawingCoord.x - mLastPointInDrawingCoord.x;
				double diffY = -(mFirstPointInDrawingCoord.y - mLastPointInDrawingCoord.y); // y +ve down
				double atanAngle = Math.toDegrees(Math.atan2(diffY, diffX));	// atanAngle is angle positive counter clockwise (rel to 0 at right/east), 
				mPathAngle = 90.0 - atanAngle; 							// makes angle rel to up/north
				mPathAngle = (mPathAngle + 360.0) % 360.0;				// make clockwise like map heading measurements and constrain to 0-360
//				Log.e(TAG, "Path angle: " + mDataObject.mName + " -- " + mPathAngle + " | "+ atanAngle +" | "+ -diffX + ", " + diffY);
			}

		}
	}	

	public void Release() {
		mPath[0].reset();
		mPath[0] = null;
		if(mPath[1] != null) {
			mPath[1].reset();
			mPath[1] = null;
		}
//		mBoundingBox = null;
//		mPolylinePoints.clear();
	}

//
//	public RectF GetBoundingBox(){			
//
//		if(mBoundingBox == null) {
//			mBoundingBox = new RectF();
//			mPath[0].computeBounds(mBoundingBox, true);
//		}
//		
//		return mBoundingBox;
//	}

	public Path GetPath(double mapUpDirHeading){	//mapUpDirHeading is 0-360

		if(mPath[1] == null) {	// ie, not angle dependent
			return mPath[0];
		}

		double rebasedAngle = (mPathAngle - mapUpDirHeading + 360.0) % 360.0;
		if(rebasedAngle >= 0.0 && rebasedAngle < 180.0) {
			mCurrentPathIndex = 1;
		}
		else {
			mCurrentPathIndex = 0;
		}

		return mPath[mCurrentPathIndex];
	}	

	public void Draw(Canvas canvas, double heading, Paint paint, Matrix transformMatrix) {
		if(paint != null) {		// if not defined or trail width == 0 in xml
			mDrawPath.set(GetPath(heading));		// get path based on heading if more than one path defined
			mDrawPath.transform(transformMatrix);
			canvas.drawPath(mDrawPath, paint);
		}
	}

	public void DrawNames(Canvas canvas, Paint textPaint, Paint textOutlinePaint, float trailNameOffsetFactor, Matrix transformMatrix, String name, CollisionDetector collisionDetector) {
		// depends on mDrawPath set in Draw()   - TODO remove this call order dependency
		if(textPaint != null) {
			transformMatrix.getValues(v);
			float scaleFactor = 0.3f;
			if(v[Matrix.MSCALE_X] < 7.9) scaleFactor = 0.8f;
			if(v[Matrix.MSCALE_X] < 3.9) scaleFactor = 0.85f;
			if(v[Matrix.MSCALE_X] < 1.9) scaleFactor = 0.9f;
			float testLength = (float) (mLineLengthInPixels * v[Matrix.MSCALE_X] * scaleFactor);
			
			vcp[0] = mLabelCenterPoint.x;
			vcp[1] = mLabelCenterPoint.y;
			
			transformMatrix.mapPoints(vcp);		// calc scaled/rotated label center point
			
			//label bounding boxes are the correct size, but just not oriented the right way.

//			Log.d(TAG, "Drawing path: " + name + "with angle: " + Math.toDegrees(mLabelAngleFromHorizontalInRads));
			textPaint.getTextBounds(name.toCharArray() , 0, name.length()-1, mLabelBounds);
			
//    		Log.i(TAG, "path name test: "+ (mLabelBounds.right-mLabelBounds.left) + " - " + testLength + " (lineLength="+mLineLengthInPixels+", scale="+ v[Matrix.MSCALE_X]+")");
			if((mLabelBounds.right-mLabelBounds.left) < testLength) {		
				if(collisionDetector.CanAddObjectLabel(name, mLabelBounds, vcp[0], vcp[1], mLabelAngleFromHorizontalInRads)) { 					// draw if not collided with other labels
					canvas.drawTextOnPath( name, mDrawPath, 0, mLabelBounds.height() * trailNameOffsetFactor, textPaint );
				}
				else {
//					Log.e(TAG, "  - collision stopped draw");
//					if(ShowHiddenLabels) {
//						Paint collisionPaint = new Paint(textOutlinePaint);
//						collisionPaint.setColor(Color.RED);		
//						canvas.drawTextOnPath( name, mDrawPath, 0, mLabelBounds.height() * trailNameOffsetFactor, textPaint );
//					}
				}
			}
			else {
//				Log.i(TAG, "  - hiding name, too short");
			}
		}
	}

}
