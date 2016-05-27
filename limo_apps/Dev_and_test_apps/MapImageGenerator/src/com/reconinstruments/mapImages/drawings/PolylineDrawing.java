package com.reconinstruments.mapImages.drawings;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.reconinstruments.mapImages.helpers.LocationTransformer;
import com.reconinstruments.mapImages.objects.Trail;
import com.reconinstruments.mapImages.prim.PointD;

public class PolylineDrawing {
	private final static String TAG = "PolylineDrawing";

	public static final int		PATH_ALPHA = 255;
	public static final float 	DISTANCE_PER_PIXEL = LocationTransformer.DISTANCE_PER_PIXEL;
	public static final float	TRAIL_WIDTH = 20.f/DISTANCE_PER_PIXEL;
	public static final float	TRUNK_WIDTH = 20.f/DISTANCE_PER_PIXEL;
	public static final float	WALKWAY_WIDTH = 10.f/DISTANCE_PER_PIXEL;
	public static final float	CHWAY_WIDTH = 30.f/DISTANCE_PER_PIXEL;
	public static final float	LIFT_WIDTH = 30.f/DISTANCE_PER_PIXEL;
		
	public static final float	STROKE_WIDTH = 10.f;
	public static final int		STROKE_COLOR = 0xffff0000;
	   
	public static final float	TRAIL_NAME_HOFFSET = 0.f;
	public static final float	TRAIL_NAME_VOFFSET = 5.f;
	
	//the threshold  for  dropping the trail name rendering
	//if length between the trail.startpoint and trail.endpoint is larger
	//then the threshold, the trail name will be rendered
	public static final float	MINI_LEN_FOR_TRAIL_NAME = 100;			
	
	public static final int		NAME_TEXT_SIZE = 20;
	public static final int		NAME_TEXT_COLOR = 0xffffffff;
	
//	public static final double	HYSTERESIS_ANGLE = 5.0;
//
	public ArrayList<PointD>	mPolylinePoints;
	protected Path[]			mPath = null;				// base path at scale 1
	protected Path 				mDrawPath = new Path();		// scale, translated version of mPath
	protected Path 				mTestPath = new Path();		// scale, translated version of mPath
	protected RectF 			mBoundingBox = null;
//	private Paint				mPolylinePaint = null;
	public static Paint 		mTextPaint = null;
	public static Paint 		mTextOutlinePaint = null;
	protected static Paint 		mTwoWayLiftPaint = null;
//	private double 				mPrevScale = -1;
	public boolean				mInitialized = false;
	public double				mLineLengthInPixels = -1;
	protected Rect 				mBounds = new Rect();
    protected int				mCurrentPathIndex = -1;	// -1 == not set
    protected double			mPathAngle = 0.0;
    protected Matrix			mRotationMatrix = new Matrix();
    public double rebasedAngle;
	int mFirstTime = 0;
	
	public PolylineDrawing(ArrayList<PointD> pathPoints, boolean angleDependent) {
		mPolylinePoints = pathPoints;
		SetPaths(angleDependent);
	}
	
	public void Release(){
		mPath[0].reset();
		mPath[0] = null;
		if(mPath[1] != null) {
			mPath[1].reset();
			mPath[1] = null;
		}
		mBoundingBox = null;
		mPolylinePoints.clear();
	}
	
