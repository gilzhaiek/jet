/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.routing;

/**
 * This class defined an Edge that connect two Nodes in the routing Network
 */

import java.util.ArrayList;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

public class ReEdge
{
	public String mName;								//the edge name. Evaluate later if we need this for routing or not;
	public ArrayList<PointF> mPolyline;					//the polyline that defines the physical trace of the edge in the map local space
	public float mLength;								//the Euclidean length of the mPolyline in meter
	public RectF  mBBox;								//the bounding box of the mPolyline; Will be used for intersection calculation and culling
	public ReNode  mStartNode;							//the starting node of the edge; for one-way Edge, this is the actual Node the edge start
	public ReNode mEndNode;								//the ending node of the edge; for one-way Edge, this is the actual Node the edge ends;
	public boolean mOneway;								//the edge is one-way, i.e start from mStartNode, pointing to mEndNode if true
	public int	mRoadType;								//the type of road this edge represent:  green/red/blue/black trail/trunk, lift, walkway etc
	public float mSpeedLimit;							//speed limit of this edge: -1: no speed limit
	public boolean mIsTemporary;						//if true, this edge is a temporarily added for routing plan for current task
	public boolean mVisited;							//Used for Network verification, ie. ReNetwork.traverse. Debug only field

	 @Override 
	 public boolean equals(Object o) 
	 {
	     // Return true if the objects are identical.
	     // (This is just an optimization, not required for correctness.)
	     if (this == o) 
	     {
	       return true;
	     }
	     else
	     {
	    	 return false;
	     }
	 }
	 
	public ReEdge( ArrayList<PointF> polyline, int roadType, float speedLimit, boolean oneway, String name )
	{
		mBBox = new RectF();
		mBBox.setEmpty();
		mPolyline = polyline;
		mName = name;
		calcLength();
		
		mStartNode = null;
		mEndNode = null;
		mOneway = oneway;
		mSpeedLimit = speedLimit;
		mRoadType = roadType;
		mIsTemporary = false;		
	}
	
	//reset the edge for next planning
	public void reset()
	{
		//anything to do here?
		mVisited = false;
	}
	
	//clear the edge, and it will not be used anymore
	public void clear()
	{
		//anything to do here?
	}
	
	//test if a edge is a loop edge by check if the startNode and
	//endNode are the same. Please note a ZERO length loop edge
	//will trigger a RuntimeExecption when we contruct the ReMap
	//i.e inside the ReMap.addEdge( ). 
	public boolean isLoopEge()
	{
		return mStartNode == mEndNode;
	}
	
	private void calcLength( )
	{
		mLength = 0;
		float maxx, maxy, minx, miny;
		
		if( mPolyline.size() > 0 )
		{
			PointF p = mPolyline.get(0);
			minx = maxx = p.x;
			miny = maxy = p.y;
		}
		else
		{
			Log.e(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "ReEdge" + mName + " has no vertexes" );
			minx  = miny = Float.MAX_VALUE;
			maxx = maxy = -Float.MAX_VALUE;			
			throw new RuntimeException();
		}
		
		float x, y;
		for( int i = 0; i < mPolyline.size() - 1; ++i )
		{
			PointF p1 = mPolyline.get(i);
			PointF p2 = mPolyline.get(i+1);
			
			x = p1.x - p2.x;
			y = p1.y - p2.y;
			
			mLength += (float)Math.sqrt(x*x+ y*y);
			
			minx = Math.min(minx, p2.x);
			miny = Math.min(miny, p2.y);
			maxx = Math.max(maxx, p2.x);
			maxy = Math.max(maxy, p2.y);
			
		}
		
		mBBox.set( minx, miny, maxx, maxy );
		mLength *= ReUtil.DISTANCE_PER_PIXEL;
		
		Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "ReEdge" + mName + " has length of: " + mLength );
	}
}