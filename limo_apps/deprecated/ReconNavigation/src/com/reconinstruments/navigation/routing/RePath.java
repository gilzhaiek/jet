/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.routing;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;


/**
 * This class defined an routing path finded by the routing engine
 * A RePath consists of a set of ReNode and ReEdge. The first ReNode
 * is the starting node in the Network, and the last node is the 
 * destinational node in the network. ReEdge connects all the ReNode
 * together
 */
public class RePath
{
	public ArrayList<ReNode> mNodes = null;
	public ArrayList<ReEdge> mEdges = null;
	
	static private RectF sTempRect = new RectF();
	
	public float mLength = 0;
	
	public RePath( int numNodes, int numEdges )
	{
		mNodes = new ArrayList<ReNode>( numNodes );
		mEdges = new ArrayList<ReEdge>( numEdges );
		
	}
	
	public RePath( )
	{
		mNodes = new ArrayList<ReNode>( 32 );
		mEdges = new ArrayList<ReEdge>( 32 );		
	}
	
	public void reset( )
	{
		mNodes.clear();
		mEdges.clear();
		mLength = 0;
	}
	
	public void addNode( ReNode node )
	{
		mNodes.add( node );
	}
	
	public void addEdge( ReEdge edge )
	{
		mEdges.add( edge );
		mLength += edge.mLength;
	}
		

	
	public void drawPath( Canvas canvas, Matrix transform )
	{
		//the current viewport of canvas
		float clipRight = canvas.getWidth();
		float clipBottom = canvas.getHeight();
	
/*		
		for( ReEdge edge: mEdges )
		{
			//transform the bounding box of the trail			
			transform.mapRect(sTempRect, edge.mRenderer.getBBox());

			//render the trail only if its bounding box
			//is intersected with the viewport
			if( sTempRect.intersects(0, 0, clipRight, clipBottom) )
			{
				edge.mRenderer.drawHilite(canvas, transform);
			}
		}
*/			
	}
}
