/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.routing;

import java.util.ArrayList;

import android.graphics.PointF;
import android.graphics.RectF;

import com.reconinstruments.navigation.navigation.Trail;

/**
 * This class defined the data structure for performing a hit-testing of a point
 * against a ReNetwork. The result is: the shortest distance from the test-point
 * to the ReNetwork the found hit-test point which has the shortest distance to
 * the test-point, could either be a point on the edges, or one of the ReNode.
 */

public class ReHitTestInfo {
	static public final float DEFAULT_MAX_HIT_DISTANCE = 50.f / (float) ReUtil.DISTANCE_PER_PIXEL;
	public float mMaxDist = DEFAULT_MAX_HIT_DISTANCE; // the maximum valid
														// distance for
														// performing the
														// hit-test against the
														// test point
														// i.e, any edge or
														// point has larger
														// distance to the
														// test-point will be
														// ignored;
														// this is for
														// performance
														// consideration

	public PointF mTestPoint = null; // the location we are testing hit against
										// a ReNetwork

	public ReEdge mEdge = null; // the edge hitted by the test
	public PointF mHitPoint = new PointF(); // the hit point after performing
											// the hit-test;
	public int mSegmentId = -1; // the index of the segment where the hit-point
								// located on; -1 means no point hit
	private int mFilterMask = 0; // the mask for filtering out certain type of
									// trails from
									// being tested for hitting. For example to
									// filter out lift. we set the
									// the mask |= Trail.Trail_Lift << 1;
	public float mShortestDist = Float.MAX_VALUE; // the shortest distance found
													// out so far;
	private RectF mTestBound = new RectF(); // the tested bounding box around
											// the test point
	private PointF mTempPoint = new PointF(); // the temporary point for
												// hit-testing calculation

	public ReHitTestInfo(PointF testPoint, float maxDist) {
		mTestPoint = testPoint;
		mMaxDist = maxDist;
		reset();
	}

	public void reset() {
		mEdge = null;
		mSegmentId = -1;
		mFilterMask = 0;
		mTestBound.setEmpty();
	}

	public void filterTrail(int trailType) {
		mFilterMask |= (1 << trailType);
	}

	// test if certain type of trail is filtered out
	// of hit-testing or not
	private boolean isFiltered(int trailType) {
		return (mFilterMask & (1 << trailType)) != 0;
	}

	// perform a hit test against the Network,
	// if hit any edge within the distance of mMaxDist
	// return true, and record down the hitTestInfo
	// for the closest hit-point/edge
	public boolean hitTest(ReNetwork network) {

		// if there is already a hit-point from previous hitTest
		// for example, test against another network, use it as the
		// test bound, otherwise, use the default mMaxDist as the
		// the test bound around the test point.
		float maxDist = mMaxDist < mShortestDist ? mMaxDist : mShortestDist;

		// set the testbound as a rectangle centered at mTestPoint with
		// the dimension of maxDist;
		mTestBound.set(mTestPoint.x - maxDist, mTestPoint.y - maxDist,
				mTestPoint.x + maxDist, mTestPoint.y + maxDist);

		boolean hitSomething = false;
		int idx = -1;
		for (ReEdge edge : network.mEdges) {
			++idx;
			// skip this edge if it is filtered from hit-testing
			if (mFilterMask != 0 && isFiltered(edge.mRoadType)) {
				continue;
			}

			// test for bounding box intersection, if the edge's bounding
			// box not touch mTestBound, quickly reject for further test
			if (RectF.intersects(mTestBound, edge.mBBox) == false ) {
				continue;
			}

			// otherwise, perform a hit test on the edge
			boolean hitResult = hitTest(edge);

			// this edge is hit as the closest edge so far
			// mark it
			if (hitResult) 
			{
				mEdge = edge;
				hitSomething = true;
			}
		}

		return hitSomething;
	}

