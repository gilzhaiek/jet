/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.routing;

/**
 * This class defined an network of the route engine
 * A network is a set Edges connected by Nodes
 */

import java.util.ArrayList;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

public class ReNetwork
{
	public ArrayList<ReNode>  mNodes;					//all ReNode's that consist of the network
	public ArrayList<ReEdge>  mEdges;					//all ReEdge's that consist of the network
	public RectF mBBox;									//the bounding box of the Network in the map local space
	
	public ReNetwork( )
	{
		mNodes = new ArrayList<ReNode>( );
		mEdges = new ArrayList<ReEdge>( );
		mBBox = new RectF();
		mBBox.setEmpty();
	}
	//reset the network for next route planning task
	public void reset( )
	{
		for( ReEdge edge : mEdges )
		{
			edge.reset( );
		}
		
		for( ReNode node : mNodes )
		{
			node.reset( );
		}
	}
	
	//clear the network, and will be out-of-date afterwards
	public void clear( )
	{
		reset( );
		for( ReEdge edge : mEdges )
		{
			edge.clear( );
		}
		
		for( ReNode node : mNodes )
		{
			node.clear( );
		}
		mNodes.clear();
		mEdges.clear();
	}
	
	public int getNumNodes()
	{
		return mNodes.size();
	}
	
	public int getNumEdges()
	{
		return mEdges.size();
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
	     else
	     {
	    	 return false;
	     }
	 }
	 
	 public void addNode( ReNode node )
	 {
		 if( ReUtil.RE_CONSTRUCT_VERIFY_ON )
		 {
			 boolean exists = mNodes.contains( node );
			 if( exists )
			 {
				 Log.e(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Node is duplicated in the Network");
				 throw new RuntimeException();
			 }
		 }
		 
		 node.mNetwork = this;
		 mNodes.add( node );
	 }
	 
	 public void addEdge( ReEdge edge )
	 {
		 if( ReUtil.RE_CONSTRUCT_VERIFY_ON )
		 {
			 boolean exists = mEdges.contains( edge );
			 if( exists )
			 {
				 Log.e(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Edge is duplicated in the Network");
				 throw new RuntimeException();
			 }
		 }
		 
		 mEdges.add( edge );

		 //inflate the bounding box
		 mBBox.union( edge.mBBox );
	 }
	 
	 //merge the network to this
	 public void merge( ReNetwork network )
	 {
		 Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Starting merge two networks");
		 
		 //move all nodes from network to this
		 for( ReNode node : network.mNodes )
		 {
			 //change the network of the node
			 node.mNetwork = this;
			 mNodes.add( node );
		 }
		 
		 //move all edges from network to this
		 for( ReEdge edge : network.mEdges )
		 {
			 mEdges.add( edge );
		 }
		 
		 //inflate the bounding box
		 mBBox.union( network.mBBox );
		 
		 network.mNodes.clear();
		 network.mEdges.clear();
	
		 Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "The merged networks has " + mNodes.size() + " nodes and " + mEdges.size() + " edges");
		 Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Starting merge two networks");
	 }
	 
	 //given a location, search the network for a node that
	 //is located in that position. return null if can not
	 //find out any
	 public ReNode searchNode( PointF pos )
	 {
		 //move all nodes from network to this
		 for( ReNode node : mNodes )
		 {
			 if( ReUtil.PointEqual( pos, node.mPosition, ReUtil.MIN_RENODE_DISTANCE ))
			 {
				 return node;
			 }

		 }
		 
		 return null;
	 }
	 
	 
	 //traverse the whole network to verify 
	 //if any nodes or edges are not reachable
	 private void traverse( ReNode rootNode )
	 {
		 rootNode.mVisited = true;
		 
		 for( ReEdge edge : rootNode.mOutEdges )
		 {
			 if( edge.mOneway == true  )
			 {
				 if( edge.mEndNode.mVisited == false)
				 {
					 traverse( edge.mEndNode );
				 }				 
			 }
			 else
			 {
				 if( edge.mStartNode == rootNode && !edge.mEndNode.mVisited)
				 {
					 traverse( edge.mEndNode );
				 }
				 else if( edge.mEndNode == rootNode && !edge.mStartNode.mVisited)
				 {
					 traverse( edge.mStartNode );
				 }
			 }
			 edge.mVisited = true;
		 }
		 
	 }
	 
	 //utility function for verify the topology of the network
	 public void verifyTopology()
	 {
		 int idx1 = 0;
		 for( ReNode node : mNodes )
		 {
			 Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Start Verify node #" + idx1 + "...");
			 reset();
			 traverse( node );
			 
			 int idx2 = 0;
			 for( ReNode node2 : mNodes )
			 {
				 if( node2.mVisited == false )
				 {
					 Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Node #" + idx2 + "is not reachable from Node #" + idx1 );
				 }
				 ++idx2;
			 }
			 

			 idx2 = 0;
			 for( ReEdge edge : mEdges )
			 {
				 if( edge.mVisited == false )
				 {
					 Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Edge #" + idx2 + "(" + edge.mName + ")" + "is not reachable from Node #" + idx1 );
				 }
				 ++idx2;
			 }
			 
			 Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Finish Verify node #" + idx1 + "...");
			 ++idx1;
		 }
		 
	 }

}