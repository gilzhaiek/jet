/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This is a utility class for defining a 2D rectangle with co-ordinator of double
 *Using this class to avoid of introducing the dependence on android.graphics.RectF
 *so that the com.recon.shp and com.recon.dbf can be a pure java package independent
 *of andriod
 *Author: Hongzhi Wang at 2011 
 */
package com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp;

public class RectangleD
{
	public PointD topLeft;
	public PointD bottomRight;
	
	public RectangleD( )
	{
		topLeft = new PointD( );
		bottomRight = new PointD( );
	}
	
	public RectangleD( PointD topLeft, PointD bottomRight )
	{
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}
	
	public RectangleD( double tlX, double tlY, double brX, double brY )
	{
		this.topLeft = new PointD( tlX, tlY );
		this.bottomRight = new PointD( brX, brY );
	}
}