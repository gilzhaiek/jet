/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.routing;

import java.util.ArrayList;

import android.graphics.PointF;
import android.util.Log;

/**
 * This class defined a route-engine manager.
 * A ReManager is in-charge-of:
 *   1. constructing a ReMap
 *   2. planning a route
 *   3. something else that I have not thought of yet:)
 */

public class ReManager
{
	public ReMap mMap;
	
	public ReManager( )
	{
		mMap = new ReMap();
	}
	
	public void addEdge( ArrayList<PointF> points, int roadType, float speedLimit, boolean oneWay, String name  )
	{
		mMap.addEdge( points, roadType, speedLimit, oneWay, name );
	}
	
	public void dumpNetworks()
	{
		if( ReUtil.RE_CONSTRUCT_VERIFY_ON )
		{
			Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Number of Networks in the map: " + mMap.mNetworks.size());
			int i = 0;
			for( ReNetwork network : mMap.mNetworks  )
			{
				Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Network #" + i + ": Nodes# " + network.mNodes.size() + " Edges#" + network.mEdges.size() );
				++i;
				
				int numSegments = 0;
				for( ReEdge edge : network.mEdges )
				{
					numSegments += edge.mPolyline.size() - 1;
				}
				
				Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Network #" + numSegments + ": Segments" );
			}
		}
	}
	
	public void verifyTopology( )
	{		
		ReVerifyInfoProvider.fill(mMap);
		//mMap.verifyTopology();
	}
	
	public void reset( )
	{
		if( mMap != null )
		{
			mMap.reset();
		}
	}
	
	public void clear()
	{
		if( mMap != null )
		{
			mMap.clear( );
		}
	}
}
