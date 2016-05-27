/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.routing;

import java.util.ArrayList;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

/**
 * This class defined a route-engine map.
 * A ReMap consists of one or more ReNetworks, which
 * are an isolated islands that have no connections from one to another
 */
public class ReMap
{
	public ArrayList<ReNetwork> mNetworks;
	RectF mBBox;
	
	public ReMap( )
	{
		mBBox = new RectF();
		mBBox.setEmpty();
		
		mNetworks = new ArrayList<ReNetwork>();
	}
	//reset the map to prepare for next routing task
	public void reset( )
	{
		for( ReNetwork network : mNetworks )
		{
			network.reset( );
		}
	}
	
	//clear out the map for constructing a different map topology
	//This is necessary when loading a new map
	public void clear( )
	{
		reset( );
		for( ReNetwork network : mNetworks )
		{
			network.clear( );
		}
		
		mNetworks.clear();
	}
	
	public void addEdge( ArrayList<PointF> points, int roadType, float speedLimit, boolean oneWay, String name  )
	{
		ReEdge edge = new ReEdge( points, roadType, speedLimit, oneWay, name );
		ReNode startNode=null, endNode=null;
		
		PointF startPos = edge.mPolyline.get(0);
		PointF endPos = edge.mPolyline.get(edge.mPolyline.size() - 1);
		startNode = searchNode( startPos );
		endNode = searchNode( endPos );
		
		if( startNode != null && endNode != null )
		{
			//both nodes of the edge exist
			//start/end nodes reside in different network. triggering a merge
			//merge to a network that has more nodes
			if( startNode.mNetwork != endNode.mNetwork )
			{

				Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Number of Network before mergeing " + mNetworks.size());
				Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Merge two networks (" + startNode.mNetwork.getNumNodes() + "," + endNode.mNetwork.getNumNodes() + ")");
				
				if( startNode.mNetwork.getNumNodes() > endNode.mNetwork.getNumNodes() )
				{
					//cache the network which endNode reside in for later removing
					//from the Map's network collection
					ReNetwork endNetwork = endNode.mNetwork;
					startNode.mNetwork.merge( endNode.mNetwork );
					
					//remove the network that has been merged to another
					boolean result = mNetworks.remove(endNetwork);
					
					if( result == false )
					{
						//some thing is wrong, the network should exist
						Log.e(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Try to removed the merged ReNetwork from ReMap, however, it is not existed in the map");
						throw new RuntimeException();
					}
				}
				else
				{
					//cache the network which startNode reside in for later removing
					//from the Map's network collection
					ReNetwork startNetwork = startNode.mNetwork;
					endNode.mNetwork.merge( startNode.mNetwork );
					
					//remove the network that has been merged to another
					boolean result = mNetworks.remove(startNetwork);
					if( result == false )
					{
						//some thing is wrong, the network should exist
						Log.e(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Try to removed the merged ReNetwork from ReMap, however, it is not existed in the map");
						throw new RuntimeException();
					}

				}
				
				Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Number of Network after mergeing " + mNetworks.size());
					
			}
		}
		else if( startNode != null )
		{
			endNode = new ReNode( endPos );
			
			//add the endNode to the startNode's network
			startNode.mNetwork.addNode(endNode);
			
			Log.d( ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Create a new ReNode to an existed Network at ( " + endNode.mPosition.x + "," + endNode.mPosition.y + ")" );
		}
		else if( endNode != null )
		{
			startNode = new ReNode( startPos );
		
			//add the startNode to the endNode's network
			endNode.mNetwork.addNode(startNode);

			Log.d( ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Create a new ReNode to an existed Network at ( " + startNode.mPosition.x + "," + startNode.mPosition.y + ")" );
		}
		else
		{
			//both nodes are not existed in any network. create a new network
			ReNetwork netWork = new ReNetwork();
			if( ReUtil.PointEqual(startPos, endPos, ReUtil.POINT_COMPARE_LENGTH))
			{
				//this is a loop edge, let's verify that the edge is not zero length
				if( edge.mLength < ReUtil.MIN_EDGE_LENGTH )
				{
					Log.e( ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Find a loop edge with ZERO distance same at: ( " + startPos.x + "," + startPos.y + ")" );
					throw new RuntimeException();
				}
				//this is a loop edge, so we just add the startNode, which is the same as the endNode
				Log.d( ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "A loop edge with start and end position the same at: ( " + startPos.x + "," + startPos.y + ")" );
				startNode = new ReNode( startPos );
				endNode = startNode;
				netWork.addNode( startNode );
			}
			else
			{
				endNode = new ReNode( endPos );
				startNode = new ReNode( startPos );
				netWork.addNode( startNode );
				netWork.addNode( endNode );	
			}
			
			mNetworks.add( netWork );			
			Log.d( ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Created a new ReNetwork -- Total Number of Network: " + mNetworks.size() );
		}
		
		//the startNode and endNode must reside in the same network
		//set the nodes of the edge and add the edge to the network
		edge.mStartNode = startNode;
		edge.mEndNode = endNode;
		if( startNode != endNode )
		{
			//added edge to startNode and endNode if it is not a loop edge
			startNode.addEdge( edge );
			endNode.addEdge( edge );
		}
		else
		{
			startNode.addEdge( edge );
		}
		
		startNode.mNetwork.addEdge( edge );
		
		Log.d( ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Added a new ReEdge: (" + + startNode.mPosition.x + "," + startNode.mPosition.y + ")---(" +
				+ endNode.mPosition.x + "," + endNode.mPosition.y + ")" );

	}
	
	//search the startNode and endNode of a gvien edge
	private ReNode searchNode( PointF pos )
	{
		for( ReNetwork network : mNetworks )
		{
			ReNode node = network.searchNode(pos);
			if( node != null )
			{
				return node;
			}
		}
		
		return null;
	}
	
	public void verifyTopology()
	{
		int idx = 0;
		for( ReNetwork network : mNetworks )
		{
			Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Start Verifying Topology for Network #" + idx + "with #" + network.mNodes.size() + "Nodes");
			network.verifyTopology();
			Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Finish Verify Topology for Network #" + idx + "with #" + network.mNodes.size() + "Nodes");
			++idx;
		}
	}
}

