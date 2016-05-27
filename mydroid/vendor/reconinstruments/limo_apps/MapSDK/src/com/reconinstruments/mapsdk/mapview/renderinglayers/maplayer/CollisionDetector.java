package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer;

import java.util.ArrayList;

import android.graphics.Rect;
import android.util.Log;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;

public class CollisionDetector {
// constants
	private final static String TAG = "CollisionDetector";
	private final static int MIN_CENTER_DISTANCE = 25;
	private final static double EPSILON = 0.000001;
	public LabelLineModel mLastLineModel;
	
// members
	public 	ArrayList<LabelLineModel> mDrawnLabels = new ArrayList<LabelLineModel>();	// 
	
	public class LabelLineModel {
		public String   mLabelName;
		public PointXY	mCenterPoint;
		public PointXY	mEndPoint1;
		public PointXY 	mEndPoint2;
		public RectXY	mBoundingBox ;
		public PointXY 	mVector;		// mEndPoint2 translated so mEndPoint1 goes to 0
		
		public LabelLineModel(String labelName, Rect labelBounds, float centerX, float centerY, float angleInRads) {
//			Log.e(TAG, "here1 - " + labelBounds.left + ", " +labelBounds.right + ", " + centerX + ", " +  centerY + ", " +  angleInRads);
			mCenterPoint = new PointXY(centerX, centerY);
			mLabelName = labelName;
//			Log.e(TAG, "here2 - " +  mCenterPoint.x + ", " +mCenterPoint.y);
			// calc endPoints
			float halfLabelWidth = (labelBounds.right-labelBounds.left)/2.f * 1.4f;
			float halfLabelHeight = ((float)labelBounds.bottom - (float)labelBounds.top)/2.f * 1.8f; //getTextBounds() returns coordinates where +Y is down, so flip sign
			float halfLabelWidthX = halfLabelWidth;
			float halfLabelWidthY = halfLabelHeight;
			halfLabelWidthX = halfLabelWidth * (float) Math.cos(angleInRads);
			halfLabelWidthX = (halfLabelWidthX >=0 ) ? Math.max(halfLabelWidthX, halfLabelHeight) : Math.min(halfLabelWidthX, -halfLabelHeight);
			
			halfLabelWidthY = halfLabelWidth * (float) Math.sin(angleInRads);
			halfLabelWidthY = (halfLabelWidthY >= 0) ? Math.max(halfLabelWidthY, halfLabelHeight) : Math.min(halfLabelWidthY, -halfLabelHeight);

			mEndPoint1 = new PointXY(centerX - halfLabelWidthX, centerY - halfLabelWidthY);
//			Log.e(TAG, "here3 - " +  mEndPoint1.x + ", " +mEndPoint1.y);
			mEndPoint2 = new PointXY(centerX + halfLabelWidthX, centerY + halfLabelWidthY);
//			Log.e(TAG, "here4 - " +  mEndPoint2.x + ", " + mEndPoint2.y);
			
			mVector = new PointXY(mEndPoint2.x - mEndPoint1.x, mEndPoint2.y - mEndPoint1.y);	// treat line segment as vector for fast testing with cross products
			
			// calc boundingBox
			mBoundingBox = new RectXY(mEndPoint1.x < mEndPoint2.x ? mEndPoint1.x : mEndPoint2.x,
							mEndPoint1.y > mEndPoint2.y ? mEndPoint1.y : mEndPoint2.y,
							mEndPoint1.x > mEndPoint2.x ? mEndPoint1.x : mEndPoint2.x,
							mEndPoint1.y < mEndPoint2.y ? mEndPoint1.y : mEndPoint2.y);
//			Log.e(TAG, "Label bounding box: " + mBoundingBox.left + ", " +mBoundingBox.right + ", " + mBoundingBox.top + ", " +mBoundingBox.bottom );
			
		}
		
	    public double crossProduct(PointXY a, PointXY b) {
	        return a.x * b.y - b.x * a.y;
	    }
	    
		public boolean CollidesWith(LabelLineModel line) {
			// quick test bounding boxes..
			if(!mBoundingBox.Intersects(line.mBoundingBox))  {
//				Log.d(TAG, "Fail boundary box test");
				return false;
			}
			// test center distances
			if(mCenterPoint.DistanceToPoint(line.mCenterPoint) < MIN_CENTER_DISTANCE) {
//				Log.d(TAG, "Fail boundary box test");
//				Log.e(TAG, "  -- collision (close centers) detected with " + line.mLabelName);
				return true;
			}
			
			// normalize test points then test for crossing (ie, convert to a vector, where the test point = end of vector, other end at 0,0)
			PointXY tPoint1 = new PointXY(line.mEndPoint1.x - mEndPoint1.x, line.mEndPoint1.y - mEndPoint1.y);
			PointXY tPoint2 = new PointXY(line.mEndPoint2.x - mEndPoint1.x, line.mEndPoint2.y - mEndPoint1.y);
			
			if(Math.abs(crossProduct(mVector, tPoint1)) < EPSILON || Math.abs(crossProduct(mVector, tPoint2)) < EPSILON) {
//				Log.e(TAG, "  -- collision (touch line) detected with " + line.mLabelName);
				return true;
			}
			
			boolean lineCrossesThisLine = false;
			if((crossProduct(mVector, tPoint1) * crossProduct(mVector, tPoint2)) < 0) {
				lineCrossesThisLine = true;
			}

			tPoint1 = new PointXY(mEndPoint1.x - line.mEndPoint1.x, mEndPoint1.y - line.mEndPoint1.y);
			tPoint2 = new PointXY(mEndPoint2.x - line.mEndPoint1.x, mEndPoint2.y - line.mEndPoint1.y);
			boolean thisLineCrossesLine = false;
			if((crossProduct(line.mVector, tPoint1) * crossProduct(line.mVector, tPoint2)) < 0) {
				thisLineCrossesLine = true;
			}
			if(lineCrossesThisLine && thisLineCrossesLine) {
//				Log.d(TAG, "Collision - crossed line");
//				Log.e(TAG, "  -- collision (crossed line) detected with " + line.mLabelName);
				return true;
			}
//			Log.d(TAG, "Fail - line segments do not cross");
			return false;
		}
		
	}

	
			
// methods
	public CollisionDetector() {
		Reset();
	}
	
	public void Reset() {
		mDrawnLabels.clear();
	}
	
	public boolean CanAddObjectLabel(String labelName, Rect labelBounds, float centerX, float centerY, float angleInRads) {
		
//		Log.e(TAG, "here1 - " + labelBounds.left + ", " +labelBounds.right + ", " + centerX + ", " +  centerY + ", " +  angleInRads);
		LabelLineModel newLine = new LabelLineModel(labelName, labelBounds, centerX, centerY, angleInRads);
		mLastLineModel = newLine;
//		Log.e(TAG, "here2");
		// if any exist in map
		for(LabelLineModel line : mDrawnLabels) {
			if(newLine.CollidesWith(line)) {
				return false;
			}
		}
		mDrawnLabels.add(newLine);
//		Log.e(TAG, "here3");
		return true;
	}
}
