package com.reconinstruments.navigation.routing;

import java.util.ArrayList;

import android.graphics.PointF;
import android.util.Log;

public class ReVerifyInfoProvider
{
	/**
	 * 
	 * A utility class for keeping the POI  category description(Category name+number of POI-item in this category)
	 * and the poi-type
	 */
	static public class NetworkInfo
	{
		public NetworkInfo( String desc, PointF center )
		{
			mDesc = desc;
			mCenter = center; 
		}
		
		/*
		 * Override the toString( ) to provide the string 
		 * Requested by ArrayAdapter to feed the correct content for rendering
		 * in a listView 
		 */
		@Override
		public String toString()
		{
			return mDesc;
		}
		
		public String mDesc;					//the description of the poi-category
		public PointF   mCenter;					//the type of point-of-interest
	}
	
	
	/**
	 * 
	 * describe some location info, such as the network center, the problematic node
	 *
	 */
	static public class LocationInfo
	{
		
		public LocationInfo( String desc, PointF pos )
		{
			mDesc = desc;
			mPosition = pos; 
		}
		
		/*
		 * Override the toString( ) to provide the string 
		 * Requested by ArrayAdapter to feed the correct content for rendering
		 * in a listView 
		 */
		@Override
		public String toString()
		{
			return mDesc;
		}
		
		public String mDesc;
		public PointF mPosition;
		
	}
	
	static public  ArrayList<NetworkInfo> sNetworkInfo = new ArrayList<NetworkInfo>( 16 );
	static public  ArrayList<ArrayList<LocationInfo>> sNetworkLocationInfo = null;
	
	static public void reset( )
	{
		sNetworkInfo.clear();

		if( sNetworkLocationInfo != null )
		{
			for( ArrayList<LocationInfo> names : sNetworkLocationInfo )
			{
				names.clear();
			}
			sNetworkLocationInfo.clear();
		}
	}
	
	static public void fill( ReMap map )
	{
		if( sNetworkLocationInfo == null )
		{
			sNetworkLocationInfo = new ArrayList<ArrayList<LocationInfo>>( 16 );	
		}
		else
		{
			reset( );
		}
	
		int netcount = 0;
		for( ReNetwork network : map.mNetworks )
		{
			ArrayList<LocationInfo> networkInfo = new ArrayList<LocationInfo>( );
			sNetworkLocationInfo.add( networkInfo );
			
			LocationInfo info = new LocationInfo( "Center", new PointF( network.mBBox.centerX(), network.mBBox.centerY()));
			
			networkInfo.add( info );
			
			int count = 0;
			for( ReNode node : network.mNodes )
			{
				//this node has no in-come edge, 
				//then it is not reachable by any other node in the network
				//lets diagnositic it
				if( node.mInEdges.size() == 0 )
				{
					String desc = "Node #"+count +": ";
					Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "Isolated Node# "+count+"...");
					int edgeIdx = 0;
					for( ReEdge edge : node.mOutEdges )
					{
						Log.d(ReUtil.LOG_TAG_NETWORK_CONSTRUCT, "		---Outer Edge#"+edgeIdx+": " +edge.mName+", Type: " + edge.mRoadType);
						++edgeIdx;
						desc = desc + "--Edge #" + edgeIdx + ": " + edge.mName + "(" + edge.mRoadType + ")";  
					}
					
					LocationInfo nodeInfo  = new LocationInfo( desc, node.mPosition );
					networkInfo.add( nodeInfo );	

					++count;
				}
								
			}
			
			int segments = 0;
			for( ReEdge edge : network.mEdges )
			{
				segments += edge.mPolyline.size() -1;
			}
			
			NetworkInfo netInfo = new NetworkInfo( "Network #"+netcount + "( nodes:" + network.mNodes.size() +
					", edges:" + network.mEdges.size() + ", segments:" + segments + 
					", isolated:"  +  count +")", new PointF( ));			
			sNetworkInfo.add(netInfo);
			
			++netcount;
		}	
	}	
}