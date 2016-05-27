/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.routing;

import android.graphics.PointF;

/**
 * Util class for routing-engine constants and functions
 */
public class ReUtil
{
	static final double DISTANCE_PER_PIXEL = 0.5;		//the distance in meter that a pixel will be mapped to initially
	
	//log tags for routing-engine
	static final String LOG_TAG_NETWORK_CONSTRUCT = "RE Construction";		//Map content related info/debug/warning/error
	
	static final boolean RE_CONSTRUCT_VERIFY_ON = false; 
	
	static final double FLOAT_COMPARE_EPSILON = 1e-5;
	
	static final float POINT_COMPARE_LENGTH = 0.01f;
	
	static final double MIN_EDGE_LENGTH = 1e-5;
	
	//the distance of two ReNode's are less than this, they are treated as the
	//same ReNode
	static final float MIN_RENODE_DISTANCE = 1.f/(float)DISTANCE_PER_PIXEL;
	
	static public final boolean RE_VERIFY_TOPOLOGY = true;
	
	static public boolean FloatEqual( float f1, float f2, float epsilon )
	{
		if( f1 > f2 - epsilon  && f1 < f2 + epsilon )
			return true;
		else
			return false;
	}
	
	static public boolean PointEqual( PointF p1, PointF p2, float epsilon )
	{
		float x = p1.x - p2.x;
		float y = p1.y - p2.y;
		
		if( (x*x + y*y) < (epsilon*epsilon) )
			return true;
		else
			return false;
		
	}
}