	// find the closest hit point on the given edge against
	// the mTestPoint, if it is closer than so-far-hit mHitPoint
	// set the mHitPoint, mShortestDist, mEdge etc, and return true
	// otherwise, return false
	private boolean hitTest(ReEdge edge) {
		boolean hit = false;
		for (int idx = 0; idx < edge.mPolyline.size() - 1; ++idx) {
			PointF p1 = edge.mPolyline.get(idx);
			PointF p2 = edge.mPolyline.get(idx + 1);

			float x1 = p2.x - p1.x;
			float y1 = p2.y - p1.y;

			float x2 = mTestPoint.x - p1.x;
			float y2 = mTestPoint.y - p1.y;

			float x3 = mTestPoint.x - p2.x;
			float y3 = mTestPoint.y - p2.y;

			// dot product between pp1 and p2p1, p is the mTestPoint
			float d1 = x1 * x2 + y1 * y2;

			// dot product between pp2, p2p1
			float d2 = x1 * x3 + y1 * y3;

			float shortestDist = Float.MAX_VALUE;
			// the angle between pp1 and p2p1 is greater than 90
			if (d1 <= 0.f) {
				// the shortest distance should be the length of vector p->p1
				shortestDist = (float) Math.sqrt(x2 * x2 + y2 * y2);
				mTempPoint.set(p1.x, p1.y);
			} else if (d2 >= 0.f) {
				// the angle between pp2 and p1p2 is greater than 90
				shortestDist = (float) Math.sqrt(x3 * x3 + y3 * y3);
				mTempPoint.set(p2.x, p2.y);
			} else {
				// otherwise, the project from p to p1p2 falling between p1->p2
				// so the shortest distance from p to p1p2 is the actual
				// projection

				// the projected point from p(mTestPoint) to p1p2 is calculated
				// as
				// (V(p1,p2)/Length(V(p1,p2))*((V(p1,p2) dot
				// V(p1,p)))/Length(V(p1,p2) + p1
				// which is equal to V(p1,p2)*(V(p1,p2) dot
				// V(p1,p))/Square(Length(V(p1,p2))) + p1
				// d1 = V(p1,p2) dot V(p1,p)
				// lenSq = Square(Length(V(p1,p2)))
				// V(p1, p2)=(x1,y1)
				float lenSq = x1 * x1 + y1 * y1; // the length square of p2->p1
				d1 /= lenSq;
				float projX = x1 * d1 + p1.x;
				float projY = y1 * d1 + p1.y;
				mTempPoint.set(projX, projY); // hit point is (projX, projY)

				float x = mTestPoint.x - projX;
				float y = mTestPoint.y - projY;
				shortestDist = (float) Math.sqrt(x * x + y * y);

			}

			if (shortestDist < mShortestDist) {
				mShortestDist = shortestDist;
				hit = true;
				mHitPoint.set(mTempPoint.x, mTempPoint.y);
				mSegmentId = idx;
			}
		}

		return hit;
	}

	// temporary utility function for constructing a
	// RePath for rendering
	public RePath constructPath( )
	{
		//if hit nothing
		if( mSegmentId == -1 )
		{
			return null;
		}
		else
		{
			ArrayList<PointF> points = new ArrayList<PointF>(mEdge.mPolyline.size()- mSegmentId + 1);
			points.add(mHitPoint);
			for( int idx = mSegmentId + 1; idx < mEdge.mPolyline.size(); ++idx )
			{
				points.add(mEdge.mPolyline.get(idx));
			}
			
			Trail trail = new Trail(points, mEdge.mName, mEdge.mRoadType, mEdge.mOneway );
			
			ReEdge edge = new ReEdge( points, mEdge.mRoadType, 0, mEdge.mOneway, mEdge.mName );
		
			RePath path = new RePath();
			path.addNode( mEdge.mStartNode );
			path.addNode( mEdge.mEndNode );
			path.addEdge(edge);
			//path.addEdge( mEdge );
			return path;
		}
		
	}
}