	public void SetPaths(boolean angleDependent){
		if(mPolylinePoints != null) { 
			PointD prevPoint = null;
			mPath = new Path[2];

			mLineLengthInPixels = 0.0;
			int numPoints = mPolylinePoints.size();
			int idx = 0;
			
			mPath[0] = new Path();
			mPath[0].incReserve(numPoints);

			PointD firstPoint = mPolylinePoints.get(0);
			PointD lastPoint = mPolylinePoints.get(numPoints-1);

			// set up base path
			for( PointD pointD : mPolylinePoints) {
				if( idx++ == 0 ){			
					mPath[0].moveTo((float)pointD.x, (float)pointD.y);
					prevPoint = pointD;
				}
				else {
					mPath[0].lineTo((float)pointD.x, (float)pointD.y);

					double xDiff = (double)prevPoint.x - (double)pointD.x;
					double yDiff = (double)prevPoint.y - (double)pointD.y;
					mLineLengthInPixels += Math.sqrt(xDiff*xDiff + yDiff*yDiff);
					prevPoint = pointD;
				}
			}

			if(!angleDependent) {
				mPath[1] = null;
				mPathAngle = 0.0;
			}
			else {
				// set up alternative path
				mPath[1] = new Path();
				mPath[1].incReserve(mPolylinePoints.size());

				//create the inversed path for rendering name label on it
				for( idx = numPoints - 1; idx >=0; --idx ) {
					PointD pointD = mPolylinePoints.get(idx);
					if( idx == numPoints - 1 ) {				
						mPath[1].moveTo((float)pointD.x, (float)pointD.y);
					}
					else {
						mPath[1].lineTo((float)pointD.x, (float)pointD.y);
					}						
					prevPoint = pointD;
				}	
				
//				// calc mPathAngle in range -180 to 180, rel to up/north, positive clockwise
//				double diffX = firstPoint.x - lastPoint.x;
//				double diffY = firstPoint.y - lastPoint.y;
//				mPathAngle = Math.toDegrees(Math.atan2(diffY, -diffX));	// diffX makes angle positive counter clockwise (rel to 0 at right/east), 
////				double atanAngle = mPathAngle;
//				mPathAngle -= 90.0; 					// makes angle rel to up/north
				
				double diffX = lastPoint.x - firstPoint.x;
				double diffY = -(lastPoint.y - firstPoint.y); // y +ve down
				mPathAngle = Math.toDegrees(Math.atan2(diffY, diffX));	// diffX makes angle positive counter clockwise (rel to 0 at right/east), 
//				double atanAngle = mPathAngle;
				mPathAngle = 90.0 - mPathAngle; 									// makes angle rel to up/north
				mPathAngle = (mPathAngle + 360.0) % 360.0;				// make clockwise like map heading measurements and constrain to 0-360
//				Log.e(TAG, "Path angle: " + mPathAngle +" | "+ atanAngle +" | "+ -diffX + ", " + diffY);
			}

		}
	}	

	public RectF GetBoundingBox(){

		if(mBoundingBox == null) {
			mBoundingBox = new RectF();
			mPath[0].computeBounds(mBoundingBox, true);
		}
		
		return mBoundingBox;
	}

	public Path GetPath(double mapUpDirHeading){	//mapUpDirHeading is 0-360

		if(mPath[1] == null) {
			return mPath[0];
		}
		
		// use heading to pick path index
//		double padding = 0.0;
//		switch(mCurrentPathIndex) {
//		case -1:
//			padding = 0.0;
//			break;
//		case 0:
//			padding = HYSTERESIS_ANGLE;
//			break;
//		case 1:
//			padding = -HYSTERESIS_ANGLE;
//			break;
//		}
//
		rebasedAngle = (mPathAngle - mapUpDirHeading + 360.0) % 360.0;
//		Log.i(TAG, "rebase: " + rebasedAngle + " = " + mapUpDirHeading + " - " + mPathAngle);
		if(rebasedAngle >= 0.0 && rebasedAngle < 180.0) {
			mCurrentPathIndex = 0;
		}
		else {
			mCurrentPathIndex = 1;
		}
	
		return mPath[mCurrentPathIndex];
	}	
	
	public void Draw(Canvas canvas, double heading, Paint paint, Matrix transformMatrix) {
		if(paint != null) {
			mDrawPath.set(GetPath(heading));		// get path based on heading if more than one path defined
			mDrawPath.transform(transformMatrix);
			canvas.drawPath(mDrawPath, paint);
		}
	}

	public void DrawNames(Canvas canvas, Paint textPaint, Paint textOutlinePaint, Matrix transformMatrix, String name, int type) {
		// depends on mDrawPath set in Draw()   - TODO remove this call order dependency
		if(textPaint != null) {
			float[] v = new float[9]; 
			transformMatrix.getValues(v);
			float nameSize = textPaint.measureText(name);
			float scaleFactor = 0.3f;
			if(v[Matrix.MSCALE_X] < 7.9) scaleFactor = 0.8f;
			if(v[Matrix.MSCALE_X] < 3.9) scaleFactor = 0.85f;
			if(v[Matrix.MSCALE_X] < 1.9) scaleFactor = 0.9f;
			float testLength = (float) (mLineLengthInPixels * v[Matrix.MSCALE_X] * scaleFactor);
			//		Log.i(TAG, "path name test: "+ nameSize + " - " + testLength + " (lineLength="+mLineLengthInPixels+", scale="+ v[Matrix.MSCALE_X]+")");
			if(nameSize < testLength || type == Trail.SKI_LIFT) {
				textPaint.getTextBounds(name.toCharArray() , 0, name.length(), mBounds);
				canvas.drawTextOnPath( name, mDrawPath, 0, mBounds.height()/3, textOutlinePaint );
				canvas.drawTextOnPath( name, mDrawPath, 0, mBounds.height()/3, textPaint );
			}
			else {
				//			Log.i(TAG, "Hiding name: "+ mTrail.Name);
			}
		}
	}

}
