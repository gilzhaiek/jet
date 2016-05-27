/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.routing;

/**
 * This class defined a Node  in the routing Network
 */

import java.util.ArrayList;

import android.graphics.PointF;
import android.util.Log;

public class ReNode
{
	static public final int STATUS_NONE = 0;				//nothing happened yet; this is the initial status
	static public final int STATUS_EVALUATING  = 1;			//the node is being evaluated by the current routing task.
	static public final int STATUS_VISITED = 2;				//the node has been evludated, and the shortest distance to the start node
															//has been found.
	static public final int DISTANCE_INFINITY = -1;			//the Infinity distance; the initial value set for the shortest distance of a node
	static public final int DEFAULT_OUTEDGE_NUM = 4;		//the default capacity to reserve for the outer-edge list
	
	public PointF mPosition;								//the position of the node defined in the map local space
	public ReNetwork mNetwork;								//the network that this node resides in
	public ArrayList<ReEdge> mOutEdges;						//edges that started from this node, and going out
	public ArrayList<ReEdge> mInEdges;						//the incident edges that point toward this node
	
	//Mutable fields for routing. dynamically set during the routing process
	//and should be initialized before the routing process started.
	public int mStatus;										//The visiting status of the node
	public boolean mIsTemporary;							//if ture, this is an assistant node added by routing task; and will being removed later on
															//re-evaluate later: do we really need this flag here?
	public ReNode mComeFromNode;						    //the parent node in the shortest path tree; it is set by the routing task while performing
															//path find. It is reset each time before the path-searching start
	public ReEdge mComeFromEdge;							//the edge that connect this node to the mComeFromNode. 
															//this is for contructing a planned route by back-tracing from the end node.
															//And since there could be multiple edge connect two node. we have to set the Come-from
															//edge for un-ambiguously identify the edge
	public float  mShortestDistance;						//the shortest distance so far calculated from the start node of a routing task.
															//set as ReNode.DISTANCE_INFINITY initially.
	
	public boolean mVisited;								//field for Network verification usage only. Used by Network traverse function; Debug only
	 
	public ReNode( PointF position )
	{
		mPosition = position;
		mNetwork = null;
		mOutEdges = new ArrayList<ReEdge>( ReNode.DEFAULT_OUTEDGE_NUM );
		mInEdges = new ArrayList<ReEdge>( ReNode.DEFAULT_OUTEDGE_NUM );
		
		reset( );
	}
	
	 @Override 
	 public boolean equals(Object o) 
	 {
	     // Return true if the objects are identical.
	     // (This is just an optimization, not required for correctness.)
	     if (this == o) 
	     {
	       return true;
	     }
	     else if( o.getClass() == this.getClass())
	     {
	    	 ReNode node = (ReNode )o;
	    	 
	    	 if( ReUtil.PointEqual(node.mPosition, this.mPosition, ReUtil.MIN_RENODE_DISTANCE))
	    	 {
	    		 return true;
	    	 }
	    	 else
	    	 {
	    		 return false;
	    	 }
	     }
	     else
	     {
	    	 return false;
	     }
	 }
	
	//reset the node for next routing plan
	public void reset()
	{
		mStatus = STATUS_NONE;
		mComeFromNode = null;
		mComeFromEdge = null;
		mShortestDistance = ReNode.DISTANCE_INFINITY;
		mVisited = false;
	}
	
	//clear the node, and it will not be used anymore
	public void clear( )
	{
		mOutEdges.clear();
		mInEdges.clear();
	}
	
	//add a ReEdge that is going out from this node
	public void addEdge( ReEdge edge )
	{
		if( ReUtil.RE_CONSTRUCT_VERIFY_ON )
		{
			if( mOutEdges.contains(edge) || mInEdges.contains(edge) )
			{
				Log.e(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "The same edge already exists in the node");
				throw new RuntimeException();
			}
		}
		
		if( edge.mOneway )
		{
			if( edge.mStartNode == this )
			{
				mOutEdges.add( edge );
			}
			else if( edge.mEndNode == this )
			{
				mInEdges.add(edge);
			}
			else
			{
				Log.e(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "The edge is not adjacent to the node");
				throw new RuntimeException();
			}
		}
		else
		{
			if( edge.mStartNode == this  || edge.mEndNode == this)
			{
				mOutEdges.add( edge );
				mInEdges.add( edge );
			}
			else
			{
				Log.e(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "The edge is not adjacent to the node");
				throw new RuntimeException();
			}
		}
	}
}

